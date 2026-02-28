package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.domain.model.SecurityAuditEvent
import com.kprflow.enterprise.domain.model.SecurityMetrics
import com.kprflow.enterprise.domain.model.SecurityReport
import com.kprflow.enterprise.domain.model.SecurityViolation
import kotlinx.coroutines.flow.Flow

interface SecurityAuditRepository {
    suspend fun logSecurityEvent(event: SecurityAuditEvent)
    suspend fun logSecurityViolation(violation: SecurityViolation)
    suspend fun getSecurityEvents(
        eventType: String? = null,
        userId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int = 100
    ): Flow<List<SecurityAuditEvent>>
    
    suspend fun getSecurityViolations(
        violationType: String? = null,
        resolved: Boolean? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int = 100
    ): Flow<List<SecurityViolation>>
    
    suspend fun getSecurityMetrics(
        startTime: Long,
        endTime: Long
    ): SecurityMetrics
    
    suspend fun generateSecurityReport(
        startTime: Long,
        endTime: Long
    ): SecurityReport
    
    suspend fun resolveViolation(violationId: String, resolvedBy: String): Result<Unit>
    suspend fun getSecurityConfig(): Map<String, Any>
    suspend fun updateSecurityConfig(config: Map<String, Any>): Result<Unit>
}
