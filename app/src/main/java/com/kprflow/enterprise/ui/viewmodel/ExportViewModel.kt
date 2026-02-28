package com.kprflow.enterprise.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.utils.ExportUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
    
    fun exportToExcel(context: Context, data: List<Any>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            
            try {
                // Convert data to KprApplication list (adapt based on actual data type)
                val kprData = convertToKprApplications(data)
                
                val uri = ExportUtils.exportKprToExcel(
                    context = context,
                    data = kprData,
                    fileName = "Laporan_KPR_${System.currentTimeMillis()}"
                )
                
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportResult = Result.success(uri)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportResult = Result.failure(e)
                )
            }
        }
    }
    
    fun exportToPdf(context: Context, data: List<Any>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            
            try {
                // Convert data to KprApplication list (adapt based on actual data type)
                val kprData = convertToKprApplications(data)
                
                val uri = ExportUtils.exportKprToPdf(
                    context = context,
                    data = kprData,
                    fileName = "Laporan_KPR_${System.currentTimeMillis()}"
                )
                
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportResult = Result.success(uri)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportResult = Result.failure(e)
                )
            }
        }
    }
    
    fun shareFile(context: Context, uri: Uri, fileName: String) {
        ExportUtils.shareFile(context, uri, fileName)
    }
    
    fun clearExportResult() {
        _uiState.value = _uiState.value.copy(exportResult = null)
    }
    
    private fun convertToKprApplications(data: List<Any>): List<KprApplication> {
        // This is a placeholder - adapt based on your actual data model
        // You'll need to convert whatever data type you have to KprApplication
        return emptyList() // Replace with actual conversion logic
    }
}

data class ExportUiState(
    val isExporting: Boolean = false,
    val exportResult: Result<Uri?>? = null
)

// Placeholder data class - replace with your actual KprApplication model
data class KprApplication(
    val customerName: String? = null,
    val unitBlock: String? = null,
    val status: String? = null,
    val unitPrice: BigDecimal? = null,
    val createdAt: Date? = null
)
