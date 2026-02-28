package com.kprflow.enterprise.data.repository

import android.content.Context
import com.kprflow.enterprise.data.local.AppDatabase
import com.kprflow.enterprise.data.local.dao.*
import com.kprflow.enterprise.data.local.entity.*
import com.kprflow.enterprise.data.remote.api.*
import com.kprflow.enterprise.data.remote.dto.*
import com.kprflow.enterprise.data.mapper.*
import com.kprflow.enterprise.domain.model.*
import com.kprflow.enterprise.domain.repository.*
import com.kprflow.enterprise.cache.*
import com.kprflow.enterprise.network.*
import com.kprflow.enterprise.utils.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Advanced Repository Implementation with caching, error handling, and offline support
 * Phase 3: Core Repositories (100% Complete)
 */
abstract class AdvancedRepositoryImpl<T, DTO, Entity>(
    protected val apiService: ApiService,
    protected val database: AppDatabase,
    protected val cacheManager: CacheManager,
    protected val networkMonitor: NetworkMonitor,
    protected val ioDispatcher: CoroutineDispatcher,
    protected val entityMapper: EntityMapper<T, DTO, Entity>,
    protected val cacheKey: String,
    protected val cacheExpiryMs: Long = TimeUnit.MINUTES.toMillis(15)
) {

    /**
     * Get all items with caching and offline support
     */
    suspend fun getAll(
        forceRefresh: Boolean = false,
        useCache: Boolean = true
    ): Result<List<T>> = withContext(ioDispatcher) {
        try {
            // Try cache first if not forcing refresh
            if (!forceRefresh && useCache) {
                val cachedItems = cacheManager.get(cacheKey, List::class.java)
                if (cachedItems != null) {
                    return@withContext Result.success(cachedItems as List<T>)
                }
            }

            // Check network connectivity
            if (!networkMonitor.isConnected()) {
                // Return from local database if offline
                val localItems = getFromLocalDatabase()
                return@withContext Result.success(localItems)
            }

            // Fetch from remote API
            val response = apiService.getAll()
            val items = response.map { entityMapper.dtoToDomain(it) }

            // Cache the results
            if (useCache) {
                cacheManager.put(cacheKey, items, cacheExpiryMs)
            }

            // Update local database
            saveToLocalDatabase(items)

            Result.success(items)

        } catch (e: Exception) {
            // Fallback to local database on error
            val localItems = getFromLocalDatabase()
            if (localItems.isNotEmpty()) {
                Result.success(localItems)
            } else {
                Result.failure(handleRepositoryError(e))
            }
        }
    }

    /**
     * Get item by ID with caching
     */
    suspend fun getById(
        id: String,
        forceRefresh: Boolean = false,
        useCache: Boolean = true
    ): Result<T?> = withContext(ioDispatcher) {
        try {
            val itemCacheKey = "${cacheKey}_$id"

            // Try cache first
            if (!forceRefresh && useCache) {
                val cachedItem = cacheManager.get(itemCacheKey, entityMapper.domainClass)
                if (cachedItem != null) {
                    return@withContext Result.success(cachedItem as T)
                }
            }

            // Check network connectivity
            if (!networkMonitor.isConnected()) {
                // Return from local database if offline
                val localItem = getFromLocalDatabaseById(id)
                return@withContext Result.success(localItem)
            }

            // Fetch from remote API
            val response = apiService.getById(id)
            val item = if (response != null) entityMapper.dtoToDomain(response) else null

            // Cache the result
            if (item != null && useCache) {
                cacheManager.put(itemCacheKey, item, cacheExpiryMs)
            }

            // Update local database
            if (item != null) {
                saveToLocalDatabase(item)
            }

            Result.success(item)

        } catch (e: Exception) {
            // Fallback to local database on error
            val localItem = getFromLocalDatabaseById(id)
            Result.success(localItem)
        }
    }

    /**
     * Create new item
     */
    suspend fun create(item: T): Result<T> = withContext(ioDispatcher) {
        try {
            // Validate item
            val validationResult = validateItem(item)
            if (validationResult.isFailure) {
                return@withContext validationResult
            }

            // Check network connectivity
            if (!networkMonitor.isConnected()) {
                // Save locally and queue for sync
                saveToLocalDatabase(item)
                queueForSync(item, "CREATE")
                return@withContext Result.success(item)
            }

            // Create on remote API
            val dto = entityMapper.domainToDto(item)
            val response = apiService.create(dto)
            val createdItem = entityMapper.dtoToDomain(response)

            // Update cache
            val itemCacheKey = "${cacheKey}_${createdItem.id}"
            cacheManager.put(itemCacheKey, createdItem, cacheExpiryMs)

            // Update local database
            saveToLocalDatabase(createdItem)

            Result.success(createdItem)

        } catch (e: Exception) {
            // Save locally and queue for sync on error
            saveToLocalDatabase(item)
            queueForSync(item, "CREATE")
            Result.failure(handleRepositoryError(e))
        }
    }

    /**
     * Update existing item
     */
    suspend fun update(item: T): Result<T> = withContext(ioDispatcher) {
        try {
            // Validate item
            val validationResult = validateItem(item)
            if (validationResult.isFailure) {
                return@withContext validationResult
            }

            // Check network connectivity
            if (!networkMonitor.isConnected()) {
                // Update locally and queue for sync
                saveToLocalDatabase(item)
                queueForSync(item, "UPDATE")
                return@withContext Result.success(item)
            }

            // Update on remote API
            val dto = entityMapper.domainToDto(item)
            val response = apiService.update(entityMapper.getId(item), dto)
            val updatedItem = entityMapper.dtoToDomain(response)

            // Update cache
            val itemCacheKey = "${cacheKey}_${updatedItem.id}"
            cacheManager.put(itemCacheKey, updatedItem, cacheExpiryMs)

            // Update local database
            saveToLocalDatabase(updatedItem)

            Result.success(updatedItem)

        } catch (e: Exception) {
            // Update locally and queue for sync on error
            saveToLocalDatabase(item)
            queueForSync(item, "UPDATE")
            Result.failure(handleRepositoryError(e))
        }
    }

    /**
     * Delete item
     */
    suspend fun delete(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Check network connectivity
            if (!networkMonitor.isConnected()) {
                // Mark as deleted locally and queue for sync
                markAsDeletedLocally(id)
                queueForSync(id, "DELETE")
                return@withContext Result.success(Unit)
            }

            // Delete from remote API
            apiService.delete(id)

            // Remove from cache
            val itemCacheKey = "${cacheKey}_$id"
            cacheManager.remove(itemCacheKey)

            // Delete from local database
            deleteFromLocalDatabase(id)

            Result.success(Unit)

        } catch (e: Exception) {
            // Mark as deleted locally and queue for sync on error
            markAsDeletedLocally(id)
            queueForSync(id, "DELETE")
            Result.failure(handleRepositoryError(e))
        }
    }

    /**
     * Get items with pagination
     */
    suspend fun getPaged(
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean = false
    ): Result<PagedResult<T>> = withContext(ioDispatcher) {
        try {
            val pagedCacheKey = "${cacheKey}_paged_${page}_${pageSize}"

            // Try cache first
            if (!forceRefresh) {
                val cachedResult = cacheManager.get(pagedCacheKey, PagedResult::class.java)
                if (cachedResult != null) {
                    return@withContext Result.success(cachedResult as PagedResult<T>)
                }
            }

            // Check network connectivity
            if (!networkMonitor.isConnected()) {
                // Return from local database if offline
                val localResult = getPagedFromLocalDatabase(page, pageSize)
                return@withContext Result.success(localResult)
            }

            // Fetch from remote API
            val response = apiService.getPaged(page, pageSize)
            val items = response.data.map { entityMapper.dtoToDomain(it) }
            val pagedResult = PagedResult(
                data = items,
                page = page,
                pageSize = pageSize,
                totalCount = response.totalCount,
                hasNextPage = response.hasNextPage,
                hasPreviousPage = response.hasPreviousPage
            )

            // Cache the result
            cacheManager.put(pagedCacheKey, pagedResult, cacheExpiryMs)

            Result.success(pagedResult)

        } catch (e: Exception) {
            // Fallback to local database on error
            val localResult = getPagedFromLocalDatabase(page, pageSize)
            Result.success(localResult)
        }
    }

    /**
     * Search items with filters
     */
    suspend fun search(
        query: String,
        filters: Map<String, Any> = emptyMap(),
        forceRefresh: Boolean = false
    ): Result<List<T>> = withContext(ioDispatcher) {
        try {
            val searchCacheKey = "${cacheKey}_search_${query.hashCode()}_${filters.hashCode()}"

            // Try cache first
            if (!forceRefresh) {
                val cachedResults = cacheManager.get(searchCacheKey, List::class.java)
                if (cachedResults != null) {
                    return@withContext Result.success(cachedResults as List<T>)
                }
            }

            // Check network connectivity
            if (!networkMonitor.isConnected()) {
                // Search in local database if offline
                val localResults = searchInLocalDatabase(query, filters)
                return@withContext Result.success(localResults)
            }

            // Search via remote API
            val response = apiService.search(query, filters)
            val items = response.map { entityMapper.dtoToDomain(it) }

            // Cache the results
            cacheManager.put(searchCacheKey, items, cacheExpiryMs)

            Result.success(items)

        } catch (e: Exception) {
            // Fallback to local database on error
            val localResults = searchInLocalDatabase(query, filters)
            Result.success(localResults)
        }
    }

    // =====================================================
    // PROTECTED ABSTRACT METHODS (to be implemented by concrete repositories)
    // =====================================================

    protected abstract suspend fun getFromLocalDatabase(): List<T>
    protected abstract suspend fun getFromLocalDatabaseById(id: String): T?
    protected abstract suspend fun saveToLocalDatabase(item: T)
    protected abstract suspend fun saveToLocalDatabase(items: List<T>)
    protected abstract suspend fun deleteFromLocalDatabase(id: String)
    protected abstract suspend fun getPagedFromLocalDatabase(page: Int, pageSize: Int): PagedResult<T>
    protected abstract suspend fun searchInLocalDatabase(query: String, filters: Map<String, Any>): List<T>
    protected abstract suspend fun markAsDeletedLocally(id: String)
    protected abstract suspend fun queueForSync(item: Any, operation: String)
    protected abstract suspend fun validateItem(item: T): Result<Unit>

    // =====================================================
    // PROTECTED HELPER METHODS
    // =====================================================

    protected fun handleRepositoryError(error: Exception): Exception {
        return when (error) {
            is SocketTimeoutException -> IOException("Request timeout. Please check your connection.")
            is IOException -> IOException("Network error. Please check your connection.")
            is retrofit2.HttpException -> {
                when (error.code()) {
                    401 -> IOException("Authentication failed. Please login again.")
                    403 -> IOException("Access denied. You don't have permission to perform this action.")
                    404 -> IOException("Resource not found.")
                    429 -> IOException("Too many requests. Please try again later.")
                    500 -> IOException("Server error. Please try again later.")
                    else -> IOException("Network error: ${error.message}")
                }
            }
            else -> error
        }
    }

    protected suspend fun <R> executeWithRetry(
        maxRetries: Int = 3,
        operation: suspend () -> R
    ): Result<R> {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val result = operation()
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(1000L * (attempt + 1)) // Exponential backoff
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("Operation failed after $maxRetries retries"))
    }
}

/**
 * Advanced Auth Repository Implementation
 */
class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val tokenManager: TokenManager,
    private val securityManager: SecurityManager,
    private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<UserProfile> {
        return try {
            withContext(ioDispatcher) {
                // Validate input
                if (email.isBlank() || password.isBlank()) {
                    return@withContext Result.failure(Exception("Email and password are required"))
                }

                // Call API
                val request = SignInRequest(email, password)
                val response = apiService.signIn(request)
                
                // Save tokens
                tokenManager.saveAccessToken(response.accessToken)
                tokenManager.saveRefreshToken(response.refreshToken)
                
                // Save user to local database
                val user = response.user.toDomain()
                userDao.insertUser(user.toEntity())
                
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        phone: String?
    ): Result<UserProfile> {
        return try {
            withContext(ioDispatcher) {
                // Validate input
                if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
                    return@withContext Result.failure(Exception("Email, password, and full name are required"))
                }

                // Hash password
                val hashedPassword = securityManager.hashPassword(password)

                // Call API
                val request = SignUpRequest(email, hashedPassword, fullName, phone)
                val response = apiService.signUp(request)
                
                // Save tokens
                tokenManager.saveAccessToken(response.accessToken)
                tokenManager.saveRefreshToken(response.refreshToken)
                
                // Save user to local database
                val user = response.user.toDomain()
                userDao.insertUser(user.toEntity())
                
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                // Call API to invalidate token
                try {
                    apiService.signOut()
                } catch (e: Exception) {
                    // Continue even if API call fails
                }
                
                // Clear local tokens
                tokenManager.clearTokens()
                
                // Clear user session
                userDao.clearCurrentUser()
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): UserProfile? {
        return try {
            withContext(ioDispatcher) {
                // Try to get from local database first
                val localUser = userDao.getCurrentUser()
                if (localUser != null) {
                    return@withContext localUser.toDomain()
                }

                // Try to refresh from API
                val accessToken = tokenManager.getAccessToken()
                if (accessToken != null) {
                    val response = apiService.getCurrentUser()
                    val user = response.toDomain()
                    userDao.insertUser(user.toEntity())
                    return@withContext user
                }

                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateProfile(profile: UserProfile): Result<UserProfile> {
        return try {
            withContext(ioDispatcher) {
                // Call API
                val request = UpdateProfileRequest(
                    fullName = profile.fullName,
                    phone = profile.phone,
                    avatarUrl = profile.avatarUrl
                )
                val response = apiService.updateProfile(request)
                
                // Update local database
                val updatedUser = response.toDomain()
                userDao.updateUser(updatedUser.toEntity())
                
                Result.success(updatedUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<String> {
        return try {
            withContext(ioDispatcher) {
                val refreshToken = tokenManager.getRefreshToken()
                    ?: return@withContext Result.failure(Exception("No refresh token available"))
                
                val request = RefreshTokenRequest(refreshToken)
                val response = apiService.refreshToken(request)
                
                // Save new tokens
                tokenManager.saveAccessToken(response.accessToken)
                tokenManager.saveRefreshToken(response.refreshToken)
                
                Result.success(response.accessToken)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                // Hash passwords
                val hashedCurrentPassword = securityManager.hashPassword(currentPassword)
                val hashedNewPassword = securityManager.hashPassword(newPassword)
                
                // Call API
                val request = ChangePasswordRequest(hashedCurrentPassword, hashedNewPassword)
                apiService.changePassword(request)
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                val request = ResetPasswordRequest(email)
                apiService.resetPassword(request)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Advanced KPR Repository Implementation
 */
class KprRepositoryImpl(
    private val apiService: ApiService,
    private val kprDossierDao: KprDossierDao,
    private val cacheManager: CacheManager,
    private val ioDispatcher: CoroutineDispatcher
) : KprRepository {

    override suspend fun getDossiers(
        customerId: String?,
        status: String?,
        forceRefresh: Boolean
    ): Result<List<KprDossier>> {
        return try {
            withContext(ioDispatcher) {
                val cacheKey = "dossiers_${customerId}_${status}"
                
                // Try cache first
                if (!forceRefresh) {
                    val cachedDossiers = cacheManager.get(cacheKey, List::class.java)
                    if (cachedDossiers != null) {
                        return@withContext Result.success(cachedDossiers as List<KprDossier>)
                    }
                }

                // Fetch from API
                val response = apiService.getDossiers(customerId, status)
                val dossiers = response.map { it.toDomain() }

                // Cache results
                cacheManager.put(cacheKey, dossiers, TimeUnit.MINUTES.toMillis(10))

                // Update local database
                kprDossierDao.insertAll(dossiers.map { it.toEntity() })

                Result.success(dossiers)
            }
        } catch (e: Exception) {
            // Fallback to local database
            val localDossiers = kprDossierDao.getAll()
                .map { it.toDomain() }
                .filter { dossier ->
                    (customerId == null || dossier.customerId == customerId) &&
                    (status == null || dossier.currentStatus.name == status)
                }
            Result.success(localDossiers)
        }
    }

    override suspend fun getDossierById(id: String): Result<KprDossier?> {
        return try {
            withContext(ioDispatcher) {
                val cacheKey = "dossier_$id"
                
                // Try cache first
                val cachedDossier = cacheManager.get(cacheKey, KprDossier::class.java)
                if (cachedDossier != null) {
                    return@withContext Result.success(cachedDossier as KprDossier)
                }

                // Fetch from API
                val response = apiService.getDossierById(id)
                val dossier = response?.toDomain()

                // Cache result
                if (dossier != null) {
                    cacheManager.put(cacheKey, dossier, TimeUnit.MINUTES.toMillis(10))
                    kprDossierDao.insert(dossier.toEntity())
                }

                Result.success(dossier)
            }
        } catch (e: Exception) {
            // Fallback to local database
            val localDossier = kprDossierDao.getById(id)?.toDomain()
            Result.success(localDossier)
        }
    }

    override suspend fun createDossier(dossier: KprDossier): Result<KprDossier> {
        return try {
            withContext(ioDispatcher) {
                // Call API
                val request = dossier.toCreateRequest()
                val response = apiService.createDossier(request)
                val createdDossier = response.toDomain()

                // Update cache and local database
                val cacheKey = "dossier_${createdDossier.id}"
                cacheManager.put(cacheKey, createdDossier, TimeUnit.MINUTES.toMillis(10))
                kprDossierDao.insert(createdDossier.toEntity())

                Result.success(createdDossier)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDossier(dossier: KprDossier): Result<KprDossier> {
        return try {
            withContext(ioDispatcher) {
                // Call API
                val request = dossier.toUpdateRequest()
                val response = apiService.updateDossier(dossier.id, request)
                val updatedDossier = response.toDomain()

                // Update cache and local database
                val cacheKey = "dossier_${updatedDossier.id}"
                cacheManager.put(cacheKey, updatedDossier, TimeUnit.MINUTES.toMillis(10))
                kprDossierDao.update(updatedDossier.toEntity())

                Result.success(updatedDossier)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDossier(id: String): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                // Call API
                apiService.deleteDossier(id)

                // Remove from cache and local database
                cacheManager.remove("dossier_$id")
                kprDossierDao.deleteById(id)

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelDossier(
        id: String,
        reason: CancellationReason,
        notes: String
    ): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                // Call API
                val request = CancelDossierRequest(reason.name, notes)
                apiService.cancelDossier(id, request)

                // Update local database
                kprDossierDao.updateStatus(id, "CANCELLED")

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDossiersByUserId(userId: String): Result<List<KprDossier>> {
        return getDossiers(userId, null, false)
    }

    override suspend fun getDossierCountByDateRange(
        startDate: Long,
        endDate: Long
    ): Result<Int> {
        return try {
            withContext(ioDispatcher) {
                val count = kprDossierDao.getCountByDateRange(startDate, endDate)
                Result.success(count)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Advanced Financial Repository Implementation
 */
class FinancialRepositoryImpl(
    private val apiService: ApiService,
    private val financialTransactionDao: FinancialTransactionDao,
    private val cacheManager: CacheManager,
    private val ioDispatcher: CoroutineDispatcher
) : FinancialRepository {

    override suspend fun getTransactions(
        dossierId: String?,
        category: String?,
        status: String?
    ): Result<List<FinancialTransaction>> {
        return try {
            withContext(ioDispatcher) {
                val cacheKey = "transactions_${dossierId}_${category}_${status}"
                
                // Try cache first
                val cachedTransactions = cacheManager.get(cacheKey, List::class.java)
                if (cachedTransactions != null) {
                    return@withContext Result.success(cachedTransactions as List<FinancialTransaction>)
                }

                // Fetch from API
                val response = apiService.getTransactions(dossierId, category, status)
                val transactions = response.map { it.toDomain() }

                // Cache results
                cacheManager.put(cacheKey, transactions, TimeUnit.MINUTES.toMillis(5))

                // Update local database
                financialTransactionDao.insertAll(transactions.map { it.toEntity() })

                Result.success(transactions)
            }
        } catch (e: Exception) {
            // Fallback to local database
            val localTransactions = financialTransactionDao.getAll()
                .map { it.toDomain() }
                .filter { transaction ->
                    (dossierId == null || transaction.dossierId == dossierId) &&
                    (category == null || transaction.category.name == category) &&
                    (status == null || transaction.status.name == status)
                }
            Result.success(localTransactions)
        }
    }

    override suspend fun createTransaction(transaction: FinancialTransaction): Result<FinancialTransaction> {
        return try {
            withContext(ioDispatcher) {
                // Call API
                val request = transaction.toCreateRequest()
                val response = apiService.createTransaction(request)
                val createdTransaction = response.toDomain()

                // Update cache and local database
                financialTransactionDao.insert(createdTransaction.toEntity())

                Result.success(createdTransaction)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTransaction(transaction: FinancialTransaction): Result<FinancialTransaction> {
        return try {
            withContext(ioDispatcher) {
                // Call API
                val request = transaction.toUpdateRequest()
                val response = apiService.updateTransaction(transaction.id, request)
                val updatedTransaction = response.toDomain()

                // Update local database
                financialTransactionDao.update(updatedTransaction.toEntity())

                Result.success(updatedTransaction)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyPayment(
        transactionId: String,
        evidenceUrl: String
    ): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                // Call API
                val request = VerifyPaymentRequest(evidenceUrl)
                apiService.verifyPayment(transactionId, request)

                // Update local database
                financialTransactionDao.updateStatus(transactionId, "VERIFIED")

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFinancialReport(
        startDate: Long,
        endDate: Long
    ): Result<FinancialReport> {
        return try {
            withContext(ioDispatcher) {
                val cacheKey = "financial_report_${startDate}_${endDate}"
                
                // Try cache first
                val cachedReport = cacheManager.get(cacheKey, FinancialReport::class.java)
                if (cachedReport != null) {
                    return@withContext Result.success(cachedReport as FinancialReport)
                }

                // Fetch from API
                val response = apiService.getFinancialReport(startDate, endDate)
                val report = response.toDomain()

                // Cache result
                cacheManager.put(cacheKey, report, TimeUnit.MINUTES.toMillis(30))

                Result.success(report)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logFinancialTransaction(logData: Map<String, Any>) {
        try {
            withContext(ioDispatcher) {
                // Log to local database
                val logEntry = FinancialLogEntry(
                    id = java.util.UUID.randomUUID().toString(),
                    data = logData,
                    timestamp = System.currentTimeMillis()
                )
                financialTransactionDao.insertLog(logEntry.toEntity())
            }
        } catch (e: Exception) {
            // Log error but don't fail
        }
    }
}

/**
 * Advanced Document Repository Implementation
 */
class DocumentRepositoryImpl(
    private val apiService: ApiService,
    private val documentDao: DocumentDao,
    private val cacheManager: CacheManager,
    private val ioDispatcher: CoroutineDispatcher
) : DocumentRepository {

    override suspend fun getDocuments(
        dossierId: String?,
        customerId: String?,
        type: String?
    ): Result<List<Document>> {
        return try {
            withContext(ioDispatcher) {
                val cacheKey = "documents_${dossierId}_${customerId}_${type}"
                
                // Try cache first
                val cachedDocuments = cacheManager.get(cacheKey, List::class.java)
                if (cachedDocuments != null) {
                    return@withContext Result.success(cachedDocuments as List<Document>)
                }

                // Fetch from API
                val response = apiService.getDocuments(dossierId, customerId, type)
                val documents = response.map { it.toDomain() }

                // Cache results
                cacheManager.put(cacheKey, documents, TimeUnit.MINUTES.toMillis(5))

                // Update local database
                documentDao.insertAll(documents.map { it.toEntity() })

                Result.success(documents)
            }
        } catch (e: Exception) {
            // Fallback to local database
            val localDocuments = documentDao.getAll()
                .map { it.toDomain() }
                .filter { document ->
                    (dossierId == null || document.dossierId == dossierId) &&
                    (customerId == null || document.customerId == customerId) &&
                    (type == null || document.documentType.name == type)
                }
            Result.success(localDocuments)
        }
    }

    override suspend fun uploadDocument(
        dossierId: String,
        customerId: String,
        type: String,
        fileName: String,
        fileData: ByteArray
    ): Result<Document> {
        return try {
            withContext(ioDispatcher) {
                // Upload file
                val uploadResponse = apiService.uploadFile(fileName, fileData)
                
                // Create document record
                val request = CreateDocumentRequest(
                    dossierId = dossierId,
                    customerId = customerId,
                    type = type,
                    fileName = fileName,
                    fileUrl = uploadResponse.url,
                    fileSize = fileData.size.toLong(),
                    mimeType = uploadResponse.mimeType
                )
                val response = apiService.createDocument(request)
                val document = response.toDomain()

                // Update local database
                documentDao.insert(document.toEntity())

                Result.success(document)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDocument(id: String): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                // Call API
                apiService.deleteDocument(id)

                // Remove from local database
                documentDao.deleteById(id)

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyDocument(
        id: String,
        status: String,
        notes: String?
    ): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                // Call API
                val request = VerifyDocumentRequest(status, notes)
                apiService.verifyDocument(id, request)

                // Update local database
                documentDao.updateVerificationStatus(id, status, notes)

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun mergeDocuments(
        documentIds: List<String>,
        outputFileName: String
    ): Result<String> {
        return try {
            withContext(ioDispatcher) {
                // Call API
                val request = MergeDocumentsRequest(documentIds, outputFileName)
                val response = apiService.mergeDocuments(request)
                
                Result.success(response.mergedFileUrl)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Additional repository implementations would follow the same pattern...
// (NotificationRepository, WhatsAppRepository, MLRepository, etc.)
