-- =====================================================
-- PRE-AKAD VERIFICATION SYSTEM
-- Cross-department verification with role-based security
-- =====================================================

-- =====================================================
-- PRE-AKAD CHECKLIST TABLE
-- Source of truth for verification status across departments
-- =====================================================

CREATE TABLE "PreAkadChecklist" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dossier_id UUID REFERENCES "KprDossier"(id) ON DELETE CASCADE UNIQUE,
    
    -- Finance Department Verification
    is_pph_paid BOOLEAN DEFAULT false,
    pph_paid_at TIMESTAMP WITH TIME ZONE,
    pph_paid_by UUID REFERENCES "UserProfile"(id),
    pph_payment_proof TEXT,
    pph_amount DECIMAL(15, 2),
    
    is_bphtb_paid BOOLEAN DEFAULT false,
    bphtb_paid_at TIMESTAMP WITH TIME ZONE,
    bphtb_paid_by UUID REFERENCES "UserProfile"(id),
    bphtb_payment_proof TEXT,
    bphtb_amount DECIMAL(15, 2),
    
    -- Legal Department Verification
    is_ajb_draft_ready BOOLEAN DEFAULT false,
    ajb_draft_ready_at TIMESTAMP WITH TIME ZONE,
    ajb_draft_ready_by UUID REFERENCES "UserProfile"(id),
    ajb_draft_url TEXT,
    ajb_draft_notes TEXT,
    
    -- Marketing Department Verification
    is_spr_final_signed BOOLEAN DEFAULT false,
    spr_final_signed_at TIMESTAMP WITH TIME ZONE,
    spr_final_signed_by UUID REFERENCES "UserProfile"(id),
    spr_final_document_url TEXT,
    spr_final_notes TEXT,
    
    -- Additional Verification Fields
    is_bast_ready BOOLEAN DEFAULT false,
    bast_ready_at TIMESTAMP WITH TIME ZONE,
    bast_ready_by UUID REFERENCES "UserProfile"(id),
    bast_document_url TEXT,
    
    is_insurance_paid BOOLEAN DEFAULT false,
    insurance_paid_at TIMESTAMP WITH TIME ZONE,
    insurance_paid_by UUID REFERENCES "UserProfile"(id),
    insurance_policy_url TEXT,
    insurance_amount DECIMAL(15, 2),
    
    -- Overall Status
    verification_status TEXT DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING', 'IN_PROGRESS', 'READY', 'BLOCKED')),
    verification_completed_at TIMESTAMP WITH TIME ZONE,
    verification_completed_by UUID REFERENCES "UserProfile"(id),
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_updated_by UUID REFERENCES "UserProfile"(id)
);

-- =====================================================
-- VERIFICATION READINESS VIEW
-- Real-time calculation of akad readiness
-- =====================================================

CREATE OR REPLACE VIEW v_akad_readiness AS
SELECT 
    pak.id,
    pak.dossier_id,
    d.customer_name,
    d.status AS dossier_status,
    d.booking_date,
    up.block,
    up.unit_number,
    up.type AS unit_type,
    up.price AS unit_price,
    
    -- Individual Verification Status
    pak.is_pph_paid,
    pak.is_bphtb_paid,
    pak.is_ajb_draft_ready,
    pak.is_spr_final_signed,
    pak.is_bast_ready,
    pak.is_insurance_paid,
    
    -- Verification Timestamps
    pak.pph_paid_at,
    pak.bphtb_paid_at,
    pak.ajb_draft_ready_at,
    pak.spr_final_signed_at,
    pak.bast_ready_at,
    pak.insurance_paid_at,
    
    -- Verification Personnel
    pak.pph_paid_by,
    pak.bphtb_paid_by,
    pak.ajb_draft_ready_by,
    pak.spr_final_signed_by,
    pak.bast_ready_by,
    pak.insurance_paid_by,
    
    -- Readiness Calculation
    CASE 
        WHEN pak.is_pph_paid = true 
         AND pak.is_bphtb_paid = true 
         AND pak.is_ajb_draft_ready = true 
         AND pak.is_spr_final_signed = true 
         AND pak.is_bast_ready = true 
         AND pak.is_insurance_paid = true THEN true
        ELSE false
    END AS is_ready_for_akad,
    
    -- Completion Percentage
    CASE 
        WHEN pak.is_pph_paid = true AND pak.is_bphtb_paid = true AND 
             pak.is_ajb_draft_ready = true AND pak.is_spr_final_signed = true AND
             pak.is_bast_ready = true AND pak.is_insurance_paid = true THEN 100
        ELSE (
            (CASE WHEN pak.is_pph_paid = true THEN 1 ELSE 0 END) +
            (CASE WHEN pak.is_bphtb_paid = true THEN 1 ELSE 0 END) +
            (CASE WHEN pak.is_ajb_draft_ready = true THEN 1 ELSE 0 END) +
            (CASE WHEN pak.is_spr_final_signed = true THEN 1 ELSE 0 END) +
            (CASE WHEN pak.is_bast_ready = true THEN 1 ELSE 0 END) +
            (CASE WHEN pak.is_insurance_paid = true THEN 1 ELSE 0 END)
        ) * 100 / 6
    END AS completion_percentage,
    
    -- Missing Verifications
    ARRAY_REMOVE(ARRAY[
        CASE WHEN NOT pak.is_pph_paid THEN 'PPh Payment' END,
        CASE WHEN NOT pak.is_bphtb_paid THEN 'BPHTB Payment' END,
        CASE WHEN NOT pak.is_ajb_draft_ready THEN 'AJB Draft' END,
        CASE WHEN NOT pak.is_spr_final_signed THEN 'SPR Final' END,
        CASE WHEN NOT pak.is_bast_ready THEN 'BAST' END,
        CASE WHEN NOT pak.is_insurance_paid THEN 'Insurance' END
    ], NULL) AS missing_verifications,
    
    -- Department Status
    CASE 
        WHEN pak.is_pph_paid = true AND pak.is_bphtb_paid = true THEN 'COMPLETE'
        WHEN pak.is_pph_paid = true OR pak.is_bphtb_paid = true THEN 'PARTIAL'
        ELSE 'PENDING'
    END AS finance_status,
    
    CASE 
        WHEN pak.is_ajb_draft_ready = true THEN 'COMPLETE'
        ELSE 'PENDING'
    END AS legal_status,
    
    CASE 
        WHEN pak.is_spr_final_signed = true THEN 'COMPLETE'
        ELSE 'PENDING'
    END AS marketing_status,
    
    -- Overall Verification Status
    pak.verification_status,
    pak.verification_completed_at,
    pak.verification_completed_by,
    
    -- Last Update Info
    pak.updated_at,
    pak.last_updated_by
    
FROM "PreAkadChecklist" pak
JOIN "KprDossier" d ON pak.dossier_id = d.id
LEFT JOIN "UnitProperty" up ON d.unit_id::text = up.id::text;

-- =====================================================
-- DEPARTMENT VERIFICATION SUMMARY VIEW
-- =====================================================

CREATE OR REPLACE VIEW v_department_verification_summary AS
SELECT 
    -- Overall Metrics
    COUNT(*) AS total_dossiers,
    COUNT(CASE WHEN is_ready_for_akad = true THEN 1 END) AS ready_dossiers,
    COUNT(CASE WHEN is_ready_for_akad = false THEN 1 END) AS pending_dossiers,
    
    -- Completion Metrics
    AVG(completion_percentage) AS avg_completion_percentage,
    MAX(completion_percentage) AS max_completion_percentage,
    MIN(completion_percentage) AS min_completion_percentage,
    
    -- Department Metrics
    COUNT(CASE WHEN is_pph_paid = true THEN 1 END) AS pph_completed,
    COUNT(CASE WHEN is_bphtb_paid = true THEN 1 END) AS bphtb_completed,
    COUNT(CASE WHEN is_ajb_draft_ready = true THEN 1 END) AS ajb_completed,
    COUNT(CASE WHEN is_spr_final_signed = true THEN 1 END) AS spr_completed,
    COUNT(CASE WHEN is_bast_ready = true THEN 1 END) AS bast_completed,
    COUNT(CASE WHEN is_insurance_paid = true THEN 1 END) AS insurance_completed,
    
    -- Department Completion Rates
    ROUND(COUNT(CASE WHEN is_pph_paid = true THEN 1 END) * 100.0 / COUNT(*), 2) AS pph_completion_rate,
    ROUND(COUNT(CASE WHEN is_bphtb_paid = true THEN 1 END) * 100.0 / COUNT(*), 2) AS bphtb_completion_rate,
    ROUND(COUNT(CASE WHEN is_ajb_draft_ready = true THEN 1 END) * 100.0 / COUNT(*), 2) AS ajb_completion_rate,
    ROUND(COUNT(CASE WHEN is_spr_final_signed = true THEN 1 END) * 100.0 / COUNT(*), 2) AS spr_completion_rate,
    ROUND(COUNT(CASE WHEN is_bast_ready = true THEN 1 END) * 100.0 / COUNT(*), 2) AS bast_completion_rate,
    ROUND(COUNT(CASE WHEN is_insurance_paid = true THEN 1 END) * 100.0 / COUNT(*), 2) AS insurance_completion_rate,
    
    -- Status Breakdown
    COUNT(CASE WHEN verification_status = 'READY' THEN 1 END) AS status_ready,
    COUNT(CASE WHEN verification_status = 'IN_PROGRESS' THEN 1 END) AS status_in_progress,
    COUNT(CASE WHEN verification_status = 'PENDING' THEN 1 END) AS status_pending,
    COUNT(CASE WHEN verification_status = 'BLOCKED' THEN 1 END) AS status_blocked,
    
    -- Ready for Akad Percentage
    ROUND(COUNT(CASE WHEN is_ready_for_akad = true THEN 1 END) * 100.0 / COUNT(*), 2) AS readiness_percentage,
    
    -- Report Date
    CURRENT_DATE AS report_date
    
FROM v_akad_readiness;

-- =====================================================
-- TRIGGERS FOR AUTOMATIC STATUS UPDATES
-- =====================================================

-- Function to update verification status based on checklist completion
CREATE OR REPLACE FUNCTION update_verification_status()
RETURNS TRIGGER AS $$
BEGIN
    -- Calculate completion status
    DECLARE
        pph_complete BOOLEAN := NEW.is_pph_paid = true;
        bphtb_complete BOOLEAN := NEW.is_bphtb_paid = true;
        ajb_complete BOOLEAN := NEW.is_ajb_draft_ready = true;
        spr_complete BOOLEAN := NEW.is_spr_final_signed = true;
        bast_complete BOOLEAN := NEW.is_bast_ready = true;
        insurance_complete BOOLEAN := NEW.is_insurance_paid = true;
        
        all_complete BOOLEAN;
        any_complete BOOLEAN;
    BEGIN
        all_complete := pph_complete AND bphtb_complete AND ajb_complete AND 
                        spr_complete AND bast_complete AND insurance_complete;
        any_complete := pph_complete OR bphtb_complete OR ajb_complete OR 
                        spr_complete OR bast_complete OR insurance_complete;
        
        -- Update verification status
        IF all_complete THEN
            NEW.verification_status := 'READY';
            NEW.verification_completed_at := NOW();
        ELSIF any_complete THEN
            NEW.verification_status := 'IN_PROGRESS';
        ELSE
            NEW.verification_status := 'PENDING';
        END IF;
        
        NEW.updated_at := NOW();
    END;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger
CREATE TRIGGER trigger_update_verification_status
    BEFORE INSERT OR UPDATE ON "PreAkadChecklist"
    FOR EACH ROW EXECUTE FUNCTION update_verification_status();

-- =====================================================
-- SECURITY FUNCTIONS FOR ROLE-BASED ACCESS
-- =====================================================

-- Function to check if user can update specific verification field
CREATE OR REPLACE FUNCTION can_update_verification_field(
    user_id UUID,
    field_name TEXT
) RETURNS BOOLEAN AS $$
DECLARE
    user_role TEXT;
BEGIN
    -- Get user role
    SELECT role INTO user_role 
    FROM "UserProfile" 
    WHERE id = user_id;
    
    -- Role-based field access control
    CASE user_role
        WHEN 'FINANCE' THEN
            RETURN field_name IN ('is_pph_paid', 'pph_paid_at', 'pph_paid_by', 'pph_payment_proof', 'pph_amount',
                                   'is_bphtb_paid', 'bphtb_paid_at', 'bphtb_paid_by', 'bphtb_payment_proof', 'bphtb_amount',
                                   'is_insurance_paid', 'insurance_paid_at', 'insurance_paid_by', 'insurance_policy_url', 'insurance_amount');
        WHEN 'LEGAL' THEN
            RETURN field_name IN ('is_ajb_draft_ready', 'ajb_draft_ready_at', 'ajb_draft_ready_by', 'ajb_draft_url', 'ajb_draft_notes');
        WHEN 'MARKETING' THEN
            RETURN field_name IN ('is_spr_final_signed', 'spr_final_signed_at', 'spr_final_signed_by', 'spr_final_document_url', 'spr_final_notes');
        WHEN 'BOD', 'MANAGER' THEN
            RETURN true; -- Full access for management
        ELSE
            RETURN false; -- No access for other roles
    END CASE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_pre_akad_checklist_dossier_id ON "PreAkadChecklist"(dossier_id);
CREATE INDEX IF NOT EXISTS idx_pre_akad_checklist_verification_status ON "PreAkadChecklist"(verification_status);
CREATE INDEX IF NOT EXISTS idx_pre_akad_checklist_updated_at ON "PreAkadChecklist"(updated_at);
CREATE INDEX IF NOT EXISTS idx_pre_akad_checklist_pph_paid ON "PreAkadChecklist"(is_pph_paid);
CREATE INDEX IF NOT EXISTS idx_pre_akad_checklist_bphtb_paid ON "PreAkadChecklist"(is_bphtb_paid);
CREATE INDEX IF NOT EXISTS idx_pre_akad_checklist_ajb_ready ON "PreAkadChecklist"(is_ajb_draft_ready);
CREATE INDEX IF NOT EXISTS idx_pre_akad_checklist_spr_signed ON "PreAkadChecklist"(is_spr_final_signed);

-- =====================================================
-- RLS POLICIES FOR VERIFICATION DATA
-- =====================================================

ALTER TABLE "PreAkadChecklist" ENABLE ROW LEVEL SECURITY;

-- Finance can view all verification data
CREATE POLICY "Finance view verification data" ON "PreAkadChecklist"
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- Legal can view all verification data
CREATE POLICY "Legal view verification data" ON "PreAkadChecklist"
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Marketing can view verification data
CREATE POLICY "Marketing view verification data" ON "PreAkadChecklist"
    FOR SELECT USING (auth.jwt() ->> 'role' = 'MARKETING');

-- BOD and Manager can view all verification data
CREATE POLICY "BOD view verification data" ON "PreAkadChecklist"
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('BOD', 'MANAGER'));

-- Role-based update policies with field restrictions
CREATE POLICY "Finance update finance fields" ON "PreAkadChecklist"
    FOR UPDATE USING (
        auth.jwt() ->> 'role' = 'FINANCE' AND
        can_update_verification_field(auth.uid()::uuid, TG_ARGV[1])
    );

CREATE POLICY "Legal update legal fields" ON "PreAkadChecklist"
    FOR UPDATE USING (
        auth.jwt() ->> 'role' = 'LEGAL' AND
        can_update_verification_field(auth.uid()::uuid, TG_ARGV[1])
    );

CREATE POLICY "Marketing update marketing fields" ON "PreAkadChecklist"
    FOR UPDATE USING (
        auth.jwt() ->> 'role' = 'MARKETING' AND
        can_update_verification_field(auth.uid()::uuid, TG_ARGV[1])
    );

CREATE POLICY "Management update all fields" ON "PreAkadChecklist"
    FOR UPDATE USING (auth.jwt() ->> 'role' IN ('BOD', 'MANAGER'));

-- Insert policies
CREATE POLICY "Finance insert verification data" ON "PreAkadChecklist"
    FOR INSERT WITH CHECK (auth.jwt() ->> 'role' IN ('FINANCE', 'LEGAL', 'MARKETING', 'BOD', 'MANAGER'));

-- RLS for views
ALTER VIEW v_akad_readiness SET (security_barrier = true);
ALTER VIEW v_department_verification_summary SET (security_barrier = true);

-- View policies
CREATE POLICY "All roles view akad readiness" ON v_akad_readiness
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('FINANCE', 'LEGAL', 'MARKETING', 'BOD', 'MANAGER'));

CREATE POLICY "Management view department summary" ON v_department_verification_summary
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('BOD', 'MANAGER'));

-- =====================================================
-- SAMPLE QUERIES FOR TESTING
-- =====================================================

-- Get akad readiness for all dossiers
-- SELECT * FROM v_akad_readiness ORDER BY completion_percentage DESC;

-- Get department verification summary
-- SELECT * FROM v_department_verification_summary;

-- Get dossiers ready for akad
-- SELECT * FROM v_akad_readiness WHERE is_ready_for_akad = true;

-- Get missing verifications for a specific dossier
-- SELECT missing_verifications FROM v_akad_readiness WHERE dossier_id = 'dossier-uuid';

-- Update verification status (Finance only for finance fields)
-- UPDATE "PreAkadChecklist" 
-- SET is_pph_paid = true, pph_paid_at = NOW(), pph_paid_by = 'finance-user-uuid'
-- WHERE dossier_id = 'dossier-uuid';

-- Check if user can update specific field
-- SELECT can_update_verification_field('user-uuid', 'is_pph_paid');
