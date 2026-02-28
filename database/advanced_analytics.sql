-- Advanced Analytics & Business Intelligence for Phase 15
-- KPRFlow Enterprise

-- Analytics Views for Executive Dashboard
CREATE OR REPLACE VIEW v_executive_summary AS
SELECT 
    -- KPI Metrics
    COUNT(DISTINCT kd.id) as total_applications,
    COUNT(DISTINCT CASE WHEN kd.status = 'FUNDS_DISBURSED' THEN kd.id END) as total_disbursed,
    COUNT(DISTINCT CASE WHEN kd.status IN ('LEAD', 'PEMBERKASAN') THEN kd.id END) as active_pipeline,
    COUNT(DISTINCT CASE WHEN kd.status = 'BAST_COMPLETED' THEN kd.id END) as total_completed,
    
    -- Financial Metrics
    COALESCE(SUM(CASE WHEN ft.is_realized = true THEN ft.amount ELSE 0 END), 0) as total_revenue,
    COALESCE(SUM(CASE WHEN ft.is_realized = true AND ft.category = 'BOOKING_FEE' THEN ft.amount ELSE 0 END), 0) as booking_fee_revenue,
    COALESCE(SUM(CASE WHEN ft.is_realized = true AND ft.category LIKE 'DP_%' THEN ft.amount ELSE 0 END), 0) as dp_revenue,
    
    -- Performance Metrics
    ROUND(AVG(
        CASE 
            WHEN kd.status = 'FUNDS_DISBURSED' THEN 
                EXTRACT(EPOCH FROM (kd.updated_at - kd.created_at))/86400
            ELSE NULL 
        END
    ), 1) as avg_processing_days,
    
    -- Conversion Rates
    ROUND(
        COUNT(DISTINCT CASE WHEN kd.status = 'FUNDS_DISBURSED' THEN kd.id END) * 100.0 / 
        NULLIF(COUNT(DISTINCT kd.id), 0), 2
    ) as conversion_rate,
    
    -- SLA Compliance
    COUNT(DISTINCT CASE 
        WHEN kd.created_at >= NOW() - INTERVAL '14 days' 
        AND kd.status NOT IN ('LEAD', 'PEMBERKASAN') 
        THEN kd.id 
    END) as sla_compliant_14d,
    
    COUNT(DISTINCT CASE 
        WHEN kd.created_at >= NOW() - INTERVAL '60 days' 
        AND kd.status IN ('FUNDS_DISBURSED', 'BAST_COMPLETED') 
        THEN kd.id 
    END) as sla_compliant_60d,
    
    -- Time Period
    DATE_TRUNC('month', CURRENT_DATE) as reporting_month
    
FROM kpr_dossiers kd
LEFT JOIN financial_transactions ft ON kd.id = ft.dossier_id
WHERE kd.created_at >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '12 months';

-- Operational Analytics View
CREATE OR REPLACE VIEW v_operational_master AS
SELECT 
    kd.id as dossier_id,
    kd.status as current_status,
    kd.created_at as application_date,
    kd.updated_at as last_updated,
    up.name as customer_name,
    up.phone_number as customer_phone,
    up_block.block || '/' || up_block.unit_number as unit_info,
    up_block.project_name,
    up_block.price as unit_price,
    
    -- Financial Summary
    COALESCE(ft_summary.total_paid, 0) as total_paid,
    COALESCE(ft_summary.total_pending, 0) as total_pending,
    ft_summary.payment_progress,
    
    -- Document Status
    doc_summary.total_documents,
    doc_summary.verified_documents,
    doc_summary.completion_percentage,
    
    -- SLA Tracking
    CASE 
        WHEN kd.status IN ('LEAD', 'PEMBERKASAN') THEN
            GREATEST(0, 14 - EXTRACT(DAY FROM (NOW() - kd.created_at)))
        WHEN kd.status IN ('PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT') THEN
            GREATEST(0, 60 - EXTRACT(DAY FROM (NOW() - kd.created_at)))
        ELSE NULL
    END as sla_days_remaining,
    
    -- Age Analysis
    EXTRACT(DAY FROM (NOW() - kd.created_at)) as application_age_days,
    
    -- Subsidy Status
    up.status_sikasep,
    up.sikasep_checked_at,
    
    -- Team Assignment
    kd.assigned_to,
    kd.marketing_id,
    kd.legal_id,
    kd.finance_id
    
FROM kpr_dossiers kd
LEFT JOIN user_profiles up ON kd.user_id = up.id
LEFT JOIN unit_properties up_block ON kd.unit_id = up_block.id
LEFT JOIN (
    SELECT 
        dossier_id,
        COALESCE(SUM(CASE WHEN is_realized = true THEN amount ELSE 0 END), 0) as total_paid,
        COALESCE(SUM(CASE WHEN is_realized = false THEN amount ELSE 0 END), 0) as total_pending,
        CASE 
            WHEN COALESCE(SUM(amount), 0) > 0 THEN
                ROUND(COALESCE(SUM(CASE WHEN is_realized = true THEN amount ELSE 0 END), 0) * 100.0 / SUM(amount), 2)
            ELSE 0
        END as payment_progress
    FROM financial_transactions
    GROUP BY dossier_id
) ft_summary ON kd.id = ft_summary.dossier_id
LEFT JOIN (
    SELECT 
        dossier_id,
        COUNT(*) as total_documents,
        COUNT(*) FILTER (WHERE status = 'VERIFIED') as verified_documents,
        ROUND(COUNT(*) FILTER (WHERE status = 'VERIFIED') * 100.0 / COUNT(*), 2) as completion_percentage
    FROM documents
    GROUP BY dossier_id
) doc_summary ON kd.id = doc_summary.dossier_id;

-- Performance Analytics View
CREATE OR REPLACE VIEW v_performance_analytics AS
SELECT 
    -- Time-based Performance
    DATE_TRUNC('month', kd.created_at) as month,
    COUNT(*) as new_applications,
    COUNT(*) FILTER (WHERE kd.status = 'FUNDS_DISBURSED') as completed_applications,
    ROUND(AVG(
        EXTRACT(EPOCH FROM (kd.updated_at - kd.created_at))/86400
    ), 1) as avg_processing_time,
    
    -- Revenue Performance
    COALESCE(SUM(ft.amount), 0) as monthly_revenue,
    COALESCE(AVG(ft.amount), 0) as avg_deal_size,
    
    -- Team Performance
    COUNT(*) FILTER (WHERE kd.marketing_id IS NOT NULL) as marketing_assigned,
    COUNT(*) FILTER (WHERE kd.legal_id IS NOT NULL) as legal_assigned,
    COUNT(*) FILTER (WHERE kd.finance_id IS NOT NULL) as finance_assigned,
    
    -- Conversion Funnel
    COUNT(*) FILTER (WHERE kd.status = 'LEAD') as leads,
    COUNT(*) FILTER (WHERE kd.status = 'PEMBERKASAN') as documentation,
    COUNT(*) FILTER (WHERE kd.status = 'PROSES_BANK') as bank_submission,
    COUNT(*) FILTER (WHERE kd.status = 'FUNDS_DISBURSED') as disbursed,
    
    -- Project Performance
    up_block.project_name,
    COUNT(*) as project_applications,
    COALESCE(SUM(up_block.price), 0) as project_value
    
FROM kpr_dossiers kd
LEFT JOIN financial_transactions ft ON kd.id = ft.dossier_id AND ft.is_realized = true
LEFT JOIN unit_properties up_block ON kd.unit_id = up_block.id
WHERE kd.created_at >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '24 months'
GROUP BY DATE_TRUNC('month', kd.created_at), up_block.project_name
ORDER BY month DESC, project_name;

-- Risk Analytics View
CREATE OR REPLACE VIEW v_risk_analytics AS
SELECT 
    -- Document Risk
    COUNT(*) as total_applications,
    COUNT(*) FILTER (WHERE doc_summary.completion_percentage < 50) as high_doc_risk,
    COUNT(*) FILTER (WHERE doc_summary.completion_percentage BETWEEN 50 AND 80) as medium_doc_risk,
    COUNT(*) FILTER (WHERE doc_summary.completion_percentage > 80) as low_doc_risk,
    
    -- SLA Risk
    COUNT(*) FILTER (
        WHEN kd.status IN ('LEAD', 'PEMBERKASAN') 
        AND EXTRACT(DAY FROM (NOW() - kd.created_at)) > 10
    ) as sla_14d_risk,
    
    COUNT(*) FILTER (
        WHEN kd.status IN ('PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT') 
        AND EXTRACT(DAY FROM (NOW() - kd.created_at)) > 45
    ) as sla_60d_risk,
    
    -- Financial Risk
    COUNT(*) FILTER (WHERE ft_summary.payment_progress < 30) as high_financial_risk,
    COUNT(*) FILTER (WHERE ft_summary.payment_progress BETWEEN 30 AND 70) as medium_financial_risk,
    COUNT(*) FILTER (WHERE ft_summary.payment_progress > 70) as low_financial_risk,
    
    -- Cancellation Risk
    COUNT(*) FILTER (WHERE kd.status IN ('LEAD', 'PEMBERKASAN') AND kd.created_at < NOW() - INTERVAL '30 days') as stale_applications,
    
    -- Risk Score Calculation
    ROUND(
        (
            (COUNT(*) FILTER (WHERE doc_summary.completion_percentage < 50) * 0.3) +
            (COUNT(*) FILTER (
                WHEN kd.status IN ('LEAD', 'PEMBERKASAN') 
                AND EXTRACT(DAY FROM (NOW() - kd.created_at)) > 10
            ) * 0.25) +
            (COUNT(*) FILTER (WHERE ft_summary.payment_progress < 30) * 0.25) +
            (COUNT(*) FILTER (WHERE kd.created_at < NOW() - INTERVAL '30 days') * 0.2)
        ) * 100.0 / NULLIF(COUNT(*), 0), 2
    ) as overall_risk_score
    
FROM kpr_dossiers kd
LEFT JOIN (
    SELECT 
        dossier_id,
        ROUND(COUNT(*) FILTER (WHERE status = 'VERIFIED') * 100.0 / COUNT(*), 2) as completion_percentage
    FROM documents
    GROUP BY dossier_id
) doc_summary ON kd.id = doc_summary.dossier_id
LEFT JOIN (
    SELECT 
        dossier_id,
        CASE 
            WHEN COALESCE(SUM(amount), 0) > 0 THEN
                COALESCE(SUM(CASE WHEN is_realized = true THEN amount ELSE 0 END), 0) * 100.0 / SUM(amount)
            ELSE 0
        END as payment_progress
    FROM financial_transactions
    GROUP BY dossier_id
) ft_summary ON kd.id = ft_summary.dossier_id;

-- Predictive Analytics Function
CREATE OR REPLACE FUNCTION predict_completion_probability(
    p_dossier_id UUID
) RETURNS TABLE (
    probability NUMERIC,
    factors JSONB,
    recommendation TEXT
) AS $$
DECLARE
    v_dossier RECORD;
    v_doc_completion NUMERIC;
    v_payment_progress NUMERIC;
    v_sla_compliance NUMERIC;
    v_probability NUMERIC;
    v_factors JSONB;
    v_recommendation TEXT;
BEGIN
    -- Get dossier data
    SELECT 
        kd.*,
        doc_summary.completion_percentage,
        ft_summary.payment_progress,
        EXTRACT(DAY FROM (NOW() - kd.created_at)) as age_days
    INTO v_dossier
    FROM kpr_dossiers kd
    LEFT JOIN (
        SELECT 
            dossier_id,
            COALESCE(SUM(CASE WHEN status = 'VERIFIED' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 0) as completion_percentage
        FROM documents
        GROUP BY dossier_id
    ) doc_summary ON kd.id = doc_summary.dossier_id
    LEFT JOIN (
        SELECT 
            dossier_id,
            COALESCE(SUM(CASE WHEN is_realized = true THEN amount ELSE 0 END) * 100.0 / SUM(amount), 0) as payment_progress
        FROM financial_transactions
        GROUP BY dossier_id
    ) ft_summary ON kd.id = ft_summary.dossier_id
    WHERE kd.id = p_dossier_id;
    
    IF NOT FOUND THEN
        RETURN QUERY SELECT 0, '{}'::JSONB, 'Dossier not found';
        RETURN;
    END IF;
    
    -- Calculate factors
    v_doc_completion := COALESCE(v_dossier.completion_percentage, 0);
    v_payment_progress := COALESCE(v_dossier.payment_progress, 0);
    
    -- SLA compliance (0-100 score)
    v_sla_compliance := CASE 
        WHEN v_dossier.status IN ('LEAD', 'PEMBERKASAN') THEN
            GREATEST(0, 100 - (v_dossier.age_days - 14) * 5)
        WHEN v_dossier.status IN ('PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT') THEN
            GREATEST(0, 100 - (v_dossier.age_days - 60) * 2)
        ELSE 100
    END;
    
    -- Calculate probability (weighted average)
    v_probability := (
        (v_doc_completion * 0.4) + 
        (v_payment_progress * 0.4) + 
        (v_sla_compliance * 0.2)
    );
    
    -- Build factors JSON
    v_factors := jsonb_build_object(
        'document_completion', v_doc_completion,
        'payment_progress', v_payment_progress,
        'sla_compliance', v_sla_compliance,
        'application_age', v_dossier.age_days,
        'current_status', v_dossier.status
    );
    
    -- Generate recommendation
    v_recommendation := CASE 
        WHEN v_probability >= 80 THEN 'High probability of completion - Continue normal processing'
        WHEN v_probability >= 60 THEN 'Medium probability - Focus on document completion'
        WHEN v_probability >= 40 THEN 'Low probability - Requires intervention and prioritization'
        ELSE 'Very low probability - Consider cancellation or reassignment'
    END;
    
    RETURN QUERY SELECT v_probability, v_factors, v_recommendation;
END;
$$ LANGUAGE plpgsql;

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_v_executive_summary_month ON v_executive_summary(reporting_month);
CREATE INDEX IF NOT EXISTS idx_v_operational_master_status ON v_operational_master(current_status);
CREATE INDEX IF NOT EXISTS idx_v_performance_analytics_month ON v_performance_analytics(month);
CREATE INDEX IF NOT EXISTS idx_v_risk_analytics_risk_score ON v_risk_analytics(overall_risk_score);

-- Grant permissions
GRANT SELECT ON v_executive_summary TO authenticated;
GRANT SELECT ON v_operational_master TO authenticated;
GRANT SELECT ON v_performance_analytics TO authenticated;
GRANT SELECT ON v_risk_analytics TO authenticated;
GRANT EXECUTE ON FUNCTION predict_completion_probability TO authenticated;
