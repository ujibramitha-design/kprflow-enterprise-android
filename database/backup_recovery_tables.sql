-- Backup & Recovery tables for Phase 22: Data Protection System

-- Backup Schedules table
CREATE TABLE IF NOT EXISTS backup_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    backup_type VARCHAR(50) NOT NULL, -- 'DATABASE', 'FILES', 'FULL', 'INCREMENTAL', 'DIFFERENTIAL'
    frequency VARCHAR(20) NOT NULL, -- 'HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY'
    retention_days INTEGER NOT NULL,
    storage_location VARCHAR(50) NOT NULL, -- 'LOCAL', 'CLOUD', 'HYBRID'
    include_files TEXT[], -- Array of file types to include
    compression_enabled BOOLEAN DEFAULT TRUE,
    encryption_enabled BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    next_run TIMESTAMP WITH TIME ZONE NOT NULL,
    last_run TIMESTAMP WITH TIME ZONE,
    created_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Backup History table
CREATE TABLE IF NOT EXISTS backup_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    backup_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL, -- 'DATABASE', 'FILES'
    status VARCHAR(20) NOT NULL, -- 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED', 'DELETED'
    backup_size BIGINT NOT NULL,
    duration_seconds INTEGER,
    file_path VARCHAR(500),
    checksum VARCHAR(128),
    verification_status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'VERIFIED', 'FAILED'
    compression_enabled BOOLEAN DEFAULT TRUE,
    encryption_enabled BOOLEAN DEFAULT TRUE,
    storage_location VARCHAR(50) NOT NULL,
    description TEXT,
    triggered_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    verified_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Restore History table
CREATE TABLE IF NOT EXISTS restore_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    backup_id UUID NOT NULL REFERENCES backup_history(id) ON DELETE CASCADE,
    restore_type VARCHAR(20) NOT NULL, -- 'FULL', 'PARTIAL', 'SELECTIVE'
    target_location VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'
    restore_size BIGINT,
    duration_seconds INTEGER,
    triggered_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Backup Verification Results table
CREATE TABLE IF NOT EXISTS backup_verification_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    backup_id UUID NOT NULL REFERENCES backup_history(id) ON DELETE CASCADE,
    verification_type VARCHAR(50) NOT NULL, -- 'CHECKSUM', 'INTEGRITY', 'RESTORE_TEST'
    status VARCHAR(20) NOT NULL, -- 'PENDING', 'VERIFIED', 'FAILED'
    checksum VARCHAR(128),
    issues JSONB,
    verification_time TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Backup Storage Locations table
CREATE TABLE IF NOT EXISTS backup_storage_locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'LOCAL', 'CLOUD', 'HYBRID'
    config JSONB NOT NULL, -- Storage configuration
    is_active BOOLEAN DEFAULT TRUE,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Backup Retention Policies table
CREATE TABLE IF NOT EXISTS backup_retention_policies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    backup_type VARCHAR(50) NOT NULL,
    policy_name VARCHAR(100) NOT NULL,
    retention_days INTEGER NOT NULL,
    auto_cleanup BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_backup_schedules_type ON backup_schedules(backup_type);
CREATE INDEX IF NOT EXISTS idx_backup_schedules_active ON backup_schedules(is_active);
CREATE INDEX IF NOT EXISTS idx_backup_schedules_next_run ON backup_schedules(next_run);
CREATE INDEX IF NOT EXISTS idx_backup_schedules_created_by ON backup_schedules(created_by);

CREATE INDEX IF NOT EXISTS idx_backup_history_type ON backup_history(backup_type);
CREATE INDEX IF NOT EXISTS idx_backup_history_status ON backup_history(status);
CREATE INDEX IF NOT EXISTS idx_backup_history_started_at ON backup_history(started_at);
CREATE INDEX IF NOT EXISTS idx_backup_history_target_type ON backup_history(target_type);
CREATE INDEX IF NOT EXISTS idx_backup_history_storage ON backup_history(storage_location);

CREATE INDEX IF NOT EXISTS idx_restore_history_backup_id ON restore_history(backup_id);
CREATE INDEX IF NOT EXISTS idx_restore_history_status ON restore_history(status);
CREATE INDEX IF NOT EXISTS idx_restore_history_started_at ON restore_history(started_at);

CREATE INDEX IF NOT EXISTS idx_backup_verification_backup_id ON backup_verification_results(backup_id);
CREATE INDEX IF NOT EXISTS idx_backup_verification_status ON backup_verification_results(status);
CREATE INDEX IF NOT EXISTS idx_backup_verification_type ON backup_verification_results(verification_type);

CREATE INDEX IF NOT EXISTS idx_backup_storage_locations_type ON backup_storage_locations(type);
CREATE INDEX IF NOT EXISTS idx_backup_storage_locations_active ON backup_storage_locations(is_active);

CREATE INDEX IF NOT EXISTS idx_backup_retention_policies_type ON backup_retention_policies(backup_type);
CREATE INDEX IF NOT EXISTS idx_backup_retention_policies_active ON backup_retention_policies(is_active);

-- RLS Policies for backup schedules
ALTER TABLE backup_schedules ENABLE ROW LEVEL SECURITY;

-- Admin can manage backup schedules
CREATE POLICY "Admin can manage backup schedules" ON backup_schedules
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view backup schedules
CREATE POLICY "BOD can view backup schedules" ON backup_schedules
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view their own backup schedules
CREATE POLICY "Users can view own backup schedules" ON backup_schedules
    FOR SELECT USING (auth.uid()::text = created_by::text);

-- RLS Policies for backup history
ALTER TABLE backup_history ENABLE ROW LEVEL SECURITY;

-- Admin can manage backup history
CREATE POLICY "Admin can manage backup history" ON backup_history
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view backup history
CREATE POLICY "BOD can view backup history" ON backup_history
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view backup history they triggered
CREATE POLICY "Users can view own backup history" ON backup_history
    FOR SELECT USING (auth.uid()::text = triggered_by::text);

-- RLS Policies for restore history
ALTER TABLE restore_history ENABLE ROW LEVEL SECURITY;

-- Admin can manage restore history
CREATE POLICY "Admin can manage restore history" ON restore_history
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view restore history
CREATE POLICY "BOD can view restore history" ON restore_history
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view restore history they triggered
CREATE POLICY "Users can view own restore history" ON restore_history
    FOR SELECT USING (auth.uid()::text = triggered_by::text);

-- RLS Policies for backup verification results
ALTER TABLE backup_verification_results ENABLE ROW LEVEL SECURITY;

-- Admin can manage backup verification results
CREATE POLICY "Admin can manage backup verification results" ON backup_verification_results
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view backup verification results
CREATE POLICY "BOD can view backup verification results" ON backup_verification_results
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- RLS Policies for backup storage locations
ALTER TABLE backup_storage_locations ENABLE ROW LEVEL SECURITY;

-- Admin can manage backup storage locations
CREATE POLICY "Admin can manage backup storage locations" ON backup_storage_locations
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view backup storage locations
CREATE POLICY "BOD can view backup storage locations" ON backup_storage_locations
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- RLS Policies for backup retention policies
ALTER TABLE backup_retention_policies ENABLE ROW LEVEL SECURITY;

-- Admin can manage backup retention policies
CREATE POLICY "Admin can manage backup retention policies" ON backup_retention_policies
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view backup retention policies
CREATE POLICY "BOD can view backup retention policies" ON backup_retention_policies
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Function to create backup schedule
CREATE OR REPLACE FUNCTION create_backup_schedule(
    p_backup_type VARCHAR(50),
    p_frequency VARCHAR(20),
    p_retention_days INTEGER,
    p_storage_location VARCHAR(50),
    p_include_files TEXT[] DEFAULT NULL,
    p_compression_enabled BOOLEAN DEFAULT TRUE,
    p_encryption_enabled BOOLEAN DEFAULT TRUE,
    p_created_by UUID DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    schedule_id UUID;
    next_run_time TIMESTAMP WITH TIME ZONE;
BEGIN
    -- Calculate next run time based on frequency
    next_run_time := CASE 
        WHEN p_frequency = 'HOURLY' THEN NOW() + INTERVAL '1 hour'
        WHEN p_frequency = 'DAILY' THEN NOW() + INTERVAL '1 day'
        WHEN p_frequency = 'WEEKLY' THEN NOW() + INTERVAL '1 week'
        WHEN p_frequency = 'MONTHLY' THEN NOW() + INTERVAL '1 month'
        ELSE NOW() + INTERVAL '1 day'
    END;
    
    INSERT INTO backup_schedules (
        backup_type, frequency, retention_days, storage_location,
        include_files, compression_enabled, encryption_enabled,
        is_active, next_run, created_by
    ) VALUES (
        p_backup_type, p_frequency, p_retention_days, p_storage_location,
        p_include_files, p_compression_enabled, p_encryption_enabled,
        TRUE, next_run_time, p_created_by
    ) RETURNING id INTO schedule_id;
    
    RETURN schedule_id;
END;
$$ LANGUAGE plpgsql;

-- Function to record backup start
CREATE OR REPLACE FUNCTION start_backup(
    p_backup_id UUID,
    p_backup_type VARCHAR(50),
    p_target_type VARCHAR(50),
    p_compression_enabled BOOLEAN DEFAULT TRUE,
    p_encryption_enabled BOOLEAN DEFAULT TRUE,
    p_storage_location VARCHAR(50),
    p_description TEXT DEFAULT NULL,
    p_triggered_by UUID DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    backup_record_id UUID;
BEGIN
    INSERT INTO backup_history (
        id, backup_type, target_type, status,
        compression_enabled, encryption_enabled, storage_location,
        description, triggered_by, started_at
    ) VALUES (
        p_backup_id, p_backup_type, p_target_type, 'IN_PROGRESS',
        p_compression_enabled, p_encryption_enabled, p_storage_location,
        p_description, p_triggered_by, NOW()
    ) RETURNING id INTO backup_record_id;
    
    RETURN backup_record_id;
END;
$$ LANGUAGE plpgsql;

-- Function to complete backup
CREATE OR REPLACE FUNCTION complete_backup(
    p_backup_id UUID,
    p_backup_size BIGINT,
    p_duration_seconds INTEGER,
    p_file_path VARCHAR(500),
    p_checksum VARCHAR(128)
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE backup_history
    SET 
        status = 'COMPLETED',
        backup_size = p_backup_size,
        duration_seconds = p_duration_seconds,
        file_path = p_file_path,
        checksum = p_checksum,
        completed_at = NOW(),
        updated_at = NOW()
    WHERE id = p_backup_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to fail backup
CREATE OR REPLACE FUNCTION fail_backup(
    p_backup_id UUID,
    p_error_message TEXT
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE backup_history
    SET 
        status = 'FAILED',
        completed_at = NOW(),
        updated_at = NOW()
    WHERE id = p_backup_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to start restore
CREATE OR REPLACE FUNCTION start_restore(
    p_backup_id UUID,
    p_restore_type VARCHAR(20),
    p_target_location VARCHAR(100),
    p_triggered_by UUID DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    restore_id UUID;
BEGIN
    INSERT INTO restore_history (
        backup_id, restore_type, target_location, status,
        triggered_by, started_at
    ) VALUES (
        p_backup_id, p_restore_type, p_target_location, 'IN_PROGRESS',
        p_triggered_by, NOW()
    ) RETURNING id INTO restore_id;
    
    RETURN restore_id;
END;
$$ LANGUAGE plpgsql;

-- Function to complete restore
CREATE OR REPLACE FUNCTION complete_restore(
    p_restore_id UUID,
    p_restore_size BIGINT,
    p_duration_seconds INTEGER
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE restore_history
    SET 
        status = 'COMPLETED',
        restore_size = p_restore_size,
        duration_seconds = p_duration_seconds,
        completed_at = NOW(),
        updated_at = NOW()
    WHERE id = p_restore_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to verify backup integrity
CREATE OR REPLACE FUNCTION verify_backup_integrity(
    p_backup_id UUID,
    p_verification_type VARCHAR(50) DEFAULT 'CHECKSUM',
    p_verified_by UUID DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
    verification_id UUID;
    is_valid BOOLEAN DEFAULT FALSE;
    backup_info RECORD;
BEGIN
    -- Get backup information
    SELECT * INTO backup_info
    FROM backup_history
    WHERE id = p_backup_id;
    
    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;
    
    -- Simulate integrity check
    is_valid := (backup_info.checksum IS NOT NULL);
    
    -- Record verification result
    INSERT INTO backup_verification_results (
        backup_id, verification_type, status, checksum,
        verification_time, verified_by
    ) VALUES (
        p_backup_id, p_verification_type, 
        CASE WHEN is_valid THEN 'VERIFIED' ELSE 'FAILED' END,
        backup_info.checksum,
        NOW(), p_verified_by
    ) RETURNING id INTO verification_id;
    
    -- Update backup verification status
    UPDATE backup_history
    SET 
        verification_status = CASE WHEN is_valid THEN 'VERIFIED' ELSE 'FAILED' END,
        verified_at = NOW(),
        updated_at = NOW()
    WHERE id = p_backup_id;
    
    RETURN is_valid;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old backups
CREATE OR REPLACE FUNCTION cleanup_old_backups()
RETURNS TABLE (
    cleaned_backups BIGINT,
    errors TEXT[]
) AS $$
DECLARE
    backup_record RECORD;
    cleanup_count BIGINT DEFAULT 0;
    error_list TEXT[] DEFAULT '{}';
BEGIN
    -- Iterate through backup schedules
    FOR backup_record IN 
        SELECT bs.*, br.id as backup_id
        FROM backup_schedules bs
        JOIN backup_history br ON br.started_at >= NOW() - INTERVAL '1 day' * bs.retention_days
        WHERE bs.is_active = TRUE
    LOOP
        BEGIN
            -- Delete backup file (simulated)
            BEGIN
                -- In a real implementation, this would delete the actual backup file
                -- For now, we just mark it as deleted
                UPDATE backup_history
                SET 
                    status = 'DELETED',
                    deleted_at = NOW(),
                    updated_at = NOW()
                WHERE id = backup_record.backup_id;
                
                cleanup_count := cleanup_count + 1;
            EXCEPTION WHEN OTHERS THEN
                error_list := array_append(error_list, ARRAY[CONCAT('Failed to delete backup: ', backup_record.backup_id)]);
            END;
        END LOOP;
    
    RETURN QUERY SELECT cleanup_count, error_list;
END;
$$ LANGUAGE plpgsql;

-- Function to get backup statistics
CREATE OR REPLACE FUNCTION get_backup_statistics()
RETURNS TABLE (
    total_backups BIGINT,
    successful_backups BIGINT,
    failed_backups BIGINT,
    total_backup_size BIGINT,
    average_backup_size DECIMAL(15,2),
    last_backup_time TIMESTAMP WITH TIME ZONE,
    next_scheduled_backup TIMESTAMP WITH TIME ZONE,
    storage_breakdown JSONB,
    type_breakdown JSONB
) AS $$
BEGIN
    RETURN QUERY
    WITH backup_stats AS (
        SELECT 
            COUNT(*) as total_backups,
            COUNT(*) FILTER (WHERE status = 'COMPLETED') as successful_backups,
            COUNT(*) FILTER (WHERE status = 'FAILED') as failed_backups,
            COALESCECE(SUM(backup_size), 0) as total_backup_size,
            COALESCE(AVG(backup_size), 0) as average_backup_size,
            MAX(started_at) as last_backup_time
        FROM backup_history
        WHERE status != 'DELETED'
    ),
    storage_stats AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'location', storage_location,
                'count', location_count,
                'size', COALESCE(SUM(backup_size), 0)
            )
        ) as storage_breakdown
        FROM backup_history
        WHERE status != 'DELETED'
        GROUP BY storage_location
    ),
    type_stats AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'type', backup_type,
                'count', type_count,
                'size', COALESCE(SUM(backup_size), 0)
            )
        ) as type_breakdown
        FROM backup_history
        WHERE status != 'DELETED'
        GROUP BY backup_type
    ),
    next_backup AS (
        SELECT MIN(next_run) as next_scheduled_backup
        FROM backup_schedules
        WHERE is_active = TRUE
    )
    SELECT 
        bs.total_backups,
        bs.successful_backups,
        bs.failed_backups,
        bs.total_backup_size,
        bs.average_backup_size,
        bs.last_backup_time,
        nb.next_scheduled_backup,
        ss.storage_breakdown,
        ts.type_breakdown
    FROM backup_stats bs, storage_stats ss, type_stats ts, next_backup nb;
END;
$$ LANGUAGE plpgsql;

-- Function to update next run time for schedules
CREATE OR REPLACE FUNCTION update_backup_schedule_next_run()
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER DEFAULT 0;
BEGIN
    -- Update next run time for active schedules
    UPDATE backup_schedules
    SET 
        next_run = CASE 
            WHEN frequency = 'HOURLY' THEN NOW() + INTERVAL '1 hour'
            WHEN frequency = 'DAILY' THEN NOW() + INTERVAL '1 day'
            WHEN frequency = 'WEEKLY' THEN NOW() + INTERVAL '1 week'
            WHEN frequency = 'MONTHLY' THEN NOW() + INTERVAL '1 month'
            ELSE next_run -- Keep existing for custom schedules
        END,
        last_run = NOW(),
        updated_at = NOW()
    WHERE is_active = TRUE
    AND next_run <= NOW();
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_backup_schedules_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_backup_schedules_updated_at BEFORE UPDATE ON backup_schedules FOR EACH ROW EXECUTE FUNCTION update_backup_schedules_updated_at();

CREATE OR REPLACE FUNCTION update_backup_history_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_backup_history_updated_at BEFORE UPDATE ON backup_history FOR EACH ROW EXECUTE FUNCTION update_backup_history_updated_at();

CREATE OR REPLACE FUNCTION update_restore_history_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_restore_history_updated_at BEFORE UPDATE ON restore_history FOR EACH ROW EXECUTE FUNCTION update_restore_history_updated_at();

-- Insert default backup storage locations
INSERT INTO backup_storage_locations (name, type, config, is_default) VALUES
('Local Storage', 'LOCAL', '{"path": "/backups", "max_size_gb": 100}', FALSE),
('AWS S3', 'CLOUD', '{"bucket": "kprflow-backups", "region": "us-east-1", "max_size_gb": 1000}', TRUE),
('Hybrid Storage', 'HYBRID', '{"local": {"path": "/backups", "max_size_gb": 50}, "cloud": {"bucket": "kprflow-backups", "region": "us-east-1", "max_size_gb": 500}}', FALSE)
ON CONFLICT (name) DO NOTHING;

-- Insert default retention policies
INSERT INTO backup_retention_policies (backup_type, policy_name, retention_days, auto_cleanup) VALUES
('DATABASE', 'Daily Database Retention', 30, TRUE),
('FILES', 'Weekly Files Retention', 90, TRUE),
('FULL', 'Monthly Full Backup Retention', 365, TRUE),
('INCREMENTAL', 'Daily Incremental Retention', 7, TRUE),
('DIFFERENTIAL', 'Weekly Differential Retention', 30, TRUE)
ON CONFLICT (backup_type) DO NOTHING;

-- Schedule cleanup function (run daily)
-- SELECT cron.schedule('cleanup-old-backups', '0 2 * * *', 'SELECT * FROM cleanup_old_backups();');

-- Schedule next run update (run every hour)
-- SELECT cron.schedule('update-backup-next-run', '0 * * * *', 'SELECT update_backup_schedule_next_run();');
