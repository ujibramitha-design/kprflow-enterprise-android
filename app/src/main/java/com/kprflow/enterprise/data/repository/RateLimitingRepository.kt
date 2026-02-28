package com.kprflow.enterprise.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateLimitingRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    
    companion object {
        // Default rate limits (requests per minute)
        const val DEFAULT_USER_LIMIT = 100
        const val DEFAULT_IP_LIMIT = 200
        const val DEFAULT_API_KEY_LIMIT = 1000
        
        // Rate limit windows (in minutes)
        const val DEFAULT_WINDOW_MINUTES = 1
        const val HOURLY_WINDOW = 60
        const val DAILY_WINDOW = 1440
        
        // Rate limit types
        const val USER_RATE_LIMIT = "USER"
        const val IP_RATE_LIMIT = "IP"
        const val API_KEY_RATE_LIMIT = "API_KEY"
        const val ENDPOINT_RATE_LIMIT = "ENDPOINT"
    }
    
    suspend fun checkRateLimit(
        identifier: String,
        limitType: String,
        maxRequests: Int = DEFAULT_USER_LIMIT,
        windowMinutes: Int = DEFAULT_WINDOW_MINUTES
    ): Result<RateLimitResult> {
        return try {
            // Check if identifier is blocked
            val isBlocked = isIdentifierBlocked(identifier, limitType)
                .getOrNull() ?: false
            
            if (isBlocked) {
                return Result.success(
                    RateLimitResult(
                        allowed = false,
                        remainingRequests = 0,
                        resetTime = Instant.now().plus(windowMinutes.toLong(), ChronoUnit.MINUTES),
                        blocked = true,
                        blockReason = "Identifier is blocked"
                    )
                )
            }
            
            // Get current request count
            val currentCount = getCurrentRequestCount(identifier, limitType, windowMinutes)
                .getOrNull() ?: 0
            
            val remainingRequests = maxRequests - currentCount
            
            if (remainingRequests <= 0) {
                // Rate limit exceeded
                val resetTime = calculateResetTime(identifier, limitType, windowMinutes)
                
                // Log rate limit violation
                logRateLimitViolation(identifier, limitType, currentCount, maxRequests, windowMinutes)
                
                Result.success(
                    RateLimitResult(
                        allowed = false,
                        remainingRequests = 0,
                        resetTime = resetTime,
                        blocked = false,
                        blockReason = null
                    )
                )
            } else {
                // Increment request count
                incrementRequestCount(identifier, limitType, windowMinutes)
                
                Result.success(
                    RateLimitResult(
                        allowed = true,
                        remainingRequests = remainingRequests,
                        resetTime = Instant.now().plus(windowMinutes.toLong(), ChronoUnit.MINUTES),
                        blocked = false,
                        blockReason = null
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun checkUserRateLimit(
        userId: String,
        maxRequests: Int = DEFAULT_USER_LIMIT,
        windowMinutes: Int = DEFAULT_WINDOW_MINUTES
    ): Result<RateLimitResult> {
        return checkRateLimit(userId, USER_RATE_LIMIT, maxRequests, windowMinutes)
    }
    
    suspend fun checkIPRateLimit(
        ipAddress: String,
        maxRequests: Int = DEFAULT_IP_LIMIT,
        windowMinutes: Int = DEFAULT_WINDOW_MINUTES
    ): Result<RateLimitResult> {
        return checkRateLimit(ipAddress, IP_RATE_LIMIT, maxRequests, windowMinutes)
    }
    
    suspend fun checkAPIKeyRateLimit(
        apiKey: String,
        maxRequests: Int = DEFAULT_API_KEY_LIMIT,
        windowMinutes: Int = DEFAULT_WINDOW_MINUTES
    ): Result<RateLimitResult> {
        return checkRateLimit(apiKey, API_KEY_RATE_LIMIT, maxRequests, windowMinutes)
    }
    
    suspend fun checkEndpointRateLimit(
        endpoint: String,
        identifier: String,
        maxRequests: Int,
        windowMinutes: Int = DEFAULT_WINDOW_MINUTES
    ): Result<RateLimitResult> {
        val compositeKey = "${identifier}:${endpoint}"
        return checkRateLimit(compositeKey, ENDPOINT_RATE_LIMIT, maxRequests, windowMinutes)
    }
    
    suspend fun blockIdentifier(
        identifier: String,
        limitType: String,
        reason: String,
        blockDurationMinutes: Int = 60
    ): Result<Unit> {
        return try {
            val blockData = mapOf(
                "identifier" to identifier,
                "limit_type" to limitType,
                "reason" to reason,
                "blocked_at" to Instant.now().toString(),
                "unblock_at" to Instant.now().plus(blockDurationMinutes.toLong(), ChronoUnit.MINUTES).toString(),
                "is_active" to true
            )
            
            postgrest.from("rate_limit_blocks")
                .insert(blockData)
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun unblockIdentifier(
        identifier: String,
        limitType: String
    ): Result<Unit> {
        return try {
            postgrest.from("rate_limit_blocks")
                .update(
                    mapOf(
                        "is_active" to false,
                        "unblocked_at" to Instant.now().toString()
                    )
                )
                .filter { 
                    eq("identifier", identifier)
                    eq("limit_type", limitType)
                    eq("is_active", true)
                }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRateLimitStatistics(
        startDate: Instant? = null,
        endDate: Instant? = null
    ): Result<RateLimitStatistics> {
        return try {
            // Get total requests
            val totalRequests = postgrest.from("rate_limit_logs")
                .select("count")
                .let { query ->
                    if (startDate != null) {
                        query.filter { gte("created_at", startDate.toString()) }
                    } else query
                }
                .let { query ->
                    if (endDate != null) {
                        query.filter { lte("created_at", endDate.toString()) }
                    } else query
                }
                .maybeSingle()
                .data
            
            // Get violations
            val violations = postgrest.from("rate_limit_violations")
                .select("count")
                .let { query ->
                    if (startDate != null) {
                        query.filter { gte("created_at", startDate.toString()) }
                    } else query
                }
                .let { query ->
                    if (endDate != null) {
                        query.filter { lte("created_at", endDate.toString()) }
                    } else query
                }
                .maybeSingle()
                .data
            
            // Get active blocks
            val activeBlocks = postgrest.from("rate_limit_blocks")
                .select("count")
                .filter { eq("is_active", true) }
                .maybeSingle()
                .data
            
            val totalCount = when (totalRequests) {
                is Map<*, *> -> (totalRequests["count"] as? Number)?.toInt() ?: 0
                else -> 0
            }
            
            val violationCount = when (violations) {
                is Map<*, *> -> (violations["count"] as? Number)?.toInt() ?: 0
                else -> 0
            }
            
            val blockCount = when (activeBlocks) {
                is Map<*, *> -> (activeBlocks["count"] as? Number)?.toInt() ?: 0
                else -> 0
            }
            
            val statistics = RateLimitStatistics(
                totalRequests = totalCount,
                totalViolations = violationCount,
                activeBlocks = blockCount,
                violationRate = if (totalCount > 0) (violationCount.toDouble() / totalCount) * 100 else 0.0,
                generatedAt = Instant.now().toString()
            )
            
            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTopViolators(
        limit: Int = 10,
        startDate: Instant? = null,
        endDate: Instant? = null
    ): Result<List<TopViolator>> {
        return try {
            val violators = postgrest.from("rate_limit_violations")
                .select("identifier, limit_type, count(*) as violation_count")
                .let { query ->
                    if (startDate != null) {
                        query.filter { gte("created_at", startDate.toString()) }
                    } else query
                }
                .let { query ->
                    if (endDate != null) {
                        query.filter { lte("created_at", endDate.toString()) }
                    } else query
                }
                .order("violation_count", ascending = false)
                .limit(limit)
                .data
            
            val topViolators = violators.map { violation ->
                TopViolator(
                    identifier = violation.identifier,
                    limitType = violation.limit_type,
                    violationCount = violation.violation_count,
                    lastViolation = violation.created_at
                )
            }
            
            Result.success(topViolators)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRateLimitConfig(
        limitType: String
    ): Result<RateLimitConfig> {
        return try {
            val config = postgrest.from("rate_limit_configs")
                .select()
                .filter { eq("limit_type", limitType) }
                .maybeSingle()
                .data
            
            if (config != null) {
                Result.success(
                    RateLimitConfig(
                        limitType = config.limit_type,
                        maxRequests = config.max_requests,
                        windowMinutes = config.window_minutes,
                        blockDurationMinutes = config.block_duration_minutes,
                        isActive = config.is_active
                    )
                )
            } else {
                // Return default config
                val defaultConfig = when (limitType) {
                    USER_RATE_LIMIT -> RateLimitConfig(limitType, DEFAULT_USER_LIMIT, DEFAULT_WINDOW_MINUTES, 60, true)
                    IP_RATE_LIMIT -> RateLimitConfig(limitType, DEFAULT_IP_LIMIT, DEFAULT_WINDOW_MINUTES, 60, true)
                    API_KEY_RATE_LIMIT -> RateLimitConfig(limitType, DEFAULT_API_KEY_LIMIT, DEFAULT_WINDOW_MINUTES, 60, true)
                    else -> RateLimitConfig(limitType, 100, 1, 60, true)
                }
                Result.success(defaultConfig)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateRateLimitConfig(
        config: RateLimitConfig
    ): Result<RateLimitConfig> {
        return try {
            val updateData = mapOf(
                "max_requests" to config.maxRequests,
                "window_minutes" to config.windowMinutes,
                "block_duration_minutes" to config.blockDurationMinutes,
                "is_active" to config.isActive,
                "updated_at" to Instant.now().toString()
            )
            
            val updatedConfig = postgrest.from("rate_limit_configs")
                .update(updateData)
                .filter { eq("limit_type", config.limitType) }
                .maybeSingle()
                .data
            
            if (updatedConfig != null) {
                Result.success(config)
            } else {
                // Insert new config
                val insertData = mapOf(
                    "limit_type" to config.limitType,
                    "max_requests" to config.maxRequests,
                    "window_minutes" to config.windowMinutes,
                    "block_duration_minutes" to config.blockDurationMinutes,
                    "is_active" to config.isActive,
                    "created_at" to Instant.now().toString(),
                    "updated_at" to Instant.now().toString()
                )
                
                postgrest.from("rate_limit_configs")
                    .insert(insertData)
                    .maybeSingle()
                    .data
                
                Result.success(config)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getCurrentRequestCount(
        identifier: String,
        limitType: String,
        windowMinutes: Int
    ): Result<Int> {
        return try {
            val windowStart = Instant.now().minus(windowMinutes.toLong(), ChronoUnit.MINUTES)
            
            val count = postgrest.from("rate_limit_logs")
                .select("count")
                .filter { 
                    eq("identifier", identifier)
                    eq("limit_type", limitType)
                    gte("created_at", windowStart.toString())
                }
                .maybeSingle()
                .data
            
            val requestCount = when (count) {
                is Map<*, *> -> (count["count"] as? Number)?.toInt() ?: 0
                else -> 0
            }
            
            Result.success(requestCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun incrementRequestCount(
        identifier: String,
        limitType: String,
        windowMinutes: Int
    ): Result<Unit> {
        return try {
            val logData = mapOf(
                "identifier" to identifier,
                "limit_type" to limitType,
                "window_minutes" to windowMinutes,
                "created_at" to Instant.now().toString()
            )
            
            postgrest.from("rate_limit_logs")
                .insert(logData)
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun isIdentifierBlocked(
        identifier: String,
        limitType: String
    ): Result<Boolean> {
        return try {
            val block = postgrest.from("rate_limit_blocks")
                .select()
                .filter { 
                    eq("identifier", identifier)
                    eq("limit_type", limitType)
                    eq("is_active", true)
                }
                .maybeSingle()
                .data
            
            // Check if block has expired
            if (block != null) {
                val unblockTime = Instant.parse(block.unblock_at)
                if (Instant.now().isAfter(unblockTime)) {
                    // Auto-unblock expired block
                    unblockIdentifier(identifier, limitType)
                    Result.success(false)
                } else {
                    Result.success(true)
                }
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun calculateResetTime(
        identifier: String,
        limitType: String,
        windowMinutes: Int
    ): Instant {
        return try {
            val oldestRequest = postgrest.from("rate_limit_logs")
                .select("created_at")
                .filter { 
                    eq("identifier", identifier)
                    eq("limit_type", limitType)
                }
                .order("created_at", ascending = true)
                .limit(1)
                .maybeSingle()
                .data
            
            if (oldestRequest != null) {
                val requestTime = Instant.parse(oldestRequest.created_at)
                requestTime.plus(windowMinutes.toLong(), ChronoUnit.MINUTES)
            } else {
                Instant.now().plus(windowMinutes.toLong(), ChronoUnit.MINUTES)
            }
        } catch (e: Exception) {
            Instant.now().plus(windowMinutes.toLong(), ChronoUnit.MINUTES)
        }
    }
    
    private suspend fun logRateLimitViolation(
        identifier: String,
        limitType: String,
        currentCount: Int,
        maxRequests: Int,
        windowMinutes: Int
    ): Result<Unit> {
        return try {
            val violationData = mapOf(
                "identifier" to identifier,
                "limit_type" to limitType,
                "current_count" to currentCount,
                "max_requests" to maxRequests,
                "window_minutes" to windowMinutes,
                "created_at" to Instant.now().toString()
            )
            
            postgrest.from("rate_limit_violations")
                .insert(violationData)
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeRateLimitUpdates(): Flow<RateLimitUpdate> = flow {
        try {
            // TODO: Implement real-time updates via Supabase Realtime
            emit(RateLimitUpdate.ConfigUpdated)
        } catch (e: Exception) {
            emit(RateLimitUpdate.Error(e.message ?: "Unknown error"))
        }
    }
}

// Data classes
data class RateLimitResult(
    val allowed: Boolean,
    val remainingRequests: Int,
    val resetTime: Instant,
    val blocked: Boolean,
    val blockReason: String?
)

data class RateLimitStatistics(
    val totalRequests: Int,
    val totalViolations: Int,
    val activeBlocks: Int,
    val violationRate: Double,
    val generatedAt: String
)

data class TopViolator(
    val identifier: String,
    val limitType: String,
    val violationCount: Int,
    val lastViolation: String
)

data class RateLimitConfig(
    val limitType: String,
    val maxRequests: Int,
    val windowMinutes: Int,
    val blockDurationMinutes: Int,
    val isActive: Boolean
)

sealed class RateLimitUpdate {
    object ConfigUpdated : RateLimitUpdate()
    object NewBlock : RateLimitUpdate()
    object BlockRemoved : RateLimitUpdate()
    data class Error(val message: String) : RateLimitUpdate()
}
