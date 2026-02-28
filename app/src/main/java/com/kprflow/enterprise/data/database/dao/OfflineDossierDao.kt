package com.kprflow.enterprise.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kprflow.enterprise.data.database.entities.OfflineDossierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineDossierDao {
    
    @Query("SELECT * FROM offline_dossiers ORDER BY createdAt DESC")
    fun getAllDossiers(): Flow<List<OfflineDossierEntity>>
    
    @Query("SELECT * FROM offline_dossiers WHERE id = :id")
    suspend fun getDossierById(id: String): OfflineDossierEntity?
    
    @Query("SELECT * FROM offline_dossiers WHERE customerPhone = :phone")
    suspend fun getDossierByPhone(phone: String): OfflineDossierEntity?
    
    @Query("SELECT * FROM offline_dossiers WHERE unitId = :unitId ORDER BY createdAt DESC")
    fun getDossiersByUnit(unitId: String): Flow<List<OfflineDossierEntity>>
    
    @Query("SELECT * FROM offline_dossiers WHERE currentStatus = :status ORDER BY createdAt DESC")
    fun getDossiersByStatus(status: String): Flow<List<OfflineDossierEntity>>
    
    @Query("SELECT * FROM offline_dossiers WHERE assignedTo = :userId ORDER BY createdAt DESC")
    fun getDossiersByAssignee(userId: String): Flow<List<OfflineDossierEntity>>
    
    @Query("SELECT * FROM offline_dossiers WHERE marketingId = :marketingId ORDER BY createdAt DESC")
    fun getDossiersByMarketing(marketingId: String): Flow<List<OfflineDossierEntity>>
    
    @Query("SELECT * FROM offline_dossiers WHERE legalId = :legalId ORDER BY createdAt DESC")
    fun getDossiersByLegal(legalId: String): Flow<List<OfflineDossierEntity>>
    
    @Query("SELECT * FROM offline_dossiers WHERE financeId = :financeId ORDER BY createdAt DESC")
    fun getDossiersByFinance(financeId: String): Flow<List<OfflineDossierEntity>>
    
    @Query("SELECT * FROM offline_dossiers WHERE isDirty = 1")
    suspend fun getDirtyDossiers(): List<OfflineDossierEntity>>
    
    @Query("SELECT * FROM offline_dossiers WHERE lastSynced < :timestamp")
    suspend fun getDossiersNeedingSync(timestamp: Long): List<OfflineDossierEntity>>
    
    @Query("SELECT * FROM offline_dossiers WHERE documentCompletion >= :minCompletion ORDER BY documentCompletion DESC")
    fun getDossiersByCompletion(minCompletion: Int): Flow<List<OfflineDossierEntity>>
    
    @Query("SELECT * FROM offline_dossiers WHERE paymentProgress >= :minProgress ORDER BY paymentProgress DESC")
    fun getDossiersByPaymentProgress(minProgress: Double): Flow<List<OfflineDossierEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDossier(dossier: OfflineDossierEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDossiers(dossiers: List<OfflineDossierEntity>)
    
    @Update
    suspend fun updateDossier(dossier: OfflineDossierEntity)
    
    @Delete
    suspend fun deleteDossier(dossier: OfflineDossierEntity)
    
    @Query("DELETE FROM offline_dossiers WHERE id = :id")
    suspend fun deleteDossierById(id: String)
    
    @Query("UPDATE offline_dossiers SET isDirty = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDossierAsDirty(id: String, timestamp: Long)
    
    @Query("UPDATE offline_dossiers SET lastSynced = :timestamp, isDirty = 0 WHERE id = :id")
    suspend fun markDossierAsSynced(id: String, timestamp: Long)
    
    @Query("UPDATE offline_dossiers SET currentStatus = :status, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateDossierStatus(id: String, status: String, timestamp: Long)
    
    @Query("UPDATE offline_dossiers SET documentCompletion = :completion, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateDocumentCompletion(id: String, completion: Int, timestamp: Long)
    
    @Query("UPDATE offline_dossiers SET paymentProgress = :progress, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePaymentProgress(id: String, progress: Double, timestamp: Long)
    
    @Query("DELETE FROM offline_dossiers")
    suspend fun deleteAllDossiers()
    
    @Query("SELECT COUNT(*) FROM offline_dossiers")
    suspend fun getDossierCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_dossiers WHERE isDirty = 1")
    suspend fun getDirtyDossierCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_dossiers WHERE currentStatus = :status")
    suspend fun getDossierCountByStatus(status: String): Int
}
