package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.model.SPRData
import com.kprflow.enterprise.data.model.WhatsAppMessage
import com.kprflow.enterprise.domain.usecase.whatsapp.ProcessWhatsAppSPRUseCase
import com.kprflow.enterprise.domain.usecase.whatsapp.MonitorWhatsAppMessagesUseCase
import com.kprflow.enterprise.domain.usecase.whatsapp.ActivateSPRUseCase
import com.kprflow.enterprise.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhatsAppSPRViewModel @Inject constructor(
    private val processWhatsAppSPRUseCase: ProcessWhatsAppSPRUseCase,
    private val monitorWhatsAppMessagesUseCase: MonitorWhatsAppMessagesUseCase,
    private val activateSPRUseCase: ActivateSPRUseCase
) : ViewModel() {
    
    private val _processState = MutableStateFlow<Resource<List<String>>>(Resource.Loading)
    val processState: StateFlow<Resource<List<String>>> = _processState.asStateFlow()
    
    private val _inactiveSPRs = MutableStateFlow<Resource<List<SPRData>>>(Resource.Loading)
    val inactiveSPRs: StateFlow<Resource<List<SPRData>>> = _inactiveSPRs.asStateFlow()
    
    private val _newMessages = MutableStateFlow<List<WhatsAppMessage>>(emptyList())
    val newMessages: StateFlow<List<WhatsAppMessage>> = _newMessages.asStateFlow()
    
    private val _activationState = MutableStateFlow<Resource<Unit>>(Resource.Loading)
    val activationState: StateFlow<Resource<Unit>> = _activationState.asStateFlow()
    
    fun processWhatsAppGroup(groupId: String) {
        viewModelScope.launch {
            _processState.value = Resource.Loading
            
            processWhatsAppSPRUseCase(groupId)
                .onSuccess { sprIds ->
                    _processState.value = Resource.Success(sprIds)
                }
                .onFailure { exception ->
                    _processState.value = Resource.Error(
                        message = "Failed to process WhatsApp messages: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    fun startMonitoring(groupId: String) {
        viewModelScope.launch {
            monitorWhatsAppMessagesUseCase(groupId).collect { message ->
                val currentMessages = _newMessages.value.toMutableList()
                currentMessages.add(0, message) // Add to top
                _newMessages.value = currentMessages.take(50) // Keep last 50 messages
            }
        }
    }
    
    fun activateSPR(sprId: String, userId: String) {
        viewModelScope.launch {
            _activationState.value = Resource.Loading
            
            activateSPRUseCase(sprId, userId)
                .onSuccess {
                    _activationState.value = Resource.Success(Unit)
                }
                .onFailure { exception ->
                    _activationState.value = Resource.Error(
                        message = "Failed to activate SPR: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    fun clearProcessState() {
        _processState.value = Resource.Loading
    }
    
    fun clearActivationState() {
        _activationState.value = Resource.Loading
    }
}
