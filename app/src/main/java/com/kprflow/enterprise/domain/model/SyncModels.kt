package com.kprflow.enterprise.domain.model

data class SyncResult(
    val syncedCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val duration: Long,
    val timestamp: Long,
    val errors: List<SyncError>
)

data class SyncError(
    val entityType: String,
    val entityId: String,
    val operation: String,
    val errorMessage: String,
    val retryCount: Int
)

data class OfflineStatus(
    val isOnline: Boolean,
    val totalEntities: Int,
    val dirtyEntities: Int,
    val pendingUploads: Int,
    val lastSyncTime: Long?,
    val syncInProgress: Boolean
)

data class NetworkStatus(
    val isConnected: Boolean,
    val networkType: NetworkType,
    val signalStrength: Int,
    val isMetered: Boolean,
    val lastChecked: Long
)

enum class NetworkType {
    NONE,
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN
}

data class SyncProgress(
    val currentEntity: String,
    val currentOperation: String,
    val progress: Double, // 0.0 to 1.0
    val totalEntities: Int,
    val processedEntities: Int,
    val failedEntities: Int
)

data class ConflictResolution(
    val entityId: String,
    val entityType: String,
    val localVersion: String,
    val remoteVersion: String,
    val resolution: ConflictResolutionStrategy,
    val resolvedAt: Long
)

enum class ConflictResolutionStrategy {
    LOCAL_WINS,
    REMOTE_WINS,
    MERGE,
    MANUAL
}

data class SyncQueueItem(
    val id: String,
    val entityType: String,
    val entityId: String,
    val operation: SyncOperation,
    val priority: SyncPriority,
    val retryCount: Int,
    val maxRetries: Int,
    val nextRetryAt: Long,
    val status: SyncStatus,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long
)

enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE,
    UPLOAD
}

enum class SyncPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

enum class SyncStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}
