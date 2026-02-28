package com.kprflow.enterprise.data.repository

import com.kprflow.enterprise.domain.repository.SLAStatus
import com.kprflow.enterprise.domain.repository.SLASummary
import com.kprflow.enterprise.domain.repository.ISLARepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SLARepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val realtime: Realtime
) : ISLARepository {

    override suspend fun getDossierSLAStatus(dossierId: String): Result<SLAStatus> {
        return try {
            val slaData = postgrest.from("v_dossier_sla_status")
                .select()
                .filter { eq("dossier_id", dossierId) }
                .maybeSingle()
                .data
            
            slaData?.let { 
                Result.success(mapToSLAStatus(it)) 
            } ?: Result.failure(Exception("SLA status not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllSLAStatuses(): Result<List<SLAStatus>> {
        return try {
            val slaDataList = postgrest.from("v_dossier_sla_status")
                .select()
                .order("priority_level", ascending = false)
                .data
            
            val slaStatuses = slaDataList.map { mapToSLAStatus(it) }
            Result.success(slaStatuses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOverdueDossiers(type: String): Result<List<SLAStatus>> {
        return try {
            val query = postgrest.from("v_dossier_sla_status")
                .select()
                .order("priority_level", ascending = false)
            
            val filteredQuery = when (type.uppercase()) {
                "DOCUMENT" -> query.filter { eq("is_doc_overdue", true) }
                "BANK" -> query.filter { eq("is_bank_overdue", true) }
                else -> query.filter { 
                    or {
                        eq("is_doc_overdue", true)
                        eq("is_bank_overdue", true)
                    }
                }
            }
            
            val slaDataList = filteredQuery.data
            val slaStatuses = slaDataList.map { mapToSLAStatus(it) }
            Result.success(slaStatuses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMarketingSLASummary(): Result<SLASummary> {
        return try {
            val summaryData = postgrest.from("v_marketing_sla_summary")
                .select()
                .maybeSingle()
                .data
            
            summaryData?.let { 
                Result.success(mapToSLASummary(it)) 
            } ?: Result.failure(Exception("Marketing SLA summary not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLegalSLASummary(): Result<SLASummary> {
        return try {
            val summaryData = postgrest.from("v_legal_sla_summary")
                .select()
                .maybeSingle()
                .data
            
            summaryData?.let { 
                Result.success(mapToSLASummary(it)) 
            } ?: Result.failure(Exception("Legal SLA summary not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFinanceSLASummary(): Result<SLASummary> {
        return try {
            val summaryData = postgrest.from("v_finance_sla_summary")
                .select()
                .maybeSingle()
                .data
            
            summaryData?.let { 
                Result.success(mapToSLASummary(it)) 
            } ?: Result.failure(Exception("Finance SLA summary not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeSLAStatusChanges(dossierId: String): Flow<SLAStatus> = flow {
        try {
            val channel = realtime.channel("sla_changes")
            
            channel.onPostgresChange("postgres_changes", schema = "public", table = "kpr_dossiers") { change ->
                if (change.record?.get("id") == dossierId) {
                    val updatedSLA = getDossierSLAStatus(dossierId).getOrNull()
                    updatedSLA?.let { emit(it) }
                }
            }
            
            channel.connect()
        } catch (e: Exception) {
            // Handle error
        }
    }

    override suspend fun getCriticalDossiers(): Result<List<SLAStatus>> {
        return try {
            val slaDataList = postgrest.from("v_dossier_sla_status")
                .select()
                .filter { eq("sla_status", "CRITICAL") }
                .order("priority_level", ascending = false)
                .data
            
            val slaStatuses = slaDataList.map { mapToSLAStatus(it) }
            Result.success(slaStatuses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDossiersBySLAStatus(slaStatus: String): Result<List<SLAStatus>> {
        return try {
            val slaDataList = postgrest.from("v_dossier_sla_status")
                .select()
                .filter { eq("sla_status", slaStatus) }
                .order("priority_level", ascending = false)
                .data
            
            val slaStatuses = slaDataList.map { mapToSLAStatus(it) }
            Result.success(slaStatuses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToSLAStatus(data: Any): SLAStatus {
        // This would need proper JSON parsing based on the actual data structure
        // For now, using a simplified mapping
        return SLAStatus(
            dossierId = "placeholder", // Extract from data
            customerName = "placeholder", // Extract from data
            docDaysLeft = 0, // Extract from data
            bankDaysLeft = 0, // Extract from data
            daysElapsed = 0, // Extract from data
            isDocOverdue = false, // Extract from data
            isBankOverdue = false, // Extract from data
            slaStatus = "NORMAL", // Extract from data
            priorityLevel = 1 // Extract from data
        )
    }

    private fun mapToSLASummary(data: Any): SLASummary {
        // This would need proper JSON parsing based on the actual data structure
        return SLASummary(
            totalDossiers = 0, // Extract from data
            activeCount = 0, // Extract from data
            overdueCount = 0, // Extract from data
            criticalCount = 0, // Extract from data
            warningCount = 0, // Extract from data
            normalCount = 0, // Extract from data
            avgDaysRemaining = 0.0, // Extract from data
            urgentCount = 0, // Extract from data
            completionRate = 0.0 // Extract from data
        )
    }
}
