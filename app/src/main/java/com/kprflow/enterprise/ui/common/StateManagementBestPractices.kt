package com.kprflow.enterprise.ui.common

/**
 * KPRFlow Enterprise State Management Best Practices
 * 
 * This document outlines the standardized patterns and conventions
 * for consistent state management across all dashboards and components.
 */

/**
 * BEST PRACTICES FOR STATE MANAGEMENT IN KPRFLOW ENTERPRISE
 * 
 * 1. ALWAYS USE STANDARDIZED UISTATE
 *    - Use UiState<T> from common package
 *    - Never create custom sealed classes for UI states
 *    - Use type aliases for specific screens if needed
 * 
 * 2. FLOW OVER LIST FOR REAL-TIME DATA
 *    - Use StateFlow/SharedFlow for Supabase Realtime
 *    - Implement RealtimeStateManager for consistent patterns
 *    - Always handle connection states and errors
 * 
 * 3. CONSISTENT ERROR HANDLING
 *    - Use UiState.Error with meaningful messages
 *    - Include error codes for debugging
 *    - Provide retry mechanisms for failed operations
 * 
 * 4. LOADING STATES
 *    - Show loading indicators for async operations
 *    - Use skeleton loaders for better UX
 *    - Distinguish between initial loading and refresh
 * 
 * 5. CACHE STRATEGIES
 *    - Implement UiStateCache for performance
 *    - Set appropriate cache durations
 *    - Clear cache on data updates
 * 
 * 6. PAGINATION
 *    - Use PaginationState for large datasets
 *    - Implement load more functionality
 *    - Handle empty and error states
 * 
 * 7. FORM VALIDATION
 *    - Use FormState for input validation
 *    - Provide real-time validation feedback
 *    - Handle submission states
 * 
 * 8. NETWORK AWARENESS
 *    - Monitor network connectivity
 *    - Show offline indicators
 *    - Implement offline-first strategies
 */

/**
 * EXAMPLE IMPLEMENTATIONS
 */

// ✅ CORRECT: Standardized UI State
class CorrectViewModel {
    private val _uiState = MutableStateFlow<UiState<List<Data>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Data>>> = _uiState.asStateFlow()
}

// ❌ INCORRECT: Custom sealed class
class IncorrectViewModel {
    private val _uiState = MutableStateFlow<CustomUiState>(CustomUiState.Loading)
    val uiState: StateFlow<CustomUiState> = _uiState.asStateFlow()
    
    sealed class CustomUiState { /* Don't do this */ }
}

// ✅ CORRECT: Real-time state management
class CorrectRealtimeViewModel {
    private val realtimeManager = RealtimeStateManager(
        realtime = supabase.realtime,
        tableName = "data",
        initialState = emptyList<Data>(),
        dataMapper = { mapToData(it) }
    )
    
    val uiState: StateFlow<UiState<List<Data>>> = realtimeManager.uiState
}

// ✅ CORRECT: Error handling with retry
fun ViewModel.loadDataWithRetry() {
    viewModelScope.launch {
        retryUiStateOperation(maxRetries = 3) {
            repository.getData()
        }.let { result ->
            _uiState.value = result
        }
    }
}

// ✅ CORRECT: Pagination implementation
class CorrectPaginationViewModel {
    private val _paginationState = MutableStateFlow(
        PaginationState<Data>(pageSize = 20)
    )
    val paginationState: StateFlow<PaginationState<Data>> = _paginationState.asStateFlow()
    
    fun loadMore() {
        val currentState = _paginationState.value
        if (!currentState.hasMore || currentState.isLoadingMore) return
        
        viewModelScope.launch {
            // Load next page logic
        }
    }
}

/**
 * STATE MANAGEMENT PATTERNS
 */

/**
 * Pattern 1: Simple Data Loading
 */
abstract class SimpleDataViewModel<T> : ViewModel() {
    protected abstract val repository: AnyRepository<T>
    
    private val _uiState = MutableStateFlow<UiState<T>>(UiState.Loading)
    val uiState: StateFlow<UiState<T>> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            safeExecute(_uiState) {
                repository.getData()
            }
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            refreshWithLoading(_uiState) {
                repository.getData()
            }
        }
    }
}

/**
 * Pattern 2: Real-time Data with Cache
 */
abstract class RealtimeDataViewModel<T> : ViewModel() {
    protected abstract val realtimeManager: RealtimeCollectionManager<T>
    protected abstract val cache: UiStateCache<T>
    
    val uiState: StateFlow<UiState<List<T>>> = realtimeManager.uiState
    
    init {
        startRealtimeListening()
    }
    
    private fun startRealtimeListening() {
        viewModelScope.launch {
            realtimeManager.startListening()
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            cache.clear()
            realtimeManager.refresh()
        }
    }
}

/**
 * Pattern 3: Form with Validation
 */
abstract class FormViewModel<T> : ViewModel() {
    protected abstract val initialFormData: T
    
    private val _formState = MutableStateFlow(
        createFormUiState(initialFormData)
    )
    val formState: StateFlow<UiState<FormState<T>>> = _formState.asStateFlow()
    
    protected fun updateFormData(data: T) {
        val currentState = _formState.value.getDataOrNull()
        currentState?.let { current ->
            val errors = validateFormData(data)
            val isValid = errors.isEmpty()
            
            _formState.value = createFormUiState(
                data = data,
                isValid = isValid,
                errors = errors,
                isDirty = true
            )
        }
    }
    
    protected abstract fun validateFormData(data: T): Map<String, String>
    
    fun submitForm() {
        val currentState = _formState.value.getDataOrNull()
        currentState?.let { current ->
            if (current.isValid && !current.isSubmitting) {
                viewModelScope.launch {
                    val submittingState = createFormUiState(
                        data = current.data,
                        isValid = current.isValid,
                        errors = current.errors,
                        isDirty = current.isDirty,
                        isSubmitting = true
                    )
                    _formState.value = submittingState
                    
                    try {
                        submitFormData(current.data)
                    } catch (e: Exception) {
                        val errorState = createFormUiState(
                            data = current.data,
                            isValid = false,
                            errors = mapOf("submit" to e.message ?: "Submission failed"),
                            isDirty = current.isDirty,
                            isSubmitting = false
                        )
                        _formState.value = errorState
                    }
                }
            }
        }
    }
    
    protected abstract suspend fun submitFormData(data: T)
}

/**
 * Pattern 4: Search with Filters
 */
abstract class SearchViewModel<T> : ViewModel() {
    protected abstract val repository: SearchRepository<T>
    
    private val _searchState = MutableStateFlow(
        createSearchUiState<T>()
    )
    val searchState: StateFlow<UiState<SearchState<T>>> = _searchState.asStateFlow()
    
    fun search(query: String, filters: Map<String, Any> = emptyMap()) {
        viewModelScope.launch {
            val searchingState = createSearchUiState<T>(
                query = query,
                filters = filters,
                isLoading = true,
                hasSearched = true
            )
            _searchState.value = searchingState
            
            try {
                val results = repository.search(query, filters)
                val successState = createSearchUiState(
                    query = query,
                    results = results,
                    filters = filters,
                    isLoading = false,
                    hasSearched = true,
                    totalCount = results.size
                )
                _searchState.value = successState
            } catch (e: Exception) {
                val errorState = UiState.Error<SearchState<T>>(e.message ?: "Search failed")
                _searchState.value = errorState
            }
        }
    }
}

/**
 * COMPOSE INTEGRATION PATTERNS
 */

/**
 * Pattern 1: Simple State Collection
 */
@Composable
fun <T> SimpleStateScreen(
    uiState: StateFlow<UiState<T>>,
    onSuccess: @Composable (T) -> Unit,
    onLoading: @Composable () -> Unit,
    onError: @Composable (String) -> Unit,
    onEmpty: @Composable () -> Unit = {}
) {
    val state by uiState.collectAsState()
    
    state.collectSafe(
        onSuccess = onSuccess,
        onLoading = onLoading,
        onError = onError,
        onEmpty = onEmpty
    )
}

/**
 * Pattern 2: Combined States
 */
@Composable
fun <T1, T2> CombinedStateScreen(
    state1: StateFlow<UiState<T1>>,
    state2: StateFlow<UiState<T2>>,
    onSuccess: @Composable (T1, T2) -> Unit,
    onLoading: @Composable () -> Unit,
    onError: @Composable (String) -> Unit
) {
    val state1Value by state1.collectAsState()
    val state2Value by state2.collectAsState()
    
    val combinedState = combineUiStates(state1Value, state2Value)
    
    combinedState.collectSafe(
        onSuccess = { (data1, data2) -> onSuccess(data1, data2) },
        onLoading = onLoading,
        onError = onError
    )
}

/**
 * Pattern 3: Real-time with Refresh
 */
@Composable
fun <T> RealtimeStateScreen(
    viewModel: RealtimeDataViewModel<T>,
    onSuccess: @Composable (T) -> Unit,
    onLoading: @Composable () -> Unit,
    onError: @Composable (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRealtime by remember { derivedStateOf { viewModel.isRealtime() } }
    
    Box {
        uiState.collectSafe(
            onSuccess = onSuccess,
            onLoading = onLoading,
            onError = onError
        )
        
        // Real-time indicator
        if (isRealtime) {
            RealtimeIndicator()
        }
        
        // Refresh button
        RefreshButton(
            onClick = { viewModel.refresh() },
            isLoading = uiState.isLoading()
        )
    }
}

/**
 * TESTING PATTERNS
 */

/**
 * Pattern 1: ViewModel Testing
 */
class ViewModelTest {
    fun testLoadingState() {
        // Given
        val viewModel = TestViewModel()
        
        // When
        // Trigger data loading
        
        // Then
        assert(viewModel.uiState.value.isLoading())
    }
    
    fun testSuccessState() {
        // Given
        val viewModel = TestViewModel()
        val testData = TestData()
        
        // When
        // Simulate successful data loading
        
        // Then
        assert(viewModel.uiState.value.isSuccess())
        assert(viewModel.uiState.value.getDataOrNull() == testData)
    }
    
    fun testErrorState() {
        // Given
        val viewModel = TestViewModel()
        val errorMessage = "Test error"
        
        // When
        // Simulate error
        
        // Then
        assert(viewModel.uiState.value.isError())
        assert(viewModel.uiState.value.getErrorOrNull() == errorMessage)
    }
}

/**
 * PERFORMANCE OPTIMIZATION
 */

/**
 * Pattern 1: State Debouncing
 */
fun <T> StateFlow<UiState<T>>.debounceUpdates(
    debounceTime: Long = 300L
): StateFlow<UiState<T>> {
    return this
        .debounceUiState(debounceTime)
        .distinctUntilChangedUiState()
        .stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )
}

/**
 * Pattern 2: Memoized State
 */
@Composable
fun <T> MemoizedState(
    state: StateFlow<UiState<T>>,
    key: Any? = null
): State<UiState<T>> {
    return remember(key) { state }.collectAsState()
}

/**
 * MIGRATION GUIDE
 */

/**
 * MIGRATING FROM CUSTOM SEALED CLASSES TO STANDARDIZED UISTATE
 * 
 * Step 1: Replace custom sealed class with UiState<T>
 * Step 2: Update type aliases if needed
 * Step 3: Update ViewModel implementations
 * Step 4: Update Compose UI code
 * Step 5: Add deprecation warnings for old classes
 * Step 6: Remove old classes after migration
 */

/**
 * BEFORE (Custom Implementation):
 */
sealed class OldUiState<T> {
    object Loading : OldUiState<Nothing>()
    data class Success<T>(val data: T) : OldUiState<T>()
    data class Error(val message: String) : OldUiState<Nothing>()
}

/**
 * AFTER (Standardized Implementation):
 */
// Use UiState<T> from common package
typealias NewUiState<T> = UiState<T>

/**
 * VIEWMODEL MIGRATION:
 */
// BEFORE:
class OldViewModel {
    private val _uiState = MutableStateFlow<OldUiState<Data>>(OldUiState.Loading)
    val uiState: StateFlow<OldUiState<Data>> = _uiState.asStateFlow()
}

// AFTER:
class NewViewModel {
    private val _uiState = MutableStateFlow<UiState<Data>>(UiState.Loading)
    val uiState: StateFlow<UiState<Data>> = _uiState.asStateFlow()
}

/**
 * COMPOSE MIGRATION:
 */
// BEFORE:
@Composable
fun OldScreen(viewModel: OldViewModel) {
    val state by viewModel.uiState.collectAsState()
    
    when (state) {
        is OldUiState.Loading -> LoadingIndicator()
        is OldUiState.Success -> Content(state.data)
        is OldUiState.Error -> ErrorMessage(state.message)
    }
}

// AFTER:
@Composable
fun NewScreen(viewModel: NewViewModel) {
    val state by viewModel.uiState.collectAsState()
    
    state.collectSafe(
        onLoading = { LoadingIndicator() },
        onSuccess = { data -> Content(data) },
        onError = { error -> ErrorMessage(error) }
    )
}
