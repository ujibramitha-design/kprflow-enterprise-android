-- Cancellation Functions for Phase 10
-- KPRFlow Enterprise

-- Cancellation Reason Enum
CREATE TYPE cancellation_reason AS ENUM (
    'DATA_TIDAK_VALID',
    'CUSTOMER_MUNDUR',
    'REJECT_BANK',
    'DOKUMEN_TIDAK_LENGKAP',
    'SYARAT_TIDAK_MEMENUHI',
    'DUPLIKASI_PESANAN',
    'KESALAHAN_SISTEM',
    'LAINNYA'
);

-- Add cancellation columns to kpr_dossiers
ALTER TABLE kpr_dossiers 
ADD COLUMN IF NOT EXISTS cancellation_reason cancellation_reason,
ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS cancelled_by UUID REFERENCES user_profiles(id);

-- Add cancellation columns to unit_properties
ALTER TABLE unit_properties 
ADD COLUMN IF NOT EXISTS locked_by UUID REFERENCES user_profiles(id),
ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP WITH TIME ZONE;

-- Main Cancellation Function
CREATE OR REPLACE FUNCTION cancel_kpr_application(
    p_kpr_id UUID,
    p_reason cancellation_reason,
    p_admin_id UUID,
    p_additional_notes TEXT DEFAULT NULL
) RETURNS TABLE (
    success BOOLEAN,
    message TEXT,
    unit_id UUID,
    previous_status kpr_status
) AS $$
DECLARE
    v_unit_id UUID;
    v_previous_status kpr_status;
    v_customer_id UUID;
    v_transaction_count INTEGER;
BEGIN
    -- Get current KPR data
    SELECT unit_id, status, user_id INTO v_unit_id, v_previous_status, v_customer_id
    FROM kpr_dossiers
    WHERE id = p_kpr_id;
    
    -- Check if KPR exists
    IF NOT FOUND THEN
        RETURN QUERY SELECT false, 'KPR application not found', NULL::UUID, NULL::kpr_status;
        RETURN;
    END IF;
    
    -- Check if already cancelled
    IF v_previous_status = 'CANCELLED' THEN
        RETURN QUERY SELECT false, 'KPR application already cancelled', v_unit_id, v_previous_status;
        RETURN;
    END IF;
    
    -- Check if can be cancelled (not after BAST)
    IF v_previous_status = 'BAST_COMPLETED' THEN
        RETURN QUERY SELECT false, 'Cannot cancel completed BAST', v_unit_id, v_previous_status;
        RETURN;
    END IF;
    
    -- Begin transaction
    BEGIN
        -- 1. Update KPR status
        UPDATE kpr_dossiers 
        SET 
            status = 'CANCELLED',
            cancellation_reason = p_reason,
            cancelled_at = NOW(),
            cancelled_by = p_admin_id,
            notes = COALESCE(notes, '') || 
                    CASE WHEN p_additional_notes IS NOT NULL THEN 
                        '\nCancellation: ' || p_additional_notes 
                    ELSE '' END,
            updated_at = NOW()
        WHERE id = p_kpr_id;
        
        -- 2. Release unit if locked
        IF v_unit_id IS NOT NULL THEN
            UPDATE unit_properties 
            SET 
                status = 'AVAILABLE',
                locked_by = NULL,
                locked_at = NULL,
                updated_at = NOW()
            WHERE id = v_unit_id;
        END IF;
        
        -- 3. Void pending financial transactions
        UPDATE financial_transactions 
        SET 
            status = 'VOID',
            notes = COALESCE(notes, '') || '\nVoided due to KPR cancellation',
            updated_at = NOW()
        WHERE dossier_id = p_kpr_id 
        AND status = 'PENDING';
        
        GET DIAGNOSTICS v_transaction_count = ROW_COUNT;
        
        -- 4. Insert audit log
        INSERT INTO audit_log (
            table_name,
            record_id,
            action,
            old_values,
            new_values,
            user_id,
            metadata,
            created_at
        ) VALUES (
            'kpr_dossiers',
            p_kpr_id,
            'CANCEL',
            json_build_object('status', v_previous_status, 'unit_status', 'LOCKED'),
            json_build_object('status', 'CANCELLED', 'cancellation_reason', p_reason, 'unit_status', 'AVAILABLE'),
            p_admin_id,
            json_build_object(
                'cancellation_reason', p_reason,
                'additional_notes', p_additional_notes,
                'customer_id', v_customer_id,
                'unit_id', v_unit_id,
                'transactions_voided', v_transaction_count
            ),
            NOW()
        );
        
        -- 5. Create notification for marketing team
        INSERT INTO notifications (
            user_id,
            title,
            message,
            type,
            data,
            reference_id,
            created_at
        ) 
        SELECT 
            up.id,
            'Unit Dibatalkan',
            format('Unit %s telah dibatalkan dan kembali tersedia', up.block || '/' || up.unit_number),
            'unit_cancelled',
            json_build_object(
                'kpr_id', p_kpr_id,
                'unit_id', v_unit_id,
                'reason', p_reason,
                'cancelled_by', p_admin_id
            ),
            v_unit_id,
            NOW()
        FROM user_profiles up
        WHERE up.role = 'MARKETING' 
        AND up.is_active = true;
        
        -- Success
        RETURN QUERY 
        SELECT 
            true, 
            format('KPR %s cancelled successfully. Unit %s released.', p_kpr_id, v_unit_id),
            v_unit_id,
            v_previous_status;
        
        COMMIT;
        
    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            RETURN QUERY 
            SELECT 
                false, 
                'Cancellation failed: ' || SQLERRM,
                v_unit_id,
                v_previous_status;
    END;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Helper function to check if KPR can be cancelled
CREATE OR REPLACE FUNCTION can_cancel_kpr(p_kpr_id UUID) RETURNS BOOLEAN AS $$
DECLARE
    v_status kpr_status;
BEGIN
    SELECT status INTO v_status
    FROM kpr_dossiers
    WHERE id = p_kpr_id;
    
    RETURN v_status NOT IN ('CANCELLED', 'BAST_COMPLETED');
END;
$$ LANGUAGE plpgsql;

-- Function to get cancellation history
CREATE OR REPLACE FUNCTION get_cancellation_history(p_unit_id UUID DEFAULT NULL) 
RETURNS TABLE (
    kpr_id UUID,
    customer_name TEXT,
    unit_block TEXT,
    unit_number TEXT,
    cancellation_reason cancellation_reason,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by_name TEXT,
    previous_status kpr_status
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        kd.id,
        up.name,
        up_block.block,
        up_block.unit_number,
        kd.cancellation_reason,
        kd.cancelled_at,
        admin.name,
        kd.previous_status
    FROM kpr_dossiers kd
    JOIN user_profiles up ON kd.user_id = up.id
    JOIN unit_properties up_block ON kd.unit_id = up_block.id
    LEFT JOIN user_profiles admin ON kd.cancelled_by = admin.id
    WHERE kd.status = 'CANCELLED'
    AND (p_unit_id IS NULL OR kd.unit_id = p_unit_id)
    ORDER BY kd.cancelled_at DESC;
END;
$$ LANGUAGE plpgsql;

-- RLS Policies for cancellation functions
GRANT EXECUTE ON FUNCTION cancel_kpr_application TO authenticated;
GRANT EXECUTE ON FUNCTION can_cancel_kpr TO authenticated;
GRANT EXECUTE ON FUNCTION get_cancellation_history TO authenticated;

-- RLS Policy for cancelled KPRs
CREATE POLICY "Marketing can view cancelled KPRs" ON kpr_dossiers
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM user_profiles 
            WHERE id = auth.uid() AND role = 'MARKETING'
        ) AND status = 'CANCELLED'
    );

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_cancelled_at ON kpr_dossiers(cancelled_at) WHERE cancelled_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_cancellation_reason ON kpr_dossiers(cancellation_reason) WHERE cancellation_reason IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_unit_properties_locked_by ON unit_properties(locked_by) WHERE locked_by IS NOT NULL;
