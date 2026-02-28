package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.LocalizationRepository
import com.kprflow.enterprise.ui.screens.LanguageState
import com.kprflow.enterprise.ui.screens.LanguageSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageSettingsViewModel @Inject constructor(
    private val localizationRepository: LocalizationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LanguageSettingsUiState())
    val uiState: StateFlow<LanguageSettingsUiState> = _uiState.asStateFlow()
    
    private val _languagesState = MutableStateFlow<LanguageState<List<com.kprflow.enterprise.data.repository.LanguageInfo>>>(LanguageState.Loading)
    val languagesState: StateFlow<LanguageState<List<com.kprflow.enterprise.data.repository.LanguageInfo>>> = _languagesState.asStateState()
    
    private val _currentLanguageState = MutableStateFlow<LanguageState<com.kprflow.enterprise.data.repository.LanguageInfo>>(LanguageState.Loading)
    val currentLanguageState: StateFlow<LanguageState<com.kprflow.enterprise.data.repository.LanguageInfo>> = _currentLanguageState.asStateState()
    
    private val _currentUserId = MutableStateFlow<String?>(null)
    
    init {
        // TODO: Get current user from AuthRepository
        _currentUserId.value = "current-user-id" // Placeholder
    }
    
    fun loadLanguages() {
        viewModelScope.launch {
            _languagesState.value = LanguageState.Loading
            
            try {
                val languages = localizationRepository.getAllLanguages()
                    .getOrNull().orEmpty()
                
                _languagesState.value = LanguageState.Success(languages)
            } catch (e: Exception) {
                _languagesState.value = LanguageState.Error(e.message ?: "Failed to load languages")
            }
        }
    }
    
    fun loadCurrentLanguage() {
        viewModelScope.launch {
            _currentLanguageState.value = LanguageState.Loading
            
            val userId = _currentUserId.value ?: return@launch
            
            try {
                val userLanguage = localizationRepository.getUserLanguage(userId)
                    .getOrNull()
                
                val languages = when (val state = _languagesState.value) {
                    is LanguageState.Success -> state.data
                    else -> {
                        // Load languages if not already loaded
                        val langs = localizationRepository.getAllLanguages()
                            .getOrNull().orEmpty()
                        _languagesState.value = LanguageState.Success(langs)
                        langs
                    }
                }
                
                val currentLanguage = languages.find { it.code == userLanguage }
                    ?: languages.find { it.isDefault }
                    ?: languages.firstOrNull()
                
                if (currentLanguage != null) {
                    _currentLanguageState.value = LanguageState.Success(currentLanguage)
                } else {
                    _currentLanguageState.value = LanguageState.Error("Current language not found")
                }
            } catch (e: Exception) {
                _currentLanguageState.value = LanguageState.Error(e.message ?: "Failed to load current language")
            }
        }
    }
    
    fun selectLanguage(language: com.kprflow.enterprise.data.repository.LanguageInfo) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val userId = _currentUserId.value ?: return@launch
                
                val result = localizationRepository.setUserLanguage(userId, language.code)
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    
                    // Update current language state
                    _currentLanguageState.value = LanguageState.Success(language)
                    
                    // TODO: Notify app to refresh UI with new language
                    // This would typically involve updating a global language state
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to set language preference"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to set language preference: ${e.message}"
                )
            }
        }
    }
    
    fun addTranslation(
        key: String,
        language: String,
        value: String,
        category: String? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = localizationRepository.addTranslation(
                    key = key,
                    language = language,
                    value = value,
                    category = category
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    
                    // TODO: Show success message
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to add translation"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to add translation: ${e.message}"
                )
            }
        }
    }
    
    fun updateTranslation(
        key: String,
        language: String,
        value: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = localizationRepository.updateTranslation(
                    key = key,
                    language = language,
                    value = value
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    
                    // TODO: Show success message
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to update translation"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update translation: ${e.message}"
                )
            }
        }
    }
    
    fun deleteTranslation(
        key: String,
        language: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = localizationRepository.deleteTranslation(
                    key = key,
                    language = language
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    
                    // TODO: Show success message
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to delete translation"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to delete translation: ${e.message}"
                )
            }
        }
    }
    
    fun exportTranslations(format: String = "CSV") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val exportData = localizationRepository.exportTranslations(format)
                    .getOrNull()
                
                if (exportData != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    
                    // TODO: Handle export data (save to file, share, etc.)
                    // This would typically involve saving to device storage or sharing
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to export translations"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to export translations: ${e.message}"
                )
            }
        }
    }
    
    fun showTranslationManager() {
        _uiState.value = _uiState.value.copy(showTranslationManager = true)
    }
    
    fun hideTranslationManager() {
        _uiState.value = _uiState.value.copy(showTranslationManager = false)
    }
    
    fun showExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = true)
    }
    
    fun hideExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false)
    }
    
    fun refreshData() {
        loadLanguages()
        loadCurrentLanguage()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun getTranslation(
        key: String,
        language: String? = null,
        parameters: Map<String, String> = emptyMap()
    ): String {
        val targetLanguage = language ?: when (val state = _currentLanguageState.value) {
            is LanguageState.Success -> state.currentLanguage.code
            else -> com.kprflow.enterprise.data.repository.LocalizationRepository.DEFAULT_LANGUAGE
        }
        
        // Try to get translation from repository
        // For now, return the key as fallback
        return key
    }
    
    fun formatCurrency(
        amount: java.math.BigDecimal,
        language: String? = null
    ): String {
        val targetLanguage = language ?: when (val state = _currentLanguageState.value) {
            is LanguageState.Success -> state.currentLanguage.code
            else -> com.kprflow.enterprise.data.repository.LocalizationRepository.DEFAULT_LANGUAGE
        }
        
        return localizationRepository.formatCurrency(amount, targetLanguage)
    }
    
    fun formatDate(
        date: java.time.LocalDate,
        language: String? = null
    ): String {
        val targetLanguage = language ?: when (val state = _currentLanguageState.value) {
            is LanguageState.Success -> state.currentLanguage.code
            else -> com.kprflow.enterprise.data.repository.LocalizationRepository.DEFAULT_LANGUAGE
        }
        
        return localizationRepository.formatDate(date, targetLanguage)
    }
    
    fun formatDateTime(
        dateTime: java.time.LocalDateTime,
        language: String? = null
    ): String {
        val targetLanguage = language ?: when (val state = _currentLanguageState.value) {
            is LanguageState.Success -> state.currentLanguage.code
            else -> com.kprflow.enterprise.data.repository.LocalizationRepository.DEFAULT_LANGUAGE
        }
        
        return localizationRepository.formatDateTime(dateTime, targetLanguage)
    }
    
    fun isAdmin(): Boolean {
        // TODO: Check if current user is admin
        // This would typically check user role from AuthRepository
        return true // Placeholder
    }
    
    fun getTranslationStatistics() {
        viewModelScope.launch {
            try {
                val statistics = localizationRepository.getTranslationStatistics()
                    .getOrNull()
                
                // TODO: Handle statistics data
                // This could be used to show translation coverage metrics
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to load translation statistics")
            }
        }
    }
}
