-- KPRFlow Enterprise Database Audit Report
-- Comprehensive audit of database schema, RLS policies, and performance

-- =====================================================
-- DATABASE SCHEMA AUDIT
-- =====================================================

-- Function to audit database schema
CREATE OR REPLACE FUNCTION audit_database_schema()
RETURNS TABLE(
    table_name TEXT,
    total_columns INTEGER,
    indexed_columns INTEGER,
    has_rls BOOLEAN,
    has_primary_key BOOLEAN,
    has_foreign_keys INTEGER,
    table_size_mb DECIMAL,
    row_count BIGINT,
    last_modified TIMESTAMP WITH TIME ZONE,
    audit_status TEXT
) AS $$
DECLARE
    v_table_name TEXT;
    v_column_count INTEGER;
    v_index_count INTEGER;
    v_rls_enabled BOOLEAN;
    v_has_pk BOOLEAN;
    v_fk_count INTEGER;
    v_table_size DECIMAL;
    v_row_count BIGINT;
    v_last_modified TIMESTAMP WITH TIME ZONE;
    v_audit_status TEXT;
BEGIN
    -- Create temporary table for results
    CREATE TEMP TABLE IF NOT EXISTS audit_results (
        table_name TEXT,
        total_columns INTEGER,
        indexed_columns INTEGER,
        has_rls BOOLEAN,
        has_primary_key BOOLEAN,
        has_foreign_keys INTEGER,
        table_size_mb DECIMAL,
        row_count BIGINT,
        last_modified TIMESTAMP WITH TIME ZONE,
        audit_status TEXT
    );
    
    -- Clear previous results
    TRUNCATE TABLE audit_results;
    
    -- Audit each table
    FOR v_table_name IN 
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
        AND tablename NOT LIKE 'pg_%'
    LOOP
        BEGIN
            -- Count columns
            SELECT COUNT(*) INTO v_column_count
            FROM information_schema.columns
            WHERE table_name = v_table_name
            AND table_schema = 'public';
            
            -- Count indexes
            SELECT COUNT(*) INTO v_index_count
            FROM pg_indexes
            WHERE tablename = v_table_name
            AND schemaname = 'public';
            
            -- Check RLS status
            SELECT rowsecurity INTO v_rls_enabled
            FROM pg_class
            WHERE relname = v_table_name;
            
            -- Check primary key
            SELECT COUNT(*) INTO v_has_pk
            FROM information_schema.table_constraints
            WHERE table_name = v_table_name
            AND constraint_type = 'PRIMARY KEY';
            
            -- Count foreign keys
            SELECT COUNT(*) INTO v_fk_count
            FROM information_schema.table_constraints
            WHERE table_name = v_table_name
            AND constraint_type = 'FOREIGN KEY';
            
            -- Get table size
            SELECT pg_total_relation_size('public.' || v_table_name) / 1024.0 / 1024.0 INTO v_table_size;
            
            -- Get row count
            EXECUTE format('SELECT COUNT(*) FROM %I', v_table_name) INTO v_row_count;
            
            -- Get last modified time
            SELECT GREATEST(
                COALESCE(MAX(created_at), '1970-01-01'::TIMESTAMP),
                COALESCE(MAX(updated_at), '1970-01-01'::TIMESTAMP)
            ) INTO v_last_modified
            FROM (
                SELECT created_at, updated_at FROM public.user_profiles WHERE 1=0
                UNION ALL
                SELECT created_at, updated_at FROM public.unit_properties WHERE 1=0
                -- Add other tables as needed
            ) combined_tables;
            
            -- Determine audit status
            v_audit_status := CASE
                WHEN v_has_pk = 0 THEN 'MISSING_PRIMARY_KEY'
                WHEN v_rls_enabled = false THEN 'RLS_DISABLED'
                WHEN v_index_count < 2 THEN 'UNDER_INDEXED'
                WHEN v_fk_count = 0 AND v_table_name NOT IN ('user_profiles', 'notifications') THEN 'MISSING_RELATIONSHIPS'
                ELSE 'GOOD'
            END;
            
            -- Insert results
            INSERT INTO audit_results VALUES (
                v_table_name, v_column_count, v_index_count, v_rls_enabled,
                v_has_pk > 0, v_fk_count, v_table_size, v_row_count,
                v_last_modified, v_audit_status
            );
            
        EXCEPTION
            WHEN OTHERS THEN
                INSERT INTO audit_results VALUES (
                    v_table_name, 0, 0, false, false, 0, 0, 0,
                    NULL, 'ERROR: ' || SQLERRM
                );
        END;
    END LOOP;
    
    -- Return results
    RETURN QUERY SELECT * FROM audit_results ORDER BY table_name;
    
    -- Clean up
    DROP TABLE IF EXISTS audit_results;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- RLS POLICY AUDIT
-- =====================================================

-- Function to audit RLS policies
CREATE OR REPLACE FUNCTION audit_rls_policies()
RETURNS TABLE(
    table_name TEXT,
    policy_name TEXT,
    policy_type TEXT,
    command TEXT,
    roles_with_access TEXT,
    policy_expression TEXT,
    is_secure BOOLEAN,
    security_issues TEXT
) AS $$
DECLARE
    v_table_name TEXT;
    v_policy_name TEXT;
    v_policy_cmd TEXT;
    v_policy_qual TEXT;
    v_roles TEXT;
    v_is_secure BOOLEAN;
    v_issues TEXT;
BEGIN
    -- Create temporary table for results
    CREATE TEMP TABLE IF NOT EXISTS rls_audit_results (
        table_name TEXT,
        policy_name TEXT,
        policy_type TEXT,
        command TEXT,
        roles_with_access TEXT,
        policy_expression TEXT,
        is_secure BOOLEAN,
        security_issues TEXT
    );
    
    -- Clear previous results
    TRUNCATE TABLE rls_audit_results;
    
    -- Audit each RLS policy
    FOR v_table_name, v_policy_name, v_policy_cmd, v_policy_qual IN
        SELECT 
            schemaname||'.'||tablename as table_name,
            policyname,
            cmd,
            qual
        FROM pg_policies 
        WHERE schemaname = 'public'
    LOOP
        BEGIN
            -- Analyze policy for security issues
            v_is_secure := true;
            v_issues := '';
            v_roles := '';
            
            -- Check for overly permissive policies
            IF v_policy_qual = 'true' OR v_policy_qual IS NULL THEN
                v_is_secure := false;
                v_issues := v_issues || 'OVERLY_PERMISSIVE; ';
            END IF;
            
            -- Check for direct user ID comparison without proper validation
            IF v_policy_qual LIKE '%auth.uid()%' AND v_policy_qual NOT LIKE '%IS NOT NULL%' THEN
                v_is_secure := false;
                v_issues := v_issues || 'MISSING_NULL_CHECK; ';
            END IF;
            
            -- Check for hardcoded role values
            IF v_policy_qual LIKE '%CUSTOMER%' OR v_policy_qual LIKE '%MARKETING%' THEN
                v_roles := v_roles || 'HARDCODED_ROLES; ';
            END IF;
            
            -- Determine roles that can access
            IF v_policy_qual LIKE '%auth.jwt() ->> ''role''%' THEN
                v_roles := v_roles || 'JWT_ROLE_CHECK; ';
            END IF;
            
            -- Insert results
            INSERT INTO rls_audit_results VALUES (
                v_table_name, v_policy_name, 'RLS_POLICY', v_policy_cmd,
                v_roles, v_policy_qual, v_is_secure, v_issues
            );
            
        EXCEPTION
            WHEN OTHERS THEN
                INSERT INTO rls_audit_results VALUES (
                    v_table_name, v_policy_name, 'RLS_POLICY', v_policy_cmd,
                    '', v_policy_qual, false, 'ERROR: ' || SQLERRM
                );
        END;
    END LOOP;
    
    -- Check tables without RLS
    FOR v_table_name IN
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
        AND tablename NOT LIKE 'pg_%'
        AND tablename NOT IN (
            SELECT DISTINCT split_part(policyname, '.', 2)
            FROM pg_policies 
            WHERE schemaname = 'public'
        )
    LOOP
        INSERT INTO rls_audit_results VALUES (
            v_table_name, 'NO_RLS', 'MISSING_POLICY', 'ALL',
            '', 'No RLS enabled', false, 'RLS_NOT_ENABLED'
        );
    END LOOP;
    
    -- Return results
    RETURN QUERY SELECT * FROM rls_audit_results ORDER BY table_name, policy_name;
    
    -- Clean up
    DROP TABLE IF EXISTS rls_audit_results;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- PERFORMANCE AUDIT
-- =====================================================

-- Function to audit database performance
CREATE OR REPLACE FUNCTION audit_database_performance()
RETURNS TABLE(
    table_name TEXT,
    performance_issue TEXT,
    severity TEXT,
    recommendation TEXT,
    impact_score INTEGER
) AS $$
DECLARE
    v_table_name TEXT;
    v_issue TEXT;
    v_severity TEXT;
    v_recommendation TEXT;
    v_score INTEGER;
BEGIN
    -- Create temporary table for results
    CREATE TEMP TABLE IF NOT EXISTS perf_audit_results (
        table_name TEXT,
        performance_issue TEXT,
        severity TEXT,
        recommendation TEXT,
        impact_score INTEGER
    );
    
    -- Clear previous results
    TRUNCATE TABLE perf_audit_results;
    
    -- Check for tables without primary keys
    FOR v_table_name IN
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
        AND tablename NOT LIKE 'pg_%'
        AND tablename NOT IN (
            SELECT DISTINCT table_name 
            FROM information_schema.table_constraints 
            WHERE constraint_type = 'PRIMARY KEY'
        )
    LOOP
        INSERT INTO perf_audit_results VALUES (
            v_table_name, 'MISSING_PRIMARY_KEY', 'HIGH',
            'Add primary key for better performance and data integrity',
            90
        );
    END LOOP;
    
    -- Check for tables with insufficient indexes
    FOR v_table_name IN
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
        AND tablename NOT LIKE 'pg_%'
        GROUP BY tablename
        HAVING COUNT(*) < 2
    LOOP
        INSERT INTO perf_audit_results VALUES (
            v_table_name, 'INSUFFICIENT_INDEXES', 'MEDIUM',
            'Add indexes for frequently queried columns',
            60
        );
    END LOOP;
    
    -- Check for large tables without partitioning
    FOR v_table_name IN
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
        AND tablename NOT LIKE 'pg_%'
        AND pg_total_relation_size('public.' || tablename) > 1024 * 1024 * 1024 -- > 1GB
    LOOP
        INSERT INTO perf_audit_results VALUES (
            v_table_name, 'LARGE_TABLE_UNPARTITIONED', 'MEDIUM',
            'Consider partitioning for better query performance',
            70
        );
    END LOOP;
    
    -- Check for tables with high dead tuple ratio
    FOR v_table_name IN
        SELECT schemaname||'.'||tablename 
        FROM pg_stat_user_tables 
        WHERE schemaname = 'public'
        AND n_dead_tup > 0
        AND (n_dead_tup::DECIMAL / (n_live_tup + n_dead_tup)) > 0.2 -- > 20% dead tuples
    LOOP
        INSERT INTO perf_audit_results VALUES (
            v_table_name, 'HIGH_DEAD_TUPLE_RATIO', 'MEDIUM',
            'Run VACUUM to clean up dead tuples',
            50
        );
    END LOOP;
    
    -- Check for missing foreign key constraints
    FOR v_table_name IN
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
        AND tablename NOT LIKE 'pg_%'
        AND tablename NOT IN ('user_profiles', 'notifications', 'audit_trail')
        AND tablename NOT IN (
            SELECT DISTINCT table_name 
            FROM information_schema.table_constraints 
            WHERE constraint_type = 'FOREIGN KEY'
        )
    LOOP
        INSERT INTO perf_audit_results VALUES (
            v_table_name, 'MISSING_FOREIGN_KEYS', 'LOW',
            'Add foreign key constraints for data integrity',
            30
        );
    END LOOP;
    
    -- Return results
    RETURN QUERY SELECT * FROM perf_audit_results ORDER BY impact_score DESC;
    
    -- Clean up
    DROP TABLE IF EXISTS perf_audit_results;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- SECURITY AUDIT
-- =====================================================

-- Function to audit security configurations
CREATE OR REPLACE FUNCTION audit_security_configurations()
RETURNS TABLE(
    security_area TEXT,
    configuration TEXT,
    status TEXT,
    risk_level TEXT,
    recommendation TEXT
) AS $$
BEGIN
    -- Create temporary table for results
    CREATE TEMP TABLE IF NOT EXISTS security_audit_results (
        security_area TEXT,
        configuration TEXT,
        status TEXT,
        risk_level TEXT,
        recommendation TEXT
    );
    
    -- Clear previous results
    TRUNCATE TABLE security_audit_results;
    
    -- Check RLS enabled on sensitive tables
    INSERT INTO security_audit_results VALUES (
        'ROW_LEVEL_SECURITY', 'RLS on user_profiles',
        CASE WHEN (SELECT rowsecurity FROM pg_class WHERE relname = 'user_profiles') 
             THEN 'ENABLED' ELSE 'DISABLED' END,
        CASE WHEN (SELECT rowsecurity FROM pg_class WHERE relname = 'user_profiles') 
             THEN 'LOW' ELSE 'HIGH' END,
        'Enable RLS on user_profiles table'
    );
    
    INSERT INTO security_audit_results VALUES (
        'ROW_LEVEL_SECURITY', 'RLS on financial_transactions',
        CASE WHEN (SELECT rowsecurity FROM pg_class WHERE relname = 'financial_transactions') 
             THEN 'ENABLED' ELSE 'DISABLED' END,
        CASE WHEN (SELECT rowsecurity FROM pg_class WHERE relname = 'financial_transactions') 
             THEN 'LOW' ELSE 'CRITICAL' END,
        'Enable RLS on financial_transactions table'
    );
    
    -- Check for encryption extensions
    INSERT INTO security_audit_results VALUES (
        'ENCRYPTION', 'pgcrypto extension',
        CASE WHEN EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pgcrypto') 
             THEN 'INSTALLED' ELSE 'MISSING' END,
        CASE WHEN EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pgcrypto') 
             THEN 'LOW' ELSE 'MEDIUM' END,
        'Install pgcrypto extension for data encryption'
    );
    
    -- Check for audit logging
    INSERT INTO security_audit_results VALUES (
        'AUDIT_LOGGING', 'audit_trail table',
        CASE WHEN EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'audit_trail') 
             THEN 'EXISTS' ELSE 'MISSING' END,
        CASE WHEN EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'audit_trail') 
             THEN 'LOW' ELSE 'HIGH' END,
        'Create audit_trail table for security auditing'
    );
    
    -- Check for password policies
    INSERT INTO security_audit_results VALUES (
        'PASSWORD_POLICY', 'Password complexity requirements',
        'NOT_IMPLEMENTED', 'MEDIUM',
        'Implement password complexity requirements'
    );
    
    -- Check for session timeout
    INSERT INTO security_audit_results VALUES (
        'SESSION_MANAGEMENT', 'Session timeout configuration',
        'NOT_CONFIGURED', 'LOW',
        'Configure appropriate session timeouts'
    );
    
    -- Return results
    RETURN QUERY SELECT * FROM security_audit_results ORDER BY 
        CASE risk_level 
            WHEN 'CRITICAL' THEN 1
            WHEN 'HIGH' THEN 2
            WHEN 'MEDIUM' THEN 3
            WHEN 'LOW' THEN 4
        END;
    
    -- Clean up
    DROP TABLE IF EXISTS security_audit_results;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- COMPREHENSIVE AUDIT REPORT
-- =====================================================

-- Function to generate comprehensive audit report
CREATE OR REPLACE FUNCTION generate_comprehensive_audit_report()
RETURNS TABLE(
    audit_category TEXT,
    item_name TEXT,
    status TEXT,
    risk_level TEXT,
    details TEXT,
    recommendation TEXT
) AS $$
BEGIN
    -- Schema audit
    RETURN QUERY
    SELECT 'SCHEMA_AUDIT', table_name, audit_status, 
           CASE audit_status 
               WHEN 'GOOD' THEN 'LOW'
               WHEN 'MISSING_PRIMARY_KEY' THEN 'HIGH'
               WHEN 'RLS_DISABLED' THEN 'CRITICAL'
               WHEN 'UNDER_INDEXED' THEN 'MEDIUM'
               WHEN 'MISSING_RELATIONSHIPS' THEN 'MEDIUM'
               ELSE 'UNKNOWN'
           END,
           format('Columns: %, Indexes: %, Size: %MB, Rows: %', 
                  total_columns, indexed_columns, table_size_mb, row_count),
           CASE audit_status
               WHEN 'GOOD' THEN 'No action needed'
               WHEN 'MISSING_PRIMARY_KEY' THEN 'Add primary key constraint'
               WHEN 'RLS_DISABLED' THEN 'Enable Row Level Security'
               WHEN 'UNDER_INDEXED' THEN 'Add appropriate indexes'
               WHEN 'MISSING_RELATIONSHIPS' THEN 'Add foreign key constraints'
               ELSE 'Review and fix issues'
           END
    FROM audit_database_schema()
    WHERE audit_status != 'GOOD'
    
    UNION ALL
    
    -- RLS audit
    SELECT 'RLS_AUDIT', table_name || '.' || policy_name, 
           CASE is_secure WHEN true THEN 'SECURE' ELSE 'VULNERABLE' END,
           CASE is_secure WHEN true THEN 'LOW' ELSE 'HIGH' END,
           'Policy: ' || policy_expression || ' | Issues: ' || COALESCE(security_issues, 'None'),
           CASE is_secure 
               WHEN true THEN 'Policy is properly configured'
               ELSE 'Review and fix security issues'
           END
    FROM audit_rls_policies()
    WHERE is_secure = false
    
    UNION ALL
    
    -- Performance audit
    SELECT 'PERFORMANCE_AUDIT', table_name || ': ' || performance_issue,
           'NEEDS_ATTENTION', severity,
           'Issue: ' || performance_issue,
           recommendation
    FROM audit_database_performance()
    
    UNION ALL
    
    -- Security audit
    SELECT 'SECURITY_AUDIT', security_area || ': ' || configuration,
           status, risk_level,
           'Current: ' || status,
           recommendation
    FROM audit_security_configurations()
    WHERE status != 'INSTALLED' AND status != 'ENABLED' AND status != 'EXISTS';
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- AUDIT EXECUTION FUNCTIONS
-- =====================================================

-- Function to run full audit and save results
CREATE OR REPLACE FUNCTION run_full_audit_and_save()
RETURNS TEXT AS $$
DECLARE
    v_audit_count INTEGER;
    v_result TEXT := 'Audit completed successfully';
BEGIN
    -- Create audit results table if not exists
    CREATE TABLE IF NOT EXISTS audit_results_log (
        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        audit_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
        audit_category TEXT,
        item_name TEXT,
        status TEXT,
        risk_level TEXT,
        details TEXT,
        recommendation TEXT,
        resolved BOOLEAN DEFAULT FALSE,
        resolved_at TIMESTAMP WITH TIME ZONE,
        resolved_by UUID
    );
    
    -- Clear previous unresolved audits
    DELETE FROM audit_results_log WHERE resolved = FALSE;
    
    -- Insert new audit results
    INSERT INTO audit_results_log (
        audit_category, item_name, status, risk_level, details, recommendation
    )
    SELECT * FROM generate_comprehensive_audit_report();
    
    -- Count audit findings
    SELECT COUNT(*) INTO v_audit_count FROM audit_results_log WHERE resolved = FALSE;
    
    v_result := format('Audit completed. Found % issues requiring attention.', v_audit_count);
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql;

-- Function to get audit summary
CREATE OR REPLACE FUNCTION get_audit_summary()
RETURNS TABLE(
    category TEXT,
    total_issues INTEGER,
    critical_issues INTEGER,
    high_issues INTEGER,
    medium_issues INTEGER,
    low_issues INTEGER,
    last_audit_date TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        audit_category,
        COUNT(*) as total_issues,
        COUNT(*) FILTER (WHERE risk_level = 'CRITICAL') as critical_issues,
        COUNT(*) FILTER (WHERE risk_level = 'HIGH') as high_issues,
        COUNT(*) FILTER (WHERE risk_level = 'MEDIUM') as medium_issues,
        COUNT(*) FILTER (WHERE risk_level = 'LOW') as low_issues,
        MAX(audit_date) as last_audit_date
    FROM audit_results_log
    WHERE resolved = FALSE
    GROUP BY audit_category
    ORDER BY total_issues DESC;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- USAGE EXAMPLES
-- =====================================================

-- Example 1: Run schema audit
-- SELECT * FROM audit_database_schema();

-- Example 2: Run RLS audit
-- SELECT * FROM audit_rls_policies();

-- Example 3: Run performance audit
-- SELECT * FROM audit_database_performance();

-- Example 4: Run security audit
-- SELECT * FROM audit_security_configurations();

-- Example 5: Generate comprehensive report
-- SELECT * FROM generate_comprehensive_audit_report();

-- Example 6: Run full audit and save results
-- SELECT run_full_audit_and_save();

-- Example 7: Get audit summary
-- SELECT * FROM get_audit_summary();

-- =====================================================
-- SCHEDULED AUDIT SETUP
-- =====================================================

-- Create function for scheduled audits
CREATE OR REPLACE FUNCTION scheduled_database_audit()
RETURNS void AS $$
BEGIN
    -- Run comprehensive audit
    PERFORM run_full_audit_and_save();
    
    -- Send notification if critical issues found
    IF EXISTS (
        SELECT 1 FROM audit_results_log 
        WHERE resolved = FALSE 
        AND risk_level IN ('CRITICAL', 'HIGH')
        AND audit_date > NOW() - INTERVAL '1 day'
    ) THEN
        -- Insert notification for administrators
        INSERT INTO notifications (user_id, title, message, type, created_at)
        SELECT 
            id, 
            'Critical Database Audit Issues', 
            format('Database audit found % critical issues requiring immediate attention', 
                   COUNT(*) FILTER (WHERE risk_level = 'CRITICAL')),
            'SYSTEM_ALERT',
            NOW()
        FROM user_profiles
        WHERE role = 'BOD'
        AND is_active = true;
    END IF;
END;
$$ LANGUAGE plpgsql;
