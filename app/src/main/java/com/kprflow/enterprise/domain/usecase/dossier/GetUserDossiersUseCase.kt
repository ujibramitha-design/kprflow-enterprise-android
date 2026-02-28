package com.kprflow.enterprise.domain.usecase.dossier

import com.kprflow.enterprise.data.model.KprDossier
import com.kprflow.enterprise.domain.repository.IKprRepository
import com.kprflow.enterprise.domain.usecase.auth.GetCurrentUserUseCase
import javax.inject.Inject

/**
 * Use Case for Getting User Dossiers
 * Following Clean Architecture - business logic in domain layer
 */
class GetUserDossiersUseCase @Inject constructor(
    private val kprRepository: IKprRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) {
    suspend operator fun invoke(): Result<List<KprDossier>> {
        val currentUser = getCurrentUserUseCase()
            ?: return Result.failure(IllegalStateException("User not logged in"))
        
        return kprRepository.getDossiersByUserId(currentUser.id)
    }
}
