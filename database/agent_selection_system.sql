-- Agent Selection System
-- Customer Agent Selection & Legal Manual Assignment
-- KPRFlow Enterprise - Agent Selection Control

-- =====================================================
-- AGENT SELECTION TABLES
-- =====================================================

-- Agent Profiles Table
CREATE TABLE agent_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    agent_code VARCHAR(20) UNIQUE NOT NULL,
    specialization VARCHAR(50) NOT NULL, -- 'LEGAL', 'MARKETING', 'FINANCE', 'SUPPORT'
    tier VARCHAR(20) NOT NULL DEFAULT 'REGULAR', -- 'BASIC', 'REGULAR', 'PREMIUM', 'VIP'
    max_customers INTEGER DEFAULT 50,
    current_customers INTEGER DEFAULT 0,
    is_available BOOLEAN DEFAULT true,
    rating DECIMAL(3,2) DEFAULT 5.0,
    total_cases INTEGER DEFAULT 0,
    success_rate DECIMAL(5,2) DEFAULT 100.00,
    languages TEXT[], -- Array of languages: ['id', 'en', 'zh', 'ar']
    expertise_areas TEXT[], -- Array of expertise: ['KPR_SUBSIDI', 'KPR_KOMERSIL', 'CASH_KERAS']
    working_hours JSONB DEFAULT '{"start": "09:00", "end": "17:00", "timezone": "Asia/Jakarta"}',
    response_time_minutes INTEGER DEFAULT 30,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Customer Agent Preferences Table
CREATE TABLE customer_agent_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    preferred_agent_id UUID REFERENCES agent_profiles(id) ON DELETE SET NULL,
    preference_type VARCHAR(20) NOT NULL, -- 'MANUAL', 'AUTO', 'LEGAL_ASSIGNED'
    selection_reason TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(customer_id)
);

-- Legal Agent Assignments Table
CREATE TABLE legal_agent_assignments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    assigned_by UUID REFERENCES user_profiles(id) ON DELETE SET NULL, -- Legal staff who assigned
    agent_id UUID REFERENCES agent_profiles(id) ON DELETE CASCADE,
    assignment_reason TEXT NOT NULL,
    priority_level VARCHAR(10) DEFAULT 'NORMAL', -- 'LOW', 'NORMAL', 'HIGH', 'URGENT'
    is_temporary BOOLEAN DEFAULT false,
    temporary_until TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- 'ACTIVE', 'INACTIVE', 'REPLACED'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Agent Performance Metrics Table
CREATE TABLE agent_performance_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agent_id UUID REFERENCES agent_profiles(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    total_interactions INTEGER DEFAULT 0,
    avg_response_time_minutes INTEGER DEFAULT 0,
    customer_satisfaction_score DECIMAL(3,2) DEFAULT 0.0,
    cases_resolved INTEGER DEFAULT 0,
    cases_pending INTEGER DEFAULT 0,
    escalation_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(agent_id, metric_date)
);

-- =====================================================
-- AGENT SELECTION FUNCTIONS
-- =====================================================

-- Function to get available agents for customer selection
CREATE OR REPLACE FUNCTION get_available_agents_for_customer(
    p_customer_id UUID,
    p_specialization VARCHAR(50) DEFAULT NULL,
    p_language VARCHAR(10) DEFAULT 'id'
)
RETURNS JSONB AS $$
DECLARE
    v_customer_tier VARCHAR(20);
    v_available_agents JSONB;
BEGIN
    -- Get customer tier
    SELECT 
        CASE 
            WHEN EXISTS(SELECT 1 FROM kpr_dossiers WHERE user_id = p_customer_id AND status = 'BAST_COMPLETED') THEN 'PREMIUM'
            WHEN EXISTS(SELECT 1 FROM kpr_dossiers WHERE user_id = p_customer_id AND status IN ('PUTUSAN_KREDIT_ACC', 'SP3K_TERBIT')) THEN 'REGULAR'
            ELSE 'BASIC'
        END INTO v_customer_tier;
    
    -- Get available agents based on customer tier and preferences
    SELECT json_agg(
        json_build_object(
            'agent_id', ap.id,
            'agent_code', ap.agent_code,
            'user_id', ap.user_id,
            'specialization', ap.specialization,
            'tier', ap.tier,
            'name', up.name,
            'email', up.email,
            'phone', up.phone_number,
            'rating', ap.rating,
            'total_cases', ap.total_cases,
            'success_rate', ap.success_rate,
            'languages', ap.languages,
            'expertise_areas', ap.expertise_areas,
            'current_customers', ap.current_customers,
            'max_customers', ap.max_customers,
            'response_time_minutes', ap.response_time_minutes,
            'working_hours', ap.working_hours,
            'is_available', ap.is_available AND ap.current_customers < ap.max_customers,
            'match_score', 
                CASE 
                    WHEN ap.tier = v_customer_tier THEN 100
                    WHEN ap.tier = 'PREMIUM' AND v_customer_tier IN ('REGULAR', 'BASIC') THEN 80
                    WHEN ap.tier = 'REGULAR' AND v_customer_tier = 'BASIC' THEN 90
                    WHEN ap.tier = 'BASIC' AND v.customer_tier IN ('REGULAR', 'PREMIUM') THEN 70
                    ELSE 60
                END
        ) ORDER BY 
            CASE 
                WHEN ap.tier = v_customer_tier THEN 1
                WHEN ap.tier = 'PREMIUM' AND v_customer_tier IN ('REGULAR', 'BASIC') THEN 2
                WHEN ap.tier = 'REGULAR' AND v.customer_tier = 'BASIC' THEN 2
                ELSE 3
            END,
            ap.rating DESC,
            ap.success_rate DESC,
            ap.current_customers ASC
        )
    ) INTO v_available_agents
    FROM agent_profiles ap
    JOIN user_profiles up ON ap.user_id = up.id
    WHERE ap.is_available = true
    AND ap.current_customers < ap.max_customers
    AND (p_specialization IS NULL OR ap.specialization = p_specialization)
    AND (p_language = 'id' OR p_language = ANY(ap.languages));
    
    RETURN COALESCE(v_available_agents, '[]'::jsonb);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function for customer to select preferred agent
CREATE OR REPLACE FUNCTION customer_select_agent(
    p_customer_id UUID,
    p_agent_id UUID,
    p_selection_reason TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_agent_available BOOLEAN;
    v_assignment_count INTEGER;
    v_result JSONB;
BEGIN
    -- Check if agent is available
    SELECT (is_available AND current_customers < max_customers) INTO v_agent_available
    FROM agent_profiles WHERE id = p_agent_id;
    
    IF NOT v_agent_available THEN
        RETURN json_build_object('success', false, 'message', 'Agent is not available');
    END IF;
    
    -- Check if customer already has a preference
    SELECT COUNT(*) INTO v_assignment_count
    FROM customer_agent_preferences WHERE customer_id = p_customer_id AND is_active = true;
    
    IF v_assignment_count > 0 THEN
        -- Update existing preference
        UPDATE customer_agent_preferences 
        SET preferred_agent_id = p_agent_id,
            preference_type = 'MANUAL',
            selection_reason = p_selection_reason,
            updated_at = NOW()
        WHERE customer_id = p_customer_id AND is_active = true;
    ELSE
        -- Create new preference
        INSERT INTO customer_agent_preferences (
            customer_id, preferred_agent_id, preference_type, selection_reason
        ) VALUES (p_customer_id, p_agent_id, 'MANUAL', p_selection_reason);
    END IF;
    
    -- Update agent customer count
    UPDATE agent_profiles 
    SET current_customers = current_customers + 1
    WHERE id = p_agent_id;
    
    -- Create legal assignment record
    INSERT INTO legal_agent_assignments (
        customer_id, assigned_by, agent_id, assignment_reason, priority_level
    ) VALUES (
        p_customer_id, NULL, p_agent_id, 
        COALESCE(p_selection_reason, 'Customer selected agent'), 'NORMAL'
    );
    
    RETURN json_build_object(
        'success', true, 
        'message', 'Agent selected successfully',
        'agent_id', p_agent_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function for legal to manually assign agent
CREATE OR REPLACE FUNCTION legal_assign_agent(
    p_legal_staff_id UUID,
    p_customer_id UUID,
    p_agent_id UUID,
    p_assignment_reason TEXT,
    p_priority_level VARCHAR(10) DEFAULT 'NORMAL',
    p_is_temporary BOOLEAN DEFAULT false,
    p_temporary_until TIMESTAMP WITH TIME ZONE DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_agent_available BOOLEAN;
    v_existing_assignment UUID;
    v_result JSONB;
BEGIN
    -- Check if agent is available
    SELECT (is_available AND current_customers < max_customers) INTO v_agent_available
    FROM agent_profiles WHERE id = p_agent_id;
    
    IF NOT v_agent_available THEN
        RETURN json_build_object('success', false, 'message', 'Agent is not available');
    END IF;
    
    -- Check existing assignment
    SELECT id INTO v_existing_assignment
    FROM legal_agent_assignments 
    WHERE customer_id = p_customer_id AND status = 'ACTIVE';
    
    IF v_existing_assignment IS NOT NULL THEN
        -- Deactivate existing assignment
        UPDATE legal_agent_assignments 
        SET status = 'REPLACED', updated_at = NOW()
        WHERE id = v_existing_assignment;
        
        -- Decrease old agent customer count
        UPDATE agent_profiles 
        SET current_customers = current_customers - 1
        WHERE id = (SELECT agent_id FROM legal_agent_assignments WHERE id = v_existing_assignment);
    END IF;
    
    -- Create new assignment
    INSERT INTO legal_agent_assignments (
        customer_id, assigned_by, agent_id, assignment_reason, 
        priority_level, is_temporary, temporary_until
    ) VALUES (
        p_customer_id, p_legal_staff_id, p_agent_id, 
        p_assignment_reason, p_priority_level, p_is_temporary, p_temporary_until
    );
    
    -- Update customer preference
    INSERT INTO customer_agent_preferences (
        customer_id, preferred_agent_id, preference_type, selection_reason
    ) VALUES (
        p_customer_id, p_agent_id, 'LEGAL_ASSIGNED', p_assignment_reason
    )
    ON CONFLICT (customer_id) DO UPDATE SET
        preferred_agent_id = p_agent_id,
        preference_type = 'LEGAL_ASSIGNED',
        selection_reason = p_assignment_reason,
        updated_at = NOW();
    
    -- Update agent customer count
    UPDATE agent_profiles 
    SET current_customers = current_customers + 1
    WHERE id = p_agent_id;
    
    RETURN json_build_object(
        'success', true, 
        'message', 'Agent assigned successfully',
        'agent_id', p_agent_id,
        'assignment_type', CASE WHEN p_is_temporary THEN 'TEMPORARY' ELSE 'PERMANENT' END
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get customer's assigned agent
CREATE OR REPLACE FUNCTION get_customer_assigned_agent(p_customer_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_agent_info JSONB;
BEGIN
    SELECT json_build_object(
        'agent_id', ap.id,
        'agent_code', ap.agent_code,
        'user_id', ap.user_id,
        'name', up.name,
        'email', up.email,
        'phone', up.phone_number,
        'specialization', ap.specialization,
        'tier', ap.tier,
        'rating', ap.rating,
        'languages', ap.languages,
        'expertise_areas', ap.expertise_areas,
        'response_time_minutes', ap.response_time_minutes,
        'working_hours', ap.working_hours,
        'assignment_type', cap.preference_type,
        'assignment_reason', cap.selection_reason,
        'assigned_at', cap.created_at,
        'legal_assignment', json_build_object(
            'assigned_by', la.assigned_by,
            'assignment_reason', la.assignment_reason,
            'priority_level', la.priority_level,
            'is_temporary', la.is_temporary,
            'temporary_until', la.temporary_until,
            'assigned_at', la.created_at
        )
    ) INTO v_agent_info
    FROM customer_agent_preferences cap
    JOIN agent_profiles ap ON cap.preferred_agent_id = ap.id
    JOIN user_profiles up ON ap.user_id = up.id
    LEFT JOIN legal_agent_assignments la ON cap.customer_id = la.customer_id AND la.status = 'ACTIVE'
    WHERE cap.customer_id = p_customer_id AND cap.is_active = true;
    
    RETURN COALESCE(v_agent_info, '{}'::jsonb);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- VIEWS FOR AGENT SELECTION
-- =====================================================

-- Agent Selection View for Customers
CREATE VIEW customer_agent_selection_view AS
SELECT 
    cap.customer_id,
    cap.preferred_agent_id,
    cap.preference_type,
    cap.selection_reason,
    ap.agent_code,
    up.name as agent_name,
    up.email as agent_email,
    up.phone_number as agent_phone,
    ap.specialization,
    ap.tier,
    ap.rating,
    ap.languages,
    ap.expertise_areas,
    ap.response_time_minutes,
    ap.working_hours,
    la.assigned_by as legal_assigned_by,
    la.assignment_reason as legal_assignment_reason,
    la.priority_level,
    la.is_temporary,
    la.temporary_until
FROM customer_agent_preferences cap
JOIN agent_profiles ap ON cap.preferred_agent_id = ap.id
JOIN user_profiles up ON ap.user_id = up.id
LEFT JOIN legal_agent_assignments la ON cap.customer_id = la.customer_id AND la.status = 'ACTIVE'
WHERE cap.is_active = true;

-- Legal Agent Management View
CREATE VIEW legal_agent_management_view AS
SELECT 
    la.id as assignment_id,
    la.customer_id,
    up.name as customer_name,
    up.email as customer_email,
    la.agent_id,
    ap.agent_code,
    agent_up.name as agent_name,
    agent_up.email as agent_email,
    la.assigned_by,
    legal_up.name as assigned_by_name,
    la.assignment_reason,
    la.priority_level,
    la.is_temporary,
    la.temporary_until,
    la.status,
    la.created_at as assigned_at,
    la.updated_at
FROM legal_agent_assignments la
JOIN user_profiles up ON la.customer_id = up.id
JOIN agent_profiles ap ON la.agent_id = ap.id
JOIN user_profiles agent_up ON ap.user_id = agent_up.id
LEFT JOIN user_profiles legal_up ON la.assigned_by = legal_up.id;

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX idx_agent_profiles_specialization ON agent_profiles(specialization);
CREATE INDEX idx_agent_profiles_tier ON agent_profiles(tier);
CREATE INDEX idx_agent_profiles_available ON agent_profiles(is_available);
CREATE INDEX idx_customer_agent_preferences_customer_id ON customer_agent_preferences(customer_id);
CREATE INDEX idx_customer_agent_preferences_agent_id ON customer_agent_preferences(preferred_agent_id);
CREATE INDEX idx_legal_agent_assignments_customer_id ON legal_agent_assignments(customer_id);
CREATE INDEX idx_legal_agent_assignments_agent_id ON legal_agent_assignments(agent_id);
CREATE INDEX idx_legal_agent_assignments_status ON legal_agent_assignments(status);

-- =====================================================
-- RLS POLICIES
-- =====================================================

-- Customer can view their own agent preferences
CREATE POLICY "Customers view own agent preferences" ON customer_agent_preferences
FOR SELECT USING (auth.uid() = customer_id);

-- Customer can update their own preferences (only manual selection)
CREATE POLICY "Customers update own preferences" ON customer_agent_preferences
FOR UPDATE USING (
    auth.uid() = customer_id 
    AND preference_type = 'MANUAL'
);

-- Legal staff can manage agent assignments
CREATE POLICY "Legal staff manage assignments" ON legal_agent_assignments
FOR ALL USING (
    EXISTS (
        SELECT 1 FROM user_profiles 
        WHERE id = auth.uid() 
        AND role = 'LEGAL'
    )
);

-- All authenticated users can view agent profiles
CREATE POLICY "View agent profiles" ON agent_profiles
FOR SELECT USING (is_active = true);

-- =====================================================
-- SAMPLE DATA
-- =====================================================

-- Insert sample agents
INSERT INTO agent_profiles (user_id, agent_code, specialization, tier, max_customers, languages, expertise_areas) VALUES
-- Legal Agents
((SELECT id FROM user_profiles WHERE email = 'legal1@kprflow.com'), 'LEG001', 'LEGAL', 'PREMIUM', 30, ARRAY['id', 'en'], ARRAY['KPR_SUBSIDI', 'KPR_KOMERSIL']),
((SELECT id FROM user_profiles WHERE email = 'legal2@kprflow.com'), 'LEG002', 'LEGAL', 'REGULAR', 40, ARRAY['id'], ARRAY['KPR_SUBSIDI', 'CASH_KERAS']),
((SELECT id FROM user_profiles WHERE email = 'legal3@kprflow.com'), 'LEG003', 'LEGAL', 'BASIC', 50, ARRAY['id'], ARRAY['KPR_KOMERSIL']),

-- Support Agents
((SELECT id FROM user_profiles WHERE email = 'support1@kprflow.com'), 'SUP001', 'SUPPORT', 'REGULAR', 60, ARRAY['id', 'en'], ARRAY['ALL']),
((SELECT id FROM user_profiles WHERE email = 'support2@kprflow.com'), 'SUP002', 'SUPPORT', 'BASIC', 80, ARRAY['id'], ARRAY['ALL']);

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
AGENT SELECTION SYSTEM DOCUMENTATION

========================================
FEATURES:
1. Customer Agent Selection:
   - Customer can choose preferred agent from available list
   - Filter by specialization, language, rating
   - View agent profile and availability
   - Manual selection with reason tracking

2. Legal Manual Assignment:
   - Legal staff can manually assign agents to customers
   - Override customer preferences if needed
   - Temporary assignments with expiry
   - Priority levels for urgent cases
   - Assignment history tracking

3. Agent Management:
   - Agent profiles with specialization and tier
   - Availability and capacity management
   - Performance metrics tracking
   - Language and expertise areas
   - Working hours and response time

========================================
BENEFITS:
- Customer Choice: Customers can choose preferred agents
- Legal Control: Legal staff can override when necessary
- Flexibility: Temporary and permanent assignments
- Transparency: Clear assignment reasons and history
- Performance: Agent performance tracking and optimization

========================================
SECURITY:
- RLS policies for data access control
- Customer can only manage own preferences
- Legal staff can manage assignments
- Agent profile visibility for all authenticated users
- Audit trail for all assignments

========================================
USAGE:
1. Customer selects agent: customer_select_agent()
2. Legal assigns agent: legal_assign_agent()
3. Get available agents: get_available_agents_for_customer()
4. Get assigned agent: get_customer_assigned_agent()
5. View assignments: customer_agent_selection_view, legal_agent_management_view
*/
