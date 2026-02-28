package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.FinancialTransaction
import com.kprflow.enterprise.data.model.FinancialAnalytics
import com.kprflow.enterprise.data.model.FinancialSummary
import com.kprflow.enterprise.data.model.TransactionType
import com.kprflow.enterprise.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Financial Repository
 * Handles atomic financial transactions and analytics
 */
interface IFinancialRepository {
    // Transaction Operations
    suspend fun createTransaction(
        dossierId: String,
        type: TransactionType,
        nominal: Double,
        description: String,
        paymentMethod: String? = null,
        bankReference: String? = null
    ): Result<FinancialTransaction>
    
    suspend fun verifyTransaction(
        transactionId: String,
        verifiedBy: String
    ): Result<Boolean>
    
    suspend fun rejectTransaction(
        transactionId: String,
        rejectedBy: String,
        rejectionReason: String
    ): Result<Boolean>
    
    // Query Operations
    suspend fun getTransactionById(transactionId: String): Result<FinancialTransaction>
    suspend fun getTransactionsByDossier(dossierId: String): Result<List<FinancialTransaction>>
    suspend fun getPendingTransactions(): Result<List<FinancialTransaction>>
    suspend fun getVerifiedTransactions(): Result<List<FinancialTransaction>>
    
    // Analytics Operations
    suspend fun getFinancialAnalytics(dossierId: String): Result<FinancialAnalytics>
    suspend fun getAllFinancialAnalytics(): Result<List<FinancialAnalytics>>
    suspend fun getFinancialSummary(): Result<FinancialSummary>
    suspend fun getMonthlyTrends(): Result<List<FinancialMonthlyTrend>>
    
    // Validation Operations
    suspend fun validateTransactionAmount(
        dossierId: String,
        type: TransactionType,
        nominal: Double
    ): Result<Boolean>
    
    // Real-time Operations
    fun observePendingTransactions(): Flow<List<FinancialTransaction>>
    fun observeDossierTransactions(dossierId: String): Flow<List<FinancialTransaction>>
    fun observeFinancialSummary(): Flow<FinancialSummary>
    
    // Statistics
    suspend fun getTransactionStatistics(): Result<TransactionStatistics>
}

// Data classes for financial analytics
data class FinancialMonthlyTrend(
    val month: String,
    val newDossiers: Int,
    val monthlyRealizedCash: Double,
    val monthlyProjectedCash: Double,
    val monthlyBookingCash: Double,
    val monthlyDpCash: Double,
    val monthlyDisbursementCash: Double,
    val avgCompletionPercentage: Double,
    val healthyCount: Int,
    val atRiskCount: Int
)

data class TransactionStatistics(
    val totalTransactions: Int,
    val pendingTransactions: Int,
    val verifiedTransactions: Int,
    val rejectedTransactions: Int,
    val totalVolume: Double,
    val averageTransactionAmount: Double,
    val transactionsByType: Map<TransactionType, Int>,
    val transactionsByStatus: Map<TransactionStatus, Int>
)
