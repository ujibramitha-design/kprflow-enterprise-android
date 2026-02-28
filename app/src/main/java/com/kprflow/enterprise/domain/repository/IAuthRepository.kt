package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.UserProfile
import com.kprflow.enterprise.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import io.github.jan.supabase.auth.user.UserSession

/**
 * Interface for Authentication Repository
 * Following dependency injection best practices for testability
 */
interface IAuthRepository {
    suspend fun signIn(email: String, password: String): Result<UserProfile>
    suspend fun signUp(email: String, password: String, name: String, nik: String, phoneNumber: String, maritalStatus: String): Result<UserProfile>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): UserProfile?
    fun getCurrentSession(): UserSession?
    fun isUserLoggedIn(): Boolean
    suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile>
    suspend fun resetPassword(email: String): Result<Unit>
    fun observeAuthState(): Flow<UserProfile?>
    suspend fun hasRole(role: UserRole): Boolean
    suspend fun isStaff(): Boolean
}
