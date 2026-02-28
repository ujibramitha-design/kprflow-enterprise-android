package com.kprflow.enterprise.domain.usecase

import com.kprflow.enterprise.domain.model.CrashStatistics
import com.kprflow.enterprise.domain.repository.MonitoringRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetCrashStatisticsUseCase @Inject constructor(
    private val monitoringRepository: MonitoringRepository
) {
    suspend operator fun invoke(): Result<CrashStatistics> {
        return try {
            val crashStats = monitoringRepository.getCrashStatistics()
            Result.success(crashStats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
