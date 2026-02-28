-- Additional tables for Phase 6: SPR Email Parser

-- Notifications table for marketing team notifications
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'new_spr', 'status_change', 'sla_warning', etc.
    reference_id UUID, -- Reference to dossier, transaction, etc.
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- SPR Processing Log for debugging and audit
CREATE TABLE IF NOT EXISTS spr_processing_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID REFERENCES kpr_dossiers(id) ON DELETE SET NULL,
    nik VARCHAR(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    unit_block VARCHAR(10) NOT NULL,
    email VARCHAR(255),
    phone_number VARCHAR(20),
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status VARCHAR(20) NOT NULL, -- 'success', 'failed', 'needs_review'
    error_message TEXT,
    raw_data JSONB -- Store raw email data for debugging
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_spr_log_nik ON spr_processing_log(nik);
CREATE INDEX IF NOT EXISTS idx_spr_log_processed_at ON spr_processing_log(processed_at);
CREATE INDEX IF NOT EXISTS idx_spr_log_status ON spr_processing_log(status);

-- Create updated_at trigger for notifications
CREATE OR REPLACE FUNCTION update_notifications_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_notifications_updated_at BEFORE UPDATE ON notifications FOR EACH ROW EXECUTE FUNCTION update_notifications_updated_at();

-- RLS Policies for notifications
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- Users can only see their own notifications
CREATE POLICY "Users can view own notifications" ON notifications
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- Users can update their own notifications (mark as read)
CREATE POLICY "Users can update own notifications" ON notifications
    FOR UPDATE USING (auth.uid()::text = user_id::text);

-- Staff roles can view notifications for their department
CREATE POLICY "Staff can view department notifications" ON notifications
    FOR SELECT USING (
        auth.jwt() ->> 'role' IN ('MARKETING', 'LEGAL', 'FINANCE', 'BOD')
    );

-- RLS Policies for SPR Processing Log (BOD and Marketing only)
ALTER TABLE spr_processing_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Marketing can view SPR logs" ON spr_processing_log
    FOR SELECT USING (auth.jwt() ->> 'role' = 'MARKETING');

CREATE POLICY "BOD can view SPR logs" ON spr_processing_log
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');
