-- WhatsApp SPR Integration Tables
-- Auto-fill SPR data from WhatsApp Group messages

-- =====================================================
-- WHATSAPP MESSAGES TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS whatsapp_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id TEXT UNIQUE NOT NULL,
    sender_name TEXT NOT NULL,
    sender_phone TEXT NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    message_type TEXT DEFAULT 'text',
    group_id TEXT NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- SPR DATA TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS spr_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_name TEXT NOT NULL,
    customer_phone TEXT NOT NULL,
    customer_email TEXT,
    customer_nik TEXT,
    unit_type TEXT NOT NULL,
    block TEXT NOT NULL,
    unit_number TEXT NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    dp_amount DECIMAL(15,2),
    kpr_amount DECIMAL(15,2),
    bank_name TEXT,
    marketing_name TEXT,
    notes TEXT,
    source TEXT DEFAULT 'WHATSAPP_GROUP',
    status TEXT DEFAULT 'INACTIVE',
    whatsapp_message_id TEXT REFERENCES whatsapp_messages(message_id),
    user_id UUID REFERENCES user_profiles(id),
    dossier_id UUID REFERENCES kpr_dossiers(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- WHATSAPP GROUP CONFIGURATION
-- =====================================================

CREATE TABLE IF NOT EXISTS whatsapp_group_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id TEXT UNIQUE NOT NULL,
    group_name TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    auto_process BOOLEAN DEFAULT TRUE,
    last_processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- SPR PROCESSING LOG
-- =====================================================

CREATE TABLE IF NOT EXISTS spr_processing_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    whatsapp_message_id TEXT REFERENCES whatsapp_messages(message_id),
    spr_data_id UUID REFERENCES spr_data(id),
    processing_status TEXT NOT NULL, -- SUCCESS, FAILED, PENDING
    error_message TEXT,
    processing_time_ms INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_whatsapp_messages_group_id ON whatsapp_messages(group_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_messages_processed ON whatsapp_messages(processed);
CREATE INDEX IF NOT EXISTS idx_whatsapp_messages_timestamp ON whatsapp_messages(timestamp);
CREATE INDEX IF NOT EXISTS idx_spr_data_status ON spr_data(status);
CREATE INDEX IF NOT EXISTS idx_spr_data_source ON spr_data(source);
CREATE INDEX IF NOT EXISTS idx_spr_data_customer_phone ON spr_data(customer_phone);
CREATE INDEX IF NOT EXISTS idx_spr_data_whatsapp_message_id ON spr_data(whatsapp_message_id);
CREATE INDEX IF NOT EXISTS idx_spr_processing_log_status ON spr_processing_log(processing_status);

-- =====================================================
-- TRIGGERS FOR UPDATED_AT
-- =====================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_whatsapp_messages_updated_at 
    BEFORE UPDATE ON whatsapp_messages 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_spr_data_updated_at 
    BEFORE UPDATE ON spr_data 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_whatsapp_group_config_updated_at 
    BEFORE UPDATE ON whatsapp_group_config 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- RLS POLICIES
-- =====================================================

ALTER TABLE whatsapp_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE spr_data ENABLE ROW LEVEL SECURITY;
ALTER TABLE whatsapp_group_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE spr_processing_log ENABLE ROW LEVEL SECURITY;

-- WhatsApp Messages - Marketing can view all, others limited
CREATE POLICY "Marketing view all whatsapp messages" ON whatsapp_messages
    FOR SELECT USING (auth.jwt() ->> 'role' = 'MARKETING');

CREATE POLICY "Users view own whatsapp messages" ON whatsapp_messages
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'CUSTOMER' AND
        sender_phone IN (
            SELECT phone_number FROM user_profiles WHERE id = auth.uid()
        )
    );

-- SPR Data - Role-based access
CREATE POLICY "Marketing view all spr data" ON spr_data
    FOR SELECT USING (auth.jwt() ->> 'role' = 'MARKETING');

CREATE POLICY "Legal view all spr data" ON spr_data
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

CREATE POLICY "Finance view all spr data" ON spr_data
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

CREATE POLICY "BOD view all spr data" ON spr_data
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

CREATE POLICY "Users view own spr data" ON spr_data
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'CUSTOMER' AND
        user_id = auth.uid()
    );

-- WhatsApp Group Config - Staff only
CREATE POLICY "Staff manage whatsapp groups" ON whatsapp_group_config
    FOR ALL USING (
        auth.jwt() ->> 'role' IN ('MARKETING', 'LEGAL', 'FINANCE', 'BOD')
    );

-- Processing Log - Staff only
CREATE POLICY "Staff view processing log" ON spr_processing_log
    FOR SELECT USING (
        auth.jwt() ->> 'role' IN ('MARKETING', 'LEGAL', 'FINANCE', 'BOD')
    );

-- =====================================================
-- SAMPLE DATA
-- =====================================================

INSERT INTO whatsapp_group_config (group_id, group_name, is_active, auto_process)
VALUES 
    ('KPRFlow_Group_001', 'KPRFlow Customer Group', true, true),
    ('KPRFlow_Marketing_001', 'Internal Marketing Team', true, false)
ON CONFLICT (group_id) DO NOTHING;

-- =====================================================
-- FUNCTIONS FOR SPR PROCESSING
-- =====================================================

CREATE OR REPLACE FUNCTION process_whatsapp_spr(
    p_message_id TEXT,
    p_customer_name TEXT,
    p_customer_phone TEXT,
    p_unit_type TEXT,
    p_block TEXT,
    p_unit_number TEXT,
    p_price DECIMAL,
    p_dp_amount DECIMAL DEFAULT NULL,
    p_kpr_amount DECIMAL DEFAULT NULL,
    p_bank_name TEXT DEFAULT NULL,
    p_notes TEXT DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    v_spr_id UUID;
    v_processing_time_ms INTEGER;
    v_start_time TIMESTAMP WITH TIME ZONE;
BEGIN
    v_start_time = clock_timestamp();
    
    -- Insert SPR data
    INSERT INTO spr_data (
        customer_name, customer_phone, unit_type, block, unit_number,
        price, dp_amount, kpr_amount, bank_name, notes, whatsapp_message_id
    ) VALUES (
        p_customer_name, p_customer_phone, p_unit_type, p_block, p_unit_number,
        p_price, p_dp_amount, p_kpr_amount, p_bank_name, p_notes, p_message_id
    ) RETURNING id INTO v_spr_id;
    
    -- Mark message as processed
    UPDATE whatsapp_messages 
    SET processed = TRUE 
    WHERE message_id = p_message_id;
    
    -- Calculate processing time
    v_processing_time_ms = EXTRACT(MILLISECOND FROM (clock_timestamp() - v_start_time));
    
    -- Log processing
    INSERT INTO spr_processing_log (
        whatsapp_message_id, spr_data_id, processing_status, processing_time_ms
    ) VALUES (
        p_message_id, v_spr_id, 'SUCCESS', v_processing_time_ms
    );
    
    RETURN v_spr_id;
EXCEPTION
    WHEN OTHERS THEN
        -- Log error
        v_processing_time_ms = EXTRACT(MILLISECOND FROM (clock_timestamp() - v_start_time));
        
        INSERT INTO spr_processing_log (
            whatsapp_message_id, processing_status, error_message, processing_time_ms
        ) VALUES (
            p_message_id, 'FAILED', SQLERRM, v_processing_time_ms
        );
        
        RAISE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- VIEWS FOR REPORTING
-- =====================================================

CREATE OR REPLACE VIEW whatsapp_spr_summary AS
SELECT 
    DATE(wm.timestamp) as message_date,
    COUNT(*) as total_messages,
    COUNT(CASE WHEN wm.processed = TRUE THEN 1 END) as processed_messages,
    COUNT(CASE WHEN sd.id IS NOT NULL THEN 1 END) as spr_created,
    COUNT(CASE WHEN sd.status = 'INACTIVE' THEN 1 END) as inactive_spr,
    COUNT(CASE WHEN sd.status = 'ACTIVE' THEN 1 END) as active_spr
FROM whatsapp_messages wm
LEFT JOIN spr_data sd ON wm.message_id = sd.whatsapp_message_id
GROUP BY DATE(wm.timestamp)
ORDER BY message_date DESC;
