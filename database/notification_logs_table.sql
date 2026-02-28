-- Notification Logs table for Phase 10: WhatsApp Integration

CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    dossier_id UUID REFERENCES kpr_dossiers(id) ON DELETE SET NULL,
    type VARCHAR(50) NOT NULL, -- 'document_missing', 'sp3k_issued', 'sla_warning', 'bast_invitation'
    channel VARCHAR(20) NOT NULL DEFAULT 'whatsapp', -- 'whatsapp', 'email', 'sms'
    message_sid VARCHAR(255), -- Twilio message SID or other provider ID
    status VARCHAR(20) NOT NULL DEFAULT 'sent', -- 'sent', 'delivered', 'read', 'failed'
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    metadata JSONB -- Additional metadata like template variables, etc.
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_notification_logs_user_id ON notification_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_logs_dossier_id ON notification_logs(dossier_id);
CREATE INDEX IF NOT EXISTS idx_notification_logs_type ON notification_logs(type);
CREATE INDEX IF NOT EXISTS idx_notification_logs_status ON notification_logs(status);
CREATE INDEX IF NOT EXISTS idx_notification_logs_sent_at ON notification_logs(sent_at);

-- RLS Policies for notification logs
ALTER TABLE notification_logs ENABLE ROW LEVEL SECURITY;

-- Users can view their own notification logs
CREATE POLICY "Users can view own notification logs" ON notification_logs
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- Marketing can view notification logs for their customers
CREATE POLICY "Marketing can view customer notification logs" ON notification_logs
    FOR SELECT USING (auth.jwt() ->> 'role' = 'MARKETING');

-- Legal can view notification logs for legal-related notifications
CREATE POLICY "Legal can view legal notification logs" ON notification_logs
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'LEGAL' AND
        type IN ('document_missing', 'sp3k_issued')
    );

-- Finance can view notification logs for finance-related notifications
CREATE POLICY "Finance can view finance notification logs" ON notification_logs
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'FINANCE' AND
        type IN ('sla_warning', 'bast_invitation')
    );

-- BOD can view all notification logs
CREATE POLICY "BOD can view all notification logs" ON notification_logs
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Function to update notification status (for webhooks)
CREATE OR REPLACE FUNCTION update_notification_status(
    p_message_sid VARCHAR(255),
    p_status VARCHAR(20),
    p_error_message TEXT DEFAULT NULL
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE notification_logs 
    SET 
        status = p_status,
        delivered_at = CASE WHEN p_status = 'delivered' THEN NOW() ELSE delivered_at END,
        read_at = CASE WHEN p_status = 'read' THEN NOW() ELSE read_at END,
        error_message = p_error_message,
        updated_at = NOW()
    WHERE message_sid = p_message_sid;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to get notification statistics
CREATE OR REPLACE FUNCTION get_notification_stats(
    p_user_id UUID DEFAULT NULL,
    p_date_range_start DATE DEFAULT NULL,
    p_date_range_end DATE DEFAULT NULL
)
RETURNS TABLE (
    total_sent BIGINT,
    total_delivered BIGINT,
    total_read BIGINT,
    total_failed BIGINT,
    delivery_rate DECIMAL,
    read_rate DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_sent,
        COUNT(*) FILTER (WHERE status = 'delivered') as total_delivered,
        COUNT(*) FILTER (WHERE status = 'read') as total_read,
        COUNT(*) FILTER (WHERE status = 'failed') as total_failed,
        CASE 
            WHEN COUNT(*) > 0 THEN 
                ROUND((COUNT(*) FILTER (WHERE status = 'delivered')::DECIMAL / COUNT(*)::DECIMAL) * 100, 2)
            ELSE 0 
        END as delivery_rate,
        CASE 
            WHEN COUNT(*) > 0 THEN 
                ROUND((COUNT(*) FILTER (WHERE status = 'read')::DECIMAL / COUNT(*)::DECIMAL) * 100, 2)
            ELSE 0 
        END as read_rate
    FROM notification_logs
    WHERE 
        (p_user_id IS NULL OR user_id = p_user_id)
        AND (p_date_range_start IS NULL OR DATE(sent_at) >= p_date_range_start)
        AND (p_date_range_end IS NULL OR DATE(sent_at) <= p_date_range_end);
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_notification_logs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

ALTER TABLE notification_logs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

CREATE TRIGGER update_notification_logs_updated_at BEFORE UPDATE ON notification_logs FOR EACH ROW EXECUTE FUNCTION update_notification_logs_updated_at();
