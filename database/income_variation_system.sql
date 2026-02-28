-- Income Variation System
-- Support for different income types per job category
-- KPRFlow Enterprise - Enhanced Income Tracking

-- =====================================================
-- INCOME VARIATION TABLES
-- =====================================================

-- Income Variation Types Table
CREATE TABLE IF NOT EXISTS income_variation_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_category_id UUID REFERENCES job_categories(id) ON DELETE CASCADE,
    variation_name VARCHAR(100) NOT NULL,
    variation_code VARCHAR(20) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(job_category_id, variation_code)
);

-- Customer Income Variation Table
CREATE TABLE IF NOT EXISTS customer_income_variations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    variation_type_id UUID REFERENCES income_variation_types(id) ON DELETE CASCADE,
    bank_account_number VARCHAR(50),
    bank_name VARCHAR(100),
    bank_account_status VARCHAR(20) DEFAULT 'ACTIVE', -- 'ACTIVE', 'INACTIVE'
    monthly_income DECIMAL(15,2),
    additional_income DECIMAL(15,2) DEFAULT 0,
    income_proof_document_url TEXT,
    verification_status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'VERIFIED', 'REJECTED'
    verified_by UUID REFERENCES user_profiles(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(customer_id, variation_type_id)
);

-- =====================================================
-- INSERT INCOME VARIATION DATA
-- =====================================================

-- Get job category IDs
DO $$
DECLARE
    v_karyawan_id UUID;
    v_wirausaha_id UUID;
BEGIN
    SELECT id INTO v_karyawan_id FROM job_categories WHERE category_code = 'KARYAWAN';
    SELECT id INTO v_wirausaha_id FROM job_categories WHERE category_code = 'WIRAUSAHA';
    
    -- Insert income variations for Karyawan
    INSERT INTO income_variation_types (job_category_id, variation_name, variation_code, description) VALUES
    (v_karyawan_id, 'Payroll', 'PAYROLL', 'Gaji bulanan tetap dari perusahaan'),
    (v_karyawan_id, 'Non-Payroll', 'NON_PAYROLL', 'Penghasilan tambahan di luar gaji');
    
    -- Insert income variations for Wirausaha
    INSERT INTO income_variation_types (job_category_id, variation_name, variation_code, description) VALUES
    (v_wirausaha_id, 'Rekening Aktif', 'REK_AKTIF', 'Rekening bank yang aktif digunakan untuk operasional'),
    (v_wirausaha_id, 'Rekening Tidak Aktif', 'REK_TIDAK_AKTIF', 'Rekening bank yang tidak aktif/dormant');
END $$;

-- =====================================================
-- FUNCTIONS FOR INCOME VARIATION
-- =====================================================

-- Function to get income variations by job category
CREATE OR REPLACE FUNCTION get_income_variations_by_job_category(p_job_category_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_variations JSONB;
BEGIN
    SELECT json_agg(
        json_build_object(
            'id', ivt.id,
            'variation_name', ivt.variation_name,
            'variation_code', ivt.variation_code,
            'description', ivt.description,
            'is_active', ivt.is_active
        ) ORDER BY ivt.variation_name
    ) INTO v_variations
    FROM income_variation_types ivt
    WHERE ivt.job_category_id = p_job_category_id AND ivt.is_active = true;
    
    RETURN COALESCE(v_variations, '[]'::jsonb);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to save customer income variation
CREATE OR REPLACE FUNCTION save_customer_income_variation(
    p_customer_id UUID,
    p_variation_type_id UUID,
    p_bank_account_number VARCHAR(50),
    p_bank_name VARCHAR(100),
    p_bank_account_status VARCHAR(20) DEFAULT 'ACTIVE',
    p_monthly_income DECIMAL(15,2),
    p_additional_income DECIMAL(15,2) DEFAULT 0,
    p_income_proof_document_url TEXT DEFAULT NULL,
    p_notes TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_existing_id UUID;
    v_result JSONB;
BEGIN
    -- Check if variation already exists
    SELECT id INTO v_existing_id
    FROM customer_income_variations 
    WHERE customer_id = p_customer_id AND variation_type_id = p_variation_type_id;
    
    IF v_existing_id IS NOT NULL THEN
        -- Update existing record
        UPDATE customer_income_variations SET
            bank_account_number = p_bank_account_number,
            bank_name = p_bank_name,
            bank_account_status = p_bank_account_status,
            monthly_income = p_monthly_income,
            additional_income = p_additional_income,
            income_proof_document_url = p_income_proof_document_url,
            verification_status = 'PENDING',
            notes = p_notes,
            updated_at = NOW()
        WHERE id = v_existing_id;
        
        v_result := json_build_object(
            'success', true,
            'message', 'Income variation updated successfully',
            'variation_id', v_existing_id
        );
    ELSE
        -- Insert new record
        INSERT INTO customer_income_variations (
            customer_id, variation_type_id, bank_account_number, bank_name,
            bank_account_status, monthly_income, additional_income,
            income_proof_document_url, notes
        ) VALUES (
            p_customer_id, p_variation_type_id, p_bank_account_number, p_bank_name,
            p_bank_account_status, p_monthly_income, p_additional_income,
            p_income_proof_document_url, p_notes
        ) RETURNING id INTO v_existing_id;
        
        v_result := json_build_object(
            'success', true,
            'message', 'Income variation created successfully',
            'variation_id', v_existing_id
        );
    END IF;
    
    -- Update profile completion
    PERFORM calculate_profile_completion(p_customer_id);
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get customer income variations
CREATE OR REPLACE FUNCTION get_customer_income_variations(p_customer_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_variations JSONB;
BEGIN
    SELECT json_agg(
        json_build_object(
            'id', civ.id,
            'variation_type', json_build_object(
                'id', ivt.id,
                'variation_name', ivt.variation_name,
                'variation_code', ivt.variation_code,
                'description', ivt.description
            ),
            'bank_account_number', civ.bank_account_number,
            'bank_name', civ.bank_name,
            'bank_account_status', civ.bank_account_status,
            'monthly_income', civ.monthly_income,
            'additional_income', civ.additional_income,
            'total_income', civ.monthly_income + civ.additional_income,
            'income_proof_document_url', civ.income_proof_document_url,
            'verification_status', civ.verification_status,
            'verified_by', civ.verified_by,
            'verified_at', civ.verified_at,
            'notes', civ.notes,
            'created_at', civ.created_at,
            'updated_at', civ.updated_at
        ) ORDER BY ivt.variation_name
    ) INTO v_variations
    FROM customer_income_variations civ
    JOIN income_variation_types ivt ON civ.variation_type_id = ivt.id
    WHERE civ.customer_id = p_customer_id;
    
    RETURN COALESCE(v_variations, '[]'::jsonb);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to calculate total customer income
CREATE OR REPLACE FUNCTION calculate_total_customer_income(p_customer_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_total_income DECIMAL(15,2) := 0;
    v_income_breakdown JSONB;
BEGIN
    -- Calculate total income from all variations
    SELECT COALESCE(SUM(monthly_income + additional_income), 0) INTO v_total_income
    FROM customer_income_variations
    WHERE customer_id = p_customer_id AND verification_status = 'VERIFIED';
    
    -- Get income breakdown
    SELECT json_agg(
        json_build_object(
            'variation_name', ivt.variation_name,
            'monthly_income', civ.monthly_income,
            'additional_income', civ.additional_income,
            'total', civ.monthly_income + civ.additional_income,
            'verification_status', civ.verification_status,
            'bank_account_status', civ.bank_account_status
        )
    ) INTO v_income_breakdown
    FROM customer_income_variations civ
    JOIN income_variation_types ivt ON civ.variation_type_id = ivt.id
    WHERE civ.customer_id = p_customer_id;
    
    -- Update monthly_income in main profile
    UPDATE user_profiles 
    SET monthly_income = v_total_income
    WHERE id = p_customer_id;
    
    RETURN json_build_object(
        'total_income', v_total_income,
        'breakdown', COALESCE(v_income_breakdown, '[]'::jsonb),
        'verified_variations', (
            SELECT COUNT(*) 
            FROM customer_income_variations 
            WHERE customer_id = p_customer_id AND verification_status = 'VERIFIED'
        )
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- VIEWS FOR INCOME VARIATION
-- =====================================================

-- Customer Income Summary View
CREATE VIEW customer_income_summary AS
SELECT 
    up.id as customer_id,
    up.name as customer_name,
    up.nik,
    jc.category_name as job_category,
    COALESCE(SUM(civ.monthly_income + civ.additional_income), 0) as total_income,
    COUNT(civ.id) as total_variations,
    COUNT(CASE WHEN civ.verification_status = 'VERIFIED' THEN 1 END) as verified_variations,
    COUNT(CASE WHEN civ.bank_account_status = 'ACTIVE' THEN 1 END) as active_accounts,
    MAX(civ.updated_at) as last_income_update
FROM user_profiles up
LEFT JOIN job_categories jc ON up.current_job_id = jc.id
LEFT JOIN customer_income_variations civ ON up.id = civ.customer_id
WHERE up.role = 'CUSTOMER' AND up.is_active = true
GROUP BY up.id, up.name, up.nik, jc.category_name;

-- Income Variation Report View
CREATE VIEW income_variation_report AS
SELECT 
    jc.category_name as job_category,
    ivt.variation_name,
    ivt.variation_code,
    COUNT(civ.id) as total_customers,
    COUNT(CASE WHEN civ.verification_status = 'VERIFIED' THEN 1 END) as verified_customers,
    COUNT(CASE WHEN civ.bank_account_status = 'ACTIVE' THEN 1 END) as active_accounts,
    AVG(civ.monthly_income + civ.additional_income) as avg_income,
    MAX(civ.monthly_income + civ.additional_income) as max_income,
    MIN(civ.monthly_income + civ.additional_income) as min_income
FROM job_categories jc
JOIN income_variation_types ivt ON jc.id = ivt.job_category_id
LEFT JOIN customer_income_variations civ ON ivt.id = civ.variation_type_id
WHERE jc.is_active = true AND ivt.is_active = true
GROUP BY jc.category_name, ivt.variation_name, ivt.variation_code
ORDER BY jc.category_name, ivt.variation_name;

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_income_variation_types_job_category ON income_variation_types(job_category_id);
CREATE INDEX IF NOT EXISTS idx_income_variation_types_active ON income_variation_types(is_active);
CREATE INDEX IF NOT EXISTS idx_customer_income_variations_customer ON customer_income_variations(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_income_variations_variation_type ON customer_income_variations(variation_type_id);
CREATE INDEX IF NOT EXISTS idx_customer_income_variations_status ON customer_income_variations(verification_status);
CREATE INDEX IF NOT EXISTS idx_customer_income_variations_account_status ON customer_income_variations(bank_account_status);

-- =====================================================
-- RLS POLICIES
-- =====================================================

-- Customer can manage own income variations
CREATE POLICY "Customers manage own income variations" ON customer_income_variations
FOR ALL USING (auth.uid() = customer_id AND role = 'CUSTOMER');

-- All authenticated users can view income variation types
CREATE POLICY "View income variation types" ON income_variation_types
FOR SELECT USING (is_active = true);

-- =====================================================
-- SAMPLE DATA VERIFICATION
-- =====================================================

-- Verify income variation data
SELECT 
    'Income Variation Types' as table_name, 
    COUNT(*) as count 
FROM income_variation_types 
WHERE is_active = true

UNION ALL

SELECT 
    'Job Categories' as table_name, 
    COUNT(*) as count 
FROM job_categories 
WHERE is_active = true;

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
INCOME VARIATION SYSTEM DOCUMENTATION

========================================
FEATURES:
1. Job Category Income Variations:
   - Karyawan: Payroll & Non-Payroll
   - Wirausaha: Rekening Aktif & Rekening Tidak Aktif

2. Bank Account Tracking:
   - Account number and name
   - Account status (Active/Inactive)
   - Verification status
   - Document proof upload

3. Income Calculation:
   - Base monthly income
   - Additional income
   - Total income calculation
   - Verification tracking

4. Reporting & Analytics:
   - Customer income summary
   - Variation type reports
   - Verification status tracking
   - Account status monitoring

========================================
BENEFITS:
- Detailed income tracking per variation
- Bank account verification
- Flexible income types per job category
- Comprehensive audit trail
- Better credit assessment data

========================================
USAGE:
1. Customer selects job category
2. System shows available income variations
3. Customer fills income details per variation
4. Upload proof documents
5. Verification process
6. Income calculation and reporting

========================================
SECURITY:
- RLS policies for customer data
- Verification workflow
- Audit trail for all changes
- Document tracking
- Status monitoring
*/
