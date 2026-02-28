package com.kprflow.enterprise.communication

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WhatsApp Notifier - Nerve Test: WhatsApp Gateway Communication
 * Phase 25: Final Approval with WhatsApp Integration
 */
@Singleton
class WhatsAppNotifier @Inject constructor() {
    
    private val _notifierState = MutableStateFlow<NotifierState>(NotifierState.Idle)
    val notifierState: StateFlow<NotifierState> = _notifierState.asStateFlow()
    
    private val _messageStatus = MutableStateFlow<MessageStatus?>(null)
    val messageStatus: StateFlow<MessageStatus?> = _messageStatus.asStateFlow()
    
    private val _deliveryReport = MutableStateFlow<DeliveryReport?>(null)
    val deliveryReport: StateFlow<DeliveryReport?> = _deliveryReport.asStateFlow()
    
    /**
     * Send WhatsApp Message - Nerve Test
     */
    suspend fun sendWhatsAppMessage(
        recipient: String,
        message: String,
        messageType: MessageType = MessageType.NOTIFICATION
    ): MessageResult {
        return try {
            _notifierState.value = NotifierState.Sending
            _messageStatus.value = MessageStatus(
                recipient = recipient,
                message = message,
                messageType = messageType,
                status = MessageStatus.SENDING,
                timestamp = System.currentTimeMillis()
            )
            
            // Simulate WhatsApp API call
            delay(2000) // Simulate network delay
            
            // Simulate success/failure (90% success rate)
            val success = (1..10).random() > 1
            
            if (success) {
                val messageId = generateMessageId()
                val deliveryTime = System.currentTimeMillis()
                
                _messageStatus.value = _messageStatus.value?.copy(
                    status = MessageStatus.SENT,
                    messageId = messageId,
                    sentAt = deliveryTime
                )
                
                // Simulate delivery confirmation
                delay(1000)
                _messageStatus.value = _messageStatus.value?.copy(
                    status = MessageStatus.DELIVERED,
                    deliveredAt = System.currentTimeMillis()
                )
                
                // Create delivery report
                val report = DeliveryReport(
                    messageId = messageId,
                    recipient = recipient,
                    message = message,
                    messageType = messageType,
                    sentAt = deliveryTime,
                    deliveredAt = System.currentTimeMillis(),
                    status = DeliveryStatus.DELIVERED,
                    deliveryTime = 1000L, // 1 second delivery time
                    error = null
                )
                
                _deliveryReport.value = report
                _notifierState.value = NotifierState.Sent
                
                MessageResult(
                    success = true,
                    messageId = messageId,
                    error = null,
                    deliveryReport = report
                )
            } else {
                val error = "WhatsApp API error: Failed to send message"
                
                _messageStatus.value = _messageStatus.value?.copy(
                    status = MessageStatus.FAILED,
                    error = error,
                    failedAt = System.currentTimeMillis()
                )
                
                _notifierState.value = NotifierState.Error(error)
                
                MessageResult(
                    success = false,
                    messageId = null,
                    error = error,
                    deliveryReport = null
                )
            }
            
        } catch (exc: Exception) {
            val error = "WhatsApp notification failed: ${exc.message}"
            
            _messageStatus.value = _messageStatus.value?.copy(
                status = MessageStatus.FAILED,
                error = error,
                failedAt = System.currentTimeMillis()
            )
            
            _notifierState.value = NotifierState.Error(error)
            
            MessageResult(
                success = false,
                messageId = null,
                error = error,
                deliveryReport = null
            )
        }
    }
    
    /**
     * Send Bulk WhatsApp Messages
     */
    suspend fun sendBulkMessages(
        recipients: List<String>,
        message: String,
        messageType: MessageType = MessageType.NOTIFICATION
    ): BulkMessageResult {
        return try {
            _notifierState.value = NotifierState.Sending
            
            val results = mutableListOf<MessageResult>()
            val successfulDeliveries = mutableListOf<DeliveryReport>()
            
            for (recipient in recipients) {
                val result = sendWhatsAppMessage(recipient, message, messageType)
                results.add(result)
                
                if (result.success && result.deliveryReport != null) {
                    successfulDeliveries.add(result.deliveryReport)
                }
                
                // Small delay between messages to avoid rate limiting
                delay(500)
            }
            
            val successRate = (successfulDeliveries.size.toDouble() / recipients.size) * 100
            
            _notifierState.value = if (successRate >= 80) NotifierState.Sent else NotifierState.PartiallySent
            
            BulkMessageResult(
                totalRecipients = recipients.size,
                successfulDeliveries = successfulDeliveries.size,
                failedDeliveries = recipients.size - successfulDeliveries.size,
                successRate = successRate,
                results = results,
                deliveryReports = successfulDeliveries
            )
            
        } catch (exc: Exception) {
            _notifierState.value = NotifierState.Error("Bulk messaging failed: ${exc.message}")
            BulkMessageResult(
                totalRecipients = recipients.size,
                successfulDeliveries = 0,
                failedDeliveries = recipients.size,
                successRate = 0.0,
                results = emptyList(),
                deliveryReports = emptyList()
            )
        }
    }
    
    /**
     * Send Phase-Specific Notifications
     */
    suspend fun sendPhaseNotification(
        phase: Int,
        recipient: String,
        phaseData: Map<String, String>
    ): MessageResult {
        val message = generatePhaseMessage(phase, phaseData)
        return sendWhatsAppMessage(recipient, message, MessageType.PHASE_UPDATE)
    }
    
    /**
     * Send SLA Alert
     */
    suspend fun sendSLAAlert(
        recipient: String,
        slaData: Map<String, String>
    ): MessageResult {
        val message = generateSLAMessage(slaData)
        return sendWhatsAppMessage(recipient, message, MessageType.SLA_ALERT)
    }
    
    /**
     * Send Approval Notification
     */
    suspend fun sendApprovalNotification(
        recipient: String,
        approvalData: Map<String, String>
    ): MessageResult {
        val message = generateApprovalMessage(approvalData)
        return sendWhatsAppMessage(recipient, message, MessageType.APPROVAL)
    }
    
    /**
     * Generate Phase Message
     */
    private fun generatePhaseMessage(phase: Int, data: Map<String, String>): String {
        return when (phase) {
            24 -> """
                🏗️ *PHASE 24: QUALITY CONTROL*
                
                Property Verification Complete:
                📍 Location: ${data["location"] ?: "Unknown"}
                📸 Photo: Captured
                📋 Document: Verified
                ✅ Status: ${data["status"] ?: "Verified"}
                
                Field verification completed successfully!
            """.trimIndent()
            
            25 -> """
                🎯 *PHASE 25: FINAL APPROVAL*
                
                Application Status:
                📄 Application ID: ${data["applicationId"] ?: "Unknown"}
                👤 Applicant: ${data["applicant"] ?: "Unknown"}
                🏠 Property: ${data["property"] ?: "Unknown"}
                ✅ Status: ${data["status"] ?: "Approved"}
                
                Congratulations! Your application has been approved.
            """.trimIndent()
            
            else -> """
                📢 *PHASE $phase UPDATE*
                
                ${data["message"] ?: "Phase $phase update"}
                
                Status: ${data["status"] ?: "In Progress"}
            """.trimIndent()
        }
    }
    
    /**
     * Generate SLA Message
     */
    private fun generateSLAMessage(data: Map<String, String>): String {
        return """
            ⚠️ *SLA ALERT*
            
            Task: ${data["task"] ?: "Unknown"}
            Deadline: ${data["deadline"] ?: "Unknown"}
            Status: ${data["status"] ?: "Overdue"}
            
            ⏰ Time Remaining: ${data["timeRemaining"] ?: "Expired"}
            
            Please take immediate action!
        """.trimIndent()
    }
    
    /**
     * Generate Approval Message
     */
    private fun generateApprovalMessage(data: Map<String, String>): String {
        return """
            ✅ *APPROVAL NOTIFICATION*
            
            Document: ${data["document"] ?: "Unknown"}
            Requested by: ${data["requester"] ?: "Unknown"}
            Approved by: ${data["approver"] ?: "Unknown"}
            
            Status: ${data["status"] ?: "Approved"}
            Comments: ${data["comments"] ?: "No comments"}
            
            Process can now continue to next phase.
        """.trimIndent()
    }
    
    /**
     * Generate Message ID
     */
    private fun generateMessageId(): String {
        return "WA_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * Get Notifier State
     */
    fun getNotifierState(): NotifierState = _notifierState.value
    
    /**
     * Get Message Status
     */
    fun getMessageStatus(): MessageStatus? = _messageStatus.value
    
    /**
     * Get Delivery Report
     */
    fun getDeliveryReport(): DeliveryReport? = _deliveryReport.value
    
    /**
     * Clear Status
     */
    fun clearStatus() {
        _messageStatus.value = null
        _deliveryReport.value = null
        _notifierState.value = NotifierState.Idle
    }
    
    /**
     * Test WhatsApp Connection (Nerve Test)
     */
    suspend fun testWhatsAppConnection(): TestResult {
        return try {
            _notifierState.value = NotifierState.Testing
            
            // Send test message
            val testResult = sendWhatsAppMessage(
                recipient = "+628123456789", // Test number
                message = "🧪 WhatsApp Gateway Test - Connection Verified",
                messageType = MessageType.TEST
            )
            
            _notifierState.value = if (testResult.success) NotifierState.Connected else NotifierState.Error(testResult.error ?: "Unknown error")
            
            TestResult(
                success = testResult.success,
                responseTime = testResult.deliveryReport?.deliveryTime ?: 0L,
                error = testResult.error,
                messageId = testResult.messageId
            )
            
        } catch (exc: Exception) {
            _notifierState.value = NotifierState.Error("Connection test failed: ${exc.message}")
            TestResult(
                success = false,
                responseTime = 0L,
                error = exc.message,
                messageId = null
            )
        }
    }
}

/**
 * Notifier State
 */
sealed class NotifierState {
    object Idle : NotifierState()
    object Sending : NotifierState()
    object Sent : NotifierState()
    object PartiallySent : NotifierState()
    object Testing : NotifierState()
    object Connected : NotifierState()
    data class Error(val message: String) : NotifierState()
}

/**
 * Message Status
 */
data class MessageStatus(
    val recipient: String,
    val message: String,
    val messageType: MessageType,
    val status: String,
    val timestamp: Long,
    val messageId: String? = null,
    val sentAt: Long? = null,
    val deliveredAt: Long? = null,
    val failedAt: Long? = null,
    val error: String? = null
) {
    companion object {
        const val SENDING = "SENDING"
        const val SENT = "SENT"
        const val DELIVERED = "DELIVERED"
        const val FAILED = "FAILED"
    }
}

/**
 * Delivery Report
 */
data class DeliveryReport(
    val messageId: String,
    val recipient: String,
    val message: String,
    val messageType: MessageType,
    val sentAt: Long,
    val deliveredAt: Long,
    val status: DeliveryStatus,
    val deliveryTime: Long,
    val error: String?
)

/**
 * Message Result
 */
data class MessageResult(
    val success: Boolean,
    val messageId: String?,
    val error: String?,
    val deliveryReport: DeliveryReport?
)

/**
 * Bulk Message Result
 */
data class BulkMessageResult(
    val totalRecipients: Int,
    val successfulDeliveries: Int,
    val failedDeliveries: Int,
    val successRate: Double,
    val results: List<MessageResult>,
    val deliveryReports: List<DeliveryReport>
)

/**
 * Test Result
 */
data class TestResult(
    val success: Boolean,
    val responseTime: Long,
    val error: String?,
    val messageId: String?
)

/**
 * Message Type
 */
enum class MessageType {
    NOTIFICATION,
    PHASE_UPDATE,
    SLA_ALERT,
    APPROVAL,
    TEST
}

/**
 * Delivery Status
 */
enum class DeliveryStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    READ
}
