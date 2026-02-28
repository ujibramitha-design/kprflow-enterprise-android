-- Bank Submissions table for Phase 7: Document Management

CREATE TABLE IF NOT EXISTS bank_submissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    public_url TEXT NOT NULL,
    document_count INTEGER NOT NULL DEFAULT 0,
    file_size BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'GENERATED', -- 'GENERATED', 'SUBMITTED', 'ACCEPTED', 'REJECTED'
    submitted_at TIMESTAMP WITH TIME ZONE,
    bank_response_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_bank_submissions_dossier_id ON bank_submissions(dossier_id);
CREATE INDEX IF NOT EXISTS idx_bank_submissions_status ON bank_submissions(status);
CREATE INDEX IF NOT EXISTS idx_bank_submissions_created_at ON bank_submissions(created_at);

-- Create updated_at trigger
CREATE OR REPLACE FUNCTION update_bank_submissions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_bank_submissions_updated_at BEFORE UPDATE ON bank_submissions FOR EACH ROW EXECUTE FUNCTION update_bank_submissions_updated_at();

-- RLS Policies for bank submissions
ALTER TABLE bank_submissions ENABLE ROW LEVEL SECURITY;

-- Marketing and Legal can view submissions
CREATE POLICY "Marketing can view bank submissions" ON bank_submissions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'MARKETING');

CREATE POLICY "Legal can view bank submissions" ON bank_submissions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- Finance can manage submissions
CREATE POLICY "Finance can manage bank submissions" ON bank_submissions
    FOR ALL USING (auth.jwt() ->> 'role' = 'FINANCE');

-- BOD can view all submissions
CREATE POLICY "BOD can view all bank submissions" ON bank_submissions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Customers can view their own submissions
CREATE POLICY "Customers can view own submissions" ON bank_submissions
    FOR SELECT USING (
        auth.uid()::text IN (
            SELECT user_id::text FROM kpr_dossiers WHERE id = dossier_id
        )
    );
