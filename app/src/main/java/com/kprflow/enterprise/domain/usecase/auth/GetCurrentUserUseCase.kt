package com.kprflow.enterprise.domain.usecase.auth

import com.kprflow.enterprise.data.model.UserProfile
import com.kprflow.enterprise.domain.repository.IAuthRepository
import javax.inject.Inject

/**
 * Use Case for Getting Current User
 * Following Clean Architecture - business logic in domain layer
 */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(): UserProfile? {
        return authRepository.getCurrentUser()
    }
}
