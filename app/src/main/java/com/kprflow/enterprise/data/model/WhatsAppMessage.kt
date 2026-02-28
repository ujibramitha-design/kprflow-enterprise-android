package com.kprflow.enterprise.data.model

import kotlinx.serialization.Serializable

/**
 * WhatsApp Message Data Model
 * Used for WhatsApp Group integration
 */
@Serializable
data class WhatsAppMessage(
    val id: String,
    val senderName: String,
    val senderPhone: String,
    val content: String,
    val timestamp: String,
    val messageType: String = "text",
    val groupId: String = "KPRFlow_Group_001",
    val processed: Boolean = false
)
