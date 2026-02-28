package com.kprflow.enterprise.domain.repository

import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audit Repository - Data Layer for Audit Log Management
 * Anti-Tamper System Repository Implementation
 */
@Singleton
class AuditRepository @Inject constructor(
    private val supabaseClient: com.supabase.gotrue.SupabaseClient
) {
    
    /**
     * Get audit history for a specific record
     */
    fun getAuditHistory(recordId: UUID): Flow<List<com.kprflow.enterprise.domain.model.AuditLog>> {
        return try {
            supabaseClient.from("AuditLogHistory")
                .select {
                    eq("record_id", recordId)
                }
                .order("created_at", com.supabase.gotrue.PostgrestQuery.Order.DESCENDING)
                .flow()
                .map { response ->
                    response.data?.mapNotNull { it ->
                        it as? com.supabase.gotrue.PostgrestResult.MapData
                    }?.map { data ->
                        com.kprflow.enterprise.domain.model.AuditLog(
                            id = UUID.fromString(data["id"] as String),
                            createdAt = (data["created_at"] as String).toTimestamp(),
                            userName = data["user_name"] as String,
                            userRole = data["user_role"] as String,
                            userDepartment = data["user_department"] as String,
                            action = data["action"] as String,
                            tableName = data["table_name"] as String,
                            recordId = UUID.fromString(data["record_id"] as String),
                            description = data["description"] as String,
                            isCritical = data["is_critical"] as Boolean,
                            ipAddress = data["ip_address"] as String?,
                            oldData = (data["old_data"] as? Map<String, String>)?.mapValues { it.value.toString() },
                            newData = (data["new_data"] as? Map<String, String>)?.mapValues { it.value.toString() }
                        )
                    } ?: emptyList()
                }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flow { emptyList<com.kprflow.enterprise.domain.model.AuditLog>() }
        }
    }
    
    /**
     * Get audit history for a specific table
     */
    fun getTableAuditHistory(tableName: String): Flow<List<com.kprflow.enterprise.domain.model.AuditLog>> {
        return try {
            supabaseClient.from("AuditLogHistory")
                .select {
                    eq("table_name", tableName)
                }
                .order("created_at", com.supabase.gotrue.PostgrestQuery.Order.DESCENDING)
                .flow()
                .map { response ->
                    response.data?.mapNotNull { it ->
                        it as? com.supabase.gotrue.PostgrestResult.MapData
                    }?.map { data ->
                        com.kprflow.enterprise.domain.model.AuditLog(
                            id = UUID.fromString(data["id"] as String),
                            createdAt = (data["created_at"] as String).toTimestamp(),
                            userName = data["user_name"] as String,
                            userRole = data["user_role"] as String,
                            userDepartment = data["user_department"] as String,
                            action = data["action"] as String,
                            tableName = data["table_name"] as String,
                            recordId = UUID.fromString(data["record_id"] as String),
                            description = data["description"] as String,
                            isCritical = data["is_critical"] as Boolean,
                            ipAddress = data["ip_address"] as String?,
                            oldData = (data["old_data"] as? Map<String, String>)?.mapValues { it.value.toString() },
                            newData = (data["new_data"] as? Map<String, String>)?.mapValues { it.value.toString() }
                        )
                    } ?: emptyList()
                }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flow { emptyList<com.kprflow.enterprise.domain.model.AuditLog>() }
        }
    }
    
    /**
     * Get audit history for a specific user
     */
    fun getUserAuditHistory(userId: UUID): Flow<List<com.kprflow.enterprise.domain.model.AuditLog>> {
        return try {
            supabaseClient.from("AuditLogHistory")
                .select {
                    eq("user_id", userId)
                }
                .order("created_at", com.supabase.gotrue.PostgrestQuery.Order.DESCENDING)
                .flow()
                .map { response ->
                    response.data?.mapNotNull { it ->
                        it as? com.supabase.gotrue.PostgrestResult.MapData
                    }?.map { data ->
                        com.kprflow.enterprise.domain.model.AuditLog(
                            id = UUID.fromString(data["id"] as String),
                            createdAt = (data["created_at"] as String).toTimestamp(),
                            userName = data["user_name"] as String,
                            userRole = data["user_role"] as String,
                            userDepartment = data["user_department"] as String,
                            action = data["action"] as String,
                            tableName = data["table_name"] as String,
                            recordId = UUID.fromString(data["record_id"] as String),
                            description = data["description"] as String,
                            isCritical = data["is_critical"] as Boolean,
                            ipAddress = data["ip_address"] as String?,
                            oldData = (data["old_data"] as? Map<String, String>)?.mapValues { it.value.toString() },
                            newData = (data["new_data"] as? Map<String, String>)?.mapValues { it.value.toString() }
                        )
                    } ?: emptyList()
                }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flow { emptyList<com.kprflow.enterprise.domain.model.AuditLog>() }
        }
    }
    
    /**
     * Get critical audit logs (for BOD and Managers)
     */
    fun getCriticalAuditLogs(): Flow<List<com.kprflow.enterprise.domain.model.AuditLog>> {
        return try {
            supabaseClient.from("AuditLogHistory")
                .select {
                    eq("is_critical", true)
                }
                .order("created_at", com.supabase.gotrue.PostgrestQuery.Order.DESCENDING)
                .flow()
                .map { response ->
                    response.data?.mapNotNull { it ->
                        it as? com.supabase.gotrue.PostgrestResult.MapData
                    }?.map { data ->
                        com.kprflow.enterprise.domain.model.AuditLog(
                            id = UUID.fromString(data["id"] as String),
                            createdAt = (data["created_at"] as String).toTimestamp(),
                            userName = data["user_name"] as String,
                            userRole = data["user_role"] as String,
                            userDepartment = data["user_department"] as String,
                            action = data["action"] as String,
                            tableName = data["table_name"] as String,
                            recordId = UUID.fromString(data["record_id"] as String),
                            description = data["description"] as String,
                            isCritical = data["is_critical"] as Boolean,
                            ipAddress = data["ip_address"] as String?,
                            oldData = (data["old_data"] as? Map<String, String>)?.mapValues { it.value.toString() },
                            newData = (data["new_data"] as? Map<String, String>)?.mapValues { it.value.toString() }
                        )
                    } ?: emptyList()
                }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flow { emptyList<com.kprflow.enterprise.domain.model.AuditLog>() }
        }
    }
    
    /**
     * Get audit statistics
     */
    suspend fun getAuditStatistics(): com.kprflow.enterprise.domain.usecase.AuditStatistics {
        return try {
            // Get total logs
            val totalLogsResponse = supabaseClient.from("AuditLog")
                .select("count")
                .single()
            
            val totalLogs = (totalLogsResponse.data?.get(0) as? Map<String, Any>)?.get("count") as? Int ?: 0
            
            // Get critical logs
            val criticalLogsResponse = supabaseClient.from("AuditLog")
                .select("count")
                .eq("is_critical", true)
                .single()
            
            val criticalLogs = (criticalLogsResponse.data?.get(0) as? Map<String, Any>)?.get("count") as? Int ?: 0
            
            // Get today's logs
            val today = java.time.LocalDate.now().toString()
            val todayLogsResponse = supabaseClient.from("AuditLog")
                .select("count")
                .gte("created_at", today)
                .single()
            
            val todayLogs = (todayLogsResponse.data?.get(0) as? Map<String, Any>)?.get("count") as? Int ?: 0
            
            // Get weekly logs
            val weekAgo = java.time.LocalDate.now().minusDays(7).toString()
            val weeklyLogsResponse = supabaseClient.from("AuditLog")
                .select("count")
                .gte("created_at", weekAgo)
                .single()
            
            val weeklyLogs = (weeklyLogsResponse.data?.get(0) as? Map<String, Any>)?.get("count") as? Int ?: 0
            
            // Get monthly logs
            val monthAgo = java.time.LocalDate.now().minusDays(30).toString()
            val monthlyLogsResponse = supabaseClient.from("AuditLog")
                .select("count")
                .gte("created_at", monthAgo)
                .single()
            
            val monthlyLogs = (monthlyLogsResponse.data?.get(0) as? Map<String, Any>)?.get("count") as? Int ?: 0
            
            // Get top users
            val topUsersResponse = supabaseClient.from("AuditLogHistory")
                .select("user_id, user_name, count(*) as action_count, max(created_at) as last_activity")
                .group("user_id, user_name")
                .order("action_count", com.supabase.gotrue.PostgrestQuery.Order.DESCENDING)
                .limit(10)
                .single()
            
            val topUsers = topUsersResponse.data?.mapNotNull { it ->
                it as? com.supabase.gotrue.PostgrestResult.MapData
            }?.map { data ->
                com.kprflow.enterprise.domain.usecase.UserActivity(
                    userId = UUID.fromString(data["user_id"] as String),
                    userName = data["user_name"] as String,
                    actionCount = (data["action_count"] as Number).toInt(),
                    lastActivity = (data["last_activity"] as String).toTimestamp()
                )
            } ?: emptyList()
            
            // Get top tables
            val topTablesResponse = supabaseClient.from("AuditLogHistory")
                .select("table_name, count(*) as action_count, max(created_at) as last_activity")
                .group("table_name")
                .order("action_count", com.supabase.gotrue.PostgrestQuery.Order.DESCENDING)
                .limit(10)
                .single()
            
            val topTables = topTablesResponse.data?.mapNotNull { it ->
                it as? com.supabase.gotrue.PostgrestResult.MapData
            }?.map { data ->
                com.kprflow.enterprise.domain.usecase.TableActivity(
                    tableName = data["table_name"] as String,
                    actionCount = (data["action_count"] as Number).toInt(),
                    lastActivity = (data["last_activity"] as String).toTimestamp()
                )
            } ?: emptyList()
            
            // Get critical alerts
            val criticalAlertsResponse = supabaseClient.from("Notification")
                .select("count")
                .eq("is_critical", true)
                .eq("type", "CRITICAL_CHANGE")
                .single()
            
            val criticalAlerts = (criticalAlertsResponse.data?.get(0) as? Map<String, Any>)?.get("count") as? Int ?: 0
            
            com.kprflow.enterprise.domain.usecase.AuditStatistics(
                totalLogs = totalLogs,
                criticalLogs = criticalLogs,
                todayLogs = todayLogs,
                weeklyLogs = weeklyLogs,
                monthlyLogs = monthlyLogs,
                topUsers = topUsers,
                topTables = topTables,
                criticalAlerts = criticalAlerts
            )
        } catch (e: Exception) {
            com.kprflow.enterprise.domain.usecase.AuditStatistics(
                totalLogs = 0,
                criticalLogs = 0,
                todayLogs = 0,
                weeklyLogs = 0,
                monthlyLogs = 0,
                topUsers = emptyList(),
                topTables = emptyList(),
                criticalAlerts = 0
            )
        }
    }
    
    /**
     * Search audit logs
     */
    fun searchAuditLogs(query: String): Flow<List<com.kprflow.enterprise.domain.model.AuditLog>> {
        return try {
            supabaseClient.from("AuditLogHistory")
                .select {
                    or(
                        ilike("description", "%$query%"),
                        ilike("user_name", "%$query%"),
                        ilike("table_name", "%$query%")
                    )
                }
                .order("created_at", com.supabase.gotrue.PostgrestQuery.Order.DESCENDING)
                .flow()
                .map { response ->
                    response.data?.mapNotNull { it ->
                        it as? com.supabase.gotrue.PostgrestResult.MapData
                    }?.map { data ->
                        com.kprflow.enterprise.domain.model.AuditLog(
                            id = UUID.fromString(data["id"] as String),
                            createdAt = (data["created_at"] as String).toTimestamp(),
                            userName = data["user_name"] as String,
                            userRole = data["user_role"] as String,
                            userDepartment = data["user_department"] as String,
                            action = data["action"] as String,
                            tableName = data["table_name"] as String,
                            recordId = UUID.fromString(data["record_id"] as String),
                            description = data["description"] as String,
                            isCritical = data["is_critical"] as Boolean,
                            ipAddress = data["ip_address"] as String?,
                            oldData = (data["old_data"] as? Map<String, String>)?.mapValues { it.value.toString() },
                            newData = (data["new_data"] as? Map<String, String>)?.mapValues { it.value.toString() }
                        )
                    } ?: emptyList()
                }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flow { emptyList<com.kprflow.enterprise.domain.model.AuditLog>() }
        }
    }
    
    /**
     * Get consumer audit timeline
     */
    fun getConsumerAuditTimeline(dossierId: UUID): Flow<List<com.kprflow.enterprise.domain.model.AuditLog>> {
        return try {
            supabaseClient.from("AuditLogHistory")
                .select {
                    or(
                        eq("record_id", dossierId),
                        and(
                            eq("table_name", "FinancialTransaction"),
                            in_("record_id", 
                                supabaseClient.from("FinancialTransaction")
                                    .select("id")
                                    .eq("dossier_id", dossierId)
                            )
                        ),
                        and(
                            eq("table_name", "Document"),
                            in_("record_id", 
                                supabaseClient.from("Document")
                                    .select("id")
                                    .eq("dossier_id", dossierId)
                            )
                        )
                    )
                }
                .order("created_at", com.supabase.gotrue.PostgrestQuery.Order.DESCENDING)
                .flow()
                .map { response ->
                    response.data?.mapNotNull { it ->
                        it as? com.supabase.gotrue.PostgrestResult.MapData
                    }?.map { data ->
                        com.kprflow.enterprise.domain.model.AuditLog(
                            id = UUID.fromString(data["id"] as String),
                            createdAt = (data["created_at"] as String).toTimestamp(),
                            userName = data["user_name"] as String,
                            userRole = data["user_role"] as String,
                            userDepartment = data["user_department"] as String,
                            action = data["action"] as String,
                            tableName = data["table_name"] as String,
                            recordId = UUID.fromString(data["record_id"] as String),
                            description = data["description"] as String,
                            isCritical = data["is_critical"] as Boolean,
                            ipAddress = data["ip_address"] as String?,
                            oldData = (data["old_data"] as? Map<String, String>)?.mapValues { it.value.toString() },
                            newData = (data["new_data"] as? Map<String, String>)?.mapValues { it.value.toString() }
                        )
                    } ?: emptyList()
                }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flow { emptyList<com.kprflow.enterprise.domain.model.AuditLog>() }
        }
    }
    
    /**
     * Set user context for audit logging
     */
    suspend fun setUserContext(
        userId: UUID,
        ipAddress: String? = null,
        userAgent: String? = null,
        sessionId: UUID? = null
    ) {
        try {
            supabaseClient.rpc("set_user_context") {
                param("p_user_id", userId.toString())
                param("p_ip_address", ipAddress)
                param("p_user_agent", userAgent)
                param("p_session_id", sessionId?.toString())
            }
        } catch (e: Exception) {
            // Handle error silently for now
        }
    }
}

/**
 * Extension function to convert timestamp string to Long
 */
private fun String.toTimestamp(): Long {
    return try {
        java.time.Instant.parse(this).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}
