package com.kprflow.enterprise.data.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Financial Transaction Data Model
 * Represents atomic financial transactions
 */
@Serializable
data class FinancialTransaction(
    val id: String,
    val dossierId: String,
    val type: TransactionType,
    val nominal: BigDecimal,
    val status: TransactionStatus,
    val description: String? = null,
    val verifiedAt: String? = null,
    val verifiedBy: String? = null,
    val rejectedAt: String? = null,
    val rejectedBy: String? = null,
    val rejectionReason: String? = null,
    val paymentMethod: String? = null,
    val bankReference: String? = null,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Transaction Type Enum
 */
@Serializable
enum class TransactionType {
    BOOKING,        // Booking fee payment
    DP,             // Down payment
    DISBURSEMENT,   // KPR disbursement from bank
    REFUND,         // Refund payment
    PENALTY,        // Penalty charges
    BONUS           // Bonus or incentive
}

/**
 * Transaction Status Enum
 */
@Serializable
enum class TransactionStatus {
    PENDING,        // Waiting for verification
    VERIFIED,       // Verified by Finance team
    REJECTED,       // Rejected by Finance team
    PROCESSING      // Being processed
}

/**
 * Financial Analytics Data Model
 */
@Serializable
data class FinancialAnalytics(
    val dossierId: String,
    val customerName: String,
    val customerEmail: String? = null,
    val dossierStatus: String,
    val bookingDate: String,
    val unitId: String? = null,
    val unitType: String? = null,
    val unitBlock: String? = null,
    val unitNumber: String? = null,
    val unitPrice: BigDecimal? = null,
    
    // Cash metrics
    val realizedCash: BigDecimal,
    val projectedCash: BigDecimal,
    val pendingCash: BigDecimal,
    
    // Transaction breakdown
    val bookingCash: BigDecimal,
    val dpCash: BigDecimal,
    val disbursementCash: BigDecimal,
    
    // Transaction counts
    val verifiedTransactions: Int,
    val pendingTransactions: Int,
    
    // Metadata
    val lastTransactionDate: String? = null,
    val financialHealth: FinancialHealth,
    val paymentCompletionPercentage: Double
)

/**
 * Financial Health Enum
 */
@Serializable
enum class FinancialHealth {
    HEALTHY,     // Good payment progress
    MODERATE,    // Moderate payment progress
    AT_RISK      // Poor payment progress
}

/**
 * Financial Summary Data Model
 */
@Serializable
data class FinancialSummary(
    val totalDossiers: Int,
    val dossiersWithPayments: Int,
    
    // Cash metrics
    val totalRealizedCash: BigDecimal,
    val totalProjectedCash: BigDecimal,
    val totalPendingCash: BigDecimal,
    
    // Transaction metrics
    val totalBookingCash: BigDecimal,
    val totalDpCash: BigDecimal,
    val totalDisbursementCash: BigDecimal,
    
    // Health metrics
    val healthyDossiers: Int,
    val moderateDossiers: Int,
    val atRiskDossiers: Int,
    
    // Average metrics
    val avgRealizedCash: BigDecimal,
    val avgCompletionPercentage: Double,
    
    // Status breakdown
    val leadCount: Int,
    val documentCount: Int,
    val bankCount: Int,
    val approvedCount: Int,
    val sp3kCount: Int,
    val disbursedCount: Int,
    
    // Date metrics
    val reportDate: String,
    val currentMonth: Int,
    val currentYear: Int
)

/**
 * Transaction Form Data
 */
@Serializable
data class TransactionForm(
    val dossierId: String,
    val customerName: String,
    val type: TransactionType,
    val nominal: BigDecimal,
    val description: String = "",
    val paymentMethod: String = "",
    val bankReference: String = ""
) {
    fun isValid(): Boolean {
        return nominal > BigDecimal.ZERO &&
               description.isNotBlank() &&
               paymentMethod.isNotBlank()
    }
    
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (nominal <= BigDecimal.ZERO) {
            errors.add("Nominal must be greater than 0")
        }
        
        if (description.isBlank()) {
            errors.add("Description is required")
        }
        
        if (paymentMethod.isBlank()) {
            errors.add("Payment method is required")
        }
        
        return errors
    }
    
    fun getFormattedNominal(): String {
        return "Rp ${String.format("%,.0f", nominal)}"
    }
}

/**
 * Transaction Verification Data
 */
@Serializable
data class TransactionVerification(
    val transactionId: String,
    val verifiedBy: String,
    val verificationNotes: String? = null,
    val verifiedAt: String = java.time.Instant.now().toString()
)

/**
 * Transaction Rejection Data
 */
@Serializable
data class TransactionRejection(
    val transactionId: String,
    val rejectedBy: String,
    val rejectionReason: String,
    val rejectedAt: String = java.time.Instant.now().toString()
) {
    fun isValid(): Boolean {
        return rejectionReason.isNotBlank() && rejectionReason.length >= 5
    }
}

/**
 * Financial Metrics for Dashboard
 */
@Serializable
data class FinancialMetrics(
    val totalRevenue: BigDecimal,
    val pendingRevenue: BigDecimal,
    val projectedRevenue: BigDecimal,
    val revenueGrowth: Double,
    val transactionVolume: Int,
    val averageTransactionValue: BigDecimal,
    val completionRate: Double,
    val healthScore: Double
)

/**
 * Cash Flow Data
 */
@Serializable
data class CashFlow(
    val period: String,
    val inflow: BigDecimal,
    val outflow: BigDecimal,
    val netFlow: BigDecimal,
    val projectedInflow: BigDecimal,
    val projectedOutflow: BigDecimal
)
