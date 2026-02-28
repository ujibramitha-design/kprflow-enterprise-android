package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.domain.usecase.sla.*
import com.kprflow.enterprise.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DossierSLAViewModel @Inject constructor(
    private val getDossierSlaUseCase: GetDossierSlaUseCase,
    private val getDocumentSlaUseCase: GetDocumentSlaUseCase,
    private val getSLAWarningLevelUseCase: GetSLAWarningLevelUseCase,
    private val getSLAColorConfigUseCase: GetSLAColorConfigUseCase
) : ViewModel() {
    
    private val _slaStatus = MutableStateFlow<Resource<com.kprflow.enterprise.domain.usecase.sla.SlaStatus>>(Resource.Loading)
    val slaStatus: StateFlow<Resource<com.kprflow.enterprise.domain.usecase.sla.SlaStatus>> = _slaStatus.asStateFlow()
    
    private val _documentSlaStatus = MutableStateFlow<Resource<com.kprflow.enterprise.domain.usecase.sla.DocumentSlaStatus>>(Resource.Loading)
    val documentSlaStatus: StateFlow<Resource<com.kprflow.enterprise.domain.usecase.sla.DocumentSlaStatus>> = _documentSlaStatus.asStateFlow()
    
    private val _warningLevel = MutableStateFlow<Resource<com.kprflow.enterprise.domain.usecase.sla.SLAWarningLevel>>(Resource.Loading)
    val warningLevel: StateFlow<Resource<com.kprflow.enterprise.domain.usecase.sla.SLAWarningLevel>> = _warningLevel.asStateFlow()
    
    private val _colorConfig = MutableStateFlow<com.kprflow.enterprise.domain.usecase.sla.SLAColorConfig>(com.kprflow.enterprise.domain.usecase.sla.SLAColorConfig.DEFAULT)
    val colorConfig: StateFlow<com.kprflow.enterprise.domain.usecase.sla.SLAColorConfig> = _colorConfig.asStateFlow()
    
    fun loadSLAStatus(dossierId: String) {
        viewModelScope.launch {
            _slaStatus.value = Resource.Loading
            
            try {
                getDossierSlaUseCase(dossierId).collect { status ->
                    _slaStatus.value = Resource.Success(status)
                    
                    // Update warning level based on status
                    updateWarningLevel(dossierId)
                }
            } catch (e: Exception) {
                _slaStatus.value = Resource.Error(
                    message = "Failed to load SLA status: ${e.message}",
                    exception = e
                )
            }
        }
    }
    
    fun loadDocumentSLAStatus(dossierId: String) {
        viewModelScope.launch {
            _documentSlaStatus.value = Resource.Loading
            
            try {
                getDocumentSlaUseCase(dossierId).collect { status ->
                    _documentSlaStatus.value = Resource.Success(status)
                }
            } catch (e: Exception) {
                _documentSlaStatus.value = Resource.Error(
                    message = "Failed to load document SLA status: ${e.message}",
                    exception = e
                )
            }
        }
    }
    
    private fun updateWarningLevel(dossierId: String) {
        viewModelScope.launch {
            getSLAWarningLevelUseCase(dossierId)
                .onSuccess { level ->
                    _warningLevel.value = Resource.Success(level)
                    _colorConfig.value = getSLAColorConfigUseCase(level)
                }
                .onFailure { exception ->
                    _warningLevel.value = Resource.Error(
                        message = "Failed to get warning level: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    fun refreshSLAStatus(dossierId: String) {
        loadSLAStatus(dossierId)
        loadDocumentSLAStatus(dossierId)
    }
    
    fun clearStates() {
        _slaStatus.value = Resource.Loading
        _documentSlaStatus.value = Resource.Loading
        _warningLevel.value = Resource.Loading
        _colorConfig.value = com.kprflow.enterprise.domain.usecase.sla.SLAColorConfig.DEFAULT
    }
}
