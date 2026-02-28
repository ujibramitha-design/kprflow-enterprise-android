package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.LegalDocumentStatus
import com.kprflow.enterprise.data.model.LegalDashboardSummary
import com.kprflow.enterprise.data.model.DocumentSyncResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Legal Document Repository
 * Handles legal document management and sync operations
 */
interface ILegalDocumentRepository {
    // Document Status Operations
    suspend fun getLegalDocumentStatus(unitId: String): Result<LegalDocumentStatus>
    suspend fun getAllLegalDocumentStatuses(): Result<List<LegalDocumentStatus>>
    suspend fun getLegalDocumentStatusesByBlock(block: String): Result<List<LegalDocumentStatus>>
    
    // Dashboard Operations
    suspend fun getLegalDashboardSummary(): Result<LegalDashboardSummary>
    
    // Document URL Operations
    suspend fun updateDocumentUrl(
        unitId: String,
        documentType: String,
        url: String
    ): Result<Boolean>
    
    suspend fun getDocumentUrl(
        unitId: String,
        documentType: String
    ): Result<String>
    
    // Sync Operations
    suspend fun triggerDocumentSync(): Result<DocumentSyncResult>
    suspend fun getSyncStatus(): Result<DocumentSyncResult>
    suspend fun forceDocumentSync(): Result<DocumentSyncResult>
    
    // Filter Operations
    suspend fun getUnitsByReadinessStatus(status: String): Result<List<LegalDocumentStatus>>
    suspend fun getUnitsWithMissingDocuments(): Result<List<LegalDocumentStatus>>
    suspend fun getUnitsReadyForProcessing(): Result<List<LegalDocumentStatus>>
    
    // Real-time Operations
    fun observeDocumentStatusChanges(unitId: String): Flow<LegalDocumentStatus>
    fun observeDashboardSummary(): Flow<LegalDashboardSummary>
    fun observeSyncStatus(): Flow<DocumentSyncResult>
    
    // Statistics
    suspend fun getDocumentStatistics(): Result<LegalDocumentStatistics>
}

// Data classes for legal document operations
data class LegalDocumentStatistics(
    val totalUnits: Int,
    val unitsWithSHGB: Int,
    val unitsWithPBG: Int,
    val unitsWithIMB: Int,
    val readyUnits: Int,
    val partialUnits: Int,
    val incompleteUnits: Int,
    val syncedUnits: Int,
    val pendingUnits: Int,
    val errorUnits: Int,
    val avgCompletenessScore: Double,
    val readinessPercentage: Double,
    val shgbCoveragePercentage: Double,
    val pbgCoveragePercentage: Double,
    val imbCoveragePercentage: Double,
    val syncedLast7Days: Int,
    val syncedLast30Days: Int,
    val unitsWithErrors: Int
)

data class DocumentSyncResult(
    val success: Boolean,
    val message: String,
    val processed: Int,
    val errors: List<String>,
    val timestamp: String = java.time.Instant.now().toString()
)
