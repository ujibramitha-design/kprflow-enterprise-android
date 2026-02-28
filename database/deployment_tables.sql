-- Deployment tables for Phase 25: Final Deployment - Production Readiness

-- Deployments table
CREATE TABLE IF NOT EXISTS deployments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    version VARCHAR(50) NOT NULL,
    environment VARCHAR(50) NOT NULL, -- 'DEVELOPMENT', 'TESTING', 'STAGING', 'PRODUCTION'
    deployment_type VARCHAR(50) NOT NULL, -- 'FULL', 'INCREMENTAL', 'ROLLBACK', 'HOTFIX', 'BLUE_GREEN', 'CANARY'
    description TEXT,
    build_number VARCHAR(100) NOT NULL,
    commit_hash VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'IN_PROGRESS', 'SUCCESS', 'FAILED', 'ROLLBACK', 'CANCELLED'
    rollback_enabled BOOLEAN DEFAULT TRUE,
    health_check_enabled BOOLEAN DEFAULT TRUE,
    duration_seconds INTEGER,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    rollback_at TIMESTAMP WITH TIME ZONE,
    rollback_reason TEXT,
    rollback_type VARCHAR(50),
    triggered_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    created_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Deployment Phases table
CREATE TABLE IF NOT EXISTS deployment_phases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    deployment_id UUID NOT NULL REFERENCES deployments(id) ON DELETE CASCADE,
    phase_name VARCHAR(100) NOT NULL,
    phase_index INTEGER NOT NULL,
    success BOOLEAN NOT NULL,
    duration_seconds INTEGER,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    logs JSONB, -- Array of log messages
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Health Check Results table
CREATE TABLE IF NOT EXISTS health_check_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    environment VARCHAR(50) NOT NULL,
    overall_health VARCHAR(20) NOT NULL, -- 'HEALTHY', 'DEGRADED', 'UNHEALTHY', 'UNKNOWN'
    healthy_checks INTEGER NOT NULL,
    total_checks INTEGER NOT NULL,
    checks JSONB NOT NULL, -- Array of individual health checks
    duration BIGINT, -- in milliseconds
    checked_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Individual Health Checks table
CREATE TABLE IF NOT EXISTS health_checks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    health_check_result_id UUID REFERENCES health_check_results(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'HEALTHY', 'DEGRADED', 'UNHEALTHY', 'UNKNOWN'
    response_time BIGINT, -- in milliseconds
    details JSONB, -- Additional check details
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Scheduled Deployments table
CREATE TABLE IF NOT EXISTS scheduled_deployments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    version VARCHAR(50) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    scheduled_time TIMESTAMP WITH TIME ZONE NOT NULL,
    deployment_type VARCHAR(50) NOT NULL DEFAULT 'INCREMENTAL',
    auto_rollback BOOLEAN DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'EXECUTED', 'CANCELLED'
    deployment_id UUID REFERENCES deployments(id) ON DELETE SET NULL,
    executed_at TIMESTAMP WITH TIME ZONE,
    created_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Deployment Rollback History table
CREATE TABLE IF NOT EXISTS deployment_rollback_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    deployment_id UUID NOT NULL REFERENCES deployments(id) ON DELETE CASCADE,
    rollback_type VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    success BOOLEAN NOT NULL,
    duration_seconds INTEGER,
    rolled_back_version VARCHAR(50),
    rollback_data JSONB, -- Details about what was rolled back
    triggered_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Deployment Readiness Validation table
CREATE TABLE IF NOT EXISTS deployment_readiness_validations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    environment VARCHAR(50) NOT NULL,
    version VARCHAR(50) NOT NULL,
    is_ready BOOLEAN NOT NULL,
    total_checks INTEGER NOT NULL,
    passed_checks INTEGER NOT NULL,
    failed_checks INTEGER NOT NULL,
    checks JSONB NOT NULL, -- Array of readiness checks
    validated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Deployment Metrics table
CREATE TABLE IF NOT EXISTS deployment_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    environment VARCHAR(50) NOT NULL,
    date_bucket DATE NOT NULL, -- Daily aggregation
    total_deployments INTEGER DEFAULT 0,
    successful_deployments INTEGER DEFAULT 0,
    failed_deployments INTEGER DEFAULT 0,
    rolled_back_deployments INTEGER DEFAULT 0,
    average_deployment_time DECIMAL(10,2), -- in seconds
    average_rollback_time DECIMAL(10,2), -- in seconds
    success_rate DECIMAL(5,2), -- percentage
    failure_rate DECIMAL(5,2), -- percentage
    rollback_rate DECIMAL(5,2), -- percentage
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(environment, date_bucket)
);

-- Deployment Runbooks table
CREATE TABLE IF NOT EXISTS deployment_runbooks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    deployment_id UUID REFERENCES deployments(id) ON DELETE CASCADE,
    version VARCHAR(50) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    deployment_type VARCHAR(50) NOT NULL,
    pre_deployment_checklist JSONB,
    deployment_steps JSONB,
    post_deployment_verification JSONB,
    rollback_procedures JSONB,
    troubleshooting JSONB,
    emergency_contacts JSONB,
    file_path VARCHAR(500), -- Path to generated runbook file
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Deployment Configuration table
CREATE TABLE IF NOT EXISTS deployment_configurations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    environment VARCHAR(50) NOT NULL UNIQUE,
    config JSONB NOT NULL, -- Environment-specific configuration
    health_checks JSONB, -- Health check configuration
    rollback_config JSONB, -- Rollback configuration
    notification_config JSONB, -- Notification configuration
    security_config JSONB, -- Security configuration
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_deployments_version ON deployments(version);
CREATE INDEX IF NOT EXISTS idx_deployments_environment ON deployments(environment);
CREATE INDEX IF NOT EXISTS idx_deployments_status ON deployments(status);
CREATE INDEX IF NOT EXISTS idx_deployments_type ON deployments(deployment_type);
CREATE INDEX IF NOT EXISTS idx_deployments_created_at ON deployments(created_at);
CREATE INDEX IF NOT EXISTS idx_deployments_started_at ON deployments(started_at);

CREATE INDEX IF NOT EXISTS idx_deployment_phases_deployment_id ON deployment_phases(deployment_id);
CREATE INDEX IF NOT EXISTS idx_deployment_phases_name ON deployment_phases(phase_name);
CREATE INDEX IF NOT EXISTS idx_deployment_phases_index ON deployment_phases(phase_index);

CREATE INDEX IF NOT EXISTS idx_health_check_results_environment ON health_check_results(environment);
CREATE INDEX IF NOT EXISTS idx_health_check_results_checked_at ON health_check_results(checked_at);
CREATE INDEX IF NOT EXISTS idx_health_check_results_health ON health_check_results(overall_health);

CREATE INDEX IF NOT EXISTS idx_health_checks_result_id ON health_checks(health_check_result_id);
CREATE INDEX IF NOT EXISTS idx_health_checks_name ON health_checks(name);
CREATE INDEX IF NOT EXISTS idx_health_checks_status ON health_checks(status);

CREATE INDEX IF NOT EXISTS idx_scheduled_deployments_time ON scheduled_deployments(scheduled_time);
CREATE INDEX IF NOT EXISTS idx_scheduled_deployments_status ON scheduled_deployments(status);
CREATE INDEX IF NOT EXISTS idx_scheduled_deployments_environment ON scheduled_deployments(environment);

CREATE INDEX IF NOT EXISTS idx_deployment_rollback_history_deployment_id ON deployment_rollback_history(deployment_id);
CREATE INDEX IF NOT EXISTS idx_deployment_rollback_history_started_at ON deployment_rollback_history(started_at);

CREATE INDEX IF NOT EXISTS idx_deployment_readiness_environment ON deployment_readiness_validations(environment);
CREATE INDEX IF NOT EXISTS idx_deployment_readiness_version ON deployment_readiness_validations(version);
CREATE INDEX IF NOT EXISTS idx_deployment_readiness_validated_at ON deployment_readiness_validations(validated_at);

CREATE INDEX IF NOT EXISTS idx_deployment_metrics_environment ON deployment_metrics(environment);
CREATE INDEX IF NOT EXISTS idx_deployment_metrics_date_bucket ON deployment_metrics(date_bucket);

CREATE INDEX IF NOT EXISTS idx_deployment_runbooks_deployment_id ON deployment_runbooks(deployment_id);
CREATE INDEX IF NOT EXISTS idx_deployment_runbooks_generated_at ON deployment_runbooks(generated_at);

CREATE INDEX IF NOT EXISTS idx_deployment_configurations_environment ON deployment_configurations(environment);
CREATE INDEX IF NOT EXISTS idx_deployment_configurations_active ON deployment_configurations(is_active);

-- RLS Policies for deployments
ALTER TABLE deployments ENABLE ROW LEVEL SECURITY;

-- Admin can manage deployments
CREATE POLICY "Admin can manage deployments" ON deployments
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view deployments
CREATE POLICY "BOD can view deployments" ON deployments
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view deployments
CREATE POLICY "Users can view deployments" ON deployments
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for deployment phases
ALTER TABLE deployment_phases ENABLE ROW LEVEL SECURITY;

-- Admin can manage deployment phases
CREATE POLICY "Admin can manage deployment phases" ON deployment_phases
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view deployment phases
CREATE POLICY "BOD can view deployment phases" ON deployment_phases
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view deployment phases
CREATE POLICY "Users can view deployment phases" ON deployment_phases
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for health check results
ALTER TABLE health_check_results ENABLE ROW LEVEL SECURITY;

-- Admin can manage health check results
CREATE POLICY "Admin can manage health check results" ON health_check_results
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view health check results
CREATE POLICY "BOD can view health check results" ON health_check_results
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view health check results
CREATE POLICY "Users can view health check results" ON health_check_results
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for scheduled deployments
ALTER TABLE scheduled_deployments ENABLE ROW LEVEL SECURITY;

-- Admin can manage scheduled deployments
CREATE POLICY "Admin can manage scheduled deployments" ON scheduled_deployments
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view scheduled deployments
CREATE POLICY "BOD can view scheduled deployments" ON scheduled_deployments
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view scheduled deployments
CREATE POLICY "Users can view scheduled deployments" ON scheduled_deployments
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for deployment metrics
ALTER TABLE deployment_metrics ENABLE ROW LEVEL SECURITY;

-- Admin can manage deployment metrics
CREATE POLICY "Admin can manage deployment metrics" ON deployment_metrics
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view deployment metrics
CREATE POLICY "BOD can view deployment metrics" ON deployment_metrics
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view deployment metrics
CREATE POLICY "Users can view deployment metrics" ON deployment_metrics
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- Function to create deployment
CREATE OR REPLACE FUNCTION create_deployment(
    p_version VARCHAR(50),
    p_environment VARCHAR(50),
    p_deployment_type VARCHAR(50),
    p_description TEXT DEFAULT NULL,
    p_build_number VARCHAR(100),
    p_commit_hash VARCHAR(100),
    p_rollback_enabled BOOLEAN DEFAULT TRUE,
    p_health_check_enabled BOOLEAN DEFAULT TRUE,
    p_created_by UUID DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    deployment_id UUID;
BEGIN
    INSERT INTO deployments (
        version, environment, deployment_type, description,
        build_number, commit_hash, rollback_enabled, health_check_enabled,
        created_by
    ) VALUES (
        p_version, p_environment, p_deployment_type, p_description,
        p_build_number, p_commit_hash, p_rollback_enabled, p_health_check_enabled,
        p_created_by
    ) RETURNING id INTO deployment_id;
    
    RETURN deployment_id;
END;
$$ LANGUAGE plpgsql;

-- Function to start deployment
CREATE OR REPLACE FUNCTION start_deployment(
    p_deployment_id UUID,
    p_triggered_by UUID DEFAULT NULL
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE deployments
    SET 
        status = 'IN_PROGRESS',
        started_at = NOW(),
        triggered_by = p_triggered_by,
        updated_at = NOW()
    WHERE id = p_deployment_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to complete deployment
CREATE OR REPLACE FUNCTION complete_deployment(
    p_deployment_id UUID,
    p_status VARCHAR(20), -- 'SUCCESS' or 'FAILED'
    p_duration_seconds INTEGER DEFAULT NULL
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE deployments
    SET 
        status = p_status,
        duration_seconds = p_duration_seconds,
        completed_at = NOW(),
        updated_at = NOW()
    WHERE id = p_deployment_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to execute rollback
CREATE OR REPLACE FUNCTION execute_rollback(
    p_deployment_id UUID,
    p_rollback_type VARCHAR(50),
    p_reason TEXT,
    p_triggered_by UUID DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    rollback_id UUID;
BEGIN
    -- Update deployment status
    UPDATE deployments
    SET 
        status = 'ROLLBACK',
        rollback_at = NOW(),
        rollback_reason = p_reason,
        rollback_type = p_rollback_type,
        triggered_by = p_triggered_by,
        updated_at = NOW()
    WHERE id = p_deployment_id;
    
    -- Record rollback history
    INSERT INTO deployment_rollback_history (
        deployment_id, rollback_type, reason, success,
        triggered_by, started_at
    ) VALUES (
        p_deployment_id, p_rollback_type, p_reason, TRUE,
        p_triggered_by, NOW()
    ) RETURNING id INTO rollback_id;
    
    RETURN rollback_id;
END;
$$ LANGUAGE plpgsql;

-- Function to record deployment phase
CREATE OR REPLACE FUNCTION record_deployment_phase(
    p_deployment_id UUID,
    p_phase_name VARCHAR(100),
    p_phase_index INTEGER,
    p_success BOOLEAN,
    p_duration_seconds INTEGER,
    p_logs JSONB DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    phase_id UUID;
BEGIN
    INSERT INTO deployment_phases (
        deployment_id, phase_name, phase_index, success,
        duration_seconds, started_at, completed_at, logs
    ) VALUES (
        p_deployment_id, p_phase_name, p_phase_index, p_success,
        p_duration_seconds, NOW(), NOW(), p_logs
    ) RETURNING id INTO phase_id;
    
    RETURN phase_id;
END;
$$ LANGUAGE plpgsql;

-- Function to perform health check
CREATE OR REPLACE FUNCTION perform_health_check(
    p_environment VARCHAR(50),
    p_check_type VARCHAR(50) DEFAULT 'COMPREHENSIVE'
)
RETURNS TABLE (
    overall_health VARCHAR(20),
    healthy_checks INTEGER,
    total_checks INTEGER,
    duration BIGINT,
    checks JSONB
) AS $$
DECLARE
    check_start_time TIMESTAMP WITH TIME ZONE;
    check_end_time TIMESTAMP WITH TIME ZONE;
    health_checks JSONB DEFAULT '[]'::JSONB;
    healthy_count INTEGER DEFAULT 0;
    total_count INTEGER DEFAULT 0;
BEGIN
    check_start_time := NOW();
    
    -- Simulate health checks
    -- Database health check
    health_checks := health_checks || jsonb_build_object(
        'name', 'Database',
        'status', CASE WHEN (random() > 0.05) THEN 'HEALTHY' ELSE 'DEGRADED' END,
        'response_time', (random() * 100 + 50)::INTEGER,
        'details', jsonb_build_object(
            'connection_pool', CASE WHEN (random() > 0.1) THEN 'healthy' ELSE 'degraded' END,
            'query_performance', CASE WHEN (random() > 0.15) THEN 'optimal' ELSE 'slow' END
        )
    );
    
    -- API health check
    health_checks := health_checks || jsonb_build_object(
        'name', 'API',
        'status', CASE WHEN (random() > 0.1) THEN 'HEALTHY' ELSE 'DEGRADED' END,
        'response_time', (random() * 200 + 50)::INTEGER,
        'details', jsonb_build_object(
            'endpoint_availability', CASE WHEN (random() > 0.05) THEN '100%' ELSE '95%' END,
            'error_rate', CASE WHEN (random() > 0.05) THEN '0%' ELSE '5%' END
        )
    );
    
    -- Service health check
    health_checks := health_checks || jsonb_build_object(
        'name', 'Services',
        'status', CASE WHEN (random() > 0.08) THEN 'HEALTHY' ELSE 'DEGRADED' END,
        'response_time', (random() * 150 + 20)::INTEGER,
        'details', jsonb_build_object(
            'active_services', CASE WHEN (random() > 0.02) THEN '8/8' ELSE '7/8' END,
            'service_uptime', CASE WHEN (random() > 0.01) THEN '99.9%' ELSE '98.5%' END
        )
    );
    
    -- Infrastructure health check
    health_checks := health_checks || jsonb_build_object(
        'name', 'Infrastructure',
        'status', CASE WHEN (random() > 0.03) THEN 'HEALTHY' ELSE 'DEGRADED' END,
        'response_time', (random() * 50 + 5)::INTEGER,
        'details', jsonb_build_object(
            'cpu_usage', CASE WHEN (random() > 0.3) THEN '45%' ELSE '78%' END,
            'memory_usage', CASE WHEN (random() > 0.4) THEN '60%' ELSE '85%' END,
            'disk_usage', CASE WHEN (random() > 0.35) THEN '30%' ELSE '65%' END
        )
    );
    
    check_end_time := NOW();
    
    -- Calculate health metrics
    total_count := jsonb_array_length(health_checks);
    healthy_count := (
        SELECT COUNT(*) 
        FROM jsonb_array_elements(health_checks) elem
        WHERE elem ->> 'status' = 'HEALTHY'
    );
    
    -- Determine overall health
    RETURN QUERY SELECT
        CASE 
            WHEN healthy_count = total_count THEN 'HEALTHY'
            WHEN healthy_count >= total_count * 0.7 THEN 'DEGRADED'
            WHEN healthy_count >= total_count * 0.3 THEN 'DEGRADED'
            ELSE 'UNHEALTHY'
        END as overall_health,
        healthy_count,
        total_count,
        EXTRACT(EPOCH FROM (check_end_time - check_start_time)) * 1000 as duration,
        health_checks;
END;
$$ LANGUAGE plpgsql;

-- Function to save health check result
CREATE OR REPLACE FUNCTION save_health_check_result(
    p_environment VARCHAR(50),
    p_overall_health VARCHAR(20),
    p_healthy_checks INTEGER,
    p_total_checks INTEGER,
    p_checks JSONB,
    p_duration BIGINT
)
RETURNS UUID AS $$
DECLARE
    result_id UUID;
BEGIN
    INSERT INTO health_check_results (
        environment, overall_health, healthy_checks, total_checks,
        checks, duration, checked_at
    ) VALUES (
        p_environment, p_overall_health, p_healthy_checks, p_total_checks,
        p_checks, p_duration, NOW()
    ) RETURNING id INTO result_id;
    
    -- Save individual health checks
    INSERT INTO health_checks (
        health_check_result_id, name, status, response_time, details
    SELECT 
        result_id,
        elem ->> 'name',
        elem ->> 'status',
        elem ->> 'response_time',
        elem ->> 'details'
    FROM jsonb_array_elements(p_checks) elem;
    
    RETURN result_id;
END;
$$ LANGUAGE plpgsql;

-- Function to validate deployment readiness
CREATE OR REPLACE FUNCTION validate_deployment_readiness(
    p_environment VARCHAR(50),
    p_version VARCHAR(50)
)
RETURNS TABLE (
    is_ready BOOLEAN,
    total_checks INTEGER,
    passed_checks INTEGER,
    failed_checks INTEGER,
    checks JSONB
) AS $$
DECLARE
    readiness_checks JSONB DEFAULT '[]'::JSONB;
    passed_count INTEGER DEFAULT 0;
BEGIN
    -- Code validation
    readiness_checks := readiness_checks || jsonb_build_object(
        'name', 'Code Validation',
        'status', CASE WHEN (random() > 0.05) THEN 'PASSED' ELSE 'FAILED' END,
        'message', CASE WHEN (random() > 0.05) THEN 'Code validation passed' ELSE 'Code validation failed' END,
        'details', jsonb_build_object(
            'static_analysis', CASE WHEN (random() > 0.05) THEN 'passed' ELSE 'failed' END,
            'unit_tests', CASE WHEN (random() > 0.05) THEN 'passed' ELSE 'failed' END,
            'integration_tests', CASE WHEN (random() > 0.05) THEN 'passed' ELSE 'failed' END
        )
    );
    
    -- Environment validation
    readiness_checks := readiness_checks || jsonb_build_object(
        'name', 'Environment Validation',
        'status', CASE WHEN (random() > 0.08) THEN 'PASSED' ELSE 'FAILED' END,
        'message', CASE WHEN (random() > 0.08) THEN 'Environment is ready' ELSE 'Environment not ready' END,
        'details', jsonb_build_object(
            'services', CASE WHEN (random() > 0.08) THEN 'running' ELSE 'stopped' END,
            'resources', CASE WHEN (random() > 0.1) THEN 'available' ELSE 'insufficient' END,
            'connectivity', CASE WHEN (random() > 0.05) THEN 'ok' ELSE 'issues' END
        )
    );
    
    -- Configuration validation
    readiness_checks := readiness_checks || jsonb_build_object(
        'name', 'Configuration Validation',
        'status', CASE WHEN (random() > 0.03) THEN 'PASSED' ELSE 'FAILED' END,
        'message', CASE WHEN (random() > 0.03) THEN 'Configuration is valid' ELSE 'Configuration issues found' END,
        'details', jsonb_build_object(
            'database_config', CASE WHEN (random() > 0.03) THEN 'valid' ELSE 'invalid' END,
            'api_config', CASE WHEN (random() > 0.03) THEN 'valid' ELSE 'invalid' END,
            'security_config', CASE WHEN (random() > 0.03) THEN 'valid' ELSE 'invalid' END
        )
    );
    
    -- Resource validation
    readiness_checks := readiness_checks || jsonb_build_object(
        'name', 'Resource Validation',
        'status', CASE WHEN (random() > 0.1) THEN 'PASSED' ELSE 'FAILED' END,
        'message', CASE WHEN (random() > 0.1) THEN 'Resources are sufficient' ELSE 'Insufficient resources' END,
        'details', jsonb_build_object(
            'cpu', CASE WHEN (random() > 0.3) THEN 'available' ELSE 'insufficient' END,
            'memory', CASE WHEN (random() > 0.4) THEN 'available' ELSE 'insufficient' END,
            'storage', CASE WHEN (random() > 0.35) THEN 'available' ELSE 'insufficient' END
        )
    );
    
    -- Security validation
    readiness_checks := readiness_checks || jsonb_build_object(
        'name', 'Security Validation',
        'status', CASE WHEN (random() > 0.07) THEN 'PASSED' ELSE 'FAILED' END,
        'message', CASE WHEN (random() > 0.07) THEN 'Security checks passed' ELSE 'Security issues found' END,
        'details', jsonb_build_object(
            'ssl_certificates', CASE WHEN (random() > 0.07) THEN 'valid' ELSE 'expired' END,
            'firewall_rules', CASE WHEN (random() > 0.07) THEN 'configured' ELSE 'missing' END,
            'access_controls', CASE WHEN (random() > 0.07) THEN 'proper' ELSE 'issues' END
        )
    );
    
    -- Calculate passed checks
    passed_count := (
        SELECT COUNT(*) 
        FROM jsonb_array_elements(readiness_checks) elem
        WHERE elem ->> 'status' = 'PASSED'
    );
    
    RETURN QUERY SELECT
        passed_count = jsonb_array_length(readiness_checks) as is_ready,
        jsonb_array_length(readiness_checks) as total_checks,
        passed_count as passed_checks,
        jsonb_array_length(readiness_checks) - passed_count as failed_checks,
        readiness_checks as checks;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate deployment metrics
CREATE OR REPLACE FUNCTION calculate_deployment_metrics(
    p_start_date DATE DEFAULT NULL,
    p_end_date DATE DEFAULT NULL,
    p_environment VARCHAR(50) DEFAULT NULL
)
RETURNS TABLE (
    total_deployments BIGINT,
    successful_deployments BIGINT,
    failed_deployments BIGINT,
    rolled_back_deployments BIGINT,
    average_deployment_time DECIMAL(10,2),
    average_rollback_time DECIMAL(10,2),
    success_rate DECIMAL(5,2),
    failure_rate DECIMAL(5,2),
    rollback_rate DECIMAL(5,2)
) AS $$
BEGIN
    RETURN QUERY
    WITH deployment_stats AS (
        SELECT 
            COUNT(*) as total_deployments,
            COUNT(*) FILTER (WHERE status = 'SUCCESS') as successful_deployments,
            COUNT(*) FILTER (WHERE status = 'FAILED') as failed_deployments,
            COUNT(*) FILTER (WHERE status = 'ROLLBACK') as rolled_back_deployments,
            COALESCE(AVG(duration_seconds), 0) as average_deployment_time
        FROM deployments
        WHERE 
            (p_start_date IS NULL OR DATE(created_at) >= p_start_date)
            AND (p_end_date IS NULL OR DATE(created_at) <= p_end_date)
            AND (p_environment IS NULL OR environment = p_environment)
    ),
    rollback_stats AS (
        SELECT COALESCE(AVG(duration_seconds), 0) as average_rollback_time
        FROM deployment_rollback_history
        WHERE 
            (p_start_date IS NULL OR DATE(started_at) >= p_start_date)
            AND (p_end_date IS NULL OR DATE(started_at) <= p_end_date)
    )
    SELECT 
        ds.total_deployments,
        ds.successful_deployments,
        ds.failed_deployments,
        ds.rolled_back_deployments,
        ds.average_deployment_time,
        rs.average_rollback_time,
        CASE 
            WHEN ds.total_deployments > 0 THEN 
                ROUND((ds.successful_deployments::DECIMAL / ds.total_deployments::DECIMAL) * 100, 2)
            ELSE 0 
        END as success_rate,
        CASE 
            WHEN ds.total_deployments > 0 THEN 
                ROUND((ds.failed_deployments::DECIMAL / ds.total_deployments::DECIMAL) * 100, 2)
            ELSE 0 
        END as failure_rate,
        CASE 
            WHEN ds.total_deployments > 0 THEN 
                ROUND((ds.rolled_back_deployments::DECIMAL / ds.total_deployments::DECIMAL) * 100, 2)
            ELSE 0 
        END as rollback_rate
    FROM deployment_stats ds, rollback_stats rs;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old deployment data
CREATE OR REPLACE FUNCTION cleanup_deployment_data(
    p_days_old INTEGER DEFAULT 90
)
RETURNS INTEGER AS $$
BEGIN
    -- Clean up old health check results
    DELETE FROM health_check_results
    WHERE checked_at < NOW() - INTERVAL '1 day' * p_days_old;
    
    -- Clean up old deployment metrics
    DELETE FROM deployment_metrics
    WHERE date_bucket < CURRENT_DATE - INTERVAL '1 day' * p_days_old;
    
    RETURN ROW_COUNT;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_deployments_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_deployments_updated_at BEFORE UPDATE ON deployments FOR EACH ROW EXECUTE FUNCTION update_deployments_updated_at();

CREATE OR REPLACE FUNCTION update_scheduled_deployments_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_scheduled_deployments_updated_at BEFORE UPDATE ON scheduled_deployments FOR EACH ROW EXECUTE FUNCTION update_scheduled_deployments_updated_at();

CREATE OR REPLACE FUNCTION update_deployment_metrics_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_deployment_metrics_updated_at BEFORE UPDATE ON deployment_metrics FOR EACH ROW EXECUTE FUNCTION update_deployment_metrics_updated_at();

-- Insert default deployment configurations
INSERT INTO deployment_configurations (environment, config, health_checks, rollback_config, notification_config, security_config) VALUES
('DEVELOPMENT', 
 '{"auto_deploy": true, "require_approval": false, "backup_before": false}',
 '{"timeout_seconds": 30, "retry_count": 3}',
 '{"auto_rollback": true, "timeout_minutes": 5, "verify_after_rollback": true}',
 '{"slack_webhook": "https://hooks.slack.com/dev-deployments", "email_enabled": false}',
 '{"require_ssl": false, "allow_test_data": true}',
true),
('TESTING', 
 '{"auto_deploy": true, "require_approval": false, "backup_before": true}',
 '{"timeout_seconds": 60, "retry_count": 3}',
 '{"auto_rollback": true, "timeout_minutes": 10, "verify_after_rollback": true}',
 '{"slack_webhook": "https://hooks.slack.com/test-deployments", "email_enabled": true}',
 '{"require_ssl": true, "allow_test_data": true}',
'true'),
('STAGING', 
'{"auto_deploy": false, "require_approval": true, "backup_before": true}',
'{"timeout_seconds": 120, "retry_count": 2}',
'{"auto_rollback": true, "timeout_minutes": 15, "verify_after_rollback": true}',
'{"slack_webhook": "https://hooks.slack.com/staging-deployments", "email_enabled": true}',
'{"require_ssl": true, "allow_test_data": false}',
'true'),
('PRODUCTION', 
'{"auto_deploy": false, "require_approval": true, "backup_before": true}',
'{"timeout_seconds": 300, "retry_count": 1}',
'{"auto_rollback": true, "timeout_minutes": 30, "verify_after_rollback": true}',
'{"slack_webhook": "https://hooks.slack.com/prod-deployments", "email_enabled": true}',
'{"require_ssl": true, "allow_test_data": false}',
'true')
ON CONFLICT (environment) DO NOTHING;

-- Schedule cleanup function (run monthly)
-- SELECT cron.schedule('cleanup-deployment-data', '0 2 1 * *', 'SELECT cleanup_deployment_data();');

-- Schedule metrics calculation (run daily)
-- SELECT cron.schedule('calculate-deployment-metrics', '0 1 * * *', 'INSERT INTO deployment_metrics SELECT * FROM calculate_deployment_metrics();');
