package com.kprflow.enterprise.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.FinancialRepository
import com.kprflow.enterprise.ui.components.PaymentCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class FinancialViewModel @Inject constructor(
    private val financialRepository: FinancialRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FinancialUiState())
    val uiState: StateFlow<FinancialUiState> = _uiState.asStateFlow()
    
    fun uploadPaymentProof(
        dossierId: String,
        category: PaymentCategory,
        amount: BigDecimal,
        paymentMethod: String,
        bankName: String? = null,
        accountNumber: String? = null,
        accountName: String? = null,
        referenceNumber: String? = null,
        evidenceUri: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, error = null)
            
            try {
                val result = financialRepository.createFinancialTransaction(
                    dossierId = dossierId,
                    category = category.name,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    bankName = bankName,
                    accountNumber = accountNumber,
                    accountName = accountName,
                    referenceNumber = referenceNumber,
                    evidenceUrl = evidenceUri,
                    notes = notes
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        uploadResult = Result.success(Unit)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = result.exceptionOrNull()?.message ?: "Gagal upload bukti pembayaran"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = e.message ?: "Terjadi kesalahan saat upload bukti pembayaran"
                )
            }
        }
    }
    
    fun verifyTransaction(
        transactionId: String,
        isApproved: Boolean,
        rejectionReason: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifying = true, error = null)
            
            try {
                val result = financialRepository.verifyFinancialTransaction(
                    transactionId = transactionId,
                    isApproved = isApproved,
                    rejectionReason = rejectionReason
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isVerifying = false,
                        verificationResult = Result.success(Unit)
                    )
                    // Refresh pending transactions
                    loadPendingTransactions()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isVerifying = false,
                        error = result.exceptionOrNull()?.message ?: "Gagal verifikasi transaksi"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isVerifying = false,
                    error = e.message ?: "Terjadi kesalahan saat verifikasi transaksi"
                )
            }
        }
    }
    
    fun loadPendingTransactions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val transactions = financialRepository.getPendingTransactions()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pendingTransactions = transactions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal memuat transaksi pending"
                )
            }
        }
    }
    
    fun loadFinancialDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val dashboard = financialRepository.getFinancialDashboard()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dashboardData = dashboard
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal memuat dashboard keuangan"
                )
            }
        }
    }
    
    fun loadTransactionHistory(dossierId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val transactions = financialRepository.getTransactionHistory(dossierId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    transactionHistory = transactions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal memuat riwayat transaksi"
                )
            }
        }
    }
    
    fun clearUploadResult() {
        _uiState.value = _uiState.value.copy(uploadResult = null)
    }
    
    fun clearVerificationResult() {
        _uiState.value = _uiState.value.copy(verificationResult = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    init {
        loadPendingTransactions()
        loadFinancialDashboard()
    }
}

data class FinancialUiState(
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val isVerifying: Boolean = false,
    val pendingTransactions: List<FinancialTransaction> = emptyList(),
    val transactionHistory: List<FinancialTransaction> = emptyList(),
    val dashboardData: FinancialDashboard? = null,
    val uploadResult: Result<Unit>? = null,
    val verificationResult: Result<Unit>? = null,
    val error: String? = null
)

data class FinancialTransaction(
    val id: String,
    val dossierId: String,
    val unitId: String,
    val category: String,
    val amount: BigDecimal,
    val status: String,
    val paymentMethod: String,
    val bankName: String?,
    val accountNumber: String?,
    val accountName: String?,
    val referenceNumber: String?,
    val evidenceUrl: String?,
    val isRealized: Boolean,
    val verifiedAt: String?,
    val verifiedBy: String?,
    val rejectionReason: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String,
    // Related data
    val customerName: String?,
    val unitBlock: String?,
    val projectName: String?
)

data class FinancialDashboard(
    val totalRevenue: BigDecimal,
    val pendingVerification: BigDecimal,
    val rejectedAmount: BigDecimal,
    val totalTransactions: Long,
    val pendingCount: Long,
    val verifiedCount: Long,
    val rejectedCount: Long,
    val categoryBreakdown: List<CategoryBreakdown>,
    val dailySummary: List<DailySummary>
)

data class CategoryBreakdown(
    val category: String,
    val total: BigDecimal,
    val count: Long,
    val pending: BigDecimal
)

data class DailySummary(
    val date: String,
    val total: BigDecimal,
    val count: Long
)
