-- Push Notifications tables for Phase 16: Customer Mobile App

-- Push Device Tokens table
CREATE TABLE IF NOT EXISTS push_device_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    device_token TEXT NOT NULL,
    device_type VARCHAR(20) DEFAULT 'android',
    is_active BOOLEAN DEFAULT TRUE,
    registered_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    unregistered_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, device_token)
);

-- Push Notifications table
CREATE TABLE IF NOT EXISTS push_notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'status_update', 'document_verification', 'payment_reminder', 'bast_invitation', etc.
    data JSONB, -- Additional notification data
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    sent_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_push_device_tokens_user_id ON push_device_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_push_device_tokens_device_token ON push_device_tokens(device_token);
CREATE INDEX IF NOT EXISTS idx_push_device_tokens_is_active ON push_device_tokens(is_active);

CREATE INDEX IF NOT EXISTS idx_push_notifications_user_id ON push_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_push_notifications_type ON push_notifications(type);
CREATE INDEX IF NOT EXISTS idx_push_notifications_status ON push_notifications(status);
CREATE INDEX IF NOT EXISTS idx_push_notifications_created_at ON push_notifications(created_at);

-- RLS Policies for push device tokens
ALTER TABLE push_device_tokens ENABLE ROW LEVEL SECURITY;

-- Users can manage their own device tokens
CREATE POLICY "Users can manage own device tokens" ON push_device_tokens
    FOR ALL USING (auth.uid()::text = user_id::text);

-- RLS Policies for push notifications
ALTER TABLE push_notifications ENABLE ROW LEVEL SECURITY;

-- Users can view their own notifications
CREATE POLICY "Users can view own notifications" ON push_notifications
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- Users can update their own notifications (mark as read)
CREATE POLICY "Users can update own notifications" ON push_notifications
    FOR UPDATE USING (auth.uid()::text = user_id::text);

-- System can send notifications to any user
CREATE POLICY "System can send notifications" ON push_notifications
    FOR INSERT WITH CHECK (auth.jwt() ->> 'role' = 'SYSTEM');

-- Function to get user's unread notification count
CREATE OR REPLACE FUNCTION get_unread_notification_count(p_user_id UUID)
RETURNS INTEGER AS $$
BEGIN
    RETURN (
        SELECT COUNT(*)::INTEGER
        FROM push_notifications
        WHERE user_id = p_user_id
        AND read_at IS NULL
    );
END;
$$ LANGUAGE plpgsql;

-- Function to mark all notifications as read for a user
CREATE OR REPLACE FUNCTION mark_all_notifications_read(p_user_id UUID)
RETURNS INTEGER AS $$
BEGIN
    UPDATE push_notifications
    SET read_at = NOW(),
        updated_at = NOW()
    WHERE user_id = p_user_id
    AND read_at IS NULL;
    
    RETURN ROW_COUNT;
END;
$$ LANGUAGE plpgsql;

-- Function to send push notification (placeholder for actual push service)
CREATE OR REPLACE FUNCTION send_push_notification(
    p_user_id UUID,
    p_title VARCHAR(255),
    p_message TEXT,
    p_type VARCHAR(50),
    p_data JSONB DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    notification_id UUID;
BEGIN
    -- Create notification record
    INSERT INTO push_notifications (
        user_id, title, message, type, data, status
    ) VALUES (
        p_user_id, p_title, p_message, p_type, p_data, 'SENT'
    ) RETURNING id INTO notification_id;
    
    -- TODO: Integrate with actual push notification service (Firebase Cloud Messaging)
    -- This would involve:
    -- 1. Get active device tokens for the user
    -- 2. Send notification via FCM
    -- 3. Update notification status based on FCM response
    
    RETURN notification_id;
END;
$$ LANGUAGE plpgsql;

-- Function to send bulk push notifications
CREATE OR REPLACE FUNCTION send_bulk_push_notification(
    p_user_ids UUID[],
    p_title VARCHAR(255),
    p_message TEXT,
    p_type VARCHAR(50),
    p_data JSONB DEFAULT NULL
)
RETURNS TABLE (
    notification_id UUID,
    user_id UUID,
    success BOOLEAN,
    error_message TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        send_push_notification(uid, p_title, p_message, p_type, p_data) as notification_id,
        uid as user_id,
        true as success,
        NULL::TEXT as error_message
    FROM unnest(p_user_ids) uid;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old notifications
CREATE OR REPLACE FUNCTION cleanup_old_notifications(
    p_days_old INTEGER DEFAULT 90
)
RETURNS INTEGER AS $$
BEGIN
    DELETE FROM push_notifications
    WHERE created_at < NOW() - INTERVAL '1 day' * p_days_old;
    
    RETURN ROW_COUNT;
END;
$$ LANGUAGE plpgsql;

-- Function to get notification statistics
CREATE OR REPLACE FUNCTION get_notification_statistics(
    p_date_range_start DATE DEFAULT NULL,
    p_date_range_end DATE DEFAULT NULL
)
RETURNS TABLE (
    total_notifications BIGINT,
    sent_notifications BIGINT,
    delivered_notifications BIGINT,
    read_notifications BIGINT,
    failed_notifications BIGINT,
    delivery_rate DECIMAL(5,2),
    read_rate DECIMAL(5,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_notifications,
        COUNT(*) FILTER (WHERE status = 'SENT') as sent_notifications,
        COUNT(*) FILTER (WHERE status = 'DELIVERED') as delivered_notifications,
        COUNT(*) FILTER (WHERE status = 'READ') as read_notifications,
        COUNT(*) FILTER (WHERE status = 'FAILED') as failed_notifications,
        CASE 
            WHEN COUNT(*) > 0 THEN 
                ROUND((COUNT(*) FILTER (WHERE status = 'DELIVERED')::DECIMAL / COUNT(*)::DECIMAL) * 100, 2)
            ELSE 0 
        END as delivery_rate,
        CASE 
            WHEN COUNT(*) > 0 THEN 
                ROUND((COUNT(*) FILTER (WHERE status = 'READ')::DECIMAL / COUNT(*)::DECIMAL) * 100, 2)
            ELSE 0 
        END as read_rate
    FROM push_notifications
    WHERE 
        (p_date_range_start IS NULL OR DATE(created_at) >= p_date_range_start)
        AND (p_date_range_end IS NULL OR DATE(created_at) <= p_date_range_end);
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_push_device_tokens_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_push_device_tokens_updated_at BEFORE UPDATE ON push_device_tokens FOR EACH ROW EXECUTE FUNCTION update_push_device_tokens_updated_at();

CREATE OR REPLACE FUNCTION update_push_notifications_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_push_notifications_updated_at BEFORE UPDATE ON push_notifications FOR EACH ROW EXECUTE FUNCTION update_push_notifications_updated_at();
