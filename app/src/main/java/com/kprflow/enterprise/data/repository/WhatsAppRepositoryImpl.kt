package com.kprflow.enterprise.data.repository

import com.kprflow.enterprise.data.model.SPRData
import com.kprflow.enterprise.data.model.WhatsAppMessage
import com.kprflow.enterprise.domain.repository.IWhatsAppRepository
import com.kprflow.enterprise.domain.repository.IKprRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val realtime: Realtime,
    private val kprRepository: IKprRepository
) : IWhatsAppRepository {

    override suspend fun connectToWhatsAppGroup(groupId: String): Result<Boolean> {
        return try {
            // Simulate WhatsApp Business API connection
            // In real implementation, this would connect to WhatsApp Business API
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchGroupMessages(groupId: String, limit: Int): Result<List<WhatsAppMessage>> {
        return try {
            // Simulate fetching messages from WhatsApp Group
            // In real implementation, this would use WhatsApp Business API
            val mockMessages = listOf(
                WhatsAppMessage(
                    id = "msg_001",
                    senderName = "Budi Santoso",
                    senderPhone = "+62812345678",
                    content = "Min, saya mau pesan rumah type 36/72 di Blok A No. 15, harga 450jt, DP 10%",
                    timestamp = java.time.Instant.now().toString()
                ),
                WhatsAppMessage(
                    id = "msg_002", 
                    senderName = "Siti Nurhaliza",
                    senderPhone = "+62823456789",
                    content = "Saya tertarik unit 45/90 di Blok B No. 23, harga 550jt, mau KPR BCA",
                    timestamp = java.time.Instant.now().toString()
                )
            )
            Result.success(mockMessages.take(limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun parseSPRFromMessage(message: WhatsAppMessage): Result<SPRData> {
        return try {
            val sprData = extractSPRData(message.content, message)
            Result.success(sprData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createInactiveSPR(sprData: SPRData): Result<String> {
        return try {
            // Create dossier with INACTIVE status
            val dossier = kprRepository.createDossier(
                userId = "auto_generated", // Will be updated when user registers
                unitId = null, // Will be assigned later
                kprAmount = sprData.kprAmount,
                dpAmount = sprData.dpAmount,
                bankName = sprData.bankName,
                notes = "Auto-generated from WhatsApp: ${sprData.notes}"
            )
            
            dossier.getOrNull()?.let { 
                Result.success(it.id) 
            } ?: Result.failure(Exception("Failed to create dossier"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeNewMessages(groupId: String): Flow<WhatsAppMessage> = flow {
        // Simulate real-time message observation
        // In real implementation, this would use WhatsApp webhook
        try {
            // Mock new message
            val newMessage = WhatsAppMessage(
                id = "msg_new_001",
                senderName = "Ahmad Wijaya",
                senderPhone = "+62834567890",
                content = "Mau pesan rumah 30/60 Blok C No. 5, harga 350jt, cash",
                timestamp = java.time.Instant.now().toString()
            )
            emit(newMessage)
        } catch (e: Exception) {
            // Handle error
        }
    }

    override suspend fun sendConfirmationMessage(phoneNumber: String, sprId: String): Result<Unit> {
        return try {
            // Simulate sending WhatsApp confirmation
            // In real implementation, this would use WhatsApp Business API
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractSPRData(content: String, message: WhatsAppMessage): SPRData {
        // Extract information using regex patterns
        val namePattern = Pattern.compile("(?:saya|nama|aku)\\s+([A-Za-z\\s]+)", Pattern.CASE_INSENSITIVE)
        val phonePattern = Pattern.compile("\\+?628\\d{8,12}")
        val typePattern = Pattern.compile("type\\s+(\\d+/\\d+)", Pattern.CASE_INSENSITIVE)
        val blockPattern = Pattern.compile("blok\\s+([A-Z])\\s*(?:no\\s*\\.?\\s*(\\d+))?", Pattern.CASE_INSENSITIVE)
        val pricePattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:jt|juta|miliar|m)", Pattern.CASE_INSENSITIVE)
        val dpPattern = Pattern.compile("dp\\s+(\\d+(?:\\.\\d+)?)\\s*%?", Pattern.CASE_INSENSITIVE)
        val bankPattern = Pattern.compile("(?:kpr|bank)\\s+([A-Z]{3})", Pattern.CASE_INSENSITIVE)

        val nameMatcher = namePattern.matcher(content)
        val phoneMatcher = phonePattern.matcher(content)
        val typeMatcher = typePattern.matcher(content)
        val blockMatcher = blockPattern.matcher(content)
        val priceMatcher = pricePattern.matcher(content)
        val dpMatcher = dpPattern.matcher(content)
        val bankMatcher = bankPattern.matcher(content)

        val customerName = if (nameMatcher.find()) nameMatcher.group(1).trim() else message.senderName
        val customerPhone = if (phoneMatcher.find()) phoneMatcher.group() else message.senderPhone
        val unitType = if (typeMatcher.find()) typeMatcher.group(1) else "36/72"
        val block = if (blockMatcher.find()) blockMatcher.group(1) else "A"
        val unitNumber = if (blockMatcher.find() && blockMatcher.group(2) != null) blockMatcher.group(2) else "1"
        
        val price = if (priceMatcher.find()) {
            val priceValue = priceMatcher.group(1).replace(".", "").toDouble()
            val multiplier = when {
                content.contains("miliar", ignoreCase = true) -> 1_000_000_000
                content.contains("juta", ignoreCase = true) || content.contains("jt", ignoreCase = true) -> 1_000_000
                else -> 1_000_000
            }
            BigDecimal((priceValue * multiplier).toLong())
        } else {
            BigDecimal("450000000")
        }

        val dpAmount = if (dpMatcher.find()) {
            val dpValue = dpMatcher.group(1).replace(".", "").toDouble()
            BigDecimal((price.toDouble() * dpValue / 100).toLong())
        } else {
            null
        }

        val bankName = if (bankMatcher.find()) bankMatcher.group(1) else null

        return SPRData(
            customerName = customerName,
            customerPhone = customerPhone,
            unitType = unitType,
            block = block,
            unitNumber = unitNumber,
            price = price,
            dpAmount = dpAmount,
            kprAmount = price - (dpAmount ?: BigDecimal.ZERO),
            bankName = bankName,
            notes = content,
            whatsappMessageId = message.id
        )
    }
}
