package com.kprflow.enterprise.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.kprflow.enterprise.data.datasource.MonitoringDataSource
import com.kprflow.enterprise.domain.model.CrashStatistics
import com.kprflow.enterprise.domain.model.PerformanceMetrics
import com.kprflow.enterprise.domain.model.UserActivityMetrics
import com.kprflow.enterprise.domain.repository.MonitoringRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoringRepositoryImpl @Inject constructor(
    private val crashlytics: FirebaseCrashlytics,
    private val analytics: FirebaseAnalytics,
    private val monitoringDataSource: MonitoringDataSource
) : MonitoringRepository {

    override suspend fun getCrashStatistics(): CrashStatistics {
        return try {
            monitoringDataSource.getCrashStatistics()
        } catch (e: Exception) {
            logError(e, "getCrashStatistics")
            CrashStatistics(
                totalCrashes = 0,
                crashRate = 0.0,
                mostAffectedVersion = "",
                topCrashReasons = emptyList(),
                crashesByDevice = emptyList(),
                crashesByOSVersion = emptyList(),
                averageRecoveryTime = 0L,
                userImpactScore = 0.0
            )
        }
    }

    override suspend fun getPerformanceMetrics(): PerformanceMetrics {
        return try {
            monitoringDataSource.getPerformanceMetrics()
        } catch (e: Exception) {
            logError(e, "getPerformanceMetrics")
            PerformanceMetrics(
                appStartupTime = 0L,
                screenLoadTime = 0L,
                apiResponseTime = 0L,
                memoryUsage = 0L,
                batteryUsage = 0.0,
                networkLatency = 0L,
                cpuUsage = 0.0
            )
        }
    }

    override suspend fun getUserActivityMetrics(): UserActivityMetrics {
        return try {
            monitoringDataSource.getUserActivityMetrics()
        } catch (e: Exception) {
            logError(e, "getUserActivityMetrics")
            UserActivityMetrics(
                activeUsers = 0,
                totalSessions = 0,
                averageSessionDuration = 0L,
                retentionRate = 0.0,
                churnRate = 0.0,
                dailyActiveUsers = 0,
                weeklyActiveUsers = 0,
                monthlyActiveUsers = 0
            )
        }
    }

    override suspend fun logError(error: Throwable, context: String) {
        try {
            crashlytics.recordException(
                RuntimeException("$context: ${error.message}", error)
            )
            
            crashlytics.log("$context: ${error.message}")
            
            analytics.logEvent("app_error") {
                param("error_context", context)
                param("error_message", error.message ?: "Unknown error")
                param("error_type", error::class.simpleName ?: "Unknown")
            }
        } catch (e: Exception) {
            // Fail silently to avoid infinite loops
        }
    }

    override suspend fun logCustomEvent(event: String, params: Map<String, Any>) {
        try {
            crashlytics.log("$event: ${params.entries.joinToString()}")
            
            analytics.logEvent(event) {
                params.forEach { (key, value) ->
                    param(key, value.toString())
                }
            }
        } catch (e: Exception) {
            // Fail silently to avoid infinite loops
        }
    }

    override suspend fun trackUserAction(action: String, userId: String?) {
        try {
            analytics.logEvent("user_action") {
                param("action_type", action)
                param("user_id", userId ?: "anonymous")
                param("timestamp", System.currentTimeMillis().toString())
            }
            
            userId?.let { crashlytics.setUserId(it) }
        } catch (e: Exception) {
            // Fail silently to avoid infinite loops
        }
    }

    override suspend fun recordPerformanceMetric(metric: String, value: Double, unit: String) {
        try {
            analytics.logEvent("performance_metric") {
                param("metric_name", metric)
                param("metric_value", value.toString())
                param("metric_unit", unit)
                param("timestamp", System.currentTimeMillis().toString())
            }
            
            crashlytics.setCustomKey(metric, value.toString())
        } catch (e: Exception) {
            // Fail silently to avoid infinite loops
        }
    }
}
