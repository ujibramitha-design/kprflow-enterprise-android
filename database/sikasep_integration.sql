-- SiKasep Integration for Phase 13
-- KPRFlow Enterprise

-- SiKasep Status Enum
CREATE TYPE sikasep_status AS ENUM (
    'NOT_CHECKED',
    'IN_PROCESS',
    'ELIGIBLE',
    'NOT_ELIGIBLE',
    'ERROR',
    'DATA_NOT_FOUND'
);

-- Add SiKasep columns to user_profiles
ALTER TABLE user_profiles 
ADD COLUMN IF NOT EXISTS id_sikasep VARCHAR(50),
ADD COLUMN IF NOT EXISTS status_sikasep sikasep_status DEFAULT 'NOT_CHECKED',
ADD COLUMN IF NOT EXISTS sikasep_checked_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS sikasep_rejection_reason TEXT,
ADD COLUMN IF NOT EXISTS is_first_home BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS monthly_income NUMERIC(12,2);

-- SiKasep Logs Table
CREATE TABLE IF NOT EXISTS sikasep_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    nik VARCHAR(16) NOT NULL,
    status sikasep_status NOT NULL,
    id_sikasep VARCHAR(50),
    rejection_reason TEXT,
    screenshot_url TEXT,
    raw_response JSONB,
    processing_time_ms INTEGER,
    checked_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    checked_by UUID REFERENCES user_profiles(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Subsidy Eligibility Criteria Table
CREATE TABLE IF NOT EXISTS subsidy_criteria (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    max_monthly_income NUMERIC(12,2),
    max_property_price NUMERIC(15,2),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Insert default subsidy criteria
INSERT INTO subsidy_criteria (name, description, max_monthly_income, max_property_price) VALUES
('FLPP - KPR Sejahtera', 'Fasilitas Likuiditas Pembiayaan Perumahan untuk KPR Sejahtera', 8000000, 1500000000),
('Subsidi Bunga', 'Subsidi bunga KPR untuk rumah pertama', 10000000, 2000000000),
('BP2BT', 'Bantuan Pembiayaan Perumahan Berbasis Tabungan', 7000000, 1200000000)
ON CONFLICT DO NOTHING;

-- Function to check SiKasep eligibility
CREATE OR REPLACE FUNCTION check_sikasep_eligibility(
    p_user_id UUID,
    p_checked_by UUID DEFAULT NULL
) RETURNS TABLE (
    success BOOLEAN,
    message TEXT,
    sikasep_status sikasep_status,
    id_sikasep VARCHAR,
    rejection_reason TEXT,
    log_id UUID
) AS $$
DECLARE
    v_user_profile RECORD;
    v_log_id UUID;
    v_is_eligible BOOLEAN := false;
    v_rejection_reason TEXT;
    v_monthly_income NUMERIC(12,2);
    v_is_first_home BOOLEAN;
BEGIN
    -- Get user profile
    SELECT up.* INTO v_user_profile
    FROM user_profiles up
    WHERE up.id = p_user_id;
    
    IF NOT FOUND THEN
        RETURN QUERY SELECT false, 'User not found', 'ERROR'::sikasep_status, NULL, NULL, NULL::UUID;
        RETURN;
    END IF;
    
    -- Check if NIK is available
    IF v_user_profile.nik IS NULL OR v_user_profile.nik = '' THEN
        RETURN QUERY SELECT false, 'NIK not available', 'ERROR'::sikasep_status, NULL, NULL, NULL::UUID;
        RETURN;
    END IF;
    
    -- Check basic eligibility criteria
    v_monthly_income := COALESCE(v_user_profile.monthly_income, 0);
    v_is_first_home := COALESCE(v_user_profile.is_first_home, true);
    
    -- Basic eligibility check
    IF v_monthly_income > 8000000 THEN
        v_is_eligible := false;
        v_rejection_reason := 'Monthly income exceeds FLPP limit (Rp 8.000.000)';
    ELSIF NOT v_is_first_home THEN
        v_is_eligible := false;
        v_rejection_reason := 'Not first home purchase';
    ELSE
        v_is_eligible := true;
    END IF;
    
    -- Create log entry
    INSERT INTO sikasep_logs (
        user_id, nik, status, rejection_reason, 
        raw_response, checked_by
    ) VALUES (
        p_user_id, 
        v_user_profile.nik,
        CASE 
            WHEN v_is_eligible THEN 'ELIGIBLE'::sikasep_status
            ELSE 'NOT_ELIGIBLE'::sikasep_status
        END,
        v_rejection_reason,
        json_build_object(
            'monthly_income', v_monthly_income,
            'is_first_home', v_is_first_home,
            'checked_at', NOW()
        ),
        p_checked_by
    ) RETURNING id INTO v_log_id;
    
    -- Update user profile
    UPDATE user_profiles
    SET 
        status_sikasep = CASE 
            WHEN v_is_eligible THEN 'ELIGIBLE'::sikasep_status
            ELSE 'NOT_ELIGIBLE'::sikasep_status
        END,
        sikasep_checked_at = NOW(),
        sikasep_rejection_reason = v_rejection_reason,
        updated_at = NOW()
    WHERE id = p_user_id;
    
    RETURN QUERY 
    SELECT 
        true,
        CASE 
            WHEN v_is_eligible THEN 'User eligible for FLPP subsidy'
            ELSE 'User not eligible: ' || COALESCE(v_rejection_reason, 'Unknown reason')
        END,
        CASE 
            WHEN v_is_eligible THEN 'ELIGIBLE'::sikasep_status
            ELSE 'NOT_ELIGIBLE'::sikasep_status
        END,
        NULL, -- id_sikasep (would be populated by actual SiKasep API)
        v_rejection_reason,
        v_log_id;
END;
$$ LANGUAGE plpgsql;

-- Function to get SiKasep statistics
CREATE OR REPLACE FUNCTION get_sikasep_statistics(
    p_start_date TIMESTAMP WITH TIME ZONE DEFAULT NOW() - INTERVAL '30 days',
    p_end_date TIMESTAMP WITH TIME ZONE DEFAULT NOW()
) RETURNS TABLE (
    total_checked BIGINT,
    eligible_count BIGINT,
    not_eligible_count BIGINT,
    error_count BIGINT,
    eligibility_rate NUMERIC,
    average_income NUMERIC,
    rejection_breakdown JSONB
) AS $$
BEGIN
    RETURN QUERY
    WITH stats AS (
        SELECT 
            COUNT(*) as total,
            COUNT(*) FILTER (WHERE status = 'ELIGIBLE') as eligible,
            COUNT(*) FILTER (WHERE status = 'NOT_ELIGIBLE') as not_eligible,
            COUNT(*) FILTER (WHERE status = 'ERROR') as error,
            AVG((raw_response->>'monthly_income')::NUMERIC) as avg_income
        FROM sikasep_logs
        WHERE checked_at BETWEEN p_start_date AND p_end_date
    ),
    rejections AS (
        SELECT 
            rejection_reason,
            COUNT(*) as count
        FROM sikasep_logs
        WHERE checked_at BETWEEN p_start_date AND p_end_date
        AND status = 'NOT_ELIGIBLE'
        AND rejection_reason IS NOT NULL
        GROUP BY rejection_reason
    )
    SELECT 
        s.total,
        s.eligible,
        s.not_eligible,
        s.error,
        CASE 
            WHEN s.total > 0 THEN ROUND((s.eligible::NUMERIC / s.total) * 100, 2)
            ELSE 0
        END as eligibility_rate,
        COALESCE(s.avg_income, 0),
        (SELECT jsonb_agg(jsonb_build_object('reason', rejection_reason, 'count', count)) 
         FROM rejections) as rejection_breakdown
    FROM stats s;
END;
$$ LANGUAGE plpgsql;

-- Function to bulk check SiKasep eligibility
CREATE OR REPLACE FUNCTION bulk_check_sikasep_eligibility(
    p_user_ids UUID[],
    p_checked_by UUID DEFAULT NULL
) RETURNS TABLE (
    user_id UUID,
    success BOOLEAN,
    message TEXT,
    sikasep_status sikasep_status
) AS $$
DECLARE
    v_user_id UUID;
BEGIN
    FOREACH v_user_id IN ARRAY p_user_ids
    LOOP
        RETURN QUERY
        SELECT 
            v_user_id,
            result.success,
            result.message,
            result.sikasep_status
        FROM check_sikasep_eligibility(v_user_id, p_checked_by) AS result;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- View for SiKasep dashboard
CREATE OR REPLACE VIEW v_sikasep_dashboard AS
SELECT 
    up.id as user_id,
    up.name,
    up.nik,
    up.monthly_income,
    up.is_first_home,
    up.status_sikasep,
    up.sikasep_checked_at,
    up.sikasep_rejection_reason,
    kd.id as dossier_id,
    kd.status as kpr_status,
    up_block.block || '/' || up_block.unit_number as unit_info,
    CASE 
        WHEN up.status_sikasep = 'ELIGIBLE' THEN true
        WHEN up.status_sikasep = 'NOT_ELIGIBLE' THEN false
        ELSE NULL
    END as is_eligible
FROM user_profiles up
LEFT JOIN kpr_dossiers kd ON up.id = kd.user_id
LEFT JOIN unit_properties up_block ON kd.unit_id = up_block.id
WHERE up.is_active = true
AND up.role = 'CUSTOMER';

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_sikasep_logs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_sikasep_logs_updated_at
    BEFORE UPDATE ON sikasep_logs
    FOR EACH ROW EXECUTE FUNCTION update_sikasep_logs_updated_at();

CREATE TRIGGER tr_subsidy_criteria_updated_at
    BEFORE UPDATE ON subsidy_criteria
    FOR EACH ROW EXECUTE FUNCTION update_sikasep_logs_updated_at();

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_user_profiles_status_sikasep ON user_profiles(status_sikasep);
CREATE INDEX IF NOT EXISTS idx_user_profiles_sikasep_checked_at ON user_profiles(sikasep_checked_at);
CREATE INDEX IF NOT EXISTS idx_sikasep_logs_user_id ON sikasep_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_sikasep_logs_nik ON sikasep_logs(nik);
CREATE INDEX IF NOT EXISTS idx_sikasep_logs_status ON sikasep_logs(status);
CREATE INDEX IF NOT EXISTS idx_sikasep_logs_checked_at ON sikasep_logs(checked_at);

-- RLS Policies
ALTER TABLE sikasep_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE subsidy_criteria ENABLE ROW LEVEL SECURITY;

-- SiKasep Logs RLS
CREATE POLICY "Users can view their own SiKasep logs" ON sikasep_logs
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Marketing can view all SiKasep logs" ON sikasep_logs
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM user_profiles
            WHERE id = auth.uid() AND role = 'MARKETING'
        )
    );

CREATE POLICY "System can insert SiKasep logs" ON sikasep_logs
    FOR INSERT WITH CHECK (true);

-- Subsidy Criteria RLS
CREATE POLICY "Authenticated users can view subsidy criteria" ON subsidy_criteria
    FOR SELECT USING (auth.role() = 'authenticated');

-- Grant permissions
GRANT EXECUTE ON FUNCTION check_sikasep_eligibility TO authenticated;
GRANT EXECUTE ON FUNCTION get_sikasep_statistics TO authenticated;
GRANT EXECUTE ON FUNCTION bulk_check_sikasep_eligibility TO authenticated;
GRANT SELECT ON v_sikasep_dashboard TO authenticated;
