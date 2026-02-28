-- Payment Schedules tables for Phase 16: Customer Mobile App

-- Payment Schedules table
CREATE TABLE IF NOT EXISTS payment_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    total_amount DECIMAL(15,2) NOT NULL,
    down_payment DECIMAL(15,2) NOT NULL,
    loan_amount DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    loan_term_months INTEGER NOT NULL,
    monthly_payment DECIMAL(15,2) NOT NULL,
    start_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- 'ACTIVE', 'COMPLETED', 'CANCELLED'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Payment Installments table
CREATE TABLE IF NOT EXISTS payment_installments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    schedule_id UUID NOT NULL REFERENCES payment_schedules(id) ON DELETE CASCADE,
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL,
    interest_amount DECIMAL(15,2) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    remaining_balance DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'PARTIALLY_PAID', 'PAID', 'OVERDUE'
    paid_amount DECIMAL(15,2) DEFAULT 0.00,
    paid_at DATE,
    payment_method VARCHAR(50),
    payment_reference VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Payment Transactions table
CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    installment_id UUID REFERENCES payment_installments(id) ON DELETE SET NULL,
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    amount DECIMAL(15,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(100),
    bank_reference VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_payment_schedules_dossier_id ON payment_schedules(dossier_id);
CREATE INDEX IF NOT EXISTS idx_payment_schedules_status ON payment_schedules(status);
CREATE INDEX IF NOT EXISTS idx_payment_schedules_start_date ON payment_schedules(start_date);

CREATE INDEX IF NOT EXISTS idx_payment_installments_schedule_id ON payment_installments(schedule_id);
CREATE INDEX IF NOT EXISTS idx_payment_installments_dossier_id ON payment_installments(dossier_id);
CREATE INDEX IF NOT EXISTS idx_payment_installments_due_date ON payment_installments(due_date);
CREATE INDEX IF NOT EXISTS idx_payment_installments_status ON payment_installments(status);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_dossier_id ON payment_transactions(dossier_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_status ON payment_transactions(status);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_created_at ON payment_transactions(created_at);

-- RLS Policies for payment schedules
ALTER TABLE payment_schedules ENABLE ROW LEVEL SECURITY;

-- Customers can view their own payment schedules
CREATE POLICY "Customers can view own payment schedules" ON payment_schedules
    FOR SELECT USING (
        auth.uid()::text IN (
            SELECT user_id::text FROM kpr_dossiers WHERE id = dossier_id
        )
    );

-- Finance can manage payment schedules
CREATE POLICY "Finance can manage payment schedules" ON payment_schedules
    FOR ALL USING (auth.jwt() ->> 'role' = 'FINANCE');

-- BOD can view all payment schedules
CREATE POLICY "BOD can view all payment schedules" ON payment_schedules
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- RLS Policies for payment installments
ALTER TABLE payment_installments ENABLE ROW LEVEL SECURITY;

-- Customers can view their own payment installments
CREATE POLICY "Customers can view own payment installments" ON payment_installments
    FOR SELECT USING (
        auth.uid()::text IN (
            SELECT user_id::text FROM kpr_dossiers WHERE id = dossier_id
        )
    );

-- Finance can manage payment installments
CREATE POLICY "Finance can manage payment installments" ON payment_installments
    FOR ALL USING (auth.jwt() ->> 'role' = 'FINANCE');

-- BOD can view all payment installments
CREATE POLICY "BOD can view all payment installments" ON payment_installments
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- RLS Policies for payment transactions
ALTER TABLE payment_transactions ENABLE ROW LEVEL SECURITY;

-- Customers can view their own payment transactions
CREATE POLICY "Customers can view own payment transactions" ON payment_transactions
    FOR SELECT USING (
        auth.uid()::text IN (
            SELECT user_id::text FROM kpr_dossiers WHERE id = dossier_id
        )
    );

-- Finance can manage payment transactions
CREATE POLICY "Finance can manage payment transactions" ON payment_transactions
    FOR ALL USING (auth.jwt() ->> 'role' = 'FINANCE');

-- BOD can view all payment transactions
CREATE POLICY "BOD can view all payment transactions" ON payment_transactions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Function to calculate monthly payment
CREATE OR REPLACE FUNCTION calculate_monthly_payment(
    p_loan_amount DECIMAL(15,2),
    p_interest_rate DECIMAL(5,2),
    p_loan_term_months INTEGER
)
RETURNS DECIMAL(15,2) AS $$
DECLARE
    monthly_rate DECIMAL(10,8);
BEGIN
    monthly_rate := p_interest_rate / 100 / 12;
    
    IF monthly_rate = 0 THEN
        RETURN p_loan_amount / p_loan_term_months;
    END IF;
    
    RETURN p_loan_amount * (monthly_rate * POWER(1 + monthly_rate, p_loan_term_months)) /
           (POWER(1 + monthly_rate, p_loan_term_months) - 1);
END;
$$ LANGUAGE plpgsql;

-- Function to get payment summary
CREATE OR REPLACE FUNCTION get_payment_summary(p_dossier_id UUID)
RETURNS TABLE (
    total_amount DECIMAL(15,2),
    down_payment DECIMAL(15,2),
    loan_amount DECIMAL(15,2),
    total_paid DECIMAL(15,2),
    total_remaining DECIMAL(15,2),
    paid_installments INTEGER,
    total_installments INTEGER,
    overdue_installments INTEGER,
    completion_percentage DECIMAL(5,2),
    next_payment_date DATE,
    next_payment_amount DECIMAL(15,2)
) AS $$
BEGIN
    RETURN QUERY
    WITH payment_data AS (
        SELECT 
            ps.total_amount,
            ps.down_payment,
            ps.loan_amount,
            COALESCE(SUM(pi.paid_amount), 0) as total_paid,
            COUNT(*) FILTER (WHERE pi.status = 'PAID') as paid_installments,
            COUNT(*) as total_installments,
            COUNT(*) FILTER (
                WHERE pi.status != 'PAID' 
                AND pi.due_date < CURRENT_DATE
            ) as overdue_installments,
            MIN(pi.due_date) FILTER (WHERE pi.status != 'PAID') as next_payment_date,
            MIN(pi.total_amount) FILTER (WHERE pi.status != 'PAID') as next_payment_amount
        FROM payment_schedules ps
        LEFT JOIN payment_installments pi ON ps.id = pi.schedule_id
        WHERE ps.dossier_id = p_dossier_id
        GROUP BY ps.total_amount, ps.down_payment, ps.loan_amount
    )
    SELECT 
        pd.total_amount,
        pd.down_payment,
        pd.loan_amount,
        pd.total_paid,
        (pd.total_amount - pd.total_paid) as total_remaining,
        pd.paid_installments,
        pd.total_installments,
        pd.overdue_installments,
        CASE 
            WHEN pd.total_amount > 0 THEN 
                ROUND((pd.total_paid / pd.total_amount) * 100, 2)
            ELSE 0 
        END as completion_percentage,
        pd.next_payment_date,
        pd.next_payment_amount
    FROM payment_data pd;
END;
$$ LANGUAGE plpgsql;

-- Function to get overdue payments
CREATE OR REPLACE FUNCTION get_overdue_payments(p_dossier_id UUID)
RETURNS TABLE (
    installment_id UUID,
    installment_number INTEGER,
    due_date DATE,
    total_amount DECIMAL(15,2),
    paid_amount DECIMAL(15,2),
    overdue_days INTEGER,
    overdue_amount DECIMAL(15,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        pi.id as installment_id,
        pi.installment_number,
        pi.due_date,
        pi.total_amount,
        pi.paid_amount,
        (CURRENT_DATE - pi.due_date)::INTEGER as overdue_days,
        (pi.total_amount - pi.paid_amount) as overdue_amount
    FROM payment_installments pi
    WHERE pi.dossier_id = p_dossier_id
    AND pi.status != 'PAID'
    AND pi.due_date < CURRENT_DATE
    ORDER BY pi.due_date;
END;
$$ LANGUAGE plpgsql;

-- Function to process payment
CREATE OR REPLACE FUNCTION process_payment(
    p_installment_id UUID,
    p_payment_amount DECIMAL(15,2),
    p_payment_method VARCHAR(50),
    p_payment_reference VARCHAR(100),
    p_bank_reference VARCHAR(100)
)
RETURNS TABLE (
    success BOOLEAN,
    message TEXT,
    updated_installment_id UUID,
    transaction_id UUID
) AS $$
DECLARE
    installment_record payment_installments%ROWTYPE;
    remaining_amount DECIMAL(15,2);
    new_status VARCHAR(20);
    transaction_id UUID;
BEGIN
    -- Get installment details
    SELECT * INTO installment_record
    FROM payment_installments
    WHERE id = p_installment_id;
    
    IF NOT FOUND THEN
        RETURN QUERY SELECT false, 'Installment not found'::TEXT, NULL::UUID, NULL::UUID;
    END IF;
    
    IF installment_record.status = 'PAID' THEN
        RETURN QUERY SELECT false, 'Installment already paid'::TEXT, NULL::UUID, NULL::UUID;
    END IF;
    
    -- Calculate new paid amount and status
    new_paid_amount := installment_record.paid_amount + p_payment_amount;
    remaining_amount := installment_record.total_amount - new_paid_amount;
    
    IF remaining_amount <= 0 THEN
        new_status := 'PAID';
    ELSE
        new_status := 'PARTIALLY_PAID';
    END IF;
    
    -- Create transaction record
    INSERT INTO payment_transactions (
        installment_id, dossier_id, amount, payment_method, 
        transaction_reference, bank_reference, status
    ) VALUES (
        p_installment_id, installment_record.dossier_id, p_payment_amount,
        p_payment_method, p_payment_reference, p_bank_reference, 'COMPLETED'
    ) RETURNING id INTO transaction_id;
    
    -- Update installment
    UPDATE payment_installments
    SET 
        paid_amount = new_paid_amount,
        status = new_status,
        paid_at = CURRENT_DATE,
        payment_method = p_payment_method,
        payment_reference = p_payment_reference,
        updated_at = NOW()
    WHERE id = p_installment_id;
    
    -- Check if all installments are paid
    IF new_status = 'PAID' THEN
        DECLARE
            remaining_installments INTEGER;
        BEGIN
            SELECT COUNT(*) INTO remaining_installments
            FROM payment_installments
            WHERE schedule_id = installment_record.schedule_id
            AND status != 'PAID';
            
            IF remaining_installments = 0 THEN
                UPDATE payment_schedules
                SET status = 'COMPLETED',
                updated_at = NOW()
                WHERE id = installment_record.schedule_id;
            END IF;
        END;
    END IF;
    
    RETURN QUERY SELECT true, 'Payment processed successfully'::TEXT, p_installment_id::UUID, transaction_id;
END;
$$ LANGUAGE plpgsql;

-- Function to generate payment schedule
CREATE OR REPLACE FUNCTION generate_payment_schedule(
    p_dossier_id UUID,
    p_total_amount DECIMAL(15,2),
    p_down_payment DECIMAL(15,2),
    p_loan_amount DECIMAL(15,2),
    p_interest_rate DECIMAL(5,2),
    p_loan_term_months INTEGER,
    p_start_date DATE
)
RETURNS TABLE (
    success BOOLEAN,
    message TEXT,
    schedule_id UUID
) AS $$
DECLARE
    schedule_id UUID;
    monthly_payment DECIMAL(15,2);
    remaining_balance DECIMAL(15,2);
    current_month INTEGER;
BEGIN
    -- Calculate monthly payment
    monthly_payment := calculate_monthly_payment(p_loan_amount, p_interest_rate, p_loan_term_months);
    
    -- Create payment schedule
    INSERT INTO payment_schedules (
        dossier_id, total_amount, down_payment, loan_amount,
        interest_rate, loan_term_months, monthly_payment, start_date
    ) VALUES (
        p_dossier_id, p_total_amount, p_down_payment, p_loan_amount,
        p_interest_rate, p_loan_term_months, monthly_payment, p_start_date
    ) RETURNING id INTO schedule_id;
    
    -- Generate installments
    remaining_balance := p_loan_amount;
    
    FOR current_month IN 1..p_loan_term_months LOOP
        DECLARE
            due_date DATE;
            principal_amount DECIMAL(15,2);
            interest_amount DECIMAL(15,2);
            installment_id UUID;
        BEGIN
            due_date := p_start_date + (current_month || ' months')::INTERVAL;
            interest_amount := remaining_balance * (p_interest_rate / 100 / 12);
            principal_amount := monthly_payment - interest_amount;
            remaining_balance := remaining_balance - principal_amount;
            
            INSERT INTO payment_installments (
                schedule_id, dossier_id, installment_number, due_date,
                principal_amount, interest_amount, total_amount, remaining_balance
            ) VALUES (
                schedule_id, p_dossier_id, current_month, due_date,
                principal_amount, interest_amount, monthly_payment, remaining_balance
            ) RETURNING id INTO installment_id;
        END;
    END LOOP;
    
    RETURN QUERY SELECT true, 'Payment schedule generated successfully'::TEXT, schedule_id::UUID;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_payment_schedules_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_payment_schedules_updated_at BEFORE UPDATE ON payment_schedules FOR EACH ROW EXECUTE FUNCTION update_payment_schedules_updated_at();

CREATE OR REPLACE FUNCTION update_payment_installments_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_payment_installments_updated_at BEFORE UPDATE ON payment_installments FOR EACH ROW EXECUTE FUNCTION update_payment_installments_updated_at();

CREATE OR REPLACE FUNCTION update_payment_transactions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_payment_transactions_updated_at BEFORE UPDATE ON payment_transactions FOR EACH ROW EXECUTE FUNCTION update_payment_transactions_updated_at();
