package com.kprflow.enterprise.domain.usecase

import com.kprflow.enterprise.domain.model.PerformanceMetrics
import com.kprflow.enterprise.domain.repository.MonitoringRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetPerformanceMetricsUseCase @Inject constructor(
    private val monitoringRepository: MonitoringRepository
) {
    suspend operator fun invoke(): Result<PerformanceMetrics> {
        return try {
            val metrics = monitoringRepository.getPerformanceMetrics()
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
