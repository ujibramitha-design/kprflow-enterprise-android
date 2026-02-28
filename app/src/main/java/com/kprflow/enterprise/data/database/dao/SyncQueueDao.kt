package com.kprflow.enterprise.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kprflow.enterprise.data.database.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    
    @Query("SELECT * FROM sync_queue ORDER BY priority DESC, createdAt ASC")
    fun getAllSyncItems(): Flow<List<SyncQueueEntity>>
    
    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    fun getSyncItemsByStatus(status: String): Flow<List<SyncQueueEntity>>
    
    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType ORDER BY priority DESC, createdAt ASC")
    fun getSyncItemsByEntityType(entityType: String): Flow<List<SyncQueueEntity>>
    
    @Query("SELECT * FROM sync_queue WHERE operation = :operation ORDER BY priority DESC, createdAt ASC")
    fun getSyncItemsByOperation(operation: String): Flow<List<SyncQueueEntity>>
    
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' AND nextRetryAt <= :timestamp ORDER BY priority DESC, createdAt ASC")
    suspend fun getPendingSyncItems(timestamp: Long): List<SyncQueueEntity>
    
    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED' AND retryCount < maxRetries AND nextRetryAt <= :timestamp ORDER BY priority DESC, createdAt ASC")
    suspend fun getRetryableFailedItems(timestamp: Long): List<SyncQueueEntity>
    
    @Query("SELECT * FROM sync_queue WHERE entityId = :entityId ORDER BY createdAt DESC")
    fun getSyncItemsByEntityId(entityId: String): Flow<List<SyncQueueEntity>>
    
    @Query("SELECT * FROM sync_queue WHERE priority >= :minPriority ORDER BY priority DESC, createdAt ASC")
    fun getSyncItemsByMinPriority(minPriority: Int): Flow<List<SyncQueueEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(syncItem: SyncQueueEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItems(syncItems: List<SyncQueueEntity>)
    
    @Update
    suspend fun updateSyncItem(syncItem: SyncQueueEntity)
    
    @Delete
    suspend fun deleteSyncItem(syncItem: SyncQueueEntity)
    
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteSyncItemById(id: String)
    
    @Query("DELETE FROM sync_queue WHERE entityId = :entityId")
    suspend fun deleteSyncItemsByEntityId(entityId: String)
    
    @Query("DELETE FROM sync_queue WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedSyncItems()
    
    @Query("DELETE FROM sync_queue WHERE status = 'FAILED' AND retryCount >= maxRetries")
    suspend fun deleteFailedSyncItems()
    
    @Query("UPDATE sync_queue SET status = 'PROCESSING', updatedAt = :timestamp WHERE id = :id")
    suspend fun markAsProcessing(id: String, timestamp: Long)
    
    @Query("UPDATE sync_queue SET status = 'COMPLETED', updatedAt = :timestamp WHERE id = :id")
    suspend fun markAsCompleted(id: String, timestamp: Long)
    
    @Query("UPDATE sync_queue SET status = 'FAILED', errorMessage = :error, retryCount = retryCount + 1, nextRetryAt = :nextRetry, updatedAt = :timestamp WHERE id = :id")
    suspend fun markAsFailed(id: String, error: String, nextRetry: Long, timestamp: Long)
    
    @Query("UPDATE sync_queue SET priority = :priority, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePriority(id: String, priority: Int, timestamp: Long)
    
    @Query("UPDATE sync_queue SET data = :data, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateSyncData(id: String, data: String, timestamp: Long)
    
    @Query("DELETE FROM sync_queue")
    suspend fun deleteAllSyncItems()
    
    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getSyncItemCount(): Int
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    suspend fun getPendingSyncCount(): Int
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PROCESSING'")
    suspend fun getProcessingSyncCount(): Int
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'FAILED'")
    suspend fun getFailedSyncCount(): Int
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'COMPLETED'")
    suspend fun getCompletedSyncCount(): Int
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE entityType = :entityType")
    suspend fun getSyncCountByEntityType(entityType: String): Int
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE operation = :operation")
    suspend fun getSyncCountByOperation(operation: String): Int
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE retryCount >= maxRetries")
    suspend fun getPermanentlyFailedCount(): Int
}
