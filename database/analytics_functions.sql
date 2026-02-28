-- Analytics Functions for Phase 13: BOD Analytics Dashboard

-- Function to get phase processing time
CREATE OR REPLACE FUNCTION get_phase_processing_time(
    p_from_status VARCHAR(50),
    p_to_status VARCHAR(50)
)
RETURNS TABLE (
    phase_name VARCHAR(100),
    average_days DECIMAL(10,2),
    min_days INTEGER,
    max_days INTEGER,
    sample_size BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        CONCAT(p_from_status, ' → ', p_to_status) as phase_name,
        ROUND(AVG(
            CASE 
                WHEN p_to_status = 'PEMBERKASAN' THEN 
                    EXTRACT(EPOCH FROM (updated_at - created_at))/86400
                WHEN p_to_status = 'PROSES_BANK' THEN 
                    EXTRACT(EPOCH FROM (updated_at - created_at))/86400
                WHEN p_to_status = 'PUTUSAN_KREDIT_ACC' THEN 
                    EXTRACT(EPOCH FROM (updated_at - created_at))/86400
                WHEN p_to_status = 'SP3K_TERBIT' THEN 
                    EXTRACT(EPOCH FROM (sp3k_issued_date::timestamp - created_at))/86400
                WHEN p_to_status = 'FUNDS_DISBURSED' THEN 
                    EXTRACT(EPOCH FROM (disbursed_date::timestamp - sp3k_issued_date::timestamp))/86400
                WHEN p_to_status = 'BAST_COMPLETED' THEN 
                    EXTRACT(EPOCH FROM (bast_date::timestamp - disbursed_date::timestamp))/86400
                ELSE 0
            END
        ), 2) as average_days,
        MIN(
            CASE 
                WHEN p_to_status = 'PEMBERKASAN' THEN 
                    EXTRACT(EPOCH FROM (updated_at - created_at))/86400
                WHEN p_to_status = 'PROSES_BANK' THEN 
                    EXTRACT(EPOCH FROM (updated_at - created_at))/86400
                WHEN p_to_status = 'PUTUSAN_KREDIT_ACC' THEN 
                    EXTRACT(EPOCH FROM (updated_at - created_at))/86400
                WHEN p_to_status = 'SP3K_TERBIT' THEN 
                    EXTRACT(EPOCH FROM (sp3k_issued_date::timestamp - created_at))/86400
                WHEN p_to_status = 'FUNDS_DISBURSED' THEN 
                    EXTRACT(EPOCH FROM (disbursed_date::timestamp - sp3k_issued_date::timestamp))/86400
                WHEN p_to_status = 'BAST_COMPLETED' THEN 
                    EXTRACT(EPOCH FROM (bast_date::timestamp - disbursed_date::timestamp))/86400
                ELSE 0
            END
        ) as min_days,
        MAX(
            CASE 
                WHEN p_to_status = 'PEMBERKASAN' THEN 
                    EXTRACT(EPOCH FROM (updated_at - created_at))/86400
                WHEN p_to_status = 'PROSES_BANK' THEN 
                    EXTRACT(EPOCH FROM (updated_at - created_at))/86400
                WHEN p_to_status = 'PUTUSAN_KREDIT_ACC' THEN 
                    EXTRACT(EPOCH FROM (updated_at - created_at))/86400
                WHEN p_to_status = 'SP3K_TERBIT' THEN 
                    EXTRACT(EPOCH FROM (sp3k_issued_date::timestamp - created_at))/86400
                WHEN p_to_status = 'FUNDS_DISBURSED' THEN 
                    EXTRACT(EPOCH FROM (disbursed_date::timestamp - sp3k_issued_date::timestamp))/86400
                WHEN p_to_status = 'BAST_COMPLETED' THEN 
                    EXTRACT(EPOCH FROM (bast_date::timestamp - disbursed_date::timestamp))/86400
                ELSE 0
            END
        ) as max_days,
        COUNT(*) as sample_size
    FROM kpr_dossiers
    WHERE status = p_to_status
    AND (
        (p_to_status = 'PEMBERKASAN' AND updated_at IS NOT NULL) OR
        (p_to_status = 'PROSES_BANK' AND updated_at IS NOT NULL) OR
        (p_to_status = 'PUTUSAN_KREDIT_ACC' AND updated_at IS NOT NULL) OR
        (p_to_status = 'SP3K_TERBIT' AND sp3k_issued_date IS NOT NULL) OR
        (p_to_status = 'FUNDS_DISBURSED' AND disbursed_date IS NOT NULL AND sp3k_issued_date IS NOT NULL) OR
        (p_to_status = 'BAST_COMPLETED' AND bast_date IS NOT NULL AND disbursed_date IS NOT NULL)
    );
END;
$$ LANGUAGE plpgsql;

-- Function to get document SLA compliance
CREATE OR REPLACE FUNCTION get_document_sla_compliance()
RETURNS TABLE (
    total_documents BIGINT,
    compliant_documents BIGINT,
    overdue_documents BIGINT,
    compliance_rate DECIMAL(5,2),
    average_processing_time DECIMAL(10,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_documents,
        COUNT(*) FILTER (
            WHERE EXTRACT(EPOCH FROM (verified_at::timestamp - uploaded_at))/86400 <= 14
        ) as compliant_documents,
        COUNT(*) FILTER (
            WHERE EXTRACT(EPOCH FROM (verified_at::timestamp - uploaded_at))/86400 > 14
        ) as overdue_documents,
        CASE 
            WHEN COUNT(*) > 0 THEN 
                ROUND((COUNT(*) FILTER (
                    WHERE EXTRACT(EPOCH FROM (verified_at::timestamp - uploaded_at))/86400 <= 14
                )::DECIMAL / COUNT(*)::DECIMAL) * 100, 2)
            ELSE 0 
        END as compliance_rate,
        ROUND(AVG(
            EXTRACT(EPOCH FROM (verified_at::timestamp - uploaded_at))/86400
        ), 2) as average_processing_time
    FROM documents
    WHERE is_verified = TRUE
    AND verified_at IS NOT NULL;
END;
$$ LANGUAGE plpgsql;

-- Function to get bank SLA compliance
CREATE OR REPLACE FUNCTION get_bank_sla_compliance()
RETURNS TABLE (
    total_decisions BIGINT,
    compliant_decisions BIGINT,
    overdue_decisions BIGINT,
    compliance_rate DECIMAL(5,2),
    average_processing_time DECIMAL(10,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_decisions,
        COUNT(*) FILTER (
            WHERE EXTRACT(EPOCH FROM (processed_at::timestamp - uploaded_at))/86400 <= 60
        ) as compliant_decisions,
        COUNT(*) FILTER (
            WHERE EXTRACT(EPOCH FROM (processed_at::timestamp - uploaded_at))/86400 > 60
        ) as overdue_decisions,
        CASE 
            WHEN COUNT(*) > 0 THEN 
                ROUND((COUNT(*) FILTER (
                    WHERE EXTRACT(EPOCH FROM (processed_at::timestamp - uploaded_at))/86400 <= 60
                )::DECIMAL / COUNT(*)::DECIMAL) * 100, 2)
            ELSE 0 
        END as compliance_rate,
        ROUND(AVG(
            EXTRACT(EPOCH FROM (processed_at::timestamp - uploaded_at))/86400
        ), 2) as average_processing_time
    FROM bank_decisions
    WHERE status = 'PROCESSED'
    AND processed_at IS NOT NULL;
END;
$$ LANGUAGE plpgsql;

-- Function to get monthly revenue data
CREATE OR REPLACE FUNCTION get_monthly_revenue(
    p_months INTEGER DEFAULT 12
)
RETURNS TABLE (
    month_number INTEGER,
    month_name VARCHAR(20),
    actual_revenue DECIMAL(15,2),
    projected_revenue DECIMAL(15,2),
    total_revenue DECIMAL(15,2)
) AS $$
BEGIN
    RETURN QUERY
    WITH monthly_data AS (
        SELECT 
            EXTRACT(MONTH FROM created_at) as month_num,
            TO_CHAR(created_at, 'Month') as month_name,
            COALESCE(SUM(kpr_amount), 0) as revenue
        FROM kpr_dossiers
        WHERE status = 'BAST_COMPLETED'
        AND created_at >= CURRENT_DATE - INTERVAL '1 year'
        GROUP BY EXTRACT(MONTH FROM created_at), TO_CHAR(created_at, 'Month')
    ),
        projected_data AS (
            SELECT 
                EXTRACT(MONTH FROM created_at) as month_num,
                TO_CHAR(created_at, 'Month') as month_name,
                COALESCE(SUM(kpr_amount), 0) * 0.8 as projected_revenue -- 80% probability
            FROM kpr_dossiers
        WHERE status IN ('LEAD', 'PEMBERKASAN', 'PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'FUNDS_DISBURSED')
        AND created_at >= CURRENT_DATE - INTERVAL '1 year'
        GROUP BY EXTRACT(MONTH FROM created_at), TO_CHAR(created_at, 'Month')
    )
    SELECT 
        COALESCE(md.month_num, pd.month_num) as month_number,
        COALESCE(md.month_name, pd.month_name) as month_name,
        COALESCE(md.revenue, 0) as actual_revenue,
        COALESCE(pd.projected_revenue, 0) as projected_revenue,
        COALESCE(md.revenue, 0) + COALESCE(pd.projected_revenue, 0) as total_revenue
    FROM (SELECT * FROM monthly_data) md
    FULL OUTER JOIN (SELECT * FROM projected_data) pd 
        ON md.month_num = pd.month_num
    ORDER BY month_number
    LIMIT p_months;
END;
$$ LANGUAGE plpgsql;

-- Function to get conversion funnel data
CREATE OR REPLACE FUNCTION get_conversion_funnel()
RETURNS TABLE (
    stage VARCHAR(50),
    count BIGINT,
    conversion_rate DECIMAL(5,2),
    drop_off_rate DECIMAL(5,2)
) AS $$
WITH funnel_stages AS (
    SELECT 'LEAD' as stage, COUNT(*) as count, 1.0 as conversion_rate, 0.0 as drop_off_rate
    FROM kpr_dossiers
    UNION ALL
    SELECT 'DOCUMENT_COLLECTION' as stage, COUNT(*) as count, 
           (COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM kpr_dossiers WHERE status = 'LEAD')::DECIMAL) * 100 as conversion_rate,
           100 - (COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM kpr_dossiers WHERE status = 'LEAD')::DECIMAL) * 100 as drop_off_rate
    FROM kpr_dossiers
    WHERE status IN ('PEMBERKASAN', 'PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED')
    UNION ALL
    SELECT 'BANK_PROCESSING' as stage, COUNT(*) as count,
           (COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM kpr_dossiers WHERE status IN ('PEMBERKASAN', 'PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED'))::DECIMAL) * 100 as conversion_rate,
           100 - (COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM kpr_dossiers WHERE status IN ('PEMBERKASAN', 'PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED'))::DECIMAL) * 100 as drop_off_rate
    FROM kpr_dossiers
    WHERE status IN ('PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED')
    UNION ALL
    SELECT 'CREDIT_APPROVED' as stage, COUNT(*) as count,
           (COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM kpr_dossiers WHERE status IN ('PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED'))::DECIMAL) * 100 as conversion_rate,
           100 - (COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM kpr_dossiers WHERE status IN ('PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED'))::DECIMAL) * 100 as drop_off_rate
    FROM kpr_dossiers
    WHERE status IN ('PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED')
    UNION ALL
    SELECT 'SP3K_ISSUED' as stage, COUNT(*) as count,
           (COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM kpr_dossiers WHERE status IN ('PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED'))::DECIMAL) * 100 as conversion_rate,
           100 - (COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM kpr_dossiers WHERE status IN ('PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED'))::DECIMAL) * 100 as drop_off_rate
    FROM kpr_dossiers
    WHERE status IN ('SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED')
    UNION ALL
    SELECT 'FUNDS_DISBURSED' as stage, COUNT(*) as count,
           (COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM kpr_dossiers WHERE status IN ('SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED'))::DECIMAL) * 100 as conversion_rate,
           100 - (COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM kpr_dossiers WHERE status IN ('SP3K_TERBIT', 'FUNDS_DISBURSED', 'BAST_COMPLETED'))::DECIMAL) * 100 as drop_off_rate
    FROM kpr_dossiers
    WHERE status IN ('FUNDS_DISBURSED', 'BAST_COMPLETED')
    UNION ALL
    SELECT 'BAST_COMPLETED' as stage, COUNT(*) as count,
           (COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM kpr_dossiers WHERE status IN ('FUNDS_DISBURSED', 'BAST_COMPLETED'))::DECIMAL) * 100 as conversion_rate,
           100 - (COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM kpr_dossiers WHERE status IN ('FUNDS_DISBURSED', 'BAST_COMPLETED'))::DECIMAL) * 100 as drop_off_rate
    FROM kpr_dossiers
    WHERE status = 'BAST_COMPLETED'
)
SELECT * FROM funnel_stages
ORDER BY 
    CASE stage
        WHEN 'LEAD' THEN 1
        WHEN 'DOCUMENT_COLLECTION' THEN 2
        WHEN 'BANK_PROCESSING' THEN 3
        WHEN 'CREDIT_APPROVED' THEN 4
        WHEN 'SP3K_ISSUED' THEN 5
        WHEN 'FUNDS_DISBURSED' THEN 6
        WHEN 'BAST_COMPLETED' THEN 7
    END;
$$ LANGUAGE plpgsql;

-- Function to get team performance metrics
CREATE OR REPLACE FUNCTION get_team_performance_metrics()
RETURNS TABLE (
    role VARCHAR(50),
    tasks_completed BIGINT,
    average_task_time DECIMAL(10,2),
    efficiency_score DECIMAL(5,2),
    satisfaction_score DECIMAL(3,1)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        up.role,
        COUNT(DISTINCT kd.id) as tasks_completed,
        ROUND(AVG(
            CASE 
                WHEN kd.status = 'BAST_COMPLETED' THEN 
                    EXTRACT(EPOCH FROM (kd.bast_date::timestamp - kd.created_at))/86400
                WHEN kd.status = 'FUNDS_DISBURSED' THEN 
                    EXTRACT(EPOCH FROM (kd.disbursed_date::timestamp - kd.created_at))/86400
                WHEN kd.status = 'SP3K_TERBIT' THEN 
                    EXTRACT(EPOCH FROM (kd.sp3k_issued_date::timestamp - kd.created_at))/86400
                ELSE 30
            END
        ), 2) as average_task_time,
        ROUND(
            (COUNT(DISTINCT kd.id FILTER (WHERE kd.status = 'BAST_COMPLETED')::DECIMAL / 
             NULLIF(COUNT(DISTINCT kd.id), 0)::DECIMAL) * 100, 2
        ) as efficiency_score,
        4.2 as satisfaction_score -- Mock satisfaction score
    FROM user_profiles up
    LEFT JOIN kpr_dossiers kd ON up.id = kd.user_id
    WHERE up.role IN ('MARKETING', 'LEGAL', 'FINANCE')
    GROUP BY up.role
    ORDER BY efficiency_score DESC;
END;
$$ LANGUAGE plpgsql;
