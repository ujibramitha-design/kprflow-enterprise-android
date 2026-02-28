package com.kprflow.enterprise.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditTrailRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    
    companion object {
        // Audit action types
        const val ACTION_CREATE = "CREATE"
        const val ACTION_UPDATE = "UPDATE"
        const val ACTION_DELETE = "DELETE"
        const val ACTION_READ = "READ"
        const val ACTION_LOGIN = "LOGIN"
        const val ACTION_LOGOUT = "LOGOUT"
        const val ACTION_UPLOAD = "UPLOAD"
        const val ACTION_DOWNLOAD = "DOWNLOAD"
        const val ACTION_APPROVE = "APPROVE"
        const val ACTION_REJECT = "REJECT"
        const val ACTION_SIGN = "SIGN"
        const val ACTION_EXPORT = "EXPORT"
        const val ACTION_IMPORT = "IMPORT"
        
        // Entity types
        const val ENTITY_DOSSIER = "DOSSIER"
        const val ENTITY_DOCUMENT = "DOCUMENT"
        const val ENTITY_USER = "USER"
        const val ENTITY_UNIT = "UNIT"
        const val ENTITY_PAYMENT = "PAYMENT"
        const val ENTITY_REPORT = "REPORT"
        const val ENTITY_CONFIG = "CONFIG"
        const val ENTITY_NOTIFICATION = "NOTIFICATION"
        const val ENTITY_VOTE = "VOTE"
        const val ENTITY_BLOCK = "BLOCK"
        
        // Severity levels
        const val SEVERITY_LOW = "LOW"
        const val SEVERITY_MEDIUM = "MEDIUM"
        const val SEVERITY_HIGH = "HIGH"
        const val SEVERITY_CRITICAL = "CRITICAL"
    }
    
    suspend fun logAction(
        userId: String,
        action: String,
        entityType: String,
        entityId: String,
        description: String,
        ipAddress: String? = null,
        userAgent: String? = null,
        oldValues: Map<String, Any>? = null,
        newValues: Map<String, Any>? = null,
        severity: String = SEVERITY_LOW,
        category: String? = null
    ): Result<String> {
        return try {
            val auditData = mapOf(
                "user_id" to userId,
                "action" to action,
                "entity_type" to entityType,
                "entity_id" to entityId,
                "description" to description,
                "ip_address" to ipAddress,
                "user_agent" to userAgent,
                "old_values" to oldValues,
                "new_values" to newValues,
                "severity" to severity,
                "category" to category,
                "created_at" to Instant.now().toString()
            )
            
            val auditLog = postgrest.from("audit_trail")
                .insert(auditData)
                .maybeSingle()
                .data
            
            auditLog?.let { 
                    Result.success(it.id)
                }
                ?: Result.failure(Exception("Failed to create audit log"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun logUserAction(
        userId: String,
        action: String,
        description: String,
        ipAddress: String? = null,
        userAgent: String? = null
    ): Result<String> {
        return logAction(
            userId = userId,
            action = action,
            entityType = ENTITY_USER,
            entityId = userId,
            description = description,
            ipAddress = ipAddress,
            userAgent = userAgent
        )
    }
    
    suspend fun logDossierAction(
        userId: String,
        action: String,
        dossierId: String,
        description: String,
        oldValues: Map<String, Any>? = null,
        newValues: Map<String, Any>? = null,
        ipAddress: String? = null,
        userAgent: String? = null,
        severity: String = SEVERITY_MEDIUM
    ): Result<String> {
        return logAction(
            userId = userId,
            action = action,
            entityType = ENTITY_DOSSIER,
            entityId = dossierId,
            description = description,
            ipAddress = ipAddress,
            userAgent = userAgent,
            oldValues = oldValues,
            newValues = newValues,
            severity = severity,
            category = "DOSSIER_MANAGEMENT"
        )
    }
    
    suspend fun logDocumentAction(
        userId: String,
        action: String,
        documentId: String,
        description: String,
        oldValues: Map<String, Any>? = null,
        newValues: Map<String, Any>? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ): Result<String> {
        return logAction(
            userId = userId,
            action = action,
            entityType = ENTITY_DOCUMENT,
            entityId = documentId,
            description = description,
            ipAddress = ipAddress,
            userAgent = userAgent,
            oldValues = oldValues,
            newValues = newValues,
            severity = SEVERITY_MEDIUM,
            category = "DOCUMENT_MANAGEMENT"
        )
    }
    
    suspend fun logPaymentAction(
        userId: String,
        action: String,
        paymentId: String,
        description: String,
        oldValues: Map<String, Any>? = null,
        newValues: Map<String, Any>? = null,
        ipAddress: String? = null,
        userAgent: String? = null,
        severity: String = SEVERITY_HIGH
    ): Result<String> {
        return logAction(
            userId = userId,
            action = action,
            entityType = ENTITY_PAYMENT,
            entityId = paymentId,
            description = description,
            ipAddress = ipAddress,
            userAgent = userAgent,
            oldValues = oldValues,
            newValues = newValues,
            severity = severity,
            category = "PAYMENT_PROCESSING"
        )
    }
    
    suspend fun logSecurityAction(
        userId: String,
        action: String,
        description: String,
        ipAddress: String? = null,
        userAgent: String? = null,
        severity: String = SEVERITY_HIGH
    ): Result<String> {
        return logAction(
            userId = userId,
            action = action,
            entityType = "SECURITY",
            entityId = userId,
            description = description,
            ipAddress = ipAddress,
            userAgent = userAgent,
            severity = severity,
            category = "SECURITY"
        )
    }
    
    suspend fun logDataChange(
        userId: String,
        entityType: String,
        entityId: String,
        fieldName: String,
        oldValue: Any?,
        newValue: Any?,
        description: String,
        ipAddress: String? = null,
        userAgent: String? = null
    ): Result<String> {
        val oldValues = if (oldValue != null) mapOf(fieldName to oldValue) else null
        val newValues = if (newValue != null) mapOf(fieldName to newValue) else null
        
        return logAction(
            userId = userId,
            action = ACTION_UPDATE,
            entityType = entityType,
            entityId = entityId,
            description = description,
            ipAddress = ipAddress,
            userAgent = userAgent,
            oldValues = oldValues,
            newValues = newValues,
            severity = SEVERITY_MEDIUM,
            category = "DATA_CHANGE"
        )
    }
    
    suspend fun getAuditLogs(
        userId: String? = null,
        entityType: String? = null,
        entityId: String? = null,
        action: String? = null,
        severity: String? = null,
        category: String? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<AuditLog>> {
        return try {
            var query = postgrest.from("audit_trail")
                .select()
            
            // Apply filters
            userId?.let { query = query.filter { eq("user_id", it) } }
            entityType?.let { query = query.filter { eq("entity_type", it) } }
            entityId?.let { query = query.filter { eq("entity_id", it) } }
            action?.let { query = query.filter { eq("action", it) } }
            severity?.let { query = query.filter { eq("severity", it) } }
            category?.let { query = query.filter { eq("category", it) } }
            
            // Apply date range
            startDate?.let { query = query.filter { gte("created_at", it.toString()) } }
            endDate?.let { query = query.filter { lte("created_at", it.toString()) } }
            
            // Apply pagination and ordering
            val logs = query
                .order("created_at", ascending = false)
                .range(offset, offset + limit - 1)
                .data
            
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAuditLogById(logId: String): Result<AuditLog?> {
        return try {
            val log = postgrest.from("audit_trail")
                .select()
                .filter { eq("id", logId) }
                .maybeSingle()
                .data
            
            Result.success(log)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAuditLogsByEntity(
        entityType: String,
        entityId: String,
        limit: Int = 50
    ): Result<List<AuditLog>> {
        return getAuditLogs(
            entityType = entityType,
            entityId = entityId,
            limit = limit
        )
    }
    
    suspend fun getAuditLogsByUser(
        userId: String,
        limit: Int = 50
    ): Result<List<AuditLog>> {
        return getAuditLogs(
            userId = userId,
            limit = limit
        )
    }
    
    suspend fun getSecurityAuditLogs(
        startDate: Instant? = null,
        endDate: Instant? = null,
        limit: Int = 100
    ): Result<List<AuditLog>> {
        return getAuditLogs(
            category = "SECURITY",
            startDate = startDate,
            endDate = endDate,
            limit = limit
        )
    }
    
    suspend fun getHighSeverityLogs(
        startDate: Instant? = null,
        endDate: Instant? = null,
        limit: Int = 100
    ): Result<List<AuditLog>> {
        return getAuditLogs(
            severity = SEVERITY_HIGH,
            startDate = startDate,
            endDate = endDate,
            limit = limit
        )
    }
    
    suspend fun searchAuditLogs(
        searchTerm: String,
        limit: Int = 100
    ): Result<List<AuditLog>> {
        return try {
            val logs = postgrest.from("audit_trail")
                .select()
                .or(
                    listOf(
                        "description.ilike.%$searchTerm%",
                        "entity_id.ilike.%$searchTerm%",
                        "old_values::text.ilike.%$searchTerm%",
                        "new_values::text.ilike.%$searchTerm%"
                    )
                )
                .order("created_at", ascending = false)
                .limit(limit)
                .data
            
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAuditStatistics(
        startDate: Instant? = null,
        endDate: Instant? = null
    ): Result<AuditStatistics> {
        return try {
            var query = postgrest.from("audit_trail")
                .select("count(*) as total_logs")
            
            startDate?.let { query = query.filter { gte("created_at", it.toString()) } }
            endDate?.let { query = query.filter { lte("created_at", it.toString()) } }
            
            val totalResult = query.maybeSingle().data
            val totalLogs = when (totalResult) {
                is Map<*, *> -> (totalResult["total_logs"] as? Number)?.toInt() ?: 0
                else -> 0
            }
            
            // Get action breakdown
            val actionBreakdown = postgrest.from("audit_trail")
                .select("action, count(*) as count")
                .let { query ->
                    startDate?.let { query.filter { gte("created_at", it.toString()) } } ?: query
                }
                .let { query ->
                    endDate?.let { query.filter { lte("created_at", it.toString()) } } ?: query
                }
                .group("action")
                .order("count", ascending = false)
                .data
            
            // Get severity breakdown
            val severityBreakdown = postgrest.from("audit_trail")
                .select("severity, count(*) as count")
                .let { query ->
                    startDate?.let { query.filter { gte("created_at", it.toString()) } } ?: query
                }
                .let { query ->
                    endDate?.let { query.filter { lte("created_at", it.toString()) } } ?: query
                }
                .group("severity")
                .order("count", ascending = false)
                .data
            
            val statistics = AuditStatistics(
                totalLogs = totalLogs,
                actionBreakdown = actionBreakdown.map { 
                    AuditBreakdown(it.action, it.count) 
                },
                severityBreakdown = severityBreakdown.map { 
                    AuditBreakdown(it.severity, it.count) 
                },
                generatedAt = Instant.now().toString()
            )
            
            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateComplianceReport(
        reportType: String,
        startDate: Instant,
        endDate: Instant
    ): Result<ComplianceReport> {
        return try {
            val logs = getAuditLogs(
                startDate = startDate,
                endDate = endDate,
                limit = 10000
            ).getOrNull().orEmpty()
            
            // Generate compliance metrics
            val totalActions = logs.size
            val highSeverityActions = logs.count { it.severity == SEVERITY_HIGH || it.severity == SEVERITY_CRITICAL }
            val securityActions = logs.count { it.category == "SECURITY" }
            val dataChangeActions = logs.count { it.category == "DATA_CHANGE" }
            
            val report = ComplianceReport(
                id = java.util.UUID.randomUUID().toString(),
                reportType = reportType,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                totalActions = totalActions,
                highSeverityActions = highSeverityActions,
                securityActions = securityActions,
                dataChangeActions = dataChangeActions,
                complianceScore = calculateComplianceScore(totalActions, highSeverityActions, securityActions),
                logs = logs,
                generatedAt = Instant.now().toString()
            )
            
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportAuditLogs(
        format: String = "CSV",
        filters: AuditLogFilters? = null
    ): Result<String> {
        return try {
            val logs = getAuditLogs(
                userId = filters?.userId,
                entityType = filters?.entityType,
                entityId = filters?.entityId,
                action = filters?.action,
                severity = filters?.severity,
                category = filters?.category,
                startDate = filters?.startDate,
                endDate = filters?.endDate,
                limit = 10000
            ).getOrNull().orEmpty()
            
            val exportData = when (format.uppercase()) {
                "CSV" -> generateCSVExport(logs)
                "JSON" -> generateJSONExport(logs)
                else -> generateCSVExport(logs)
            }
            
            Result.success(exportData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeAuditUpdates(): Flow<AuditUpdate> = flow {
        try {
            // TODO: Implement real-time updates via Supabase Realtime
            emit(AuditUpdate.NewLogEntry)
        } catch (e: Exception) {
            emit(AuditUpdate.Error(e.message ?: "Unknown error"))
        }
    }
    
    private fun calculateComplianceScore(
        totalActions: Int,
        highSeverityActions: Int,
        securityActions: Int
    ): Double {
        if (totalActions == 0) return 100.0
        
        val highSeverityPenalty = (highSeverityActions.toDouble() / totalActions) * 50
        val securityBonus = if (securityActions > 0) 5.0 else 0.0
        
        return (100.0 - highSeverityPenalty + securityBonus).coerceIn(0.0, 100.0)
    }
    
    private fun generateCSVExport(logs: List<AuditLog>): String {
        val csvHeader = "ID,User ID,Action,Entity Type,Entity ID,Description,Severity,Category,IP Address,User Agent,Created At\n"
        val csvRows = logs.joinToString("\n") { log ->
            listOf(
                log.id,
                log.userId,
                log.action,
                log.entityType,
                log.entityId,
                log.description.replace("\"", "\"\""),
                log.severity,
                log.category ?: "",
                log.ipAddress ?: "",
                log.userAgent ?: "",
                log.createdAt
            ).joinToString(",") { "\"$it\"" }
        }
        
        return csvHeader + csvRows
    }
    
    private fun generateJSONExport(logs: List<AuditLog>): String {
        val jsonLogs = logs.map { log ->
            mapOf(
                "id" to log.id,
                "userId" to log.userId,
                "action" to log.action,
                "entityType" to log.entityType,
                "entityId" to log.entityId,
                "description" to log.description,
                "severity" to log.severity,
                "category" to log.category,
                "ipAddress" to log.ipAddress,
                "userAgent" to log.userAgent,
                "oldValues" to log.oldValues,
                "newValues" to log.newValues,
                "createdAt" to log.createdAt
            )
        }
        
        // Convert to JSON string (simplified - would use proper JSON library)
        return jsonLogs.toString()
    }
}

// Data classes
data class AuditLog(
    val id: String,
    val userId: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val description: String,
    val ipAddress: String?,
    val userAgent: String?,
    val oldValues: Map<String, Any>?,
    val newValues: Map<String, Any>?,
    val severity: String,
    val category: String?,
    val createdAt: String
)

data class AuditStatistics(
    val totalLogs: Int,
    val actionBreakdown: List<AuditBreakdown>,
    val severityBreakdown: List<AuditBreakdown>,
    val generatedAt: String
)

data class AuditBreakdown(
    val name: String,
    val count: Int
)

data class ComplianceReport(
    val id: String,
    val reportType: String,
    val startDate: String,
    val endDate: String,
    val totalActions: Int,
    val highSeverityActions: Int,
    val securityActions: Int,
    val dataChangeActions: Int,
    val complianceScore: Double,
    val logs: List<AuditLog>,
    val generatedAt: String
)

data class AuditLogFilters(
    val userId: String? = null,
    val entityType: String? = null,
    val entityId: String? = null,
    val action: String? = null,
    val severity: String? = null,
    val category: String? = null,
    val startDate: Instant? = null,
    val endDate: Instant? = null
)

sealed class AuditUpdate {
    object NewLogEntry : AuditUpdate()
    object HighSeverityAlert : AuditUpdate()
    object SecurityEvent : AuditUpdate()
    data class Error(val message: String) : AuditUpdate()
}
