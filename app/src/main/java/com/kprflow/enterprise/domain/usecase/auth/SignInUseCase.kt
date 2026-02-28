package com.kprflow.enterprise.domain.usecase.auth

import com.kprflow.enterprise.data.model.UserProfile
import com.kprflow.enterprise.domain.repository.IAuthRepository
import javax.inject.Inject

/**
 * Use Case for User Sign In
 * Following Clean Architecture - business logic in domain layer
 */
class SignInUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<UserProfile> {
        // Business logic validation
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be empty"))
        }
        
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password cannot be empty"))
        }
        
        if (!email.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }
        
        return authRepository.signIn(email, password)
    }
}
