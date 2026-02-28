package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.KprDossier
import com.kprflow.enterprise.data.model.KprStatus
import com.kprflow.enterprise.data.model.UnitProperty
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * Interface for KPR Repository
 * Following dependency injection best practices for testability
 */
interface IKprRepository {
    suspend fun createDossier(
        userId: String,
        unitId: String? = null,
        kprAmount: BigDecimal? = null,
        dpAmount: BigDecimal? = null,
        bankName: String? = null,
        notes: String? = null
    ): Result<KprDossier>
    
    suspend fun getDossierById(dossierId: String): Result<KprDossier?>
    suspend fun getDossiersByUserId(userId: String): Result<List<KprDossier>>
    suspend fun getAllDossiers(): Result<List<KprDossier>>
    suspend fun getDossiersByStatus(status: KprStatus): Result<List<KprDossier>>
    suspend fun updateDossierStatus(dossierId: String, newStatus: KprStatus, notes: String? = null): Result<KprDossier>
    suspend fun updateDossier(dossier: KprDossier): Result<KprDossier>
    suspend fun assignUnitToDossier(dossierId: String, unitId: String): Result<KprDossier>
    suspend fun releaseUnitFromDossier(dossierId: String): Result<KprDossier>
    suspend fun getAvailableUnits(): Result<List<UnitProperty>>
    suspend fun getUnitById(unitId: String): Result<UnitProperty?>
    
    // Real-time listeners
    fun observeDossier(dossierId: String): Flow<KprDossier?>
    fun observeDossiersByStatus(status: KprStatus): Flow<List<KprDossier>>
    fun observeUserDossiers(userId: String): Flow<List<KprDossier>>
    fun setupRealtimeChannel(): Flow<KprDossier>
    
    // Advanced queries
    suspend fun getDossiersWithUnits(): Result<List<DossierWithUnit>>
    suspend fun getDossierStatistics(): Result<DossierStatistics>
}

// Data classes for repository responses
data class DossierWithUnit(
    val dossier: KprDossier,
    val unit: UnitProperty?
)

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
