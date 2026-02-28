-- Export/Import tables for Phase 20: Excel & PDF Reports

-- Report Schedules table
CREATE TABLE IF NOT EXISTS report_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_type VARCHAR(50) NOT NULL, -- 'DOSSIER_LIST', 'PAYMENT_SCHEDULE', 'ANALYTICS', etc.
    format VARCHAR(20) NOT NULL, -- 'EXCEL', 'PDF', 'CSV'
    schedule_type VARCHAR(20) NOT NULL, -- 'DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM'
    schedule_config JSONB NOT NULL, -- Schedule configuration
    recipients TEXT[] NOT NULL, -- Email recipients
    filters JSONB, -- Report filters
    is_active BOOLEAN DEFAULT TRUE,
    last_run TIMESTAMP WITH TIME ZONE,
    next_run TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Export History table
CREATE TABLE IF NOT EXISTS export_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_type VARCHAR(50) NOT NULL,
    format VARCHAR(20) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_path VARCHAR(500),
    filters JSONB,
    generated_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    downloaded_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE
);

-- Import History table
CREATE TABLE IF NOT EXISTS import_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    import_type VARCHAR(50) NOT NULL, -- 'DOSSIER_BULK', 'PAYMENT_BULK', etc.
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_path VARCHAR(500),
    total_rows INTEGER NOT NULL,
    successful_imports INTEGER NOT NULL,
    failed_imports INTEGER NOT NULL,
    import_mapping JSONB, -- Field mapping configuration
    errors JSONB, -- Import errors
    imported_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    imported_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Import Results table (detailed results for each import)
CREATE TABLE IF NOT EXISTS import_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    import_history_id UUID REFERENCES import_history(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'SUCCESS', 'ERROR', 'WARNING'
    entity_type VARCHAR(50), -- 'DOSSIER', 'PAYMENT', etc.
    entity_id UUID, -- ID of created entity
    message TEXT,
    data JSONB, -- Imported data
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Report Templates table
CREATE TABLE IF NOT EXISTS report_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    format VARCHAR(20) NOT NULL,
    template_config JSONB NOT NULL, -- Template configuration
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_report_schedules_type ON report_schedules(report_type);
CREATE INDEX IF NOT EXISTS idx_report_schedules_active ON report_schedules(is_active);
CREATE INDEX IF NOT EXISTS idx_report_schedules_next_run ON report_schedules(next_run);
CREATE INDEX IF NOT EXISTS idx_report_schedules_created_by ON report_schedules(created_by);

CREATE INDEX IF NOT EXISTS idx_export_history_type ON export_history(report_type);
CREATE INDEX IF NOT EXISTS idx_export_history_generated_by ON export_history(generated_by);
CREATE INDEX IF NOT EXISTS idx_export_history_generated_at ON export_history(generated_at);
CREATE INDEX IF NOT EXISTS idx_export_history_expires_at ON export_history(expires_at);

CREATE INDEX IF NOT EXISTS idx_import_history_type ON import_history(import_type);
CREATE INDEX IF NOT EXISTS idx_import_history_imported_by ON import_history(imported_by);
CREATE INDEX IF NOT EXISTS idx_import_history_imported_at ON import_history(imported_at);

CREATE INDEX IF NOT EXISTS idx_import_results_history_id ON import_results(import_history_id);
CREATE INDEX IF NOT EXISTS idx_import_results_status ON import_results(status);
CREATE INDEX IF NOT EXISTS idx_import_results_entity_id ON import_results(entity_id);

CREATE INDEX IF NOT EXISTS idx_report_templates_type ON report_templates(report_type);
CREATE INDEX IF NOT EXISTS idx_report_templates_active ON report_templates(is_active);
CREATE INDEX IF NOT EXISTS idx_report_templates_default ON report_templates(is_default);

-- RLS Policies for report schedules
ALTER TABLE report_schedules ENABLE ROW LEVEL SECURITY;

-- Admin can manage report schedules
CREATE POLICY "Admin can manage report schedules" ON report_schedules
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view report schedules
CREATE POLICY "BOD can view report schedules" ON report_schedules
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view their own report schedules
CREATE POLICY "Users can view own report schedules" ON report_schedules
    FOR SELECT USING (auth.uid()::text = created_by::text);

-- RLS Policies for export history
ALTER TABLE export_history ENABLE ROW LEVEL SECURITY;

-- Admin can manage export history
CREATE POLICY "Admin can manage export history" ON export_history
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view export history
CREATE POLICY "BOD can view export history" ON export_history
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view their own export history
CREATE POLICY "Users can view own export history" ON export_history
    FOR SELECT USING (auth.uid()::text = generated_by::text);

-- RLS Policies for import history
ALTER TABLE import_history ENABLE ROW LEVEL SECURITY;

-- Admin can manage import history
CREATE POLICY "Admin can manage import history" ON import_history
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view import history
CREATE POLICY "BOD can view import history" ON import_history
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view their own import history
CREATE POLICY "Users can view own import history" ON import_history
    FOR SELECT USING (auth.uid()::text = imported_by::text);

-- RLS Policies for import results
ALTER TABLE import_results ENABLE ROW LEVEL SECURITY;

-- Admin can manage import results
CREATE POLICY "Admin can manage import results" ON import_results
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view import results
CREATE POLICY "BOD can view import results" ON import_results
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view import results for their imports
CREATE POLICY "Users can view own import results" ON import_results
    FOR SELECT USING (
        auth.uid()::text IN (
            SELECT imported_by::text FROM import_history WHERE id = import_history_id
        )
    );

-- RLS Policies for report templates
ALTER TABLE report_templates ENABLE ROW LEVEL SECURITY;

-- Admin can manage report templates
CREATE POLICY "Admin can manage report templates" ON report_templates
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view report templates
CREATE POLICY "BOD can view report templates" ON report_templates
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Function to log export
CREATE OR REPLACE FUNCTION log_export(
    p_report_type VARCHAR(50),
    p_format VARCHAR(20),
    p_file_name VARCHAR(255),
    p_file_size BIGINT,
    p_file_path VARCHAR(500),
    p_filters JSONB DEFAULT NULL,
    p_generated_by UUID DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    export_id UUID;
BEGIN
    INSERT INTO export_history (
        report_type, format, file_name, file_size, file_path,
        filters, generated_by, generated_at, expires_at
    ) VALUES (
        p_report_type, p_format, p_file_name, p_file_size, p_file_path,
        p_filters, p_generated_by, NOW(), NOW() + INTERVAL '30 days'
    ) RETURNING id INTO export_id;
    
    RETURN export_id;
END;
$$ LANGUAGE plpgsql;

-- Function to log import
CREATE OR REPLACE FUNCTION log_import(
    p_import_type VARCHAR(50),
    p_file_name VARCHAR(255),
    p_file_size BIGINT,
    p_file_path VARCHAR(500),
    p_total_rows INTEGER,
    p_successful_imports INTEGER,
    p_failed_imports INTEGER,
    p_import_mapping JSONB DEFAULT NULL,
    p_errors JSONB DEFAULT NULL,
    p_imported_by UUID DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    import_id UUID;
BEGIN
    INSERT INTO import_history (
        import_type, file_name, file_size, file_path,
        total_rows, successful_imports, failed_imports,
        import_mapping, errors, imported_by, imported_at
    ) VALUES (
        p_import_type, p_file_name, p_file_size, p_file_path,
        p_total_rows, p_successful_imports, p_failed_imports,
        p_import_mapping, p_errors, p_imported_by, NOW()
    ) RETURNING id INTO import_id;
    
    RETURN import_id;
END;
$$ LANGUAGE plpgsql;

-- Function to log import result
CREATE OR REPLACE FUNCTION log_import_result(
    p_import_history_id UUID,
    p_row_number INTEGER,
    p_status VARCHAR(20),
    p_entity_type VARCHAR(50) DEFAULT NULL,
    p_entity_id UUID DEFAULT NULL,
    p_message TEXT DEFAULT NULL,
    p_data JSONB DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    result_id UUID;
BEGIN
    INSERT INTO import_results (
        import_history_id, row_number, status, entity_type,
        entity_id, message, data, created_at
    ) VALUES (
        p_import_history_id, p_row_number, p_status, p_entity_type,
        p_entity_id, p_message, p_data, NOW()
    ) RETURNING id INTO result_id;
    
    RETURN result_id;
END;
$$ LANGUAGE plpgsql;

-- Function to get export statistics
CREATE OR REPLACE FUNCTION get_export_statistics(
    p_start_date TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    p_end_date TIMESTAMP WITH TIME ZONE DEFAULT NULL
)
RETURNS TABLE (
    total_exports BIGINT,
    total_file_size BIGINT,
    report_type_breakdown JSONB,
    format_breakdown JSONB,
    top_exporters JSONB
) AS $$
BEGIN
    RETURN QUERY
    WITH export_stats AS (
        SELECT 
            COUNT(*) as total_exports,
            SUM(file_size) as total_file_size
        FROM export_history
        WHERE 
            (p_start_date IS NULL OR generated_at >= p_start_date)
            AND (p_end_date IS NULL OR generated_at <= p_end_date)
    ),
    type_breakdown AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'report_type', report_type,
                'count', type_count,
                'percentage', ROUND((type_count::DECIMAL / export_stats.total_exports::DECIMAL) * 100, 2)
            )
        ) as type_breakdown
        FROM export_stats, (
            SELECT report_type, COUNT(*) as type_count
            FROM export_history
            WHERE 
                (p_start_date IS NULL OR generated_at >= p_start_date)
                AND (p_end_date IS NULL OR generated_at <= p_end_date)
            GROUP BY report_type
        ) tb
    ),
    format_breakdown AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'format', format,
                'count', format_count,
                'percentage', ROUND((format_count::DECIMAL / export_stats.total_exports::DECIMAL) * 100, 2)
            )
        ) as format_breakdown
        FROM export_stats, (
            SELECT format, COUNT(*) as format_count
            FROM export_history
            WHERE 
                (p_start_date IS NULL OR generated_at >= p_start_date)
                AND (p_end_date IS NULL OR generated_at <= p_end_date)
            GROUP BY format
        ) fb
    ),
    top_exporters AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'user_id', generated_by,
                'export_count', export_count
            )
        ) as top_exporters
        FROM (
            SELECT generated_by, COUNT(*) as export_count
            FROM export_history
            WHERE 
                (p_start_date IS NULL OR generated_at >= p_start_date)
                AND (p_end_date IS NULL OR generated_at <= p_end_date)
            GROUP BY generated_by
            ORDER BY export_count DESC
            LIMIT 10
        ) te
    )
    SELECT 
        es.total_exports,
        es.total_file_size,
        tb.type_breakdown,
        fb.format_breakdown,
        te.top_exporters
    FROM export_stats es, type_breakdown tb, format_breakdown fb, top_exporters te;
END;
$$ LANGUAGE plpgsql;

-- Function to get import statistics
CREATE OR REPLACE FUNCTION get_import_statistics(
    p_start_date TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    p_end_date TIMESTAMP WITH TIME ZONE DEFAULT NULL
)
RETURNS TABLE (
    total_imports BIGINT,
    total_rows BIGINT,
    successful_imports BIGINT,
    failed_imports BIGINT,
    success_rate DECIMAL(5,2),
    import_type_breakdown JSONB,
    top_importers JSONB
) AS $$
BEGIN
    RETURN QUERY
    WITH import_stats AS (
        SELECT 
            COUNT(*) as total_imports,
            SUM(total_rows) as total_rows,
            SUM(successful_imports) as successful_imports,
            SUM(failed_imports) as failed_imports
        FROM import_history
        WHERE 
            (p_start_date IS NULL OR imported_at >= p_start_date)
            AND (p_end_date IS NULL OR imported_at <= p_end_date)
    ),
    type_breakdown AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'import_type', import_type,
                'count', type_count,
                'percentage', ROUND((type_count::DECIMAL / import_stats.total_imports::DECIMAL) * 100, 2)
            )
        ) as type_breakdown
        FROM import_stats, (
            SELECT import_type, COUNT(*) as type_count
            FROM import_history
            WHERE 
                (p_start_date IS NULL OR imported_at >= p_start_date)
                AND (p_end_date IS NULL OR imported_at <= p_end_date)
            GROUP BY import_type
        ) tb
    ),
    top_importers AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'user_id', imported_by,
                'import_count', import_count
            )
        ) as top_importers
        FROM (
            SELECT imported_by, COUNT(*) as import_count
            FROM import_history
            WHERE 
                (p_start_date IS NULL OR imported_at >= p_start_date)
                AND (p_end_date IS NULL OR imported_at <= p_end_date)
            GROUP BY imported_by
            ORDER BY import_count DESC
            LIMIT 10
        ) ti
    )
    SELECT 
        is.total_imports,
        is.total_rows,
        is.successful_imports,
        is.failed_imports,
        CASE 
            WHEN is.total_rows > 0 THEN 
                ROUND((is.successful_imports::DECIMAL / is.total_rows::DECIMAL) * 100, 2)
            ELSE 0 
        END as success_rate,
        tb.type_breakdown,
        ti.top_importers
    FROM import_stats is, type_breakdown tb, top_importers ti;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old export files
CREATE OR REPLACE FUNCTION cleanup_old_exports(
    p_days_old INTEGER DEFAULT 30
)
RETURNS INTEGER AS $$
BEGIN
    DELETE FROM export_history
    WHERE expires_at < NOW();
    
    RETURN ROW_COUNT;
END;
$$ LANGUAGE plpgsql;

-- Function to update next run time for schedules
CREATE OR REPLACE FUNCTION update_schedule_next_run()
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    -- Update next run time based on schedule type
    UPDATE report_schedules
    SET next_run = CASE 
        WHEN schedule_type = 'DAILY' THEN NOW() + INTERVAL '1 day'
        WHEN schedule_type = 'WEEKLY' THEN NOW() + INTERVAL '1 week'
        WHEN schedule_type = 'MONTHLY' THEN NOW() + INTERVAL '1 month'
        ELSE next_run -- Keep existing for custom schedules
    END,
    last_run = NOW()
    WHERE is_active = TRUE
    AND next_run <= NOW();
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_report_schedules_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_report_schedules_updated_at BEFORE UPDATE ON report_schedules FOR EACH ROW EXECUTE FUNCTION update_report_schedules_updated_at();

CREATE OR REPLACE FUNCTION update_report_templates_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_report_templates_updated_at BEFORE UPDATE ON report_templates FOR EACH ROW EXECUTE FUNCTION update_report_templates_updated_at();

-- Schedule cleanup function (run daily)
-- SELECT cron.schedule('cleanup-old-exports', '0 2 * * *', 'SELECT cleanup_old_exports();');

-- Schedule next run update (run every hour)
-- SELECT cron.schedule('update-schedule-next-run', '0 * * * *', 'SELECT update_schedule_next_run();');
