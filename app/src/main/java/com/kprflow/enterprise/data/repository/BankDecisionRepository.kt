package com.kprflow.enterprise.data.repository

import com.kprflow.enterprise.data.model.KprStatus
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankDecisionRepository @Inject constructor(
    private val storage: Storage,
    private val postgrest: Postgrest
) {
    
    private val bankDecisionsBucket = "bank_decisions"
    
    suspend fun uploadBankDecision(
        dossierId: String,
        decisionFile: File,
        decisionType: BankDecisionType,
        bankName: String,
        notes: String? = null
    ): Result<BankDecisionResult> {
        return try {
            // Step 1: Upload decision file to storage
            val fileName = "bank_decision_${dossierId}_${System.currentTimeMillis()}.pdf"
            val filePath = "dossiers/$dossierId/$fileName"
            
            storage[bankDecisionsBucket].upload(filePath, decisionFile.readBytes())
            val publicUrl = storage[bankDecisionsBucket].publicUrl(filePath)
            
            // Step 2: Parse decision from file (simplified - in real implementation would use OCR)
            val parsedDecision = parseBankDecision(decisionFile, decisionType)
            
            // Step 3: Update dossier status based on decision
            val newStatus = when (parsedDecision.decision) {
                BankDecisionResultType.APPROVED -> KprStatus.PUTUSAN_KREDIT_ACC
                BankDecisionResultType.REJECTED -> KprStatus.CANCELLED_BY_SYSTEM
                BankDecisionResultType.PENDING -> KprStatus.PROSES_BANK
            }
            
            val updateResult = updateDossierStatus(dossierId, newStatus, notes, parsedDecision.reason)
            if (updateResult.isFailure) {
                // Rollback file upload if dossier update fails
                storage[bankDecisionsBucket].delete(filePath)
                return updateResult
            }
            
            // Step 4: Save decision record
            val decisionRecord = BankDecisionRecord(
                id = UUID.randomUUID().toString(),
                dossierId = dossierId,
                fileName = fileName,
                filePath = filePath,
                publicUrl = publicUrl,
                decisionType = parsedDecision.decision,
                bankName = bankName,
                decisionReason = parsedDecision.reason,
                approvedAmount = parsedDecision.approvedAmount,
                rejectionReason = parsedDecision.reason,
                notes = notes,
                uploadedAt = java.time.Instant.now().toString()
            )
            
            saveDecisionRecord(decisionRecord)
            
            Result.success(
                BankDecisionResult(
                    success = true,
                    decisionId = decisionRecord.id,
                    publicUrl = publicUrl,
                    decisionType = parsedDecision.decision,
                    newStatus = newStatus,
                    approvedAmount = parsedDecision.approvedAmount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getBankDecisions(dossierId: String): Result<List<BankDecisionRecord>> {
        return try {
            val decisions = postgrest.from("bank_decisions")
                .select()
                .filter { eq("dossier_id", dossierId) }
                .order("uploaded_at", ascending = false)
                .data
            Result.success(decisions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLatestBankDecision(dossierId: String): Result<BankDecisionRecord?> {
        return try {
            val decision = postgrest.from("bank_decisions")
                .select()
                .filter { eq("dossier_id", dossierId) }
                .order("uploaded_at", ascending = false)
                .limit(1)
                .maybeSingle()
                .data
            Result.success(decision)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteBankDecision(decisionId: String): Result<Unit> {
        return try {
            // Get decision record
            val decision = postgrest.from("bank_decisions")
                .select()
                .filter { eq("id", decisionId) }
                .maybeSingle()
                .data ?: return Result.failure(Exception("Decision not found"))
            
            // Delete from storage
            storage[bankDecisionsBucket].delete(decision.file_path)
            
            // Delete from database
            postgrest.from("bank_decisions")
                .delete()
                .filter { eq("id", decisionId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateDecisionStatus(
        decisionId: String,
        status: BankDecisionStatus,
        notes: String? = null
    ): Result<BankDecisionRecord> {
        return try {
            val updateData = mutableMapOf<String, Any>(
                "status" to status.name,
                "updated_at" to java.time.Instant.now().toString()
            )
            
            if (notes != null) {
                updateData["notes"] = notes
            }
            
            val updatedDecision = postgrest.from("bank_decisions")
                .update(updateData)
                .filter { eq("id", decisionId) }
                .maybeSingle()
                .data
            
            updatedDecision?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to update decision"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeDecisionUpdates(dossierId: String): Flow<BankDecisionRecord?> = flow {
        try {
            val latestDecision = getLatestBankDecision(dossierId).getOrNull()
            emit(latestDecision)
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    private suspend fun parseBankDecision(
        file: File,
        defaultType: BankDecisionType
    ): ParsedBankDecision {
        // TODO: Implement actual PDF parsing with OCR
        // For now, return default based on input type
        
        return when (defaultType) {
            BankDecisionType.APPROVED -> ParsedBankDecision(
                decision = BankDecisionResultType.APPROVED,
                reason = "Credit application approved by bank",
                approvedAmount = null // Would be extracted from PDF
            )
            
            BankDecisionType.REJECTED -> ParsedBankDecision(
                decision = BankDecisionResultType.REJECTED,
                reason = "Credit application rejected by bank",
                approvedAmount = null
            )
            
            BankDecisionType.AUTO_PARSE -> {
                // In real implementation, would use OCR to extract decision
                // For demo, randomly decide
                val isApproved = (0..1).random() == 1
                ParsedBankDecision(
                    decision = if (isApproved) BankDecisionResultType.APPROVED else BankDecisionResultType.REJECTED,
                    reason = if (isApproved) "Auto-detected approval" else "Auto-detected rejection",
                    approvedAmount = if (isApproved) java.math.BigDecimal("500000000") else null
                )
            }
        }
    }
    
    private suspend fun updateDossierStatus(
        dossierId: String,
        newStatus: KprStatus,
        notes: String?,
        reason: String?
    ): Result<Unit> {
        return try {
            val updateData = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "updated_at" to java.time.Instant.now().toString()
            )
            
            if (notes != null) {
                updateData["notes"] = notes
            }
            
            if (reason != null) {
                updateData["cancellation_reason"] = reason
            }
            
            postgrest.from("kpr_dossiers")
                .update(updateData)
                .filter { eq("id", dossierId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun saveDecisionRecord(record: BankDecisionRecord): Result<Unit> {
        return try {
            postgrest.from("bank_decisions")
                .insert(record)
                .maybeSingle()
                .data
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDecisionDownloadUrl(decisionId: String): Result<String> {
        return try {
            val decision = postgrest.from("bank_decisions")
                .select("public_url")
                .filter { eq("id", decisionId) }
                .maybeSingle()
                .data ?: return Result.failure(Exception("Decision not found"))
            
            Result.success(decision.public_url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getBankDecisionStats(): Result<BankDecisionStats> {
        return try {
            val allDecisions = postgrest.from("bank_decisions")
                .select("decision_type, uploaded_at")
                .data
            
            val approved = allDecisions.count { it.decision_type == "APPROVED" }
            val rejected = allDecisions.count { it.decision_type == "REJECTED" }
            val pending = allDecisions.count { it.decision_type == "PENDING" }
            val total = allDecisions.size
            
            val stats = BankDecisionStats(
                totalDecisions = total,
                approvedDecisions = approved,
                rejectedDecisions = rejected,
                pendingDecisions = pending,
                approvalRate = if (total > 0) (approved.toDouble() / total) * 100 else 0.0
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

enum class BankDecisionType {
    APPROVED,
    REJECTED,
    AUTO_PARSE
}

enum class BankDecisionResultType {
    APPROVED,
    REJECTED,
    PENDING
}

enum class BankDecisionStatus {
    UPLOADED,
    REVIEWED,
    PROCESSED,
    ARCHIVED
}

data class ParsedBankDecision(
    val decision: BankDecisionResultType,
    val reason: String,
    val approvedAmount: java.math.BigDecimal?
)

data class BankDecisionResult(
    val success: Boolean,
    val decisionId: String,
    val publicUrl: String,
    val decisionType: BankDecisionResultType,
    val newStatus: KprStatus,
    val approvedAmount: java.math.BigDecimal?
)

data class BankDecisionRecord(
    val id: String,
    val dossierId: String,
    val fileName: String,
    val filePath: String,
    val publicUrl: String,
    val decisionType: BankDecisionResultType,
    val bankName: String,
    val decisionReason: String,
    val approvedAmount: java.math.BigDecimal?,
    val rejectionReason: String?,
    val notes: String?,
    val uploadedAt: String
)

data class BankDecisionStats(
    val totalDecisions: Int,
    val approvedDecisions: Int,
    val rejectedDecisions: Int,
    val pendingDecisions: Int,
    val approvalRate: Double
)
