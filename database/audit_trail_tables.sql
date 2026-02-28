-- Audit Trail tables for Phase 18: Complete Activity Logging

-- Audit Trail table
CREATE TABLE IF NOT EXISTS audit_trail (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL, -- 'CREATE', 'UPDATE', 'DELETE', 'READ', 'LOGIN', 'LOGOUT', etc.
    entity_type VARCHAR(50) NOT NULL, -- 'DOSSIER', 'DOCUMENT', 'USER', 'UNIT', 'PAYMENT', etc.
    entity_id VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    ip_address INET,
    user_agent TEXT,
    old_values JSONB,
    new_values JSONB,
    severity VARCHAR(20) DEFAULT 'LOW', -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    category VARCHAR(50), -- 'DOSSIER_MANAGEMENT', 'DOCUMENT_MANAGEMENT', 'SECURITY', etc.
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Audit Report table
CREATE TABLE IF NOT EXISTS audit_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_type VARCHAR(50) NOT NULL, -- 'COMPLIANCE', 'SECURITY', 'PERFORMANCE', etc.
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    total_actions INTEGER NOT NULL,
    high_severity_actions INTEGER DEFAULT 0,
    security_actions INTEGER DEFAULT 0,
    data_change_actions INTEGER DEFAULT 0,
    compliance_score DECIMAL(5,2),
    report_data JSONB, -- Detailed report data
    generated_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_audit_trail_user_id ON audit_trail(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_trail_action ON audit_trail(action);
CREATE INDEX IF NOT EXISTS idx_audit_trail_entity_type ON audit_trail(entity_type);
CREATE INDEX IF NOT EXISTS idx_audit_trail_entity_id ON audit_trail(entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_trail_severity ON audit_trail(severity);
CREATE INDEX IF NOT EXISTS idx_audit_trail_category ON audit_trail(category);
CREATE INDEX IF NOT EXISTS idx_audit_trail_created_at ON audit_trail(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_trail_ip_address ON audit_trail(ip_address);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_audit_trail_user_action ON audit_trail(user_id, action);
CREATE INDEX IF NOT EXISTS idx_audit_trail_entity_action ON audit_trail(entity_type, entity_id, action);
CREATE INDEX IF NOT EXISTS idx_audit_trail_severity_created ON audit_trail(severity, created_at);

CREATE INDEX IF NOT EXISTS idx_audit_reports_type ON audit_reports(report_type);
CREATE INDEX IF NOT EXISTS idx_audit_reports_date_range ON audit_reports(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_audit_reports_generated_at ON audit_reports(generated_at);

-- RLS Policies for audit trail
ALTER TABLE audit_trail ENABLE ROW LEVEL SECURITY;

-- System can manage audit logs
CREATE POLICY "System can manage audit logs" ON audit_trail
    FOR ALL USING (auth.jwt() ->> 'role' = 'SYSTEM');

-- Admin can read audit logs
CREATE POLICY "Admin can read audit logs" ON audit_trail
    FOR SELECT USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can read audit logs
CREATE POLICY "BOD can read audit logs" ON audit_trail
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can read their own audit logs
CREATE POLICY "Users can read own audit logs" ON audit_trail
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- RLS Policies for audit reports
ALTER TABLE audit_reports ENABLE ROW LEVEL SECURITY;

-- Admin can manage audit reports
CREATE POLICY "Admin can manage audit reports" ON audit_reports
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can read audit reports
CREATE POLICY "BOD can read audit reports" ON audit_reports
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Function to log audit action
CREATE OR REPLACE FUNCTION log_audit_action(
    p_user_id UUID,
    p_action VARCHAR(50),
    p_entity_type VARCHAR(50),
    p_entity_id VARCHAR(255),
    p_description TEXT,
    p_ip_address INET DEFAULT NULL,
    p_user_agent TEXT DEFAULT NULL,
    p_old_values JSONB DEFAULT NULL,
    p_new_values JSONB DEFAULT NULL,
    p_severity VARCHAR(20) DEFAULT 'LOW',
    p_category VARCHAR(50) DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    audit_id UUID;
BEGIN
    INSERT INTO audit_trail (
        user_id, action, entity_type, entity_id, description,
        ip_address, user_agent, old_values, new_values, severity, category
    ) VALUES (
        p_user_id, p_action, p_entity_type, p_entity_id, p_description,
        p_ip_address, p_user_agent, p_old_values, p_new_values, p_severity, p_category
    ) RETURNING id INTO audit_id;
    
    RETURN audit_id;
END;
$$ LANGUAGE plpgsql;

-- Function to log data change
CREATE OR REPLACE FUNCTION log_data_change(
    p_user_id UUID,
    p_entity_type VARCHAR(50),
    p_entity_id VARCHAR(255),
    p_field_name VARCHAR(100),
    p_old_value TEXT,
    p_new_value TEXT,
    p_description TEXT,
    p_ip_address INET DEFAULT NULL,
    p_user_agent TEXT DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    old_values JSONB;
    new_values JSONB;
BEGIN
    -- Create JSON objects for old and new values
    IF p_old_value IS NOT NULL THEN
        old_values := jsonb_build_object(p_field_name, p_old_value);
    END IF;
    
    IF p_new_value IS NOT NULL THEN
        new_values := jsonb_build_object(p_field_name, p_new_value);
    END IF;
    
    RETURN log_audit_action(
        p_user_id, 'UPDATE', p_entity_type, p_entity_id, p_description,
        p_ip_address, p_user_agent, old_values, new_values, 'MEDIUM', 'DATA_CHANGE'
    );
END;
$$ LANGUAGE plpgsql;

-- Function to get audit statistics
CREATE OR REPLACE FUNCTION get_audit_statistics(
    p_start_date TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    p_end_date TIMESTAMP WITH TIME ZONE DEFAULT NULL
)
RETURNS TABLE (
    total_logs BIGINT,
    unique_users BIGINT,
    unique_entities BIGINT,
    high_severity_logs BIGINT,
    security_events BIGINT,
    data_changes BIGINT,
    top_actions JSONB,
    top_entities JSONB,
    top_users JSONB
) AS $$
BEGIN
    RETURN QUERY
    WITH stats AS (
        SELECT 
            COUNT(*) as total_logs,
            COUNT(DISTINCT user_id) as unique_users,
            COUNT(DISTINCT entity_id) as unique_entities,
            COUNT(*) FILTER (WHERE severity IN ('HIGH', 'CRITICAL')) as high_severity_logs,
            COUNT(*) FILTER (WHERE category = 'SECURITY') as security_events,
            COUNT(*) FILTER (WHERE category = 'DATA_CHANGE') as data_changes
        FROM audit_trail
        WHERE 
            (p_start_date IS NULL OR created_at >= p_start_date)
            AND (p_end_date IS NULL OR created_at <= p_end_date)
    ),
    top_actions AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'action', action,
                'count', action_count
            ) ORDER BY action_count DESC
        ) as top_actions
        FROM (
            SELECT action, COUNT(*) as action_count
            FROM audit_trail
            WHERE 
                (p_start_date IS NULL OR created_at >= p_start_date)
                AND (p_end_date IS NULL OR created_at <= p_end_date)
            GROUP BY action
            ORDER BY action_count DESC
            LIMIT 10
        ) ta
    ),
    top_entities AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'entity_type', entity_type,
                'entity_id', entity_id,
                'count', entity_count
            ) ORDER BY entity_count DESC
        ) as top_entities
        FROM (
            SELECT entity_type, entity_id, COUNT(*) as entity_count
            FROM audit_trail
            WHERE 
                (p_start_date IS NULL OR created_at >= p_start_date)
                AND (p_end_date IS NULL OR created_at <= p_end_date)
            GROUP BY entity_type, entity_id
            ORDER BY entity_count DESC
            LIMIT 10
        ) te
    ),
    top_users AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'user_id', user_id,
                'count', user_count
            ) ORDER BY user_count DESC
        ) as top_users
        FROM (
            SELECT user_id, COUNT(*) as user_count
            FROM audit_trail
            WHERE 
                (p_start_date IS NULL OR created_at >= p_start_date)
                AND (p_end_date IS NULL OR created_at <= p_end_date)
            GROUP BY user_id
            ORDER BY user_count DESC
            LIMIT 10
        ) tu
    )
    SELECT 
        s.total_logs,
        s.unique_users,
        s.unique_entities,
        s.high_severity_logs,
        s.security_events,
        s.data_changes,
        ta.top_actions,
        te.top_entities,
        tu.top_users
    FROM stats s, top_actions ta, top_entities te, top_users tu;
END;
$$ LANGUAGE plpgsql;

-- Function to generate compliance report
CREATE OR REPLACE FUNCTION generate_compliance_report(
    p_report_type VARCHAR(50),
    p_start_date TIMESTAMP WITH TIME ZONE,
    p_end_date TIMESTAMP WITH TIME ZONE,
    p_generated_by UUID DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    report_id UUID;
    total_actions INTEGER;
    high_severity_actions INTEGER;
    security_actions INTEGER;
    data_change_actions INTEGER;
    compliance_score DECIMAL(5,2);
    report_data JSONB;
BEGIN
    -- Get statistics
    SELECT 
        COUNT(*) as total_actions,
        COUNT(*) FILTER (WHERE severity IN ('HIGH', 'CRITICAL')) as high_severity_actions,
        COUNT(*) FILTER (WHERE category = 'SECURITY') as security_actions,
        COUNT(*) FILTER (WHERE category = 'DATA_CHANGE') as data_change_actions
    INTO total_actions, high_severity_actions, security_actions, data_change_actions
    FROM audit_trail
    WHERE created_at BETWEEN p_start_date AND p_end_date;
    
    -- Calculate compliance score
    compliance_score := CASE 
        WHEN total_actions = 0 THEN 100.0
        ELSE GREATEST(0.0, 100.0 - ((high_severity_actions::DECIMAL / total_actions::DECIMAL) * 50))
    END;
    
    -- Create report data
    report_data := jsonb_build_object(
        'total_actions', total_actions,
        'high_severity_actions', high_severity_actions,
        'security_actions', security_actions,
        'data_change_actions', data_change_actions,
        'compliance_score', compliance_score
    );
    
    -- Insert report
    INSERT INTO audit_reports (
        report_type, start_date, end_date, total_actions,
        high_severity_actions, security_actions, data_change_actions,
        compliance_score, report_data, generated_by
    ) VALUES (
        p_report_type, p_start_date, p_end_date, total_actions,
        high_severity_actions, security_actions, data_change_actions,
        compliance_score, report_data, p_generated_by
    ) RETURNING id INTO report_id;
    
    RETURN report_id;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old audit logs
CREATE OR REPLACE FUNCTION cleanup_audit_logs(
    p_days_old INTEGER DEFAULT 365
)
RETURNS INTEGER AS $$
BEGIN
    DELETE FROM audit_trail
    WHERE created_at < NOW() - INTERVAL '1 day' * p_days_old;
    
    RETURN ROW_COUNT;
END;
$$ LANGUAGE plpgsql;

-- Function to detect suspicious activity
CREATE OR REPLACE FUNCTION detect_suspicious_activity(
    p_user_id UUID,
    p_time_window_minutes INTEGER DEFAULT 60
)
RETURNS TABLE (
    is_suspicious BOOLEAN,
    risk_score DECIMAL(5,2),
    risk_factors JSONB
) AS $$
BEGIN
    RETURN QUERY
    WITH user_activity AS (
        SELECT 
            COUNT(*) as total_actions,
            COUNT(*) FILTER (WHERE severity IN ('HIGH', 'CRITICAL')) as high_severity_actions,
            COUNT(*) FILTER (WHERE category = 'SECURITY') as security_actions,
            COUNT(DISTINCT ip_address) as unique_ips,
            COUNT(*) FILTER (WHERE action = 'LOGIN') as login_attempts,
            COUNT(*) FILTER (WHERE action = 'DELETE') as delete_actions
        FROM audit_trail
        WHERE user_id = p_user_id
        AND created_at >= NOW() - INTERVAL '1 minute' * p_time_window_minutes
    ),
    risk_factors AS (
        SELECT jsonb_build_object(
            'high_severity_ratio', CASE 
                WHEN ua.total_actions > 0 THEN (ua.high_severity_actions::DECIMAL / ua.total_actions::DECIMAL) * 100 
                ELSE 0 
            END,
            'security_action_ratio', CASE 
                WHEN ua.total_actions > 0 THEN (ua.security_actions::DECIMAL / ua.total_actions::DECIMAL) * 100 
                ELSE 0 
            END,
            'multiple_ips', ua.unique_ips > 1,
            'excessive_logins', ua.login_attempts > 5,
            'excessive_deletes', ua.delete_actions > 10
        ) as risk_factors
        FROM user_activity ua
    ),
    risk_score AS (
        SELECT 
            GREATEST(
                (rf.high_severity_ratio * 0.4),
                (rf.security_action_ratio * 0.3),
                CASE WHEN rf.multiple_ips THEN 20 ELSE 0 END,
                CASE WHEN rf.excessive_logins THEN 15 ELSE 0 END,
                CASE WHEN rf.excessive_deletes THEN 25 ELSE 0 END
            ) as score
        FROM risk_factors rf
    )
    SELECT 
        rs.score >= 30 as is_suspicious,
        rs.score as risk_score,
        rf.risk_factors
    FROM risk_score rs, risk_factors rf;
END;
$$ LANGUAGE plpgsql;

-- Trigger for automatic audit logging
CREATE OR REPLACE FUNCTION trigger_audit_logging()
RETURNS TRIGGER AS $$
BEGIN
    -- This trigger would be used on critical tables to automatically log changes
    -- Implementation depends on specific table structure
    
    -- Example for kpr_dossiers table
    IF TG_TABLE_NAME = 'kpr_dossiers' THEN
        IF TG_OP = 'INSERT' THEN
            PERFORM log_audit_action(
                COALESCE(NEW.user_id, 'system'),
                'CREATE',
                'DOSSIER',
                NEW.id,
                'Dossier created: ' || COALESCE(NEW.notes, ''),
                NULL,
                NULL,
                NULL,
                to_jsonb(NEW),
                'MEDIUM',
                'DOSSIER_MANAGEMENT'
            );
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM log_audit_action(
                COALESCE(NEW.user_id, 'system'),
                'UPDATE',
                'DOSSIER',
                NEW.id,
                'Dossier updated: ' || COALESCE(NEW.notes, ''),
                NULL,
                NULL,
                to_jsonb(OLD),
                to_jsonb(NEW),
                'MEDIUM',
                'DOSSIER_MANAGEMENT'
            );
        ELSIF TG_OP = 'DELETE' THEN
            PERFORM log_audit_action(
                COALESCE(OLD.user_id, 'system'),
                'DELETE',
                'DOSSIER',
                OLD.id,
                'Dossier deleted',
                NULL,
                NULL,
                to_jsonb(OLD),
                NULL,
                'HIGH',
                'DOSSIER_MANAGEMENT'
            );
        END IF;
    END IF;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Schedule cleanup function (run daily)
-- SELECT cron.schedule('cleanup-audit-logs', '0 2 * * *', 'SELECT cleanup_audit_logs();');

-- Schedule suspicious activity detection (run every hour)
-- SELECT cron.schedule('detect-suspicious-activity', '0 * * * *', 'SELECT * FROM detect_suspicious_activity(NULL, 60) WHERE is_suspicious = TRUE;');
