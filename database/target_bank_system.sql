-- Target Bank & Appraisal/LPA System Implementation
-- Bank-specific features for Appraisal/LPA and BPN/Clearance
-- KPRFlow Enterprise - Enhanced Bank Management

-- =====================================================
-- TARGET BANK MASTER DATA
-- =====================================================

-- Target Banks Table
CREATE TABLE target_banks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bank_code VARCHAR(10) NOT NULL UNIQUE,
    bank_name VARCHAR(100) NOT NULL,
    bank_type VARCHAR(20) NOT NULL, -- CONVENTIONAL, SYARIAH
    bank_category VARCHAR(20) NOT NULL, -- STATE, PRIVATE, MIXED
    supports_appraisal_lpa BOOLEAN DEFAULT false,
    supports_bpn_clearance BOOLEAN DEFAULT false,
    appraisal_lpa_template_url TEXT,
    bpn_clearance_template_url TEXT,
    bank_contact_info JSONB, -- Contact person, phone, email
    bank_requirements JSONB, -- Specific requirements per bank
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Bank-Specific Features Configuration
CREATE TABLE bank_features_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bank_id UUID NOT NULL REFERENCES target_banks(id) ON DELETE CASCADE,
    feature_type VARCHAR(50) NOT NULL, -- APPRAISAL_LPA, BPN_CLEARANCE
    is_enabled BOOLEAN DEFAULT false,
    auto_generate BOOLEAN DEFAULT false,
    template_required BOOLEAN DEFAULT true,
    additional_instructions TEXT,
    processing_days INTEGER DEFAULT 0,
    required_documents JSONB, -- Bank-specific required documents
    notification_settings JSONB, -- Notification preferences
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- INSERT TARGET BANKS WITH SPECIFIC CONFIGURATIONS
-- =====================================================

-- Insert Target Banks
INSERT INTO target_banks (bank_code, bank_name, bank_type, bank_category, supports_appraisal_lpa, supports_bpn_clearance) VALUES
('BRI', 'Bank BRI', 'CONVENTIONAL', 'STATE', false, false),
('MANDIRI', 'Bank Mandiri', 'CONVENTIONAL', 'STATE', false, false),
('BTN', 'Bank BTN', 'CONVENTIONAL', 'STATE', true, true),
('BJB', 'Bank BJB', 'CONVENTIONAL', 'STATE', false, false),
('BSI', 'Bank Syariah Indonesia', 'SYARIAH', 'STATE', false, true),
('BSN', 'Bank Syariah Nasional', 'SYARIAH', 'STATE', false, false),
('BCA', 'Bank BCA', 'CONVENTIONAL', 'PRIVATE', false, false),
('NOBU', 'Bank NOBU', 'CONVENTIONAL', 'PRIVATE', false, false),
('BNI', 'Bank BNI', 'CONVENTIONAL', 'STATE', false, true);

-- Configure Bank-Specific Features
-- Bank BTN - Supports both Appraisal/LPA and BPN/Clearance
INSERT INTO bank_features_config (bank_id, feature_type, is_enabled, auto_generate, template_required, processing_days, required_documents) VALUES
((SELECT id FROM target_banks WHERE bank_code = 'BTN'), 'APPRAISAL_LPA', true, true, true, 7, 
 json_build_object(
     'documents', json_build_array(
         'KTP', 'KK', 'NPWP', 'SLIP_GAJI', 'REKENING_KORAN', 'SURAT_KERJA', 'FOTO_RUMAH', 'FOTO_LINGKUNGAN'
     ),
     'special_requirements', json_build_array(
         'Foto rumah dari 4 sisi',
         'Foto lingkungan sekitar',
         'Denah lokasi',
         'Surat kepemilikan tanah'
     )
 )),
((SELECT id FROM target_banks WHERE bank_code = 'BTN'), 'BPN_CLEARANCE', true, false, true, 14,
 json_build_object(
     'documents', json_build_array(
         'SHGB', 'IMB', 'PBB', 'SURAT_KETERANGAN_LURAH', 'FOTO_BANGUNAN'
     ),
     'special_requirements', json_build_array(
         'Cek fisik bangunan',
         'Verifikasi kepemilikan tanah',
         'Cek status legalitas'
     )
 ));

-- Bank BNI - Supports BPN/Clearance only
INSERT INTO bank_features_config (bank_id, feature_type, is_enabled, auto_generate, template_required, processing_days, required_documents) VALUES
((SELECT id FROM target_banks WHERE bank_code = 'BNI'), 'BPN_CLEARANCE', true, false, true, 10,
 json_build_object(
     'documents', json_build_array(
         'SHGB', 'IMB', 'PBB', 'SURAT_KETERANGAN_LURAH', 'FOTO_BANGUNAN', 'SURAT_KETERANGAN_TANAH'
     ),
     'special_requirements', json_build_array(
         'Verifikasi status tanah di BPN',
         'Cek legalitas bangunan',
         'Surat keterangan tidak sengketa'
     )
 ));

-- Bank Syariah Indonesia - Supports BPN/Clearance only
INSERT INTO bank_features_config (bank_id, feature_type, is_enabled, auto_generate, template_required, processing_days, required_documents) VALUES
((SELECT id FROM target_banks WHERE bank_code = 'BSI'), 'BPN_CLEARANCE', true, false, true, 12,
 json_build_object(
     'documents', json_build_array(
         'SHGB', 'IMB', 'PBB', 'SURAT_KETERANGAN_LURAH', 'FOTO_BANGUNAN', 'SURAT_KETERANGAN_WAKAF'
     ),
     'special_requirements', json_build_array(
         'Cek status wakaf (jika ada)',
         'Verifikasi kepemilikan sesuai syariah',
         'Cek status legalitas tanah'
     )
 ));

-- =====================================================
-- APPRAISAL/LPA MANAGEMENT TABLES
-- =====================================================

-- Appraisal/LPA Requests Table
CREATE TABLE appraisal_lpa_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    bank_id UUID NOT NULL REFERENCES target_banks(id),
    request_type VARCHAR(20) NOT NULL, -- APPRAISAL, LPA
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, CANCELLED
    request_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expected_completion_date DATE,
    actual_completion_date DATE,
    appraisal_company VARCHAR(255),
    appraiser_name VARCHAR(255),
    appraiser_license VARCHAR(50),
    appraisal_value DECIMAL(15,2),
    lpa_value DECIMAL(15,2),
    property_address TEXT,
    property_description TEXT,
    property_type VARCHAR(50), -- RUMAH, RUKO, APARTEMEN, TANAH
    property_size DECIMAL(10,2), -- Luas tanah/bangunan dalam m2
    building_year INTEGER,
    building_condition VARCHAR(20), -- BAGUS, SEDANG, KURANG, RUSAK
    documents_submitted JSONB,
    additional_notes TEXT,
    created_by UUID REFERENCES user_profiles(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Appraisal/LPA Documents Table
CREATE TABLE appraisal_lpa_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id UUID NOT NULL REFERENCES appraisal_lpa_requests(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_url TEXT NOT NULL,
    document_size DECIMAL(10,2), -- Size in MB
    upload_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    uploaded_by UUID REFERENCES user_profiles(id),
    is_verified BOOLEAN DEFAULT false,
    verification_date TIMESTAMP WITH TIME ZONE,
    verified_by UUID REFERENCES user_profiles(id),
    verification_notes TEXT
);

-- =====================================================
-- BPN/CLEARANCE MANAGEMENT TABLES
-- =====================================================

-- BPN/Clearance Requests Table
CREATE TABLE bpn_clearance_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    bank_id UUID NOT NULL REFERENCES target_banks(id),
    request_type VARCHAR(20) NOT NULL, -- BPN_CEK, CLEARANCE
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, CANCELLED
    request_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expected_completion_date DATE,
    actual_completion_date DATE,
    bpn_office VARCHAR(255),
    land_certificate_number VARCHAR(100),
    land_certificate_type VARCHAR(20), -- SHGB, SHM, AJB
    land_area DECIMAL(10,2), -- Luas tanah dalam m2
    building_permit_number VARCHAR(50),
    building_permit_date DATE,
    tax_payment_year INTEGER,
    land_value_assessment DECIMAL(15,2),
    building_value_assessment DECIMAL(15,2),
    clearance_status VARCHAR(20), -- CLEAR, NOT_CLEAR, UNDER_REVIEW
    clearance_notes TEXT,
    documents_submitted JSONB,
    additional_instructions TEXT,
    created_by UUID REFERENCES user_profiles(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- BPN/Clearance Documents Table
CREATE TABLE bpn_clearance_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id UUID NOT NULL REFERENCES bpn_clearance_requests(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_url TEXT NOT NULL,
    document_size DECIMAL(10,2), -- Size in MB
    upload_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    uploaded_by UUID REFERENCES user_profiles(id),
    is_verified BOOLEAN DEFAULT false,
    verification_date TIMESTAMP WITH TIME ZONE,
    verified_by UUID REFERENCES user_profiles(id),
    verification_notes TEXT
);

-- =====================================================
-- BANK-SPECIFIC WORKFLOW FUNCTIONS
-- =====================================================

-- Function to check if bank supports Appraisal/LPA
CREATE OR REPLACE FUNCTION bank_supports_appraisal_lpa(p_bank_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS(
        SELECT 1 FROM bank_features_config 
        WHERE bank_id = p_bank_id 
        AND feature_type = 'APPRAISAL_LPA' 
        AND is_enabled = true
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to check if bank supports BPN/Clearance
CREATE OR REPLACE FUNCTION bank_supports_bpn_clearance(p_bank_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS(
        SELECT 1 FROM bank_features_config 
        WHERE bank_id = p_bank_id 
        AND feature_type = 'BPN_CLEARANCE' 
        AND is_enabled = true
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to auto-generate Appraisal/LPA request for Bank BTN
CREATE OR REPLACE FUNCTION auto_generate_appraisal_lpa_btn(p_dossier_id UUID, p_legal_id UUID)
RETURNS UUID AS $$
DECLARE
    v_request_id UUID;
    v_dossier_data RECORD;
    v_bank_config RECORD;
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
        v_bank_config.required_documents,
        p_legal_id
    ) RETURNING id INTO v_request_id;
    
    -- Generate internal memo for Appraisal/LPA
    INSERT INTO internal_memos (
        dossier_id, memo_type, memo_title, memo_content, memo_date, memo_number,
        priority, recipients, document_url, created_by, created_at
    ) VALUES (
        p_dossier_id,
        'APPRAISAL_REQUEST',
        'Auto-Generated Appraisal Request - ' || v_dossier_data.bank_name,
        'Appraisal request otomatis untuk Bank ' || v_dossier_data.bank_name || 
        '. Customer: ' || v_dossier_data.user_id || 
        '. Property: ' || v_dossier_data.unit_id ||
        '. Expected completion: ' || (CURRENT_DATE + v_bank_config.processing_days) ||
        '. Processing days: ' || v_bank_config.processing_days,
        CURRENT_DATE,
        'APPRAISAL-' || TO_CHAR(NOW(), 'YYYY-MM-DD') || '-' || 
        (SELECT COUNT(*) + 1 FROM internal_memos WHERE memo_type = 'APPRAISAL_REQUEST' AND memo_date = CURRENT_DATE),
        'HIGH',
        json_build_array(p_legal_id),
        '/documents/blank/appraisal_request_btn.pdf', -- Blank PDF template
        p_legal_id,
        NOW()
    );
    
    -- Send WhatsApp notification
    INSERT INTO whatsapp_notifications (
        dossier_id, notification_type, recipient_phone, recipient_name,
        message_content, message_template, variables_used, sent_by, sent_at
    ) SELECT 
        p_dossier_id,
        'APPRAISAL_REQUEST_AUTO',
        up.phone_number,
        up.name,
        'Appraisal request otomatis telah dibuat untuk Bank ' || v_dossier_data.bank_name ||
        '. Estimasi selesai: ' || (CURRENT_DATE + v_bank_config.processing_days) ||
        '. Mohon menyiapkan dokumen yang diperlukan.',
        'APPRAISAL_AUTO_GENERATED',
        json_build_object(
            'bank_name', v_dossier_data.bank_name,
            'customer_name', up.name,
            'expected_completion', CURRENT_DATE + v_bank_config.processing_days,
            'processing_days', v_bank_config.processing_days
        ),
        p_legal_id,
        NOW()
    FROM user_profiles up
    JOIN kpr_dossiers kd ON up.id = kd.user_id
    WHERE kd.id = p_dossier_id;
    
    RETURN v_request_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to create BPN/Clearance request with warning
CREATE OR REPLACE FUNCTION create_bpn_clearance_with_warning(p_dossier_id UUID, p_legal_id UUID)
RETURNS UUID AS $$
DECLARE
    v_request_id UUID;
    v_dossier_data RECORD;
    v_bank_config RECORD;
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
        v_bank_config.required_documents,
        'Additional instructions for Bank ' || v_dossier_data.bank_name || ': ' || 
        COALESCE(v_bank_config.additional_instructions, 'Standard BPN clearance process'),
        p_legal_id
    ) RETURNING id INTO v_request_id;
    
    -- Generate warning memo for BPN/Clearance
    INSERT INTO internal_memos (
        dossier_id, memo_type, memo_title, memo_content, memo_date, memo_number,
        priority, recipients, document_url, created_by, created_at
    ) VALUES (
        p_dossier_id,
        'BPN_CLEARANCE_WARNING',
        'WARNING: BPN/Clearance Required - ' || v_dossier_data.bank_name,
        'PERINGATAN: Bank ' || v_dossier_data.bank_name || ' memerlukan BPN/Clearance check.' ||
        ' Customer: ' || v_dossier_data.user_id ||
        '. Property: ' || v_dossier_data.unit_id ||
        '. Expected completion: ' || (CURRENT_DATE + v_bank_config.processing_days) ||
        '. Processing days: ' || v_bank_config.processing_days ||
        '. Additional instructions: ' || COALESCE(v_bank_config.additional_instructions, 'Standard process'),
        CURRENT_DATE,
        'BPN-' || TO_CHAR(NOW(), 'YYYY-MM-DD') || '-' || 
        (SELECT COUNT(*) + 1 FROM internal_memos WHERE memo_type = 'BPN_CLEARANCE_WARNING' AND memo_date = CURRENT_DATE),
        'HIGH',
        json_build_array(p_legal_id),
        '/documents/blank/bpn_clearance_warning.pdf', -- Blank PDF template
        p_legal_id,
        NOW()
    );
    
    -- Send WhatsApp notification with warning
    INSERT INTO whatsapp_notifications (
        dossier_id, notification_type, recipient_phone, recipient_name,
        message_content, message_template, variables_used, sent_by, sent_at
    ) SELECT 
        p_dossier_id,
        'BPN_CLEARANCE_WARNING',
        up.phone_number,
        up.name,
        'PERINGATAN: Bank ' || v_dossier_data.bank_name || ' memerlukan BPN/Clearance check.' ||
        ' Estimasi selesai: ' || (CURRENT_DATE + v_bank_config.processing_days) ||
        '. Mohon menyiapkan dokumen yang diperlukan segera.',
        'BPN_CLEARANCE_WARNING',
        json_build_object(
            'bank_name', v_dossier_data.bank_name,
            'customer_name', up.name,
            'expected_completion', CURRENT_DATE + v_bank_config.processing_days,
            'processing_days', v_bank_config.processing_days,
            'additional_instructions', COALESCE(v_bank_config.additional_instructions, 'Standard process')
        ),
        p_legal_id,
        NOW()
    FROM user_profiles up
    JOIN kpr_dossiers kd ON up.id = kd.user_id
    WHERE kd.id = p_dossier_id;
    
    RETURN v_request_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to trigger bank-specific features when Akad is scheduled
CREATE OR REPLACE FUNCTION trigger_bank_specific_features(p_akad_id UUID, p_legal_id UUID)
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
        -- Auto-generate Appraisal/LPA request
        v_appraisal_id := auto_generate_appraisal_lpa_btn(v_dossier_data.dossier_id, p_legal_id);
    END IF;
    
    -- Check if bank supports BPN/Clearance (Bank BNI, BSI, BTN)
    IF bank_supports_bpn_clearance(v_dossier_data.bank_id) THEN
        -- Create BPN/Clearance request with warning
        v_bpn_id := create_bpn_clearance_with_warning(v_dossier_data.dossier_id, p_legal_id);
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- VIEWS FOR BANK-SPECIFIC FEATURES
-- =====================================================

-- Target Banks with Features View
CREATE VIEW target_banks_with_features AS
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
    bpn_config.processing_days as bpn_processing_days,
    bpn_config.additional_instructions as bpn_additional_instructions,
    tb.created_at,
    tb.updated_at
FROM target_banks tb
LEFT JOIN bank_features_config appraisal_config ON tb.id = appraisal_config.bank_id AND appraisal_config.feature_type = 'APPRAISAL_LPA'
LEFT JOIN bank_features_config bpn_config ON tb.id = bpn_config.bank_id AND bpn_config.feature_type = 'BPN_CLEARANCE'
WHERE tb.is_active = true
ORDER BY tb.bank_name;

-- Appraisal/LPA Requests View
CREATE VIEW appraisal_lpa_requests_view AS
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
    alr.appraisal_value,
    alr.lpa_value,
    alr.property_address,
    alr.property_type,
    alr.property_size,
    alr.building_year,
    alr.building_condition,
    alr.documents_submitted,
    alr.additional_notes,
    alr.created_at,
    alr.updated_at,
    -- Calculated fields
    CASE 
        WHEN alr.actual_completion_date IS NOT NULL THEN 
            (alr.actual_completion_date::date - alr.request_date::date)
        WHEN alr.expected_completion_date IS NOT NULL THEN 
            (alr.expected_completion_date::date - CURRENT_DATE)
        ELSE NULL
    END as days_status
FROM appraisal_lpa_requests alr
JOIN kpr_dossiers kd ON alr.dossier_id = kd.id
JOIN user_profiles up ON kd.user_id = up.id
JOIN target_banks tb ON alr.bank_id = tb.id
ORDER BY alr.request_date DESC, alr.request_status;

-- BPN/Clearance Requests View
CREATE VIEW bpn_clearance_requests_view AS
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
    bcr.created_at,
    bcr.updated_at,
    -- Calculated fields
    CASE 
        WHEN bcr.actual_completion_date IS NOT NULL THEN 
            (bcr.actual_completion_date::date - bcr.request_date::date)
        WHEN bcr.expected_completion_date IS NOT NULL THEN 
            (bcr.expected_completion_date::date - CURRENT_DATE)
        ELSE NULL
    END as days_status
FROM bpn_clearance_requests bcr
JOIN kpr_dossiers kd ON bcr.dossier_id = kd.id
JOIN user_profiles up ON kd.user_id = up.id
JOIN target_banks tb ON bcr.bank_id = tb.id
ORDER BY bcr.request_date DESC, bcr.request_status;

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Target Banks Indexes
CREATE INDEX idx_target_banks_code ON target_banks(bank_code);
CREATE INDEX idx_target_banks_name ON target_banks(bank_name);
CREATE INDEX idx_target_banks_active ON target_banks(is_active);

-- Bank Features Config Indexes
CREATE INDEX idx_bank_features_bank_id ON bank_features_config(bank_id);
CREATE INDEX idx_bank_features_type ON bank_features_config(feature_type);
CREATE INDEX idx_bank_features_enabled ON bank_features_config(is_enabled);

-- Appraisal/LPA Indexes
CREATE INDEX idx_appraisal_dossier_id ON appraisal_lpa_requests(dossier_id);
CREATE INDEX idx_appraisal_bank_id ON appraisal_lpa_requests(bank_id);
CREATE INDEX idx_appraisal_status ON appraisal_lpa_requests(request_status);
CREATE INDEX idx_appraisal_date ON appraisal_lpa_requests(request_date);

-- BPN/Clearance Indexes
CREATE INDEX idx_bpn_dossier_id ON bpn_clearance_requests(dossier_id);
CREATE INDEX idx_bpn_bank_id ON bpn_clearance_requests(bank_id);
CREATE INDEX idx_bpn_status ON bpn_clearance_requests(request_status);
CREATE INDEX idx_bpn_date ON bpn_clearance_requests(request_date);

-- Document Indexes
CREATE INDEX idx_appraisal_docs_request_id ON appraisal_lpa_documents(request_id);
CREATE INDEX idx_bpn_docs_request_id ON bpn_clearance_documents(request_id);

-- =====================================================
-- RLS POLICIES
-- =====================================================

-- Enable RLS on all new tables
ALTER TABLE target_banks ENABLE ROW LEVEL SECURITY;
ALTER TABLE bank_features_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE appraisal_lpa_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE appraisal_lpa_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE bpn_clearance_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE bpn_clearance_documents ENABLE ROW LEVEL SECURITY;

-- Target Banks RLS Policies
CREATE POLICY "All can view active target banks" ON target_banks
    FOR SELECT USING (is_active = true);

CREATE POLICY "Legal can manage target banks" ON target_banks
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

-- Bank Features Config RLS Policies
CREATE POLICY "Legal can manage bank features" ON bank_features_config
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

CREATE POLICY "All can view bank features" ON bank_features_config
    FOR SELECT USING (true);

-- Appraisal/LPA Requests RLS Policies
CREATE POLICY "Legal can manage appraisal requests" ON appraisal_lpa_requests
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

CREATE POLICY "Finance can view appraisal requests" ON appraisal_lpa_requests
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('FINANCE', 'LEGAL', 'BOD'));

CREATE POLICY "Marketing can view appraisal requests" ON appraisal_lpa_requests
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('MARKETING', 'LEGAL', 'BOD'));

-- BPN/Clearance Requests RLS Policies
CREATE POLICY "Legal can manage bpn requests" ON bpn_clearance_requests
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

CREATE POLICY "Finance can view bpn requests" ON bpn_clearance_requests
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('FINANCE', 'LEGAL', 'BOD'));

CREATE POLICY "Marketing can view bpn requests" ON bpn_clearance_requests
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('MARKETING', 'LEGAL', 'BOD'));

-- Document Tables RLS Policies
CREATE POLICY "Legal can manage appraisal documents" ON appraisal_lpa_documents
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

CREATE POLICY "Legal can manage bpn documents" ON bpn_clearance_documents
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

-- =====================================================
-- TRIGGERS FOR AUTOMATIC WORKFLOW
-- =====================================================

-- Trigger bank-specific features when Akad is scheduled
CREATE OR REPLACE FUNCTION trigger_bank_features_on_akad_scheduled()
RETURNS TRIGGER AS $$
BEGIN
    -- Trigger bank-specific features
    PERFORM trigger_bank_specific_features(NEW.id, NEW.legal_assigned_id);
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_bank_features_on_akad_scheduled
    AFTER UPDATE ON akad_credit_management
    FOR EACH ROW EXECUTE FUNCTION trigger_bank_features_on_akad_scheduled();

-- Update timestamp triggers
CREATE TRIGGER trigger_appraisal_updated_at
    BEFORE UPDATE ON appraisal_lpa_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_bpn_updated_at
    BEFORE UPDATE ON bpn_clearance_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- SAMPLE DATA FOR TESTING
-- =====================================================

-- Update bank contact info
UPDATE target_banks SET 
    bank_contact_info = json_build_object(
        'contact_person', 'Bank BTN Contact',
        'phone', '021-5551234',
        'email', 'btn@bankbtn.com',
        'address', 'Jakarta'
    )
WHERE bank_code = 'BTN';

UPDATE target_banks SET 
    bank_contact_info = json_build_object(
        'contact_person', 'Bank BNI Contact',
        'phone', '021-6665678',
        'email', 'bni@bankbni.com',
        'address', 'Jakarta'
    )
WHERE bank_code = 'BNI';

UPDATE target_banks SET 
    bank_contact_info = json_build_object(
        'contact_person', 'Bank Syariah Indonesia Contact',
        'phone', '021-7779012',
        'email', 'bsi@bankbsi.com',
        'address', 'Jakarta'
    )
WHERE bank_code = 'BSI';

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
TARGET BANK SYSTEM DOCUMENTATION

========================================
SUPPORTED BANKS:
1. Bank BRI - No special features
2. Bank Mandiri - No special features  
3. Bank BTN - Appraisal/LPA (Auto-generate) + BPN/Clearance (Warning)
4. Bank BJB - No special features
5. Bank Syariah Indonesia - BPN/Clearance (Warning)
6. Bank Syariah Nasional - No special features
7. Bank BCA - No special features
8. Bank NOBU - No special features
9. Bank BNI - BPN/Clearance (Warning)

========================================
BANK-SPECIFIC FEATURES:
- Bank BTN: Auto-generate Appraisal/LPA requests + BPN/Clearance warnings
- Bank BNI: BPN/Clearance warnings only
- Bank Syariah Indonesia: BPN/Clearance warnings only

========================================
WORKFLOW TRIGGERS:
- Akad Credit scheduled → Check bank features
- Bank BTN → Auto-generate Appraisal/LPA + BPN/Clearance warning
- Bank BNI → BPN/Clearance warning only
- Bank BSI → BPN/Clearance warning only

========================================
DOCUMENT REQUIREMENTS:
- Appraisal/LPA: KTP, KK, NPWP, Slip Gaji, Rekening Koran, Surat Kerja, Foto Rumah, Foto Lingkungan
- BPN/Clearance: SHGB, IMB, PBB, Surat Keterangan Lurah, Foto Bangunan

========================================
BENEFITS:
- Bank-specific workflow automation
- Proper document management
- Clear process tracking
- Warning system for BPN/Clearance
- Auto-generation for Appraisal/LPA (Bank BTN)
*/
