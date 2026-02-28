package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.WhatsAppRepository
import com.kprflow.enterprise.data.repository.WhatsAppMessageResult
import com.kprflow.enterprise.data.repository.WhatsAppTemplateType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhatsAppNotificationViewModel @Inject constructor(
    private val whatsAppRepository: WhatsAppRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<WhatsAppNotificationUiState>(WhatsAppNotificationUiState.Idle)
    val uiState: StateFlow<WhatsAppNotificationUiState> = _uiState.asStateFlow()
    
    private val _notificationHistory = MutableStateFlow<List<com.kprflow.enterprise.data.repository.WhatsAppNotification>>(emptyList())
    val notificationHistory: StateFlow<List<com.kprflow.enterprise.data.repository.WhatsAppNotification>> = _notificationHistory.asStateFlow()
    
    fun sendDocumentReminder(
        userId: String,
        dossierId: String,
        missingDocuments: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = WhatsAppNotificationUiState.Sending
            
            try {
                val result = whatsAppRepository.sendDocumentReminder(userId, dossierId, missingDocuments)
                
                if (result.isSuccess) {
                    _uiState.value = WhatsAppNotificationUiState.Success(
                        message = "Document reminder sent successfully",
                        messageSid = result.getOrNull()?.messageSid
                    )
                    loadNotificationHistory(userId)
                } else {
                    _uiState.value = WhatsAppNotificationUiState.Error(
                        "Failed to send document reminder"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = WhatsAppNotificationUiState.Error(
                    "Error sending document reminder: ${e.message}"
                )
            }
        }
    }
    
    fun sendSP3KNotification(
        userId: String,
        dossierId: String,
        sp3kNumber: String
    ) {
        viewModelScope.launch {
            _uiState.value = WhatsAppNotificationUiState.Sending
            
            try {
                val result = whatsAppRepository.sendSP3KNotification(userId, dossierId, sp3kNumber)
                
                if (result.isSuccess) {
                    _uiState.value = WhatsAppNotificationUiState.Success(
                        message = "SP3K notification sent successfully",
                        messageSid = result.getOrNull()?.messageSid
                    )
                    loadNotificationHistory(userId)
                } else {
                    _uiState.value = WhatsAppNotificationUiState.Error(
                        "Failed to send SP3K notification"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = WhatsAppNotificationUiState.Error(
                    "Error sending SP3K notification: ${e.message}"
                )
            }
        }
    }
    
    fun sendSLAWarning(
        userId: String,
        dossierId: String,
        daysRemaining: Int
    ) {
        viewModelScope.launch {
            _uiState.value = WhatsAppNotificationUiState.Sending
            
            try {
                val result = whatsAppRepository.sendSLAWarning(userId, dossierId, daysRemaining)
                
                if (result.isSuccess) {
                    _uiState.value = WhatsAppNotificationUiState.Success(
                        message = "SLA warning sent successfully",
                        messageSid = result.getOrNull()?.messageSid
                    )
                    loadNotificationHistory(userId)
                } else {
                    _uiState.value = WhatsAppNotificationUiState.Error(
                        "Failed to send SLA warning"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = WhatsAppNotificationUiState.Error(
                    "Error sending SLA warning: ${e.message}"
                )
            }
        }
    }
    
    fun sendBASTInvitation(
        userId: String,
        dossierId: String,
        bastDate: String,
        location: String
    ) {
        viewModelScope.launch {
            _uiState.value = WhatsAppNotificationUiState.Sending
            
            try {
                val result = whatsAppRepository.sendBASTInvitation(userId, dossierId, bastDate, location)
                
                if (result.isSuccess) {
                    _uiState.value = WhatsAppNotificationUiState.Success(
                        message = "BAST invitation sent successfully",
                        messageSid = result.getOrNull()?.messageSid
                    )
                    loadNotificationHistory(userId)
                } else {
                    _uiState.value = WhatsAppNotificationUiState.Error(
                        "Failed to send BAST invitation"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = WhatsAppNotificationUiState.Error(
                    "Error sending BAST invitation: ${e.message}"
                )
            }
        }
    }
    
    fun sendCustomMessage(
        userId: String,
        templateType: WhatsAppTemplateType,
        variables: Map<String, String>
    ) {
        viewModelScope.launch {
            _uiState.value = WhatsAppNotificationUiState.Sending
            
            try {
                val result = whatsAppRepository.sendWhatsAppMessage(userId, templateType, variables)
                
                if (result.isSuccess) {
                    _uiState.value = WhatsAppNotificationUiState.Success(
                        message = "Message sent successfully",
                        messageSid = result.getOrNull()?.messageSid
                    )
                    loadNotificationHistory(userId)
                } else {
                    _uiState.value = WhatsAppNotificationUiState.Error(
                        "Failed to send message"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = WhatsAppNotificationUiState.Error(
                    "Error sending message: ${e.message}"
                )
            }
        }
    }
    
    fun loadNotificationHistory(userId: String) {
        viewModelScope.launch {
            try {
                val history = whatsAppRepository.getNotificationHistory(userId)
                    .getOrNull().orEmpty()
                _notificationHistory.value = history
            } catch (e: Exception) {
                // Handle error silently or update UI state
            }
        }
    }
    
    fun retryFailedMessage(messageSid: String) {
        viewModelScope.launch {
            try {
                val result = whatsAppRepository.retryFailedMessage(messageSid)
                
                if (result.isSuccess) {
                    _uiState.value = WhatsAppNotificationUiState.Success(
                        message = "Message retry sent successfully",
                        messageSid = result.getOrNull()?.messageSid
                    )
                } else {
                    _uiState.value = WhatsAppNotificationUiState.Error(
                        "Failed to retry message"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = WhatsAppNotificationUiState.Error(
                    "Error retrying message: ${e.message}"
                )
            }
        }
    }
    
    fun clearState() {
        _uiState.value = WhatsAppNotificationUiState.Idle
    }
}

// UI State
sealed class WhatsAppNotificationUiState {
    object Idle : WhatsAppNotificationUiState()
    object Sending : WhatsAppNotificationUiState()
    data class Success(val message: String, val messageSid: String?) : WhatsAppNotificationUiState()
    data class Error(val message: String) : WhatsAppNotificationUiState()
}
