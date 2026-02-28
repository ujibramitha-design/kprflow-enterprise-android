-- Performance Monitoring tables for Phase 21: System Health Metrics

-- Performance Metrics table
CREATE TABLE IF NOT EXISTS performance_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    metric_type VARCHAR(50) NOT NULL, -- 'DATABASE', 'API', 'MEMORY', 'CPU', 'DISK', 'NETWORK', 'USER_ACTIVITY', 'SYSTEM_HEALTH'
    endpoint VARCHAR(255),
    method VARCHAR(10),
    response_time BIGINT, -- in milliseconds
    status_code INTEGER,
    query_type VARCHAR(100),
    query TEXT,
    execution_time BIGINT, -- in milliseconds
    rows_affected INTEGER,
    cpu_usage DECIMAL(5,2), -- percentage
    memory_usage DECIMAL(5,2), -- percentage
    disk_usage DECIMAL(5,2), -- percentage
    network_in BIGINT, -- in bytes
    network_out BIGINT, -- in bytes
    user_id UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    activity_type VARCHAR(100),
    session_id VARCHAR(255),
    duration BIGINT, -- in seconds
    details JSONB,
    has_alert BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Performance Alerts table
CREATE TABLE IF NOT EXISTS performance_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    metric_type VARCHAR(50) NOT NULL,
    level VARCHAR(20) NOT NULL, -- 'INFO', 'WARNING', 'CRITICAL'
    message TEXT NOT NULL,
    details JSONB,
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- System Health Summary table (aggregated data)
CREATE TABLE IF NOT EXISTS system_health_summary (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    overall_status VARCHAR(20) NOT NULL, -- 'EXCELLENT', 'GOOD', 'FAIR', 'POOR', 'CRITICAL'
    health_score DECIMAL(5,2) NOT NULL,
    cpu_usage DECIMAL(5,2),
    memory_usage DECIMAL(5,2),
    disk_usage DECIMAL(5,2),
    network_in BIGINT,
    network_out BIGINT,
    active_alerts INTEGER DEFAULT 0,
    total_metrics BIGINT DEFAULT 0,
    api_metrics BIGINT DEFAULT 0,
    database_metrics BIGINT DEFAULT 0,
    system_metrics BIGINT DEFAULT 0
);

-- Performance Trends table (pre-calculated for faster queries)
CREATE TABLE IF NOT EXISTS performance_trends (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    metric_type VARCHAR(50) NOT NULL,
    resource VARCHAR(255),
    time_bucket TIMESTAMP WITH TIME ZONE NOT NULL,
    granularity VARCHAR(10) NOT NULL, -- '1m', '5m', '15m', '1h', '6h', '1d'
    average_value DECIMAL(10,2),
    min_value DECIMAL(10,2),
    max_value DECIMAL(10,2),
    count_samples INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_performance_metrics_type ON performance_metrics(metric_type);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_timestamp ON performance_metrics(created_at);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_endpoint ON performance_metrics(endpoint);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_user_id ON performance_metrics(user_id);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_has_alert ON performance_metrics(has_alert);

CREATE INDEX IF NOT EXISTS idx_performance_alerts_type ON performance_alerts(metric_type);
CREATE INDEX IF NOT EXISTS idx_performance_alerts_level ON performance_alerts(level);
CREATE INDEX IF NOT EXISTS idx_performance_alerts_resolved ON performance_alerts(is_resolved);
CREATE INDEX IF NOT EXISTS idx_performance_alerts_created_at ON performance_alerts(created_at);

CREATE INDEX IF NOT EXISTS idx_system_health_summary_timestamp ON system_health_summary(timestamp);
CREATE INDEX IF NOT EXISTS idx_system_health_summary_status ON system_health_summary(overall_status);

CREATE INDEX IF NOT EXISTS idx_performance_trends_type ON performance_trends(metric_type);
CREATE INDEX IF NOT EXISTS idx_performance_trends_bucket ON performance_trends(time_bucket);
CREATE INDEX IF NOT EXISTS idx_performance_trends_granularity ON performance_trends(granularity);

-- RLS Policies for performance metrics
ALTER TABLE performance_metrics ENABLE ROW LEVEL SECURITY;

-- System can manage performance metrics
CREATE POLICY "System can manage performance metrics" ON performance_metrics
    FOR ALL USING (auth.jwt() ->> 'role' = 'SYSTEM');

-- Admin can read performance metrics
CREATE POLICY "Admin can read performance metrics" ON performance_metrics
    FOR SELECT USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can read performance metrics
CREATE POLICY "BOD can read performance metrics" ON performance_metrics
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- RLS Policies for performance alerts
ALTER TABLE performance_alerts ENABLE ROW LEVEL SECURITY;

-- System can manage performance alerts
CREATE POLICY "System can manage performance alerts" ON performance_alerts
    FOR ALL USING (auth.jwt() ->> 'role' = 'SYSTEM');

-- Admin can manage performance alerts
CREATE POLICY "Admin can manage performance alerts" ON performance_alerts
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can read performance alerts
CREATE POLICY "BOD can read performance alerts" ON performance_alerts
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- RLS Policies for system health summary
ALTER TABLE system_health_summary ENABLE ROW LEVEL SECURITY;

-- System can manage system health summary
CREATE POLICY "System can manage system health summary" ON system_health_summary
    FOR ALL USING (auth.jwt() ->> 'role' = 'SYSTEM');

-- Admin can read system health summary
CREATE POLICY "Admin can read system health summary" ON system_health_summary
    FOR SELECT USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can read system health summary
CREATE POLICY "BOD can read system health summary" ON system_health_summary
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- RLS Policies for performance trends
ALTER TABLE performance_trends ENABLE ROW LEVEL SECURITY;

-- System can manage performance trends
CREATE POLICY "System can manage performance trends" ON performance_trends
    FOR ALL USING (auth.jwt() ->> 'role' = 'SYSTEM');

-- Admin can read performance trends
CREATE POLICY "Admin can read performance trends" ON performance_trends
    FOR SELECT USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can read performance trends
CREATE POLICY "BOD can read performance trends" ON performance_trends
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Function to record performance metric
CREATE OR REPLACE FUNCTION record_performance_metric(
    p_metric_type VARCHAR(50),
    p_endpoint VARCHAR(255) DEFAULT NULL,
    p_method VARCHAR(10) DEFAULT NULL,
    p_response_time BIGINT DEFAULT NULL,
    p_status_code INTEGER DEFAULT NULL,
    p_query_type VARCHAR(100) DEFAULT NULL,
    p_query TEXT DEFAULT NULL,
    p_execution_time BIGINT DEFAULT NULL,
    p_rows_affected INTEGER DEFAULT NULL,
    p_cpu_usage DECIMAL(5,2) DEFAULT NULL,
    p_memory_usage DECIMAL(5,2) DEFAULT NULL,
    p_disk_usage DECIMAL(5,2) DEFAULT NULL,
    p_network_in BIGINT DEFAULT NULL,
    p_network_out BIGINT DEFAULT NULL,
    p_user_id UUID DEFAULT NULL,
    p_activity_type VARCHAR(100) DEFAULT NULL,
    p_session_id VARCHAR(255) DEFAULT NULL,
    p_duration BIGINT DEFAULT NULL,
    p_details JSONB DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    metric_id UUID;
    has_alert BOOLEAN DEFAULT FALSE;
BEGIN
    -- Insert performance metric
    INSERT INTO performance_metrics (
        metric_type, endpoint, method, response_time, status_code,
        query_type, query, execution_time, rows_affected,
        cpu_usage, memory_usage, disk_usage, network_in, network_out,
        user_id, activity_type, session_id, duration, details
    ) VALUES (
        p_metric_type, p_endpoint, p_method, p_response_time, p_status_code,
        p_query_type, p_query, p_execution_time, p_rows_affected,
        p_cpu_usage, p_memory_usage, p_disk_usage, p_network_in, p_network_out,
        p_user_id, p_activity_type, p_session_id, p_duration, p_details
    ) RETURNING id INTO metric_id;
    
    -- Check for performance alerts
    IF p_metric_type = 'API' AND p_response_time > 5000 THEN
        has_alert := TRUE;
        INSERT INTO performance_alerts (metric_type, level, message, details)
        VALUES ('API', 'CRITICAL', 'API response time exceeded critical threshold', 
                jsonb_build_object('endpoint', p_endpoint, 'response_time', p_response_time));
    ELSIF p_metric_type = 'API' AND p_response_time > 2000 THEN
        has_alert := TRUE;
        INSERT INTO performance_alerts (metric_type, level, message, details)
        VALUES ('API', 'WARNING', 'API response time exceeded warning threshold', 
                jsonb_build_object('endpoint', p_endpoint, 'response_time', p_response_time));
    END IF;
    
    IF p_metric_type = 'DATABASE' AND p_execution_time > 3000 THEN
        has_alert := TRUE;
        INSERT INTO performance_alerts (metric_type, level, message, details)
        VALUES ('DATABASE', 'CRITICAL', 'Database query time exceeded critical threshold', 
                jsonb_build_object('query_type', p_query_type, 'execution_time', p_execution_time));
    ELSIF p_metric_type = 'DATABASE' AND p_execution_time > 1000 THEN
        has_alert := TRUE;
        INSERT INTO performance_alerts (metric_type, level, message, details)
        VALUES ('DATABASE', 'WARNING', 'Database query time exceeded warning threshold', 
                jsonb_build_object('query_type', p_query_type, 'execution_time', p_execution_time));
    END IF;
    
    IF p_metric_type = 'SYSTEM_HEALTH' THEN
        IF p_cpu_usage > 90 THEN
            has_alert := TRUE;
            INSERT INTO performance_alerts (metric_type, level, message, details)
            VALUES ('CPU', 'CRITICAL', 'CPU usage exceeded critical threshold', 
                    jsonb_build_object('cpu_usage', p_cpu_usage));
        ELSIF p_cpu_usage > 70 THEN
            has_alert := TRUE;
            INSERT INTO performance_alerts (metric_type, level, message, details)
            VALUES ('CPU', 'WARNING', 'CPU usage exceeded warning threshold', 
                    jsonb_build_object('cpu_usage', p_cpu_usage));
        END IF;
        
        IF p_memory_usage > 95 THEN
            has_alert := TRUE;
            INSERT INTO performance_alerts (metric_type, level, message, details)
            VALUES ('MEMORY', 'CRITICAL', 'Memory usage exceeded critical threshold', 
                    jsonb_build_object('memory_usage', p_memory_usage));
        ELSIF p_memory_usage > 80 THEN
            has_alert := TRUE;
            INSERT INTO performance_alerts (metric_type, level, message, details)
            VALUES ('MEMORY', 'WARNING', 'Memory usage exceeded warning threshold', 
                    jsonb_build_object('memory_usage', p_memory_usage));
        END IF;
        
        IF p_disk_usage > 95 THEN
            has_alert := TRUE;
            INSERT INTO performance_alerts (metric_type, level, message, details)
            VALUES ('DISK', 'CRITICAL', 'Disk usage exceeded critical threshold', 
                    jsonb_build_object('disk_usage', p_disk_usage));
        ELSIF p_disk_usage > 80 THEN
            has_alert := TRUE;
            INSERT INTO performance_alerts (metric_type, level, message, details)
            VALUES ('DISK', 'WARNING', 'Disk usage exceeded warning threshold', 
                    jsonb_build_object('disk_usage', p_disk_usage));
        END IF;
    END IF;
    
    -- Update has_alert flag
    IF has_alert THEN
        UPDATE performance_metrics SET has_alert = TRUE WHERE id = metric_id;
    END IF;
    
    RETURN metric_id;
END;
$$ LANGUAGE plpgsql;

-- Function to get system health status
CREATE OR REPLACE FUNCTION get_system_health_status()
RETURNS TABLE (
    overall_status VARCHAR(20),
    health_score DECIMAL(5,2),
    cpu_usage DECIMAL(5,2),
    memory_usage DECIMAL(5,2),
    disk_usage DECIMAL(5,2),
    network_in BIGINT,
    network_out BIGINT,
    active_alerts INTEGER,
    last_updated TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    WITH latest_metrics AS (
        SELECT cpu_usage, memory_usage, disk_usage, network_in, network_out
        FROM performance_metrics
        WHERE metric_type = 'SYSTEM_HEALTH'
        ORDER BY created_at DESC
        LIMIT 1
    ),
    recent_alerts AS (
        SELECT COUNT(*) as alert_count
        FROM performance_alerts
        WHERE is_resolved = FALSE
        AND created_at >= NOW() - INTERVAL '24 hours'
    ),
    health_score AS (
        SELECT 
            GREATEST(0, 100 - 
                CASE WHEN lm.cpu_usage > 90 THEN 20
                     WHEN lm.cpu_usage > 70 THEN 10
                     ELSE 0 END -
                CASE WHEN lm.memory_usage > 95 THEN 20
                     WHEN lm.memory_usage > 80 THEN 10
                     ELSE 0 END -
                CASE WHEN lm.disk_usage > 95 THEN 20
                     WHEN lm.disk_usage > 80 THEN 10
                     ELSE 0 END -
                (ra.alert_count * 5)
            ) as score
        FROM latest_metrics lm, recent_alerts ra
    )
    SELECT 
        CASE 
            WHEN hs.score >= 90 THEN 'EXCELLENT'
            WHEN hs.score >= 75 THEN 'GOOD'
            WHEN hs.score >= 60 THEN 'FAIR'
            WHEN hs.score >= 40 THEN 'POOR'
            ELSE 'CRITICAL'
        END as overall_status,
        hs.score as health_score,
        lm.cpu_usage,
        lm.memory_usage,
        lm.disk_usage,
        lm.network_in,
        lm.network_out,
        ra.alert_count as active_alerts,
        NOW() as last_updated
    FROM latest_metrics lm, recent_alerts ra, health_score hs;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate performance trends
CREATE OR REPLACE FUNCTION calculate_performance_trends(
    p_metric_type VARCHAR(50),
    p_start_time TIMESTAMP WITH TIME ZONE,
    p_end_time TIMESTAMP WITH TIME ZONE,
    p_granularity VARCHAR(10) DEFAULT '1h'
)
RETURNS TABLE (
    time_bucket TIMESTAMP WITH TIME ZONE,
    average_value DECIMAL(10,2),
    min_value DECIMAL(10,2),
    max_value DECIMAL(10,2),
    count_samples INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        date_trunc(p_granularity, created_at) as time_bucket,
        AVG(
            CASE 
                WHEN metric_type = 'API' THEN response_time::DECIMAL
                WHEN metric_type = 'DATABASE' THEN execution_time::DECIMAL
                WHEN metric_type = 'SYSTEM_HEALTH' THEN cpu_usage
                ELSE 0
            END
        ) as average_value,
        MIN(
            CASE 
                WHEN metric_type = 'API' THEN response_time::DECIMAL
                WHEN metric_type = 'DATABASE' THEN execution_time::DECIMAL
                WHEN metric_type = 'SYSTEM_HEALTH' THEN cpu_usage
                ELSE 0
            END
        ) as min_value,
        MAX(
            CASE 
                WHEN metric_type = 'API' THEN response_time::DECIMAL
                WHEN metric_type = 'DATABASE' THEN execution_time::DECIMAL
                WHEN metric_type = 'SYSTEM_HEALTH' THEN cpu_usage
                ELSE 0
            END
        ) as max_value,
        COUNT(*) as count_samples
    FROM performance_metrics
    WHERE metric_type = p_metric_type
    AND created_at BETWEEN p_start_time AND p_end_time
    GROUP BY date_trunc(p_granularity, created_at)
    ORDER BY time_bucket;
END;
$$ LANGUAGE plpgsql;

-- Function to identify performance bottlenecks
CREATE OR REPLACE FUNCTION identify_performance_bottlenecks(
    p_start_time TIMESTAMP WITH TIME ZONE DEFAULT NOW() - INTERVAL '24 hours',
    p_end_time TIMESTAMP WITH TIME ZONE DEFAULT NOW()
)
RETURNS TABLE (
    bottleneck_type VARCHAR(50),
    resource VARCHAR(255),
    average_response_time DECIMAL(10,2),
    max_response_time BIGINT,
    occurrence_count INTEGER,
    severity VARCHAR(20)
) AS $$
BEGIN
    RETURN QUERY
    -- API bottlenecks
    SELECT 
        'API' as bottleneck_type,
        endpoint as resource,
        AVG(response_time::DECIMAL) as average_response_time,
        MAX(response_time) as max_response_time,
        COUNT(*) as occurrence_count,
        CASE 
            WHEN AVG(response_time) > 5000 THEN 'HIGH'
            WHEN AVG(response_time) > 2000 THEN 'MEDIUM'
            ELSE 'LOW'
        END as severity
    FROM performance_metrics
    WHERE metric_type = 'API'
    AND created_at BETWEEN p_start_time AND p_end_time
    AND response_time > 2000
    GROUP BY endpoint
    
    UNION ALL
    
    -- Database bottlenecks
    SELECT 
        'DATABASE' as bottleneck_type,
        query_type as resource,
        AVG(execution_time::DECIMAL) as average_response_time,
        MAX(execution_time) as max_response_time,
        COUNT(*) as occurrence_count,
        CASE 
            WHEN AVG(execution_time) > 3000 THEN 'HIGH'
            WHEN AVG(execution_time) > 1000 THEN 'MEDIUM'
            ELSE 'LOW'
        END as severity
    FROM performance_metrics
    WHERE metric_type = 'DATABASE'
    AND created_at BETWEEN p_start_time AND p_end_time
    AND execution_time > 1000
    GROUP BY query_type
    
    ORDER BY average_response_time DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old performance metrics
CREATE OR REPLACE FUNCTION cleanup_performance_metrics(
    p_days_old INTEGER DEFAULT 30
)
RETURNS INTEGER AS $$
BEGIN
    DELETE FROM performance_metrics
    WHERE created_at < NOW() - INTERVAL '1 day' * p_days_old;
    
    RETURN ROW_COUNT;
END;
$$ LANGUAGE plpgsql;

-- Function to aggregate performance metrics into trends
CREATE OR REPLACE FUNCTION aggregate_performance_metrics()
RETURNS INTEGER AS $$
DECLARE
    aggregated_count INTEGER := 0;
BEGIN
    -- Aggregate API metrics by hour
    INSERT INTO performance_trends (metric_type, resource, time_bucket, granularity, average_value, min_value, max_value, count_samples)
    SELECT 
        'API' as metric_type,
        endpoint as resource,
        date_trunc('hour', created_at) as time_bucket,
        '1h' as granularity,
        AVG(response_time::DECIMAL) as average_value,
        MIN(response_time::DECIMAL) as min_value,
        MAX(response_time::DECIMAL) as max_value,
        COUNT(*) as count_samples
    FROM performance_metrics
    WHERE metric_type = 'API'
    AND created_at >= NOW() - INTERVAL '1 hour'
    GROUP BY endpoint, date_trunc('hour', created_at)
    ON CONFLICT (metric_type, resource, time_bucket, granularity) 
    DO UPDATE SET 
        average_value = EXCLUDED.average_value,
        min_value = EXCLUDED.min_value,
        max_value = EXCLUDED.max_value,
        count_samples = EXCLUDED.count_samples;
    
    GET DIAGNOSTICS aggregated_count = ROW_COUNT;
    
    -- Aggregate database metrics by hour
    INSERT INTO performance_trends (metric_type, resource, time_bucket, granularity, average_value, min_value, max_value, count_samples)
    SELECT 
        'DATABASE' as metric_type,
        query_type as resource,
        date_trunc('hour', created_at) as time_bucket,
        '1h' as granularity,
        AVG(execution_time::DECIMAL) as average_value,
        MIN(execution_time::DECIMAL) as min_value,
        MAX(execution_time::DECIMAL) as max_value,
        COUNT(*) as count_samples
    FROM performance_metrics
    WHERE metric_type = 'DATABASE'
    AND created_at >= NOW() - INTERVAL '1 hour'
    GROUP BY query_type, date_trunc('hour', created_at)
    ON CONFLICT (metric_type, resource, time_bucket, granularity) 
    DO UPDATE SET 
        average_value = EXCLUDED.average_value,
        min_value = EXCLUDED.min_value,
        max_value = EXCLUDED.max_value,
        count_samples = EXCLUDED.count_samples;
    
    GET DIAGNOSTICS aggregated_count = aggregated_count + ROW_COUNT;
    
    RETURN aggregated_count;
END;
$$ LANGUAGE plpgsql;

-- Schedule aggregation function (run every 5 minutes)
-- SELECT cron.schedule('aggregate-performance-metrics', '*/5 * * * *', 'SELECT aggregate_performance_metrics();');

-- Schedule cleanup function (run daily)
-- SELECT cron.schedule('cleanup-performance-metrics', '0 2 * * *', 'SELECT cleanup_performance_metrics();');

-- Schedule system health summary (run every 10 minutes)
-- SELECT cron.schedule('update-system-health', '*/10 * * * *', 'INSERT INTO system_health_summary SELECT * FROM get_system_health_status();');
