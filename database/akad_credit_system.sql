-- Pra-Akad & Akad Credit System Implementation
-- Integrated Legal and Finance Workflow for Credit Agreement Process
-- KPRFlow Enterprise - Enhanced KPR Status Management

-- =====================================================
-- UPDATE KPR STATUS ENUM FOR NEW STAGES
-- =====================================================

-- First, update the existing kpr_status enum to include new stages
-- Note: In PostgreSQL, we need to drop and recreate the enum
DROP TYPE IF EXISTS kpr_status CASCADE;

CREATE TYPE kpr_status AS ENUM (
    'LEAD', 'PEMBERKASAN', 'PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 
    'SP3K_TERBIT', 'PRA_AKAD', 'PRA_AKAD_SCHEDULED', 'AKAD_SCHEDULED', 'AKAD_IN_PROGRESS',
    'AKAD_COMPLETED', 'PRA_AKAD', 'FUNDS_DISBURSED', 'BAST_READY', 'BAST_COMPLETED', 
    'FLOATING_DOSSIER', 'CANCELLED_BY_SYSTEM'
);

-- =====================================================
-- NEW TABLES FOR PRA-AKAD & AKAD SYSTEM
-- =====================================================

-- Pra-Akad Management Table
CREATE TABLE pra_akad_management (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    legal_assigned_id UUID REFERENCES user_profiles(id), -- Legal officer assigned
    finance_notified_id UUID REFERENCES user_profiles(id), -- Finance officer notified
    pra_akad_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, SCHEDULED, COMPLETED, CANCELLED
    pra_akad_date DATE, -- Planned pra-akad date
    pra_akad_time TIME, -- Planned pra-akad time
    pra_akad_location VARCHAR(255), -- Location for pra-akad
    pra_akad_notes TEXT,
    documents_required JSONB, -- List of required documents
    documents_completed JSONB, -- List of completed documents
    si_surat_keterangan_lunas_status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PREPARING, READY
    si_surat_keterangan_lunas_url TEXT, -- URL to SI document
    finance_warning_sent BOOLEAN DEFAULT FALSE,
    finance_warning_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    updated_by UUID REFERENCES user_profiles(id)
);

-- Akad Credit Management Table
CREATE TABLE akad_credit_management (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    pra_akad_id UUID REFERENCES pra_akad_management(id) ON DELETE CASCADE,
    legal_assigned_id UUID REFERENCES user_profiles(id), -- Legal officer for akad
    notaris_id UUID NOT NULL, -- Notaris assigned
    notaris_name VARCHAR(255) NOT NULL,
    notaris_contact VARCHAR(50),
    notaris_address TEXT,
    akad_status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    akad_date DATE NOT NULL, -- Final akad date (set by Legal only)
    akad_time TIME NOT NULL, -- Final akad time (set by Legal only)
    akad_location VARCHAR(255) NOT NULL,
    akad_notes TEXT,
    akad_documents_ready BOOLEAN DEFAULT FALSE,
    customer_notified BOOLEAN DEFAULT FALSE,
    customer_notification_date TIMESTAMP WITH TIME ZONE,
    whatsapp_notification_sent BOOLEAN DEFAULT FALSE,
    whatsapp_notification_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    updated_by UUID REFERENCES user_profiles(id)
);

-- Document Templates for Akad System
CREATE TABLE akad_document_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_name VARCHAR(255) NOT NULL,
    template_type VARCHAR(50) NOT NULL, -- INVITATION, MEMO_INTERNAL, MEMO_APPRAISAL, SO_LEGAL, SI_LUNAS
    template_description TEXT,
    template_url TEXT, -- URL to blank PDF template
    template_variables JSONB, -- Variables that can be replaced
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Generated Documents Table
CREATE TABLE akad_generated_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    pra_akad_id UUID REFERENCES pra_akad_management(id) ON DELETE CASCADE,
    akad_id UUID REFERENCES akad_credit_management(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL, -- INVITATION, MEMO_INTERNAL, MEMO_APPRAISAL, SO_LEGAL, SI_LUNAS
    template_id UUID REFERENCES akad_document_templates(id),
    document_name VARCHAR(255) NOT NULL,
    document_url TEXT NOT NULL,
    document_variables JSONB, -- Actual values used for template
    generated_by UUID REFERENCES user_profiles(id),
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_final BOOLEAN DEFAULT FALSE
);

-- WhatsApp Notifications Log
CREATE TABLE whatsapp_notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL, -- PRA_AKAD_INVITATION, AKAD_INVITATION, REMINDER
    recipient_phone VARCHAR(20) NOT NULL,
    recipient_name VARCHAR(255),
    message_content TEXT NOT NULL,
    message_template TEXT,
    variables_used JSONB,
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    sent_by UUID REFERENCES user_profiles(id),
    delivery_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, DELIVERED, FAILED, READ
    delivery_report JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Internal Memos Table
CREATE TABLE internal_memos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    memo_type VARCHAR(50) NOT NULL, -- INTERNAL, APPRAISAL_REQUEST
    memo_title VARCHAR(255) NOT NULL,
    memo_content TEXT NOT NULL,
    memo_date DATE NOT NULL,
    memo_number VARCHAR(100), -- Auto-generated memo number
    priority VARCHAR(20) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT
    recipients JSONB, -- Array of user IDs who should receive this memo
    document_url TEXT, -- URL to generated memo PDF
    created_by UUID REFERENCES user_profiles(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Notaris Management Table
CREATE TABLE notaris_management (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    notaris_name VARCHAR(255) NOT NULL,
    notaris_office_name VARCHAR(255),
    notaris_address TEXT,
    notaris_phone VARCHAR(20),
    notaris_email VARCHAR(255),
    notaris_sk_number VARCHAR(100), -- Surat Keterangan Notaris
    notaris_region VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- UPDATE DOCUMENTS TYPE ENUM TO INCLUDE KTP PASANGAN
-- =====================================================

-- Drop and recreate document_type enum
DROP TYPE IF EXISTS document_type CASCADE;

CREATE TYPE document_type AS ENUM (
    'KTP', 'KK', 'NPWP', 'MARRIAGE_CERTIFICATE', 'PAYSLIP', 'BANK_STATEMENT', 
    'WORKPLACE_PHOTO', 'SPR_FORM', 'KTP_PASANGAN', 'SP3K', 'SHGB', 'PBG_IMB'
);

-- =====================================================
-- VIEWS FOR PRA-AKAD & AKAD SYSTEM
-- =====================================================

-- Pra-Akad Comprehensive View
CREATE VIEW pra_akad_comprehensive AS
SELECT 
    pam.id,
    pam.dossier_id,
    kd.user_id as customer_id,
    up.name as customer_name,
    up.phone_number as customer_phone,
    up.email as customer_email,
    kd.unit_id,
    uproperty.block_number,
    uproperty.unit_number,
    kd.kpr_amount,
    kd.dp_amount,
    kd.bank_name,
    pam.pra_akad_status,
    pam.pra_akad_date,
    pam.pra_akad_time,
    pam.pra_akad_location,
    pam.pra_akad_notes,
    pam.documents_required,
    pam.documents_completed,
    pam.si_surat_keterangan_lunas_status,
    pam.si_surat_keterangan_lunas_url,
    pam.finance_warning_sent,
    pam.finance_warning_date,
    legal.name as legal_assigned_name,
    legal.phone_number as legal_phone,
    finance.name as finance_notified_name,
    pam.created_at,
    pam.updated_at
FROM pra_akad_management pam
JOIN kpr_dossiers kd ON pam.dossier_id = kd.id
JOIN user_profiles up ON kd.user_id = up.id
LEFT JOIN unit_properties uproperty ON kd.unit_id = uproperty.id
LEFT JOIN user_profiles legal ON pam.legal_assigned_id = legal.id
LEFT JOIN user_profiles finance ON pam.finance_notified_id = finance.id
ORDER BY pam.created_at DESC;

-- Akad Credit Comprehensive View
CREATE VIEW akad_credit_comprehensive AS
SELECT 
    acm.id,
    acm.dossier_id,
    kd.user_id as customer_id,
    up.name as customer_name,
    up.phone_number as customer_phone,
    up.email as customer_email,
    kd.unit_id,
    uproperty.block_number,
    uproperty.unit_number,
    kd.kpr_amount,
    kd.dp_amount,
    kd.bank_name,
    acm.akad_status,
    acm.akad_date,
    acm.akad_time,
    acm.akad_location,
    acm.akad_notes,
    acm.akad_documents_ready,
    acm.customer_notified,
    acm.customer_notification_date,
    acm.whatsapp_notification_sent,
    acm.whatsapp_notification_date,
    legal.name as legal_assigned_name,
    legal.phone_number as legal_phone,
    acm.notaris_id,
    acm.notaris_name,
    acm.notaris_contact,
    acm.notaris_address,
    pam.pra_akad_date as pra_akad_date,
    pam.pra_akad_status as pra_akad_status,
    acm.created_at,
    acm.updated_at
FROM akad_credit_management acm
JOIN kpr_dossiers kd ON acm.dossier_id = kd.id
JOIN user_profiles up ON kd.user_id = up.id
LEFT JOIN unit_properties uproperty ON kd.unit_id = uproperty.id
LEFT JOIN user_profiles legal ON acm.legal_assigned_id = legal.id
LEFT JOIN pra_akad_management pam ON acm.pra_akad_id = pam.id
ORDER BY acm.created_at DESC;

-- Documents Integration View for SO Legal
CREATE VIEW so_legal_documents_view AS
SELECT 
    kd.id as dossier_id,
    up.name as customer_name,
    up.nik as customer_nik,
    up.phone_number as customer_phone,
    up.email as customer_email,
    kd.kpr_amount,
    kd.dp_amount,
    kd.bank_name,
    uproperty.block_number,
    uproperty.unit_number,
    uproperty.type as unit_type,
    -- Documents URLs
    ktp_doc.url as ktp_url,
    kk_doc.url as kk_url,
    npwp_doc.url as npwp_url,
    ktp_pasangan_doc.url as ktp_pasangan_url,
    sp3k_doc.url as sp3k_url,
    shgb_doc.url as shgb_url,
    pbg_imb_doc.url as pbg_imb_url,
    marriage_doc.url as marriage_certificate_url,
    -- Document verification status
    ktp_doc.is_verified as ktp_verified,
    kk_doc.is_verified as kk_verified,
    npwp_doc.is_verified as npwp_verified,
    ktp_pasangan_doc.is_verified as ktp_pasangan_verified,
    sp3k_doc.is_verified as sp3k_verified,
    shgb_doc.is_verified as shgb_verified,
    pbg_imb_doc.is_verified as pbg_imb_verified,
    marriage_doc.is_verified as marriage_verified
FROM kpr_dossiers kd
JOIN user_profiles up ON kd.user_id = up.id
LEFT JOIN unit_properties uproperty ON kd.unit_id = uproperty.id
-- Left joins for documents
LEFT JOIN documents ktp_doc ON kd.id = ktp_doc.dossier_id AND ktp_doc.type = 'KTP'
LEFT JOIN documents kk_doc ON kd.id = kk_doc.dossier_id AND kk_doc.type = 'KK'
LEFT JOIN documents npwp_doc ON kd.id = npwp_doc.dossier_id AND npwp_doc.type = 'NPWP'
LEFT JOIN documents ktp_pasangan_doc ON kd.id = ktp_pasangan_doc.dossier_id AND ktp_pasangan_doc.type = 'KTP_PASANGAN'
LEFT JOIN documents sp3k_doc ON kd.id = sp3k_doc.dossier_id AND sp3k_doc.type = 'SP3K'
LEFT JOIN documents shgb_doc ON kd.id = shgb_doc.dossier_id AND shgb_doc.type = 'SHGB'
LEFT JOIN documents pbg_imb_doc ON kd.id = pbg_imb_doc.dossier_id AND pbg_imb_doc.type = 'PBG_IMB'
LEFT JOIN documents marriage_doc ON kd.id = marriage_doc.dossier_id AND marriage_doc.type = 'MARRIAGE_CERTIFICATE'
WHERE kd.status IN ('SP3K_TERBIT', 'PRA_AKAD', 'PRA_AKAD_SCHEDULED', 'AKAD_SCHEDULED', 'AKAD_IN_PROGRESS', 'AKAD_COMPLETED');

-- =====================================================
-- FUNCTIONS FOR PRA-AKAD & AKAD SYSTEM
-- =====================================================

-- Function to create Pra-Akad when SP3K is issued
CREATE OR REPLACE FUNCTION create_pra_akad_from_sp3k(p_dossier_id UUID, p_legal_id UUID)
RETURNS UUID AS $$
DECLARE
    v_pra_akad_id UUID;
    v_kpr_data RECORD;
BEGIN
    -- Get KPR dossier data
    SELECT * INTO v_kpr_data
    FROM kpr_dossiers kd
    JOIN user_profiles up ON kd.user_id = up.id
    WHERE kd.id = p_dossier_id;
    
    -- Create Pra-Akad record
    INSERT INTO pra_akad_management (
        dossier_id, legal_assigned_id, pra_akad_status, documents_required,
        created_by, created_at
    ) VALUES (
        p_dossier_id, 
        p_legal_id, 
        'PENDING',
        json_build_object(
            'SI_SURAT_KETERANGAN_LUNAS', false,
            'SP3K_COPY', false,
            'IDENTITY_DOCUMENTS', false,
            'MARRIAGE_DOCUMENTS', false
        ),
        p_legal_id,
        NOW()
    ) RETURNING id INTO v_pra_akad_id;
    
    -- Update KPR status to PRA_AKAD
    UPDATE kpr_dossiers 
    SET status = 'PRA_AKAD', updated_at = NOW()
    WHERE id = p_dossier_id;
    
    -- Send notification to Finance
    INSERT INTO whatsapp_notifications (
        dossier_id, notification_type, recipient_phone, recipient_name,
        message_content, message_template, variables_used, sent_by, sent_at
    ) SELECT 
        p_dossier_id,
        'FINANCE_WARNING_SI_LUNAS',
        up.phone_number,
        up.name,
        'Prepare SI Surat Keterangan Lunas for ' || up.name || ' - KPR Amount: ' || kd.kpr_amount,
        'FINANCE_SI_LUNAS_WARNING',
        json_build_object(
            'customer_name', up.name,
            'kpr_amount', kd.kpr_amount,
            'bank_name', kd.bank_name
        ),
        p_legal_id,
        NOW()
    FROM kpr_dossiers kd
    JOIN user_profiles up ON kd.user_id = up.id
    WHERE kd.id = p_dossier_id;
    
    -- Update finance warning flag
    UPDATE pra_akad_management 
    SET finance_warning_sent = true, finance_warning_date = NOW()
    WHERE id = v_pra_akad_id;
    
    RETURN v_pra_akad_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to schedule Pra-Akad
CREATE OR REPLACE FUNCTION schedule_pra_akad(
    p_pra_akad_id UUID, 
    p_pra_akad_date DATE, 
    p_pra_akad_time TIME,
    p_pra_akad_location VARCHAR,
    p_pra_akad_notes TEXT,
    p_legal_id UUID
)
RETURNS BOOLEAN AS $$
DECLARE
    v_dossier_data RECORD;
BEGIN
    -- Update Pra-Akad schedule
    UPDATE pra_akad_management 
    SET 
        pra_akad_date = p_pra_akad_date,
        pra_akad_time = p_pra_akad_time,
        pra_akad_location = p_pra_akad_location,
        pra_akad_notes = p_pra_akad_notes,
        pra_akad_status = 'SCHEDULED',
        updated_by = p_legal_id,
        updated_at = NOW()
    WHERE id = p_pra_akad_id;
    
    -- Get dossier data for notification
    SELECT * INTO v_dossier_data
    FROM pra_akad_comprehensive
    WHERE id = p_pra_akad_id;
    
    -- Generate Pra-Akad Invitation Document
    INSERT INTO akad_generated_documents (
        dossier_id, pra_akad_id, document_type, template_id,
        document_name, document_url, document_variables, generated_by, generated_at
    ) SELECT 
        v_dossier_data.dossier_id,
        p_pra_akad_id,
        'INVITATION',
        (SELECT id FROM akad_document_templates WHERE template_type = 'INVITATION' LIMIT 1),
        'Pra-Akad Invitation - ' || v_dossier_data.customer_name,
        '/documents/blank/pra_akad_invitation.pdf', -- Blank PDF template
        json_build_object(
            'customer_name', v_dossier_data.customer_name,
            'pra_akad_date', p_pra_akad_date,
            'pra_akad_time', p_pra_akad_time,
            'pra_akad_location', p_pra_akad_location,
            'unit_info', v_dossier_data.block_number || '-' || v_dossier_data.unit_number,
            'kpr_amount', v_dossier_data.kpr_amount
        ),
        p_legal_id,
        NOW();
    
    -- Send WhatsApp notification to customer
    INSERT INTO whatsapp_notifications (
        dossier_id, notification_type, recipient_phone, recipient_name,
        message_content, message_template, variables_used, sent_by, sent_at
    ) VALUES (
        v_dossier_data.dossier_id,
        'PRA_AKAD_INVITATION',
        v_dossier_data.customer_phone,
        v_dossier_data.customer_name,
        'Undangan Pra-Akad: ' || p_pra_akad_date || ' pukul ' || p_pra_akad_time || ' di ' || p_pra_akad_location,
        'PRA_AKAD_INVITATION',
        json_build_object(
            'customer_name', v_dossier_data.customer_name,
            'pra_akad_date', p_pra_akad_date,
            'pra_akad_time', p_pra_akad_time,
            'pra_akad_location', p_pra_akad_location
        ),
        p_legal_id,
        NOW()
    );
    
    -- Update KPR status
    UPDATE kpr_dossiers 
    SET status = 'PRA_AKAD_SCHEDULED', updated_at = NOW()
    WHERE id = v_dossier_data.dossier_id;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to create Akad Credit from Pra-Akad
CREATE OR REPLACE FUNCTION create_akad_from_pra_akad(p_pra_akad_id UUID, p_legal_id UUID, p_notaris_id UUID)
RETURNS UUID AS $$
DECLARE
    v_akad_id UUID;
    v_pra_akad_data RECORD;
    v_notaris_data RECORD;
BEGIN
    -- Get Pra-Akad data
    SELECT * INTO v_pra_akad_data
    FROM pra_akad_comprehensive
    WHERE id = p_pra_akad_id;
    
    -- Get Notaris data
    SELECT * INTO v_notaris_data
    FROM notaris_management
    WHERE id = p_notaris_id;
    
    -- Create Akad Credit record
    INSERT INTO akad_credit_management (
        dossier_id, pra_akad_id, legal_assigned_id, notaris_id, notaris_name,
        notaris_contact, notaris_address, akad_status, created_by, created_at
    ) VALUES (
        v_pra_akad_data.dossier_id,
        p_pra_akad_id,
        p_legal_id,
        p_notaris_id,
        v_notaris_data.notaris_name,
        v_notaris_data.notaris_phone,
        v_notaris_data.notaris_address,
        'SCHEDULED',
        p_legal_id,
        NOW()
    ) RETURNING id INTO v_akad_id;
    
    -- Update KPR status to AKAD_SCHEDULED
    UPDATE kpr_dossiers 
    SET status = 'AKAD_SCHEDULED', updated_at = NOW()
    WHERE id = v_pra_akad_data.dossier_id;
    
    RETURN v_akad_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to schedule Akad Credit (Legal only access)
CREATE OR REPLACE FUNCTION schedule_akad_credit(
    p_akad_id UUID, 
    p_akad_date DATE, 
    p_akad_time TIME,
    p_akad_location VARCHAR,
    p_akad_notes TEXT,
    p_legal_id UUID
)
RETURNS BOOLEAN AS $$
DECLARE
    v_dossier_data RECORD;
BEGIN
    -- Update Akad Credit schedule (only Legal can set date/time)
    UPDATE akad_credit_management 
    SET 
        akad_date = p_akad_date,
        akad_time = p_akad_time,
        akad_location = p_akad_location,
        akad_notes = p_akad_notes,
        akad_status = 'SCHEDULED',
        updated_by = p_legal_id,
        updated_at = NOW()
    WHERE id = p_akad_id;
    
    -- Get dossier data for notification
    SELECT * INTO v_dossier_data
    FROM akad_credit_comprehensive
    WHERE id = p_akad_id;
    
    -- Generate Akad Invitation Document
    INSERT INTO akad_generated_documents (
        dossier_id, akad_id, document_type, template_id,
        document_name, document_url, document_variables, generated_by, generated_at
    ) SELECT 
        v_dossier_data.dossier_id,
        p_akad_id,
        'INVITATION',
        (SELECT id FROM akad_document_templates WHERE template_type = 'INVITATION' LIMIT 1),
        'Akad Credit Invitation - ' || v_dossier_data.customer_name,
        '/documents/blank/akad_invitation.pdf', -- Blank PDF template
        json_build_object(
            'customer_name', v_dossier_data.customer_name,
            'akad_date', p_akad_date,
            'akad_time', p_akad_time,
            'akad_location', p_akad_location,
            'notaris_name', v_dossier_data.notaris_name,
            'unit_info', v_dossier_data.block_number || '-' || v_dossier_data.unit_number,
            'kpr_amount', v_dossier_data.kpr_amount
        ),
        p_legal_id,
        NOW();
    
    -- Send WhatsApp notification to customer
    INSERT INTO whatsapp_notifications (
        dossier_id, notification_type, recipient_phone, recipient_name,
        message_content, message_template, variables_used, sent_by, sent_at
    ) VALUES (
        v_dossier_data.dossier_id,
        'AKAD_INVITATION',
        v_dossier_data.customer_phone,
        v_dossier_data.customer_name,
        'Undangan Akad Credit: ' || p_akad_date || ' pukul ' || p_akad_time || ' di ' || p_akad_location || ' dengan Notaris ' || v_dossier_data.notaris_name,
        'AKAD_INVITATION',
        json_build_object(
            'customer_name', v_dossier_data.customer_name,
            'akad_date', p_akad_date,
            'akad_time', p_akad_time,
            'akad_location', p_akad_location,
            'notaris_name', v_dossier_data.notaris_name
        ),
        p_legal_id,
        NOW()
    );
    
    -- Update customer notification flag
    UPDATE akad_credit_management 
    SET customer_notified = true, customer_notification_date = NOW()
    WHERE id = p_akad_id;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to generate Hari H documents (Internal Memo + Appraisal Memo + SO Legal)
CREATE OR REPLACE FUNCTION generate_hari_h_documents(p_akad_id UUID, p_legal_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_dossier_data RECORD;
    v_memo_number VARCHAR;
BEGIN
    -- Get dossier data
    SELECT * INTO v_dossier_data
    FROM akad_credit_comprehensive
    WHERE id = p_akad_id;
    
    -- Generate auto memo number
    v_memo_number := 'MEMO-' || TO_CHAR(NOW(), 'YYYY-MM-DD') || '-' || 
                   (SELECT COUNT(*) + 1 FROM internal_memos WHERE memo_date = CURRENT_DATE);
    
    -- Generate Internal Memo
    INSERT INTO internal_memos (
        dossier_id, memo_type, memo_title, memo_content, memo_date, memo_number,
        priority, recipients, document_url, created_by, created_at
    ) VALUES (
        v_dossier_data.dossier_id,
        'INTERNAL',
        'Memo Internal - Persiapan Akad Credit ' || v_dossier_data.customer_name,
        'Memo internal untuk persiapan pelaksanaan Akad Credit pada tanggal ' || v_dossier_data.akad_date ||
        ' untuk nasabah ' || v_dossier_data.customer_name || ' dengan Notaris ' || v_dossier_data.notaris_name,
        v_dossier_data.akad_date,
        v_memo_number,
        'HIGH',
        json_build_array(p_legal_id), -- Send to Legal
        '/documents/blank/memo_internal_akad.pdf', -- Blank PDF template
        p_legal_id,
        NOW()
    );
    
    -- Generate Appraisal Request Memo
    INSERT INTO internal_memos (
        dossier_id, memo_type, memo_title, memo_content, memo_date, memo_number,
        priority, recipients, document_url, created_by, created_at
    ) VALUES (
        v_dossier_data.dossier_id,
        'APPRAISAL_REQUEST',
        'Memo Pengajuan Taksasi/Appraisal - ' || v_dossier_data.customer_name,
        'Memo pengajuan taksasi kepada pihak bank untuk unit ' || v_dossier_data.block_number || '-' || v_dossier_data.unit_number ||
        ' atas nama ' || v_dossier_data.customer_name || ' dengan plafon KPR ' || v_dossier_data.kpr_amount,
        v_dossier_data.akad_date,
        'APPRAISAL-' || TO_CHAR(NOW(), 'YYYY-MM-DD') || '-' || 
        (SELECT COUNT(*) + 1 FROM internal_memos WHERE memo_type = 'APPRAISAL_REQUEST' AND memo_date = CURRENT_DATE),
        'HIGH',
        json_build_array(p_legal_id), -- Send to Legal for bank coordination
        '/documents/blank/memo_appraisal_request.pdf', -- Blank PDF template
        p_legal_id,
        NOW()
    );
    
    -- Generate SO Legal for Notaris with complete documents
    INSERT INTO akad_generated_documents (
        dossier_id, akad_id, document_type, template_id,
        document_name, document_url, document_variables, generated_by, generated_at
    ) SELECT 
        v_dossier_data.dossier_id,
        p_akad_id,
        'SO_LEGAL',
        (SELECT id FROM akad_document_templates WHERE template_type = 'SO_LEGAL' LIMIT 1),
        'SO Legal untuk Notaris - ' || v_dossier_data.customer_name,
        '/documents/blank/so_legal_notaris.pdf', -- Blank PDF template
        json_build_object(
            'customer_name', v_dossier_data.customer_name,
            'customer_nik', v_dossier_data.customer_nik,
            'customer_phone', v_dossier_data.customer_phone,
            'customer_email', v_dossier_data.customer_email,
            'kpr_amount', v_dossier_data.kpr_amount,
            'dp_amount', v_dossier_data.dp_amount,
            'bank_name', v_dossier_data.bank_name,
            'unit_info', v_dossier_data.block_number || '-' || v_dossier_data.unit_number,
            'unit_type', v_dossier_data.unit_type,
            'akad_date', v_dossier_data.akad_date,
            'akad_time', v_dossier_data.akad_time,
            'akad_location', v_dossier_data.akad_location,
            'notaris_name', v_dossier_data.notaris_name,
            'documents_attached', json_build_object(
                'ktp_url', v_dossier_data.ktp_url,
                'kk_url', v_dossier_data.kk_url,
                'npwp_url', v_dossier_data.npwp_url,
                'ktp_pasangan_url', v_dossier_data.ktp_pasangan_url,
                'sp3k_url', v_dossier_data.sp3k_url,
                'shgb_url', v_dossier_data.shgb_url,
                'pbg_imb_url', v_dossier_data.pbg_imb_url,
                'marriage_certificate_url', v_dossier_data.marriage_certificate_url
            )
        ),
        p_legal_id,
        NOW();
    
    -- Update KPR status to AKAD_IN_PROGRESS
    UPDATE kpr_dossiers 
    SET status = 'AKAD_IN_PROGRESS', updated_at = NOW()
    WHERE id = v_dossier_data.dossier_id;
    
    -- Update Akad status
    UPDATE akad_credit_management 
    SET akad_status = 'IN_PROGRESS', updated_at = NOW()
    WHERE id = p_akad_id;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Pra-Akad Management Indexes
CREATE INDEX idx_pra_akad_dossier_id ON pra_akad_management(dossier_id);
CREATE INDEX idx_pra_akad_legal_assigned ON pra_akad_management(legal_assigned_id);
CREATE INDEX idx_pra_akad_status ON pra_akad_management(pra_akad_status);
CREATE INDEX idx_pra_akad_date ON pra_akad_management(pra_akad_date);
CREATE INDEX idx_pra_akad_created_at ON pra_akad_management(created_at);

-- Akad Credit Management Indexes
CREATE INDEX idx_akad_dossier_id ON akad_credit_management(dossier_id);
CREATE INDEX idx_akad_pra_akad_id ON akad_credit_management(pra_akad_id);
CREATE INDEX idx_akad_legal_assigned ON akad_credit_management(legal_assigned_id);
CREATE INDEX idx_akad_notaris_id ON akad_credit_management(notaris_id);
CREATE INDEX idx_akad_status ON akad_credit_management(akad_status);
CREATE INDEX idx_akad_date ON akad_credit_management(akad_date);
CREATE INDEX idx_akad_created_at ON akad_credit_management(created_at);

-- Generated Documents Indexes
CREATE INDEX idx_generated_docs_dossier_id ON akad_generated_documents(dossier_id);
CREATE INDEX idx_generated_docs_pra_akad_id ON akad_generated_documents(pra_akad_id);
CREATE INDEX idx_generated_docs_akad_id ON akad_generated_documents(akad_id);
CREATE INDEX idx_generated_docs_type ON akad_generated_documents(document_type);
CREATE INDEX idx_generated_docs_generated_at ON akad_generated_documents(generated_at);

-- WhatsApp Notifications Indexes
CREATE INDEX idx_whatsapp_dossier_id ON whatsapp_notifications(dossier_id);
CREATE INDEX idx_whatsapp_type ON whatsapp_notifications(notification_type);
CREATE INDEX idx_whatsapp_status ON whatsapp_notifications(delivery_status);
CREATE INDEX idx_whatsapp_sent_at ON whatsapp_notifications(sent_at);

-- Internal Memos Indexes
CREATE INDEX idx_memos_dossier_id ON internal_memos(dossier_id);
CREATE INDEX idx_memos_type ON internal_memos(memo_type);
CREATE INDEX idx_memos_date ON internal_memos(memo_date);
CREATE INDEX idx_memos_priority ON internal_memos(priority);
CREATE INDEX idx_memos_created_at ON internal_memos(created_at);

-- =====================================================
-- RLS POLICIES FOR PRA-AKAD & AKAD SYSTEM
-- =====================================================

-- Enable RLS on all new tables
ALTER TABLE pra_akad_management ENABLE ROW LEVEL SECURITY;
ALTER TABLE akad_credit_management ENABLE ROW LEVEL SECURITY;
ALTER TABLE akad_document_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE akad_generated_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE whatsapp_notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE internal_memos ENABLE ROW LEVEL SECURITY;
ALTER TABLE notaris_management ENABLE ROW LEVEL SECURITY;

-- Pra-Akad Management RLS Policies
CREATE POLICY "Legal can manage pra-akad" ON pra_akad_management
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

CREATE POLICY "Finance can view pra-akad" ON pra_akad_management
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('FINANCE', 'LEGAL', 'BOD'));

CREATE POLICY "Marketing can view pra-akad status" ON pra_akad_management
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('MARKETING', 'LEGAL', 'BOD'));

-- Akad Credit Management RLS Policies
CREATE POLICY "Legal can manage akad credit" ON akad_credit_management
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

CREATE POLICY "Finance can view akad credit" ON akad_credit_management
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('FINANCE', 'LEGAL', 'BOD'));

CREATE POLICY "Marketing can view akad credit status" ON akad_credit_management
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('MARKETING', 'LEGAL', 'BOD'));

-- Generated Documents RLS Policies
CREATE POLICY "Legal can manage generated documents" ON akad_generated_documents
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

CREATE POLICY "Finance can view generated documents" ON akad_generated_documents
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('FINANCE', 'LEGAL', 'BOD'));

-- WhatsApp Notifications RLS Policies
CREATE POLICY "Legal can manage whatsapp notifications" ON whatsapp_notifications
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

-- Internal Memos RLS Policies
CREATE POLICY "Legal can manage internal memos" ON internal_memos
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

CREATE POLICY "Finance can view internal memos" ON internal_memos
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('FINANCE', 'LEGAL', 'BOD'));

-- Notaris Management RLS Policies
CREATE POLICY "Legal can manage notaris" ON notaris_management
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

CREATE POLICY "All can view active notaris" ON notaris_management
    FOR SELECT USING (is_active = true);

-- =====================================================
-- SAMPLE DATA INSERTION
-- =====================================================

-- Insert document templates
INSERT INTO akad_document_templates (template_name, template_type, template_description, template_url) VALUES
('Pra-Akad Invitation Template', 'INVITATION', 'Template untuk undangan Pra-Akad', '/templates/blank/pra_akad_invitation.pdf'),
('Akad Credit Invitation Template', 'INVITATION', 'Template untuk undangan Akad Credit', '/templates/blank/akad_invitation.pdf'),
('Memo Internal Template', 'MEMO_INTERNAL', 'Template untuk memo internal persiapan akad', '/templates/blank/memo_internal.pdf'),
('Memo Appraisal Request Template', 'MEMO_APPRAISAL', 'Template untuk memo pengajuan taksasi', '/templates/blank/memo_appraisal.pdf'),
('SO Legal Notaris Template', 'SO_LEGAL', 'Template untuk SO Legal ke notaris', '/templates/blank/so_legal_notaris.pdf'),
('SI Surat Keterangan Lunas Template', 'SI_LUNAS', 'Template untuk SI Surat Keterangan Lunas', '/templates/blank/si_lunas.pdf');

-- Insert sample notaris
INSERT INTO notaris_management (notaris_name, notaris_office_name, notaris_address, notaris_phone, notaris_email, notaris_sk_number, notaris_region) VALUES
('Notaris Ahmad Wijaya, S.H., M.Kn.', 'Kantor Notaris Ahmad Wijaya', 'Jl. Legal No. 123, Jakarta Selatan', '021-5551234', 'ahmad.wijaya@notaris.com', 'SK-123-2022', 'Jakarta Selatan'),
('Notaris Siti Nurhaliza, S.H., M.Kn.', 'Kantor Notaris Siti Nurhaliza', 'Jl. Notariaat No. 456, Jakarta Pusat', '021-6665678', 'siti.nurhaliza@notaris.com', 'SK-456-2022', 'Jakarta Pusat'),
('Notaris Budi Santoso, S.H., M.Kn.', 'Kantor Notaris Budi Santoso', 'Jl. Akad No. 789, Jakarta Barat', '021-7779012', 'budi.santoso@notaris.com', 'SK-789-2022', 'Jakarta Barat');

-- =====================================================
-- TRIGGERS FOR AUTOMATIC WORKFLOW
-- =====================================================

-- Trigger to create Pra-Akad when SP3K is issued
CREATE OR REPLACE FUNCTION trigger_create_pra_akad_on_sp3k()
RETURNS TRIGGER AS $$
BEGIN
    -- Only create Pra-Akad if status changes to SP3K_TERBIT
    IF NEW.status = 'SP3K_TERBIT' AND OLD.status != 'SP3K_TERBIT' THEN
        -- Find Legal officer (for now, assign to first Legal user)
        DECLARE
            v_legal_id UUID;
        BEGIN
            SELECT id INTO v_legal_id
            FROM user_profiles 
            WHERE role = 'LEGAL' AND is_active = true 
            LIMIT 1;
            
            IF v_legal_id IS NOT NULL THEN
                PERFORM create_pra_akad_from_sp3k(NEW.id, v_legal_id);
            END IF;
        END;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_create_pra_akad_on_sp3k
    AFTER UPDATE ON kpr_dossiers
    FOR EACH ROW EXECUTE FUNCTION trigger_create_pra_akad_on_sp3k();

-- Function to update timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply timestamp triggers
CREATE TRIGGER trigger_pra_akad_updated_at
    BEFORE UPDATE ON pra_akad_management
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_akad_credit_updated_at
    BEFORE UPDATE ON akad_credit_management
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_internal_memos_updated_at
    BEFORE UPDATE ON internal_memos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
