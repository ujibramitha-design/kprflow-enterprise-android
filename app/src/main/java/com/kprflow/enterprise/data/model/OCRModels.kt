package com.kprflow.enterprise.data.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class OCRDocumentType(
    val id: String,
    val documentName: String,
    val documentCode: String,
    val description: String,
    val processingTemplate: Map<String, List<String>>? = null,
    val autoFillMapping: Map<String, String>? = null,
    val isActive: Boolean = true
)

@Serializable
data class OCRProcessingLog(
    val id: String,
    val userId: String,
    val documentTypeId: String,
    val originalFilename: String,
    val fileUrl: String,
    val extractedData: Map<String, Any>,
    val confidenceScore: Double,
    val processingStatus: String, // 'PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'MANUAL_REVIEW'
    val autoFillStatus: String, // 'PENDING', 'SUCCESS', 'FAILED', 'PARTIAL'
    val autoFilledFields: List<String>,
    val manualReviewReason: String? = null,
    val processingTimeMs: Int,
    val errorMessage: String? = null,
    val createdAt: String,
    val processedAt: String? = null
)

@Serializable
data class NoSprData(
    val id: String,
    val processingLogId: String,
    val customerId: String,
    val kprDossierId: String? = null,
    val sprNumber: String,
    val customerName: String,
    val propertyAddress: String,
    val propertyPrice: BigDecimal,
    val approvalDate: String,
    val developerName: String,
    val verificationStatus: String, // 'PENDING', 'VERIFIED', 'REJECTED'
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class BonusMemoData(
    val id: String,
    val processingLogId: String,
    val customerId: String,
    val employeeName: String,
    val companyName: String,
    val bonusAmount: BigDecimal,
    val bonusDate: String,
    val bonusType: String,
    val department: String,
    val position: String,
    val incomeVariationId: String? = null,
    val verificationStatus: String, // 'PENDING', 'VERIFIED', 'REJECTED'
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class DocumentCategory(
    val id: String,
    val categoryName: String,
    val categoryCode: String,
    val description: String,
    val priority: Int,
    val autoAssignmentRules: Map<String, Any>,
    val isActive: Boolean = true
)

@Serializable
data class DocumentCategoryAssignment(
    val id: String,
    val processingLogId: String,
    val categoryId: String,
    val assignmentType: String, // 'AUTO', 'MANUAL', 'SYSTEM'
    val confidenceScore: Double,
    val assignedBy: String? = null,
    val assignedAt: String
)

@Serializable
data class OCRProcessingRequest(
    val userId: String,
    val documentTypeCode: String,
    val fileUrl: String,
    val originalFilename: String,
    val extractedData: Map<String, Any>,
    val confidenceScore: Double
)

@Serializable
data class OCRProcessingResponse(
    val success: Boolean,
    val message: String,
    val processingLogId: String? = null,
    val documentData: Map<String, Any>? = null,
    val autoFillStatus: String? = null,
    val autoFilledFields: List<String> = emptyList(),
    val assignedCategory: String? = null,
    val confidenceScore: Double? = null
)

@Serializable
data class NoSprOCRRequest(
    val userId: String,
    val fileUrl: String,
    val extractedData: Map<String, Any>,
    val confidenceScore: Double
)

@Serializable
data class NoSprOCRResponse(
    val success: Boolean,
    val message: String,
    val processingLogId: String,
    val noSprId: String,
    val autoFillStatus: String,
    val autoFilledFields: List<String>,
    val extractedData: Map<String, Any>
)

@Serializable
data class BonusMemoOCRRequest(
    val userId: String,
    val fileUrl: String,
    val extractedData: Map<String, Any>,
    val confidenceScore: Double
)

@Serializable
data class BonusMemoOCRResponse(
    val success: Boolean,
    val message: String,
    val processingLogId: String,
    val bonusMemoId: String,
    val incomeVariationId: String? = null,
    val autoFillStatus: String,
    val autoFilledFields: List<String>,
    val extractedData: Map<String, Any>
)

@Serializable
data class OCRProcessingSummary(
    val totalProcessed: Int,
    val successfulProcessing: Int,
    val failedProcessing: Int,
    val autoFillSuccess: Int,
    val autoFillPartial: Int,
    val averageConfidence: Double,
    val documentTypes: List<DocumentTypeSummary>
)

@Serializable
data class DocumentTypeSummary(
    val documentType: String,
    val count: Int,
    val successRate: Double
)

@Serializable
data class OCRDashboardData(
    val processingDate: String,
    val totalDocuments: Int,
    val successful: Int,
    val failed: Int,
    val autoFilled: Int,
    val avgConfidence: Double,
    val avgProcessingTime: Double,
    val documentName: String
)

@Serializable
data class DocumentCategorySummary(
    val categoryName: String,
    val categoryCode: String,
    val assignedDocuments: Int,
    val avgConfidence: Double,
    val autoAssigned: Int,
    val manuallyAssigned: Int
)

// OCR Field Extraction Models
@Serializable
data class NoSprExtractedFields(
    val sprNumber: String? = null,
    val customerName: String? = null,
    val propertyAddress: String? = null,
    val propertyPrice: BigDecimal? = null,
    val approvalDate: String? = null,
    val developerName: String? = null
)

@Serializable
data class BonusMemoExtractedFields(
    val employeeName: String? = null,
    val companyName: String? = null,
    val bonusAmount: BigDecimal? = null,
    val bonusDate: String? = null,
    val bonusType: String? = null,
    val department: String? = null,
    val position: String? = null
)

// OCR Configuration
@Serializable
data class OCRConfiguration(
    val enabledDocumentTypes: List<String>,
    val confidenceThreshold: Double,
    val autoFillThreshold: Int,
    val maxFileSize: Int, // in MB
    val supportedFormats: List<String>
)

// OCR Validation Models
@Serializable
data class OCRValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val confidenceScore: Double,
    val extractedFields: Map<String, Any>
)

// OCR Processing Queue
@Serializable
data class OCRProcessingQueue(
    val id: String,
    val userId: String,
    val documentType: String,
    val fileUrl: String,
    val priority: Int,
    val status: String, // 'QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED'
    val queuedAt: String,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val estimatedProcessingTime: Int? = null // in seconds
)

// OCR Analytics
@Serializable
data class OCRAnalytics(
    val dailyProcessingCount: Int,
    val averageProcessingTime: Double,
    val successRate: Double,
    val averageConfidence: Double,
    val topDocumentTypes: List<DocumentTypeAnalytics>,
    val errorBreakdown: List<ErrorAnalytics>
)

@Serializable
data class DocumentTypeAnalytics(
    val documentType: String,
    val count: Int,
    val successRate: Double,
    val averageConfidence: Double
)

@Serializable
data class ErrorAnalytics(
    val errorType: String,
    val count: Int,
    val percentage: Double,
    val commonCauses: List<String>
)

// OCR Integration Events
@Serializable
data class OCRProcessingEvent(
    val eventType: String, // 'STARTED', 'COMPLETED', 'FAILED', 'VERIFIED'
    val processingLogId: String,
    val userId: String,
    val documentType: String,
    val timestamp: String,
    val data: Map<String, Any>? = null
)

// OCR Template Management
@Serializable
data class OCRTemplate(
    val id: String,
    val documentTypeCode: String,
    val templateName: String,
    val fieldMappings: Map<String, FieldMapping>,
    val validationRules: List<ValidationRule>,
    val isActive: Boolean = true
)

@Serializable
data class FieldMapping(
    val fieldName: String,
    val ocrFieldPath: String,
    val dataType: String, // 'STRING', 'NUMBER', 'DATE', 'BOOLEAN'
    val required: Boolean = false,
    val validationPattern: String? = null
)

@Serializable
data class ValidationRule(
    val fieldName: String,
    val ruleType: String, // 'REQUIRED', 'FORMAT', 'RANGE', 'CUSTOM'
    val ruleValue: String,
    val errorMessage: String
)
