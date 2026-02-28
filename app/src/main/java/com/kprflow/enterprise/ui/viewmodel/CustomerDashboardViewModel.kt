package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.model.KprDossier
import com.kprflow.enterprise.domain.usecase.dossier.GetUserDossiersUseCase
import com.kprflow.enterprise.domain.usecase.dossier.CreateDossierUseCase
import com.kprflow.enterprise.ui.screens.CustomerDashboardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerDashboardViewModel @Inject constructor(
    private val getUserDossiersUseCase: GetUserDossiersUseCase,
    private val createDossierUseCase: CreateDossierUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<CustomerDashboardUiState>(CustomerDashboardUiState.Loading)
    val uiState: StateFlow<CustomerDashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDossiers()
        setupRealtimeUpdates()
    }
    
    private fun setupRealtimeUpdates() {
        viewModelScope.launch {
            try {
                kprRepository.setupRealtimeChannel().collect { updatedDossier ->
                    // Update UI state when dossier changes
                    val currentState = _uiState.value
                    if (currentState is CustomerDashboardUiState.Success) {
                        val updatedDossiers = currentState.dossiers.map { dossier ->
                            if (dossier.id == updatedDossier.id) {
                                updatedDossier
                            } else {
                                dossier
                            }
                        }
                        _uiState.value = CustomerDashboardUiState.Success(updatedDossiers)
                    }
                }
            } catch (e: Exception) {
                // Handle realtime connection error
            }
        }
    }
    
    fun loadDossiers() {
        viewModelScope.launch {
            _uiState.value = CustomerDashboardUiState.Loading
            
            getUserDossiersUseCase()
                .onSuccess { dossiers ->
                    _uiState.value = CustomerDashboardUiState.Success(dossiers)
                }
                .onFailure { exception ->
                    _uiState.value = CustomerDashboardUiState.Error(
                        exception.message ?: "Unknown error"
                    )
                }
        }
    }
    
    fun createDossier(
        unitId: String? = null,
        kprAmount: java.math.BigDecimal? = null,
        dpAmount: java.math.BigDecimal? = null,
        bankName: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            createDossierUseCase(unitId, kprAmount, dpAmount, bankName, notes)
                .onSuccess { dossier ->
                    // Refresh dossiers after creation
                    loadDossiers()
                }
                .onFailure { exception ->
                    _uiState.value = CustomerDashboardUiState.Error(
                        "Failed to create dossier: ${exception.message}"
                    )
                }
        }
    }
    
    fun refreshDossiers() {
        loadDossiers()
    }
}
