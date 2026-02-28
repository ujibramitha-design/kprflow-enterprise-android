package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.model.KprDossier
import com.kprflow.enterprise.domain.repository.IKprRepository
import com.kprflow.enterprise.ui.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StandardizedCustomerDashboardViewModel @Inject constructor(
    private val kprRepository: IKprRepository
) : ViewModel() {
    
    // Standardized UI State using common UiState wrapper
    private val _uiState = MutableStateFlow<UiState<List<KprDossier>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<KprDossier>>> = _uiState.asStateFlow()
    
    // Real-time state management
    private val realtimeManager = RealtimeCollectionManager(
        realtime = kprRepository.realtime,
        tableName = "kpr_dossiers",
        initialData = emptyList(),
        dataMapper = { data ->
            // Map Supabase data to KprDossier
            KprDossier(
                id = data["id"] as String,
                userId = data["user_id"] as String,
                unitId = data["unit_id"] as? String,
                status = com.kprflow.enterprise.data.model.KprStatus.valueOf(data["status"] as String),
                // ... other fields
            )
        },
        idExtractor = { it.id }
    )
    
    // Combined state for complex UI scenarios
    private val _combinedState = MutableStateFlow<CombinedUiState>(
        CombinedUiState(
            primary = UiState.Loading,
            secondary = UiState.Loading,
            network = NetworkState.Connected,
            isRefreshing = false
        )
    )
    val combinedState: StateFlow<CombinedUiState> = _combinedState.asStateFlow()
    
    // Pagination state for large datasets
    private val _paginationState = MutableStateFlow<PaginationState<KprDossier>>(
        PaginationState()
    )
    val paginationState: StateFlow<PaginationState<KprDossier>> = _paginationState.asStateFlow()
    
    // Cache for performance optimization
    private val cache = UiStateCache<List<KprDossier>>()
    
    init {
        // Start real-time listening
        viewModelScope.launch {
            realtimeManager.startListening()
            realtimeManager.uiState.collect { state ->
                _uiState.value = state
                updateCombinedState()
            }
        }
        
        // Load initial data
        loadCustomerDossiers()
    }
    
    /**
     * Load customer dossiers with standardized state management
     */
    private fun loadCustomerDossiers() {
        viewModelScope.launch {
            safeExecute(_uiState) {
                // Try to get from cache first
                cache.get()?.let { cachedState ->
                    return@safeExecute cachedState.getDataOrNull() ?: emptyList()
                }
                
                // Load from repository
                val dossiers = kprRepository.getDossiersByUserId("current_user_id").getOrNull()
                    ?: emptyList()
                
                // Cache the result
                cache.put(UiState.Success(dossiers))
                
                dossiers
            }
        }
    }
    
    /**
     * Refresh data with loading state
     */
    fun refresh() {
        viewModelScope.launch {
            _combinedState.value = _combinedState.value.copy(isRefreshing = true)
            cache.clear()
            
            refreshWithLoading(_uiState) {
                val dossiers = kprRepository.getDossiersByUserId("current_user_id").getOrNull()
                    ?: emptyList()
                cache.put(UiState.Success(dossiers))
                dossiers
            }
            
            _combinedState.value = _combinedState.value.copy(isRefreshing = false)
        }
    }
    
    /**
     * Load more data (pagination)
     */
    fun loadMore() {
        val currentState = _paginationState.value
        if (currentState.isLoadingMore || !currentState.hasMore) return
        
        viewModelScope.launch {
            val updatedState = currentState.copy(isLoadingMore = true)
            _paginationState.value = updatedState
            
            try {
                // Load next page
                val nextPage = currentState.currentPage + 1
                val newDossiers = kprRepository.getDossiersByUserId("current_user_id").getOrNull()
                    ?.drop((nextPage - 1) * currentState.pageSize)
                    ?.take(currentState.pageSize)
                    ?: emptyList()
                
                val updatedData = currentState.data + newDossiers
                val hasMore = newDossiers.size == currentState.pageSize
                
                _paginationState.value = currentState.copy(
                    data = updatedData,
                    isLoadingMore = false,
                    hasMore = hasMore,
                    currentPage = nextPage
                )
            } catch (e: Exception) {
                _paginationState.value = currentState.copy(isLoadingMore = false)
                _uiState.value = UiState.Error(e.message ?: "Failed to load more data")
            }
        }
    }
    
    /**
     * Update combined state
     */
    private fun updateCombinedState() {
        val primaryState = _uiState.value
        val secondaryState = realtimeManager.uiState.value
        
        _combinedState.value = CombinedUiState(
            primary = primaryState,
            secondary = secondaryState,
            network = NetworkState.Connected, // Would come from network monitor
            isRefreshing = _combinedState.value.isRefreshing
        )
    }
    
    /**
     * Handle real-time events
     */
    private fun handleRealtimeEvents() {
        viewModelScope.launch {
            realtimeManager.events.collect { event ->
                when (event) {
                    is RealtimeEvent.DataChanged<*> -> {
                        // Data changed, refresh if needed
                        refresh()
                    }
                    is RealtimeEvent.ConnectionStateChanged -> {
                        // Update network state
                        val networkState = if (event.isConnected) {
                            NetworkState.Connected
                        } else {
                            NetworkState.Disconnected
                        }
                        _combinedState.value = _combinedState.value.copy(network = networkState)
                    }
                    is RealtimeEvent.ErrorOccurred -> {
                        _uiState.value = UiState.Error(event.error)
                    }
                }
            }
        }
    }
    
    /**
     * Get current data safely
     */
    fun getCurrentDossiers(): List<KprDossier> {
        return _uiState.value.getDataOrNull() ?: emptyList()
    }
    
    /**
     * Check if data is from real-time update
     */
    fun isRealtimeData(): Boolean {
        return realtimeManager.isRealtime()
    }
    
    /**
     * Get loading state
     */
    fun isLoading(): Boolean {
        return _uiState.value.isLoading()
    }
    
    /**
     * Get error state
     */
    fun getError(): String? {
        return _uiState.value.getErrorOrNull()
    }
    
    /**
     * Retry failed operation
     */
    fun retry() {
        if (_uiState.value.isError()) {
            refresh()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up real-time connections
        viewModelScope.launch {
            realtimeManager.stopListening()
        }
    }
}

/**
 * Example of how to use the standardized ViewModel in Compose
 */
@Composable
fun StandardizedCustomerDashboard(
    viewModel: StandardizedCustomerDashboardViewModel = hiltViewModel(),
    onDossierClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val combinedState by viewModel.combinedState.collectAsState()
    val paginationState by viewModel.paginationState.collectAsState()
    
    // Handle different UI states with standardized pattern
    uiState.collectSafe(
        onSuccess = { dossiers ->
            CustomerDashboardContent(
                dossiers = dossiers,
                paginationState = paginationState,
                onDossierClick = onDossierClick,
                onLoadMore = { viewModel.loadMore() },
                onRefresh = { viewModel.refresh() }
            )
        },
        onLoading = {
            LoadingIndicator()
        },
        onError = { error ->
            ErrorState(
                message = error,
                onRetry = { viewModel.retry() }
            )
        },
        onEmpty = {
            EmptyState(
                message = "No KPR applications found",
                onRefresh = { viewModel.refresh() }
            )
        }
    )
    
    // Show network status if disconnected
    if (combinedState.network is NetworkState.Disconnected) {
        NetworkOfflineBanner()
    }
    
    // Show refresh indicator
    if (combinedState.isRefreshing) {
        RefreshIndicator()
    }
}
