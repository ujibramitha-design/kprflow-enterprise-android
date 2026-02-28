package com.kprflow.enterprise.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SLAMonitoringRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    
    companion object {
        const val DOCUMENT_PROCESSING_SLA_DAYS = 14L
        const val BANK_DECISION_SLA_DAYS = 60L
        const val SP3K_ISSUANCE_SLA_DAYS = 7L
        const val DISBURSEMENT_SLA_DAYS = 10L
        const val BAST_COMPLETION_SLA_DAYS = 30L
    }
    
    suspend fun getSLAComplianceOverview(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<SLAComplianceOverview> {
        return try {
            // Get document processing SLA metrics
            val documentSLA = getDocumentProcessingSLA(startDate, endDate)
                .getOrNull() ?: DocumentSLAMetrics(0, 0, 0, 0.0, 0.0)
            
            // Get bank decision SLA metrics
            val bankSLA = getBankDecisionSLA(startDate, endDate)
                .getOrNull() ?: BankSLAMetrics(0, 0, 0, 0.0, 0.0)
            
            // Get other SLA metrics
            val sp3kSLA = getSP3KIssuanceSLA(startDate, endDate)
                .getOrNull() ?: SP3KSLAMetrics(0, 0, 0.0)
            
            val disbursementSLA = getDisbursementSLA(startDate, endDate)
                .getOrNull() ?: DisbursementSLAMetrics(0, 0, 0.0)
            
            val bastSLA = getBASTCompletionSLA(startDate, endDate)
                .getOrNull() ?: BASTSLAMetrics(0, 0, 0.0)
            
            // Calculate overall compliance
            val totalItems = documentSLA.totalItems + bankSLA.totalItems + 
                            sp3kSLA.totalItems + disbursementSLA.totalItems + bastSLA.totalItems
            
            val totalCompliant = documentSLA.compliantItems + bankSLA.compliantItems + 
                               sp3kSLA.compliantItems + disbursementSLA.compliantItems + bastSLA.compliantItems
            
            val overallComplianceRate = if (totalItems > 0) {
                (totalCompliant.toDouble() / totalItems) * 100
            } else 0.0
            
            val overview = SLAComplianceOverview(
                documentProcessingSLA = documentSLA,
                bankDecisionSLA = bankSLA,
                sp3kIssuanceSLA = sp3kSLA,
                disbursementSLA = disbursementSLA,
                bastCompletionSLA = bastSLA,
                overallComplianceRate = overallComplianceRate,
                totalItems = totalItems,
                totalCompliantItems = totalCompliant,
                totalOverdueItems = (documentSLA.overdueItems + bankSLA.overdueItems + 
                                  sp3kSLA.overdueItems + disbursementSLA.overdueItems + 
                                  bastSLA.overdueItems).toInt(),
                generatedAt = Instant.now().toString()
            )
            
            Result.success(overview)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDocumentProcessingSLA(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<DocumentSLAMetrics> {
        return try {
            // Get documents with processing times
            val documents = postgrest.from("documents")
                .select("uploaded_at, verified_at, is_verified, dossier_id")
                .let { query ->
                    if (startDate != null) {
                        query.filter { gte("uploaded_at", startDate.toString()) }
                    } else query
                }
                .let { query ->
                    if (endDate != null) {
                        query.filter { lte("uploaded_at", endDate.toString()) }
                    } else query
                }
                .data
            
            val totalItems = documents.size
            var compliantItems = 0
            var overdueItems = 0
            var totalProcessingDays = 0.0
            val overdueDocuments = mutableListOf<OverdueItem>()
            
            documents.forEach { doc ->
                val uploadedAt = Instant.parse(doc.uploaded_at)
                val verifiedAt = if (doc.is_verified && doc.verified_at != null) {
                    Instant.parse(doc.verified_at!!)
                } else {
                    Instant.now() // Use current time for unverified documents
                }
                
                val processingDays = ChronoUnit.DAYS.between(uploadedAt, verifiedAt).toDouble()
                totalProcessingDays += processingDays
                
                val isCompliant = processingDays <= DOCUMENT_PROCESSING_SLA_DAYS
                if (isCompliant) {
                    compliantItems++
                } else {
                    overdueItems++
                    
                    // Add to overdue list if still not verified
                    if (!doc.is_verified) {
                        overdueDocuments.add(
                            OverdueItem(
                                id = doc.dossier_id,
                                type = "Document Processing",
                                itemName = "Document Verification",
                                overdueDays = (processingDays - DOCUMENT_PROCESSING_SLA_DAYS).toInt(),
                                slaDays = DOCUMENT_PROCESSING_SLA_DAYS.toInt(),
                                actualDays = processingDays.toInt(),
                                responsibleTeam = "Legal",
                                priority = if (processingDays > DOCUMENT_PROCESSING_SLA_DAYS * 2) "HIGH" else "MEDIUM"
                            )
                        )
                    }
                }
            }
            
            val averageProcessingDays = if (totalItems > 0) totalProcessingDays / totalItems else 0.0
            val complianceRate = if (totalItems > 0) (compliantItems.toDouble() / totalItems) * 100 else 0.0
            
            val metrics = DocumentSLAMetrics(
                totalItems = totalItems,
                compliantItems = compliantItems,
                overdueItems = overdueItems,
                averageProcessingDays = averageProcessingDays,
                complianceRate = complianceRate,
                overdueDocuments = overdueDocuments
            )
            
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getBankDecisionSLA(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<BankSLAMetrics> {
        return try {
            // Get bank decisions with processing times
            val decisions = postgrest.from("bank_decisions")
                .select("uploaded_at, processed_at, status, dossier_id, bank_name")
                .let { query ->
                    if (startDate != null) {
                        query.filter { gte("uploaded_at", startDate.toString()) }
                    } else query
                }
                .let { query ->
                    if (endDate != null) {
                        query.filter { lte("uploaded_at", endDate.toString()) }
                    } else query
                }
                .data
            
            val totalItems = decisions.size
            var compliantItems = 0
            var overdueItems = 0
            var totalProcessingDays = 0.0
            val overdueDecisions = mutableListOf<OverdueItem>()
            
            decisions.forEach { decision ->
                val uploadedAt = Instant.parse(decision.uploaded_at)
                val processedAt = if (decision.status == "PROCESSED" && decision.processed_at != null) {
                    Instant.parse(decision.processed_at!!)
                } else {
                    Instant.now() // Use current time for unprocessed decisions
                }
                
                val processingDays = ChronoUnit.DAYS.between(uploadedAt, processedAt).toDouble()
                totalProcessingDays += processingDays
                
                val isCompliant = processingDays <= BANK_DECISION_SLA_DAYS
                if (isCompliant) {
                    compliantItems++
                } else {
                    overdueItems++
                    
                    // Add to overdue list if still not processed
                    if (decision.status != "PROCESSED") {
                        overdueDecisions.add(
                            OverdueItem(
                                id = decision.dossier_id,
                                type = "Bank Decision",
                                itemName = "Bank: ${decision.bank_name}",
                                overdueDays = (processingDays - BANK_DECISION_SLA_DAYS).toInt(),
                                slaDays = BANK_DECISION_SLA_DAYS.toInt(),
                                actualDays = processingDays.toInt(),
                                responsibleTeam = "Marketing",
                                priority = if (processingDays > BANK_DECISION_SLA_DAYS * 1.5) "HIGH" else "MEDIUM"
                            )
                        )
                    }
                }
            }
            
            val averageProcessingDays = if (totalItems > 0) totalProcessingDays / totalItems else 0.0
            val complianceRate = if (totalItems > 0) (compliantItems.toDouble() / totalItems) * 100 else 0.0
            
            val metrics = BankSLAMetrics(
                totalItems = totalItems,
                compliantItems = compliantItems,
                overdueItems = overdueItems,
                averageProcessingDays = averageProcessingDays,
                complianceRate = complianceRate,
                overdueDecisions = overdueDecisions
            )
            
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSP3KIssuanceSLA(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<SP3KSLAMetrics> {
        return try {
            // Get SP3K issuance data
            val dossiers = postgrest.from("kpr_dossiers")
                .select("sp3k_issued_date, updated_at, status, id")
                .filter { eq("status", "PUTUSAN_KREDIT_ACC") }
                .let { query ->
                    if (startDate != null) {
                        query.filter { gte("updated_at", startDate.toString()) }
                    } else query
                }
                .let { query ->
                    if (endDate != null) {
                        query.filter { lte("updated_at", endDate.toString()) }
                    } else query
                }
                .data
            
            val totalItems = dossiers.size
            var compliantItems = 0
            var overdueItems = 0
            var totalProcessingDays = 0.0
            
            dossiers.forEach { dossier ->
                val approvedAt = Instant.parse(dossier.updated_at)
                val sp3kIssuedAt = if (dossier.sp3k_issued_date != null) {
                    LocalDate.parse(dossier.sp3k_issued_date).atStartOfDay(ZoneId.systemDefault()).toInstant()
                } else {
                    Instant.now() // Use current time for unissued SP3K
                }
                
                val processingDays = ChronoUnit.DAYS.between(approvedAt, sp3kIssuedAt).toDouble()
                totalProcessingDays += processingDays
                
                val isCompliant = processingDays <= SP3K_ISSUANCE_SLA_DAYS
                if (isCompliant) {
                    compliantItems++
                } else {
                    overdueItems++
                }
            }
            
            val averageProcessingDays = if (totalItems > 0) totalProcessingDays / totalItems else 0.0
            val complianceRate = if (totalItems > 0) (compliantItems.toDouble() / totalItems) * 100 else 0.0
            val overdueCount = totalItems - compliantItems
            
            val metrics = SP3KSLAMetrics(
                totalItems = totalItems,
                compliantItems = compliantItems,
                overdueItems = overdueCount,
                averageProcessingDays = averageProcessingDays,
                complianceRate = complianceRate
            )
            
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDisbursementSLA(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<DisbursementSLAMetrics> {
        return try {
            // Get disbursement data
            val dossiers = postgrest.from("kpr_dossiers")
                .select("sp3k_issued_date, disbursed_date, status, id")
                .filter { eq("status", "SP3K_TERBIT") }
                .let { query ->
                    if (startDate != null) {
                        query.filter { gte("sp3k_issued_date", startDate.toString()) }
                    } else query
                }
                .let { query ->
                    if (endDate != null) {
                        query.filter { lte("sp3k_issued_date", endDate.toString()) }
                    } else query
                }
                .data
            
            val totalItems = dossiers.size
            var compliantItems = 0
            var overdueItems = 0
            var totalProcessingDays = 0.0
            
            dossiers.forEach { dossier ->
                val sp3kIssuedAt = LocalDate.parse(dossier.sp3k_issued_date).atStartOfDay(ZoneId.systemDefault()).toInstant()
                val disbursedAt = if (dossier.disbursed_date != null) {
                    LocalDate.parse(dossier.disbursed_date).atStartOfDay(ZoneId.systemDefault()).toInstant()
                } else {
                    Instant.now() // Use current time for undisbursed
                }
                
                val processingDays = ChronoUnit.DAYS.between(sp3kIssuedAt, disbursedAt).toDouble()
                totalProcessingDays += processingDays
                
                val isCompliant = processingDays <= DISBURSEMENT_SLA_DAYS
                if (isCompliant) {
                    compliantItems++
                } else {
                    overdueItems++
                }
            }
            
            val averageProcessingDays = if (totalItems > 0) totalProcessingDays / totalItems else 0.0
            val complianceRate = if (totalItems > 0) (compliantItems.toDouble() / totalItems) * 100 else 0.0
            val overdueCount = totalItems - compliantItems
            
            val metrics = DisbursementSLAMetrics(
                totalItems = totalItems,
                compliantItems = compliantItems,
                overdueItems = overdueCount,
                averageProcessingDays = averageProcessingDays,
                complianceRate = complianceRate
            )
            
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getBASTCompletionSLA(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<BASTSLAMetrics> {
        return try {
            // Get BAST completion data
            val dossiers = postgrest.from("kpr_dossiers")
                .select("disbursed_date, bast_date, status, id")
                .filter { eq("status", "FUNDS_DISBURSED") }
                .let { query ->
                    if (startDate != null) {
                        query.filter { gte("disbursed_date", startDate.toString()) }
                    } else query
                }
                .let { query ->
                    if (endDate != null) {
                        query.filter { lte("disbursed_date", endDate.toString()) }
                    } else query
                }
                .data
            
            val totalItems = dossiers.size
            var compliantItems = 0
            var overdueItems = 0
            var totalProcessingDays = 0.0
            
            dossiers.forEach { dossier ->
                val disbursedAt = LocalDate.parse(dossier.disbursed_date).atStartOfDay(ZoneId.systemDefault()).toInstant()
                val bastCompletedAt = if (dossier.bast_date != null) {
                    LocalDate.parse(dossier.bast_date).atStartOfDay(ZoneId.systemDefault()).toInstant()
                } else {
                    Instant.now() // Use current time for incomplete BAST
                }
                
                val processingDays = ChronoUnit.DAYS.between(disbursedAt, bastCompletedAt).toDouble()
                totalProcessingDays += processingDays
                
                val isCompliant = processingDays <= BAST_COMPLETION_SLA_DAYS
                if (isCompliant) {
                    compliantItems++
                } else {
                    overdueItems++
                }
            }
            
            val averageProcessingDays = if (totalItems > 0) totalProcessingDays / totalItems else 0.0
            val complianceRate = if (totalItems > 0) (compliantItems.toDouble() / totalItems) * 100 else 0.0
            val overdueCount = totalItems - compliantItems
            
            val metrics = BASTSLAMetrics(
                totalItems = totalItems,
                compliantItems = compliantItems,
                overdueItems = overdueCount,
                averageProcessingDays = averageProcessingDays,
                complianceRate = complianceRate
            )
            
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getOverdueItems(): Result<List<OverdueItem>> {
        return try {
            val overdueItems = mutableListOf<OverdueItem>()
            
            // Get overdue documents
            val documentSLA = getDocumentProcessingSLA().getOrNull()
            documentSLA?.overdueDocuments?.let { overdueItems.addAll(it) }
            
            // Get overdue bank decisions
            val bankSLA = getBankDecisionSLA().getOrNull()
            bankSLA?.overdueDecisions?.let { overdueItems.addAll(it) }
            
            // Sort by priority and overdue days
            val sortedItems = overdueItems.sortedWith(compareByDescending<OverdueItem> { it.priority }
                .thenByDescending { it.overdueDays })
            
            Result.success(sortedItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateSLABreachReport(
        reportType: SLAReportType,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<SLABreachReport> {
        return try {
            val overview = getSLAComplianceOverview(startDate, endDate).getOrNull()
                ?: return Result.failure(Exception("Failed to generate SLA overview"))
            
            val overdueItems = getOverdueItems().getOrNull().orEmpty()
            
            val report = SLABreachReport(
                id = java.util.UUID.randomUUID().toString(),
                reportType = reportType,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                slaOverview = overview,
                overdueItems = overdueItems,
                generatedAt = Instant.now().toString(),
                generatedBy = "system" // TODO: Get current user
            )
            
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendSLAAlerts(): Result<List<SLAAlert>> {
        return try {
            val alerts = mutableListOf<SLAAlert>()
            val overdueItems = getOverdueItems().getOrNull().orEmpty()
            
            overdueItems.forEach { item ->
                val alert = SLAAlert(
                    id = java.util.UUID.randomUUID().toString(),
                    itemId = item.id,
                    itemType = item.type,
                    itemName = item.itemName,
                    overdueDays = item.overdueDays,
                    slaDays = item.slaDays,
                    responsibleTeam = item.responsibleTeam,
                    priority = item.priority,
                    message = generateAlertMessage(item),
                    createdAt = Instant.now().toString(),
                    sentAt = null,
                    acknowledgedAt = null
                )
                alerts.add(alert)
            }
            
            Result.success(alerts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeSLAUpdates(): Flow<SLAUpdate> = flow {
        try {
            // TODO: Implement real-time updates via Supabase Realtime
            emit(SLAUpdate.OverdueItemsChanged)
            emit(SLAUpdate.ComplianceRateChanged)
        } catch (e: Exception) {
            emit(SLAUpdate.Error(e.message ?: "Unknown error"))
        }
    }
    
    private fun generateAlertMessage(item: OverdueItem): String {
        return when (item.priority) {
            "HIGH" -> "URGENT: ${item.itemName} is ${item.overdueDays} days overdue (SLA: ${item.slaDays} days). Immediate action required!"
            "MEDIUM" -> "WARNING: ${item.itemName} is ${item.overdueDays} days overdue (SLA: ${item.slaDays} days). Please address promptly."
            else -> "INFO: ${item.itemName} is ${item.overdueDays} days overdue (SLA: ${item.slaDays} days)."
        }
    }
}

// Data classes
data class SLAComplianceOverview(
    val documentProcessingSLA: DocumentSLAMetrics,
    val bankDecisionSLA: BankSLAMetrics,
    val sp3kIssuanceSLA: SP3KSLAMetrics,
    val disbursementSLA: DisbursementSLAMetrics,
    val bastCompletionSLA: BASTSLAMetrics,
    val overallComplianceRate: Double,
    val totalItems: Int,
    val totalCompliantItems: Int,
    val totalOverdueItems: Int,
    val generatedAt: String
)

data class DocumentSLAMetrics(
    val totalItems: Int,
    val compliantItems: Int,
    val overdueItems: Int,
    val averageProcessingDays: Double,
    val complianceRate: Double,
    val overdueDocuments: List<OverdueItem> = emptyList()
)

data class BankSLAMetrics(
    val totalItems: Int,
    val compliantItems: Int,
    val overdueItems: Int,
    val averageProcessingDays: Double,
    val complianceRate: Double,
    val overdueDecisions: List<OverdueItem> = emptyList()
)

data class SP3KSLAMetrics(
    val totalItems: Int,
    val compliantItems: Int,
    val overdueItems: Int,
    val averageProcessingDays: Double,
    val complianceRate: Double
)

data class DisbursementSLAMetrics(
    val totalItems: Int,
    val compliantItems: Int,
    val overdueItems: Int,
    val averageProcessingDays: Double,
    val complianceRate: Double
)

data class BASTSLAMetrics(
    val totalItems: Int,
    val compliantItems: Int,
    val overdueItems: Int,
    val averageProcessingDays: Double,
    val complianceRate: Double
)

data class OverdueItem(
    val id: String,
    val type: String,
    val itemName: String,
    val overdueDays: Int,
    val slaDays: Int,
    val actualDays: Int,
    val responsibleTeam: String,
    val priority: String // "HIGH", "MEDIUM", "LOW"
)

enum class SLAReportType {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY
}

data class SLABreachReport(
    val id: String,
    val reportType: SLAReportType,
    val startDate: String,
    val endDate: String,
    val slaOverview: SLAComplianceOverview,
    val overdueItems: List<OverdueItem>,
    val generatedAt: String,
    val generatedBy: String
)

data class SLAAlert(
    val id: String,
    val itemId: String,
    val itemType: String,
    val itemName: String,
    val overdueDays: Int,
    val slaDays: Int,
    val responsibleTeam: String,
    val priority: String,
    val message: String,
    val createdAt: String,
    val sentAt: String?,
    val acknowledgedAt: String?
)

sealed class SLAUpdate {
    object OverdueItemsChanged : SLAUpdate()
    object ComplianceRateChanged : SLAUpdate()
    object NewAlertTriggered : SLAUpdate()
    data class Error(val message: String) : SLAUpdate()
}
