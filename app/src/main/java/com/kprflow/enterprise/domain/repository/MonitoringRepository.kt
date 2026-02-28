package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.domain.model.CrashStatistics
import com.kprflow.enterprise.domain.model.PerformanceMetrics
import com.kprflow.enterprise.domain.model.UserActivityMetrics

interface MonitoringRepository {
    suspend fun getCrashStatistics(): CrashStatistics
    suspend fun getPerformanceMetrics(): PerformanceMetrics
    suspend fun getUserActivityMetrics(): UserActivityMetrics
    suspend fun logError(error: Throwable, context: String)
    suspend fun logCustomEvent(event: String, params: Map<String, Any>)
    suspend fun trackUserAction(action: String, userId: String?)
    suspend fun recordPerformanceMetric(metric: String, value: Double, unit: String)
}
