package com.kprflow.enterprise.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val id: String,
    val dossierId: String,
    val type: DocumentType,
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val isVerified: Boolean = false,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val rejectionReason: String? = null,
    val uploadedAt: String,
    val updatedAt: String
)
