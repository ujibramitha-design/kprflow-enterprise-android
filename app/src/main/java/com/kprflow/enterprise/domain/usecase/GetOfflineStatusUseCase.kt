package com.kprflow.enterprise.domain.usecase

import com.kprflow.enterprise.domain.model.OfflineStatus
import com.kprflow.enterprise.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetOfflineStatusUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(): Flow<OfflineStatus> {
        return kotlinx.coroutines.flow.flow {
            try {
                val status = syncRepository.getOfflineStatus()
                emit(status)
            } catch (e: Exception) {
                // Emit default status on error
                emit(OfflineStatus(
                    isOnline = false,
                    totalEntities = 0,
                    dirtyEntities = 0,
                    pendingUploads = 0,
                    lastSyncTime = null,
                    syncInProgress = false
                ))
            }
        }
    }
}
