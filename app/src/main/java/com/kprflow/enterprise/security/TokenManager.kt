package com.kprflow.enterprise.security

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kprflow.enterprise.domain.model.SecurityToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val encryptedPrefs: SharedPreferences,
    private val auditLogger: AuditLogger
) {
    
    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val TOKEN_EXPIRY_KEY = "token_expiry"
        private const val TOKEN_ISSUED_AT_KEY = "token_issued_at"
        private const val REFRESH_THRESHOLD_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    private val _tokenState = MutableStateFlow<TokenState>(TokenState.NotAuthenticated)
    val tokenState: Flow<TokenState> = _tokenState.asStateFlow()
    
    fun getValidToken(): String {
        val currentToken = getCurrentToken()
        return when {
            currentToken == null -> {
                throw SecurityException("No authentication token available")
            }
            isTokenExpired() -> {
                throw SecurityException("Authentication token expired")
            }
            isTokenNearExpiry() -> {
                auditLogger.logSecurityEvent(
                    eventType = "TOKEN_NEAR_EXPIRY",
                    details = mapOf(
                        "expiry_time" to getTokenExpiryTime(),
                        "current_time" to System.currentTimeMillis()
                    )
                )
                currentToken.token
            }
            else -> currentToken.token
        }
    }
    
    fun storeTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        try {
            val currentTime = System.currentTimeMillis()
            val expiryTime = currentTime + (expiresIn * 1000)
            
            encryptedPrefs.edit()
                .putString(ACCESS_TOKEN_KEY, accessToken)
                .putString(REFRESH_TOKEN_KEY, refreshToken)
                .putLong(TOKEN_EXPIRY_KEY, expiryTime)
                .putLong(TOKEN_ISSUED_AT_KEY, currentTime)
                .apply()
            
            _tokenState.value = TokenState.Authenticated(
                token = SecurityToken(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    issuedAt = currentTime,
                    expiresAt = expiryTime
                )
            )
            
            auditLogger.logSecurityEvent(
                eventType = "TOKENS_STORED",
                details = mapOf(
                    "issued_at" to currentTime,
                    "expires_at" to expiryTime,
                    "expires_in" to expiresIn
                )
            )
        } catch (e: Exception) {
            auditLogger.logSecurityEvent(
                eventType = "TOKEN_STORAGE_FAILED",
                details = mapOf("error" to e.message)
            )
            throw SecurityException("Failed to store tokens", e)
        }
    }
    
    fun refreshTokens(): Boolean {
        return try {
            val refreshToken = encryptedPrefs.getString(REFRESH_TOKEN_KEY, null)
                ?: return false
            
            // In a real implementation, this would call the refresh endpoint
            // For now, we'll simulate a successful refresh
            val newAccessToken = generateSecureToken()
            val newRefreshToken = generateSecureToken()
            val expiresIn = TimeUnit.HOURS.toSeconds(1) // 1 hour
            
            storeTokens(newAccessToken, newRefreshToken, expiresIn)
            
            auditLogger.logSecurityEvent(
                eventType = "TOKENS_REFRESHED",
                details = mapOf(
                    "refresh_time" to System.currentTimeMillis()
                )
            )
            
            true
        } catch (e: Exception) {
            auditLogger.logSecurityEvent(
                eventType = "TOKEN_REFRESH_FAILED",
                details = mapOf("error" to e.message)
            )
            false
        }
    }
    
    fun clearTokens() {
        try {
            encryptedPrefs.edit()
                .remove(ACCESS_TOKEN_KEY)
                .remove(REFRESH_TOKEN_KEY)
                .remove(TOKEN_EXPIRY_KEY)
                .remove(TOKEN_ISSUED_AT_KEY)
                .apply()
            
            _tokenState.value = TokenState.NotAuthenticated
            
            auditLogger.logSecurityEvent(
                eventType = "TOKENS_CLEARED",
                details = mapOf("timestamp" to System.currentTimeMillis())
            )
        } catch (e: Exception) {
            auditLogger.logSecurityEvent(
                eventType = "TOKEN_CLEAR_FAILED",
                details = mapOf("error" to e.message)
            )
        }
    }
    
    fun isTokenExpired(): Boolean {
        val expiryTime = getTokenExpiryTime()
        return expiryTime <= System.currentTimeMillis()
    }
    
    fun isTokenNearExpiry(): Boolean {
        val expiryTime = getTokenExpiryTime()
        return expiryTime - System.currentTimeMillis() <= REFRESH_THRESHOLD_MS
    }
    
    private fun getCurrentToken(): SecurityToken? {
        val accessToken = encryptedPrefs.getString(ACCESS_TOKEN_KEY, null)
            ?: return null
        val refreshToken = encryptedPrefs.getString(REFRESH_TOKEN_KEY, null)
            ?: return null
        val issuedAt = encryptedPrefs.getLong(TOKEN_ISSUED_AT_KEY, 0L)
        val expiresAt = encryptedPrefs.getLong(TOKEN_EXPIRY_KEY, 0L)
        
        return SecurityToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            issuedAt = issuedAt,
            expiresAt = expiresAt
        )
    }
    
    private fun getTokenExpiryTime(): Long {
        return encryptedPrefs.getLong(TOKEN_EXPIRY_KEY, 0L)
    }
    
    private fun generateSecureToken(): String {
        val random = java.security.SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

sealed class TokenState {
    object NotAuthenticated : TokenState()
    data class Authenticated(val token: SecurityToken) : TokenState()
    data class Error(val message: String) : TokenState()
}
