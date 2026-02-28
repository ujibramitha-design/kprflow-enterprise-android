package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.SLAStatus
import kotlinx.coroutines.flow.Flow

/**
 * Interface for SLA Repository
 * Using SQL Views for performance optimization
 */
interface ISLARepository {
    suspend fun getDossierSLAStatus(dossierId: String): Result<SLAStatus>
    suspend fun getAllSLAStatuses(): Result<List<SLAStatus>>
    suspend fun getOverdueDossiers(type: String = "ALL"): Result<List<SLAStatus>>
    suspend fun getMarketingSLASummary(): Result<SLASummary>
    suspend fun getLegalSLASummary(): Result<SLASummary>
    suspend fun getFinanceSLASummary(): Result<SLASummary>
    fun observeSLAStatusChanges(dossierId: String): Flow<SLAStatus>
    suspend fun getCriticalDossiers(): Result<List<SLAStatus>>
    suspend fun getDossiersBySLAStatus(slaStatus: String): Result<List<SLAStatus>>
}

data class SLAStatus(
    val dossierId: String,
    val customerName: String,
    val customerEmail: String? = null,
    val customerPhone: String? = null,
    val status: String,
    val bookingDate: String,
    val docDaysLeft: Int,
    val bankDaysLeft: Int,
    val daysElapsed: Int,
    val isDocOverdue: Boolean,
    val isBankOverdue: Boolean,
    val slaStatus: String,
    val priorityLevel: Int,
    val unitBlock: String? = null,
    val unitNumber: String? = null,
    val unitType: String? = null,
    val unitPrice: java.math.BigDecimal? = null,
    val completionPercentage: Int = 0,
    val estimatedCompletionDate: String? = null
)

data class SLASummary(
    val totalDossiers: Int,
    val activeCount: Int,
    val overdueCount: Int,
    val criticalCount: Int,
    val warningCount: Int,
    val normalCount: Int,
    val avgDaysRemaining: Double,
    val urgentCount: Int,
    val completionRate: Double
)
