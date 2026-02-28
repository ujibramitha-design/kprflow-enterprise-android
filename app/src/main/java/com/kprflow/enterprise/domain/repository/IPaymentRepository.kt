package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.FinancialTransaction
import com.kprflow.enterprise.data.model.PaymentSchedule
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Payment Repository
 * Following Clean Architecture principles for testability
 */
interface IPaymentRepository {
    suspend fun createPayment(payment: FinancialTransaction): Result<FinancialTransaction>
    suspend fun getPaymentsByUserId(userId: String): Result<List<FinancialTransaction>>
    suspend fun getPaymentSchedule(dossierId: String): Result<List<PaymentSchedule>>
    suspend fun updatePaymentStatus(paymentId: String, status: String): Result<FinancialTransaction>
    suspend fun processPayment(paymentId: String, amount: BigDecimal): Result<Unit>
    fun observePaymentStatus(paymentId: String): Flow<FinancialTransaction>
    suspend fun getPaymentHistory(dossierId: String): Result<List<FinancialTransaction>>
}
