package com.kprflow.enterprise.legal.notification

import com.kprflow.enterprise.domain.model.*
import com.kprflow.enterprise.legal.model.*
import com.kprflow.enterprise.whatsapp.AdvancedWhatsAppManager
import com.kprflow.enterprise.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * PPJB Notification Service - Centralized Notification Management
 * Phase 16: Legal & Documentation Automation
 */
class PPJBNotificationService(
    private val notificationRepository: NotificationRepository,
    private val whatsAppManager: AdvancedWhatsAppManager
) {
    
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    private val dateTimeFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
    
    /**
     * Send PPJB notification
     */
    suspend fun sendPPJBNotification(
        process: PPJBDeveloperProcess,
        notificationType: PPJBNotificationType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        
        try {
            val (title, message) = generateNotificationContent(process, notificationType)
            
            // Send in-app notification
            notificationRepository.sendNotification(
                userId = process.customerId,
                title = title,
                message = message,
                type = "PPJB",
                data = mapOf(
                    "ppjb_id" to process.id,
                    "dossier_id" to process.dossierId,
                    "notification_type" to notificationType.name
                )
            )
            
            // Send WhatsApp notification
            val customerPhone = getCustomerPhoneNumber(process.customerId)
            whatsAppManager.sendAdvancedMessage(
                recipient = customerPhone,
                templateType = WhatsAppTemplateType.SYSTEM_NOTIFICATION,
                variables = mapOf(
                    "title" to title,
                    "message" to message,
                    "ppjb_date" to dateFormat.format(process.scheduledDate),
                    "expiry_date" to dateFormat.format(process.expiryDate),
                    "unit_block" to process.unitBlock,
                    "unit_number" to process.unitNumber,
                    "ppjb_type" to process.ppjbType.name
                )
            )
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send PPJB reminder
     */
    suspend fun sendPPJBReminder(
        process: PPJBDeveloperProcess,
        reminderNumber: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        
        try {
            val (title, message) = generateReminderContent(process, reminderNumber)
            
            // Send in-app notification
            notificationRepository.sendNotification(
                userId = process.customerId,
                title = title,
                message = message,
                type = "PPJB_REMINDER",
                data = mapOf(
                    "ppjb_id" to process.id,
                    "dossier_id" to process.dossierId,
                    "reminder_number" to reminderNumber,
                    "max_reminders" to process.maxReminders
                )
            )
            
            // Send WhatsApp notification
            val customerPhone = getCustomerPhoneNumber(process.customerId)
            whatsAppManager.sendAdvancedMessage(
                recipient = customerPhone,
                templateType = WhatsAppTemplateType.PAYMENT_REMINDER,
                variables = mapOf(
                    "title" to title,
                    "message" to message,
                    "ppjb_date" to dateFormat.format(process.scheduledDate),
                    "days_remaining" to calculateDaysRemaining(process.expiryDate).toString(),
                    "reminder_number" to reminderNumber.toString(),
                    "max_reminders" to process.maxReminders.toString()
                )
            )
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send PPJB invitation notification
     */
    suspend fun sendPPJBInvitationNotification(
        process: PPJBDeveloperProcess,
        invitationDate: Date
    ): Result<Unit> = withContext(Dispatchers.IO) {
        
        try {
            val (title, message) = generateInvitationNotificationContent(process, invitationDate)
            
            // Send in-app notification
            notificationRepository.sendNotification(
                userId = process.customerId,
                title = title,
                message = message,
                type = "PPJB_INVITATION",
                data = mapOf(
                    "ppjb_id" to process.id,
                    "dossier_id" to process.dossierId,
                    "invitation_date" to dateFormat.format(invitationDate)
                )
            )
            
            // Send WhatsApp notification
            val customerPhone = getCustomerPhoneNumber(process.customerId)
            whatsAppManager.sendAdvancedMessage(
                recipient = customerPhone,
                templateType = WhatsAppTemplateType.APPOINTMENT_REMINDER,
                variables = mapOf(
                    "title" to title,
                    "message" to message,
                    "invitation_date" to dateFormat.format(invitationDate),
                    "invitation_time" to "10:00 WIB",
                    "venue" to "Kantor Marketing PT. KPRFlow Enterprise",
                    "unit_block" to process.unitBlock,
                    "unit_number" to process.unitNumber
                )
            )
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send PPJB expiry warning
     */
    suspend fun sendPPJBExpiryWarning(
        process: PPJBDeveloperProcess
    ): Result<Unit> = withContext(Dispatchers.IO) {
        
        try {
            val (title, message) = generateExpiryWarningContent(process)
            
            // Send in-app notification
            notificationRepository.sendNotification(
                userId = process.customerId,
                title = title,
                message = message,
                type = "PPJB_EXPIRY_WARNING",
                priority = NotificationPriority.HIGH,
                data = mapOf(
                    "ppjb_id" to process.id,
                    "dossier_id" to process.dossierId,
                    "expiry_date" to dateFormat.format(process.expiryDate),
                    "days_remaining" to calculateDaysRemaining(process.expiryDate).toString()
                )
            )
            
            // Send WhatsApp notification
            val customerPhone = getCustomerPhoneNumber(process.customerId)
            whatsAppManager.sendAdvancedMessage(
                recipient = customerPhone,
                templateType = WhatsAppTemplateType.SYSTEM_NOTIFICATION,
                variables = mapOf(
                    "title" to title,
                    "message" to message,
                    "expiry_date" to dateFormat.format(process.expiryDate),
                    "days_remaining" to calculateDaysRemaining(process.expiryDate).toString(),
                    "unit_block" to process.unitBlock,
                    "unit_number" to process.unitNumber
                )
            )
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send PPJB completion notification
     */
    suspend fun sendPPJBCompletionNotification(
        process: PPJBDeveloperProcess
    ): Result<Unit> = withContext(Dispatchers.IO) {
        
        try {
            val (title, message) = generateCompletionContent(process)
            
            // Send in-app notification
            notificationRepository.sendNotification(
                userId = process.customerId,
                title = title,
                message = message,
                type = "PPJB_COMPLETED",
                data = mapOf(
                    "ppjb_id" to process.id,
                    "dossier_id" to process.dossierId,
                    "completion_date" to dateFormat.format(Date())
                )
            )
            
            // Send WhatsApp notification
            val customerPhone = getCustomerPhoneNumber(process.customerId)
            whatsAppManager.sendAdvancedMessage(
                recipient = customerPhone,
                templateType = WhatsAppTemplateType.STATUS_CHANGE,
                variables = mapOf(
                    "title" to title,
                    "message" to message,
                    "completion_date" to dateFormat.format(Date()),
                    "unit_block" to process.unitBlock,
                    "unit_number" to process.unitNumber
                )
            )
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send PPJB cancellation notification
     */
    suspend fun sendPPJBCancellationNotification(
        process: PPJBDeveloperProcess,
        reason: String,
        notes: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        
        try {
            val (title, message) = generateCancellationContent(process, reason, notes)
            
            // Send in-app notification
            notificationRepository.sendNotification(
                userId = process.customerId,
                title = title,
                message = message,
                type = "PPJB_CANCELLED",
                priority = NotificationPriority.HIGH,
                data = mapOf(
                    "ppjb_id" to process.id,
                    "dossier_id" to process.dossierId,
                    "cancellation_reason" to reason,
                    "cancellation_notes" to notes,
                    "cancellation_date" to dateFormat.format(Date())
                )
            )
            
            // Send WhatsApp notification
            val customerPhone = getCustomerPhoneNumber(process.customerId)
            whatsAppManager.sendAdvancedMessage(
                recipient = customerPhone,
                templateType = WhatsAppTemplateType.UNIT_CANCELLED,
                variables = mapOf(
                    "title" to title,
                    "message" to message,
                    "cancellation_reason" to reason,
                    "cancellation_notes" to notes ?: "",
                    "unit_block" to process.unitBlock,
                    "unit_number" to process.unitNumber
                )
            )
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Private helper methods
    private fun generateNotificationContent(
        process: PPJBDeveloperProcess,
        notificationType: PPJBNotificationType
    ): Pair<String, String> {
        return when (notificationType) {
            PPJBNotificationType.INITIAL -> {
                "PPJB Developer Dijadwalkan" to """
                    📋 **PPJB Developer Telah Dijadwalkan**
                    
                    🏠 **Unit:** ${process.unitBlock}-${process.unitNumber}
                    📅 **Tanggal:** ${dateFormat.format(process.scheduledDate)}
                    ⏰ **Batas Akad:** ${dateFormat.format(process.expiryDate)}
                    
                    📄 **Dokumen PPJB telah disiapkan**
                    📱 **Undangan akan dikirim 7 hari sebelumnya**
                    
                    Harap persiapkan dokumen yang diperlukan.
                """.trimIndent()
            }
            PPJBNotificationType.REMINDER -> {
                "Reminder PPJB Developer" to """
                    ⏰ **Reminder PPJB Developer**
                    
                    📅 **Tanggal PPJB:** ${dateFormat.format(process.scheduledDate)}
                    ⏰ **Sisa waktu:** ${calculateDaysRemaining(process.expiryDate)} hari
                    
                    📋 **Status:** ${process.status.name}
                    
                    Segera selesaikan proses PPJB Anda.
                """.trimIndent()
            }
            PPJBNotificationType.EXPIRY_WARNING -> {
                "Peringatan Kadaluarsa PPJB" to """
                    ⚠️ **PERINGATAN KADALUARSA PPJB**
                    
                    ⏰ **PPJB akan kadaluarsa dalam:** ${calculateDaysRemaining(process.expiryDate)} hari
                    📅 **Tanggal Kadaluarsa:** ${dateFormat.format(process.expiryDate)}
                    
                    🚨 **Jika tidak diselesaikan, unit akan dibatalkan otomatis**
                    
                    Segera hubungi developer untuk menyelesaikan PPJB.
                """.trimIndent()
            }
            PPJBNotificationType.CANCELLED -> {
                "PPJB Developer Dibatalkan" to """
                    ❌ **PPJB Developer Dibatalkan**
                    
                    📅 **Tanggal:** ${dateFormat.format(Date())}
                    🏠 **Unit:** ${process.unitBlock}-${process.unitNumber}
                    
                    🔄 **Unit telah dikembalikan ke status tersedia**
                    
                    Untuk informasi lebih lanjut, hubungi developer.
                """.trimIndent()
            }
        }
    }
    
    private fun generateReminderContent(
        process: PPJBDeveloperProcess,
        reminderNumber: Int
    ): Pair<String, String> {
        return "Reminder PPJB Developer #$reminderNumber" to """
            ⏰ **Reminder PPJB Developer #$reminderNumber**
            
            📅 **Tanggal PPJB:** ${dateFormat.format(process.scheduledDate)}
            ⏰ **Sisa waktu:** ${calculateDaysRemaining(process.expiryDate)} hari
            📋 **Status:** ${process.status.name}
            🔄 **Reminder:** $reminderNumber dari ${process.maxReminders}
            
            Segera selesaikan proses PPJB Anda.
        """.trimIndent()
    }
    
    private fun generateInvitationNotificationContent(
        process: PPJBDeveloperProcess,
        invitationDate: Date
    ): Pair<String, String> {
        return "Undangan PPJB Developer" to """
            📋 **Undangan PPJB Developer**
            
            📅 **Tanggal:** ${dateFormat.format(invitationDate)}
            ⏰ **Waktu:** 10:00 WIB
            📍 **Tempat:** Kantor Marketing PT. KPRFlow Enterprise
            
            🏠 **Unit:** ${process.unitBlock}-${process.unitNumber}
            📄 **Tipe:** ${process.ppjbType.name}
            
            Mohon hadir 15 menit sebelum acara dimulai.
        """.trimIndent()
    }
    
    private fun generateExpiryWarningContent(
        process: PPJBDeveloperProcess
    ): Pair<String, String> {
        return "⚠️ Peringatan Kadaluarsa PPJB" to """
            ⚠️ **PERINGATAN KADALUARSA PPJB**
            
            ⏰ **PPJB akan kadaluarsa dalam:** ${calculateDaysRemaining(process.expiryDate)} hari
            📅 **Tanggal Kadaluarsa:** ${dateFormat.format(process.expiryDate)}
            
            🚨 **Jika tidak diselesaikan, unit akan dibatalkan otomatis**
            
            Segera hubungi developer untuk menyelesaikan PPJB.
        """.trimIndent()
    }
    
    private fun generateCompletionContent(
        process: PPJBDeveloperProcess
    ): Pair<String, String> {
        return "✅ PPJB Developer Selesai" to """
            ✅ **PPJB Developer Telah Selesai**
            
            📅 **Tanggal:** ${dateFormat.format(Date())}
            🏠 **Unit:** ${process.unitBlock}-${process.unitNumber}
            📄 **Tipe:** ${process.ppjbType.name}
            
            Terima kasih atas kehadiran dan kerjasamanya.
        """.trimIndent()
    }
    
    private fun generateCancellationContent(
        process: PPJBDeveloperProcess,
        reason: String,
        notes: String?
    ): Pair<String, String> {
        return "❌ PPJB Developer Dibatalkan" to """
            ❌ **PPJB Developer Dibatalkan**
            
            📅 **Tanggal:** ${dateFormat.format(Date())}
            🏠 **Unit:** ${process.unitBlock}-${process.unitNumber}
            📋 **Alasan:** $reason
            
            ${notes?.let { "📝 **Catatan:** $it" } ?: ""}
            
            🔄 **Unit telah dikembalikan ke status tersedia**
            
            Untuk informasi lebih lanjut, hubungi developer.
        """.trimIndent()
    }
    
    private fun calculateDaysRemaining(expiryDate: Date): Int {
        val now = Date()
        return ((expiryDate.time - now.time) / (24 * 60 * 60 * 1000)).toInt()
    }
    
    private suspend fun getCustomerPhoneNumber(customerId: String): String {
        // Implementation depends on your user repository
        return "+628123456789" // Dummy implementation
    }
}
