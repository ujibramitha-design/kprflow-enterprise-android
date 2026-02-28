package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.domain.model.CrashStatistics
import com.kprflow.enterprise.domain.model.PerformanceMetrics
import com.kprflow.enterprise.domain.model.UserActivityMetrics
import com.kprflow.enterprise.domain.usecase.GetCrashStatisticsUseCase
import com.kprflow.enterprise.domain.usecase.GetPerformanceMetricsUseCase
import com.kprflow.enterprise.domain.usecase.GetUserActivityMetricsUseCase
import com.kprflow.enterprise.domain.usecase.LogCustomEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MonitoringViewModel @Inject constructor(
    private val getCrashStatisticsUseCase: GetCrashStatisticsUseCase,
    private val getPerformanceMetricsUseCase: GetPerformanceMetricsUseCase,
    private val getUserActivityMetricsUseCase: GetUserActivityMetricsUseCase,
    private val logCustomEventUseCase: LogCustomEventUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MonitoringUiState())
    val uiState: StateFlow<MonitoringUiState> = _uiState.asStateFlow()
    
    init {
        loadMonitoringData()
    }
    
    fun loadMonitoringData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val crashStats = getCrashStatisticsUseCase()
                val performanceMetrics = getPerformanceMetricsUseCase()
                val userActivityMetrics = getUserActivityMetricsUseCase()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    crashStatistics = crashStats.getOrNull(),
                    performanceMetrics = performanceMetrics.getOrNull(),
                    userActivityMetrics = userActivityMetrics.getOrNull(),
                    error = null
                )
                
                // Log monitoring dashboard access
                logCustomEventUseCase("monitoring_dashboard_loaded", mapOf(
                    "crash_rate" to (crashStats.getOrNull()?.crashRate ?: 0.0),
                    "active_users" to (userActivityMetrics.getOrNull()?.activeUsers ?: 0)
                ))
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load monitoring data"
                )
            }
        }
    }
    
    fun refreshData() {
        loadMonitoringData()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun exportMonitoringReport() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExporting = true)
                
                // Log export event
                logCustomEventUseCase("monitoring_report_exported", mapOf(
                    "export_timestamp" to System.currentTimeMillis(),
                    "crash_count" to (_uiState.value.crashStatistics?.totalCrashes ?: 0),
                    "active_users" to (_uiState.value.userActivityMetrics?.activeUsers ?: 0)
                ))
                
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Failed to export monitoring report: ${e.message}"
                )
            }
        }
    }
}

data class MonitoringUiState(
    val isLoading: Boolean = false,
    val crashStatistics: CrashStatistics? = null,
    val performanceMetrics: PerformanceMetrics? = null,
    val userActivityMetrics: UserActivityMetrics? = null,
    val error: String? = null,
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false
)
