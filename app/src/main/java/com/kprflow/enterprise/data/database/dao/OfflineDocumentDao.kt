package com.kprflow.enterprise.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kprflow.enterprise.data.database.entities.OfflineDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineDocumentDao {
    
    @Query("SELECT * FROM offline_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<OfflineDocumentEntity>>
    
    @Query("SELECT * FROM offline_documents WHERE dossierId = :dossierId ORDER BY createdAt DESC")
    fun getDocumentsByDossier(dossierId: String): Flow<List<OfflineDocumentEntity>>
    
    @Query("SELECT * FROM offline_documents WHERE id = :id")
    suspend fun getDocumentById(id: String): OfflineDocumentEntity?
    
    @Query("SELECT * FROM offline_documents WHERE documentType = :type ORDER BY createdAt DESC")
    fun getDocumentsByType(type: String): Flow<List<OfflineDocumentEntity>>
    
    @Query("SELECT * FROM offline_documents WHERE uploadStatus = :status ORDER BY createdAt DESC")
    fun getDocumentsByUploadStatus(status: String): Flow<List<OfflineDocumentEntity>>
    
    @Query("SELECT * FROM offline_documents WHERE isUploaded = 0 ORDER BY createdAt DESC")
    fun getPendingUploads(): Flow<List<OfflineDocumentEntity>>
    
    @Query("SELECT * FROM offline_documents WHERE isDirty = 1")
    suspend fun getDirtyDocuments(): List<OfflineDocumentEntity>
    
    @Query("SELECT * FROM offline_documents WHERE localPath IS NOT NULL AND isUploaded = 0")
    suspend fun getDocumentsToUpload(): List<OfflineDocumentEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: OfflineDocumentEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<OfflineDocumentEntity>)
    
    @Update
    suspend fun updateDocument(document: OfflineDocumentEntity)
    
    @Delete
    suspend fun deleteDocument(document: OfflineDocumentEntity)
    
    @Query("DELETE FROM offline_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)
    
    @Query("DELETE FROM offline_documents WHERE dossierId = :dossierId")
    suspend fun deleteDocumentsByDossier(dossierId: String)
    
    @Query("UPDATE offline_documents SET isDirty = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDocumentAsDirty(id: String, timestamp: Long)
    
    @Query("UPDATE offline_documents SET isUploaded = 1, uploadedAt = :timestamp, uploadStatus = 'COMPLETED' WHERE id = :id")
    suspend fun markDocumentAsUploaded(id: String, timestamp: Long)
    
    @Query("UPDATE offline_documents SET uploadStatus = :status, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateUploadStatus(id: String, status: String, timestamp: Long)
    
    @Query("UPDATE offline_documents SET localPath = :path, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateLocalPath(id: String, path: String, timestamp: Long)
    
    @Query("UPDATE offline_documents SET filePath = :path, fileSize = :size, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateFileInfo(id: String, path: String, size: Long, timestamp: Long)
    
    @Query("DELETE FROM offline_documents")
    suspend fun deleteAllDocuments()
    
    @Query("SELECT COUNT(*) FROM offline_documents")
    suspend fun getDocumentCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_documents WHERE dossierId = :dossierId")
    suspend fun getDocumentCountByDossier(dossierId: String): Int
    
    @Query("SELECT COUNT(*) FROM offline_documents WHERE isUploaded = 0")
    suspend fun getPendingUploadCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_documents WHERE isDirty = 1")
    suspend fun getDirtyDocumentCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_documents WHERE documentType = :type")
    suspend fun getDocumentCountByType(type: String): Int
}
