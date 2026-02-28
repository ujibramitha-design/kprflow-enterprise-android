-- Rate Limiting tables for Phase 17: API Rate Limiting

-- Rate Limit Configs table
CREATE TABLE IF NOT EXISTS rate_limit_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    limit_type VARCHAR(50) NOT NULL UNIQUE, -- 'USER', 'IP', 'API_KEY', 'ENDPOINT'
    max_requests INTEGER NOT NULL,
    window_minutes INTEGER NOT NULL,
    block_duration_minutes INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Rate Limit Logs table
CREATE TABLE IF NOT EXISTS rate_limit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    identifier VARCHAR(255) NOT NULL, -- user_id, IP address, API key, or endpoint+identifier
    limit_type VARCHAR(50) NOT NULL,
    window_minutes INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Rate Limit Violations table
CREATE TABLE IF NOT EXISTS rate_limit_violations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    identifier VARCHAR(255) NOT NULL,
    limit_type VARCHAR(50) NOT NULL,
    current_count INTEGER NOT NULL,
    max_requests INTEGER NOT NULL,
    window_minutes INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Rate Limit Blocks table
CREATE TABLE IF NOT EXISTS rate_limit_blocks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    identifier VARCHAR(255) NOT NULL,
    limit_type VARCHAR(50) NOT NULL,
    reason TEXT,
    blocked_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    unblock_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    unblocked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_rate_limit_configs_limit_type ON rate_limit_configs(limit_type);
CREATE INDEX IF NOT EXISTS idx_rate_limit_configs_is_active ON rate_limit_configs(is_active);

CREATE INDEX IF NOT EXISTS idx_rate_limit_logs_identifier ON rate_limit_logs(identifier);
CREATE INDEX IF NOT EXISTS idx_rate_limit_logs_limit_type ON rate_limit_logs(limit_type);
CREATE INDEX IF NOT EXISTS idx_rate_limit_logs_created_at ON rate_limit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_rate_limit_logs_composite ON rate_limit_logs(identifier, limit_type, created_at);

CREATE INDEX IF NOT EXISTS idx_rate_limit_violations_identifier ON rate_limit_violations(identifier);
CREATE INDEX IF NOT EXISTS idx_rate_limit_violations_limit_type ON rate_limit_violations(limit_type);
CREATE INDEX IF NOT EXISTS idx_rate_limit_violations_created_at ON rate_limit_violations(created_at);

CREATE INDEX IF NOT EXISTS idx_rate_limit_blocks_identifier ON rate_limit_blocks(identifier);
CREATE INDEX IF NOT EXISTS idx_rate_limit_blocks_limit_type ON rate_limit_blocks(limit_type);
CREATE INDEX IF NOT EXISTS idx_rate_limit_blocks_is_active ON rate_limit_blocks(is_active);
CREATE INDEX IF NOT EXISTS idx_rate_limit_blocks_unblock_at ON rate_limit_blocks(unblock_at);

-- RLS Policies for rate limit configs
ALTER TABLE rate_limit_configs ENABLE ROW LEVEL SECURITY;

-- Admin can manage rate limit configs
CREATE POLICY "Admin can manage rate limit configs" ON rate_limit_configs
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- System can read rate limit configs
CREATE POLICY "System can read rate limit configs" ON rate_limit_configs
    FOR SELECT USING (auth.jwt() ->> 'role' = 'SYSTEM');

-- RLS Policies for rate limit logs
ALTER TABLE rate_limit_logs ENABLE ROW LEVEL SECURITY;

-- System can manage rate limit logs
CREATE POLICY "System can manage rate limit logs" ON rate_limit_logs
    FOR ALL USING (auth.jwt() ->> 'role' = 'SYSTEM');

-- Admin can read rate limit logs
CREATE POLICY "Admin can read rate limit logs" ON rate_limit_logs
    FOR SELECT USING (auth.jwt() ->> 'role' = 'ADMIN');

-- RLS Policies for rate limit violations
ALTER TABLE rate_limit_violations ENABLE ROW LEVEL SECURITY;

-- System can manage rate limit violations
CREATE POLICY "System can manage rate limit violations" ON rate_limit_violations
    FOR ALL USING (auth.jwt() ->> 'role' = 'SYSTEM');

-- Admin can read rate limit violations
CREATE POLICY "Admin can read rate limit violations" ON rate_limit_violations
    FOR SELECT USING (auth.jwt() ->> 'role' = 'ADMIN');

-- RLS Policies for rate limit blocks
ALTER TABLE rate_limit_blocks ENABLE ROW LEVEL SECURITY;

-- System can manage rate limit blocks
CREATE POLICY "System can manage rate limit blocks" ON rate_limit_blocks
    FOR ALL USING (auth.jwt() ->> 'role' = 'SYSTEM');

-- Admin can manage rate limit blocks
CREATE POLICY "Admin can manage rate limit blocks" ON rate_limit_blocks
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- Insert default rate limit configs
INSERT INTO rate_limit_configs (limit_type, max_requests, window_minutes, block_duration_minutes) VALUES
('USER', 100, 1, 60),
('IP', 200, 1, 60),
('API_KEY', 1000, 1, 60),
('ENDPOINT', 50, 1, 30)
ON CONFLICT (limit_type) DO NOTHING;

-- Function to check rate limit
CREATE OR REPLACE FUNCTION check_rate_limit(
    p_identifier VARCHAR(255),
    p_limit_type VARCHAR(50),
    p_max_requests INTEGER DEFAULT NULL,
    p_window_minutes INTEGER DEFAULT NULL
)
RETURNS TABLE (
    allowed BOOLEAN,
    remaining_requests INTEGER,
    reset_time TIMESTAMP WITH TIME ZONE,
    blocked BOOLEAN,
    block_reason TEXT
) AS $$
DECLARE
    v_config RECORD;
    v_current_count INTEGER;
    v_remaining_requests INTEGER;
    v_reset_time TIMESTAMP WITH TIME ZONE;
    v_is_blocked BOOLEAN;
    v_block_reason TEXT;
BEGIN
    -- Get rate limit config
    SELECT * INTO v_config
    FROM rate_limit_configs
    WHERE limit_type = p_limit_type AND is_active = TRUE;
    
    -- Use provided values or defaults from config
    IF v_config IS NULL THEN
        RETURN QUERY SELECT FALSE, 0, NOW() + INTERVAL '1 minute', FALSE, 'Rate limit config not found';
    END IF;
    
    v_max_requests := COALESCE(p_max_requests, v_config.max_requests);
    p_window_minutes := COALESCE(p_window_minutes, v_config.window_minutes);
    
    -- Check if identifier is blocked
    SELECT EXISTS(
        SELECT 1 FROM rate_limit_blocks
        WHERE identifier = p_identifier
        AND limit_type = p_limit_type
        AND is_active = TRUE
        AND unblock_at > NOW()
    ) INTO v_is_blocked;
    
    IF v_is_blocked THEN
        SELECT reason INTO v_block_reason
        FROM rate_limit_blocks
        WHERE identifier = p_identifier
        AND limit_type = p_limit_type
        AND is_active = TRUE
        AND unblock_at > NOW()
        LIMIT 1;
        
        RETURN QUERY SELECT FALSE, 0, NOW() + INTERVAL '1 minute', TRUE, v_block_reason;
    END IF;
    
    -- Get current request count
    SELECT COUNT(*) INTO v_current_count
    FROM rate_limit_logs
    WHERE identifier = p_identifier
    AND limit_type = p_limit_type
    AND created_at >= NOW() - INTERVAL '1 minute' * p_window_minutes;
    
    v_remaining_requests := v_max_requests - v_current_count;
    v_reset_time := NOW() + INTERVAL '1 minute' * p_window_minutes;
    
    -- If rate limit exceeded, log violation and return not allowed
    IF v_remaining_requests <= 0 THEN
        -- Log violation
        INSERT INTO rate_limit_violations (
            identifier, limit_type, current_count, max_requests, window_minutes
        ) VALUES (
            p_identifier, p_limit_type, v_current_count, v_max_requests, p_window_minutes
        );
        
        RETURN QUERY SELECT FALSE, 0, v_reset_time, FALSE, NULL;
    END IF;
    
    -- Log the request and return allowed
    INSERT INTO rate_limit_logs (identifier, limit_type, window_minutes)
    VALUES (p_identifier, p_limit_type, p_window_minutes);
    
    RETURN QUERY SELECT TRUE, v_remaining_requests, v_reset_time, FALSE, NULL;
END;
$$ LANGUAGE plpgsql;

-- Function to block identifier
CREATE OR REPLACE FUNCTION block_identifier(
    p_identifier VARCHAR(255),
    p_limit_type VARCHAR(50),
    p_reason TEXT DEFAULT NULL,
    p_block_duration_minutes INTEGER DEFAULT 60
)
RETURNS BOOLEAN AS $$
BEGIN
    INSERT INTO rate_limit_blocks (
        identifier, limit_type, reason, blocked_at, unblock_at, is_active
    ) VALUES (
        p_identifier, p_limit_type, p_reason, NOW(), 
        NOW() + INTERVAL '1 minute' * p_block_duration_minutes, TRUE
    );
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to unblock identifier
CREATE OR REPLACE FUNCTION unblock_identifier(
    p_identifier VARCHAR(255),
    p_limit_type VARCHAR(50)
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE rate_limit_blocks
    SET is_active = FALSE, unblocked_at = NOW()
    WHERE identifier = p_identifier
    AND limit_type = p_limit_type
    AND is_active = TRUE;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old rate limit logs
CREATE OR REPLACE FUNCTION cleanup_rate_limit_logs(
    p_days_old INTEGER DEFAULT 7
)
RETURNS INTEGER AS $$
BEGIN
    DELETE FROM rate_limit_logs
    WHERE created_at < NOW() - INTERVAL '1 day' * p_days_old;
    
    RETURN ROW_COUNT;
END;
$$ LANGUAGE plpgsql;

-- Function to get rate limit statistics
CREATE OR REPLACE FUNCTION get_rate_limit_statistics(
    p_start_date TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    p_end_date TIMESTAMP WITH TIME ZONE DEFAULT NULL
)
RETURNS TABLE (
    total_requests BIGINT,
    total_violations BIGINT,
    active_blocks BIGINT,
    violation_rate DECIMAL(5,2),
    top_violators JSONB
) AS $$
BEGIN
    RETURN QUERY
    WITH stats AS (
        SELECT 
            (SELECT COUNT(*) FROM rate_limit_logs 
             WHERE (p_start_date IS NULL OR created_at >= p_start_date)
             AND (p_end_date IS NULL OR created_at <= p_end_date)) as total_requests,
            (SELECT COUNT(*) FROM rate_limit_violations 
             WHERE (p_start_date IS NULL OR created_at >= p_start_date)
             AND (p_end_date IS NULL OR created_at <= p_end_date)) as total_violations,
            (SELECT COUNT(*) FROM rate_limit_blocks 
             WHERE is_active = TRUE) as active_blocks
    ),
        top_violators AS (
            SELECT jsonb_agg(
                jsonb_build_object(
                    'identifier', identifier,
                    'limit_type', limit_type,
                    'violation_count', violation_count
                )
            ) as top_violators
            FROM (
                SELECT 
                    identifier, 
                    limit_type, 
                    COUNT(*) as violation_count
                FROM rate_limit_violations
                WHERE (p_start_date IS NULL OR created_at >= p_start_date)
                AND (p_end_date IS NULL OR created_at <= p_end_date)
                GROUP BY identifier, limit_type
                ORDER BY violation_count DESC
                LIMIT 10
            ) tv
        )
    SELECT 
        s.total_requests,
        s.total_violations,
        s.active_blocks,
        CASE 
            WHEN s.total_requests > 0 THEN 
                ROUND((s.total_violations::DECIMAL / s.total_requests::DECIMAL) * 100, 2)
            ELSE 0 
        END as violation_rate,
        tv.top_violators
    FROM stats s, top_violators tv;
END;
$$ LANGUAGE plpgsql;

-- Function to auto-unblock expired blocks
CREATE OR REPLACE FUNCTION auto_unblock_expired_blocks()
RETURNS INTEGER AS $$
DECLARE
    unblocked_count INTEGER;
BEGIN
    UPDATE rate_limit_blocks
    SET is_active = FALSE, unblocked_at = NOW()
    WHERE is_active = TRUE
    AND unblock_at <= NOW();
    
    GET DIAGNOSTICS unblocked_count = ROW_COUNT;
    
    RETURN unblocked_count;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_rate_limit_configs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_rate_limit_configs_updated_at BEFORE UPDATE ON rate_limit_configs FOR EACH ROW EXECUTE FUNCTION update_rate_limit_configs_updated_at();

-- Schedule cleanup function (run daily)
-- This would be set up via pg_cron or external scheduler
-- SELECT cron.schedule('cleanup-rate-limit-logs', '0 2 * * *', 'SELECT cleanup_rate_limit_logs();');

-- Schedule auto-unblock function (run every 5 minutes)
-- SELECT cron.schedule('auto-unblock-expired', '*/5 * * * *', 'SELECT auto_unblock_expired_blocks();');
