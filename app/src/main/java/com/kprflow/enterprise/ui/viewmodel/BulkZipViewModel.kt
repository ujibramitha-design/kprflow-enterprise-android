package com.kprflow.enterprise.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.DocumentRepository
import com.kprflow.enterprise.utils.BulkZipGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BulkZipViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val zipGenerator: BulkZipGenerator
) : AndroidViewModel(android.app.Application()) {
    
    private val _uiState = MutableStateFlow(BulkZipUiState())
    val uiState: StateFlow<BulkZipUiState> = _uiState.asStateFlow()
    
    fun generateBankPack(
        dossierId: String,
        customerName: String,
        unitBlock: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
            
            try {
                // Get documents for the dossier
                val documents = documentRepository.getDocumentsByDossierId(dossierId)
                
                if (documents.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = "Tidak ada dokumen yang tersedia untuk dossiers ini"
                    )
                    return@launch
                }
                
                // Convert to DocumentFile format
                val documentFiles = documents.map { doc ->
                    BulkZipGenerator.DocumentFile(
                        name = doc.name,
                        url = doc.fileUrl,
                        type = doc.type,
                        folder = determineFolder(doc.type),
                        uploadedAt = doc.createdAt
                    )
                }
                
                // Generate ZIP
                val result = zipGenerator.generateBankSubmissionPack(
                    dossierId = dossierId,
                    customerName = customerName,
                    unitBlock = unitBlock,
                    documents = documentFiles
                ) { progress ->
                    _uiState.value = _uiState.value.copy(progress = progress)
                }
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        zipResult = Result.success(result.getOrNull()!!)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = result.exceptionOrNull()?.message ?: "Gagal membuat paket bank"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Terjadi kesalahan saat membuat paket bank"
                )
            }
        }
    }
    
    fun validateDocuments(dossierId: String) {
        viewModelScope.launch {
            try {
                val documents = documentRepository.getDocumentsByDossierId(dossierId)
                
                val documentFiles = documents.map { doc ->
                    BulkZipGenerator.DocumentFile(
                        name = doc.name,
                        url = doc.fileUrl,
                        type = doc.type,
                        folder = determineFolder(doc.type),
                        uploadedAt = doc.createdAt
                    )
                }
                
                val validation = zipGenerator.validateDocuments(documentFiles)
                _uiState.value = _uiState.value.copy(validationResult = validation)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Gagal memvalidasi dokumen"
                )
            }
        }
    }
    
    fun loadDocuments(dossierId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val documents = documentRepository.getDocumentsByDossierId(dossierId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    documents = documents
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal memuat dokumen"
                )
            }
        }
    }
    
    fun clearZipResult() {
        _uiState.value = _uiState.value.copy(zipResult = null)
    }
    
    fun clearValidationError() {
        _uiState.value = _uiState.value.copy(validationResult = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun determineFolder(documentType: String): String {
        val upperType = documentType.uppercase()
        
        return when {
            upperType.contains("KTP") || upperType.contains("KK") || 
            upperType.contains("AKTA") || upperType.contains("NIKAH") -> "01_Legal"
            
            upperType.contains("SLIP") || upperType.contains("GAJI") || 
            upperType.contains("REKENING") || upperType.contains("TABUNGAN") -> "02_Income"
            
            upperType.contains("SERTIFIKAT") || upperType.contains("IMB") || 
            upperType.contains("PBB") -> "03_Property"
            
            upperType.contains("DOSSIER") || upperType.contains("MERGED") -> "04_Dossier"
            
            else -> ""
        }
    }
}

data class BulkZipUiState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val documents: List<Document> = emptyList(),
    val progress: BulkZipGenerator.ZipProgress? = null,
    val validationResult: BulkZipGenerator.ValidationResult? = null,
    val zipResult: Result<Uri>? = null,
    val error: String? = null
)

data class Document(
    val id: String,
    val dossierId: String,
    val name: String,
    val type: String,
    val fileUrl: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val uploadedBy: String?,
    val fileSize: Long?,
    val mimeType: String?
)
