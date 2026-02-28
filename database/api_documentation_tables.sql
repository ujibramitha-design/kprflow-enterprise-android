-- API Documentation tables for Phase 23: Swagger/OpenAPI Integration

-- API Endpoints table
CREATE TABLE IF NOT EXISTS api_endpoints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    path VARCHAR(500) NOT NULL,
    method VARCHAR(10) NOT NULL, -- 'GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'
    summary VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL, -- 'AUTHENTICATION', 'DOSSIER', 'DOCUMENT', 'PAYMENT', etc.
    parameters JSONB, -- Array of parameter objects
    request_body JSONB, -- Request body schema and example
    responses JSONB NOT NULL, -- Response schemas by status code
    tags TEXT[], -- Array of tags
    deprecated BOOLEAN DEFAULT FALSE,
    deprecation_reason TEXT,
    deprecation_date TIMESTAMP WITH TIME ZONE,
    alternative_endpoint VARCHAR(500),
    version VARCHAR(10) NOT NULL DEFAULT 'v1',
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(path, method, version)
);

-- API Schemas table
CREATE TABLE IF NOT EXISTS api_schemas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'object', 'array', 'string', 'number', 'boolean', 'integer'
    properties JSONB, -- Schema properties
    required TEXT[], -- Required fields
    description TEXT,
    example JSONB,
    version VARCHAR(10) NOT NULL DEFAULT 'v1',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(name, version)
);

-- API Examples table
CREATE TABLE IF NOT EXISTS api_examples (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    endpoint_path VARCHAR(500) NOT NULL,
    method VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    example_data JSONB NOT NULL,
    request_headers JSONB,
    response_status INTEGER,
    response_body JSONB,
    version VARCHAR(10) NOT NULL DEFAULT 'v1',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- API Documentation table
CREATE TABLE IF NOT EXISTS api_documentation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    version VARCHAR(10) NOT NULL,
    doc_type VARCHAR(20) NOT NULL, -- 'OPENAPI', 'SWAGGER', 'POSTMAN', 'RAML'
    content JSONB NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    checksum VARCHAR(128),
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);

-- API Test Results table
CREATE TABLE IF NOT EXISTS api_test_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    endpoint_id UUID REFERENCES api_endpoints(id) ON DELETE CASCADE,
    status_code INTEGER NOT NULL,
    response_time BIGINT NOT NULL, -- in milliseconds
    success BOOLEAN NOT NULL,
    error_message TEXT,
    test_data JSONB,
    response_data JSONB,
    tested_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    tested_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- API Usage Statistics table
CREATE TABLE IF NOT EXISTS api_usage_statistics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    endpoint_path VARCHAR(500) NOT NULL,
    method VARCHAR(10) NOT NULL,
    request_count BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    average_response_time DECIMAL(10,2), -- in milliseconds
    last_accessed TIMESTAMP WITH TIME ZONE,
    date_bucket DATE NOT NULL, -- Daily aggregation
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(endpoint_path, method, date_bucket)
);

-- API Version Management table
CREATE TABLE IF NOT EXISTS api_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    version VARCHAR(10) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL, -- 'DEVELOPMENT', 'STABLE', 'DEPRECATED', 'SUNSET'
    release_date TIMESTAMP WITH TIME ZONE,
    deprecation_date TIMESTAMP WITH TIME ZONE,
    sunset_date TIMESTAMP WITH TIME ZONE,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_api_endpoints_path ON api_endpoints(path);
CREATE INDEX IF NOT EXISTS idx_api_endpoints_method ON api_endpoints(method);
CREATE INDEX IF NOT EXISTS idx_api_endpoints_category ON api_endpoints(category);
CREATE INDEX IF NOT EXISTS idx_api_endpoints_version ON api_endpoints(version);
CREATE INDEX IF NOT EXISTS idx_api_endpoints_deprecated ON api_endpoints(deprecated);
CREATE INDEX IF NOT EXISTS idx_api_endpoints_active ON api_endpoints(is_active);

CREATE INDEX IF NOT EXISTS idx_api_schemas_name ON api_schemas(name);
CREATE INDEX IF NOT EXISTS idx_api_schemas_version ON api_schemas(version);
CREATE INDEX IF NOT EXISTS idx_api_schemas_type ON api_schemas(type);
CREATE INDEX IF NOT EXISTS idx_api_schemas_active ON api_schemas(is_active);

CREATE INDEX IF NOT EXISTS idx_api_examples_endpoint ON api_examples(endpoint_path, method);
CREATE INDEX IF NOT EXISTS idx_api_examples_version ON api_examples(version);
CREATE INDEX IF NOT EXISTS idx_api_examples_active ON api_examples(is_active);

CREATE INDEX IF NOT EXISTS idx_api_documentation_version ON api_documentation(version);
CREATE INDEX IF NOT EXISTS idx_api_documentation_type ON api_documentation(doc_type);
CREATE INDEX IF NOT EXISTS idx_api_documentation_active ON api_documentation(is_active);

CREATE INDEX IF NOT EXISTS idx_api_test_results_endpoint ON api_test_results(endpoint_id);
CREATE INDEX IF NOT EXISTS idx_api_test_results_tested_at ON api_test_results(tested_at);
CREATE INDEX IF NOT EXISTS idx_api_test_results_success ON api_test_results(success);

CREATE INDEX IF NOT EXISTS idx_api_usage_endpoint ON api_usage_statistics(endpoint_path, method);
CREATE INDEX IF NOT EXISTS idx_api_usage_date_bucket ON api_usage_statistics(date_bucket);
CREATE INDEX IF NOT EXISTS idx_api_usage_last_accessed ON api_usage_statistics(last_accessed);

CREATE INDEX IF NOT EXISTS idx_api_versions_status ON api_versions(status);
CREATE INDEX IF NOT EXISTS idx_api_versions_active ON api_versions(is_active);

-- RLS Policies for API endpoints
ALTER TABLE api_endpoints ENABLE ROW LEVEL SECURITY;

-- Admin can manage API endpoints
CREATE POLICY "Admin can manage API endpoints" ON api_endpoints
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view API endpoints
CREATE POLICY "BOD can view API endpoints" ON api_endpoints
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view API endpoints
CREATE POLICY "Users can view API endpoints" ON api_endpoints
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for API schemas
ALTER TABLE api_schemas ENABLE ROW LEVEL SECURITY;

-- Admin can manage API schemas
CREATE POLICY "Admin can manage API schemas" ON api_schemas
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view API schemas
CREATE POLICY "BOD can view API schemas" ON api_schemas
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view API schemas
CREATE POLICY "Users can view API schemas" ON api_schemas
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for API examples
ALTER TABLE api_examples ENABLE ROW LEVEL SECURITY;

-- Admin can manage API examples
CREATE POLICY "Admin can manage API examples" ON api_examples
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view API examples
CREATE POLICY "BOD can view API examples" ON api_examples
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view API examples
CREATE POLICY "Users can view API examples" ON api_examples
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for API documentation
ALTER TABLE api_documentation ENABLE ROW LEVEL SECURITY;

-- Admin can manage API documentation
CREATE POLICY "Admin can manage API documentation" ON api_documentation
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view API documentation
CREATE POLICY "BOD can view API documentation" ON api_documentation
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view API documentation
CREATE POLICY "Users can view API documentation" ON api_documentation
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for API test results
ALTER TABLE api_test_results ENABLE ROW LEVEL SECURITY;

-- Admin can manage API test results
CREATE POLICY "Admin can manage API test results" ON api_test_results
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view API test results
CREATE POLICY "BOD can view API test results" ON api_test_results
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view their own API test results
CREATE POLICY "Users can view own API test results" ON api_test_results
    FOR SELECT USING (auth.uid()::text = tested_by::text);

-- RLS Policies for API usage statistics
ALTER TABLE api_usage_statistics ENABLE ROW LEVEL SECURITY;

-- Admin can manage API usage statistics
CREATE POLICY "Admin can manage API usage statistics" ON api_usage_statistics
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view API usage statistics
CREATE POLICY "BOD can view API usage statistics" ON api_usage_statistics
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- RLS Policies for API versions
ALTER TABLE api_versions ENABLE ROW LEVEL SECURITY;

-- Admin can manage API versions
CREATE POLICY "Admin can manage API versions" ON api_versions
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view API versions
CREATE POLICY "BOD can view API versions" ON api_versions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view API versions
CREATE POLICY "Users can view API versions" ON api_versions
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- Function to register API endpoint
CREATE OR REPLACE FUNCTION register_api_endpoint(
    p_path VARCHAR(500),
    p_method VARCHAR(10),
    p_summary VARCHAR(255),
    p_description TEXT DEFAULT NULL,
    p_category VARCHAR(50),
    p_parameters JSONB DEFAULT NULL,
    p_request_body JSONB DEFAULT NULL,
    p_responses JSONB,
    p_tags TEXT[] DEFAULT NULL,
    p_deprecated BOOLEAN DEFAULT FALSE,
    p_version VARCHAR(10) DEFAULT 'v1',
    p_created_by UUID DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    endpoint_id UUID;
BEGIN
    INSERT INTO api_endpoints (
        path, method, summary, description, category,
        parameters, request_body, responses, tags,
        deprecated, version, created_by
    ) VALUES (
        p_path, p_method, p_summary, p_description, p_category,
        p_parameters, p_request_body, p_responses, p_tags,
        p_deprecated, p_version, p_created_by
    ) RETURNING id INTO endpoint_id;
    
    RETURN endpoint_id;
END;
$$ LANGUAGE plpgsql;

-- Function to deprecate API endpoint
CREATE OR REPLACE FUNCTION deprecate_api_endpoint(
    p_endpoint_id UUID,
    p_deprecation_reason TEXT,
    p_deprecation_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    p_alternative_endpoint VARCHAR(500) DEFAULT NULL
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE api_endpoints
    SET 
        deprecated = TRUE,
        deprecation_reason = p_deprecation_reason,
        deprecation_date = p_deprecation_date,
        alternative_endpoint = p_alternative_endpoint,
        updated_at = NOW()
    WHERE id = p_endpoint_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to record API usage
CREATE OR REPLACE FUNCTION record_api_usage(
    p_endpoint_path VARCHAR(500),
    p_method VARCHAR(10),
    p_status_code INTEGER,
    p_response_time BIGINT,
    p_success BOOLEAN,
    p_error_message TEXT DEFAULT NULL
)
RETURNS VOID AS $$
BEGIN
    -- Update daily usage statistics
    INSERT INTO api_usage_statistics (
        endpoint_path, method, request_count, success_count, error_count,
        average_response_time, last_accessed, date_bucket
    ) VALUES (
        p_endpoint_path, p_method, 1, 
        CASE WHEN p_success THEN 1 ELSE 0 END,
        CASE WHEN p_success THEN 0 ELSE 1 END,
        p_response_time::DECIMAL, NOW(), CURRENT_DATE
    )
    ON CONFLICT (endpoint_path, method, date_bucket)
    DO UPDATE SET
        request_count = api_usage_statistics.request_count + 1,
        success_count = api_usage_statistics.success_count + CASE WHEN p_success THEN 1 ELSE 0 END,
        error_count = api_usage_statistics.error_count + CASE WHEN p_success THEN 0 ELSE 1 END,
        average_response_time = (
            (api_usage_statistics.average_response_time * api_usage_statistics.request_count) + p_response_time
        ) / (api_usage_statistics.request_count + 1),
        last_accessed = NOW(),
        updated_at = NOW();
END;
$$ LANGUAGE plpgsql;

-- Function to get API usage statistics
CREATE OR REPLACE FUNCTION get_api_usage_statistics(
    p_start_date DATE DEFAULT NULL,
    p_end_date DATE DEFAULT NULL,
    p_version VARCHAR(10) DEFAULT 'v1'
)
RETURNS TABLE (
    total_requests BIGINT,
    successful_requests BIGINT,
    failed_requests BIGINT,
    average_response_time DECIMAL(10,2),
    top_endpoints JSONB,
    error_breakdown JSONB,
    category_breakdown JSONB
) AS $$
BEGIN
    RETURN QUERY
    WITH usage_stats AS (
        SELECT 
            COUNT(*) as total_requests,
            COUNT(*) FILTER (WHERE success_count > 0) as successful_requests,
            COUNT(*) FILTER (WHERE error_count > 0) as failed_requests,
            AVG(average_response_time) as average_response_time
        FROM api_usage_statistics
        WHERE 
            (p_start_date IS NULL OR date_bucket >= p_start_date)
            AND (p_end_date IS NULL OR date_bucket <= p_end_date)
    ),
    top_endpoints AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'path', endpoint_path,
                'method', method,
                'count', total_requests,
                'success_rate', ROUND((success_count::DECIMAL / request_count::DECIMAL) * 100, 2)
            )
        ) as top_endpoints
        FROM (
            SELECT 
                endpoint_path, method,
                SUM(request_count) as total_requests,
                SUM(success_count) as success_count,
                SUM(request_count) as request_count
            FROM api_usage_statistics
            WHERE 
                (p_start_date IS NULL OR date_bucket >= p_start_date)
                AND (p_end_date IS NULL OR date_bucket <= p_end_date)
            GROUP BY endpoint_path, method
            ORDER BY total_requests DESC
            LIMIT 10
        ) te
    ),
    error_breakdown AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'status_code', status_code,
                'count', error_count
            )
        ) as error_breakdown
        FROM (
            SELECT 
                status_code,
                COUNT(*) as error_count
            FROM api_test_results
            WHERE success = FALSE
            AND (p_start_date IS NULL OR tested_at::DATE >= p_start_date)
            AND (p_end_date IS NULL OR tested_at::DATE <= p_end_date)
            GROUP BY status_code
            ORDER BY error_count DESC
        ) eb
    ),
    category_breakdown AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'category', category,
                'count', category_requests,
                'success_rate', ROUND((category_success::DECIMAL / category_requests::DECIMAL) * 100, 2)
            )
        ) as category_breakdown
        FROM (
            SELECT 
                ae.category,
                COUNT(*) as category_requests,
                COUNT(*) FILTER (WHERE success_count > 0) as category_success
            FROM api_usage_statistics aus
            JOIN api_endpoints ae ON aus.endpoint_path = ae.path AND aus.method = ae.method
            WHERE 
                (p_start_date IS NULL OR aus.date_bucket >= p_start_date)
                AND (p_end_date IS NULL OR aus.date_bucket <= p_end_date)
                AND ae.version = p_version
            GROUP BY ae.category
            ORDER BY category_requests DESC
        ) cb
    )
    SELECT 
        us.total_requests,
        us.successful_requests,
        us.failed_requests,
        us.average_response_time,
        te.top_endpoints,
        eb.error_breakdown,
        cb.category_breakdown
    FROM usage_stats us, top_endpoints te, error_breakdown eb, category_breakdown cb;
END;
$$ LANGUAGE plpgsql;

-- Function to validate API documentation
CREATE OR REPLACE FUNCTION validate_api_documentation(
    p_version VARCHAR(10) DEFAULT 'v1',
    p_doc_type VARCHAR(20) DEFAULT 'OPENAPI'
)
RETURNS TABLE (
    is_valid BOOLEAN,
    issues JSONB,
    warnings JSONB
) AS $$
DECLARE
    doc_record RECORD;
    issues_list JSONB DEFAULT '[]'::JSONB;
    warnings_list JSONB DEFAULT '[]'::JSONB;
BEGIN
    -- Get documentation record
    SELECT * INTO doc_record
    FROM api_documentation
    WHERE version = p_version AND doc_type = p_doc_type AND is_active = TRUE;
    
    IF NOT FOUND THEN
        RETURN QUERY SELECT FALSE, 
            jsonb_build_array('Documentation not found')::JSONB, 
            '[]'::JSONB;
    END IF;
    
    -- Validate based on documentation type
    IF p_doc_type = 'OPENAPI' THEN
        -- Check for required OpenAPI fields
        IF NOT (doc_record.content ? 'openapi') THEN
            issues_list := issues_list || jsonb_build_array('Missing openapi version');
        END IF;
        
        IF NOT (doc_record.content ? 'info') THEN
            issues_list := issues_list || jsonb_build_array('Missing info object');
        END IF;
        
        IF NOT (doc_record.content ? 'paths') THEN
            issues_list := issues_list || jsonb_build_array('Missing paths object');
        END IF;
        
        -- Check for empty paths
        IF jsonb_array_length(doc_record.content -> 'paths') = 0 THEN
            issues_list := issues_list || jsonb_build_array('No paths defined');
        END IF;
        
        -- Check for missing security schemes
        IF NOT (doc_record.content ? 'components' -> 'securitySchemes') THEN
            warnings_list := warnings_list || jsonb_build_array('No security schemes defined');
        END IF;
    END IF;
    
    RETURN QUERY SELECT 
        jsonb_array_length(issues_list) = 0 as is_valid,
        issues_list as issues,
        warnings_list as warnings;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old API usage statistics
CREATE OR REPLACE FUNCTION cleanup_api_usage_statistics(
    p_days_old INTEGER DEFAULT 365
)
RETURNS INTEGER AS $$
BEGIN
    DELETE FROM api_usage_statistics
    WHERE date_bucket < CURRENT_DATE - INTERVAL '1 day' * p_days_old;
    
    RETURN ROW_COUNT;
END;
$$ LANGUAGE plpgsql;

-- Function to aggregate API usage statistics
CREATE OR REPLACE FUNCTION aggregate_api_usage_statistics()
RETURNS INTEGER AS $$
DECLARE
    aggregated_count INTEGER := 0;
BEGIN
    -- This function could be used to aggregate statistics into different time buckets
    -- For now, it's a placeholder for future implementation
    
    -- Example: Aggregate daily stats into weekly stats
    INSERT INTO api_usage_statistics (endpoint_path, method, request_count, success_count, error_count, average_response_time, date_bucket)
    SELECT 
        endpoint_path,
        method,
        SUM(request_count),
        SUM(success_count),
        SUM(error_count),
        AVG(average_response_time),
        date_trunc('week', date_bucket)::DATE
    FROM api_usage_statistics
    WHERE date_bucket >= CURRENT_DATE - INTERVAL '7 days'
    GROUP BY endpoint_path, method, date_trunc('week', date_bucket)
    ON CONFLICT (endpoint_path, method, date_bucket)
    DO UPDATE SET
        request_count = api_usage_statistics.request_count + EXCLUDED.request_count,
        success_count = api_usage_statistics.success_count + EXCLUDED.success_count,
        error_count = api_usage_statistics.error_count + EXCLUDED.error_count,
        average_response_time = (
            (api_usage_statistics.average_response_time * api_usage_statistics.request_count) + 
            (EXCLUDED.average_response_time * EXCLUDED.request_count)
        ) / (api_usage_statistics.request_count + EXCLUDED.request_count),
        updated_at = NOW();
    
    GET DIAGNOSTICS aggregated_count = ROW_COUNT;
    
    RETURN aggregated_count;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_api_endpoints_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_api_endpoints_updated_at BEFORE UPDATE ON api_endpoints FOR EACH ROW EXECUTE FUNCTION update_api_endpoints_updated_at();

CREATE OR REPLACE FUNCTION update_api_schemas_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_api_schemas_updated_at BEFORE UPDATE ON api_schemas FOR EACH ROW EXECUTE FUNCTION update_api_schemas_updated_at();

CREATE OR REPLACE FUNCTION update_api_examples_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE OR REPLACE FUNCTION update_api_usage_statistics_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE OR REPLACE FUNCTION update_api_versions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Insert default API version
INSERT INTO api_versions (version, status, description) VALUES
('v1', 'STABLE', 'Current stable API version'),
('v2', 'DEVELOPMENT', 'Next generation API in development')
ON CONFLICT (version) DO NOTHING;

-- Schedule cleanup function (run monthly)
-- SELECT cron.schedule('cleanup-api-usage-stats', '0 2 1 * *', 'SELECT cleanup_api_usage_statistics();');

-- Schedule aggregation function (run daily)
-- SELECT cron.schedule('aggregate-api-usage-stats', '0 1 * * *', 'SELECT aggregate_api_usage_statistics();');
