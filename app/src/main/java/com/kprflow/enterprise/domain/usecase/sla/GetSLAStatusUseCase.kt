package com.kprflow.enterprise.domain.usecase.sla

import com.kprflow.enterprise.domain.repository.ISLARepository
import javax.inject.Inject

/**
 * Use Case for Getting SLA Status
 * Using SQL Views for performance optimization
 */
class GetSLAStatusUseCase @Inject constructor(
    private val slaRepository: ISLARepository
) {
    suspend operator fun invoke(dossierId: String) = slaRepository.getDossierSLAStatus(dossierId)
}

/**
 * Use Case for Getting All SLA Statuses
 */
class GetAllSLAStatusesUseCase @Inject constructor(
    private val slaRepository: ISLARepository
) {
    suspend operator fun invoke() = slaRepository.getAllSLAStatuses()
}

/**
 * Use Case for Getting Overdue Dossiers
 */
class GetOverdueDossiersUseCase @Inject constructor(
    private val slaRepository: ISLARepository
) {
    suspend operator fun invoke(type: String = "ALL") = slaRepository.getOverdueDossiers(type)
}

/**
 * Use Case for Getting SLA Summary for Dashboard
 */
class GetSLASummaryUseCase @Inject constructor(
    private val slaRepository: ISLARepository
) {
    suspend operator fun invoke(role: String) = when (role.uppercase()) {
        "MARKETING" -> slaRepository.getMarketingSLASummary()
        "LEGAL" -> slaRepository.getLegalSLASummary()
        "FINANCE" -> slaRepository.getFinanceSLASummary()
        "BOD" -> slaRepository.getMarketingSLASummary() // BOD gets comprehensive view
        else -> slaRepository.getMarketingSLASummary()
    }
}

/**
 * Use Case for Getting Critical Dossiers
 */
class GetCriticalDossiersUseCase @Inject constructor(
    private val slaRepository: ISLARepository
) {
    suspend operator fun invoke() = slaRepository.getCriticalDossiers()
}

/**
 * Use Case for Monitoring SLA Changes
 */
class MonitorSLAChangesUseCase @Inject constructor(
    private val slaRepository: ISLARepository
) {
    operator fun invoke(dossierId: String) = slaRepository.observeSLAStatusChanges(dossierId)
}
