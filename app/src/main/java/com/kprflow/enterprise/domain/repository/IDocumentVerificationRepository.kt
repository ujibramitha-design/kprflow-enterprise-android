package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.Document
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Document Verification Repository
 * Following Clean Architecture principles for testability
 */
interface IDocumentVerificationRepository {
    suspend fun verifyDocument(documentId: String): Result<Document>
    suspend fun getPendingDocuments(): Result<List<Document>>
    suspend fun updateDocumentStatus(documentId: String, status: String, notes: String? = null): Result<Document>
    suspend fun batchVerifyDocuments(documentIds: List<String>): Result<List<Document>>
    fun observeVerificationQueue(): Flow<List<Document>>
    suspend fun getVerificationStatistics(): Result<Map<String, Int>>
}
