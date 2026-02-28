package com.kprflow.enterprise.domain.usecase.financial

import com.kprflow.enterprise.data.model.TransactionStatus
import com.kprflow.enterprise.data.model.UserRole
import com.kprflow.enterprise.domain.repository.IFinancialRepository
import com.kprflow.enterprise.domain.repository.IAuthRepository
import javax.inject.Inject

/**
 * Use Case for Verifying Financial Transactions
 * Business Rules: Only FINANCE role can verify transactions
 */
class VerifyTransactionUseCase @Inject constructor(
    private val financialRepository: IFinancialRepository,
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(
        transactionId: String,
        verificationNotes: String? = null
    ): Result<Boolean> {
        // Get current user and validate role
        val currentUser = authRepository.getCurrentUser().getOrNull()
            ?: return Result.failure(Exception("User not authenticated"))
        
        // Validate user role - only FINANCE can verify
        if (currentUser.role != UserRole.FINANCE) {
            return Result.failure(
                SecurityException("Access Denied: Only FINANCE role can verify transactions")
            )
        }
        
        // Verify the transaction
        return financialRepository.verifyTransaction(
            transactionId = transactionId,
            verifiedBy = currentUser.id
        )
    }
}

/**
 * Use Case for Rejecting Financial Transactions
 */
class RejectTransactionUseCase @Inject constructor(
    private val financialRepository: IFinancialRepository,
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(
        transactionId: String,
        rejectionReason: String
    ): Result<Boolean> {
        // Validate rejection reason
        if (rejectionReason.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Rejection reason is required")
            )
        }
        
        if (rejectionReason.length < 5) {
            return Result.failure(
                IllegalArgumentException("Rejection reason must be at least 5 characters")
            )
        }
        
        // Get current user and validate role
        val currentUser = authRepository.getCurrentUser().getOrNull()
            ?: return Result.failure(Exception("User not authenticated"))
        
        // Validate user role - only FINANCE can reject
        if (currentUser.role != UserRole.FINANCE) {
            return Result.failure(
                SecurityException("Access Denied: Only FINANCE role can reject transactions")
            )
        }
        
        // Reject the transaction
        return financialRepository.rejectTransaction(
            transactionId = transactionId,
            rejectedBy = currentUser.id,
            rejectionReason = rejectionReason
        )
    }
}

/**
 * Use Case for Creating Financial Transactions
 */
class CreateTransactionUseCase @Inject constructor(
    private val financialRepository: IFinancialRepository,
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(
        dossierId: String,
        type: com.kprflow.enterprise.data.model.TransactionType,
        nominal: Double,
        description: String,
        paymentMethod: String? = null,
        bankReference: String? = null
    ): Result<com.kprflow.enterprise.data.model.FinancialTransaction> {
        // Validate input
        if (nominal <= 0.0) {
            return Result.failure(
                IllegalArgumentException("Transaction nominal must be greater than 0")
            )
        }
        
        if (description.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Transaction description is required")
            )
        }
        
        // Get current user and validate role
        val currentUser = authRepository.getCurrentUser().getOrNull()
            ?: return Result.failure(Exception("User not authenticated"))
        
        // Only FINANCE can create transactions
        if (currentUser.role != UserRole.FINANCE) {
            return Result.failure(
                SecurityException("Access Denied: Only FINANCE role can create transactions")
            )
        }
        
        // Validate transaction amount against business rules
        val validation = financialRepository.validateTransactionAmount(
            dossierId = dossierId,
            type = type,
            nominal = nominal
        ).getOrNull() ?: return Result.failure(Exception("Failed to validate transaction amount"))
        
        if (!validation) {
            return Result.failure(
                IllegalArgumentException("Transaction amount validation failed")
            )
        }
        
        // Create the transaction
        return financialRepository.createTransaction(
            dossierId = dossierId,
            type = type,
            nominal = nominal,
            description = description,
            paymentMethod = paymentMethod,
            bankReference = bankReference
        )
    }
}

/**
 * Use Case for Getting Financial Analytics
 */
class GetFinancialAnalyticsUseCase @Inject constructor(
    private val financialRepository: IFinancialRepository
) {
    suspend operator fun invoke(dossierId: String) = 
        financialRepository.getFinancialAnalytics(dossierId)
    
    suspend operator fun invoke() = financialRepository.getAllFinancialAnalytics()
}

/**
 * Use Case for Getting Financial Summary
 */
class GetFinancialSummaryUseCase @Inject constructor(
    private val financialRepository: IFinancialRepository
) {
    suspend operator fun invoke() = financialRepository.getFinancialSummary()
}

/**
 * Use Case for Getting Pending Transactions
 */
class GetPendingTransactionsUseCase @Inject constructor(
    private val financialRepository: IFinancialRepository
) {
    suspend operator fun invoke() = financialRepository.getPendingTransactions()
}

/**
 * Use Case for Monitoring Financial Changes
 */
class MonitorFinancialChangesUseCase @Inject constructor(
    private val financialRepository: IFinancialRepository
) {
    operator fun invoke() = financialRepository.observePendingTransactions()
}

/**
 * Use Case for Getting Transaction Statistics
 */
class GetTransactionStatisticsUseCase @Inject constructor(
    private val financialRepository: IFinancialRepository
) {
    suspend operator fun invoke() = financialRepository.getTransactionStatistics()
}

/**
 * Use Case for Validating Transaction Amount
 */
class ValidateTransactionAmountUseCase @Inject constructor(
    private val financialRepository: IFinancialRepository
) {
    suspend operator fun invoke(
        dossierId: String,
        type: com.kprflow.enterprise.data.model.TransactionType,
        nominal: Double
    ): Result<TransactionValidation> {
        val isValid = financialRepository.validateTransactionAmount(
            dossierId = dossierId,
            type = type,
            nominal = nominal
        ).getOrNull() ?: return Result.failure(Exception("Validation failed"))
        
        return Result.success(
            TransactionValidation(
                isValid = isValid,
                maxAllowed = when (type) {
                    com.kprflow.enterprise.data.model.TransactionType.BOOKING -> 0.05 // 5% max
                    com.kprflow.enterprise.data.model.TransactionType.DP -> 0.10 // 10% min
                    else -> Double.MAX_VALUE
                },
                message = if (isValid) "Transaction amount is valid" else "Transaction amount exceeds limits"
            )
        )
    }
}

/**
 * Transaction Validation Result
 */
data class TransactionValidation(
    val isValid: Boolean,
    val maxAllowed: Double,
    val message: String
)
