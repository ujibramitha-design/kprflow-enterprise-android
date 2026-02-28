-- Financial Tracking System for Phase 12
-- KPRFlow Enterprise

-- Financial Transaction Categories Enum
CREATE TYPE transaction_category AS ENUM (
    'BOOKING_FEE',
    'DP_1',
    'DP_2', 
    'DP_PELUNASAN',
    'BIAYA_STRATEGIS',
    'REFUND',
    'ADMIN_FEE',
    'NOTARY_FEE',
    'INSURANCE_FEE'
);

-- Transaction Status Enum
CREATE TYPE transaction_status AS ENUM (
    'PENDING',
    'VERIFIED',
    'REJECTED',
    'CANCELLED'
);

-- Update financial_transactions table if not exists
ALTER TABLE financial_transactions 
ADD COLUMN IF NOT EXISTS category transaction_category NOT NULL DEFAULT 'BOOKING_FEE',
ADD COLUMN IF NOT EXISTS status transaction_status NOT NULL DEFAULT 'PENDING',
ADD COLUMN IF NOT EXISTS amount NUMERIC(15,2) NOT NULL,
ADD COLUMN IF NOT EXISTS evidence_url TEXT,
ADD COLUMN IF NOT EXISTS is_realized BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS verified_by UUID REFERENCES user_profiles(id),
ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
ADD COLUMN IF NOT EXISTS reference_number VARCHAR(50),
ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20) DEFAULT 'TRANSFER',
ADD COLUMN IF NOT EXISTS bank_name VARCHAR(50),
ADD COLUMN IF NOT EXISTS account_number VARCHAR(50),
ADD COLUMN IF NOT EXISTS account_name VARCHAR(100);

-- Financial Summary View
CREATE OR REPLACE VIEW v_financial_summary AS
SELECT 
    up.project_name,
    ft.category,
    COUNT(*) as transaction_count,
    COALESCE(SUM(CASE WHEN ft.status = 'VERIFIED' AND ft.is_realized = true THEN ft.amount ELSE 0 END), 0) as realized_amount,
    COALESCE(SUM(CASE WHEN ft.status = 'PENDING' THEN ft.amount ELSE 0 END), 0) as pending_amount,
    COALESCE(SUM(CASE WHEN ft.status = 'REJECTED' THEN ft.amount ELSE 0 END), 0) as rejected_amount,
    COALESCE(SUM(ft.amount), 0) as total_amount,
    EXTRACT(MONTH FROM ft.created_at) as month,
    EXTRACT(YEAR FROM ft.created_at) as year
FROM financial_transactions ft
LEFT JOIN unit_properties up ON ft.unit_id = up.id
GROUP BY 
    up.project_name, 
    ft.category, 
    EXTRACT(MONTH FROM ft.created_at),
    EXTRACT(YEAR FROM ft.created_at);

-- Function to create financial transaction
CREATE OR REPLACE FUNCTION create_financial_transaction(
    p_dossier_id UUID,
    p_unit_id UUID,
    p_category transaction_category,
    p_amount NUMERIC(15,2),
    p_payment_method VARCHAR(20) DEFAULT 'TRANSFER',
    p_bank_name VARCHAR(50) DEFAULT NULL,
    p_account_number VARCHAR(50) DEFAULT NULL,
    p_account_name VARCHAR(100) DEFAULT NULL,
    p_reference_number VARCHAR(50) DEFAULT NULL,
    p_evidence_url TEXT DEFAULT NULL,
    p_notes TEXT DEFAULT NULL,
    p_created_by UUID DEFAULT NULL
) RETURNS TABLE (
    success BOOLEAN,
    message TEXT,
    transaction_id UUID
) AS $$
DECLARE
    v_transaction_id UUID;
    v_kpr_status kpr_status;
BEGIN
    -- Validate amount
    IF p_amount <= 0 THEN
        RETURN QUERY SELECT false, 'Amount must be greater than 0', NULL::UUID;
        RETURN;
    END IF;
    
    -- Get current KPR status
    SELECT status INTO v_kpr_status
    FROM kpr_dossiers
    WHERE id = p_dossier_id;
    
    IF NOT FOUND THEN
        RETURN QUERY SELECT false, 'KPR dossier not found', NULL::UUID;
        RETURN;
    END IF;
    
    -- Create transaction
    INSERT INTO financial_transactions (
        dossier_id, unit_id, category, amount, payment_method,
        bank_name, account_number, account_name, reference_number,
        evidence_url, notes, created_by
    ) VALUES (
        p_dossier_id, p_unit_id, p_category, p_amount, p_payment_method,
        p_bank_name, p_account_number, p_account_name, p_reference_number,
        p_evidence_url, p_notes, p_created_by
    ) RETURNING id INTO v_transaction_id;
    
    -- Auto-update KPR status based on payment category
    IF p_category = 'BOOKING_FEE' AND v_kpr_status = 'LEAD' THEN
        UPDATE kpr_dossiers
        SET status = 'PEMBERKASAN',
            updated_at = NOW()
        WHERE id = p_dossier_id;
    ELSIF p_category = 'DP_1' AND v_kpr_status = 'PEMBERKASAN' THEN
        UPDATE kpr_dossiers
        SET status = 'VERIFIKASI_KELALENGKAPAN',
            updated_at = NOW()
        WHERE id = p_dossier_id;
    ELSIF p_category = 'DP_2' AND v_kpr_status = 'VERIFIKASI_KELALENGKAPAN' THEN
        UPDATE kpr_dossiers
        SET status = 'PENGAJUAN_BANK',
            updated_at = NOW()
        WHERE id = p_dossier_id;
    ELSIF p_category = 'DP_PELUNASAN' AND v_kpr_status = 'PENGAJUAN_BANK' THEN
        UPDATE kpr_dossiers
        SET status = 'APPROVAL_BANK',
            updated_at = NOW()
        WHERE id = p_dossier_id;
    END IF;
    
    RETURN QUERY 
    SELECT true, 'Transaction created successfully', v_transaction_id;
END;
$$ LANGUAGE plpgsql;

-- Function to verify transaction
CREATE OR REPLACE FUNCTION verify_financial_transaction(
    p_transaction_id UUID,
    p_verified_by UUID,
    p_is_approved BOOLEAN DEFAULT true,
    p_rejection_reason TEXT DEFAULT NULL
) RETURNS TABLE (
    success BOOLEAN,
    message TEXT
) AS $$
DECLARE
    v_transaction RECORD;
    v_kpr_status kpr_status;
BEGIN
    -- Get transaction details
    SELECT ft.*, kd.status as kpr_status
    INTO v_transaction
    FROM financial_transactions ft
    JOIN kpr_dossiers kd ON ft.dossier_id = kd.id
    WHERE ft.id = p_transaction_id;
    
    IF NOT FOUND THEN
        RETURN QUERY SELECT false, 'Transaction not found';
        RETURN;
    END IF;
    
    -- Check if already verified
    IF v_transaction.is_realized THEN
        RETURN QUERY SELECT false, 'Transaction already verified';
        RETURN;
    END IF;
    
    -- Update transaction
    UPDATE financial_transactions
    SET 
        status = CASE WHEN p_is_approved THEN 'VERIFIED' ELSE 'REJECTED' END,
        is_realized = p_is_approved,
        verified_at = NOW(),
        verified_by = p_verified_by,
        rejection_reason = CASE WHEN p_is_approved THEN NULL ELSE p_rejection_reason END,
        updated_at = NOW()
    WHERE id = p_transaction_id;
    
    -- Send notification to customer
    PERFORM send_whatsapp_notification(
        v_transaction.dossier_id,
        NULL,
        CASE 
            WHEN p_is_approved THEN 'Pembayaran Terverifikasi'
            ELSE 'Pembayaran Ditolak'
        END,
        CASE 
            WHEN p_is_approved THEN 
                format('Pembayaran %s sebesar Rp %s telah terverifikasi. Terima kasih.', 
                    v_transaction.category, 
                    TO_CHAR(v_transaction.amount, 'FM999,999,999,999'))
            ELSE 
                format('Pembayaran %s ditolak. %s', 
                    v_transaction.category, 
                    COALESCE(p_rejection_reason, 'Silakan hubungi marketing.'))
        END,
        'status_change',
        json_build_object(
            'transaction_id', p_transaction_id,
            'category', v_transaction.category,
            'amount', v_transaction.amount,
            'is_approved', p_is_approved
        ),
        p_transaction_id
    );
    
    RETURN QUERY 
    SELECT true, 
    CASE 
        WHEN p_is_approved THEN 'Transaction verified successfully'
        ELSE 'Transaction rejected successfully'
    END;
END;
$$ LANGUAGE plpgsql;

-- Function to get financial dashboard data
CREATE OR REPLACE FUNCTION get_financial_dashboard(
    p_start_date TIMESTAMP WITH TIME ZONE DEFAULT NOW() - INTERVAL '30 days',
    p_end_date TIMESTAMP WITH TIME ZONE DEFAULT NOW()
) RETURNS TABLE (
    total_revenue NUMERIC(15,2),
    pending_verification NUMERIC(15,2),
    rejected_amount NUMERIC(15,2),
    total_transactions BIGINT,
    pending_count BIGINT,
    verified_count BIGINT,
    rejected_count BIGINT,
    category_breakdown JSONB,
    daily_summary JSONB
) AS $$
DECLARE
    v_category_breakdown JSONB;
    v_daily_summary JSONB;
BEGIN
    -- Category breakdown
    SELECT jsonb_agg(
        jsonb_build_object(
            'category', category,
            'total', COALESCE(SUM(amount), 0),
            'count', COUNT(*),
            'pending', COALESCE(SUM(CASE WHEN status = 'PENDING' THEN amount ELSE 0 END), 0)
        )
    ) INTO v_category_breakdown
    FROM financial_transactions
    WHERE created_at BETWEEN p_start_date AND p_end_date
    GROUP BY category;
    
    -- Daily summary for last 7 days
    SELECT jsonb_agg(
        jsonb_build_object(
            'date', DATE(created_at),
            'total', COALESCE(SUM(amount), 0),
            'count', COUNT(*)
        )
    ) INTO v_daily_summary
    FROM financial_transactions
    WHERE created_at >= NOW() - INTERVAL '7 days'
    GROUP BY DATE(created_at)
    ORDER BY DATE(created_at);
    
    RETURN QUERY
    SELECT 
        COALESCE(SUM(CASE WHEN is_realized = true THEN amount ELSE 0 END), 0) as total_revenue,
        COALESCE(SUM(CASE WHEN status = 'PENDING' THEN amount ELSE 0 END), 0) as pending_verification,
        COALESCE(SUM(CASE WHEN status = 'REJECTED' THEN amount ELSE 0 END), 0) as rejected_amount,
        COUNT(*) as total_transactions,
        COUNT(*) FILTER (WHERE status = 'PENDING') as pending_count,
        COUNT(*) FILTER (WHERE status = 'VERIFIED') as verified_count,
        COUNT(*) FILTER (WHERE status = 'REJECTED') as rejected_count,
        v_category_breakdown,
        v_daily_summary
    FROM financial_transactions
    WHERE created_at BETWEEN p_start_date AND p_end_date;
END;
$$ LANGUAGE plpgsql;

-- Trigger for automatic status updates
CREATE OR REPLACE FUNCTION tr_check_payment_completion()
RETURNS TRIGGER AS $$
BEGIN
    -- Update KPR status based on payment completion
    IF TG_OP = 'UPDATE' AND NEW.is_realized = true AND OLD.is_realized = false THEN
        -- Payment was just verified, update KPR status if needed
        CASE NEW.category
            WHEN 'BOOKING_FEE' THEN
                UPDATE kpr_dossiers 
                SET status = 'PEMBERKASAN', updated_at = NOW()
                WHERE id = NEW.dossier_id AND status = 'LEAD';
            WHEN 'DP_1' THEN
                UPDATE kpr_dossiers 
                SET status = 'VERIFIKASI_KELALENGKAPAN', updated_at = NOW()
                WHERE id = NEW.dossier_id AND status = 'PEMBERKASAN';
            WHEN 'DP_2' THEN
                UPDATE kpr_dossiers 
                SET status = 'PENGAJUAN_BANK', updated_at = NOW()
                WHERE id = NEW.dossier_id AND status = 'VERIFIKASI_KELALENGKAPAN';
            WHEN 'DP_PELUNASAN' THEN
                UPDATE kpr_dossiers 
                SET status = 'APPROVAL_BANK', updated_at = NOW()
                WHERE id = NEW.dossier_id AND status = 'PENGAJUAN_BANK';
        END CASE;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
DROP TRIGGER IF EXISTS tr_financial_transaction_update ON financial_transactions;
CREATE TRIGGER tr_financial_transaction_update
    AFTER UPDATE ON financial_transactions
    FOR EACH ROW EXECUTE FUNCTION tr_check_payment_completion();

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_financial_transactions_category ON financial_transactions(category);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_status ON financial_transactions(status);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_is_realized ON financial_transactions(is_realized);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_created_at ON financial_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_dossier_id ON financial_transactions(dossier_id);

-- RLS Policies
ALTER TABLE financial_transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Finance can manage all transactions" ON financial_transactions
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM user_profiles
            WHERE id = auth.uid() AND role = 'FINANCE'
        )
    );

CREATE POLICY "Marketing can view transactions" ON financial_transactions
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM user_profiles
            WHERE id = auth.uid() AND role = 'MARKETING'
        )
    );

CREATE POLICY "Users can view their own transactions" ON financial_transactions
    FOR SELECT USING (
        dossier_id IN (
            SELECT id FROM kpr_dossiers WHERE user_id = auth.uid()
        )
    );

CREATE POLICY "System can insert transactions" ON financial_transactions
    FOR INSERT WITH CHECK (true);

-- Grant permissions
GRANT EXECUTE ON FUNCTION create_financial_transaction TO authenticated;
GRANT EXECUTE ON FUNCTION verify_financial_transaction TO authenticated;
GRANT EXECUTE ON FUNCTION get_financial_dashboard TO authenticated;
GRANT SELECT ON v_financial_summary TO authenticated;
