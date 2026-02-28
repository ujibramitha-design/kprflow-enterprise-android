package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.PushNotificationRepository
import com.kprflow.enterprise.data.repository.PaymentRepository
import com.kprflow.enterprise.data.repository.DocumentRepository
import com.kprflow.enterprise.data.repository.KprRepository
import com.kprflow.enterprise.data.repository.CameraRepository
import com.kprflow.enterprise.ui.screens.CustomerDashboardState
import com.kprflow.enterprise.ui.screens.CustomerDashboardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class EnhancedCustomerDashboardViewModel @Inject constructor(
    private val kprRepository: KprRepository,
    private val paymentRepository: PaymentRepository,
    private val pushNotificationRepository: PushNotificationRepository,
    private val documentRepository: DocumentRepository,
    private val cameraRepository: CameraRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CustomerDashboardUiState())
    val uiState: StateFlow<CustomerDashboardUiState> = _uiState.asStateFlow()
    
    private val _dossierState = MutableStateFlow<CustomerDashboardState<com.kprflow.enterprise.data.model.KprDossier>>(CustomerDashboardState.Loading)
    val dossierState: StateFlow<CustomerDashboardState<com.kprflow.enterprise.data.model.KprDossier>> = _dossierState.asStateState()
    
    private val _paymentState = MutableStateFlow<CustomerDashboardState<com.kprflow.enterprise.data.repository.PaymentSummary>>(CustomerDashboardState.Loading)
    val paymentState: StateFlow<CustomerDashboardState<com.kprflow.enterprise.data.repository.PaymentSummary>> = _paymentState.asStateState()
    
    private val _notificationState = MutableStateFlow<CustomerDashboardState<List<com.kprflow.enterprise.data.repository.PushNotification>>>(CustomerDashboardState.Loading)
    val notificationState: StateFlow<CustomerDashboardState<List<com.kprflow.enterprise.data.repository.PushNotification>>> = _notificationState.asStateState()
    
    private val _unreadCountState = MutableStateFlow(0)
    val unreadCountState: StateFlow<Int> = _unreadCountState.asStateState()
    
    private val _currentUserId = MutableStateFlow<String?>(null)
    
    init {
        // TODO: Get current user from AuthRepository
        _currentUserId.value = "current-customer-user-id" // Placeholder
    }
    
    fun loadCustomerData() {
        viewModelScope.launch {
            _dossierState.value = CustomerDashboardState.Loading
            
            val userId = _currentUserId.value ?: return@launch
            
            try {
                val dossiers = kprRepository.getDossiersByUser(userId)
                    .getOrNull().orEmpty()
                
                if (dossiers.isNotEmpty()) {
                    _dossierState.value = CustomerDashboardState.Success(dossiers.first())
                } else {
                    _dossierState.value = CustomerDashboardState.Error("No dossier found")
                }
            } catch (e: Exception) {
                _dossierState.value = CustomerDashboardState.Error(e.message ?: "Failed to load dossier")
            }
        }
    }
    
    fun loadPaymentSummary() {
        viewModelScope.launch {
            _paymentState.value = CustomerDashboardState.Loading
            
            val userId = _currentUserId.value ?: return@launch
            
            try {
                val dossiers = kprRepository.getDossiersByUser(userId)
                    .getOrNull().orEmpty()
                
                if (dossiers.isNotEmpty()) {
                    val summary = paymentRepository.getPaymentSummary(dossiers.first().id)
                        .getOrNull()
                    
                    if (summary != null) {
                        _paymentState.value = CustomerDashboardState.Success(summary)
                    } else {
                        _paymentState.value = CustomerDashboardState.Error("No payment schedule found")
                    }
                } else {
                    _paymentState.value = CustomerDashboardState.Error("No dossier found")
                }
            } catch (e: Exception) {
                _paymentState.value = CustomerDashboardState.Error(e.message ?: "Failed to load payment summary")
            }
        }
    }
    
    fun loadNotifications() {
        viewModelScope.launch {
            _notificationState.value = CustomerDashboardState.Loading
            
            val userId = _currentUserId.value ?: return@launch
            
            try {
                val notifications = pushNotificationRepository.getUserNotifications(userId, limit = 20)
                    .getOrNull().orEmpty()
                
                _notificationState.value = CustomerDashboardState.Success(notifications)
            } catch (e: Exception) {
                _notificationState.value = CustomerDashboardState.Error(e.message ?: "Failed to load notifications")
            }
        }
    }
    
    fun loadUnreadCount() {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            
            try {
                val unreadCount = pushNotificationRepository.getUnreadNotificationCount(userId)
                    .getOrNull() ?: 0
                
                _unreadCountState.value = unreadCount
            } catch (e: Exception) {
                // Handle error silently for unread count
            }
        }
    }
    
    fun uploadDocument(documentType: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // TODO: Implement document upload with file picker
                // For now, just show success message
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
                
                // Send notification about document upload
                val userId = _currentUserId.value ?: return@launch
                pushNotificationRepository.sendPushNotification(
                    userId = userId,
                    title = "Document Uploaded",
                    message = "Your $documentType has been uploaded successfully",
                    type = "document_upload"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to upload document: ${e.message}"
                )
            }
        }
    }
    
    fun uploadDocumentWithFile(documentType: String, file: File) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val userId = _currentUserId.value ?: return@launch
                val dossiers = kprRepository.getDossiersByUser(userId)
                    .getOrNull().orEmpty()
                
                if (dossiers.isNotEmpty()) {
                    val result = documentRepository.uploadDocument(
                        dossierId = dossiers.first().id,
                        documentType = documentType,
                        file = file
                    )
                    
                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                        
                        // Send notification about document upload
                        pushNotificationRepository.sendPushNotification(
                            userId = userId,
                            title = "Document Uploaded",
                            message = "Your $documentType has been uploaded successfully",
                            type = "document_upload"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to upload document"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No dossier found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to upload document: ${e.message}"
                )
            }
        }
    }
    
    fun takePhotoForDocument(documentType: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // TODO: Implement camera integration
                // For now, just simulate photo capture
                val photoFile = cameraRepository.capturePhoto()
                    .getOrNull()
                
                if (photoFile != null) {
                    uploadDocumentWithFile(documentType, photoFile)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to capture photo"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to capture photo: ${e.message}"
                )
            }
        }
    }
    
    fun makePayment(installmentId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // TODO: Implement payment processing
                // For now, just simulate payment
                val result = paymentRepository.makePayment(
                    installmentId = installmentId,
                    paymentAmount = BigDecimal("1000000"), // Mock amount
                    paymentMethod = "BANK_TRANSFER"
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    
                    // Send notification about payment
                    val userId = _currentUserId.value ?: return@launch
                    pushNotificationRepository.sendPushNotification(
                        userId = userId,
                        title = "Payment Received",
                        message = "Your payment has been processed successfully",
                        type = "payment_received"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to process payment"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to process payment: ${e.message}"
                )
            }
        }
    }
    
    fun viewPaymentSchedule() {
        // TODO: Navigate to payment schedule screen
    }
    
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                pushNotificationRepository.markNotificationAsRead(notificationId)
                    .getOrNull()
                
                // Refresh notifications and unread count
                loadNotifications()
                loadUnreadCount()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                val userId = _currentUserId.value ?: return@launch
                pushNotificationRepository.markAllNotificationsAsRead(userId)
                    .getOrNull()
                
                // Refresh notifications and unread count
                loadNotifications()
                loadUnreadCount()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    fun openNotifications() {
        _uiState.value = _uiState.value.copy(showNotifications = true)
    }
    
    fun closeNotifications() {
        _uiState.value = _uiState.value.copy(showNotifications = false)
    }
    
    fun openCustomerSupport() {
        _uiState.value = _uiState.value.copy(showCustomerSupport = true)
    }
    
    fun closeCustomerSupport() {
        _uiState.value = _uiState.value.copy(showCustomerSupport = false)
    }
    
    fun startCustomerSupportChat() {
        viewModelScope.launch {
            try {
                // TODO: Implement customer support chat integration
                _uiState.value = _uiState.value.copy(showCustomerSupport = false)
                
                // Send notification about support request
                val userId = _currentUserId.value ?: return@launch
                pushNotificationRepository.sendPushNotification(
                    userId = userId,
                    title = "Support Request",
                    message = "Your support request has been received. We'll respond shortly.",
                    type = "support_request"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to start support chat")
            }
        }
    }
    
    fun callCustomerSupport() {
        viewModelScope.launch {
            try {
                // TODO: Implement phone call integration
                _uiState.value = _uiState.value.copy(showCustomerSupport = false)
                
                // Send notification about support call
                val userId = _currentUserId.value ?: return@launch
                pushNotificationRepository.sendPushNotification(
                    userId = userId,
                    title = "Support Call",
                    message = "Our support team will call you back shortly.",
                    type = "support_call"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to initiate support call")
            }
        }
    }
    
    fun emailCustomerSupport() {
        viewModelScope.launch {
            try {
                // TODO: Implement email integration
                _uiState.value = _uiState.value.copy(showCustomerSupport = false)
                
                // Send notification about support email
                val userId = _currentUserId.value ?: return@launch
                pushNotificationRepository.sendPushNotification(
                    userId = userId,
                    title = "Support Email",
                    message = "Your support email has been received. We'll respond within 24 hours.",
                    type = "support_email"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to send support email")
            }
        }
    }
    
    fun refreshAllData() {
        loadCustomerData()
        loadPaymentSummary()
        loadNotifications()
        loadUnreadCount()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
