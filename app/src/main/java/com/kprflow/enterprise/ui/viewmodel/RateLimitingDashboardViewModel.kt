package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.RateLimitingRepository
import com.kprflow.enterprise.ui.screens.RateLimitState
import com.kprflow.enterprise.ui.screens.RateLimitingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RateLimitingDashboardViewModel @Inject constructor(
    private val rateLimitingRepository: RateLimitingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RateLimitingUiState())
    val uiState: StateFlow<RateLimitingUiState> = _uiState.asStateFlow()
    
    private val _statisticsState = MutableStateFlow<RateLimitState<com.kprflow.enterprise.data.repository.RateLimitStatistics>>(RateLimitState.Loading)
    val statisticsState: StateFlow<RateLimitState<com.kprflow.enterprise.data.repository.RateLimitStatistics>> = _statisticsState.asStateState()
    
    private val _violatorsState = MutableStateFlow<RateLimitState<List<com.kprflow.enterprise.data.repository.TopViolator>>>(RateLimitState.Loading)
    val violatorsState: StateFlow<RateLimitState<List<com.kprflow.enterprise.data.repository.TopViolator>>> = _violatorsState.asStateState()
    
    private val _configsState = MutableStateFlow<RateLimitState<List<com.kprflow.enterprise.data.repository.RateLimitConfig>>>(RateLimitState.Loading)
    val configsState: StateFlow<RateLimitState<List<com.kprflow.enterprise.data.repository.RateLimitConfig>>> = _configsState.asStateState()
    
    fun loadStatistics() {
        viewModelScope.launch {
            _statisticsState.value = RateLimitState.Loading
            
            try {
                val statistics = rateLimitingRepository.getRateLimitStatistics()
                    .getOrNull()
                
                if (statistics != null) {
                    _statisticsState.value = RateLimitState.Success(statistics)
                } else {
                    _statisticsState.value = RateLimitState.Error("Failed to load statistics")
                }
            } catch (e: Exception) {
                _statisticsState.value = RateLimitState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun loadTopViolators(
        limit: Int = 10
    ) {
        viewModelScope.launch {
            _violatorsState.value = RateLimitState.Loading
            
            try {
                val violators = rateLimitingRepository.getTopViolators(limit)
                    .getOrNull().orEmpty()
                
                _violatorsState.value = RateLimitState.Success(violators)
            } catch (e: Exception) {
                _violatorsState.value = RateLimitState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun loadConfigs() {
        viewModelScope.launch {
            _configsState.value = RateLimitState.Loading
            
            try {
                val configs = mutableListOf<com.kprflow.enterprise.data.repository.RateLimitConfig>()
                
                // Load all config types
                val configTypes = listOf("USER", "IP", "API_KEY", "ENDPOINT")
                
                configTypes.forEach { type ->
                    val config = rateLimitingRepository.getRateLimitConfig(type)
                        .getOrNull()
                    config?.let { configs.add(it) }
                }
                
                _configsState.value = RateLimitState.Success(configs)
            } catch (e: Exception) {
                _configsState.value = RateLimitState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun blockIdentifier(
        identifier: String,
        limitType: String,
        reason: String = "",
        durationMinutes: Int = 60
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = rateLimitingRepository.blockIdentifier(
                    identifier = identifier,
                    limitType = limitType,
                    reason = reason,
                    blockDurationMinutes = durationMinutes
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    
                    // Refresh violators list
                    loadTopViolators()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to block identifier"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to block identifier: ${e.message}"
                )
            }
        }
    }
    
    fun unblockIdentifier(
        identifier: String,
        limitType: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = rateLimitingRepository.unblockIdentifier(
                    identifier = identifier,
                    limitType = limitType
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    
                    // Refresh violators list
                    loadTopViolators()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to unblock identifier"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to unblock identifier: ${e.message}"
                )
            }
        }
    }
    
    fun editConfig(config: com.kprflow.enterprise.data.repository.RateLimitConfig) {
        _uiState.value = _uiState.value.copy(
            showConfigDialog = true,
            selectedConfig = config
        )
    }
    
    fun saveConfig(config: com.kprflow.enterprise.data.repository.RateLimitConfig) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = rateLimitingRepository.updateRateLimitConfig(config)
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        showConfigDialog = false,
                        selectedConfig = null
                    )
                    
                    // Refresh configs list
                    loadConfigs()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to save config"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to save config: ${e.message}"
                )
            }
        }
    }
    
    fun showConfigDialog() {
        _uiState.value = _uiState.value.copy(showConfigDialog = true)
    }
    
    fun hideConfigDialog() {
        _uiState.value = _uiState.value.copy(
            showConfigDialog = false,
            selectedConfig = null
        )
    }
    
    fun showBlockDialog() {
        _uiState.value = _uiState.value.copy(showBlockDialog = true)
    }
    
    fun hideBlockDialog() {
        _uiState.value = _uiState.value.copy(showBlockDialog = false)
    }
    
    fun refreshAllData() {
        loadStatistics()
        loadTopViolators()
        loadConfigs()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun testRateLimit(
        identifier: String,
        limitType: String,
        maxRequests: Int = 10,
        windowMinutes: Int = 1
    ) {
        viewModelScope.launch {
            try {
                // Test rate limit by making multiple requests
                var allowedCount = 0
                var blockedCount = 0
                
                for (i in 1..(maxRequests + 5)) {
                    val result = rateLimitingRepository.checkRateLimit(
                        identifier = identifier,
                        limitType = limitType,
                        maxRequests = maxRequests,
                        windowMinutes = windowMinutes
                    )
                    
                    if (result.isSuccess) {
                        val rateLimitResult = result.getOrNull()
                        if (rateLimitResult?.allowed == true) {
                            allowedCount++
                        } else {
                            blockedCount++
                        }
                    }
                }
                
                // Refresh statistics to show test results
                loadStatistics()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to test rate limit: ${e.message}")
            }
        }
    }
    
    fun cleanupOldLogs(daysOld: Int = 7) {
        viewModelScope.launch {
            try {
                // TODO: Implement cleanup function
                // This would call a cleanup function in the repository
                _uiState.value = _uiState.value.copy(
                    error = null
                )
                
                // Refresh statistics
                loadStatistics()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to cleanup logs: ${e.message}")
            }
        }
    }
    
    fun exportStatistics() {
        viewModelScope.launch {
            try {
                // TODO: Implement export functionality
                // This would generate a CSV or JSON export of statistics
                _uiState.value = _uiState.value.copy(
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to export statistics: ${e.message}")
            }
        }
    }
    
    fun getRateLimitConfig(limitType: String) {
        viewModelScope.launch {
            try {
                val config = rateLimitingRepository.getRateLimitConfig(limitType)
                    .getOrNull()
                
                if (config != null) {
                    // Update the config in the list
                    val currentConfigs = when (val state = _configsState.value) {
                        is RateLimitState.Success -> state.data.toMutableList()
                        else -> return@launch
                    }
                    
                    val index = currentConfigs.indexOfFirst { it.limitType == limitType }
                    if (index >= 0) {
                        currentConfigs[index] = config
                        _configsState.value = RateLimitState.Success(currentConfigs)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to get config: ${e.message}")
            }
        }
    }
}
