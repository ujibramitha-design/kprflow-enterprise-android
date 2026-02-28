package com.kprflow.enterprise.data.repository

import com.kprflow.enterprise.data.model.KprDossier
import com.kprflow.enterprise.data.model.KprStatus
import com.kprflow.enterprise.data.model.UnitProperty
import com.kprflow.enterprise.data.model.UnitStatus
import com.kprflow.enterprise.domain.repository.IKprRepository
import com.kprflow.enterprise.domain.repository.DossierWithUnit
import com.kprflow.enterprise.domain.repository.DossierStatistics
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KprRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val realtime: Realtime
) : IKprRepository {
    
    override suspend fun createDossier(
        userId: String,
        unitId: String? = null,
        kprAmount: BigDecimal? = null,
        dpAmount: BigDecimal? = null,
        bankName: String? = null,
        notes: String? = null
    ): Result<KprDossier> {
        return try {
            val dossier = KprDossier(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                unitId = unitId,
                status = KprStatus.LEAD,
                bookingDate = LocalDate.now(),
                kprAmount = kprAmount,
                dpAmount = dpAmount,
                bankName = bankName,
                notes = notes,
                createdAt = java.time.Instant.now().toString(),
                updatedAt = java.time.Instant.now().toString()
            )
            
            val createdDossier = postgrest.from("kpr_dossiers")
                .insert(dossier)
                .maybeSingle()
                .data
            
            createdDossier?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to create dossier"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getDossierById(dossierId: String): Result<KprDossier?> {
        return try {
            val dossier = postgrest.from("kpr_dossiers")
                .select()
                .filter { eq("id", dossierId) }
                .maybeSingle()
                .data
            Result.success(dossier)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getDossiersByUserId(userId: String): Result<List<KprDossier>> {
        return try {
            val dossiers = postgrest.from("kpr_dossiers")
                .select()
                .filter { eq("user_id", userId) }
                .order("created_at", ascending = false)
                .data
            Result.success(dossiers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAllDossiers(): Result<List<KprDossier>> {
        return try {
            val dossiers = postgrest.from("kpr_dossiers")
                .select()
                .order("created_at", ascending = false)
                .data
            Result.success(dossiers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getDossiersByStatus(status: KprStatus): Result<List<KprDossier>> {
        return try {
            val dossiers = postgrest.from("kpr_dossiers")
                .select()
                .filter { eq("status", status.name) }
                .order("created_at", ascending = false)
                .data
            Result.success(dossiers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateDossierStatus(dossierId: String, newStatus: KprStatus, notes: String? = null): Result<KprDossier> {
        return try {
            val updateData = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "updated_at" to java.time.Instant.now().toString()
            )
            
            // Add status-specific updates
            when (newStatus) {
                KprStatus.SP3K_TERBIT -> updateData["sp3k_issued_date"] = LocalDate.now().toString()
                KprStatus.AKAD_BELUM_CAIR -> updateData["akad_date"] = LocalDate.now().toString()
                KprStatus.FUNDS_DISBURSED -> updateData["disbursed_date"] = LocalDate.now().toString()
                KprStatus.BAST_COMPLETED -> updateData["bast_date"] = LocalDate.now().toString()
                KprStatus.CANCELLED_BY_SYSTEM -> {
                    updateData["cancellation_reason"] = notes ?: "System cancellation"
                    updateData["unit_id"] = null // Release unit
                }
                else -> {}
            }
            
            if (notes != null) {
                updateData["notes"] = notes
            }
            
            val updatedDossier = postgrest.from("kpr_dossiers")
                .update(updateData)
                .filter { eq("id", dossierId) }
                .maybeSingle()
                .data
            
            updatedDossier?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to update dossier status"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateDossier(dossier: KprDossier): Result<KprDossier> {
        return try {
            val updatedDossier = postgrest.from("kpr_dossiers")
                .update(dossier)
                .filter { eq("id", dossier.id) }
                .maybeSingle()
                .data
            
            updatedDossier?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to update dossier"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun assignUnitToDossier(dossierId: String, unitId: String): Result<KprDossier> {
        return try {
            // Check if unit is available
            val unit = getUnitById(unitId).getOrNull()
                ?: return Result.failure(Exception("Unit not found"))
            
            if (unit.status != UnitStatus.AVAILABLE) {
                return Result.failure(Exception("Unit is not available"))
            }
            
            // Update dossier with unit
            val updatedDossier = postgrest.from("kpr_dossiers")
                .update(
                    mapOf(
                        "unit_id" to unitId,
                        "updated_at" to java.time.Instant.now().toString()
                    )
                )
                .filter { eq("id", dossierId) }
                .maybeSingle()
                .data
            
            // Update unit status to BOOKED
            if (updatedDossier != null) {
                updateUnitStatus(unitId, UnitStatus.BOOKED)
            }
            
            updatedDossier?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to assign unit to dossier"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun releaseUnitFromDossier(dossierId: String): Result<KprDossier> {
        return try {
            val dossier = getDossierById(dossierId).getOrNull()
                ?: return Result.failure(Exception("Dossier not found"))
            
            if (dossier.unitId != null) {
                // Release unit back to AVAILABLE
                updateUnitStatus(dossier.unitId, UnitStatus.AVAILABLE)
            }
            
            // Update dossier to remove unit
            val updatedDossier = postgrest.from("kpr_dossiers")
                .update(
                    mapOf(
                        "unit_id" to null,
                        "updated_at" to java.time.Instant.now().toString()
                    )
                )
                .filter { eq("id", dossierId) }
                .maybeSingle()
                .data
            
            updatedDossier?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to release unit from dossier"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAvailableUnits(): Result<List<UnitProperty>> {
        return try {
            val units = postgrest.from("unit_properties")
                .select()
                .filter { eq("status", UnitStatus.AVAILABLE.name) }
                .order("block", ascending = true)
                .order("unit_number", ascending = true)
                .data
            Result.success(units)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUnitById(unitId: String): Result<UnitProperty?> {
        return try {
            val unit = postgrest.from("unit_properties")
                .select()
                .filter { eq("id", unitId) }
                .maybeSingle()
                .data
            Result.success(unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun updateUnitStatus(unitId: String, newStatus: UnitStatus): Result<UnitProperty> {
        return try {
            val updatedUnit = postgrest.from("unit_properties")
                .update(
                    mapOf(
                        "status" to newStatus.name,
                        "updated_at" to java.time.Instant.now().toString()
                    )
                )
                .filter { eq("id", unitId) }
                .maybeSingle()
                .data
            
            updatedUnit?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to update unit status"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Real-time listeners
    override fun observeDossier(dossierId: String): Flow<KprDossier?> = flow {
        try {
            val dossier = getDossierById(dossierId).getOrNull()
            emit(dossier)
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    override fun observeDossiersByStatus(status: KprStatus): Flow<List<KprDossier>> = flow {
        try {
            val dossiers = getDossiersByStatus(status).getOrNull().orEmpty()
            emit(dossiers)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    override fun observeUserDossiers(userId: String): Flow<List<KprDossier>> = flow {
        try {
            val dossiers = getDossiersByUserId(userId).getOrNull().orEmpty()
            emit(dossiers)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    // Setup real-time channel for dossier updates
    override fun setupRealtimeChannel(): Flow<KprDossier> = flow {
        try {
            val channel = realtime.channel("kpr_dossiers")
            
            channel.onPostgresChange("postgres_changes", schema = "public", table = "kpr_dossiers") { change ->
                // Emit updated dossier when database changes
                val dossier = change.record as? KprDossier
                dossier?.let { emit(it) }
            }
            
            channel.connect()
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    override suspend fun getDossiersWithUnits(): Result<List<DossierWithUnit>> {
        return try {
            val dossiers = postgrest.from("kpr_dossiers")
                .select(
                    """
                    id, user_id, unit_id, status, booking_date, kpr_amount, dp_amount, 
                    bank_name, sp3k_issued_date, akad_date, disbursed_date, bast_date, 
                    cancellation_reason, created_at, updated_at, notes,
                    unit_properties!inner(id, block, unit_number, type, price, status)
                    """
                )
                .order("created_at", ascending = false)
                .data
                
            val dossiersWithUnits = dossiers.map { dossier ->
                // Parse the nested unit property from the joined query
                // This would need proper serialization handling
                DossierWithUnit(
                    dossier = dossier,
                    unit = null // Would need to extract from joined data
                )
            }
            
            Result.success(dossiersWithUnits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getDossierStatistics(): Result<DossierStatistics> {
        return try {
            val allDossiers = getAllDossiers().getOrNull().orEmpty()
            
            val stats = DossierStatistics(
                totalDossiers = allDossiers.size,
                leadCount = allDossiers.count { it.status == KprStatus.LEAD },
                documentCollectionCount = allDossiers.count { it.status == KprStatus.PEMBERKASAN },
                bankProcessingCount = allDossiers.count { it.status == KprStatus.PROSES_BANK },
                creditApprovedCount = allDossiers.count { it.status == KprStatus.PUTUSAN_KREDIT_ACC },
                sp3kIssuedCount = allDossiers.count { it.status == KprStatus.SP3K_TERBIT },
                praAkadCount = allDossiers.count { it.status == KprStatus.PRA_AKAD },
                akadSignedCount = allDossiers.count { it.status == KprStatus.AKAD_BELUM_CAIR },
                fundsDisbursedCount = allDossiers.count { it.status == KprStatus.FUNDS_DISBURSED },
                bastReadyCount = allDossiers.count { it.status == KprStatus.BAST_READY },
                bastCompletedCount = allDossiers.count { it.status == KprStatus.BAST_COMPLETED }
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Serializable
data class DossierWithUnit(
    val dossier: KprDossier,
    val unit: UnitProperty?
)

@Serializable
data class DossierStatistics(
    val totalDossiers: Int,
    val leadCount: Int,
    val documentCollectionCount: Int,
    val bankProcessingCount: Int,
    val creditApprovedCount: Int,
    val sp3kIssuedCount: Int,
    val praAkadCount: Int,
    val akadSignedCount: Int,
    val fundsDisbursedCount: Int,
    val bastReadyCount: Int,
    val bastCompletedCount: Int
)
