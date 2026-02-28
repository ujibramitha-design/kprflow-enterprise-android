package com.kprflow.enterprise.domain.usecase.verification

import com.kprflow.enterprise.data.model.AkadReadiness
import com.kprflow.enterprise.data.model.UserRole
import com.kprflow.enterprise.domain.repository.IVerificationRepository
import com.kprflow.enterprise.domain.repository.IAuthRepository
import javax.inject.Inject

/**
 * Use Case for Getting Akad Readiness
 * The Gatekeeper - Cross-department validation logic
 */
class GetAkadReadinessUseCase @Inject constructor(
    private val verificationRepository: IVerificationRepository,
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(dossierId: String): Result<AkadReadiness> {
        // Get akad readiness status
        val readiness = verificationRepository.getAkadReadiness(dossierId).getOrNull()
            ?: return Result.failure(Exception("Failed to get akad readiness"))
        
        return Result.success(readiness)
    }
}

/**
 * Use Case for Updating Verification Status
 * Role-based verification updates with security validation
 */
class UpdateVerificationUseCase @Inject constructor(
    private val verificationRepository: IVerificationRepository,
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(
        dossierId: String,
        verificationType: VerificationType,
        isCompleted: Boolean,
        notes: String? = null,
        documentUrl: String? = null,
        amount: Double? = null
    ): Result<Boolean> {
        // Get current user and validate role
        val currentUser = authRepository.getCurrentUser().getOrNull()
            ?: return Result.failure(Exception("User not authenticated"))
        
        // Validate role-based access
        if (!canUpdateVerification(currentUser.role, verificationType)) {
            return Result.failure(
                SecurityException("Access Denied: ${currentUser.role} cannot update ${verificationType.name} verification")
            )
        }
        
        // Update verification
        return verificationRepository.updateVerification(
            dossierId = dossierId,
            verificationType = verificationType,
            isCompleted = isCompleted,
            updatedBy = currentUser.id,
            notes = notes,
            documentUrl = documentUrl,
            amount = amount
        )
    }
    
    private fun canUpdateVerification(role: UserRole, type: VerificationType): Boolean {
        return when (role) {
            UserRole.FINANCE -> type in listOf(
                VerificationType.PPH_PAID,
                VerificationType.BPHTB_PAID,
                VerificationType.INSURANCE_PAID
            )
            UserRole.LEGAL -> type == VerificationType.AJB_DRAFT_READY
            UserRole.MARKETING -> type == VerificationType.SPR_FINAL_SIGNED
            UserRole.BOD, UserRole.MANAGER -> true // Full access
            else -> false
        }
    }
}

/**
 * Use Case for Finalizing Akad Schedule
 * Only LEGAL or MANAGER can trigger finalization
 */
class FinalizeAkadScheduleUseCase @Inject constructor(
    private val verificationRepository: IVerificationRepository,
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(
        dossierId: String,
        scheduledDate: String,
        notes: String? = null
    ): Result<Boolean> {
        // Get current user and validate role
        val currentUser = authRepository.getCurrentUser().getOrNull()
            ?: return Result.failure(Exception("User not authenticated"))
        
        // Validate role - only LEGAL or MANAGER can finalize
        if (currentUser.role !in listOf(UserRole.LEGAL, UserRole.MANAGER)) {
            return Result.failure(
                SecurityException("Access Denied: Only LEGAL or MANAGER can finalize akad schedule")
            )
        }
        
        // Check if all verifications are complete
        val readiness = verificationRepository.getAkadReadiness(dossierId).getOrNull()
            ?: return Result.failure(Exception("Failed to get verification status"))
        
        if (!readiness.isReadyForAkad) {
            return Result.failure(
                IllegalStateException("Cannot finalize akad: Missing verifications - ${readiness.missingVerifications.joinToString(", ")}")
            )
        }
        
        // Finalize akad schedule
        return verificationRepository.finalizeAkadSchedule(
            dossierId = dossierId,
            finalizedBy = currentUser.id,
            scheduledDate = scheduledDate,
            notes = notes
        )
    }
}

/**
 * Use Case for Getting Department Verification Status
 */
class GetDepartmentVerificationStatusUseCase @Inject constructor(
    private val verificationRepository: IVerificationRepository
) {
    suspend operator fun invoke(department: Department) = 
        verificationRepository.getDepartmentVerificationStatus(department)
}

/**
 * Use Case for Getting All Verification Statuses
 */
class GetAllVerificationStatusesUseCase @Inject constructor(
    private val verificationRepository: IVerificationRepository
) {
    suspend operator fun invoke() = verificationRepository.getAllVerificationStatuses()
}

/**
 * Use Case for Getting Verification Summary
 */
class GetVerificationSummaryUseCase @Inject constructor(
    private val verificationRepository: IVerificationRepository
) {
    suspend operator fun invoke() = verificationRepository.getVerificationSummary()
}

/**
 * Use Case for Monitoring Verification Changes
 */
class MonitorVerificationChangesUseCase @Inject constructor(
    private val verificationRepository: IVerificationRepository
) {
    operator fun invoke(dossierId: String) = verificationRepository.observeVerificationChanges(dossierId)
}

/**
 * Use Case for Bulk Verification Updates
 */
class BulkUpdateVerificationUseCase @Inject constructor(
    private val updateVerificationUseCase: UpdateVerificationUseCase,
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(
        updates: List<BulkVerificationUpdate>
    ): Result<BulkUpdateResult> {
        val currentUser = authRepository.getCurrentUser().getOrNull()
            ?: return Result.failure(Exception("User not authenticated"))
        
        val results = mutableListOf<VerificationUpdateResult>()
        var successCount = 0
        var errorCount = 0
        
        for (update in updates) {
            try {
                val result = updateVerificationUseCase(
                    dossierId = update.dossierId,
                    verificationType = update.verificationType,
                    isCompleted = update.isCompleted,
                    notes = update.notes,
                    documentUrl = update.documentUrl,
                    amount = update.amount
                )
                
                if (result.isSuccess) {
                    successCount++
                    results.add(
                        VerificationUpdateResult(
                            dossierId = update.dossierId,
                            success = true,
                            message = "Updated successfully"
                        )
                    )
                } else {
                    errorCount++
                    results.add(
                        VerificationUpdateResult(
                            dossierId = update.dossierId,
                            success = false,
                            message = result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    )
                }
            } catch (e: Exception) {
                errorCount++
                results.add(
                    VerificationUpdateResult(
                        dossierId = update.dossierId,
                        success = false,
                        message = e.message ?: "Unknown error"
                    )
                )
            }
        }
        
        return Result.success(
            BulkUpdateResult(
                totalUpdates = updates.size,
                successCount = successCount,
                errorCount = errorCount,
                results = results
            )
        )
    }
}

// =====================================================
// DATA MODELS FOR VERIFICATION
// =====================================================

enum class VerificationType {
    PPH_PAID,
    BPHTB_PAID,
    AJB_DRAFT_READY,
    SPR_FINAL_SIGNED,
    BAST_READY,
    INSURANCE_PAID
}

enum class Department {
    FINANCE,
    LEGAL,
    MARKETING
}

data class BulkVerificationUpdate(
    val dossierId: String,
    val verificationType: VerificationType,
    val isCompleted: Boolean,
    val notes: String? = null,
    val documentUrl: String? = null,
    val amount: Double? = null
)

data class BulkUpdateResult(
    val totalUpdates: Int,
    val successCount: Int,
    val errorCount: Int,
    val results: List<VerificationUpdateResult>
)

data class VerificationUpdateResult(
    val dossierId: String,
    val success: Boolean,
    val message: String
)
