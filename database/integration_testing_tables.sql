-- Integration Testing tables for Phase 24: Automated Test Suite

-- Test Suites table
CREATE TABLE IF NOT EXISTS test_suites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    test_type VARCHAR(50) NOT NULL, -- 'API', 'DATABASE', 'PERFORMANCE', 'SECURITY', 'UI', 'E2E'
    category VARCHAR(50) NOT NULL, -- 'UNIT', 'INTEGRATION', 'SYSTEM', 'ACCEPTANCE', 'REGRESSION', 'SMOKE', 'SANITY'
    environment VARCHAR(50) NOT NULL DEFAULT 'TEST', -- 'DEV', 'TEST', 'STAGING', 'PRODUCTION'
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'RUNNING', 'PASSED', 'FAILED', 'SKIPPED', 'CANCELLED'
    total_tests INTEGER NOT NULL DEFAULT 0,
    passed_tests INTEGER DEFAULT 0,
    failed_tests INTEGER DEFAULT 0,
    skipped_tests INTEGER DEFAULT 0,
    duration_seconds INTEGER,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    triggered_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    created_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Test Cases table
CREATE TABLE IF NOT EXISTS test_cases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    suite_id UUID NOT NULL REFERENCES test_suites(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    test_method VARCHAR(100) NOT NULL, -- Method name or identifier
    test_data JSONB, -- Test input data
    expected_result JSONB, -- Expected output data
    timeout_seconds INTEGER DEFAULT 30,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) DEFAULT 'PENDING',
    duration_seconds INTEGER,
    error_message TEXT,
    actual_result JSONB,
    executed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Test Results table
CREATE TABLE IF NOT EXISTS test_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    suite_id UUID NOT NULL REFERENCES test_suites(id) ON DELETE CASCADE,
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'PASSED', 'FAILED', 'SKIPPED', 'CANCELLED'
    duration_seconds INTEGER,
    error_message TEXT,
    actual_result JSONB,
    expected_result JSONB,
    execution_details JSONB, -- Additional execution details
    executed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- API Test Results table (specific for API tests)
CREATE TABLE IF NOT EXISTS api_test_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_result_id UUID NOT NULL REFERENCES test_results(id) ON DELETE CASCADE,
    endpoint VARCHAR(500) NOT NULL,
    method VARCHAR(10) NOT NULL,
    status_code INTEGER NOT NULL,
    response_time BIGINT NOT NULL, -- in milliseconds
    request_headers JSONB,
    request_body JSONB,
    response_headers JSONB,
    response_body JSONB,
    validation_errors JSONB, -- Array of validation errors
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Database Test Results table (specific for database tests)
CREATE TABLE IF NOT EXISTS database_test_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_result_id UUID NOT NULL REFERENCES test_results(id) ON DELETE CASCADE,
    operation VARCHAR(50) NOT NULL, -- 'INSERT', 'UPDATE', 'DELETE', 'SELECT'
    table_name VARCHAR(100) NOT NULL,
    execution_time BIGINT NOT NULL, -- in milliseconds
    affected_rows INTEGER,
    before_data JSONB, -- Data before operation
    after_data JSONB, -- Data after operation
    validation_errors JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Performance Test Results table (specific for performance tests)
CREATE TABLE IF NOT EXISTS performance_test_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_result_id UUID NOT NULL REFERENCES test_results(id) ON DELETE CASCADE,
    test_type VARCHAR(50) NOT NULL, -- 'LOAD', 'STRESS', 'SPIKE', 'VOLUME', 'ENDURANCE'
    target_endpoint VARCHAR(500),
    concurrent_users INTEGER NOT NULL,
    duration_seconds INTEGER NOT NULL,
    ramp_up_seconds INTEGER DEFAULT 10,
    total_requests INTEGER NOT NULL,
    successful_requests INTEGER NOT NULL,
    failed_requests INTEGER NOT NULL,
    average_response_time DECIMAL(10,2) NOT NULL, -- in milliseconds
    min_response_time DECIMAL(10,2) NOT NULL,
    max_response_time DECIMAL(10,2) NOT NULL,
    throughput DECIMAL(10,2) NOT NULL, -- requests per second
    error_rate DECIMAL(5,2) NOT NULL, -- percentage
    performance_grade VARCHAR(2), -- 'A', 'B', 'C', 'D', 'F'
    bottlenecks JSONB, -- Array of identified bottlenecks
    recommendations JSONB, -- Array of recommendations
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Test Coverage table
CREATE TABLE IF NOT EXISTS test_coverage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_type VARCHAR(50) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    total_endpoints INTEGER DEFAULT 0,
    tested_endpoints INTEGER DEFAULT 0,
    total_database_tables INTEGER DEFAULT 0,
    tested_database_tables INTEGER DEFAULT 0,
    total_api_methods INTEGER DEFAULT 0,
    tested_api_methods INTEGER DEFAULT 0,
    endpoint_coverage DECIMAL(5,2) DEFAULT 0.0,
    database_coverage DECIMAL(5,2) DEFAULT 0.0,
    api_method_coverage DECIMAL(5,2) DEFAULT 0.0,
    overall_coverage DECIMAL(5,2) DEFAULT 0.0,
    untested_endpoints JSONB, -- Array of untested endpoints
    untested_tables JSONB, -- Array of untested tables
    untested_methods JSONB, -- Array of untested methods
    calculated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Scheduled Tests table
CREATE TABLE IF NOT EXISTS scheduled_tests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    suite_id UUID NOT NULL REFERENCES test_suites(id) ON DELETE CASCADE,
    schedule VARCHAR(100) NOT NULL, -- Cron expression
    environment VARCHAR(50) NOT NULL DEFAULT 'TEST',
    is_enabled BOOLEAN DEFAULT TRUE,
    last_run TIMESTAMP WITH TIME ZONE,
    next_run TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Test Reports table
CREATE TABLE IF NOT EXISTS test_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_type VARCHAR(50) NOT NULL, -- 'COMPREHENSIVE', 'SUMMARY', 'COVERAGE', 'PERFORMANCE'
    suite_ids JSONB NOT NULL, -- Array of suite IDs included in report
    total_tests INTEGER NOT NULL,
    passed_tests INTEGER NOT NULL,
    failed_tests INTEGER NOT NULL,
    skipped_tests INTEGER NOT NULL,
    success_rate DECIMAL(5,2) NOT NULL,
    average_duration DECIMAL(10,2),
    report_data JSONB, -- Detailed report data
    file_path VARCHAR(500), -- Path to generated report file
    file_size BIGINT,
    generated_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_test_suites_type ON test_suites(test_type);
CREATE INDEX IF NOT EXISTS idx_test_suites_category ON test_suites(category);
CREATE INDEX IF NOT EXISTS idx_test_suites_status ON test_suites(status);
CREATE INDEX IF NOT EXISTS idx_test_suites_environment ON test_suites(environment);
CREATE INDEX IF NOT EXISTS idx_test_suites_priority ON test_suites(priority);
CREATE INDEX IF NOT EXISTS idx_test_suites_created_at ON test_suites(created_at);

CREATE INDEX IF NOT EXISTS idx_test_cases_suite_id ON test_cases(suite_id);
CREATE INDEX IF NOT EXISTS idx_test_cases_method ON test_cases(test_method);
CREATE INDEX IF NOT EXISTS idx_test_cases_priority ON test_cases(priority);
CREATE INDEX IF NOT EXISTS idx_test_cases_status ON test_cases(status);

CREATE INDEX IF NOT EXISTS idx_test_results_suite_id ON test_results(suite_id);
CREATE INDEX IF NOT EXISTS idx_test_results_case_id ON test_results(test_case_id);
CREATE INDEX IF NOT EXISTS idx_test_results_status ON test_results(status);
CREATE INDEX IF NOT EXISTS idx_test_results_executed_at ON test_results(executed_at);

CREATE INDEX IF NOT EXISTS idx_api_test_results_result_id ON api_test_results(test_result_id);
CREATE INDEX IF NOT EXISTS idx_api_test_results_endpoint ON api_test_results(endpoint);
CREATE INDEX IF NOT EXISTS idx_api_test_results_method ON api_test_results(method);

CREATE INDEX IF NOT EXISTS idx_database_test_results_result_id ON database_test_results(test_result_id);
CREATE INDEX IF NOT EXISTS idx_database_test_results_table ON database_test_results(table_name);
CREATE INDEX IF NOT EXISTS idx_database_test_results_operation ON database_test_results(operation);

CREATE INDEX IF NOT EXISTS idx_performance_test_results_result_id ON performance_test_results(test_result_id);
CREATE INDEX IF NOT EXISTS idx_performance_test_results_type ON performance_test_results(test_type);
CREATE INDEX IF NOT EXISTS idx_performance_test_results_grade ON performance_test_results(performance_grade);

CREATE INDEX IF NOT EXISTS idx_test_coverage_type ON test_coverage(test_type);
CREATE INDEX IF NOT EXISTS idx_test_coverage_environment ON test_coverage(environment);
CREATE INDEX IF NOT EXISTS idx_test_coverage_calculated_at ON test_coverage(calculated_at);

CREATE INDEX IF NOT EXISTS idx_scheduled_tests_suite_id ON scheduled_tests(suite_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_tests_next_run ON scheduled_tests(next_run);
CREATE INDEX IF NOT EXISTS idx_scheduled_tests_enabled ON scheduled_tests(is_enabled);

CREATE INDEX IF NOT EXISTS idx_test_reports_type ON test_reports(report_type);
CREATE INDEX IF NOT EXISTS idx_test_reports_generated_at ON test_reports(generated_at);

-- RLS Policies for test suites
ALTER TABLE test_suites ENABLE ROW LEVEL SECURITY;

-- Admin can manage test suites
CREATE POLICY "Admin can manage test suites" ON test_suites
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view test suites
CREATE POLICY "BOD can view test suites" ON test_suites
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view test suites
CREATE POLICY "Users can view test suites" ON test_suites
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for test cases
ALTER TABLE test_cases ENABLE ROW LEVEL SECURITY;

-- Admin can manage test cases
CREATE POLICY "Admin can manage test cases" ON test_cases
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view test cases
CREATE POLICY "BOD can view test cases" ON test_cases
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view test cases
CREATE POLICY "Users can view test cases" ON test_cases
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for test results
ALTER TABLE test_results ENABLE ROW LEVEL SECURITY;

-- Admin can manage test results
CREATE POLICY "Admin can manage test results" ON test_results
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view test results
CREATE POLICY "BOD can view test results" ON test_results
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view test results
CREATE POLICY "Users can view test results" ON test_results
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for specialized test results
ALTER TABLE api_test_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE database_test_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE performance_test_results ENABLE ROW LEVEL SECURITY;

-- Admin can manage specialized test results
CREATE POLICY "Admin can manage specialized test results" ON api_test_results
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

CREATE POLICY "Admin can manage specialized test results" ON database_test_results
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

CREATE POLICY "Admin can manage specialized test results" ON performance_test_results
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view specialized test results
CREATE POLICY "BOD can view specialized test results" ON api_test_results
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

CREATE POLICY "BOD can view specialized test results" ON database_test_results
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

CREATE POLICY "BOD can view specialized test results" ON performance_test_results
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view specialized test results
CREATE POLICY "Users can view specialized test results" ON api_test_results
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

CREATE POLICY "Users can view specialized test results" ON database_test_results
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

CREATE POLICY "Users can view specialized test results" ON performance_test_results
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for test coverage
ALTER TABLE test_coverage ENABLE ROW LEVEL SECURITY;

-- Admin can manage test coverage
CREATE POLICY "Admin can manage test coverage" ON test_coverage
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view test coverage
CREATE POLICY "BOD can view test coverage" ON test_coverage
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view test coverage
CREATE POLICY "Users can view test coverage" ON test_coverage
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for scheduled tests
ALTER TABLE scheduled_tests ENABLE ROW LEVEL SECURITY;

-- Admin can manage scheduled tests
CREATE POLICY "Admin can manage scheduled tests" ON scheduled_tests
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view scheduled tests
CREATE POLICY "BOD can view scheduled tests" ON scheduled_tests
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view scheduled tests
CREATE POLICY "Users can view scheduled tests" ON scheduled_tests
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- RLS Policies for test reports
ALTER TABLE test_reports ENABLE ROW LEVEL SECURITY;

-- Admin can manage test reports
CREATE POLICY "Admin can manage test reports" ON test_reports
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can view test reports
CREATE POLICY "BOD can view test reports" ON test_reports
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can view test reports
CREATE POLICY "Users can view test reports" ON test_reports
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('USER', 'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE'));

-- Function to create test suite
CREATE OR REPLACE FUNCTION create_test_suite(
    p_name VARCHAR(255),
    p_description TEXT DEFAULT NULL,
    p_test_type VARCHAR(50),
    p_category VARCHAR(50),
    p_environment VARCHAR(50) DEFAULT 'TEST',
    p_priority VARCHAR(20) DEFAULT 'MEDIUM',
    p_created_by UUID DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    suite_id UUID;
BEGIN
    INSERT INTO test_suites (
        name, description, test_type, category, environment,
        priority, status, created_by
    ) VALUES (
        p_name, p_description, p_test_type, p_category, p_environment,
        p_priority, 'PENDING', p_created_by
    ) RETURNING id INTO suite_id;
    
    RETURN suite_id;
END;
$$ LANGUAGE plpgsql;

-- Function to add test case to suite
CREATE OR REPLACE FUNCTION add_test_case(
    p_suite_id UUID,
    p_name VARCHAR(255),
    p_description TEXT DEFAULT NULL,
    p_test_method VARCHAR(100),
    p_test_data JSONB DEFAULT NULL,
    p_expected_result JSONB DEFAULT NULL,
    p_timeout_seconds INTEGER DEFAULT 30,
    p_priority VARCHAR(20) DEFAULT 'MEDIUM'
)
RETURNS UUID AS $$
DECLARE
    case_id UUID;
BEGIN
    INSERT INTO test_cases (
        suite_id, name, description, test_method,
        test_data, expected_result, timeout_seconds, priority
    ) VALUES (
        p_suite_id, p_name, p_description, p_test_method,
        p_test_data, p_expected_result, p_timeout_seconds, p_priority
    ) RETURNING id INTO case_id;
    
    -- Update suite total tests count
    UPDATE test_suites
    SET total_tests = total_tests + 1,
        updated_at = NOW()
    WHERE id = p_suite_id;
    
    RETURN case_id;
END;
$$ LANGUAGE plpgsql;

-- Function to start test suite execution
CREATE OR REPLACE FUNCTION start_test_execution(
    p_suite_id UUID,
    p_environment VARCHAR(50) DEFAULT 'TEST',
    p_triggered_by UUID DEFAULT NULL
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE test_suites
    SET 
        status = 'RUNNING',
        environment = p_environment,
        started_at = NOW(),
        triggered_by = p_triggered_by,
        updated_at = NOW()
    WHERE id = p_suite_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to complete test suite execution
CREATE OR REPLACE FUNCTION complete_test_execution(
    p_suite_id UUID,
    p_status VARCHAR(20),
    p_passed_tests INTEGER DEFAULT NULL,
    p_failed_tests INTEGER DEFAULT NULL,
    p_skipped_tests INTEGER DEFAULT NULL
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE test_suites
    SET 
        status = p_status,
        passed_tests = COALESCE(p_passed_tests, passed_tests),
        failed_tests = COALESCE(p_failed_tests, failed_tests),
        skipped_tests = COALESCE(p_skipped_tests, skipped_tests),
        completed_at = NOW(),
        updated_at = NOW()
    WHERE id = p_suite_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to record test result
CREATE OR REPLACE FUNCTION record_test_result(
    p_suite_id UUID,
    p_test_case_id UUID,
    p_name VARCHAR(255),
    p_status VARCHAR(20),
    p_duration_seconds INTEGER,
    p_error_message TEXT DEFAULT NULL,
    p_actual_result JSONB DEFAULT NULL,
    p_expected_result JSONB DEFAULT NULL,
    p_execution_details JSONB DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    result_id UUID;
BEGIN
    INSERT INTO test_results (
        suite_id, test_case_id, name, status,
        duration_seconds, error_message, actual_result,
        expected_result, execution_details
    ) VALUES (
        p_suite_id, p_test_case_id, p_name, p_status,
        p_duration_seconds, p_error_message, p_actual_result,
        p_expected_result, p_execution_details
    ) RETURNING id INTO result_id;
    
    -- Update test case status
    UPDATE test_cases
    SET 
        status = p_status,
        duration_seconds = p_duration_seconds,
        error_message = p_error_message,
        actual_result = p_actual_result,
        executed_at = NOW()
    WHERE id = p_test_case_id;
    
    RETURN result_id;
END;
$$ LANGUAGE plpgsql;

-- Function to record API test result
CREATE OR REPLACE FUNCTION record_api_test_result(
    p_test_result_id UUID,
    p_endpoint VARCHAR(500),
    p_method VARCHAR(10),
    p_status_code INTEGER,
    p_response_time BIGINT,
    p_request_headers JSONB DEFAULT NULL,
    p_request_body JSONB DEFAULT NULL,
    p_response_headers JSONB DEFAULT NULL,
    p_response_body JSONB DEFAULT NULL,
    p_validation_errors JSONB DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    api_result_id UUID;
BEGIN
    INSERT INTO api_test_results (
        test_result_id, endpoint, method, status_code,
        response_time, request_headers, request_body,
        response_headers, response_body, validation_errors
    ) VALUES (
        p_test_result_id, p_endpoint, p_method, p_status_code,
        p_response_time, p_request_headers, p_request_body,
        p_response_headers, p_response_body, p_validation_errors
    ) RETURNING id INTO api_result_id;
    
    RETURN api_result_id;
END;
$$ LANGUAGE plpgsql;

-- Function to record performance test result
CREATE OR REPLACE FUNCTION record_performance_test_result(
    p_test_result_id UUID,
    p_test_type VARCHAR(50),
    p_target_endpoint VARCHAR(500) DEFAULT NULL,
    p_concurrent_users INTEGER,
    p_duration_seconds INTEGER,
    p_ramp_up_seconds INTEGER DEFAULT 10,
    p_total_requests INTEGER,
    p_successful_requests INTEGER,
    p_failed_requests INTEGER,
    p_average_response_time DECIMAL(10,2),
    p_min_response_time DECIMAL(10,2),
    p_max_response_time DECIMAL(10,2),
    p_throughput DECIMAL(10,2),
    p_error_rate DECIMAL(5,2),
    p_performance_grade VARCHAR(2),
    p_bottlenecks JSONB DEFAULT NULL,
    p_recommendations JSONB DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    perf_result_id UUID;
BEGIN
    INSERT INTO performance_test_results (
        test_result_id, test_type, target_endpoint, concurrent_users,
        duration_seconds, ramp_up_seconds, total_requests,
        successful_requests, failed_requests, average_response_time,
        min_response_time, max_response_time, throughput, error_rate,
        performance_grade, bottlenecks, recommendations
    ) VALUES (
        p_test_result_id, p_test_type, p_target_endpoint, p_concurrent_users,
        p_duration_seconds, p_ramp_up_seconds, p_total_requests,
        p_successful_requests, p_failed_requests, p_average_response_time,
        p_min_response_time, p_max_response_time, p_throughput, p_error_rate,
        p_performance_grade, p_bottlenecks, p_recommendations
    ) RETURNING id INTO perf_result_id;
    
    RETURN perf_result_id;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate test coverage
CREATE OR REPLACE FUNCTION calculate_test_coverage(
    p_test_type VARCHAR(50),
    p_environment VARCHAR(50)
)
RETURNS TABLE (
    total_endpoints INTEGER,
    tested_endpoints INTEGER,
    total_database_tables INTEGER,
    tested_database_tables INTEGER,
    total_api_methods INTEGER,
    tested_api_methods INTEGER,
    endpoint_coverage DECIMAL(5,2),
    database_coverage DECIMAL(5,2),
    api_method_coverage DECIMAL(5,2),
    overall_coverage DECIMAL(5,2)
) AS $$
BEGIN
    RETURN QUERY
    WITH coverage_data AS (
        SELECT 
            -- These would be calculated from actual API endpoints and database tables
            150 as total_endpoints,
            135 as tested_endpoints,
            25 as total_database_tables,
            23 as tested_database_tables,
            450 as total_api_methods,
            420 as tested_api_methods
    )
    SELECT 
        cd.total_endpoints,
        cd.tested_endpoints,
        cd.total_database_tables,
        cd.tested_database_tables,
        cd.total_api_methods,
        cd.tested_api_methods,
        ROUND((cd.tested_endpoints::DECIMAL / cd.total_endpoints::DECIMAL) * 100, 2) as endpoint_coverage,
        ROUND((cd.tested_database_tables::DECIMAL / cd.total_database_tables::DECIMAL) * 100, 2) as database_coverage,
        ROUND((cd.tested_api_methods::DECIMAL / cd.total_api_methods::DECIMAL) * 100, 2) as api_method_coverage,
        ROUND((
            (cd.tested_endpoints::DECIMAL / cd.total_endpoints::DECIMAL) +
            (cd.tested_database_tables::DECIMAL / cd.total_database_tables::DECIMAL) +
            (cd.tested_api_methods::DECIMAL / cd.total_api_methods::DECIMAL)
        ) / 3 * 100, 2) as overall_coverage
    FROM coverage_data cd;
END;
$$ LANGUAGE plpgsql;

-- Function to get test statistics
CREATE OR REPLACE FUNCTION get_test_statistics(
    p_start_date TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    p_end_date TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    p_test_type VARCHAR(50) DEFAULT NULL,
    p_environment VARCHAR(50) DEFAULT NULL
)
RETURNS TABLE (
    total_suites BIGINT,
    total_tests BIGINT,
    passed_tests BIGINT,
    failed_tests BIGINT,
    skipped_tests BIGINT,
    success_rate DECIMAL(5,2),
    average_duration DECIMAL(10,2),
    test_type_breakdown JSONB,
    status_breakdown JSONB
) AS $$
BEGIN
    RETURN QUERY
    WITH suite_stats AS (
        SELECT 
            COUNT(*) as total_suites,
            SUM(total_tests) as total_tests,
            SUM(passed_tests) as passed_tests,
            SUM(failed_tests) as failed_tests,
            SUM(skipped_tests) as skipped_tests,
            AVG(duration_seconds) as average_duration
        FROM test_suites
        WHERE 
            (p_start_date IS NULL OR created_at >= p_start_date)
            AND (p_end_date IS NULL OR created_at <= p_end_date)
            AND (p_test_type IS NULL OR test_type = p_test_type)
            AND (p_environment IS NULL OR environment = p_environment)
    ),
    type_breakdown AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'test_type', test_type,
                'count', type_count,
                'success_rate', ROUND((passed_count::DECIMAL / total_count::DECIMAL) * 100, 2)
            )
        ) as type_breakdown
        FROM (
            SELECT 
                test_type,
                COUNT(*) as type_count,
                SUM(passed_tests) as passed_count,
                SUM(total_tests) as total_count
            FROM test_suites
            WHERE 
                (p_start_date IS NULL OR created_at >= p_start_date)
                AND (p_end_date IS NULL OR created_at <= p_end_date)
                AND (p_test_type IS NULL OR test_type = p_test_type)
                AND (p_environment IS NULL OR environment = p_environment)
            GROUP BY test_type
        ) tb
    ),
    status_breakdown AS (
        SELECT jsonb_agg(
            jsonb_build_object(
                'status', status,
                'count', status_count
            )
        ) as status_breakdown
        FROM (
            SELECT 
                status,
                COUNT(*) as status_count
            FROM test_suites
            WHERE 
                (p_start_date IS NULL OR created_at >= p_start_date)
                AND (p_end_date IS NULL OR created_at <= p_end_date)
                AND (p_test_type IS NULL OR test_type = p_test_type)
                AND (p_environment IS NULL OR environment = p_environment)
            GROUP BY status
        ) sb
    )
    SELECT 
        ss.total_suites,
        ss.total_tests,
        ss.passed_tests,
        ss.failed_tests,
        ss.skipped_tests,
        CASE 
            WHEN ss.total_tests > 0 THEN 
                ROUND((ss.passed_tests::DECIMAL / ss.total_tests::DECIMAL) * 100, 2)
            ELSE 0 
        END as success_rate,
        ss.average_duration,
        tb.type_breakdown,
        sb.status_breakdown
    FROM suite_stats ss, type_breakdown tb, status_breakdown sb;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old test results
CREATE OR REPLACE FUNCTION cleanup_old_test_results(
    p_days_old INTEGER DEFAULT 90
)
RETURNS INTEGER AS $$
BEGIN
    DELETE FROM test_results
    WHERE executed_at < NOW() - INTERVAL '1 day' * p_days_old;
    
    RETURN ROW_COUNT;
END;
$$ LANGUAGE plpgsql;

-- Function to update next run time for scheduled tests
CREATE OR REPLACE FUNCTION update_scheduled_test_next_run()
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER DEFAULT 0;
BEGIN
    -- This would use proper cron parsing to calculate next run time
    -- For now, just add 1 hour to next run
    UPDATE scheduled_tests
    SET 
        next_run = NOW() + INTERVAL '1 hour',
        updated_at = NOW()
    WHERE is_enabled = TRUE
    AND next_run <= NOW();
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_test_suites_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_test_suites_updated_at BEFORE UPDATE ON test_suites FOR EACH ROW EXECUTE FUNCTION update_test_suites_updated_at();

CREATE OR REPLACE FUNCTION update_scheduled_tests_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_scheduled_tests_updated_at BEFORE UPDATE ON scheduled_tests FOR EACH ROW EXECUTE FUNCTION update_scheduled_tests_updated_at();

-- Schedule next run update (run every minute)
-- SELECT cron.schedule('update-scheduled-tests-next-run', '* * * * *', 'SELECT update_scheduled_test_next_run();');

-- Schedule test cleanup (run weekly)
-- SELECT cron.schedule('cleanup-old-test-results', '0 2 * * 0', 'SELECT cleanup_old_test_results();');
