package com.kprflow.enterprise.legal.validator

import com.kprflow.enterprise.domain.model.*
import com.kprflow.enterprise.legal.model.*
import com.kprflow.enterprise.legal.request.*
import java.util.*

/**
 * PPJB Validator - Business Logic Validation
 * Phase 16: Legal & Documentation Automation
 */
class PPJBValidator {
    
    /**
     * Validate create PPJB request
     */
    fun validateCreateRequest(request: CreatePPJBRequest): Result<Unit> {
        return when {
            request.dossierId.isBlank() -> {
                Result.failure(PPJBValidationException("Dossier ID is required"))
            }
            request.scheduledDate?.before(Date()) == true -> {
                Result.failure(PPJBValidationException("Scheduled date cannot be in the past"))
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate update PPJB request
     */
    fun validateUpdateRequest(request: UpdatePPJBRequest): Result<Unit> {
        return when {
            request.scheduledDate?.before(Date()) == true -> {
                Result.failure(PPJBValidationException("Scheduled date cannot be in the past"))
            }
            request.expiryDate?.before(request.scheduledDate ?: Date()) == true -> {
                Result.failure(PPJBValidationException("Expiry date cannot be before scheduled date"))
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate PPJB eligibility
     */
    fun validatePPJBEligibility(dossier: KprDossier): Result<Unit> {
        return when {
            dossier.currentStatus != DossierStatus.APPROVAL_BANK -> {
                Result.failure(PPJBValidationException("Dossier must be in APPROVAL_BANK status"))
            }
            dossier.loanAmount < 0 -> {
                Result.failure(PPJBValidationException("Invalid loan amount"))
            }
            dossier.customerId.isBlank() -> {
                Result.failure(PPJBValidationException("Customer ID is required"))
            }
            dossier.unitPropertyId.isBlank() -> {
                Result.failure(PPJBValidationException("Unit Property ID is required"))
            }
            dossier.monthlyIncome <= 0 -> {
                Result.failure(PPJBValidationException("Monthly income must be greater than 0"))
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate PPJB document generation
     */
    fun validatePPJBDocumentGeneration(process: PPJBDeveloperProcess): Result<Unit> {
        return when {
            process.status == PPJBStatus.CANCELLED -> {
                Result.failure(PPJBValidationException("Cannot generate document for cancelled PPJB"))
            }
            process.status == PPJBStatus.COMPLETED -> {
                Result.failure(PPJBValidationException("Document already generated"))
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate PPJB reminder
     */
    fun validatePPJBReminder(process: PPJBDeveloperProcess): Result<Unit> {
        return when {
            process.reminderCount >= process.maxReminders -> {
                Result.failure(PPJBValidationException("Maximum reminders reached"))
            }
            process.status == PPJBStatus.CANCELLED -> {
                Result.failure(PPJBValidationException("Cannot send reminder for cancelled PPJB"))
            }
            process.status == PPJBStatus.COMPLETED -> {
                Result.failure(PPJBValidationException("Cannot send reminder for completed PPJB"))
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate PPJB invitation
     */
    fun validatePPJBInvitation(
        process: PPJBDeveloperProcess,
        invitationDate: Date
    ): Result<Unit> {
        return when {
            process.status == PPJBStatus.CANCELLED -> {
                Result.failure(PPJBValidationException("Cannot generate invitation for cancelled PPJB"))
            }
            invitationDate.before(Date()) -> {
                Result.failure(PPJBValidationException("Invitation date cannot be in the past"))
            }
            invitationDate.after(process.expiryDate) -> {
                Result.failure(PPJBValidationException("Invitation date cannot be after PPJB expiry"))
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate PPJB cancellation
     */
    fun validatePPJBCancellation(
        process: PPJBDeveloperProcess,
        reason: String
    ): Result<Unit> {
        return when {
            process.status == PPJBStatus.COMPLETED -> {
                Result.failure(PPJBValidationException("Cannot cancel completed PPJB"))
            }
            process.status == PPJBStatus.CANCELLED -> {
                Result.failure(PPJBValidationException("PPJB already cancelled"))
            }
            reason.isBlank() -> {
                Result.failure(PPJBValidationException("Cancellation reason is required"))
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate PPJB date range
     */
    fun validatePPJBDateRange(
        scheduledDate: Date,
        expiryDate: Date
    ): Result<Unit> {
        val daysBetween = (expiryDate.time - scheduledDate.time) / (24 * 60 * 60 * 1000)
        
        return when {
            scheduledDate.before(Date()) -> {
                Result.failure(PPJBValidationException("Scheduled date cannot be in the past"))
            }
            expiryDate.before(scheduledDate) -> {
                Result.failure(PPJBValidationException("Expiry date cannot be before scheduled date"))
            }
            daysBetween < 7 -> {
                Result.failure(PPJBValidationException("PPJB period must be at least 7 days"))
            }
            daysBetween > 60 -> {
                Result.failure(PPJBValidationException("PPJB period cannot exceed 60 days"))
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate customer data for PPJB
     */
    fun validateCustomerData(customer: UserProfile): Result<Unit> {
        return when {
            customer.fullName.isBlank() -> {
                Result.failure(PPJBValidationException("Customer full name is required"))
            }
            customer.phone.isBlank() -> {
                Result.failure(PPJBValidationException("Customer phone number is required"))
            }
            customer.email.isBlank() -> {
                Result.failure(PPJBValidationException("Customer email is required"))
            }
            !isValidEmail(customer.email) -> {
                Result.failure(PPJBValidationException("Invalid email format"))
            }
            !isValidPhone(customer.phone) -> {
                Result.failure(PPJBValidationException("Invalid phone number format"))
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate unit data for PPJB
     */
    fun validateUnitData(unit: UnitProperty): Result<Unit> {
        return when {
            unit.projectName.isBlank() -> {
                Result.failure(PPJBValidationException("Project name is required"))
            }
            unit.block.isBlank() -> {
                Result.failure(PPJBValidationException("Block is required"))
            }
            unit.unitNumber.isBlank() -> {
                Result.failure(PPJBValidationException("Unit number is required"))
            }
            unit.unitType.isBlank() -> {
                Result.failure(PPJBValidationException("Unit type is required"))
            }
            unit.price <= 0 -> {
                Result.failure(PPJBValidationException("Unit price must be greater than 0"))
            }
            unit.status != UnitStatus.AVAILABLE && unit.status != UnitStatus.RESERVED -> {
                Result.failure(PPJBValidationException("Unit must be available or reserved"))
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate PPJB document content
     */
    fun validatePPJBDocumentContent(content: String): Result<Unit> {
        return when {
            content.isBlank() -> {
                Result.failure(PPJBValidationException("Document content cannot be empty"))
            }
            content.length < 100 -> {
                Result.failure(PPJBValidationException("Document content too short"))
            }
            !containsRequiredFields(content) -> {
                Result.failure(PPJBValidationException("Document missing required fields"))
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate PPJB reminder content
     */
    fun validateReminderContent(content: String): Result<Unit> {
        return when {
            content.isBlank() -> {
                Result.failure(PPJBValidationException("Reminder content cannot be empty"))
            }
            content.length < 20 -> {
                Result.failure(PPJBValidationException("Reminder content too short"))
            }
            content.length > 500 -> {
                Result.failure(PPJBValidationException("Reminder content too long"))
            }
            else -> Result.success(Unit)
        }
    }
    
    // Private helper methods
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    private fun isValidPhone(phone: String): Boolean {
        val cleanPhone = phone.replace("[^0-9+]".toRegex(), "")
        return cleanPhone.length >= 10 && cleanPhone.startsWith("+")
    }
    
    private fun containsRequiredFields(content: String): Boolean {
        val requiredFields = listOf(
            "PERJANJIAN PENGIKATAN JUAL BELI",
            "PIHAK PERTAMA",
            "PIHAK KEDUA",
            "DATA UNIT",
            "TANDA TANGAN"
        )
        
        return requiredFields.all { field ->
            content.contains(field, ignoreCase = true)
        }
    }
}

/**
 * PPJB Validation Exception
 */
class PPJBValidationException(message: String) : Exception(message)
