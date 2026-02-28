package com.kprflow.enterprise.domain.usecase.extension

import com.kprflow.enterprise.data.model.ExtensionRequest
import com.kprflow.enterprise.data.model.ExtensionRequestForm
import com.kprflow.enterprise.data.model.ExtensionValidation
import com.kprflow.enterprise.domain.repository.IExtensionRepository
import com.kprflow.enterprise.domain.repository.IKprRepository
import javax.inject.Inject

/**
 * Use Case for Requesting Dossier Extension
 * Business logic for extension validation and processing
 */
class RequestExtensionUseCase @Inject constructor(
    private val extensionRepository: IExtensionRepository,
    private val kprRepository: IKprRepository
) {
    suspend operator fun invoke(form: ExtensionRequestForm): Result<ExtensionRequest> {
        // Validate form
        if (!form.isValid()) {
            return Result.failure(
                IllegalArgumentException("Invalid form: ${form.getValidationErrors().joinToString()}")
            )
        }
        
        // Check if user can request extension
        val canExtend = extensionRepository.canRequestExtension(form.dossierId).getOrNull()
            ?: return Result.failure(Exception("Failed to check extension eligibility"))
        
        if (!canExtend) {
            return Result.failure(
                IllegalArgumentException("Dossier is not eligible for extension")
            )
        }
        
        // Check extension count limit
        val extensionCount = extensionRepository.getDossierExtensionCount(form.dossierId).getOrNull()
            ?: return Result.failure(Exception("Failed to get extension count"))
        
        if (extensionCount >= 3) {
            return Result.failure(
                IllegalArgumentException("Maximum extension limit reached (3 extensions)")
            )
        }
        
        // Request extension
        return extensionRepository.requestExtension(
            dossierId = form.dossierId,
            days = form.extensionDays,
            reason = form.extensionReason,
            requestedBy = form.requestedBy
        )
    }
}

/**
 * Use Case for Approving Extension Request
 */
class ApproveExtensionUseCase @Inject constructor(
    private val extensionRepository: IExtensionRepository
) {
    suspend operator fun invoke(
        extensionId: String,
        approvedBy: String,
        approvalNotes: String? = null
    ): Result<Boolean> {
        // Validate extension exists and is pending
        val extension = extensionRepository.getExtensionById(extensionId).getOrNull()
            ?: return Result.failure(Exception("Extension request not found"))
        
        if (extension.status != com.kprflow.enterprise.data.model.ExtensionStatus.PENDING) {
            return Result.failure(
                IllegalArgumentException("Extension request is not pending")
            )
        }
        
        // Approve extension
        return extensionRepository.approveExtension(
            extensionId = extensionId,
            approvedBy = approvedBy,
            approvalNotes = approvalNotes
        )
    }
}

/**
 * Use Case for Rejecting Extension Request
 */
class RejectExtensionUseCase @Inject constructor(
    private val extensionRepository: IExtensionRepository
) {
    suspend operator fun invoke(
        extensionId: String,
        rejectedBy: String,
        rejectionReason: String
    ): Result<Boolean> {
        // Validate rejection reason
        if (rejectionReason.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Rejection reason is required")
            )
        }
        
        if (rejectionReason.length < 5) {
            return Result.failure(
                IllegalArgumentException("Rejection reason must be at least 5 characters")
            )
        }
        
        // Validate extension exists and is pending
        val extension = extensionRepository.getExtensionById(extensionId).getOrNull()
            ?: return Result.failure(Exception("Extension request not found"))
        
        if (extension.status != com.kprflow.enterprise.data.model.ExtensionStatus.PENDING) {
            return Result.failure(
                IllegalArgumentException("Extension request is not pending")
            )
        }
        
        // Reject extension
        return extensionRepository.rejectExtension(
            extensionId = extensionId,
            rejectedBy = rejectedBy,
            rejectionReason = rejectionReason
        )
    }
}

/**
 * Use Case for Getting Extension Validation
 */
class GetExtensionValidationUseCase @Inject constructor(
    private val extensionRepository: IExtensionRepository
) {
    suspend operator fun invoke(dossierId: String): Result<ExtensionValidation> {
        val canExtend = extensionRepository.canRequestExtension(dossierId).getOrNull()
            ?: return Result.failure(Exception("Failed to check extension eligibility"))
        
        val extensionCount = extensionRepository.getDossierExtensionCount(dossierId).getOrNull()
            ?: return Result.failure(Exception("Failed to get extension count"))
        
        val remainingExtensions = 3 - extensionCount
        
        val reason = when {
            !canExtend -> "Dossier is not eligible for extension"
            extensionCount >= 3 -> "Maximum extension limit reached"
            else -> "Extension can be requested"
        }
        
        return Result.success(
            ExtensionValidation(
                canExtend = canExtend && extensionCount < 3,
                reason = reason,
                remainingExtensions = remainingExtensions,
                maxExtensions = 3
            )
        )
    }
}

/**
 * Use Case for Getting Pending Extensions
 */
class GetPendingExtensionsUseCase @Inject constructor(
    private val extensionRepository: IExtensionRepository
) {
    suspend operator fun invoke() = extensionRepository.getPendingExtensions()
}

/**
 * Use Case for Getting Extension History
 */
class GetExtensionHistoryUseCase @Inject constructor(
    private val extensionRepository: IExtensionRepository
) {
    suspend operator fun invoke(dossierId: String) = extensionRepository.getExtensionHistory(dossierId)
}

/**
 * Use Case for Getting Extension Statistics
 */
class GetExtensionStatisticsUseCase @Inject constructor(
    private val extensionRepository: IExtensionRepository
) {
    suspend operator fun invoke() = extensionRepository.getExtensionStatistics()
}

/**
 * Use Case for Monitoring Extension Changes
 */
class MonitorExtensionChangesUseCase @Inject constructor(
    private val extensionRepository: IExtensionRepository
) {
    operator fun invoke() = extensionRepository.observePendingExtensions()
}

/**
 * Use Case for Monitoring Dossier Extensions
 */
class MonitorDossierExtensionsUseCase @Inject constructor(
    private val extensionRepository: IExtensionRepository
) {
    operator fun invoke(dossierId: String) = extensionRepository.observeDossierExtensions(dossierId)
}
