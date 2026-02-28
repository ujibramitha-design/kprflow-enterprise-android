-- Bank Decisions table for Phase 11: Bank Decision Matrix

CREATE TABLE IF NOT EXISTS bank_decisions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    public_url TEXT NOT NULL,
    decision_type VARCHAR(20) NOT NULL, -- 'APPROVED', 'REJECTED', 'PENDING'
    bank_name VARCHAR(100) NOT NULL,
    decision_reason TEXT,
    approved_amount DECIMAL(15,2), -- Approved loan amount if approved
    rejection_reason TEXT,
    notes TEXT,
    status VARCHAR(20) DEFAULT 'UPLOADED', -- 'UPLOADED', 'REVIEWED', 'PROCESSED', 'ARCHIVED'
    uploaded_by UUID REFERENCES user_profiles(id),
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    processed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_bank_decisions_dossier_id ON bank_decisions(dossier_id);
CREATE INDEX IF NOT EXISTS idx_bank_decisions_decision_type ON bank_decisions(decision_type);
CREATE INDEX IF NOT EXISTS idx_bank_decisions_bank_name ON bank_decisions(bank_name);
CREATE INDEX IF NOT EXISTS idx_bank_decisions_status ON bank_decisions(status);
CREATE INDEX IF NOT EXISTS idx_bank_decisions_uploaded_at ON bank_decisions(uploaded_at);

-- RLS Policies for bank decisions
ALTER TABLE bank_decisions ENABLE ROW LEVEL SECURITY;

-- Bank users can manage decisions
CREATE POLICY "Bank can manage bank decisions" ON bank_decisions
    FOR ALL USING (auth.jwt() ->> 'role' = 'BANK');

-- Marketing can view decisions
CREATE POLICY "Marketing can view bank decisions" ON bank_decisions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'MARKETING');

-- Legal can view decisions
CREATE POLICY "Legal can view bank decisions" ON bank_decisions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Finance can view decisions
CREATE POLICY "Finance can view bank decisions" ON bank_decisions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- BOD can view all decisions
CREATE POLICY "BOD can view all bank decisions" ON bank_decisions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Customers can view their own decisions
CREATE POLICY "Customers can view own decisions" ON bank_decisions
    FOR SELECT USING (
        auth.uid()::text IN (
            SELECT user_id::text FROM kpr_dossiers WHERE id = dossier_id
        )
    );

-- Function to automatically update dossier status when bank decision is processed
CREATE OR REPLACE FUNCTION update_dossier_from_bank_decision()
RETURNS TRIGGER AS $$
BEGIN
    -- Only update if decision is processed and status is PROCESSED
    IF NEW.status = 'PROCESSED' AND OLD.status != 'PROCESSED' THEN
        UPDATE kpr_dossiers SET
            status = CASE 
                WHEN NEW.decision_type = 'APPROVED' THEN 'PUTUSAN_KREDIT_ACC'
                WHEN NEW.decision_type = 'REJECTED' THEN 'CANCELLED_BY_SYSTEM'
                ELSE status
            END,
            updated_at = NOW(),
            notes = COALESCE(
                CONCAT(
                    COALESCE(notes, ''), 
                    CASE 
                        WHEN notes IS NOT NULL AND notes != '' THEN '; ' ELSE '' 
                    END,
                    'Bank decision: ', NEW.decision_type, ' by ', NEW.bank_name
                )
            ),
            cancellation_reason = CASE 
                WHEN NEW.decision_type = 'REJECTED' THEN NEW.rejection_reason
                ELSE cancellation_reason
            END
        WHERE id = NEW.dossier_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for automatic dossier status update
CREATE TRIGGER update_dossier_from_bank_decision_trigger
    AFTER UPDATE ON bank_decisions
    FOR EACH ROW
    EXECUTE FUNCTION update_dossier_from_bank_decision();

-- Function to get bank decision statistics
CREATE OR REPLACE FUNCTION get_bank_decision_stats(
    p_date_range_start DATE DEFAULT NULL,
    p_date_range_end DATE DEFAULT NULL,
    p_bank_name VARCHAR(100) DEFAULT NULL
)
RETURNS TABLE (
    total_decisions BIGINT,
    approved_decisions BIGINT,
    rejected_decisions BIGINT,
    pending_decisions BIGINT,
    approval_rate DECIMAL,
    average_approved_amount DECIMAL,
    total_approved_amount DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_decisions,
        COUNT(*) FILTER (WHERE decision_type = 'APPROVED') as approved_decisions,
        COUNT(*) FILTER (WHERE decision_type = 'REJECTED') as rejected_decisions,
        COUNT(*) FILTER (WHERE decision_type = 'PENDING') as pending_decisions,
        CASE 
            WHEN COUNT(*) > 0 THEN 
                ROUND((COUNT(*) FILTER (WHERE decision_type = 'APPROVED')::DECIMAL / COUNT(*)::DECIMAL) * 100, 2)
            ELSE 0 
        END as approval_rate,
        CASE 
            WHEN COUNT(*) FILTER (WHERE decision_type = 'APPROVED') > 0 THEN
                ROUND(AVG(approved_amount), 2)
            ELSE 0 
        END as average_approved_amount,
        COALESCE(SUM(approved_amount), 0) as total_approved_amount
    FROM bank_decisions
    WHERE 
        (p_date_range_start IS NULL OR DATE(uploaded_at) >= p_date_range_start)
        AND (p_date_range_end IS NULL OR DATE(uploaded_at) <= p_date_range_end)
        AND (p_bank_name IS NULL OR bank_name = p_bank_name);
END;
$$ LANGUAGE plpgsql;

-- Function to get decision timeline for a dossier
CREATE OR REPLACE FUNCTION get_dossier_decision_timeline(p_dossier_id UUID)
RETURNS TABLE (
    decision_date TIMESTAMP WITH TIME ZONE,
    decision_type VARCHAR(20),
    bank_name VARCHAR(100),
    decision_reason TEXT,
    approved_amount DECIMAL(15,2),
    status VARCHAR(20)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        uploaded_at as decision_date,
        decision_type,
        bank_name,
        decision_reason,
        approved_amount,
        status
    FROM bank_decisions
    WHERE dossier_id = p_dossier_id
    ORDER BY uploaded_at ASC;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_bank_decisions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_bank_decisions_updated_at BEFORE UPDATE ON bank_decisions FOR EACH ROW EXECUTE FUNCTION update_bank_decisions_updated_at();
