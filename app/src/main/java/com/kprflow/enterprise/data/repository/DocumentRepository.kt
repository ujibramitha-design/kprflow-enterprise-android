package com.kprflow.enterprise.data.repository

import com.kprflow.enterprise.data.model.Document
import com.kprflow.enterprise.data.model.DocumentStatus
import com.kprflow.enterprise.data.model.DocumentType
import com.kprflow.enterprise.domain.repository.IDocumentRepository
import com.kprflow.enterprise.domain.repository.DocumentStatistics
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val storage: Storage,
    private val postgrest: Postgrest
) : IDocumentRepository {
    
    private val bucketName = "kpr_documents"
    
    override suspend fun uploadDocument(
        dossierId: String,
        documentType: DocumentType,
        file: File,
        fileName: String
    ): Result<Document> {
        return try {
            // Generate unique file path
            val uniqueFileName = "${UUID.randomUUID()}_$fileName"
            val filePath = "dossiers/$dossierId/${documentType.name}/$uniqueFileName"
            
            // Upload file to Supabase Storage
            storage[bucketName].upload(filePath, file.readBytes())
            
            // Get public URL
            val publicUrl = storage[bucketName].publicUrl(filePath)
            
            // Create document record in database
            val document = Document(
                id = UUID.randomUUID().toString(),
                dossierId = dossierId,
                type = documentType,
                url = publicUrl,
                fileName = fileName,
                fileSize = file.length(),
                isVerified = false,
                uploadedAt = java.time.Instant.now().toString(),
                updatedAt = java.time.Instant.now().toString()
            )
            
            createDocumentRecord(document)
            Result.success(document)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun downloadDocument(documentId: String): Result<File> {
        return try {
            val document = getDocumentById(documentId)
                ?: return Result.failure(Exception("Document not found"))
            
            // Extract file path from URL
            val filePath = extractFilePathFromUrl(document.url)
            
            // Download file from Supabase Storage
            val bytes = storage[bucketName].download(filePath)
            val file = File.createTempFile("document", ".tmp")
            file.writeBytes(bytes)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getDocumentsByDossierId(dossierId: String): Result<List<Document>> {
        return try {
            val documents = postgrest.from("documents")
                .select()
                .filter { eq("dossier_id", dossierId) }
                .data
            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getDocumentById(documentId: String): Result<Document?> {
        return try {
            val document = postgrest.from("documents")
                .select()
                .filter { eq("id", documentId) }
                .maybeSingle()
                .data
            Result.success(document)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun verifyDocument(documentId: String, verifiedBy: String, isVerified: Boolean, rejectionReason: String? = null): Result<Document> {
        return try {
            val updateData = mapOf(
                "is_verified" to isVerified,
                "verified_by" to if (isVerified) verifiedBy else null,
                "verified_at" to if (isVerified) java.time.Instant.now().toString() else null,
                "rejection_reason" to if (!isVerified) rejectionReason else null,
                "updated_at" to java.time.Instant.now().toString()
            )
            
            val updatedDocument = postgrest.from("documents")
                .update(updateData)
                .filter { eq("id", documentId) }
                .maybeSingle()
                .data
            
            updatedDocument?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to verify document"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteDocument(documentId: String): Result<Unit> {
        return try {
            val document = getDocumentById(documentId).getOrNull()
                ?: return Result.failure(Exception("Document not found"))
            
            // Delete from storage
            val filePath = extractFilePathFromUrl(document.url)
            storage[bucketName].delete(filePath)
            
            // Delete from database
            postgrest.from("documents")
                .delete()
                .filter { eq("id", documentId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMandatoryDocumentsStatus(dossierId: String): Result<Map<DocumentType, Boolean>> {
        return try {
            val documents = getDocumentsByDossierId(dossierId).getOrNull().orEmpty()
            val mandatoryTypes = DocumentType.getMandatoryDocuments()
            
            val statusMap = mandatoryTypes.associate { type ->
                val isUploaded = documents.any { it.type == type }
                val isVerified = documents.find { it.type == type }?.isVerified == true
                type to (isUploaded && isVerified)
            }
            
            Result.success(statusMap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllVerifiedDocuments(dossierId: String): Result<List<Document>> {
        return try {
            val documents = postgrest.from("documents")
                .select()
                .filter { 
                    eq("dossier_id", dossierId)
                    eq("is_verified", true)
                }
                .data
            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observeDocumentsByDossier(dossierId: String): Flow<List<Document>> = flow {
        try {
            val documents = getDocumentsByDossierId(dossierId).getOrNull().orEmpty()
            emit(documents)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    private suspend fun createDocumentRecord(document: Document): Result<Unit> {
        return try {
            postgrest.from("documents")
                .insert(document)
                .maybeSingle()
                .data
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun extractFilePathFromUrl(url: String): String {
        // Extract path from Supabase storage URL
        // Example: https://storage.supabase.co/storage/v1/object/public/kpr_documents/dossiers/123/KTP/file.pdf
        return url.substringAfter("kpr_documents/")
    }
    
    suspend fun getDocumentUrl(documentId: String): Result<String> {
        return try {
            val document = getDocumentById(documentId).getOrNull()
                ?: return Result.failure(Exception("Document not found"))
            
            Result.success(document.url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateDocument(document: Document): Result<Document> {
        return try {
            val updatedDocument = postgrest.from("documents")
                .update(document)
                .filter { eq("id", document.id) }
                .maybeSingle()
                .data
            
            updatedDocument?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to update document"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
