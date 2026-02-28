package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.domain.model.SyncResult
import com.kprflow.enterprise.domain.model.OfflineStatus
import com.kprflow.enterprise.domain.model.NetworkStatus

interface SyncRepository {
    suspend fun performSync(): Result<SyncResult>
    suspend fun syncDossier(dossierId: String): Result<Unit>
    suspend fun syncDocument(documentId: String): Result<Unit>
    suspend fun uploadDocument(documentId: String): Result<Unit>
    suspend fun syncLocation(dossierId: String, latitude: Double, longitude: Double, timestamp: Long): Result<Unit>
    suspend fun getOfflineStatus(): OfflineStatus
    suspend fun getNetworkStatus(): NetworkStatus
    suspend fun markEntityAsDirty(entityType: String, entityId: String)
    suspend fun markEntityAsSynced(entityType: String, entityId: String)
    suspend fun getPendingSyncCount(): Int
    suspend fun forceSyncAll(): Result<SyncResult>
}
