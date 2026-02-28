-- Database Performance Optimization for KPRFlow Enterprise
-- Indexes, Query Optimization, and Performance Monitoring

-- =====================================================
-- COMPREHENSIVE INDEXING STRATEGY
-- =====================================================

-- User Profiles Indexes
CREATE INDEX IF NOT EXISTS idx_user_profiles_email ON user_profiles(email);
CREATE INDEX IF NOT EXISTS idx_user_profiles_nik ON user_profiles(nik);
CREATE INDEX IF NOT EXISTS idx_user_profiles_phone ON user_profiles(phone_number);
CREATE INDEX IF NOT EXISTS idx_user_profiles_role ON user_profiles(role);
CREATE INDEX IF NOT EXISTS idx_user_profiles_active ON user_profiles(is_active);
CREATE INDEX IF NOT EXISTS idx_user_profiles_created_at ON user_profiles(created_at);
CREATE INDEX IF NOT EXISTS idx_user_profiles_updated_at ON user_profiles(updated_at);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_user_profiles_role_active ON user_profiles(role, is_active);
CREATE INDEX IF NOT EXISTS idx_user_profiles_created_role ON user_profiles(created_at DESC, role);

-- Unit Properties Indexes
CREATE INDEX IF NOT EXISTS idx_unit_properties_block ON unit_properties(block);
CREATE INDEX IF NOT EXISTS idx_unit_properties_unit_number ON unit_properties(unit_number);
CREATE INDEX IF NOT EXISTS idx_unit_properties_type ON unit_properties(type);
CREATE INDEX IF NOT EXISTS idx_unit_properties_status ON unit_properties(status);
CREATE INDEX IF NOT EXISTS idx_unit_properties_price ON unit_properties(price);
CREATE INDEX IF NOT EXISTS idx_unit_properties_created_at ON unit_properties(created_at);
CREATE INDEX IF NOT EXISTS idx_unit_properties_updated_at ON unit_properties(updated_at);

-- Composite indexes for unit searches
CREATE INDEX IF NOT EXISTS idx_unit_properties_status_type ON unit_properties(status, type);
CREATE INDEX IF NOT EXISTS idx_unit_properties_block_unit ON unit_properties(block, unit_number);
CREATE INDEX IF NOT EXISTS idx_unit_properties_price_status ON unit_properties(price, status);

-- KPR Dossiers Indexes
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_user_id ON kpr_dossiers(user_id);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_unit_id ON kpr_dossiers(unit_id);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_status ON kpr_dossiers(status);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_created_at ON kpr_dossiers(created_at);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_updated_at ON kpr_dossiers(updated_at);

-- Composite indexes for dossier queries
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_status_user ON kpr_dossiers(status, user_id);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_status_created ON kpr_dossiers(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_user_unit ON kpr_dossiers(user_id, unit_id);

-- Documents Indexes
CREATE INDEX IF NOT EXISTS idx_documents_user_id ON documents(user_id);
CREATE INDEX IF NOT EXISTS idx_documents_dossier_id ON documents(dossier_id);
CREATE INDEX IF NOT EXISTS idx_documents_type ON documents(document_type);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents(created_at);
CREATE INDEX IF NOT EXISTS idx_documents_updated_at ON documents(updated_at);

-- Composite indexes for document queries
CREATE INDEX IF NOT EXISTS idx_documents_user_type ON documents(user_id, document_type);
CREATE INDEX IF NOT EXISTS idx_documents_dossier_type ON documents(dossier_id, document_type);
CREATE INDEX IF NOT EXISTS idx_documents_status_type ON documents(status, document_type);

-- Financial Transactions Indexes
CREATE INDEX IF NOT EXISTS idx_financial_transactions_user_id ON financial_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_dossier_id ON financial_transactions(dossier_id);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_type ON financial_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_status ON financial_transactions(status);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_amount ON financial_transactions(amount);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_created_at ON financial_transactions(created_at);

-- Composite indexes for financial queries
CREATE INDEX IF NOT EXISTS idx_financial_transactions_user_type ON financial_transactions(user_id, transaction_type);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_status_created ON financial_transactions(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_amount_type ON financial_transactions(amount, transaction_type);

-- Quorum Voting Indexes
CREATE INDEX IF NOT EXISTS idx_quorum_voting_sessions_dossier_id ON quorum_voting_sessions(dossier_id);
CREATE INDEX IF NOT EXISTS idx_quorum_voting_sessions_status ON quorum_voting_sessions(status);
CREATE INDEX IF NOT EXISTS idx_quorum_voting_sessions_type ON quorum_voting_sessions(voting_type);
CREATE INDEX IF NOT EXISTS idx_quorum_voting_sessions_created_at ON quorum_voting_sessions(created_at);
CREATE INDEX IF NOT EXISTS idx_quorum_voting_sessions_expires_at ON quorum_voting_sessions(expires_at);

-- Composite indexes for voting queries
CREATE INDEX IF NOT EXISTS idx_quorum_voting_sessions_status_type ON quorum_voting_sessions(status, voting_type);
CREATE INDEX IF NOT EXISTS idx_quorum_voting_sessions_dossier_status ON quorum_voting_sessions(dossier_id, status);

-- Quorum Votes Indexes
CREATE INDEX IF NOT EXISTS idx_quorum_votes_session_id ON quorum_votes(session_id);
CREATE INDEX IF NOT EXISTS idx_quorum_votes_voter_id ON quorum_votes(voter_id);
CREATE INDEX IF NOT EXISTS idx_quorum_votes_vote ON quorum_votes(vote);
CREATE INDEX IF NOT EXISTS idx_quorum_votes_created_at ON quorum_votes(created_at);

-- Composite indexes for vote queries
CREATE INDEX IF NOT EXISTS idx_quorum_votes_session_vote ON quorum_votes(session_id, vote);
CREATE INDEX IF NOT EXISTS idx_quorum_votes_voter_session ON quorum_votes(voter_id, session_id);

-- Notifications Indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_read_at ON notifications(read_at);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

-- Composite indexes for notification queries
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, read_at);
CREATE INDEX IF NOT EXISTS idx_notifications_user_type ON notifications(user_id, type);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(user_id) WHERE read_at IS NULL;

-- Audit Trail Indexes
CREATE INDEX IF NOT EXISTS idx_audit_trail_table_name ON audit_trail(table_name);
CREATE INDEX IF NOT EXISTS idx_audit_trail_operation ON audit_trail(operation);
CREATE INDEX IF NOT EXISTS idx_audit_trail_performed_by ON audit_trail(performed_by);
CREATE INDEX IF NOT EXISTS idx_audit_trail_created_at ON audit_trail(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_trail_transaction_id ON audit_trail(transaction_id);

-- Composite indexes for audit queries
CREATE INDEX IF NOT EXISTS idx_audit_trail_table_operation ON audit_trail(table_name, operation);
CREATE INDEX IF NOT EXISTS idx_audit_trail_performed_date ON audit_trail(performed_by, created_at DESC);

-- Payment Schedules Indexes
CREATE INDEX IF NOT EXISTS idx_payment_schedules_user_id ON payment_schedules(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_schedules_dossier_id ON payment_schedules(dossier_id);
CREATE INDEX IF NOT EXISTS idx_payment_schedules_status ON payment_schedules(status);
CREATE INDEX IF NOT EXISTS idx_payment_schedules_due_date ON payment_schedules(due_date);
CREATE INDEX IF NOT EXISTS idx_payment_schedules_created_at ON payment_schedules(created_at);

-- Composite indexes for payment queries
CREATE INDEX IF NOT EXISTS idx_payment_schedules_user_status ON payment_schedules(user_id, status);
CREATE INDEX IF NOT EXISTS idx_payment_schedules_due_status ON payment_schedules(due_date, status);
CREATE INDEX IF NOT EXISTS idx_payment_schedules_overdue ON payment_schedules(status, due_date) WHERE status = 'PENDING' AND due_date < NOW();

-- =====================================================
-- PARTITIONING FOR LARGE TABLES
-- =====================================================

-- Partition audit_trail by month for better performance
CREATE TABLE IF NOT EXISTS audit_trail_partitioned (
    LIKE audit_trail INCLUDING ALL
) PARTITION BY RANGE (created_at);

-- Create monthly partitions for audit trail
CREATE OR REPLACE FUNCTION create_monthly_audit_partition()
RETURNS void AS $$
DECLARE
    v_start_date DATE;
    v_end_date DATE;
    v_partition_name TEXT;
BEGIN
    -- Create partition for current month
    v_start_date := date_trunc('month', CURRENT_DATE);
    v_end_date := v_start_date + interval '1 month';
    v_partition_name := 'audit_trail_' || to_char(v_start_date, 'YYYY_MM');
    
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_trail_partitioned
                    FOR VALUES FROM (%L) TO (%L)',
                   v_partition_name, v_start_date, v_end_date);
    
    -- Create partition for next month
    v_start_date := v_start_date + interval '1 month';
    v_end_date := v_start_date + interval '1 month';
    v_partition_name := 'audit_trail_' || to_char(v_start_date, 'YYYY_MM');
    
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_trail_partitioned
                    FOR VALUES FROM (%L) TO (%L)',
                   v_partition_name, v_start_date, v_end_date);
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- QUERY OPTIMIZATION FUNCTIONS
-- =====================================================

-- Function to get customer dossier with all related data efficiently
CREATE OR REPLACE FUNCTION get_customer_dossier_details(p_user_id UUID)
RETURNS TABLE(
    dossier_id UUID,
    status TEXT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    unit_id UUID,
    block TEXT,
    unit_number TEXT,
    unit_type TEXT,
    unit_price DECIMAL,
    document_count INTEGER,
    pending_documents INTEGER,
    total_financial_amount DECIMAL,
    pending_payments INTEGER
) AS $$
BEGIN
    RETURN QUERY
    WITH dossier_data AS (
        SELECT 
            d.id,
            d.status,
            d.created_at,
            d.updated_at,
            d.unit_id
        FROM kpr_dossiers d
        WHERE d.user_id = p_user_id
        AND d.status NOT IN ('CANCELLED_BY_SYSTEM', 'FLOATING_DOSSIER')
        ORDER BY d.created_at DESC
        LIMIT 1
    ),
    unit_data AS (
        SELECT 
            dd.id,
            u.block,
            u.unit_number,
            u.type,
            u.price
        FROM dossier_data dd
        LEFT JOIN unit_properties u ON u.id = dd.unit_id
    ),
    document_data AS (
        SELECT 
            dd.id,
            COUNT(*) as document_count,
            COUNT(*) FILTER (WHERE status = 'PENDING') as pending_documents
        FROM dossier_data dd
        LEFT JOIN documents doc ON doc.dossier_id = dd.id
        GROUP BY dd.id
    ),
    financial_data AS (
        SELECT 
            dd.id,
            COALESCE(SUM(ft.amount), 0) as total_financial_amount,
            COUNT(*) FILTER (WHERE ft.status = 'PENDING') as pending_payments
        FROM dossier_data dd
        LEFT JOIN financial_transactions ft ON ft.dossier_id = dd.id
        GROUP BY dd.id
    )
    SELECT 
        dd.id,
        dd.status,
        dd.created_at,
        dd.updated_at,
        dd.unit_id,
        ud.block,
        ud.unit_number,
        ud.type,
        ud.price,
        COALESCE(docdoc.document_count, 0),
        COALESCE(docdoc.pending_documents, 0),
        COALESCE(find.total_financial_amount, 0),
        COALESCE(find.pending_payments, 0)
    FROM dossier_data dd
    LEFT JOIN unit_data ud ON ud.id = dd.id
    LEFT JOIN document_data docdoc ON docdoc.id = dd.id
    LEFT JOIN financial_data find ON find.id = dd.id;
END;
$$ LANGUAGE plpgsql;

-- Function to get marketing dashboard data efficiently
CREATE OR REPLACE FUNCTION get_marketing_dashboard_data(p_date_from DATE, p_date_to DATE)
RETURNS TABLE(
    total_leads INTEGER,
    new_applications INTEGER,
    conversion_rate DECIMAL,
    active_campaigns INTEGER,
    total_revenue DECIMAL,
    avg_processing_time INTEGER
) AS $$
BEGIN
    RETURN QUERY
    WITH lead_data AS (
        SELECT 
            COUNT(*) as total_leads,
            COUNT(*) FILTER (WHERE created_at >= p_date_from AND created_at <= p_date_to) as new_applications
        FROM kpr_dossiers
        WHERE status NOT IN ('CANCELLED_BY_SYSTEM', 'FLOATING_DOSSIER')
    ),
    conversion_data AS (
        SELECT 
            COUNT(*) as converted_count
        FROM kpr_dossiers
        WHERE status IN ('SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED')
        AND created_at >= p_date_from AND created_at <= p_date_to
    ),
    campaign_data AS (
        SELECT COUNT(*) as active_campaigns
        FROM marketing_campaigns
        WHERE status = 'ACTIVE'
        AND (start_date <= p_date_to AND end_date >= p_date_from)
    ),
    revenue_data AS (
        SELECT COALESCE(SUM(amount), 0) as total_revenue
        FROM financial_transactions
        WHERE transaction_type = 'DISBURSEMENT'
        AND created_at >= p_date_from AND created_at <= p_date_to
    ),
    processing_data AS (
        SELECT AVG(EXTRACT(EPOCH FROM (updated_at - created_at))/86400)::INTEGER as avg_days
        FROM kpr_dossiers
        WHERE status IN ('SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED')
        AND created_at >= p_date_from AND created_at <= p_date_to
    )
    SELECT 
        ld.total_leads,
        ld.new_applications,
        CASE 
            WHEN ld.new_applications > 0 
            THEN ROUND((cd.converted_count::DECIMAL / ld.new_applications) * 100, 2)
            ELSE 0
        END as conversion_rate,
        COALESCE(cad.active_campaigns, 0),
        COALESCE(rd.total_revenue, 0),
        COALESCE(pd.avg_days, 0)
    FROM lead_data ld
    LEFT JOIN conversion_data cd ON true
    LEFT JOIN campaign_data cad ON true
    LEFT JOIN revenue_data rd ON true
    LEFT JOIN processing_data pd ON true;
END;
$$ LANGUAGE plpgsql;

-- Function to get legal dashboard data efficiently
CREATE OR REPLACE FUNCTION get_legal_dashboard_data()
RETURNS TABLE(
    pending_reviews INTEGER,
    approved_today INTEGER,
    rejected_today INTEGER,
    avg_review_time INTEGER,
    critical_documents INTEGER,
    overdue_reviews INTEGER
) AS $$
BEGIN
    RETURN QUERY
    WITH pending_data AS (
        SELECT COUNT(*) as pending_count
        FROM documents
        WHERE status = 'PENDING'
    ),
    today_data AS (
        SELECT 
            COUNT(*) FILTER (WHERE status = 'APPROVED') as approved_today,
            COUNT(*) FILTER (WHERE status = 'REJECTED') as rejected_today
        FROM documents
        WHERE DATE(updated_at) = CURRENT_DATE
    ),
    review_time_data AS (
        SELECT AVG(EXTRACT(EPOCH FROM (updated_at - created_at))/3600)::INTEGER as avg_hours
        FROM documents
        WHERE status IN ('APPROVED', 'REJECTED')
        AND updated_at >= CURRENT_DATE - INTERVAL '7 days'
    ),
    critical_data AS (
        SELECT COUNT(*) as critical_count
        FROM documents
        WHERE status = 'PENDING'
        AND document_type IN ('KTP', 'KK', 'NPWP')
        AND created_at < CURRENT_DATE - INTERVAL '3 days'
    ),
    overdue_data AS (
        SELECT COUNT(*) as overdue_count
        FROM documents
        WHERE status = 'PENDING'
        AND created_at < CURRENT_DATE - INTERVAL '7 days'
    )
    SELECT 
        pd.pending_count,
        td.approved_today,
        td.rejected_today,
        COALESCE(rtd.avg_hours, 0),
        cd.critical_count,
        od.overdue_count
    FROM pending_data pd
    LEFT JOIN today_data td ON true
    LEFT JOIN review_time_data rtd ON true
    LEFT JOIN critical_data cd ON true
    LEFT JOIN overdue_data od ON true;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- PERFORMANCE MONITORING FUNCTIONS
-- =====================================================

-- Function to monitor slow queries
CREATE OR REPLACE FUNCTION monitor_slow_queries()
RETURNS TABLE(
    query_text TEXT,
    execution_time DECIMAL,
    calls INTEGER,
    total_time DECIMAL
) AS $$
BEGIN
    -- This would typically query pg_stat_statements
    -- Implementation depends on your monitoring setup
    RETURN QUERY SELECT 
        'Sample query'::TEXT, 
        100.5::DECIMAL, 
        10::INTEGER, 
        1005.0::DECIMAL;
END;
$$ LANGUAGE plpgsql;

-- Function to check index usage
CREATE OR REPLACE FUNCTION check_index_usage()
RETURNS TABLE(
    table_name TEXT,
    index_name TEXT,
    usage_count INTEGER,
    last_used TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    -- This would typically query pg_stat_user_indexes
    -- Implementation depends on your monitoring setup
    RETURN QUERY SELECT 
        'user_profiles'::TEXT, 
        'idx_user_profiles_email'::TEXT, 
        100::INTEGER, 
        NOW()::TIMESTAMP WITH TIME ZONE;
END;
$$ LANGUAGE plpgsql;

-- Function to analyze table statistics
CREATE OR REPLACE FUNCTION analyze_table_statistics()
RETURNS TABLE(
    table_name TEXT,
    row_count BIGINT,
    size_mb DECIMAL,
    last_analyzed TIMESTAMP WITH TIME ZONE,
    needs_vacuum BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        schemaname||'.'||tablename as table_name,
        n_tup_ins - n_tup_del as row_count,
        pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))::DECIMAL as size_mb,
        last_analyze,
        (n_tup_ins - n_tup_del) > 10000 AND (last_vacuum < NOW() - INTERVAL '1 day' OR last_vacuum IS NULL) as needs_vacuum
    FROM pg_stat_user_tables
    WHERE schemaname = 'public'
    ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- AUTOMATIC MAINTENANCE FUNCTIONS
-- =====================================================

-- Function to perform automatic vacuum and analyze
CREATE OR REPLACE FUNCTION auto_maintenance()
RETURNS TEXT AS $$
DECLARE
    v_result TEXT := 'Maintenance completed';
    v_table_name TEXT;
BEGIN
    -- Loop through all tables and perform maintenance
    FOR v_table_name IN 
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
        AND tablename NOT LIKE 'pg_%'
    LOOP
        BEGIN
            EXECUTE 'VACUUM ANALYZE ' || quote_ident(v_table_name);
            v_result := v_result || ', ' || v_table_name;
        EXCEPTION
            WHEN OTHERS THEN
                v_result := v_result || ', ERROR on ' || v_table_name || ': ' || SQLERRM;
        END;
    END LOOP;
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql;

-- Function to update table statistics
CREATE OR REPLACE FUNCTION update_statistics()
RETURNS TEXT AS $$
DECLARE
    v_result TEXT := 'Statistics updated';
    v_table_name TEXT;
BEGIN
    -- Update statistics for all tables
    FOR v_table_name IN 
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
        AND tablename NOT LIKE 'pg_%'
    LOOP
        BEGIN
            EXECUTE 'ANALYZE ' || quote_ident(v_table_name);
            v_result := v_result || ', ' || v_table_name;
        EXCEPTION
            WHEN OTHERS THEN
                v_result := v_result || ', ERROR on ' || v_table_name || ': ' || SQLERRM;
        END;
    END LOOP;
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- SCHEDULED MAINTENANCE SETUP
-- =====================================================

-- Create a function to be called periodically for maintenance
CREATE OR REPLACE FUNCTION scheduled_maintenance()
RETURNS void AS $$
BEGIN
    -- Update statistics
    PERFORM update_statistics();
    
    -- Create new audit partitions if needed
    PERFORM create_monthly_audit_partition();
    
    -- Clean up old notifications (older than 90 days)
    DELETE FROM notifications 
    WHERE created_at < NOW() - INTERVAL '90 days'
    AND read_at IS NOT NULL;
    
    -- Clean up old audit trail entries (older than 1 year)
    DELETE FROM audit_trail 
    WHERE created_at < NOW() - INTERVAL '1 year';
    
    -- Update materialized views if any
    -- REFRESH MATERIALIZED VIEW CONCURRENTLY some_view;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- PERFORMANCE CONFIGURATION
-- =====================================================

-- Set performance parameters (these would typically be set at database level)
-- ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';
-- ALTER SYSTEM SET track_activity_query_size = 2048;
-- ALTER SYSTEM SET log_min_duration_statement = 1000;
-- ALTER SYSTEM SET log_checkpoints = on;
-- ALTER SYSTEM SET log_connections = on;
-- ALTER SYSTEM SET log_disconnections = on;
-- ALTER SYSTEM SET log_lock_waits = on;

-- Reload configuration
-- SELECT pg_reload_conf();

-- =====================================================
-- MONITORING VIEWS
-- =====================================================

-- View for monitoring table sizes
CREATE OR REPLACE VIEW table_sizes AS
SELECT 
    schemaname||'.'||tablename as table_name,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as index_size,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    n_live_tup as live_tuples,
    n_dead_tup as dead_tuples,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- View for monitoring index usage
CREATE OR REPLACE VIEW index_usage AS
SELECT 
    schemaname||'.'||tablename||'.'||indexname as index_name,
    idx_tup_read as index_reads,
    idx_tup_fetch as index_fetches,
    idx_scan as index_scans,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- View for monitoring slow queries
CREATE OR REPLACE VIEW slow_queries AS
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements
WHERE mean_time > 100 -- queries taking more than 100ms
ORDER BY mean_time DESC
LIMIT 20;
