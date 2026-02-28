package com.kprflow.enterprise.ui.screens

import com.kprflow.enterprise.data.model.KprDossier

sealed class LegalKanbanUiState {
    object Loading : LegalKanbanUiState()
    data class Success(val dossiers: List<KprDossier>) : LegalKanbanUiState()
    data class Error(val message: String) : LegalKanbanUiState()
}
