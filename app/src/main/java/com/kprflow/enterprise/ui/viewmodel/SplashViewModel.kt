package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.model.UserRole
import com.kprflow.enterprise.data.repository.AuthRepository
import com.kprflow.enterprise.ui.screens.SplashState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _splashState = MutableStateFlow<SplashState>(SplashState.CheckingAuth)
    val splashState: StateFlow<SplashState> = _splashState.asStateFlow()
    
    init {
        checkAuthentication()
    }
    
    fun checkAuthentication() {
        viewModelScope.launch {
            _splashState.value = SplashState.CheckingAuth
            
            if (authRepository.isUserLoggedIn()) {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    _splashState.value = SplashState.NavigateToDashboard(currentUser.role)
                } else {
                    _splashState.value = SplashState.NavigateToLogin
                }
            } else {
                _splashState.value = SplashState.NavigateToLogin
            }
        }
    }
}
