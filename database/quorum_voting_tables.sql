-- Quorum Voting tables for Phase 14: Legal 2:1 Voting System

-- Quorum Voting Sessions table
CREATE TABLE IF NOT EXISTS quorum_voting_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    voting_type VARCHAR(30) NOT NULL, -- 'DOCUMENT_APPROVAL', 'CREDIT_DECISION', 'EXCEPTION_HANDLING'
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- 'ACTIVE', 'CONCLUDED', 'CANCELLED', 'EXPIRED'
    created_by UUID NOT NULL REFERENCES user_profiles(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    concluded_by UUID REFERENCES user_profiles(id),
    concluded_at TIMESTAMP WITH TIME ZONE,
    conclusion_reason TEXT,
    cancelled_by UUID REFERENCES user_profiles(id),
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Quorum Votes table
CREATE TABLE IF NOT EXISTS quorum_votes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES quorum_voting_sessions(id) ON DELETE CASCADE,
    voter_id UUID NOT NULL REFERENCES user_profiles(id),
    vote VARCHAR(10) NOT NULL, -- 'APPROVE', 'REJECT', 'ABSTAIN'
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(session_id, voter_id) -- One vote per voter per session
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_quorum_sessions_dossier_id ON quorum_voting_sessions(dossier_id);
CREATE INDEX IF NOT EXISTS idx_quorum_sessions_status ON quorum_voting_sessions(status);
CREATE INDEX IF NOT EXISTS idx_quorum_sessions_created_at ON quorum_voting_sessions(created_at);
CREATE INDEX IF NOT EXISTS idx_quorum_sessions_voting_type ON quorum_voting_sessions(voting_type);
CREATE INDEX IF NOT EXISTS idx_quorum_votes_session_id ON quorum_votes(session_id);
CREATE INDEX IF NOT EXISTS idx_quorum_votes_voter_id ON quorum_votes(voter_id);
CREATE INDEX IF NOT EXISTS idx_quorum_votes_vote ON quorum_votes(vote);

-- RLS Policies for quorum voting sessions
ALTER TABLE quorum_voting_sessions ENABLE ROW LEVEL SECURITY;

-- Legal team can view voting sessions
CREATE POLICY "Legal can view voting sessions" ON quorum_voting_sessions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- BOD can view all voting sessions
CREATE POLICY "BOD can view all voting sessions" ON quorum_voting_sessions
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Legal team can create voting sessions
CREATE POLICY "Legal can create voting sessions" ON quorum_voting_sessions
    FOR INSERT WITH CHECK (auth.jwt() ->> 'role' = 'LEGAL');

-- Legal team can update voting sessions they created
CREATE POLICY "Legal can update own voting sessions" ON quorum_voting_sessions
    FOR UPDATE USING (auth.uid()::text = created_by::text);

-- BOD can conclude voting sessions
CREATE POLICY "BOD can conclude voting sessions" ON quorum_voting_sessions
    FOR UPDATE USING (auth.jwt() ->> 'role' = 'BOD');

-- RLS Policies for quorum votes
ALTER TABLE quorum_votes ENABLE ROW LEVEL SECURITY;

-- Legal team can view all votes
CREATE POLICY "Legal can view all votes" ON quorum_votes
    FOR SELECT USING (auth.jwt() ->> 'role' = 'LEGAL');

-- BOD can view all votes
CREATE POLICY "BOD can view all votes" ON quorum_votes
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Legal team members can vote
CREATE POLICY "Legal team can vote" ON quorum_votes
    FOR INSERT WITH CHECK (
        auth.jwt() ->> 'role' = 'LEGAL' AND
        auth.uid()::text NOT IN (
            SELECT voter_id::text FROM quorum_votes WHERE session_id = session_id
        )
    );

-- Voters can update their own votes (within time limit)
CREATE POLICY "Voters can update own votes" ON quorum_votes
    FOR UPDATE USING (
        auth.uid()::text = voter_id::text AND
        created_at > NOW() - INTERVAL '1 hour' -- Allow vote changes within 1 hour
    );

-- Function to check if voting should be concluded (2:1 rule)
CREATE OR REPLACE FUNCTION check_voting_conclusion(p_session_id UUID)
RETURNS TABLE (
    should_conclude BOOLEAN,
    decision VARCHAR(20),
    approve_votes BIGINT,
    reject_votes BIGINT,
    total_votes BIGINT
) AS $$
BEGIN
    RETURN QUERY
    WITH vote_counts AS (
        SELECT 
            COUNT(*) FILTER (WHERE vote = 'APPROVE') as approve_count,
            COUNT(*) FILTER (WHERE vote = 'REJECT') as reject_count,
            COUNT(*) as total_count
        FROM quorum_votes
        WHERE session_id = p_session_id
    ),
    session_info AS (
        SELECT status FROM quorum_voting_sessions WHERE id = p_session_id
    )
    SELECT 
        CASE 
            WHEN vc.total_count >= 3 AND (
                (vc.approve_count >= 2 AND vc.approve_count > vc.reject_count) OR
                (vc.reject_count >= 2 AND vc.reject_count > vc.approve_count)
            ) THEN true
            ELSE false
        END as should_conclude,
        CASE 
            WHEN vc.approve_count >= 2 AND vc.approve_count > vc.reject_count THEN 'APPROVED'
            WHEN vc.reject_count >= 2 AND vc.reject_count > vc.approve_count THEN 'REJECTED'
            ELSE 'PENDING'
        END as decision,
        vc.approve_count,
        vc.reject_count,
        vc.total_count
    FROM vote_counts vc, session_info si
    WHERE si.status = 'ACTIVE';
END;
$$ LANGUAGE plpgsql;

-- Function to automatically conclude voting and update dossier status
CREATE OR REPLACE FUNCTION auto_conclude_voting()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if voting should be concluded
    INSERT INTO quorum_voting_sessions (id, status, concluded_at, conclusion_reason)
    SELECT 
        NEW.session_id,
        'CONCLUDED',
        NOW(),
        CASE 
            WHEN decision = 'APPROVED' THEN 'Auto-concluded: 2:1 approval achieved'
            WHEN decision = 'REJECTED' THEN 'Auto-concluded: 2:1 rejection achieved'
            ELSE 'Auto-concluded: No clear majority'
        END
    FROM check_voting_conclusion(NEW.session_id)
    WHERE should_conclude = true
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        concluded_at = EXCLUDED.concluded_at,
        conclusion_reason = EXCLUDED.conclusion_reason;
    
    -- Update dossier status if voting is concluded
    UPDATE kpr_dossiers kd
    SET 
        status = CASE 
            WHEN decision = 'APPROVED' THEN 'PUTUSAN_KREDIT_ACC'
            WHEN decision = 'REJECTED' THEN 'CANCELLED_BY_SYSTEM'
            ELSE status
        END,
        updated_at = NOW(),
        notes = CONCAT(
            COALESCE(notes, ''), 
            CASE 
                WHEN notes IS NOT NULL AND notes != '' THEN '; ' ELSE '' 
            END,
            'Legal quorum decision: ', decision
        )
    FROM check_voting_conclusion(NEW.session_id) cc
    WHERE kd.id = (
        SELECT dossier_id FROM quorum_voting_sessions WHERE id = NEW.session_id
    )
    AND cc.should_conclude = true;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for automatic voting conclusion
CREATE TRIGGER auto_conclude_voting_trigger
    AFTER INSERT ON quorum_votes
    FOR EACH ROW
    EXECUTE FUNCTION auto_conclude_voting();

-- Function to get voting statistics
CREATE OR REPLACE FUNCTION get_voting_statistics(
    p_date_range_start DATE DEFAULT NULL,
    p_date_range_end DATE DEFAULT NULL
)
RETURNS TABLE (
    total_sessions BIGINT,
    concluded_sessions BIGINT,
    approved_decisions BIGINT,
    rejected_decisions BIGINT,
    pending_decisions BIGINT,
    average_voting_time_hours DECIMAL(10,2),
    participation_rate DECIMAL(5,2)
) AS $$
BEGIN
    RETURN QUERY
    WITH session_stats AS (
        SELECT 
            COUNT(*) as total_sessions,
            COUNT(*) FILTER (WHERE status = 'CONCLUDED') as concluded_sessions,
            COUNT(*) FILTER (
                EXISTS (
                    SELECT 1 FROM check_voting_conclusion(qvs.id) 
                    WHERE decision = 'APPROVED'
                )
            ) as approved_decisions,
            COUNT(*) FILTER (
                EXISTS (
                    SELECT 1 FROM check_voting_conclusion(qvs.id) 
                    WHERE decision = 'REJECTED'
                )
            ) as rejected_decisions,
            COUNT(*) FILTER (
                EXISTS (
                    SELECT 1 FROM check_voting_conclusion(qvs.id) 
                    WHERE decision = 'PENDING'
                )
            ) as pending_decisions,
            AVG(EXTRACT(EPOCH FROM (concluded_at - created_at))/3600) as avg_voting_time
        FROM quorum_voting_sessions qvs
        WHERE 
            (p_date_range_start IS NULL OR DATE(created_at) >= p_date_range_start)
            AND (p_date_range_end IS NULL OR DATE(created_at) <= p_date_range_end)
    ),
    participation_stats AS (
        SELECT 
            COUNT(DISTINCT qv.voter_id)::DECIMAL / 
            (SELECT COUNT(*) FROM user_profiles WHERE role = 'LEGAL' AND is_active = true)::DECIMAL * 100 as participation_rate
        FROM quorum_votes qv
        JOIN quorum_voting_sessions qvs ON qv.session_id = qvs.id
        WHERE 
            (p_date_range_start IS NULL OR DATE(qvs.created_at) >= p_date_range_start)
            AND (p_date_range_end IS NULL OR DATE(qvs.created_at) <= p_date_range_end)
    )
    SELECT 
        ss.total_sessions,
        ss.concluded_sessions,
        ss.approved_decisions,
        ss.rejected_decisions,
        ss.pending_decisions,
        COALESCE(ss.avg_voting_time, 0) as average_voting_time_hours,
        COALESCE(ps.participation_rate, 0) as participation_rate
    FROM session_stats ss, participation_stats ps;
END;
$$ LANGUAGE plpgsql;

-- Function to check voter eligibility
CREATE OR REPLACE FUNCTION is_voter_eligible(p_voter_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM user_profiles 
        WHERE id = p_voter_id 
        AND role = 'LEGAL' 
        AND is_active = true
    );
END;
$$ LANGUAGE plpgsql;

-- Function to get active voting sessions for a voter
CREATE OR REPLACE FUNCTION get_active_voting_sessions(p_voter_id UUID)
RETURNS TABLE (
    session_id UUID,
    dossier_id UUID,
    title VARCHAR(255),
    description TEXT,
    voting_type VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    has_voted BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        qvs.id,
        qvs.dossier_id,
        qvs.title,
        qvs.description,
        qvs.voting_type,
        qvs.created_at,
        qvs.expires_at,
        EXISTS (
            SELECT 1 FROM quorum_votes qv 
            WHERE qv.session_id = qvs.id AND qv.voter_id = p_voter_id
        ) as has_voted
    FROM quorum_voting_sessions qvs
    WHERE qvs.status = 'ACTIVE'
    AND (qvs.expires_at IS NULL OR qvs.expires_at > NOW())
    AND is_voter_eligible(p_voter_id);
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_quorum_sessions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_quorum_sessions_updated_at BEFORE UPDATE ON quorum_voting_sessions FOR EACH ROW EXECUTE FUNCTION update_quorum_sessions_updated_at();
