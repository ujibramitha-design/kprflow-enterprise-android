package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.KprRepository
import com.kprflow.enterprise.ui.components.CancellationReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CancellationViewModel @Inject constructor(
    private val kprRepository: KprRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CancellationUiState())
    val uiState: StateFlow<CancellationUiState> = _uiState.asStateFlow()
    
    fun cancelKprApplication(
        kprId: String,
        reason: CancellationReason,
        additionalNotes: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true, error = null)
            
            try {
                val result = kprRepository.cancelKprApplication(
                    kprId = kprId,
                    reason = reason.name,
                    additionalNotes = additionalNotes
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isCancelling = false,
                        cancellationResult = Result.success(Unit)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isCancelling = false,
                        error = result.exceptionOrNull()?.message ?: "Gagal membatalkan KPR"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCancelling = false,
                    error = e.message ?: "Terjadi kesalahan saat membatalkan KPR"
                )
            }
        }
    }
    
    fun clearResult() {
        _uiState.value = _uiState.value.copy(cancellationResult = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class CancellationUiState(
    val isCancelling: Boolean = false,
    val cancellationResult: Result<Unit>? = null,
    val error: String? = null
)
