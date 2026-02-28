package com.kprflow.enterprise.security

import com.kprflow.enterprise.domain.repository.SecurityAuditRepository
import com.kprflow.enterprise.domain.model.SecurityAuditEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogger @Inject constructor(
    private val securityAuditRepository: SecurityAuditRepository,
    private val encryptionManager: EncryptionManager
) {
    
    private val auditScope = CoroutineScope(Dispatchers.IO)
    private val eventQueue = mutableListOf<SecurityAuditEvent>()
    
    fun logSecurityEvent(
        eventType: String,
        details: Map<String, Any> = emptyMap(),
        severity: SecuritySeverity = SecuritySeverity.INFO,
        userId: String? = null
    ) {
        val event = SecurityAuditEvent(
            id = UUID.randomUUID().toString(),
            eventType = eventType,
            details = encryptDetails(details),
            severity = severity,
            userId = userId,
            ipAddress = getClientIpAddress(),
            userAgent = getUserAgent(),
            timestamp = System.currentTimeMillis(),
            sessionId = getCurrentSessionId()
        )
        
        auditScope.launch {
            try {
                securityAuditRepository.logSecurityEvent(event)
            } catch (e: Exception) {
                // Fallback to local storage if remote logging fails
                eventQueue.add(event)
                if (eventQueue.size > 100) {
                    eventQueue.removeAt(0) // Remove oldest event
                }
            }
        }
    }
    
    fun logAuthenticationEvent(
        action: String,
        userId: String,
        success: Boolean,
        failureReason: String? = null
    ) {
        val details = mutableMapOf<String, Any>(
            "action" to action,
            "success" to success
        )
        
        if (!success && failureReason != null) {
            details["failure_reason"] = failureReason
        }
        
        logSecurityEvent(
            eventType = "AUTHENTICATION",
            details = details,
            severity = if (success) SecuritySeverity.INFO else SecuritySeverity.WARNING,
            userId = userId
        )
    }
    
    fun logDataAccessEvent(
        resourceType: String,
        resourceId: String,
        action: String,
        userId: String,
        accessGranted: Boolean,
        reason: String? = null
    ) {
        val details = mutableMapOf<String, Any>(
            "resource_type" to resourceType,
            "resource_id" to resourceId,
            "action" to action,
            "access_granted" to accessGranted
        )
        
        if (reason != null) {
            details["reason"] = reason
        }
        
        logSecurityEvent(
            eventType = "DATA_ACCESS",
            details = details,
            severity = if (accessGranted) SecuritySeverity.INFO else SecuritySeverity.WARNING,
            userId = userId
        )
    }
    
    fun logDataModificationEvent(
        resourceType: String,
        resourceId: String,
        action: String,
        userId: String,
        oldValue: String? = null,
        newValue: String? = null
    ) {
        val details = mutableMapOf<String, Any>(
            "resource_type" to resourceType,
            "resource_id" to resourceId,
            "action" to action
        )
        
        if (oldValue != null) {
            details["old_value"] = encryptionManager.hashData(oldValue)
        }
        
        if (newValue != null) {
            details["new_value"] = encryptionManager.hashData(newValue)
        }
        
        logSecurityEvent(
            eventType = "DATA_MODIFICATION",
            details = details,
            severity = SecuritySeverity.INFO,
            userId = userId
        )
    }
    
    fun logSecurityViolation(
        violationType: String,
        description: String,
        severity: SecuritySeverity = SecuritySeverity.CRITICAL,
        userId: String? = null
    ) {
        val details = mapOf(
            "violation_type" to violationType,
            "description" to description
        )
        
        logSecurityEvent(
            eventType = "SECURITY_VIOLATION",
            details = details,
            severity = severity,
            userId = userId
        )
    }
    
    fun logNetworkSecurityEvent(
        eventType: String,
        url: String,
        method: String,
        statusCode: Int,
        duration: Long,
        success: Boolean,
        error: String? = null
    ) {
        val details = mutableMapOf<String, Any>(
            "url" to url,
            "method" to method,
            "status_code" to statusCode,
            "duration_ms" to duration,
            "success" to success
        )
        
        if (error != null) {
            details["error"] = error
        }
        
        logSecurityEvent(
            eventType = eventType,
            details = details,
            severity = if (success) SecuritySeverity.INFO else SecuritySeverity.WARNING
        )
    }
    
    fun logEncryptionEvent(
        operation: String,
        dataType: String,
        success: Boolean,
        error: String? = null
    ) {
        val details = mutableMapOf<String, Any>(
            "operation" to operation,
            "data_type" to dataType,
            "success" to success
        )
        
        if (error != null) {
            details["error"] = error
        }
        
        logSecurityEvent(
            eventType = "ENCRYPTION",
            details = details,
            severity = if (success) SecuritySeverity.INFO else SecuritySeverity.ERROR
        )
    }
    
    fun getQueuedEvents(): List<SecurityAuditEvent> {
        return eventQueue.toList()
    }
    
    fun clearQueuedEvents() {
        eventQueue.clear()
    }
    
    private fun encryptDetails(details: Map<String, Any>): String {
        return try {
            val detailsJson = details.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
            val encryptedData = encryptionManager.encrypt("{$detailsJson}")
            Base64.getEncoder().encodeToString(encryptedData.encryptedData)
        } catch (e: Exception) {
            // Fallback to unencrypted if encryption fails
            details.toString()
        }
    }
    
    private fun getClientIpAddress(): String {
        // In a real implementation, this would get the actual IP address
        return "127.0.0.1"
    }
    
    private fun getUserAgent(): String {
        return "KPRFlow-Android/1.0.0"
    }
    
    private fun getCurrentSessionId(): String {
        return UUID.randomUUID().toString()
    }
}

enum class SecuritySeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}
