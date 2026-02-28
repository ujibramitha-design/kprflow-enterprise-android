package com.kprflow.enterprise.ui.screens

import com.kprflow.enterprise.data.model.KprDossier
import com.kprflow.enterprise.ui.common.UiState

// Use standardized UiState from common package
typealias CustomerDashboardUiState = UiState<List<KprDossier>>

// Legacy sealed class kept for backward compatibility
@Deprecated("Use UiState<List<KprDossier>> instead", ReplaceWith("UiState<List<KprDossier>>"))
sealed class CustomerDashboardUiStateLegacy {
    object Loading : CustomerDashboardUiStateLegacy()
    data class Success(val dossiers: List<KprDossier>) : CustomerDashboardUiStateLegacy()
    data class Error(val message: String) : CustomerDashboardUiStateLegacy()
}
