package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.domain.repository.ISLARepository
import com.kprflow.enterprise.domain.usecase.sla.*
import com.kprflow.enterprise.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SLADashboardViewModel @Inject constructor(
    private val getSLAStatusUseCase: GetSLAStatusUseCase,
    private val getAllSLAStatusesUseCase: GetAllSLAStatusesUseCase,
    private val getOverdueDossiersUseCase: GetOverdueDossiersUseCase,
    private val getSLASummaryUseCase: GetSLASummaryUseCase,
    private val getCriticalDossiersUseCase: GetCriticalDossiersUseCase,
    private val monitorSLAChangesUseCase: MonitorSLAChangesUseCase
) : ViewModel() {
    
    private val _slaSummary = MutableStateFlow<Resource<com.kprflow.enterprise.domain.repository.SLASummary>>(Resource.Loading)
    val slaSummary: StateFlow<Resource<com.kprflow.enterprise.domain.repository.SLASummary>> = _slaSummary.asStateFlow()
    
    private val _allSLAStatuses = MutableStateFlow<Resource<List<com.kprflow.enterprise.domain.repository.SLAStatus>>>(Resource.Loading)
    val allSLAStatuses: StateFlow<Resource<List<com.kprflow.enterprise.domain.repository.SLAStatus>>> = _allSLAStatuses.asStateFlow()
    
    private val _overdueDossiers = MutableStateFlow<Resource<List<com.kprflow.enterprise.domain.repository.SLAStatus>>>(Resource.Loading)
    val overdueDossiers: StateFlow<Resource<List<com.kprflow.enterprise.domain.repository.SLAStatus>>> = _overdueDossiers.asStateFlow()
    
    private val _criticalDossiers = MutableStateFlow<Resource<List<com.kprflow.enterprise.domain.repository.SLAStatus>>>(Resource.Loading)
    val criticalDossiers: StateFlow<Resource<List<com.kprflow.enterprise.domain.repository.SLAStatus>>> = _criticalDossiers.asStateFlow()
    
    private val _selectedDossierSLA = MutableStateFlow<Resource<com.kprflow.enterprise.domain.repository.SLAStatus>>(Resource.Loading)
    val selectedDossierSLA: StateFlow<Resource<com.kprflow.enterprise.domain.repository.SLAStatus>> = _selectedDossierSLA.asStateFlow()
    
    init {
        loadSLAData()
    }
    
    fun loadSLAData(userRole: String = "MARKETING") {
        viewModelScope.launch {
            _slaSummary.value = Resource.Loading
            _allSLAStatuses.value = Resource.Loading
            _overdueDossiers.value = Resource.Loading
            _criticalDossiers.value = Resource.Loading
            
            try {
                // Load SLA summary based on user role
                getSLASummaryUseCase(userRole)
                    .onSuccess { summary ->
                        _slaSummary.value = Resource.Success(summary)
                    }
                    .onFailure { exception ->
                        _slaSummary.value = Resource.Error(
                            message = "Failed to load SLA summary: ${exception.message}",
                            exception = exception
                        )
                    }
                
                // Load all SLA statuses
                getAllSLAStatusesUseCase()
                    .onSuccess { statuses ->
                        _allSLAStatuses.value = Resource.Success(statuses)
                    }
                    .onFailure { exception ->
                        _allSLAStatuses.value = Resource.Error(
                            message = "Failed to load SLA statuses: ${exception.message}",
                            exception = exception
                        )
                    }
                
                // Load overdue dossiers
                getOverdueDossiersUseCase()
                    .onSuccess { overdue ->
                        _overdueDossiers.value = Resource.Success(overdue)
                    }
                    .onFailure { exception ->
                        _overdueDossiers.value = Resource.Error(
                            message = "Failed to load overdue dossiers: ${exception.message}",
                            exception = exception
                        )
                    }
                
                // Load critical dossiers
                getCriticalDossiersUseCase()
                    .onSuccess { critical ->
                        _criticalDossiers.value = Resource.Success(critical)
                    }
                    .onFailure { exception ->
                        _criticalDossiers.value = Resource.Error(
                            message = "Failed to load critical dossiers: ${exception.message}",
                            exception = exception
                        )
                    }
            } catch (e: Exception) {
                _slaSummary.value = Resource.Error("Unexpected error: ${e.message}")
                _allSLAStatuses.value = Resource.Error("Unexpected error: ${e.message}")
                _overdueDossiers.value = Resource.Error("Unexpected error: ${e.message}")
                _criticalDossiers.value = Resource.Error("Unexpected error: ${e.message}")
            }
        }
    }
    
    fun refreshDossierSLA(dossierId: String) {
        viewModelScope.launch {
            _selectedDossierSLA.value = Resource.Loading
            
            getSLAStatusUseCase(dossierId)
                .onSuccess { slaStatus ->
                    _selectedDossierSLA.value = Resource.Success(slaStatus)
                }
                .onFailure { exception ->
                    _selectedDossierSLA.value = Resource.Error(
                        message = "Failed to load dossier SLA: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    fun filterOverdueDossiers(type: String) {
        viewModelScope.launch {
            _overdueDossiers.value = Resource.Loading
            
            getOverdueDossiersUseCase(type)
                .onSuccess { overdue ->
                    _overdueDossiers.value = Resource.Success(overdue)
                }
                .onFailure { exception ->
                    _overdueDossiers.value = Resource.Error(
                        message = "Failed to filter overdue dossiers: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    fun startMonitoringSLA(dossierId: String) {
        viewModelScope.launch {
            monitorSLAChangesUseCase(dossierId).collect { slaStatus ->
                _selectedDossierSLA.value = Resource.Success(slaStatus)
            }
        }
    }
    
    fun clearStates() {
        _slaSummary.value = Resource.Loading
        _allSLAStatuses.value = Resource.Loading
        _overdueDossiers.value = Resource.Loading
        _criticalDossiers.value = Resource.Loading
        _selectedDossierSLA.value = Resource.Loading
    }
}
