package com.kprflow.enterprise.legal.scheduler

import com.kprflow.enterprise.legal.model.*
import com.kprflow.enterprise.legal.repository.PPJBRepository
import com.kprflow.enterprise.legal.notification.PPJBNotificationService
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * PPJB Reminder Scheduler - Automated Reminder Management
 * Phase 16: Legal & Documentation Automation
 */
class PPJBReminderScheduler(
    private val ppjbRepository: PPJBRepository,
    private val notificationService: PPJBNotificationService
) {
    
    private val schedulerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val reminderJobs = mutableMapOf<String, Job>()
    
    /**
     * Schedule reminders for PPJB process
     */
    fun scheduleReminders(process: PPJBDeveloperProcess) {
        // Cancel existing jobs for this process
        cancelReminderJobs(process.id)
        
        // Schedule first reminder (7 days after scheduled date)
        val firstReminderDate = calculateReminderDate(process.scheduledDate, 7)
        if (firstReminderDate.after(Date())) {
            scheduleReminder(process, firstReminderDate, 1)
        }
        
        // Schedule second reminder (7 days before expiry)
        val secondReminderDate = calculateReminderDate(process.expiryDate, -7)
        if (secondReminderDate.after(Date())) {
            scheduleReminder(process, secondReminderDate, 2)
        }
        
        // Schedule expiry warning (3 days before expiry)
        val expiryWarningDate = calculateReminderDate(process.expiryDate, -3)
        if (expiryWarningDate.after(Date())) {
            scheduleExpiryWarning(process, expiryWarningDate)
        }
        
        // Schedule expiry check
        scheduleExpiryCheck(process)
    }
    
    /**
     * Schedule individual reminder
     */
    private fun scheduleReminder(
        process: PPJBDeveloperProcess,
        reminderDate: Date,
        reminderNumber: Int
    ) {
        val delay = reminderDate.time - System.currentTimeMillis()
        
        if (delay > 0) {
            val job = schedulerScope.launch {
                delay(delay)
                
                try {
                    // Check if process is still active
                    val currentProcess = ppjbRepository.getPPJBProcessById(process.id)
                    if (currentProcess != null && currentProcess.status == PPJBStatus.SCHEDULED) {
                        // Send reminder
                        notificationService.sendPPJBReminder(currentProcess, reminderNumber)
                        
                        // Update reminder count
                        updateReminderCount(process.id, reminderNumber)
                        
                        // Log reminder sent
                        logReminderSent(process.id, reminderNumber)
                    }
                } catch (e: Exception) {
                    // Log error but don't fail
                    logReminderError(process.id, reminderNumber, e.message ?: "Unknown error")
                }
            }
            
            reminderJobs["${process.id}_reminder_$reminderNumber"] = job
        }
    }
    
    /**
     * Schedule expiry warning
     */
    private fun scheduleExpiryWarning(
        process: PPJBDeveloperProcess,
        warningDate: Date
    ) {
        val delay = warningDate.time - System.currentTimeMillis()
        
        if (delay > 0) {
            val job = schedulerScope.launch {
                delay(delay)
                
                try {
                    // Check if process is still active
                    val currentProcess = ppjbRepository.getPPJBProcessById(process.id)
                    if (currentProcess != null && currentProcess.status == PPJBStatus.SCHEDULED) {
                        // Send expiry warning
                        notificationService.sendPPJBExpiryWarning(currentProcess)
                        
                        // Log warning sent
                        logExpiryWarningSent(process.id)
                    }
                } catch (e: Exception) {
                    // Log error but don't fail
                    logExpiryWarningError(process.id, e.message ?: "Unknown error")
                }
            }
            
            reminderJobs["${process.id}_expiry_warning"] = job
        }
    }
    
    /**
     * Schedule expiry check
     */
    private fun scheduleExpiryCheck(process: PPJBDeveloperProcess) {
        val delay = process.expiryDate.time - System.currentTimeMillis()
        
        if (delay > 0) {
            val job = schedulerScope.launch {
                delay(delay)
                
                try {
                    // Check if process is still active
                    val currentProcess = ppjbRepository.getPPJBProcessById(process.id)
                    if (currentProcess != null && currentProcess.status == PPJBStatus.SCHEDULED) {
                        // Auto-cancel PPJB
                        autoCancelPPJB(currentProcess)
                    }
                } catch (e: Exception) {
                    // Log error but don't fail
                    logExpiryCheckError(process.id, e.message ?: "Unknown error")
                }
            }
            
            reminderJobs["${process.id}_expiry_check"] = job
        }
    }
    
    /**
     * Cancel reminder jobs for process
     */
    fun cancelReminderJobs(processId: String) {
        reminderJobs.keys.filter { it.startsWith(processId) }.forEach { key ->
            reminderJobs[key]?.cancel()
            reminderJobs.remove(key)
        }
    }
    
    /**
     * Process all pending reminders
     */
    suspend fun processPendingReminders(): Result<Int> {
        return try {
            val activeProcesses = ppjbRepository.getPPJBProcessesByDateRange(
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30),
                System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
            )
            
            var processedCount = 0
            
            activeProcesses.forEach { process ->
                // Check if reminders should be sent
                val shouldSendReminder = shouldSendReminder(process)
                if (shouldSendReminder.first) {
                    notificationService.sendPPJBReminder(process, shouldSendReminder.second)
                    updateReminderCount(process.id, shouldSendReminder.second)
                    processedCount++
                }
                
                // Check if expiry warning should be sent
                if (shouldSendExpiryWarning(process)) {
                    notificationService.sendPPJBExpiryWarning(process)
                    processedCount++
                }
                
                // Check if process should be auto-cancelled
                if (shouldAutoCancel(process)) {
                    autoCancelPPJB(process)
                    processedCount++
                }
            }
            
            Result.success(processedCount)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get reminder statistics
     */
    suspend fun getReminderStatistics(): PPJBReminderStatistics {
        val activeProcesses = ppjbRepository.getPPJBProcessesByDateRange(
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30),
            System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
        )
        
        val totalReminders = activeProcesses.sumOf { it.reminderCount }
        val avgReminders = if (activeProcesses.isNotEmpty()) {
            totalReminders.toDouble() / activeProcesses.size
        } else {
            0.0
        }
        
        val expiredProcesses = activeProcesses.filter { 
            Date().after(it.expiryDate) 
        }.size
        
        return PPJBReminderStatistics(
            totalProcesses = activeProcesses.size,
            totalReminders = totalReminders,
            avgRemindersPerProcess = avgReminders,
            expiredProcesses = expiredProcesses,
            scheduledReminders = reminderJobs.size
        )
    }
    
    // Private helper methods
    private fun calculateReminderDate(baseDate: Date, daysOffset: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = baseDate
        calendar.add(Calendar.DAY_OF_MONTH, daysOffset)
        return calendar.time
    }
    
    private suspend fun updateReminderCount(processId: String, count: Int) {
        // Update reminder count in database
        // Implementation depends on your repository
    }
    
    private suspend fun autoCancelPPJB(process: PPJBDeveloperProcess) {
        // Update PPJB status
        ppjbRepository.updatePPJBStatus(process.id, PPJBStatus.CANCELLED)
        
        // Update unit status to available
        updateUnitStatus(process.unitPropertyId, UnitStatus.AVAILABLE)
        
        // Update dossier status
        updateDossierStatus(process.dossierId, DossierStatus.CANCELLED)
        
        // Send cancellation notification
        notificationService.sendPPJBCancellationNotification(
            process = process,
            reason = "AUTO_CANCELLED_EXPIRED",
            notes = "PPJB expired on ${process.expiryDate}"
        )
        
        // Log auto-cancellation
        logPPJBCancellation(process, "AUTO_CANCELLED_EXPIRED")
        
        // Cancel reminder jobs
        cancelReminderJobs(process.id)
    }
    
    private suspend fun updateUnitStatus(unitId: String, status: UnitStatus) {
        // Update unit property status
        // Implementation depends on your unit repository
    }
    
    private suspend fun updateDossierStatus(dossierId: String, status: DossierStatus) {
        // Update dossier status
        // Implementation depends on your dossier repository
    }
    
    private fun shouldSendReminder(process: PPJBDeveloperProcess): Pair<Boolean, Int> {
        if (process.reminderCount >= process.maxReminders) {
            return Pair(false, 0)
        }
        
        val now = Date()
        val daysSinceScheduled = (now.time - process.scheduledDate.time) / (24 * 60 * 60 * 1000)
        
        return when {
            daysSinceScheduled >= 7 && process.reminderCount == 0 -> Pair(true, 1)
            daysSinceScheduled >= 14 && process.reminderCount == 1 -> Pair(true, 2)
            else -> Pair(false, 0)
        }
    }
    
    private fun shouldSendExpiryWarning(process: PPJBDeveloperProcess): Boolean {
        val now = Date()
        val daysUntilExpiry = (process.expiryDate.time - now.time) / (24 * 60 * 60 * 1000)
        
        return daysUntilExpiry <= 3 && daysUntilExpiry > 0
    }
    
    private fun shouldAutoCancel(process: PPJBDeveloperProcess): Boolean {
        val now = Date()
        return now.after(process.expiryDate)
    }
    
    // Logging methods
    private suspend fun logReminderSent(processId: String, reminderNumber: Int) {
        val logData = mapOf(
            "process_id" to processId,
            "action" to "REMINDER_SENT",
            "reminder_number" to reminderNumber,
            "timestamp" to System.currentTimeMillis()
        )
        
        // Log to audit system
        // Implementation depends on your audit repository
    }
    
    private suspend fun logReminderError(processId: String, reminderNumber: Int, error: String) {
        val logData = mapOf(
            "process_id" to processId,
            "action" to "REMINDER_ERROR",
            "reminder_number" to reminderNumber,
            "error" to error,
            "timestamp" to System.currentTimeMillis()
        )
        
        // Log to audit system
        // Implementation depends on your audit repository
    }
    
    private suspend fun logExpiryWarningSent(processId: String) {
        val logData = mapOf(
            "process_id" to processId,
            "action" to "EXPIRY_WARNING_SENT",
            "timestamp" to System.currentTimeMillis()
        )
        
        // Log to audit system
        // Implementation depends on your audit repository
    }
    
    private suspend fun logExpiryWarningError(processId: String, error: String) {
        val logData = mapOf(
            "process_id" to processId,
            "action" to "EXPIRY_WARNING_ERROR",
            "error" to error,
            "timestamp" to System.currentTimeMillis()
        )
        
        // Log to audit system
        // Implementation depends on your audit repository
    }
    
    private suspend fun logExpiryCheckError(processId: String, error: String) {
        val logData = mapOf(
            "process_id" to processId,
            "action" to "EXPIRY_CHECK_ERROR",
            "error" to error,
            "timestamp" to System.currentTimeMillis()
        )
        
        // Log to audit system
        // Implementation depends on your audit repository
    }
    
    private suspend fun logPPJBCancellation(process: PPJBDeveloperProcess, reason: String) {
        val logData = mapOf(
            "process_id" to process.id,
            "dossier_id" to process.dossierId,
            "customer_id" to process.customerId,
            "unit_id" to process.unitPropertyId,
            "action" to "AUTO_CANCELLED",
            "reason" to reason,
            "cancelled_at" to System.currentTimeMillis(),
            "reminder_count" to process.reminderCount
        )
        
        // Log to audit system
        // Implementation depends on your audit repository
    }
    
    /**
     * Cleanup scheduler
     */
    fun cleanup() {
        schedulerScope.cancel()
        reminderJobs.clear()
    }
}

/**
 * PPJB Reminder Statistics
 */
data class PPJBReminderStatistics(
    val totalProcesses: Int,
    val totalReminders: Int,
    val avgRemindersPerProcess: Double,
    val expiredProcesses: Int,
    val scheduledReminders: Int
)
