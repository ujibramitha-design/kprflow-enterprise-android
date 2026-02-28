package com.kprflow.enterprise.domain.usecase.dossier

import com.kprflow.enterprise.data.model.KprDossier
import com.kprflow.enterprise.data.model.KprStatus
import com.kprflow.enterprise.domain.repository.IKprRepository
import com.kprflow.enterprise.domain.usecase.auth.GetCurrentUserUseCase
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Use Case for Creating Dossier
 * Following Clean Architecture - business logic in domain layer
 */
class CreateDossierUseCase @Inject constructor(
    private val kprRepository: IKprRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) {
    suspend operator fun invoke(
        unitId: String? = null,
        kprAmount: BigDecimal? = null,
        dpAmount: BigDecimal? = null,
        bankName: String? = null,
        notes: String? = null
    ): Result<KprDossier> {
        
        val currentUser = getCurrentUserUseCase()
            ?: return Result.failure(IllegalStateException("User not logged in"))
        
        // Business logic validation
        kprAmount?.let { amount ->
            if (amount <= BigDecimal.ZERO) {
                return Result.failure(IllegalArgumentException("KPR amount must be positive"))
            }
        }
        
        dpAmount?.let { amount ->
            if (amount <= BigDecimal.ZERO) {
                return Result.failure(IllegalArgumentException("Down payment amount must be positive"))
            }
        }
        
        // Check if user already has active dossier
        val existingDossiers = kprRepository.getDossiersByUserId(currentUser.id).getOrNull().orEmpty()
        val activeDossiers = existingDossiers.filter { 
            it.status !in listOf(KprStatus.CANCELLED_BY_SYSTEM, KprStatus.BAST_COMPLETED)
        }
        
        if (activeDossiers.isNotEmpty()) {
            return Result.failure(IllegalStateException("User already has active dossier"))
        }
        
        return kprRepository.createDossier(
            userId = currentUser.id,
            unitId = unitId,
            kprAmount = kprAmount,
            dpAmount = dpAmount,
            bankName = bankName,
            notes = notes
        )
    }
}
