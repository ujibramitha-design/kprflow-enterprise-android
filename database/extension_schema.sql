-- =====================================================
-- BUSINESS LOGIC HARDENING: AUTO-CANCEL & EXTENSION
-- Phase 20 Implementation - Extension System
-- =====================================================

-- =====================================================
-- DATABASE SCHEMA UPDATES
-- =====================================================

-- Add extension columns to KprDossier table
ALTER TABLE kpr_dossiers 
ADD COLUMN IF NOT EXISTS extension_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS extension_reason TEXT,
ADD COLUMN IF NOT EXISTS extension_date TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS extended_by UUID REFERENCES user_profiles(id),
ADD COLUMN IF NOT EXISTS original_deadline TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS current_deadline TIMESTAMP WITH TIME ZONE;

-- Add extension history table for audit trail
CREATE TABLE IF NOT EXISTS dossier_extensions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    extension_days INTEGER NOT NULL DEFAULT 30,
    extension_reason TEXT NOT NULL,
    extended_by UUID NOT NULL REFERENCES user_profiles(id),
    extension_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    previous_deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    new_deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_by UUID REFERENCES user_profiles(id),
    approval_date TIMESTAMP WITH TIME ZONE,
    status TEXT DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Add constraints for extension logic
ALTER TABLE kpr_dossiers 
ADD CONSTRAINT check_extension_count CHECK (extension_count >= 0 AND extension_count <= 3),
ADD CONSTRAINT check_current_deadline CHECK (current_deadline >= booking_date);

-- =====================================================
-- TRIGGERS FOR AUTOMATIC CANCELLATION
-- =====================================================

-- Function to check and auto-cancel overdue dossiers
CREATE OR REPLACE FUNCTION check_overdue_dossiers()
RETURNS TRIGGER AS $$
BEGIN
    -- Auto-cancel logic for document overdue (14 days)
    IF NEW.status IN ('LEAD', 'PEMBERKASAN') AND 
       EXTRACT(DAY FROM (NOW() - NEW.booking_date)) > 14 AND
       NEW.extension_count = 0 THEN
        NEW.status = 'CANCELLED_BY_SYSTEM';
        NEW.notes = COALESCE(NEW.notes, '') || ' | Auto-cancelled: Document deadline exceeded (14 days)';
    END IF;
    
    -- Auto-cancel logic for bank overdue (60 days + extensions)
    IF NEW.status IN ('PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'PRA_AKAD', 'AKAD_BELUM_CAIR') THEN
        DECLARE
            total_days_allowed INTEGER := 60 + (NEW.extension_count * 30);
            days_elapsed INTEGER := EXTRACT(DAY FROM (NOW() - NEW.booking_date));
        BEGIN
            IF days_elapsed > total_days_allowed THEN
                NEW.status = 'CANCELLED_BY_SYSTEM';
                NEW.notes = COALESCE(NEW.notes, '') || ' | Auto-cancelled: Bank deadline exceeded (' || total_days_allowed || ' days)';
            END IF;
        END;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to kpr_dossiers table
CREATE TRIGGER trigger_check_overdue_dossiers
    BEFORE UPDATE ON kpr_dossiers
    FOR EACH ROW EXECUTE FUNCTION check_overdue_dossiers();

-- =====================================================
-- EXTENSION FUNCTIONS
-- =====================================================

-- Function to request extension
CREATE OR REPLACE FUNCTION request_dossier_extension(
    p_dossier_id UUID,
    p_extension_days INTEGER DEFAULT 30,
    p_extension_reason TEXT,
    p_requested_by UUID
)
RETURNS TABLE(
    success BOOLEAN,
    message TEXT,
    extension_id UUID
) AS $$
DECLARE
    v_dossier RECORD;
    v_extension_count INTEGER;
    v_current_deadline TIMESTAMP WITH TIME ZONE;
    v_new_deadline TIMESTAMP WITH TIME ZONE;
    v_extension_id UUID;
BEGIN
    -- Get dossier information
    SELECT * INTO v_dossier 
    FROM kpr_dossiers 
    WHERE id = p_dossier_id;
    
    IF NOT FOUND THEN
        RETURN QUERY SELECT false, 'Dossier not found', NULL::UUID;
        RETURN;
    END IF;
    
    -- Check if dossier is eligible for extension
    IF v_dossier.status IN ('CANCELLED_BY_SYSTEM', 'BAST_COMPLETED') THEN
        RETURN QUERY SELECT false, 'Dossier is not eligible for extension', NULL::UUID;
        RETURN;
    END IF;
    
    -- Check extension count limit (max 3 extensions)
    IF v_dossier.extension_count >= 3 THEN
        RETURN QUERY SELECT false, 'Maximum extension limit reached (3 extensions)', NULL::UUID;
        RETURN;
    END IF;
    
    -- Calculate new deadline
    v_current_deadline := COALESCE(v_dossier.current_deadline, v_dossier.booking_date + INTERVAL '60 days');
    v_new_deadline := v_current_deadline + (p_extension_days || ' days')::INTERVAL;
    
    -- Create extension record
    INSERT INTO dossier_extensions (
        dossier_id,
        extension_days,
        extension_reason,
        extended_by,
        previous_deadline,
        new_deadline,
        status
    ) VALUES (
        p_dossier_id,
        p_extension_days,
        p_extension_reason,
        p_requested_by,
        v_current_deadline,
        v_new_deadline,
        'PENDING'
    ) RETURNING id INTO v_extension_id;
    
    -- Update dossier
    UPDATE kpr_dossiers SET
        extension_count = extension_count + 1,
        extension_reason = p_extension_reason,
        extension_date = NOW(),
        extended_by = p_requested_by,
        original_deadline = COALESCE(original_deadline, v_current_deadline),
        current_deadline = v_new_deadline,
        updated_at = NOW()
    WHERE id = p_dossier_id;
    
    RETURN QUERY SELECT true, 'Extension request submitted successfully', v_extension_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to approve extension
CREATE OR REPLACE FUNCTION approve_dossier_extension(
    p_extension_id UUID,
    p_approved_by UUID,
    p_approval_notes TEXT DEFAULT NULL
)
RETURNS TABLE(
    success BOOLEAN,
    message TEXT
) AS $$
DECLARE
    v_extension RECORD;
BEGIN
    -- Get extension information
    SELECT * INTO v_extension 
    FROM dossier_extensions 
    WHERE id = p_extension_id AND status = 'PENDING';
    
    IF NOT FOUND THEN
        RETURN QUERY SELECT false, 'Extension request not found or already processed', NULL::TEXT;
        RETURN;
    END IF;
    
    -- Update extension status
    UPDATE dossier_extensions SET
        status = 'APPROVED',
        approved_by = p_approved_by,
        approval_date = NOW(),
        rejection_reason = NULL,
        updated_at = NOW()
    WHERE id = p_extension_id;
    
    RETURN QUERY SELECT true, 'Extension approved successfully', NULL::TEXT;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to reject extension
CREATE OR REPLACE FUNCTION reject_dossier_extension(
    p_extension_id UUID,
    p_rejected_by UUID,
    p_rejection_reason TEXT
)
RETURNS TABLE(
    success BOOLEAN,
    message TEXT
) AS $$
DECLARE
    v_extension RECORD;
    v_dossier_id UUID;
BEGIN
    -- Get extension information
    SELECT * INTO v_extension 
    FROM dossier_extensions 
    WHERE id = p_extension_id AND status = 'PENDING';
    
    IF NOT FOUND THEN
        RETURN QUERY SELECT false, 'Extension request not found or already processed', NULL::TEXT;
        RETURN;
    END IF;
    
    v_dossier_id := v_extension.dossier_id;
    
    -- Update extension status
    UPDATE dossier_extensions SET
        status = 'REJECTED',
        approved_by = p_rejected_by,
        approval_date = NOW(),
        rejection_reason = p_rejection_reason,
        updated_at = NOW()
    WHERE id = p_extension_id;
    
    -- Revert dossier extension count
    UPDATE kpr_dossiers SET
        extension_count = extension_count - 1,
        current_deadline = v_extension.previous_deadline,
        updated_at = NOW()
    WHERE id = v_dossier_id;
    
    RETURN QUERY SELECT true, 'Extension rejected successfully', NULL::TEXT;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- VIEWS FOR EXTENSION MANAGEMENT
-- =====================================================

-- View for pending extensions
CREATE OR REPLACE VIEW v_pending_extensions AS
SELECT 
    e.id,
    e.dossier_id,
    u.name AS customer_name,
    e.extension_days,
    e.extension_reason,
    e.extended_by,
    ue.name AS extended_by_name,
    e.extension_date,
    e.previous_deadline,
    e.new_deadline,
    e.status,
    d.status AS dossier_status,
    d.extension_count
FROM dossier_extensions e
JOIN kpr_dossiers d ON e.dossier_id = d.id
JOIN user_profiles u ON d.user_id::text = u.id::text
LEFT JOIN user_profiles ue ON e.extended_by::text = ue.id::text
WHERE e.status = 'PENDING'
ORDER BY e.extension_date DESC;

-- View for extension history
CREATE OR REPLACE VIEW v_extension_history AS
SELECT 
    e.id,
    e.dossier_id,
    u.name AS customer_name,
    e.extension_days,
    e.extension_reason,
    e.extended_by,
    ue.name AS extended_by_name,
    e.extension_date,
    e.previous_deadline,
    e.new_deadline,
    e.status,
    e.approved_by,
    ua.name AS approved_by_name,
    e.approval_date,
    e.rejection_reason,
    d.extension_count
FROM dossier_extensions e
JOIN kpr_dossiers d ON e.dossier_id = d.id
JOIN user_profiles u ON d.user_id::text = u.id::text
LEFT JOIN user_profiles ue ON e.extended_by::text = ue.id::text
LEFT JOIN user_profiles ua ON e.approved::text = ua.id::text
ORDER BY e.extension_date DESC;

-- =====================================================
-- RLS POLICIES FOR EXTENSION SYSTEM
-- =====================================================

-- RLS for dossier_extensions table
ALTER TABLE dossier_extensions ENABLE ROW LEVEL SECURITY;

-- Legal can view all extensions
CREATE POLICY "Legal view all extensions" ON dossier_extensions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- BOD can view all extensions
CREATE POLICY "BOD view all extensions" ON dossier_extensions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view their own dossier extensions
CREATE POLICY "Users view own extensions" ON dossier_extensions
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'CUSTOMER' AND
        dossier_id IN (
            SELECT id FROM kpr_dossiers WHERE user_id::text = auth.uid()::text
        )
    );

-- Legal can insert extension requests
CREATE POLICY "Legal can request extensions" ON dossier_extensions
    FOR INSERT WITH CHECK (auth.jwt() ->> 'role' = 'LEGAL');

-- Legal can update extensions (approve/reject)
CREATE POLICY "Legal can update extensions" ON dossier_extensions
    FOR UPDATE USING (auth.jwt() ->> 'role' = 'LEGAL');

-- RLS for views
ALTER VIEW v_pending_extensions SET (security_barrier = true);
ALTER VIEW v_extension_history SET (security_barrier = true);

-- Policies for views
CREATE POLICY "Legal view pending extensions" ON v_pending_extensions
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

CREATE POLICY "Legal view extension history" ON v_extension_history
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_dossier_extensions_dossier_id ON dossier_extensions(dossier_id);
CREATE INDEX IF NOT EXISTS idx_dossier_extensions_status ON dossier_extensions(status);
CREATE INDEX IF NOT EXISTS idx_dossier_extensions_date ON dossier_extensions(extension_date);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_extension_count ON kpr_dossiers(extension_count);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_current_deadline ON kpr_dossiers(current_deadline);

-- =====================================================
-- SAMPLE QUERIES FOR TESTING
-- =====================================================

-- Request extension (30 days)
-- SELECT * FROM request_dossier_extension('dossier-uuid', 30, 'Customer needs additional time for document preparation', 'legal-user-uuid');

-- Approve extension
-- SELECT * FROM approve_dossier_extension('extension-uuid', 'manager-uuid', 'Extension approved due to valid reasons');

-- Reject extension
-- SELECT * FROM reject_dossier_extension('extension-uuid', 'manager-uuid', 'Extension rejected: Insufficient justification');

-- Get pending extensions
-- SELECT * FROM v_pending_extensions;

-- Get extension history for a dossier
-- SELECT * FROM v_extension_history WHERE dossier_id = 'dossier-uuid';

-- Check dossiers eligible for extension
-- SELECT id, customer_name, status, extension_count, current_deadline 
-- FROM v_dossier_sla_status 
-- WHERE status NOT IN ('CANCELLED_BY_SYSTEM', 'BAST_COMPLETED') 
-- AND extension_count < 3;
