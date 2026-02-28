# KPRFlow Enterprise - Dependency Injection Best Practices

---

## 📋 **OVERVIEW**

This document outlines the best practices for Dependency Injection (DI) using Hilt in KPRFlow Enterprise, ensuring maintainable, testable, and scalable code.

---

## 🎯 **CORE PRINCIPLES**

### **1. Interface-Based Injection**
- **Always inject interfaces, not concrete classes**
- **Benefits**: Easy unit testing, loose coupling, better maintainability
- **Implementation**: Use `@Binds` for interface-to-implementation binding

### **2. Singleton Management**
- **Use `@Singleton` for stateless services**
- **Prevent memory leaks and multiple instances**
- **Ensure thread safety for shared resources**

### **3. Scope Management**
- **Use appropriate scopes**: `@Singleton`, `@ActivityScoped`, `@FragmentScoped`
- **Prevent memory leaks with proper lifecycle awareness**
- **Optimize performance with scoped dependencies**

---

## 🏗️ **ARCHITECTURE PATTERNS**

### **Repository Pattern with Interfaces**

#### **Interface Definition**
```kotlin
// Domain layer - pure interfaces
interface IAuthRepository {
    suspend fun signIn(email: String, password: String): Result<UserProfile>
    suspend fun signUp(email: String, password: String, ...): Result<UserProfile>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): UserProfile?
    fun isUserLoggedIn(): Boolean
    // ... other methods
}
```

#### **Implementation**
```kotlin
// Data layer - concrete implementations
@Singleton
class AuthRepository @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : IAuthRepository {
    
    override suspend fun signIn(email: String, password: String): Result<UserProfile> {
        // Implementation details
    }
    
    // ... other method implementations
}
```

#### **Dependency Binding**
```kotlin
// DI module - bind interface to implementation
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepository: AuthRepository
    ): IAuthRepository
}
```

#### **Usage in ViewModel**
```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: IAuthRepository  // Interface injection!
) : ViewModel() {
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            authRepository.signIn(email, password)
                .onSuccess { /* handle success */ }
                .onFailure { /* handle error */ }
        }
    }
}
```

---

## 🔧 **MODULE ORGANIZATION**

### **1. SupabaseModule**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(@ApplicationContext context: Context): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = getSupabaseUrl(),
            supabaseKey = getSupabaseKey()
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }

    @Provides
    @Singleton
    fun provideAuth(supabaseClient: SupabaseClient): Auth {
        return supabaseClient.auth
    }

    // ... other Supabase providers
}
```

### **2. RepositoryModule**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepository: AuthRepository
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindKprRepository(
        kprRepository: KprRepository
    ): IKprRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        documentRepository: DocumentRepository
    ): IDocumentRepository
}
```

### **3. AppModule**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: Auth,
        postgrest: Postgrest
    ): IAuthRepository {
        return AuthRepository(auth, postgrest)
    }
    
    // ... other providers
}
```

---

## 🧪 **TESTING WITH INTERFACE INJECTION**

### **Unit Testing Example**

#### **Test Class Setup**
```kotlin
@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @Mock
    private lateinit var mockAuthRepository: IAuthRepository

    private lateinit var authViewModel: AuthViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Easy to create ViewModel with mocked interface!
        authViewModel = AuthViewModel(mockAuthRepository)
    }

    @Test
    fun `signIn success should update login state`() = runTest {
        // Given
        val testUser = createTestUser()
        `when`(mockAuthRepository.signIn("test@example.com", "password"))
            .thenReturn(Result.success(testUser))
        
        // When
        authViewModel.signIn("test@example.com", "password")
        
        // Then
        verify(mockAuthRepository).signIn("test@example.com", "password")
        assertEquals(testUser, authViewModel.currentUser.value)
    }
}
```

#### **Benefits of Interface Injection for Testing**
- **Easy Mocking**: Mock interfaces instead of concrete classes
- **Isolation**: Test only the ViewModel logic
- **Flexibility**: Swap implementations for different test scenarios
- **No Dependencies**: No need for real database or network

---

## 📊 **MEMORY MANAGEMENT**

### **Singleton Best Practices**

#### **✅ Good: Stateless Services**
```kotlin
@Singleton
class AuthRepository @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : IAuthRepository {
    // Stateless - no mutable state
    // Thread-safe operations only
}
```

#### **❌ Bad: Stateful Singletons**
```kotlin
@Singleton
class BadRepository @Inject constructor() {
    private var cache: Map<String, Any> = mutableMapOf()  // Mutable state!
    
    // This can cause memory leaks and thread safety issues
}
```

#### **✅ Good: Scoped State Management**
```kotlin
@ActivityScoped
class UserSessionManager @Inject constructor() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Scoped to activity lifecycle
    // Automatically cleaned up when activity is destroyed
}
```

---

## 🔍 **COMMON MISTAKES TO AVOID**

### **1. Direct Class Injection**
```kotlin
// ❌ BAD: Direct class injection
@HiltViewModel
class BadViewModel @Inject constructor(
    private val authRepository: AuthRepository  // Concrete class!
) : ViewModel()

// ✅ GOOD: Interface injection
@HiltViewModel
class GoodViewModel @Inject constructor(
    private val authRepository: IAuthRepository  // Interface!
) : ViewModel()
```

### **2. Missing Singleton Annotation**
```kotlin
// ❌ BAD: Missing @Singleton
class Repository @Inject constructor(
    private val api: ApiService
) {
    // New instance created every time - memory leak!
}

// ✅ GOOD: Proper singleton
@Singleton
class Repository @Inject constructor(
    private val api: ApiService
) {
    // Single instance shared across app
}
```

### **3. Wrong Scope Usage**
```kotlin
// ❌ BAD: ViewModel in Singleton scope
@Singleton  // Wrong scope!
class BadViewModel @Inject constructor(
    private val repository: IAuthRepository
) : ViewModel() {
    // ViewModel will never be garbage collected!
}

// ✅ GOOD: ViewModel in HiltViewModel scope
@HiltViewModel  // Correct scope!
class GoodViewModel @Inject constructor(
    private val repository: IAuthRepository
) : ViewModel() {
    // Properly scoped to ViewModel lifecycle
}
```

---

## 📈 **PERFORMANCE OPTIMIZATION**

### **1. Lazy Initialization**
```kotlin
@Singleton
class ExpensiveService @Inject constructor() {
    
    private val _heavyObject = lazy {
        // Expensive initialization
        createHeavyObject()
    }
    
    fun getHeavyObject() = _heavyObject.value
}
```

### **2. Qualifiers for Multiple Implementations**
```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProductionApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MockApi

@Provides
@Singleton
@ProductionApi
fun provideProductionApi(): ApiService = ProductionApiService()

@Provides
@Singleton
@MockApi
fun provideMockApi(): ApiService = MockApiService()
```

### **3. Conditional Binding**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return if (BuildConfig.DEBUG) {
            MockApiService()
        } else {
            ProductionApiService()
        }
    }
}
```

---

## 🔒 **SECURITY CONSIDERATIONS**

### **1. Secure Configuration**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = getSecureUrl(),  // From secure storage
            supabaseKey = getSecureKey()   // From secure storage
        )
    }
    
    private fun getSecureUrl(): String {
        // Get from BuildConfig or secure storage
        return BuildConfig.SUPABASE_URL
    }
    
    private fun getSecureKey(): String {
        // Get from BuildConfig or secure storage
        return BuildConfig.SUPABASE_ANON_KEY
    }
}
```

### **2. Environment-Specific Modules**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ProductionModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService = ProductionApiService()
}

@Module
@InstallIn(SingletonComponent::class)
object DevelopmentModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService = MockApiService()
}
```

---

## 📋 **CHECKLIST**

### **Before Implementation**
- [ ] Define interfaces for all repositories
- [ ] Plan dependency scopes
- [ ] Identify singleton candidates
- [ ] Design for testability

### **During Implementation**
- [ ] Use `@Binds` for interface-to-implementation binding
- [ ] Add `@Singleton` to stateless services
- [ ] Use appropriate scopes for lifecycle-aware components
- [ ] Inject interfaces in ViewModels and UI components

### **After Implementation**
- [ ] Verify no memory leaks
- [ ] Write unit tests with mocked interfaces
- [ ] Check dependency graph for cycles
- [ ] Validate configuration in different environments

---

## 🎯 **CONCLUSION**

Following these Dependency Injection best practices ensures:

### **✅ Benefits Achieved**
- **Testability**: Easy unit testing with interface mocking
- **Maintainability**: Loose coupling and clear dependencies
- **Performance**: Proper scoping and singleton management
- **Security**: Secure configuration and environment management
- **Scalability**: Clean architecture that grows with the app

### **🔧 Key Takeaways**
- **Always inject interfaces, not concrete classes**
- **Use `@Singleton` for stateless services**
- **Choose appropriate scopes for lifecycle awareness**
- **Design for testability from the beginning**
- **Follow consistent naming and organization patterns**

**KPRFlow Enterprise Dependency Injection is now optimized for production use!** 🚀

---

*This documentation is confidential and proprietary to KPRFlow Enterprise. Unauthorized distribution is prohibited.*
