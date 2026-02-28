package com.kprflow.enterprise.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.kprflow.enterprise.domain.repository.SyncRepository
import com.kprflow.enterprise.domain.usecase.LogCustomEventUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val logCustomEventUseCase: LogCustomEventUseCase
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            logCustomEventUseCase("sync_worker_started", mapOf(
                "worker_id" to id.toString(),
                "run_attempt_count" to runAttemptCount.toString()
            ))
            
            // Perform sync operations
            val syncResult = syncRepository.performSync()
            
            when {
                syncResult.isSuccess -> {
                    logCustomEventUseCase("sync_worker_success", mapOf(
                        "synced_items" to (syncResult.getOrNull()?.syncedCount ?: 0),
                        "failed_items" to (syncResult.getOrNull()?.failedCount ?: 0)
                    ))
                    Result.success()
                }
                syncResult.isFailure -> {
                    val error = syncResult.exceptionOrNull()
                    logCustomEventUseCase("sync_worker_failed", mapOf(
                        "error_message" to (error?.message ?: "Unknown error"),
                        "run_attempt_count" to runAttemptCount.toString()
                    ))
                    
                    // Retry with exponential backoff
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
                else -> Result.failure()
            }
        } catch (e: Exception) {
            logCustomEventUseCase("sync_worker_exception", mapOf(
                "error_message" to e.message ?: "Unknown exception",
                "run_attempt_count" to runAttemptCount.toString()
            ))
            Result.failure()
        }
    }
}

@HiltWorker
class DocumentUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val logCustomEventUseCase: LogCustomEventUseCase
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val documentId = inputData.getString("document_id")
                ?: return Result.failure()
            
            logCustomEventUseCase("document_upload_started", mapOf(
                "document_id" to documentId,
                "worker_id" to id.toString()
            ))
            
            val uploadResult = syncRepository.uploadDocument(documentId)
            
            when {
                uploadResult.isSuccess -> {
                    logCustomEventUseCase("document_upload_success", mapOf(
                        "document_id" to documentId
                    ))
                    Result.success()
                }
                uploadResult.isFailure -> {
                    val error = uploadResult.exceptionOrNull()
                    logCustomEventUseCase("document_upload_failed", mapOf(
                        "document_id" to documentId,
                        "error_message" to (error?.message ?: "Unknown error")
                    ))
                    
                    if (runAttemptCount < 5) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
                else -> Result.failure()
            }
        } catch (e: Exception) {
            logCustomEventUseCase("document_upload_exception", mapOf(
                "error_message" to e.message ?: "Unknown exception"
            ))
            Result.failure()
        }
    }
}

@HiltWorker
class LocationSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val logCustomEventUseCase: LogCustomEventUseCase
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val dossierId = inputData.getString("dossier_id")
            val latitude = inputData.getDouble("latitude", 0.0)
            val longitude = inputData.getDouble("longitude", 0.0)
            val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())
            
            logCustomEventUseCase("location_sync_started", mapOf(
                "dossier_id" to dossierId,
                "latitude" to latitude,
                "longitude" to longitude
            ))
            
            val locationResult = syncRepository.syncLocation(
                dossierId = dossierId,
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp
            )
            
            when {
                locationResult.isSuccess -> {
                    logCustomEventUseCase("location_sync_success", mapOf(
                        "dossier_id" to dossierId
                    ))
                    Result.success()
                }
                locationResult.isFailure -> {
                    val error = locationResult.exceptionOrNull()
                    logCustomEventUseCase("location_sync_failed", mapOf(
                        "dossier_id" to dossierId,
                        "error_message" to (error?.message ?: "Unknown error")
                    ))
                    
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
                else -> Result.failure()
            }
        } catch (e: Exception) {
            logCustomEventUseCase("location_sync_exception", mapOf(
                "error_message" to e.message ?: "Unknown exception"
            ))
            Result.failure()
        }
    }
}

object SyncWorkManager {
    
    fun schedulePeriodicSync(context: Context) {
        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, // Repeat interval (15 minutes)
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "periodic_sync",
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicSyncRequest
            )
    }
    
    fun scheduleDocumentUpload(context: Context, documentId: String) {
        val uploadRequest = OneTimeWorkRequestBuilder<DocumentUploadWorker>()
            .setInputData(
                workDataOf("document_id" to documentId)
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "upload_$documentId",
                ExistingWorkPolicy.REPLACE,
                uploadRequest
            )
    }
    
    fun scheduleLocationSync(
        context: Context,
        dossierId: String,
        latitude: Double,
        longitude: Double,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val locationRequest = OneTimeWorkRequestBuilder<LocationSyncWorker>()
            .setInputData(
                workDataOf(
                    "dossier_id" to dossierId,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "timestamp" to timestamp
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "location_$dossierId",
                ExistingWorkPolicy.REPLACE,
                locationRequest
            )
    }
    
    fun cancelSyncWork(context: Context, workName: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }
    
    fun cancelAllSyncWork(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("sync")
    }
}
