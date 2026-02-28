-- Atomic Transactions for KPRFlow Enterprise
-- PostgreSQL Functions (RPC) for Unit Swap and Quorum Approval
-- Ensures all operations succeed or all fail together

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- UNIT SWAP ATOMIC TRANSACTION FUNCTION
-- =====================================================

-- Function to perform atomic unit swap between two dossiers
CREATE OR REPLACE FUNCTION atomic_unit_swap(
    p_from_dossier_id UUID,
    p_to_dossier_id UUID,
    p_performed_by UUID,
    p_reason TEXT DEFAULT NULL
)
RETURNS TABLE(
    success BOOLEAN,
    message TEXT,
    from_dossier_id UUID,
    to_dossier_id UUID,
    swapped_unit_id UUID,
    transaction_id UUID
) AS $$
DECLARE
    v_from_dossier_status TEXT;
    v_to_dossier_status TEXT;
    v_from_unit_id UUID;
    v_to_unit_id UUID;
    v_unit_from_status TEXT;
    v_unit_to_status TEXT;
    v_transaction_id UUID := uuid_generate_v4();
    v_error_message TEXT;
BEGIN
    -- Start atomic transaction
    -- Lock both dossiers to prevent concurrent modifications
    SELECT status, unit_id INTO v_from_dossier_status, v_from_unit_id
    FROM kpr_dossiers
    WHERE id = p_from_dossier_id
    FOR UPDATE;
    
    SELECT status, unit_id INTO v_to_dossier_status, v_to_unit_id
    FROM kpr_dossiers
    WHERE id = p_to_dossier_id
    FOR UPDATE;
    
    -- Validate preconditions
    IF v_from_dossier_id IS NULL OR v_to_dossier_id IS NULL THEN
        v_error_message := 'One or both dossiers not found';
        RAISE EXCEPTION 'Dossier not found: %', v_error_message;
    END IF;
    
    IF v_from_unit_id IS NULL OR v_to_unit_id IS NULL THEN
        v_error_message := 'One or both dossiers have no assigned units';
        RAISE EXCEPTION 'No unit assigned: %', v_error_message;
    END IF;
    
    -- Check if both dossiers are in valid status for swapping
    IF v_from_dossier_status NOT IN ('PEMBERKASAN', 'PROSES_BANK', 'PUTUSAN_KREDIT_ACC') OR
       v_to_dossier_status NOT IN ('PEMBERKASAN', 'PROSES_BANK', 'PUTUSAN_KREDIT_ACC') THEN
        v_error_message := 'Invalid dossier status for swapping';
        RAISE EXCEPTION 'Invalid status: %', v_error_message;
    END IF;
    
    -- Lock both units to prevent concurrent modifications
    SELECT status INTO v_unit_from_status
    FROM unit_properties
    WHERE id = v_from_unit_id
    FOR UPDATE;
    
    SELECT status INTO v_unit_to_status
    FROM unit_properties
    WHERE id = v_to_unit_id
    FOR UPDATE;
    
    -- Check if both units are in valid status
    IF v_unit_from_status != 'BOOKED' OR v_unit_to_status != 'BOOKED' THEN
        v_error_message := 'Units must be in BOOKED status for swapping';
        RAISE EXCEPTION 'Invalid unit status: %', v_error_message;
    END IF;
    
    -- Perform atomic swap operations
    
    -- 1. Create audit trail entry
    INSERT INTO audit_trail (
        table_name,
        operation,
        record_id,
        old_values,
        new_values,
        performed_by,
        transaction_id,
        created_at
    ) VALUES
    ('unit_swap', 'SWAP', v_transaction_id, 
     json_build_object('from_dossier', p_from_dossier_id, 'to_dossier', p_to_dossier_id),
     json_build_object('from_unit', v_from_unit_id, 'to_unit', v_to_unit_id, 'reason', p_reason),
     p_performed_by, v_transaction_id, NOW());
    
    -- 2. Update first dossier - remove unit assignment
    UPDATE kpr_dossiers 
    SET unit_id = NULL, 
        updated_at = NOW(),
        status = 'PEMBERKASAN' -- Reset to document collection
    WHERE id = p_from_dossier_id;
    
    -- 3. Update second dossier - assign new unit
    UPDATE kpr_dossiers 
    SET unit_id = v_from_unit_id,
        updated_at = NOW()
    WHERE id = p_to_dossier_id;
    
    -- 4. Update first unit status back to AVAILABLE
    UPDATE unit_properties 
    SET status = 'AVAILABLE',
        updated_at = NOW()
    WHERE id = v_from_unit_id;
    
    -- 5. Update second unit status to reflect new assignment
    UPDATE unit_properties 
    SET status = 'BOOKED',
        updated_at = NOW()
    WHERE id = v_to_unit_id;
    
    -- 6. Create unit swap record
    INSERT INTO unit_swap_requests (
        from_dossier_id,
        to_dossier_id,
        from_unit_id,
        to_unit_id,
        status,
        requested_by,
        approved_by,
        approved_at,
        reason,
        transaction_id,
        created_at,
        updated_at
    ) VALUES (
        p_from_dossier_id,
        p_to_dossier_id,
        v_from_unit_id,
        v_to_unit_id,
        'COMPLETED',
        p_performed_by,
        p_performed_by,
        NOW(),
        p_reason,
        v_transaction_id,
        NOW(),
        NOW()
    );
    
    -- 7. Create notification for both customers
    INSERT INTO notifications (
        user_id,
        title,
        message,
        type,
        data,
        read_at,
        created_at
    ) SELECT 
        p_from_dossier_id,
        'Unit Swapped',
        'Your unit has been swapped with another application',
        'UNIT_SWAP',
        json_build_object('from_unit', v_from_unit_id, 'to_unit', v_to_unit_id, 'reason', p_reason),
        NULL,
        NOW()
    UNION ALL
    SELECT 
        p_to_dossier_id,
        'Unit Updated',
        'Your unit has been updated due to unit swap',
        'UNIT_SWAP',
        json_build_object('old_unit', v_to_unit_id, 'new_unit', v_from_unit_id, 'reason', p_reason),
        NULL,
        NOW();
    
    -- Return success
    RETURN QUERY SELECT true, 'Unit swap completed successfully', 
                        p_from_dossier_id, p_to_dossier_id, v_from_unit_id, v_transaction_id;
    
EXCEPTION
    WHEN OTHERS THEN
        -- Rollback is automatic in PostgreSQL functions
        RETURN QUERY SELECT false, SQLERRM, p_from_dossier_id, p_to_dossier_id, NULL, v_transaction_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- QUORUM APPROVAL ATOMIC TRANSACTION FUNCTION
-- =====================================================

-- Function to perform atomic quorum approval with 2:1 voting ratio
CREATE OR REPLACE FUNCTION atomic_quorum_approval(
    p_session_id UUID,
    p_performed_by UUID,
    p_auto_approve BOOLEAN DEFAULT FALSE
)
RETURNS TABLE(
    success BOOLEAN,
    message TEXT,
    session_id UUID,
    final_decision TEXT,
    approval_count INTEGER,
    rejection_count INTEGER,
    abstain_count INTEGER,
    total_voters INTEGER,
    transaction_id UUID
) AS $$
DECLARE
    v_session_status TEXT;
    v_voting_type TEXT;
    v_required_voters INTEGER;
    v_approve_count INTEGER := 0;
    v_reject_count INTEGER := 0;
    v_abstain_count INTEGER := 0;
    v_total_voters INTEGER := 0;
    v_final_decision TEXT;
    v_transaction_id UUID := uuid_generate_v4();
    v_dossier_id UUID;
    v_error_message TEXT;
BEGIN
    -- Lock the voting session to prevent concurrent modifications
    SELECT status, voting_type, dossier_id INTO v_session_status, v_voting_type, v_dossier_id
    FROM quorum_voting_sessions
    WHERE id = p_session_id
    FOR UPDATE;
    
    -- Validate session
    IF v_session_id IS NULL THEN
        v_error_message := 'Voting session not found';
        RAISE EXCEPTION 'Session not found: %', v_error_message;
    END IF;
    
    IF v_session_status != 'ACTIVE' THEN
        v_error_message := 'Voting session is not active';
        RAISE EXCEPTION 'Session not active: %', v_error_message;
    END IF;
    
    -- Check if session has expired
    IF EXISTS (
        SELECT 1 FROM quorum_voting_sessions 
        WHERE id = p_session_id 
        AND expires_at < NOW()
    ) THEN
        -- Mark session as expired
        UPDATE quorum_voting_sessions
        SET status = 'EXPIRED',
            concluded_by = p_performed_by,
            concluded_at = NOW(),
            conclusion_reason = 'Session expired automatically',
            updated_at = NOW()
        WHERE id = p_session_id;
        
        v_error_message := 'Voting session has expired';
        RAISE EXCEPTION 'Session expired: %', v_error_message;
    END IF;
    
    -- Count current votes
    SELECT 
        COUNT(CASE WHEN vote = 'APPROVE' THEN 1 END),
        COUNT(CASE WHEN vote = 'REJECT' THEN 1 END),
        COUNT(CASE WHEN vote = 'ABSTAIN' THEN 1 END),
        COUNT(*)
    INTO v_approve_count, v_reject_count, v_abstain_count, v_total_voters
    FROM quorum_votes
    WHERE session_id = p_session_id;
    
    -- Determine required voters based on voting type
    v_required_voters := CASE 
        WHEN v_voting_type = 'DOCUMENT_APPROVAL' THEN 3
        WHEN v_voting_type = 'CREDIT_DECISION' THEN 3
        WHEN v_voting_type = 'EXCEPTION_HANDLING' THEN 5
        ELSE 3
    END;
    
    -- Check if we have enough votes for decision
    IF v_total_voters < v_required_voters AND NOT p_auto_approve THEN
        v_error_message := 'Insufficient votes for decision';
        RAISE EXCEPTION 'Insufficient votes: %', v_error_message;
    END IF;
    
    -- Determine final decision based on 2:1 ratio
    -- Approval requires at least 2:1 ratio of approve to reject
    IF v_approve_count >= (v_reject_count * 2) THEN
        v_final_decision := 'APPROVED';
    ELSIF v_reject_count >= (v_approve_count * 2) THEN
        v_final_decision := 'REJECTED';
    ELSE
        v_final_decision := 'NO_DECISION';
    END IF;
    
    -- Only proceed if we have a clear decision or auto-approval is enabled
    IF v_final_decision != 'NO_DECISION' OR p_auto_approve THEN
        -- Start atomic approval operations
        
        -- 1. Create audit trail entry
        INSERT INTO audit_trail (
            table_name,
            operation,
            record_id,
            old_values,
            new_values,
            performed_by,
            transaction_id,
            created_at
        ) VALUES
        ('quorum_voting_sessions', 'CONCLUDE', p_session_id,
         json_build_object('status', v_session_status, 'decision', v_final_decision),
         json_build_object('final_decision', v_final_decision, 'auto_approve', p_auto_approve),
         p_performed_by, v_transaction_id, NOW());
        
        -- 2. Update voting session status
        UPDATE quorum_voting_sessions
        SET status = 'CONCLUDED',
            concluded_by = p_performed_by,
            concluded_at = NOW(),
            conclusion_reason = CASE 
                WHEN v_final_decision = 'APPROVED' THEN 'Quorum approval achieved'
                WHEN v_final_decision = 'REJECTED' THEN 'Quorum rejection achieved'
                WHEN p_auto_approve THEN 'Auto-approved due to insufficient votes'
                ELSE 'No clear decision reached'
            END,
            updated_at = NOW()
        WHERE id = p_session_id;
        
        -- 3. If approved, update related dossier status
        IF v_final_decision = 'APPROVED' AND v_dossier_id IS NOT NULL THEN
            UPDATE kpr_dossiers
            SET status = CASE 
                WHEN v_voting_type = 'DOCUMENT_APPROVAL' THEN 'PROSES_BANK'
                WHEN v_voting_type = 'CREDIT_DECISION' THEN 'PUTUSAN_KREDIT_ACC'
                WHEN v_voting_type = 'EXCEPTION_HANDLING' THEN 'SP3K_TERBIT'
                ELSE status
            END,
            updated_at = NOW()
            WHERE id = v_dossier_id;
            
            -- 4. Create notification for dossier owner
            INSERT INTO notifications (
                user_id,
                title,
                message,
                type,
                data,
                read_at,
                created_at
            ) SELECT 
                user_id,
                'Quorum Approval Completed',
                format('Your application has been %s by quorum voting', 
                       CASE WHEN v_final_decision = 'APPROVED' THEN 'approved' ELSE 'rejected' END),
                'QUORUM_DECISION',
                json_build_object('session_id', p_session_id, 'decision', v_final_decision, 'voting_type', v_voting_type),
                NULL,
                NOW()
            FROM kpr_dossiers
            WHERE id = v_dossier_id;
        END IF;
        
        -- 5. Create notifications for all voters
        INSERT INTO notifications (
            user_id,
            title,
            message,
            type,
            data,
            read_at,
            created_at
        ) SELECT 
            voter_id,
            'Quorum Voting Concluded',
            format('Voting session has been %s', 
                   CASE WHEN v_final_decision = 'APPROVED' THEN 'approved' 
                        WHEN v_final_decision = 'REJECTED' THEN 'rejected'
                        ELSE 'concluded with no decision' END),
            'QUORUM_DECISION',
            json_build_object('session_id', p_session_id, 'decision', v_final_decision, 'final_vote_counts', 
                             json_build_object('approve', v_approve_count, 'reject', v_reject_count, 'abstain', v_abstain_count)),
            NULL,
            NOW()
        FROM quorum_votes
        WHERE session_id = p_session_id;
        
        -- Return success
        RETURN QUERY SELECT true, 
                            format('Quorum %s successfully', v_final_decision),
                            p_session_id, v_final_decision,
                            v_approve_count, v_reject_count, v_abstain_count,
                            v_total_voters, v_transaction_id;
    ELSE
        -- No clear decision reached
        RETURN QUERY SELECT false, 
                            'No clear decision reached (insufficient 2:1 ratio)',
                            p_session_id, 'NO_DECISION',
                            v_approve_count, v_reject_count, v_abstain_count,
                            v_total_voters, v_transaction_id;
    END IF;
    
EXCEPTION
    WHEN OTHERS THEN
        -- Rollback is automatic in PostgreSQL functions
        RETURN QUERY SELECT false, SQLERRM, p_session_id, 'ERROR', 0, 0, 0, 0, v_transaction_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- BATCH UNIT STATUS UPDATE ATOMIC FUNCTION
-- =====================================================

-- Function to atomically update multiple unit statuses
CREATE OR REPLACE FUNCTION atomic_batch_unit_status_update(
    p_unit_ids UUID[],
    p_new_status unit_status,
    p_performed_by UUID,
    p_reason TEXT DEFAULT NULL
)
RETURNS TABLE(
    success BOOLEAN,
    message TEXT,
    updated_count INTEGER,
    failed_count INTEGER,
    transaction_id UUID
) AS $$
DECLARE
    v_updated_count INTEGER := 0;
    v_failed_count INTEGER := 0;
    v_transaction_id UUID := uuid_generate_v4();
    v_current_status unit_status;
    v_error_details TEXT[];
BEGIN
    -- Validate input
    IF p_unit_ids IS NULL OR array_length(p_unit_ids) = 0 THEN
        RETURN QUERY SELECT false, 'No unit IDs provided', 0, 0, v_transaction_id;
    END IF;
    
    -- Process each unit in atomic transaction
    FOREACH v_unit_id IN ARRAY p_unit_ids
    LOOP
        BEGIN
            -- Lock unit for update
            SELECT status INTO v_current_status
            FROM unit_properties
            WHERE id = v_unit_id
            FOR UPDATE;
            
            -- Validate status transition
            IF NOT is_valid_unit_status_transition(v_current_status, p_new_status) THEN
                v_failed_count := v_failed_count + 1;
                v_error_details := array_append(v_error_details, 
                    format('Unit %: Invalid transition from % to %', 
                           v_unit_id, v_current_status, p_new_status));
            ELSE
                -- Update unit status
                UPDATE unit_properties
                SET status = p_new_status,
                    updated_at = NOW()
                WHERE id = v_unit_id;
                
                v_updated_count := v_updated_count + 1;
                
                -- Create audit trail
                INSERT INTO audit_trail (
                    table_name,
                    operation,
                    record_id,
                    old_values,
                    new_values,
                    performed_by,
                    transaction_id,
                    created_at
                ) VALUES
                ('unit_properties', 'UPDATE', v_unit_id,
                 json_build_object('status', v_current_status),
                 json_build_object('status', p_new_status, 'reason', p_reason),
                 p_performed_by, v_transaction_id, NOW());
            END IF;
            
        EXCEPTION
            WHEN OTHERS THEN
                v_failed_count := v_failed_count + 1;
                v_error_details := array_append(v_error_details, 
                    format('Unit %: Error - %', v_unit_id, SQLERRM));
        END;
    END LOOP;
    
    -- Return results
    RETURN QUERY SELECT 
        CASE WHEN v_failed_count = 0 THEN true ELSE false END,
        CASE 
            WHEN v_failed_count = 0 THEN format('All % units updated successfully', v_updated_count)
            WHEN v_updated_count = 0 THEN format('All % units failed to update', v_failed_count)
            ELSE format('% updated, % failed', v_updated_count, v_failed_count)
        END,
        v_updated_count, v_failed_count, v_transaction_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- HELPER FUNCTIONS
-- =====================================================

-- Function to validate unit status transitions
CREATE OR REPLACE FUNCTION is_valid_unit_status_transition(
    p_current_status unit_status,
    p_new_status unit_status
) RETURNS BOOLEAN AS $$
BEGIN
    -- Define valid transitions
    CASE p_current_status
        WHEN 'AVAILABLE' THEN
            RETURN p_new_status IN ('BOOKED', 'LOCKED', 'SOLD');
        WHEN 'BOOKED' THEN
            RETURN p_new_status IN ('AVAILABLE', 'LOCKED', 'SOLD');
        WHEN 'LOCKED' THEN
            RETURN p_new_status IN ('AVAILABLE', 'BOOKED', 'SOLD');
        WHEN 'SOLD' THEN
            RETURN FALSE; -- Sold units cannot change status
        ELSE
            RETURN FALSE;
    END CASE;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- USAGE EXAMPLES
-- =====================================================

-- Example 1: Unit Swap between two dossiers
-- SELECT * FROM atomic_unit_swap(
--     'dossier-1-uuid', 
--     'dossier-2-uuid', 
--     'admin-uuid', 
--     'Customer request for better unit location'
-- );

-- Example 2: Quorum approval
-- SELECT * FROM atomic_quorum_approval(
--     'session-uuid', 
--     'admin-uuid', 
--     false
-- );

-- Example 3: Batch unit status update
-- SELECT * FROM atomic_batch_unit_status_update(
--     ARRAY['unit-1-uuid', 'unit-2-uuid', 'unit-3-uuid'],
--     'LOCKED'::unit_status,
--     'admin-uuid',
--     'Bulk lock for maintenance'
-- );

-- =====================================================
-- PERFORMANCE CONSIDERATIONS
-- =====================================================

-- All functions use FOR UPDATE locking to prevent race conditions
-- Indexes should be created on frequently queried columns
-- Consider using connection pooling for high-volume operations
-- Monitor function execution times and optimize as needed
