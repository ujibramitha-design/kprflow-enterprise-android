package com.kprflow.enterprise.data.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Akad Readiness Data Model
 */
@Serializable
data class AkadReadiness(
    val id: String,
    val dossierId: String,
    val customerName: String,
    val dossierStatus: String,
    val bookingDate: String,
    val block: String? = null,
    val unitNumber: String? = null,
    val unitType: String? = null,
    val unitPrice: BigDecimal? = null,
    
    // Individual Verification Status
    val isPphPaid: Boolean = false,
    val isBphtbPaid: Boolean = false,
    val isAjbdraftReady: Boolean = false,
    val isSprFinalSigned: Boolean = false,
    val isBastReady: Boolean = false,
    val isInsurancePaid: Boolean = false,
    
    // Verification Timestamps
    val pphPaidAt: String? = null,
    val bphtbPaidAt: String? = null,
    val ajbdraftReadyAt: String? = null,
    val sprFinalSignedAt: String? = null,
    val bastReadyAt: String? = null,
    val insurancePaidAt: String? = null,
    
    // Verification Personnel
    val pphPaidBy: String? = null,
    val bphtbPaidBy: String? = null,
    val ajbdraftReadyBy: String? = null,
    val sprFinalSignedBy: String? = null,
    val bastReadyBy: String? = null,
    val insurancePaidBy: String? = null,
    
    // Readiness Calculation
    val isReadyForAkad: Boolean = false,
    val completionPercentage: Double = 0.0,
    val missingVerifications: List<String> = emptyList(),
    
    // Department Status
    val financeStatus: String = "PENDING",
    val legalStatus: String = "PENDING",
    val marketingStatus: String = "PENDING",
    
    // Overall Status
    val verificationStatus: String = "PENDING",
    val verificationCompletedAt: String? = null,
    val verificationCompletedBy: String? = null,
    
    // Update Info
    val updatedAt: String,
    val lastUpdatedBy: String? = null
)

/**
 * Verification Status Data Model
 */
@Serializable
data class VerificationStatus(
    val id: String,
    val dossierId: String,
    val customerName: String,
    val block: String,
    val unitNumber: String,
    val unitType: String,
    val unitPrice: BigDecimal,
    
    // Verification Fields
    val isPphPaid: Boolean,
    val isBphtbPaid: Boolean,
    val isAjbdraftReady: Boolean,
    val isSprFinalSigned: Boolean,
    val isBastReady: Boolean,
    val isInsurancePaid: Boolean,
    
    // Completion Metrics
    val completionPercentage: Double,
    val isReadyForAkad: Boolean,
    val missingVerifications: List<String>,
    
    // Department Status
    val financeStatus: String,
    val legalStatus: String,
    val marketingStatus: String,
    
    // Overall Status
    val verificationStatus: String,
    val verificationCompletedAt: String? = null,
    val updatedAt: String
)

/**
 * Department Verification Summary Data Model
 */
@Serializable
data class DepartmentVerificationSummary(
    val department: String,
    val totalDossiers: Int,
    val completedDossiers: Int,
    val pendingDossiers: Int,
    val completionRate: Double,
    val averageCompletionTime: Double,
    val lastCompletedAt: String? = null,
    val verificationFields: List<DepartmentFieldStatus>
)

/**
 * Department Field Status Data Model
 */
@Serializable
data class DepartmentFieldStatus(
    val fieldName: String,
    val fieldType: String,
    val completedCount: Int,
    val totalCount: Int,
    val completionRate: Double,
    val lastCompletedAt: String? = null
)

/**
 * Verification Update Data Model
 */
@Serializable
data class VerificationUpdate(
    val dossierId: String,
    val verificationType: VerificationType,
    val isCompleted: Boolean,
    val updatedBy: String,
    val updatedAt: String,
    val notes: String? = null,
    val documentUrl: String? = null,
    val amount: BigDecimal? = null
)

/**
 * Verification Type Enum
 */
@Serializable
enum class VerificationType {
    PPH_PAID,
    BPHTB_PAID,
    AJB_DRAFT_READY,
    SPR_FINAL_SIGNED,
    BAST_READY,
    INSURANCE_PAID
}

/**
 * Department Enum
 */
@Serializable
enum class Department {
    FINANCE,
    LEGAL,
    MARKETING
}

/**
 * Verification Form Data Model
 */
@Serializable
data class VerificationForm(
    val dossierId: String,
    val customerName: String,
    val verificationType: VerificationType,
    val isCompleted: Boolean = false,
    val notes: String = "",
    val documentUrl: String = "",
    val amount: BigDecimal? = null
) {
    fun isValid(): Boolean {
        return when (verificationType) {
            VerificationType.PPH_PAID, VerificationType.BPHTB_PAID, VerificationType.INSURANCE_PAID -> {
                isCompleted && (amount != null && amount > BigDecimal.ZERO) && documentUrl.isNotBlank()
            }
            VerificationType.AJB_DRAFT_READY, VerificationType.SPR_FINAL_SIGNED, VerificationType.BAST_READY -> {
                isCompleted && documentUrl.isNotBlank()
            }
        }
    }
    
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (isCompleted) {
            when (verificationType) {
                VerificationType.PPH_PAID, VerificationType.BPHTB_PAID, VerificationType.INSURANCE_PAID -> {
                    if (amount == null || amount <= BigDecimal.ZERO) {
                        errors.add("Amount is required and must be greater than 0")
                    }
                    if (documentUrl.isBlank()) {
                        errors.add("Document URL is required")
                    }
                }
                VerificationType.AJB_DRAFT_READY, VerificationType.SPR_FINAL_SIGNED, VerificationType.BAST_READY -> {
                    if (documentUrl.isBlank()) {
                        errors.add("Document URL is required")
                    }
                }
            }
        }
        
        return errors
    }
    
    fun getDisplayName(): String {
        return when (verificationType) {
            VerificationType.PPH_PAID -> "PPh Payment"
            VerificationType.BPHTB_PAID -> "BPHTB Payment"
            VerificationType.AJB_DRAFT_READY -> "AJB Draft Ready"
            VerificationType.SPR_FINAL_SIGNED -> "SPR Final Signed"
            VerificationType.BAST_READY -> "BAST Ready"
            VerificationType.INSURANCE_PAID -> "Insurance Paid"
        }
    }
}

/**
 * Akad Finalization Data Model
 */
@Serializable
data class AkadFinalization(
    val dossierId: String,
    val customerName: String,
    val block: String,
    val unitNumber: String,
    val scheduledDate: String,
    val finalizedBy: String,
    val finalizedAt: String,
    val notes: String? = null,
    val verificationStatus: List<VerificationStatus>
)

/**
 * Verification Audit Trail Data Model
 */
@Serializable
data class VerificationAuditTrail(
    val id: String,
    val dossierId: String,
    val verificationType: VerificationType,
    val action: String, // CREATED, UPDATED, COMPLETED
    val previousValue: Boolean? = null,
    val newValue: Boolean,
    val updatedBy: String,
    val updatedByName: String,
    val updatedByRole: String,
    val updatedAt: String,
    val notes: String? = null,
    val documentUrl: String? = null,
    val amount: BigDecimal? = null
)

/**
 * Verification Notification Data Model
 */
@Serializable
data class VerificationNotification(
    val id: String,
    val dossierId: String,
    val customerName: String,
    val verificationType: VerificationType,
    val notificationType: NotificationType,
    val message: String,
    val recipientRole: String,
    val recipientId: String,
    val isRead: Boolean = false,
    val createdAt: String,
    val readAt: String? = null
)

/**
 * Notification Type Enum
 */
@Serializable
enum class NotificationType {
    VERIFICATION_COMPLETED,
    VERIFICATION_PENDING,
    AKAD_READY,
    AKAD_SCHEDULED,
    VERIFICATION_OVERDUE,
    DEPARTMENT_REMINDER
}

/**
 * Verification Metrics Data Model
 */
@Serializable
data class VerificationMetrics(
    val totalDossiers: Int,
    val readyDossiers: Int,
    val readinessPercentage: Double,
    val averageCompletionTime: Double,
    val departmentMetrics: Map<Department, DepartmentMetrics>,
    val verificationTypeMetrics: Map<VerificationType, VerificationTypeMetrics>,
    val trends: List<VerificationTrend>
)

/**
 * Department Metrics Data Model
 */
@Serializable
data class DepartmentMetrics(
    val department: Department,
    val totalVerifications: Int,
    val completedVerifications: Int,
    val completionRate: Double,
    val averageTime: Double,
    val lastCompletedAt: String? = null
)

/**
 * Verification Type Metrics Data Model
 */
@Serializable
data class VerificationTypeMetrics(
    val verificationType: VerificationType,
    val totalCount: Int,
    val completedCount: Int,
    val completionRate: Double,
    val averageTime: Double,
    val lastCompletedAt: String? = null
)

/**
 * Verification Trend Data Model
 */
@Serializable
data class VerificationTrend(
    val period: String,
    val totalCompleted: Int,
    val readinessRate: Double,
    val departmentBreakdown: Map<Department, Int>
)
