package com.kprflow.enterprise.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    
    suspend fun registerDeviceToken(
        userId: String,
        deviceToken: String,
        deviceType: String = "android"
    ): Result<Unit> {
        return try {
            val deviceData = mapOf(
                "user_id" to userId,
                "device_token" to deviceToken,
                "device_type" to deviceType,
                "is_active" to true,
                "registered_at" to java.time.Instant.now().toString()
            )
            
            // Check if device already exists
            val existingDevice = postgrest.from("push_device_tokens")
                .select()
                .filter { 
                    eq("user_id", userId) 
                    eq("device_token", deviceToken)
                }
                .maybeSingle()
                .data
            
            if (existingDevice != null) {
                // Update existing device
                postgrest.from("push_device_tokens")
                    .update(
                        mapOf(
                            "is_active" to true,
                            "updated_at" to java.time.Instant.now().toString()
                        )
                    )
                    .filter { 
                        eq("user_id", userId) 
                        eq("device_token", deviceToken)
                    }
                    .maybeSingle()
                    .data
            } else {
                // Insert new device
                postgrest.from("push_device_tokens")
                    .insert(deviceData)
                    .maybeSingle()
                    .data
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun unregisterDeviceToken(
        userId: String,
        deviceToken: String
    ): Result<Unit> {
        return try {
            postgrest.from("push_device_tokens")
                .update(
                    mapOf(
                        "is_active" to false,
                        "unregistered_at" to java.time.Instant.now().toString(),
                        "updated_at" to java.time.Instant.now().toString()
                    )
                )
                .filter { 
                    eq("user_id", userId) 
                    eq("device_token", deviceToken)
                }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendPushNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        data: Map<String, String>? = null
    ): Result<String> {
        return try {
            val notificationData = mapOf(
                "user_id" to userId,
                "title" to title,
                "message" to message,
                "type" to type,
                "data" to data,
                "created_at" to java.time.Instant.now().toString(),
                "sent_at" to null,
                "delivered_at" to null
            )
            
            val notification = postgrest.from("push_notifications")
                .insert(notificationData)
                .maybeSingle()
                .data
            
            notification?.let { 
                    // TODO: Integrate with actual push notification service (Firebase Cloud Messaging)
                    Result.success(it.id)
                }
                ?: Result.failure(Exception("Failed to create push notification"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendBulkPushNotification(
        userIds: List<String>,
        title: String,
        message: String,
        type: String,
        data: Map<String, String>? = null
    ): Result<List<String>> {
        return try {
            val notificationIds = mutableListOf<String>()
            
            userIds.forEach { userId ->
                val result = sendPushNotification(userId, title, message, type, data)
                if (result.isSuccess) {
                    notificationIds.add(result.getOrNull() ?: "")
                }
            }
            
            Result.success(notificationIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserNotifications(
        userId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<PushNotification>> {
        return try {
            val notifications = postgrest.from("push_notifications")
                .select()
                .filter { eq("user_id", userId) }
                .order("created_at", ascending = false)
                .range(offset, offset + limit - 1)
                .data
            
            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markNotificationAsRead(
        notificationId: String
    ): Result<Unit> {
        return try {
            postgrest.from("push_notifications")
                .update(
                    mapOf(
                        "read_at" to java.time.Instant.now().toString(),
                        "updated_at" to java.time.Instant.now().toString()
                    )
                )
                .filter { eq("id", notificationId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markAllNotificationsAsRead(
        userId: String
    ): Result<Unit> {
        return try {
            postgrest.from("push_notifications")
                .update(
                    mapOf(
                        "read_at" to java.time.Instant.now().toString(),
                        "updated_at" to java.time.Instant.now().toString()
                    )
                )
                .filter { 
                    eq("user_id", userId)
                    .isNull("read_at")
                }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUnreadNotificationCount(
        userId: String
    ): Result<Int> {
        return try {
            val count = postgrest.from("push_notifications")
                .select("count")
                .filter { 
                    eq("user_id", userId)
                    .isNull("read_at")
                }
                .maybeSingle()
                .data
            
            val notificationCount = when (count) {
                is Map<*, *> -> {
                    val countValue = count["count"]
                    when (countValue) {
                        is Number -> countValue.toInt()
                        is String -> countValue.toIntOrNull() ?: 0
                        else -> 0
                    }
                }
                else -> 0
            }
            
            Result.success(notificationCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteNotification(
        notificationId: String
    ): Result<Unit> {
        return try {
            postgrest.from("push_notifications")
                .delete()
                .filter { eq("id", notificationId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendStatusUpdateNotification(
        dossierId: String,
        newStatus: String,
        previousStatus: String? = null
    ): Result<String> {
        return try {
            // Get dossier user ID
            val dossier = postgrest.from("kpr_dossiers")
                .select("user_id")
                .filter { eq("id", dossierId) }
                .maybeSingle()
                .data ?: return Result.failure(Exception("Dossier not found"))
            
            val userId = dossier.user_id
            val title = "Status Update"
            val message = if (previousStatus != null) {
                "Your KPR application status changed from $previousStatus to $newStatus"
            } else {
                "Your KPR application status is now $newStatus"
            }
            
            sendPushNotification(
                userId = userId,
                title = title,
                message = message,
                type = "status_update",
                data = mapOf(
                    "dossier_id" to dossierId,
                    "new_status" to newStatus,
                    "previous_status" to (previousStatus ?: "")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendDocumentVerificationNotification(
        userId: String,
        documentType: String,
        isVerified: Boolean,
        rejectionReason: String? = null
    ): Result<String> {
        return try {
            val title = if (isVerified) "Document Verified" else "Document Action Required"
            val message = if (isVerified) {
                "Your $documentType has been verified successfully"
            } else {
                "Your $documentType requires attention: ${rejectionReason ?: "Please check and resubmit"}"
            }
            
            sendPushNotification(
                userId = userId,
                title = title,
                message = message,
                type = "document_verification",
                data = mapOf(
                    "document_type" to documentType,
                    "is_verified" to isVerified.toString(),
                    "rejection_reason" to (rejectionReason ?: "")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendPaymentReminderNotification(
        userId: String,
        installmentNumber: Int,
        dueDate: String,
        amount: java.math.BigDecimal
    ): Result<String> {
        return try {
            val title = "Payment Reminder"
            val message = "Installment $installmentNumber due on $dueDate: ${formatCurrency(amount)}"
            
            sendPushNotification(
                userId = userId,
                title = title,
                message = message,
                type = "payment_reminder",
                data = mapOf(
                    "installment_number" to installmentNumber.toString(),
                    "due_date" to dueDate,
                    "amount" to amount.toString()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendBASTInvitationNotification(
        userId: String,
        bastDate: String,
        location: String
    ): Result<String> {
        return try {
            val title = "BAST Invitation"
            val message = "You are invited for BAST on $bastDate at $location"
            
            sendPushNotification(
                userId = userId,
                title = title,
                message = message,
                type = "bast_invitation",
                data = mapOf(
                    "bast_date" to bastDate,
                    "location" to location
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeNotificationUpdates(userId: String): Flow<NotificationUpdate> = flow {
        try {
            // TODO: Implement real-time updates via Supabase Realtime
            emit(NotificationUpdate.NewNotification)
        } catch (e: Exception) {
            emit(NotificationUpdate.Error(e.message ?: "Unknown error"))
        }
    }
    
    private fun formatCurrency(amount: java.math.BigDecimal): String {
        return "Rp ${String.format("%,.0f", amount)}"
    }
}

// Data classes
data class PushNotification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val data: Map<String, String>? = null,
    val createdAt: String,
    val readAt: String? = null,
    val sentAt: String? = null,
    val deliveredAt: String? = null
)

data class DeviceToken(
    val id: String,
    val userId: String,
    val deviceToken: String,
    val deviceType: String,
    val isActive: Boolean,
    val registeredAt: String,
    val unregisteredAt: String? = null
)

sealed class NotificationUpdate {
    object NewNotification : NotificationUpdate()
    object NotificationRead : NotificationUpdate()
    data class Error(val message: String) : NotificationUpdate()
}
