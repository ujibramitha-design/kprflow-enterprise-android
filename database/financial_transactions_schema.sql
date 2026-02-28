-- =====================================================
-- ATOMIC FINANCIAL TRANSACTIONS SCHEMA
-- Real-time Financial Analytics with Role-based Validation
-- =====================================================

-- =====================================================
-- TRANSACTION TYPES AND STATUS ENUMS
-- =====================================================

CREATE TYPE transaction_type AS ENUM (
    'BOOKING',      -- Booking fee payment
    'DP',           -- Down payment
    'DISBURSEMENT', -- KPR disbursement from bank
    'REFUND',       -- Refund payment
    'PENALTY',      -- Penalty charges
    'BONUS'         -- Bonus or incentive
);

CREATE TYPE transaction_status AS ENUM (
    'PENDING',      -- Waiting for verification
    'VERIFIED',     -- Verified by Finance team
    'REJECTED',     -- Rejected by Finance team
    'PROCESSING'    -- Being processed
);

-- =====================================================
-- FINANCIAL TRANSACTION TABLE
-- =====================================================

CREATE TABLE "FinancialTransaction" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dossier_id UUID NOT NULL REFERENCES "KprDossier"(id) ON DELETE CASCADE,
    type transaction_type NOT NULL,
    nominal DECIMAL(15, 2) NOT NULL CHECK (nominal >= 0),
    status transaction_status DEFAULT 'PENDING',
    description TEXT,
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by UUID REFERENCES "UserProfile"(id), -- Role FINANCE [cite: 6]
    rejected_at TIMESTAMP WITH TIME ZONE,
    rejected_by UUID REFERENCES "UserProfile"(id),
    rejection_reason TEXT,
    payment_method TEXT,
    bank_reference TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- FINANCIAL METRICS VIEW
-- Real-time calculation without app overhead
-- =====================================================

CREATE OR REPLACE VIEW v_financial_analytics AS
SELECT 
    d.id AS dossier_id,
    u.name AS customer_name,
    u.email AS customer_email,
    d.status AS dossier_status,
    d.booking_date,
    up.id AS unit_id,
    up.type AS unit_type,
    up.block AS unit_block,
    up.unit_number AS unit_number,
    up.price AS unit_price,
    
    -- Total dana yang benar-benar sudah masuk (Verified)
    (SELECT COALESCE(SUM(nominal), 0) 
     FROM "FinancialTransaction" 
     WHERE dossier_id = d.id AND status = 'VERIFIED') AS realized_cash,
    
    -- Potensi dana jika SP3K sudah terbit tapi belum cair
    CASE 
        WHEN d.status = 'SP3K_ISSUED' THEN 
            COALESCE(up.price, 0)
        WHEN d.status = 'PUTUSAN_KREDIT_ACC' THEN 
            COALESCE(up.price, 0) * 0.8 -- 80% projected for approved credit
        ELSE 0 
    END AS projected_cash,
    
    -- Total pending transactions
    (SELECT COALESCE(SUM(nominal), 0) 
     FROM "FinancialTransaction" 
     WHERE dossier_id = d.id AND status = 'PENDING') AS pending_cash,
    
    -- Transaction breakdown by type
    (SELECT COALESCE(SUM(nominal), 0) 
     FROM "FinancialTransaction" 
     WHERE dossier_id = d.id AND type = 'BOOKING' AND status = 'VERIFIED') AS booking_cash,
    
    (SELECT COALESCE(SUM(nominal), 0) 
     FROM "FinancialTransaction" 
     WHERE dossier_id = d.id AND type = 'DP' AND status = 'VERIFIED') AS dp_cash,
    
    (SELECT COALESCE(SUM(nominal), 0) 
     FROM "FinancialTransaction" 
     WHERE dossier_id = d.id AND type = 'DISBURSEMENT' AND status = 'VERIFIED') AS disbursement_cash,
    
    -- Transaction counts
    (SELECT COUNT(*) 
     FROM "FinancialTransaction" 
     WHERE dossier_id = d.id AND status = 'VERIFIED') AS verified_transactions,
    
    (SELECT COUNT(*) 
     FROM "FinancialTransaction" 
     WHERE dossier_id = d.id AND status = 'PENDING') AS pending_transactions,
    
    -- Last transaction date
    (SELECT MAX(created_at) 
     FROM "FinancialTransaction" 
     WHERE dossier_id = d.id) AS last_transaction_date,
    
    -- Financial health indicator
    CASE 
        WHEN (SELECT COALESCE(SUM(nominal), 0) 
             FROM "FinancialTransaction" 
             WHERE dossier_id = d.id AND status = 'VERIFIED') >= 
             COALESCE(up.price, 0) * 0.3 THEN 'HEALTHY'
        WHEN (SELECT COALESCE(SUM(nominal), 0) 
             FROM "FinancialTransaction" 
             WHERE dossier_id = d.id AND status = 'VERIFIED') >= 
             COALESCE(up.price, 0) * 0.1 THEN 'MODERATE'
        ELSE 'AT_RISK'
    END AS financial_health,
    
    -- Completion percentage based on payments
    CASE 
        WHEN COALESCE(up.price, 0) = 0 THEN 0
        ELSE ROUND(
            ((SELECT COALESCE(SUM(nominal), 0) 
              FROM "FinancialTransaction" 
              WHERE dossier_id = d.id AND status = 'VERIFIED') / 
             COALESCE(up.price, 0)) * 100, 2
        )
    END AS payment_completion_percentage
    
FROM "KprDossier" d
JOIN "UserProfile" u ON d.user_id::text = u.id::text
LEFT JOIN "UnitProperty" up ON d.unit_id::text = up.id::text;

-- =====================================================
-- FINANCIAL SUMMARY VIEWS
-- =====================================================

-- Executive summary for BOD
CREATE OR REPLACE VIEW v_financial_executive_summary AS
SELECT 
    -- Total metrics
    COUNT(*) AS total_dossiers,
    COUNT(CASE WHEN realized_cash > 0 THEN 1 END) AS dossiers_with_payments,
    
    -- Cash metrics
    SUM(realized_cash) AS total_realized_cash,
    SUM(projected_cash) AS total_projected_cash,
    SUM(pending_cash) AS total_pending_cash,
    
    -- Transaction metrics
    SUM(booking_cash) AS total_booking_cash,
    SUM(dp_cash) AS total_dp_cash,
    SUM(disbursement_cash) AS total_disbursement_cash,
    
    -- Health metrics
    COUNT(CASE WHEN financial_health = 'HEALTHY' THEN 1 END) AS healthy_dossiers,
    COUNT(CASE WHEN financial_health = 'MODERATE' THEN 1 END) AS moderate_dossiers,
    COUNT(CASE WHEN financial_health = 'AT_RISK' THEN 1 END) AS at_risk_dossiers,
    
    -- Average metrics
    AVG(realized_cash) AS avg_realized_cash,
    AVG(payment_completion_percentage) AS avg_completion_percentage,
    
    -- Status breakdown
    COUNT(CASE WHEN dossier_status = 'LEAD' THEN 1 END) AS lead_count,
    COUNT(CASE WHEN dossier_status = 'PEMBERKASAN' THEN 1 END) AS document_count,
    COUNT(CASE WHEN dossier_status = 'PROSES_BANK' THEN 1 END) AS bank_count,
    COUNT(CASE WHEN dossier_status = 'PUTUSAN_KREDIT_ACC' THEN 1 END) AS approved_count,
    COUNT(CASE WHEN dossier_status = 'SP3K_ISSUED' THEN 1 END) AS sp3k_count,
    COUNT(CASE WHEN dossier_status = 'FUNDS_DISBURSED' THEN 1 END) AS disbursed_count,
    
    -- Date metrics
    CURRENT_DATE AS report_date,
    EXTRACT(MONTH FROM CURRENT_DATE) AS current_month,
    EXTRACT(YEAR FROM CURRENT_DATE) AS current_year
    
FROM v_financial_analytics;

-- Monthly financial trends
CREATE OR REPLACE VIEW v_financial_monthly_trends AS
SELECT 
    DATE_TRUNC('month', d.booking_date) AS month,
    COUNT(*) AS new_dossiers,
    SUM(realized_cash) AS monthly_realized_cash,
    SUM(projected_cash) AS monthly_projected_cash,
    SUM(booking_cash) AS monthly_booking_cash,
    SUM(dp_cash) AS monthly_dp_cash,
    SUM(disbursement_cash) AS monthly_disbursement_cash,
    AVG(payment_completion_percentage) AS avg_completion_percentage,
    COUNT(CASE WHEN financial_health = 'HEALTHY' THEN 1 END) AS healthy_count,
    COUNT(CASE WHEN financial_health = 'AT_RISK' THEN 1 END) AS at_risk_count
    
FROM v_financial_analytics
WHERE d.booking_date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '12 months')
GROUP BY DATE_TRUNC('month', d.booking_date)
ORDER BY month DESC;

-- Pending transactions for Finance team
CREATE OR REPLACE VIEW v_pending_transactions AS
SELECT 
    ft.id,
    ft.dossier_id,
    fa.customer_name,
    fa.customer_email,
    ft.type,
    ft.nominal,
    ft.description,
    ft.payment_method,
    ft.bank_reference,
    ft.created_at,
    fa.dossier_status,
    fa.unit_type,
    fa.unit_block,
    fa.unit_number,
    fa.unit_price,
    fa.financial_health,
    fa.payment_completion_percentage
    
FROM "FinancialTransaction" ft
JOIN v_financial_analytics fa ON ft.dossier_id = fa.dossier_id
WHERE ft.status = 'PENDING'
ORDER BY ft.created_at DESC;

-- =====================================================
-- TRIGGERS FOR FINANCIAL VALIDATION
-- =====================================================

-- Function to validate transaction amounts
CREATE OR REPLACE FUNCTION validate_transaction_amount()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate booking fee (max 5% of unit price)
    IF NEW.type = 'BOOKING' THEN
        DECLARE
            unit_price DECIMAL;
        BEGIN
            SELECT price INTO unit_price 
            FROM "UnitProperty" 
            WHERE id = (SELECT unit_id FROM "KprDossier" WHERE id = NEW.dossier_id);
            
            IF unit_price IS NOT NULL AND NEW.nominal > (unit_price * 0.05) THEN
                RAISE EXCEPTION 'Booking fee cannot exceed 5%% of unit price';
            END IF;
        END;
    END IF;
    
    -- Validate DP (min 10% of unit price)
    IF NEW.type = 'DP' THEN
        DECLARE
            unit_price DECIMAL;
            booking_paid DECIMAL;
        BEGIN
            SELECT price INTO unit_price 
            FROM "UnitProperty" 
            WHERE id = (SELECT unit_id FROM "KprDossier" WHERE id = NEW.dossier_id);
            
            SELECT COALESCE(SUM(nominal), 0) INTO booking_paid
            FROM "FinancialTransaction" 
            WHERE dossier_id = NEW.dossier_id AND type = 'BOOKING' AND status = 'VERIFIED';
            
            IF unit_price IS NOT NULL AND NEW.nominal < ((unit_price * 0.1) - booking_paid) THEN
                RAISE EXCEPTION 'DP must be at least 10%% of unit price (minus booking fee)';
            END IF;
        END;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply validation trigger
CREATE TRIGGER trigger_validate_transaction_amount
    BEFORE INSERT OR UPDATE ON "FinancialTransaction"
    FOR EACH ROW EXECUTE FUNCTION validate_transaction_amount();

-- Function to update dossier financial status
CREATE OR REPLACE FUNCTION update_dossier_financial_status()
RETURNS TRIGGER AS $$
BEGIN
    -- Update dossier status based on financial transactions
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        IF NEW.status = 'VERIFIED' THEN
            -- Check if all required payments are complete
            DECLARE
                total_payments DECIMAL;
                unit_price DECIMAL;
                dossier_status TEXT;
            BEGIN
                SELECT COALESCE(SUM(nominal), 0), fa.unit_price, fa.dossier_status
                INTO total_payments, unit_price, dossier_status
                FROM "FinancialTransaction" ft
                JOIN v_financial_analytics fa ON ft.dossier_id = fa.dossier_id
                WHERE ft.dossier_id = NEW.dossier_id AND ft.status = 'VERIFIED'
                GROUP BY fa.unit_price, fa.dossier_status;
                
                -- Auto-advance dossier status based on payments
                IF unit_price IS NOT NULL AND total_payments >= (unit_price * 0.15) THEN
                    UPDATE "KprDossier" 
                    SET status = 'PEMBERKASAN' 
                    WHERE id = NEW.dossier_id AND status = 'LEAD';
                END IF;
            END;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply financial status trigger
CREATE TRIGGER trigger_update_dossier_financial_status
    AFTER INSERT OR UPDATE ON "FinancialTransaction"
    FOR EACH ROW EXECUTE FUNCTION update_dossier_financial_status();

-- =====================================================
-- RLS POLICIES FOR FINANCIAL DATA
-- =====================================================

ALTER TABLE "FinancialTransaction" ENABLE ROW LEVEL SECURITY;

-- Finance can view all transactions
CREATE POLICY "Finance view all transactions" ON "FinancialTransaction"
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- BOD can view all transactions
CREATE POLICY "BOD view all transactions" ON "FinancialTransaction"
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Marketing can view transaction summaries for their customers
CREATE POLICY "Marketing view transaction summaries" ON "FinancialTransaction"
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'MARKETING' AND
        dossier_id IN (
            SELECT id FROM "KprDossier" 
            WHERE assigned_to::text = auth.uid()::text
        )
    );

-- Legal can view transactions for legal review
CREATE POLICY "Legal view transactions" ON "FinancialTransaction"
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Finance can insert transactions
CREATE POLICY "Finance can insert transactions" ON "FinancialTransaction"
    FOR INSERT WITH CHECK (auth.jwt() ->> 'role' = 'FINANCE');

-- Finance can update transactions (verify/reject)
CREATE POLICY "Finance can update transactions" ON "FinancialTransaction"
    FOR UPDATE USING (auth.jwt() ->> 'role' = 'FINANCE');

-- RLS for views
ALTER VIEW v_financial_analytics SET (security_barrier = true);
ALTER VIEW v_financial_executive_summary SET (security_barrier = true);
ALTER VIEW v_pending_transactions SET (security_barrier = true);

-- View policies
CREATE POLICY "Finance view financial analytics" ON v_financial_analytics
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('FINANCE', 'BOD'));

CREATE POLICY "BOD view executive summary" ON v_financial_executive_summary
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

CREATE POLICY "Finance view pending transactions" ON v_pending_transactions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_financial_transactions_dossier_id ON "FinancialTransaction"(dossier_id);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_status ON "FinancialTransaction"(status);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_type ON "FinancialTransaction"(type);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_created_at ON "FinancialTransaction"(created_at);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_nominal ON "FinancialTransaction"(nominal);

-- =====================================================
-- SAMPLE QUERIES FOR TESTING
-- =====================================================

-- Get financial analytics for all dossiers
-- SELECT * FROM v_financial_analytics ORDER BY realized_cash DESC;

-- Get executive summary
-- SELECT * FROM v_financial_executive_summary;

-- Get pending transactions for Finance
-- SELECT * FROM v_pending_transactions;

-- Create a new transaction (Finance only)
-- INSERT INTO "FinancialTransaction" (dossier_id, type, nominal, description) 
-- VALUES ('dossier-uuid', 'BOOKING', 5000000, 'Initial booking fee');

-- Verify transaction (Finance only)
-- UPDATE "FinancialTransaction" 
-- SET status = 'VERIFIED', verified_at = NOW(), verified_by = 'finance-user-uuid'
-- WHERE id = 'transaction-uuid';

-- Get monthly trends
-- SELECT * FROM v_financial_monthly_trends ORDER BY month DESC;
