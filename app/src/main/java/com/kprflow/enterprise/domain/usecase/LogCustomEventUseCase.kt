package com.kprflow.enterprise.domain.usecase

import com.kprflow.enterprise.domain.repository.MonitoringRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogCustomEventUseCase @Inject constructor(
    private val monitoringRepository: MonitoringRepository
) {
    suspend operator fun invoke(event: String, params: Map<String, Any>): Result<Unit> {
        return try {
            monitoringRepository.logCustomEvent(event, params)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
