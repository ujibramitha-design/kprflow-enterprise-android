package com.kprflow.enterprise.government

import android.content.Context
import com.kprflow.enterprise.data.model.*
import com.kprflow.enterprise.domain.repository.GovernmentRepository
import com.kprflow.enterprise.domain.repository.KprRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Advanced Government API Manager with dummy implementations for SiKasep, BPJS, and Kemenkeu integration
 */
class AdvancedGovernmentAPIManager(
    private val context: Context,
    private val governmentRepository: GovernmentRepository,
    private val kprRepository: KprRepository
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val eligibilityCalculator = AdvancedEligibilityCalculator()
    
    companion object {
        private const val API_TIMEOUT_MS = 30000L
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BULK_PROCESSING_BATCH_SIZE = 20
    }
    
    /**
     * Check SiKasep eligibility with dummy API integration
     */
    suspend fun checkSiKasepEligibility(
        customerId: String,
        customerData: CustomerData
    ): Result<SiKasepEligibilityResult> = withContext(Dispatchers.IO) {
        
        try {
            // Pre-validation
            val validationResult = validateCustomerData(customerData)
            if (validationResult.isFailure) {
                return Result.failure(validationResult.exceptionOrNull()!!)
            }
            
            // Call dummy SiKasep API
            val apiResult = callDummySiKasepAPI(customerData)
            
            // Process results
            val eligibilityResult = processSiKasepResult(customerId, customerData, apiResult)
            
            // Log the check
            logSiKasepCheck(customerId, eligibilityResult)
            
            Result.success(eligibilityResult)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Bulk check SiKasep eligibility
     */
    suspend fun bulkCheckSiKasepEligibility(
        customerDataList: List<Pair<String, CustomerData>>
    ): Result<BulkSiKasepResult> = withContext(Dispatchers.IO) {
        
        try {
            val results = mutableListOf<SiKasepEligibilityResult>()
            val errors = mutableListOf<String>()
            val startTime = System.currentTimeMillis()
            
            // Process in batches
            customerDataList.chunked(BULK_PROCESSING_BATCH_SIZE).forEachIndexed { batchIndex, batch ->
                batch.forEach { (customerId, customerData) ->
                    try {
                        val result = checkSiKasepEligibility(customerId, customerData)
                        if (result.isSuccess) {
                            results.add(result.getOrNull()!!)
                        } else {
                            errors.add("Customer $customerId: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        errors.add("Customer $customerId: ${e.message}")
                    }
                }
                
                // Rate limiting to avoid overwhelming the API
                if (batchIndex < customerDataList.size / BULK_PROCESSING_BATCH_SIZE) {
                    delay(2000) // 2 seconds between batches
                }
            }
            
            val bulkResult = BulkSiKasepResult(
                totalCustomers = customerDataList.size,
                successfulChecks = results.size,
                failedChecks = errors.size,
                processingTimeMs = System.currentTimeMillis() - startTime,
                results = results,
                errors = errors,
                eligibilityRate = results.count { it.isEligible }.toDouble() / results.size
            )
            
            Result.success(bulkResult)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check BPJS eligibility
     */
    suspend fun checkBPJSEligibility(
        customerId: String,
        bpjsNumber: String
    ): Result<BPJSEligibilityResult> = withContext(Dispatchers.IO) {
        
        try {
            // Validate BPJS number
            if (bpjsNumber.isBlank() || bpjsNumber.length != 13) {
                return Result.failure(Exception("Invalid BPJS number"))
            }
            
            // Call dummy BPJS API
            val apiResult = callDummyBPJSAPI(bpjsNumber)
            
            // Process results
            val eligibilityResult = processBPJSResult(customerId, bpjsNumber, apiResult)
            
            // Log the check
            logBPJSCheck(customerId, bpjsNumber, eligibilityResult)
            
            Result.success(eligibilityResult)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check Kemenkeu tax compliance
     */
    suspend fun checkKemenkeuCompliance(
        customerId: String,
        npwpNumber: String
    ): Result<KemenkeuComplianceResult> = withContext(Dispatchers.IO) {
        
        try {
            // Validate NPWP number
            if (npwpNumber.isBlank() || npwpNumber.length != 15) {
                return Result.failure(Exception("Invalid NPWP number"))
            }
            
            // Call dummy Kemenkeu API
            val apiResult = callDummyKemenkeuAPI(npwpNumber)
            
            // Process results
            val complianceResult = processKemenkeuResult(customerId, npwpNumber, apiResult)
            
            // Log the check
            logKemenkeuCheck(customerId, npwpNumber, complianceResult)
            
            Result.success(complianceResult)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validate customer data
     */
    private fun validateCustomerData(customerData: CustomerData): Result<Unit> {
        if (customerData.nik.isBlank() || customerData.nik.length != 16) {
            return Result.failure(Exception("Invalid NIK"))
        }
        
        if (customerData.monthlyIncome <= 0) {
            return Result.failure(Exception("Invalid monthly income"))
        }
        
        if (customerData.dateOfBirth == null) {
            return Result.failure(Exception("Date of birth is required"))
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Call dummy SiKasep API
     */
    private suspend fun callDummySiKasepAPI(customerData: CustomerData): SiKasepAPIResponse {
        // Simulate API call delay
        delay(1500)
        
        // Simulate API response based on customer data
        val isEligible = eligibilityCalculator.calculateSiKasepEligibility(customerData)
        
        return SiKasepAPIResponse(
            success = true,
            isEligible = isEligible,
            siKasepId = if (isEligible) "FLPP-${UUID.randomUUID().toString().take(8).uppercase()}" else null,
            eligibilityScore = eligibilityCalculator.calculateEligibilityScore(customerData),
            rejectionReasons = if (!isEligible) eligibilityCalculator.getRejectionReasons(customerData) else emptyList(),
            maxSubsidyAmount = if (isEligible) eligibilityCalculator.calculateMaxSubsidy(customerData) else 0.0,
            processingTimeMs = 1500,
            apiVersion = "v2.0",
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Call dummy BPJS API
     */
    private suspend fun callDummyBPJSAPI(bpjsNumber: String): BPJSAPIResponse {
        delay(1000)
        
        // Simulate BPJS data
        val isActive = bpjsNumber.startsWith("1") // Simple logic for demo
        val contributionMonths = (12..36).random() // 1-3 years of contributions
        
        return BPJSAPIResponse(
            success = true,
            isActive = isActive,
            participantName = "Dummy Participant",
            registrationDate = Date(System.currentTimeMillis() - (contributionMonths * 30L * 24 * 60 * 60 * 1000)),
            contributionMonths = contributionMonths,
            lastContributionDate = Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000),
            contributionClass = "Kelas ${ (1..3).random() }",
            monthlyContribution = 100000.0 + (Math.random() * 200000), // 100K-300K
            processingTimeMs = 1000,
            apiVersion = "v1.0",
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Call dummy Kemenkeu API
     */
    private suspend fun callDummyKemenkeuAPI(npwpNumber: String): KemenkeuAPIResponse {
        delay(2000)
        
        // Simulate tax compliance data
        val isCompliant = npwpNumber.startsWith("2") // Simple logic for demo
        val taxYear = Calendar.getInstance().get(Calendar.YEAR) - 1
        
        return KemenkeuAPIResponse(
            success = true,
            isCompliant = isCompliant,
            taxpayerName = "Dummy Taxpayer",
            registrationDate = Date(System.currentTimeMillis() - 5L * 365 * 24 * 60 * 60 * 1000), // 5 years ago
            taxYear = taxYear,
            annualIncome = 60000000.0 + (Math.random() * 240000000), // 60M-300M
            taxPaid = if (isCompliant) 6000000.0 + (Math.random() * 24000000) else 0.0, // 6M-30M or 0
            taxOwed = 6000000.0 + (Math.random() * 24000000), // 6M-30M
            complianceScore = if (isCompliant) 0.8 + (Math.random() * 0.2) else 0.1 + (Math.random() * 0.3),
            lastFilingDate = if (isCompliant) Date(System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000) else null,
            processingTimeMs = 2000,
            apiVersion = "v1.5",
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Process SiKasep result
     */
    private fun processSiKasepResult(
        customerId: String,
        customerData: CustomerData,
        apiResponse: SiKasepAPIResponse
    ): SiKasepEligibilityResult {
        return SiKasepEligibilityResult(
            customerId = customerId,
            isEligible = apiResponse.isEligible,
            siKasepId = apiResponse.siKasepId,
            eligibilityScore = apiResponse.eligibilityScore,
            maxSubsidyAmount = apiResponse.maxSubsidyAmount,
            rejectionReasons = apiResponse.rejectionReasons,
            checkedAt = System.currentTimeMillis(),
            apiResponseTime = apiResponse.processingTimeMs,
            additionalInfo = mapOf(
                "monthly_income" to customerData.monthlyIncome,
                "nik" to customerData.nik,
                "age" to calculateAge(customerData.dateOfBirth),
                "first_home" to customerData.isFirstHomeBuyer,
                "marital_status" to customerData.maritalStatus
            )
        )
    }
    
    /**
     * Process BPJS result
     */
    private fun processBPJSResult(
        customerId: String,
        bpjsNumber: String,
        apiResponse: BPJSAPIResponse
    ): BPJSEligibilityResult {
        return BPJSEligibilityResult(
            customerId = customerId,
            bpjsNumber = bpjsNumber,
            isActive = apiResponse.isActive,
            participantName = apiResponse.participantName,
            registrationDate = apiResponse.registrationDate,
            contributionMonths = apiResponse.contributionMonths,
            lastContributionDate = apiResponse.lastContributionDate,
            contributionClass = apiResponse.contributionClass,
            monthlyContribution = apiResponse.monthlyContribution,
            isEligibleForHousing = apiResponse.isActive && apiResponse.contributionMonths >= 12,
            checkedAt = System.currentTimeMillis(),
            apiResponseTime = apiResponse.processingTimeMs
        )
    }
    
    /**
     * Process Kemenkeu result
     */
    private fun processKemenkeuResult(
        customerId: String,
        npwpNumber: String,
        apiResponse: KemenkeuAPIResponse
    ): KemenkeuComplianceResult {
        return KemenkeuComplianceResult(
            customerId = customerId,
            npwpNumber = npwpNumber,
            isCompliant = apiResponse.isCompliant,
            taxpayerName = apiResponse.taxpayerName,
            registrationDate = apiResponse.registrationDate,
            taxYear = apiResponse.taxYear,
            annualIncome = apiResponse.annualIncome,
            taxPaid = apiResponse.taxPaid,
            taxOwed = apiResponse.taxOwed,
            complianceScore = apiResponse.complianceScore,
            lastFilingDate = apiResponse.lastFilingDate,
            hasOutstandingTax = apiResponse.taxPaid < apiResponse.taxOwed,
            checkedAt = System.currentTimeMillis(),
            apiResponseTime = apiResponse.processingTimeMs
        )
    }
    
    /**
     * Calculate age from date of birth
     */
    private fun calculateAge(dateOfBirth: Date): Int {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.time = dateOfBirth
        
        var age = calendar.get(Calendar.YEAR) - Calendar.getInstance().get(Calendar.YEAR)
        val monthDifference = calendar.get(Calendar.MONTH) - Calendar.getInstance().get(Calendar.MONTH)
        
        if (monthDifference < 0 || (monthDifference == 0 && calendar.get(Calendar.DAY_OF_MONTH) < Calendar.getInstance().get(Calendar.DAY_OF_MONTH))) {
            age--
        }
        
        return age
    }
    
    /**
     * Log SiKasep check
     */
    private suspend fun logSiKasepCheck(customerId: String, result: SiKasepEligibilityResult) {
        val logData = mapOf(
            "customer_id" to customerId,
            "check_type" to "SIKASEP_ELIGIBILITY",
            "is_eligible" to result.isEligible,
            "eligibility_score" to result.eligibilityScore,
            "max_subsidy" to result.maxSubsidyAmount,
            "rejection_reasons" to result.rejectionReasons,
            "checked_at" to result.checkedAt
        )
        
        governmentRepository.logGovernmentCheck(logData)
    }
    
    /**
     * Log BPJS check
     */
    private suspend fun logBPJSCheck(customerId: String, bpjsNumber: String, result: BPJSEligibilityResult) {
        val logData = mapOf(
            "customer_id" to customerId,
            "check_type" to "BPJS_ELIGIBILITY",
            "bpjs_number" to bpjsNumber,
            "is_active" to result.isActive,
            "contribution_months" to result.contributionMonths,
            "is_eligible_for_housing" to result.isEligibleForHousing,
            "checked_at" to result.checkedAt
        )
        
        governmentRepository.logGovernmentCheck(logData)
    }
    
    /**
     * Log Kemenkeu check
     */
    private suspend fun logKemenkeuCheck(customerId: String, npwpNumber: String, result: KemenkeuComplianceResult) {
        val logData = mapOf(
            "customer_id" to customerId,
            "check_type" to "KEMENKEU_COMPLIANCE",
            "npwp_number" to npwpNumber,
            "is_compliant" to result.isCompliant,
            "compliance_score" to result.complianceScore,
            "has_outstanding_tax" to result.hasOutstandingTax,
            "checked_at" to result.checkedAt
        )
        
        governmentRepository.logGovernmentCheck(logData)
    }
    
    /**
     * Get government API statistics
     */
    suspend fun getGovernmentAPIStatistics(
        startDate: Date,
        endDate: Date
    ): Result<GovernmentAPIStatistics> = withContext(Dispatchers.IO) {
        
        try {
            val checks = governmentRepository.getGovernmentChecksByDateRange(startDate, endDate)
                .getOrNull() ?: emptyList()
            
            val siKasepChecks = checks.filter { it["check_type"] == "SIKASEP_ELIGIBILITY" }
            val bpjsChecks = checks.filter { it["check_type"] == "BPJS_ELIGIBILITY" }
            val kemenkeuChecks = checks.filter { it["check_type"] == "KEMENKEU_COMPLIANCE" }
            
            val statistics = GovernmentAPIStatistics(
                totalChecks = checks.size,
                siKasepChecks = siKasepChecks.size,
                bpjsChecks = bpjsChecks.size,
                kemenkeuChecks = kemenkeuChecks.size,
                siKasepEligibilityRate = siKasepChecks.count { it["is_eligible"] == true }.toDouble() / siKasepChecks.size,
                bpjsEligibilityRate = bpjsChecks.count { it["is_eligible_for_housing"] == true }.toDouble() / bpjsChecks.size,
                kemenkeuComplianceRate = kemenkeuChecks.count { it["is_compliant"] == true }.toDouble() / kemenkeuChecks.size,
                avgResponseTime = checks.mapNotNull { it["api_response_time"] as? Long }.average(),
                mostCommonRejectionReason = siKasepChecks
                    .filter { it["is_eligible"] == false }
                    .flatMap { it["rejection_reasons"] as? List<String> ?: emptyList() }
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }?.key
            )
            
            Result.success(statistics)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Advanced Eligibility Calculator
 */
class AdvancedEligibilityCalculator {
    
    /**
     * Calculate SiKasep eligibility
     */
    fun calculateSiKasepEligibility(customerData: CustomerData): Boolean {
        // Income criteria: <= Rp 8.000.000 per month
        if (customerData.monthlyIncome > 8_000_000) return false
        
        // Age criteria: 21-55 years old
        val age = calculateAge(customerData.dateOfBirth)
        if (age < 21 || age > 55) return false
        
        // First home buyer
        if (!customerData.isFirstHomeBuyer) return false
        
        // NIK validation
        if (customerData.nik.length != 16) return false
        
        // Marital status (optional criteria)
        if (customerData.maritalStatus == "MENIKAH" && customerData.monthlyIncome > 7_000_000) {
            return false // Married couples have lower income threshold
        }
        
        return true
    }
    
    /**
     * Calculate eligibility score
     */
    fun calculateEligibilityScore(customerData: CustomerData): Double {
        var score = 0.0
        
        // Income score (40% weight)
        val incomeScore = maxOf(0.0, (8_000_000 - customerData.monthlyIncome) / 8_000_000)
        score += incomeScore * 0.4
        
        // Age score (20% weight)
        val age = calculateAge(customerData.dateOfBirth)
        val ageScore = when {
            age in 21..35 -> 1.0
            age in 36..45 -> 0.8
            age in 46..55 -> 0.6
            else -> 0.0
        }
        score += ageScore * 0.2
        
        // First home buyer score (20% weight)
        val firstHomeScore = if (customerData.isFirstHomeBuyer) 1.0 else 0.0
        score += firstHomeScore * 0.2
        
        // Marital status score (10% weight)
        val maritalScore = when (customerData.maritalStatus) {
            "BELUM MENIKAH" -> 1.0
            "MENIKAH" -> 0.8
            "CERAI" -> 0.6
            else -> 0.0
        }
        score += maritalScore * 0.1
        
        // NIK validity score (10% weight)
        val nikScore = if (customerData.nik.length == 16) 1.0 else 0.0
        score += nikScore * 0.1
        
        return score
    }
    
    /**
     * Calculate maximum subsidy amount
     */
    fun calculateMaxSubsidy(customerData: CustomerData): Double {
        val baseSubsidy = 185_000_000.0 // Base FLPP subsidy
        val eligibilityScore = calculateEligibilityScore(customerData)
        
        // Adjust subsidy based on eligibility score
        return baseSubsidy * eligibilityScore
    }
    
    /**
     * Get rejection reasons
     */
    fun getRejectionReasons(customerData: CustomerData): List<String> {
        val reasons = mutableListOf<String>()
        
        if (customerData.monthlyIncome > 8_000_000) {
            reasons.add("Penghasilan melebihi batas Rp 8.000.000 per bulan")
        }
        
        val age = calculateAge(customerData.dateOfBirth)
        if (age < 21) {
            reasons.add("Usia di bawah 21 tahun")
        }
        if (age > 55) {
            reasons.add("Usia di atas 55 tahun")
        }
        
        if (!customerData.isFirstHomeBuyer) {
            reasons.add("Bukan pembeli rumah pertama")
        }
        
        if (customerData.nik.length != 16) {
            reasons.add("Format NIK tidak valid")
        }
        
        if (customerData.maritalStatus == "MENIKAH" && customerData.monthlyIncome > 7_000_000) {
            reasons.add("Penghasilan pasangan menikah melebihi batas")
        }
        
        return reasons
    }
    
    private fun calculateAge(dateOfBirth: Date): Int {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.time = dateOfBirth
        
        var age = calendar.get(Calendar.YEAR) - Calendar.getInstance().get(Calendar.YEAR)
        val monthDifference = calendar.get(Calendar.MONTH) - Calendar.getInstance().get(Calendar.MONTH)
        
        if (monthDifference < 0 || (monthDifference == 0 && calendar.get(Calendar.DAY_OF_MONTH) < Calendar.getInstance().get(Calendar.DAY_OF_MONTH))) {
            age--
        }
        
        return age
    }
}

// Data classes
data class CustomerData(
    val nik: String,
    val fullName: String,
    val dateOfBirth: Date,
    val monthlyIncome: Double,
    val maritalStatus: String,
    val isFirstHomeBuyer: Boolean,
    val phoneNumber: String,
    val email: String
)

data class SiKasepEligibilityResult(
    val customerId: String,
    val isEligible: Boolean,
    val siKasepId: String?,
    val eligibilityScore: Double,
    val maxSubsidyAmount: Double,
    val rejectionReasons: List<String>,
    val checkedAt: Long,
    val apiResponseTime: Long,
    val additionalInfo: Map<String, Any>
)

data class BPJSEligibilityResult(
    val customerId: String,
    val bpjsNumber: String,
    val isActive: Boolean,
    val participantName: String,
    val registrationDate: Date,
    val contributionMonths: Int,
    val lastContributionDate: Date,
    val contributionClass: String,
    val monthlyContribution: Double,
    val isEligibleForHousing: Boolean,
    val checkedAt: Long,
    val apiResponseTime: Long
)

data class KemenkeuComplianceResult(
    val customerId: String,
    val npwpNumber: String,
    val isCompliant: Boolean,
    val taxpayerName: String,
    val registrationDate: Date,
    val taxYear: Int,
    val annualIncome: Double,
    val taxPaid: Double,
    val taxOwed: Double,
    val complianceScore: Double,
    val lastFilingDate: Date?,
    val hasOutstandingTax: Boolean,
    val checkedAt: Long,
    val apiResponseTime: Long
)

data class BulkSiKasepResult(
    val totalCustomers: Int,
    val successfulChecks: Int,
    val failedChecks: Int,
    val processingTimeMs: Long,
    val results: List<SiKasepEligibilityResult>,
    val errors: List<String>,
    val eligibilityRate: Double
)

data class GovernmentAPIStatistics(
    val totalChecks: Int,
    val siKasepChecks: Int,
    val bpjsChecks: Int,
    val kemenkeuChecks: Int,
    val siKasepEligibilityRate: Double,
    val bpjsEligibilityRate: Double,
    val kemenkeuComplianceRate: Double,
    val avgResponseTime: Double,
    val mostCommonRejectionReason: String?
)

// API Response data classes
data class SiKasepAPIResponse(
    val success: Boolean,
    val isEligible: Boolean,
    val siKasepId: String?,
    val eligibilityScore: Double,
    val rejectionReasons: List<String>,
    val maxSubsidyAmount: Double,
    val processingTimeMs: Long,
    val apiVersion: String,
    val timestamp: Long
)

data class BPJSAPIResponse(
    val success: Boolean,
    val isActive: Boolean,
    val participantName: String,
    val registrationDate: Date,
    val contributionMonths: Int,
    val lastContributionDate: Date,
    val contributionClass: String,
    val monthlyContribution: Double,
    val processingTimeMs: Long,
    val apiVersion: String,
    val timestamp: Long
)

data class KemenkeuAPIResponse(
    val success: Boolean,
    val isCompliant: Boolean,
    val taxpayerName: String,
    val registrationDate: Date,
    val taxYear: Int,
    val annualIncome: Double,
    val taxPaid: Double,
    val taxOwed: Double,
    val complianceScore: Double,
    val lastFilingDate: Date?,
    val processingTimeMs: Long,
    val apiVersion: String,
    val timestamp: Long
)
