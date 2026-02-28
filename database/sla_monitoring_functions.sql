-- SLA Monitoring Functions for Phase 15: Real-time SLA Tracking

-- Function to get comprehensive SLA metrics
CREATE OR REPLACE FUNCTION get_sla_comprehensive_metrics(
    p_date_range_start DATE DEFAULT NULL,
    p_date_range_end DATE DEFAULT NULL
)
RETURNS TABLE (
    document_sla_compliance DECIMAL(5,2),
    document_sla_total BIGINT,
    document_sla_compliant BIGINT,
    document_sla_overdue BIGINT,
    document_avg_processing_days DECIMAL(10,2),
    
    bank_sla_compliance DECIMAL(5,2),
    bank_sla_total BIGINT,
    bank_sla_compliant BIGINT,
    bank_sla_overdue BIGINT,
    bank_avg_processing_days DECIMAL(10,2),
    
    sp3k_sla_compliance DECIMAL(5,2),
    sp3k_sla_total BIGINT,
    sp3k_sla_compliant BIGINT,
    sp3k_sla_overdue BIGINT,
    sp3k_avg_processing_days DECIMAL(10,2),
    
    disbursement_sla_compliance DECIMAL(5,2),
    disbursement_sla_total BIGINT,
    disbursement_sla_compliant BIGINT,
    disbursement_sla_overdue BIGINT,
    disbursement_avg_processing_days DECIMAL(10,2),
    
    bast_sla_compliance DECIMAL(5,2),
    bast_sla_total BIGINT,
    bast_sla_compliant BIGINT,
    bast_sla_overdue BIGINT,
    bast_avg_processing_days DECIMAL(10,2),
    
    overall_compliance_rate DECIMAL(5,2),
    total_items BIGINT,
    total_compliant BIGINT,
    total_overdue BIGINT
) AS $$
BEGIN
    RETURN QUERY
    WITH document_metrics AS (
        SELECT 
            COUNT(*) as total,
            COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (verified_at::timestamp - uploaded_at))/86400 <= 14) as compliant,
            COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (verified_at::timestamp - uploaded_at))/86400 > 14) as overdue,
            AVG(EXTRACT(EPOCH FROM (COALESCE(verified_at::timestamp, NOW()) - uploaded_at))/86400) as avg_days
        FROM documents
        WHERE 
            (p_date_range_start IS NULL OR DATE(uploaded_at) >= p_date_range_start)
            AND (p_date_range_end IS NULL OR DATE(uploaded_at) <= p_date_range_end)
    ),
    bank_metrics AS (
        SELECT 
            COUNT(*) as total,
            COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (COALESCE(processed_at::timestamp, NOW()) - uploaded_at))/86400 <= 60) as compliant,
            COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (COALESCE(processed_at::timestamp, NOW()) - uploaded_at))/86400 > 60) as overdue,
            AVG(EXTRACT(EPOCH FROM (COALESCE(processed_at::timestamp, NOW()) - uploaded_at))/86400) as avg_days
        FROM bank_decisions
        WHERE 
            (p_date_range_start IS NULL OR DATE(uploaded_at) >= p_date_range_start)
            AND (p_date_range_end IS NULL OR DATE(uploaded_at) <= p_date_range_end)
    ),
    sp3k_metrics AS (
        SELECT 
            COUNT(*) as total,
            COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (COALESCE(sp3k_issued_date::timestamp, NOW()) - updated_at))/86400 <= 7) as compliant,
            COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (COALESCE(sp3k_issued_date::timestamp, NOW()) - updated_at))/86400 > 7) as overdue,
            AVG(EXTRACT(EPOCH FROM (COALESCE(sp3k_issued_date::timestamp, NOW()) - updated_at))/86400) as avg_days
        FROM kpr_dossiers
        WHERE status = 'PUTUSAN_KREDIT_ACC'
        AND 
            (p_date_range_start IS NULL OR DATE(updated_at) >= p_date_range_start)
            AND (p_date_range_end IS NULL OR DATE(updated_at) <= p_date_range_end)
    ),
    disbursement_metrics AS (
        SELECT 
            COUNT(*) as total,
            COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (COALESCE(disbursed_date::timestamp, NOW()) - sp3k_issued_date::timestamp))/86400 <= 10) as compliant,
            COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (COALESCE(disbursed_date::timestamp, NOW()) - sp3k_issued_date::timestamp))/86400 > 10) as overdue,
            AVG(EXTRACT(EPOCH FROM (COALESCE(disbursed_date::timestamp, NOW()) - sp3k_issued_date::timestamp))/86400) as avg_days
        FROM kpr_dossiers
        WHERE status = 'SP3K_TERBIT'
        AND 
            (p_date_range_start IS NULL OR DATE(sp3k_issued_date) >= p_date_range_start)
            AND (p_date_range_end IS NULL OR DATE(sp3k_issued_date) <= p_date_range_end)
    ),
    bast_metrics AS (
        SELECT 
            COUNT(*) as total,
            COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (COALESCE(bast_date::timestamp, NOW()) - disbursed_date::timestamp))/86400 <= 30) as compliant,
            COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (COALESCE(bast_date::timestamp, NOW()) - disbursed_date::timestamp))/86400 > 30) as overdue,
            AVG(EXTRACT(EPOCH FROM (COALESCE(bast_date::timestamp, NOW()) - disbursed_date::timestamp))/86400) as avg_days
        FROM kpr_dossiers
        WHERE status = 'FUNDS_DISBURSED'
        AND 
            (p_date_range_start IS NULL OR DATE(disbursed_date) >= p_date_range_start)
            AND (p_date_range_end IS NULL OR DATE(disbursed_date) <= p_date_range_end)
    )
    SELECT 
        -- Document SLA
        CASE WHEN dm.total > 0 THEN ROUND((dm.compliant::DECIMAL / dm.total::DECIMAL) * 100, 2) ELSE 0 END as document_sla_compliance,
        dm.total as document_sla_total,
        dm.compliant as document_sla_compliant,
        dm.overdue as document_sla_overdue,
        COALESCE(dm.avg_days, 0) as document_avg_processing_days,
        
        -- Bank SLA
        CASE WHEN bm.total > 0 THEN ROUND((bm.compliant::DECIMAL / bm.total::DECIMAL) * 100, 2) ELSE 0 END as bank_sla_compliance,
        bm.total as bank_sla_total,
        bm.compliant as bank_sla_compliant,
        bm.overdue as bank_sla_overdue,
        COALESCE(bm.avg_days, 0) as bank_avg_processing_days,
        
        -- SP3K SLA
        CASE WHEN sm.total > 0 THEN ROUND((sm.compliant::DECIMAL / sm.total::DECIMAL) * 100, 2) ELSE 0 END as sp3k_sla_compliance,
        sm.total as sp3k_sla_total,
        sm.compliant as sp3k_sla_compliant,
        sm.overdue as sp3k_sla_overdue,
        COALESCE(sm.avg_days, 0) as sp3k_avg_processing_days,
        
        -- Disbursement SLA
        CASE WHEN dm2.total > 0 THEN ROUND((dm2.compliant::DECIMAL / dm2.total::DECIMAL) * 100, 2) ELSE 0 END as disbursement_sla_compliance,
        dm2.total as disbursement_sla_total,
        dm2.compliant as disbursement_sla_compliant,
        dm2.overdue as disbursement_sla_overdue,
        COALESCE(dm2.avg_days, 0) as disbursement_avg_processing_days,
        
        -- BAST SLA
        CASE WHEN bm2.total > 0 THEN ROUND((bm2.compliant::DECIMAL / bm2.total::DECIMAL) * 100, 2) ELSE 0 END as bast_sla_compliance,
        bm2.total as bast_sla_total,
        bm2.compliant as bast_sla_compliant,
        bm2.overdue as bast_sla_overdue,
        COALESCE(bm2.avg_days, 0) as bast_avg_processing_days,
        
        -- Overall metrics
        CASE 
            WHEN (dm.total + bm.total + sm.total + dm2.total + bm2.total) > 0 THEN 
                ROUND(((dm.compliant + bm.compliant + sm.compliant + dm2.compliant + bm2.compliant)::DECIMAL / 
                      (dm.total + bm.total + sm.total + dm2.total + bm2.total)::DECIMAL) * 100, 2)
            ELSE 0 
        END as overall_compliance_rate,
        (dm.total + bm.total + sm.total + dm2.total + bm2.total) as total_items,
        (dm.compliant + bm.compliant + sm.compliant + dm2.compliant + bm2.compliant) as total_compliant,
        (dm.overdue + bm.overdue + sm.overdue + dm2.overdue + bm2.overdue) as total_overdue
    FROM document_metrics dm, bank_metrics bm, sp3k_metrics sm, disbursement_metrics dm2, bast_metrics bm2;
END;
$$ LANGUAGE plpgsql;

-- Function to get overdue items with priority
CREATE OR REPLACE FUNCTION get_overdue_items()
RETURNS TABLE (
    item_id VARCHAR(255),
    item_type VARCHAR(50),
    item_name TEXT,
    overdue_days INTEGER,
    sla_days INTEGER,
    actual_days INTEGER,
    responsible_team VARCHAR(50),
    priority VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE,
    due_date TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    -- Overdue documents
    SELECT 
        d.dossier_id as item_id,
        'Document Processing' as item_type,
        'Document Verification' as item_name,
        GREATEST(0, EXTRACT(DAYS FROM (NOW() - (d.uploaded_at + INTERVAL '14 days'))))::INTEGER as overdue_days,
        14 as sla_days,
        EXTRACT(DAYS FROM (NOW() - d.uploaded_at))::INTEGER as actual_days,
        'Legal' as responsible_team,
        CASE 
            WHEN EXTRACT(DAYS FROM (NOW() - (d.uploaded_at + INTERVAL '14 days'))) > 14 THEN 'HIGH'
            ELSE 'MEDIUM'
        END as priority,
        d.uploaded_at as created_at,
        d.uploaded_at + INTERVAL '14 days' as due_date
    FROM documents d
    WHERE d.is_verified = FALSE
    AND d.uploaded_at < NOW() - INTERVAL '14 days'
    
    UNION ALL
    
    -- Overdue bank decisions
    SELECT 
        bd.dossier_id as item_id,
        'Bank Decision' as item_type,
        'Bank: ' || bd.bank_name as item_name,
        GREATEST(0, EXTRACT(DAYS FROM (NOW() - (bd.uploaded_at + INTERVAL '60 days'))))::INTEGER as overdue_days,
        60 as sla_days,
        EXTRACT(DAYS FROM (NOW() - bd.uploaded_at))::INTEGER as actual_days,
        'Marketing' as responsible_team,
        CASE 
            WHEN EXTRACT(DAYS FROM (NOW() - (bd.uploaded_at + INTERVAL '60 days'))) > 30 THEN 'HIGH'
            ELSE 'MEDIUM'
        END as priority,
        bd.uploaded_at as created_at,
        bd.uploaded_at + INTERVAL '60 days' as due_date
    FROM bank_decisions bd
    WHERE bd.status != 'PROCESSED'
    AND bd.uploaded_at < NOW() - INTERVAL '60 days'
    
    UNION ALL
    
    -- Overdue SP3K issuance
    SELECT 
        kd.id as item_id,
        'SP3K Issuance' as item_type,
        'SP3K Document' as item_name,
        GREATEST(0, EXTRACT(DAYS FROM (NOW() - (kd.updated_at + INTERVAL '7 days'))))::INTEGER as overdue_days,
        7 as sla_days,
        EXTRACT(DAYS FROM (NOW() - kd.updated_at))::INTEGER as actual_days,
        'Legal' as responsible_team,
        CASE 
            WHEN EXTRACT(DAYS FROM (NOW() - (kd.updated_at + INTERVAL '7 days'))) > 7 THEN 'HIGH'
            ELSE 'MEDIUM'
        END as priority,
        kd.updated_at as created_at,
        kd.updated_at + INTERVAL '7 days' as due_date
    FROM kpr_dossiers kd
    WHERE kd.status = 'PUTUSAN_KREDIT_ACC'
    AND kd.sp3k_issued_date IS NULL
    AND kd.updated_at < NOW() - INTERVAL '7 days'
    
    UNION ALL
    
    -- Overdue disbursement
    SELECT 
        kd.id as item_id,
        'Disbursement' as item_type,
        'Fund Disbursement' as item_name,
        GREATEST(0, EXTRACT(DAYS FROM (NOW() - (kd.sp3k_issued_date::timestamp + INTERVAL '10 days'))))::INTEGER as overdue_days,
        10 as sla_days,
        EXTRACT(DAYS FROM (NOW() - kd.sp3k_issued_date::timestamp))::INTEGER as actual_days,
        'Finance' as responsible_team,
        CASE 
            WHEN EXTRACT(DAYS FROM (NOW() - (kd.sp3k_issued_date::timestamp + INTERVAL '10 days'))) > 5 THEN 'HIGH'
            ELSE 'MEDIUM'
        END as priority,
        kd.sp3k_issued_date::timestamp as created_at,
        kd.sp3k_issued_date::timestamp + INTERVAL '10 days' as due_date
    FROM kpr_dossiers kd
    WHERE kd.status = 'SP3K_TERBIT'
    AND kd.disbursed_date IS NULL
    AND kd.sp3k_issued_date::timestamp < NOW() - INTERVAL '10 days'
    
    UNION ALL
    
    -- Overdue BAST completion
    SELECT 
        kd.id as item_id,
        'BAST Completion' as item_type,
        'Handover Process' as item_name,
        GREATEST(0, EXTRACT(DAYS FROM (NOW() - (kd.disbursed_date::timestamp + INTERVAL '30 days'))))::INTEGER as overdue_days,
        30 as sla_days,
        EXTRACT(DAYS FROM (NOW() - kd.disbursed_date::timestamp))::INTEGER as actual_days,
        'Estate' as responsible_team,
        CASE 
            WHEN EXTRACT(DAYS FROM (NOW() - (kd.disbursed_date::timestamp + INTERVAL '30 days'))) > 15 THEN 'HIGH'
            ELSE 'MEDIUM'
        END as priority,
        kd.disbursed_date::timestamp as created_at,
        kd.disbursed_date::timestamp + INTERVAL '30 days' as due_date
    FROM kpr_dossiers kd
    WHERE kd.status = 'FUNDS_DISBURSED'
    AND kd.bast_date IS NULL
    AND kd.disbursed_date::timestamp < NOW() - INTERVAL '30 days'
    
    ORDER BY 
        CASE priority 
            WHEN 'HIGH' THEN 1 
            WHEN 'MEDIUM' THEN 2 
            ELSE 3 
        END,
        overdue_days DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to generate SLA breach alerts
CREATE OR REPLACE FUNCTION generate_sla_alerts()
RETURNS TABLE (
    alert_id UUID,
    item_id VARCHAR(255),
    item_type VARCHAR(50),
    item_name TEXT,
    overdue_days INTEGER,
    sla_days INTEGER,
    responsible_team VARCHAR(50),
    priority VARCHAR(10),
    alert_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        gen_random_uuid() as alert_id,
        oi.item_id,
        oi.item_type,
        oi.item_name,
        oi.overdue_days,
        oi.sla_days,
        oi.responsible_team,
        oi.priority,
        CASE 
            WHEN oi.priority = 'HIGH' THEN 
                'URGENT: ' || oi.item_name || ' is ' || oi.overdue_days || ' days overdue (SLA: ' || oi.sla_days || ' days). Immediate action required!'
            ELSE 
                'WARNING: ' || oi.item_name || ' is ' || oi.overdue_days || ' days overdue (SLA: ' || oi.sla_days || ' days). Please address promptly.'
        END as alert_message,
        NOW() as created_at
    FROM get_overdue_items() oi
    WHERE oi.overdue_days > 0;
END;
$$ LANGUAGE plpgsql;

-- Function to get SLA trends over time
CREATE OR REPLACE FUNCTION get_sla_trends(
    p_months INTEGER DEFAULT 12
)
RETURNS TABLE (
    month_date DATE,
    month_name VARCHAR(20),
    document_compliance DECIMAL(5,2),
    bank_compliance DECIMAL(5,2),
    sp3k_compliance DECIMAL(5,2),
    disbursement_compliance DECIMAL(5,2),
    bast_compliance DECIMAL(5,2),
    overall_compliance DECIMAL(5,2)
) AS $$
BEGIN
    RETURN QUERY
    WITH monthly_data AS (
        SELECT 
            DATE_TRUNC('month', DATE(d.uploaded_at)) as month_date,
            COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (COALESCE(d.verified_at::timestamp, NOW()) - d.uploaded_at))/86400 <= 14) as doc_compliant,
            COUNT(*) as doc_total
        FROM documents d
        WHERE d.uploaded_at >= NOW() - INTERVAL '1 year'
        GROUP BY DATE_TRUNC('month', DATE(d.uploaded_at))
        
        UNION ALL
        
        SELECT 
            DATE_TRUNC('month', DATE(bd.uploaded_at)) as month_date,
            COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (COALESCE(bd.processed_at::timestamp, NOW()) - bd.uploaded_at))/86400 <= 60) as doc_compliant,
            COUNT(*) as doc_total
        FROM bank_decisions bd
        WHERE bd.uploaded_at >= NOW() - INTERVAL '1 year'
        GROUP BY DATE_TRUNC('month', DATE(bd.uploaded_at))
    )
    SELECT 
        TO_CHAR(month_date, 'YYYY-MM-DD')::DATE as month_date,
        TO_CHAR(month_date, 'Month YYYY') as month_name,
        0 as document_compliance, -- Placeholder - would need more complex query
        0 as bank_compliance,
        0 as sp3k_compliance,
        0 as disbursement_compliance,
        0 as bast_compliance,
        0 as overall_compliance
    FROM (SELECT DISTINCT month_date FROM monthly_data ORDER BY month_date DESC LIMIT p_months) md
    ORDER BY month_date;
END;
$$ LANGUAGE plpgsql;

-- Function to get team SLA performance
CREATE OR REPLACE FUNCTION get_team_sla_performance()
RETURNS TABLE (
    team VARCHAR(50),
    total_items BIGINT,
    compliant_items BIGINT,
    overdue_items BIGINT,
    compliance_rate DECIMAL(5,2),
    average_overdue_days DECIMAL(10,2),
    high_priority_overdue BIGINT
) AS $$
BEGIN
    RETURN QUERY
    -- Legal team performance (documents + SP3K)
    SELECT 
        'Legal' as team,
        (dm.total + sm.total) as total_items,
        (dm.compliant + sm.compliant) as compliant_items,
        (dm.overdue + sm.overdue) as overdue_items,
        CASE 
            WHEN (dm.total + sm.total) > 0 THEN 
                ROUND(((dm.compliant + sm.compliant)::DECIMAL / (dm.total + sm.total)::DECIMAL) * 100, 2)
            ELSE 0 
        END as compliance_rate,
        COALESCE(AVG(oi.overdue_days), 0) as average_overdue_days,
        COUNT(*) FILTER (WHERE oi.priority = 'HIGH') as high_priority_overdue
    FROM document_metrics dm, sp3k_metrics sm, get_overdue_items() oi
    WHERE oi.responsible_team = 'Legal'
    GROUP BY 'Legal'
    
    UNION ALL
    
    -- Marketing team performance (bank decisions)
    SELECT 
        'Marketing' as team,
        bm.total as total_items,
        bm.compliant as compliant_items,
        bm.overdue as overdue_items,
        CASE 
            WHEN bm.total > 0 THEN ROUND((bm.compliant::DECIMAL / bm.total::DECIMAL) * 100, 2)
            ELSE 0 
        END as compliance_rate,
        COALESCE(AVG(oi.overdue_days), 0) as average_overdue_days,
        COUNT(*) FILTER (WHERE oi.priority = 'HIGH') as high_priority_overdue
    FROM bank_metrics bm, get_overdue_items() oi
    WHERE oi.responsible_team = 'Marketing'
    GROUP BY 'Marketing'
    
    UNION ALL
    
    -- Finance team performance (disbursement)
    SELECT 
        'Finance' as team,
        dm2.total as total_items,
        dm2.compliant as compliant_items,
        dm2.overdue as overdue_items,
        CASE 
            WHEN dm2.total > 0 THEN ROUND((dm2.compliant::DECIMAL / dm2.total::DECIMAL) * 100, 2)
            ELSE 0 
        END as compliance_rate,
        COALESCE(AVG(oi.overdue_days), 0) as average_overdue_days,
        COUNT(*) FILTER (WHERE oi.priority = 'HIGH') as high_priority_overdue
    FROM disbursement_metrics dm2, get_overdue_items() oi
    WHERE oi.responsible_team = 'Finance'
    GROUP BY 'Finance'
    
    UNION ALL
    
    -- Estate team performance (BAST)
    SELECT 
        'Estate' as team,
        bm2.total as total_items,
        bm2.compliant as compliant_items,
        bm2.overdue as overdue_items,
        CASE 
            WHEN bm2.total > 0 THEN ROUND((bm2.compliant::DECIMAL / bm2.total::DECIMAL) * 100, 2)
            ELSE 0 
        END as compliance_rate,
        COALESCE(AVG(oi.overdue_days), 0) as average_overdue_days,
        COUNT(*) FILTER (WHERE oi.priority = 'HIGH') as high_priority_overdue
    FROM bast_metrics bm2, get_overdue_items() oi
    WHERE oi.responsible_team = 'Estate'
    GROUP BY 'Estate';
END;
$$ LANGUAGE plpgsql;
