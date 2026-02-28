package com.kprflow.enterprise.domain.model

import java.util.UUID

/**
 * Audit Log Domain Model
 * Anti-Tamper System Data Model
 */
data class AuditLog(
    val id: UUID,
    val createdAt: Long,
    val userName: String,
    val userRole: String,
    val userDepartment: String,
    val action: String,
    val tableName: String,
    val recordId: UUID,
    val description: String,
    val isCritical: Boolean,
    val ipAddress: String?,
    val oldData: Map<String, String>?,
    val newData: Map<String, String>?
)

/**
 * Audit Log Filter
 */
data class AuditLogFilter(
    val tableName: String? = null,
    val userId: UUID? = null,
    val isCritical: Boolean? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val searchQuery: String? = null
)

/**
 * Audit Log Statistics
 */
data class AuditLogStatistics(
    val totalLogs: Int,
    val criticalLogs: Int,
    val todayLogs: Int,
    val weeklyLogs: Int,
    val monthlyLogs: Int,
    val topUsers: List<UserActivity>,
    val topTables: List<TableActivity>,
    val criticalAlerts: Int
)

/**
 * User Activity
 */
data class UserActivity(
    val userId: UUID,
    val userName: String,
    val actionCount: Int,
    val lastActivity: Long
)

/**
 * Table Activity
 */
data class TableActivity(
    val tableName: String,
    val actionCount: Int,
    val lastActivity: Long
)

/**
 * Critical Alert
 */
data class CriticalAlert(
    val id: UUID,
    val tableName: String,
    val recordId: UUID,
    val userId: UUID,
    val alertMessage: String,
    val createdAt: Long,
    val acknowledged: Boolean,
    val acknowledgedBy: UUID?,
    val acknowledgedAt: Long?
)

/**
 * Audit Log Export
 */
data class AuditLogExport(
    val logs: List<AuditLog>,
    val exportFormat: ExportFormat,
    val exportedAt: Long,
    val exportedBy: String
)

/**
 * Export Format
 */
enum class ExportFormat {
    PDF,
    EXCEL,
    CSV,
    JSON
}

/**
 * Audit Log Summary
 */
data class AuditLogSummary(
    val totalChanges: Int,
    val criticalChanges: Int,
    val userChanges: Map<String, Int>,
    val tableChanges: Map<String, Int>,
    val timeRange: String
)
