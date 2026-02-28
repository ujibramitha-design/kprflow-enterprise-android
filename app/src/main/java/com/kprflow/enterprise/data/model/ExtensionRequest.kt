package com.kprflow.enterprise.data.model

import kotlinx.serialization.Serializable

/**
 * Extension Request Data Model
 * Represents a dossier extension request
 */
@Serializable
data class ExtensionRequest(
    val id: String,
    val dossierId: String,
    val customerName: String,
    val extensionDays: Int,
    val extensionReason: String,
    val extendedBy: String,
    val extendedByName: String,
    val extensionDate: String,
    val previousDeadline: String,
    val newDeadline: String,
    val status: ExtensionStatus,
    val approvedBy: String? = null,
    val approvedByName: String? = null,
    val approvalDate: String? = null,
    val rejectionReason: String? = null,
    val dossierStatus: String,
    val extensionCount: Int
)

/**
 * Extension Status Enum
 */
@Serializable
enum class ExtensionStatus {
    PENDING,
    APPROVED,
    REJECTED
}

/**
 * Extension Statistics Data Model
 */
@Serializable
data class ExtensionStatistics(
    val totalRequests: Int,
    val pendingRequests: Int,
    val approvedRequests: Int,
    val rejectedRequests: Int,
    val averageExtensionDays: Double,
    val mostCommonReason: String,
    val extensionsByMonth: Map<String, Int>
)

/**
 * Extension Validation Result
 */
@Serializable
data class ExtensionValidation(
    val canExtend: Boolean,
    val reason: String,
    val remainingExtensions: Int,
    val maxExtensions: Int = 3
)

/**
 * Extension Request Form Data
 */
@Serializable
data class ExtensionRequestForm(
    val dossierId: String,
    val customerName: String,
    val currentDeadline: String,
    val extensionDays: Int = 30,
    val extensionReason: String = "",
    val requestedBy: String
) {
    fun isValid(): Boolean {
        return extensionReason.isNotBlank() &&
               extensionReason.length >= 10 &&
               extensionDays in 1..30
    }
    
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (extensionReason.isBlank()) {
            errors.add("Extension reason is required")
        }
        
        if (extensionReason.length < 10) {
            errors.add("Extension reason must be at least 10 characters")
        }
        
        if (extensionDays !in 1..30) {
            errors.add("Extension days must be between 1 and 30")
        }
        
        return errors
    }
}
