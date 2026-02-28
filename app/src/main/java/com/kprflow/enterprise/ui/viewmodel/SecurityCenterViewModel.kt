package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.domain.model.SecurityMetrics
import com.kprflow.enterprise.domain.model.SecurityRecommendation
import com.kprflow.enterprise.domain.model.SecurityViolation
import com.kprflow.enterprise.domain.usecase.GetSecurityMetricsUseCase
import com.kprflow.enterprise.domain.usecase.GetSecurityViolationsUseCase
import com.kprflow.enterprise.domain.usecase.ResolveSecurityViolationUseCase
import com.kprflow.enterprise.domain.usecase.ImplementSecurityRecommendationUseCase
import com.kprflow.enterprise.domain.usecase.LogSecurityEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityCenterViewModel @Inject constructor(
    private val getSecurityMetricsUseCase: GetSecurityMetricsUseCase,
    private val getSecurityViolationsUseCase: GetSecurityViolationsUseCase,
    private val resolveSecurityViolationUseCase: ResolveSecurityViolationUseCase,
    private val implementSecurityRecommendationUseCase: ImplementSecurityRecommendationUseCase,
    private val logSecurityEventUseCase: LogSecurityEventUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SecurityCenterUiState())
    val uiState: StateFlow<SecurityCenterUiState> = _uiState.asStateFlow()
    
    init {
        loadSecurityData()
    }
    
    fun loadSecurityData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Log security center access
                logSecurityEventUseCase(
                    eventType = "SECURITY_CENTER_ACCESSED",
                    details = mapOf(
                        "timestamp" to System.currentTimeMillis()
                    )
                )
                
                // Load security metrics
                val endTime = System.currentTimeMillis()
                val startTime = endTime - (24 * 60 * 60 * 1000) // Last 24 hours
                
                val metrics = getSecurityMetricsUseCase(startTime, endTime).getOrNull()
                
                // Load recent violations
                val violations = getSecurityViolationsUseCase(
                    resolved = false,
                    limit = 10
                ).getOrNull()?.toList() ?: emptyList()
                
                // Calculate security status
                val securityStatus = calculateSecurityStatus(metrics, violations)
                
                // Generate recommendations
                val recommendations = generateRecommendations(metrics, violations)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    securityMetrics = metrics,
                    recentViolations = violations,
                    securityStatus = securityStatus,
                    recommendations = recommendations,
                    lastSecurityScan = System.currentTimeMillis(),
                    threatsDetected = violations.count { !it.resolved }
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load security data: ${e.message}"
                )
                
                logSecurityEventUseCase(
                    eventType = "SECURITY_DATA_LOAD_FAILED",
                    details = mapOf(
                        "error" to e.message
                    )
                )
            }
        }
    }
    
    fun refreshData() {
        loadSecurityData()
    }
    
    fun resolveViolation(violationId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = resolveSecurityViolationUseCase(violationId)
                
                if (result.isSuccess) {
                    logSecurityEventUseCase(
                        eventType = "SECURITY_VIOLATION_RESOLVED",
                        details = mapOf(
                            "violation_id" to violationId,
                            "resolved_by" to "current_user"
                        )
                    )
                    
                    // Refresh data to show updated state
                    loadSecurityData()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to resolve violation: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to resolve violation: ${e.message}"
                )
            }
        }
    }
    
    fun implementRecommendation(recommendationId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = implementSecurityRecommendationUseCase(recommendationId)
                
                if (result.isSuccess) {
                    logSecurityEventUseCase(
                        eventType = "SECURITY_RECOMMENDATION_IMPLEMENTED",
                        details = mapOf(
                            "recommendation_id" to recommendationId,
                            "implemented_by" to "current_user"
                        )
                    )
                    
                    // Refresh data to show updated state
                    loadSecurityData()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to implement recommendation: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to implement recommendation: ${e.message}"
                )
            }
        }
    }
    
    private fun calculateSecurityStatus(
        metrics: SecurityMetrics?,
        violations: List<SecurityViolation>
    ): SecurityStatus {
        if (metrics == null) return SecurityStatus.Unknown
        
        val criticalEvents = metrics.criticalEvents
        val unresolvedViolations = violations.count { !it.resolved }
        val authFailureRate = if (metrics.authenticationAttempts > 0) {
            metrics.authenticationFailures.toDouble() / metrics.authenticationAttempts
        } else 0.0
        
        return when {
            criticalEvents > 0 || unresolvedViolations > 5 -> SecurityStatus.AtRisk
            authFailureRate > 0.1 || unresolvedViolations > 2 -> SecurityStatus.Warning
            authFailureRate < 0.05 && unresolvedViolations == 0 -> SecurityStatus.Secure
            else -> SecurityStatus.Warning
        }
    }
    
    private fun generateRecommendations(
        metrics: SecurityMetrics?,
        violations: List<SecurityViolation>
    ): List<SecurityRecommendation> {
        val recommendations = mutableListOf<SecurityRecommendation>()
        
        if (metrics == null) {
            recommendations.add(
                SecurityRecommendation(
                    id = "enable_monitoring",
                    category = com.kprflow.enterprise.domain.model.SecurityCategory.AUDITING,
                    title = "Enable Security Monitoring",
                    description = "Configure comprehensive security monitoring and alerting",
                    priority = com.kprflow.enterprise.domain.model.SecurityPriority.HIGH
                )
            )
            return recommendations
        }
        
        // Auth failure recommendations
        if (metrics.authenticationFailures > 10) {
            recommendations.add(
                SecurityRecommendation(
                    id = "strengthen_auth",
                    category = com.kprflow.enterprise.domain.model.SecurityCategory.AUTHENTICATION,
                    title = "Strengthen Authentication",
                    description = "Implement multi-factor authentication and reduce failed attempt thresholds",
                    priority = com.kprflow.enterprise.domain.model.SecurityPriority.HIGH
                )
            )
        }
        
        // Data access recommendations
        if (metrics.dataAccessDenials > metrics.dataAccessAttempts * 0.1) {
            recommendations.add(
                SecurityRecommendation(
                    id = "review_permissions",
                    category = com.kprflow.enterprise.domain.model.SecurityCategory.AUTHORIZATION,
                    title = "Review Access Permissions",
                    description = "Review and update user access permissions to reduce access denials",
                    priority = com.kprflow.enterprise.domain.model.SecurityPriority.MEDIUM
                )
            )
        }
        
        // Encryption recommendations
        if (metrics.encryptionOperations == 0) {
            recommendations.add(
                SecurityRecommendation(
                    id = "enable_encryption",
                    category = com.kprflow.enterprise.domain.model.SecurityCategory.ENCRYPTION,
                    title = "Enable Data Encryption",
                    description = "Implement encryption for sensitive data at rest and in transit",
                    priority = com.kprflow.enterprise.domain.model.SecurityPriority.CRITICAL
                )
            )
        }
        
        // Violation-based recommendations
        val violationTypes = violations.groupBy { it.violationType }
        violationTypes.forEach { (type, violations) ->
            if (violations.size > 2) {
                recommendations.add(
                    SecurityRecommendation(
                        id = "address_${type.name.lowercase()}",
                        category = com.kprflow.enterprise.domain.model.SecurityCategory.COMPLIANCE,
                        title = "Address ${type.name} Violations",
                        description = "Investigate and resolve recurring ${type.name} security violations",
                        priority = com.kprflow.enterprise.domain.model.SecurityPriority.HIGH
                    )
                )
            }
        }
        
        return recommendations
    }
}

data class SecurityCenterUiState(
    val isLoading: Boolean = false,
    val securityMetrics: SecurityMetrics? = null,
    val recentViolations: List<SecurityViolation> = emptyList(),
    val securityStatus: SecurityStatus = SecurityStatus.Unknown,
    val recommendations: List<SecurityRecommendation> = emptyList(),
    val lastSecurityScan: Long = 0L,
    val threatsDetected: Int = 0,
    val error: String? = null
)

sealed class SecurityStatus {
    object Secure : SecurityStatus()
    object Warning : SecurityStatus()
    object AtRisk : SecurityStatus()
    object Unknown : SecurityStatus()
}
