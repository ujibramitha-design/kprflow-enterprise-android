package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.Document
import com.kprflow.enterprise.data.model.DocumentStatus
import com.kprflow.enterprise.data.model.DocumentType
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Interface for Document Repository
 * Following dependency injection best practices for testability
 */
interface IDocumentRepository {
    suspend fun uploadDocument(
        dossierId: String,
        documentType: DocumentType,
        file: File,
        fileName: String
    ): Result<Document>
    
    suspend fun getDocumentsByDossierId(dossierId: String): Result<List<Document>>
    suspend fun getDocumentById(documentId: String): Result<Document?>
    suspend fun updateDocumentStatus(documentId: String, newStatus: DocumentStatus): Result<Document>
    suspend fun deleteDocument(documentId: String): Result<Unit>
    suspend fun downloadDocument(documentId: String): Result<File>
    
    // Document validation
    suspend fun validateDocument(documentId: String): Result<Boolean>
    suspend fun getRequiredDocumentsForStatus(status: DocumentStatus): Result<List<DocumentType>>
    
    // Real-time listeners
    fun observeDocumentsByDossier(dossierId: String): Flow<List<Document>>
    fun observeDocument(documentId: String): Flow<Document?>
    
    // Batch operations
    suspend fun uploadMultipleDocuments(
        dossierId: String,
        documents: List<Pair<DocumentType, File>>
    ): Result<List<Document>>
    
    suspend fun getDocumentStatistics(dossierId: String): Result<DocumentStatistics>
}

data class DocumentStatistics(
    val totalDocuments: Int,
    val uploadedDocuments: Int,
    val validatedDocuments: Int,
    val rejectedDocuments: Int,
    val pendingDocuments: Int,
    val completionPercentage: Float
)
