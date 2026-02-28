package com.kprflow.enterprise.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceMonitoringRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    
    companion object {
        // Metric types
        const val METRIC_DATABASE = "DATABASE"
        const val METRIC_API = "API"
        const val METRIC_MEMORY = "MEMORY"
        const val METRIC_CPU = "CPU"
        const val METRIC_DISK = "DISK"
        const val METRIC_NETWORK = "NETWORK"
        const val METRIC_USER_ACTIVITY = "USER_ACTIVITY"
        const val METRIC_SYSTEM_HEALTH = "SYSTEM_HEALTH"
        
        // Alert levels
        const val ALERT_INFO = "INFO"
        const val ALERT_WARNING = "WARNING"
        const val ALERT_CRITICAL = "CRITICAL"
        
        // Performance thresholds
        const val API_RESPONSE_TIME_WARNING = 2000L // 2 seconds
        const val API_RESPONSE_TIME_CRITICAL = 5000L // 5 seconds
        const val DATABASE_QUERY_TIME_WARNING = 1000L // 1 second
        const val DATABASE_QUERY_TIME_CRITICAL = 3000L // 3 seconds
        const val MEMORY_USAGE_WARNING = 80.0 // 80%
        const val MEMORY_USAGE_CRITICAL = 95.0 // 95%
        const val CPU_USAGE_WARNING = 70.0 // 70%
        const val CPU_USAGE_CRITICAL = 90.0 // 90%
        const val DISK_USAGE_WARNING = 80.0 // 80%
        const val DISK_USAGE_CRITICAL = 95.0 // 95%
    }
    
    suspend fun recordDatabasePerformance(
        queryType: String,
        query: String,
        executionTime: Long,
        rowsAffected: Int,
        timestamp: Instant = Instant.now()
    ): Result<String> {
        return try {
            val metricData = mapOf(
                "metric_type" to METRIC_DATABASE,
                "query_type" to queryType,
                "query" to query,
                "execution_time" to executionTime,
                "rows_affected" to rowsAffected,
                "timestamp" to timestamp.toString()
            )
            
            val metric = postgrest.from("performance_metrics")
                .insert(metricData)
                .maybeSingle()
                .data
            
            // Check if performance threshold exceeded
            if (executionTime > DATABASE_QUERY_TIME_CRITICAL) {
                createPerformanceAlert(
                    metricType = METRIC_DATABASE,
                    level = ALERT_CRITICAL,
                    message = "Database query exceeded critical threshold: ${executionTime}ms",
                    details = mapOf(
                        "query_type" to queryType,
                        "query" to query,
                        "execution_time" to executionTime,
                        "threshold" to DATABASE_QUERY_TIME_CRITICAL
                    )
                )
            } else if (executionTime > DATABASE_QUERY_TIME_WARNING) {
                createPerformanceAlert(
                    metricType = METRIC_DATABASE,
                    level = ALERT_WARNING,
                    message = "Database query exceeded warning threshold: ${executionTime}ms",
                    details = mapOf(
                        "query_type" to queryType,
                        "query" to query,
                        "execution_time" to executionTime,
                        "threshold" to DATABASE_QUERY_TIME_WARNING
                    )
                )
            }
            
            metric?.let { 
                    Result.success(it.id)
                }
                ?: Result.failure(Exception("Failed to record database performance"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun recordAPIPerformance(
        endpoint: String,
        method: String,
        responseTime: Long,
        statusCode: Int,
        userId: String? = null,
        ipAddress: String? = null,
        timestamp: Instant = Instant.now()
    ): Result<String> {
        return try {
            val metricData = mapOf(
                "metric_type" to METRIC_API,
                "endpoint" to endpoint,
                "method" to method,
                "response_time" to responseTime,
                "status_code" to statusCode,
                "user_id" to userId,
                "ip_address" to ipAddress,
                "timestamp" to timestamp.toString()
            )
            
            val metric = postgrest.from("performance_metrics")
                .insert(metricData)
                .maybeSingle()
                .data
            
            // Check if API performance threshold exceeded
            if (responseTime > API_RESPONSE_TIME_CRITICAL) {
                createPerformanceAlert(
                    metricType = METRIC_API,
                    level = ALERT_CRITICAL,
                    message = "API response time exceeded critical threshold: ${responseTime}ms",
                    details = mapOf(
                        "endpoint" to endpoint,
                        "method" to method,
                        "response_time" to responseTime,
                        "status_code" to statusCode,
                        "threshold" to API_RESPONSE_TIME_CRITICAL
                    )
                )
            } else if (responseTime > API_RESPONSE_TIME_WARNING) {
                createPerformanceAlert(
                    metricType = METRIC_API,
                    level = ALERT_WARNING,
                    message = "API response time exceeded warning threshold: ${responseTime}ms",
                    details = mapOf(
                        "endpoint" to endpoint,
                        "method" to method,
                        "response_time" to responseTime,
                        "status_code" to statusCode,
                        "threshold" to API_RESPONSE_TIME_WARNING
                    )
                )
            }
            
            metric?.let { 
                    Result.success(it.id)
                }
                ?: Result.failure(Exception("Failed to record API performance"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun recordSystemMetrics(
        cpuUsage: Double,
        memoryUsage: Double,
        diskUsage: Double,
        networkIn: Long,
        networkOut: Long,
        timestamp: Instant = Instant.now()
    ): Result<String> {
        return try {
            val metricData = mapOf(
                "metric_type" to METRIC_SYSTEM_HEALTH,
                "cpu_usage" to cpuUsage,
                "memory_usage" to memoryUsage,
                "disk_usage" to diskUsage,
                "network_in" to networkIn,
                "network_out" to networkOut,
                "timestamp" to timestamp.toString()
            )
            
            val metric = postgrest.from("performance_metrics")
                .insert(metricData)
                .maybeSingle()
                .data
            
            // Check system health thresholds
            if (cpuUsage > CPU_USAGE_CRITICAL) {
                createPerformanceAlert(
                    metricType = METRIC_CPU,
                    level = ALERT_CRITICAL,
                    message = "CPU usage exceeded critical threshold: ${cpuUsage}%",
                    details = mapOf(
                        "cpu_usage" to cpuUsage,
                        "threshold" to CPU_USAGE_CRITICAL
                    )
                )
            } else if (cpuUsage > CPU_USAGE_WARNING) {
                createPerformanceAlert(
                    metricType = METRIC_CPU,
                    level = ALERT_WARNING,
                    message = "CPU usage exceeded warning threshold: ${cpuUsage}%",
                    details = mapOf(
                        "cpu_usage" to cpuUsage,
                        "threshold" to CPU_USAGE_WARNING
                    )
                )
            }
            
            if (memoryUsage > MEMORY_USAGE_CRITICAL) {
                createPerformanceAlert(
                    metricType = METRIC_MEMORY,
                    level = ALERT_CRITICAL,
                    message = "Memory usage exceeded critical threshold: ${memoryUsage}%",
                    details = mapOf(
                        "memory_usage" to memoryUsage,
                        "threshold" to MEMORY_USAGE_CRITICAL
                    )
                )
            } else if (memoryUsage > MEMORY_USAGE_WARNING) {
                createPerformanceAlert(
                    metricType = METRIC_MEMORY,
                    level = ALERT_WARNING,
                    message = "Memory usage exceeded warning threshold: ${memoryUsage}%",
                    details = mapOf(
                        "memory_usage" to memoryUsage,
                        "threshold" to MEMORY_USAGE_WARNING
                    )
                )
            }
            
            if (diskUsage > DISK_USAGE_CRITICAL) {
                createPerformanceAlert(
                    metricType = METRIC_DISK,
                    level = ALERT_CRITICAL,
                    message = "Disk usage exceeded critical threshold: ${diskUsage}%",
                    details = mapOf(
                        "disk_usage" to diskUsage,
                        "threshold" to DISK_USAGE_CRITICAL
                    )
                )
            } else if (diskUsage > DISK_USAGE_WARNING) {
                createPerformanceAlert(
                    metricType = METRIC_DISK,
                    level = ALERT_WARNING,
                    message = "Disk usage exceeded warning threshold: ${diskUsage}%",
                    details = mapOf(
                        "disk_usage" to diskUsage,
                        "threshold" to DISK_USAGE_WARNING
                    )
                )
            }
            
            metric?.let { 
                    Result.success(it.id)
                }
                ?: Result.failure(Exception("Failed to record system metrics"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun recordUserActivity(
        userId: String,
        activityType: String,
        sessionId: String,
        duration: Long,
        details: Map<String, Any>? = null,
        timestamp: Instant = Instant.now()
    ): Result<String> {
        return try {
            val metricData = mapOf(
                "metric_type" to METRIC_USER_ACTIVITY,
                "user_id" to userId,
                "activity_type" to activityType,
                "session_id" to sessionId,
                "duration" to duration,
                "details" to details,
                "timestamp" to timestamp.toString()
            )
            
            val metric = postgrest.from("performance_metrics")
                .insert(metricData)
                .maybeSingle()
                .data
            
            metric?.let { 
                    Result.success(it.id)
                }
                ?: Result.failure(Exception("Failed to record user activity"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPerformanceMetrics(
        metricType: String? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        limit: Int = 1000
    ): Result<List<PerformanceMetric>> {
        return try {
            var query = postgrest.from("performance_metrics")
                .select()
                .order("timestamp", ascending = false)
                .limit(limit)
            
            metricType?.let { query = query.filter { eq("metric_type", it) } }
            startDate?.let { query = query.filter { gte("timestamp", it.toString()) } }
            endDate?.let { query = query.filter { lte("timestamp", it.toString()) } }
            
            val metrics = query.data
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSystemHealthStatus(): Result<SystemHealthStatus> {
        return try {
            // Get latest system metrics
            val latestMetrics = postgrest.from("performance_metrics")
                .select()
                .filter { eq("metric_type", METRIC_SYSTEM_HEALTH) }
                .order("timestamp", ascending = false)
                .limit(1)
                .maybeSingle()
                .data
            
            // Get recent alerts
            val recentAlerts = postgrest.from("performance_alerts")
                .select()
                .filter { eq("is_resolved", false) }
                .order("created_at", ascending = false)
                .limit(10)
                .data
            
            // Calculate health score
            val healthScore = calculateHealthScore(latestMetrics, recentAlerts)
            
            val healthStatus = SystemHealthStatus(
                overallStatus = getOverallHealthStatus(healthScore),
                healthScore = healthScore,
                cpuUsage = latestMetrics?.cpu_usage ?: 0.0,
                memoryUsage = latestMetrics?.memory_usage ?: 0.0,
                diskUsage = latestMetrics?.disk_usage ?: 0.0,
                networkIn = latestMetrics?.network_in ?: 0L,
                networkOut = latestMetrics?.network_out ?: 0L,
                activeAlerts = recentAlerts.size,
                lastUpdated = latestMetrics?.timestamp ?: Instant.now().toString(),
                alerts = recentAlerts
            )
            
            Result.success(healthStatus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPerformanceReport(
        reportType: String,
        startDate: Instant,
        endDate: Instant
    ): Result<PerformanceReport> {
        return try {
            val metrics = getPerformanceMetrics(
                startDate = startDate,
                endDate = endDate,
                limit = 10000
            ).getOrNull().orEmpty()
            
            // Calculate statistics
            val apiMetrics = metrics.filter { it.metric_type == METRIC_API }
            val dbMetrics = metrics.filter { it.metric_type == METRIC_DATABASE }
            val systemMetrics = metrics.filter { it.metric_type == METRIC_SYSTEM_HEALTH }
            
            val report = PerformanceReport(
                id = java.util.UUID.randomUUID().toString(),
                reportType = reportType,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                totalMetrics = metrics.size,
                apiMetrics = apiMetrics.size,
                databaseMetrics = dbMetrics.size,
                systemMetrics = systemMetrics.size,
                averageApiResponseTime = apiMetrics.mapNotNull { it.response_time }.average().toLong(),
                averageDatabaseQueryTime = dbMetrics.mapNotNull { it.execution_time }.average().toLong(),
                averageCpuUsage = systemMetrics.mapNotNull { it.cpu_usage }.average(),
                averageMemoryUsage = systemMetrics.mapNotNull { it.memory_usage }.average(),
                averageDiskUsage = systemMetrics.mapNotNull { it.disk_usage }.average(),
                totalAlerts = metrics.count { it.has_alert == true },
                generatedAt = Instant.now().toString()
            )
            
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPerformanceAlerts(
        level: String? = null,
        resolved: Boolean? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        limit: Int = 100
    ): Result<List<PerformanceAlert>> {
        return try {
            var query = postgrest.from("performance_alerts")
                .select()
                .order("created_at", ascending = false)
                .limit(limit)
            
            level?.let { query = query.filter { eq("level", it) } }
            resolved?.let { query = query.filter { eq("is_resolved", it) } }
            startDate?.let { query = query.filter { gte("created_at", it.toString()) } }
            endDate?.let { query = query.filter { lte("created_at", it.toString()) } }
            
            val alerts = query.data
            Result.success(alerts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createPerformanceAlert(
        metricType: String,
        level: String,
        message: String,
        details: Map<String, Any>? = null
    ): Result<String> {
        return try {
            val alertData = mapOf(
                "metric_type" to metricType,
                "level" to level,
                "message" to message,
                "details" to details,
                "is_resolved" to false,
                "created_at" to Instant.now().toString()
            )
            
            val alert = postgrest.from("performance_alerts")
                .insert(alertData)
                .maybeSingle()
                .data
            
            alert?.let { 
                    Result.success(it.id)
                }
                ?: Result.failure(Exception("Failed to create performance alert"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resolvePerformanceAlert(
        alertId: String,
        resolvedBy: String,
        resolution: String? = null
    ): Result<Unit> {
        return try {
            val updateData = mapOf(
                "is_resolved" to true,
                "resolved_by" to resolvedBy,
                "resolved_at" to Instant.now().toString(),
                "resolution" to resolution
            )
            
            postgrest.from("performance_alerts")
                .update(updateData)
                .filter { eq("id", alertId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPerformanceTrends(
        metricType: String,
        period: String = "24h",
        granularity: String = "1h"
    ): Result<List<PerformanceTrend>> {
        return try {
            // Calculate time range based on period
            val endDate = Instant.now()
            val startDate = when (period) {
                "1h" -> endDate.minus(1, ChronoUnit.HOURS)
                "24h" -> endDate.minus(1, ChronoUnit.DAYS)
                "7d" -> endDate.minus(7, ChronoUnit.DAYS)
                "30d" -> endDate.minus(30, ChronoUnit.DAYS)
                else -> endDate.minus(1, ChronoUnit.DAYS)
            }
            
            val metrics = getPerformanceMetrics(
                metricType = metricType,
                startDate = startDate,
                endDate = endDate,
                limit = 1000
            ).getOrNull().orEmpty()
            
            // Aggregate metrics by time granularity
            val trends = aggregateMetricsByTime(metrics, granularity)
            
            Result.success(trends)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getBottleneckAnalysis(): Result<BottleneckAnalysis> {
        return try {
            // Get recent performance metrics
            val recentMetrics = getPerformanceMetrics(
                startDate = Instant.now().minus(24, ChronoUnit.HOURS),
                limit = 1000
            ).getOrNull().orEmpty()
            
            // Analyze bottlenecks
            val apiBottlenecks = analyzeAPIBottlenecks(recentMetrics)
            val databaseBottlenecks = analyzeDatabaseBottlenecks(recentMetrics)
            val systemBottlenecks = analyzeSystemBottlenecks(recentMetrics)
            
            val analysis = BottleneckAnalysis(
                apiBottlenecks = apiBottlenecks,
                databaseBottlenecks = databaseBottlenecks,
                systemBottlenecks = systemBottlenecks,
                recommendations = generateRecommendations(apiBottlenecks, databaseBottlenecks, systemBottlenecks),
                analyzedAt = Instant.now().toString()
            )
            
            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observePerformanceUpdates(): Flow<PerformanceUpdate> = flow {
        try {
            // TODO: Implement real-time updates via Supabase Realtime
            emit(PerformanceUpdate.MetricsUpdated)
        } catch (e: Exception) {
            emit(PerformanceUpdate.Error(e.message ?: "Unknown error"))
        }
    }
    
    // Private helper methods
    private fun calculateHealthScore(
        latestMetrics: PerformanceMetric?,
        recentAlerts: List<PerformanceAlert>
    ): Double {
        var score = 100.0
        
        latestMetrics?.let { metrics ->
            // Deduct points for high resource usage
            if (metrics.cpu_usage > CPU_USAGE_WARNING) score -= 10
            if (metrics.memory_usage > MEMORY_USAGE_WARNING) score -= 10
            if (metrics.disk_usage > DISK_USAGE_WARNING) score -= 10
            
            // Deduct more points for critical levels
            if (metrics.cpu_usage > CPU_USAGE_CRITICAL) score -= 20
            if (metrics.memory_usage > MEMORY_USAGE_CRITICAL) score -= 20
            if (metrics.disk_usage > DISK_USAGE_CRITICAL) score -= 20
        }
        
        // Deduct points for active alerts
        recentAlerts.forEach { alert ->
            when (alert.level) {
                ALERT_CRITICAL -> score -= 15
                ALERT_WARNING -> score -= 5
                else -> score -= 1
            }
        }
        
        return score.coerceIn(0.0, 100.0)
    }
    
    private fun getOverallHealthStatus(score: Double): String {
        return when {
            score >= 90 -> "EXCELLENT"
            score >= 75 -> "GOOD"
            score >= 60 -> "FAIR"
            score >= 40 -> "POOR"
            else -> "CRITICAL"
        }
    }
    
    private fun aggregateMetricsByTime(
        metrics: List<PerformanceMetric>,
        granularity: String
    ): List<PerformanceTrend> {
        // Simplified aggregation - would implement proper time-based grouping
        return metrics.map { metric ->
            PerformanceTrend(
                timestamp = metric.timestamp,
                value = when (metric.metric_type) {
                    METRIC_API -> metric.response_time?.toDouble() ?: 0.0
                    METRIC_DATABASE -> metric.execution_time?.toDouble() ?: 0.0
                    METRIC_SYSTEM_HEALTH -> metric.cpu_usage
                    else -> 0.0
                },
                count = 1
            )
        }
    }
    
    private fun analyzeAPIBottlenecks(metrics: List<PerformanceMetric>): List<Bottleneck> {
        return metrics
            .filter { it.metric_type == METRIC_API }
            .filter { (it.response_time ?: 0) > API_RESPONSE_TIME_WARNING }
            .groupBy { it.endpoint }
            .map { (endpoint, endpointMetrics) ->
                Bottleneck(
                    type = "API",
                    resource = endpoint,
                    averageResponseTime = endpointMetrics.mapNotNull { it.response_time }.average(),
                    maxResponseTime = endpointMetrics.mapNotNull { it.response_time }.maxOrNull() ?: 0L,
                    occurrenceCount = endpointMetrics.size,
                    severity = if (endpointMetrics.any { (it.response_time ?: 0) > API_RESPONSE_TIME_CRITICAL }) "HIGH" else "MEDIUM"
                )
            }
            .sortedByDescending { it.averageResponseTime }
    }
    
    private fun analyzeDatabaseBottlenecks(metrics: List<PerformanceMetric>): List<Bottleneck> {
        return metrics
            .filter { it.metric_type == METRIC_DATABASE }
            .filter { (it.execution_time ?: 0) > DATABASE_QUERY_TIME_WARNING }
            .groupBy { it.query_type }
            .map { (queryType, queryMetrics) ->
                Bottleneck(
                    type = "DATABASE",
                    resource = queryType,
                    averageResponseTime = queryMetrics.mapNotNull { it.execution_time }.average(),
                    maxResponseTime = queryMetrics.mapNotNull { it.execution_time }.maxOrNull() ?: 0L,
                    occurrenceCount = queryMetrics.size,
                    severity = if (queryMetrics.any { (it.execution_time ?: 0) > DATABASE_QUERY_TIME_CRITICAL }) "HIGH" else "MEDIUM"
                )
            }
            .sortedByDescending { it.averageResponseTime }
    }
    
    private fun analyzeSystemBottlenecks(metrics: List<PerformanceMetric>): List<Bottleneck> {
        val systemMetrics = metrics.filter { it.metric_type == METRIC_SYSTEM_HEALTH }
        val bottlenecks = mutableListOf<Bottleneck>()
        
        // CPU bottleneck
        val highCpuMetrics = systemMetrics.filter { it.cpu_usage > CPU_USAGE_WARNING }
        if (highCpuMetrics.isNotEmpty()) {
            bottlenecks.add(
                Bottleneck(
                    type = "SYSTEM",
                    resource = "CPU",
                    averageResponseTime = highCpuMetrics.map { it.cpu_usage }.average(),
                    maxResponseTime = highCpuMetrics.map { it.cpu_usage }.maxOrNull() ?: 0.0,
                    occurrenceCount = highCpuMetrics.size,
                    severity = if (highCpuMetrics.any { it.cpu_usage > CPU_USAGE_CRITICAL }) "HIGH" else "MEDIUM"
                )
            )
        }
        
        // Memory bottleneck
        val highMemoryMetrics = systemMetrics.filter { it.memory_usage > MEMORY_USAGE_WARNING }
        if (highMemoryMetrics.isNotEmpty()) {
            bottlenecks.add(
                Bottleneck(
                    type = "SYSTEM",
                    resource = "MEMORY",
                    averageResponseTime = highMemoryMetrics.map { it.memory_usage }.average(),
                    maxResponseTime = highMemoryMetrics.map { it.memory_usage }.maxOrNull() ?: 0.0,
                    occurrenceCount = highMemoryMetrics.size,
                    severity = if (highMemoryMetrics.any { it.memory_usage > MEMORY_USAGE_CRITICAL }) "HIGH" else "MEDIUM"
                )
            )
        }
        
        return bottlenecks.sortedByDescending { it.averageResponseTime }
    }
    
    private fun generateRecommendations(
        apiBottlenecks: List<Bottleneck>,
        databaseBottlenecks: List<Bottleneck>,
        systemBottlenecks: List<Bottleneck>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (apiBottlenecks.isNotEmpty()) {
            recommendations.add("Optimize slow API endpoints: ${apiBottlenecks.take(3).joinToString(", ") { it.resource }}")
        }
        
        if (databaseBottlenecks.isNotEmpty()) {
            recommendations.add("Add database indexes for slow queries: ${databaseBottlenecks.take(3).joinToString(", ") { it.resource }}")
        }
        
        if (systemBottlenecks.isNotEmpty()) {
            recommendations.add("Monitor and optimize resource usage: ${systemBottlenecks.joinToString(", ") { it.resource }}")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("System performance is within acceptable ranges")
        }
        
        return recommendations
    }
}

// Data classes
data class PerformanceMetric(
    val id: String,
    val metricType: String,
    val endpoint: String? = null,
    val method: String? = null,
    val responseTime: Long? = null,
    val statusCode: Int? = null,
    val queryType: String? = null,
    val query: String? = null,
    val executionTime: Long? = null,
    val rowsAffected: Int? = null,
    val cpuUsage: Double? = null,
    val memoryUsage: Double? = null,
    val diskUsage: Double? = null,
    val networkIn: Long? = null,
    val networkOut: Long? = null,
    val userId: String? = null,
    val activityType: String? = null,
    val sessionId: String? = null,
    val duration: Long? = null,
    val details: Map<String, Any>? = null,
    val timestamp: String,
    val hasAlert: Boolean = false
)

data class PerformanceAlert(
    val id: String,
    val metricType: String,
    val level: String,
    val message: String,
    val details: Map<String, Any>? = null,
    val isResolved: Boolean,
    val resolvedBy: String? = null,
    val resolvedAt: String? = null,
    val resolution: String? = null,
    val createdAt: String
)

data class SystemHealthStatus(
    val overallStatus: String,
    val healthScore: Double,
    val cpuUsage: Double,
    val memoryUsage: Double,
    val diskUsage: Double,
    val networkIn: Long,
    val networkOut: Long,
    val activeAlerts: Int,
    val lastUpdated: String,
    val alerts: List<PerformanceAlert>
)

data class PerformanceReport(
    val id: String,
    val reportType: String,
    val startDate: String,
    val endDate: String,
    val totalMetrics: Int,
    val apiMetrics: Int,
    val databaseMetrics: Int,
    val systemMetrics: Int,
    val averageApiResponseTime: Long,
    val averageDatabaseQueryTime: Long,
    val averageCpuUsage: Double,
    val averageMemoryUsage: Double,
    val averageDiskUsage: Double,
    val totalAlerts: Int,
    val generatedAt: String
)

data class PerformanceTrend(
    val timestamp: String,
    val value: Double,
    val count: Int
)

data class Bottleneck(
    val type: String,
    val resource: String,
    val averageResponseTime: Double,
    val maxResponseTime: Long,
    val occurrenceCount: Int,
    val severity: String
)

data class BottleneckAnalysis(
    val apiBottlenecks: List<Bottleneck>,
    val databaseBottlenecks: List<Bottleneck>,
    val systemBottlenecks: List<Bottleneck>,
    val recommendations: List<String>,
    val analyzedAt: String
)

sealed class PerformanceUpdate {
    object MetricsUpdated : PerformanceUpdate()
    object AlertTriggered : PerformanceUpdate()
    object HealthStatusChanged : PerformanceUpdate()
    data class Error(val message: String) : PerformanceUpdate()
}
