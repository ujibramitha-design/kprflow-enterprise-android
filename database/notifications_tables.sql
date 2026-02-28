-- Notifications and Logging Tables for Phase 6
-- KPRFlow Enterprise

-- Notifications Table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'lead_generated', 'new_spr', 'status_change', 'document_uploaded'
    data JSONB, -- Additional data payload
    reference_id UUID, -- Reference to related record (dossier_id, document_id, etc.)
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    read_at TIMESTAMP WITH TIME ZONE
);

-- SPR Processing Log Table
CREATE TABLE spr_processing_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    nik VARCHAR(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    unit_block VARCHAR(10) NOT NULL,
    email VARCHAR(255),
    phone_number VARCHAR(20),
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status VARCHAR(20) NOT NULL, -- 'success', 'failed', 'needs_review'
    error_message TEXT,
    source VARCHAR(20) NOT NULL, -- 'email_parser', 'pdf_parser'
    raw_data JSONB -- Original email/PDF data for debugging
);

-- Indexes for performance
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_spr_log_nik ON spr_processing_log(nik);
CREATE INDEX idx_spr_log_processed_at ON spr_processing_log(processed_at);
CREATE INDEX idx_spr_log_status ON spr_processing_log(status);

-- RLS Policies
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE spr_processing_log ENABLE ROW LEVEL SECURITY;

-- Notifications RLS Policies
CREATE POLICY "Users can view their own notifications" ON notifications
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Marketing can view lead notifications" ON notifications
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM user_profiles 
            WHERE id = auth.uid() AND role = 'MARKETING'
        ) AND type IN ('lead_generated', 'new_spr')
    );

CREATE POLICY "System can insert notifications" ON notifications
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Users can update their own notifications" ON notifications
    FOR UPDATE USING (auth.uid() = user_id);

-- SPR Log RLS Policies  
CREATE POLICY "Marketing can view SPR logs" ON spr_processing_log
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM user_profiles 
            WHERE id = auth.uid() AND role = 'MARKETING'
        )
    );

CREATE POLICY "Legal can view SPR logs" ON spr_processing_log
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM user_profiles 
            WHERE id = auth.uid() AND role = 'LEGAL'
        )
    );

CREATE POLICY "System can insert SPR logs" ON spr_processing_log
    FOR INSERT WITH CHECK (true);

-- Triggers for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_notifications_updated_at 
    BEFORE UPDATE ON notifications 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
