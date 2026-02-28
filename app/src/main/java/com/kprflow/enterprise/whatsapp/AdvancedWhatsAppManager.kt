package com.kprflow.enterprise.whatsapp

import android.content.Context
import com.kprflow.enterprise.data.model.*
import com.kprflow.enterprise.domain.repository.WhatsAppRepository
import com.kprflow.enterprise.i18n.LocalizationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Advanced WhatsApp Manager with template management and multi-language support
 */
class AdvancedWhatsAppManager(
    private val context: Context,
    private val whatsAppRepository: WhatsAppRepository,
    private val localizationManager: LocalizationManager
) {
    
    companion object {
        private const val TEMPLATE_VERSION = "v1.0"
        private const val MAX_TEMPLATE_VARIABLES = 10
    }
    
    /**
     * Send message with advanced template and multi-language support
     */
    suspend fun sendAdvancedMessage(
        recipient: String,
        templateType: WhatsAppTemplateType,
        variables: Map<String, Any> = emptyMap(),
        language: String = "id",
        gateway: WhatsAppGateway = WhatsAppGateway.FONNTE
    ): Result<WhatsAppMessageResult> = withContext(Dispatchers.IO) {
        
        try {
            // Get localized template
            val template = getLocalizedTemplate(templateType, language)
                ?: return Result.failure(Exception("Template not found for type: $templateType, language: $language"))
            
            // Validate variables
            val validationResult = validateTemplateVariables(template, variables)
            if (validationResult.isFailure) {
                return Result.failure(validationResult.exceptionOrNull()!!)
            }
            
            // Process template with variables
            val processedMessage = processTemplate(template, variables)
            
            // Add language-specific formatting
            val formattedMessage = addLanguageSpecificFormatting(processedMessage, language)
            
            // Send via selected gateway
            val result = when (gateway) {
                WhatsAppGateway.FONNTE -> sendViaFonnte(recipient, formattedMessage, template)
                WhatsAppGateway.WABLAS -> sendViaWablas(recipient, formattedMessage, template)
                WhatsAppGateway.WHATSAPP_BUSINESS -> sendViaWhatsAppBusiness(recipient, formattedMessage, template)
                WhatsAppGateway.DUMMY -> sendViaDummyGateway(recipient, formattedMessage, template)
            }
            
            // Log message sent
            logWhatsAppMessage(
                recipient = recipient,
                templateType = templateType,
                language = language,
                gateway = gateway,
                result = result
            )
            
            result
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get localized template
     */
    private suspend fun getLocalizedTemplate(
        templateType: WhatsAppTemplateType,
        language: String
    ): WhatsAppTemplate? {
        return when (language) {
            "id" -> getIndonesianTemplate(templateType)
            "en" -> getEnglishTemplate(templateType)
            else -> getIndonesianTemplate(templateType) // Default to Indonesian
        }
    }
    
    /**
     * Get Indonesian templates
     */
    private fun getIndonesianTemplate(templateType: WhatsAppTemplateType): WhatsAppTemplate? {
        return when (templateType) {
            WhatsAppTemplateType.LEAD_GENERATED -> WhatsAppTemplate(
                id = "lead_generated_id",
                type = templateType,
                language = "id",
                content = "🎉 *Selamat! Lead Baru Telah Dibuat*\n\n" +
                        "📋 *Detail Lead:*\n" +
                        "👤 Nama: {{customer_name}}\n" +
                        "📞 Telepon: {{customer_phone}}\n" +
                        "🏠 Unit: {{unit_block}}-{{unit_number}}\n" +
                        "💰 Harga: Rp {{unit_price}}\n\n" +
                        "📅 *Tanggal: {{date}}*\n" +
                        "🔄 *Status: {{status}}*\n\n" +
                        "Segera follow up lead ini ya! 🚀",
                variables = listOf("customer_name", "customer_phone", "unit_block", "unit_number", "unit_price", "date", "status"),
                category = WhatsAppTemplateCategory.LEAD_GENERATION
            )
            
            WhatsAppTemplateType.STATUS_CHANGE -> WhatsAppTemplate(
                id = "status_change_id",
                type = templateType,
                language = "id",
                content = "📊 *Update Status KPR*\n\n" +
                        "👤 *Pelanggan:* {{customer_name}}\n" +
                        "🏠 *Unit:* {{unit_block}}-{{unit_number}}\n\n" +
                        "🔄 *Perubahan Status:*\n" +
                        "Dari: {{previous_status}}\n" +
                        "Menjadi: {{new_status}}\n\n" +
                        "📅 *Tanggal:* {{date}}\n" +
                        "👤 *Diupdate oleh:* {{updated_by}}\n\n" +
                        "Terima kasih atas perhatiannya! 😊",
                variables = listOf("customer_name", "unit_block", "unit_number", "previous_status", "new_status", "date", "updated_by"),
                category = WhatsAppTemplateCategory.STATUS_UPDATE
            )
            
            WhatsAppTemplateType.DOCUMENT_UPLOADED -> WhatsAppTemplate(
                id = "document_uploaded_id",
                type = templateType,
                language = "id",
                content = "📄 *Dokumen Telah Diunggah*\n\n" +
                        "👤 *Pelanggan:* {{customer_name}}\n" +
                        "📋 *Jenis Dokumen:* {{document_type}}\n" +
                        "📁 *Nama File:* {{file_name}}\n\n" +
                        "✅ *Status:* {{verification_status}}\n" +
                        "📅 *Diunggah:* {{upload_date}}\n\n" +
                        "Dokumen sedang dalam proses verifikasi. 📝",
                variables = listOf("customer_name", "document_type", "file_name", "verification_status", "upload_date"),
                category = WhatsAppTemplateCategory.DOCUMENT_NOTIFICATION
            )
            
            WhatsAppTemplateType.PAYMENT_RECEIVED -> WhatsAppTemplate(
                id = "payment_received_id",
                type = templateType,
                language = "id",
                content = "💰 *Pembayaran Diterima*\n\n" +
                        "👤 *Pelanggan:* {{customer_name}}\n" +
                        "🏠 *Unit:* {{unit_block}}-{{unit_number}}\n\n" +
                        "💳 *Detail Pembayaran:*\n" +
                        "Jenis: {{payment_type}}\n" +
                        "Jumlah: Rp {{amount}}\n" +
                        "Tanggal: {{payment_date}}\n" +
                        "Status: {{payment_status}}\n\n" +
                        "Terima kasih atas pembayarannya! 🙏",
                variables = listOf("customer_name", "unit_block", "unit_number", "payment_type", "amount", "payment_date", "payment_status"),
                category = WhatsAppTemplateCategory.PAYMENT_NOTIFICATION
            )
            
            WhatsAppTemplateType.UNIT_CANCELLED -> WhatsAppTemplate(
                id = "unit_cancelled_id",
                type = templateType,
                language = "id",
                content = "❌ *Unit Dibatalkan*\n\n" +
                        "🏠 *Unit:* {{unit_block}}-{{unit_number}}\n" +
                        "👤 *Pelanggan:* {{customer_name}}\n\n" +
                        "📋 *Alasan Pembatalan:*\n" +
                        "{{cancellation_reason}}\n\n" +
                        "📅 *Tanggal:* {{cancellation_date}}\n" +
                        "👤 *Dibatalkan oleh:* {{cancelled_by}}\n\n" +
                        "Unit kembali tersedia untuk dijual. 🏪",
                variables = listOf("unit_block", "unit_number", "customer_name", "cancellation_reason", "cancellation_date", "cancelled_by"),
                category = WhatsAppTemplateCategory.CANCELLATION_NOTIFICATION
            )
            
            else -> null
        }
    }
    
    /**
     * Get English templates
     */
    private fun getEnglishTemplate(templateType: WhatsAppTemplateType): WhatsAppTemplate? {
        return when (templateType) {
            WhatsAppTemplateType.LEAD_GENERATED -> WhatsAppTemplate(
                id = "lead_generated_en",
                type = templateType,
                language = "en",
                content = "🎉 *New Lead Generated!*\n\n" +
                        "📋 *Lead Details:*\n" +
                        "👤 Name: {{customer_name}}\n" +
                        "📞 Phone: {{customer_phone}}\n" +
                        "🏠 Unit: {{unit_block}}-{{unit_number}}\n" +
                        "💰 Price: Rp {{unit_price}}\n\n" +
                        "📅 *Date: {{date}}*\n" +
                        "🔄 *Status: {{status}}*\n\n" +
                        "Please follow up this lead soon! 🚀",
                variables = listOf("customer_name", "customer_phone", "unit_block", "unit_number", "unit_price", "date", "status"),
                category = WhatsAppTemplateCategory.LEAD_GENERATION
            )
            
            WhatsAppTemplateType.STATUS_CHANGE -> WhatsAppTemplate(
                id = "status_change_en",
                type = templateType,
                language = "en",
                content = "📊 *KPR Status Update*\n\n" +
                        "👤 *Customer:* {{customer_name}}\n" +
                        "🏠 *Unit:* {{unit_block}}-{{unit_number}}\n\n" +
                        "🔄 *Status Change:*\n" +
                        "From: {{previous_status}}\n" +
                        "To: {{new_status}}\n\n" +
                        "📅 *Date:* {{date}}\n" +
                        "👤 *Updated by:* {{updated_by}}\n\n" +
                        "Thank you for your attention! 😊",
                variables = listOf("customer_name", "unit_block", "unit_number", "previous_status", "new_status", "date", "updated_by"),
                category = WhatsAppTemplateCategory.STATUS_UPDATE
            )
            
            WhatsAppTemplateType.DOCUMENT_UPLOADED -> WhatsAppTemplate(
                id = "document_uploaded_en",
                type = templateType,
                language = "en",
                content = "📄 *Document Uploaded*\n\n" +
                        "👤 *Customer:* {{customer_name}}\n" +
                        "📋 *Document Type:* {{document_type}}\n" +
                        "📁 *File Name:* {{file_name}}\n\n" +
                        "✅ *Status:* {{verification_status}}\n" +
                        "📅 *Uploaded:* {{upload_date}}\n\n" +
                        "Document is currently under verification. 📝",
                variables = listOf("customer_name", "document_type", "file_name", "verification_status", "upload_date"),
                category = WhatsAppTemplateCategory.DOCUMENT_NOTIFICATION
            )
            
            WhatsAppTemplateType.PAYMENT_RECEIVED -> WhatsAppTemplate(
                id = "payment_received_en",
                type = templateType,
                language = "en",
                content = "💰 *Payment Received*\n\n" +
                        "👤 *Customer:* {{customer_name}}\n" +
                        "🏠 *Unit:* {{unit_block}}-{{unit_number}}\n\n" +
                        "💳 *Payment Details:*\n" +
                        "Type: {{payment_type}}\n" +
                        "Amount: Rp {{amount}}\n" +
                        "Date: {{payment_date}}\n" +
                        "Status: {{payment_status}}\n\n" +
                        "Thank you for your payment! 🙏",
                variables = listOf("customer_name", "unit_block", "unit_number", "payment_type", "amount", "payment_date", "payment_status"),
                category = WhatsAppTemplateCategory.PAYMENT_NOTIFICATION
            )
            
            WhatsAppTemplateType.UNIT_CANCELLED -> WhatsAppTemplate(
                id = "unit_cancelled_en",
                type = templateType,
                language = "en",
                content = "❌ *Unit Cancelled*\n\n" +
                        "🏠 *Unit:* {{unit_block}}-{{unit_number}}\n" +
                        "👤 *Customer:* {{customer_name}}\n\n" +
                        "📋 *Cancellation Reason:*\n" +
                        "{{cancellation_reason}}\n\n" +
                        "📅 *Date:* {{cancellation_date}}\n" +
                        "👤 *Cancelled by:* {{cancelled_by}}\n\n" +
                        "Unit is now available for sale. 🏪",
                variables = listOf("unit_block", "unit_number", "customer_name", "cancellation_reason", "cancellation_date", "cancelled_by"),
                category = WhatsAppTemplateCategory.CANCELLATION_NOTIFICATION
            )
            
            else -> null
        }
    }
    
    /**
     * Validate template variables
     */
    private fun validateTemplateVariables(
        template: WhatsAppTemplate,
        variables: Map<String, Any>
    ): Result<Unit> {
        val missingVariables = template.variables.filter { variable ->
            !variables.containsKey(variable)
        }
        
        if (missingVariables.isNotEmpty()) {
            return Result.failure(
                Exception("Missing required variables: ${missingVariables.joinToString(", ")}")
            )
        }
        
        if (variables.size > MAX_TEMPLATE_VARIABLES) {
            return Result.failure(
                Exception("Too many variables. Maximum allowed: $MAX_TEMPLATE_VARIABLES")
            )
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Process template with variables
     */
    private fun processTemplate(
        template: WhatsAppTemplate,
        variables: Map<String, Any>
    ): String {
        var processedContent = template.content
        
        variables.forEach { (key, value) ->
            processedContent = processedContent.replace("{{$key}}", value.toString())
        }
        
        return processedContent
    }
    
    /**
     * Add language-specific formatting
     */
    private fun addLanguageSpecificFormatting(
        message: String,
        language: String
    ): String {
        return when (language) {
            "id" -> {
                // Indonesian formatting
                message.replace("Rp ", "Rp ")
                    .replace(".", ",") // Decimal separator
            }
            "en" -> {
                // English formatting
                message.replace("Rp ", "IDR ")
                    .replace(",", ".") // Decimal separator
            }
            else -> message
        }
    }
    
    /**
     * Send via Fonnte gateway
     */
    private suspend fun sendViaFonnte(
        recipient: String,
        message: String,
        template: WhatsAppTemplate
    ): Result<WhatsAppMessageResult> {
        return try {
            // Format Indonesian phone number
            val formattedNumber = formatPhoneNumber(recipient, "id")
            
            // Simulate API call
            val response = DummyWhatsAppResponse(
                success = true,
                messageId = UUID.randomUUID().toString(),
                status = "sent",
                timestamp = System.currentTimeMillis()
            )
            
            Result.success(
                WhatsAppMessageResult(
                    success = true,
                    messageId = response.messageId,
                    recipient = formattedNumber,
                    templateId = template.id,
                    gateway = WhatsAppGateway.FONNTE,
                    sentAt = response.timestamp,
                    deliveredAt = response.timestamp + 5000, // 5 seconds delivery
                    status = WhatsAppMessageStatus.SENT
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send via Wablas gateway
     */
    private suspend fun sendViaWablas(
        recipient: String,
        message: String,
        template: WhatsAppTemplate
    ): Result<WhatsAppMessageResult> {
        return try {
            val formattedNumber = formatPhoneNumber(recipient, "id")
            
            // Simulate API call
            val response = DummyWhatsAppResponse(
                success = true,
                messageId = UUID.randomUUID().toString(),
                status = "queued",
                timestamp = System.currentTimeMillis()
            )
            
            Result.success(
                WhatsAppMessageResult(
                    success = true,
                    messageId = response.messageId,
                    recipient = formattedNumber,
                    templateId = template.id,
                    gateway = WhatsAppGateway.WABLAS,
                    sentAt = response.timestamp,
                    deliveredAt = response.timestamp + 3000, // 3 seconds delivery
                    status = WhatsAppMessageStatus.QUEUED
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send via WhatsApp Business API
     */
    private suspend fun sendViaWhatsAppBusiness(
        recipient: String,
        message: String,
        template: WhatsAppTemplate
    ): Result<WhatsAppMessageResult> {
        return try {
            val formattedNumber = formatPhoneNumber(recipient, "id")
            
            // Simulate API call
            val response = DummyWhatsAppResponse(
                success = true,
                messageId = UUID.randomUUID().toString(),
                status = "delivered",
                timestamp = System.currentTimeMillis()
            )
            
            Result.success(
                WhatsAppMessageResult(
                    success = true,
                    messageId = response.messageId,
                    recipient = formattedNumber,
                    templateId = template.id,
                    gateway = WhatsAppGateway.WHATSAPP_BUSINESS,
                    sentAt = response.timestamp,
                    deliveredAt = response.timestamp + 1000, // 1 second delivery
                    status = WhatsAppMessageStatus.DELIVERED
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send via dummy gateway for testing
     */
    private suspend fun sendViaDummyGateway(
        recipient: String,
        message: String,
        template: WhatsAppTemplate
    ): Result<WhatsAppMessageResult> {
        return try {
            val formattedNumber = formatPhoneNumber(recipient, "id")
            
            // Simulate processing
            kotlinx.coroutines.delay(100)
            
            Result.success(
                WhatsAppMessageResult(
                    success = true,
                    messageId = UUID.randomUUID().toString(),
                    recipient = formattedNumber,
                    templateId = template.id,
                    gateway = WhatsAppGateway.DUMMY,
                    sentAt = System.currentTimeMillis(),
                    deliveredAt = System.currentTimeMillis() + 1000,
                    status = WhatsAppMessageStatus.SENT
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Format phone number for Indonesia
     */
    private fun formatPhoneNumber(phoneNumber: String, countryCode: String): String {
        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        
        return when {
            cleanNumber.startsWith("0") -> "+62$cleanNumber"
            cleanNumber.startsWith("62") -> "+$cleanNumber"
            cleanNumber.startsWith("+") -> cleanNumber
            else -> "+62$cleanNumber"
        }
    }
    
    /**
     * Log WhatsApp message
     */
    private suspend fun logWhatsAppMessage(
        recipient: String,
        templateType: WhatsAppTemplateType,
        language: String,
        gateway: WhatsAppGateway,
        result: Result<WhatsAppMessageResult>
    ) {
        val logData = mapOf(
            "recipient" to recipient,
            "template_type" to templateType.name,
            "language" to language,
            "gateway" to gateway.name,
            "success" to (result.isSuccess),
            "message_id" to (result.getOrNull()?.messageId ?: "failed"),
            "timestamp" to System.currentTimeMillis()
        )
        
        whatsAppRepository.logWhatsAppMessage(logData)
    }
    
    /**
     * Get message analytics
     */
    suspend fun getMessageAnalytics(
        startDate: Date,
        endDate: Date
    ): Result<WhatsAppAnalytics> = withContext(Dispatchers.IO) {
        try {
            val messages = whatsAppRepository.getMessagesByDateRange(startDate, endDate)
                .getOrNull() ?: emptyList()
            
            val analytics = WhatsAppAnalytics(
                totalMessages = messages.size,
                successRate = messages.count { it.success }.toDouble() / messages.size,
                messagesByGateway = messages.groupBy { it.gateway }.mapValues { it.value.size },
                messagesByTemplate = messages.groupBy { it.templateId }.mapValues { it.value.size },
                messagesByLanguage = messages.groupBy { it.language }.mapValues { it.value.size },
                avgDeliveryTime = messages.mapNotNull { it.deliveredAt - it.sentAt }.average(),
                mostUsedTemplate = messages.groupingBy { it.templateId }.eachCount().maxByOrNull { it.value }?.key,
                bestPerformingGateway = messages.groupBy { it.gateway }
                    .mapValues { it.value.count { it.success }.toDouble() / it.value.size }
                    .maxByOrNull { it.value }?.key
            )
            
            Result.success(analytics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create custom template
     */
    suspend fun createCustomTemplate(
        template: WhatsAppTemplate
    ): Result<WhatsAppTemplate> {
        return try {
            // Validate template
            if (template.content.isBlank()) {
                return Result.failure(Exception("Template content cannot be empty"))
            }
            
            if (template.variables.size > MAX_TEMPLATE_VARIABLES) {
                return Result.failure(Exception("Too many variables"))
            }
            
            // Save template
            val savedTemplate = whatsAppRepository.saveTemplate(template)
            Result.success(savedTemplate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Data classes
data class WhatsAppTemplate(
    val id: String,
    val type: WhatsAppTemplateType,
    val language: String,
    val content: String,
    val variables: List<String>,
    val category: WhatsAppTemplateCategory,
    val version: String = AdvancedWhatsAppManager.Companion.TEMPLATE_VERSION,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class WhatsAppMessageResult(
    val success: Boolean,
    val messageId: String,
    val recipient: String,
    val templateId: String,
    val gateway: WhatsAppGateway,
    val sentAt: Long,
    val deliveredAt: Long?,
    val status: WhatsAppMessageStatus,
    val error: String? = null,
    val language: String = "id"
)

data class WhatsAppAnalytics(
    val totalMessages: Int,
    val successRate: Double,
    val messagesByGateway: Map<WhatsAppGateway, Int>,
    val messagesByTemplate: Map<String, Int>,
    val messagesByLanguage: Map<String, Int>,
    val avgDeliveryTime: Double,
    val mostUsedTemplate: String?,
    val bestPerformingGateway: WhatsAppGateway?
)

data class DummyWhatsAppResponse(
    val success: Boolean,
    val messageId: String,
    val status: String,
    val timestamp: Long
)

// Enums
enum class WhatsAppTemplateType {
    LEAD_GENERATED,
    STATUS_CHANGE,
    DOCUMENT_UPLOADED,
    PAYMENT_RECEIVED,
    UNIT_CANCELLED,
    PAYMENT_REMINDER,
    APPOINTMENT_REMINDER,
    CUSTOMER_BIRTHDAY,
    SYSTEM_NOTIFICATION
}

enum class WhatsAppTemplateCategory {
    LEAD_GENERATION,
    STATUS_UPDATE,
    DOCUMENT_NOTIFICATION,
    PAYMENT_NOTIFICATION,
    CANCELLATION_NOTIFICATION,
    REMINDER,
    MARKETING,
    SYSTEM
}

enum class WhatsAppGateway {
    FONNTE,
    WABLAS,
    WHATSAPP_BUSINESS,
    DUMMY
}

enum class WhatsAppMessageStatus {
    QUEUED,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
