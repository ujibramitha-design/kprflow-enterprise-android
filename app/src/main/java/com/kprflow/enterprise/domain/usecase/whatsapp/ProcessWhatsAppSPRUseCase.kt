package com.kprflow.enterprise.domain.usecase.whatsapp

import com.kprflow.enterprise.data.model.SPRData
import com.kprflow.enterprise.data.model.SPRStatus
import com.kprflow.enterprise.domain.repository.IWhatsAppRepository
import javax.inject.Inject

/**
 * Use Case for Processing WhatsApp SPR Messages
 * Following Clean Architecture - business logic in domain layer
 */
class ProcessWhatsAppSPRUseCase @Inject constructor(
    private val whatsappRepository: IWhatsAppRepository
) {
    suspend operator fun invoke(groupId: String): Result<List<String>> {
        return try {
            // Connect to WhatsApp group
            val connected = whatsappRepository.connectToWhatsAppGroup(groupId).getOrNull()
                ?: return Result.failure(Exception("Failed to connect to WhatsApp group"))
            
            // Fetch recent messages
            val messages = whatsappRepository.fetchGroupMessages(groupId, 50).getOrNull()
                ?: return Result.failure(Exception("Failed to fetch messages"))
            
            val processedSPRIds = mutableListOf<String>()
            
            for (message in messages) {
                // Check if message contains SPR keywords
                if (containsSPRKeywords(message.content)) {
                    // Parse SPR data from message
                    val sprData = whatsappRepository.parseSPRFromMessage(message).getOrNull()
                        ?: continue
                    
                    // Create inactive SPR
                    val sprId = whatsappRepository.createInactiveSPR(sprData).getOrNull()
                        ?: continue
                    
                    processedSPRIds.add(sprId)
                    
                    // Send confirmation message
                    whatsappRepository.sendConfirmationMessage(
                        message.senderPhone,
                        sprId
                    )
                }
            }
            
            Result.success(processedSPRIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun containsSPRKeywords(content: String): Boolean {
        val keywords = listOf(
            "pesan rumah", "mau pesan", "beli rumah", "interested", 
            "type", "blok", "harga", "dp", "kpr", "cash"
        )
        
        return keywords.any { keyword ->
            content.contains(keyword, ignoreCase = true)
        }
    }
}

/**
 * Use Case for Monitoring WhatsApp Messages
 */
class MonitorWhatsAppMessagesUseCase @Inject constructor(
    private val whatsappRepository: IWhatsAppRepository
) {
    suspend operator fun invoke(groupId: String) = whatsappRepository.observeNewMessages(groupId)
}

/**
 * Use Case for Activating Inactive SPR
 */
class ActivateSPRUseCase @Inject constructor(
    private val whatsappRepository: IWhatsAppRepository
) {
    suspend operator fun invoke(sprId: String, userId: String): Result<Unit> {
        return try {
            // Business logic validation
            if (sprId.isBlank()) {
                return Result.failure(IllegalArgumentException("SPR ID cannot be empty"))
            }
            
            if (userId.isBlank()) {
                return Result.failure(IllegalArgumentException("User ID cannot be empty"))
            }
            
            // In real implementation, this would update the SPR status to ACTIVE
            // and assign it to the verified user
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
