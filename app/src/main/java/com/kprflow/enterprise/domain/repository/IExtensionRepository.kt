package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.ExtensionRequest
import com.kprflow.enterprise.data.model.ExtensionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Extension Repository
 * Handles dossier extension requests and approvals
 */
interface IExtensionRepository {
    // Extension Request Operations
    suspend fun requestExtension(
        dossierId: String,
        days: Int,
        reason: String,
        requestedBy: String
    ): Result<ExtensionRequest>
    
    suspend fun approveExtension(
        extensionId: String,
        approvedBy: String,
        approvalNotes: String?
    ): Result<Boolean>
    
    suspend fun rejectExtension(
        extensionId: String,
        rejectedBy: String,
        rejectionReason: String
    ): Result<Boolean>
    
    // Query Operations
    suspend fun getPendingExtensions(): Result<List<ExtensionRequest>>
    suspend fun getExtensionHistory(dossierId: String): Result<List<ExtensionRequest>>
    suspend fun getExtensionById(extensionId: String): Result<ExtensionRequest>
    suspend fun getDossierExtensionCount(dossierId: String): Result<Int>
    
    // Validation Operations
    suspend fun canRequestExtension(dossierId: String): Result<Boolean>
    suspend fun isEligibleForExtension(dossierId: String): Result<Boolean>
    
    // Real-time Operations
    fun observePendingExtensions(): Flow<List<ExtensionRequest>>
    fun observeDossierExtensions(dossierId: String): Flow<List<ExtensionRequest>>
    
    // Statistics
    suspend fun getExtensionStatistics(): Result<ExtensionStatistics>
}
