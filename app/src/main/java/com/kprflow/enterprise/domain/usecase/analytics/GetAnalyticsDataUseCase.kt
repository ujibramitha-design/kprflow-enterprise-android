package com.kprflow.enterprise.domain.usecase.analytics

import com.kprflow.enterprise.data.model.AnalyticsData
import com.kprflow.enterprise.domain.repository.IAnalyticsRepository
import com.kprflow.enterprise.domain.usecase.auth.GetCurrentUserUseCase
import javax.inject.Inject

/**
 * Use Case for Getting Analytics Data
 * Following Clean Architecture - business logic in domain layer
 */
class GetAnalyticsDataUseCase @Inject constructor(
    private val analyticsRepository: IAnalyticsRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) {
    suspend operator fun invoke(timeRange: String): Result<AnalyticsData> {
        // Business logic validation
        if (timeRange.isBlank()) {
            return Result.failure(IllegalArgumentException("Time range cannot be empty"))
        }
        
        val validTimeRanges = listOf("7d", "30d", "90d", "1y")
        if (timeRange !in validTimeRanges) {
            return Result.failure(IllegalArgumentException("Invalid time range. Use: 7d, 30d, 90d, 1y"))
        }
        
        return analyticsRepository.getAnalyticsData(timeRange)
    }
}
