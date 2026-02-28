-- Multi-Site Plan Master Implementation
-- Support for multiple site plan selection and management
-- Enhanced database schema for multiple projects

-- =====================================================
-- ENHANCED MULTI-SITE PLAN MASTER TABLES
-- =====================================================

-- Master Site Plan Registry (for managing multiple projects)
CREATE TABLE site_plan_registry (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL, -- For multi-tenant support
    project_name VARCHAR(255) NOT NULL,
    project_code VARCHAR(50) NOT NULL,
    developer_name VARCHAR(255) NOT NULL,
    location_address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    province VARCHAR(100) NOT NULL,
    country VARCHAR(50) DEFAULT 'Indonesia',
    postal_code VARCHAR(10),
    total_area DECIMAL(15,2) NOT NULL, -- Total area in square meters
    total_units INTEGER NOT NULL DEFAULT 0,
    project_status VARCHAR(50) NOT NULL DEFAULT 'PLANNING', -- PLANNING, DEVELOPMENT, COMPLETED, SUSPENDED
    start_date DATE,
    completion_date DATE,
    description TEXT,
    website_url TEXT,
    contact_phone VARCHAR(20),
    contact_email VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    is_featured BOOLEAN DEFAULT false, -- For highlighting premium projects
    sort_order INTEGER DEFAULT 0, -- For custom ordering
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    updated_by UUID REFERENCES user_profiles(id),
    UNIQUE(organization_id, project_code),
    CONSTRAINT chk_project_status CHECK (project_status IN ('PLANNING', 'DEVELOPMENT', 'COMPLETED', 'SUSPENDED', 'CANCELLED'))
);

-- Site Plan Categories (for grouping similar projects)
CREATE TABLE site_plan_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(20) UNIQUE NOT NULL,
    description TEXT,
    icon_url TEXT,
    color_code VARCHAR(7) DEFAULT '#1976D2', -- Hex color
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Site Plan Category Relations (many-to-many)
CREATE TABLE site_plan_category_relations (
    site_plan_id UUID NOT NULL REFERENCES site_plan_registry(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES site_plan_categories(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (site_plan_id, category_id)
);

-- Enhanced Site Plan Master (linked to registry)
CREATE TABLE site_plan_master (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    registry_id UUID NOT NULL REFERENCES site_plan_registry(id) ON DELETE CASCADE,
    phase_name VARCHAR(100), -- For multi-phase projects
    phase_number INTEGER,
    total_phases INTEGER DEFAULT 1,
    project_type VARCHAR(50) NOT NULL, -- RESIDENTIAL, COMMERCIAL, MIXED, INDUSTRIAL
    investment_type VARCHAR(50), -- NEW_BUILD, RENOVATION, EXPANSION
    target_market VARCHAR(100), -- FIRST_TIME, UPGRADING, INVESTORS
    price_range_min DECIMAL(15,2),
    price_range_max DECIMAL(15,2),
    unit_types JSONB, -- Array of available unit types
    amenities JSONB, -- Array of available amenities
    accessibility_features JSONB, -- Accessibility information
    transportation JSONB, -- Transportation options
    nearby_facilities JSONB, -- Nearby facilities (schools, hospitals, etc.)
    certification JSONB, -- Building certifications (green building, etc.)
    marketing_brochure_url TEXT,
    virtual_tour_url TEXT,
    construction_progress DECIMAL(3,2) DEFAULT 0.00, -- Percentage complete
    expected_handover_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    updated_by UUID REFERENCES user_profiles(id)
);

-- Site Plan Comparison Matrix (for comparing multiple projects)
CREATE TABLE site_plan_comparison_matrix (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    site_plan_ids UUID[] NOT NULL, -- Array of site plan IDs to compare
    comparison_name VARCHAR(255),
    comparison_criteria JSONB, -- Comparison criteria and weights
    comparison_result JSONB, -- Comparison scores and results
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- User Favorite Site Plans
CREATE TABLE user_favorite_site_plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    site_plan_id UUID NOT NULL REFERENCES site_plan_registry(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, site_plan_id)
);

-- Site Plan Viewing History
CREATE TABLE site_plan_viewing_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    site_plan_id UUID NOT NULL REFERENCES site_plan_registry(id) ON DELETE CASCADE,
    view_count INTEGER DEFAULT 1,
    last_viewed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    total_time_spent INTEGER DEFAULT 0, -- Total time in seconds
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, site_plan_id)
);

-- =====================================================
-- ENHANCED VIEWS FOR MULTI-SITE PLAN MANAGEMENT
-- =====================================================

-- Comprehensive Site Plan Registry View
CREATE VIEW site_plan_registry_comprehensive AS
SELECT 
    spr.id,
    spr.project_name,
    spr.project_code,
    spr.developer_name,
    spr.location_address,
    spr.city,
    spr.province,
    spr.total_area,
    spr.total_units,
    COUNT(DISTINCT spm.id) as total_phases,
    COUNT(DISTINCT CASE WHEN su.status = 'AVAILABLE' THEN su.id END) as available_units,
    COUNT(DISTINCT CASE WHEN su.status = 'BOOKED' THEN su.id END) as booked_units,
    COUNT(DISTINCT CASE WHEN su.status = 'SOLD' THEN su.id END) as sold_units,
    COUNT(DISTINCT CASE WHEN su.status = 'RESERVED' THEN su.id END) as reserved_units,
    spr.project_status,
    spr.start_date,
    spr.completion_date,
    spr.is_featured,
    spr.sort_order,
    spr.description,
    spr.website_url,
    spr.contact_phone,
    spr.contact_email,
    spr.created_at,
    spr.updated_at,
    COALESCE(AVG(spm.price_range_min), 0) as avg_min_price,
    COALESCE(AVG(spm.price_range_max), 0) as avg_max_price,
    STRING_AGG(DISTINCT sc.category_name, ', ') as categories
FROM site_plan_registry spr
LEFT JOIN site_plan_master spm ON spr.id = spm.registry_id
LEFT JOIN site_plan_blocks spb ON spm.id = spb.site_plan_id
LEFT JOIN site_plan_units su ON spb.id = su.block_id
LEFT JOIN site_plan_category_relations spcr ON spr.id = spcr.site_plan_id
LEFT JOIN site_plan_categories sc ON spcr.category_id = sc.id
WHERE spr.is_active = true
GROUP BY spr.id, spr.project_name, spr.project_code, spr.developer_name, 
         spr.location_address, spr.city, spr.province, spr.total_area, spr.total_units,
         spr.project_status, spr.start_date, spr.completion_date, spr.is_featured,
         spr.sort_order, spr.description, spr.website_url, spr.contact_phone,
         spr.contact_email, spr.created_at, spr.updated_at
ORDER BY spr.is_featured DESC, spr.sort_order ASC, spr.project_name ASC;

-- Site Plan Comparison View
CREATE VIEW site_plan_comparison_view AS
SELECT 
    spr.id as site_plan_id,
    spr.project_name,
    spr.project_code,
    spr.city,
    spr.province,
    spr.total_units,
    COUNT(DISTINCT CASE WHEN su.status = 'AVAILABLE' THEN su.id END) as available_units,
    COUNT(DISTINCT CASE WHEN su.status = 'BOOKED' THEN su.id END) as booked_units,
    COUNT(DISTINCT CASE WHEN su.status = 'SOLD' THEN su.id END) as sold_units,
    COALESCE(AVG(su.total_price), 0) as avg_unit_price,
    MIN(su.total_price) as min_unit_price,
    MAX(su.total_price) as max_unit_price,
    COUNT(DISTINCT spb.block_name) as total_blocks,
    spr.project_status,
    spr.is_featured,
    STRING_AGG(DISTINCT sc.category_name, ', ') as categories
FROM site_plan_registry spr
LEFT JOIN site_plan_master spm ON spr.id = spm.registry_id
LEFT JOIN site_plan_blocks spb ON spm.id = spb.site_plan_id
LEFT JOIN site_plan_units su ON spb.id = su.block_id
LEFT JOIN site_plan_category_relations spcr ON spr.id = spcr.site_plan_id
LEFT JOIN site_plan_categories sc ON spcr.category_id = sc.id
WHERE spr.is_active = true
GROUP BY spr.id, spr.project_name, spr.project_code, spr.city, spr.province,
         spr.total_units, spr.project_status, spr.is_featured
ORDER BY spr.is_featured DESC, avg_unit_price ASC;

-- User Personalized Site Plan View
CREATE VIEW user_personalized_site_plans AS
SELECT 
    spr.id,
    spr.project_name,
    spr.project_code,
    spr.city,
    spr.province,
    spr.total_units,
    COUNT(DISTINCT CASE WHEN su.status = 'AVAILABLE' THEN su.id END) as available_units,
    COUNT(DISTINCT CASE WHEN su.status = 'BOOKED' THEN su.id END) as booked_units,
    COUNT(DISTINCT CASE WHEN su.status = 'SOLD' THEN su.id END) as sold_units,
    COALESCE(AVG(su.total_price), 0) as avg_unit_price,
    spr.project_status,
    spr.is_featured,
    CASE WHEN ufav.site_plan_id IS NOT NULL THEN true ELSE false END as is_favorite,
    COALESCE(vh.view_count, 0) as view_count,
    COALESCE(vh.last_viewed_at, spr.created_at) as last_interaction,
    STRING_AGG(DISTINCT sc.category_name, ', ') as categories
FROM site_plan_registry spr
LEFT JOIN site_plan_master spm ON spr.id = spm.registry_id
LEFT JOIN site_plan_blocks spb ON spm.id = spb.site_plan_id
LEFT JOIN site_plan_units su ON spb.id = su.block_id
LEFT JOIN site_plan_category_relations spcr ON spr.id = spcr.site_plan_id
LEFT JOIN site_plan_categories sc ON spcr.category_id = sc.id
LEFT JOIN user_favorite_site_plans ufav ON spr.id = ufav.site_plan_id
LEFT JOIN site_plan_viewing_history vh ON spr.id = vh.site_plan_id
WHERE spr.is_active = true
GROUP BY spr.id, spr.project_name, spr.project_code, spr.city, spr.province,
         spr.total_units, spr.project_status, spr.is_featured, ufav.site_plan_id,
         vh.view_count, vh.last_viewed_at
ORDER BY spr.is_featured DESC, is_favorite DESC, last_interaction DESC;

-- =====================================================
-- ENHANCED INDEXES FOR MULTI-SITE PLAN PERFORMANCE
-- =====================================================

-- Site Plan Registry Indexes
CREATE INDEX idx_site_plan_registry_organization_id ON site_plan_registry(organization_id);
CREATE INDEX idx_site_plan_registry_project_code ON site_plan_registry(project_code);
CREATE INDEX idx_site_plan_registry_city_province ON site_plan_registry(city, province);
CREATE INDEX idx_site_plan_registry_status ON site_plan_registry(project_status);
CREATE INDEX idx_site_plan_registry_featured ON site_plan_registry(is_featured);
CREATE INDEX idx_site_plan_registry_active ON site_plan_registry(is_active);
CREATE INDEX idx_site_plan_registry_sort_order ON site_plan_registry(sort_order);
CREATE INDEX idx_site_plan_registry_created_at ON site_plan_registry(created_at);

-- Site Plan Categories Indexes
CREATE INDEX idx_site_plan_categories_code ON site_plan_categories(category_code);
CREATE INDEX idx_site_plan_categories_active ON site_plan_categories(is_active);
CREATE INDEX idx_site_plan_categories_sort_order ON site_plan_categories(sort_order);

-- Category Relations Indexes
CREATE INDEX idx_site_plan_category_relations_site_plan ON site_plan_category_relations(site_plan_id);
CREATE INDEX idx_site_plan_category_relations_category ON site_plan_category_relations(category_id);

-- Multi-Site Plan Master Indexes
CREATE INDEX idx_site_plan_master_registry_id ON site_plan_master(registry_id);
CREATE INDEX idx_site_plan_master_phase ON site_plan_master(phase_number);
CREATE INDEX idx_site_plan_master_project_type ON site_plan_master(project_type);
CREATE INDEX idx_site_plan_master_price_range ON site_plan_master(price_range_min, price_range_max);

-- User Favorites Indexes
CREATE INDEX idx_user_favorite_site_plans_user_id ON user_favorite_site_plans(user_id);
CREATE INDEX idx_user_favorite_site_plans_site_plan_id ON user_favorite_site_plans(site_plan_id);

-- Viewing History Indexes
CREATE INDEX idx_site_plan_viewing_history_user_id ON site_plan_viewing_history(user_id);
CREATE INDEX idx_site_plan_viewing_history_site_plan_id ON site_plan_viewing_history(site_plan_id);
CREATE INDEX idx_site_plan_viewing_history_last_viewed ON site_plan_viewing_history(last_viewed_at DESC);

-- Comparison Matrix Indexes
CREATE INDEX idx_site_plan_comparison_matrix_user_id ON site_plan_comparison_matrix(user_id);
CREATE INDEX idx_site_plan_comparison_matrix_site_plan_ids ON site_plan_comparison_matrix USING GIN(site_plan_ids);

-- =====================================================
-- ENHANCED FUNCTIONS FOR MULTI-SITE PLAN OPERATIONS
-- =====================================================

-- Function to get available site plans for selection
CREATE OR REPLACE FUNCTION get_available_site_plans(
    p_user_id UUID DEFAULT NULL,
    p_city_filter VARCHAR(100) DEFAULT NULL,
    p_province_filter VARCHAR(100) DEFAULT NULL,
    p_category_filter VARCHAR(100) DEFAULT NULL,
    p_price_min DECIMAL DEFAULT NULL,
    p_price_max DECIMAL DEFAULT NULL,
    p_featured_only BOOLEAN DEFAULT FALSE,
    p_limit INTEGER DEFAULT 50
)
RETURNS TABLE(
    site_plan_id UUID,
    project_name VARCHAR,
    project_code VARCHAR,
    developer_name VARCHAR,
    location_address VARCHAR,
    city VARCHAR,
    province VARCHAR,
    total_units INTEGER,
    available_units INTEGER,
    booked_units INTEGER,
    sold_units INTEGER,
    avg_unit_price DECIMAL,
    min_unit_price DECIMAL,
    max_unit_price DECIMAL,
    project_status VARCHAR,
    is_featured BOOLEAN,
    is_favorite BOOLEAN,
    categories TEXT,
    last_interaction TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        spr.id,
        spr.project_name,
        spr.project_code,
        spr.developer_name,
        spr.location_address,
        spr.city,
        spr.province,
        spr.total_units,
        COUNT(DISTINCT CASE WHEN su.status = 'AVAILABLE' THEN su.id END) as available_units,
        COUNT(DISTINCT CASE WHEN su.status = 'BOOKED' THEN su.id END) as booked_units,
        COUNT(DISTINCT CASE WHEN su.status = 'SOLD' THEN su.id END) as sold_units,
        COALESCE(AVG(su.total_price), 0) as avg_unit_price,
        MIN(su.total_price) as min_unit_price,
        MAX(su.total_price) as max_unit_price,
        spr.project_status,
        spr.is_featured,
        CASE WHEN ufav.site_plan_id IS NOT NULL THEN true ELSE false END as is_favorite,
        STRING_AGG(DISTINCT sc.category_name, ', ') as categories,
        COALESCE(vh.last_viewed_at, spr.created_at) as last_interaction
    FROM site_plan_registry spr
    LEFT JOIN site_plan_master spm ON spr.id = spm.registry_id
    LEFT JOIN site_plan_blocks spb ON spm.id = spb.site_plan_id
    LEFT JOIN site_plan_units su ON spb.id = su.block_id
    LEFT JOIN site_plan_category_relations spcr ON spr.id = spcr.site_plan_id
    LEFT JOIN site_plan_categories sc ON spcr.category_id = sc.id
    LEFT JOIN user_favorite_site_plans ufav ON spr.id = ufav.site_plan_id 
        AND (p_user_id IS NULL OR ufav.user_id = p_user_id)
    LEFT JOIN site_plan_viewing_history vh ON spr.id = vh.site_plan_id 
        AND (p_user_id IS NULL OR vh.user_id = p_user_id)
    WHERE spr.is_active = true
    AND (p_city_filter IS NULL OR spr.city = p_city_filter)
    AND (p_province_filter IS NULL OR spr.province = p_province_filter)
    AND (p_category_filter IS NULL OR sc.category_name = p_category_filter)
    AND (p_price_min IS NULL OR AVG(su.total_price) >= p_price_min)
    AND (p_price_max IS NULL OR AVG(su.total_price) <= p_price_max)
    AND (p_featured_only = false OR spr.is_featured = true)
    GROUP BY spr.id, spr.project_name, spr.project_code, spr.developer_name,
             spr.location_address, spr.city, spr.province, spr.total_units,
             spr.project_status, spr.is_featured, ufav.site_plan_id,
             vh.last_viewed_at, spr.created_at
    ORDER BY 
        spr.is_featured DESC,
        CASE WHEN ufav.site_plan_id IS NOT NULL THEN 1 ELSE 0 END DESC,
        COALESCE(vh.last_viewed_at, spr.created_at) DESC,
        spr.sort_order ASC,
        spr.project_name ASC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to compare multiple site plans
CREATE OR REPLACE FUNCTION compare_site_plans(
    p_site_plan_ids UUID[],
    p_comparison_criteria JSONB DEFAULT '{}',
    p_user_id UUID DEFAULT NULL
)
RETURNS TABLE(
    comparison_id UUID,
    site_plan_id UUID,
    project_name VARCHAR,
    comparison_score DECIMAL,
    detailed_scores JSONB,
    rank_position INTEGER
) AS $$
DECLARE
    v_comparison_id UUID := uuid_generate_v4();
    v_score DECIMAL;
    v_rank INTEGER := 1;
BEGIN
    -- Save comparison to matrix
    INSERT INTO site_plan_comparison_matrix (
        user_id, site_plan_ids, comparison_name, comparison_criteria, created_at
    ) VALUES (
        p_user_id, p_site_plan_ids, 'Custom Comparison', p_comparison_criteria, NOW()
    ) RETURNING id INTO v_comparison_id;
    
    -- Calculate comparison scores for each site plan
    FOR i IN 1..array_length(p_site_plan_ids, 1) LOOP
        -- Calculate score based on criteria (simplified example)
        SELECT 
            (availability_score * 0.3 + price_score * 0.3 + location_score * 0.2 + amenities_score * 0.2) * 100
        INTO v_score
        FROM (
            SELECT 
                CASE 
                    WHEN COUNT(DISTINCT CASE WHEN su.status = 'AVAILABLE' THEN su.id END) > 0 
                    THEN COUNT(DISTINCT CASE WHEN su.status = 'AVAILABLE' THEN su.id END)::DECIMAL / total_units 
                    ELSE 0 
                END as availability_score,
                CASE 
                    WHEN AVG(su.total_price) > 0 
                    THEN 1 - (AVG(su.total_price) - MIN(AVG(su.total_price)) OVER ()) / 
                         (MAX(AVG(su.total_price)) OVER () - MIN(AVG(su.total_price)) OVER ())
                    ELSE 0.5 
                END as price_score,
                0.8 as location_score, -- Simplified location score
                0.7 as amenities_score -- Simplified amenities score
            FROM site_plan_registry spr
            LEFT JOIN site_plan_master spm ON spr.id = spm.registry_id
            LEFT JOIN site_plan_blocks spb ON spm.id = spb.site_plan_id
            LEFT JOIN site_plan_units su ON spb.id = su.block_id
            WHERE spr.id = p_site_plan_ids[i]
            GROUP BY spr.id, total_units
        ) scoring;
        
        -- Return comparison result
        RETURN QUERY SELECT 
            v_comparison_id,
            p_site_plan_ids[i],
            spr.project_name,
            v_score,
            json_build_object(
                'availability', availability_score,
                'price', price_score,
                'location', location_score,
                'amenities', amenities_score
            ) as detailed_scores,
            v_rank
        FROM site_plan_registry spr
        WHERE spr.id = p_site_plan_ids[i];
        
        v_rank := v_rank + 1;
    END LOOP;
    
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get user's site plan preferences
CREATE OR REPLACE FUNCTION get_user_site_plan_preferences(p_user_id UUID)
RETURNS TABLE(
    preferred_cities TEXT[],
    preferred_provinces TEXT[],
    preferred_categories TEXT[],
    price_range_min DECIMAL,
    price_range_max DECIMAL,
    preferred_unit_types TEXT[],
    frequently_viewed_site_plans UUID[]
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        ARRAY_AGG(DISTINCT spr.city) FILTER (WHERE view_count >= 3),
        ARRAY_AGG(DISTINCT spr.province) FILTER (WHERE view_count >= 3),
        ARRAY_AGG(DISTINCT sc.category_name) FILTER (WHERE view_count >= 3),
        COALESCE(AVG(su.total_price) * 0.8, 0) as price_range_min,
        COALESCE(AVG(su.total_price) * 1.2, 999999999) as price_range_max,
        ARRAY_AGG(DISTINCT su.unit_type) FILTER (WHERE view_count >= 3),
        ARRAY_AGG(site_plan_id) FILTER (WHERE view_count >= 5)
    FROM site_plan_viewing_history vh
    JOIN site_plan_registry spr ON vh.site_plan_id = spr.id
    LEFT JOIN site_plan_master spm ON spr.id = spm.registry_id
    LEFT JOIN site_plan_blocks spb ON spm.id = spb.site_plan_id
    LEFT JOIN site_plan_units su ON spb.id = su.block_id
    LEFT JOIN site_plan_category_relations spcr ON spr.id = spcr.site_plan_id
    LEFT JOIN site_plan_categories sc ON spcr.category_id = sc.id
    WHERE vh.user_id = p_user_id
    AND vh.view_count >= 3
    GROUP BY vh.user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- SAMPLE DATA FOR MULTI-SITE PLAN MASTER
-- =====================================================

-- Insert site plan categories
INSERT INTO site_plan_categories (category_name, category_code, description, color_code) VALUES
('Residential', 'RESIDENTIAL', 'Residential properties and housing', '#4CAF50'),
('Commercial', 'COMMERCIAL', 'Commercial and business properties', '#2196F3'),
('Mixed Use', 'MIXED_USE', 'Mixed residential and commercial', '#FF9800'),
('Luxury', 'LUXURY', 'Premium and luxury properties', '#9C27B0'),
('Affordable', 'AFFORDABLE', 'Affordable housing options', '#00BCD4');

-- Insert sample site plans (3 different projects)
INSERT INTO site_plan_registry (
    project_name, project_code, developer_name, location_address, city, province,
    postal_code, total_area, total_units, project_status, start_date, completion_date,
    description, website_url, contact_phone, contact_email, is_featured, sort_order
) VALUES 
(
    'KPRFlow Garden City', 'KPR-GC-001', 'PT. KPRFlow Developer',
    'Jl. Garden City No. 123, Jakarta Selatan', 'Jakarta Selatan', 'DKI Jakarta',
    '12345', 50000.00, 500, 'DEVELOPMENT', '2024-01-01', '2026-12-31',
    'Premium residential complex with modern facilities and green environment',
    'https://kprflow-gardencity.com', '021-1234567', 'info@kprflow-gardencity.com',
    true, 1
),
(
    'KPRFlow Business Park', 'KPR-BP-002', 'PT. KPRFlow Commercial',
    'Jl. Business Park No. 456, Jakarta Pusat', 'Jakarta Pusat', 'DKI Jakarta',
    '10110', 30000.00, 200, 'COMPLETED', '2023-01-01', '2024-06-30',
    'Modern commercial complex with office spaces and retail areas',
    'https://kprflow-businesspark.com', '021-9876543', 'sales@kprflow-businesspark.com',
    true, 2
),
(
    'KPRFlow Riverside', 'KPR-RS-003', 'PT. KPRFlow Properties',
    'Jl. Riverside No. 789, Tangerang', 'Tangerang', 'Banten',
    '15111', 40000.00, 350, 'DEVELOPMENT', '2024-03-01', '2027-03-31',
    'Waterfront residential community with recreational facilities',
    'https://kprflow-riverside.com', '021-5551234', 'contact@kprflow-riverside.com',
    false, 3
);

-- Assign categories to site plans
INSERT INTO site_plan_category_relations (site_plan_id, category_id)
SELECT 
    spr.id, 
    sc.id
FROM site_plan_registry spr
CROSS JOIN site_plan_categories sc
WHERE spr.project_code IN ('KPR-GC-001', 'KPR-BP-002', 'KPR-RS-003')
AND (
    (spr.project_code = 'KPR-GC-001' AND sc.category_code IN ('RESIDENTIAL', 'LUXURY')) OR
    (spr.project_code = 'KPR-BP-002' AND sc.category_code IN ('COMMERCIAL', 'MIXED_USE')) OR
    (spr.project_code = 'KPR-RS-003' AND sc.category_code IN ('RESIDENTIAL', 'AFFORDABLE'))
);

-- =====================================================
-- RLS POLICIES FOR MULTI-SITE PLAN MASTER
-- =====================================================

-- Enable RLS on all new tables
ALTER TABLE site_plan_registry ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_category_relations ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_master ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_comparison_matrix ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_favorite_site_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE site_plan_viewing_history ENABLE ROW LEVEL SECURITY;

-- Site Plan Registry RLS Policies
CREATE POLICY "All authenticated users can view active site plans" ON site_plan_registry
    FOR SELECT USING (is_active = true);

CREATE POLICY "Marketing and ESTATE can manage site plans" ON site_plan_registry
    FOR ALL USING (
        auth.jwt() ->> 'role' IN ('MARKETING', 'ESTATE', 'BOD')
    );

CREATE POLICY "Users can view their favorite site plans" ON site_plan_registry
    FOR SELECT USING (
        id IN (
            SELECT site_plan_id FROM user_favorite_site_plans 
            WHERE user_id = auth.uid()
        )
    );

-- User Favorites RLS Policies
CREATE POLICY "Users can manage their own favorites" ON user_favorite_site_plans
    FOR ALL USING (user_id = auth.uid());

-- Viewing History RLS Policies
CREATE POLICY "Users can manage their own viewing history" ON site_plan_viewing_history
    FOR ALL USING (user_id = auth.uid());

CREATE POLICY "Users can view their viewing history" ON site_plan_viewing_history
    FOR SELECT USING (user_id = auth.uid());

-- Comparison Matrix RLS Policies
CREATE POLICY "Users can manage their own comparisons" ON site_plan_comparison_matrix
    FOR ALL USING (user_id = auth.uid());

CREATE POLICY "Users can view their own comparisons" ON site_plan_comparison_matrix
    FOR SELECT USING (user_id = auth.uid());
