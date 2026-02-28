package com.kprflow.enterprise.domain.model

import java.util.UUID

data class SecurityToken(
    val accessToken: String,
    val refreshToken: String,
    val issuedAt: Long,
    val expiresAt: Long
) {
    fun isValid(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime in issuedAt..expiresAt
    }
    
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }
    
    fun timeToExpiry(): Long {
        return expiresAt - System.currentTimeMillis()
    }
}

data class EncryptedData(
    val encryptedData: ByteArray,
    val iv: ByteArray,
    val algorithm: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EncryptedData
        
        if (!encryptedData.contentEquals(other.encryptedData)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (algorithm != other.algorithm) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = encryptedData.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        return result
    }
}

data class SecurityAuditEvent(
    val id: String,
    val eventType: String,
    val details: String,
    val severity: SecuritySeverity,
    val userId: String?,
    val ipAddress: String,
    val userAgent: String,
    val timestamp: Long,
    val sessionId: String
)

data class SecurityViolation(
    val id: String,
    val violationType: SecurityViolationType,
    val description: String,
    val severity: SecuritySeverity,
    val userId: String?,
    val ipAddress: String,
    val timestamp: Long,
    val resolved: Boolean = false,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null
)

data class SecurityMetrics(
    val totalEvents: Int,
    val criticalEvents: Int,
    val warningEvents: Int,
    val infoEvents: Int,
    val violationsCount: Int,
    val authenticationAttempts: Int,
    val authenticationFailures: Int,
    val dataAccessAttempts: Int,
    val dataAccessDenials: Int,
    val encryptionOperations: Int,
    val periodStart: Long,
    val periodEnd: Long
)

data class SecurityReport(
    val id: String,
    val generatedAt: Long,
    val periodStart: Long,
    val periodEnd: Long,
    val metrics: SecurityMetrics,
    val topViolations: List<SecurityViolation>,
    val suspiciousActivities: List<SecurityAuditEvent>,
    val recommendations: List<SecurityRecommendation>
)

data class SecurityRecommendation(
    val id: String,
    val category: SecurityCategory,
    val title: String,
    val description: String,
    val priority: SecurityPriority,
    val implemented: Boolean = false,
    val implementedAt: Long? = null
)

enum class SecuritySeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

enum class SecurityViolationType {
    UNAUTHORIZED_ACCESS,
    DATA_BREACH_ATTEMPT,
    SUSPICIOUS_ACTIVITY,
    RATE_LIMIT_EXCEEDED,
    INVALID_TOKEN,
    ENCRYPTION_FAILURE,
    CERTIFICATE_MISMATCH,
    MALICIOUS_REQUEST
}

enum class SecurityCategory {
    AUTHENTICATION,
    AUTHORIZATION,
    DATA_PROTECTION,
    NETWORK_SECURITY,
    ENCRYPTION,
    AUDITING,
    COMPLIANCE
}

enum class SecurityPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class SecurityConfig(
    val enableCertificatePinning: Boolean = true,
    val enableRequestSigning: Boolean = true,
    val enableResponseValidation: Boolean = true,
    val enableRateLimiting: Boolean = true,
    val enableAuditLogging: Boolean = true,
    val enableDataEncryption: Boolean = true,
    val sessionTimeoutMinutes: Long = 60,
    val maxFailedAttempts: Int = 3,
    val lockoutDurationMinutes: Long = 15,
    val passwordMinLength: Int = 8,
    val requireSpecialCharacters: Boolean = true,
    val tokenRefreshThresholdMinutes: Long = 5
)
