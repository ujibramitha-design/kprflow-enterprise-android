package com.kprflow.enterprise.data.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * SPR (Surat Pemesanan Rumah) Data Model
 * Auto-filled from WhatsApp Group messages
 */
@Serializable
data class SPRData(
    val id: String? = null,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String? = null,
    val customerNIK: String? = null,
    val unitType: String,
    val block: String,
    val unitNumber: String,
    val price: BigDecimal,
    val dpAmount: BigDecimal? = null,
    val kprAmount: BigDecimal? = null,
    val bankName: String? = null,
    val marketingName: String? = null,
    val notes: String? = null,
    val source: String = "WHATSAPP_GROUP",
    val status: String = "INACTIVE", // Default status for auto-filled SPR
    val whatsappMessageId: String? = null,
    val createdAt: String = java.time.Instant.now().toString(),
    val updatedAt: String = java.time.Instant.now().toString()
)

@Serializable
data class SPRStatus(
    val status: String,
    val description: String,
    val isActive: Boolean
) {
    companion object {
        val INACTIVE = SPRStatus("INACTIVE", "Menunggu Verifikasi", false)
        val ACTIVE = SPRStatus("ACTIVE", "SPR Aktif", true)
        val PROCESSING = SPRStatus("PROCESSING", "Dalam Proses", true)
        val COMPLETED = SPRStatus("COMPLETED", "Selesai", false)
    }
}
