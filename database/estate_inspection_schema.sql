-- =====================================================
-- QC & BAST SCHEMA
-- Estate Inspection with GPS Validation and Automated BAST
-- =====================================================

-- =====================================================
-- ESTATE INSPECTION TABLE
-- Quality control with location verification
-- =====================================================

CREATE TABLE "EstateInspection" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID REFERENCES "UnitProperty"(id) ON DELETE CASCADE,
    dossier_id UUID REFERENCES "KprDossier"(id) ON DELETE CASCADE,
    inspector_id UUID REFERENCES "UserProfile"(id), -- Role ESTATE
    status TEXT CHECK (status IN ('PASS', 'FAIL', 'PENDING', 'CANCELLED')) DEFAULT 'PENDING',
    notes TEXT,
    photo_evidence_url TEXT, -- Link ke Supabase Storage
    photo_evidence_urls TEXT[], -- Multiple photos support
    gps_coordinates GEOGRAPHY(POINT), -- Memastikan inspeksi dilakukan di lokasi
    gps_accuracy_meters DECIMAL(8, 2), -- GPS accuracy for validation
    unit_gps_coordinates GEOGRAPHY(POINT), -- Expected unit location
    distance_from_unit_meters DECIMAL(8, 2), -- Distance from actual unit location
    inspection_date DATE,
    inspection_time TIME,
    weather_condition TEXT, -- Weather during inspection
    temperature_celsius DECIMAL(5, 2), -- Temperature during inspection
    
    -- Defect tracking
    defect_count INTEGER DEFAULT 0,
    critical_defects INTEGER DEFAULT 0,
    minor_defects INTEGER DEFAULT 0,
    defect_categories TEXT[], -- Categories of defects found
    repair_required BOOLEAN DEFAULT false,
    estimated_repair_days INTEGER,
    
    -- BAST Related Fields
    bast_invitation_sent BOOLEAN DEFAULT false,
    bast_invitation_sent_at TIMESTAMP WITH TIME ZONE,
    bast_scheduled_date DATE,
    bast_scheduled_time TIME,
    bast_location TEXT,
    bast_attendees TEXT[], -- List of expected attendees
    bast_status TEXT DEFAULT 'PENDING' CHECK (bast_status IN ('PENDING', 'SCHEDULED', 'COMPLETED', 'CANCELLED')),
    bast_completed_at TIMESTAMP WITH TIME ZONE,
    bast_signed_document_url TEXT,
    bast_notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_updated_by UUID REFERENCES "UserProfile"(id)
);

-- =====================================================
-- UNIT GPS COORDINATES TABLE
-- Store expected GPS coordinates for each unit
-- =====================================================

CREATE TABLE IF NOT EXISTS "UnitGPSCoordinates" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID REFERENCES "UnitProperty"(id) ON DELETE CASCADE UNIQUE,
    block TEXT NOT NULL,
    unit_number TEXT NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    accuracy_radius_meters DECIMAL(8, 2) DEFAULT 10.0, -- Acceptable radius
    location_description TEXT,
    building_number TEXT,
    floor_number TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES "UserProfile"(id)
);

-- =====================================================
-- BAST INVITATION LINKS TABLE
-- Track generated invitation links for BAST scheduling
-- =====================================================

CREATE TABLE IF NOT EXISTS "BASTInvitationLinks" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id UUID REFERENCES "EstateInspection"(id) ON DELETE CASCADE,
    dossier_id UUID REFERENCES "KprDossier"(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES "UserProfile"(id),
    invitation_token TEXT UNIQUE NOT NULL,
    invitation_url TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used BOOLEAN DEFAULT false,
    used_at TIMESTAMP WITH TIME ZONE,
    scheduled_date DATE,
    scheduled_time TIME,
    customer_response TEXT CHECK (customer_response IN ('ACCEPTED', 'DECLINED', 'RESCHEDULED')),
    response_at TIMESTAMP WITH TIME ZONE,
    response_notes TEXT,
    whatsapp_sent BOOLEAN DEFAULT false,
    whatsapp_sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- INSPECTION DEFECTS TABLE
-- Detailed tracking of defects found during inspection
-- =====================================================

CREATE TABLE IF NOT EXISTS "InspectionDefects" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id UUID REFERENCES "EstateInspection"(id) ON DELETE CASCADE,
    defect_category TEXT NOT NULL,
    defect_description TEXT NOT NULL,
    severity TEXT CHECK (severity IN ('CRITICAL', 'MAJOR', 'MINOR')) DEFAULT 'MINOR',
    location_in_unit TEXT, -- Specific location within unit
    photo_url TEXT,
    repair_required BOOLEAN DEFAULT true,
    estimated_repair_cost DECIMAL(12, 2),
    repair_days INTEGER,
    assigned_to UUID REFERENCES "UserProfile"(id), -- Who should fix it
    status TEXT CHECK (status IN ('OPEN', 'IN_PROGRESS', 'FIXED', 'VERIFIED')) DEFAULT 'OPEN',
    fixed_at TIMESTAMP WITH TIME ZONE,
    fixed_by UUID REFERENCES "UserProfile"(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by UUID REFERENCES "UserProfile"(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- INSPECTION STATUS VIEW
-- Real-time inspection status with GPS validation
-- =====================================================

CREATE OR REPLACE VIEW v_estate_inspection_status AS
SELECT 
    ei.id,
    ei.unit_id,
    ei.dossier_id,
    up.block,
    up.unit_number,
    up.type AS unit_type,
    up.status AS unit_status,
    d.customer_name,
    d.customer_email,
    
    -- Inspector Info
    ei.inspector_id,
    inspector.name AS inspector_name,
    inspector.role AS inspector_role,
    
    -- Inspection Status
    ei.status,
    ei.notes,
    ei.photo_evidence_url,
    ei.photo_evidence_urls,
    ei.inspection_date,
    ei.inspection_time,
    ei.weather_condition,
    ei.temperature_celsius,
    
    -- GPS Validation
    ST_X(ei.gps_coordinates::geometry) AS inspection_latitude,
    ST_Y(ei.gps_coordinates::geometry) AS inspection_longitude,
    ei.gps_accuracy_meters,
    ST_X(ei.unit_gps_coordinates::geometry) AS unit_latitude,
    ST_Y(ei.unit_gps_coordinates::geometry) AS unit_longitude,
    ei.distance_from_unit_meters,
    
    -- GPS Validation Result
    CASE 
        WHEN ei.distance_from_unit_meters <= 50 THEN 'VALID'
        WHEN ei.distance_from_unit_meters <= 100 THEN 'QUESTIONABLE'
        ELSE 'INVALID'
    END AS gps_validation_status,
    
    -- Defect Summary
    ei.defect_count,
    ei.critical_defects,
    ei.minor_defects,
    ei.defect_categories,
    ei.repair_required,
    ei.estimated_repair_days,
    
    -- BAST Status
    ei.bast_invitation_sent,
    ei.bast_invitation_sent_at,
    ei.bast_scheduled_date,
    ei.bast_scheduled_time,
    ei.bast_location,
    ei.bast_status,
    ei.bast_completed_at,
    ei.bast_signed_document_url,
    ei.bast_notes,
    
    -- Timeline
    ei.created_at,
    ei.updated_at,
    ei.last_updated_by,
    
    -- Ready for BAST
    CASE 
        WHEN ei.status = 'PASS' AND ei.bast_status = 'PENDING' THEN true
        ELSE false
    END AS ready_for_bast,
    
    -- Inspection Completion
    CASE 
        WHEN ei.status IN ('PASS', 'FAIL') THEN true
        ELSE false
    END AS inspection_completed
    
FROM "EstateInspection" ei
JOIN "UnitProperty" up ON ei.unit_id = up.id
JOIN "KprDossier" d ON ei.dossier_id = d.id
LEFT JOIN "UserProfile" inspector ON ei.inspector_id = inspector.id
LEFT JOIN "UnitGPSCoordinates" unit_gps ON ei.unit_id = unit_gps.unit_id;

-- =====================================================
-- QC DASHBOARD SUMMARY VIEW
-- =====================================================

CREATE OR REPLACE VIEW v_qc_dashboard_summary AS
SELECT 
    -- Overall Metrics
    COUNT(*) AS total_inspections,
    COUNT(CASE WHEN status = 'PASS' THEN 1 END) AS passed_inspections,
    COUNT(CASE WHEN status = 'FAIL' THEN 1 END) AS failed_inspections,
    COUNT(CASE WHEN status = 'PENDING' THEN 1 END) AS pending_inspections,
    
    -- GPS Validation Metrics
    COUNT(CASE WHEN gps_validation_status = 'VALID' THEN 1 END) AS valid_gps_inspections,
    COUNT(CASE WHEN gps_validation_status = 'QUESTIONABLE' THEN 1 END) AS questionable_gps_inspections,
    COUNT(CASE WHEN gps_validation_status = 'INVALID' THEN 1 END) AS invalid_gps_inspections,
    
    -- Defect Metrics
    SUM(defect_count) AS total_defects,
    SUM(critical_defects) AS total_critical_defects,
    SUM(minor_defects) AS total_minor_defects,
    AVG(defect_count) AS avg_defects_per_inspection,
    
    -- BAST Metrics
    COUNT(CASE WHEN bast_invitation_sent = true THEN 1 END) AS bast_invitations_sent,
    COUNT(CASE WHEN bast_status = 'SCHEDULED' THEN 1 END) AS bast_scheduled,
    COUNT(CASE WHEN bast_status = 'COMPLETED' THEN 1 END) AS bast_completed,
    COUNT(CASE WHEN ready_for_bast = true THEN 1 END) AS ready_for_bast,
    
    -- Quality Metrics
    ROUND(COUNT(CASE WHEN status = 'PASS' THEN 1 END) * 100.0 / COUNT(*), 2) AS pass_rate,
    ROUND(COUNT(CASE WHEN gps_validation_status = 'VALID' THEN 1 END) * 100.0 / COUNT(*), 2) AS gps_validity_rate,
    ROUND(AVG(distance_from_unit_meters), 2) AS avg_distance_from_unit,
    
    -- Time Metrics
    AVG(estimated_repair_days) FILTER (WHERE estimated_repair_days IS NOT NULL) AS avg_repair_days,
    COUNT(CASE WHEN repair_required = true THEN 1 END) AS units_requiring_repair,
    
    -- Report Date
    CURRENT_DATE AS report_date
    
FROM v_estate_inspection_status;

-- =====================================================
-- TRIGGERS FOR AUTOMATIC WORKFLOWS
-- =====================================================

-- Function to calculate GPS distance and validation
CREATE OR REPLACE FUNCTION calculate_gps_distance()
RETURNS TRIGGER AS $$
BEGIN
    -- Calculate distance from unit if GPS coordinates are available
    IF NEW.gps_coordinates IS NOT NULL AND NEW.unit_id IS NOT NULL THEN
        DECLARE
            unit_gps GEOGRAPHY(POINT);
        BEGIN
            -- Get unit GPS coordinates
            SELECT coordinates INTO unit_gps
            FROM "UnitGPSCoordinates"
            WHERE unit_id = NEW.unit_id;
            
            IF unit_gps IS NOT NULL THEN
                -- Calculate distance
                NEW.distance_from_unit_meters := ST_Distance(NEW.gps_coordinates, unit_gps);
                NEW.unit_gps_coordinates := unit_gps;
            END IF;
        END;
    END IF;
    
    -- Update defect counts based on related defects
    IF TG_OP = 'UPDATE' OR TG_OP = 'INSERT' THEN
        DECLARE
            defect_counts RECORD;
        BEGIN
            SELECT 
                COUNT(*) as total,
                COUNT(CASE WHEN severity = 'CRITICAL' THEN 1 END) as critical,
                COUNT(CASE WHEN severity = 'MINOR' THEN 1 END) as minor,
                COUNT(CASE WHEN repair_required = true THEN 1 END) as repair_count,
                STRING_AGG(DISTINCT defect_category, ', ') as categories,
                AVG(estimated_repair_days) FILTER (WHERE estimated_repair_days IS NOT NULL) as avg_days
            INTO defect_counts
            FROM "InspectionDefects"
            WHERE inspection_id = NEW.id;
            
            NEW.defect_count := defect_counts.total;
            NEW.critical_defects := defect_counts.critical;
            NEW.minor_defects := defect_counts.minor;
            NEW.defect_categories := string_to_array(defect_counts.categories, ', ');
            NEW.repair_required := defect_counts.repair_count > 0;
            NEW.estimated_repair_days := defect_counts.avg_days;
        END;
    END IF;
    
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply GPS distance trigger
CREATE TRIGGER trigger_calculate_gps_distance
    BEFORE INSERT OR UPDATE ON "EstateInspection"
    FOR EACH ROW EXECUTE FUNCTION calculate_gps_distance();

-- Function to trigger BAST invitation when inspection passes
CREATE OR REPLACE FUNCTION trigger_bast_invitation()
RETURNS TRIGGER AS $$
BEGIN
    -- If inspection status changed to PASS, trigger BAST invitation
    IF TG_OP = 'UPDATE' AND OLD.status != 'PASS' AND NEW.status = 'PASS' THEN
        -- This will be handled by the Edge Function via webhook
        -- Insert record into BASTInvitationLinks will be done by Edge Function
        NEW.bast_invitation_sent := false; -- Will be updated by Edge Function
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply BAST invitation trigger
CREATE TRIGGER trigger_bast_invitation
    AFTER UPDATE ON "EstateInspection"
    FOR EACH ROW EXECUTE FUNCTION trigger_bast_invitation();

-- =====================================================
-- SECURITY FUNCTIONS FOR GPS VALIDATION
-- =====================================================

-- Function to validate GPS coordinates for inspection
CREATE OR REPLACE FUNCTION validate_inspection_gps(
    inspection_id UUID,
    user_id UUID
) RETURNS BOOLEAN AS $$
DECLARE
    inspection_gps GEOGRAPHY(POINT);
    unit_gps GEOGRAPHY(POINT);
    distance_meters DECIMAL;
    user_role TEXT;
BEGIN
    -- Get user role
    SELECT role INTO user_role
    FROM "UserProfile"
    WHERE id = user_id;
    
    -- Only ESTATE role can validate GPS
    IF user_role != 'ESTATE' THEN
        RETURN false;
    END IF;
    
    -- Get inspection GPS
    SELECT gps_coordinates, unit_gps_coordinates, distance_from_unit_meters
    INTO inspection_gps, unit_gps, distance_meters
    FROM "EstateInspection"
    WHERE id = inspection_id;
    
    -- Validate GPS coordinates
    IF inspection_gps IS NULL OR unit_gps IS NULL THEN
        RETURN false;
    END IF;
    
    -- Check if within acceptable radius (50 meters)
    RETURN distance_meters <= 50;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_estate_inspection_unit_id ON "EstateInspection"(unit_id);
CREATE INDEX IF NOT EXISTS idx_estate_inspection_dossier_id ON "EstateInspection"(dossier_id);
CREATE INDEX IF NOT EXISTS idx_estate_inspection_inspector_id ON "EstateInspection"(inspector_id);
CREATE INDEX IF NOT EXISTS idx_estate_inspection_status ON "EstateInspection"(status);
CREATE INDEX IF NOT EXISTS idx_estate_inspection_gps_coordinates ON "EstateInspection" USING GIST(gps_coordinates);
CREATE INDEX IF NOT EXISTS idx_estate_inspection_bast_status ON "EstateInspection"(bast_status);
CREATE INDEX IF NOT EXISTS idx_estate_inspection_created_at ON "EstateInspection"(created_at);

CREATE INDEX IF NOT EXISTS idx_unit_gps_coordinates_unit_id ON "UnitGPSCoordinates"(unit_id);
CREATE INDEX IF NOT EXISTS idx_unit_gps_coordinates_coordinates ON "UnitGPSCoordinates" USING GIST(coordinates);

CREATE INDEX IF NOT EXISTS idx_bast_invitation_links_token ON "BASTInvitationLinks"(invitation_token);
CREATE INDEX IF NOT EXISTS idx_bast_invitation_links_dossier_id ON "BASTInvitationLinks"(dossier_id);
CREATE INDEX IF NOT EXISTS idx_bast_invitation_links_expires_at ON "BASTInvitationLinks"(expires_at);

CREATE INDEX IF NOT EXISTS idx_inspection_defects_inspection_id ON "InspectionDefects"(inspection_id);
CREATE INDEX IF NOT EXISTS idx_inspection_defects_severity ON "InspectionDefects"(severity);
CREATE INDEX IF NOT EXISTS idx_inspection_defects_status ON "InspectionDefects"(status);

-- =====================================================
-- RLS POLICIES FOR INSPECTION DATA
-- =====================================================

ALTER TABLE "EstateInspection" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "UnitGPSCoordinates" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "BASTInvitationLinks" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "InspectionDefects" ENABLE ROW LEVEL SECURITY;

-- Estate team can manage inspections
CREATE POLICY "Estate manage inspections" ON "EstateInspection"
    FOR ALL USING (auth.jwt() ->> 'role' = 'ESTATE');

-- BOD and Management can view inspections
CREATE POLICY "Management view inspections" ON "EstateInspection"
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('BOD', 'MANAGER'));

-- Marketing can view inspection status for their customers
CREATE POLICY "Marketing view customer inspections" ON "EstateInspection"
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'MARKETING' AND
        dossier_id IN (
            SELECT id FROM "KprDossier" 
            WHERE assigned_to::text = auth.uid()::text
        )
    );

-- Legal can view inspections for BAST preparation
CREATE POLICY "Legal view inspections" ON "EstateInspection"
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Finance can view inspections for completion tracking
CREATE POLICY "Finance view inspections" ON "EstateInspection"
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- Customer can view their own inspection results
CREATE POLICY "Customer view own inspections" ON "EstateInspection"
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'CUSTOMER' AND
        dossier_id IN (
            SELECT id FROM "KprDossier" 
            WHERE user_id::text = auth.uid()::text
        )
    );

-- RLS for views
ALTER VIEW v_estate_inspection_status SET (security_barrier = true);
ALTER VIEW v_qc_dashboard_summary SET (security_barrier = true);

-- View policies
CREATE POLICY "All roles view inspection status" ON v_estate_inspection_status
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('ESTATE', 'BOD', 'MANAGER', 'MARKETING', 'LEGAL', 'FINANCE', 'CUSTOMER'));

CREATE POLICY "Management view QC summary" ON v_qc_dashboard_summary
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('BOD', 'MANAGER', 'ESTATE'));

-- =====================================================
-- SAMPLE QUERIES FOR TESTING
-- =====================================================

-- Get inspection status for all units
-- SELECT * FROM v_estate_inspection_status ORDER BY created_at DESC;

-- Get QC dashboard summary
-- SELECT * FROM v_qc_dashboard_summary;

-- Get inspections ready for BAST
-- SELECT * FROM v_estate_inspection_status WHERE ready_for_bast = true;

-- Validate GPS coordinates for inspection
-- SELECT validate_inspection_gps('inspection-uuid', 'estate-user-uuid');

-- Get GPS validation statistics
-- SELECT gps_validation_status, COUNT(*) FROM v_estate_inspection_status GROUP BY gps_validation_status;

-- Create new inspection with GPS coordinates
-- INSERT INTO "EstateInspection" (unit_id, dossier_id, inspector_id, status, gps_coordinates)
-- VALUES ('unit-uuid', 'dossier-uuid', 'inspector-uuid', 'PASS', ST_GeomFromText('POINT(106.8271 -6.1751)', 4326));

-- Add unit GPS coordinates
-- INSERT INTO "UnitGPSCoordinates" (unit_id, block, unit_number, latitude, longitude, created_by)
-- VALUES ('unit-uuid', 'A', '12', -6.1751, 106.8271, 'admin-uuid');
