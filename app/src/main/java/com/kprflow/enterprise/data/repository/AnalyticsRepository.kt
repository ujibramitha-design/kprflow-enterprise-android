package com.kprflow.enterprise.data.repository

import com.kprflow.enterprise.data.model.KprStatus
import com.kprflow.enterprise.domain.repository.IAnalyticsRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val postgrest: Postgrest
) : IAnalyticsRepository {
    
    suspend fun getKPRPipelineFunnel(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<KPRPipelineFunnel> {
        return try {
            // Get all dossiers with date filtering
            val dossiers = postgrest.from("kpr_dossiers")
                .select("status, created_at, updated_at, kpr_amount")
                .let { query ->
                    if (startDate != null) {
                        query.filter { gte("created_at", startDate.toString()) }
                    } else query
                }
                .let { query ->
                    if (endDate != null) {
                        query.filter { lte("created_at", endDate.toString()) }
                    } else query
                }
                .data
            
            // Calculate funnel metrics
            val totalLeads = dossiers.size
            val documentCollection = dossiers.count { it.status == KprStatus.PEMBERKASAN }
            val bankProcessing = dossiers.count { it.status == KprStatus.PROSES_BANK }
            val creditApproved = dossiers.count { it.status == KprStatus.PUTUSAN_KREDIT_ACC }
            val sp3kIssued = dossiers.count { it.status == KprStatus.SP3K_TERBIT }
            val fundsDisbursed = dossiers.count { it.status == KprStatus.FUNDS_DISBURSED }
            val bastCompleted = dossiers.count { it.status == KprStatus.BAST_COMPLETED }
            
            val funnel = KPRPipelineFunnel(
                totalLeads = totalLeads,
                documentCollection = documentCollection,
                bankProcessing = bankProcessing,
                creditApproved = creditApproved,
                sp3kIssued = sp3kIssued,
                fundsDisbursed = fundsDisbursed,
                bastCompleted = bastCompleted,
                leadToDocumentRate = if (totalLeads > 0) (documentCollection.toDouble() / totalLeads) * 100 else 0.0,
                documentToBankRate = if (documentCollection > 0) (bankProcessing.toDouble() / documentCollection) * 100 else 0.0,
                bankToApprovalRate = if (bankProcessing > 0) (creditApproved.toDouble() / bankProcessing) * 100 else 0.0,
                approvalToSp3kRate = if (creditApproved > 0) (sp3kIssued.toDouble() / creditApproved) * 100 else 0.0,
                sp3kToDisbursementRate = if (sp3kIssued > 0) (fundsDisbursed.toDouble() / sp3kIssued) * 100 else 0.0,
                disbursementToBastRate = if (fundsDisbursed > 0) (bastCompleted.toDouble() / fundsDisbursed) * 100 else 0.0,
                overallConversionRate = if (totalLeads > 0) (bastCompleted.toDouble() / totalLeads) * 100 else 0.0
            )
            
            Result.success(funnel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAverageProcessingTimePerPhase(): Result<List<PhaseProcessingTime>> {
        return try {
            val phases = listOf(
                KprStatus.LEAD to KprStatus.PEMBERKASAN,
                KprStatus.PEMBERKASAN to KprStatus.PROSES_BANK,
                KprStatus.PROSES_BANK to KprStatus.PUTUSAN_KREDIT_ACC,
                KprStatus.PUTUSAN_KREDIT_ACC to KprStatus.SP3K_TERBIT,
                KprStatus.SP3K_TERBIT to KprStatus.FUNDS_DISBURSED,
                KprStatus.FUNDS_DISBURSED to KprStatus.BAST_COMPLETED
            )
            
            val processingTimes = mutableListOf<PhaseProcessingTime>()
            
            phases.forEach { (fromStatus, toStatus) ->
                val phaseData = postgrest.rpc("get_phase_processing_time")
                    .params(mapOf(
                        "from_status" to fromStatus.name,
                        "to_status" to toStatus.name
                    ))
                    .data
                
                // Parse the result (simplified - in real implementation would parse actual data)
                val avgDays = when (fromStatus) {
                    KprStatus.LEAD -> 7.0 // Lead to Document Collection: 7 days average
                    KprStatus.PEMBERKASAN -> 14.0 // Document to Bank: 14 days average
                    KprStatus.PROSES_BANK -> 21.0 // Bank to Approval: 21 days average
                    KprStatus.PUTUSAN_KREDIT_ACC -> 5.0 // Approval to SP3K: 5 days average
                    KprStatus.SP3K_TERBIT -> 10.0 // SP3K to Disbursement: 10 days average
                    KprStatus.FUNDS_DISBURSED -> 14.0 // Disbursement to BAST: 14 days average
                    else -> 0.0
                }
                
                processingTimes.add(
                    PhaseProcessingTime(
                        phaseName = "${fromStatus.displayName} → ${toStatus.displayName}",
                        fromStatus = fromStatus,
                        toStatus = toStatus,
                        averageDays = avgDays,
                        minDays = avgDays * 0.5, // Mock calculation
                        maxDays = avgDays * 2.0, // Mock calculation
                        sampleSize = 50 // Mock sample size
                    )
                )
            }
            
            Result.success(processingTimes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getBankApprovalStatistics(): Result<List<BankApprovalStats>> {
        return try {
            // Get bank decisions with bank names
            val bankDecisions = postgrest.from("bank_decisions")
                .select("decision_type, bank_name, uploaded_at")
                .data
            
            // Group by bank and calculate statistics
            val bankStats = bankDecisions
                .groupBy { it.bankName }
                .map { (bankName, decisions) ->
                    val total = decisions.size
                    val approved = decisions.count { it.decision_type == "APPROVED" }
                    val rejected = decisions.count { it.decision_type == "REJECTED" }
                    val pending = decisions.count { it.decision_type == "PENDING" }
                    
                    BankApprovalStats(
                        bankName = bankName,
                        totalDecisions = total,
                        approvedDecisions = approved,
                        rejectedDecisions = rejected,
                        pendingDecisions = pending,
                        approvalRate = if (total > 0) (approved.toDouble() / total) * 100 else 0.0,
                        rejectionRate = if (total > 0) (rejected.toDouble() / total) * 100 else 0.0,
                        pendingRate = if (total > 0) (pending.toDouble() / total) * 100 else 0.0
                    )
                }
                .sortedByDescending { it.approvalRate }
            
            Result.success(bankStats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRevenueProjection(
        months: Int = 12
    ): Result<RevenueProjection> {
        return try {
            // Get completed dossiers with actual revenue
            val completedDossiers = postgrest.from("kpr_dossiers")
                .select("kpr_amount, completed_at, status")
                .filter { eq("status", "BAST_COMPLETED") }
                .data
            
            // Get pipeline dossiers with projected revenue
            val pipelineDossiers = postgrest.from("kpr_dossiers")
                .select("kpr_amount, status, created_at")
                .filter { 
                    `in`("status", listOf("LEAD", "PEMBERKASAN", "PROSES_BANK", "PUTUSAN_KREDIT_ACC", "SP3K_TERBIT", "FUNDS_DISBURSED"))
                }
                .data
            
            // Calculate actual revenue from completed dossiers
            val actualRevenue = completedDossiers
                .sumOf { it.kpr_amount ?: BigDecimal.ZERO }
            
            // Calculate projected revenue from pipeline
            val projectedRevenue = pipelineDossiers
                .map { dossier ->
                    val probability = when (dossier.status) {
                        KprStatus.LEAD -> 0.1 // 10% conversion probability
                        KprStatus.PEMBERKASAN -> 0.3 // 30% conversion probability
                        KprStatus.PROSES_BANK -> 0.5 // 50% conversion probability
                        KprStatus.PUTUSAN_KREDIT_ACC -> 0.8 // 80% conversion probability
                        KprStatus.SP3K_TERBIT -> 0.9 // 90% conversion probability
                        KprStatus.FUNDS_DISBURSED -> 0.95 // 95% conversion probability
                        else -> 0.0
                    }
                    
                    (dossier.kpr_amount ?: BigDecimal.ZERO) * BigDecimal(probability)
                }
                .sumOf { it }
            
            // Calculate monthly projection
            val monthlyProjection = (1..months).map { month ->
                val monthRevenue = projectedRevenue * BigDecimal(month.toDouble() / months)
                MonthRevenueProjection(
                    month = month,
                    projectedRevenue = monthRevenue,
                    cumulativeRevenue = monthRevenue * BigDecimal(month)
                )
            }
            
            val projection = RevenueProjection(
                actualRevenue = actualRevenue,
                projectedRevenue = projectedRevenue,
                totalProjectedRevenue = actualRevenue + projectedRevenue,
                monthlyProjections = monthlyProjection,
                confidenceLevel = 0.75, // 75% confidence level
                projectionPeriod = months
            )
            
            Result.success(projection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSLAComplianceMetrics(): Result<SLAComplianceMetrics> {
        return try {
            // Get document processing times
            val documentSLA = postgrest.rpc("get_document_sla_compliance")
                .data
            
            // Get bank processing times
            val bankSLA = postgrest.rpc("get_bank_sla_compliance")
                .data
            
            // Calculate compliance metrics (mock data for demonstration)
            val metrics = SLAComplianceMetrics(
                documentSLAComplianceRate = 85.5, // 85.5% of documents processed within 14 days
                bankSLAComplianceRate = 78.2, // 78.2% of bank decisions within 60 days
                averageDocumentProcessingTime = 12.3, // Average 12.3 days
                averageBankProcessingTime = 45.7, // Average 45.7 days
                overdueDocuments = 15, // 15 documents overdue
                overdueBankDecisions = 8, // 8 bank decisions overdue
                totalDocuments = 120, // Total documents processed
                totalBankDecisions = 95 // Total bank decisions processed
            )
            
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTeamPerformanceMetrics(): Result<TeamPerformanceMetrics> {
        return try {
            // Get performance metrics by role
            val marketingMetrics = getRolePerformanceMetrics("MARKETING")
            val legalMetrics = getRolePerformanceMetrics("LEGAL")
            val financeMetrics = getRolePerformanceMetrics("FINANCE")
            
            val metrics = TeamPerformanceMetrics(
                marketingMetrics = marketingMetrics,
                legalMetrics = legalMetrics,
                financeMetrics = financeMetrics,
                overallTeamEfficiency = 82.5 // Mock overall efficiency
            )
            
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getRolePerformanceMetrics(role: String): RolePerformanceMetrics {
        // Mock implementation - would query actual performance data
        return when (role) {
            "MARKETING" -> RolePerformanceMetrics(
                role = role,
                tasksCompleted = 45,
                averageTaskTime = 2.5,
                efficiency = 88.5,
                satisfactionScore = 4.2
            )
            "LEGAL" -> RolePerformanceMetrics(
                role = role,
                tasksCompleted = 32,
                averageTaskTime = 4.8,
                efficiency = 76.3,
                satisfactionScore = 4.1
            )
            "FINANCE" -> RolePerformanceMetrics(
                role = role,
                tasksCompleted = 28,
                averageTaskTime = 3.2,
                efficiency = 82.7,
                satisfactionScore = 4.3
            )
            else -> RolePerformanceMetrics(
                role = role,
                tasksCompleted = 0,
                averageTaskTime = 0.0,
                efficiency = 0.0,
                satisfactionScore = 0.0
            )
        }
    }
    
    fun observeRealTimeAnalytics(): Flow<AnalyticsUpdate> = flow {
        // TODO: Implement real-time analytics via Supabase Realtime
        try {
            emit(AnalyticsUpdate.PipelineUpdate)
            emit(AnalyticsUpdate.RevenueUpdate)
            emit(AnalyticsUpdate.SLAUpdate)
        } catch (e: Exception) {
            emit(AnalyticsUpdate.Error(e.message ?: "Unknown error"))
        }
    }
}

// Data classes for analytics
data class KPRPipelineFunnel(
    val totalLeads: Int,
    val documentCollection: Int,
    val bankProcessing: Int,
    val creditApproved: Int,
    val sp3kIssued: Int,
    val fundsDisbursed: Int,
    val bastCompleted: Int,
    val leadToDocumentRate: Double,
    val documentToBankRate: Double,
    val bankToApprovalRate: Double,
    val approvalToSp3kRate: Double,
    val sp3kToDisbursementRate: Double,
    val disbursementToBastRate: Double,
    val overallConversionRate: Double
)

data class PhaseProcessingTime(
    val phaseName: String,
    val fromStatus: KprStatus,
    val toStatus: KprStatus,
    val averageDays: Double,
    val minDays: Double,
    val maxDays: Double,
    val sampleSize: Int
)

data class BankApprovalStats(
    val bankName: String,
    val totalDecisions: Int,
    val approvedDecisions: Int,
    val rejectedDecisions: Int,
    val pendingDecisions: Int,
    val approvalRate: Double,
    val rejectionRate: Double,
    val pendingRate: Double
)

data class RevenueProjection(
    val actualRevenue: BigDecimal,
    val projectedRevenue: BigDecimal,
    val totalProjectedRevenue: BigDecimal,
    val monthlyProjections: List<MonthRevenueProjection>,
    val confidenceLevel: Double,
    val projectionPeriod: Int
)

data class MonthRevenueProjection(
    val month: Int,
    val projectedRevenue: BigDecimal,
    val cumulativeRevenue: BigDecimal
)

data class SLAComplianceMetrics(
    val documentSLAComplianceRate: Double,
    val bankSLAComplianceRate: Double,
    val averageDocumentProcessingTime: Double,
    val averageBankProcessingTime: Double,
    val overdueDocuments: Int,
    val overdueBankDecisions: Int,
    val totalDocuments: Int,
    val totalBankDecisions: Int
)

data class TeamPerformanceMetrics(
    val marketingMetrics: RolePerformanceMetrics,
    val legalMetrics: RolePerformanceMetrics,
    val financeMetrics: RolePerformanceMetrics,
    val overallTeamEfficiency: Double
)

data class RolePerformanceMetrics(
    val role: String,
    val tasksCompleted: Int,
    val averageTaskTime: Double,
    val efficiency: Double,
    val satisfactionScore: Double
)

sealed class AnalyticsUpdate {
    object PipelineUpdate : AnalyticsUpdate()
    object RevenueUpdate : AnalyticsUpdate()
    object SLAUpdate : AnalyticsUpdate()
    data class Error(val message: String) : AnalyticsUpdate()
}
