-- =====================================================
-- LEGAL DOCUMENTS SCHEMA
-- Automated Document Linking with Google Drive Integration
-- =====================================================

-- =====================================================
-- UNIT PROPERTY DOCUMENT REFERENCES
-- =====================================================

-- Add document reference columns to UnitProperty
ALTER TABLE "UnitProperty" 
ADD COLUMN IF NOT EXISTS shgb_url TEXT,
ADD COLUMN IF NOT EXISTS pbg_url TEXT,
ADD COLUMN IF NOT EXISTS imb_url TEXT,
ADD COLUMN IF NOT EXISTS legal_sync_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS legal_sync_status TEXT DEFAULT 'PENDING' CHECK (legal_sync_status IN ('PENDING', 'SYNCED', 'ERROR', 'NOT_FOUND')),
ADD COLUMN IF NOT EXISTS legal_sync_error TEXT;

-- =====================================================
-- LEGAL DOCUMENT SYNC LOG
-- =====================================================

CREATE TABLE IF NOT EXISTS "LegalDocumentSync" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID REFERENCES "UnitProperty"(id) ON DELETE CASCADE,
    document_type TEXT NOT NULL CHECK (document_type IN ('SHGB', 'PBG', 'IMB')),
    file_name TEXT NOT NULL,
    gdrive_file_id TEXT,
    gdrive_url TEXT,
    supabase_url TEXT,
    sync_status TEXT DEFAULT 'PENDING' CHECK (sync_status IN ('PENDING', 'DOWNLOADING', 'UPLOADING', 'SYNCED', 'ERROR')),
    sync_error TEXT,
    file_size BIGINT,
    mime_type TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    synced_at TIMESTAMP WITH TIME ZONE
);

-- =====================================================
-- DOCUMENT STATUS VIEW
-- =====================================================

CREATE OR REPLACE VIEW v_legal_document_status AS
SELECT 
    up.id AS unit_id,
    up.block,
    up.unit_number,
    up.type AS unit_type,
    up.status AS unit_status,
    up.price AS unit_price,
    
    -- Document URLs
    up.shgb_url,
    up.pbg_url,
    up.imb_url,
    
    -- Sync status
    up.legal_sync_at,
    up.legal_sync_status,
    up.legal_sync_error,
    
    -- Document availability flags
    CASE 
        WHEN up.shgb_url IS NOT NULL AND up.shgb_url != '' THEN true 
        ELSE false 
    END AS has_shgb,
    
    CASE 
        WHEN up.pbg_url IS NOT NULL AND up.pbg_url != '' THEN true 
        ELSE false 
    END AS has_pbg,
    
    CASE 
        WHEN up.imb_url IS NOT NULL AND up.imb_url != '' THEN true 
        ELSE false 
    END AS has_imb,
    
    -- Document completeness score
    CASE 
        WHEN up.shgb_url IS NOT NULL AND up.pbg_url IS NOT NULL AND up.imb_url IS NOT NULL THEN 3
        WHEN (up.shgb_url IS NOT NULL AND up.pbg_url IS NOT NULL) OR 
             (up.shgb_url IS NOT NULL AND up.imb_url IS NOT NULL) OR 
             (up.pbg_url IS NOT NULL AND up.imb_url IS NOT NULL) THEN 2
        WHEN up.shgb_url IS NOT NULL OR up.pbg_url IS NOT NULL OR up.imb_url IS NOT NULL THEN 1
        ELSE 0
    END AS document_completeness_score,
    
    -- Legal readiness status
    CASE 
        WHEN up.shgb_url IS NOT NULL AND up.pbg_url IS NOT NULL AND up.imb_url IS NOT NULL THEN 'READY'
        WHEN up.shgb_url IS NOT NULL THEN 'PARTIAL'
        ELSE 'INCOMPLETE'
    END AS legal_readiness_status,
    
    -- Last sync info
    (SELECT MAX(synced_at) 
     FROM "LegalDocumentSync" 
     WHERE unit_id = up.id) AS last_document_sync,
    
    -- Total sync attempts
    (SELECT COUNT(*) 
     FROM "LegalDocumentSync" 
     WHERE unit_id = up.id) AS total_sync_attempts
    
FROM "UnitProperty" up;

-- =====================================================
-- LEGAL DASHBOARD SUMMARY VIEW
-- =====================================================

CREATE OR REPLACE VIEW v_legal_dashboard_summary AS
SELECT 
    -- Total units
    COUNT(*) AS total_units,
    
    -- Document availability
    COUNT(CASE WHEN has_shgb THEN 1 END) AS units_with_shgb,
    COUNT(CASE WHEN has_pbg THEN 1 END) AS units_with_pbg,
    COUNT(CASE WHEN has_imb THEN 1 END) AS units_with_imb,
    
    -- Readiness status
    COUNT(CASE WHEN legal_readiness_status = 'READY' THEN 1 END) AS ready_units,
    COUNT(CASE WHEN legal_readiness_status = 'PARTIAL' THEN 1 END) AS partial_units,
    COUNT(CASE WHEN legal_readiness_status = 'INCOMPLETE' THEN 1 END) AS incomplete_units,
    
    -- Sync status
    COUNT(CASE WHEN legal_sync_status = 'SYNCED' THEN 1 END) AS synced_units,
    COUNT(CASE WHEN legal_sync_status = 'PENDING' THEN 1 END) AS pending_units,
    COUNT(CASE WHEN legal_sync_status = 'ERROR' THEN 1 END) AS error_units,
    
    -- Completion metrics
    AVG(document_completeness_score) AS avg_completeness_score,
    MAX(document_completeness_score) AS max_completeness_score,
    MIN(document_completeness_score) AS min_completeness_score,
    
    -- Readiness percentage
    ROUND(
        (COUNT(CASE WHEN legal_readiness_status = 'READY' THEN 1 END) * 100.0 / COUNT(*)), 2
    ) AS readiness_percentage,
    
    -- Document type coverage
    ROUND(
        (COUNT(CASE WHEN has_shgb THEN 1 END) * 100.0 / COUNT(*)), 2
    ) AS shgb_coverage_percentage,
    
    ROUND(
        (COUNT(CASE WHEN has_pbg THEN 1 END) * 100.0 / COUNT(*)), 2
    ) AS pbg_coverage_percentage,
    
    ROUND(
        (COUNT(CASE WHEN has_imb THEN 1 END) * 100.0 / COUNT(*)), 2
    ) AS imb_coverage_percentage,
    
    -- Recent sync activity
    COUNT(CASE WHEN legal_sync_at >= NOW() - INTERVAL '7 days' THEN 1 END) AS synced_last_7_days,
    COUNT(CASE WHEN legal_sync_at >= NOW() - INTERVAL '30 days' THEN 1 END) AS synced_last_30_days,
    
    -- Error tracking
    COUNT(CASE WHEN legal_sync_error IS NOT NULL THEN 1 END) AS units_with_errors,
    
    -- Report date
    CURRENT_DATE AS report_date
    
FROM v_legal_document_status;

-- =====================================================
-- TRIGGERS FOR AUTOMATIC SYNC STATUS
-- =====================================================

-- Function to update legal sync status when documents are added
CREATE OR REPLACE FUNCTION update_legal_sync_status()
RETURNS TRIGGER AS $$
BEGIN
    -- Update unit sync status based on document availability
    IF TG_OP = 'UPDATE' THEN
        DECLARE
            doc_count INTEGER;
        BEGIN
            -- Count available documents
            SELECT (CASE WHEN NEW.shgb_url IS NOT NULL AND NEW.shgb_url != '' THEN 1 ELSE 0 END) +
                   (CASE WHEN NEW.pbg_url IS NOT NULL AND NEW.pbg_url != '' THEN 1 ELSE 0 END) +
                   (CASE WHEN NEW.imb_url IS NOT NULL AND NEW.imb_url != '' THEN 1 ELSE 0 END)
            INTO doc_count;
            
            -- Update sync status
            IF doc_count = 3 THEN
                NEW.legal_sync_status = 'SYNCED';
            ELSE
                NEW.legal_sync_status = 'PARTIAL';
            END IF;
            
            NEW.legal_sync_at = NOW();
        END;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger
CREATE TRIGGER trigger_update_legal_sync_status
    BEFORE UPDATE ON "UnitProperty"
    FOR EACH ROW EXECUTE FUNCTION update_legal_sync_status();

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_unit_property_block ON "UnitProperty"(block);
CREATE INDEX IF NOT EXISTS idx_unit_property_legal_sync_status ON "UnitProperty"(legal_sync_status);
CREATE INDEX IF NOT EXISTS idx_unit_property_legal_sync_at ON "UnitProperty"(legal_sync_at);
CREATE INDEX IF NOT EXISTS idx_legal_document_sync_unit_id ON "LegalDocumentSync"(unit_id);
CREATE INDEX IF NOT EXISTS idx_legal_document_sync_status ON "LegalDocumentSync"(sync_status);
CREATE INDEX IF NOT EXISTS idx_legal_document_sync_type ON "LegalDocumentSync"(document_type);

-- =====================================================
-- RLS POLICIES FOR LEGAL DOCUMENTS
-- =====================================================

ALTER TABLE "UnitProperty" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "LegalDocumentSync" ENABLE ROW LEVEL SECURITY;

-- Legal can view all document statuses
CREATE POLICY "Legal view all document statuses" ON "UnitProperty"
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- BOD can view all document statuses
CREATE POLICY "BOD view all document statuses" ON "UnitProperty"
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Marketing can view document statuses for their units
CREATE POLICY "Marketing view document statuses" ON "UnitProperty"
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'MARKETING'
    );

-- Finance can view document statuses
CREATE POLICY "Finance view document statuses" ON "UnitProperty"
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- Legal can update document URLs
CREATE POLICY "Legal can update documents" ON "UnitProperty"
    FOR UPDATE USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Legal can manage sync logs
CREATE POLICY "Legal manage sync logs" ON "LegalDocumentSync"
    FOR ALL USING (auth.jwt() ->> 'role' = 'LEGAL');

-- BOD can view sync logs
CREATE POLICY "BOD view sync logs" ON "LegalDocumentSync"
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- RLS for views
ALTER VIEW v_legal_document_status SET (security_barrier = true);
ALTER VIEW v_legal_dashboard_summary SET (security_barrier = true);

-- View policies
CREATE POLICY "Legal view document status" ON v_legal_document_status
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD', 'MARKETING', 'FINANCE'));

CREATE POLICY "BOD view dashboard summary" ON v_legal_dashboard_summary
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

CREATE POLICY "Legal view dashboard summary" ON v_legal_dashboard_summary
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- =====================================================
-- SAMPLE QUERIES FOR TESTING
-- =====================================================

-- Get legal document status for all units
-- SELECT * FROM v_legal_document_status ORDER BY block, unit_number;

-- Get legal dashboard summary
-- SELECT * FROM v_legal_dashboard_summary;

-- Get units ready for legal processing
-- SELECT * FROM v_legal_document_status WHERE legal_readiness_status = 'READY';

-- Get units with missing documents
-- SELECT * FROM v_legal_document_status WHERE legal_readiness_status = 'INCOMPLETE';

-- Get sync errors
-- SELECT * FROM "UnitProperty" WHERE legal_sync_status = 'ERROR';

-- Update document URL (Legal only)
-- UPDATE "UnitProperty" 
-- SET shgb_url = 'https://storage.supabase.com/legal_documents/SHGB_A12.pdf',
--     legal_sync_at = NOW()
-- WHERE block = 'A12';

-- Get document sync history
-- SELECT * FROM "LegalDocumentSync" WHERE unit_id = 'unit-uuid' ORDER BY created_at DESC;
