-- Updated Pra-Akad & Akad Credit System
-- Finance Warning Logic Update: Only trigger when Akad Credit date is set
-- KPRFlow Enterprise - Enhanced KPR Status Management

-- =====================================================
-- UPDATED FUNCTIONS FOR ENHANCED FINANCE WARNING LOGIC
-- =====================================================

-- Updated Function to create Pra-Akad when SP3K is issued (NO FINANCE WARNING)
CREATE OR REPLACE FUNCTION create_pra_akad_from_sp3k_no_warning(p_dossier_id UUID, p_legal_id UUID)
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
    
    -- NO FINANCE WARNING at this stage
    -- Finance warning will be triggered only when Akad Credit is scheduled
    
    RETURN v_pra_akad_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Updated Function to trigger Finance Warning when Akad Credit is scheduled
CREATE OR REPLACE FUNCTION trigger_finance_warning_on_akad_scheduled(p_akad_id UUID, p_legal_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_dossier_data RECORD;
    v_pra_akad_data RECORD;
BEGIN
    -- Get Akad and dossier data
    SELECT * INTO v_dossier_data
    FROM akad_credit_comprehensive
    WHERE id = p_akad_id;
    
    -- Get Pra-Akad data
    SELECT * INTO v_pra_akad_data
    FROM pra_akad_comprehensive
    WHERE id = v_dossier_data.pra_akad_id;
    
    -- Send Finance Warning for SI preparation
    INSERT INTO whatsapp_notifications (
        dossier_id, notification_type, recipient_phone, recipient_name,
        message_content, message_template, variables_used, sent_by, sent_at
    ) SELECT 
        v_dossier_data.dossier_id,
        'FINANCE_WARNING_SI_LUNAS',
        -- Get Finance officer phone (for now, use customer phone as placeholder)
        up.phone_number,
        up.name,
        'URGENT: Prepare SI Surat Keterangan Lunas for ' || up.name || 
        ' - Akad Credit scheduled on ' || v_dossier_data.akad_date || 
        ' pukul ' || v_dossier_data.akad_time || ' dengan Notaris ' || v_dossier_data.notaris_name ||
        '. KPR Amount: ' || kd.kpr_amount,
        'FINANCE_SI_LUNAS_WARNING_AKAD_SCHEDULED',
        json_build_object(
            'customer_name', up.name,
            'kpr_amount', kd.kpr_amount,
            'bank_name', kd.bank_name,
            'akad_date', v_dossier_data.akad_date,
            'akad_time', v_dossier_data.akad_time,
            'notaris_name', v_dossier_data.notaris_name,
            'days_until_akad', (v_dossier_data.akad_date::date - CURRENT_DATE)
        ),
        p_legal_id,
        NOW()
    FROM kpr_dossiers kd
    JOIN user_profiles up ON kd.user_id = up.id
    WHERE kd.id = v_dossier_data.dossier_id;
    
    -- Update finance warning flag in Pra-Akad
    UPDATE pra_akad_management 
    SET finance_warning_sent = true, 
        finance_warning_date = NOW(),
        si_surat_keterangan_lunas_status = 'PREPARING'
    WHERE id = v_pra_akad_data.id;
    
    -- Create internal memo for Finance team
    INSERT INTO internal_memos (
        dossier_id, memo_type, memo_title, memo_content, memo_date, memo_number,
        priority, recipients, document_url, created_by, created_at
    ) VALUES (
        v_dossier_data.dossier_id,
        'INTERNAL',
        'URGENT: SI Surat Keterangan Lunas Preparation - ' || v_dossier_data.customer_name,
        'Memo internal untuk persiapan SI Surat Keterangan Lunas. Akad Credit akan dilaksanakan pada ' || 
        v_dossier_data.akad_date || ' pukul ' || v_dossier_data.akad_time || 
        ' dengan Notaris ' || v_dossier_data.notaris_name || '. Customer: ' || v_dossier_data.customer_name ||
        ' dengan plafon KPR ' || v_dossier_data.kpr_amount || '. Mohon segera menyiapkan SI Surat Keterangan Lunas.',
        CURRENT_DATE,
        'SI-LUNAS-' || TO_CHAR(NOW(), 'YYYY-MM-DD') || '-' || 
        (SELECT COUNT(*) + 1 FROM internal_memos WHERE memo_type = 'INTERNAL' AND memo_date = CURRENT_DATE),
        'URGENT',
        json_build_array(p_legal_id), -- Send to Legal for coordination
        '/documents/blank/memo_si_lunas_preparation.pdf', -- Blank PDF template
        p_legal_id,
        NOW()
    );
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Updated Function to schedule Akad Credit with Finance Warning trigger
CREATE OR REPLACE FUNCTION schedule_akad_credit_with_finance_warning(
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
    v_days_until_akad INTEGER;
BEGIN
    -- Calculate days until Akad
    v_days_until_akad := p_akad_date - CURRENT_DATE;
    
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
            'kpr_amount', v_dossier_data.kpr_amount,
            'days_until_akad', v_days_until_akad
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
            'notaris_name', v_dossier_data.notaris_name,
            'days_until_akad', v_days_until_akad
        ),
        p_legal_id,
        NOW()
    );
    
    -- Update customer notification flag
    UPDATE akad_credit_management 
    SET customer_notified = true, customer_notification_date = NOW()
    WHERE id = p_akad_id;
    
    -- TRIGGER FINANCE WARNING (NEW LOGIC)
    PERFORM trigger_finance_warning_on_akad_scheduled(p_akad_id, p_legal_id);
    
    -- Update KPR status to AKAD_SCHEDULED
    UPDATE kpr_dossiers 
    SET status = 'AKAD_SCHEDULED', updated_at = NOW()
    WHERE id = v_dossier_data.dossier_id;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to check if Finance Warning should be sent
CREATE OR REPLACE FUNCTION should_send_finance_warning(p_dossier_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_akad_scheduled BOOLEAN;
    v_finance_warning_sent BOOLEAN;
BEGIN
    -- Check if Akad Credit is scheduled
    SELECT EXISTS(
        SELECT 1 FROM akad_credit_management 
        WHERE dossier_id = p_dossier_id 
        AND akad_date IS NOT NULL 
        AND akad_time IS NOT NULL
    ) INTO v_akad_scheduled;
    
    -- Check if Finance Warning already sent
    SELECT finance_warning_sent INTO v_finance_warning_sent
    FROM pra_akad_management 
    WHERE dossier_id = p_dossier_id;
    
    -- Return true if Akad is scheduled but Finance Warning not sent
    RETURN v_akad_scheduled AND NOT COALESCE(v_finance_warning_sent, FALSE);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to send Finance Warning manually (if needed)
CREATE OR REPLACE FUNCTION send_finance_warning_manually(p_dossier_id UUID, p_legal_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_akad_data RECORD;
BEGIN
    -- Get Akad Credit data
    SELECT * INTO v_akad_data
    FROM akad_credit_comprehensive
    WHERE dossier_id = p_dossier_id
    AND akad_date IS NOT NULL
    AND akad_time IS NOT NULL;
    
    -- Check if Akad data exists
    IF v_akad_data IS NULL THEN
        RAISE EXCEPTION 'Akad Credit not scheduled yet. Cannot send Finance Warning.';
    END IF;
    
    -- Trigger Finance Warning
    PERFORM trigger_finance_warning_on_akad_scheduled(v_akad_data.id, p_legal_id);
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- UPDATED TRIGGERS
-- =====================================================

-- Updated Trigger to create Pra-Akad when SP3K is issued (NO FINANCE WARNING)
CREATE OR REPLACE FUNCTION trigger_create_pra_akad_on_sp3k_no_warning()
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
                PERFORM create_pra_akad_from_sp3k_no_warning(NEW.id, v_legal_id);
            END IF;
        END;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop old trigger and create new one
DROP TRIGGER IF EXISTS trigger_create_pra_akad_on_sp3k ON kpr_dossiers;

CREATE TRIGGER trigger_create_pra_akad_on_sp3k_no_warning
    AFTER UPDATE ON kpr_dossiers
    FOR EACH ROW EXECUTE FUNCTION trigger_create_pra_akad_on_sp3k_no_warning();

-- =====================================================
-- UPDATED VIEWS FOR FINANCE WARNING STATUS
-- =====================================================

-- Enhanced Pra-Akad View with Finance Warning Logic
CREATE OR REPLACE VIEW pra_akad_comprehensive AS
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
    -- NEW: Akad scheduling status
    CASE 
        WHEN acm.akad_date IS NOT NULL AND acm.akad_time IS NOT NULL THEN true
        ELSE false
    END as akad_scheduled,
    acm.akad_date as scheduled_akad_date,
    acm.akad_time as scheduled_akad_time,
    acm.notaris_name,
    -- NEW: Days until Akad
    CASE 
        WHEN acm.akad_date IS NOT NULL THEN (acm.akad_date::date - CURRENT_DATE)
        ELSE NULL
    END as days_until_akad,
    -- NEW: Finance warning priority
    CASE 
        WHEN acm.akad_date IS NOT NULL AND 
             acm.akad_date::date - CURRENT_DATE <= 7 AND 
             pam.finance_warning_sent = false THEN 'URGENT'
        WHEN acm.akad_date IS NOT NULL AND 
             acm.akad_date::date - CURRENT_DATE <= 14 AND 
             pam.finance_warning_sent = false THEN 'HIGH'
        WHEN acm.akad_date IS NOT NULL AND pam.finance_warning_sent = false THEN 'NORMAL'
        WHEN pam.finance_warning_sent = true THEN 'COMPLETED'
        ELSE 'PENDING'
    END as finance_warning_priority,
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
LEFT JOIN akad_credit_management acm ON pam.id = acm.pra_akad_id
ORDER BY 
    CASE 
        WHEN acm.akad_date IS NOT NULL AND 
             acm.akad_date::date - CURRENT_DATE <= 7 AND 
             pam.finance_warning_sent = false THEN 1
        WHEN acm.akad_date IS NOT NULL AND 
             acm.akad_date::date - CURRENT_DATE <= 14 AND 
             pam.finance_warning_sent = false THEN 2
        ELSE 3
    END,
    acm.akad_date ASC NULLS LAST,
    pam.created_at DESC;

-- Finance Dashboard View for SI Preparation
CREATE OR REPLACE VIEW finance_si_preparation_dashboard AS
SELECT 
    pam.id as pra_akad_id,
    pam.dossier_id,
    kd.user_id as customer_id,
    up.name as customer_name,
    up.phone_number as customer_phone,
    kd.unit_id,
    uproperty.block_number,
    uproperty.unit_number,
    kd.kpr_amount,
    kd.dp_amount,
    kd.bank_name,
    pam.pra_akad_status,
    pam.si_surat_keterangan_lunas_status,
    pam.si_surat_keterangan_lunas_url,
    pam.finance_warning_sent,
    pam.finance_warning_date,
    acm.akad_date,
    acm.akad_time,
    acm.notaris_name,
    -- Calculated fields
    CASE 
        WHEN acm.akad_date IS NOT NULL THEN (acm.akad_date::date - CURRENT_DATE)
        ELSE NULL
    END as days_until_akad,
    CASE 
        WHEN acm.akad_date IS NOT NULL AND 
             acm.akad_date::date - CURRENT_DATE <= 3 AND 
             pam.si_surat_keterangan_lunas_status != 'READY' THEN 'CRITICAL'
        WHEN acm.akad_date IS NOT NULL AND 
             acm.akad_date::date - CURRENT_DATE <= 7 AND 
             pam.si_surat_keterangan_lunas_status != 'READY' THEN 'URGENT'
        WHEN acm.akad_date IS NOT NULL AND 
             acm.akad_date::date - CURRENT_DATE <= 14 AND 
             pam.si_surat_keterangan_lunas_status != 'READY' THEN 'HIGH'
        WHEN pam.si_surat_keterangan_lunas_status = 'READY' THEN 'COMPLETED'
        ELSE 'PENDING'
    END as priority_level,
    legal.name as legal_assigned_name,
    legal.phone_number as legal_phone,
    pam.created_at,
    pam.updated_at
FROM pra_akad_management pam
JOIN kpr_dossiers kd ON pam.dossier_id = kd.id
JOIN user_profiles up ON kd.user_id = up.id
LEFT JOIN unit_properties uproperty ON kd.unit_id = uproperty.id
LEFT JOIN user_profiles legal ON pam.legal_assigned_id = legal.id
LEFT JOIN akad_credit_management acm ON pam.id = acm.pra_akad_id
WHERE acm.akad_date IS NOT NULL -- Only show scheduled Akad
ORDER BY 
    CASE 
        WHEN acm.akad_date IS NOT NULL AND 
             acm.akad_date::date - CURRENT_DATE <= 3 AND 
             pam.si_surat_keterangan_lunas_status != 'READY' THEN 1
        WHEN acm.akad_date IS NOT NULL AND 
             acm.akad_date::date - CURRENT_DATE <= 7 AND 
             pam.si_surat_keterangan_lunas_status != 'READY' THEN 2
        ELSE 3
    END,
    acm.akad_date ASC;

-- =====================================================
-- SAMPLE DATA UPDATE
-- =====================================================

-- Update sample data to demonstrate new logic
UPDATE pra_akad_management 
SET finance_warning_sent = false, 
    finance_warning_date = NULL,
    si_surat_keterangan_lunas_status = 'PENDING'
WHERE finance_warning_sent = true;

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
UPDATED FINANCE WARNING LOGIC DOCUMENTATION

========================================
OLD LOGIC:
- Finance Warning triggered saat SP3K terbit
- SI preparation dimulai sebelum Akad dijadwalkan
- Tidak ada hubungan dengan tanggal Akad

NEW LOGIC:
- Finance Warning hanya triggered saat Akad Credit dijadwalkan
- SI preparation dimulai setelah ada tanggal Akad yang pasti
- Priority level berdasarkan hari hingga Akad
- Lebih efisien dan relevan untuk Finance team

========================================
FINANCE WARNING TRIGGERS:
1. SP3K Terbit → Pra-Akad created (NO Finance Warning)
2. Akad Credit scheduled → Finance Warning triggered
3. Manual trigger jika diperlukan

========================================
FINANCE WARNING PRIORITY LEVELS:
- CRITICAL: ≤ 3 hari sebelum Akad, SI belum READY
- URGENT: 4-7 hari sebelum Akad, SI belum READY  
- HIGH: 8-14 hari sebelum Akad, SI belum READY
- COMPLETED: SI sudah READY
- PENDING: Akad belum dijadwalkan

========================================
BENEFITS:
- Finance preparation lebih efisien
- Tidak ada wasted effort untuk SI preparation
- Priority yang jelas berdasarkan urgency
- Better resource allocation untuk Finance team
- More relevant timing untuk document preparation
*/
