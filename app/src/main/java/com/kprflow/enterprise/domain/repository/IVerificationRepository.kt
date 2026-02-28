package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.AkadReadiness
import com.kprflow.enterprise.data.model.DepartmentVerificationSummary
import com.kprflow.enterprise.data.model.VerificationStatus
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Verification Repository
 * Handles cross-department verification status management
 */
interface IVerificationRepository {
    // Akad Readiness Operations
    suspend fun getAkadReadiness(dossierId: String): Result<AkadReadiness>
    suspend fun getAllAkadReadiness(): Result<List<AkadReadiness>>
    suspend fun getReadyForAkad(): Result<List<AkadReadiness>>
    suspend fun getPendingAkad(): Result<List<AkadReadiness>>
    
    // Verification Update Operations
    suspend fun updateVerification(
        dossierId: String,
        verificationType: com.kprflow.enterprise.domain.usecase.verification.VerificationType,
        isCompleted: Boolean,
        updatedBy: String,
        notes: String? = null,
        documentUrl: String? = null,
        amount: Double? = null
    ): Result<Boolean>
    
    // Akad Finalization Operations
    suspend fun finalizeAkadSchedule(
        dossierId: String,
        finalizedBy: String,
        scheduledDate: String,
        notes: String? = null
    ): Result<Boolean>
    
    // Department Status Operations
    suspend fun getDepartmentVerificationStatus(department: com.kprflow.enterprise.domain.usecase.verification.Department): Result<DepartmentVerificationSummary>
    suspend fun getAllVerificationStatuses(): Result<List<VerificationStatus>>
    
    // Summary Operations
    suspend fun getVerificationSummary(): Result<VerificationSummary>
    
    // Filter Operations
    suspend fun getVerificationStatusesByRole(userRole: String): Result<List<VerificationStatus>>
    suspend fun getVerificationStatusesByStatus(status: String): Result<List<VerificationStatus>>
    suspend fun getVerificationStatusesByDepartment(department: com.kprflow.enterprise.domain.usecase.verification.Department): Result<List<VerificationStatus>>
    
    // Real-time Operations
    fun observeVerificationChanges(dossierId: String): Flow<AkadReadiness>
    fun observeDepartmentSummary(): Flow<VerificationSummary>
    fun observeReadinessChanges(): Flow<List<AkadReadiness>>
    
    // Validation Operations
    suspend fun validateVerificationAccess(
        userId: String,
        verificationType: com.kprflow.enterprise.domain.usecase.verification.VerificationType
    ): Result<Boolean>
    
    // Statistics
    suspend fun getVerificationStatistics(): Result<VerificationStatistics>
}

// Data classes for verification operations
data class VerificationSummary(
    val totalDossiers: Int,
    val readyDossiers: Int,
    val pendingDossiers: Int,
    val avgCompletionPercentage: Double,
    val maxCompletionPercentage: Int,
    val minCompletionPercentage: Int,
    val pphCompleted: Int,
    val bphtbCompleted: Int,
    val ajbCompleted: Int,
    val sprCompleted: Int,
    val bastCompleted: Int,
    val insuranceCompleted: Int,
    val pphCompletionRate: Double,
    val bphtbCompletionRate: Double,
    val ajbCompletionRate: Double,
    val sprCompletionRate: Double,
    val bastCompletionRate: Double,
    val insuranceCompletionRate: Double,
    val statusReady: Int,
    val statusInProgress: Int,
    val statusPending: Int,
    val statusBlocked: Int,
    val readinessPercentage: Double,
    val reportDate: String
)

data class VerificationStatistics(
    val totalVerifications: Int,
    val completedVerifications: Int,
    val pendingVerifications: Int,
    val verificationByType: Map<com.kprflow.enterprise.domain.usecase.verification.VerificationType, Int>,
    val verificationByDepartment: Map<com.kprflow.enterprise.domain.usecase.verification.Department, Int>,
    val averageCompletionTime: Double,
    val fastestCompletion: String,
    val slowestCompletion: String,
    val completionRate: Double,
    val blockedCount: Int
)
