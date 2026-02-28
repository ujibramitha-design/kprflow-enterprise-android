-- =====================================================
-- REFACTORING DATA LAYER: SQL VIEWS & FUNCTIONS
-- SLA Countdown Logic moved to PostgreSQL for performance
-- =====================================================

-- =====================================================
-- DOSSIER SLA STATUS VIEW
-- =====================================================

CREATE OR REPLACE VIEW v_dossier_sla_status AS
SELECT 
    d.id AS dossier_id,
    u.name AS customer_name,
    u.email AS customer_email,
    u.phone_number AS customer_phone,
    d.status,
    d.booking_date,
    d.created_at,
    d.updated_at,
    d.unit_id,
    d.bank_name,
    d.notes,
    
    -- Hitung sisa hari untuk dokumen (14 hari SLA)
    GREATEST(0, 14 - (EXTRACT(DAY FROM (NOW() - d.booking_date)))) AS doc_days_left,
    
    -- Hitung sisa hari untuk Bank (60 hari SLA) 
    GREATEST(0, 60 - (EXTRACT(DAY FROM (NOW() - d.booking_date)))) AS bank_days_left,
    
    -- Total hari yang berlalu sejak booking
    EXTRACT(DAY FROM (NOW() - d.booking_date)) AS days_elapsed,
    
    -- Flag untuk overdue dokumen [cite: 19]
    CASE 
        WHEN (EXTRACT(DAY FROM (NOW() - d.booking_date))) > 14 
             AND d.status NOT IN ('FLOATING_DOSSIER', 'CANCELLED_BY_SYSTEM', 'BAST_COMPLETED') 
        THEN true 
        ELSE false 
    END AS is_doc_overdue,
    
    -- Flag untuk overdue bank
    CASE 
        WHEN (EXTRACT(DAY FROM (NOW() - d.booking_date))) > 60 
             AND d.status IN ('PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'PRA_AKAD', 'AKAD_BELUM_CAIR')
        THEN true 
        ELSE false 
    END AS is_bank_overdue,
    
    -- SLA Status untuk UI
    CASE 
        WHEN d.status = 'CANCELLED_BY_SYSTEM' THEN 'CANCELLED'
        WHEN d.status = 'BAST_COMPLETED' THEN 'COMPLETED'
        WHEN (EXTRACT(DAY FROM (NOW() - d.booking_date))) > 14 
             AND d.status NOT IN ('FLOATING_DOSSIER', 'CANCELLED_BY_SYSTEM', 'BAST_COMPLETED') 
        THEN 'DOC_OVERDUE'
        WHEN (EXTRACT(DAY FROM (NOW() - d.booking_date))) > 60 
             AND d.status IN ('PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'PRA_AKAD', 'AKAD_BELUM_CAIR')
        THEN 'BANK_OVERDUE'
        WHEN (EXTRACT(DAY FROM (NOW() - d.booking_date))) <= 7 THEN 'NORMAL'
        WHEN (EXTRACT(DAY FROM (NOW() - d.booking_date))) <= 14 THEN 'WARNING'
        ELSE 'CRITICAL'
    END AS sla_status,
    
    -- Priority level untuk sorting
    CASE 
        WHEN d.status = 'CANCELLED_BY_SYSTEM' THEN 0
        WHEN d.status = 'BAST_COMPLETED' THEN 1
        WHEN (EXTRACT(DAY FROM (NOW() - d.booking_date))) > 14 
             AND d.status NOT IN ('FLOATING_DOSSIER', 'CANCELLED_BY_SYSTEM', 'BAST_COMPLETED') 
        THEN 5
        WHEN (EXTRACT(DAY FROM (NOW() - d.booking_date))) > 60 
             AND d.status IN ('PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'PRA_AKAD', 'AKAD_BELUM_CAIR')
        THEN 4
        WHEN (EXTRACT(DAY FROM (NOW() - d.booking_date))) <= 7 THEN 2
        WHEN (EXTRACT(DAY FROM (NOW() - d.booking_date))) <= 14 THEN 3
        ELSE 4
    END AS priority_level
    
FROM kpr_dossiers d
JOIN user_profiles u ON d.user_id::text = u.id::text;

-- =====================================================
-- ENHANCED SLA VIEW WITH UNIT INFO
-- =====================================================

CREATE OR REPLACE VIEW v_dossier_sla_enhanced AS
SELECT 
    sla.*,
    up.block,
    up.unit_number,
    up.type AS unit_type,
    up.price AS unit_price,
    up.status AS unit_status,
    
    -- Document completion percentage
    CASE 
        WHEN sla.status = 'LEAD' THEN 0
        WHEN sla.status = 'PEMBERKASAN' THEN 25
        WHEN sla.status = 'PROSES_BANK' THEN 50
        WHEN sla.status = 'PUTUSAN_KREDIT_ACC' THEN 75
        WHEN sla.status = 'SP3K_TERBIT' THEN 85
        WHEN sla.status = 'PRA_AKAD' THEN 90
        WHEN sla.status = 'AKAD_BELUM_CAIR' THEN 95
        WHEN sla.status = 'FUNDS_DISBURSED' THEN 98
        WHEN sla.status = 'BAST_READY' THEN 99
        WHEN sla.status = 'BAST_COMPLETED' THEN 100
        ELSE 0
    END AS completion_percentage,
    
    -- Estimated completion date
    CASE 
        WHEN sla.status = 'LEAD' THEN sla.booking_date + INTERVAL '14 days'
        WHEN sla.status = 'PEMBERKASAN' THEN sla.booking_date + INTERVAL '30 days'
        WHEN sla.status = 'PROSES_BANK' THEN sla.booking_date + INTERVAL '60 days'
        WHEN sla.status = 'PUTUSAN_KREDIT_ACC' THEN sla.booking_date + INTERVAL '75 days'
        WHEN sla.status = 'SP3K_TERBIT' THEN sla.booking_date + INTERVAL '85 days'
        WHEN sla.status = 'PRA_AKAD' THEN sla.booking_date + INTERVAL '90 days'
        WHEN sla.status = 'AKAD_BELUM_CAIR' THEN sla.booking_date + INTERVAL '95 days'
        WHEN sla.status = 'FUNDS_DISBURSED' THEN sla.booking_date + INTERVAL '98 days'
        WHEN sla.status = 'BAST_READY' THEN sla.booking_date + INTERVAL '99 days'
        ELSE NULL
    END AS estimated_completion_date
    
FROM v_dossier_sla_status sla
LEFT JOIN unit_properties up ON sla.unit_id::text = up.id::text;

-- =====================================================
-- SLA FUNCTIONS
-- =====================================================

-- Function to get SLA status for specific dossier
CREATE OR REPLACE FUNCTION get_dossier_sla_status(p_dossier_id UUID)
RETURNS TABLE(
    dossier_id UUID,
    customer_name TEXT,
    status TEXT,
    doc_days_left INTEGER,
    bank_days_left INTEGER,
    is_doc_overdue BOOLEAN,
    is_bank_overdue BOOLEAN,
    sla_status TEXT,
    priority_level INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT * FROM v_dossier_sla_status 
    WHERE dossier_id = p_dossier_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get overdue dossiers
CREATE OR REPLACE FUNCTION get_overdue_dossiers(p_type TEXT DEFAULT 'ALL')
RETURNS TABLE(
    dossier_id UUID,
    customer_name TEXT,
    status TEXT,
    days_overdue INTEGER,
    overdue_type TEXT,
    priority_level INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        dossier_id,
        customer_name,
        status,
        CASE 
            WHEN is_doc_overdue THEN (EXTRACT(DAY FROM (NOW() - booking_date)) - 14)::INTEGER
            WHEN is_bank_overdue THEN (EXTRACT(DAY FROM (NOW() - booking_date)) - 60)::INTEGER
            ELSE 0
        END AS days_overdue,
        CASE 
            WHEN is_doc_overdue THEN 'DOCUMENT'
            WHEN is_bank_overdue THEN 'BANK'
            ELSE 'NONE'
        END AS overdue_type,
        priority_level
    FROM v_dossier_sla_status
    WHERE (is_doc_overdue = true AND p_type IN ('ALL', 'DOCUMENT'))
       OR (is_bank_overdue = true AND p_type IN ('ALL', 'BANK'))
    ORDER BY priority_level DESC, days_overdue DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to update SLA metrics (trigger function)
CREATE OR REPLACE FUNCTION update_sla_metrics()
RETURNS TRIGGER AS $$
BEGIN
    -- This function can be called by triggers to update SLA-related metrics
    -- For now, it's a placeholder for future enhancements
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- SLA TRIGGERS
-- =====================================================

-- Trigger to automatically update SLA status when dossier changes
CREATE TRIGGER trigger_update_sla_metrics
    AFTER UPDATE OR INSERT ON kpr_dossiers
    FOR EACH ROW EXECUTE FUNCTION update_sla_metrics();

-- =====================================================
-- SLA SUMMARY VIEWS FOR DASHBOARDS
-- =====================================================

-- Marketing Dashboard SLA Summary
CREATE OR REPLACE VIEW v_marketing_sla_summary AS
SELECT 
    COUNT(*) as total_dossiers,
    COUNT(CASE WHEN status = 'LEAD' THEN 1 END) as leads,
    COUNT(CASE WHEN status = 'PEMBERKASAN' THEN 1 END) as document_collection,
    COUNT(CASE WHEN is_doc_overdue = true THEN 1 END) as doc_overdue_count,
    COUNT(CASE WHEN is_bank_overdue = true THEN 1 END) as bank_overdue_count,
    ROUND(AVG(doc_days_left), 1) as avg_doc_days_left,
    ROUND(AVG(bank_days_left), 1) as avg_bank_days_left,
    COUNT(CASE WHEN sla_status = 'CRITICAL' THEN 1 END) as critical_count,
    COUNT(CASE WHEN sla_status = 'WARNING' THEN 1 END) as warning_count,
    COUNT(CASE WHEN sla_status = 'NORMAL' THEN 1 END) as normal_count
FROM v_dossier_sla_status
WHERE status NOT IN ('CANCELLED_BY_SYSTEM', 'BAST_COMPLETED');

-- Legal Dashboard SLA Summary
CREATE OR REPLACE VIEW v_legal_sla_summary AS
SELECT 
    COUNT(*) as total_active_dossiers,
    COUNT(CASE WHEN is_doc_overdue = true THEN 1 END) as overdue_documents,
    ROUND(AVG(doc_days_left), 1) as avg_days_remaining,
    COUNT(CASE WHEN doc_days_left <= 3 THEN 1 END) as urgent_count,
    COUNT(CASE WHEN doc_days_left <= 7 THEN 1 END) as warning_count,
    MAX(days_elapsed) as max_days_elapsed
FROM v_dossier_sla_status
WHERE status IN ('PEMBERKASAN', 'PROSES_BANK')
  AND status NOT IN ('CANCELLED_BY_SYSTEM', 'BAST_COMPLETED');

-- Finance Dashboard SLA Summary
CREATE OR REPLACE VIEW v_finance_sla_summary AS
SELECT 
    COUNT(*) as total_bank_dossiers,
    COUNT(CASE WHEN is_bank_overdue = true THEN 1 END) as overdue_bank,
    ROUND(AVG(bank_days_left), 1) as avg_bank_days_left,
    COUNT(CASE WHEN bank_days_left <= 7 THEN 1 END) as urgent_count,
    COUNT(CASE WHEN bank_days_left <= 15 THEN 1 END) as warning_count,
    SUM(CASE WHEN status = 'FUNDS_DISBURSED' THEN 1 ELSE 0 END) as disbursed_count
FROM v_dossier_sla_status
WHERE status IN ('PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT', 'PRA_AKAD', 'AKAD_BELUM_CAIR', 'FUNDS_DISBURSED')
  AND status NOT IN ('CANCELLED_BY_SYSTEM', 'BAST_COMPLETED');

-- =====================================================
-- PERFORMANCE OPTIMIZATION INDEXES
-- =====================================================

-- Indexes for SLA queries performance
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_booking_date ON kpr_dossiers(booking_date);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_status_booking ON kpr_dossiers(status, booking_date);
CREATE INDEX IF NOT EXISTS idx_user_profiles_name ON user_profiles(name);

-- Indexes for views (materialized if needed for large datasets)
-- CREATE MATERIALIZED VIEW mv_dossier_sla_status AS SELECT * FROM v_dossier_sla_status;
-- CREATE INDEX IF NOT EXISTS idx_mv_dossier_sla_status_priority ON mv_dossier_sla_status(priority_level);

-- =====================================================
-- RLS POLICIES FOR SLA VIEWS
-- =====================================================

-- Marketing can view all SLA data
CREATE POLICY "Marketing view SLA data" ON v_dossier_sla_status
    FOR SELECT USING (auth.jwt() ->> 'role' = 'MARKETING');

-- Legal can view SLA data for document processing
CREATE POLICY "Legal view SLA data" ON v_dossier_sla_status
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Finance can view SLA data for bank processing
CREATE POLICY "Finance view SLA data" ON v_dossier_sla_status
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- BOD can view all SLA data
CREATE POLICY "BOD view SLA data" ON v_dossier_sla_status
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Customers can view their own SLA data
CREATE POLICY "Customers view own SLA data" ON v_dossier_sla_status
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'CUSTOMER' AND
        dossier_id IN (
            SELECT id FROM kpr_dossiers WHERE user_id::text = auth.uid()::text
        )
    );

-- =====================================================
-- SAMPLE QUERIES FOR TESTING
-- =====================================================

-- Get all overdue dossiers
-- SELECT * FROM get_overdue_dossiers('ALL');

-- Get specific dossier SLA status
-- SELECT * FROM get_dossier_sla_status('your-dossier-id');

-- Get marketing SLA summary
-- SELECT * FROM v_marketing_sla_summary;

-- Get critical dossiers (doc overdue)
-- SELECT * FROM v_dossier_sla_status WHERE is_doc_overdue = true ORDER BY priority_level DESC;

-- Get dossiers with less than 3 days left for documents
-- SELECT * FROM v_dossier_sla_status WHERE doc_days_left <= 3 AND status = 'PEMBERKASAN';
