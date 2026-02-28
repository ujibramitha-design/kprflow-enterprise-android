package com.kprflow.enterprise.ui.common

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Extension functions for consistent state management patterns
 */

/**
 * Collect UiState as Compose state
 */
@Composable
fun <T> UiState<T>.collectAsState(): State<T?> {
    return when (this) {
        is UiState.Success -> remember(this) { mutableStateOf(data) }
        else -> remember(this) { mutableStateOf(null) }
    }
}

/**
 * Collect UiState with loading state
 */
@Composable
fun <T> UiState<T>.collectWithLoadingState(): Pair<State<T?>, Boolean> {
    val dataState = collectAsState()
    val isLoading = remember(this) { this is UiState.Loading }
    return dataState to isLoading
}

/**
 * Safe collect with error handling
 */
@Composable
fun <T> UiState<T>.collectSafe(
    onSuccess: @Composable (T) -> Unit,
    onLoading: @Composable () -> Unit,
    onError: @Composable (String) -> Unit,
    onEmpty: @Composable () -> Unit = {}
) {
    when (this) {
        is UiState.Success -> onSuccess(data)
        is UiState.Loading -> onLoading()
        is UiState.Error -> onError(message)
        is UiState.Empty -> onEmpty()
    }
}

/**
 * Transform UiState to another type
 */
fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> {
    return when (this) {
        is UiState.Success -> UiState.Success(transform(data))
        is UiState.Loading -> UiState.Loading
        is UiState.Error -> UiState.Error(message, code)
        is UiState.Empty -> UiState.Empty
    }
}

/**
 * Flat map UiState for nested operations
 */
suspend fun <T, R> UiState<T>.flatMap(transform: suspend (T) -> UiState<R>): UiState<R> {
    return when (this) {
        is UiState.Success -> transform(data)
        is UiState.Loading -> UiState.Loading
        is UiState.Error -> UiState.Error(message, code)
        is UiState.Empty -> UiState.Empty
    }
}

/**
 * Combine multiple UiStates
 */
fun <T1, T2> combineUiStates(
    state1: UiState<T1>,
    state2: UiState<T2>
): UiState<Pair<T1, T2>> {
    return when {
        state1.isError() -> UiState.Error(state1.getErrorOrNull() ?: "Unknown error")
        state2.isError() -> UiState.Error(state2.getErrorOrNull() ?: "Unknown error")
        state1.isLoading() || state2.isLoading() -> UiState.Loading
        state1.isEmpty() || state2.isEmpty() -> UiState.Empty
        state1.isSuccess() && state2.isSuccess() -> {
            val data1 = state1.getDataOrNull()
            val data2 = state2.getDataOrNull()
            if (data1 != null && data2 != null) {
                UiState.Success(Pair(data1, data2))
            } else {
                UiState.Empty
            }
        }
        else -> UiState.Loading
    }
}

/**
 * Combine three UiStates
 */
fun <T1, T2, T3> combineUiStates(
    state1: UiState<T1>,
    state2: UiState<T2>,
    state3: UiState<T3>
): UiState<Triple<T1, T2, T3>> {
    return when {
        state1.isError() -> UiState.Error(state1.getErrorOrNull() ?: "Unknown error")
        state2.isError() -> UiState.Error(state2.getErrorOrNull() ?: "Unknown error")
        state3.isError() -> UiState.Error(state3.getErrorOrNull() ?: "Unknown error")
        state1.isLoading() || state2.isLoading() || state3.isLoading() -> UiState.Loading
        state1.isEmpty() || state2.isEmpty() || state3.isEmpty() -> UiState.Empty
        state1.isSuccess() && state2.isSuccess() && state3.isSuccess() -> {
            val data1 = state1.getDataOrNull()
            val data2 = state2.getDataOrNull()
            val data3 = state3.getDataOrNull()
            if (data1 != null && data2 != null && data3 != null) {
                UiState.Success(Triple(data1, data2, data3))
            } else {
                UiState.Empty
            }
        }
        else -> UiState.Loading
    }
}

/**
 * ViewModel extension for consistent state management
 */
fun <T> androidx.lifecycle.ViewModel.createUiStateFlow(
    initialValue: UiState<T> = UiState.Loading
): MutableStateFlow<UiState<T>> = MutableStateFlow(initialValue)

/**
 * Safe execution with error handling
 */
suspend fun <T> androidx.lifecycle.ViewModel.safeExecute(
    uiState: MutableStateFlow<UiState<T>>,
    loadingState: UiState<T> = UiState.Loading,
    block: suspend () -> T
) {
    try {
        uiState.value = loadingState
        val result = block()
        uiState.value = UiState.Success(result)
    } catch (e: Exception) {
        uiState.value = UiState.Error(e.message ?: "Unknown error")
    }
}

/**
 * Refresh with loading state
 */
suspend fun <T> androidx.lifecycle.ViewModel.refreshWithLoading(
    uiState: MutableStateFlow<UiState<T>>,
    block: suspend () -> T
) {
    safeExecute(uiState, uiState.value, block)
}

/**
 * Cache UiState results
 */
class UiStateCache<T> {
    private var cachedState: UiState<T>? = null
    private var cacheTime: Long = 0
    private val cacheDuration: Long = 5 * 60 * 1000 // 5 minutes
    
    fun get(): UiState<T>? {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - cacheTime < cacheDuration) {
            cachedState
        } else {
            null
        }
    }
    
    fun put(state: UiState<T>) {
        cachedState = state
        cacheTime = System.currentTimeMillis()
    }
    
    fun clear() {
        cachedState = null
        cacheTime = 0
    }
}

/**
 * Debounce UiState updates
 */
fun <T> Flow<UiState<T>>.debounceUiState(
    debounceTime: Long = 300L
): Flow<UiState<T>> {
    return debounce(debounceTime)
}

/**
 * Filter out duplicate success states
 */
fun <T> Flow<UiState<T>>.distinctUntilChangedUiState(): Flow<UiState<T>> {
    return distinctUntilChanged { old, new ->
        when {
            old is UiState.Success && new is UiState.Success -> old.data == new.data
            old is UiState.Error && new is UiState.Error -> old.message == new.message
            old::class == new::class -> true
            else -> false
        }
    }
}

/**
 * Retry failed operations
 */
suspend fun <T> retryUiStateOperation(
    maxRetries: Int = 3,
    delay: Long = 1000L,
    operation: suspend () -> T
): UiState<T> {
    repeat(maxRetries) { attempt ->
        try {
            val result = operation()
            return UiState.Success(result)
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) {
                return UiState.Error(e.message ?: "Operation failed after $maxRetries retries")
            }
            kotlinx.coroutines.delay(delay * (attempt + 1))
        }
    }
    return UiState.Error("Operation failed")
}

/**
 * Combine UiState with network state
 */
fun <T> UiState<T>.withNetworkState(networkState: NetworkState): UiState<T> {
    return when (networkState) {
        is NetworkState.Disconnected -> UiState.Error("No internet connection")
        is NetworkState.Error -> UiState.Error(networkState.message)
        else -> this
    }
}

/**
 * Paginated UiState helper
 */
fun <T> createPaginatedUiState(
    data: List<T> = emptyList(),
    isLoading: Boolean = false,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    currentPage: Int = 1,
    totalCount: Int = 0
): UiState<PaginationState<T>> {
    val paginationState = PaginationState(
        data = data,
        isLoading = isLoading,
        isLoadingMore = isLoadingMore,
        hasMore = hasMore,
        currentPage = currentPage,
        totalCount = totalCount
    )
    return UiState.Success(paginationState)
}

/**
 * Form validation helper
 */
fun <T> createFormUiState(
    data: T,
    isValid: Boolean = false,
    errors: Map<String, String> = emptyMap(),
    isDirty: Boolean = false,
    isSubmitting: Boolean = false
): UiState<FormState<T>> {
    val formState = FormState(
        data = data,
        isValid = isValid,
        errors = errors,
        isDirty = isDirty,
        isSubmitting = isSubmitting
    )
    return UiState.Success(formState)
}

/**
 * Search state helper
 */
fun <T> createSearchUiState(
    query: String = "",
    results: List<T> = emptyList(),
    filters: Map<String, Any> = emptyMap(),
    isLoading: Boolean = false,
    hasSearched: Boolean = false,
    totalCount: Int = 0
): UiState<SearchState<T>> {
    val searchState = SearchState(
        query = query,
        results = results,
        filters = filters,
        isLoading = isLoading,
        hasSearched = hasSearched,
        totalCount = totalCount
    )
    return UiState.Success(searchState)
}
