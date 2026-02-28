package com.kprflow.enterprise.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kprflow.enterprise.data.database.entities.OfflinePaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflinePaymentDao {
    
    @Query("SELECT * FROM offline_payments ORDER BY createdAt DESC")
    fun getAllPayments(): Flow<List<OfflinePaymentEntity>>
    
    @Query("SELECT * FROM offline_payments WHERE id = :id")
    suspend fun getPaymentById(id: String): OfflinePaymentEntity?
    
    @Query("SELECT * FROM offline_payments WHERE dossierId = :dossierId ORDER BY createdAt DESC")
    fun getPaymentsByDossier(dossierId: String): Flow<List<OfflinePaymentEntity>>
    
    @Query("SELECT * FROM offline_payments WHERE paymentType = :type ORDER BY createdAt DESC")
    fun getPaymentsByType(type: String): Flow<List<OfflinePaymentEntity>>
    
    @Query("SELECT * FROM offline_payments WHERE status = :status ORDER BY createdAt DESC")
    fun getPaymentsByStatus(status: String): Flow<List<OfflinePaymentEntity>>
    
    @Query("SELECT * FROM offline_payments WHERE paymentMethod = :method ORDER BY createdAt DESC")
    fun getPaymentsByMethod(method: String): Flow<List<OfflinePaymentEntity>>
    
    @Query("SELECT * FROM offline_payments WHERE isSynced = 0 ORDER BY createdAt DESC")
    fun getPendingSyncPayments(): Flow<List<OfflinePaymentEntity>>
    
    @Query("SELECT * FROM offline_payments WHERE isDirty = 1")
    suspend fun getDirtyPayments(): List<OfflinePaymentEntity>>
    
    @Query("SELECT * FROM offline_payments WHERE paymentDate >= :startDate AND paymentDate <= :endDate ORDER BY paymentDate DESC")
    fun getPaymentsByDateRange(startDate: Long, endDate: Long): Flow<List<OfflinePaymentEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: OfflinePaymentEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<OfflinePaymentEntity>)
    
    @Update
    suspend fun updatePayment(payment: OfflinePaymentEntity)
    
    @Delete
    suspend fun deletePayment(payment: OfflinePaymentEntity)
    
    @Query("DELETE FROM offline_payments WHERE id = :id")
    suspend fun deletePaymentById(id: String)
    
    @Query("DELETE FROM offline_payments WHERE dossierId = :dossierId")
    suspend fun deletePaymentsByDossier(dossierId: String)
    
    @Query("UPDATE offline_payments SET isDirty = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markPaymentAsDirty(id: String, timestamp: Long)
    
    @Query("UPDATE offline_payments SET isSynced = 1, lastSynced = :timestamp WHERE id = :id")
    suspend fun markPaymentAsSynced(id: String, timestamp: Long)
    
    @Query("UPDATE offline_payments SET status = :status, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePaymentStatus(id: String, status: String, timestamp: Long)
    
    @Query("UPDATE offline_payments SET receiptNumber = :receiptNumber, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateReceiptNumber(id: String, receiptNumber: String, timestamp: Long)
    
    @Query("DELETE FROM offline_payments")
    suspend fun deleteAllPayments()
    
    @Query("SELECT COUNT(*) FROM offline_payments")
    suspend fun getPaymentCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_payments WHERE dossierId = :dossierId")
    suspend fun getPaymentCountByDossier(dossierId: String): Int
    
    @Query("SELECT COUNT(*) FROM offline_payments WHERE isSynced = 0")
    suspend fun getPendingSyncCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_payments WHERE isDirty = 1")
    suspend fun getDirtyPaymentCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_payments WHERE paymentType = :type")
    suspend fun getPaymentCountByType(type: String): Int
    
    @Query("SELECT SUM(amount) FROM offline_payments WHERE status = 'COMPLETED'")
    suspend fun getTotalPaidAmount(): Double?
    
    @Query("SELECT SUM(amount) FROM offline_payments WHERE dossierId = :dossierId AND status = 'COMPLETED'")
    suspend fun getDossierPaidAmount(dossierId: String): Double?
    
    @Query("SELECT SUM(amount) FROM offline_payments WHERE paymentDate >= :startDate AND paymentDate <= :endDate AND status = 'COMPLETED'")
    suspend fun getTotalRevenueByDateRange(startDate: Long, endDate: Long): Double?
}
