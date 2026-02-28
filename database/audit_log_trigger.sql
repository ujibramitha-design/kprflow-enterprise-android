-- =====================================================
-- KPRFLOW ENTERPRISE - AUDIT LOG SYSTEM
-- PostgreSQL Trigger Implementation for Anti-Tamper Protection
-- =====================================================

-- 1. CREATE AUDIT LOG TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS "AuditLog" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES "UserProfile"(id), -- Siapa
    action TEXT,                               -- Melakukan apa (INSERT/UPDATE/DELETE)
    table_name TEXT,                           -- Di tabel mana
    record_id UUID,                            -- Baris mana yang diubah
    old_data JSONB,                            -- Data sebelum diubah
    new_data JSONB,                            -- Data sesudah diubah
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    ip_address INET,                           -- IP Address untuk security
    user_agent TEXT,                           -- User Agent untuk tracking
    session_id UUID,                           -- Session ID untuk audit trail
    is_critical BOOLEAN DEFAULT FALSE,          -- Flag untuk perubahan kritis
    alert_sent BOOLEAN DEFAULT FALSE           -- Flag untuk notifikasi terkirim
);

-- 2. CREATE INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON "AuditLog"(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_table_name ON "AuditLog"(table_name);
CREATE INDEX IF NOT EXISTS idx_audit_log_record_id ON "AuditLog"(record_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON "AuditLog"(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_is_critical ON "AuditLog"(is_critical);
CREATE INDEX IF NOT EXISTS idx_audit_log_session_id ON "AuditLog"(session_id);

-- 3. CREATE FUNCTION TO GET CURRENT USER CONTEXT
-- =====================================================

CREATE OR REPLACE FUNCTION get_current_user_context()
RETURNS TABLE(user_id UUID, ip_address INET, user_agent TEXT, session_id UUID) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COALESCE(current_setting('app.current_user_id', true)::UUID, NULL::UUID) as user_id,
        COALESCE(current_setting('app.current_ip_address', true)::INET, NULL::INET) as ip_address,
        COALESCE(current_setting('app.current_user_agent', true), NULL::TEXT) as user_agent,
        COALESCE(current_setting('app.current_session_id', true)::UUID, NULL::UUID) as session_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. CREATE AUDIT TRIGGER FUNCTION
-- =====================================================

CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
DECLARE
    user_context RECORD;
    old_data_json JSONB;
    new_data_json JSONB;
    is_critical_change BOOLEAN := FALSE;
    table_name TEXT;
    record_id UUID;
BEGIN
    -- Get current user context
    SELECT * INTO user_context FROM get_current_user_context();
    
    -- Determine table name and record ID
    table_name := TG_TABLE_NAME;
    
    -- Extract record ID based on table
    IF TG_OP = 'DELETE' THEN
        record_id := COALESCE((OLD.id)::UUID, gen_random_uuid());
    ELSIF TG_OP = 'UPDATE' OR TG_OP = 'INSERT' THEN
        record_id := COALESCE((NEW.id)::UUID, gen_random_uuid());
    END IF;
    
    -- Convert row data to JSON
    IF TG_OP = 'DELETE' THEN
        old_data_json := to_jsonb(OLD);
        new_data_json := NULL;
    ELSIF TG_OP = 'INSERT' THEN
        old_data_json := NULL;
        new_data_json := to_jsonb(NEW);
    ELSIF TG_OP = 'UPDATE' THEN
        old_data_json := to_jsonb(OLD);
        new_data_json := to_jsonb(NEW);
    END IF;
    
    -- Check for critical changes
    is_critical_change := check_critical_change(table_name, old_data_json, new_data_json);
    
    -- Insert audit log entry
    INSERT INTO "AuditLog" (
        user_id,
        action,
        table_name,
        record_id,
        old_data,
        new_data,
        ip_address,
        user_agent,
        session_id,
        is_critical
    ) VALUES (
        user_context.user_id,
        TG_OP,
        table_name,
        record_id,
        old_data_json,
        new_data_json,
        user_context.ip_address,
        user_context.user_agent,
        user_context.session_id,
        is_critical_change
    );
    
    -- If critical change, trigger alert
    IF is_critical_change THEN
        PERFORM trigger_critical_alert(table_name, record_id, user_context.user_id, old_data_json, new_data_json);
    END IF;
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. CREATE FUNCTION TO CHECK CRITICAL CHANGES
-- =====================================================

CREATE OR REPLACE FUNCTION check_critical_change(
    table_name TEXT,
    old_data JSONB,
    new_data JSONB
)
RETURNS BOOLEAN AS $$
DECLARE
    is_critical BOOLEAN := FALSE;
BEGIN
    -- Check for critical changes in KPRDossier table
    IF table_name = 'KPRDossier' THEN
        -- Status changes are critical
        IF old_data IS NOT NULL AND new_data IS NOT NULL THEN
            IF (old_data->>'status') IS DISTINCT FROM (new_data->>'status') THEN
                is_critical := TRUE;
            END IF;
            
            -- Financial data changes are critical
            IF (old_data->>'loan_amount') IS DISTINCT FROM (new_data->>'loan_amount') OR
               (old_data->>'down_payment') IS DISTINCT FROM (new_data->>'down_payment') OR
               (old_data->>'interest_rate') IS DISTINCT FROM (new_data->>'interest_rate') THEN
                is_critical := TRUE;
            END IF;
        END IF;
    END IF;
    
    -- Check for critical changes in UnitProperty table
    IF table_name = 'UnitProperty' THEN
        IF old_data IS NOT NULL AND new_data IS NOT NULL THEN
            -- Price changes are critical
            IF (old_data->>'price') IS DISTINCT FROM (new_data->>'price') THEN
                is_critical := TRUE;
            END IF;
            
            -- Status changes are critical
            IF (old_data->>'status') IS DISTINCT FROM (new_data->>'status') THEN
                is_critical := TRUE;
            END IF;
        END IF;
    END IF;
    
    -- Check for critical changes in FinancialTransaction table
    IF table_name = 'FinancialTransaction' THEN
        IF old_data IS NOT NULL AND new_data IS NOT NULL THEN
            -- Amount changes are critical
            IF (old_data->>'amount') IS DISTINCT FROM (new_data->>'amount') THEN
                is_critical := TRUE;
            END IF;
            
            -- Status changes are critical
            IF (old_data->>'status') IS DISTINCT FROM (new_data->>'status') THEN
                is_critical := TRUE;
            END IF;
        END IF;
    END IF;
    
    -- Check for critical changes in UserProfile table (role changes)
    IF table_name = 'UserProfile' THEN
        IF old_data IS NOT NULL AND new_data IS NOT NULL THEN
            -- Role changes are critical
            IF (old_data->>'role') IS DISTINCT FROM (new_data->>'role') THEN
                is_critical := TRUE;
            END IF;
            
            -- Status changes are critical
            IF (old_data->>'status') IS DISTINCT FROM (new_data->>'status') THEN
                is_critical := TRUE;
            END IF;
        END IF;
    END IF;
    
    RETURN is_critical;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. CREATE FUNCTION TO TRIGGER CRITICAL ALERTS
-- =====================================================

CREATE OR REPLACE FUNCTION trigger_critical_alert(
    table_name TEXT,
    record_id UUID,
    user_id UUID,
    old_data JSONB,
    new_data JSONB
)
RETURNS VOID AS $$
DECLARE
    alert_message TEXT;
    alert_type TEXT := 'CRITICAL_CHANGE';
    affected_users JSONB;
BEGIN
    -- Build alert message
    alert_message := format(
        '🚨 CRITICAL CHANGE DETECTED%n' ||
        'Table: %s%n' ||
        'Record ID: %s%n' ||
        'User: %s%n' ||
        'Time: %s%n' ||
        'Change: %s',
        table_name,
        record_id,
        COALESCE((SELECT name FROM "UserProfile" WHERE id = user_id), 'Unknown'),
        NOW(),
        build_change_description(old_data, new_data)
    );
    
    -- Get affected users (BOD and Finance for financial changes)
    affected_users := get_affected_users_for_alert(table_name, old_data, new_data);
    
    -- Insert alert notification
    INSERT INTO "Notification" (
        id,
        type,
        message,
        recipient_ids,
        data,
        is_critical,
        created_at,
        read_at
    ) VALUES (
        gen_random_uuid(),
        alert_type,
        alert_message,
        (SELECT array_agg(id) FROM jsonb_array_elements_text(affected_users->'user_ids')::TEXT[]),
        jsonb_build_object(
            'table_name', table_name,
            'record_id', record_id,
            'user_id', user_id,
            'old_data', old_data,
            'new_data', new_data,
            'timestamp', NOW()
        ),
        TRUE,
        NOW(),
        NULL
    );
    
    -- Update audit log to mark alert as sent
    UPDATE "AuditLog" 
    SET alert_sent = TRUE 
    WHERE table_name = table_name 
    AND record_id = record_id 
    AND created_at = (
        SELECT created_at FROM "AuditLog" 
        WHERE table_name = table_name 
        AND record_id = record_id 
        ORDER BY created_at DESC 
        LIMIT 1
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 7. CREATE FUNCTION TO BUILD CHANGE DESCRIPTION
-- =====================================================

CREATE OR REPLACE FUNCTION build_change_description(
    old_data JSONB,
    new_data JSONB
)
RETURNS TEXT AS $$
DECLARE
    description TEXT := '';
    field_name TEXT;
    old_value TEXT;
    new_value TEXT;
BEGIN
    -- Compare fields and build description
    IF old_data IS NOT NULL AND new_data IS NOT NULL THEN
        FOR field_name IN SELECT key FROM jsonb_object_keys(old_data) UNION SELECT key FROM jsonb_object_keys(new_data)
        LOOP
            old_value := COALESCE(old_data->>field_name, 'NULL');
            new_value := COALESCE(new_data->>field_name, 'NULL');
            
            IF old_value IS DISTINCT FROM new_value THEN
                description := description || format('%s: %s → %s%n', field_name, old_value, new_value);
            END IF;
        END LOOP;
    END IF;
    
    RETURN COALESCE(description, 'No changes detected');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 8. CREATE FUNCTION TO GET AFFECTED USERS FOR ALERTS
-- =====================================================

CREATE OR REPLACE FUNCTION get_affected_users_for_alert(
    table_name TEXT,
    old_data JSONB,
    new_data JSONB
)
RETURNS JSONB AS $$
DECLARE
    affected_users JSONB := '{}';
    user_ids TEXT[] := '{}';
BEGIN
    -- For financial changes, notify BOD and Finance
    IF table_name IN ('KPRDossier', 'FinancialTransaction', 'UnitProperty') THEN
        -- Get BOD users
        SELECT array_agg(id) INTO user_ids FROM "UserProfile" WHERE role = 'BOD' AND status = 'Active';
        
        -- Get Finance users for financial transactions
        IF table_name = 'FinancialTransaction' OR table_name = 'KPRDossier' THEN
            SELECT array_agg(id) INTO user_ids FROM "UserProfile" WHERE department = 'Finance' AND status = 'Active';
        END IF;
        
        -- Get Legal users for KPR changes
        IF table_name = 'KPRDossier' THEN
            SELECT array_agg(id) INTO user_ids FROM "UserProfile" WHERE department = 'Legal' AND status = 'Active';
        END IF;
    END IF;
    
    affected_users := jsonb_build_object('user_ids', user_ids);
    
    RETURN affected_users;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 9. CREATE TRIGGERS FOR ALL CRITICAL TABLES
-- =====================================================

-- KPRDossier table trigger
DROP TRIGGER IF EXISTS kpr_dossier_audit_trigger ON "KPRDossier";
CREATE TRIGGER kpr_dossier_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON "KPRDossier"
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- UnitProperty table trigger
DROP TRIGGER IF EXISTS unit_property_audit_trigger ON "UnitProperty";
CREATE TRIGGER unit_property_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON "UnitProperty"
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- FinancialTransaction table trigger
DROP TRIGGER IF EXISTS financial_transaction_audit_trigger ON "FinancialTransaction";
CREATE TRIGGER financial_transaction_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON "FinancialTransaction"
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- UserProfile table trigger
DROP TRIGGER IF EXISTS user_profile_audit_trigger ON "UserProfile";
CREATE TRIGGER user_profile_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON "UserProfile"
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- Document table trigger
DROP TRIGGER IF EXISTS document_audit_trigger ON "Document";
CREATE TRIGGER document_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON "Document"
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- 10. CREATE RLS POLICIES FOR AUDIT LOG (READ-ONLY FOR MOST USERS)
-- =====================================================

-- Enable RLS
ALTER TABLE "AuditLog" ENABLE ROW LEVEL SECURITY;

-- Policy for BOD users (full access)
CREATE POLICY "BOD full access to AuditLog" ON "AuditLog"
    FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM "UserProfile" 
            WHERE id = auth.uid() 
            AND role = 'BOD' 
            AND status = 'Active'
        )
    );

-- Policy for Managers (read-only access)
CREATE POLICY "Managers read-only access to AuditLog" ON "AuditLog"
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM "UserProfile" 
            WHERE id = auth.uid() 
            AND role IN ('Manager', 'Team Lead') 
            AND status = 'Active'
        )
    );

-- Policy for users to see their own audit logs
CREATE POLICY "Users own audit logs" ON "AuditLog"
    FOR SELECT
    USING (user_id = auth.uid());

-- 11. CREATE VIEW FOR AUDIT LOG HISTORY
-- =====================================================

CREATE OR REPLACE VIEW "AuditLogHistory" AS
SELECT 
    al.id,
    al.created_at,
    up.name as user_name,
    up.role as user_role,
    up.department as user_department,
    al.action,
    al.table_name,
    al.record_id,
    al.old_data,
    al.new_data,
    al.ip_address,
    al.user_agent,
    al.is_critical,
    al.alert_sent,
    -- Extract meaningful information
    CASE 
        WHEN al.table_name = 'KPRDossier' THEN
            CASE 
                WHEN al.action = 'INSERT' THEN 'Created new KPR application'
                WHEN al.action = 'UPDATE' THEN 
                    CASE 
                        WHEN (al.old_data->>'status') IS DISTINCT FROM (al.new_data->>'status') THEN
                            format('Changed status from %s to %s', al.old_data->>'status', al.new_data->>'status')
                        WHEN (al.old_data->>'loan_amount') IS DISTINCT FROM (al.new_data->>'loan_amount') THEN
                            format('Changed loan amount from %s to %s', al.old_data->>'loan_amount', al.new_data->>'loan_amount')
                        ELSE 'Updated KPR application'
                    END
                WHEN al.action = 'DELETE' THEN 'Deleted KPR application'
                ELSE 'Modified KPR application'
            END
        WHEN al.table_name = 'UnitProperty' THEN
            CASE 
                WHEN al.action = 'INSERT' THEN 'Added new property unit'
                WHEN al.action = 'UPDATE' THEN 
                    CASE 
                        WHEN (al.old_data->>'price') IS DISTINCT FROM (al.new_data->>'price') THEN
                            format('Changed price from %s to %s', al.old_data->>'price', al.new_data->>'price')
                        WHEN (al.old_data->>'status') IS DISTINCT FROM (al.new_data->>'status') THEN
                            format('Changed status from %s to %s', al.old_data->>'status', al.new_data->>'status')
                        ELSE 'Updated property unit'
                    END
                WHEN al.action = 'DELETE' THEN 'Deleted property unit'
                ELSE 'Modified property unit'
            END
        WHEN al.table_name = 'FinancialTransaction' THEN
            CASE 
                WHEN al.action = 'INSERT' THEN 'Created new financial transaction'
                WHEN al.action = 'UPDATE' THEN 
                    CASE 
                        WHEN (al.old_data->>'status') IS DISTINCT FROM (al.new_data->>'status') THEN
                            format('Changed transaction status from %s to %s', al.old_data->>'status', al.new_data->>'status')
                        ELSE 'Updated financial transaction'
                    END
                WHEN al.action = 'DELETE' THEN 'Deleted financial transaction'
                ELSE 'Modified financial transaction'
            END
        ELSE format('%s %s record', al.action, al.table_name)
    END as description
FROM "AuditLog" al
LEFT JOIN "UserProfile" up ON al.user_id = up.id
ORDER BY al.created_at DESC;

-- 12. CREATE SECURITY FUNCTIONS
-- =====================================================

-- Function to check if user can modify audit logs
CREATE OR REPLACE FUNCTION can_modify_audit_logs()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM "UserProfile" 
        WHERE id = auth.uid() 
        AND role = 'BOD' 
        AND status = 'Active'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to check if user can view audit logs
CREATE OR REPLACE FUNCTION can_view_audit_logs()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM "UserProfile" 
        WHERE id = auth.uid() 
        AND role IN ('BOD', 'Manager', 'Team Lead') 
        AND status = 'Active'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 13. CREATE PROCEDURE TO SET USER CONTEXT
-- =====================================================

CREATE OR REPLACE PROCEDURE set_user_context(
    p_user_id UUID,
    p_ip_address INET DEFAULT NULL,
    p_user_agent TEXT DEFAULT NULL,
    p_session_id UUID DEFAULT NULL
) AS $$
BEGIN
    PERFORM set_config('app.current_user_id', p_user_id::TEXT, true);
    PERFORM set_config('app.current_ip_address', p_ip_address::TEXT, true);
    PERFORM set_config('app.current_user_agent', p_user_agent, true);
    PERFORM set_config('app.current_session_id', p_session_id::TEXT, true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- AUDIT LOG SYSTEM IMPLEMENTATION COMPLETE
-- =====================================================
