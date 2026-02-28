package com.kprflow.enterprise.ui.common

/**
 * Standardized UI State wrapper for consistent state management
 * across all dashboards and screens in KPRFlow Enterprise
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val code: String? = null) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}

/**
 * Extension functions for UiState operations
 */
fun <T> UiState<T>.isLoading(): Boolean = this is UiState.Loading
fun <T> UiState<T>.isSuccess(): Boolean = this is UiState.Success
fun <T> UiState<T>.isError(): Boolean = this is UiState.Error
fun <T> UiState<T>.isEmpty(): Boolean = this is UiState.Empty

fun <T> UiState<T>.getDataOrNull(): T? = when (this) {
    is UiState.Success -> data
    else -> null
}

fun <T> UiState<T>.getDataOrDefault(defaultValue: T): T = when (this) {
    is UiState.Success -> data
    else -> defaultValue
}

fun <T> UiState<T>.getErrorOrNull(): String? = when (this) {
    is UiState.Error -> message
    else -> null
}

/**
 * Real-time state wrapper for Supabase Realtime integration
 */
data class RealtimeState<T>(
    val data: T,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isRealtime: Boolean = true
)

/**
 * Pagination state for list data
 */
data class PaginationState<T>(
    val data: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    val pageSize: Int = 20,
    val totalCount: Int = 0,
    val error: String? = null
) {
    val isEmpty: Boolean get() = data.isEmpty() && !isLoading
    val isFirstPage: Boolean get() = currentPage == 1
    val isLastPage: Boolean get() = !hasMore || (totalCount > 0 && data.size >= totalCount)
}

/**
 * Form state for input validation
 */
data class FormState<T>(
    val data: T,
    val isValid: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val isDirty: Boolean = false,
    val isSubmitting: Boolean = false
)

/**
 * Search state with filters
 */
data class SearchState<T>(
    val query: String = "",
    val results: List<T> = emptyList(),
    val filters: Map<String, Any> = emptyMap(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val totalCount: Int = 0
)

/**
 * Refresh state for pull-to-refresh functionality
 */
data class RefreshState<T>(
    val data: T,
    val isRefreshing: Boolean = false,
    val lastRefresh: Long = System.currentTimeMillis(),
    val error: String? = null
)

/**
 * Network state for connectivity awareness
 */
sealed class NetworkState {
    object Connected : NetworkState()
    object Disconnected : NetworkState()
    object Connecting : NetworkState()
    data class Error(val message: String) : NetworkState()
}

/**
 * Cache state for offline-first architecture
 */
data class CacheState<T>(
    val data: T?,
    val isFromCache: Boolean = false,
    val lastUpdated: Long = 0,
    val isExpired: Boolean = false
)

/**
 * Combined state for complex UI scenarios
 */
data class CombinedUiState(
    val primary: UiState<*> = UiState.Loading,
    val secondary: UiState<*> = UiState.Loading,
    val network: NetworkState = NetworkState.Connected,
    val isRefreshing: Boolean = false
) {
    val isLoading: Boolean get() = primary.isLoading() || secondary.isLoading()
    val isError: Boolean get() = primary.isError() || secondary.isError()
    val isSuccess: Boolean get() = primary.isSuccess() && secondary.isSuccess()
}
