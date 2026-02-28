package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.model.KprStatus
import com.kprflow.enterprise.data.repository.KprRepository
import com.kprflow.enterprise.ui.screens.LegalKanbanUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegalKanbanViewModel @Inject constructor(
    private val kprRepository: KprRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LegalKanbanUiState>(LegalKanbanUiState.Loading)
    val uiState: StateFlow<LegalKanbanUiState> = _uiState.asStateFlow()
    
    init {
        loadDossiers()
        setupRealtimeUpdates()
    }
    
    fun refreshDossiers() {
        loadDossiers()
    }
    
    fun updateDossierStatus(dossierId: String, newStatus: KprStatus) {
        viewModelScope.launch {
            try {
                // Optimistic update
                val currentState = _uiState.value
                if (currentState is LegalKanbanUiState.Success) {
                    val updatedDossiers = currentState.dossiers.map { dossier ->
                        if (dossier.id == dossierId) {
                            dossier.copy(status = newStatus)
                        } else {
                            dossier
                        }
                    }
                    _uiState.value = LegalKanbanUiState.Success(updatedDossiers)
                }
                
                // Update in repository
                kprRepository.updateDossierStatus(dossierId, newStatus)
                
                // Reload to ensure consistency
                loadDossiers()
                
            } catch (e: Exception) {
                // Revert optimistic update on error
                loadDossiers()
            }
        }
    }
    
    private fun setupRealtimeUpdates() {
        viewModelScope.launch {
            try {
                kprRepository.setupRealtimeChannel().collect { updatedDossier ->
                    // Update UI state when dossier changes
                    val currentState = _uiState.value
                    if (currentState is LegalKanbanUiState.Success) {
                        val updatedDossiers = currentState.dossiers.map { dossier ->
                            if (dossier.id == updatedDossier.id) {
                                updatedDossier
                            } else {
                                dossier
                            }
                        }
                        _uiState.value = LegalKanbanUiState.Success(updatedDossiers)
                    }
                }
            } catch (e: Exception) {
                // Handle realtime connection error
            }
        }
    }
    
    private fun loadDossiers() {
        viewModelScope.launch {
            try {
                _uiState.value = LegalKanbanUiState.Loading
                val dossiers = kprRepository.getAllDossiers()
                _uiState.value = LegalKanbanUiState.Success(dossiers)
            } catch (e: Exception) {
                _uiState.value = LegalKanbanUiState.Error(
                    message = e.message ?: "Failed to load dossiers"
                )
            }
        }
    }
}

