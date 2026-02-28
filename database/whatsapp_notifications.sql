-- WhatsApp Notifications System for Phase 11
-- KPRFlow Enterprise

-- Notification Logs Table
CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'lead_generated', 'status_change', 'document_uploaded', 'unit_cancelled', 'payment_reminder'
    channel VARCHAR(20) NOT NULL, -- 'whatsapp', 'email', 'sms', 'push'
    message_id VARCHAR(255), -- External message ID from WhatsApp gateway
    delivery_status VARCHAR(20) DEFAULT 'pending', -- 'pending', 'sent', 'delivered', 'read', 'failed'
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    data JSONB, -- Additional notification data
    reference_id UUID, -- Reference to related record (dossier_id, unit_id, etc.)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- WhatsApp Templates Table
CREATE TABLE IF NOT EXISTS whatsapp_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL, -- 'lead_generated', 'status_change', etc.
    template_text TEXT NOT NULL,
    variables JSONB, -- Template variables definition
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Notification Settings Table
CREATE TABLE IF NOT EXISTS notification_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    is_enabled BOOLEAN DEFAULT true,
    preferences JSONB, -- User-specific preferences
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    UNIQUE(user_id, notification_type, channel)
);

-- Insert default WhatsApp templates
INSERT INTO whatsapp_templates (name, type, template_text, variables) VALUES
('lead_generated', 'lead_generated', 
'🎉 *{{title}}*\n\n{{message}}\n\nUnit: {{unit_block}}\nCustomer: {{customer_name}}\n\nTerima kasih telah menggunakan KPRFlow Enterprise.\n\nHubungi kami untuk info lebih lanjut.',
'{"title": "string", "message": "string", "unit_block": "string", "customer_name": "string"}'),

('status_change', 'status_change',
'📋 *{{title}}*\n\n{{message}}\n\nStatus: {{new_status}}\n{{#previous_status}}Previous: {{previous_status}}{{/previous_status}}\n\nLogin ke dashboard untuk detail lengkap.\n\nKPRFlow Enterprise',
'{"title": "string", "message": "string", "new_status": "string", "previous_status": "string"}'),

('document_uploaded', 'document_uploaded',
'📄 *{{title}}*\n\n{{message}}\n\nDocument: {{document_type}}\nUploaded: {{upload_date}}\n\nDocument sedang diproses oleh tim kami.\n\nKPRFlow Enterprise',
'{"title": "string", "message": "string", "document_type": "string", "upload_date": "string"}'),

('unit_cancelled', 'unit_cancelled',
'⚠️ *{{title}}*\n\n{{message}}\n\nUnit: {{unit_block}}\nReason: {{cancellation_reason}}\n\nHubungi marketing untuk informasi lebih lanjut.\n\nKPRFlow Enterprise',
'{"title": "string", "message": "string", "unit_block": "string", "cancellation_reason": "string"}'),

('payment_reminder', 'payment_reminder',
'💰 *{{title}}*\n\n{{message}}\n\nAmount: {{amount}}\nDue Date: {{due_date}}\n\nSegera lakukan pembayaran untuk menghindari keterlambatan.\n\nKPRFlow Enterprise',
'{"title": "string", "message": "string", "amount": "string", "due_date": "string"}')
ON CONFLICT (name) DO NOTHING;

-- Function to send WhatsApp notification
CREATE OR REPLACE FUNCTION send_whatsapp_notification(
    p_user_id UUID DEFAULT NULL,
    p_phone_number VARCHAR DEFAULT NULL,
    p_title VARCHAR,
    p_message TEXT,
    p_type VARCHAR,
    p_data JSONB DEFAULT NULL,
    p_reference_id UUID DEFAULT NULL
) RETURNS TABLE (
    success BOOLEAN,
    message TEXT,
    notification_id UUID
) AS $$
DECLARE
    v_phone_number VARCHAR;
    v_notification_id UUID;
    v_result JSONB;
BEGIN
    -- Get phone number if user_id provided
    IF p_user_id IS NOT NULL THEN
        SELECT phone_number INTO v_phone_number
        FROM user_profiles
        WHERE id = p_user_id;
        
        IF v_phone_number IS NULL THEN
            RETURN QUERY SELECT false, 'User phone number not found', NULL::UUID;
            RETURN;
        END IF;
    ELSE
        v_phone_number := p_phone_number;
    END IF;
    
    -- Check if user has WhatsApp notifications enabled
    IF p_user_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM notification_settings 
            WHERE user_id = p_user_id 
            AND notification_type = p_type 
            AND channel = 'whatsapp' 
            AND is_enabled = true
        ) THEN
            RETURN QUERY SELECT false, 'WhatsApp notifications disabled for user', NULL::UUID;
            RETURN;
        END IF;
    END IF;
    
    -- Create notification log
    INSERT INTO notification_logs (
        user_id, title, message, type, channel, data, reference_id
    ) VALUES (
        p_user_id, p_title, p_message, p_type, 'whatsapp', p_data, p_reference_id
    ) RETURNING id INTO v_notification_id;
    
    -- Call WhatsApp Edge Function
    SELECT content INTO v_result
    FROM http_post(
        '${supabaseProjectUrl}/functions/v1/whatsapp-engine',
        json_build_object(
            'action', 'send_notification',
            'data', json_build_object(
                'userId', p_user_id,
                'title', p_title,
                'message', p_message,
                'type', p_type,
                'data', p_data,
                'referenceId', p_reference_id
            )
        ),
        array[
            ('Authorization', 'Bearer ${supabaseServiceKey}')::text,
            ('Content-Type', 'application/json')::text
        ]
    );
    
    -- Update notification log with result
    UPDATE notification_logs
    SET message_id = (v_result->>'messageId'),
        delivery_status = CASE 
            WHEN v_result->>'success' = 'true' THEN 'sent'
            ELSE 'failed'
        END,
        updated_at = NOW()
    WHERE id = v_notification_id;
    
    RETURN QUERY 
    SELECT 
        (v_result->>'success')::boolean,
        v_result->>'error',
        v_notification_id;
END;
$$ LANGUAGE plpgsql;

-- Function to get notification statistics
CREATE OR REPLACE FUNCTION get_notification_stats(
    p_start_date TIMESTAMP WITH TIME ZONE DEFAULT NOW() - INTERVAL '30 days',
    p_end_date TIMESTAMP WITH TIME ZONE DEFAULT NOW()
) RETURNS TABLE (
    channel VARCHAR,
    type VARCHAR,
    total_sent BIGINT,
    total_delivered BIGINT,
    total_read BIGINT,
    delivery_rate NUMERIC,
    read_rate NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        nl.channel,
        nl.type,
        COUNT(*) as total_sent,
        COUNT(*) FILTER (WHERE nl.delivery_status = 'delivered') as total_delivered,
        COUNT(*) FILTER (WHERE nl.delivery_status = 'read') as total_read,
        CASE 
            WHEN COUNT(*) > 0 THEN 
                ROUND(COUNT(*) FILTER (WHERE nl.delivery_status = 'delivered')::NUMERIC / COUNT(*) * 100, 2)
            ELSE 0
        END as delivery_rate,
        CASE 
            WHEN COUNT(*) > 0 THEN 
                ROUND(COUNT(*) FILTER (WHERE nl.delivery_status = 'read')::NUMERIC / COUNT(*) * 100, 2)
            ELSE 0
        END as read_rate
    FROM notification_logs nl
    WHERE nl.created_at BETWEEN p_start_date AND p_end_date
    GROUP BY nl.channel, nl.type
    ORDER BY nl.channel, nl.type;
END;
$$ LANGUAGE plpgsql;

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_notification_logs_user_id ON notification_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_logs_type ON notification_logs(type);
CREATE INDEX IF NOT EXISTS idx_notification_logs_channel ON notification_logs(channel);
CREATE INDEX IF NOT EXISTS idx_notification_logs_status ON notification_logs(delivery_status);
CREATE INDEX IF NOT EXISTS idx_notification_logs_created_at ON notification_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_logs_reference_id ON notification_logs(reference_id);

CREATE INDEX IF NOT EXISTS idx_notification_settings_user_id ON notification_settings(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_settings_type ON notification_settings(notification_type);

-- RLS Policies
ALTER TABLE notification_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_settings ENABLE ROW LEVEL SECURITY;

-- Notification Logs RLS
CREATE POLICY "Users can view their own notification logs" ON notification_logs
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Marketing can view all notification logs" ON notification_logs
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM user_profiles
            WHERE id = auth.uid() AND role = 'MARKETING'
        )
    );

CREATE POLICY "System can insert notification logs" ON notification_logs
    FOR INSERT WITH CHECK (true);

-- Notification Settings RLS
CREATE POLICY "Users can manage their own notification settings" ON notification_settings
    FOR ALL USING (auth.uid() = user_id);

-- Triggers for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_notification_logs_updated_at
    BEFORE UPDATE ON notification_logs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_notification_settings_updated_at
    BEFORE UPDATE ON notification_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_whatsapp_templates_updated_at
    BEFORE UPDATE ON whatsapp_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
