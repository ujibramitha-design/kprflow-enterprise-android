package com.kprflow.enterprise.finance

import android.content.Context
import com.kprflow.enterprise.data.model.*
import com.kprflow.enterprise.domain.repository.FinancialRepository
import com.kprflow.enterprise.domain.repository.KprRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Advanced Financial Manager with comprehensive reporting and payment gateway integration
 */
class AdvancedFinancialManager(
    private val context: Context,
    private val financialRepository: FinancialRepository,
    private val kprRepository: KprRepository
) {
    
    private val currencyFormat = SimpleDateFormat("Rp #,##0", Locale("id", "ID"))
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    private val taxCalculator = TaxCalculator()
    private val fraudDetector = FraudDetector()
    
    companion object {
        private const val TAX_RATE = 0.11 // 11% PPN
        private const val FRAUD_THRESHOLD = 0.7 // 70% fraud probability threshold
    }
    
    /**
     * Generate comprehensive financial report
     */
    suspend fun generateAdvancedFinancialReport(
        startDate: Date,
        endDate: Date,
        reportType: FinancialReportType = FinancialReportType.COMPREHENSIVE
    ): Result<FinancialReport> = withContext(Dispatchers.IO) {
        
        try {
            // Collect financial data
            val transactions = financialRepository.getTransactionsByDateRange(startDate, endDate)
                .getOrNull() ?: emptyList()
            
            val revenueMetrics = calculateRevenueMetrics(transactions)
            val taxMetrics = calculateTaxMetrics(transactions)
            val paymentMetrics = calculatePaymentMetrics(transactions)
            val fraudAnalysis = performFraudAnalysis(transactions)
            
            val report = when (reportType) {
                FinancialReportType.COMPREHENSIVE -> generateComprehensiveReport(
                    revenueMetrics, taxMetrics, paymentMetrics, fraudAnalysis
                )
                FinancialReportType.REVENUE -> generateRevenueReport(revenueMetrics)
                FinancialReportType.TAX -> generateTaxReport(taxMetrics)
                FinancialReportType.PAYMENT -> generatePaymentReport(paymentMetrics)
                FinancialReportType.FRAUD -> generateFraudReport(fraudAnalysis)
            }
            
            Result.success(report)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Process payment with multiple gateway support
     */
    suspend fun processPayment(
        paymentRequest: PaymentRequest,
        gateway: PaymentGateway = PaymentGateway.DUMMY
    ): Result<PaymentResult> = withContext(Dispatchers.IO) {
        
        try {
            // Validate payment request
            val validationResult = validatePaymentRequest(paymentRequest)
            if (validationResult.isFailure) {
                return Result.failure(validationResult.exceptionOrNull()!!)
            }
            
            // Check for fraud
            val fraudScore = fraudDetector.calculateFraudScore(paymentRequest)
            if (fraudScore > FRAUD_THRESHOLD) {
                return Result.failure(Exception("Payment flagged for potential fraud"))
            }
            
            // Process payment via selected gateway
            val result = when (gateway) {
                PaymentGateway.DUMMY -> processDummyPayment(paymentRequest)
                PaymentGateway.MIDTRANS -> processMidtransPayment(paymentRequest)
                PaymentGateway.XENDIT -> processXenditPayment(paymentRequest)
                PaymentGateway.DOKU -> processDokuPayment(paymentRequest)
            }
            
            // Record transaction
            if (result.isSuccess) {
                val transaction = createTransaction(paymentRequest, result.getOrNull()!!)
                financialRepository.saveTransaction(transaction)
            }
            
            result
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate revenue metrics
     */
    private fun calculateRevenueMetrics(transactions: List<FinancialTransaction>): RevenueMetrics {
        val successfulTransactions = transactions.filter { it.status == TransactionStatus.VERIFIED }
        
        val totalRevenue = successfulTransactions.sumOf { it.amount }
        val revenueByCategory = successfulTransactions
            .groupBy { it.category }
            .mapValues { it.value.sumOf { transaction -> transaction.amount } }
        
        val revenueByMonth = successfulTransactions
            .groupBy { 
                val calendar = Calendar.getInstance()
                calendar.time = it.createdAt
                "${calendar.get(Calendar.YEAR)}-${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}"
            }
            .mapValues { it.value.sumOf { transaction -> transaction.amount } }
        
        val avgTransactionAmount = if (successfulTransactions.isNotEmpty()) {
            totalRevenue / successfulTransactions.size
        } else {
            0.0
        }
        
        val growthRate = calculateGrowthRate(successfulTransactions)
        
        return RevenueMetrics(
            totalRevenue = totalRevenue,
            netRevenue = totalRevenue * 0.88, // After tax
            grossRevenue = totalRevenue,
            revenueByCategory = revenueByCategory,
            revenueByMonth = revenueByMonth,
            avgTransactionAmount = avgTransactionAmount,
            growthRate = growthRate,
            projectedRevenue = calculateProjectedRevenue(revenueByMonth)
        )
    }
    
    /**
     * Calculate tax metrics
     */
    private fun calculateTaxMetrics(transactions: List<FinancialTransaction>): TaxMetrics {
        val taxableTransactions = transactions.filter { it.status == TransactionStatus.VERIFIED }
        
        val totalTaxableAmount = taxableTransactions.sumOf { it.amount }
        val totalTax = totalTaxableAmount * TAX_RATE
        val taxByCategory = taxableTransactions
            .groupBy { it.category }
            .mapValues { (_, transactions) ->
                transactions.sumOf { it.amount } * TAX_RATE
            }
        
        val taxWithholdings = calculateTaxWithholdings(taxableTransactions)
        val taxRefunds = calculateTaxRefunds(taxableTransactions)
        
        return TaxMetrics(
            totalTaxableAmount = totalTaxableAmount,
            totalTax = totalTax,
            taxRate = TAX_RATE,
            taxByCategory = taxByCategory,
            taxWithholdings = taxWithholdings,
            taxRefunds = taxRefunds,
            netTax = totalTax - taxWithholdings + taxRefunds,
            taxComplianceRate = calculateTaxComplianceRate(taxableTransactions)
        )
    }
    
    /**
     * Calculate payment metrics
     */
    private fun calculatePaymentMetrics(transactions: List<FinancialTransaction>): PaymentMetrics {
        val paymentTransactions = transactions.filter { it.category in PaymentCategory.values().map { it.name } }
        
        val paymentsByGateway = paymentTransactions
            .groupBy { it.gateway }
            .mapValues { it.value.size }
        
        val paymentsByStatus = paymentTransactions
            .groupBy { it.status }
            .mapValues { it.value.size }
        
        val avgProcessingTime = calculateAvgProcessingTime(paymentTransactions)
        val successRate = paymentTransactions.count { it.status == TransactionStatus.VERIFIED }
            .toDouble() / paymentTransactions.size
        
        val paymentFailureAnalysis = analyzePaymentFailures(paymentTransactions)
        
        return PaymentMetrics(
            totalPayments = paymentTransactions.size,
            successfulPayments = paymentsByStatus[TransactionStatus.VERIFIED] ?: 0,
            failedPayments = paymentsByStatus[TransactionStatus.REJECTED] ?: 0,
            paymentsByGateway = paymentsByGateway,
            paymentsByStatus = paymentsByStatus,
            avgProcessingTime = avgProcessingTime,
            successRate = successRate,
            paymentFailureAnalysis = paymentFailureAnalysis
        )
    }
    
    /**
     * Perform fraud analysis
     */
    private fun performFraudAnalysis(transactions: List<FinancialTransaction>): FraudAnalysis {
        val suspiciousTransactions = transactions.filter { transaction ->
            fraudDetector.calculateFraudScore(
                PaymentRequest(
                    customerId = transaction.customerId,
                    amount = transaction.amount,
                    paymentMethod = transaction.paymentMethod,
                    ipAddress = transaction.ipAddress ?: "",
                    deviceId = transaction.deviceId ?: ""
                )
            ) > FRAUD_THRESHOLD
        }
        
        val fraudByType = suspiciousTransactions
            .groupBy { 
                when {
                    it.amount > 100_000_000 -> "HIGH_AMOUNT"
                    it.createdAt.time < System.currentTimeMillis() - 24 * 60 * 60 * 1000 -> "RAPID_TRANSACTION"
                    else -> "SUSPICIOUS_PATTERN"
                }
            }
            .mapValues { it.value.size }
        
        val fraudTrends = analyzeFraudTrends(suspiciousTransactions)
        
        return FraudAnalysis(
            totalTransactions = transactions.size,
            suspiciousTransactions = suspiciousTransactions.size,
            fraudRate = suspiciousTransactions.size.toDouble() / transactions.size,
            fraudByType = fraudByType,
            fraudTrends = fraudTrends,
            highRiskTransactions = suspiciousTransactions.take(10),
            fraudPreventionRecommendations = generateFraudPreventionRecommendations(fraudByType)
        )
    }
    
    /**
     * Process dummy payment for testing
     */
    private suspend fun processDummyPayment(paymentRequest: PaymentRequest): Result<PaymentResult> {
        return try {
            // Simulate processing delay
            kotlinx.coroutines.delay(1000)
            
            // Simulate success
            val paymentResult = PaymentResult(
                success = true,
                transactionId = UUID.randomUUID().toString(),
                paymentId = UUID.randomUUID().toString(),
                amount = paymentRequest.amount,
                currency = "IDR",
                status = PaymentStatus.SUCCESS,
                gateway = PaymentGateway.DUMMY,
                processedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000, // 24 hours
                paymentUrl = "https://dummy.payment.kprflow.com/pay/${UUID.randomUUID()}",
                qrCode = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
                fraudScore = 0.1 // Low fraud score
            )
            
            Result.success(paymentResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Process Midtrans payment
     */
    private suspend fun processMidtransPayment(paymentRequest: PaymentRequest): Result<PaymentResult> {
        return try {
            // Simulate Midtrans API call
            kotlinx.coroutines.delay(2000)
            
            val paymentResult = PaymentResult(
                success = true,
                transactionId = "MT-${UUID.randomUUID()}",
                paymentId = UUID.randomUUID().toString(),
                amount = paymentRequest.amount,
                currency = "IDR",
                status = PaymentStatus.PENDING,
                gateway = PaymentGateway.MIDTRANS,
                processedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000,
                paymentUrl = "https://api.midtrans.com/v2/pay/${UUID.randomUUID()}",
                qrCode = null,
                fraudScore = fraudDetector.calculateFraudScore(paymentRequest)
            )
            
            Result.success(paymentResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Process Xendit payment
     */
    private suspend fun processXenditPayment(paymentRequest: PaymentRequest): Result<PaymentResult> {
        return try {
            // Simulate Xendit API call
            kotlinx.coroutines.delay(1500)
            
            val paymentResult = PaymentResult(
                success = true,
                transactionId = "XD-${UUID.randomUUID()}",
                paymentId = UUID.randomUUID().toString(),
                amount = paymentRequest.amount,
                currency = "IDR",
                status = PaymentStatus.SUCCESS,
                gateway = PaymentGateway.XENDIT,
                processedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000,
                paymentUrl = "https://checkout.xendit.co/${UUID.randomUUID()}",
                qrCode = null,
                fraudScore = fraudDetector.calculateFraudScore(paymentRequest)
            )
            
            Result.success(paymentResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Process Doku payment
     */
    private suspend fun processDokuPayment(paymentRequest: PaymentRequest): Result<PaymentResult> {
        return try {
            // Simulate Doku API call
            kotlinx.coroutines.delay(1800)
            
            val paymentResult = PaymentResult(
                success = true,
                transactionId = "DK-${UUID.randomUUID()}",
                paymentId = UUID.randomUUID().toString(),
                amount = paymentRequest.amount,
                currency = "IDR",
                status = PaymentStatus.PENDING,
                gateway = PaymentGateway.DOKU,
                processedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000,
                paymentUrl = "https://doku.com/payment/${UUID.randomUUID()}",
                qrCode = null,
                fraudScore = fraudDetector.calculateFraudScore(paymentRequest)
            )
            
            Result.success(paymentResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validate payment request
     */
    private fun validatePaymentRequest(paymentRequest: PaymentRequest): Result<Unit> {
        if (paymentRequest.amount <= 0) {
            return Result.failure(Exception("Amount must be greater than 0"))
        }
        
        if (paymentRequest.customerId.isBlank()) {
            return Result.failure(Exception("Customer ID is required"))
        }
        
        if (paymentRequest.paymentMethod.isBlank()) {
            return Result.failure(Exception("Payment method is required"))
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Create transaction record
     */
    private fun createTransaction(
        paymentRequest: PaymentRequest,
        paymentResult: PaymentResult
    ): FinancialTransaction {
        return FinancialTransaction(
            id = UUID.randomUUID().toString(),
            customerId = paymentRequest.customerId,
            dossierId = paymentRequest.dossierId,
            amount = paymentResult.amount,
            category = determineTransactionCategory(paymentRequest),
            paymentMethod = paymentRequest.paymentMethod,
            status = when (paymentResult.status) {
                PaymentStatus.SUCCESS -> TransactionStatus.VERIFIED
                PaymentStatus.PENDING -> TransactionStatus.PENDING
                PaymentStatus.FAILED -> TransactionStatus.REJECTED
                else -> TransactionStatus.PENDING
            },
            gateway = paymentResult.gateway,
            transactionId = paymentResult.transactionId,
            paymentId = paymentResult.paymentId,
            evidenceUrl = paymentResult.paymentUrl,
            createdAt = paymentResult.processedAt,
            updatedAt = paymentResult.processedAt,
            ipAddress = paymentRequest.ipAddress,
            deviceId = paymentRequest.deviceId,
            fraudScore = paymentResult.fraudScore
        )
    }
    
    /**
     * Determine transaction category
     */
    private fun determineTransactionCategory(paymentRequest: PaymentRequest): String {
        return when (paymentRequest.paymentType) {
            PaymentType.BOOKING_FEE -> "BOOKING_FEE"
            PaymentType.DP_1 -> "DP_1"
            PaymentType.DP_2 -> "DP_2"
            PaymentType.DP_PELUNASAN -> "DP_PELUNASAN"
            PaymentType.BIAYA_STRATEGIS -> "BIAYA_STRATEGIS"
            PaymentType.BIAYA_ADMIN -> "BIAYA_ADMIN"
            PaymentType.BIAYA_NOTARIS -> "BIAYA_NOTARIS"
            PaymentType.BIAYA_ASURANSI -> "BIAYA_ASURANSI"
        }
    }
    
    // Helper methods for calculations
    private fun calculateGrowthRate(transactions: List<FinancialTransaction>): Double {
        if (transactions.size < 2) return 0.0
        
        val sortedTransactions = transactions.sortedBy { it.createdAt }
        val firstHalf = sortedTransactions.take(sortedTransactions.size / 2)
        val secondHalf = sortedTransactions.drop(sortedTransactions.size / 2)
        
        val firstHalfRevenue = firstHalf.sumOf { it.amount }
        val secondHalfRevenue = secondHalf.sumOf { it.amount }
        
        return if (firstHalfRevenue > 0) {
            ((secondHalfRevenue - firstHalfRevenue) / firstHalfRevenue) * 100
        } else {
            0.0
        }
    }
    
    private fun calculateProjectedRevenue(revenueByMonth: Map<String, Double>): Double {
        if (revenueByMonth.isEmpty()) return 0.0
        
        val recentMonths = revenueByMonth.values.takeLast(3)
        if (recentMonths.size < 2) return 0.0
        
        val avgGrowth = recentMonths.zipWithNext { a, b -> (b - a) / a }.average()
        val lastMonthRevenue = recentMonths.last()
        
        return lastMonthRevenue * (1 + avgGrowth)
    }
    
    private fun calculateTaxWithholdings(transactions: List<FinancialTransaction>): Double {
        // Simplified tax withholding calculation
        return transactions.sumOf { transaction ->
            when (transaction.category) {
                "DP_1", "DP_2", "DP_PELUNASAN" -> transaction.amount * 0.02 // 2% withholding
                else -> 0.0
            }
        }
    }
    
    private fun calculateTaxRefunds(transactions: List<FinancialTransaction>): Double {
        // Simplified tax refund calculation
        return 0.0 // No refunds in this simplified implementation
    }
    
    private fun calculateTaxComplianceRate(transactions: List<FinancialTransaction>): Double {
        val compliantTransactions = transactions.filter { transaction ->
            // Check if transaction has proper tax documentation
            transaction.evidenceUrl != null
        }
        
        return if (transactions.isNotEmpty()) {
            compliantTransactions.size.toDouble() / transactions.size
        } else {
            0.0
        }
    }
    
    private fun calculateAvgProcessingTime(transactions: List<FinancialTransaction>): Double {
        val processedTransactions = transactions.filter { it.status == TransactionStatus.VERIFIED }
        
        if (processedTransactions.isEmpty()) return 0.0
        
        val totalProcessingTime = processedTransactions.sumOf { transaction ->
            transaction.updatedAt - transaction.createdAt
        }
        
        return totalProcessingTime.toDouble() / processedTransactions.size / (1000 * 60) // Convert to minutes
    }
    
    private fun analyzePaymentFailures(transactions: List<FinancialTransaction>): PaymentFailureAnalysis {
        val failedTransactions = transactions.filter { it.status == TransactionStatus.REJECTED }
        
        val failuresByGateway = failedTransactions.groupBy { it.gateway }.mapValues { it.value.size }
        val failuresByReason = failedTransactions.groupBy { 
            when {
                it.fraudScore > FRAUD_THRESHOLD -> "FRAUD_DETECTED"
                it.amount > 100_000_000 -> "AMOUNT_EXCEEDED"
                else -> "TECHNICAL_ERROR"
            }
        }.mapValues { it.value.size }
        
        return PaymentFailureAnalysis(
            totalFailures = failedTransactions.size,
            failuresByGateway = failuresByGateway,
            failuresByReason = failuresByReason,
            mostCommonFailureReason = failuresByReason.maxByOrNull { it.value }?.key,
            recommendations = generateFailureRecommendations(failuresByReason)
        )
    }
    
    private fun analyzeFraudTrends(suspiciousTransactions: List<FinancialTransaction>): Map<String, Double> {
        return suspiciousTransactions
            .groupBy { 
                val calendar = Calendar.getInstance()
                calendar.time = it.createdAt
                "${calendar.get(Calendar.YEAR)}-${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}"
            }
            .mapValues { it.value.size.toDouble() }
    }
    
    private fun generateFraudPreventionRecommendations(fraudByType: Map<String, Int>): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (fraudByType["HIGH_AMOUNT"] ?: 0 > 5) {
            recommendations.add("Implement additional verification for high-value transactions")
        }
        
        if (fraudByType["RAPID_TRANSACTION"] ?: 0 > 3) {
            recommendations.add("Add rate limiting for rapid transactions")
        }
        
        if (fraudByType["SUSPICIOUS_PATTERN"] ?: 0 > 10) {
            recommendations.add("Enhance fraud detection algorithms")
        }
        
        return recommendations
    }
    
    private fun generateFailureRecommendations(failuresByReason: Map<String, Int>): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (failuresByReason["FRAUD_DETECTED"] ?: 0 > 0) {
            recommendations.add("Review fraud detection thresholds")
        }
        
        if (failuresByReason["AMOUNT_EXCEEDED"] ?: 0 > 0) {
            recommendations.add("Implement amount limits and warnings")
        }
        
        if (failuresByReason["TECHNICAL_ERROR"] ?: 0 > 0) {
            recommendations.add("Improve gateway integration and error handling")
        }
        
        return recommendations
    }
    
    // Report generation methods
    private fun generateComprehensiveReport(
        revenueMetrics: RevenueMetrics,
        taxMetrics: TaxMetrics,
        paymentMetrics: PaymentMetrics,
        fraudAnalysis: FraudAnalysis
    ): FinancialReport {
        return FinancialReport(
            reportType = FinancialReportType.COMPREHENSIVE,
            generatedAt = Date(),
            period = ReportPeriod(Date(), Date()),
            revenueMetrics = revenueMetrics,
            taxMetrics = taxMetrics,
            paymentMetrics = paymentMetrics,
            fraudAnalysis = fraudAnalysis,
            recommendations = generateFinancialRecommendations(revenueMetrics, taxMetrics, paymentMetrics, fraudAnalysis)
        )
    }
    
    private fun generateRevenueReport(revenueMetrics: RevenueMetrics): FinancialReport {
        return FinancialReport(
            reportType = FinancialReportType.REVENUE,
            generatedAt = Date(),
            period = ReportPeriod(Date(), Date()),
            revenueMetrics = revenueMetrics,
            taxMetrics = TaxMetrics(0.0, 0.0, 0.0, emptyMap(), 0.0, 0.0, 0.0, 0.0),
            paymentMetrics = PaymentMetrics(0, 0, 0, emptyMap(), emptyMap(), 0.0, 0.0, PaymentFailureAnalysis(0, emptyMap(), emptyMap(), null, emptyList())),
            fraudAnalysis = FraudAnalysis(0, 0, 0.0, emptyMap(), emptyMap(), emptyList(), emptyList()),
            recommendations = emptyList()
        )
    }
    
    private fun generateTaxReport(taxMetrics: TaxMetrics): FinancialReport {
        return FinancialReport(
            reportType = FinancialReportType.TAX,
            generatedAt = Date(),
            period = ReportPeriod(Date(), Date()),
            revenueMetrics = RevenueMetrics(0.0, 0.0, 0.0, emptyMap(), emptyMap(), 0.0, 0.0, 0.0),
            taxMetrics = taxMetrics,
            paymentMetrics = PaymentMetrics(0, 0, 0, emptyMap(), emptyMap(), 0.0, 0.0, PaymentFailureAnalysis(0, emptyMap(), emptyMap(), null, emptyList())),
            fraudAnalysis = FraudAnalysis(0, 0, 0.0, emptyMap(), emptyMap(), emptyList(), emptyList()),
            recommendations = emptyList()
        )
    }
    
    private fun generatePaymentReport(paymentMetrics: PaymentMetrics): FinancialReport {
        return FinancialReport(
            reportType = FinancialReportType.PAYMENT,
            generatedAt = Date(),
            period = ReportPeriod(Date(), Date()),
            revenueMetrics = RevenueMetrics(0.0, 0.0, 0.0, emptyMap(), emptyMap(), 0.0, 0.0, 0.0),
            taxMetrics = TaxMetrics(0.0, 0.0, 0.0, emptyMap(), 0.0, 0.0, 0.0, 0.0),
            paymentMetrics = paymentMetrics,
            fraudAnalysis = FraudAnalysis(0, 0, 0.0, emptyMap(), emptyMap(), emptyList(), emptyList()),
            recommendations = emptyList()
        )
    }
    
    private fun generateFraudReport(fraudAnalysis: FraudAnalysis): FinancialReport {
        return FinancialReport(
            reportType = FinancialReportType.FRAUD,
            generatedAt = Date(),
            period = ReportPeriod(Date(), Date()),
            revenueMetrics = RevenueMetrics(0.0, 0.0, 0.0, emptyMap(), emptyMap(), 0.0, 0.0, 0.0),
            taxMetrics = TaxMetrics(0.0, 0.0, 0.0, emptyMap(), 0.0, 0.0, 0.0, 0.0),
            paymentMetrics = PaymentMetrics(0, 0, 0, emptyMap(), emptyMap(), 0.0, 0.0, PaymentFailureAnalysis(0, emptyMap(), emptyMap(), null, emptyList())),
            fraudAnalysis = fraudAnalysis,
            recommendations = fraudAnalysis.fraudPreventionRecommendations
        )
    }
    
    private fun generateFinancialRecommendations(
        revenueMetrics: RevenueMetrics,
        taxMetrics: TaxMetrics,
        paymentMetrics: PaymentMetrics,
        fraudAnalysis: FraudAnalysis
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (revenueMetrics.growthRate < 0) {
            recommendations.add("Revenue declining - implement marketing strategies")
        }
        
        if (paymentMetrics.successRate < 0.9) {
            recommendations.add("Payment success rate below 90% - review gateway performance")
        }
        
        if (fraudAnalysis.fraudRate > 0.05) {
            recommendations.add("High fraud rate detected - enhance security measures")
        }
        
        return recommendations
    }
}

// Supporting classes
class TaxCalculator {
    fun calculateTax(amount: Double, rate: Double = 0.11): Double {
        return amount * rate
    }
}

class FraudDetector {
    fun calculateFraudScore(paymentRequest: PaymentRequest): Double {
        var fraudScore = 0.0
        
        // Amount-based scoring
        if (paymentRequest.amount > 100_000_000) fraudScore += 0.3
        if (paymentRequest.amount > 500_000_000) fraudScore += 0.4
        
        // Time-based scoring
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour < 6 || currentHour > 22) fraudScore += 0.2
        
        // Device-based scoring
        if (paymentRequest.deviceId.isBlank()) fraudScore += 0.1
        
        return minOf(1.0, fraudScore)
    }
}

// Data classes
data class FinancialReport(
    val reportType: FinancialReportType,
    val generatedAt: Date,
    val period: ReportPeriod,
    val revenueMetrics: RevenueMetrics,
    val taxMetrics: TaxMetrics,
    val paymentMetrics: PaymentMetrics,
    val fraudAnalysis: FraudAnalysis,
    val recommendations: List<String>
)

data class RevenueMetrics(
    val totalRevenue: Double,
    val netRevenue: Double,
    val grossRevenue: Double,
    val revenueByCategory: Map<String, Double>,
    val revenueByMonth: Map<String, Double>,
    val avgTransactionAmount: Double,
    val growthRate: Double,
    val projectedRevenue: Double
)

data class TaxMetrics(
    val totalTaxableAmount: Double,
    val totalTax: Double,
    val taxRate: Double,
    val taxByCategory: Map<String, Double>,
    val taxWithholdings: Double,
    val taxRefunds: Double,
    val netTax: Double,
    val taxComplianceRate: Double
)

data class PaymentMetrics(
    val totalPayments: Int,
    val successfulPayments: Int,
    val failedPayments: Int,
    val paymentsByGateway: Map<PaymentGateway, Int>,
    val paymentsByStatus: Map<TransactionStatus, Int>,
    val avgProcessingTime: Double,
    val successRate: Double,
    val paymentFailureAnalysis: PaymentFailureAnalysis
)

data class PaymentFailureAnalysis(
    val totalFailures: Int,
    val failuresByGateway: Map<PaymentGateway, Int>,
    val failuresByReason: Map<String, Int>,
    val mostCommonFailureReason: String?,
    val recommendations: List<String>
)

data class FraudAnalysis(
    val totalTransactions: Int,
    val suspiciousTransactions: Int,
    val fraudRate: Double,
    val fraudByType: Map<String, Int>,
    val fraudTrends: Map<String, Double>,
    val highRiskTransactions: List<FinancialTransaction>,
    val fraudPreventionRecommendations: List<String>
)

data class PaymentRequest(
    val customerId: String,
    val dossierId: String? = null,
    val amount: Double,
    val paymentType: PaymentType,
    val paymentMethod: String,
    val ipAddress: String = "",
    val deviceId: String = ""
)

data class PaymentResult(
    val success: Boolean,
    val transactionId: String,
    val paymentId: String,
    val amount: Double,
    val currency: String,
    val status: PaymentStatus,
    val gateway: PaymentGateway,
    val processedAt: Long,
    val expiresAt: Long,
    val paymentUrl: String?,
    val qrCode: String?,
    val fraudScore: Double
)

data class ReportPeriod(
    val startDate: Date,
    val endDate: Date
)

// Enums
enum class FinancialReportType {
    COMPREHENSIVE, REVENUE, TAX, PAYMENT, FRAUD
}

enum class PaymentGateway {
    DUMMY, MIDTRANS, XENDIT, DOKU
}

enum class PaymentStatus {
    SUCCESS, PENDING, FAILED, EXPIRED
}

enum class PaymentType {
    BOOKING_FEE, DP_1, DP_2, DP_PELUNASAN, BIAYA_STRATEGIS, BIAYA_ADMIN, BIAYA_NOTARIS, BIAYA_ASURANSI
}

enum class PaymentCategory {
    BOOKING_FEE, DOWN_PAYMENT, ADDITIONAL_FEES
}

enum class TransactionStatus {
    PENDING, VERIFIED, REJECTED, CANCELLED
}
