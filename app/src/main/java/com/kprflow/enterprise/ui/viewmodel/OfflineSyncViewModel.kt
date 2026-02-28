package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.domain.model.NetworkStatus
import com.kprflow.enterprise.domain.model.OfflineStatus
import com.kprflow.enterprise.domain.model.SyncResult
import com.kprflow.enterprise.domain.usecase.ForceSyncAllUseCase
import com.kprflow.enterprise.domain.usecase.GetNetworkStatusUseCase
import com.kprflow.enterprise.domain.usecase.GetOfflineStatusUseCase
import com.kprflow.enterprise.domain.usecase.LogCustomEventUseCase
import com.kprflow.enterprise.work.SyncWorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfflineSyncViewModel @Inject constructor(
    private val getOfflineStatusUseCase: GetOfflineStatusUseCase,
    private val getNetworkStatusUseCase: GetNetworkStatusUseCase,
    private val forceSyncAllUseCase: ForceSyncAllUseCase,
    private val logCustomEventUseCase: LogCustomEventUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OfflineSyncUiState())
    val uiState: StateFlow<OfflineSyncUiState> = _uiState.asStateFlow()
    
    init {
        observeOfflineStatus()
        observeNetworkStatus()
    }
    
    private fun observeOfflineStatus() {
        viewModelScope.launch {
            getOfflineStatusUseCase().collect { offlineStatus ->
                _uiState.value = _uiState.value.copy(
                    offlineStatus = offlineStatus,
                    syncStatus = when {
                        !offlineStatus.isOnline -> SyncStatus.Offline
                        offlineStatus.syncInProgress -> SyncStatus.Syncing
                        offlineStatus.pendingUploads > 0 -> SyncStatus.Online
                        else -> SyncStatus.Online
                    }
                )
            }
        }
    }
    
    private fun observeNetworkStatus() {
        viewModelScope.launch {
            getNetworkStatusUseCase().collect { networkStatus ->
                _uiState.value = _uiState.value.copy(
                    networkStatus = networkStatus
                )
                
                // Auto-sync when network becomes available
                if (networkStatus.isConnected && _uiState.value.offlineStatus.pendingUploads > 0) {
                    startAutoSync()
                }
            }
        }
    }
    
    fun manualSync() {
        viewModelScope.launch {
            if (!_uiState.value.offlineStatus.isOnline) {
                _uiState.value = _uiState.value.copy(
                    error = "Cannot sync while offline"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                error = null
            )
            
            try {
                logCustomEventUseCase("manual_sync_started", mapOf(
                    "pending_items" to _uiState.value.offlineStatus.pendingUploads
                ))
                
                val syncResult = forceSyncAllUseCase()
                
                when {
                    syncResult.isSuccess -> {
                        val result = syncResult.getOrNull()
                        logCustomEventUseCase("manual_sync_success", mapOf(
                            "synced_count" to (result?.syncedCount ?: 0),
                            "failed_count" to (result?.failedCount ?: 0)
                        ))
                        
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            syncResult = result,
                            showSuccessMessage = true
                        )
                    }
                    syncResult.isFailure -> {
                        val error = syncResult.exceptionOrNull()
                        logCustomEventUseCase("manual_sync_failed", mapOf(
                            "error_message" to (error?.message ?: "Unknown error")
                        ))
                        
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            error = "Sync failed: ${error?.message ?: "Unknown error"}"
                        )
                    }
                }
            } catch (e: Exception) {
                logCustomEventUseCase("manual_sync_exception", mapOf(
                    "error_message" to e.message ?: "Unknown exception"
                ))
                
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    error = "Sync failed: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }
    
    private fun startAutoSync() {
        viewModelScope.launch {
            if (_uiState.value.offlineStatus.isOnline && 
                !_uiState.value.offlineStatus.syncInProgress &&
                _uiState.value.offlineStatus.pendingUploads > 0) {
                
                logCustomEventUseCase("auto_sync_triggered", mapOf(
                    "pending_items" to _uiState.value.offlineStatus.pendingUploads
                ))
                
                // Schedule background sync
                // This would be handled by WorkManager in real implementation
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun dismissSuccessMessage() {
        _uiState.value = _uiState.value.copy(showSuccessMessage = false)
    }
    
    fun refreshStatus() {
        viewModelScope.launch {
            // Trigger status refresh
            observeOfflineStatus()
            observeNetworkStatus()
        }
    }
}

data class OfflineSyncUiState(
    val offlineStatus: OfflineStatus = OfflineStatus(
        isOnline = false,
        totalEntities = 0,
        dirtyEntities = 0,
        pendingUploads = 0,
        lastSyncTime = null,
        syncInProgress = false
    ),
    val networkStatus: NetworkStatus = NetworkStatus(
        isConnected = false,
        networkType = com.kprflow.enterprise.domain.model.NetworkType.NONE,
        signalStrength = 0,
        isMetered = false,
        lastChecked = System.currentTimeMillis()
    ),
    val syncStatus: SyncStatus = SyncStatus.Offline,
    val isSyncing: Boolean = false,
    val syncResult: SyncResult? = null,
    val error: String? = null,
    val showSuccessMessage: Boolean = false
)

sealed class SyncStatus {
    object Syncing : SyncStatus()
    object Offline : SyncStatus()
    object Online : SyncStatus()
    object Error : SyncStatus()
}
