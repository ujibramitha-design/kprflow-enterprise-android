package com.kprflow.enterprise.data.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class KprDossier(
    val id: String,
    val userId: String,
    val unitId: String? = null,
    val status: KprStatus,
    val bookingDate: LocalDate,
    val kprAmount: BigDecimal? = null,
    val dpAmount: BigDecimal? = null,
    val bankName: String? = null,
    val sp3kIssuedDate: LocalDate? = null,
    val akadDate: LocalDate? = null,
    val disbursedDate: LocalDate? = null,
    val bastDate: LocalDate? = null,
    val cancellationReason: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val notes: String? = null,
    // Additional properties for UI
    val customerName: String? = null,
    val customerEmail: String? = null,
    val customerPhone: String? = null
)
