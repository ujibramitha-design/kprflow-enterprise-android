package com.kprflow.enterprise.legal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.legal.model.*
import com.kprflow.enterprise.legal.service.PPJBDeveloperService
import com.kprflow.enterprise.legal.scheduler.PPJBReminderScheduler
import com.kprflow.enterprise.legal.request.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PPJB Developer ViewModel - Refactored for Clean Architecture
 * Phase 16: Legal & Documentation Automation
 */
@HiltViewModel
class PPJBDeveloperViewModel @Inject constructor(
    private val ppjbService: PPJBDeveloperService,
    private val reminderScheduler: PPJBReminderScheduler
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PPJBUIState())
    val uiState: StateFlow<PPJBUIState> = _uiState.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow(PPJBFilter.ALL)
    val selectedFilter: StateFlow<PPJBFilter> = _selectedFilter.asStateFlow()
    
    init {
        loadPPJBProcesses()
        observeFilterChanges()
    }
    
    /**
     * Load PPJB processes
     */
    fun loadPPJBProcesses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            ppjbService.getPPJBProcesses(_selectedFilter.value)
                .catch { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Unknown error occurred"
                        )
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { processes ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    ppjbProcesses = processes,
                                    error = null
                                )
                            }
                        },
                        onFailure = { exception ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = exception.message ?: "Unknown error occurred"
                                )
                            }
                        }
                    )
                }
        }
    }
    
    /**
     * Refresh data
     */
    fun refreshData() {
        loadPPJBProcesses()
    }
    
    /**
     * Create new PPJB process
     */
    fun createPPJBProcess() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Create dummy request for demo
            val request = CreatePPJBRequest(
                dossierId = "550e8400-e29b-41d4-a716-446655440000",
                scheduledDate = null
            )
            
            ppjbService.createPPJBProcess(request)
                .fold(
                    onSuccess = { process ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "PPJB process created successfully"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to create PPJB process"
                            )
                        }
                    }
                )
        }
    }
    
    /**
     * Generate PPJB document
     */
    fun generatePPJBDocument(ppjbId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            ppjbService.generatePPJBDocument(ppjbId)
                .fold(
                    onSuccess = { document ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "PPJB document generated successfully"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to generate PPJB document"
                            )
                        }
                    }
                )
        }
    }
    
    /**
     * Generate invitation
     */
    fun generateInvitation(ppjbId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val invitationDate = java.util.Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
            
            ppjbService.generateInvitation(ppjbId, invitationDate)
                .fold(
                    onSuccess = { document ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Invitation generated successfully"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to generate invitation"
                            )
                        }
                    }
                )
        }
    }
    
    /**
     * Send reminder
     */
    fun sendReminder(ppjbId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            ppjbService.sendReminder(ppjbId)
                .fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Reminder sent successfully"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to send reminder"
                            )
                        }
                    }
                )
        }
    }
    
    /**
     * Cancel PPJB process
     */
    fun cancelPPJB(ppjbId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            ppjbService.cancelPPJBProcess(
                id = ppjbId,
                reason = "USER_CANCELLED",
                notes = "Cancelled by user request"
            )
                .fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "PPJB process cancelled successfully"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to cancel PPJB process"
                            )
                        }
                    }
                )
        }
    }
    
    /**
     * Process expired PPJB processes
     */
    fun processExpiredPPJBProcesses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            ppjbService.processExpiredPPJBProcesses()
                .fold(
                    onSuccess = { processedCount ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Processed $processedCount expired PPJB processes"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to process expired PPJB processes"
                            )
                        }
                    }
                )
        }
    }
    
    /**
     * Set filter
     */
    fun setFilter(filter: PPJBFilter) {
        _selectedFilter.value = filter
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    /**
     * Get PPJB statistics
     */
    fun getPPJBStatistics(): StateFlow<PPJBStatistics> {
        return ppjbService.getPPJBStatistics()
    }
    
    /**
     * Observe filter changes
     */
    private fun observeFilterChanges() {
        viewModelScope.launch {
            _selectedFilter.collect { filter ->
                loadPPJBProcesses()
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        reminderScheduler.cleanup()
    }
}

/**
 * PPJB UI State
 */
data class PPJBUIState(
    val isLoading: Boolean = false,
    val ppjbProcesses: List<PPJBDeveloperProcess> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)
