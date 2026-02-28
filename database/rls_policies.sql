-- Row Level Security (RLS) Policies for KPRFlow Enterprise
-- Enable RLS on all tables

ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE unit_properties ENABLE ROW LEVEL SECURITY;
ALTER TABLE kpr_dossiers ENABLE ROW LEVEL SECURITY;
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE financial_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE unit_swap_requests ENABLE ROW LEVEL SECURITY;

-- User Profiles RLS Policies
-- Users can see their own profile
CREATE POLICY "Users can view own profile" ON user_profiles
    FOR SELECT USING (auth.uid()::text = id::text);

-- Users can update their own profile (limited fields)
CREATE POLICY "Users can update own profile" ON user_profiles
    FOR UPDATE USING (auth.uid()::text = id::text);

-- Marketing and Legal can view all customer profiles
CREATE POLICY "Marketing can view customer profiles" ON user_profiles
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'MARKETING' OR 
        auth.jwt() ->> 'role' = 'LEGAL' OR
        auth.jwt() ->> 'role' = 'FINANCE' OR
        auth.jwt() ->> 'role' = 'BOD'
    );

-- Finance and BOD can view all profiles
CREATE POLICY "Finance can view all profiles" ON user_profiles
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'FINANCE' OR
        auth.jwt() ->> 'role' = 'BOD'
    );

-- Unit Properties RLS Policies
-- All authenticated users can view available units
CREATE POLICY "All users can view available units" ON unit_properties
    FOR SELECT USING (status = 'AVAILABLE');

-- Marketing and Estate can view all units
CREATE POLICY "Marketing can view all units" ON unit_properties
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'MARKETING' OR
        auth.jwt() ->> 'role' = 'ESTATE' OR
        auth.jwt() ->> 'role' = 'BOD'
    );

-- Marketing and Estate can manage units
CREATE POLICY "Marketing can manage units" ON unit_properties
    FOR ALL USING (
        auth.jwt() ->> 'role' = 'MARKETING' OR
        auth.jwt() ->> 'role' = 'ESTATE' OR
        auth.jwt() ->> 'role' = 'BOD'
    );

-- KPR Dossiers RLS Policies
-- Customers can view their own dossiers
CREATE POLICY "Customers can view own dossiers" ON kpr_dossiers
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'CUSTOMER' AND 
        auth.uid()::text = user_id::text
    );

-- Marketing, Legal, Finance can view relevant dossiers
CREATE POLICY "Staff can view relevant dossiers" ON kpr_dossiers
    FOR SELECT USING (
        auth.jwt() ->> 'role' IN ('MARKETING', 'LEGAL', 'FINANCE', 'BANK', 'TEKNIK', 'ESTATE', 'BOD')
    );

-- Marketing can create new dossiers (leads)
CREATE POLICY "Marketing can create dossiers" ON kpr_dossiers
    FOR INSERT WITH CHECK (
        auth.jwt() ->> 'role' IN ('MARKETING', 'BOD')
    );

-- Legal and Finance can update dossier status
CREATE POLICY "Legal can update dossier status" ON kpr_dossiers
    FOR UPDATE USING (
        auth.jwt() ->> 'role' IN ('LEGAL', 'FINANCE', 'MARKETING', 'BOD')
    );

-- Documents RLS Policies
-- Customers can view their own documents
CREATE POLICY "Customers can view own documents" ON documents
    FOR SELECT USING (
        auth.jwt() ->> 'role' = 'CUSTOMER' AND 
        EXISTS (
            SELECT 1 FROM kpr_dossiers 
            WHERE kpr_dossiers.id = documents.dossier_id 
            AND kpr_dossiers.user_id::text = auth.uid()::text
        )
    );

-- Legal and Finance can view all documents
CREATE POLICY "Legal can view all documents" ON documents
    FOR SELECT USING (
        auth.jwt() ->> 'role' IN ('LEGAL', 'FINANCE', 'BOD')
    );

-- Legal can verify documents
CREATE POLICY "Legal can verify documents" ON documents
    FOR UPDATE USING (
        auth.jwt() ->> 'role' IN ('LEGAL', 'FINANCE')
    );

-- Customers can upload their own documents
CREATE POLICY "Customers can upload documents" ON documents
    FOR INSERT WITH CHECK (
        auth.jwt() ->> 'role' = 'CUSTOMER' AND 
        EXISTS (
            SELECT 1 FROM kpr_dossiers 
            WHERE kpr_dossiers.id = NEW.dossier_id 
            AND kpr_dossiers.user_id::text = auth.uid()::text
        )
    );

-- Financial Transactions RLS Policies
-- Finance can view all transactions
CREATE POLICY "Finance can view all transactions" ON financial_transactions
    FOR SELECT USING (
        auth.jwt() ->> 'role' IN ('FINANCE', 'BOD')
    );

-- Finance can manage transactions
CREATE POLICY "Finance can manage transactions" ON financial_transactions
    FOR ALL USING (
        auth.jwt() ->> 'role' IN ('FINANCE', 'BOD')
    );

-- Marketing can view transaction summaries
CREATE POLICY "Marketing can view transactions" ON financial_transactions
    FOR SELECT USING (
        auth.jwt() ->> 'role' IN ('MARKETING', 'BOD')
    );

-- Unit Swap Requests RLS Policies
-- All relevant staff can view swap requests
CREATE POLICY "Staff can view swap requests" ON unit_swap_requests
    FOR SELECT USING (
        auth.jwt() ->> 'role' IN ('MARKETING', 'LEGAL', 'FINANCE', 'BOD')
    );

-- Finance, Marketing, Legal can vote on swap requests
CREATE POLICY "Finance can vote on swaps" ON unit_swap_requests
    FOR UPDATE USING (
        auth.jwt() ->> 'role' = 'FINANCE'
    );

CREATE POLICY "Marketing can vote on swaps" ON unit_swap_requests
    FOR UPDATE USING (
        auth.jwt() ->> 'role' = 'MARKETING'
    );

CREATE POLICY "Legal can vote on swaps" ON unit_swap_requests
    FOR UPDATE USING (
        auth.jwt() ->> 'role' = 'LEGAL'
    );

-- Create function to check if user has specific role
CREATE OR REPLACE FUNCTION has_role(role_name TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN auth.jwt() ->> 'role' = role_name;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create function to check if user owns the dossier
CREATE OR REPLACE FUNCTION owns_dossier(dossier_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM kpr_dossiers 
        WHERE id = dossier_id 
        AND user_id::text = auth.uid()::text
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
