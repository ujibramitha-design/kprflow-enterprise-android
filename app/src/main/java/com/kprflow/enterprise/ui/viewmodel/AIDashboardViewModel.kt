package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.domain.model.*
import com.kprflow.enterprise.domain.usecase.PredictChurnUseCase
import com.kprflow.enterprise.domain.usecase.GetInventoryRecommendationsUseCase
import com.kprflow.enterprise.domain.usecase.GetAIInsightsUseCase
import com.kprflow.enterprise.domain.usecase.GetModelPerformanceUseCase
import com.kprflow.enterprise.domain.usecase.TrainAIModelsUseCase
import com.kprflow.enterprise.domain.usecase.LogCustomEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIDashboardViewModel @Inject constructor(
    private val predictChurnUseCase: PredictChurnUseCase,
    private val getInventoryRecommendationsUseCase: GetInventoryRecommendationsUseCase,
    private val getAIInsightsUseCase: GetAIInsightsUseCase,
    private val getModelPerformanceUseCase: GetModelPerformanceUseCase,
    private val trainAIModelsUseCase: TrainAIModelsUseCase,
    private val logCustomEventUseCase: LogCustomEventUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AIDashboardUiState())
    val uiState: StateFlow<AIDashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadAIData()
    }
    
    fun loadAIData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Log AI dashboard access
                logCustomEventUseCase(
                    "ai_dashboard_accessed",
                    mapOf("timestamp" to System.currentTimeMillis())
                )
                
                // Load AI data in parallel
                val recommendationsDeferred = async { getInventoryRecommendationsUseCase() }
                val insightsDeferred = async { getAIInsightsUseCase() }
                val modelPerformanceDeferred = async { getModelPerformanceUseCase() }
                
                // Wait for all to complete
                val recommendations = recommendationsDeferred.await().getOrNull() ?: emptyList()
                val insights = insightsDeferred.await().getOrNull() ?: emptyList()
                val modelPerformance = modelPerformanceDeferred.await().getOrNull() ?: emptyMap()
                
                // Get churn risk alerts
                val highRiskCustomers = getHighRiskCustomers()
                
                // Calculate overall metrics
                val totalPredictions = calculateTotalPredictions(modelPerformance)
                val overallAccuracy = calculateOverallAccuracy(modelPerformance)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    topRecommendations = recommendations,
                    aiInsights = insights,
                    modelPerformance = modelPerformance,
                    highRiskCustomers = highRiskCustomers,
                    totalPredictions = totalPredictions,
                    overallAccuracy = overallAccuracy
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load AI data: ${e.message}"
                )
                
                logCustomEventUseCase(
                    "ai_dashboard_load_failed",
                    mapOf("error" to e.message)
                )
            }
        }
    }
    
    fun refreshData() {
        loadAIData()
    }
    
    fun trainModels() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isTraining = true)
                
                val result = trainAIModelsUseCase()
                
                if (result.isSuccess) {
                    logCustomEventUseCase(
                        "ai_models_trained",
                        mapOf("timestamp" to System.currentTimeMillis())
                    )
                    
                    // Refresh data after training
                    loadAIData()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isTraining = false,
                        error = "Failed to train AI models: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTraining = false,
                    error = "Failed to train AI models: ${e.message}"
                )
            }
        }
    }
    
    fun applyRecommendation(recommendation: InventoryRecommendation) {
        viewModelScope.launch {
            try {
                logCustomEventUseCase(
                    "inventory_recommendation_applied",
                    mapOf(
                        "unit_id" to recommendation.unitId,
                        "score" to recommendation.score,
                        "priority" to recommendation.priority.name
                    )
                )
                
                // In a real implementation, this would apply the recommendation
                // For now, we'll just log the action
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to apply recommendation: ${e.message}"
                )
            }
        }
    }
    
    fun applyInsight(insight: AIInsight) {
        viewModelScope.launch {
            try {
                logCustomEventUseCase(
                    "ai_insight_applied",
                    mapOf(
                        "insight_id" to insight.id,
                        "insight_type" to insight.insightType.name,
                        "impact" to insight.impact.name
                    )
                )
                
                // In a real implementation, this would apply the insight
                // For now, we'll just log the action
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to apply insight: ${e.message}"
                )
            }
        }
    }
    
    fun viewAllChurnRisks() {
        viewModelScope.launch {
            try {
                val allHighRiskCustomers = getHighRiskCustomers(limit = 50)
                _uiState.value = _uiState.value.copy(
                    highRiskCustomers = allHighRiskCustomers
                )
                
                logCustomEventUseCase(
                    "view_all_churn_risks",
                    mapOf("count" to allHighRiskCustomers.size)
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load churn risks: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun getHighRiskCustomers(limit: Int = 10): List<ChurnRiskAlert> {
        return try {
            // Get all dossiers and predict churn for each
            val churnPredictions = predictChurnUseCase.predictBatch(
                listOf("dossier_1", "dossier_2", "dossier_3") // Placeholder IDs
            ).getOrNull() ?: emptyList()
            
            // Filter high risk customers and convert to alerts
            churnPredictions
                .filter { it.riskLevel == ChurnRiskLevel.HIGH || it.riskLevel == ChurnRiskLevel.CRITICAL }
                .sortedByDescending { it.churnProbability }
                .take(limit)
                .map { prediction ->
                    ChurnRiskAlert(
                        dossierId = prediction.dossierId,
                        customerName = "Customer ${prediction.dossierId}", // Placeholder
                        unitName = "Unit ${prediction.dossierId}", // Placeholder
                        churnProbability = prediction.churnProbability,
                        riskLevel = prediction.riskLevel
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun calculateTotalPredictions(modelPerformance: Map<String, Double>): Long {
        // In a real implementation, this would sum up inference counts from all models
        return modelPerformance.values.sum().toLong()
    }
    
    private fun calculateOverallAccuracy(modelPerformance: Map<String, Double>): Double {
        return if (modelPerformance.isNotEmpty()) {
            modelPerformance.values.average()
        } else 0.0
    }
}

data class AIDashboardUiState(
    val isLoading: Boolean = false,
    val isTraining: Boolean = false,
    val topRecommendations: List<InventoryRecommendation> = emptyList(),
    val aiInsights: List<AIInsight> = emptyList(),
    val modelPerformance: Map<String, Double> = emptyMap(),
    val highRiskCustomers: List<ChurnRiskAlert> = emptyList(),
    val totalPredictions: Long = 0L,
    val overallAccuracy: Double = 0.0,
    val error: String? = null
)
