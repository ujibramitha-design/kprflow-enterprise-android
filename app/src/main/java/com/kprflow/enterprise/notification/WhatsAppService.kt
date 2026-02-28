package com.kprflow.enterprise.notification

import android.content.Context
import com.kprflow.enterprise.data.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository
) {
    
    suspend fun sendNotification(
        userId: String? = null,
        phoneNumber: String? = null,
        title: String,
        message: String,
        type: NotificationType,
        data: Map<String, Any>? = null,
        referenceId: String? = null
    ): Flow<Result<NotificationResult>> = flow {
        try {
            val result = notificationRepository.sendWhatsAppNotification(
                userId = userId,
                phoneNumber = phoneNumber,
                title = title,
                message = message,
                type = type.name,
                data = data,
                referenceId = referenceId
            )
            
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun sendBulkNotifications(
        notifications: List<WhatsAppNotification>
    ): Flow<Result<BulkNotificationResult>> = flow {
        try {
            val result = notificationRepository.sendBulkWhatsAppNotifications(notifications)
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getNotificationHistory(
        userId: String? = null,
        type: NotificationType? = null,
        limit: Int = 50
    ): Flow<Result<List<NotificationLog>>> = flow {
        try {
            val logs = notificationRepository.getNotificationLogs(
                userId = userId,
                type = type?.name,
                limit = limit
            )
            emit(Result.success(logs))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getNotificationStats(
        startDate: Long = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L), // 30 days ago
        endDate: Long = System.currentTimeMillis()
    ): Flow<Result<List<NotificationStats>>> = flow {
        try {
            val stats = notificationRepository.getNotificationStats(startDate, endDate)
            emit(Result.success(stats))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun updateNotificationSettings(
        userId: String,
        type: NotificationType,
        enabled: Boolean
    ): Flow<Result<Unit>> = flow {
        try {
            notificationRepository.updateNotificationSettings(userId, type.name, enabled)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getNotificationSettings(userId: String): Flow<Result<List<NotificationSetting>>> = flow {
        try {
            val settings = notificationRepository.getNotificationSettings(userId)
            emit(Result.success(settings))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

enum class NotificationType {
    LEAD_GENERATED,
    STATUS_CHANGE,
    DOCUMENT_UPLOADED,
    UNIT_CANCELLED,
    PAYMENT_REMINDER
}

data class WhatsAppNotification(
    val userId: String? = null,
    val phoneNumber: String? = null,
    val title: String,
    val message: String,
    val type: NotificationType,
    val data: Map<String, Any>? = null,
    val referenceId: String? = null
)

data class NotificationResult(
    val success: Boolean,
    val messageId: String?,
    val notificationId: String,
    val error: String? = null
)

data class BulkNotificationResult(
    val success: Boolean,
    val total: Int,
    val results: List<NotificationResult>
)

data class NotificationLog(
    val id: String,
    val userId: String?,
    val title: String,
    val message: String,
    val type: String,
    val channel: String,
    val messageId: String?,
    val deliveryStatus: String,
    val deliveredAt: String?,
    val readAt: String?,
    val data: Map<String, Any>?,
    val referenceId: String?,
    val createdAt: String
)

data class NotificationStats(
    val channel: String,
    val type: String,
    val totalSent: Long,
    val totalDelivered: Long,
    val totalRead: Long,
    val deliveryRate: Double,
    val readRate: Double
)

data class NotificationSetting(
    val userId: String,
    val notificationType: String,
    val channel: String,
    val isEnabled: Boolean,
    val preferences: Map<String, Any>?
)
