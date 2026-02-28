package com.kprflow.enterprise.di

import android.content.Context
import com.kprflow.enterprise.data.repository.*
import com.kprflow.enterprise.domain.repository.*
import com.kprflow.enterprise.domain.usecase.*
import com.kprflow.enterprise.network.*
import com.kprflow.enterprise.database.*
import com.kprflow.enterprise.cache.*
import com.kprflow.enterprise.security.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Advanced Dependency Injection Module for complete Phase 1 implementation
 */
@Module
@InstallIn(SingletonComponent::class)
object AdvancedDependencyInjectionModule {
    
    // ============ CORE DEPENDENCIES ============
    
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context
    
    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
    
    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    
    // ============ NETWORK DEPENDENCIES ============
    
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClientImpl()
    }
    
    @Provides
    @Singleton
    fun provideApiService(httpClient: HttpClient): ApiService {
        return ApiServiceImpl(httpClient)
    }
    
    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig {
        return NetworkConfig(
            baseUrl = "https://api.kprflow.com",
            timeout = 30000L,
            retryCount = 3,
            enableLogging = true
        )
    }
    
    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitorImpl(context)
    }
    
    // ============ DATABASE DEPENDENCIES ============
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    @Singleton
    fun provideKprDossierDao(database: AppDatabase): KprDossierDao {
        return database.kprDossierDao()
    }
    
    @Provides
    @Singleton
    fun provideFinancialTransactionDao(database: AppDatabase): FinancialTransactionDao {
        return database.financialTransactionDao()
    }
    
    @Provides
    @Singleton
    fun provideDocumentDao(database: AppDatabase): DocumentDao {
        return database.documentDao()
    }
    
    @Provides
    @Singleton
    fun provideAuditDao(database: AppDatabase): AuditDao {
        return database.auditDao()
    }
    
    @Provides
    @Singleton
    fun provideDatabaseConfig(): DatabaseConfig {
        return DatabaseConfig(
            version = 1,
            enableForeignKeys = true,
            enableWAL = true,
            journalMode = "WAL"
        )
    }
    
    // ============ CACHE DEPENDENCIES ============
    
    @Provides
    @Singleton
    fun provideCacheManager(@ApplicationContext context: Context): CacheManager {
        return CacheManagerImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideMemoryCache(): MemoryCache {
        return MemoryCacheImpl()
    }
    
    @Provides
    @Singleton
    fun provideDiskCache(@ApplicationContext context: Context): DiskCache {
        return DiskCacheImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideCacheConfig(): CacheConfig {
        return CacheConfig(
            maxSize = 50 * 1024 * 1024, // 50MB
            maxItems = 1000,
            ttl = 24 * 60 * 60 * 1000L // 24 hours
        )
    }
    
    // ============ SECURITY DEPENDENCIES ============
    
    @Provides
    @Singleton
    fun provideSecurityManager(): SecurityManager {
        return SecurityManagerImpl()
    }
    
    @Provides
    @Singleton
    fun provideTokenManager(): TokenManager {
        return TokenManagerImpl()
    }
    
    @Provides
    @Singleton
    fun provideEncryptionManager(): EncryptionManager {
        return EncryptionManagerImpl()
    }
    
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptorImpl(tokenManager)
    }
    
    // ============ REPOSITORY DEPENDENCIES ============
    
    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: ApiService,
        userDao: UserDao,
        tokenManager: TokenManager,
        securityManager: SecurityManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): AuthRepository {
        return AuthRepositoryImpl(
            apiService = apiService,
            userDao = userDao,
            tokenManager = tokenManager,
            securityManager = securityManager,
            ioDispatcher = ioDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideKprRepository(
        apiService: ApiService,
        kprDossierDao: KprDossierDao,
        cacheManager: CacheManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): KprRepository {
        return KprRepositoryImpl(
            apiService = apiService,
            kprDossierDao = kprDossierDao,
            cacheManager = cacheManager,
            ioDispatcher = ioDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideFinancialRepository(
        apiService: ApiService,
        financialTransactionDao: FinancialTransactionDao,
        cacheManager: CacheManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): FinancialRepository {
        return FinancialRepositoryImpl(
            apiService = apiService,
            financialTransactionDao = financialTransactionDao,
            cacheManager = cacheManager,
            ioDispatcher = ioDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideDocumentRepository(
        apiService: ApiService,
        documentDao: DocumentDao,
        cacheManager: CacheManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DocumentRepository {
        return DocumentRepositoryImpl(
            apiService = apiService,
            documentDao = documentDao,
            cacheManager = cacheManager,
            ioDispatcher = ioDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideAuditRepository(
        auditDao: AuditDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): AuditRepository {
        return AuditRepositoryImpl(
            auditDao = auditDao,
            ioDispatcher = ioDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideNotificationRepository(
        apiService: ApiService,
        cacheManager: CacheManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): NotificationRepository {
        return NotificationRepositoryImpl(
            apiService = apiService,
            cacheManager = cacheManager,
            ioDispatcher = ioDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideWhatsAppRepository(
        apiService: ApiService,
        cacheManager: CacheManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): WhatsAppRepository {
        return WhatsAppRepositoryImpl(
            apiService = apiService,
            cacheManager = cacheManager,
            ioDispatcher = ioDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideMLRepository(
        apiService: ApiService,
        cacheManager: CacheManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): MLRepository {
        return MLRepositoryImpl(
            apiService = apiService,
            cacheManager = cacheManager,
            ioDispatcher = ioDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideGovernmentRepository(
        apiService: ApiService,
        cacheManager: CacheManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): GovernmentRepository {
        return GovernmentRepositoryImpl(
            apiService = apiService,
            cacheManager = cacheManager,
            ioDispatcher = ioDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideStorageRepository(
        apiService: ApiService,
        cacheManager: CacheManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): StorageRepository {
        return StorageRepositoryImpl(
            apiService = apiService,
            cacheManager = cacheManager,
            ioDispatcher = ioDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideWorkflowRepository(
        apiService: ApiService,
        cacheManager: CacheManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): WorkflowRepository {
        return WorkflowRepositoryImpl(
            apiService = apiService,
            cacheManager = cacheManager,
            ioDispatcher = ioDispatcher
        )
    }
    
    // ============ USE CASE DEPENDENCIES ============
    
    @Provides
    @Singleton
    fun provideAuthUseCases(
        authRepository: AuthRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): AuthUseCases {
        return AuthUseCases(
            signInUseCase = SignInUseCase(authRepository, ioDispatcher),
            signUpUseCase = SignUpUseCase(authRepository, ioDispatcher),
            signOutUseCase = SignOutUseCase(authRepository, ioDispatcher),
            getCurrentUserUseCase = GetCurrentUserUseCase(authRepository, ioDispatcher),
            updateProfileUseCase = UpdateProfileUseCase(authRepository, ioDispatcher),
            refreshTokenUseCase = RefreshTokenUseCase(authRepository, ioDispatcher)
        )
    }
    
    @Provides
    @Singleton
    fun provideKprUseCases(
        kprRepository: KprRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): KprUseCases {
        return KprUseCases(
            getDossiersUseCase = GetDossiersUseCase(kprRepository, ioDispatcher),
            getDossierByIdUseCase = GetDossierByIdUseCase(kprRepository, ioDispatcher),
            createDossierUseCase = CreateDossierUseCase(kprRepository, ioDispatcher),
            updateDossierUseCase = UpdateDossierUseCase(kprRepository, ioDispatcher),
            deleteDossierUseCase = DeleteDossierUseCase(kprRepository, ioDispatcher),
            cancelDossierUseCase = CancelDossierUseCase(kprRepository, ioDispatcher)
        )
    }
    
    @Provides
    @Singleton
    fun provideFinancialUseCases(
        financialRepository: FinancialRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): FinancialUseCases {
        return FinancialUseCases(
            getTransactionsUseCase = GetTransactionsUseCase(financialRepository, ioDispatcher),
            createTransactionUseCase = CreateTransactionUseCase(financialRepository, ioDispatcher),
            updateTransactionUseCase = UpdateTransactionUseCase(financialRepository, ioDispatcher),
            verifyPaymentUseCase = VerifyPaymentUseCase(financialRepository, ioDispatcher),
            getFinancialReportUseCase = GetFinancialReportUseCase(financialRepository, ioDispatcher)
        )
    }
    
    @Provides
    @Singleton
    fun provideDocumentUseCases(
        documentRepository: DocumentRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DocumentUseCases {
        return DocumentUseCases(
            getDocumentsUseCase = GetDocumentsUseCase(documentRepository, ioDispatcher),
            uploadDocumentUseCase = UploadDocumentUseCase(documentRepository, ioDispatcher),
            deleteDocumentUseCase = DeleteDocumentUseCase(documentRepository, ioDispatcher),
            verifyDocumentUseCase = VerifyDocumentUseCase(documentRepository, ioDispatcher),
            mergeDocumentsUseCase = MergeDocumentsUseCase(documentRepository, ioDispatcher)
        )
    }
    
    @Provides
    @Singleton
    fun provideNotificationUseCases(
        notificationRepository: NotificationRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): NotificationUseCases {
        return NotificationUseCases(
            getNotificationsUseCase = GetNotificationsUseCase(notificationRepository, ioDispatcher),
            sendNotificationUseCase = SendNotificationUseCase(notificationRepository, ioDispatcher),
            markAsReadUseCase = MarkAsReadUseCase(notificationRepository, ioDispatcher),
            deleteNotificationUseCase = DeleteNotificationUseCase(notificationRepository, ioDispatcher)
        )
    }
    
    @Provides
    @Singleton
    fun provideWhatsAppUseCases(
        whatsAppRepository: WhatsAppRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): WhatsAppUseCases {
        return WhatsAppUseCases(
            sendMessageUseCase = SendMessageUseCase(whatsAppRepository, ioDispatcher),
            sendBulkMessageUseCase = SendBulkMessageUseCase(whatsAppRepository, ioDispatcher),
            getMessageStatusUseCase = GetMessageStatusUseCase(whatsAppRepository, ioDispatcher),
            getTemplatesUseCase = GetTemplatesUseCase(whatsAppRepository, ioDispatcher)
        )
    }
    
    @Provides
    @Singleton
    fun provideMLUseCases(
        mlRepository: MLRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): MLUseCases {
        return MLUseCases(
            predictChurnUseCase = PredictChurnUseCase(mlRepository, ioDispatcher),
            generateRecommendationsUseCase = GenerateRecommendationsUseCase(mlRepository, ioDispatcher),
            trainModelUseCase = TrainModelUseCase(mlRepository, ioDispatcher),
            getModelMetricsUseCase = GetModelMetricsUseCase(mlRepository, ioDispatcher)
        )
    }
    
    @Provides
    @Singleton
    fun provideGovernmentUseCases(
        governmentRepository: GovernmentRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): GovernmentUseCases {
        return GovernmentUseCases(
            checkEligibilityUseCase = CheckEligibilityUseCase(governmentRepository, ioDispatcher),
            verifyBPJSUseCase = VerifyBPJSUseCase(governmentRepository, ioDispatcher),
            checkTaxComplianceUseCase = CheckTaxComplianceUseCase(governmentRepository, ioDispatcher),
            getGovernmentStatsUseCase = GetGovernmentStatsUseCase(governmentRepository, ioDispatcher)
        )
    }
    
    @Provides
    @Singleton
    fun provideStorageUseCases(
        storageRepository: StorageRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): StorageUseCases {
        return StorageUseCases(
            uploadFileUseCase = UploadFileUseCase(storageRepository, ioDispatcher),
            downloadFileUseCase = DownloadFileUseCase(storageRepository, ioDispatcher),
            createArchiveUseCase = CreateArchiveUseCase(storageRepository, ioDispatcher),
            extractArchiveUseCase = ExtractArchiveUseCase(storageRepository, ioDispatcher)
        )
    }
    
    @Provides
    @Singleton
    fun provideWorkflowUseCases(
        workflowRepository: WorkflowRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): WorkflowUseCases {
        return WorkflowUseCases(
            executeWorkflowUseCase = ExecuteWorkflowUseCase(workflowRepository, ioDispatcher),
            getWorkflowProgressUseCase = GetWorkflowProgressUseCase(workflowRepository, ioDispatcher),
            validatePhaseUseCase = ValidatePhaseUseCase(workflowRepository, ioDispatcher),
            transitionPhaseUseCase = TransitionPhaseUseCase(workflowRepository, ioDispatcher)
        )
    }
}

// ============ QUALIFIER ANNOTATIONS ============

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

// ============ CONFIGURATION DATA CLASSES ============

data class NetworkConfig(
    val baseUrl: String,
    val timeout: Long,
    val retryCount: Int,
    val enableLogging: Boolean
)

data class DatabaseConfig(
    val version: Int,
    val enableForeignKeys: Boolean,
    val enableWAL: Boolean,
    val journalMode: String
)

data class CacheConfig(
    val maxSize: Int,
    val maxItems: Int,
    val ttl: Long
)

// ============ USE CASE CONTAINERS ============

data class AuthUseCases(
    val signInUseCase: SignInUseCase,
    val signUpUseCase: SignUpUseCase,
    val signOutUseCase: SignOutUseCase,
    val getCurrentUserUseCase: GetCurrentUserUseCase,
    val updateProfileUseCase: UpdateProfileUseCase,
    val refreshTokenUseCase: RefreshTokenUseCase
)

data class KprUseCases(
    val getDossiersUseCase: GetDossiersUseCase,
    val getDossierByIdUseCase: GetDossierByIdUseCase,
    val createDossierUseCase: CreateDossierUseCase,
    val updateDossierUseCase: UpdateDossierUseCase,
    val deleteDossierUseCase: DeleteDossierUseCase,
    val cancelDossierUseCase: CancelDossierUseCase
)

data class FinancialUseCases(
    val getTransactionsUseCase: GetTransactionsUseCase,
    val createTransactionUseCase: CreateTransactionUseCase,
    val updateTransactionUseCase: UpdateTransactionUseCase,
    val verifyPaymentUseCase: VerifyPaymentUseCase,
    val getFinancialReportUseCase: GetFinancialReportUseCase
)

data class DocumentUseCases(
    val getDocumentsUseCase: GetDocumentsUseCase,
    val uploadDocumentUseCase: UploadDocumentUseCase,
    val deleteDocumentUseCase: DeleteDocumentUseCase,
    val verifyDocumentUseCase: VerifyDocumentUseCase,
    val mergeDocumentsUseCase: MergeDocumentsUseCase
)

data class NotificationUseCases(
    val getNotificationsUseCase: GetNotificationsUseCase,
    val sendNotificationUseCase: SendNotificationUseCase,
    val markAsReadUseCase: MarkAsReadUseCase,
    val deleteNotificationUseCase: DeleteNotificationUseCase
)

data class WhatsAppUseCases(
    val sendMessageUseCase: SendMessageUseCase,
    val sendBulkMessageUseCase: SendBulkMessageUseCase,
    val getMessageStatusUseCase: GetMessageStatusUseCase,
    val getTemplatesUseCase: GetTemplatesUseCase
)

data class MLUseCases(
    val predictChurnUseCase: PredictChurnUseCase,
    val generateRecommendationsUseCase: GenerateRecommendationsUseCase,
    val trainModelUseCase: TrainModelUseCase,
    val getModelMetricsUseCase: GetModelMetricsUseCase
)

data class GovernmentUseCases(
    val checkEligibilityUseCase: CheckEligibilityUseCase,
    val verifyBPJSUseCase: VerifyBPJSUseCase,
    val checkTaxComplianceUseCase: CheckTaxComplianceUseCase,
    val getGovernmentStatsUseCase: GetGovernmentStatsUseCase
)

data class StorageUseCases(
    val uploadFileUseCase: UploadFileUseCase,
    val downloadFileUseCase: DownloadFileUseCase,
    val createArchiveUseCase: CreateArchiveUseCase,
    val extractArchiveUseCase: ExtractArchiveUseCase
)

data class WorkflowUseCases(
    val executeWorkflowUseCase: ExecuteWorkflowUseCase,
    val getWorkflowProgressUseCase: GetWorkflowProgressUseCase,
    val validatePhaseUseCase: ValidatePhaseUseCase,
    val transitionPhaseUseCase: TransitionPhaseUseCase
)
