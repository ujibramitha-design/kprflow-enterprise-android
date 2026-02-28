package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    fun loadExecutiveData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val executiveSummary = analyticsRepository.getExecutiveSummary()
                val revenueData = analyticsRepository.getRevenueData()
                val performanceMetrics = analyticsRepository.getPerformanceMetrics()
                val riskAnalysis = analyticsRepository.getRiskAnalysis()
                val teamPerformance = analyticsRepository.getTeamPerformance()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    executiveSummary = executiveSummary,
                    revenueData = revenueData,
                    performanceMetrics = performanceMetrics,
                    riskAnalysis = riskAnalysis,
                    teamPerformance = teamPerformance
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load analytics data"
                )
            }
        }
    }
    
    fun loadOperationalData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val operationalData = analyticsRepository.getOperationalData()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationalData = operationalData
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load operational data"
                )
            }
        }
    }
    
    fun loadPerformanceData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val performanceData = analyticsRepository.getPerformanceData()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    performanceData = performanceData
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load performance data"
                )
            }
        }
    }
    
    fun predictCompletionProbability(dossierId: String) {
        viewModelScope.launch {
            try {
                val prediction = analyticsRepository.predictCompletionProbability(dossierId)
                _uiState.value = _uiState.value.copy(
                    predictionResult = prediction
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to predict completion probability"
                )
            }
        }
    }
    
    fun refreshData() {
        loadExecutiveData()
        loadOperationalData()
        loadPerformanceData()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val executiveSummary: ExecutiveSummary? = null,
    val revenueData: RevenueData? = null,
    val performanceMetrics: PerformanceMetrics? = null,
    val riskAnalysis: RiskAnalysis? = null,
    val teamPerformance: TeamPerformance? = null,
    val operationalData: List<OperationalData>? = null,
    val performanceData: List<PerformanceData>? = null,
    val predictionResult: PredictionResult? = null,
    val error: String? = null
)

data class ExecutiveSummary(
    val totalApplications: Int,
    val totalDisbursed: Int,
    val activePipeline: Int,
    val totalCompleted: Int,
    val totalRevenue: BigDecimal,
    val bookingFeeRevenue: BigDecimal,
    val dpRevenue: BigDecimal,
    val avgProcessingDays: Double,
    val conversionRate: Double,
    val slaCompliant14d: Int,
    val slaCompliant60d: Int,
    val reportingMonth: String
)

data class RevenueData(
    val totalRevenue: BigDecimal,
    val monthlyAverage: BigDecimal,
    val maxRevenue: BigDecimal,
    val monthlyData: List<MonthlyRevenue>
)

data class MonthlyRevenue(
    val month: String,
    val revenue: BigDecimal
)

data class PerformanceMetrics(
    val items: List<MetricItem>
)

data class MetricItem(
    val label: String,
    val value: String,
    val progress: Float
)

data class RiskAnalysis(
    val overallRiskScore: Int,
    val categories: List<RiskCategory>
)

data class RiskCategory(
    val name: String,
    val count: Int,
    val riskLevel: String
)

data class TeamPerformance(
    val members: List<TeamMember>
)

data class TeamMember(
    val name: String,
    val role: String,
    val completedApplications: Int
)

data class OperationalData(
    val dossierId: String,
    val currentStatus: String,
    val applicationDate: String,
    val lastUpdated: String,
    val customerName: String,
    val customerPhone: String,
    val unitInfo: String,
    val projectName: String,
    val unitPrice: BigDecimal,
    val totalPaid: BigDecimal,
    val totalPending: BigDecimal,
    val paymentProgress: Double,
    val totalDocuments: Int,
    val verifiedDocuments: Int,
    val completionPercentage: Double,
    val slaDaysRemaining: Int?,
    val applicationAgeDays: Int,
    val sikasepStatus: String?,
    val sikasepCheckedAt: String?,
    val assignedTo: String?,
    val marketingId: String?,
    val legalId: String?,
    val financeId: String?
)

data class PerformanceData(
    val month: String,
    val newApplications: Int,
    val completedApplications: Int,
    val avgProcessingTime: Double,
    val monthlyRevenue: BigDecimal,
    val avgDealSize: BigDecimal,
    val marketingAssigned: Int,
    val legalAssigned: Int,
    val financeAssigned: Int,
    val leads: Int,
    val documentation: Int,
    val bankSubmission: Int,
    val disbursed: Int,
    val projectName: String,
    val projectApplications: Int,
    val projectValue: BigDecimal
)

data class PredictionResult(
    val probability: Double,
    val factors: Map<String, Any>,
    val recommendation: String
)
