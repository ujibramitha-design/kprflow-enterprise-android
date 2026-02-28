package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.ReportGeneratorRepository
import com.kprflow.enterprise.data.repository.FinalReportRecord
import com.kprflow.enterprise.ui.screens.ReportGenerationUiState
import com.kprflow.enterprise.ui.screens.ReportsState
import com.kprflow.enterprise.ui.screens.BASTData
import com.kprflow.enterprise.ui.screens.HandoverCertificateData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportGenerationViewModel @Inject constructor(
    private val reportGeneratorRepository: ReportGeneratorRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ReportGenerationUiState>(ReportGenerationUiState.Idle)
    val uiState: StateFlow<ReportGenerationUiState> = _uiState.asStateFlow()
    
    private val _reportsState = MutableStateFlow<ReportsState>(ReportsState.Loading)
    val reportsState: StateFlow<ReportsState> = _reportsState.asStateFlow()
    
    fun loadReports(dossierId: String) {
        viewModelScope.launch {
            _reportsState.value = ReportsState.Loading
            
            try {
                val reports = reportGeneratorRepository.getFinalReports(dossierId)
                    .getOrNull().orEmpty()
                _reportsState.value = ReportsState.Success(reports)
            } catch (e: Exception) {
                _reportsState.value = ReportsState.Error(e.message ?: "Failed to load reports")
            }
        }
    }
    
    fun generateBASTReport(dossierId: String, bastData: BASTData) {
        viewModelScope.launch {
            try {
                _uiState.value = ReportGenerationUiState.Generating
                
                val result = reportGeneratorRepository.generateBASTReport(
                    dossierId = dossierId,
                    handoverDate = bastData.handoverDate,
                    handoverTime = bastData.handoverTime,
                    handoverLocation = bastData.handoverLocation,
                    witnessName = bastData.witnessName,
                    witnessPosition = bastData.witnessPosition
                )
                
                if (result.isSuccess) {
                    _uiState.value = ReportGenerationUiState.Success(
                        reportId = result.getOrNull()?.reportId ?: "",
                        publicUrl = result.getOrNull()?.publicUrl ?: ""
                    )
                    
                    // Refresh reports list
                    loadReports(dossierId)
                } else {
                    _uiState.value = ReportGenerationUiState.Error(
                        "Failed to generate BAST report"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ReportGenerationUiState.Error(
                    "Error generating BAST report: ${e.message}"
                )
            }
        }
    }
    
    fun generateHandoverCertificate(dossierId: String, certificateData: HandoverCertificateData) {
        viewModelScope.launch {
            try {
                _uiState.value = ReportGenerationUiState.Generating
                
                val result = reportGeneratorRepository.generateHandoverCertificate(
                    dossierId = dossierId,
                    handoverDate = certificateData.handoverDate,
                    propertyCondition = certificateData.propertyCondition,
                    includedItems = certificateData.includedItems,
                    excludedItems = emptyList(),
                    specialNotes = certificateData.specialNotes
                )
                
                if (result.isSuccess) {
                    _uiState.value = ReportGenerationUiState.Success(
                        reportId = result.getOrNull()?.reportId ?: "",
                        publicUrl = result.getOrNull()?.publicUrl ?: ""
                    )
                    
                    // Refresh reports list
                    loadReports(dossierId)
                } else {
                    _uiState.value = ReportGenerationUiState.Error(
                        "Failed to generate handover certificate"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ReportGenerationUiState.Error(
                    "Error generating handover certificate: ${e.message}"
                )
            }
        }
    }
    
    fun downloadReport(reportId: String) {
        viewModelScope.launch {
            try {
                val downloadUrl = reportGeneratorRepository.getReportDownloadUrl(reportId)
                    .getOrNull()
                
                downloadUrl?.let { url ->
                    // TODO: Open download URL in browser or trigger download
                }
            } catch (e: Exception) {
                // TODO: Show error message
            }
        }
    }
    
    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            try {
                reportGeneratorRepository.deleteReport(reportId)
                    .getOrThrow()
                
                // TODO: Get current dossier ID and refresh reports
                // For now, just show success
            } catch (e: Exception) {
                // TODO: Show error message
            }
        }
    }
    
    fun updateSignatures(reportId: String, customerSigned: Boolean, developerSigned: Boolean) {
        viewModelScope.launch {
            try {
                reportGeneratorRepository.updateReportSignatures(
                    reportId = reportId,
                    customerSigned = customerSigned,
                    developerSigned = developerSigned
                ).getOrThrow()
                
                // TODO: Get current dossier ID and refresh reports
                // For now, just show success
            } catch (e: Exception) {
                // TODO: Show error message
            }
        }
    }
    
    fun showBASTDialog(dossierId: String) {
        _uiState.value = com.kprflow.enterprise.ui.screens.ShowBASTDialog
    }
    
    fun hideBASTDialog() {
        _uiState.value = ReportGenerationUiState.Idle
    }
    
    fun showHandoverDialog(dossierId: String) {
        _uiState.value = com.kprflow.enterprise.ui.screens.ShowHandoverDialog
    }
    
    fun hideHandoverDialog() {
        _uiState.value = ReportGenerationUiState.Idle
    }
    
    fun clearState() {
        _uiState.value = ReportGenerationUiState.Idle
    }
    
    fun refreshReports(dossierId: String) {
        loadReports(dossierId)
    }
}
