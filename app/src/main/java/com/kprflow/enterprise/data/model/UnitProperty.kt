package com.kprflow.enterprise.data.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class UnitProperty(
    val id: String,
    val block: String,
    val unitNumber: String,
    val type: String,
    val price: BigDecimal,
    val status: UnitStatus,
    val description: String? = null,
    val createdAt: String,
    val updatedAt: String
)
