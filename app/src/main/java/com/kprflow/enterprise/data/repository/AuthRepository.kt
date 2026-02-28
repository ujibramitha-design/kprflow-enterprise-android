package com.kprflow.enterprise.data.repository

import com.kprflow.enterprise.data.model.UserProfile
import com.kprflow.enterprise.data.model.UserRole
import com.kprflow.enterprise.domain.repository.IAuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : IAuthRepository {
    
    override suspend fun signIn(email: String, password: String): Result<UserProfile> {
        return try {
            val result = auth.signInWith(email, password)
            result.data?.user?.let { user ->
                val userProfile = getUserProfile(user.id)
                if (userProfile != null) {
                    Result.success(userProfile)
                } else {
                    Result.failure(Exception("User profile not found"))
                }
            } ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signUp(email: String, password: String, name: String, nik: String, phoneNumber: String, maritalStatus: String): Result<UserProfile> {
        return try {
            val result = auth.signUpWith(email, password)
            result.data?.user?.let { user ->
                // Create user profile after successful signup
                val userProfile = UserProfile(
                    id = user.id,
                    name = name,
                    email = email,
                    nik = nik,
                    phoneNumber = phoneNumber,
                    maritalStatus = maritalStatus,
                    role = UserRole.CUSTOMER, // Default role for new users
                    createdAt = user.createdAt.toString(),
                    updatedAt = user.createdAt.toString(),
                    isActive = true
                )
                
                createUserProfile(userProfile)
                Result.success(userProfile)
            } ?: Result.failure(Exception("Sign up failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCurrentUser(): UserProfile? {
        return auth.currentUser?.let { user ->
            getUserProfile(user.id)
        }
    }
    
    override fun getCurrentSession(): UserSession? {
        return auth.currentSessionOrNull()
    }
    
    override fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    override suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return try {
            postgrest.from("user_profiles")
                .update(userProfile)
                .filter { eq("id", userProfile.id) }
                .maybeSingle()
                .data?.let { updatedProfile ->
                    Result.success(updatedProfile)
                } ?: Result.failure(Exception("Failed to update profile"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observeAuthState(): Flow<UserProfile?> = flow {
        try {
            auth.currentUser?.let { user ->
                emit(getUserProfile(user.id))
            } ?: emit(null)
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    private suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            postgrest.from("user_profiles")
                .select()
                .filter { eq("id", userId) }
                .maybeSingle()
                .data
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun createUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            postgrest.from("user_profiles")
                .insert(userProfile)
                .maybeSingle()
                .data
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun hasRole(role: UserRole): Boolean {
        val currentUser = getCurrentUser()
        return currentUser?.role == role
    }
    
    override suspend fun isStaff(): Boolean {
        val currentUser = getCurrentUser()
        return currentUser?.role in listOf(
            UserRole.MARKETING,
            UserRole.LEGAL,
            UserRole.FINANCE,
            UserRole.BANK,
            UserRole.TEKNIK,
            UserRole.ESTATE,
            UserRole.BOD
        )
    }
}
