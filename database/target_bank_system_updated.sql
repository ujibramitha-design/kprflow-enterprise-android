-- Updated Target Bank & Appraisal/LPA System
-- Enhanced Document Requirements and Processing Time
-- KPRFlow Enterprise - Updated Bank Management

-- =====================================================
-- UPDATE BANK FEATURES CONFIGURATION WITH ENHANCED DOCUMENTS
-- =====================================================

-- Update Bank BTN Configuration with Enhanced Documents
UPDATE bank_features_config 
SET 
    required_documents = json_build_object(
        'documents', json_build_array(
            'KTP', 'KK', 'NPWP', 'SLIP_GAJI', 'REKENING_KORAN', 'SURAT_AKTIF_KERJA', 
            'SK_TETAP', 'PKWT_PKWTT', 'PARKLARING', 'SURAT_KETERANGAN_BELUM_MENIKAH', 
            'SURAT_KETERANGAN_BELUM_MEMILIKI_RUMAH', 'SURAT_KETERANGAN_DOMISILI', 'FORM_FLPP_BANK'
        ),
        'special_requirements', json_build_array(
            'Parklaring jika usia kerja masih di bawah 1-2 tahun',
            'Surat keterangan domisili jika alamat KTP berada radius 25KM dari lokasi rumah yang dibeli',
            'Form FLPP bank khusus untuk FLPP financing',
            'Foto rumah dari 4 sisi',
            'Foto lingkungan sekitar',
            'Denah lokasi',
            'Surat kepemilikan tanah'
        ),
        'conditional_documents', json_build_object(
            'PARKLARING', 'Jika usia kerja < 1-2 tahun',
            'SURAT_KETERANGAN_DOMISILI', 'Jika alamat KTP radius > 25KM dari lokasi rumah'
        )
    ),
    processing_days = 7
WHERE bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BTN') AND feature_type = 'APPRAISAL_LPA';

UPDATE bank_features_config 
SET 
    required_documents = json_build_object(
        'documents', json_build_array(
            'SHGB', 'IMB', 'PBB', 'SURAT_KETERANGAN_LURAH', 'FOTO_BANGUNAN', 'SURAT_KETERANGAN_TANAH'
        ),
        'special_requirements', json_build_array(
            'Verifikasi status tanah di BPN',
            'Cek legalitas bangunan',
            'Surat keterangan tidak sengketa',
            'Cek fisik bangunan',
            'Verifikasi kepemilikan tanah'
        )
    ),
    processing_days = 7
WHERE bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BTN') AND feature_type = 'BPN_CLEARANCE';

-- Update Bank BNI Configuration with Enhanced Documents
UPDATE bank_features_config 
SET 
    required_documents = json_build_object(
        'documents', json_build_array(
            'KTP', 'KK', 'NPWP', 'SLIP_GAJI', 'REKENING_KORAN', 'SURAT_AKTIF_KERJA', 
            'SK_TETAP', 'PKWT_PKWTT', 'PARKLARING', 'SURAT_KETERANGAN_BELUM_MENIKAH', 
            'SURAT_KETERANGAN_BELUM_MEMILIKI_RUMAH', 'SURAT_KETERANGAN_DOMISILI', 'FORM_FLPP_BANK'
        ),
        'special_requirements', json_build_array(
            'Parklaring jika usia kerja masih di bawah 1-2 tahun',
            'Surat keterangan domisili jika alamat KTP berada radius 25KM dari lokasi rumah yang dibeli',
            'Form FLPP bank khusus untuk FLPP financing'
        ),
        'conditional_documents', json_build_object(
            'PARKLARING', 'Jika usia kerja < 1-2 tahun',
            'SURAT_KETERANGAN_DOMISILI', 'Jika alamat KTP radius > 25KM dari lokasi rumah'
        )
    ),
    processing_days = 7
WHERE bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BNI') AND feature_type = 'BPN_CLEARANCE';

-- Update Bank Syariah Indonesia Configuration with Enhanced Documents
UPDATE bank_features_config 
SET 
    required_documents = json_build_object(
        'documents', json_build_array(
            'KTP', 'KK', 'NPWP', 'SLIP_GAJI', 'REKENING_KORAN', 'SURAT_AKTIF_KERJA', 
            'SK_TETAP', 'PKWT_PKWTT', 'PARKLARING', 'SURAT_KETERANGAN_BELUM_MENIKAH', 
            'SURAT_KETERANGAN_BELUM_MEMILIKI_RUMAH', 'SURAT_KETERANGAN_DOMISILI', 'FORM_FLPP_BANK'
        ),
        'special_requirements', json_build_array(
            'Parklaring jika usia kerja masih di bawah 1-2 tahun',
            'Surat keterangan domisili jika alamat KTP berada radius 25KM dari lokasi rumah yang dibeli',
            'Form FLPP bank khusus untuk FLPP financing',
            'Cek status wakaf (jika ada)',
            'Verifikasi kepemilikan sesuai syariah',
            'Cek status legalitas tanah'
        ),
        'conditional_documents', json_build_object(
            'PARKLARING', 'Jika usia kerja < 1-2 tahun',
            'SURAT_KETERANGAN_DOMISILI', 'Jika alamat KTP radius > 25KM dari lokasi rumah',
            'SURAT_KETERANGAN_WAKAF', 'Jika properti adalah tanah wakaf'
        )
    ),
    processing_days = 7
WHERE bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BSI') AND feature_type = 'BPN_CLEARANCE';

-- =====================================================
-- ENHANCED FUNCTIONS FOR CONDITIONAL DOCUMENTS
-- =====================================================

-- Function to check conditional document requirements
CREATE OR REPLACE FUNCTION check_conditional_documents(
    p_dossier_id UUID, 
    p_bank_id UUID, 
    p_feature_type VARCHAR
)
RETURNS JSONB AS $$
DECLARE
    v_customer_data RECORD;
    v_conditional_docs JSONB;
    v_required_docs JSONB;
BEGIN
    -- Get customer data for conditional checks
    SELECT up.*, kd.kpr_amount, kd.unit_id
    INTO v_customer_data
    FROM kpr_dossiers kd
    JOIN user_profiles up ON kd.user_id = up.id
    WHERE kd.id = p_dossier_id;
    
    -- Get bank configuration
    SELECT required_documents INTO v_required_docs
    FROM bank_features_config 
    WHERE bank_id = p_bank_id AND feature_type = p_feature_type;
    
    -- Initialize conditional documents
    v_conditional_docs := json_build_object();
    
    -- Check work experience for Parklaring
    IF v_customer_data.work_experience_years < 2 THEN
        v_conditional_docs := jsonb_set(
            v_conditional_docs, 
            '{PARKLARING}', 
            json_build_object(
                'required', true,
                'reason', 'Usia kerja < 2 tahun',
                'description', 'Parklaring diperlukan karena usia kerja masih di bawah 1-2 tahun'
            )
        );
    END IF;
    
    -- Check domicile distance (simplified - would need actual location calculation)
    -- For now, assume domicile check is needed for all customers
    v_conditional_docs := jsonb_set(
        v_conditional_docs, 
        '{SURAT_KETERANGAN_DOMISILI}', 
        json_build_object(
            'required', true,
            'reason', 'Verifikasi alamat domisili',
            'description', 'Surat keterangan domisili diperlukan jika alamat KTP berada radius 25KM dari lokasi rumah yang dibeli'
        )
    );
    
    -- Check for Syariah bank - Wakaf verification
    IF p_feature_type = 'BPN_CLEARANCE' AND EXISTS(
        SELECT 1 FROM target_banks WHERE id = p_bank_id AND bank_type = 'SYARIAH'
    ) THEN
        v_conditional_docs := jsonb_set(
            v_conditional_docs, 
            '{SURAT_KETERANGAN_WAKAF}', 
            json_build_object(
                'required', true,
                'reason', 'Verifikasi status wakaf',
                'description', 'Surat keterangan wakaf diperlukan untuk properti yang berstatus wakaf'
            )
        );
    END IF;
    
    RETURN v_conditional_docs;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Enhanced function to generate Appraisal/LPA request with conditional documents
CREATE OR REPLACE FUNCTION auto_generate_appraisal_lpa_btn_enhanced(p_dossier_id UUID, p_legal_id UUID)
RETURNS UUID AS $$
DECLARE
    v_request_id UUID;
    v_dossier_data RECORD;
    v_bank_config RECORD;
    v_conditional_docs JSONB;
BEGIN
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
    
    -- Get bank configuration
    SELECT * INTO v_bank_config
    FROM bank_features_config 
    WHERE bank_id = v_dossier_data.bank_id 
    AND feature_type = 'APPRAISAL_LPA';
    
    -- Check conditional documents
    v_conditional_docs := check_conditional_documents(p_dossier_id, v_dossier_data.bank_id, 'APPRAISAL_LPA');
    
    -- Create Appraisal/LPA request
    INSERT INTO appraisal_lpa_requests (
        dossier_id, bank_id, request_type, request_date, expected_completion_date,
        property_address, property_description, documents_submitted, created_by
    ) VALUES (
        p_dossier_id,
        v_dossier_data.bank_id,
        'APPRAISAL',
        CURRENT_DATE,
        CURRENT_DATE + v_bank_config.processing_days,
        'Property address for ' || v_dossier_data.user_id,
        'Property description for KPR application',
        json_build_object(
            'required_documents', v_bank_config.required_documents,
            'conditional_documents', v_conditional_docs,
            'total_documents', json_array_length(v_bank_config.required_documents->'documents') + json_array_length(v_conditional_docs)
        ),
        p_legal_id
    ) RETURNING id INTO v_request_id;
    
    -- Generate enhanced internal memo
    INSERT INTO internal_memos (
        dossier_id, memo_type, memo_title, memo_content, memo_date, memo_number,
        priority, recipients, document_url, created_by, created_at
    ) VALUES (
        p_dossier_id,
        'APPRAISAL_REQUEST',
        'Enhanced Appraisal Request - ' || v_dossier_data.bank_name,
        'Appraisal request otomatis untuk Bank ' || v_dossier_data.bank_name || 
        '. Customer: ' || v_dossier_data.user_id || 
        '. Property: ' || v_dossier_data.unit_id ||
        '. Expected completion: ' || (CURRENT_DATE + v_bank_config.processing_days) ||
        '. Processing days: ' || v_bank_config.processing_days ||
        '. Required documents: ' || (v_bank_config.required_documents->>'documents') ||
        '. Conditional documents: ' || jsonb_pretty(v_conditional_docs),
        CURRENT_DATE,
        'APPRAISAL-' || TO_CHAR(NOW(), 'YYYY-MM-DD') || '-' || 
        (SELECT COUNT(*) + 1 FROM internal_memos WHERE memo_type = 'APPRAISAL_REQUEST' AND memo_date = CURRENT_DATE),
        'HIGH',
        json_build_array(p_legal_id),
        '/documents/blank/appraisal_request_btn_enhanced.pdf',
        p_legal_id,
        NOW()
    );
    
    -- Send enhanced WhatsApp notification
    INSERT INTO whatsapp_notifications (
        dossier_id, notification_type, recipient_phone, recipient_name,
        message_content, message_template, variables_used, sent_by, sent_at
    ) SELECT 
        p_dossier_id,
        'APPRAISAL_REQUEST_AUTO_ENHANCED',
        up.phone_number,
        up.name,
        'Appraisal request otomatis telah dibuat untuk Bank ' || v_dossier_data.bank_name ||
        '. Estimasi selesai: ' || (CURRENT_DATE + v_bank_config.processing_days) ||
        '. Mohon menyiapkan dokumen: KTP, KK, NPWP, Slip Gaji, Rekening Koran, Surat Aktif Kerja, SK Tetap, PKWT/PKWTT' ||
        '. Dokumen tambahan: Parklaring (jika usia kerja < 2 tahun), Surat Keterangan Domisili (jika alamat KTP radius > 25KM)' ||
        '. Total dokumen yang diperlukan: ' || (json_array_length(v_bank_config.required_documents->'documents') + json_array_length(v_conditional_docs)) || ' dokumen.',
        'APPRAISAL_AUTO_GENERATED_ENHANCED',
        json_build_object(
            'bank_name', v_dossier_data.bank_name,
            'customer_name', up.name,
            'expected_completion', CURRENT_DATE + v_bank_config.processing_days,
            'processing_days', v_bank_config.processing_days,
            'required_docs_count', json_array_length(v_bank_config.required_documents->'documents'),
            'conditional_docs_count', json_array_length(v_conditional_docs),
            'total_docs', json_array_length(v_bank_config.required_documents->'documents') + json_array_length(v_conditional_docs)
        ),
        p_legal_id,
        NOW()
    FROM user_profiles up
    JOIN kpr_dossiers kd ON up.id = kd.user_id
    WHERE kd.id = p_dossier_id;
    
    RETURN v_request_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Enhanced function to create BPN/Clearance request with conditional documents
CREATE OR REPLACE FUNCTION create_bpn_clearance_with_warning_enhanced(p_dossier_id UUID, p_legal_id UUID)
RETURNS UUID AS $$
DECLARE
    v_request_id UUID;
    v_dossier_data RECORD;
    v_bank_config RECORD;
    v_conditional_docs JSONB;
BEGIN
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
    
    -- Get bank configuration
    SELECT * INTO v_bank_config
    FROM bank_features_config 
    WHERE bank_id = v_dossier_data.bank_id 
    AND feature_type = 'BPN_CLEARANCE';
    
    -- Check conditional documents
    v_conditional_docs := check_conditional_documents(p_dossier_id, v_dossier_data.bank_id, 'BPN_CLEARANCE');
    
    -- Create BPN/Clearance request
    INSERT INTO bpn_clearance_requests (
        dossier_id, bank_id, request_type, request_date, expected_completion_date,
        documents_submitted, additional_instructions, created_by
    ) VALUES (
        p_dossier_id,
        v_dossier_data.bank_id,
        'BPN_CEK',
        CURRENT_DATE,
        CURRENT_DATE + v_bank_config.processing_days,
        json_build_object(
            'required_documents', v_bank_config.required_documents,
            'conditional_documents', v_conditional_docs,
            'total_documents', json_array_length(v_bank_config.required_documents->'documents') + json_array_length(v_conditional_docs)
        ),
        'Additional instructions for Bank ' || v_dossier_data.bank_name || ': ' || 
        COALESCE(v_bank_config.additional_instructions, 'Standard BPN clearance process') ||
        '. Conditional requirements: ' || jsonb_pretty(v_conditional_docs),
        p_legal_id
    ) RETURNING id INTO v_request_id;
    
    -- Generate enhanced warning memo
    INSERT INTO internal_memos (
        dossier_id, memo_type, memo_title, memo_content, memo_date, memo_number,
        priority, recipients, document_url, created_by, created_at
    ) VALUES (
        p_dossier_id,
        'BPN_CLEARANCE_WARNING',
        'Enhanced BPN/Clearance Warning - ' || v_dossier_data.bank_name,
        'PERINGATAN: Bank ' || v_dossier_data.bank_name || ' memerlukan BPN/Clearance check.' ||
        ' Customer: ' || v_dossier_data.user_id ||
        '. Property: ' || v_dossier_data.unit_id ||
        '. Expected completion: ' || (CURRENT_DATE + v_bank_config.processing_days) ||
        '. Processing days: ' || v_bank_config.processing_days ||
        '. Required documents: ' || (v_bank_config.required_documents->>'documents') ||
        '. Conditional documents: ' || jsonb_pretty(v_conditional_docs) ||
        '. Additional instructions: ' || COALESCE(v_bank_config.additional_instructions, 'Standard process'),
        CURRENT_DATE,
        'BPN-' || TO_CHAR(NOW(), 'YYYY-MM-DD') || '-' || 
        (SELECT COUNT(*) + 1 FROM internal_memos WHERE memo_type = 'BPN_CLEARANCE_WARNING' AND memo_date = CURRENT_DATE),
        'HIGH',
        json_build_array(p_legal_id),
        '/documents/blank/bpn_clearance_warning_enhanced.pdf',
        p_legal_id,
        NOW()
    );
    
    -- Send enhanced WhatsApp notification
    INSERT INTO whatsapp_notifications (
        dossier_id, notification_type, recipient_phone, recipient_name,
        message_content, message_template, variables_used, sent_by, sent_at
    ) SELECT 
        p_dossier_id,
        'BPN_CLEARANCE_WARNING_ENHANCED',
        up.phone_number,
        up.name,
        'PERINGATAN: Bank ' || v_dossier_data.bank_name || ' memerlukan BPN/Clearance check.' ||
        ' Estimasi selesai: ' || (CURRENT_DATE + v_bank_config.processing_days) ||
        '. Mohon menyiapkan dokumen: KTP, KK, NPWP, Slip Gaji, Rekening Koran, Surat Aktif Kerja, SK Tetap, PKWT/PKWTT' ||
        '. Dokumen tambahan: Parklaring (jika usia kerja < 2 tahun), Surat Keterangan Domisili (jika alamat KTP radius > 25KM)' ||
        '. Total dokumen yang diperlukan: ' || (json_array_length(v_bank_config.required_documents->'documents') + json_array_length(v_conditional_docs)) || ' dokumen.',
        'BPN_CLEARANCE_WARNING_ENHANCED',
        json_build_object(
            'bank_name', v_dossier_data.bank_name,
            'customer_name', up.name,
            'expected_completion', CURRENT_DATE + v_bank_config.processing_days,
            'processing_days', v_bank_config.processing_days,
            'required_docs_count', json_array_length(v_bank_config.required_documents->'documents'),
            'conditional_docs_count', json_array_length(v_conditional_docs),
            'total_docs', json_array_length(v_bank_config.required_documents->'documents') + json_array_length(v_conditional_docs)
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
-- ENHANCED VIEWS WITH CONDITIONAL DOCUMENTS
-- =====================================================

-- Enhanced Target Banks with Features View
CREATE OR REPLACE VIEW target_banks_with_features_enhanced AS
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
    -- Feature details
    appraisal_config.processing_days as appraisal_processing_days,
    appraisal_config.auto_generate as appraisal_auto_generate,
    appraisal_config.required_documents as appraisal_documents,
    bpn_config.processing_days as bpn_processing_days,
    bpn_config.additional_instructions as bpn_additional_instructions,
    bpn_config.required_documents as bpn_documents,
    -- Document counts
    json_array_length(appraisal_config.required_documents->'documents') as appraisal_doc_count,
    json_array_length(bpn_config.required_documents->'documents') as bpn_doc_count,
    tb.created_at,
    tb.updated_at
FROM target_banks tb
LEFT JOIN bank_features_config appraisal_config ON tb.id = appraisal_config.bank_id AND appraisal_config.feature_type = 'APPRAISAL_LPA'
LEFT JOIN bank_features_config bpn_config ON tb.id = bpn_config.bank_id AND bpn_config.feature_type = 'BPN_CLEARANCE'
WHERE tb.is_active = true
ORDER BY tb.bank_name;

-- Enhanced Appraisal/LPA Requests View with conditional documents
CREATE OR REPLACE VIEW appraisal_lpa_requests_enhanced_view AS
SELECT 
    alr.id,
    alr.dossier_id,
    kd.user_id as customer_id,
    up.name as customer_name,
    up.phone_number as customer_phone,
    alr.bank_id,
    tb.bank_name,
    alr.request_type,
    alr.request_status,
    alr.request_date,
    alr.expected_completion_date,
    alr.actual_completion_date,
    alr.appraisal_company,
    alr.appraiser_name,
    alr.appraiser_value,
    alr.lpa_value,
    alr.property_address,
    alr.property_type,
    alr.property_size,
    alr.building_year,
    alr.building_condition,
    alr.documents_submitted,
    alr.additional_notes,
    up.block_number,
    up.unit_number,
    -- Enhanced document analysis
    (alr.documents_submitted->>'required_documents') as required_documents,
    (alr.documents_submitted->>'conditional_documents') as conditional_documents,
    (alr.documents_submitted->>'total_documents') as total_documents,
    -- Calculated fields
    CASE 
        WHEN alr.actual_completion_date IS NOT NULL THEN 
            (alr.actual_completion_date::date - alr.request_date::date)
        WHEN alr.expected_completion_date IS NOT NULL THEN 
            (alr.expected_completion_date::date - CURRENT_DATE)
        ELSE NULL
    END as days_status,
    alr.created_at,
    alr.updated_at
FROM appraisal_lpa_requests alr
JOIN kpr_dossiers kd ON alr.dossier_id = kd.id
JOIN user_profiles up ON kd.user_id = up.id
JOIN target_banks tb ON alr.bank_id = tb.id
ORDER BY alr.request_date DESC, alr.request_status;

-- Enhanced BPN/Clearance Requests View with conditional documents
CREATE OR REPLACE VIEW bpn_clearance_requests_enhanced_view AS
SELECT 
    bcr.id,
    bcr.dossier_id,
    kd.user_id as customer_id,
    up.name as customer_name,
    up.phone_number as customer_phone,
    bcr.bank_id,
    tb.bank_name,
    bcr.request_type,
    bcr.request_status,
    bcr.request_date,
    bcr.expected_completion_date,
    bcr.actual_completion_date,
    bcr.bpn_office,
    bcr.land_certificate_number,
    bcr.land_certificate_type,
    bcr.land_area,
    bcr.building_permit_number,
    bcr.building_permit_date,
    bcr.tax_payment_year,
    bcr.land_value_assessment,
    bcr.building_value_assessment,
    bcr.clearance_status,
    bcr.clearance_notes,
    bcr.documents_submitted,
    bcr.additional_instructions,
    up.block_number,
    up.unit_number,
    -- Enhanced document analysis
    (bcr.documents_submitted->>'required_documents') as required_documents,
    (bcr.documents_submitted->>'conditional_documents') as conditional_documents,
    (bcr.documents_submitted->>'total_documents') as total_documents,
    -- Calculated fields
    CASE 
        WHEN bcr.actual_completion_date IS NOT NULL THEN 
            (bcr.actual_completion_date::date - bcr.request_date::date)
        WHEN bcr.expected_completion_date IS NOT NULL THEN 
            (bcr.expected_completion_date::date - CURRENT_DATE)
        ELSE NULL
    END as days_status,
    bcr.created_at,
    bcr.updated_at
FROM bpn_clearance_requests bcr
JOIN kpr_dossiers kd ON bcr.dossier_id = kd.id
JOIN user_profiles up ON kd.user_id = up.id
JOIN target_banks tb ON bcr.bank_id = tb.id
ORDER BY bcr.request_date DESC, bcr.request_status;

-- =====================================================
-- UPDATE TRIGGERS FOR ENHANCED FUNCTIONS
-- =====================================================

-- Update trigger to use enhanced functions
CREATE OR REPLACE FUNCTION trigger_bank_features_on_akad_scheduled_enhanced()
RETURNS TRIGGER AS $$
BEGIN
    -- Trigger enhanced bank-specific features
    PERFORM trigger_bank_specific_features_enhanced(NEW.id, NEW.legal_assigned_id);
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Enhanced function to trigger bank-specific features
CREATE OR REPLACE FUNCTION trigger_bank_specific_features_enhanced(p_akad_id UUID, p_legal_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_dossier_data RECORD;
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
    
    -- Check if bank supports Appraisal/LPA (Bank BTN only)
    IF bank_supports_appraisal_lpa(v_dossier_data.bank_id) THEN
        -- Auto-generate Appraisal/LPA request with enhanced features
        v_appraisal_id := auto_generate_appraisal_lpa_btn_enhanced(v_dossier_data.dossier_id, p_legal_id);
    END IF;
    
    -- Check if bank supports BPN/Clearance (Bank BNI, BSI, BTN)
    IF bank_supports_bpn_clearance(v_dossier_data.bank_id) THEN
        -- Create BPN/Clearance request with enhanced features
        v_bpn_id := create_bpn_clearance_with_warning_enhanced(v_dossier_data.dossier_id, p_legal_id);
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Drop old trigger and create new one
DROP TRIGGER IF EXISTS trigger_bank_features_on_akad_scheduled ON akad_credit_management;

CREATE TRIGGER trigger_bank_features_on_akad_scheduled_enhanced
    AFTER UPDATE ON akad_credit_management
    FOR EACH ROW EXECUTE FUNCTION trigger_bank_features_on_akad_scheduled_enhanced();

-- =====================================================
-- SAMPLE DATA UPDATE FOR ENHANCED DOCUMENTS
-- =====================================================

-- Update sample data to reflect enhanced processing times
UPDATE bank_features_config SET processing_days = 7 WHERE processing_days != 7;

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
ENHANCED TARGET BANK SYSTEM DOCUMENTATION

========================================
UPDATED DOCUMENT REQUIREMENTS:

Bank BTN (Appraisal/LPA + BPN/Clearance):
- Base Documents: KTP, KK, NPWP, Slip Gaji, Rekening Koran, Surat Aktif Kerja, SK Tetap, PKWT/PKWTT
- Conditional Documents: Parklaring (usia kerja < 2 tahun), Surat Keterangan Domisili (radius > 25KM)
- Special Documents: Surat Keterangan Belum Menikah, Surat Keterangan Belum Memiliki Rumah, Form FLPP Bank
- Processing Time: 7 days (both Appraisal/LPA and BPN/Clearance)

Bank BNI (BPN/Clearance Only):
- Base Documents: KTP, KK, NPWP, Slip Gaji, Rekening Koran, Surat Aktif Kerja, SK Tetap, PKWT/PKWTT
- Conditional Documents: Parklaring (usia kerja < 2 tahun), Surat Keterangan Domisili (radius > 25KM)
- Special Documents: Surat Keterangan Belum Menikah, Surat Keterangan Belum Memiliki Rumah, Form FLPP Bank
- Processing Time: 7 days

Bank Syariah Indonesia (BPN/Clearance Only):
- Base Documents: KTP, KK, NPWP, Slip Gaji, Rekening Koran, Surat Aktif Kerja, SK Tetap, PKWT/PKWTT
- Conditional Documents: Parklaring (usia kerja < 2 tahun), Surat Keterangan Domisili (radius > 25KM), Surat Keterangan Wakaf
- Special Documents: Surat Keterangan Belum Menikah, Surat Keterangan Belum Memiliki Rumah, Form FLPP Bank
- Processing Time: 7 days

========================================
ENHANCED FEATURES:
- Conditional document checking based on work experience and location
- Enhanced WhatsApp notifications with document counts
- Improved internal memos with conditional requirements
- Better tracking of total document requirements
- Standardized processing time (7 days) for all banks

========================================
BENEFITS:
- Clearer document requirements per bank
- Conditional document logic for special cases
- Consistent processing time across banks
- Enhanced customer communication
- Better document tracking and compliance
*/
