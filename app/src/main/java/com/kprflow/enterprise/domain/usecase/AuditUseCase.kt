package com.kprflow.enterprise.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audit Use Case - Domain Layer for Audit Log Management
 * Anti-Tamper System for KPRFlow Enterprise
 */
@Singleton
class AuditUseCase @Inject constructor(
    private val auditRepository: com.kprflow.enterprise.domain.repository.AuditRepository
) {
    
    /**
     * Get audit history for a specific record
     */
    fun getAuditHistory(recordId: UUID): Flow<List<AuditHistoryItem>> {
        return auditRepository.getAuditHistory(recordId).map { logs ->
            logs.map { log ->
                AuditHistoryItem(
                    id = log.id,
                    timestamp = log.createdAt,
                    userName = log.userName,
                    userRole = log.userRole,
                    userDepartment = log.userDepartment,
                    action = log.action,
                    description = log.description,
                    isCritical = log.isCritical,
                    ipAddress = log.ipAddress,
                    details = buildAuditDetails(log)
                )
            }
        }
    }
    
    /**
     * Get audit history for a specific table
     */
    fun getTableAuditHistory(tableName: String): Flow<List<AuditHistoryItem>> {
        return auditRepository.getTableAuditHistory(tableName).map { logs ->
            logs.map { log ->
                AuditHistoryItem(
                    id = log.id,
                    timestamp = log.createdAt,
                    userName = log.userName,
                    userRole = log.userRole,
                    userDepartment = log.userDepartment,
                    action = log.action,
                    description = log.description,
                    isCritical = log.isCritical,
                    ipAddress = log.ipAddress,
                    details = buildAuditDetails(log)
                )
            }
        }
    }
    
    /**
     * Get audit history for a specific user
     */
    fun getUserAuditHistory(userId: UUID): Flow<List<AuditHistoryItem>> {
        return auditRepository.getUserAuditHistory(userId).map { logs ->
            logs.map { log ->
                AuditHistoryItem(
                    id = log.id,
                    timestamp = log.createdAt,
                    userName = log.userName,
                    userRole = log.userRole,
                    userDepartment = log.userDepartment,
                    action = log.action,
                    description = log.description,
                    isCritical = log.isCritical,
                    ipAddress = log.ipAddress,
                    details = buildAuditDetails(log)
                )
            }
        }
    }
    
    /**
     * Get critical audit logs (for BOD and Managers)
     */
    fun getCriticalAuditLogs(): Flow<List<AuditHistoryItem>> {
        return auditRepository.getCriticalAuditLogs().map { logs ->
            logs.map { log ->
                AuditHistoryItem(
                    id = log.id,
                    timestamp = log.createdAt,
                    userName = log.userName,
                    userRole = log.userRole,
                    userDepartment = log.userDepartment,
                    action = log.action,
                    description = log.description,
                    isCritical = log.isCritical,
                    ipAddress = log.ipAddress,
                    details = buildAuditDetails(log)
                )
            }
        }
    }
    
    /**
     * Get audit statistics
     */
    suspend fun getAuditStatistics(): AuditStatistics {
        return auditRepository.getAuditStatistics()
    }
    
    /**
     * Search audit logs
     */
    fun searchAuditLogs(query: String): Flow<List<AuditHistoryItem>> {
        return auditRepository.searchAuditLogs(query).map { logs ->
            logs.map { log ->
                AuditHistoryItem(
                    id = log.id,
                    timestamp = log.createdAt,
                    userName = log.userName,
                    userRole = log.userRole,
                    userDepartment = log.userDepartment,
                    action = log.action,
                    description = log.description,
                    isCritical = log.isCritical,
                    ipAddress = log.ipAddress,
                    details = buildAuditDetails(log)
                )
            }
        }
    }
    
    /**
     * Build detailed audit information
     */
    private fun buildAuditDetails(log: com.kprflow.enterprise.domain.model.AuditLog): AuditDetails {
        return when (log.tableName) {
            "KPRDossier" -> buildKPRDossierAuditDetails(log)
            "UnitProperty" -> buildUnitPropertyAuditDetails(log)
            "FinancialTransaction" -> buildFinancialTransactionAuditDetails(log)
            "Document" -> buildDocumentAuditDetails(log)
            "UserProfile" -> buildUserProfileAuditDetails(log)
            else -> AuditDetails(
                tableName = log.tableName,
                recordId = log.recordId,
                oldData = log.oldData,
                newData = log.newData,
                changes = extractChanges(log.oldData, log.newData)
            )
        }
    }
    
    /**
     * Build KPR Dossier specific audit details
     */
    private fun buildKPRDossierAuditDetails(log: com.kprflow.enterprise.domain.model.AuditLog): AuditDetails {
        val oldData = log.oldData
        val newData = log.newData
        
        return AuditDetails(
            tableName = log.tableName,
            recordId = log.recordId,
            oldData = oldData,
            newData = newData,
            changes = extractChanges(oldData, newData),
            specificDetails = KPRDossierAuditDetails(
                applicantId = newData?.get("applicant_id"),
                propertyId = newData?.get("property_id"),
                statusChange = StatusChange(
                    from = oldData?.get("status"),
                    to = newData?.get("status")
                ),
                financialChange = FinancialChange(
                    loanAmountFrom = oldData?.get("loan_amount"),
                    loanAmountTo = newData?.get("loan_amount"),
                    downPaymentFrom = oldData?.get("down_payment"),
                    downPaymentTo = newData?.get("down_payment"),
                    interestRateFrom = oldData?.get("interest_rate"),
                    interestRateTo = newData?.get("interest_rate")
                )
            )
        )
    }
    
    /**
     * Build Unit Property specific audit details
     */
    private fun buildUnitPropertyAuditDetails(log: com.kprflow.enterprise.domain.model.AuditLog): AuditDetails {
        val oldData = log.oldData
        val newData = log.newData
        
        return AuditDetails(
            tableName = log.tableName,
            recordId = log.recordId,
            oldData = oldData,
            newData = newData,
            changes = extractChanges(oldData, newData),
            specificDetails = UnitPropertyAuditDetails(
                block = newData?.get("block"),
                type = newData?.get("type"),
                priceChange = PriceChange(
                    from = oldData?.get("price"),
                    to = newData?.get("price")
                ),
                statusChange = StatusChange(
                    from = oldData?.get("status"),
                    to = newData?.get("status")
                )
            )
        )
    }
    
    /**
     * Build Financial Transaction specific audit details
     */
    private fun buildFinancialTransactionAuditDetails(log: com.kprflow.enterprise.domain.model.AuditLog): AuditDetails {
        val oldData = log.oldData
        val newData = log.newData
        
        return AuditDetails(
            tableName = log.tableName,
            recordId = log.recordId,
            oldData = oldData,
            newData = newData,
            changes = extractChanges(oldData, newData),
            specificDetails = FinancialTransactionAuditDetails(
                dossierId = newData?.get("dossier_id"),
                type = newData?.get("type"),
                amountChange = AmountChange(
                    from = oldData?.get("amount"),
                    to = newData?.get("amount")
                ),
                statusChange = StatusChange(
                    from = oldData?.get("status"),
                    to = newData?.get("status")
                )
            )
        )
    }
    
    /**
     * Build Document specific audit details
     */
    private fun buildDocumentAuditDetails(log: com.kprflow.enterprise.domain.model.AuditLog): AuditDetails {
        val oldData = log.oldData
        val newData = log.newData
        
        return AuditDetails(
            tableName = log.tableName,
            recordId = log.recordId,
            oldData = oldData,
            newData = newData,
            changes = extractChanges(oldData, newData),
            specificDetails = DocumentAuditDetails(
                dossierId = newData?.get("dossier_id"),
                type = newData?.get("type"),
                fileName = newData?.get("file_name"),
                statusChange = StatusChange(
                    from = oldData?.get("status"),
                    to = newData?.get("status")
                )
            )
        )
    }
    
    /**
     * Build User Profile specific audit details
     */
    private fun buildUserProfileAuditDetails(log: com.kprflow.enterprise.domain.model.AuditLog): AuditDetails {
        val oldData = log.oldData
        val newData = log.newData
        
        return AuditDetails(
            tableName = log.tableName,
            recordId = log.recordId,
            oldData = oldData,
            newData = newData,
            changes = extractChanges(oldData, newData),
            specificDetails = UserProfileAuditDetails(
                email = newData?.get("email"),
                roleChange = StatusChange(
                    from = oldData?.get("role"),
                    to = newData?.get("role")
                ),
                departmentChange = StatusChange(
                    from = oldData?.get("department"),
                    to = newData?.get("department")
                ),
                statusChange = StatusChange(
                    from = oldData?.get("status"),
                    to = newData?.get("status")
                )
            )
        )
    }
    
    /**
     * Extract changes from old and new data
     */
    private fun extractChanges(oldData: Map<String, String>?, newData: Map<String, String>?): List<FieldChange> {
        val changes = mutableListOf<FieldChange>()
        
        if (oldData != null && newData != null) {
            // Find all keys from both old and new data
            val allKeys = (oldData.keys + newData.keys).toSet()
            
            for (key in allKeys) {
                val oldValue = oldData[key]
                val newValue = newData[key]
                
                if (oldValue != newValue) {
                    changes.add(
                        FieldChange(
                            fieldName = key,
                            oldValue = oldValue,
                            newValue = newValue
                        )
                    )
                }
            }
        }
        
        return changes
    }
    
    /**
     * Get audit timeline for consumer detail view
     */
    fun getConsumerAuditTimeline(dossierId: UUID): Flow<List<TimelineItem>> {
        return auditRepository.getConsumerAuditTimeline(dossierId).map { logs ->
            logs.map { log ->
                TimelineItem(
                    id = log.id,
                    timestamp = log.createdAt,
                    type = when {
                        log.isCritical -> TimelineType.CRITICAL
                        log.action == "INSERT" -> TimelineType.CREATED
                        log.action == "UPDATE" -> TimelineType.UPDATED
                        log.action == "DELETE" -> TimelineType.DELETED
                        else -> TimelineType.GENERAL
                    },
                    title = generateTimelineTitle(log),
                    description = log.description,
                    userName = log.userName,
                    userRole = log.userRole,
                    userDepartment = log.userDepartment,
                    icon = getTimelineIcon(log),
                    color = getTimelineColor(log)
                )
            }
        }
    }
    
    /**
     * Generate timeline title
     */
    private fun generateTimelineTitle(log: com.kprflow.enterprise.domain.model.AuditLog): String {
        return when (log.tableName) {
            "KPRDossier" -> {
                when {
                    log.action == "INSERT" -> "KPR Application Created"
                    log.action == "UPDATE" && log.oldData?.get("status") != log.newData?.get("status") -> 
                        "Status Changed to ${log.newData?.get("status")}"
                    log.action == "UPDATE" -> "KPR Application Updated"
                    log.action == "DELETE" -> "KPR Application Deleted"
                    else -> "KPR Application Modified"
                }
            }
            "FinancialTransaction" -> {
                when {
                    log.action == "INSERT" -> "Financial Transaction Created"
                    log.action == "UPDATE" && log.oldData?.get("status") != log.newData?.get("status") -> 
                        "Payment Status Changed to ${log.newData?.get("status")}"
                    log.action == "UPDATE" -> "Financial Transaction Updated"
                    log.action == "DELETE" -> "Financial Transaction Deleted"
                    else -> "Financial Transaction Modified"
                }
            }
            "Document" -> {
                when {
                    log.action == "INSERT" -> "Document Uploaded: ${log.newData?.get("type")}"
                    log.action == "UPDATE" && log.oldData?.get("status") != log.newData?.get("status") -> 
                        "Document Status Changed to ${log.newData?.get("status")}"
                    log.action == "UPDATE" -> "Document Updated"
                    log.action == "DELETE" -> "Document Deleted"
                    else -> "Document Modified"
                }
            }
            else -> "${log.action} ${log.tableName}"
        }
    }
    
    /**
     * Get timeline icon
     */
    private fun getTimelineIcon(log: com.kprflow.enterprise.domain.model.AuditLog): String {
        return when (log.tableName) {
            "KPRDossier" -> {
                when (log.action) {
                    "INSERT" -> "📝"
                    "UPDATE" -> "🔄"
                    "DELETE" -> "🗑️"
                    else -> "📄"
                }
            }
            "FinancialTransaction" -> {
                when (log.action) {
                    "INSERT" -> "💰"
                    "UPDATE" -> "💸"
                    "DELETE" -> "🗑️"
                    else -> "💳"
                }
            }
            "Document" -> {
                when (log.action) {
                    "INSERT" -> "📎"
                    "UPDATE" -> "📄"
                    "DELETE" -> "🗑️"
                    else -> "📋"
                }
            }
            "UserProfile" -> {
                when (log.action) {
                    "INSERT" -> "👤"
                    "UPDATE" -> "🔄"
                    "DELETE" -> "🗑️"
                    else -> "👥"
                }
            }
            else -> "📋"
        }
    }
    
    /**
     * Get timeline color
     */
    private fun getTimelineColor(log: com.kprflow.enterprise.domain.model.AuditLog): String {
        return when {
            log.isCritical -> "#FF0000" // Red for critical
            log.action == "INSERT" -> "#00FF00" // Green for create
            log.action == "UPDATE" -> "#0080FF" // Blue for update
            log.action == "DELETE" -> "#FF8000" // Orange for delete
            else -> "#808080" // Gray for general
        }
    }
}

/**
 * Audit History Item
 */
data class AuditHistoryItem(
    val id: UUID,
    val timestamp: Long,
    val userName: String,
    val userRole: String,
    val userDepartment: String,
    val action: String,
    val description: String,
    val isCritical: Boolean,
    val ipAddress: String?,
    val details: AuditDetails
)

/**
 * Audit Details
 */
data class AuditDetails(
    val tableName: String,
    val recordId: UUID,
    val oldData: Map<String, String>?,
    val newData: Map<String, String>?,
    val changes: List<FieldChange>,
    val specificDetails: Any? = null
)

/**
 * Field Change
 */
data class FieldChange(
    val fieldName: String,
    val oldValue: String?,
    val newValue: String?
)

/**
 * Status Change
 */
data class StatusChange(
    val from: String?,
    val to: String?
)

/**
 * Price Change
 */
data class PriceChange(
    val from: String?,
    val to: String?
)

/**
 * Amount Change
 */
data class AmountChange(
    val from: String?,
    val to: String?
)

/**
 * Financial Change
 */
data class FinancialChange(
    val loanAmountFrom: String?,
    val loanAmountTo: String?,
    val downPaymentFrom: String?,
    val downPaymentTo: String?,
    val interestRateFrom: String?,
    val interestRateTo: String?
)

/**
 * KPR Dossier Audit Details
 */
data class KPRDossierAuditDetails(
    val applicantId: String?,
    val propertyId: String?,
    val statusChange: StatusChange,
    val financialChange: FinancialChange
)

/**
 * Unit Property Audit Details
 */
data class UnitPropertyAuditDetails(
    val block: String?,
    val type: String?,
    val priceChange: PriceChange,
    val statusChange: StatusChange
)

/**
 * Financial Transaction Audit Details
 */
data class FinancialTransactionAuditDetails(
    val dossierId: String?,
    val type: String?,
    val amountChange: AmountChange,
    val statusChange: StatusChange
)

/**
 * Document Audit Details
 */
data class DocumentAuditDetails(
    val dossierId: String?,
    val type: String?,
    val fileName: String?,
    val statusChange: StatusChange
)

/**
 * User Profile Audit Details
 */
data class UserProfileAuditDetails(
    val email: String?,
    val roleChange: StatusChange,
    val departmentChange: StatusChange,
    val statusChange: StatusChange
)

/**
 * Timeline Item
 */
data class TimelineItem(
    val id: UUID,
    val timestamp: Long,
    val type: TimelineType,
    val title: String,
    val description: String,
    val userName: String,
    val userRole: String,
    val userDepartment: String,
    val icon: String,
    val color: String
)

/**
 * Timeline Type
 */
enum class TimelineType {
    CREATED,
    UPDATED,
    DELETED,
    CRITICAL,
    GENERAL
}

/**
 * Audit Statistics
 */
data class AuditStatistics(
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
