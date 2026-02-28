package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.SiKasepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class SiKasepViewModel @Inject constructor(
    private val siKasepRepository: SiKasepRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SiKasepUiState())
    val uiState: StateFlow<SiKasepUiState> = _uiState.asStateFlow()
    
    fun checkEligibility(
        userId: String,
        nik: String,
        monthlyIncome: BigDecimal?,
        isFirstHome: Boolean?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, error = null)
            
            try {
                val result = siKasepRepository.checkEligibility(
                    userId = userId,
                    nik = nik,
                    monthlyIncome = monthlyIncome,
                    isFirstHome = isFirstHome
                )
                
                if (result.isSuccess) {
                    val eligibilityResult = result.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        checkResult = Result.success(Unit),
                        currentStatus = eligibilityResult?.status,
                        sikasepId = eligibilityResult?.idSikasep,
                        rejectionReason = eligibilityResult?.rejectionReason,
                        lastChecked = java.time.Instant.now().toString()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        error = result.exceptionOrNull()?.message ?: "Gagal mengecek kelayakan subsidi"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    error = e.message ?: "Terjadi kesalahan saat mengecek kelayakan subsidi"
                )
            }
        }
    }
    
    fun bulkCheckEligibility(userIds: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBulkChecking = true, error = null)
            
            try {
                val result = siKasepRepository.bulkCheckEligibility(userIds)
                
                if (result.isSuccess) {
                    val bulkResult = result.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        isBulkChecking = false,
                        bulkCheckResult = Result.success(bulkResult ?: emptyList())
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isBulkChecking = false,
                        error = result.exceptionOrNull()?.message ?: "Gagal mengecek kelayakan subsidi massal"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBulkChecking = false,
                    error = e.message ?: "Terjadi kesalahan saat mengecek kelayakan subsidi massal"
                )
            }
        }
    }
    
    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val stats = siKasepRepository.getStatistics()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statistics = stats
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal memuat statistik subsidi"
                )
            }
        }
    }
    
    fun loadEligibilityHistory(userId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val history = siKasepRepository.getEligibilityHistory(userId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    eligibilityHistory = history
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal memuat riwayat kelayakan"
                )
            }
        }
    }
    
    fun clearCheckResult() {
        _uiState.value = _uiState.value.copy(checkResult = null)
    }
    
    fun clearBulkCheckResult() {
        _uiState.value = _uiState.value.copy(bulkCheckResult = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    init {
        loadStatistics()
    }
}

data class SiKasepUiState(
    val isLoading: Boolean = false,
    val isChecking: Boolean = false,
    val isBulkChecking: Boolean = false,
    val currentStatus: String? = null,
    val sikasepId: String? = null,
    val rejectionReason: String? = null,
    val lastChecked: String? = null,
    val statistics: SiKasepStatistics? = null,
    val eligibilityHistory: List<SiKasepLog> = emptyList(),
    val checkResult: Result<Unit>? = null,
    val bulkCheckResult: Result<List<BulkCheckResult>>? = null,
    val error: String? = null
)

data class SiKasepStatistics(
    val totalChecked: Long,
    val eligibleCount: Long,
    val notEligibleCount: Long,
    val errorCount: Long,
    val eligibilityRate: Double,
    val averageIncome: BigDecimal,
    val rejectionBreakdown: List<RejectionReason>
)

data class RejectionReason(
    val reason: String,
    val count: Long
)

data class SiKasepLog(
    val id: String,
    val userId: String,
    val nik: String,
    val status: String,
    val idSikasep: String?,
    val rejectionReason: String?,
    val screenshotUrl: String?,
    val processingTimeMs: Int,
    val checkedAt: String,
    val checkedBy: String?,
    val customerName: String?
)

data class BulkCheckResult(
    val userId: String,
    val success: Boolean,
    val status: String?,
    val idSikasep: String?,
    val rejectionReason: String?,
    val error: String?
)
