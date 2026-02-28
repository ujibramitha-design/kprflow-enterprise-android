package com.kprflow.enterprise.ai

import com.kprflow.enterprise.domain.model.InventoryRecommendation
import com.kprflow.enterprise.domain.model.UnitProperty
import com.kprflow.enterprise.domain.model.SalesMetrics
import com.kprflow.enterprise.domain.model.MarketTrends
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRecommendationEngine @Inject constructor(
    private val modelInterpreter: Interpreter
) {
    
    companion object {
        private const val INPUT_SIZE = 15
        private const val OUTPUT_SIZE = 5 // Top 5 recommendations
        private const val MODEL_INPUT_SIZE = INPUT_SIZE * 4
    }
    
    suspend fun generateRecommendations(
        availableUnits: List<UnitProperty>,
        salesMetrics: SalesMetrics,
        marketTrends: MarketTrends,
        customerPreferences: Map<String, Any> = emptyMap()
    ): Result<List<InventoryRecommendation>> = withContext(Dispatchers.IO) {
        try {
            val recommendations = mutableListOf<InventoryRecommendation>()
            
            // Generate recommendations for each available unit
            for (unit in availableUnits.take(20)) { // Limit to top 20 units for performance
                val recommendation = predictUnitRecommendation(
                    unit = unit,
                    salesMetrics = salesMetrics,
                    marketTrends = marketTrends,
                    customerPreferences = customerPreferences
                )
                
                if (recommendation.isSuccess) {
                    recommendations.add(recommendation.getOrNull()!!)
                }
            }
            
            // Sort by recommendation score
            val sortedRecommendations = recommendations.sortedByDescending { it.score }
            
            Result.success(sortedRecommendations.take(10)) // Return top 10 recommendations
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun predictUnitRecommendation(
        unit: UnitProperty,
        salesMetrics: SalesMetrics,
        marketTrends: MarketTrends,
        customerPreferences: Map<String, Any>
    ): Result<InventoryRecommendation> {
        return try {
            // Prepare input features
            val inputFeatures = prepareInputFeatures(unit, salesMetrics, marketTrends, customerPreferences)
            
            // Run inference
            val output = Array(1) { FloatArray(OUTPUT_SIZE) }
            modelInterpreter.run(inputFeatures, output)
            
            // Get recommendation score
            val score = output[0][0]
            
            // Generate recommendation details
            val recommendation = InventoryRecommendation(
                unitId = unit.id,
                unitName = "${unit.blockName} - ${unit.unitNumber}",
                recommendationType = determineRecommendationType(score, unit),
                score = score.toDouble(),
                confidence = calculateConfidence(score),
                reasons = generateRecommendationReasons(unit, salesMetrics, marketTrends),
                suggestedPrice = calculateOptimalPrice(unit, marketTrends),
                targetMarket = identifyTargetMarket(unit, customerPreferences),
                expectedSaleTime = estimateSaleTime(unit, score, marketTrends),
                priority = determinePriority(score),
                generatedAt = System.currentTimeMillis()
            )
            
            Result.success(recommendation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun prepareInputFeatures(
        unit: UnitProperty,
        salesMetrics: SalesMetrics,
        marketTrends: MarketTrends,
        customerPreferences: Map<String, Any>
    ): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(MODEL_INPUT_SIZE)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        // Feature 1: Unit price (normalized)
        inputBuffer.putFloat(normalizePrice(unit.unitPrice))
        
        // Feature 2: Block popularity score (0-1)
        inputBuffer.putFloat(calculateBlockPopularity(unit.blockName, salesMetrics))
        
        // Feature 3: Unit type demand (0-1)
        inputBuffer.putFloat(calculateTypeDemand(unit.unitType, salesMetrics))
        
        // Feature 4: Days on market
        inputBuffer.putFloat(unit.daysOnMarket.toFloat())
        
        // Feature 5: Price change history (0-1)
        inputBuffer.putFloat(calculatePriceChangeHistory(unit))
        
        // Feature 6: Market demand index (0-1)
        inputBuffer.putFloat(marketTrends.demandIndex.toFloat())
        
        // Feature 7: Seasonal factor (0-1)
        inputBuffer.putFloat(calculateSeasonalFactor(marketTrends.currentSeason))
        
        // Feature 8: Competitor pricing ratio (0-1)
        inputBuffer.putFloat(calculateCompetitorPricingRatio(unit, marketTrends))
        
        // Feature 9: Customer budget preference (0-1)
        inputBuffer.putFloat(calculateBudgetPreference(unit.unitPrice, customerPreferences))
        
        // Feature 10: Location score (0-1)
        inputBuffer.putFloat(calculateLocationScore(unit))
        
        // Feature 11: Amenities score (0-1)
        inputBuffer.putFloat(calculateAmenitiesScore(unit))
        
        // Feature 12: View score (0-1)
        inputBuffer.putFloat(calculateViewScore(unit))
        
        // Feature 13: Recent sales velocity (0-1)
        inputBuffer.putFloat(calculateSalesVelocity(unit.blockName, salesMetrics))
        
        // Feature 14: Inventory level (0-1)
        inputBuffer.putFloat(calculateInventoryLevel(unit.blockName, salesMetrics))
        
        // Feature 15: Market trend direction (-1 to 1)
        inputBuffer.putFloat(marketTrends.trendDirection.toFloat())
        
        return inputBuffer
    }
    
    private fun determineRecommendationType(score: Float, unit: UnitProperty): RecommendationType {
        return when {
            score > 0.8 -> RecommendationType.HIGH_PRIORITY
            score > 0.6 -> RecommendationType.RECOMMENDED
            score > 0.4 -> RecommendationType.CONSIDER
            score > 0.2 -> RecommendationType.LOW_PRIORITY
            else -> RecommendationType.NOT_RECOMMENDED
        }
    }
    
    private fun calculateConfidence(score: Float): Double {
        return when {
            score > 0.9 || score < 0.1 -> 0.95
            score > 0.8 || score < 0.2 -> 0.85
            score > 0.7 || score < 0.3 -> 0.75
            score > 0.6 || score < 0.4 -> 0.65
            else -> 0.55
        }
    }
    
    private fun generateRecommendationReasons(
        unit: UnitProperty,
        salesMetrics: SalesMetrics,
        marketTrends: MarketTrends
    ): List<String> {
        val reasons = mutableListOf<String>()
        
        if (unit.daysOnMarket < 30) {
            reasons.add("Recently listed property")
        }
        
        if (calculateBlockPopularity(unit.blockName, salesMetrics) > 0.7) {
            reasons.add("High demand block location")
        }
        
        if (calculateTypeDemand(unit.unitType, salesMetrics) > 0.8) {
            reasons.add("Popular unit type")
        }
        
        if (marketTrends.demandIndex > 0.8) {
            reasons.add("Strong market demand")
        }
        
        if (calculateCompetitorPricingRatio(unit, marketTrends) < 0.9) {
            reasons.add("Competitive pricing advantage")
        }
        
        if (calculateSeasonalFactor(marketTrends.currentSeason) > 0.7) {
            reasons.add("Optimal selling season")
        }
        
        return reasons
    }
    
    private fun calculateOptimalPrice(unit: UnitProperty, marketTrends: MarketTrends): Double {
        val basePrice = unit.unitPrice
        val marketMultiplier = 1.0 + (marketTrends.trendDirection * 0.05)
        val seasonalMultiplier = 1.0 + (calculateSeasonalFactor(marketTrends.currentSeason) * 0.03)
        
        return basePrice * marketMultiplier * seasonalMultiplier
    }
    
    private fun identifyTargetMarket(unit: UnitProperty, customerPreferences: Map<String, Any>): String {
        val priceRange = customerPreferences["price_range"] as? String ?: "medium"
        val unitType = customerPreferences["unit_type"] as? String ?: "any"
        
        return when {
            unit.unitPrice < 500_000_000 -> "First-time buyers"
            unit.unitPrice < 1_000_000_000 -> "Middle-income families"
            unit.unitPrice < 2_000_000_000 -> "High-income professionals"
            else -> "Luxury segment"
        }
    }
    
    private fun estimateSaleTime(unit: UnitProperty, score: Float, marketTrends: MarketTrends): Int {
        val baseTime = when (score) {
            > 0.8 -> 30
            > 0.6 -> 60
            > 0.4 -> 90
            > 0.2 -> 120
            else -> 180
        }
        
        val marketFactor = when {
            marketTrends.demandIndex > 0.8 -> 0.7
            marketTrends.demandIndex < 0.3 -> 1.5
            else -> 1.0
        }
        
        return (baseTime * marketFactor).toInt()
    }
    
    private fun determinePriority(score: Float): RecommendationPriority {
        return when {
            score > 0.8 -> RecommendationPriority.CRITICAL
            score > 0.6 -> RecommendationPriority.HIGH
            score > 0.4 -> RecommendationPriority.MEDIUM
            score > 0.2 -> RecommendationPriority.LOW
            else -> RecommendationPriority.VERY_LOW
        }
    }
    
    // Helper functions for feature calculation
    private fun normalizePrice(price: Double): Float {
        return (price / 2_000_000_000.0).toFloat().coerceAtMost(1.0f)
    }
    
    private fun calculateBlockPopularity(blockName: String, salesMetrics: SalesMetrics): Float {
        return salesMetrics.blockSalesData[blockName]?.let { sales ->
            (sales.totalSold / (sales.totalSold + sales.totalAvailable)).toFloat()
        } ?: 0.5f
    }
    
    private fun calculateTypeDemand(unitType: String, salesMetrics: SalesMetrics): Float {
        return salesMetrics.typeSalesData[unitType]?.let { sales ->
            (sales.totalSold / (sales.totalSold + sales.totalAvailable)).toFloat()
        } ?: 0.5f
    }
    
    private fun calculatePriceChangeHistory(unit: UnitProperty): Float {
        // Calculate price change trend (0 = decreasing, 1 = increasing)
        return 0.5f // Placeholder - would calculate from historical data
    }
    
    private fun calculateSeasonalFactor(season: String): Float {
        return when (season.lowercase()) {
            "spring" -> 1.2f
            "summer" -> 1.0f
            "fall" -> 0.9f
            "winter" -> 0.8f
            else -> 1.0f
        }
    }
    
    private fun calculateCompetitorPricingRatio(unit: UnitProperty, marketTrends: MarketTrends): Float {
        return marketTrends.competitorPricing[unit.unitType]?.let { avgPrice ->
            (avgPrice / unit.unitPrice).toFloat()
        } ?: 1.0f
    }
    
    private fun calculateBudgetPreference(unitPrice: Double, preferences: Map<String, Any>): Float {
        val customerBudget = preferences["budget"] as? Double ?: unitPrice
        return when {
            customerBudget >= unitPrice -> 1.0f
            customerBudget >= unitPrice * 0.9 -> 0.8f
            customerBudget >= unitPrice * 0.8 -> 0.6f
            customerBudget >= unitPrice * 0.7 -> 0.4f
            else -> 0.2f
        }
    }
    
    private fun calculateLocationScore(unit: UnitProperty): Float {
        return 0.7f // Placeholder - would calculate from location data
    }
    
    private fun calculateAmenitiesScore(unit: UnitProperty): Float {
        return 0.6f // Placeholder - would calculate from amenities data
    }
    
    private fun calculateViewScore(unit: UnitProperty): Float {
        return 0.5f // Placeholder - would calculate from view data
    }
    
    private fun calculateSalesVelocity(blockName: String, salesMetrics: SalesMetrics): Float {
        return salesMetrics.blockSalesData[blockName]?.let { sales ->
            (sales.averageDaysToSell / 90.0f).coerceAtMost(1.0f)
        } ?: 0.5f
    }
    
    private fun calculateInventoryLevel(blockName: String, salesMetrics: SalesMetrics): Float {
        return salesMetrics.blockSalesData[blockName]?.let { sales ->
            (sales.totalAvailable.toFloat() / (sales.totalSold + sales.totalAvailable)).toFloat()
        } ?: 0.5f
    }
}

enum class RecommendationType {
    HIGH_PRIORITY,
    RECOMMENDED,
    CONSIDER,
    LOW_PRIORITY,
    NOT_RECOMMENDED
}

enum class RecommendationPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    VERY_LOW
}
