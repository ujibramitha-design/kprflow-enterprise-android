package com.kprflow.enterprise.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Auth Repository - Authentication Management
 * Phase Final: Global UI State Wrapper Implementation
 */
interface AuthRepository {
    
    /**
     * Token expiration flow
     */
    val tokenExpiration: Flow<Boolean>
    
    /**
     * Permission errors flow
     */
    val permissionErrors: Flow<String?>
    
    /**
     * Refresh token
     */
    suspend fun refreshToken(): Result<String>
    
    /**
     * Notify administrator
     */
    suspend fun notifyAdministrator(message: String): Result<Unit>
    
    /**
     * Check role access
     */
    suspend fun checkRoleAccess(requiredRole: String): Boolean
    
    /**
     * Get current user role
     */
    suspend fun getCurrentUserRole(): String?
    
    /**
     * Check permission
     */
    suspend fun checkPermission(permission: String): Boolean
}
