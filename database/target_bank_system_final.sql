-- Final Target Bank & Appraisal/LPA System
-- 14-Day Processing Time & Unit Completion Conditions
-- KPRFlow Enterprise - Final Bank Management with Unit Validation

-- =====================================================
-- UPDATE PROCESSING TIME TO 14 DAYS FOR ALL BANKS
-- =====================================================

-- Update all bank features to 14 days processing time
UPDATE bank_features_config 
SET processing_days = 14 
WHERE processing_days = 7;

-- =====================================================
-- ADD UNIT COMPLETION VALIDATION FUNCTIONS
-- =====================================================

-- Function to check unit completion percentage
CREATE OR REPLACE FUNCTION check_unit_completion_percentage(p_unit_id UUID)
RETURNS DECIMAL(5,2) AS $$
DECLARE
    v_unit_completion DECIMAL(5,2);
BEGIN
    -- Get unit completion percentage from units table
    SELECT COALESCE(completion_percentage, 0) INTO v_unit_completion
    FROM units 
    WHERE id = p_unit_id;
    
    RETURN v_unit_completion;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to validate unit for bank selection
CREATE OR REPLACE FUNCTION validate_unit_for_bank_selection(p_unit_id UUID, p_dossier_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_completion_percentage DECIMAL(5,2);
    v_validation_result JSONB;
BEGIN
    -- Get unit completion percentage
    v_completion_percentage := check_unit_completion_percentage(p_unit_id);
    
    -- Build validation result
    v_validation_result := json_build_object(
        'unit_id', p_unit_id,
        'completion_percentage', v_completion_percentage,
        'can_select_bank', CASE WHEN v_completion_percentage >= 60 THEN true ELSE false END,
        'can_upload_documents', true,
        'validation_status', CASE 
            WHEN v_completion_percentage >= 100 THEN 'FULLY_COMPLETE'
            WHEN v_completion_percentage >= 60 THEN 'PARTIALLY_COMPLETE'
            ELSE 'INCOMPLETE'
        END,
        'bank_selection_allowed', v_completion_percentage >= 60,
        'document_upload_allowed', true,
        'recommendations', CASE 
            WHEN v_completion_percentage < 60 THEN json_build_array(
                'Unit completion below 60%',
                'Cannot select bank',
                'Can upload documents for preparation',
                'Wait for unit completion to reach 60%'
            )
            WHEN v_completion_percentage < 100 THEN json_build_array(
                'Unit completion between 60-99%',
                'Can select bank',
                'Can upload documents',
                'Processing time: 14 days'
            )
            ELSE json_build_array(
                'Unit fully complete (100%)',
                'Can select bank',
                'Can upload documents',
                'Processing time: 14 days'
            )
        END
    );
    
    RETURN v_validation_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to check if dossier can proceed to bank selection
CREATE OR REPLACE FUNCTION can_proceed_to_bank_selection(p_dossier_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_unit_id UUID;
    v_completion_percentage DECIMAL(5,2);
BEGIN
    -- Get unit ID from dossier
    SELECT unit_id INTO v_unit_id
    FROM kpr_dossiers 
    WHERE id = p_dossier_id;
    
    -- Check unit completion
    v_completion_percentage := check_unit_completion_percentage(v_unit_id);
    
    -- Return true if completion >= 60%
    RETURN v_completion_percentage >= 60;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- ENHANCED BANK SELECTION WITH UNIT VALIDATION
-- =====================================================

-- Enhanced function to auto-generate Appraisal/LPA with unit validation
CREATE OR REPLACE FUNCTION auto_generate_appraisal_lpa_btn_final(p_dossier_id UUID, p_legal_id UUID)
RETURNS UUID AS $$
DECLARE
    v_request_id UUID;
    v_dossier_data RECORD;
    v_bank_config RECORD;
    v_conditional_docs JSONB;
    v_unit_validation JSONB;
    v_completion_percentage DECIMAL(5,2);
BEGIN
    -- Validate unit completion first
    SELECT unit_id INTO v_dossier_data.unit_id
    FROM kpr_dossiers 
    WHERE id = p_dossier_id;
    
    v_unit_validation := validate_unit_for_bank_selection(v_dossier_data.unit_id, p_dossier_id);
    v_completion_percentage := (v_unit_validation->>'completion_percentage')::DECIMAL;
    
    -- Check if unit completion is sufficient for bank selection
    IF NOT (v_unit_validation->>'can_select_bank')::BOOLEAN THEN
        RAISE EXCEPTION 'Unit completion % is below 60%. Cannot proceed with bank selection.', v_completion_percentage;
    END IF;
    
    -- Get dossier and bank data
    SELECT kd.*, tb.id as bank_id, tb.bank_name
    INTO v_dossier_data
    FROM kpr_dossiers kd
    JOIN target_banks tb ON kd.bank_name = tb.bank_name
    WHERE kd.id = p_dossier_id;
    
    -- Check if bank supports Appraisal/LPA
    IF NOT bank_supports_appraisal_lpa(v_dossier_data.bank_id) THEN
        RAISE EXCEPTION 'Bank % does not support Appraisal/LPA', v_dossier_data.bank_name;
    END IF;
    
    -- Get bank configuration (now 14 days)
    SELECT * INTO v_bank_config
    FROM bank_features_config 
    WHERE bank_id = v_dossier_data.bank_id 
    AND feature_type = 'APPRAISAL_LPA';
    
    -- Check conditional documents
    v_conditional_docs := check_conditional_documents(p_dossier_id, v_dossier_data.bank_id, 'APPRAISAL_LPA');
    
    -- Create Appraisal/LPA request with 14 days processing
    INSERT INTO appraisal_lpa_requests (
        dossier_id, bank_id, request_type, request_date, expected_completion_date,
        property_address, property_description, documents_submitted, created_by
    ) VALUES (
        p_dossier_id,
        v_dossier_data.bank_id,
        'APPRAISAL',
        CURRENT_DATE,
        CURRENT_DATE + 14, -- Updated to 14 days
        'Property address for ' || v_dossier_data.user_id,
        'Property description for KPR application',
        json_build_object(
            'required_documents', v_bank_config.required_documents,
            'conditional_documents', v_conditional_docs,
            'total_documents', json_array_length(v_bank_config.required_documents->'documents') + json_array_length(v_conditional_docs),
            'unit_completion_percentage', v_completion_percentage,
            'unit_validation', v_unit_validation
        ),
        p_legal_id
    ) RETURNING id INTO v_request_id;
    
    -- Generate enhanced internal memo with unit validation
    INSERT INTO internal_memos (
        dossier_id, memo_type, memo_title, memo_content, memo_date, memo_number,
        priority, recipients, document_url, created_by, created_at
    ) VALUES (
        p_dossier_id,
        'APPRAISAL_REQUEST',
        'Final Appraisal Request - ' || v_dossier_data.bank_name || ' (14 Days)',
        'Appraisal request otomatis untuk Bank ' || v_dossier_data.bank_name || 
        '. Customer: ' || v_dossier_data.user_id || 
        '. Property: ' || v_dossier_data.unit_id ||
        '. Unit Completion: ' || v_completion_percentage || '%' ||
        '. Expected completion: ' || (CURRENT_DATE + 14) ||
        '. Processing days: 14' ||
        '. Required documents: ' || (v_bank_config.required_documents->>'documents') ||
        '. Conditional documents: ' || jsonb_pretty(v_conditional_docs) ||
        '. Unit validation: ' || jsonb_pretty(v_unit_validation),
        CURRENT_DATE,
        'APPRAISAL-' || TO_CHAR(NOW(), 'YYYY-MM-DD') || '-' || 
        (SELECT COUNT(*) + 1 FROM internal_memos WHERE memo_type = 'APPRAISAL_REQUEST' AND memo_date = CURRENT_DATE),
        'HIGH',
        json_build_array(p_legal_id),
        '/documents/blank/appraisal_request_btn_final.pdf',
        p_legal_id,
        NOW()
    );
    
    -- Send enhanced WhatsApp notification with 14 days and unit validation
    INSERT INTO whatsapp_notifications (
        dossier_id, notification_type, recipient_phone, recipient_name,
        message_content, message_template, variables_used, sent_by, sent_at
    ) SELECT 
        p_dossier_id,
        'APPRAISAL_REQUEST_AUTO_FINAL',
        up.phone_number,
        up.name,
        'Appraisal request otomatis telah dibuat untuk Bank ' || v_dossier_data.bank_name ||
        '. Estimasi selesai: ' || (CURRENT_DATE + 14) ||
        '. Processing time: 14 hari' ||
        '. Unit completion: ' || v_completion_percentage || '%' ||
        '. Mohon menyiapkan dokumen: KTP, KK, NPWP, Slip Gaji, Rekening Koran, Surat Aktif Kerja, SK Tetap, PKWT/PKWTT' ||
        '. Dokumen tambahan: Parklaring (jika usia kerja < 2 tahun), Surat Keterangan Domisili (jika alamat KTP radius > 25KM)' ||
        '. Total dokumen yang diperlukan: ' || (json_array_length(v_bank_config.required_documents->'documents') + json_array_length(v_conditional_docs)) || ' dokumen.' ||
        '. Status: Unit ' || CASE 
            WHEN v_completion_percentage >= 100 THEN 'sudah 100% lengkap'
            WHEN v_completion_percentage >= 60 THEN 'lengkap ' || v_completion_percentage || '%'
            ELSE 'belum lengkap'
        END,
        'APPRAISAL_AUTO_GENERATED_FINAL',
        json_build_object(
            'bank_name', v_dossier_data.bank_name,
            'customer_name', up.name,
            'expected_completion', CURRENT_DATE + 14,
            'processing_days', 14,
            'unit_completion_percentage', v_completion_percentage,
            'required_docs_count', json_array_length(v_bank_config.required_documents->'documents'),
            'conditional_docs_count', json_array_length(v_conditional_docs),
            'total_docs', json_array_length(v_bank_config.required_documents->'documents') + json_array_length(v_conditional_docs),
            'unit_status', CASE 
                WHEN v_completion_percentage >= 100 THEN 'FULLY_COMPLETE'
                WHEN v_completion_percentage >= 60 THEN 'PARTIALLY_COMPLETE'
                ELSE 'INCOMPLETE'
            END
        ),
        p_legal_id,
        NOW()
    FROM user_profiles up
    JOIN kpr_dossiers kd ON up.id = kd.user_id
    WHERE kd.id = p_dossier_id;
    
    RETURN v_request_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Enhanced function to create BPN/Clearance request with unit validation
CREATE OR REPLACE FUNCTION create_bpn_clearance_with_warning_final(p_dossier_id UUID, p_legal_id UUID)
RETURNS UUID AS $$
DECLARE
    v_request_id UUID;
    v_dossier_data RECORD;
    v_bank_config RECORD;
    v_conditional_docs JSONB;
    v_unit_validation JSONB;
    v_completion_percentage DECIMAL(5,2);
BEGIN
    -- Validate unit completion first
    SELECT unit_id INTO v_dossier_data.unit_id
    FROM kpr_dossiers 
    WHERE id = p_dossier_id;
    
    v_unit_validation := validate_unit_for_bank_selection(v_dossier_data.unit_id, p_dossier_id);
    v_completion_percentage := (v_unit_validation->>'completion_percentage')::DECIMAL;
    
    -- Check if unit completion is sufficient for bank selection
    IF NOT (v_unit_validation->>'can_select_bank')::BOOLEAN THEN
        RAISE EXCEPTION 'Unit completion % is below 60%. Cannot proceed with bank selection.', v_completion_percentage;
    END IF;
    
    -- Get dossier and bank data
    SELECT kd.*, tb.id as bank_id, tb.bank_name
    INTO v_dossier_data
    FROM kpr_dossiers kd
    JOIN target_banks tb ON kd.bank_name = tb.bank_name
    WHERE kd.id = p_dossier_id;
    
    -- Check if bank supports BPN/Clearance
    IF NOT bank_supports_bpn_clearance(v_dossier_data.bank_id) THEN
        RAISE EXCEPTION 'Bank % does not support BPN/Clearance', v_dossier_data.bank_name;
    END IF;
    
    -- Get bank configuration (now 14 days)
    SELECT * INTO v_bank_config
    FROM bank_features_config 
    WHERE bank_id = v_dossier_data.bank_id 
    AND feature_type = 'BPN_CLEARANCE';
    
    -- Check conditional documents
    v_conditional_docs := check_conditional_documents(p_dossier_id, v_dossier_data.bank_id, 'BPN_CLEARANCE');
    
    -- Create BPN/Clearance request with 14 days processing
    INSERT INTO bpn_clearance_requests (
        dossier_id, bank_id, request_type, request_date, expected_completion_date,
        documents_submitted, additional_instructions, created_by
    ) VALUES (
        p_dossier_id,
        v_dossier_data.bank_id,
        'BPN_CEK',
        CURRENT_DATE,
        CURRENT_DATE + 14, -- Updated to 14 days
        json_build_object(
            'required_documents', v_bank_config.required_documents,
            'conditional_documents', v_conditional_docs,
            'total_documents', json_array_length(v_bank_config.required_documents->'documents') + json_array_length(v_conditional_docs),
            'unit_completion_percentage', v_completion_percentage,
            'unit_validation', v_unit_validation
        ),
        'Additional instructions for Bank ' || v_dossier_data.bank_name || ': ' || 
        COALESCE(v_bank_config.additional_instructions, 'Standard BPN clearance process') ||
        '. Conditional requirements: ' || jsonb_pretty(v_conditional_docs) ||
        '. Unit completion: ' || v_completion_percentage || '%',
        p_legal_id
    ) RETURNING id INTO v_request_id;
    
    -- Generate enhanced warning memo with unit validation
    INSERT INTO internal_memos (
        dossier_id, memo_type, memo_title, memo_content, memo_date, memo_number,
        priority, recipients, document_url, created_by, created_at
    ) VALUES (
        p_dossier_id,
        'BPN_CLEARANCE_WARNING',
        'Final BPN/Clearance Warning - ' || v_dossier_data.bank_name || ' (14 Days)',
        'PERINGATAN: Bank ' || v_dossier_data.bank_name || ' memerlukan BPN/Clearance check.' ||
        ' Customer: ' || v_dossier_data.user_id ||
        '. Property: ' || v_dossier_data.unit_id ||
        '. Unit Completion: ' || v_completion_percentage || '%' ||
        '. Expected completion: ' || (CURRENT_DATE + 14) ||
        '. Processing days: 14' ||
        '. Required documents: ' || (v_bank_config.required_documents->>'documents') ||
        '. Conditional documents: ' || jsonb_pretty(v_conditional_docs) ||
        '. Additional instructions: ' || COALESCE(v_bank_config.additional_instructions, 'Standard process') ||
        '. Unit validation: ' || jsonb_pretty(v_unit_validation),
        CURRENT_DATE,
        'BPN-' || TO_CHAR(NOW(), 'YYYY-MM-DD') || '-' || 
        (SELECT COUNT(*) + 1 FROM internal_memos WHERE memo_type = 'BPN_CLEARANCE_WARNING' AND memo_date = CURRENT_DATE),
        'HIGH',
        json_build_array(p_legal_id),
        '/documents/blank/bpn_clearance_warning_final.pdf',
        p_legal_id,
        NOW()
    );
    
    -- Send enhanced WhatsApp notification with 14 days and unit validation
    INSERT INTO whatsapp_notifications (
        dossier_id, notification_type, recipient_phone, recipient_name,
        message_content, message_template, variables_used, sent_by, sent_at
    ) SELECT 
        p_dossier_id,
        'BPN_CLEARANCE_WARNING_FINAL',
        up.phone_number,
        up.name,
        'PERINGATAN: Bank ' || v_dossier_data.bank_name || ' memerlukan BPN/Clearance check.' ||
        '. Estimasi selesai: ' || (CURRENT_DATE + 14) ||
        '. Processing time: 14 hari' ||
        '. Unit completion: ' || v_completion_percentage || '%' ||
        '. Mohon menyiapkan dokumen: KTP, KK, NPWP, Slip Gaji, Rekening Koran, Surat Aktif Kerja, SK Tetap, PKWT/PKWTT' ||
        '. Dokumen tambahan: Parklaring (jika usia kerja < 2 tahun), Surat Keterangan Domisili (jika alamat KTP radius > 25KM)' ||
        '. Total dokumen yang diperlukan: ' || (json_array_length(v_bank_config.required_documents->'documents') + json_array_length(v_conditional_docs)) || ' dokumen.' ||
        '. Status: Unit ' || CASE 
            WHEN v_completion_percentage >= 100 THEN 'sudah 100% lengkap'
            WHEN v_completion_percentage >= 60 THEN 'lengkap ' || v_completion_percentage || '%'
            ELSE 'belum lengkap'
        END,
        'BPN_CLEARANCE_WARNING_FINAL',
        json_build_object(
            'bank_name', v_dossier_data.bank_name,
            'customer_name', up.name,
            'expected_completion', CURRENT_DATE + 14,
            'processing_days', 14,
            'unit_completion_percentage', v_completion_percentage,
            'required_docs_count', json_array_length(v_bank_config.required_documents->'documents'),
            'conditional_docs_count', json_array_length(v_conditional_docs),
            'total_docs', json_array_length(v_bank_config.required_documents->'documents') + json_array_length(v_conditional_docs),
            'unit_status', CASE 
                WHEN v_completion_percentage >= 100 THEN 'FULLY_COMPLETE'
                WHEN v_completion_percentage >= 60 THEN 'PARTIALLY_COMPLETE'
                ELSE 'INCOMPLETE'
            END
        ),
        p_legal_id,
        NOW()
    FROM user_profiles up
    JOIN kpr_dossiers kd ON up.id = kd.user_id
    WHERE kd.id = p_dossier_id;
    
    RETURN v_request_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- ENHANCED VIEWS WITH UNIT VALIDATION
-- =====================================================

-- Enhanced Target Banks View with 14-day processing
CREATE OR REPLACE VIEW target_banks_with_features_final AS
SELECT 
    tb.id,
    tb.bank_code,
    tb.bank_name,
    tb.bank_type,
    tb.bank_category,
    tb.is_active,
    -- Appraisal/LPA Support
    CASE 
        WHEN EXISTS(
            SELECT 1 FROM bank_features_config 
            WHERE bank_id = tb.id AND feature_type = 'APPRAISAL_LPA' AND is_enabled = true
        ) THEN true ELSE false 
    END as supports_appraisal_lpa,
    -- BPN/Clearance Support
    CASE 
        WHEN EXISTS(
            SELECT 1 FROM bank_features_config 
            WHERE bank_id = tb.id AND feature_type = 'BPN_CLEARANCE' AND is_enabled = true
        ) THEN true ELSE false 
    END as supports_bpn_clearance,
    -- Feature details (14 days)
    14 as appraisal_processing_days,
    appraisal_config.auto_generate as appraisal_auto_generate,
    appraisal_config.required_documents as appraisal_documents,
    14 as bpn_processing_days,
    bpn_config.additional_instructions as bpn_additional_instructions,
    bpn_config.required_documents as bpn_documents,
    -- Document counts
    json_array_length(appraisal_config.required_documents->'documents') as appraisal_doc_count,
    json_array_length(bpn_config.required_documents->'documents') as bpn_doc_count,
    -- Unit validation requirements
    60 as min_unit_completion_percentage,
    true as document_upload_always_allowed,
    tb.created_at,
    tb.updated_at
FROM target_banks tb
LEFT JOIN bank_features_config appraisal_config ON tb.id = appraisal_config.bank_id AND appraisal_config.feature_type = 'APPRAISAL_LPA'
LEFT JOIN bank_features_config bpn_config ON tb.id = bpn_config.bank_id AND bpn_config.feature_type = 'BPN_CLEARANCE'
WHERE tb.is_active = true
ORDER BY tb.bank_name;

-- Enhanced KPR Dossiers View with unit validation
CREATE OR REPLACE VIEW kpr_dossiers_with_unit_validation AS
SELECT 
    kd.*,
    u.unit_number,
    u.block_number,
    u.completion_percentage,
    u.unit_type,
    u.price,
    -- Unit validation
    validate_unit_for_bank_selection(kd.unit_id, kd.id) as unit_validation,
    -- Bank selection eligibility
    CASE WHEN u.completion_percentage >= 60 THEN true ELSE false END as can_select_bank,
    true as can_upload_documents,
    -- Processing time (14 days)
    14 as standard_processing_days,
    -- Status based on unit completion
    CASE 
        WHEN u.completion_percentage >= 100 THEN 'UNIT_FULLY_COMPLETE'
        WHEN u.completion_percentage >= 60 THEN 'UNIT_PARTIALLY_COMPLETE'
        ELSE 'UNIT_INCOMPLETE'
    END as unit_completion_status
FROM kpr_dossiers kd
JOIN units u ON kd.unit_id = u.id
ORDER BY kd.created_at DESC;

-- =====================================================
-- UPDATE TRIGGERS FOR FINAL FUNCTIONS
-- =====================================================

-- Update trigger to use final functions
CREATE OR REPLACE FUNCTION trigger_bank_features_on_akad_scheduled_final()
RETURNS TRIGGER AS $$
BEGIN
    -- Trigger final bank-specific features with unit validation
    PERFORM trigger_bank_specific_features_final(NEW.id, NEW.legal_assigned_id);
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Final function to trigger bank-specific features with unit validation
CREATE OR REPLACE FUNCTION trigger_bank_specific_features_final(p_akad_id UUID, p_legal_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_dossier_data RECORD;
    v_unit_validation JSONB;
    v_appraisal_id UUID;
    v_bpn_id UUID;
BEGIN
    -- Get dossier and bank data
    SELECT kd.*, tb.id as bank_id, tb.bank_name
    INTO v_dossier_data
    FROM akad_credit_management acm
    JOIN kpr_dossiers kd ON acm.dossier_id = kd.id
    JOIN target_banks tb ON kd.bank_name = tb.bank_name
    WHERE acm.id = p_akad_id;
    
    -- Validate unit completion
    v_unit_validation := validate_unit_for_bank_selection(v_dossier_data.unit_id, v_dossier_data.dossier_id);
    
    -- Check if unit completion is sufficient for bank selection
    IF NOT (v_unit_validation->>'can_select_bank')::BOOLEAN THEN
        RAISE EXCEPTION 'Unit completion % is below 60%. Cannot trigger bank-specific features.', (v_unit_validation->>'completion_percentage')::DECIMAL;
    END IF;
    
    -- Check if bank supports Appraisal/LPA (Bank BTN only)
    IF bank_supports_appraisal_lpa(v_dossier_data.bank_id) THEN
        -- Auto-generate Appraisal/LPA request with final features
        v_appraisal_id := auto_generate_appraisal_lpa_btn_final(v_dossier_data.dossier_id, p_legal_id);
    END IF;
    
    -- Check if bank supports BPN/Clearance (Bank BNI, BSI, BTN)
    IF bank_supports_bpn_clearance(v_dossier_data.bank_id) THEN
        -- Create BPN/Clearance request with final features
        v_bpn_id := create_bpn_clearance_with_warning_final(v_dossier_data.dossier_id, p_legal_id);
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Drop old trigger and create new one
DROP TRIGGER IF EXISTS trigger_bank_features_on_akad_scheduled_enhanced ON akad_credit_management;

CREATE TRIGGER trigger_bank_features_on_akad_scheduled_final
    AFTER UPDATE ON akad_credit_management
    FOR EACH ROW EXECUTE FUNCTION trigger_bank_features_on_akad_scheduled_final();

-- =====================================================
-- UNIT COMPLETION VALIDATION POLICIES
-- =====================================================

-- RLS Policy for unit validation
CREATE POLICY "Users can view their own unit validation" ON kpr_dossiers_with_unit_validation
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Legal can view all unit validations" ON kpr_dossiers_with_unit_validation
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM user_profiles 
            WHERE id = auth.uid() AND role = 'LEGAL'
        )
    );

-- =====================================================
-- SAMPLE DATA UPDATE FOR FINAL SYSTEM
-- =====================================================

-- Update sample units with completion percentages
UPDATE units SET completion_percentage = 100 WHERE id IN (
    SELECT unit_id FROM kpr_dossiers WHERE status = 'APPROVED'
);

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
FINAL TARGET BANK SYSTEM DOCUMENTATION

========================================
UPDATED PROCESSING TIME & UNIT VALIDATION:

Processing Time: 14 days for all banks and all features
Unit Completion Requirements:
- 100% completion: Full bank selection and document upload
- 60-99% completion: Can select bank and upload documents
- <60% completion: Cannot select bank, but can upload documents

========================================
UNIT VALIDATION LOGIC:

1. Unit Completion >= 100%:
   - Can select any bank
   - Can upload documents
   - Processing time: 14 days
   - Status: UNIT_FULLY_COMPLETE

2. Unit Completion 60-99%:
   - Can select bank
   - Can upload documents
   - Processing time: 14 days
   - Status: UNIT_PARTIALLY_COMPLETE

3. Unit Completion < 60%:
   - Cannot select bank
   - Can upload documents (for preparation)
   - Cannot proceed with bank-specific features
   - Status: UNIT_INCOMPLETE

========================================
BANK FEATURES WITH 14-DAY PROCESSING:

Bank BTN (Appraisal/LPA + BPN/Clearance):
- Processing time: 14 days
- Document requirements: 13 base + 2 conditional
- Unit requirement: >=60% for bank selection

Bank BNI (BPN/Clearance Only):
- Processing time: 14 days
- Document requirements: 11 base + 2 conditional
- Unit requirement: >=60% for bank selection

Bank Syariah Indonesia (BPN/Clearance Only):
- Processing time: 14 days
- Document requirements: 11 base + 3 conditional
- Unit requirement: >=60% for bank selection

========================================
ENHANCED FEATURES:
- Unit completion validation before bank selection
- 14-day standardized processing time
- Document upload always allowed (even for <60% units)
- Enhanced WhatsApp notifications with unit status
- Better resource planning with consistent timeline
*/
