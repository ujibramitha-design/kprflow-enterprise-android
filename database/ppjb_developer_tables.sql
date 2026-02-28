-- =====================================================
-- KPRFlow Enterprise - PPJB Developer Database Schema
-- Phase 16: Legal & Documentation Automation
-- =====================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- PPJB DEVELOPER PROCESSES TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS ppjb_developer_processes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    unit_property_id UUID NOT NULL REFERENCES unit_properties(id) ON DELETE CASCADE,
    ppjb_type ppjb_type NOT NULL,
    status ppjb_status NOT NULL DEFAULT 'SCHEDULED',
    scheduled_date TIMESTAMP WITH TIME ZONE NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    reminder_count INTEGER DEFAULT 0,
    max_reminders INTEGER DEFAULT 2,
    sla_days INTEGER DEFAULT 30,
    actual_ppjb_date TIMESTAMP WITH TIME ZONE,
    completion_date TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    cancellation_notes TEXT,
    auto_cancelled BOOLEAN DEFAULT false,
    document_generated BOOLEAN DEFAULT false,
    invitation_sent BOOLEAN DEFAULT false,
    last_reminder_sent TIMESTAMP WITH TIME ZONE,
    next_reminder_date TIMESTAMP WITH TIME ZONE,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    updated_by UUID REFERENCES user_profiles(id)
);

-- =====================================================
-- PPJB DOCUMENTS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS ppjb_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ppjb_process_id UUID NOT NULL REFERENCES ppjb_developer_processes(id) ON DELETE CASCADE,
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    document_type ppjb_document_type NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    file_size BIGINT NOT NULL,
    file_mime_type VARCHAR(100) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    storage_provider VARCHAR(50) DEFAULT 'SUPABASE',
    storage_path TEXT,
    public_url TEXT,
    status document_status NOT NULL DEFAULT 'GENERATED',
    verification_status verification_status DEFAULT 'PENDING',
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    signed_at TIMESTAMP WITH TIME ZONE,
    verified_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    updated_by UUID REFERENCES user_profiles(id)
);

-- =====================================================
-- PPJB REMINDERS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS ppjb_reminders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ppjb_process_id UUID NOT NULL REFERENCES ppjb_developer_processes(id) ON DELETE CASCADE,
    reminder_type ppjb_reminder_type NOT NULL,
    scheduled_date TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_date TIMESTAMP WITH TIME ZONE,
    status reminder_status NOT NULL DEFAULT 'SCHEDULED',
    delivery_method VARCHAR(50) DEFAULT 'WHATSAPP',
    recipient_phone VARCHAR(20),
    message_content TEXT,
    delivery_status VARCHAR(50) DEFAULT 'PENDING',
    delivery_attempts INTEGER DEFAULT 0,
    last_delivery_attempt TIMESTAMP WITH TIME ZONE,
    delivery_error TEXT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- PPJB INVITATIONS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS ppjb_invitations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ppjb_process_id UUID NOT NULL REFERENCES ppjb_developer_processes(id) ON DELETE CASCADE,
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    invitation_number VARCHAR(50) UNIQUE NOT NULL,
    invitation_date TIMESTAMP WITH TIME ZONE NOT NULL,
    invitation_time TIME NOT NULL DEFAULT '10:00:00',
    venue VARCHAR(255) NOT NULL,
    venue_address TEXT NOT NULL,
    contact_person VARCHAR(255),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(255),
    attendance_status invitation_attendance_status DEFAULT 'PENDING',
    confirmation_date TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    document_generated BOOLEAN DEFAULT false,
    document_url TEXT,
    sent_at TIMESTAMP WITH TIME ZONE,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    updated_by UUID REFERENCES user_profiles(id)
);

-- =====================================================
-- PPJB AUDIT LOG TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS ppjb_audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ppjb_process_id UUID REFERENCES ppjb_developer_processes(id) ON DELETE SET NULL,
    dossier_id UUID REFERENCES kpr_dossiers(id) ON DELETE SET NULL,
    customer_id UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    user_id UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    session_id VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- PPJB Processes indexes
CREATE INDEX IF NOT EXISTS idx_ppjb_processes_dossier_id ON ppjb_developer_processes(dossier_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_processes_customer_id ON ppjb_developer_processes(customer_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_processes_unit_id ON ppjb_developer_processes(unit_property_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_processes_status ON ppjb_developer_processes(status);
CREATE INDEX IF NOT EXISTS idx_ppjb_processes_type ON ppjb_developer_processes(ppjb_type);
CREATE INDEX IF NOT EXISTS idx_ppjb_processes_scheduled_date ON ppjb_developer_processes(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_ppjb_processes_expiry_date ON ppjb_developer_processes(expiry_date);
CREATE INDEX IF NOT EXISTS idx_ppjb_processes_active ON ppjb_developer_processes(status) WHERE status IN ('SCHEDULED', 'REMINDER_SENT');
CREATE INDEX IF NOT EXISTS idx_ppjb_processes_next_reminder ON ppjb_developer_processes(next_reminder_date) WHERE next_reminder_date IS NOT NULL;

-- PPJB Documents indexes
CREATE INDEX IF NOT EXISTS idx_ppjb_documents_process_id ON ppjb_documents(ppjb_process_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_documents_dossier_id ON ppjb_documents(dossier_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_documents_customer_id ON ppjb_documents(customer_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_documents_type ON ppjb_documents(document_type);
CREATE INDEX IF NOT EXISTS idx_ppjb_documents_status ON ppjb_documents(status);
CREATE INDEX IF NOT EXISTS idx_ppjb_documents_verification ON ppjb_documents(verification_status);

-- PPJB Reminders indexes
CREATE INDEX IF NOT EXISTS idx_ppjb_reminders_process_id ON ppjb_reminders(ppjb_process_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_reminders_type ON ppjb_reminders(reminder_type);
CREATE INDEX IF NOT EXISTS idx_ppjb_reminders_status ON ppjb_reminders(status);
CREATE INDEX IF NOT EXISTS idx_ppjb_reminders_scheduled_date ON ppjb_reminders(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_ppjb_reminders_pending ON ppjb_reminders(status) WHERE status = 'SCHEDULED';

-- PPJB Invitations indexes
CREATE INDEX IF NOT EXISTS idx_ppjb_invitations_process_id ON ppjb_invitations(ppjb_process_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_invitations_dossier_id ON ppjb_invitations(dossier_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_invitations_customer_id ON ppjb_invitations(customer_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_invitations_status ON ppjb_invitations(attendance_status);
CREATE INDEX IF NOT EXISTS idx_ppjb_invitations_date ON ppjb_invitations(invitation_date);
CREATE INDEX IF NOT EXISTS idx_ppjb_invitations_number ON ppjb_invitations(invitation_number);

-- PPJB Audit Logs indexes
CREATE INDEX IF NOT EXISTS idx_ppjb_audit_process_id ON ppjb_audit_logs(ppjb_process_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_audit_dossier_id ON ppjb_audit_logs(dossier_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_audit_customer_id ON ppjb_audit_logs(customer_id);
CREATE INDEX IF NOT EXISTS idx_ppjb_audit_action ON ppjb_audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_ppjb_audit_timestamp ON ppjb_audit_logs(timestamp DESC);

-- =====================================================
-- TRIGGERS AND FUNCTIONS
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_ppjb_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at trigger to PPJB tables
CREATE TRIGGER update_ppjb_processes_updated_at BEFORE UPDATE ON ppjb_developer_processes FOR EACH ROW EXECUTE FUNCTION update_ppjb_updated_at();
CREATE TRIGGER update_ppjb_documents_updated_at BEFORE UPDATE ON ppjb_documents FOR EACH ROW EXECUTE FUNCTION update_ppjb_updated_at();
CREATE TRIGGER update_ppjb_reminders_updated_at BEFORE UPDATE ON ppjb_reminders FOR EACH ROW EXECUTE FUNCTION update_ppjb_updated_at();
CREATE TRIGGER update_ppjb_invitations_updated_at BEFORE UPDATE ON ppjb_invitations FOR EACH ROW EXECUTE FUNCTION update_ppjb_updated_at();

-- Function to log PPJB changes
CREATE OR REPLACE FUNCTION log_ppjb_changes()
RETURNS TRIGGER AS $$
BEGIN
    -- Log to audit table
    INSERT INTO ppjb_audit_logs (
        ppjb_process_id,
        dossier_id,
        customer_id,
        action,
        entity_type,
        entity_id,
        old_values,
        new_values,
        timestamp
    ) VALUES (
        COALESCE(NEW.id, OLD.id),
        COALESCE(NEW.dossier_id, OLD.dossier_id),
        COALESCE(NEW.customer_id, OLD.customer_id),
        CASE
            WHEN TG_OP = 'INSERT' THEN 'CREATE'
            WHEN TG_OP = 'UPDATE' THEN 'UPDATE'
            WHEN TG_OP = 'DELETE' THEN 'DELETE'
        END,
        TG_TABLE_NAME,
        COALESCE(NEW.id, OLD.id),
        CASE
            WHEN TG_OP = 'UPDATE' THEN row_to_json(OLD)
            ELSE NULL
        END,
        CASE
            WHEN TG_OP IN ('INSERT', 'UPDATE') THEN row_to_json(NEW)
            ELSE NULL
        END,
        NOW()
    );
    
    RETURN COALESCE(NEW, OLD);
END;
$$ language 'plpgsql';

-- Apply audit trigger to PPJB tables
CREATE TRIGGER log_ppjb_processes_changes AFTER INSERT OR UPDATE OR DELETE ON ppjb_developer_processes FOR EACH ROW EXECUTE FUNCTION log_ppjb_changes();
CREATE TRIGGER log_ppjb_documents_changes AFTER INSERT OR UPDATE OR DELETE ON ppjb_documents FOR EACH ROW EXECUTE FUNCTION log_ppjb_changes();
CREATE TRIGGER log_ppjb_invitations_changes AFTER INSERT OR UPDATE OR DELETE ON ppjb_invitations FOR EACH ROW EXECUTE FUNCTION log_ppjb_changes();

-- Function to check PPJB expiry
CREATE OR REPLACE FUNCTION check_ppjb_expiry()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if PPJB has expired
    IF NEW.status != 'CANCELLED' AND NEW.status != 'COMPLETED' THEN
        IF NOW() > NEW.expiry_date THEN
            -- Auto-cancel PPJB
            UPDATE ppjb_developer_processes 
            SET 
                status = 'CANCELLED',
                cancellation_reason = 'AUTO_CANCELLED_EXPIRED',
                auto_cancelled = true,
                updated_at = NOW()
            WHERE id = NEW.id;
            
            -- Update unit status to available
            UPDATE unit_properties 
            SET status = 'AVAILABLE', updated_at = NOW()
            WHERE id = NEW.unit_property_id;
            
            -- Update dossier status
            UPDATE kpr_dossiers 
            SET status = 'CANCELLED', updated_at = NOW()
            WHERE id = NEW.dossier_id;
            
            -- Log auto-cancellation
            INSERT INTO ppjb_audit_logs (
                ppjb_process_id,
                dossier_id,
                customer_id,
                action,
                entity_type,
                entity_id,
                new_values,
                timestamp
            ) VALUES (
                NEW.id,
                NEW.dossier_id,
                NEW.customer_id,
                'AUTO_CANCEL',
                'PPJB_PROCESS',
                NEW.id,
                jsonb_build_object(
                    'reason', 'AUTO_CANCELLED_EXPIRED',
                    'expiry_date', NEW.expiry_date,
                    'cancelled_at', NOW()
                ),
                NOW()
            );
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply expiry check trigger
CREATE TRIGGER check_ppjb_expiry_trigger 
    AFTER UPDATE ON ppjb_developer_processes 
    FOR EACH ROW EXECUTE FUNCTION check_ppjb_expiry();

-- =====================================================
-- VIEWS FOR REPORTING
-- =====================================================

-- PPJB Summary View
CREATE OR REPLACE VIEW ppjb_summary AS
SELECT 
    pp.id,
    pp.dossier_id,
    pp.customer_id,
    pp.unit_property_id,
    pp.ppjb_type,
    pp.status,
    pp.scheduled_date,
    pp.expiry_date,
    pp.reminder_count,
    pp.actual_ppjb_date,
    kd.application_number,
    up.project_name,
    up.block,
    up.unit_number,
    up.price as unit_price,
    kd.loan_amount,
    kd.monthly_income,
    up.full_name as customer_name,
    up.phone as customer_phone,
    CASE 
        WHEN NOW() > pp.expiry_date THEN 'EXPIRED'
        WHEN pp.status = 'CANCELLED' THEN 'CANCELLED'
        WHEN pp.status = 'COMPLETED' THEN 'COMPLETED'
        WHEN pp.reminder_count >= pp.max_reminders THEN 'MAX_REMINDERS'
        ELSE 'ACTIVE'
    END as current_status,
    EXTRACT(EPOCH FROM (pp.expiry_date - NOW())) / (24 * 3600) as days_remaining,
    EXTRACT(EPOCH FROM (NOW() - pp.scheduled_date)) / (24 * 3600) as days_since_scheduled
FROM ppjb_developer_processes pp
JOIN kpr_dossiers kd ON pp.dossier_id = kd.id
JOIN unit_properties up ON pp.unit_property_id = up.id
JOIN user_profiles upf ON pp.customer_id = upf.id;

-- PPJB Performance View
CREATE OR REPLACE VIEW ppjb_performance AS
SELECT 
    ppjb_type,
    status,
    COUNT(*) as total_count,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed_count,
    COUNT(*) FILTER (WHERE status = 'CANCELLED') as cancelled_count,
    COUNT(*) FILTER (WHERE status = 'SCHEDULED') as scheduled_count,
    ROUND(AVG(EXTRACT(EPOCH FROM (actual_ppjb_date - scheduled_date)) / (24 * 3600)), 2) as avg_days_to_complete,
    ROUND(AVG(reminder_count), 2) as avg_reminder_count,
    ROUND(COUNT(*) FILTER (WHERE status = 'COMPLETED') * 100.0 / COUNT(*), 2) as completion_rate
FROM ppjb_developer_processes
GROUP BY ppjb_type, status;

-- =====================================================
-- SAMPLE DATA INSERTION
-- =====================================================

-- Insert sample PPJB process
INSERT INTO ppjb_developer_processes (
    dossier_id,
    customer_id,
    unit_property_id,
    ppjb_type,
    status,
    scheduled_date,
    expiry_date,
    reminder_count,
    max_reminders,
    sla_days
) VALUES
(
    '550e8400-e29b-41d4-a716-446655440000',
    '550e8400-e29b-41d4-a716-446655440001',
    '550e8400-e29b-41d4-a716-446655440002',
    'KPR',
    'SCHEDULED',
    NOW() + INTERVAL '7 days',
    NOW() + INTERVAL '37 days',
    0,
    2,
    30
) ON CONFLICT DO NOTHING;

-- =====================================================
-- COMPLETION VERIFICATION
-- =====================================================

-- Verify all tables are created
DO $$
DECLARE
    table_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO table_count 
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name LIKE 'ppjb_%';
    
    IF table_count >= 5 THEN
        RAISE NOTICE 'PPJB Developer Database: 100% Complete - % tables created', table_count;
    ELSE
        RAISE NOTICE 'PPJB Developer Database: Incomplete - only % tables created', table_count;
    END IF;
END $$;
