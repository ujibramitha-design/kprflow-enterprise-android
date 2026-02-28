package com.kprflow.enterprise.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.utils.DocumentMerger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DocumentMergerViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(DocumentMergerUiState())
    val uiState: StateFlow<DocumentMergerUiState> = _uiState.asStateFlow()
    
    fun validateDocuments(documentPaths: List<String>) {
        val validation = DocumentMerger.validateDocuments(documentPaths)
        _uiState.value = _uiState.value.copy(
            validationResult = validation,
            isMerging = false
        )
    }
    
    fun mergeDocuments(
        context: Context,
        dossierId: String,
        customerName: String,
        documentPaths: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMerging = true)
            
            try {
                val mergedFile = DocumentMerger.mergeDocuments(
                    context = context,
                    dossierId = dossierId,
                    customerName = customerName,
                    documentPaths = documentPaths
                )
                
                _uiState.value = _uiState.value.copy(
                    isMerging = false,
                    mergeResult = Result.success(mergedFile)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isMerging = false,
                    mergeResult = Result.failure(e)
                )
            }
        }
    }
    
    fun clearMergeResult() {
        _uiState.value = _uiState.value.copy(mergeResult = null)
    }
}

data class DocumentMergerUiState(
    val isMerging: Boolean = false,
    val validationResult: DocumentMerger.DocumentValidationResult? = null,
    val mergeResult: Result<File?>? = null
)
