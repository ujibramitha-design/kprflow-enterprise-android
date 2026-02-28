package com.kprflow.enterprise.ai

import com.kprflow.enterprise.domain.model.ChurnPrediction
import com.kprflow.enterprise.domain.model.CustomerBehavior
import com.kprflow.enterprise.domain.model.DossierMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChurnPredictionModel @Inject constructor(
    private val modelInterpreter: Interpreter
) {
    
    companion object {
        private const val INPUT_SIZE = 10
        private const val OUTPUT_SIZE = 1
        private const val MODEL_INPUT_SIZE = INPUT_SIZE * 4 // 4 bytes per float
    }
    
    suspend fun predictChurn(
        customerBehavior: CustomerBehavior,
        dossierMetrics: DossierMetrics
    ): Result<ChurnPrediction> = withContext(Dispatchers.IO) {
        try {
            // Prepare input features
            val inputFeatures = prepareInputFeatures(customerBehavior, dossierMetrics)
            
            // Run inference
            val output = Array(1) { FloatArray(OUTPUT_SIZE) }
            modelInterpreter.run(inputFeatures, output)
            
            // Process output
            val churnProbability = output[0][0]
            val riskLevel = calculateRiskLevel(churnProbability)
            val confidence = calculateConfidence(churnProbability)
            
            val prediction = ChurnPrediction(
                dossierId = dossierMetrics.dossierId,
                churnProbability = churnProbability,
                riskLevel = riskLevel,
                confidence = confidence,
                riskFactors = identifyRiskFactors(customerBehavior, dossierMetrics),
                recommendations = generateRecommendations(riskLevel, customerBehavior),
                predictedAt = System.currentTimeMillis()
            )
            
            Result.success(prediction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun prepareInputFeatures(
        customerBehavior: CustomerBehavior,
        dossierMetrics: DossierMetrics
    ): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(MODEL_INPUT_SIZE)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        // Feature 1: Document completion rate (0-1)
        inputBuffer.putFloat(customerBehavior.documentCompletionRate.toFloat())
        
        // Feature 2: Payment progress rate (0-1)
        inputBuffer.putFloat(customerBehavior.paymentProgressRate.toFloat())
        
        // Feature 3: Days since last activity
        inputBuffer.putFloat(customerBehavior.daysSinceLastActivity.toFloat())
        
        // Feature 4: Number of support interactions
        inputBuffer.putFloat(customerBehavior.supportInteractions.toFloat())
        
        // Feature 5: Average response time (normalized)
        inputBuffer.putFloat(normalizeResponseTime(customerBehavior.averageResponseTime))
        
        // Feature 6: SLA compliance rate (0-1)
        inputBuffer.putFloat(customerBehavior.slaComplianceRate.toFloat())
        
        // Feature 7: Number of status changes
        inputBuffer.putFloat(dossierMetrics.statusChanges.toFloat())
        
        // Feature 8: Time in current status (days)
        inputBuffer.putFloat(dossierMetrics.timeInCurrentStatus.toFloat())
        
        // Feature 9: Total amount (normalized)
        inputBuffer.putFloat(normalizeAmount(dossierMetrics.totalAmount))
        
        // Feature 10: Down payment ratio (0-1)
        inputBuffer.putFloat(dossierMetrics.downPaymentRatio.toFloat())
        
        return inputBuffer
    }
    
    private fun calculateRiskLevel(probability: Double): ChurnRiskLevel {
        return when {
            probability >= 0.8 -> ChurnRiskLevel.CRITICAL
            probability >= 0.6 -> ChurnRiskLevel.HIGH
            probability >= 0.4 -> ChurnRiskLevel.MEDIUM
            probability >= 0.2 -> ChurnRiskLevel.LOW
            else -> ChurnRiskLevel.VERY_LOW
        }
    }
    
    private fun calculateConfidence(probability: Double): Double {
        // Confidence based on distance from decision boundaries
        return when {
            probability >= 0.9 || probability <= 0.1 -> 0.95
            probability >= 0.8 || probability <= 0.2 -> 0.85
            probability >= 0.7 || probability <= 0.3 -> 0.75
            probability >= 0.6 || probability <= 0.4 -> 0.65
            else -> 0.55
        }
    }
    
    private fun identifyRiskFactors(
        customerBehavior: CustomerBehavior,
        dossierMetrics: DossierMetrics
    ): List<String> {
        val riskFactors = mutableListOf<String>()
        
        if (customerBehavior.daysSinceLastActivity > 30) {
            riskFactors.add("Low customer engagement")
        }
        
        if (customerBehavior.documentCompletionRate < 0.5) {
            riskFactors.add("Incomplete documentation")
        }
        
        if (customerBehavior.paymentProgressRate < 0.3) {
            riskFactors.add("Low payment progress")
        }
        
        if (customerBehavior.supportInteractions > 5) {
            riskFactors.add("High support interactions")
        }
        
        if (dossierMetrics.timeInCurrentStatus > 60) {
            riskFactors.add("Stalled application process")
        }
        
        if (customerBehavior.slaComplianceRate < 0.7) {
            riskFactors.add("SLA compliance issues")
        }
        
        return riskFactors
    }
    
    private fun generateRecommendations(
        riskLevel: ChurnRiskLevel,
        customerBehavior: CustomerBehavior
    ): List<String> {
        return when (riskLevel) {
            ChurnRiskLevel.CRITICAL -> listOf(
                "Immediate customer outreach required",
                "Assign dedicated account manager",
                "Offer special incentives",
                "Review entire application process"
            )
            
            ChurnRiskLevel.HIGH -> listOf(
                "Schedule customer follow-up call",
                "Review documentation requirements",
                "Provide additional support resources",
                "Monitor closely for next 7 days"
            )
            
            ChurnRiskLevel.MEDIUM -> listOf(
                "Send personalized engagement email",
                "Check on application progress",
                "Offer assistance with next steps"
            )
            
            ChurnRiskLevel.LOW -> listOf(
                "Maintain regular communication",
                "Monitor for any changes in behavior"
            )
            
            ChurnRiskLevel.VERY_LOW -> listOf(
                "Continue standard engagement process"
            )
        }
    }
    
    private fun normalizeResponseTime(responseTime: Long): Float {
        // Normalize response time (in hours) to 0-1 scale
        return (responseTime / 24.0f).coerceAtMost(1.0f)
    }
    
    private fun normalizeAmount(amount: Double): Float {
        // Normalize amount to 0-1 scale (assuming max amount of 2 billion)
        return (amount / 2_000_000_000.0).toFloat().coerceAtMost(1.0f)
    }
}

enum class ChurnRiskLevel {
    VERY_LOW,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
