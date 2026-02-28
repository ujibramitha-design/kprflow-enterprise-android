package com.kprflow.enterprise.domain.model

import java.util.UUID

data class ChurnPrediction(
    val dossierId: String,
    val churnProbability: Double,
    val riskLevel: ChurnRiskLevel,
    val confidence: Double,
    val riskFactors: List<String>,
    val recommendations: List<String>,
    val predictedAt: Long
)

data class CustomerBehavior(
    val customerId: String,
    val dossierId: String,
    val documentCompletionRate: Double,
    val paymentProgressRate: Double,
    val daysSinceLastActivity: Int,
    val supportInteractions: Int,
    val averageResponseTime: Long, // in hours
    val slaComplianceRate: Double,
    val lastUpdated: Long
)

data class DossierMetrics(
    val dossierId: String,
    val statusChanges: Int,
    val timeInCurrentStatus: Int, // in days
    val totalAmount: Double,
    val downPaymentRatio: Double,
    val createdAt: Long,
    val lastUpdated: Long
)

data class InventoryRecommendation(
    val unitId: String,
    val unitName: String,
    val recommendationType: RecommendationType,
    val score: Double,
    val confidence: Double,
    val reasons: List<String>,
    val suggestedPrice: Double,
    val targetMarket: String,
    val expectedSaleTime: Int, // in days
    val priority: RecommendationPriority,
    val generatedAt: Long
)

data class SalesMetrics(
    val totalSales: Int,
    val totalRevenue: Double,
    val averageSaleTime: Int, // in days
    val conversionRate: Double,
    val blockSalesData: Map<String, BlockSalesData>,
    val typeSalesData: Map<String, TypeSalesData>,
    val periodStart: Long,
    val periodEnd: Long
)

data class BlockSalesData(
    val blockName: String,
    val totalSold: Int,
    val totalAvailable: Int,
    val averageSalePrice: Double,
    val averageDaysToSell: Int
)

data class TypeSalesData(
    val unitType: String,
    val totalSold: Int,
    val totalAvailable: Int,
    val averageSalePrice: Double,
    val averageDaysToSell: Int
)

data class MarketTrends(
    val demandIndex: Double,
    val trendDirection: Double, // -1.0 to 1.0
    val currentSeason: String,
    val competitorPricing: Map<String, Double>,
    val marketSentiment: Double, // 0.0 to 1.0
    val lastUpdated: Long
)

data class AIInsight(
    val id: String,
    val insightType: AIInsightType,
    val title: String,
    val description: String,
    val confidence: Double,
    val impact: ImpactLevel,
    val actionable: Boolean,
    val recommendedActions: List<String>,
    val relatedEntities: List<String>,
    val validUntil: Long,
    val generatedAt: Long
)

data class AIModelPerformance(
    val modelName: String,
    val modelVersion: String,
    val accuracy: Double,
    val precision: Double,
    val recall: Double,
    val f1Score: Double,
    val lastTrainedAt: Long,
    val trainingDataSize: Int,
    val validationDataSize: Int,
    val inferenceCount: Long,
    val averageInferenceTime: Double // in milliseconds
)

data class PredictiveAnalytics(
    val id: String,
    val predictionType: PredictionType,
    val targetEntity: String,
    val prediction: Any,
    val probability: Double,
    val confidenceInterval: Pair<Double, Double>,
    val features: Map<String, Double>,
    val modelUsed: String,
    val generatedAt: Long,
    val expiresAt: Long
)

data class CustomerSegment(
    val id: String,
    val segmentName: String,
    val description: String,
    val characteristics: Map<String, Any>,
    val size: Int,
    val averageValue: Double,
    val churnRate: Double,
    val conversionRate: Double,
    val recommendations: List<String>,
    val createdAt: Long,
    val lastUpdated: Long
)

data class SalesForecast(
    val id: String,
    val forecastType: ForecastType,
    val period: String,
    val predictedSales: Int,
    val predictedRevenue: Double,
    val confidence: Double,
    val factors: List<String>,
    val methodology: String,
    val generatedAt: Long,
    val validUntil: Long
)

enum class ChurnRiskLevel {
    VERY_LOW,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
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

enum class AIInsightType {
    CHURN_RISK,
    SALES_OPPORTUNITY,
    MARKET_TREND,
    OPERATIONAL_EFFICIENCY,
    CUSTOMER_BEHAVIOR,
    INVENTORY_OPTIMIZATION
}

enum class ImpactLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class PredictionType {
    CHURN_PROBABILITY,
    SALES_FORECAST,
    INVENTORY_DEMAND,
    PRICE_OPTIMIZATION,
    CUSTOMER_SEGMENTATION,
    MARKET_TREND
}

enum class ForecastType {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY
}
