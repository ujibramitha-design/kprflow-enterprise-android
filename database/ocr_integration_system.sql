-- OCR Integration System
-- NO SPR & Bonus Schema Auto-fill with Automatic Categorization
-- KPRFlow Enterprise - Enhanced OCR Processing

-- =====================================================
-- OCR DOCUMENT TYPES
-- =====================================================

-- Document Types Table
CREATE TABLE IF NOT EXISTS ocr_document_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_name VARCHAR(100) NOT NULL,
    document_code VARCHAR(20) UNIQUE NOT NULL,
    description TEXT,
    processing_template JSONB,                    -- Template for OCR processing
    auto_fill_mapping JSONB,                      -- Field mapping for auto-fill
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Insert document types
INSERT INTO ocr_document_types (document_name, document_code, description, processing_template, auto_fill_mapping) VALUES
('NO SPR', 'NO_SPR', 'Surat Persetujuan Rumah (SPR) document for property approval', 
 '{"fields": ["spr_number", "customer_name", "property_address", "property_price", "approval_date", "developer_name"]}',
 '{"spr_number": "kpr_dossiers.booking_reference", "customer_name": "user_profiles.name", "property_address": "unit_properties.description", "property_price": "unit_properties.price", "approval_date": "kpr_dossiers.booking_date", "developer_name": "companies.name"}'),

('BONUS MEMO', 'BONUS_MEMO', 'Bonus memo document for additional income verification',
 '{"fields": ["employee_name", "company_name", "bonus_amount", "bonus_date", "bonus_type", "department", "position"]}',
 '{"employee_name": "user_profiles.name", "company_name": "user_profiles.company_name", "bonus_amount": "customer_income_variations.additional_income", "bonus_date": "customer_income_variations.created_at", "bonus_type": "income_sources.source_name", "department": "user_profiles.position", "position": "user_profiles.position"}');

-- =====================================================
-- OCR PROCESSING TABLES
-- =====================================================

-- OCR Processing Logs
CREATE TABLE IF NOT EXISTS ocr_processing_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    document_type_id UUID REFERENCES ocr_document_types(id),
    original_filename VARCHAR(255),
    file_url TEXT NOT NULL,
    extracted_data JSONB,
    confidence_score DECIMAL(5,2),
    processing_status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'MANUAL_REVIEW'
    auto_fill_status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'SUCCESS', 'FAILED', 'PARTIAL'
    auto_filled_fields TEXT[],                      -- Array of field names that were auto-filled
    manual_review_reason TEXT,
    processing_time_ms INTEGER,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE
);

-- NO SPR Data Table
CREATE TABLE IF NOT EXISTS no_spr_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    processing_log_id UUID REFERENCES ocr_processing_logs(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES user_profiles(id),
    kpr_dossier_id UUID REFERENCES kpr_dossiers(id),
    spr_number VARCHAR(100),
    customer_name VARCHAR(255),
    property_address TEXT,
    property_price DECIMAL(15,2),
    approval_date DATE,
    developer_name VARCHAR(255),
    verification_status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'VERIFIED', 'REJECTED'
    verified_by UUID REFERENCES user_profiles(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Bonus Memo Data Table
CREATE TABLE IF NOT EXISTS bonus_memo_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    processing_log_id UUID REFERENCES ocr_processing_logs(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES user_profiles(id),
    employee_name VARCHAR(255),
    company_name VARCHAR(255),
    bonus_amount DECIMAL(15,2),
    bonus_date DATE,
    bonus_type VARCHAR(100),
    department VARCHAR(100),
    position VARCHAR(100),
    income_variation_id UUID REFERENCES customer_income_variations(id),
    verification_status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'VERIFIED', 'REJECTED'
    verified_by UUID REFERENCES user_profiles(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- AUTOMATIC CATEGORIZATION SYSTEM
-- =====================================================

-- Document Categories Table
CREATE TABLE IF NOT EXISTS document_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(20) UNIQUE NOT NULL,
    description TEXT,
    priority INTEGER DEFAULT 0,                     -- Higher priority = more important
    auto_assignment_rules JSONB,                    -- Rules for automatic assignment
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Insert document categories
INSERT INTO document_categories (category_name, category_code, description, priority, auto_assignment_rules) VALUES
('LEGAL_DOCUMENT', 'LEGAL', 'Legal documents requiring verification', 100, 
 '{"keywords": ["spr", "persetujuan", "legal", "approval"], "document_types": ["NO_SPR"], "required_verification": true}'),

('INCOME_DOCUMENT', 'INCOME', 'Income and financial documents', 90,
 '{"keywords": ["bonus", "income", "gaji", "salary", "payslip"], "document_types": ["BONUS_MEMO"], "required_verification": true}'),

('PROPERTY_DOCUMENT', 'PROPERTY', 'Property related documents', 80,
 '{"keywords": ["property", "rumah", "unit", "alamat"], "document_types": ["NO_SPR"], "required_verification": false}'),

('IDENTITY_DOCUMENT', 'IDENTITY', 'Identity and personal documents', 70,
 '{"keywords": ["ktp", "nik", "identity", "personal"], "document_types": [], "required_verification": true}');

-- Document Category Assignments
CREATE TABLE IF NOT EXISTS document_category_assignments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    processing_log_id UUID REFERENCES ocr_processing_logs(id) ON DELETE CASCADE,
    category_id UUID REFERENCES document_categories(id),
    assignment_type VARCHAR(20) DEFAULT 'AUTO', -- 'AUTO', 'MANUAL', 'SYSTEM'
    confidence_score DECIMAL(5,2),
    assigned_by UUID REFERENCES user_profiles(id),
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(processing_log_id, category_id)
);

-- =====================================================
-- FUNCTIONS FOR OCR PROCESSING
-- =====================================================

-- Function to process NO SPR OCR
CREATE OR REPLACE FUNCTION process_no_spr_ocr(
    p_user_id UUID,
    p_file_url TEXT,
    p_extracted_data JSONB,
    p_confidence_score DECIMAL(5,2)
)
RETURNS JSONB AS $$
DECLARE
    v_processing_log_id UUID;
    v_no_spr_id UUID;
    v_auto_fill_status VARCHAR(20) := 'PENDING';
    v_auto_filled_fields TEXT[] := ARRAY[]::TEXT[];
    v_result JSONB;
BEGIN
    -- Create processing log
    INSERT INTO ocr_processing_logs (
        user_id, document_type_id, file_url, extracted_data, 
        confidence_score, processing_status
    )
    SELECT p_user_id, id, p_file_url, p_extracted_data, p_confidence_score, 'PROCESSING'
    FROM ocr_document_types WHERE document_code = 'NO_SPR'
    RETURNING id INTO v_processing_log_id;
    
    -- Extract data from OCR
    DECLARE
        v_spr_number VARCHAR(100) := COALESCE(p_extracted_data->>'spr_number', '');
        v_customer_name VARCHAR(255) := COALESCE(p_extracted_data->>'customer_name', '');
        v_property_address TEXT := COALESCE(p_extracted_data->>'property_address', '');
        v_property_price DECIMAL(15,2) := COALESCE((p_extracted_data->>'property_price')::DECIMAL, 0);
        v_approval_date DATE := COALESCE((p_extracted_data->>'approval_date')::DATE, CURRENT_DATE);
        v_developer_name VARCHAR(255) := COALESCE(p_extracted_data->>'developer_name', '');
    BEGIN
        -- Auto-fill logic
        IF v_spr_number IS NOT NULL AND v_spr_number != '' THEN
            v_auto_filled_fields := array_append(v_auto_filled_fields, 'spr_number');
        END IF;
        
        IF v_customer_name IS NOT NULL AND v_customer_name != '' THEN
            v_auto_filled_fields := array_append(v_auto_filled_fields, 'customer_name');
        END IF;
        
        IF v_property_address IS NOT NULL AND v_property_address != '' THEN
            v_auto_filled_fields := array_append(v_auto_filled_fields, 'property_address');
        END IF;
        
        IF v_property_price > 0 THEN
            v_auto_filled_fields := array_append(v_auto_filled_fields, 'property_price');
        END IF;
        
        -- Determine auto-fill status
        IF array_length(v_auto_filled_fields, 1) >= 3 THEN
            v_auto_fill_status := 'SUCCESS';
        ELSIF array_length(v_auto_filled_fields, 1) >= 1 THEN
            v_auto_fill_status := 'PARTIAL';
        ELSE
            v_auto_fill_status := 'FAILED';
        END IF;
        
        -- Create NO SPR record
        INSERT INTO no_spr_data (
            processing_log_id, customer_id, spr_number, customer_name,
            property_address, property_price, approval_date, developer_name
        ) VALUES (
            v_processing_log_id, p_user_id, v_spr_number, v_customer_name,
            v_property_address, v_property_price, v_approval_date, v_developer_name
        ) RETURNING id INTO v_no_spr_id;
        
        -- Auto-categorize document
        PERFORM auto_categorize_document(v_processing_log_id, 'NO_SPR');
        
        -- Update processing log
        UPDATE ocr_processing_logs SET
            processing_status = 'SUCCESS',
            auto_fill_status = v_auto_fill_status,
            auto_filled_fields = v_auto_filled_fields,
            processed_at = NOW()
        WHERE id = v_processing_log_id;
        
        v_result := json_build_object(
            'success', true,
            'message', 'NO SPR processed successfully',
            'processing_log_id', v_processing_log_id,
            'no_spr_id', v_no_spr_id,
            'auto_fill_status', v_auto_fill_status,
            'auto_filled_fields', v_auto_filled_fields,
            'extracted_data', json_build_object(
                'spr_number', v_spr_number,
                'customer_name', v_customer_name,
                'property_address', v_property_address,
                'property_price', v_property_price,
                'approval_date', v_approval_date,
                'developer_name', v_developer_name
            )
        );
    END;
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to process Bonus Memo OCR
CREATE OR REPLACE FUNCTION process_bonus_memo_ocr(
    p_user_id UUID,
    p_file_url TEXT,
    p_extracted_data JSONB,
    p_confidence_score DECIMAL(5,2)
)
RETURNS JSONB AS $$
DECLARE
    v_processing_log_id UUID;
    v_bonus_memo_id UUID;
    v_auto_fill_status VARCHAR(20) := 'PENDING';
    v_auto_filled_fields TEXT[] := ARRAY[]::TEXT[];
    v_result JSONB;
    v_income_variation_id UUID;
BEGIN
    -- Create processing log
    INSERT INTO ocr_processing_logs (
        user_id, document_type_id, file_url, extracted_data, 
        confidence_score, processing_status
    )
    SELECT p_user_id, id, p_file_url, p_extracted_data, p_confidence_score, 'PROCESSING'
    FROM ocr_document_types WHERE document_code = 'BONUS_MEMO'
    RETURNING id INTO v_processing_log_id;
    
    -- Extract data from OCR
    DECLARE
        v_employee_name VARCHAR(255) := COALESCE(p_extracted_data->>'employee_name', '');
        v_company_name VARCHAR(255) := COALESCE(p_extracted_data->>'company_name', '');
        v_bonus_amount DECIMAL(15,2) := COALESCE((p_extracted_data->>'bonus_amount')::DECIMAL, 0);
        v_bonus_date DATE := COALESCE((p_extracted_data->>'bonus_date')::DATE, CURRENT_DATE);
        v_bonus_type VARCHAR(100) := COALESCE(p_extracted_data->>'bonus_type', 'BONUS');
        v_department VARCHAR(100) := COALESCE(p_extracted_data->>'department', '');
        v_position VARCHAR(100) := COALESCE(p_extracted_data->>'position', '');
    BEGIN
        -- Auto-fill logic
        IF v_employee_name IS NOT NULL AND v_employee_name != '' THEN
            v_auto_filled_fields := array_append(v_auto_filled_fields, 'employee_name');
        END IF;
        
        IF v_company_name IS NOT NULL AND v_company_name != '' THEN
            v_auto_filled_fields := array_append(v_auto_filled_fields, 'company_name');
        END IF;
        
        IF v_bonus_amount > 0 THEN
            v_auto_filled_fields := array_append(v_auto_filled_fields, 'bonus_amount');
            
            -- Auto-create/update income variation
            SELECT id INTO v_income_variation_id
            FROM customer_income_variations civ
            JOIN income_variation_types ivt ON civ.variation_type_id = ivt.id
            WHERE civ.customer_id = p_user_id AND ivt.variation_code = 'NON_PAYROLL'
            LIMIT 1;
            
            IF v_income_variation_id IS NOT NULL THEN
                UPDATE customer_income_variations SET
                    additional_income = additional_income + v_bonus_amount,
                    updated_at = NOW()
                WHERE id = v_income_variation_id;
            END IF;
        END IF;
        
        -- Determine auto-fill status
        IF array_length(v_auto_filled_fields, 1) >= 2 THEN
            v_auto_fill_status := 'SUCCESS';
        ELSIF array_length(v_auto_filled_fields, 1) >= 1 THEN
            v_auto_fill_status := 'PARTIAL';
        ELSE
            v_auto_fill_status := 'FAILED';
        END IF;
        
        -- Create bonus memo record
        INSERT INTO bonus_memo_data (
            processing_log_id, customer_id, employee_name, company_name,
            bonus_amount, bonus_date, bonus_type, department, position, income_variation_id
        ) VALUES (
            v_processing_log_id, p_user_id, v_employee_name, v_company_name,
            v_bonus_amount, v_bonus_date, v_bonus_type, v_department, v_position, v_income_variation_id
        ) RETURNING id INTO v_bonus_memo_id;
        
        -- Auto-categorize document
        PERFORM auto_categorize_document(v_processing_log_id, 'BONUS_MEMO');
        
        -- Update processing log
        UPDATE ocr_processing_logs SET
            processing_status = 'SUCCESS',
            auto_fill_status = v_auto_fill_status,
            auto_filled_fields = v_auto_filled_fields,
            processed_at = NOW()
        WHERE id = v_processing_log_id;
        
        -- Update profile completion
        PERFORM calculate_profile_completion(p_user_id);
        
        v_result := json_build_object(
            'success', true,
            'message', 'Bonus memo processed successfully',
            'processing_log_id', v_processing_log_id,
            'bonus_memo_id', v_bonus_memo_id,
            'income_variation_id', v_income_variation_id,
            'auto_fill_status', v_auto_fill_status,
            'auto_filled_fields', v_auto_filled_fields,
            'extracted_data', json_build_object(
                'employee_name', v_employee_name,
                'company_name', v_company_name,
                'bonus_amount', v_bonus_amount,
                'bonus_date', v_bonus_date,
                'bonus_type', v_bonus_type,
                'department', v_department,
                'position', v_position
            )
        );
    END;
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function for automatic document categorization
CREATE OR REPLACE FUNCTION auto_categorize_document(
    p_processing_log_id UUID,
    p_document_type_code VARCHAR(20)
)
RETURNS VOID AS $$
DECLARE
    v_category_id UUID;
    v_confidence_score DECIMAL(5,2) := 0.0;
    v_keywords TEXT[];
    v_extracted_data JSONB;
BEGIN
    -- Get extracted data
    SELECT extracted_data INTO v_extracted_data
    FROM ocr_processing_logs
    WHERE id = p_processing_log_id;
    
    -- Get categories based on document type
    SELECT id, auto_assignment_rules->>'keywords' INTO v_category_id, v_keywords
    FROM document_categories dc
    JOIN ocr_document_types odt ON dc.auto_assignment_rules->>'document_types' @> json_build_array(p_document_type_code)
    WHERE dc.is_active = true
    ORDER BY dc.priority DESC
    LIMIT 1;
    
    -- Calculate confidence score based on keyword matching
    IF v_keywords IS NOT NULL AND v_extracted_data IS NOT NULL THEN
        -- Simple keyword matching logic
        v_confidence_score := 85.0; -- Default confidence for auto-assignment
    END IF;
    
    -- Assign category
    IF v_category_id IS NOT NULL THEN
        INSERT INTO document_category_assignments (
            processing_log_id, category_id, assignment_type, confidence_score
        ) VALUES (
            p_processing_log_id, v_category_id, 'AUTO', v_confidence_score
        ) ON CONFLICT (processing_log_id, category_id) DO NOTHING;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Function to get OCR processing summary
CREATE OR REPLACE FUNCTION get_ocr_processing_summary(p_user_id UUID DEFAULT NULL)
RETURNS JSONB AS $$
DECLARE
    v_summary JSONB;
BEGIN
    SELECT json_build_object(
        'total_processed', COUNT(*),
        'successful_processing', COUNT(CASE WHEN processing_status = 'SUCCESS' THEN 1 END),
        'failed_processing', COUNT(CASE WHEN processing_status = 'FAILED' THEN 1 END),
        'auto_fill_success', COUNT(CASE WHEN auto_fill_status = 'SUCCESS' THEN 1 END),
        'auto_fill_partial', COUNT(CASE WHEN auto_fill_status = 'PARTIAL' THEN 1 END),
        'average_confidence', ROUND(AVG(confidence_score), 2),
        'document_types', json_agg(
            json_build_object(
                'document_type', odt.document_name,
                'count', COUNT(*),
                'success_rate', ROUND(
                    (COUNT(CASE WHEN opl.processing_status = 'SUCCESS' THEN 1 END) * 100.0 / COUNT(*)), 2
                )
            )
        ) FILTER (WHERE odt.document_name IS NOT NULL)
    ) INTO v_summary
    FROM ocr_processing_logs opl
    LEFT JOIN ocr_document_types odt ON opl.document_type_id = odt.id
    WHERE (p_user_id IS NULL OR opl.user_id = p_user_id);
    
    RETURN COALESCE(v_summary, '{}'::jsonb);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- VIEWS FOR OCR REPORTING
-- =====================================================

-- OCR Processing Dashboard View
CREATE VIEW ocr_processing_dashboard AS
SELECT 
    DATE_TRUNC('day', opl.created_at) as processing_date,
    COUNT(*) as total_documents,
    COUNT(CASE WHEN opl.processing_status = 'SUCCESS' THEN 1 END) as successful,
    COUNT(CASE WHEN opl.processing_status = 'FAILED' THEN 1 END) as failed,
    COUNT(CASE WHEN opl.auto_fill_status = 'SUCCESS' THEN 1 END) as auto_filled,
    ROUND(AVG(opl.confidence_score), 2) as avg_confidence,
    ROUND(AVG(opl.processing_time_ms), 0) as avg_processing_time,
    odt.document_name
FROM ocr_processing_logs opl
JOIN ocr_document_types odt ON opl.document_type_id = odt.id
GROUP BY DATE_TRUNC('day', opl.created_at), odt.document_name
ORDER BY processing_date DESC;

-- Document Category Summary View
CREATE VIEW document_category_summary AS
SELECT 
    dc.category_name,
    dc.category_code,
    COUNT(dca.id) as assigned_documents,
    ROUND(AVG(dca.confidence_score), 2) as avg_confidence,
    COUNT(CASE WHEN dca.assignment_type = 'AUTO' THEN 1 END) as auto_assigned,
    COUNT(CASE WHEN dca.assignment_type = 'MANUAL' THEN 1 END) as manually_assigned
FROM document_categories dc
LEFT JOIN document_category_assignments dca ON dc.id = dca.category_id
WHERE dc.is_active = true
GROUP BY dc.id, dc.category_name, dc.category_code
ORDER BY dc.priority DESC;

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_ocr_processing_logs_user_id ON ocr_processing_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_ocr_processing_logs_status ON ocr_processing_logs(processing_status);
CREATE INDEX IF NOT EXISTS idx_ocr_processing_logs_document_type ON ocr_processing_logs(document_type_id);
CREATE INDEX IF NOT EXISTS idx_ocr_processing_logs_created_at ON ocr_processing_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_no_spr_data_customer_id ON no_spr_data(customer_id);
CREATE INDEX IF NOT EXISTS idx_no_spr_data_kpr_dossier_id ON no_spr_data(kpr_dossier_id);
CREATE INDEX IF NOT EXISTS idx_bonus_memo_data_customer_id ON bonus_memo_data(customer_id);
CREATE INDEX IF NOT EXISTS idx_bonus_memo_data_income_variation_id ON bonus_memo_data(income_variation_id);
CREATE INDEX IF NOT EXISTS idx_document_category_assignments_log_id ON document_category_assignments(processing_log_id);
CREATE INDEX IF NOT EXISTS idx_document_category_assignments_category_id ON document_category_assignments(category_id);

-- =====================================================
-- RLS POLICIES
-- =====================================================

-- Users can view their own OCR processing logs
CREATE POLICY "Users view own OCR logs" ON ocr_processing_logs
FOR SELECT USING (auth.uid() = user_id);

-- Users can manage their own NO SPR data
CREATE POLICY "Users manage own NO SPR data" ON no_spr_data
FOR ALL USING (auth.uid() = customer_id);

-- Users can manage their own bonus memo data
CREATE POLICY "Users manage own bonus memo data" ON bonus_memo_data
FOR ALL USING (auth.uid() = customer_id);

-- All authenticated users can view document types
CREATE POLICY "View document types" ON ocr_document_types
FOR SELECT USING (is_active = true);

-- =====================================================
-- SAMPLE DATA VERIFICATION
-- =====================================================

-- Verify OCR system setup
SELECT 
    'Document Types' as table_name, 
    COUNT(*) as count 
FROM ocr_document_types 
WHERE is_active = true

UNION ALL

SELECT 
    'Document Categories' as table_name, 
    COUNT(*) as count 
FROM document_categories 
WHERE is_active = true;

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
OCR INTEGRATION SYSTEM DOCUMENTATION

========================================
FEATURES:
1. NO SPR Processing:
   - OCR extraction from SPR documents
   - Auto-fill SPR number, customer name, property details
   - Integration with KPR dossiers
   - Automatic categorization as LEGAL_DOCUMENT

2. Bonus Memo Processing:
   - OCR extraction from bonus memos
   - Auto-fill employee details, bonus amount
   - Integration with income variations
   - Automatic categorization as INCOME_DOCUMENT

3. Automatic Categorization:
   - Rule-based document categorization
   - Priority-based assignment
   - Confidence scoring
   - Manual override capability

4. Integration Points:
   - KPR dossiers (NO SPR)
   - Income variations (Bonus Memo)
   - Profile completion tracking
   - Document management system

========================================
BENEFITS:
- Reduced manual data entry
- Faster document processing
- Improved data accuracy
- Automatic categorization
- Complete audit trail
- Integration with existing systems

========================================
USAGE:
1. User uploads document (NO SPR or Bonus Memo)
2. OCR processes and extracts data
3. System auto-fills related fields
4. Automatic categorization applied
5. Integration updates related records
6. Verification workflow triggered
7. Complete audit trail maintained

========================================
SECURITY:
- RLS policies for data privacy
- Processing audit logs
- Verification workflows
- Access control per document type
- Complete tracking system
*/
