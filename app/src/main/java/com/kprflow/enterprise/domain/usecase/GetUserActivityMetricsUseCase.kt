package com.kprflow.enterprise.domain.usecase

import com.kprflow.enterprise.domain.model.UserActivityMetrics
import com.kprflow.enterprise.domain.repository.MonitoringRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUserActivityMetricsUseCase @Inject constructor(
    private val monitoringRepository: MonitoringRepository
) {
    suspend operator fun invoke(): Result<UserActivityMetrics> {
        return try {
            val metrics = monitoringRepository.getUserActivityMetrics()
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
