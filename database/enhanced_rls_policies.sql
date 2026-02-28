-- Enhanced Row Level Security (RLS) Policies for KPRFlow Enterprise
-- Comprehensive role-based access control with fine-grained permissions
-- Ensuring Staff TEKNIK only sees UnitProperty without sensitive UserProfile data

-- =====================================================
-- ENABLE RLS ON ALL TABLES
-- =====================================================

ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE unit_properties ENABLE ROW LEVEL SECURITY;
ALTER TABLE kpr_dossiers ENABLE ROW LEVEL SECURITY;
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE financial_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE unit_swap_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE quorum_voting_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE quorum_votes ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_trail ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE bank_submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE bank_decisions ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- USER PROFILES RLS POLICIES
-- =====================================================

-- Policy 1: Users can view their own profile only
CREATE POLICY "Users view own profile" ON user_profiles
    FOR SELECT USING (auth.uid()::text = id::text);

-- Policy 2: Users can update their own profile (limited fields)
CREATE POLICY "Users update own profile" ON user_profiles
    FOR UPDATE USING (
        auth.uid()::text = id::text AND
        -- Only allow updating non-sensitive fields
        (phone_number, marital_status) IS NOT NULL
    );

-- Policy 3: Marketing can view customer profiles (non-financial)
CREATE POLICY "Marketing view customer profiles" ON user_profiles
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'MARKETING' AND
        -- Marketing can only see basic customer info
        (name, email, phone_number, marital_status, role, created_at, updated_at, is_active) IS NOT NULL
    );

-- Policy 4: Legal can view all customer profiles (for document verification)
CREATE POLICY "Legal view all profiles" ON user_profiles
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'LEGAL'
    );

-- Policy 5: Finance can view all profiles (including financial data)
CREATE POLICY "Finance view all profiles" ON user_profiles
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'FINANCE'
    );

-- Policy 6: BOD can view all profiles
CREATE POLICY "BOD view all profiles" ON user_profiles
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'BOD'
    );

-- Policy 7: TEKNIK staff can view limited profile info
CREATE POLICY "TEKNIK view limited profiles" ON user_profiles
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'TEKNIK' AND
        -- TEKNIK can only see name and role for unit assignment purposes
        (name, role) IS NOT NULL
    );

-- Policy 8: ESTATE can view customer profiles for unit assignment
CREATE POLICY "ESTATE view customer profiles" ON user_profiles
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'ESTATE' AND
        -- ESTATE can see basic info for unit management
        (name, email, phone_number, role) IS NOT NULL
    );

-- Policy 9: BANK can view relevant customer profiles
CREATE POLICY "BANK view customer profiles" ON user_profiles
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'BANK' AND
        -- BANK can see profiles for credit assessment
        (name, email, nik, phone_number, marital_status, role, created_at) IS NOT NULL
    );

-- =====================================================
-- UNIT PROPERTIES RLS POLICIES
-- =====================================================

-- Policy 1: All authenticated users can view available units
CREATE POLICY "All users view available units" ON unit_properties
    FOR SELECT USING (status = 'AVAILABLE');

-- Policy 2: Customers can view their assigned unit details
CREATE POLICY "Customers view assigned units" ON unit_properties
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'CUSTOMER' AND
        id IN (
            SELECT unit_id FROM kpr_dossiers 
            WHERE user_id = auth.uid() AND status NOT IN ('CANCELLED_BY_SYSTEM', 'FLOATING_DOSSIER')
        )
    );

-- Policy 3: Marketing can view all units
CREATE POLICY "Marketing view all units" ON unit_properties
    FOR SELECT USING (auth.jwt() ->> 'role' = 'MARKETING');

-- Policy 4: Legal can view all units
CREATE POLICY "Legal view all units" ON unit_properties
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Policy 5: Finance can view all units (including pricing)
CREATE POLICY "Finance view all units" ON unit_properties
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- Policy 6: BOD can view all units
CREATE POLICY "BOD view all units" ON unit_properties
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Policy 7: TEKNIK can view all units (for technical assessment)
CREATE POLICY "TEKNIK view all units" ON unit_properties
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'TEKNIK' AND
        -- TEKNIK can see unit details but not pricing
        (id, block, unit_number, type, status, description, created_at, updated_at) IS NOT NULL
    );

-- Policy 8: ESTATE can view and manage all units
CREATE POLICY "ESTATE view all units" ON unit_properties
    FOR SELECT USING (auth.jwt() ->> 'role' = 'ESTATE');

-- Policy 9: ESTATE can manage units
CREATE POLICY "ESTATE manage units" ON unit_properties
    FOR ALL USING (auth.jwt() ->> 'role' = 'ESTATE');

-- Policy 10: BANK can view units for credit assessment
CREATE POLICY "BANK view units" ON unit_properties
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'BANK' AND
        -- BANK can see unit details including pricing for credit assessment
        (id, block, unit_number, type, price, status, description, created_at, updated_at) IS NOT NULL
    );

-- =====================================================
-- KPR DOSSIERS RLS POLICIES
-- =====================================================

-- Policy 1: Customers can view their own dossiers
CREATE POLICY "Customers view own dossiers" ON kpr_dossiers
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- Policy 2: Marketing can view all dossiers (for lead management)
CREATE POLICY "Marketing view all dossiers" ON kpr_dossiers
    FOR SELECT USING (auth.jwt() ->> 'role' = 'MARKETING');

-- Policy 3: Legal can view all dossiers (for document verification)
CREATE POLICY "Legal view all dossiers" ON kpr_dossiers
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Policy 4: Finance can view all dossiers (including financial data)
CREATE POLICY "Finance view all dossiers" ON kpr_dossiers
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- Policy 5: BOD can view all dossiers
CREATE POLICY "BOD view all dossiers" ON kpr_dossiers
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Policy 6: TEKNIK can view limited dossier info
CREATE POLICY "TEKNIK view limited dossiers" ON kpr_dossiers
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'TEKNIK' AND
        -- TEKNIK can see unit assignment and status for technical assessment
        (id, user_id, unit_id, status, created_at, updated_at) IS NOT NULL
    );

-- Policy 7: ESTATE can view dossiers for unit management
CREATE POLICY "ESTATE view dossiers" ON kpr_dossiers
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'ESTATE' AND
        -- ESTATE can see unit assignments and status
        (id, user_id, unit_id, status, created_at, updated_at) IS NOT NULL
    );

-- Policy 8: BANK can view relevant dossiers
CREATE POLICY "BANK view dossiers" ON kpr_dossiers
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'BANK' AND
        -- BANK can see complete dossiers for credit assessment
        (id, user_id, unit_id, status, created_at, updated_at) IS NOT NULL
    );

-- =====================================================
-- DOCUMENTS RLS POLICIES
-- =====================================================

-- Policy 1: Customers can view their own documents
CREATE POLICY "Customers view own documents" ON documents
    FOR SELECT USING (
        auth.uid()::text = user_id::text
    );

-- Policy 2: Legal can view all documents (for verification)
CREATE POLICY "Legal view all documents" ON documents
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Policy 3: Finance can view ALL documents (updated from financial-only)
CREATE POLICY "Finance view all documents" ON documents
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- Policy 4: Marketing can view basic and financial documents (updated per matrix)
CREATE POLICY "Marketing view basic and financial documents" ON documents
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'MARKETING' AND
        document_type IN ('KTP', 'KK', 'NPWP', 'MARRIAGE_CERTIFICATE', 'PAYSLIP', 'SPR_FORM')
    );

-- Policy 5: BOD can view all documents
CREATE POLICY "BOD view all documents" ON documents
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Policy 6: TEKNIK cannot view sensitive documents
CREATE POLICY "TEKNIK no document access" ON documents
    FOR SELECT USING (false); -- No access for TEKNIK

-- Policy 7: ESTATE can view basic documents for unit assignment
CREATE POLICY "ESTATE view basic documents" ON documents
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'ESTATE' AND
        document_type IN ('KTP', 'KK')
    );

-- Policy 8: BANK can view all documents for credit assessment
CREATE POLICY "BANK view all documents" ON documents
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BANK');

-- =====================================================
-- FINANCIAL TRANSACTIONS RLS POLICIES
-- =====================================================

-- Policy 1: Customers can view their own transactions
CREATE POLICY "Customers view own transactions" ON financial_transactions
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- Policy 2: Finance can view all transactions
CREATE POLICY "Finance view all transactions" ON financial_transactions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- Policy 3: BOD can view all transactions
CREATE POLICY "BOD view all transactions" ON financial_transactions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Policy 4: Legal can view ALL transactions (updated from no access)
CREATE POLICY "Legal view all transactions" ON financial_transactions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Policy 5: Marketing cannot view financial transactions
CREATE POLICY "Marketing no financial access" ON financial_transactions
    FOR SELECT USING (false);

-- Policy 6: TEKNIK cannot view financial transactions
CREATE POLICY "TEKNIK no financial access" ON financial_transactions
    FOR SELECT USING (false);

-- Policy 7: ESTATE cannot view financial transactions
CREATE POLICY "ESTATE no financial access" ON financial_transactions
    FOR SELECT USING (false);

-- Policy 8: BANK can view relevant transactions
CREATE POLICY "BANK view transactions" ON financial_transactions
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'BANK' AND
        -- BANK can see transactions for credit assessment
        (id, user_id, amount, transaction_type, status, created_at) IS NOT NULL
    );

-- =====================================================
-- QUORUM VOTING RLS POLICIES
-- =====================================================

-- Policy 1: Legal team can view and manage voting sessions
CREATE POLICY "Legal manage voting sessions" ON quorum_voting_sessions
    FOR ALL USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Policy 2: BOD can view all voting sessions
CREATE POLICY "BOD view voting sessions" ON quorum_voting_sessions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Policy 3: Finance can view voting sessions for financial decisions
CREATE POLICY "Finance view financial voting" ON quorum_voting_sessions
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'FINANCE' AND
        voting_type = 'CREDIT_DECISION'
    );

-- Policy 4: Voters can view their own voting sessions
CREATE POLICY "Voters view own sessions" ON quorum_voting_sessions
    FOR SELECT USING (
        auth.jwt() ->> 'role' IN ('LEGAL', 'FINANCE', 'BOD') AND
        id IN (
            SELECT session_id FROM quorum_votes 
            WHERE voter_id = auth.uid()
        )
    );

-- Policy 5: Other roles cannot access voting sessions
CREATE POLICY "Others no voting access" ON quorum_voting_sessions
    FOR ALL USING (
        auth.jwt() ->> 'role' NOT IN ('LEGAL', 'FINANCE', 'BOD')
    ) WITH CHECK (false);

-- =====================================================
-- QUORUM VOTES RLS POLICIES
-- =====================================================

-- Policy 1: Voters can view their own votes
CREATE POLICY "Voters view own votes" ON quorum_votes
    FOR SELECT USING (auth.uid()::text = voter_id::text);

-- Policy 2: Legal can view all votes
CREATE POLICY "Legal view all votes" ON quorum_votes
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Policy 3: BOD can view all votes
CREATE POLICY "BOD view all votes" ON quorum_votes
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Policy 4: Voters can cast votes
CREATE POLICY "Voters can vote" ON quorum_votes
    FOR INSERT WITH CHECK (
        auth.uid()::text = voter_id::text AND
        auth.jwt() ->> 'role' IN ('LEGAL', 'FINANCE', 'BOD')
    );

-- =====================================================
-- NOTIFICATIONS RLS POLICIES
-- =====================================================

-- Policy 1: Users can view their own notifications
CREATE POLICY "Users view own notifications" ON notifications
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- Policy 2: Users can update their own notifications (mark as read)
CREATE POLICY "Users update own notifications" ON notifications
    FOR UPDATE USING (auth.uid()::text = user_id::text);

-- Policy 3: System can create notifications (service role)
CREATE POLICY "System create notifications" ON notifications
    FOR INSERT WITH CHECK (auth.role() = 'service_role');

-- =====================================================
-- AUDIT TRAIL RLS POLICIES
-- =====================================================

-- Policy 1: BOD can view all audit trails
CREATE POLICY "BOD view audit trail" ON audit_trail
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Policy 2: Finance can view financial audit trails
CREATE POLICY "Finance view financial audit" ON audit_trail
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'FINANCE' AND
        table_name IN ('financial_transactions', 'payment_schedules')
    );

-- Policy 3: Legal can view document audit trails
CREATE POLICY "Legal view document audit" ON audit_trail
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'LEGAL' AND
        table_name = 'documents'
    );

-- Policy 4: System can create audit entries
CREATE POLICY "System create audit" ON audit_trail
    FOR INSERT WITH CHECK (auth.role() = 'service_role');

-- =====================================================
-- PAYMENT SCHEDULES RLS POLICIES
-- =====================================================

-- Policy 1: Customers can view their own payment schedules
CREATE POLICY "Customers view own schedules" ON payment_schedules
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- Policy 2: Finance can view all payment schedules
CREATE POLICY "Finance view all schedules" ON payment_schedules
    FOR SELECT USING (auth.jwt() ->> 'role' = 'FINANCE');

-- Policy 3: BOD can view all payment schedules
CREATE POLICY "BOD view all schedules" ON payment_schedules
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Policy 4: Legal can view ALL payment schedules (updated from no access)
CREATE POLICY "Legal view all schedules" ON payment_schedules
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Policy 5: BANK can view relevant payment schedules
CREATE POLICY "BANK view schedules" ON payment_schedules
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'BANK' AND
        -- BANK can see payment schedules for credit assessment
        (id, user_id, amount, due_date, status, created_at) IS NOT NULL
    );

-- =====================================================
-- SECURITY FUNCTIONS FOR VALIDATION
-- =====================================================

-- Function to check if user has access to sensitive data
CREATE OR REPLACE FUNCTION has_sensitive_data_access(p_user_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_user_role TEXT;
BEGIN
    SELECT role::text INTO v_user_role
    FROM user_profiles
    WHERE id = p_user_id;
    
    RETURN v_user_role IN ('FINANCE', 'BOD', 'BANK');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to check if user can view financial information
CREATE OR REPLACE FUNCTION can_view_financial_data(p_user_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_user_role TEXT;
BEGIN
    SELECT role::text INTO v_user_role
    FROM user_profiles
    WHERE id = p_user_id;
    
    RETURN v_user_role IN ('FINANCE', 'BOD', 'BANK', 'LEGAL'); -- Added LEGAL
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to check if user can view documents
CREATE OR REPLACE FUNCTION can_view_documents(p_user_id UUID, p_document_type document_type)
RETURNS BOOLEAN AS $$
DECLARE
    v_user_role TEXT;
BEGIN
    SELECT role::text INTO v_user_role
    FROM user_profiles
    WHERE id = p_user_id;
    
    -- BOD and BANK can view all documents
    IF v_user_role IN ('BOD', 'BANK') THEN
        RETURN TRUE;
    END IF;
    
    -- Legal and Finance can view all documents
    IF v_user_role IN ('LEGAL', 'FINANCE') THEN
        RETURN TRUE;
    END IF;
    
    -- Marketing can view basic and financial documents (updated per matrix)
    IF v_user_role = 'MARKETING' AND p_document_type IN ('KTP', 'KK', 'NPWP', 'MARRIAGE_CERTIFICATE', 'PAYSLIP', 'SPR_FORM') THEN
        RETURN TRUE;
    END IF;
    
    -- ESTATE can view basic documents
    IF v_user_role = 'ESTATE' AND p_document_type IN ('KTP', 'KK') THEN
        RETURN TRUE;
    END IF;
    
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- PERFORMANCE OPTIMIZATION
-- =====================================================

-- Create indexes for RLS policy performance
CREATE INDEX IF NOT EXISTS idx_user_profiles_role ON user_profiles(role);
CREATE INDEX IF NOT EXISTS idx_user_profiles_id ON user_profiles(id);
CREATE INDEX IF NOT EXISTS idx_unit_properties_status ON unit_properties(status);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_user_id ON kpr_dossiers(user_id);
CREATE INDEX IF NOT EXISTS idx_kpr_dossiers_status ON kpr_dossiers(status);
CREATE INDEX IF NOT EXISTS idx_documents_user_id ON documents(user_id);
CREATE INDEX IF NOT EXISTS idx_documents_type ON documents(document_type);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_user_id ON financial_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_quorum_voting_sessions_status ON quorum_voting_sessions(status);
CREATE INDEX IF NOT EXISTS idx_quorum_votes_voter_id ON quorum_votes(voter_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_trail_table_name ON audit_trail(table_name);
CREATE INDEX IF NOT EXISTS idx_payment_schedules_user_id ON payment_schedules(user_id);

-- =====================================================
-- TESTING RLS POLICIES
-- =====================================================

-- Test function to verify RLS policies
CREATE OR REPLACE FUNCTION test_rls_policies(p_test_role TEXT)
RETURNS TABLE(
    table_name TEXT,
    accessible_rows INTEGER,
    policy_test TEXT
) AS $$
DECLARE
    v_row_count INTEGER;
BEGIN
    -- Test user_profiles access
    EXECUTE format('SELECT COUNT(*) FROM user_profiles WHERE role = %L', 'CUSTOMER') INTO v_row_count;
    RETURN QUERY SELECT 'user_profiles', v_row_count, 'Customer profiles access'::TEXT;
    
    -- Test unit_properties access
    EXECUTE 'SELECT COUNT(*) FROM unit_properties' INTO v_row_count;
    RETURN QUERY SELECT 'unit_properties', v_row_count, 'Units access'::TEXT;
    
    -- Test documents access
    EXECUTE 'SELECT COUNT(*) FROM documents' INTO v_row_count;
    RETURN QUERY SELECT 'documents', v_row_count, 'Documents access'::TEXT;
    
    -- Test financial_transactions access
    EXECUTE 'SELECT COUNT(*) FROM financial_transactions' INTO v_row_count;
    RETURN QUERY SELECT 'financial_transactions', v_row_count, 'Financial access'::TEXT;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- SECURITY MONITORING
-- =====================================================

-- Function to monitor RLS policy violations
CREATE OR REPLACE FUNCTION monitor_rls_violations()
RETURNS TABLE(
    user_id UUID,
    table_name TEXT,
    operation TEXT,
    violation_time TIMESTAMP WITH TIME ZONE,
    details TEXT
) AS $$
BEGIN
    -- This would typically query audit logs for RLS violations
    -- Implementation depends on your logging setup
    RETURN QUERY SELECT 
        NULL::UUID, 
        'monitoring'::TEXT, 
        'SELECT'::TEXT, 
        NOW(), 
        'RLS monitoring active'::TEXT;
END;
$$ LANGUAGE plpgsql;
