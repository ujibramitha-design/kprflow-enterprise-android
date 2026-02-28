package com.kprflow.enterprise.data.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Legal Document Status Data Model
 */
@Serializable
data class LegalDocumentStatus(
    val unitId: String,
    val block: String,
    val unitNumber: String,
    val unitType: String? = null,
    val unitStatus: String,
    val unitPrice: BigDecimal? = null,
    
    // Document URLs
    val shgbUrl: String? = null,
    val pbgUrl: String? = null,
    val imbUrl: String? = null,
    
    // Sync Status
    val legalSyncAt: String? = null,
    val legalSyncStatus: String,
    val legalSyncError: String? = null,
    
    // Document Availability
    val hasSHGB: Boolean = false,
    val hasPBG: Boolean = false,
    val hasIMB: Boolean = false,
    
    // Document Metrics
    val documentCompletenessScore: Int,
    val legalReadinessStatus: LegalReadinessStatus,
    val lastDocumentSync: String? = null,
    val totalSyncAttempts: Int = 0
)

/**
 * Legal Readiness Status Enum
 */
@Serializable
enum class LegalReadinessStatus {
    READY,      // All documents available
    PARTIAL,    // Some documents available
    INCOMPLETE  // No documents available
}

/**
 * Legal Dashboard Summary Data Model
 */
@Serializable
data class LegalDashboardSummary(
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
    val maxCompletenessScore: Int,
    val minCompletenessScore: Int,
    val readinessPercentage: Double,
    val shgbCoveragePercentage: Double,
    val pbgCoveragePercentage: Double,
    val imbCoveragePercentage: Double,
    val syncedLast7Days: Int,
    val syncedLast30Days: Int,
    val unitsWithErrors: Int,
    val reportDate: String
)

/**
 * Document Type Enum
 */
@Serializable
enum class DocumentType {
    SHGB,  // Surat Hak Guna Bangunan
    PBG,   // Persetujuan Bangunan Gedung
    IMB    // Izin Mendirikan Bangunan
}

/**
 * Document Sync Log Data Model
 */
@Serializable
data class DocumentSyncLog(
    val id: String,
    val unitId: String,
    val documentType: DocumentType,
    val fileName: String,
    val gdriveFileId: String? = null,
    val gdriveUrl: String? = null,
    val supabaseUrl: String? = null,
    val syncStatus: String,
    val syncError: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val syncedAt: String? = null
)

/**
 * Document Upload Form Data
 */
@Serializable
data class DocumentUploadForm(
    val unitId: String,
    val block: String,
    val unitNumber: String,
    val documentType: DocumentType,
    val fileName: String,
    val fileUrl: String,
    val fileSize: Long,
    val mimeType: String
) {
    fun isValid(): Boolean {
        return fileName.isNotBlank() &&
               fileUrl.isNotBlank() &&
               fileSize > 0 &&
               mimeType.isNotBlank()
    }
    
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (fileName.isBlank()) {
            errors.add("File name is required")
        }
        
        if (fileUrl.isBlank()) {
            errors.add("File URL is required")
        }
        
        if (fileSize <= 0) {
            errors.add("File size must be greater than 0")
        }
        
        if (mimeType.isBlank()) {
            errors.add("MIME type is required")
        }
        
        return errors
    }
}

/**
 * Document Verification Data
 */
@Serializable
data class DocumentVerification(
    val documentId: String,
    val verifiedBy: String,
    val verificationNotes: String? = null,
    val verificationStatus: DocumentVerificationStatus,
    val verifiedAt: String = java.time.Instant.now().toString()
)

/**
 * Document Verification Status Enum
 */
@Serializable
enum class DocumentVerificationStatus {
    VERIFIED,
    REJECTED,
    PENDING_REVIEW
}

/**
 * Legal Document Filter Options
 */
@Serializable
data class LegalDocumentFilter(
    val readinessStatus: LegalReadinessStatus? = null,
    val documentType: DocumentType? = null,
    val syncStatus: String? = null,
    val block: String? = null,
    val hasErrors: Boolean? = null,
    val lastSyncAfter: String? = null
)

/**
 * Document Analytics Data
 */
@Serializable
data class DocumentAnalytics(
    val totalDocuments: Int,
    val documentsByType: Map<DocumentType, Int>,
    val documentsByStatus: Map<LegalReadinessStatus, Int>,
    val syncSuccessRate: Double,
    val averageSyncTime: Double,
    val mostRecentSync: String,
    val errorRate: Double,
    val coverageByDocumentType: Map<DocumentType, Double>
)

/**
 * Document Storage Info
 */
@Serializable
data class DocumentStorageInfo(
    val bucketName: String,
    val filePath: String,
    publicUrl: String,
    signedUrl: String? = null,
    expiresAt: String? = null
)

/**
 * Legal Document Export Data
 */
@Serializable
data class LegalDocumentExport(
    val unitId: String,
    val block: String,
    val unitNumber: String,
    val unitType: String,
    val unitPrice: BigDecimal,
    val shgbStatus: String,
    val shgbUrl: String?,
    val pbgStatus: String,
    val pbgUrl: String?,
    val imbStatus: String,
    val imbUrl: String?,
    val legalReadinessStatus: String,
    val documentCompletenessScore: Int,
    val lastSyncDate: String,
    val syncStatus: String
)
