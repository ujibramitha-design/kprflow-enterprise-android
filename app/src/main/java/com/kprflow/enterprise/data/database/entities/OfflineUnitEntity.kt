package com.kprflow.enterprise.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.UUID

@Entity(tableName = "offline_units")
data class OfflineUnitEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val projectName: String,
    val blockName: String,
    val unitNumber: String,
    val unitType: String,
    val unitPrice: Double,
    val status: String,
    val description: String?,
    val specifications: String?,
    val lastSynced: Long,
    val isDirty: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "offline_dossiers")
data class OfflineDossierEntity(
    @PrimaryKey
    val id: String,
    val customerName: String,
    val customerPhone: String?,
    val customerEmail: String?,
    val customerNIK: String?,
    val unitId: String,
    val currentStatus: String,
    val documentCompletion: Int,
    val paymentProgress: Double,
    val totalAmount: Double,
    val downPayment: Double,
    val applicationDate: Long,
    val lastUpdated: Long,
    val lastSynced: Long,
    val isDirty: Boolean,
    val notes: String?,
    val assignedTo: String?,
    val marketingId: String?,
    val legalId: String?,
    val financeId: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "offline_documents")
data class OfflineDocumentEntity(
    @PrimaryKey
    val id: String,
    val dossierId: String,
    val documentType: String,
    val fileName: String,
    val filePath: String?,
    val fileSize: Long,
    val uploadStatus: String,
    val localPath: String?,
    val isUploaded: Boolean,
    val isDirty: Boolean,
    val uploadedAt: Long?,
    val lastSynced: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "offline_payments")
data class OfflinePaymentEntity(
    @PrimaryKey
    val id: String,
    val dossierId: String,
    val paymentType: String,
    val amount: Double,
    val paymentDate: Long,
    val paymentMethod: String,
    val status: String,
    val receiptNumber: String?,
    val notes: String?,
    val isSynced: Boolean,
    val isDirty: Boolean,
    val lastSynced: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val data: String,
    val priority: Int,
    val retryCount: Int,
    val maxRetries: Int,
    val nextRetryAt: Long,
    val status: String,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long
)
