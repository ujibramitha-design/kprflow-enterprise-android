package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.SPRData
import com.kprflow.enterprise.data.model.WhatsAppMessage
import kotlinx.coroutines.flow.Flow

/**
 * Interface for WhatsApp Repository
 * Following Clean Architecture principles for testability
 */
interface IWhatsAppRepository {
    suspend fun connectToWhatsAppGroup(groupId: String): Result<Boolean>
    suspend fun fetchGroupMessages(groupId: String, limit: Int = 50): Result<List<WhatsAppMessage>>
    suspend fun parseSPRFromMessage(message: WhatsAppMessage): Result<SPRData>
    suspend fun createInactiveSPR(sprData: SPRData): Result<String>
    fun observeNewMessages(groupId: String): Flow<WhatsAppMessage>
    suspend fun sendConfirmationMessage(phoneNumber: String, sprId: String): Result<Unit>
}
