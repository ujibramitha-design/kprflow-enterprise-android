package com.kprflow.enterprise.security

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureApiClient @Inject constructor(
    private val certificatePinner: CertificatePinner,
    private val tokenManager: TokenManager,
    private val auditLogger: AuditLogger
) {
    
    private val client: OkHttpClient by lazy {
        certificatePinner.createSecureClient().newBuilder()
            .addInterceptor(AuthInterceptor())
            .addInterceptor(SecurityAuditInterceptor())
            .addInterceptor(RateLimitInterceptor())
            .build()
    }
    
    suspend fun executeSecureRequest(request: Request): Response {
        return try {
            val secureRequest = request.newBuilder()
                .addHeader("X-Client-Version", "1.0.0")
                .addHeader("X-Platform", "Android")
                .addHeader("X-Request-ID", generateRequestId())
                .build()
            
            client.newCall(secureRequest).execute()
        } catch (e: Exception) {
            auditLogger.logSecurityEvent(
                eventType = "SECURE_REQUEST_FAILED",
                details = mapOf(
                    "url" to request.url.toString(),
                    "error" to e.message
                )
            )
            throw e
        }
    }
    
    private fun generateRequestId(): String {
        return java.util.UUID.randomUUID().toString()
    }
    
    inner class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            
            val token = tokenManager.getValidToken()
            val authenticatedRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            
            return chain.proceed(authenticatedRequest)
        }
    }
    
    inner class SecurityAuditInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val startTime = System.currentTimeMillis()
            
            try {
                val response = chain.proceed(request)
                val endTime = System.currentTimeMillis()
                
                auditLogger.logSecurityEvent(
                    eventType = "API_CALL",
                    details = mapOf(
                        "url" to request.url.toString(),
                        "method" to request.method,
                        "status_code" to response.code,
                        "duration_ms" to (endTime - startTime),
                        "request_id" to request.header("X-Request-ID")
                    )
                )
                
                return response
            } catch (e: Exception) {
                val endTime = System.currentTimeMillis()
                
                auditLogger.logSecurityEvent(
                    eventType = "API_CALL_FAILED",
                    details = mapOf(
                        "url" to request.url.toString(),
                        "method" to request.method,
                        "duration_ms" to (endTime - startTime),
                        "error" to e.message,
                        "request_id" to request.header("X-Request-ID")
                    )
                )
                
                throw e
            }
        }
    }
    
    inner class RateLimitInterceptor : Interceptor {
        private val requestTimestamps = mutableMapOf<String, Long>()
        private val rateLimitMs = 1000 // 1 second between requests
        
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val endpoint = request.url.encodedPath
            
            val currentTime = System.currentTimeMillis()
            val lastRequestTime = requestTimestamps[endpoint] ?: 0
            
            if (currentTime - lastRequestTime < rateLimitMs) {
                auditLogger.logSecurityEvent(
                    eventType = "RATE_LIMIT_TRIGGERED",
                    details = mapOf(
                        "endpoint" to endpoint,
                        "last_request" to lastRequestTime,
                        "current_request" to currentTime
                    )
                )
                
                throw SecurityException("Rate limit exceeded for endpoint: $endpoint")
            }
            
            requestTimestamps[endpoint] = currentTime
            return chain.proceed(request)
        }
    }
}
