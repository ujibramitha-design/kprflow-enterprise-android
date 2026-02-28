package com.kprflow.enterprise.legal

import android.content.Context
import com.kprflow.enterprise.data.model.*
import com.kprflow.enterprise.domain.repository.*
import com.kprflow.enterprise.whatsapp.AdvancedWhatsAppManager
import com.kprflow.enterprise.utils.DocumentGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * PPJB Developer Manager - Akad Pengikatan Bawah Tangan
 * Phase 16: Legal & Documentation Automation
 */
class PPJBDeveloperManager(
    private val context: Context,
    private val kprRepository: KprRepository,
    private val documentRepository: DocumentRepository,
    private val notificationRepository: NotificationRepository,
    private val whatsAppManager: AdvancedWhatsAppManager,
    private val documentGenerator: DocumentGenerator
) {
    
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    private val dateTimeFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
    
    companion object {
        private const val PPJB_SLA_DAYS = 30
        private const val REMINDER_INTERVAL_DAYS = 7
        private const val MAX_REMINDERS = 2
        private const val PPJB_DOCUMENT_TYPE = "PPJB_DEVELOPER"
    }
    
    /**
     * Initiate PPJB Developer process
     */
    suspend fun initiatePPJBDeveloper(
        dossierId: String,
        scheduledDate: Date? = null
    ): Result<PPJBDeveloperProcess> = withContext(Dispatchers.IO) {
        
        try {
            // Get dossier details
            val dossierResult = kprRepository.getDossierById(dossierId)
            if (dossierResult.isFailure) {
                return Result.failure(dossierResult.exceptionOrNull()!!)
            }
            
            val dossier = dossierResult.getOrNull()
                ?: return Result.failure(Exception("Dossier not found"))
            
            // Validate eligibility for PPJB
            val validationResult = validatePPJBEligibility(dossier)
            if (validationResult.isFailure) {
                return Result.failure(validationResult.exceptionOrNull()!!)
            }
            
            // Create PPJB process
            val ppjbProcess = createPPJBProcess(dossier, scheduledDate)
            
            // Generate PPJB document
            val documentResult = generatePPJBDocument(ppjbProcess)
            if (documentResult.isFailure) {
                return Result.failure(documentResult.exceptionOrNull()!!)
            }
            
            // Save process to database
            savePPJBProcess(ppjbProcess)
            
            // Send initial notification
            sendPPJBNotification(ppjbProcess, PPJBNotificationType.INITIAL)
            
            // Schedule reminders
            schedulePPJBReminders(ppjbProcess)
            
            Result.success(ppjbProcess)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validate PPJB eligibility
     */
    private suspend fun validatePPJBEligibility(dossier: KprDossier): Result<Unit> {
        return when {
            dossier.currentStatus != DossierStatus.APPROVAL_BANK -> {
                Result.failure(Exception("Dossier must be in APPROVAL_BANK status"))
            }
            dossier.loanAmount <= 0 -> {
                Result.failure(Exception("Invalid loan amount"))
            }
            dossier.customerId.isBlank() -> {
                Result.failure(Exception("Customer ID is required"))
            }
            dossier.unitPropertyId.isBlank() -> {
                Result.failure(Exception("Unit Property ID is required"))
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Create PPJB process
     */
    private suspend fun createPPJBProcess(
        dossier: KprDossier,
        scheduledDate: Date?
    ): PPJBDeveloperProcess {
        val now = Date()
        val ppjbDate = scheduledDate ?: calculateDefaultPPJBDate(now)
        val expiryDate = calculateExpiryDate(ppjbDate)
        
        return PPJBDeveloperProcess(
            id = UUID.randomUUID().toString(),
            dossierId = dossier.id,
            customerId = dossier.customerId,
            unitPropertyId = dossier.unitPropertyId,
            ppjbType = determinePPJBType(dossier),
            status = PPJBStatus.SCHEDULED,
            scheduledDate = ppjbDate,
            expiryDate = expiryDate,
            reminderCount = 0,
            maxReminders = MAX_REMINDERS,
            slaDays = PPJB_SLA_DAYS,
            createdAt = now,
            updatedAt = now
        )
    }
    
    /**
     * Determine PPJB type based on dossier
     */
    private fun determinePPJBType(dossier: KprDossier): PPJBType {
        return when {
            dossier.loanAmount > 0 -> PPJBType.KPR
            else -> PPJBType.CASH_KERAS
        }
    }
    
    /**
     * Calculate default PPJB date (7 days from now)
     */
    private fun calculateDefaultPPJBDate(fromDate: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = fromDate
        calendar.add(Calendar.DAY_OF_MONTH, REMINDER_INTERVAL_DAYS)
        return calendar.time
    }
    
    /**
     * Calculate expiry date (30 days from PPJB date)
     */
    private fun calculateExpiryDate(ppjbDate: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = ppjbDate
        calendar.add(Calendar.DAY_OF_MONTH, PPJB_SLA_DAYS)
        return calendar.time
    }
    
    /**
     * Generate PPJB document
     */
    private suspend fun generatePPJBDocument(
        process: PPJBDeveloperProcess
    ): Result<Document> = withContext(Dispatchers.IO) {
        
        try {
            // Get dossier and customer data
            val dossierResult = kprRepository.getDossierById(process.dossierId)
            val dossier = dossierResult.getOrNull()
                ?: return Result.failure(Exception("Dossier not found"))
            
            // Generate PPJB document based on type
            val documentContent = when (process.ppjbType) {
                PPJBType.KPR -> generateKPRPPJBContent(dossier, process)
                PPJBType.CASH_KERAS -> generateCashKerasPPJBContent(dossier, process)
            }
            
            // Create document
            val document = Document(
                id = UUID.randomUUID().toString(),
                dossierId = process.dossierId,
                customerId = process.customerId,
                documentType = DocumentType.PPJB_DEVELOPER,
                documentName = "PPJB_${process.ppjbType.name}_${dossier.applicationNumber}",
                fileName = "PPJB_${process.ppjbType.name}_${dossier.applicationNumber}.pdf",
                fileUrl = documentContent.fileUrl,
                fileSize = documentContent.fileSize,
                mimeType = "application/pdf",
                status = DocumentStatus.GENERATED,
                verificationStatus = VerificationStatus.PENDING,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Save document
            documentRepository.createDocument(
                dossierId = process.dossierId,
                customerId = process.customerId,
                type = "PPJB_DEVELOPER",
                fileName = document.fileName,
                fileData = documentContent.fileData
            )
            
            Result.success(document)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate KPR PPJB content
     */
    private suspend fun generateKPRPPJBContent(
        dossier: KprDossier,
        process: PPJBDeveloperProcess
    ): PPJBDocumentContent {
        
        val content = buildString {
            appendLine("PERJANJIAN PENGIKATAN JUAL BELI (PPJB)")
            appendLine("TIPE KPR")
            appendLine("=")
            appendLine()
            appendLine("Nomor: ${dossier.applicationNumber}")
            appendLine("Tanggal: ${dateFormat.format(Date())}")
            appendLine()
            appendLine("PIHAK PERTAMA (DEVELOPER):")
            appendLine("PT. KPRFlow Enterprise")
            appendLine("Alamat: Jl. Developer No. 123, Jakarta")
            appendLine()
            appendLine("PIHAK KEDUA (PEMBELI):")
            appendLine("Nama: [Customer Name]")
            appendLine("NIK: [Customer NIK]")
            appendLine("Alamat: [Customer Address]")
            appendLine()
            appendLine("DATA UNIT:")
            appendLine("Project: ${dossier.projectName}")
            appendLine("Block: ${dossier.unitBlock}")
            appendLine("Unit: ${dossier.unitNumber}")
            appendLine("Tipe: ${dossier.unitType}")
            appendLine()
            appendLine("DATA KPR:")
            appendLine("Jumlah Pinjaman: Rp ${String.format("%,.0f", dossier.loanAmount)}")
            appendLine("Tenor: ${dossier.loanTermMonths} bulan")
            appendLine("Bunga: ${dossier.interestRate}%")
            appendLine()
            appendLine("JADWAL PPJB:")
            appendLine("Tanggal PPJB: ${dateFormat.format(process.scheduledDate)}")
            appendLine("Batas Akad: ${dateFormat.format(process.expiryDate)}")
            appendLine()
            appendLine("KETENTUAN:")
            appendLine("1. PPJB berlaku selama 30 hari")
            appendLine("2. Pembeli wajib menyelesaikan akad KPR dalam jangka waktu tersebut")
            appendLine("3. Jika tidak terlaksana, PPJB batal dengan sendirinya")
            appendLine("4. Unit akan dikembalikan ke status tersedia")
            appendLine()
            appendLine("Tanda Tangan:")
            appendLine()
            appendLine("Pihak Pertama")
            appendLine("(Developer)")
            appendLine()
            appendLine()
            appendLine("Pihak Kedua")
            appendLine("(Pembeli)")
        }
        
        return PPJBDocumentContent(
            fileData = content.toByteArray(),
            fileSize = content.length.toLong(),
            fileUrl = "generated://ppjb_${process.id}.pdf"
        )
    }
    
    /**
     * Generate Cash Keras PPJB content
     */
    private suspend fun generateCashKerasPPJBContent(
        dossier: KprDossier,
        process: PPJBDeveloperProcess
    ): PPJBDocumentContent {
        
        val content = buildString {
            appendLine("PERJANJIAN PENGIKATAN JUAL BELI (PPJB)")
            appendLine("TIPE CASH KERAS")
            appendLine("=")
            appendLine()
            appendLine("Nomor: ${dossier.applicationNumber}")
            appendLine("Tanggal: ${dateFormat.format(Date())}")
            appendLine()
            appendLine("PIHAK PERTAMA (DEVELOPER):")
            appendLine("PT. KPRFlow Enterprise")
            appendLine("Alamat: Jl. Developer No. 123, Jakarta")
            appendLine()
            appendLine("PIHAK KEDUA (PEMBELI):")
            appendLine("Nama: [Customer Name]")
            appendLine("NIK: [Customer NIK]")
            appendLine("Alamat: [Customer Address]")
            appendLine()
            appendLine("DATA UNIT:")
            appendLine("Project: ${dossier.projectName}")
            appendLine("Block: ${dossier.unitBlock}")
            appendLine("Unit: ${dossier.unitNumber}")
            appendLine("Tipe: ${dossier.unitType}")
            appendLine()
            appendLine("DATA PEMBAYARAN:")
            appendLine("Tipe: Cash Keras")
            appendLine("Harga Unit: Rp ${String.format("%,.0f", dossier.estimatedLoanAmount)}")
            appendLine()
            appendLine("JADWAL PPJB:")
            appendLine("Tanggal PPJB: ${dateFormat.format(process.scheduledDate)}")
            appendLine("Batas Akad: ${dateFormat.format(process.expiryDate)}")
            appendLine()
            appendLine("KETENTUAN:")
            appendLine("1. PPJB berlaku selama 30 hari")
            appendLine("2. Pembeli wajib menyelesaikan pembayaran cash dalam jangka waktu tersebut")
            appendLine("3. Jika tidak terlaksana, PPJB batal dengan sendirinya")
            appendLine("4. Unit akan dikembalikan ke status tersedia")
            appendLine()
            appendLine("Tanda Tangan:")
            appendLine()
            appendLine("Pihak Pertama")
            appendLine("(Developer)")
            appendLine()
            appendLine()
            appendLine("Pihak Kedua")
            appendLine("(Pembeli)")
        }
        
        return PPJBDocumentContent(
            fileData = content.toByteArray(),
            fileSize = content.length.toLong(),
            fileUrl = "generated://ppjb_${process.id}.pdf"
        )
    }
    
    /**
     * Save PPJB process
     */
    private suspend fun savePPJBProcess(process: PPJBDeveloperProcess) {
        // Save to database (implementation depends on your database structure)
        // This would typically save to a ppjb_processes table
    }
    
    /**
     * Send PPJB notification
     */
    private suspend fun sendPPJBNotification(
        process: PPJBDeveloperProcess,
        notificationType: PPJBNotificationType
    ) {
        val (title, message) = when (notificationType) {
            PPJBNotificationType.INITIAL -> {
                "PPJB Developer Dijadwalkan" to """
                    📋 **PPJB Developer Telah Dijadwalkan**
                    
                    🏠 **Unit:** ${process.unitPropertyId}
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
                    🏠 **Unit:** ${process.unitPropertyId}
                    
                    🔄 **Unit telah dikembalikan ke status tersedia**
                    
                    Untuk informasi lebih lanjut, hubungi developer.
                """.trimIndent()
            }
        }
        
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
        whatsAppManager.sendAdvancedMessage(
            recipient = getCustomerPhoneNumber(process.customerId),
            templateType = WhatsAppTemplateType.SYSTEM_NOTIFICATION,
            variables = mapOf(
                "title" to title,
                "message" to message,
                "ppjb_date" to dateFormat.format(process.scheduledDate),
                "expiry_date" to dateFormat.format(process.expiryDate)
            )
        )
    }
    
    /**
     * Schedule PPJB reminders
     */
    private suspend fun schedulePPJBReminders(process: PPJBDeveloperProcess) {
        // Schedule reminders at 7-day intervals
        val reminderDates = listOf(
            calculateReminderDate(process.scheduledDate, -7), // 7 days before PPJB
            calculateReminderDate(process.expiryDate, -7)    // 7 days before expiry
        )
        
        reminderDates.forEach { reminderDate ->
            if (reminderDate.after(Date())) {
                scheduleReminder(process, reminderDate)
            }
        }
    }
    
    /**
     * Calculate reminder date
     */
    private fun calculateReminderDate(baseDate: Date, daysOffset: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = baseDate
        calendar.add(Calendar.DAY_OF_MONTH, daysOffset)
        return calendar.time
    }
    
    /**
     * Schedule individual reminder
     */
    private suspend fun scheduleReminder(process: PPJBDeveloperProcess, reminderDate: Date) {
        // This would typically use a job scheduler like WorkManager
        // For now, we'll simulate the scheduling
    }
    
    /**
     * Process PPJB reminders
     */
    suspend fun processPPJBReminders(): Result<Int> = withContext(Dispatchers.IO) {
        
        try {
            var processedCount = 0
            
            // Get all active PPJB processes
            val activeProcesses = getActivePPJBProcesses()
            
            activeProcesses.forEach { process ->
                val shouldRemind = shouldSendReminder(process)
                
                if (shouldRemind) {
                    sendPPJBNotification(process, PPJBNotificationType.REMINDER)
                    updateReminderCount(process.id, process.reminderCount + 1)
                    processedCount++
                }
                
                // Check for expiry warning
                if (shouldSendExpiryWarning(process)) {
                    sendPPJBNotification(process, PPJBNotificationType.EXPIRY_WARNING)
                }
                
                // Check for auto-cancellation
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
     * Check if reminder should be sent
     */
    private fun shouldSendReminder(process: PPJBDeveloperProcess): Boolean {
        if (process.reminderCount >= process.maxReminders) {
            return false
        }
        
        val now = Date()
        val daysSinceScheduled = abs(now.time - process.scheduledDate.time) / (24 * 60 * 60 * 1000)
        
        return daysSinceScheduled >= REMINDER_INTERVAL_DAYS && (process.reminderCount == 0)
    }
    
    /**
     * Check if expiry warning should be sent
     */
    private fun shouldSendExpiryWarning(process: PPJBDeveloperProcess): Boolean {
        val now = Date()
        val daysUntilExpiry = (process.expiryDate.time - now.time) / (24 * 60 * 60 * 1000)
        
        return daysUntilExpiry <= 7 && daysUntilExpiry > 0
    }
    
    /**
     * Check if PPJB should be auto-cancelled
     */
    private fun shouldAutoCancel(process: PPJBDeveloperProcess): Boolean {
        val now = Date()
        return now.after(process.expiryDate)
    }
    
    /**
     * Auto-cancel PPJB
     */
    private suspend fun autoCancelPPJB(process: PPJBDeveloperProcess) {
        // Update PPJB status
        updatePPJBStatus(process.id, PPJBStatus.CANCELLED)
        
        // Update unit status to FREE
        updateUnitStatus(process.unitPropertyId, UnitStatus.AVAILABLE)
        
        // Update dossier status
        updateDossierStatus(process.dossierId, DossierStatus.CANCELLED)
        
        // Send cancellation notification
        sendPPJBNotification(process, PPJBNotificationType.CANCELLED)
        
        // Log cancellation
        logPPJBCancellation(process)
    }
    
    /**
     * Update PPJB status
     */
    private suspend fun updatePPJBStatus(ppjbId: String, status: PPJBStatus) {
        // Update in database
    }
    
    /**
     * Update unit status
     */
    private suspend fun updateUnitStatus(unitId: String, status: UnitStatus) {
        // Update unit property in database
    }
    
    /**
     * Update dossier status
     */
    private suspend fun updateDossierStatus(dossierId: String, status: DossierStatus) {
        // Update dossier in database
    }
    
    /**
     * Update reminder count
     */
    private suspend fun updateReminderCount(ppjbId: String, count: Int) {
        // Update in database
    }
    
    /**
     * Get customer phone number
     */
    private suspend fun getCustomerPhoneNumber(customerId: String): String {
        // Get from customer profile
        return "+628123456789" // Dummy implementation
    }
    
    /**
     * Get active PPJB processes
     */
    private suspend fun getActivePPJBProcesses(): List<PPJBDeveloperProcess> {
        // Get from database
        return emptyList() // Dummy implementation
    }
    
    /**
     * Calculate days remaining
     */
    private fun calculateDaysRemaining(expiryDate: Date): Int {
        val now = Date()
        return ((expiryDate.time - now.time) / (24 * 60 * 60 * 1000)).toInt()
    }
    
    /**
     * Log PPJB cancellation
     */
    private suspend fun logPPJBCancellation(process: PPJBDeveloperProcess) {
        val logData = mapOf(
            "ppjb_id" to process.id,
            "dossier_id" to process.dossierId,
            "customer_id" to process.customerId,
            "unit_id" to process.unitPropertyId,
            "cancelled_at" to System.currentTimeMillis(),
            "reason" to "AUTO_CANCELLED_EXPIRED",
            "reminder_count" to process.reminderCount
        )
        
        // Log to audit system
    }
    
    /**
     * Generate invitation document
     */
    suspend fun generateInvitation(
        ppjbId: String,
        invitationDate: Date
    ): Result<Document> = withContext(Dispatchers.IO) {
        
        try {
            // Get PPJB process
            val process = getPPJBProcess(ppjbId)
                ?: return Result.failure(Exception("PPJB process not found"))
            
            // Get dossier details
            val dossierResult = kprRepository.getDossierById(process.dossierId)
            val dossier = dossierResult.getOrNull()
                ?: return Result.failure(Exception("Dossier not found"))
            
            // Generate invitation content
            val invitationContent = generateInvitationContent(dossier, process, invitationDate)
            
            // Create document
            val document = Document(
                id = UUID.randomUUID().toString(),
                dossierId = process.dossierId,
                customerId = process.customerId,
                documentType = DocumentType.UNDANGAN_PPJB,
                documentName = "Undangan_PPJB_${dossier.applicationNumber}",
                fileName = "Undangan_PPJB_${dossier.applicationNumber}.pdf",
                fileUrl = invitationContent.fileUrl,
                fileSize = invitationContent.fileSize,
                mimeType = "application/pdf",
                status = DocumentStatus.GENERATED,
                verificationStatus = VerificationStatus.PENDING,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Save document
            documentRepository.createDocument(
                dossierId = process.dossierId,
                customerId = process.customerId,
                type = "UNDANGAN_PPJB",
                fileName = document.fileName,
                fileData = invitationContent.fileData
            )
            
            Result.success(document)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate invitation content
     */
    private suspend fun generateInvitationContent(
        dossier: KprDossier,
        process: PPJBDeveloperProcess,
        invitationDate: Date
    ): PPJBDocumentContent {
        
        val content = buildString {
            appendLine("UNDANGAN PPJB DEVELOPER")
            appendLine("=")
            appendLine()
            appendLine("Kepada Yth:")
            appendLine("[Customer Name]")
            appendLine("[Customer Address]")
            appendLine()
            appendLine("Dengan hormat,")
            appendLine()
            appendLine("Bersama ini kami mengundang Anda untuk menghadiri:")
            appendLine()
            appendLine("**ACARA PENANDATANGANAN PPJB DEVELOPER**")
            appendLine()
            appendLine("📅 **Tanggal:** ${dateFormat.format(invitationDate)}")
            appendLine("⏰ **Waktu:** 10:00 WIB")
            appendLine("📍 **Tempat:** Kantor Marketing PT. KPRFlow Enterprise")
            appendLine("📍 **Alamat:** Jl. Developer No. 123, Jakarta")
            appendLine()
            appendLine("DATA TRANSAKSI:")
            appendLine("📋 **No. Aplikasi:** ${dossier.applicationNumber}")
            appendLine("🏠 **Unit:** ${dossier.unitBlock}-${dossier.unitNumber}")
            appendLine("📄 **Tipe PPJB:** ${process.ppjbType.name}")
            appendLine()
            appendLine("DOKUMEN YANG DIBAWA:")
            appendLine("1. KTP Asli (Pembeli & Pasangan)")
            appendLine("2. KK Asli")
            appendLine("3. NPWP Asli")
            appendLine("4. Slip Gaji (3 bulan terakhir)")
            appendLine("5. Bukti Pembayaran Booking Fee")
            appendLine()
            appendLine("CATATAN:")
            appendLine("- Mohon hadir 15 menit sebelum acara dimulai")
            appendLine("- Bawa dokumen asli dan fotokopi")
            appendLine("- Jika tidak dapat hadir, mohon konfirmasi 2 hari sebelumnya")
            appendLine()
            appendLine("Konfirmasi Kehadiran:")
            appendLine("📱 WhatsApp: 0812-3456-7890")
            appendLine("📧 Email: marketing@kprflow.com")
            appendLine()
            appendLine("Demikian undangan ini kami sampaikan.")
            appendLine("Atas perhatian dan kehadirannya, kami ucapkan terima kasih.")
            appendLine()
            appendLine("Hormat kami,")
            appendLine()
            appendLine("PT. KPRFlow Enterprise")
            appendLine("Marketing Division")
            appendLine()
            appendLine("${dateFormat.format(Date())}")
        }
        
        return PPJBDocumentContent(
            fileData = content.toByteArray(),
            fileSize = content.length.toLong(),
            fileUrl = "generated://invitation_${process.id}.pdf"
        )
    }
    
    /**
     * Get PPJB process
     */
    private suspend fun getPPJBProcess(ppjbId: String): PPJBDeveloperProcess? {
        // Get from database
        return null // Dummy implementation
    }
}

// Data classes
data class PPJBDeveloperProcess(
    val id: String,
    val dossierId: String,
    val customerId: String,
    val unitPropertyId: String,
    val ppjbType: PPJBType,
    val status: PPJBStatus,
    val scheduledDate: Date,
    val expiryDate: Date,
    val reminderCount: Int,
    val maxReminders: Int,
    val slaDays: Int,
    val createdAt: Date,
    val updatedAt: Date
)

data class PPJBDocumentContent(
    val fileData: ByteArray,
    val fileSize: Long,
    val fileUrl: String
)

// Enums
enum class PPJBType {
    KPR, CASH_KERAS
}

enum class PPJBStatus {
    SCHEDULED, REMINDED, WARNING_SENT, CANCELLED, COMPLETED
}

enum class PPJBNotificationType {
    INITIAL, REMINDER, EXPIRY_WARNING, CANCELLED
}
