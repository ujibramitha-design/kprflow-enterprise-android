package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.AnalyticsRepository
import com.kprflow.enterprise.ui.screens.AnalyticsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BODAnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AnalyticsState<Nothing>>(AnalyticsState.Loading)
    val uiState: StateFlow<AnalyticsState<Nothing>> = _uiState.asStateFlow()
    
    private val _funnelState = MutableStateFlow<AnalyticsState<com.kprflow.enterprise.data.repository.KPRPipelineFunnel>>(AnalyticsState.Loading)
    val funnelState: StateFlow<AnalyticsState<com.kprflow.enterprise.data.repository.KPRPipelineFunnel>> = _funnelState.asStateFlow()
    
    private val _processingTimeState = MutableStateFlow<AnalyticsState<List<com.kprflow.enterprise.data.repository.PhaseProcessingTime>>>(AnalyticsState.Loading)
    val processingTimeState: StateFlow<AnalyticsState<List<com.kprflow.enterprise.data.repository.PhaseProcessingTime>>> = _processingTimeState.asStateFlow()
    
    private val _bankStatsState = MutableStateFlow<AnalyticsState<List<com.kprflow.enterprise.data.repository.BankApprovalStats>>>(AnalyticsState.Loading)
    val bankStatsState: StateFlow<AnalyticsState<List<com.kprflow.enterprise.data.repository.BankApprovalStats>>> = _bankStatsState.asStateFlow()
    
    private val _revenueState = MutableStateFlow<AnalyticsState<com.kprflow.enterprise.data.repository.RevenueProjection>>(AnalyticsState.Loading)
    val revenueState: StateFlow<AnalyticsState<com.kprflow.enterprise.data.repository.RevenueProjection>> = _revenueState.asStateState()
    
    private val _slaState = MutableStateFlow<AnalyticsState<com.kprflow.enterprise.data.repository.SLAComplianceMetrics>>(AnalyticsState.Loading)
    val slaState: StateFlow<AnalyticsState<com.kprflow.enterprise.data.repository.SLAComplianceMetrics>> = _slaState.asStateState()
    
    private val _teamState = MutableStateFlow<AnalyticsState<com.kprflow.enterprise.data.repository.TeamPerformanceMetrics>>(AnalyticsState.Loading)
    val teamState: StateFlow<AnalyticsState<com.kprflow.enterprise.data.repository.TeamPerformanceMetrics>> = _teamState.asStateState()
    
    fun loadAllAnalytics() {
        loadPipelineFunnel()
        loadProcessingTimes()
        loadBankStatistics()
        loadRevenueProjection()
        loadSLAMetrics()
        loadTeamPerformance()
    }
    
    fun loadPipelineFunnel() {
        viewModelScope.launch {
            _funnelState.value = AnalyticsState.Loading
            
            try {
                val funnel = analyticsRepository.getKPRPipelineFunnel()
                    .getOrNull()
                
                if (funnel != null) {
                    _funnelState.value = AnalyticsState.Success(funnel)
                } else {
                    _funnelState.value = AnalyticsState.Error("Failed to load pipeline funnel")
                }
            } catch (e: Exception) {
                _funnelState.value = AnalyticsState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun loadProcessingTimes() {
        viewModelScope.launch {
            _processingTimeState.value = AnalyticsState.Loading
            
            try {
                val processingTimes = analyticsRepository.getAverageProcessingTimePerPhase()
                    .getOrNull().orEmpty()
                
                _processingTimeState.value = AnalyticsState.Success(processingTimes)
            } catch (e: Exception) {
                _processingTimeState.value = AnalyticsState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun loadBankStatistics() {
        viewModelScope.launch {
            _bankStatsState.value = AnalyticsState.Loading
            
            try {
                val bankStats = analyticsRepository.getBankApprovalStatistics()
                    .getOrNull().orEmpty()
                
                _bankStatsState.value = AnalyticsState.Success(bankStats)
            } catch (e: Exception) {
                _bankStatsState.value = AnalyticsState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun loadRevenueProjection() {
        viewModelScope.launch {
            _revenueState.value = AnalyticsState.Loading
            
            try {
                val projection = analyticsRepository.getRevenueProjection(12)
                    .getOrNull()
                
                if (projection != null) {
                    _revenueState.value = AnalyticsState.Success(projection)
                } else {
                    _revenueState.value = AnalyticsState.Error("Failed to load revenue projection")
                }
            } catch (e: Exception) {
                _revenueState.value = AnalyticsState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun loadSLAMetrics() {
        viewModelScope.launch {
            _slaState.value = AnalyticsState.Loading
            
            try {
                val slaMetrics = analyticsRepository.getSLAComplianceMetrics()
                    .getOrNull()
                
                if (slaMetrics != null) {
                    _slaState.value = AnalyticsState.Success(slaMetrics)
                } else {
                    _slaState.value = AnalyticsState.Error("Failed to load SLA metrics")
                }
            } catch (e: Exception) {
                _slaState.value = AnalyticsState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun loadTeamPerformance() {
        viewModelScope.launch {
            _teamState.value = AnalyticsState.Loading
            
            try {
                val teamMetrics = analyticsRepository.getTeamPerformanceMetrics()
                    .getOrNull()
                
                if (teamMetrics != null) {
                    _teamState.value = AnalyticsState.Success(teamMetrics)
                } else {
                    _teamState.value = AnalyticsState.Error("Failed to load team performance")
                }
            } catch (e: Exception) {
                _teamState.value = AnalyticsState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun refreshAllAnalytics() {
        loadAllAnalytics()
    }
    
    fun refreshPipelineFunnel() {
        loadPipelineFunnel()
    }
    
    fun refreshProcessingTimes() {
        loadProcessingTimes()
    }
    
    fun refreshBankStatistics() {
        loadBankStatistics()
    }
    
    fun refreshRevenueProjection() {
        loadRevenueProjection()
    }
    
    fun refreshSLAMetrics() {
        loadSLAMetrics()
    }
    
    fun refreshTeamPerformance() {
        loadTeamPerformance()
    }
}
