package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.model.*
import com.kprflow.enterprise.domain.usecase.extension.*
import com.kprflow.enterprise.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExtensionViewModel @Inject constructor(
    private val requestExtensionUseCase: RequestExtensionUseCase,
    private val approveExtensionUseCase: ApproveExtensionUseCase,
    private val rejectExtensionUseCase: RejectExtensionUseCase,
    private val getExtensionValidationUseCase: GetExtensionValidationUseCase,
    private val getPendingExtensionsUseCase: GetPendingExtensionsUseCase,
    private val getExtensionHistoryUseCase: GetExtensionHistoryUseCase,
    private val getExtensionStatisticsUseCase: GetExtensionStatisticsUseCase,
    private val monitorExtensionChangesUseCase: MonitorExtensionChangesUseCase,
    private val monitorDossierExtensionsUseCase: MonitorDossierExtensionsUseCase
) : ViewModel() {
    
    // Extension Request Form State
    private val _extensionForm = MutableStateFlow<ExtensionRequestForm?>(null)
    val extensionForm: StateFlow<ExtensionRequestForm?> = _extensionForm.asStateFlow()
    
    // Validation State
    private val _validationState = MutableStateFlow<Resource<ExtensionValidation>>(Resource.Loading)
    val validationState: StateFlow<Resource<ExtensionValidation>> = _validationState.asStateFlow()
    
    // Pending Extensions State
    private val _pendingExtensions = MutableStateFlow<Resource<List<ExtensionRequest>>>(Resource.Loading)
    val pendingExtensions: StateFlow<Resource<List<ExtensionRequest>>> = _pendingExtensions.asStateFlow()
    
    // Extension History State
    private val _extensionHistory = MutableStateFlow<Resource<List<ExtensionRequest>>>(Resource.Loading)
    val extensionHistory: StateFlow<Resource<List<ExtensionRequest>>> = _extensionHistory.asStateFlow()
    
    // Statistics State
    private val _statistics = MutableStateFlow<Resource<ExtensionStatistics>>(Resource.Loading)
    val statistics: StateFlow<Resource<ExtensionStatistics>> = _statistics.asStateFlow()
    
    // Request State
    private val _requestState = MutableStateFlow<Resource<ExtensionRequest>>(Resource.Loading)
    val requestState: StateFlow<Resource<ExtensionRequest>> = _requestState.asStateFlow()
    
    // Approval State
    private val _approvalState = MutableStateFlow<Resource<Boolean>>(Resource.Loading)
    val approvalState: StateFlow<Resource<Boolean>> = _approvalState.asStateFlow()
    
    init {
        loadPendingExtensions()
        loadStatistics()
        startMonitoring()
    }
    
    // Form Management
    fun initializeExtensionForm(
        dossierId: String,
        customerName: String,
        currentDeadline: String,
        requestedBy: String
    ) {
        _extensionForm.value = ExtensionRequestForm(
            dossierId = dossierId,
            customerName = customerName,
            currentDeadline = currentDeadline,
            requestedBy = requestedBy
        )
        
        // Load validation for this dossier
        loadValidation(dossierId)
    }
    
    fun updateExtensionReason(reason: String) {
        _extensionForm.value?.let { form ->
            _extensionForm.value = form.copy(extensionReason = reason)
        }
    }
    
    fun updateExtensionDays(days: Int) {
        _extensionForm.value?.let { form ->
            _extensionForm.value = form.copy(extensionDays = days)
        }
    }
    
    // Validation
    private fun loadValidation(dossierId: String) {
        viewModelScope.launch {
            _validationState.value = Resource.Loading
            
            getExtensionValidationUseCase(dossierId)
                .onSuccess { validation ->
                    _validationState.value = Resource.Success(validation)
                }
                .onFailure { exception ->
                    _validationState.value = Resource.Error(
                        message = "Failed to validate extension: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    // Extension Request
    fun requestExtension() {
        val form = _extensionForm.value
        if (form == null) {
            _requestState.value = Resource.Error("Extension form not initialized")
            return
        }
        
        viewModelScope.launch {
            _requestState.value = Resource.Loading
            
            requestExtensionUseCase(form)
                .onSuccess { extension ->
                    _requestState.value = Resource.Success(extension)
                    // Refresh pending extensions
                    loadPendingExtensions()
                    // Clear form
                    _extensionForm.value = null
                }
                .onFailure { exception ->
                    _requestState.value = Resource.Error(
                        message = "Failed to request extension: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    // Extension Approval
    fun approveExtension(
        extensionId: String,
        approvedBy: String,
        approvalNotes: String? = null
    ) {
        viewModelScope.launch {
            _approvalState.value = Resource.Loading
            
            approveExtensionUseCase(extensionId, approvedBy, approvalNotes)
                .onSuccess { success ->
                    _approvalState.value = Resource.Success(success)
                    // Refresh pending extensions
                    loadPendingExtensions()
                }
                .onFailure { exception ->
                    _approvalState.value = Resource.Error(
                        message = "Failed to approve extension: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    // Extension Rejection
    fun rejectExtension(
        extensionId: String,
        rejectedBy: String,
        rejectionReason: String
    ) {
        viewModelScope.launch {
            _approvalState.value = Resource.Loading
            
            rejectExtensionUseCase(extensionId, rejectedBy, rejectionReason)
                .onSuccess { success ->
                    _approvalState.value = Resource.Success(success)
                    // Refresh pending extensions
                    loadPendingExtensions()
                }
                .onFailure { exception ->
                    _approvalState.value = Resource.Error(
                        message = "Failed to reject extension: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    // Data Loading
    fun loadPendingExtensions() {
        viewModelScope.launch {
            _pendingExtensions.value = Resource.Loading
            
            getPendingExtensionsUseCase()
                .onSuccess { extensions ->
                    _pendingExtensions.value = Resource.Success(extensions)
                }
                .onFailure { exception ->
                    _pendingExtensions.value = Resource.Error(
                        message = "Failed to load pending extensions: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    fun loadExtensionHistory(dossierId: String) {
        viewModelScope.launch {
            _extensionHistory.value = Resource.Loading
            
            getExtensionHistoryUseCase(dossierId)
                .onSuccess { history ->
                    _extensionHistory.value = Resource.Success(history)
                }
                .onFailure { exception ->
                    _extensionHistory.value = Resource.Error(
                        message = "Failed to load extension history: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    fun loadStatistics() {
        viewModelScope.launch {
            _statistics.value = Resource.Loading
            
            getExtensionStatisticsUseCase()
                .onSuccess { stats ->
                    _statistics.value = Resource.Success(stats)
                }
                .onFailure { exception ->
                    _statistics.value = Resource.Error(
                        message = "Failed to load statistics: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    // Real-time Monitoring
    private fun startMonitoring() {
        viewModelScope.launch {
            monitorExtensionChangesUseCase().collect { extensions ->
                _pendingExtensions.value = Resource.Success(extensions)
            }
        }
    }
    
    fun startMonitoringDossierExtensions(dossierId: String) {
        viewModelScope.launch {
            monitorDossierExtensionsUseCase(dossierId).collect { extensions ->
                _extensionHistory.value = Resource.Success(extensions)
            }
        }
    }
    
    // Form Validation
    fun validateForm(): List<String> {
        val form = _extensionForm.value ?: return listOf("Form not initialized")
        return form.getValidationErrors()
    }
    
    fun isFormValid(): Boolean {
        val form = _extensionForm.value ?: return false
        return form.isValid()
    }
    
    // State Management
    fun clearRequestState() {
        _requestState.value = Resource.Loading
    }
    
    fun clearApprovalState() {
        _approvalState.value = Resource.Loading
    }
    
    fun clearForm() {
        _extensionForm.value = null
        _validationState.value = Resource.Loading
    }
    
    fun refreshData() {
        loadPendingExtensions()
        loadStatistics()
        _extensionForm.value?.let { form ->
            loadValidation(form.dossierId)
        }
    }
}
