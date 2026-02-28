package com.kprflow.enterprise.ui.screens

import com.kprflow.enterprise.ui.common.UiState

// Use standardized UiState from common package
typealias SLAState<T> = UiState<T>

// Legacy sealed class kept for backward compatibility
@Deprecated("Use UiState<T> instead", ReplaceWith("UiState<T>"))
sealed class SLAStateLegacy<T> {
    object Loading : SLAStateLegacy<Nothing>()
    data class Success<T>(val data: T) : SLAStateLegacy<T>()
    data class Error(val message: String) : SLAStateLegacy<Nothing>()
}
