package com.kprflow.enterprise.domain.usecase

import com.kprflow.enterprise.domain.model.NetworkStatus
import com.kprflow.enterprise.domain.model.NetworkType
import com.kprflow.enterprise.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetNetworkStatusUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(): Flow<NetworkStatus> {
        return flow {
            try {
                val status = syncRepository.getNetworkStatus()
                emit(status)
            } catch (e: Exception) {
                // Emit default status on error
                emit(NetworkStatus(
                    isConnected = false,
                    networkType = NetworkType.NONE,
                    signalStrength = 0,
                    isMetered = false,
                    lastChecked = System.currentTimeMillis()
                ))
            }
        }
    }
}
