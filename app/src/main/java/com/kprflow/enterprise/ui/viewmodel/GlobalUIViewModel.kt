package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.domain.repository.NetworkRepository
import com.kprflow.enterprise.domain.repository.AuthRepository
import com.kprflow.enterprise.domain.repository.WhatsAppRepository
import com.kprflow.enterprise.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Global UI ViewModel - Centralized UI State Management
 * Phase Final: Global UI State Wrapper Implementation
 */
@HiltViewModel
class GlobalUIViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val authRepository: AuthRepository,
    private val whatsAppRepository: WhatsAppRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GlobalUIState())
    val uiState: StateFlow<GlobalUIState> = _uiState.asStateFlow()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connected())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    init {
        observeNetworkState()
        observeTokenExpiration()
        observePermissionErrors()
    }
    
    /**
     * Observe network state changes
     */
    private fun observeNetworkState() {
        viewModelScope.launch {
            networkRepository.networkState
                .collect { state ->
                    val connectionState = when {
                        state.isConnected -> {
                            ConnectionState.Connected(
                                type = when {
                                    state.isWifi -> ConnectionType.WIFI
                                    state.isMobile -> ConnectionType.MOBILE
                                    else -> ConnectionType.ETHERNET
                                },
                                strength = state.signalStrength
                            )
                        }
                        state.isConnecting -> {
                            ConnectionState.Connecting()
                        }
                        else -> {
                            ConnectionState.Disconnected("No internet connection")
                        }
                    }
                    
                    _connectionState.value = connectionState
                    _uiState.update { it.copy(connectionState = connectionState) }
                    
                    // Handle reconnection
                    if (state.isConnected && _uiState.value.snackbarState.title == "Koneksi Internet Hilang") {
                        handleReconnection()
                    }
                }
        }
    }
    
    /**
     * Observe token expiration
     */
    private fun observeTokenExpiration() {
        viewModelScope.launch {
            authRepository.tokenExpiration
                .collect { isExpired ->
                    if (isExpired) {
                        showTokenExpiredError()
                    }
                }
        }
    }
    
    /**
     * Observe permission errors
     */
    private fun observePermissionErrors() {
        viewModelScope.launch {
            authRepository.permissionErrors
                .collect { error ->
                    if (error != null) {
                        showPermissionDeniedError(error)
                    }
                }
        }
    }
    
    /**
     * Show snackbar message
     */
    fun showSnackbar(
        type: SnackbarType,
        title: String,
        message: String,
        actions: List<SnackbarAction> = emptyList(),
        showProgress: Boolean = false,
        autoDismiss: Boolean = true
    ) {
        _uiState.update {
            it.copy(
                snackbarState = SnackbarState(
                    isVisible = true,
                    type = type,
                    title = title,
                    message = message,
                    actions = actions,
                    showProgress = showProgress,
                    autoDismiss = autoDismiss
                )
            )
        }
        
        // Auto dismiss if enabled
        if (autoDismiss) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(4000)
                dismissSnackbar()
            }
        }
    }
    
    /**
     * Show loading state
     */
    fun showLoading(message: String = "Memuat...") {
        _uiState.update {
            it.copy(
                isLoading = true,
                loadingMessage = message
            )
        }
    }
    
    /**
     * Hide loading state
     */
    fun hideLoading() {
        _uiState.update {
            it.copy(
                isLoading = false
            )
        }
    }
    
    /**
     * Dismiss snackbar
     */
    fun dismissSnackbar() {
        _uiState.update {
            it.copy(
                snackbarState = it.snackbarState.copy(isVisible = false)
            )
        }
    }
    
    /**
     * Handle snackbar action
     */
    fun handleSnackbarAction(actionId: String) {
        when (actionId) {
            "retry" -> {
                handleRetryAction()
            }
            "refresh_token" -> {
                refreshWhatsAppToken()
            }
            "contact_admin" -> {
                contactAdministrator()
            }
            "offline" -> {
                enableOfflineMode()
            }
            else -> {
                // Handle custom actions
                handleCustomAction(actionId)
            }
        }
    }
    
    /**
     * Refresh connection
     */
    fun refreshConnection() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectionState = ConnectionState.Connecting(),
                    snackbarState = ErrorStates.InternetDisconnected.copy(
                        showProgress = true,
                        progress = 0f
                    )
                )
            }
            
            // Simulate connection attempt
            repeat(10) { attempt ->
                kotlinx.coroutines.delay(500)
                _uiState.update {
                    it.copy(
                        snackbarState = it.snackbarState.copy(
                            progress = (attempt + 1) * 0.1f
                        )
                    )
                }
            }
            
            // Check actual connection
            val isConnected = networkRepository.checkConnection()
            if (isConnected) {
                handleReconnection()
            } else {
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.Disconnected(),
                        snackbarState = ErrorStates.InternetDisconnected.copy(
                            showProgress = false,
                            progress = 0f
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Handle reconnection
     */
    private fun handleReconnection() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectionState = ConnectionState.Connected(),
                    snackbarState = SnackbarState(
                        isVisible = true,
                        type = SnackbarType.SUCCESS,
                        title = "Koneksi Pulih",
                        message = "Upload otomatis akan dilanjutkan",
                        autoDismiss = true
                    )
                )
            }
            
            // Resume pending uploads
            resumePendingUploads()
        }
    }
    
    /**
     * Resume pending uploads
     */
    private fun resumePendingUploads() {
        viewModelScope.launch {
            showLoading("Melanjutkan upload yang tertunda...")
            
            try {
                // Resume pending uploads
                val pendingCount = networkRepository.resumePendingUploads()
                
                hideLoading()
                
                showSnackbar(
                    type = SnackbarType.SUCCESS,
                    title = "Upload Dilanjutkan",
                    message = "$pendingCount file berhasil diupload",
                    autoDismiss = true
                )
                
            } catch (e: Exception) {
                hideLoading()
                
                showSnackbar(
                    type = SnackbarType.ERROR,
                    title = "Upload Gagal",
                    message = e.message ?: "Terjadi kesalahan saat melanjutkan upload",
                    actions = listOf(
                        SnackbarAction("retry", "Coba Lagi")
                    ),
                    autoDismiss = false
                )
            }
        }
    }
    
    /**
     * Show token expired error
     */
    private fun showTokenExpiredError() {
        _uiState.update {
            it.copy(
                snackbarState = ErrorStates.WhatsAppTokenExpired
            )
        }
    }
    
    /**
     * Refresh WhatsApp token
     */
    private fun refreshWhatsAppToken() {
        viewModelScope.launch {
            showLoading("Refresh token WhatsApp...")
            
            try {
                val result = whatsAppRepository.refreshToken()
                
                hideLoading()
                
                if (result.isSuccess) {
                    showSnackbar(
                        type = SnackbarType.SUCCESS,
                        title = "Token Berhasil Diperbarui",
                        message = "Token WhatsApp Gateway telah diperbarui",
                        autoDismiss = true
                    )
                } else {
                    showSnackbar(
                        type = SnackbarType.ERROR,
                        title = "Refresh Token Gagal",
                        message = result.exceptionOrNull()?.message ?: "Terjadi kesalahan",
                        actions = listOf(
                            SnackbarAction("retry", "Coba Lagi")
                        ),
                        autoDismiss = false
                    )
                }
                
            } catch (e: Exception) {
                hideLoading()
                
                showSnackbar(
                    type = SnackbarType.ERROR,
                    title = "Refresh Token Gagal",
                    message = e.message ?: "Terjadi kesalahan",
                    actions = listOf(
                        SnackbarAction("retry", "Coba Lagi")
                    ),
                    autoDismiss = false
                )
            }
        }
    }
    
    /**
     * Show permission denied error
     */
    private fun showPermissionDeniedError(error: String) {
        _uiState.update {
            it.copy(
                snackbarState = ErrorStates.PermissionDenied.copy(
                    message = error
                )
            )
        }
    }
    
    /**
     * Contact administrator
     */
    private fun contactAdministrator() {
        viewModelScope.launch {
            showSnackbar(
                type = SnackbarType.INFO,
                title = "Menghubungi Administrator",
                message = "Permintaan bantuan telah dikirim ke administrator",
                autoDismiss = true
            )
            
            // Send notification to admin
            try {
                authRepository.notifyAdministrator(
                    message = "User meminta bantuan untuk permission denied error"
                )
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    /**
     * Enable offline mode
     */
    private fun enableOfflineMode() {
        viewModelScope.launch {
            showLoading("Mengaktifkan mode offline...")
            
            try {
                networkRepository.enableOfflineMode()
                
                hideLoading()
                
                showSnackbar(
                    type = SnackbarType.SUCCESS,
                    title = "Mode Offline Diaktifkan",
                    message = "Aplikasi akan berjalan dalam mode offline",
                    autoDismiss = true
                )
                
            } catch (e: Exception) {
                hideLoading()
                
                showSnackbar(
                    type = SnackbarType.ERROR,
                    title = "Mode Offline Gagal",
                    message = e.message ?: "Terjadi kesalahan",
                    autoDismiss = false
                )
            }
        }
    }
    
    /**
     * Handle retry action
     */
    private fun handleRetryAction() {
        when (_uiState.value.snackbarState.title) {
            "Koneksi Internet Hilang" -> {
                refreshConnection()
            }
            "Token WhatsApp Expired" -> {
                refreshWhatsAppToken()
            }
            else -> {
                // Generic retry
                refreshConnection()
            }
        }
    }
    
    /**
     * Handle custom action
     */
    private fun handleCustomAction(actionId: String) {
        // Handle custom actions based on action ID
        viewModelScope.launch {
            try {
                when (actionId) {
                    "upload_photo" -> {
                        // Retry photo upload
                        networkRepository.retryFailedUpload("photo")
                    }
                    "sync_data" -> {
                        // Sync data
                        networkRepository.syncData()
                    }
                    else -> {
                        // Handle other custom actions
                    }
                }
            } catch (e: Exception) {
                showSnackbar(
                    type = SnackbarType.ERROR,
                    title = "Aksi Gagal",
                    message = e.message ?: "Terjadi kesalahan",
                    autoDismiss = false
                )
            }
        }
    }
    
    /**
     * Handle critical error
     */
    fun handleCriticalError(
        title: String,
        message: String,
        errorCode: String? = null
    ) {
        _uiState.update {
            it.copy(
                errorState = ErrorState.Critical(
                    title = title,
                    message = message,
                    errorCode = errorCode
                )
            )
        }
    }
    
    /**
     * Clear error state
     */
    fun clearErrorState() {
        _uiState.update {
            it.copy(
                errorState = ErrorState.None
            )
        }
    }
    
    /**
     * Handle network error
     */
    fun handleNetworkError(error: String) {
        _uiState.update {
            it.copy(
                snackbarState = SnackbarState(
                    isVisible = true,
                    type = SnackbarType.ERROR,
                    title = "Error Jaringan",
                    message = error,
                    actions = listOf(
                        SnackbarAction("retry", "Coba Lagi")
                    ),
                    autoDismiss = false
                )
            )
        }
    }
    
    /**
     * Handle success message
     */
    fun handleSuccess(
        title: String,
        message: String
    ) {
        showSnackbar(
            type = SnackbarType.SUCCESS,
            title = title,
            message = message,
            autoDismiss = true
        )
    }
    
    /**
     * Handle warning message
     */
    fun handleWarning(
        title: String,
        message: String,
        actions: List<SnackbarAction> = emptyList()
    ) {
        showSnackbar(
            type = SnackbarType.WARNING,
            title = title,
            message = message,
            actions = actions,
            autoDismiss = false
        )
    }
}
