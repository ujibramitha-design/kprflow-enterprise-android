package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.AnalyticsData
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Analytics Repository
 * Following Clean Architecture principles for testability
 */
interface IAnalyticsRepository {
    suspend fun getAnalyticsData(timeRange: String): Result<AnalyticsData>
    suspend fun getUserMetrics(userId: String): Result<Map<String, Any>>
    suspend fun getSystemMetrics(): Result<Map<String, Any>>
    fun observeRealTimeAnalytics(): Flow<AnalyticsData>
    suspend fun generateReport(reportType: String, parameters: Map<String, Any>): Result<String>
    
    // Additional methods for existing AnalyticsRepository
    suspend fun getKPRPipelineFunnel(startDate: java.time.LocalDate? = null, endDate: java.time.LocalDate? = null): Result<Any>
    suspend fun getRevenueMetrics(timeRange: String): Result<Any>
    suspend fun getConversionMetrics(timeRange: String): Result<Any>
    suspend fun getSLAComplianceMetrics(): Result<Any>
}
