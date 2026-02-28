package com.kprflow.enterprise.legal.service

import com.kprflow.enterprise.domain.model.*
import com.kprflow.enterprise.domain.repository.*
import com.kprflow.enterprise.legal.model.*
import com.kprflow.enterprise.legal.repository.*
import com.kprflow.enterprise.legal.validator.*
import com.kprflow.enterprise.legal.generator.*
import com.kprflow.enterprise.legal.notification.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PPJB Developer Service - Refactored for Clean Architecture
 * Phase 16: Legal & Documentation Automation
 */
@Singleton
class PPJBDeveloperService @Inject constructor(
    private val ppjbRepository: PPJBRepository,
    private val dossierRepository: KprRepository,
    private val documentRepository: DocumentRepository,
    private val notificationRepository: NotificationRepository,
    private val ppjbValidator: PPJBValidator,
    private val ppjbDocumentGenerator: PPJBDocumentGenerator,
    private val ppjbNotificationService: PPJBNotificationService,
    private val ppjbReminderService: PPJBReminderService
) {

    /**
     * Get all PPJB processes with filtering
     */
    fun getPPJBProcesses(filter: PPJBFilter = PPJBFilter.ALL): Flow<Result<List<PPJBDeveloperProcess>>> {
        return ppjbRepository.getPPJBProcesses(filter)
            .map { Result.success(it) }
    }

    /**
     * Get PPJB process by ID
     */
    suspend fun getPPJBProcessById(id: String): Result<PPJBDeveloperProcess> {
        return try {
            val process = ppjbRepository.getPPJBProcessById(id)
                ?: return Result.failure(Exception("PPJB process not found"))
            Result.success(process)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create new PPJB process
     */
    suspend fun createPPJBProcess(
        request: CreatePPJBRequest
    ): Result<PPJBDeveloperProcess> {
        return try {
            // Validate request
            ppjbValidator.validateCreateRequest(request)
                .let { if (it.isFailure) return it }

            // Get dossier details
            val dossier = dossierRepository.getDossierById(request.dossierId)
                .getOrNull()
                ?: return Result.failure(Exception("Dossier not found"))

            // Validate PPJB eligibility
            ppjbValidator.validatePPJBEligibility(dossier)
                .let { if (it.isFailure) return it }

            // Create PPJB process
            val process = ppjbRepository.createPPJBProcess(
                PPJBDeveloperProcess(
                    id = java.util.UUID.randomUUID().toString(),
                    dossierId = request.dossierId,
                    customerId = dossier.customerId,
                    unitPropertyId = dossier.unitPropertyId,
                    ppjbType = determinePPJBType(dossier),
                    status = PPJBStatus.SCHEDULED,
                    scheduledDate = request.scheduledDate ?: calculateDefaultPPJBDate(),
                    expiryDate = calculateExpiryDate(request.scheduledDate ?: calculateDefaultPPJBDate()),
                    reminderCount = 0,
                    maxReminders = PPJBConstants.MAX_REMINDERS,
                    slaDays = PPJBConstants.SLA_DAYS,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Generate PPJB document
            val documentResult = ppjbDocumentGenerator.generatePPJBDocument(process)
            if (documentResult.isFailure) {
                return documentResult
            }

            // Send initial notification
            ppjbNotificationService.sendPPJBNotification(
                process = process,
                notificationType = PPJBNotificationType.INITIAL
            )

            // Schedule reminders
            ppjbReminderService.scheduleReminders(process)

            Result.success(process)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update PPJB process
     */
    suspend fun updatePPJBProcess(
        id: String,
        request: UpdatePPJBRequest
    ): Result<PPJBDeveloperProcess> {
        return try {
            // Validate request
            ppjbValidator.validateUpdateRequest(request)
                .let { if (it.isFailure) return it }

            // Get existing process
            val existingProcess = ppjbRepository.getPPJBProcessById(id)
                ?: return Result.failure(Exception("PPJB process not found"))

            // Update process
            val updatedProcess = existingProcess.copy(
                scheduledDate = request.scheduledDate ?: existingProcess.scheduledDate,
                expiryDate = request.expiryDate ?: existingProcess.expiryDate,
                status = request.status ?: existingProcess.status,
                updatedAt = System.currentTimeMillis()
            )

            ppjbRepository.updatePPJBProcess(updatedProcess)
            Result.success(updatedProcess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancel PPJB process
     */
    suspend fun cancelPPJBProcess(
        id: String,
        reason: String,
        notes: String? = null
    ): Result<Unit> {
        return try {
            val process = ppjbRepository.getPPJBProcessById(id)
                ?: return Result.failure(Exception("PPJB process not found"))

            // Update status
            ppjbRepository.updatePPJBStatus(id, PPJBStatus.CANCELLED)

            // Update unit status to available
            updateUnitStatus(process.unitPropertyId, UnitStatus.AVAILABLE)

            // Update dossier status
            updateDossierStatus(process.dossierId, DossierStatus.CANCELLED)

            // Send cancellation notification
            ppjbNotificationService.sendPPJBNotification(
                process = process,
                notificationType = PPJBNotificationType.CANCELLED
            )

            // Log cancellation
            logPPJBCancellation(process, reason, notes)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate PPJB document
     */
    suspend fun generatePPJBDocument(
        ppjbId: String
    ): Result<Document> {
        return try {
            val process = ppjbRepository.getPPJBProcessById(ppjbId)
                ?: return Result.failure(Exception("PPJB process not found"))

            ppjbDocumentGenerator.generatePPJBDocument(process)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate invitation
     */
    suspend fun generateInvitation(
        ppjbId: String,
        invitationDate: java.util.Date
    ): Result<Document> {
        return try {
            val process = ppjbRepository.getPPJBProcessById(ppjbId)
                ?: return Result.failure(Exception("PPJB process not found"))

            ppjbDocumentGenerator.generateInvitation(process, invitationDate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send reminder
     */
    suspend fun sendReminder(
        ppjbId: String
    ): Result<Unit> {
        return try {
            val process = ppjbRepository.getPPJBProcessById(ppjbId)
                ?: return Result.failure(Exception("PPJB process not found"))

            ppjbReminderService.sendReminder(process)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process expired PPJB processes
     */
    suspend fun processExpiredPPJBProcesses(): Result<Int> {
        return try {
            val expiredProcesses = ppjbRepository.getExpiredPPJBProcesses()
            var processedCount = 0

            expiredProcesses.forEach { process ->
                cancelPPJBProcess(
                    id = process.id,
                    reason = "AUTO_CANCELLED_EXPIRED",
                    notes = "PPJB expired on ${process.expiryDate}"
                )
                processedCount++
            }

            Result.success(processedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get PPJB statistics
     */
    fun getPPJBStatistics(): Flow<PPJBStatistics> {
        return ppjbRepository.getPPJBStatistics()
    }

    // Private helper methods
    private fun determinePPJBType(dossier: KprDossier): PPJBType {
        return if (dossier.loanAmount > 0) PPJBType.KPR else PPJBType.CASH_KERAS
    }

    private fun calculateDefaultPPJBDate(): java.util.Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_MONTH, PPJBConstants.DEFAULT_PPJB_DAYS)
        return calendar.time
    }

    private fun calculateExpiryDate(ppjbDate: java.util.Date): java.util.Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = ppjbDate
        calendar.add(java.util.Calendar.DAY_OF_MONTH, PPJBConstants.SLA_DAYS)
        return calendar.time
    }

    private suspend fun updateUnitStatus(unitId: String, status: UnitStatus) {
        // Update unit property status
        // Implementation depends on your unit repository
    }

    private suspend fun updateDossierStatus(dossierId: String, status: DossierStatus) {
        // Update dossier status
        // Implementation depends on your dossier repository
    }

    private suspend fun logPPJBCancellation(
        process: PPJBDeveloperProcess,
        reason: String,
        notes: String?
    ) {
        val logData = mapOf(
            "ppjb_id" to process.id,
            "dossier_id" to process.dossierId,
            "customer_id" to process.customerId,
            "unit_id" to process.unitPropertyId,
            "cancelled_at" to System.currentTimeMillis(),
            "reason" to reason,
            "notes" to notes,
            "reminder_count" to process.reminderCount
        )

        // Log to audit system
        // Implementation depends on your audit repository
    }
}

/**
 * PPJB Constants
 */
object PPJBConstants {
    const val SLA_DAYS = 30
    const val DEFAULT_PPJB_DAYS = 7
    const val MAX_REMINDERS = 2
    const val REMINDER_INTERVAL_DAYS = 7
}

/**
 * PPJB Filter
 */
enum class PPJBFilter {
    ALL, ACTIVE, COMPLETED, CANCELLED, EXPIRED
}

/**
 * PPJB Statistics
 */
data class PPJBStatistics(
    val totalProcesses: Int,
    val activeProcesses: Int,
    val completedProcesses: Int,
    val cancelledProcesses: Int,
    val expiredProcesses: Int,
    val avgProcessingDays: Double,
    val completionRate: Double,
    val cancellationRate: Double
)
