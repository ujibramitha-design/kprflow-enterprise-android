package com.kprflow.enterprise.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kprflow.enterprise.data.database.entities.OfflineUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineUnitDao {
    
    @Query("SELECT * FROM offline_units ORDER BY createdAt DESC")
    fun getAllUnits(): Flow<List<OfflineUnitEntity>>
    
    @Query("SELECT * FROM offline_units WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getUnitsByProject(projectId: String): Flow<List<OfflineUnitEntity>>
    
    @Query("SELECT * FROM offline_units WHERE id = :id")
    suspend fun getUnitById(id: String): OfflineUnitEntity?
    
    @Query("SELECT * FROM offline_units WHERE status = :status ORDER BY createdAt DESC")
    fun getUnitsByStatus(status: String): Flow<List<OfflineUnitEntity>>
    
    @Query("SELECT * FROM offline_units WHERE blockName = :blockName ORDER BY unitNumber")
    fun getUnitsByBlock(blockName: String): Flow<List<OfflineUnitEntity>>
    
    @Query("SELECT * FROM offline_units WHERE isDirty = 1")
    suspend fun getDirtyUnits(): List<OfflineUnitEntity>
    
    @Query("SELECT * FROM offline_units WHERE lastSynced < :timestamp")
    suspend fun getUnitsNeedingSync(timestamp: Long): List<OfflineUnitEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: OfflineUnitEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<OfflineUnitEntity>)
    
    @Update
    suspend fun updateUnit(unit: OfflineUnitEntity)
    
    @Delete
    suspend fun deleteUnit(unit: OfflineUnitEntity)
    
    @Query("DELETE FROM offline_units WHERE id = :id")
    suspend fun deleteUnitById(id: String)
    
    @Query("UPDATE offline_units SET isDirty = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markUnitAsDirty(id: String, timestamp: Long)
    
    @Query("UPDATE offline_units SET lastSynced = :timestamp, isDirty = 0 WHERE id = :id")
    suspend fun markUnitAsSynced(id: String, timestamp: Long)
    
    @Query("DELETE FROM offline_units")
    suspend fun deleteAllUnits()
    
    @Query("SELECT COUNT(*) FROM offline_units")
    suspend fun getUnitCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_units WHERE isDirty = 1")
    suspend fun getDirtyUnitCount(): Int
}
