package com.kprflow.enterprise.data.repository

import com.kprflow.enterprise.data.model.KprStatus
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuorumVotingRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    
    suspend fun createVotingSession(
        dossierId: String,
        votingType: QuorumVotingType,
        title: String,
        description: String,
        createdBy: String,
        expiresAt: Instant? = null
    ): Result<QuorumVotingSession> {
        return try {
            val session = QuorumVotingSession(
                id = UUID.randomUUID().toString(),
                dossierId = dossierId,
                votingType = votingType,
                title = title,
                description = description,
                status = QuorumVotingStatus.ACTIVE,
                createdBy = createdBy,
                createdAt = Instant.now().toString(),
                expiresAt = expiresAt?.toString()
            )
            
            val createdSession = postgrest.from("quorum_voting_sessions")
                .insert(session)
                .maybeSingle()
                .data
            
            createdSession?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to create voting session"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun castVote(
        sessionId: String,
        voterId: String,
        vote: QuorumVote,
        comment: String? = null
    ): Result<QuorumVoteRecord> {
        return try {
            // Check if voter has already voted
            val existingVote = postgrest.from("quorum_votes")
                .select()
                .filter { 
                    eq("session_id", sessionId)
                    eq("voter_id", voterId)
                }
                .maybeSingle()
                .data
            
            if (existingVote != null) {
                return Result.failure(Exception("Voter has already cast a vote"))
            }
            
            // Check if voting session is still active
            val session = postgrest.from("quorum_voting_sessions")
                .select()
                .filter { eq("id", sessionId) }
                .maybeSingle()
                .data ?: return Result.failure(Exception("Voting session not found"))
            
            if (session.status != QuorumVotingStatus.ACTIVE) {
                return Result.failure(Exception("Voting session is not active"))
            }
            
            // Check if session has expired
            session.expiresAt?.let { expiry ->
                if (Instant.parse(expiry).isBefore(Instant.now())) {
                    return Result.failure(Exception("Voting session has expired"))
                }
            }
            
            val voteRecord = QuorumVoteRecord(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                voterId = voterId,
                vote = vote,
                comment = comment,
                createdAt = Instant.now().toString()
            )
            
            val createdVote = postgrest.from("quorum_votes")
                .insert(voteRecord)
                .maybeSingle()
                .data
            
            createdVote?.let { 
                // Check if voting should be concluded
                checkVotingConclusion(sessionId)
                Result.success(it)
                }
                ?: Result.failure(Exception("Failed to cast vote"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getVotingSession(sessionId: String): Result<QuorumVotingSession?> {
        return try {
            val session = postgrest.from("quorum_voting_sessions")
                .select()
                .filter { eq("id", sessionId) }
                .maybeSingle()
                .data
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getVotingSessions(dossierId: String): Result<List<QuorumVotingSession>> {
        return try {
            val sessions = postgrest.from("quorum_voting_sessions")
                .select()
                .filter { eq("dossier_id", dossierId) }
                .order("created_at", ascending = false)
                .data
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getVotingResults(sessionId: String): Result<QuorumVotingResult> {
        return try {
            // Get voting session
            val session = getVotingSession(sessionId).getOrNull()
                ?: return Result.failure(Exception("Voting session not found"))
            
            // Get all votes for this session
            val votes = postgrest.from("quorum_votes")
                .select()
                .filter { eq("session_id", sessionId) }
                .data
            
            // Count votes
            val approveVotes = votes.count { it.vote == QuorumVote.APPROVE }
            val rejectVotes = votes.count { it.vote == QuorumVote.REJECT }
            val totalVotes = approveVotes + rejectVotes
            
            // Determine result based on 2:1 rule
            val result = when {
                approveVotes >= 2 && approveVotes > rejectVotes -> QuorumVotingDecision.APPROVED
                rejectVotes >= 2 && rejectVotes > approveVotes -> QuorumVotingDecision.REJECTED
                totalVotes < 3 -> QuorumVotingDecision.PENDING // Need at least 3 votes for 2:1 rule
                else -> QuorumVotingDecision.PENDING // No clear majority
            }
            
            val votingResult = QuorumVotingResult(
                sessionId = sessionId,
                dossierId = session.dossierId,
                votingType = session.votingType,
                totalVotes = totalVotes,
                approveVotes = approveVotes,
                rejectVotes = rejectVotes,
                decision = result,
                concludedAt = if (result != QuorumVotingDecision.PENDING) Instant.now().toString() else null,
                votes = votes.map { vote ->
                    QuorumVoteDetail(
                        voteId = vote.id,
                        voterId = vote.voterId,
                        vote = vote.vote,
                        comment = vote.comment,
                        createdAt = vote.createdAt
                    )
                }
            )
            
            Result.success(votingResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getActiveVotingSessions(): Result<List<QuorumVotingSession>> {
        return try {
            val sessions = postgrest.from("quorum_voting_sessions")
                .select()
                .filter { 
                    eq("status", QuorumVotingStatus.ACTIVE.name)
                }
                .order("created_at", ascending = false)
                .data
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getVoterEligibility(voterId: String): Result<VoterEligibility> {
        return try {
            // Get voter profile
            val voter = postgrest.from("user_profiles")
                .select("role, is_active")
                .filter { eq("id", voterId) }
                .maybeSingle()
                .data ?: return Result.failure(Exception("Voter not found"))
            
            // Check if voter is eligible (Legal team members)
            val isEligible = voter.role == "LEGAL" && voter.is_active
            
            Result.success(
                VoterEligibility(
                    voterId = voterId,
                    isEligible = isEligible,
                    role = voter.role,
                    isActive = voter.is_active
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun concludeVotingSession(
        sessionId: String,
        concludedBy: String,
        reason: String? = null
    ): Result<QuorumVotingSession> {
        return try {
            val updateData = mapOf(
                "status" to QuorumVotingStatus.CONCLUDED.name,
                "concluded_by" to concludedBy,
                "concluded_at" to Instant.now().toString(),
                "conclusion_reason" to reason,
                "updated_at" to Instant.now().toString()
            )
            
            val updatedSession = postgrest.from("quorum_voting_sessions")
                .update(updateData)
                .filter { eq("id", sessionId) }
                .maybeSingle()
                .data
            
            updatedSession?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to conclude voting session"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun cancelVotingSession(
        sessionId: String,
        cancelledBy: String,
        reason: String? = null
    ): Result<QuorumVotingSession> {
        return try {
            val updateData = mapOf(
                "status" to QuorumVotingStatus.CANCELLED.name,
                "cancelled_by" to cancelledBy,
                "cancelled_at" to Instant.now().toString(),
                "cancellation_reason" to reason,
                "updated_at" to Instant.now().toString()
            )
            
            val updatedSession = postgrest.from("quorum_voting_sessions")
                .update(updateData)
                .filter { eq("id", sessionId) }
                .maybeSingle()
                .data
            
            updatedSession?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to cancel voting session"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getVotingHistory(dossierId: String): Result<List<QuorumVotingResult>> {
        return try {
            val sessions = getVotingSessions(dossierId).getOrNull().orEmpty()
            val results = mutableListOf<QuorumVotingResult>()
            
            sessions.forEach { session ->
                val result = getVotingResults(session.id).getOrNull()
                result?.let { results.add(it) }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeVotingUpdates(sessionId: String): Flow<QuorumVotingUpdate> = flow {
        try {
            // TODO: Implement real-time updates via Supabase Realtime
            val result = getVotingResults(sessionId).getOrNull()
            result?.let { emit(QuorumVotingUpdate.VoteCast(it)) }
        } catch (e: Exception) {
            emit(QuorumVotingUpdate.Error(e.message ?: "Unknown error"))
        }
    }
    
    private suspend fun checkVotingConclusion(sessionId: String): Result<Unit> {
        return try {
            val result = getVotingResults(sessionId).getOrNull()
                ?: return Result.success(Unit) // No conclusion needed
            
            if (result.decision != QuorumVotingDecision.PENDING) {
                // Update dossier status based on voting result
                val newStatus = when (result.decision) {
                    QuorumVotingDecision.APPROVED -> KprStatus.PUTUSAN_KREDIT_ACC
                    QuorumVotingDecision.REJECTED -> KprStatus.CANCELLED_BY_SYSTEM
                    else -> KprStatus.PROSES_BANK
                }
                
                updateDossierStatus(result.dossierId, newStatus, result.decision.name)
                
                // Conclude voting session
                concludeVotingSession(sessionId, "system", "Auto-concluded based on voting results")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun updateDossierStatus(
        dossierId: String,
        newStatus: KprStatus,
        reason: String
    ): Result<Unit> {
        return try {
            postgrest.from("kpr_dossiers")
                .update(
                    mapOf(
                        "status" to newStatus.name,
                        "updated_at" to Instant.now().toString(),
                        "notes" to reason
                    )
                )
                .filter { eq("id", dossierId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Data classes
enum class QuorumVotingType {
    DOCUMENT_APPROVAL,
    CREDIT_DECISION,
    EXCEPTION_HANDLING
}

enum class QuorumVotingStatus {
    ACTIVE,
    CONCLUDED,
    CANCELLED,
    EXPIRED
}

enum class QuorumVote {
    APPROVE,
    REJECT,
    ABSTAIN
}

enum class QuorumVotingDecision {
    APPROVED,
    REJECTED,
    PENDING
}

data class QuorumVotingSession(
    val id: String,
    val dossierId: String,
    val votingType: QuorumVotingType,
    val title: String,
    val description: String,
    val status: QuorumVotingStatus,
    val createdBy: String,
    val createdAt: String,
    val expiresAt: String? = null,
    val concludedBy: String? = null,
    val concludedAt: String? = null,
    val conclusionReason: String? = null,
    val cancelledBy: String? = null,
    val cancelledAt: String? = null,
    val cancellationReason: String? = null,
    val updatedAt: String? = null
)

data class QuorumVoteRecord(
    val id: String,
    val sessionId: String,
    val voterId: String,
    val vote: QuorumVote,
    val comment: String? = null,
    val createdAt: String
)

data class QuorumVotingResult(
    val sessionId: String,
    val dossierId: String,
    val votingType: QuorumVotingType,
    val totalVotes: Int,
    val approveVotes: Int,
    val rejectVotes: Int,
    val decision: QuorumVotingDecision,
    val concludedAt: String? = null,
    val votes: List<QuorumVoteDetail>
)

data class QuorumVoteDetail(
    val voteId: String,
    val voterId: String,
    val vote: QuorumVote,
    val comment: String? = null,
    val createdAt: String
)

data class VoterEligibility(
    val voterId: String,
    val isEligible: Boolean,
    val role: String,
    val isActive: Boolean
)

sealed class QuorumVotingUpdate {
    data class VoteCast(val result: QuorumVotingResult) : QuorumVotingUpdate()
    data class SessionConcluded(val sessionId: String) : QuorumVotingUpdate()
    data class SessionCancelled(val sessionId: String) : QuorumVotingUpdate()
    data class Error(val message: String) : QuorumVotingUpdate()
}
