-- Map Site Plan Master Implementation
-- Phase 16: Mobile App Optimization - Enhanced Features
-- KPRFlow Enterprise Property Mapping System

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis"; -- For geospatial data
CREATE EXTENSION IF NOT EXISTS "postgis_topology";

-- =====================================================
-- MAP SITE PLAN MASTER TABLES
-- =====================================================

-- Master Site Plan Table
CREATE TABLE site_plan_master (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_name VARCHAR(255) NOT NULL,
    project_code VARCHAR(50) UNIQUE NOT NULL,
    developer_name VARCHAR(255) NOT NULL,
    location_address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    province VARCHAR(100) NOT NULL,
    postal_code VARCHAR(10),
    total_area DECIMAL(15,2) NOT NULL, -- Total area in square meters
    total_units INTEGER NOT NULL DEFAULT 0,
    project_status VARCHAR(50) NOT NULL DEFAULT 'PLANNING', -- PLANNING, DEVELOPMENT, COMPLETED
    start_date DATE,
    completion_date DATE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    is_active BOOLEAN DEFAULT true
);

-- Site Plan Blocks/Lots Table
CREATE TABLE site_plan_blocks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    site_plan_id UUID NOT NULL REFERENCES site_plan_master(id) ON DELETE CASCADE,
    block_name VARCHAR(10) NOT NULL,
    block_type VARCHAR(50) NOT NULL, -- RESIDENTIAL, COMMERCIAL, MIXED, FACILITY
    total_lots INTEGER NOT NULL DEFAULT 0,
    available_lots INTEGER NOT NULL DEFAULT 0,
    block_area DECIMAL(15,2) NOT NULL,
    description TEXT,
    coordinates JSONB, -- GeoJSON coordinates for block boundary
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(site_plan_id, block_name)
);

-- Site Plan Units/Lots Detail Table
CREATE TABLE site_plan_units (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    site_plan_id UUID NOT NULL REFERENCES site_plan_master(id) ON DELETE CASCADE,
    block_id UUID NOT NULL REFERENCES site_plan_blocks(id) ON DELETE CASCADE,
    unit_number VARCHAR(10) NOT NULL,
    unit_type VARCHAR(50) NOT NULL, -- TYPE_21, TYPE_36, TYPE_45, TYPE_70, COMMERCIAL, etc.
    unit_category VARCHAR(50) NOT NULL, -- RESIDENTIAL, COMMERCIAL, FACILITY
    land_area DECIMAL(15,2) NOT NULL, -- Land area in square meters
    building_area DECIMAL(15,2) NOT NULL, -- Building area in square meters
    price_per_meter DECIMAL(15,2) NOT NULL,
    total_price DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, BOOKED, SOLD, RESERVED
    orientation VARCHAR(20), -- NORTH, SOUTH, EAST, WEST, NORTHEAST, etc.
    floor_level INTEGER,
    rooms INTEGER DEFAULT 0,
    bedrooms INTEGER DEFAULT 0,
    bathrooms INTEGER DEFAULT 0,
    car_parking INTEGER DEFAULT 0,
    specifications JSONB, -- Unit specifications in JSON format
    coordinates JSONB, -- GeoJSON coordinates for unit location
    polygon_points JSONB, -- Polygon points for unit boundary
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(site_plan_id, block_id, unit_number)
);

-- Site Plan Facilities Table
CREATE TABLE site_plan_facilities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    site_plan_id UUID NOT NULL REFERENCES site_plan_master(id) ON DELETE CASCADE,
    facility_name VARCHAR(255) NOT NULL,
    facility_type VARCHAR(50) NOT NULL, -- PARKING, GARDEN, PLAYGROUND, SECURITY, etc.
    facility_category VARCHAR(50) NOT NULL, -- PUBLIC, PRIVATE, COMMERCIAL
    area DECIMAL(15,2) NOT NULL,
    capacity INTEGER,
    description TEXT,
    coordinates JSONB, -- GeoJSON coordinates for facility location
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Site Plan Infrastructure Table
CREATE TABLE site_plan_infrastructure (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    site_plan_id UUID NOT NULL REFERENCES site_plan_master(id) ON DELETE CASCADE,
    infrastructure_type VARCHAR(50) NOT NULL, -- ROAD, DRAINAGE, ELECTRICITY, WATER, etc.
    infrastructure_name VARCHAR(255) NOT NULL,
    specifications JSONB, -- Infrastructure specifications
    coordinates JSONB, -- GeoJSON coordinates for infrastructure
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED', -- PLANNED, IN_PROGRESS, COMPLETED
    completion_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Site Plan Images/Media Table
CREATE TABLE site_plan_media (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    site_plan_id UUID NOT NULL REFERENCES site_plan_master(id) ON DELETE CASCADE,
    media_type VARCHAR(50) NOT NULL, -- MASTER_PLAN, SITE_MAP, 3D_RENDER, PHOTO, VIDEO
    media_url TEXT NOT NULL,
    media_title VARCHAR(255),
    media_description TEXT,
    file_size BIGINT,
    is_primary BOOLEAN DEFAULT false,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Site Plan Documents Table
CREATE TABLE site_plan_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    site_plan_id UUID NOT NULL REFERENCES site_plan_master(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL, -- PERMIT, LICENSE, CERTIFICATE, AGREEMENT
    document_name VARCHAR(255) NOT NULL,
    document_url TEXT NOT NULL,
    document_number VARCHAR(100),
    issue_date DATE,
    expiry_date DATE,
    issuing_authority VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, REVOKED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Site Plan Pricing Table
CREATE TABLE site_plan_pricing (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    site_plan_id UUID NOT NULL REFERENCES site_plan_master(id) ON DELETE CASCADE,
    unit_type VARCHAR(50) NOT NULL,
    base_price DECIMAL(15,2) NOT NULL,
    price_per_meter DECIMAL(15,2) NOT NULL,
    minimum_dp_percentage DECIMAL(5,2) NOT NULL DEFAULT 20.0,
    installment_terms JSONB, -- Available installment terms
    discounts JSONB, -- Available discounts and promotions
    price_history JSONB, -- Historical price changes
    effective_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date DATE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Site Plan Availability Table
CREATE TABLE site_plan_availability (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    site_plan_id UUID NOT NULL REFERENCES site_plan_master(id) ON DELETE CASCADE,
    block_id UUID REFERENCES site_plan_blocks(id) ON DELETE CASCADE,
    unit_id UUID REFERENCES site_plan_units(id) ON DELETE CASCADE,
    availability_date DATE NOT NULL DEFAULT CURRENT_DATE,
    total_units INTEGER NOT NULL DEFAULT 0,
    available_units INTEGER NOT NULL DEFAULT 0,
    booked_units INTEGER NOT NULL DEFAULT 0,
    sold_units INTEGER NOT NULL DEFAULT 0,
    reserved_units INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(site_plan_id, availability_date)
);

-- =====================================================
-- VIEWS FOR SITE PLAN MASTER
-- =====================================================

-- Comprehensive Site Plan View
CREATE VIEW site_plan_comprehensive AS
SELECT 
    spm.id,
    spm.project_name,
    spm.project_code,
    spm.developer_name,
    spm.location_address,
    spm.city,
    spm.province,
    spm.total_area,
    spm.total_units,
    COUNT(DISTINCT su.id) as actual_units,
    COUNT(DISTINCT CASE WHEN su.status = 'AVAILABLE' THEN su.id END) as available_units,
    COUNT(DISTINCT CASE WHEN su.status = 'BOOKED' THEN su.id END) as booked_units,
    COUNT(DISTINCT CASE WHEN su.status = 'SOLD' THEN su.id END) as sold_units,
    COUNT(DISTINCT CASE WHEN su.status = 'RESERVED' THEN su.id END) as reserved_units,
    spm.project_status,
    spm.start_date,
    spm.completion_date,
    spm.description,
    spm.created_at,
    spm.updated_at,
    spm.is_active
FROM site_plan_master spm
LEFT JOIN site_plan_blocks spb ON spm.id = spb.site_plan_id
LEFT JOIN site_plan_units su ON spb.id = su.block_id
WHERE spm.is_active = true
GROUP BY spm.id, spm.project_name, spm.project_code, spm.developer_name, 
         spm.location_address, spm.city, spm.province, spm.total_area, spm.total_units,
         spm.project_status, spm.start_date, spm.completion_date, spm.description,
         spm.created_at, spm.updated_at, spm.is_active;

-- Unit Availability View
CREATE VIEW unit_availability_view AS
SELECT 
    spm.project_name,
    spm.project_code,
    spb.block_name,
    su.unit_number,
    su.unit_type,
    su.unit_category,
    su.land_area,
    su.building_area,
    su.total_price,
    su.status,
    su.orientation,
    su.floor_level,
    su.bedrooms,
    su.bathrooms,
    su.car_parking,
    sp.coordinates as unit_coordinates,
    su.created_at,
    su.updated_at
FROM site_plan_master spm
JOIN site_plan_blocks spb ON spm.id = spb.site_plan_id
JOIN site_plan_units su ON spb.id = su.block_id
WHERE spm.is_active = true
ORDER BY spm.project_name, spb.block_name, su.unit_number;

-- Block Summary View
CREATE VIEW block_summary_view AS
SELECT 
    spm.project_name,
    spm.project_code,
    spb.block_name,
    spb.block_type,
    spb.total_lots,
    COUNT(DISTINCT su.id) as actual_units,
    COUNT(DISTINCT CASE WHEN su.status = 'AVAILABLE' THEN su.id END) as available_units,
    COUNT(DISTINCT CASE WHEN su.status = 'BOOKED' THEN su.id END) as booked_units,
    COUNT(DISTINCT CASE WHEN su.status = 'SOLD' THEN su.id END) as sold_units,
    COUNT(DISTINCT CASE WHEN su.status = 'RESERVED' THEN su.id END) as reserved_units,
    ROUND(AVG(su.price_per_meter), 2) as avg_price_per_meter,
    MIN(su.price_per_meter) as min_price_per_meter,
    MAX(su.price_per_meter) as max_price_per_meter,
    spb.block_area,
    spb.coordinates as block_coordinates
FROM site_plan_master spm
JOIN site_plan_blocks spb ON spm.id = spb.site_plan_id
LEFT JOIN site_plan_units su ON spb.id = su.block_id
WHERE spm.is_active = true
GROUP BY spm.project_name, spm.project_code, spb.block_name, spb.block_type, 
         spb.total_lots, spb.block_area, spb.coordinates
ORDER BY spm.project_name, spb.block_name;

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Site Plan Master Indexes
CREATE INDEX idx_site_plan_master_project_code ON site_plan_master(project_code);
CREATE INDEX idx_site_plan_master_status ON site_plan_master(project_status);
CREATE INDEX idx_site_plan_master_city ON site_plan_master(city);
CREATE INDEX idx_site_plan_master_active ON site_plan_master(is_active);
CREATE INDEX idx_site_plan_master_created_at ON site_plan_master(created_at);

-- Site Plan Blocks Indexes
CREATE INDEX idx_site_plan_blocks_site_plan_id ON site_plan_blocks(site_plan_id);
CREATE INDEX idx_site_plan_blocks_block_name ON site_plan_blocks(block_name);
CREATE INDEX idx_site_plan_blocks_block_type ON site_plan_blocks(block_type);

-- Site Plan Units Indexes
CREATE INDEX idx_site_plan_units_site_plan_id ON site_plan_units(site_plan_id);
CREATE INDEX idx_site_plan_units_block_id ON site_plan_units(block_id);
CREATE INDEX idx_site_plan_units_unit_number ON site_plan_units(unit_number);
CREATE INDEX idx_site_plan_units_unit_type ON site_plan_units(unit_type);
CREATE INDEX idx_site_plan_units_status ON site_plan_units(status);
CREATE INDEX idx_site_plan_units_price ON site_plan_units(total_price);
CREATE INDEX idx_site_plan_units_coordinates ON site_plan_units USING GIN(coordinates);

-- Site Plan Facilities Indexes
CREATE INDEX idx_site_plan_facilities_site_plan_id ON site_plan_facilities(site_plan_id);
CREATE INDEX idx_site_plan_facilities_facility_type ON site_plan_facilities(facility_type);
CREATE INDEX idx_site_plan_facilities_coordinates ON site_plan_facilities USING GIN(coordinates);

-- Site Plan Media Indexes
CREATE INDEX idx_site_plan_media_site_plan_id ON site_plan_media(site_plan_id);
CREATE INDEX idx_site_plan_media_media_type ON site_plan_media(media_type);
CREATE INDEX idx_site_plan_media_primary ON site_plan_media(is_primary);
CREATE INDEX idx_site_plan_media_sort_order ON site_plan_media(sort_order);

-- Site Plan Documents Indexes
CREATE INDEX idx_site_plan_documents_site_plan_id ON site_plan_documents(site_plan_id);
CREATE INDEX idx_site_plan_documents_document_type ON site_plan_documents(document_type);
CREATE INDEX idx_site_plan_documents_status ON site_plan_documents(status);
CREATE INDEX idx_site_plan_documents_expiry_date ON site_plan_documents(expiry_date);

-- Site Plan Pricing Indexes
CREATE INDEX idx_site_plan_pricing_site_plan_id ON site_plan_pricing(site_plan_id);
CREATE INDEX idx_site_plan_pricing_unit_type ON site_plan_pricing(unit_type);
CREATE INDEX idx_site_plan_pricing_effective_date ON site_plan_pricing(effective_date);
CREATE INDEX idx_site_plan_pricing_active ON site_plan_pricing(is_active);

-- Site Plan Availability Indexes
CREATE INDEX idx_site_plan_availability_site_plan_id ON site_plan_availability(site_plan_id);
CREATE INDEX idx_site_plan_availability_date ON site_plan_availability(availability_date);

-- =====================================================
-- TRIGGERS FOR AUTOMATIC UPDATES
-- =====================================================

-- Update total units count when units are added/removed
CREATE OR REPLACE FUNCTION update_site_plan_units_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE site_plan_master spm
    SET total_units = (
        SELECT COUNT(*)
        FROM site_plan_blocks spb
        JOIN site_plan_units su ON spb.id = su.block_id
        WHERE spb.site_plan_id = spm.id
    ),
    updated_at = NOW()
    WHERE spm.id = NEW.site_plan_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_site_plan_units_count
    AFTER INSERT OR UPDATE OR DELETE ON site_plan_units
    FOR EACH ROW EXECUTE FUNCTION update_site_plan_units_count();

-- Update block units count when units are added/removed
CREATE OR REPLACE FUNCTION update_block_units_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE site_plan_blocks spb
    SET total_lots = (
        SELECT COUNT(*)
        FROM site_plan_units su
        WHERE su.block_id = spb.id
    ),
    available_lots = (
        SELECT COUNT(*)
        FROM site_plan_units su
        WHERE su.block_id = spb.id AND su.status = 'AVAILABLE'
    ),
    updated_at = NOW()
    WHERE spb.id = NEW.block_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_block_units_count
    AFTER INSERT OR UPDATE OR DELETE ON site_plan_units
    FOR EACH ROW EXECUTE FUNCTION update_block_units_count();

-- =====================================================
-- SAMPLE DATA INSERTION
-- =====================================================

-- Insert sample site plan master
INSERT INTO site_plan_master (
    project_name, project_code, developer_name, location_address, city, province,
    postal_code, total_area, total_units, project_status, start_date, completion_date,
    description, created_by
) VALUES (
    'KPRFlow Garden City', 'KPR-GC-001', 'PT. KPRFlow Developer',
    'Jl. Garden City No. 123, Jakarta Selatan', 'Jakarta Selatan', 'DKI Jakarta',
    '12345', 50000.00, 500, 'DEVELOPMENT', '2024-01-01', '2026-12-31',
    'Premium residential complex with modern facilities and green environment',
    (SELECT id FROM user_profiles WHERE role = 'BOD' LIMIT 1)
);

-- Insert sample blocks
INSERT INTO site_plan_blocks (
    site_plan_id, block_name, block_type, total_lots, available_lots, block_area,
    description, coordinates
) SELECT 
    spm.id, 'A', 'RESIDENTIAL', 100, 85, 10000.00,
    'Block A - Residential units with garden view',
    '{"type": "Polygon", "coordinates": [[[...]]]}'
FROM site_plan_master spm
WHERE spm.project_code = 'KPR-GC-001';

-- Insert sample units
INSERT INTO site_plan_units (
    site_plan_id, block_id, unit_number, unit_type, unit_category, land_area, building_area,
    price_per_meter, total_price, status, orientation, floor_level, bedrooms, bathrooms,
    car_parking, coordinates, polygon_points
) SELECT 
    spm.id, spb.id, 'A-001', 'TYPE_36', 'RESIDENTIAL', 120.00, 90.00,
    15000000.00, 1350000000.00, 'AVAILABLE', 'NORTH', 1, 2, 1, 1,
    '{"type": "Point", "coordinates": [106.8225, -6.2088]}',
    '{"type": "Polygon", "coordinates": [[[...]]]}'
FROM site_plan_master spm
JOIN site_plan_blocks spb ON spm.id = spb.site_plan_id
WHERE spm.project_code = 'KPR-GC-001' AND spb.block_name = 'A'
LIMIT 1;

-- =====================================================
-- RLS POLICIES FOR SITE PLAN MASTER
-- =====================================================

-- Enable RLS on all site plan tables
ALTER TABLE site_plan_master ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_blocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_units ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_facilities ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_infrastructure ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_pricing ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_availability ENABLE ROW LEVEL SECURITY;

-- Site Plan Master RLS Policies
CREATE POLICY "All authenticated users can view active site plans" ON site_plan_master
    FOR SELECT USING (is_active = true);

CREATE POLICY "Marketing and ESTATE can manage site plans" ON site_plan_master
    FOR ALL USING (
        auth.jwt() ->> 'role' IN ('MARKETING', 'ESTATE', 'BOD')
    );

-- Site Plan Units RLS Policies
CREATE POLICY "All authenticated users can view available units" ON site_plan_units
    FOR SELECT USING (status = 'AVAILABLE');

CREATE POLICY "Marketing and ESTATE can manage units" ON site_plan_units
    FOR ALL USING (
        auth.jwt() ->> 'role' IN ('MARKETING', 'ESTATE', 'BOD')
    );

-- Site Plan Media RLS Policies
CREATE POLICY "All authenticated users can view site plan media" ON site_plan_media
    FOR SELECT USING (true);

CREATE POLICY "Marketing and ESTATE can manage media" ON site_plan_media
    FOR ALL USING (
        auth.jwt() ->> 'role' IN ('MARKETING', 'ESTATE', 'BOD')
    );

-- =====================================================
-- API FUNCTIONS FOR SITE PLAN MASTER
-- =====================================================

-- Function to get available units by project
CREATE OR REPLACE FUNCTION get_available_units_by_project(p_project_code VARCHAR)
RETURNS TABLE(
    unit_id UUID,
    unit_number VARCHAR,
    unit_type VARCHAR,
    unit_category VARCHAR,
    land_area DECIMAL,
    building_area DECIMAL,
    total_price DECIMAL,
    status VARCHAR,
    coordinates JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        su.id,
        su.unit_number,
        su.unit_type,
        su.unit_category,
        su.land_area,
        su.building_area,
        su.total_price,
        su.status,
        su.coordinates
    FROM site_plan_units su
    JOIN site_plan_blocks spb ON su.block_id = spb.id
    JOIN site_plan_master spm ON spb.site_plan_id = spm.id
    WHERE spm.project_code = p_project_code
    AND su.status = 'AVAILABLE'
    AND spm.is_active = true
    ORDER BY spb.block_name, su.unit_number;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get project statistics
CREATE OR REPLACE FUNCTION get_project_statistics(p_project_code VARCHAR)
RETURNS TABLE(
    total_units INTEGER,
    available_units INTEGER,
    booked_units INTEGER,
    sold_units INTEGER,
    reserved_units INTEGER,
    avg_price_per_meter DECIMAL,
    min_price DECIMAL,
    max_price DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_units,
        COUNT(*) FILTER (WHERE status = 'AVAILABLE') as available_units,
        COUNT(*) FILTER (WHERE status = 'BOOKED') as booked_units,
        COUNT(*) FILTER (WHERE status = 'SOLD') as sold_units,
        COUNT(*) FILTER (WHERE status = 'RESERVED') as reserved_units,
        ROUND(AVG(price_per_meter), 2) as avg_price_per_meter,
        MIN(total_price) as min_price,
        MAX(total_price) as max_price
    FROM site_plan_units su
    JOIN site_plan_blocks spb ON su.block_id = spb.id
    JOIN site_plan_master spm ON spb.site_plan_id = spm.id
    WHERE spm.project_code = p_project_code
    AND spm.is_active = true;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to search units by criteria
CREATE OR REPLACE FUNCTION search_units(
    p_project_code VARCHAR DEFAULT NULL,
    p_unit_type VARCHAR DEFAULT NULL,
    p_min_price DECIMAL DEFAULT NULL,
    p_max_price DECIMAL DEFAULT NULL,
    p_bedrooms INTEGER DEFAULT NULL,
    p_status VARCHAR DEFAULT 'AVAILABLE'
)
RETURNS TABLE(
    unit_id UUID,
    project_name VARCHAR,
    block_name VARCHAR,
    unit_number VARCHAR,
    unit_type VARCHAR,
    unit_category VARCHAR,
    land_area DECIMAL,
    building_area DECIMAL,
    total_price DECIMAL,
    status VARCHAR,
    bedrooms INTEGER,
    bathrooms INTEGER,
    coordinates JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        su.id,
        spm.project_name,
        spb.block_name,
        su.unit_number,
        su.unit_type,
        su.unit_category,
        su.land_area,
        su.building_area,
        su.total_price,
        su.status,
        su.bedrooms,
        su.bathrooms,
        su.coordinates
    FROM site_plan_units su
    JOIN site_plan_blocks spb ON su.block_id = spb.id
    JOIN site_plan_master spm ON spb.site_plan_id = spm.id
    WHERE 
        spm.is_active = true
        AND (p_project_code IS NULL OR spm.project_code = p_project_code)
        AND (p_unit_type IS NULL OR su.unit_type = p_unit_type)
        AND (p_min_price IS NULL OR su.total_price >= p_min_price)
        AND (p_max_price IS NULL OR su.total_price <= p_max_price)
        AND (p_bedrooms IS NULL OR su.bedrooms = p_bedrooms)
        AND su.status = p_status
    ORDER BY spm.project_name, spb.block_name, su.unit_number;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
