package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AIRepository {
    // Churn Prediction
    suspend fun predictChurn(dossierId: String): Result<ChurnPrediction>
    suspend fun getChurnPredictions(dossierIds: List<String>): Result<List<ChurnPrediction>>
    suspend fun updateChurnPrediction(dossierId: String): Result<ChurnPrediction>
    
    // Inventory Recommendations
    suspend fun getInventoryRecommendations(
        customerPreferences: Map<String, Any> = emptyMap()
    ): Result<List<InventoryRecommendation>>
    
    suspend fun generateUnitRecommendation(unitId: String): Result<InventoryRecommendation>
    
    // Customer Behavior
    suspend fun getCustomerBehavior(customerId: String): Result<CustomerBehavior>
    suspend fun updateCustomerBehavior(behavior: CustomerBehavior): Result<Unit>
    
    // Sales Metrics
    suspend fun getSalesMetrics(
        startDate: Long,
        endDate: Long
    ): Result<SalesMetrics>
    
    // Market Trends
    suspend fun getMarketTrends(): Result<MarketTrends>
    suspend fun updateMarketTrends(trends: MarketTrends): Result<Unit>
    
    // AI Insights
    suspend fun getAIInsights(
        insightType: AIInsightType? = null,
        limit: Int = 50
    ): Flow<List<AIInsight>>
    
    suspend fun generateAIInsight(
        insightType: AIInsightType,
        entityId: String
    ): Result<AIInsight>
    
    // Model Performance
    suspend fun getModelPerformance(modelName: String): Result<AIModelPerformance>
    suspend fun updateModelPerformance(performance: AIModelPerformance): Result<Unit>
    
    // Predictive Analytics
    suspend fun getPredictiveAnalytics(
        predictionType: PredictionType,
        entityId: String? = null
    ): Flow<List<PredictiveAnalytics>>
    
    suspend fun generatePrediction(
        predictionType: PredictionType,
        targetEntity: String,
        features: Map<String, Double>
    ): Result<PredictiveAnalytics>
    
    // Customer Segmentation
    suspend fun getCustomerSegments(): Flow<List<CustomerSegment>>
    suspend fun segmentCustomers(): Result<List<CustomerSegment>>
    
    // Sales Forecasting
    suspend fun getSalesForecast(
        forecastType: ForecastType,
        period: String
    ): Result<SalesForecast>
    
    suspend fun generateSalesForecast(
        forecastType: ForecastType,
        period: String
    ): Result<SalesForecast>
    
    // Model Training
    suspend fun trainModel(modelName: String): Result<Unit>
    suspend fun getModelTrainingStatus(modelName: String): Result<ModelTrainingStatus>
    
    // Data Collection
    suspend fun collectTrainingData(
        dataType: String,
        startDate: Long,
        endDate: Long
    ): Result<List<Map<String, Any>>>
    
    // Feature Engineering
    suspend fun extractFeatures(
        entityType: String,
        entityId: String
    ): Result<Map<String, Double>>
}
