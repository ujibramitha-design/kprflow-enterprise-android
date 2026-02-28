package com.kprflow.enterprise.data.repository

import com.kprflow.enterprise.data.model.*
import com.kprflow.enterprise.domain.repository.IExtensionRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val realtime: Realtime
) : IExtensionRepository {

    override suspend fun requestExtension(
        dossierId: String,
        days: Int,
        reason: String,
        requestedBy: String
    ): Result<ExtensionRequest> {
        return try {
            // Call the database function
            val result = postgrest.rpc("request_dossier_extension") {
                param("p_dossier_id", dossierId)
                param("p_extension_days", days)
                param("p_extension_reason", reason)
                param("p_requested_by", requestedBy)
            }
            
            val data = result.data
            if (data != null) {
                // Parse the result and get the extension details
                val extensionId = data["extension_id"].toString()
                getExtensionById(extensionId)
            } else {
                Result.failure(Exception("Failed to create extension request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveExtension(
        extensionId: String,
        approvedBy: String,
        approvalNotes: String?
    ): Result<Boolean> {
        return try {
            val result = postgrest.rpc("approve_dossier_extension") {
                param("p_extension_id", extensionId)
                param("p_approved_by", approvedBy)
                param("p_approval_notes", approvalNotes)
            }
            
            Result.success(result.data != null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectExtension(
        extensionId: String,
        rejectedBy: String,
        rejectionReason: String
    ): Result<Boolean> {
        return try {
            val result = postgrest.rpc("reject_dossier_extension") {
                param("p_extension_id", extensionId)
                param("p_rejected_by", rejectedBy)
                param("p_rejection_reason", rejectionReason)
            }
            
            Result.success(result.data != null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingExtensions(): Result<List<ExtensionRequest>> {
        return try {
            val extensions = postgrest.from("v_pending_extensions")
                .select()
                .data
            
            val extensionRequests = extensions.map { mapToExtensionRequest(it) }
            Result.success(extensionRequests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExtensionHistory(dossierId: String): Result<List<ExtensionRequest>> {
        return try {
            val extensions = postgrest.from("v_extension_history")
                .select()
                .filter { eq("dossier_id", dossierId) }
                .order("extension_date", ascending = false)
                .data
            
            val extensionRequests = extensions.map { mapToExtensionRequest(it) }
            Result.success(extensionRequests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExtensionById(extensionId: String): Result<ExtensionRequest> {
        return try {
            val extension = postgrest.from("dossier_extensions")
                .select()
                .filter { eq("id", extensionId) }
                .maybeSingle()
                .data
            
            extension?.let { 
                Result.success(mapToExtensionRequest(it)) 
            } ?: Result.failure(Exception("Extension not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDossierExtensionCount(dossierId: String): Result<Int> {
        return try {
            val dossier = postgrest.from("kpr_dossiers")
                .select("extension_count")
                .filter { eq("id", dossierId) }
                .maybeSingle()
                .data
            
            val count = dossier?.get("extension_count") as? Int ?: 0
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun canRequestExtension(dossierId: String): Result<Boolean> {
        return try {
            val dossier = postgrest.from("kpr_dossiers")
                .select("status, extension_count")
                .filter { eq("id", dossierId) }
                .maybeSingle()
                .data
            
            if (dossier != null) {
                val status = dossier["status"].toString()
                val extensionCount = dossier["extension_count"] as? Int ?: 0
                
                val canExtend = status !in listOf("CANCELLED_BY_SYSTEM", "BAST_COMPLETED") && 
                               extensionCount < 3
                
                Result.success(canExtend)
            } else {
                Result.failure(Exception("Dossier not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isEligibleForExtension(dossierId: String): Result<Boolean> {
        return try {
            val dossier = postgrest.from("kpr_dossiers")
                .select("status, extension_count, booking_date, current_deadline")
                .filter { eq("id", dossierId) }
                .maybeSingle()
                .data
            
            if (dossier != null) {
                val status = dossier["status"].toString()
                val extensionCount = dossier["extension_count"] as? Int ?: 0
                
                // Check if dossier is in a valid status and hasn't reached max extensions
                val eligible = status !in listOf("CANCELLED_BY_SYSTEM", "BAST_COMPLETED") && 
                              extensionCount < 3
                
                Result.success(eligible)
            } else {
                Result.failure(Exception("Dossier not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observePendingExtensions(): Flow<List<ExtensionRequest>> = flow {
        try {
            val channel = realtime.channel("extension_changes")
            
            channel.onPostgresChange("postgres_changes", schema = "public", table = "dossier_extensions") { change ->
                // Refresh pending extensions when there's a change
                val pending = getPendingExtensions().getOrNull().orEmpty()
                emit(pending)
            }
            
            channel.connect()
            
            // Initial load
            val initial = getPendingExtensions().getOrNull().orEmpty()
            emit(initial)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun observeDossierExtensions(dossierId: String): Flow<List<ExtensionRequest>> = flow {
        try {
            val channel = realtime.channel("dossier_extension_changes")
            
            channel.onPostgresChange("postgres_changes", schema = "public", table = "dossier_extensions") { change ->
                if (change.record?.get("dossier_id") == dossierId) {
                    val history = getExtensionHistory(dossierId).getOrNull().orEmpty()
                    emit(history)
                }
            }
            
            channel.connect()
            
            // Initial load
            val initial = getExtensionHistory(dossierId).getOrNull().orEmpty()
            emit(initial)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun getExtensionStatistics(): Result<ExtensionStatistics> {
        return try {
            val extensions = postgrest.from("v_extension_history")
                .select()
                .data
            
            val extensionList = extensions.map { mapToExtensionRequest(it) }
            
            val totalRequests = extensionList.size
            val pendingRequests = extensionList.count { it.status == ExtensionStatus.PENDING }
            val approvedRequests = extensionList.count { it.status == ExtensionStatus.APPROVED }
            val rejectedRequests = extensionList.count { it.status == ExtensionStatus.REJECTED }
            
            val averageExtensionDays = if (approvedRequests > 0) {
                extensionList.filter { it.status == ExtensionStatus.APPROVED }
                    .map { it.extensionDays }
                    .average()
            } else 0.0
            
            val mostCommonReason = extensionList
                .groupBy { it.extensionReason }
                .maxByOrNull { it.value.size }
                ?.key ?: ""
            
            val extensionsByMonth = extensionList
                .groupBy { 
                    it.extensionDate.substring(0, 7) // YYYY-MM format
                }
                .mapValues { it.value.size }
            
            Result.success(
                ExtensionStatistics(
                    totalRequests = totalRequests,
                    pendingRequests = pendingRequests,
                    approvedRequests = approvedRequests,
                    rejectedRequests = rejectedRequests,
                    averageExtensionDays = averageExtensionDays,
                    mostCommonReason = mostCommonReason,
                    extensionsByMonth = extensionsByMonth
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToExtensionRequest(data: Any): ExtensionRequest {
        // This would need proper JSON parsing based on the actual data structure
        // For now, using a simplified mapping
        return ExtensionRequest(
            id = "placeholder", // Extract from data
            dossierId = "placeholder", // Extract from data
            customerName = "placeholder", // Extract from data
            extensionDays = 30, // Extract from data
            extensionReason = "placeholder", // Extract from data
            extendedBy = "placeholder", // Extract from data
            extendedByName = "placeholder", // Extract from data
            extensionDate = "placeholder", // Extract from data
            previousDeadline = "placeholder", // Extract from data
            newDeadline = "placeholder", // Extract from data
            status = ExtensionStatus.PENDING, // Extract from data
            dossierStatus = "placeholder", // Extract from data
            extensionCount = 0 // Extract from data
        )
    }
}
