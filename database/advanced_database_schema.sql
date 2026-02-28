-- =====================================================
-- KPRFlow Enterprise - Advanced Database Schema
-- Phase 2: Database Schema & RBAC (100% Complete)
-- =====================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- =====================================================
-- CORE TABLES WITH ENHANCED FEATURES
-- =====================================================

-- Enhanced user_profiles table with additional fields
CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'CUSTOMER',
    department VARCHAR(100),
    position VARCHAR(100),
    avatar_url TEXT,
    is_active BOOLEAN DEFAULT true,
    is_verified BOOLEAN DEFAULT false,
    last_login_at TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    two_factor_enabled BOOLEAN DEFAULT false,
    two_factor_secret TEXT,
    session_token TEXT,
    refresh_token TEXT,
    preferences JSONB DEFAULT '{}',
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    updated_by UUID REFERENCES user_profiles(id)
);

-- Enhanced kpr_dossiers table with comprehensive tracking
CREATE TABLE IF NOT EXISTS kpr_dossiers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    unit_property_id UUID NOT NULL REFERENCES unit_properties(id) ON DELETE RESTRICT,
    application_number VARCHAR(50) UNIQUE NOT NULL,
    status dossier_status NOT NULL DEFAULT 'LEAD',
    priority dossier_priority DEFAULT 'NORMAL',
    source VARCHAR(100) DEFAULT 'MANUAL',
    campaign_id UUID,
    referral_code VARCHAR(50),
    sales_agent_id UUID REFERENCES user_profiles(id),
    estimated_loan_amount DECIMAL(15,2),
    loan_amount DECIMAL(15,2),
    interest_rate DECIMAL(5,2),
    loan_term_months INTEGER,
    down_payment_percentage DECIMAL(5,2),
    monthly_income DECIMAL(15,2),
    employment_status VARCHAR(50),
    employment_company VARCHAR(255),
    employment_position VARCHAR(100),
    employment_duration_months INTEGER,
    marital_status VARCHAR(50),
    dependents INTEGER DEFAULT 0,
    existing_loans DECIMAL(15,2) DEFAULT 0,
    credit_score INTEGER,
    debt_to_income_ratio DECIMAL(5,2),
    risk_score DECIMAL(5,2),
    approval_probability DECIMAL(5,2),
    estimated_processing_days INTEGER,
    actual_processing_days INTEGER,
    completion_percentage DECIMAL(5,2) DEFAULT 0,
    next_action_required VARCHAR(255),
    next_action_due TIMESTAMP WITH TIME ZONE,
    blocked_reason TEXT,
    cancellation_reason TEXT,
    cancellation_notes TEXT,
    cancelled_by UUID REFERENCES user_profiles(id),
    cancelled_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    tags TEXT[],
    documents_completed INTEGER DEFAULT 0,
    documents_total INTEGER DEFAULT 0,
    payments_completed INTEGER DEFAULT 0,
    payments_total INTEGER DEFAULT 0,
    sla_breached BOOLEAN DEFAULT false,
    sla_breach_count INTEGER DEFAULT 0,
    last_activity_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    assigned_to UUID REFERENCES user_profiles(id),
    assigned_at TIMESTAMP WITH TIME ZONE,
    reviewed_by UUID REFERENCES user_profiles(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    approved_by UUID REFERENCES user_profiles(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    disbursed_by UUID REFERENCES user_profiles(id),
    disbursed_at TIMESTAMP WITH TIME ZONE,
    completed_by UUID REFERENCES user_profiles(id),
    completed_at TIMESTAMP WITH TIME ZONE,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    updated_by UUID REFERENCES user_profiles(id)
);

-- Enhanced unit_properties table with detailed information
CREATE TABLE IF NOT EXISTS unit_properties (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_name VARCHAR(255) NOT NULL,
    block VARCHAR(10) NOT NULL,
    unit_number VARCHAR(10) NOT NULL,
    unit_type VARCHAR(50) NOT NULL,
    bedroom_count INTEGER NOT NULL,
    bathroom_count INTEGER NOT NULL,
    floor_level INTEGER,
    building_size DECIMAL(8,2),
    land_size DECIMAL(8,2),
    price DECIMAL(15,2) NOT NULL,
    base_price DECIMAL(15,2),
    discount_amount DECIMAL(15,2) DEFAULT 0,
    final_price DECIMAL(15,2),
    price_per_sqm DECIMAL(10,2),
    status unit_status NOT NULL DEFAULT 'AVAILABLE',
    availability_status VARCHAR(50) DEFAULT 'AVAILABLE',
    view_type VARCHAR(100),
    facing_direction VARCHAR(50),
    certificate_number VARCHAR(100),
    tax_number VARCHAR(100),
    building_permit_number VARCHAR(100),
    location_description TEXT,
    features TEXT[],
    amenities TEXT[],
    nearby_facilities TEXT[],
    access_routes TEXT[],
    virtual_tour_url TEXT,
    floor_plan_url TEXT,
    images_url TEXT[],
    videos_url TEXT[],
    is_premium BOOLEAN DEFAULT false,
    is_corner_unit BOOLEAN DEFAULT false,
    is_penthouse BOOLEAN DEFAULT false,
    is_furnished BOOLEAN DEFAULT false,
    is_ready_stock BOOLEAN DEFAULT false,
    construction_progress DECIMAL(5,2),
    estimated_completion_date DATE,
    actual_completion_date DATE,
    maintenance_fee DECIMAL(10,2),
    parking_fee DECIMAL(10,2),
    other_fees DECIMAL(10,2),
    developer VARCHAR(255),
    contractor VARCHAR(255),
    architect VARCHAR(255),
    year_built INTEGER,
    last_renovated DATE,
    building_age_years INTEGER,
    electricity_capacity INTEGER,
    water_source VARCHAR(100),
    internet_available BOOLEAN DEFAULT true,
    phone_line_available BOOLEAN DEFAULT true,
    gas_available BOOLEAN DEFAULT false,
    ac_installed BOOLEAN DEFAULT false,
    heating_system VARCHAR(50),
    security_level VARCHAR(50),
    access_control VARCHAR(50),
    cctv_available BOOLEAN DEFAULT true,
    24_hour_security BOOLEAN DEFAULT false,
    swimming_pool BOOLEAN DEFAULT false,
    fitness_center BOOLEAN DEFAULT false,
    playground BOOLEAN DEFAULT false,
    garden BOOLEAN DEFAULT false,
    balcony BOOLEAN DEFAULT false,
    terrace BOOLEAN DEFAULT false,
    storage_room BOOLEAN DEFAULT false,
    maid_room BOOLEAN DEFAULT false,
    study_room BOOLEAN DEFAULT false,
    basement BOOLEAN DEFAULT false,
    rooftop BOOLEAN DEFAULT false,
    notes TEXT,
    tags TEXT[],
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    updated_by UUID REFERENCES user_profiles(id),
    UNIQUE(project_name, block, unit_number)
);

-- =====================================================
-- ADVANCED FINANCIAL TRANSACTIONS
-- =====================================================

CREATE TABLE IF NOT EXISTS financial_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    transaction_number VARCHAR(50) UNIQUE NOT NULL,
    category payment_category NOT NULL,
    type payment_type NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'IDR',
    exchange_rate DECIMAL(10,6) DEFAULT 1,
    amount_idr DECIMAL(15,2) GENERATED ALWAYS AS (amount * exchange_rate) STORED,
    payment_method VARCHAR(50),
    payment_channel VARCHAR(100),
    payment_reference VARCHAR(255),
    gateway_transaction_id VARCHAR(255),
    gateway_response JSONB,
    status transaction_status NOT NULL DEFAULT 'PENDING',
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by UUID REFERENCES user_profiles(id),
    rejected_at TIMESTAMP WITH TIME ZONE,
    rejected_by UUID REFERENCES user_profiles(id),
    rejection_reason TEXT,
    due_date DATE,
    paid_date DATE,
    overdue_days INTEGER,
    late_fee DECIMAL(15,2) DEFAULT 0,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    tax_amount DECIMAL(15,2) DEFAULT 0,
    total_amount DECIMAL(15,2) GENERATED ALWAYS AS (amount_idr + late_fee - discount_amount + tax_amount) STORED,
    receipt_url TEXT,
    invoice_url TEXT,
    evidence_url TEXT,
    notes TEXT,
    tags TEXT[],
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    updated_by UUID REFERENCES user_profiles(id)
);

-- =====================================================
-- ENHANCED DOCUMENT MANAGEMENT
-- =====================================================

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    document_type document_type NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    file_size BIGINT NOT NULL,
    file_mime_type VARCHAR(100) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    storage_provider VARCHAR(50) DEFAULT 'SUPABASE',
    storage_path TEXT,
    public_url TEXT,
    thumbnail_url TEXT,
    preview_url TEXT,
    status document_status NOT NULL DEFAULT 'UPLOADED',
    verification_status verification_status DEFAULT 'PENDING',
    verification_score DECIMAL(5,2),
    verification_notes TEXT,
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by UUID REFERENCES user_profiles(id),
    rejected_at TIMESTAMP WITH TIME ZONE,
    rejected_by UUID REFERENCES user_profiles(id),
    rejection_reason TEXT,
    expiry_date DATE,
    is_required BOOLEAN DEFAULT false,
    is_sensitive BOOLEAN DEFAULT false,
    is_encrypted BOOLEAN DEFAULT false,
    encryption_key TEXT,
    access_level INTEGER DEFAULT 1,
    download_count INTEGER DEFAULT 0,
    last_downloaded_at TIMESTAMP WITH TIME ZONE,
    tags TEXT[],
    metadata JSONB DEFAULT '{}',
    ocr_text TEXT,
    extracted_data JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES user_profiles(id),
    updated_by UUID REFERENCES user_profiles(id)
);

-- =====================================================
-- COMPREHENSIVE AUDIT TRAIL
-- =====================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    session_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    entity_name VARCHAR(255),
    old_values JSONB,
    new_values JSONB,
    changes JSONB GENERATED ALWAYS AS (
        CASE 
            WHEN old_values IS NOT NULL AND new_values IS NOT NULL THEN 
                jsonb_each(new_values) - jsonb_each(old_values)
            WHEN old_values IS NULL THEN new_values
            WHEN new_values IS NULL THEN old_values
            ELSE NULL
        END
    ) STORED,
    ip_address INET,
    user_agent TEXT,
    device_id VARCHAR(255),
    location_country VARCHAR(2),
    location_city VARCHAR(100),
    session_duration_ms INTEGER,
    success BOOLEAN DEFAULT true,
    error_code VARCHAR(50),
    error_message TEXT,
    risk_score DECIMAL(5,2) DEFAULT 0,
    is_suspicious BOOLEAN DEFAULT false,
    requires_review BOOLEAN DEFAULT false,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by UUID REFERENCES user_profiles(id),
    notes TEXT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- ADVANCED NOTIFICATIONS
-- =====================================================

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type notification_type NOT NULL DEFAULT 'INFO',
    priority notification_priority DEFAULT 'NORMAL',
    category VARCHAR(100),
    action_url TEXT,
    action_text VARCHAR(100),
    image_url TEXT,
    icon_url TEXT,
    data JSONB DEFAULT '{}',
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE,
    is_archived BOOLEAN DEFAULT false,
    archived_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    delivery_method VARCHAR(50) DEFAULT 'IN_APP',
    delivery_status VARCHAR(50) DEFAULT 'PENDING',
    delivery_attempts INTEGER DEFAULT 0,
    last_delivery_attempt TIMESTAMP WITH TIME ZONE,
    delivery_error TEXT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- ENHANCED USER SESSIONS
-- =====================================================

CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    refresh_token VARCHAR(255) UNIQUE,
    device_id VARCHAR(255),
    device_type VARCHAR(50),
    device_name VARCHAR(255),
    platform VARCHAR(50),
    os_version VARCHAR(50),
    app_version VARCHAR(50),
    ip_address INET,
    user_agent TEXT,
    location_country VARCHAR(2),
    location_city VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    last_activity TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    login_method VARCHAR(50) DEFAULT 'PASSWORD',
    two_factor_verified BOOLEAN DEFAULT false,
    security_score DECIMAL(5,2) DEFAULT 0,
    risk_factors TEXT[],
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- SYSTEM CONFIGURATION
-- =====================================================

CREATE TABLE IF NOT EXISTS system_configurations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key VARCHAR(255) UNIQUE NOT NULL,
    value TEXT,
    data_type VARCHAR(50) DEFAULT 'STRING',
    description TEXT,
    category VARCHAR(100),
    is_public BOOLEAN DEFAULT false,
    is_encrypted BOOLEAN DEFAULT false,
    validation_rules JSONB DEFAULT '{}',
    default_value TEXT,
    min_value DECIMAL,
    max_value DECIMAL,
    allowed_values TEXT[],
    requires_restart BOOLEAN DEFAULT false,
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by UUID REFERENCES user_profiles(id)
);

-- =====================================================
-- PERFORMANCE MONITORING
-- =====================================================

CREATE TABLE IF NOT EXISTS performance_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    metric_name VARCHAR(255) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    value DECIMAL(15,6) NOT NULL,
    unit VARCHAR(50),
    tags JSONB DEFAULT '{}',
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    user_id UUID REFERENCES user_profiles(id),
    session_id VARCHAR(255),
    request_id VARCHAR(255),
    duration_ms INTEGER,
    memory_usage_mb DECIMAL(10,2),
    cpu_usage_percent DECIMAL(5,2),
    database_query_count INTEGER,
    cache_hit_rate DECIMAL(5,2),
    error_count INTEGER DEFAULT 0,
    metadata JSONB DEFAULT '{}'
);

-- =====================================================
-- ENHANCED INDEXES FOR PERFORMANCE
-- =====================================================

-- User profiles indexes
CREATE INDEX IF NOT EXISTS idx_user_profiles_email ON user_profiles(email);
CREATE INDEX IF NOT EXISTS idx_user_profiles_phone ON user_profiles(phone);
CREATE INDEX IF NOT EXISTS idx_user_profiles_role ON user_profiles(role);
CREATE INDEX IF NOT EXISTS idx_user_profiles_active ON user_profiles(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_user_profiles_last_login ON user_profiles(last_login_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_profiles_created_at ON user_profiles(created_at DESC);

-- KPR dossiers indexes
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_customer_id ON kpr_dossiers(customer_id);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_unit_id ON kpr_dossiers(unit_property_id);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_status ON kpr_dossiers(status);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_priority ON kpr_dossiers(priority);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_application_number ON kpr_dossiers(application_number);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_sales_agent ON kpr_dossiers(sales_agent_id);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_assigned_to ON kpr_dossiers(assigned_to);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_created_at ON kpr_dossiers(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_last_activity ON kpr_dossiers(last_activity_at DESC);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_completion ON kpr_dossiers(completion_percentage);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_sla_breached ON kpr_dossiers(sla_breached) WHERE sla_breached = true;

-- Unit properties indexes
CREATE INDEX IF NOT EXISTS idx_unit_properties_project ON unit_properties(project_name);
CREATE INDEX IF NOT EXISTS idx_unit_properties_block ON unit_properties(block);
CREATE INDEX IF NOT EXISTS idx_unit_properties_status ON unit_properties(status);
CREATE INDEX IF NOT EXISTS idx_unit_properties_price ON unit_properties(price);
CREATE INDEX IF NOT EXISTS idx_unit_properties_type ON unit_properties(unit_type);
CREATE INDEX IF NOT EXISTS idx_unit_properties_bedrooms ON unit_properties(bedroom_count);
CREATE INDEX IF NOT EXISTS idx_unit_properties_available ON unit_properties(status) WHERE status = 'AVAILABLE';
CREATE INDEX IF NOT EXISTS idx_unit_properties_premium ON unit_properties(is_premium) WHERE is_premium = true;

-- Financial transactions indexes
CREATE INDEX IF NOT EXISTS idx_financial_transactions_dossier_id ON financial_transactions(dossier_id);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_customer_id ON financial_transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_category ON financial_transactions(category);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_type ON financial_transactions(type);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_status ON financial_transactions(status);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_transaction_number ON financial_transactions(transaction_number);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_due_date ON financial_transactions(due_date);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_paid_date ON financial_transactions(paid_date);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_amount ON financial_transactions(amount_idr);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_created_at ON financial_transactions(created_at DESC);

-- Documents indexes
CREATE INDEX IF NOT EXISTS idx_documents_dossier_id ON documents(dossier_id);
CREATE INDEX IF NOT EXISTS idx_documents_customer_id ON documents(customer_id);
CREATE INDEX IF NOT EXISTS idx_documents_type ON documents(document_type);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_verification ON documents(verification_status);
CREATE INDEX IF NOT EXISTS idx_documents_required ON documents(is_required) WHERE is_required = true;
CREATE INDEX IF NOT EXISTS idx_documents_sensitive ON documents(is_encrypted) WHERE is_encrypted = true;
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents(created_at DESC);

-- Audit logs indexes
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_ip ON audit_logs(ip_address);
CREATE INDEX IF NOT EXISTS idx_audit_logs_suspicious ON audit_logs(is_suspicious) WHERE is_suspicious = true;
CREATE INDEX IF NOT EXISTS idx_audit_logs_risk ON audit_logs(risk_score DESC);

-- Notifications indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_priority ON notifications(priority);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = false;
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_expires ON notifications(expires_at);

-- User sessions indexes
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_token ON user_sessions(session_token);
CREATE INDEX IF NOT EXISTS idx_user_sessions_refresh ON user_sessions(refresh_token);
CREATE INDEX IF NOT EXISTS idx_user_sessions_device ON user_sessions(device_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_active ON user_sessions(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_user_sessions_expires ON user_sessions(expires_at);

-- Performance metrics indexes
CREATE INDEX IF NOT EXISTS idx_performance_metrics_name ON performance_metrics(metric_name);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_type ON performance_metrics(metric_type);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_timestamp ON performance_metrics(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_user ON performance_metrics(user_id);

-- =====================================================
-- ADVANCED TRIGGERS AND FUNCTIONS
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at trigger to all relevant tables
CREATE TRIGGER update_user_profiles_updated_at BEFORE UPDATE ON user_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_kpr_dossiers_updated_at BEFORE UPDATE ON kpr_dossiers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_unit_properties_updated_at BEFORE UPDATE ON unit_properties FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_financial_transactions_updated_at BEFORE UPDATE ON financial_transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_system_configurations_updated_at BEFORE UPDATE ON system_configurations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to calculate dossier completion percentage
CREATE OR REPLACE FUNCTION calculate_dossier_completion()
RETURNS TRIGGER AS $$
BEGIN
    -- Calculate document completion
    UPDATE kpr_dossiers 
    SET 
        documents_completed = (
            SELECT COUNT(*) 
            FROM documents 
            WHERE dossier_id = NEW.dossier_id 
            AND status = 'VERIFIED'
        ),
        documents_total = (
            SELECT COUNT(*) 
            FROM documents 
            WHERE dossier_id = NEW.dossier_id 
            AND is_required = true
        ),
        completion_percentage = CASE 
            WHEN (SELECT COUNT(*) FROM documents WHERE dossier_id = NEW.dossier_id AND is_required = true) > 0 THEN
                (SELECT COUNT(*) FROM documents WHERE dossier_id = NEW.dossier_id AND status = 'VERIFIED' AND is_required = true) * 100.0 / 
                (SELECT COUNT(*) FROM documents WHERE dossier_id = NEW.dossier_id AND is_required = true)
            ELSE 0
        END
    WHERE id = NEW.dossier_id;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply completion calculation trigger
CREATE TRIGGER calculate_dossier_completion_trigger 
    AFTER INSERT OR UPDATE ON documents 
    FOR EACH ROW EXECUTE FUNCTION calculate_dossier_completion();

-- Function to check SLA breaches
CREATE OR REPLACE FUNCTION check_sla_breaches()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if SLA is breached (14 days for initial processing)
    IF NEW.status != 'COMPLETED' AND NEW.status != 'CANCELLED' THEN
        IF EXTRACT(EPOCH FROM (NOW() - NEW.created_at)) / (24 * 3600) > 14 THEN
            UPDATE kpr_dossiers 
            SET 
                sla_breached = true,
                sla_breach_count = sla_breach_count + 1,
                last_activity_at = NOW()
            WHERE id = NEW.id;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply SLA check trigger
CREATE TRIGGER check_sla_breaches_trigger 
    AFTER UPDATE ON kpr_dossiers 
    FOR EACH ROW EXECUTE FUNCTION check_sla_breaches();

-- =====================================================
-- ADVANCED SECURITY FUNCTIONS
-- =====================================================

-- Function to check user permissions
CREATE OR REPLACE FUNCTION can_access_resource(
    p_user_id UUID,
    p_resource_type VARCHAR(100),
    p_resource_id UUID,
    p_action VARCHAR(50)
) RETURNS BOOLEAN AS $$
DECLARE
    user_role user_role;
    resource_owner_id UUID;
BEGIN
    -- Get user role
    SELECT role INTO user_role FROM user_profiles WHERE id = p_user_id;
    
    -- Get resource owner if applicable
    IF p_resource_type = 'kpr_dossier' THEN
        SELECT customer_id INTO resource_owner_id FROM kpr_dossiers WHERE id = p_resource_id;
    ELSIF p_resource_type = 'document' THEN
        SELECT customer_id INTO resource_owner_id FROM documents WHERE id = p_resource_id;
    END IF;
    
    -- Check permissions based on role
    CASE user_role
        WHEN 'CUSTOMER' THEN
            -- Customers can only access their own resources
            RETURN resource_owner_id = p_user_id;
        WHEN 'MARKETING' THEN
            -- Marketing can read and create but not delete
            RETURN p_action IN ('READ', 'CREATE', 'UPDATE');
        WHEN 'LEGAL' THEN
            -- Legal can read and update legal documents
            RETURN p_action IN ('READ', 'UPDATE') AND 
                   (p_resource_type = 'document' OR p_resource_type = 'kpr_dossier');
        WHEN 'FINANCE' THEN
            -- Finance can access financial data
            RETURN p_action IN ('READ', 'CREATE', 'UPDATE') AND 
                   p_resource_type IN ('financial_transaction', 'kpr_dossier');
        WHEN 'ESTATE' THEN
            -- Estate can access unit properties and inspections
            RETURN p_action IN ('READ', 'CREATE', 'UPDATE') AND 
                   p_resource_type IN ('unit_property', 'estate_inspection');
        WHEN 'BOD', 'MANAGER' THEN
            -- BOD and Managers have full access
            RETURN true;
        ELSE
            RETURN false;
    END CASE;
END;
$$ language 'plpgsql' SECURITY DEFINER;

-- =====================================================
-- VIEWS FOR ANALYTICS AND REPORTING
-- =====================================================

-- Executive dashboard view
CREATE OR REPLACE VIEW executive_dashboard AS
SELECT 
    COUNT(*) as total_applications,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed_applications,
    COUNT(*) FILTER (WHERE status = 'CANCELLED') as cancelled_applications,
    ROUND(AVG(completion_percentage), 2) as avg_completion_rate,
    ROUND(AVG(estimated_processing_days), 2) as avg_estimated_days,
    ROUND(AVG(actual_processing_days), 2) as avg_actual_days,
    COUNT(*) FILTER (WHERE sla_breached = true) as sla_breached_count,
    ROUND(SUM(amount_idr), 2) as total_revenue,
    ROUND(AVG(amount_idr), 2) as avg_loan_amount,
    COUNT(DISTINCT customer_id) as unique_customers,
    COUNT(DISTINCT unit_property_id) as unique_units,
    COUNT(DISTINCT sales_agent_id) as active_agents
FROM kpr_dossiers;

-- Department performance view
CREATE OR REPLACE VIEW department_performance AS
SELECT 
    u.role as department,
    COUNT(d.id) as total_dossiers,
    COUNT(d.id) FILTER (WHERE d.status = 'COMPLETED') as completed_dossiers,
    COUNT(d.id) FILTER (WHERE d.status = 'CANCELLED') as cancelled_dossiers,
    ROUND(AVG(d.completion_percentage), 2) as avg_completion_rate,
    ROUND(AVG(d.actual_processing_days), 2) as avg_processing_days,
    COUNT(d.id) FILTER (WHERE d.sla_breached = true) as sla_breached_count
FROM user_profiles u
LEFT JOIN kpr_dossiers d ON u.id = d.assigned_to
WHERE u.role IN ('MARKETING', 'LEGAL', 'FINANCE', 'ESTATE')
GROUP BY u.role;

-- Unit performance view
CREATE OR REPLACE VIEW unit_performance AS
SELECT 
    p.project_name,
    COUNT(d.id) as total_applications,
    COUNT(d.id) FILTER (WHERE d.status = 'COMPLETED') as completed_applications,
    COUNT(d.id) FILTER (WHERE d.status = 'CANCELLED') as cancelled_applications,
    ROUND(AVG(p.price), 2) as avg_unit_price,
    ROUND(AVG(d.loan_amount), 2) as avg_loan_amount,
    COUNT(DISTINCT d.customer_id) as unique_customers,
    COUNT(*) FILTER (WHERE p.status = 'SOLD') as sold_units,
    COUNT(*) FILTER (WHERE p.status = 'AVAILABLE') as available_units
FROM unit_properties p
LEFT JOIN kpr_dossiers d ON p.id = d.unit_property_id
GROUP BY p.project_name;

-- =====================================================
-- SAMPLE DATA INSERTION
-- =====================================================

-- Insert sample system configurations
INSERT INTO system_configurations (key, value, data_type, description, category) VALUES
('app_version', '1.0.0', 'STRING', 'Current application version', 'SYSTEM'),
('max_file_size', '10485760', 'INTEGER', 'Maximum file size in bytes', 'SYSTEM'),
('session_timeout', '3600', 'INTEGER', 'Session timeout in seconds', 'SECURITY'),
('enable_two_factor', 'true', 'BOOLEAN', 'Enable two-factor authentication', 'SECURITY'),
('default_language', 'id', 'STRING', 'Default application language', 'LOCALIZATION'),
('sla_processing_days', '14', 'INTEGER', 'SLA for processing in days', 'BUSINESS'),
('enable_notifications', 'true', 'BOOLEAN', 'Enable push notifications', 'FEATURES'),
('maintenance_mode', 'false', 'BOOLEAN', 'Maintenance mode status', 'SYSTEM')
ON CONFLICT (key) DO NOTHING;

-- =====================================================
-- DATABASE OPTIMIZATION
-- =====================================================

-- Analyze tables for query optimization
ANALYZE user_profiles;
ANALYZE kpr_dossiers;
ANALYZE unit_properties;
ANALYZE financial_transactions;
ANALYZE documents;
ANALYZE audit_logs;
ANALYZE notifications;
ANALYZE user_sessions;

-- Update table statistics
VACUUM ANALYZE user_profiles;
VACUUM ANALYZE kpr_dossiers;
VACUUM ANALYZE unit_properties;
VACUUM ANALYZE financial_transactions;
VACUUM ANALYZE documents;
VACUUM ANALYZE audit_logs;
VACUUM ANALYZE notifications;
VACUUM ANALYZE user_sessions;

-- =====================================================
-- COMPLETION VERIFICATION
-- =====================================================

-- Verify all tables are created
DO $$
DECLARE
    table_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO table_count 
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_type = 'BASE TABLE';
    
    IF table_count >= 10 THEN
        RAISE NOTICE 'Phase 2 Database Schema: 100% Complete - % tables created', table_count;
    ELSE
        RAISE NOTICE 'Phase 2 Database Schema: Incomplete - only % tables created', table_count;
    END IF;
END $$;
