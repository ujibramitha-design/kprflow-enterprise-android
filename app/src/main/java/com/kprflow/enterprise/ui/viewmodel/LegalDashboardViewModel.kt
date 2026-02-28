package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.model.KprDossier
import com.kprflow.enterprise.data.model.KprStatus
import com.kprflow.enterprise.data.repository.KprRepository
import com.kprflow.enterprise.ui.screens.LegalDashboardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegalDashboardViewModel @Inject constructor(
    private val kprRepository: KprRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LegalDashboardUiState>(LegalDashboardUiState.Loading)
    val uiState: StateFlow<LegalDashboardUiState> = _uiState.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow<KprStatus?>(null)
    val selectedFilter: StateFlow<KprStatus?> = _selectedFilter.asStateFlow()
    
    init {
        loadDossiers()
    }
    
    fun loadDossiers() {
        viewModelScope.launch {
            _uiState.value = LegalDashboardUiState.Loading
            
            try {
                val allDossiers = kprRepository.getAllDossiers()
                    .getOrNull().orEmpty()
                
                // Group dossiers by status
                val dossiersByStatus = allDossiers
                    .groupBy { it.status }
                    .toMap()
                
                _uiState.value = LegalDashboardUiState.Success(dossiersByStatus)
            } catch (e: Exception) {
                _uiState.value = LegalDashboardUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun setFilter(status: KprStatus?) {
        _selectedFilter.value = status
        // TODO: Apply filter to dossiers
    }
    
    fun updateDossierStatus(dossierId: String, newStatus: KprStatus) {
        viewModelScope.launch {
            try {
                kprRepository.updateDossierStatus(dossierId, newStatus)
                    .onSuccess {
                        // Refresh the dossiers after status update
                        loadDossiers()
                    }
                    .onFailure { exception ->
                        // Handle error - maybe show a snackbar
                    }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun refreshDossiers() {
        loadDossiers()
    }
}
