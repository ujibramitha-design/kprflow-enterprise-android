package com.kprflow.enterprise.ui.common

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.serialization.json.Json

/**
 * Centralized Realtime State Manager for consistent real-time data handling
 * across all dashboards and components in KPRFlow Enterprise
 */
class RealtimeStateManager<T>(
    private val realtime: Realtime,
    private val tableName: String,
    private val initialState: T,
    private val dataMapper: (Map<String, Any>) -> T
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<RealtimeState<T>>(RealtimeState(initialState))
    val state: StateFlow<RealtimeState<T>> = _state.asStateFlow()
    
    private val _uiState = MutableStateFlow<UiState<T>>(UiState.Loading)
    val uiState: StateFlow<UiState<T>> = _uiState.asStateFlow()
    
    private var channel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var isConnected = false
    
    /**
     * Start listening to real-time updates
     */
    suspend fun startListening() {
        mutex.withLock {
            if (!isConnected) {
                try {
                    channel = realtime.channel(tableName)
                    
                    channel?.onPostgresChange("postgres_changes", schema = "public", table = tableName) { change ->
                        handleRealtimeChange(change)
                    }
                    
                    channel?.connect()
                    isConnected = true
                    
                    // Load initial data
                    loadInitialData()
                } catch (e: Exception) {
                    _uiState.value = UiState.Error("Failed to connect to realtime: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Stop listening to real-time updates
     */
    suspend fun stopListening() {
        mutex.withLock {
            channel?.let {
                it.unsubscribe()
                it.disconnect()
            }
            channel = null
            isConnected = false
        }
    }
    
    /**
     * Handle real-time data changes
     */
    private suspend fun handleRealtimeChange(change: io.github.jan.supabase.realtime.RealtimePostgresChange) {
        try {
            when (change.eventType) {
                "INSERT", "UPDATE" -> {
                    val record = change.record as? Map<String, Any>
                    record?.let { data ->
                        val newData = dataMapper(data)
                        updateState(newData)
                    }
                }
                "DELETE" -> {
                    // Handle deletion if needed
                    _uiState.value = UiState.Empty
                }
            }
        } catch (e: Exception) {
            _uiState.value = UiState.Error("Failed to process realtime update: ${e.message}")
        }
    }
    
    /**
     * Update state with new data
     */
    private suspend fun updateState(newData: T) {
        mutex.withLock {
            val currentTime = System.currentTimeMillis()
            _state.value = RealtimeState(newData, currentTime, true)
            _uiState.value = UiState.Success(newData)
        }
    }
    
    /**
     * Load initial data
     */
    private suspend fun loadInitialData() {
        try {
            // This would typically load from repository
            // For now, we'll use the initial state
            _uiState.value = UiState.Success(initialState)
        } catch (e: Exception) {
            _uiState.value = UiState.Error("Failed to load initial data: ${e.message}")
        }
    }
    
    /**
     * Force refresh data
     */
    suspend fun refresh() {
        loadInitialData()
    }
    
    /**
     * Get current data
     */
    fun getCurrentData(): T? = _state.value.data
    
    /**
     * Check if data is realtime
     */
    fun isRealtime(): Boolean = _state.value.isRealtime
}

/**
 * Factory for creating RealtimeStateManager instances
 */
class RealtimeStateManagerFactory {
    companion object {
        fun <T> create(
            realtime: Realtime,
            tableName: String,
            initialState: T,
            dataMapper: (Map<String, Any>) -> T
        ): RealtimeStateManager<T> {
            return RealtimeStateManager(realtime, tableName, initialState, dataMapper)
        }
    }
}

/**
 * Base ViewModel with integrated realtime state management
 */
abstract class BaseRealtimeViewModel<T>(
    private val realtimeStateManager: RealtimeStateManager<T>
) {
    protected val uiState: StateFlow<UiState<T>> = realtimeStateManager.uiState
    protected val realtimeState: StateFlow<RealtimeState<T>> = realtimeStateManager.state
    
    init {
        // Start listening when ViewModel is created
        startRealtimeListening()
    }
    
    private fun startRealtimeListening() {
        // This would be called in a coroutine scope
        // Implementation depends on specific ViewModel setup
    }
    
    /**
     * Refresh data
     */
    suspend fun refresh() {
        realtimeStateManager.refresh()
    }
    
    /**
     * Get current data
     */
    protected fun getCurrentData(): T? = realtimeStateManager.getCurrentData()
    
    /**
     * Check if data is realtime
     */
    protected fun isRealtime(): Boolean = realtimeStateManager.isRealtime()
}

/**
 * Real-time collection manager for list data
 */
class RealtimeCollectionManager<T>(
    private val realtime: Realtime,
    private val tableName: String,
    private val initialData: List<T> = emptyList(),
    private val dataMapper: (Map<String, Any>) -> T,
    private val idExtractor: (T) -> String
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<RealtimeState<List<T>>>(RealtimeState(initialData))
    val state: StateFlow<RealtimeState<List<T>>> = _state.asStateFlow()
    
    private val _uiState = MutableStateFlow<UiState<List<T>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<T>>> = _uiState.asStateFlow()
    
    private var channel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var isConnected = false
    
    /**
     * Start listening to collection changes
     */
    suspend fun startListening() {
        mutex.withLock {
            if (!isConnected) {
                try {
                    channel = realtime.channel(tableName)
                    
                    channel?.onPostgresChange("postgres_changes", schema = "public", table = tableName) { change ->
                        handleCollectionChange(change)
                    }
                    
                    channel?.connect()
                    isConnected = true
                    
                    _uiState.value = UiState.Success(initialData)
                } catch (e: Exception) {
                    _uiState.value = UiState.Error("Failed to connect to collection: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Handle collection changes
     */
    private suspend fun handleCollectionChange(change: io.github.jan.supabase.realtime.RealtimePostgresChange) {
        try {
            val currentData = _state.value.data.toMutableList()
            
            when (change.eventType) {
                "INSERT" -> {
                    val record = change.record as? Map<String, Any>
                    record?.let { data ->
                        val newItem = dataMapper(data)
                        currentData.add(newItem)
                        updateCollection(currentData)
                    }
                }
                "UPDATE" -> {
                    val record = change.record as? Map<String, Any>
                    record?.let { data ->
                        val updatedItem = dataMapper(data)
                        val itemId = idExtractor(updatedItem)
                        val index = currentData.indexOfFirst { idExtractor(it) == itemId }
                        if (index >= 0) {
                            currentData[index] = updatedItem
                            updateCollection(currentData)
                        }
                    }
                }
                "DELETE" -> {
                    val oldRecord = change.oldRecord as? Map<String, Any>
                    oldRecord?.let { data ->
                        val deletedItem = dataMapper(data)
                        val itemId = idExtractor(deletedItem)
                        currentData.removeAll { idExtractor(it) == itemId }
                        updateCollection(currentData)
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.value = UiState.Error("Failed to process collection change: ${e.message}")
        }
    }
    
    /**
     * Update collection state
     */
    private suspend fun updateCollection(newData: List<T>) {
        mutex.withLock {
            val currentTime = System.currentTimeMillis()
            _state.value = RealtimeState(newData, currentTime, true)
            _uiState.value = UiState.Success(newData)
        }
    }
    
    /**
     * Stop listening
     */
    suspend fun stopListening() {
        mutex.withLock {
            channel?.let {
                it.unsubscribe()
                it.disconnect()
            }
            channel = null
            isConnected = false
        }
    }
}

/**
 * Real-time event manager for cross-component communication
 */
class RealtimeEventManager {
    private val _events = MutableSharedFlow<RealtimeEvent>()
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()
    
    /**
     * Emit event
     */
    suspend fun emitEvent(event: RealtimeEvent) {
        _events.emit(event)
    }
    
    /**
     * Listen to events
     */
    fun listenToEvents(): Flow<RealtimeEvent> = events
}

/**
 * Real-time event types
 */
sealed class RealtimeEvent {
    data class DataChanged<T>(val data: T, val source: String) : RealtimeEvent()
    data class StatusChanged(val status: String, val timestamp: Long) : RealtimeEvent()
    data class ErrorOccurred(val error: String, val source: String) : RealtimeEvent()
    data class ConnectionStateChanged(val isConnected: Boolean) : RealtimeEvent()
}
