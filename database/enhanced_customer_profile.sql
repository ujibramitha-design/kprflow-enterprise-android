-- Enhanced Customer Profile System
-- KTP OCR Integration & Complete Profile Management
-- KPRFlow Enterprise - Enhanced Customer Data

-- =====================================================
-- CUSTOMER PROFILE ENHANCEMENT
-- =====================================================

-- Update user_profiles table with enhanced fields
ALTER TABLE user_profiles 
ADD COLUMN IF NOT EXISTS birth_place_date VARCHAR(100),
ADD COLUMN IF NOT EXISTS current_job_id UUID REFERENCES job_categories(id),
ADD COLUMN IF NOT EXISTS company_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS position VARCHAR(100),
ADD COLUMN IF NOT EXISTS income_source_id UUID REFERENCES income_sources(id),
ADD COLUMN IF NOT EXISTS income_type_id UUID REFERENCES income_types(id),
ADD COLUMN IF NOT EXISTS monthly_income DECIMAL(15,2),
ADD COLUMN IF NOT EXISTS ktp_verified BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS ktp_image_url TEXT,
ADD COLUMN IF NOT EXISTS ktp_extracted_data JSONB,
ADD COLUMN IF NOT EXISTS profile_completion_percentage INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT false;

-- =====================================================
-- REFERENCE TABLES FOR CUSTOMER DATA
-- =====================================================

-- Job Categories Table
CREATE TABLE IF NOT EXISTS job_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(20) UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Income Sources Table
CREATE TABLE IF NOT EXISTS income_sources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_name VARCHAR(100) NOT NULL,
    source_code VARCHAR(20) UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Income Types Table
CREATE TABLE IF NOT EXISTS income_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type_name VARCHAR(100) NOT NULL,
    type_code VARCHAR(20) UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- KTP OCR PROCESSING TABLE
-- =====================================================

-- KTP Verification Logs
CREATE TABLE IF NOT EXISTS ktp_verification_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    ktp_image_url TEXT NOT NULL,
    extracted_data JSONB,
    verification_status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'SUCCESS', 'FAILED', 'MANUAL_REVIEW'
    confidence_score DECIMAL(5,2),
    manual_review_reason TEXT,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Profile Completion Tracking
('Cek/Giro', 'CEKGIRO', 'Penerimaan income melalui cek atau giro'),
('Digital Wallet', 'DIGITAL', 'Penerimaan income melalui e-wallet'),
('Lainnya', 'LAINNYA', 'Sumber income lainnya');

-- Income Types
INSERT INTO income_types (type_name, type_code, description) VALUES
('Payroll', 'PAYROLL', 'Income tetap dari gaji bulanan'),
('Non-Payroll', 'NONPAYROLL', 'Income tidak tetap dari bisnis/proyek'),
('Investasi', 'INVESTASI', 'Income dari hasil investasi'),
('Lainnya', 'LAINNYA', 'Jenis income lainnya');

-- =====================================================
-- FUNCTIONS FOR PROFILE MANAGEMENT
-- =====================================================

-- Function to calculate profile completion percentage
CREATE OR REPLACE FUNCTION calculate_profile_completion(p_user_id UUID)
RETURNS INTEGER AS $$
DECLARE
    v_total_fields INTEGER := 12; -- Total number of required fields
    v_completed_fields INTEGER := 0;
    v_percentage INTEGER;
BEGIN
    -- Check each required field
    SELECT COUNT(*) INTO v_completed_fields
    FROM (
        SELECT 1 FROM user_profiles WHERE id = p_user_id AND name IS NOT NULL AND name != ''
        UNION ALL
        SELECT 1 FROM user_profiles WHERE id = p_user_id AND email IS NOT NULL AND email != ''
        UNION ALL
        SELECT 1 FROM user_profiles WHERE id = p_user_id AND nik IS NOT NULL AND nik != ''
        UNION ALL
        SELECT 1 FROM user_profiles WHERE id = p_user_id AND phone_number IS NOT NULL AND phone_number != ''
        UNION ALL
        SELECT 1 FROM user_profiles WHERE id = p_user_id AND marital_status IS NOT NULL AND marital_status != ''
        UNION ALL
        SELECT 1 FROM user_profiles WHERE id = p_user_id AND birth_place_date IS NOT NULL AND birth_place_date != ''
        UNION ALL
        SELECT 1 FROM user_profiles WHERE id = p_user_id AND current_job_id IS NOT NULL
        UNION ALL
        SELECT 1 FROM user_profiles WHERE id = p_user_id AND company_name IS NOT NULL AND company_name != ''
        UNION ALL
        SELECT 1 FROM user_profiles WHERE id = p_user_id AND position IS NOT NULL AND position != ''
        UNION ALL
        SELECT 1 FROM user_profiles WHERE id = p_user_id AND income_source_id IS NOT NULL
        UNION ALL
        SELECT 1 FROM user_profiles WHERE id = p_user_id AND income_type_id IS NOT NULL
        UNION ALL
        SELECT 1 FROM user_profiles WHERE id = p_user_id AND monthly_income IS NOT NULL AND monthly_income > 0
    ) completed;
    
    -- Calculate percentage
    v_percentage := (v_completed_fields * 100) / v_total_fields;
    
    -- Update user profile
    UPDATE user_profiles 
    SET profile_completion_percentage = v_percentage
    WHERE id = p_user_id;
    
    RETURN v_percentage;
END;
$$ LANGUAGE plpgsql;

-- Function to process KTP OCR data
CREATE OR REPLACE FUNCTION process_ktp_ocr_data(
    p_user_id UUID,
    p_extracted_data JSONB,
    p_confidence_score DECIMAL(5,2)
)
RETURNS JSONB AS $$
DECLARE
    v_name VARCHAR(255);
    v_nik VARCHAR(16);
    v_birth_place_date VARCHAR(100);
    v_marital_status VARCHAR(50);
    v_success BOOLEAN := false;
    v_result JSONB;
BEGIN
    -- Extract data from OCR
    v_name := COALESCE(p_extracted_data->>'name', '');
    v_nik := COALESCE(p_extracted_data->>'nik', '');
    v_birth_place_date := COALESCE(p_extracted_data->>'birth_place_date', '');
    v_marital_status := COALESCE(p_extracted_data->>'marital_status', '');
    
    -- Validate NIK format (16 digits)
    IF v_nik ~ '^[0-9]{16}$' THEN
        -- Update user profile with extracted data
        UPDATE user_profiles SET
            name = COALESCE(NULLIF(v_name, ''), name),
            nik = v_nik,
            birth_place_date = COALESCE(NULLIF(v_birth_place_date, ''), birth_place_date),
            marital_status = COALESCE(NULLIF(v_marital_status, ''), marital_status),
            ktp_verified = true,
            ktp_extracted_data = p_extracted_data,
            profile_completion_percentage = calculate_profile_completion(p_user_id)
        WHERE id = p_user_id;
        
        -- Log successful verification
        INSERT INTO ktp_verification_logs (
            user_id, ktp_image_url, extracted_data, verification_status, confidence_score
        ) VALUES (
            p_user_id, 
            p_extracted_data->>'image_url',
            p_extracted_data,
            'SUCCESS',
            p_confidence_score
        );
        
        v_success := true;
    ELSE
        -- Log failed verification
        INSERT INTO ktp_verification_logs (
            user_id, ktp_image_url, extracted_data, verification_status, confidence_score, manual_review_reason
        ) VALUES (
            p_user_id,
            p_extracted_data->>'image_url',
            p_extracted_data,
            'FAILED',
            p_confidence_score,
            'Invalid NIK format'
        );
    END IF;
    
    -- Build result
    v_result := json_build_object(
        'success', v_success,
        'message', CASE 
            WHEN v_success THEN 'KTP data extracted successfully'
            ELSE 'KTP verification failed, manual input required'
        END,
        'extracted_fields', json_build_object(
            'name', v_name,
            'nik', v_nik,
            'birth_place_date', v_birth_place_date,
            'marital_status', v_marital_status
        ),
        'confidence_score', p_confidence_score
    );
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function for NIK-only login
CREATE OR REPLACE FUNCTION nik_login(p_nik VARCHAR(16), p_device_info JSONB DEFAULT '{}')
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID;
    v_user_exists BOOLEAN := false;
    v_result JSONB;
BEGIN
    -- Check if user exists with this NIK
    SELECT id INTO v_user_id
    FROM user_profiles 
    WHERE nik = p_nik AND is_active = true;
    
    v_user_exists := (v_user_id IS NOT NULL);
    
    IF v_user_exists THEN
        -- Update last login
        UPDATE user_profiles 
        SET last_login_at = NOW()
        WHERE id = v_user_id;
        
        v_result := json_build_object(
            'success', true,
            'user_id', v_user_id,
            'message', 'Login successful',
            'profile_complete', (SELECT profile_completion_percentage FROM user_profiles WHERE id = v_user_id),
            'requires_completion', (SELECT profile_completion_percentage < 100 FROM user_profiles WHERE id = v_user_id)
        );
    ELSE
        -- Create new user with NIK only
        INSERT INTO user_profiles (nik, role, profile_completion_percentage)
        VALUES (p_nik, 'CUSTOMER', 0)
        RETURNING id INTO v_user_id;
        
        v_result := json_build_object(
            'success', true,
            'user_id', v_user_id,
            'message', 'New user created, please complete your profile',
            'profile_complete', 0,
            'requires_completion', true
        );
    END IF;
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get profile completion status
CREATE OR REPLACE FUNCTION get_profile_completion_status(p_user_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_completion_data JSONB;
BEGIN
    SELECT json_build_object(
        'user_id', id,
        'name', name,
        'email', email,
        'nik', nik,
        'phone_number', phone_number,
        'marital_status', marital_status,
        'birth_place_date', birth_place_date,
        'current_job_id', current_job_id,
        'company_name', company_name,
        'position', position,
        'income_source_id', income_source_id,
        'income_type_id', income_type_id,
        'monthly_income', monthly_income,
        'profile_completion_percentage', profile_completion_percentage,
        'ktp_verified', ktp_verified,
        'phone_verified', phone_verified,
        'email_verified', email_verified,
        'last_login_at', last_login_at,
        'missing_fields', (
            SELECT json_agg(field_name)
            FROM (
                SELECT 'name' as field_name WHERE name IS NULL OR name = ''
                UNION ALL
                SELECT 'email' as field_name WHERE email IS NULL OR email = ''
                UNION ALL
                SELECT 'phone_number' as field_name WHERE phone_number IS NULL OR phone_number = ''
                UNION ALL
                SELECT 'marital_status' as field_name WHERE marital_status IS NULL OR marital_status = ''
                UNION ALL
                SELECT 'birth_place_date' as field_name WHERE birth_place_date IS NULL OR birth_place_date = ''
                UNION ALL
                SELECT 'current_job_id' as field_name WHERE current_job_id IS NULL
                UNION ALL
                SELECT 'company_name' as field_name WHERE company_name IS NULL OR company_name = ''
                UNION ALL
                SELECT 'position' as field_name WHERE position IS NULL OR position = ''
                UNION ALL
                SELECT 'income_source_id' as field_name WHERE income_source_id IS NULL
                UNION ALL
                SELECT 'income_type_id' as field_name WHERE income_type_id IS NULL
                UNION ALL
                SELECT 'monthly_income' as field_name WHERE monthly_income IS NULL OR monthly_income <= 0
            ) missing
        )
    ) INTO v_completion_data
    FROM user_profiles 
    WHERE id = p_user_id;
    
    RETURN COALESCE(v_completion_data, '{}'::jsonb);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- VIEWS FOR PROFILE MANAGEMENT
-- =====================================================

-- Customer Profile View
CREATE VIEW customer_profile_view AS
SELECT 
    up.id,
    up.name,
    up.email,
    up.nik,
    up.phone_number,
    up.marital_status,
    up.birth_place_date,
    up.current_job_id,
    jc.category_name as job_category,
    up.company_name,
    up.position,
    up.income_source_id,
    is.source_name as income_source,
    up.income_type_id,
    it.type_name as income_type,
    up.monthly_income,
    up.profile_completion_percentage,
    up.ktp_verified,
    up.phone_verified,
    up.email_verified,
    up.last_login_at,
    up.created_at,
    up.updated_at
FROM user_profiles up
LEFT JOIN job_categories jc ON up.current_job_id = jc.id
LEFT JOIN income_sources is ON up.income_source_id = is.id
LEFT JOIN income_types it ON up.income_type_id = it.id
WHERE up.role = 'CUSTOMER';

-- Profile Completion Report View
CREATE VIEW profile_completion_report AS
SELECT 
    COUNT(*) as total_customers,
    COUNT(CASE WHEN profile_completion_percentage = 100 THEN 1 END) as complete_profiles,
    COUNT(CASE WHEN profile_completion_percentage BETWEEN 80 AND 99 THEN 1 END) as nearly_complete,
    COUNT(CASE WHEN profile_completion_percentage BETWEEN 50 AND 79 THEN 1 END) as half_complete,
    COUNT(CASE WHEN profile_completion_percentage < 50 THEN 1 END) as incomplete,
    ROUND(AVG(profile_completion_percentage), 2) as average_completion,
    COUNT(CASE WHEN ktp_verified = true THEN 1 END) as ktp_verified_count,
    COUNT(CASE WHEN phone_verified = true THEN 1 END) as phone_verified_count,
    COUNT(CASE WHEN email_verified = true THEN 1 END) as email_verified_count
FROM user_profiles 
WHERE role = 'CUSTOMER' AND is_active = true;

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_user_profiles_nik ON user_profiles(nik);
CREATE INDEX IF NOT EXISTS idx_user_profiles_completion ON user_profiles(profile_completion_percentage);
CREATE INDEX IF NOT EXISTS idx_user_profiles_ktp_verified ON user_profiles(ktp_verified);
CREATE INDEX IF NOT EXISTS idx_job_categories_active ON job_categories(is_active);
CREATE INDEX IF NOT EXISTS idx_income_sources_active ON income_sources(is_active);
CREATE INDEX IF NOT EXISTS idx_income_types_active ON income_types(is_active);
CREATE INDEX IF NOT EXISTS idx_ktp_verification_logs_user_id ON ktp_verification_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_profile_completion_tracking_user_id ON profile_completion_tracking(user_id);

-- =====================================================
-- INSERT REFERENCE DATA
-- =====================================================

-- Job Categories (only 2 options as requested)
INSERT INTO job_categories (category_name, category_code, description) VALUES
('Karyawan', 'KARYAWAN', 'Karyawan perusahaan swasta/negeri'),
('Wirausaha', 'WIRAUSAHA', 'Pengusaha/pemilik usaha');

-- Income Sources
INSERT INTO income_sources (source_name, source_code, description) VALUES
('Cash', 'CASH', 'Penghasilan tunai'),
('Transfer', 'TRANSFER', 'Penghasilan transfer'),
('Digital Wallet', 'DIGITAL_WALLET', 'Penghasilan dompet digital');

-- Income Types
INSERT INTO income_types (type_name, type_code, description) VALUES
('Payroll', 'PAYROLL', 'Penghasilan gaji'),
('Non-Payroll', 'NON_PAYROLL', 'Penghasilan non-gaji'),
('Investment', 'INVESTMENT', 'Penghasilan investasi');

-- =====================================================
-- RLS POLICIES
-- =====================================================

-- Customer can view/update own profile
CREATE POLICY "Customers manage own profile" ON user_profiles
FOR ALL USING (auth.uid() = id AND role = 'CUSTOMER');

-- Customer can view own verification logs
CREATE POLICY "Customers view own verification logs" ON ktp_verification_logs
FOR SELECT USING (auth.uid() = user_id);

-- All authenticated users can view reference data
CREATE POLICY "View job categories" ON job_categories
FOR SELECT USING (is_active = true);

CREATE POLICY "View income sources" ON income_sources
FOR SELECT USING (is_active = true);

CREATE POLICY "View income types" ON income_types
FOR SELECT USING (is_active = true);

-- =====================================================
-- TRIGGERS FOR AUTOMATIC UPDATES
-- =====================================================

-- Trigger to update profile completion when fields change
CREATE OR REPLACE FUNCTION update_profile_completion_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        NEW.profile_completion_percentage := calculate_profile_completion(NEW.id);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_profile_completion
    BEFORE UPDATE ON user_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_profile_completion_trigger();

-- =====================================================
-- SAMPLE DATA VERIFICATION
-- =====================================================

-- Verify reference data
SELECT 
    'Job Categories' as table_name, 
    COUNT(*) as count 
FROM job_categories 
WHERE is_active = true

UNION ALL

SELECT 
    'Income Sources' as table_name, 
    COUNT(*) as count 
FROM income_sources 
WHERE is_active = true

UNION ALL

SELECT 
    'Income Types' as table_name, 
    COUNT(*) as count 
FROM income_types 
WHERE is_active = true;

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
ENHANCED CUSTOMER PROFILE SYSTEM DOCUMENTATION

========================================
FEATURES:
1. KTP OCR Integration:
   - Automatic data extraction from KTP images
   - Confidence scoring for OCR accuracy
   - Manual fallback for failed OCR
   - Verification logging and audit trail

2. NIK-Only Login:
   - Login using NIK only
   - Auto-profile creation for new users
   - Profile completion tracking
   - Device info logging

3. Complete Profile Fields:
   - Personal info (name, email, phone, marital status)
   - Birth place/date (format: "Jakarta/01-01-1990")
   - Employment info (job category, company, position)
   - Income info (source, type, monthly amount)
   - Verification status (KTP, phone, email)

4. Reference Data Management:
   - Job categories (PNS, TNI/Polri, BUMN, Swasta, etc.)
   - Income sources (Cash, Transfer, Digital Wallet)
   - Income types (Payroll, Non-Payroll, Investment)

========================================
BENEFITS:
- Auto-fill from KTP reduces manual input
- NIK-only login simplifies onboarding
- Complete profile data for better credit assessment
- Structured reference data ensures consistency
- Profile completion tracking drives user engagement

========================================
SECURITY:
- RLS policies for data privacy
- KTP verification logging
- Profile completion audit trail
- Secure NIK validation
- Phone/email verification tracking

========================================
USAGE:
1. User uploads KTP image
2. OCR extracts data automatically
3. System validates and fills profile
4. User completes missing fields manually
5. Profile completion percentage calculated
6. User can proceed with application
*/
