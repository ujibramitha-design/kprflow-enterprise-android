package com.kprflow.enterprise.domain.usecase

import com.kprflow.enterprise.domain.model.SyncResult
import com.kprflow.enterprise.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForceSyncAllUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<SyncResult> {
        return try {
            val syncResult = syncRepository.forceSyncAll()
            syncResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
