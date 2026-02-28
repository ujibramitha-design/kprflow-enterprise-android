-- Final Reports table for Phase 12: Final Report Generation

CREATE TABLE IF NOT EXISTS final_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    report_type VARCHAR(30) NOT NULL, -- 'BAST', 'HANDOVER_CERTIFICATE'
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    public_url TEXT NOT NULL,
    handover_date DATE NOT NULL,
    handover_time TIME,
    handover_location TEXT,
    witness_name VARCHAR(255),
    witness_position VARCHAR(255),
    property_condition TEXT,
    included_items TEXT[], -- Array of included items
    excluded_items TEXT[], -- Array of excluded items
    special_notes TEXT,
    customer_signed BOOLEAN DEFAULT FALSE,
    developer_signed BOOLEAN DEFAULT FALSE,
    generated_by UUID REFERENCES user_profiles(id),
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_final_reports_dossier_id ON final_reports(dossier_id);
CREATE INDEX IF NOT EXISTS idx_final_reports_report_type ON final_reports(report_type);
CREATE INDEX IF NOT EXISTS idx_final_reports_handover_date ON final_reports(handover_date);
CREATE INDEX IF NOT EXISTS idx_final_reports_generated_at ON final_reports(generated_at);

-- RLS Policies for final reports
ALTER TABLE final_reports ENABLE ROW LEVEL SECURITY;

-- Estate team can manage final reports
CREATE POLICY "Estate can manage final reports" ON final_reports
    FOR ALL USING (auth.jwt() ->> 'role' = 'ESTATE');

-- Marketing can view final reports
CREATE POLICY "Marketing can view final reports" ON final_reports
    FOR SELECT USING (auth.jwt() ->> 'role' = 'MARKETING');

-- Legal can view final reports
CREATE POLICY "Legal can view final reports" ON final_reports
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Finance can view final reports
CREATE POLICY "Finance can view final reports" ON final_reports
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- BOD can view all final reports
CREATE POLICY "BOD can view all final reports" ON final_reports
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Customers can view their own final reports
CREATE POLICY "Customers can view own final reports" ON final_reports
    FOR SELECT USING (
        auth.uid()::text IN (
            SELECT user_id::text FROM kpr_dossiers WHERE id = dossier_id
        )
    );

-- Function to check if BAST is completed for a dossier
CREATE OR REPLACE FUNCTION is_bast_completed(p_dossier_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM final_reports 
        WHERE dossier_id = p_dossier_id 
        AND report_type = 'BAST'
        AND customer_signed = TRUE
        AND developer_signed = TRUE
    );
END;
$$ LANGUAGE plpgsql;

-- Function to get final report statistics
CREATE OR REPLACE FUNCTION get_final_report_stats(
    p_date_range_start DATE DEFAULT NULL,
    p_date_range_end DATE DEFAULT NULL
)
RETURNS TABLE (
    total_reports BIGINT,
    bast_reports BIGINT,
    handover_certificates BIGINT,
    signed_reports BIGINT,
    pending_signatures BIGINT,
    completion_rate DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_reports,
        COUNT(*) FILTER (WHERE report_type = 'BAST') as bast_reports,
        COUNT(*) FILTER (WHERE report_type = 'HANDOVER_CERTIFICATE') as handover_certificates,
        COUNT(*) FILTER (WHERE customer_signed = TRUE AND developer_signed = TRUE) as signed_reports,
        COUNT(*) FILTER (WHERE (customer_signed = FALSE OR developer_signed = FALSE)) as pending_signatures,
        CASE 
            WHEN COUNT(*) > 0 THEN 
                ROUND((COUNT(*) FILTER (WHERE customer_signed = TRUE AND developer_signed = TRUE)::DECIMAL / COUNT(*)::DECIMAL) * 100, 2)
            ELSE 0 
        END as completion_rate
    FROM final_reports
    WHERE 
        (p_date_range_start IS NULL OR handover_date >= p_date_range_start)
        AND (p_date_range_end IS NULL OR handover_date <= p_date_range_end);
END;
$$ LANGUAGE plpgsql;

-- Function to get handover timeline for a dossier
CREATE OR REPLACE FUNCTION get_handover_timeline(p_dossier_id UUID)
RETURNS TABLE (
    report_date DATE,
    report_type VARCHAR(30),
    handover_location TEXT,
    witness_name VARCHAR(255),
    customer_signed BOOLEAN,
    developer_signed BOOLEAN,
    generated_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        handover_date as report_date,
        report_type,
        handover_location,
        witness_name,
        customer_signed,
        developer_signed,
        generated_at
    FROM final_reports
    WHERE dossier_id = p_dossier_id
    ORDER BY handover_date ASC, generated_at ASC;
END;
$$ LANGUAGE plpgsql;

-- Function to automatically update dossier status when both BAST signatures are complete
CREATE OR REPLACE FUNCTION update_dossier_on_bast_completion()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if this is a BAST report and both signatures are complete
    IF NEW.report_type = 'BAST' AND 
       NEW.customer_signed = TRUE AND 
       NEW.developer_signed = TRUE AND
       (OLD.customer_signed != TRUE OR OLD.developer_signed != TRUE) THEN
       
        -- Update dossier status to BAST_COMPLETED
        UPDATE kpr_dossiers SET
            status = 'BAST_COMPLETED',
            updated_at = NOW(),
            bast_date = NEW.handover_date
        WHERE id = NEW.dossier_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for automatic dossier status update
CREATE TRIGGER update_dossier_on_bast_completion_trigger
    AFTER UPDATE ON final_reports
    FOR EACH ROW
    EXECUTE FUNCTION update_dossier_on_bast_completion();

-- Function to validate handover date
CREATE OR REPLACE FUNCTION validate_handover_date()
RETURNS TRIGGER AS $$
BEGIN
    -- Ensure handover date is not in the past
    IF NEW.handover_date < CURRENT_DATE THEN
        RAISE EXCEPTION 'Handover date cannot be in the past';
    END IF;
    
    -- Ensure handover date is not more than 1 year in the future
    IF NEW.handover_date > CURRENT_DATE + INTERVAL '1 year' THEN
        RAISE EXCEPTION 'Handover date cannot be more than 1 year in the future';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for handover date validation
CREATE TRIGGER validate_handover_date_trigger
    BEFORE INSERT OR UPDATE ON final_reports
    FOR EACH ROW
    EXECUTE FUNCTION validate_handover_date();

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_final_reports_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_final_reports_updated_at BEFORE UPDATE ON final_reports FOR EACH ROW EXECUTE FUNCTION update_final_reports_updated_at();
