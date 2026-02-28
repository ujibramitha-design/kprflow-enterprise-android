package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.model.UserProfile
import com.kprflow.enterprise.domain.usecase.auth.SignInUseCase
import com.kprflow.enterprise.domain.usecase.auth.GetCurrentUserUseCase
import com.kprflow.enterprise.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<Resource<UserProfile>>(Resource.Loading)
    val loginState: StateFlow<Resource<UserProfile>> = _loginState.asStateFlow()
    
    private val _registerState = MutableStateFlow<Resource<UserProfile>>(Resource.Loading)
    val registerState: StateFlow<Resource<UserProfile>> = _registerState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    init {
        checkCurrentUser()
    }
    
    private fun checkCurrentUser() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            _currentUser.value = user
            _isLoggedIn.value = user != null
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading
            signInUseCase(email, password)
                .onSuccess { user ->
                    _loginState.value = Resource.Success(user)
                    _currentUser.value = user
                    _isLoggedIn.value = true
                }
                .onFailure { exception ->
                    _loginState.value = Resource.Error(
                        message = "Login failed: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    fun signUp(
        email: String,
        password: String,
        name: String,
        nik: String,
        phoneNumber: String,
        maritalStatus: String
    ) {
        viewModelScope.launch {
            _registerState.value = Resource.Loading
            authRepository.signUp(email, password, name, nik, phoneNumber, maritalStatus)
                .onSuccess { user ->
                    _registerState.value = Resource.Success(user)
                    _currentUser.value = user
                    _isLoggedIn.value = true
                }
                .onFailure { exception ->
                    _registerState.value = Resource.Error(
                        message = "Registration failed: ${exception.message}",
                        exception = exception
                    )
                }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
                .onSuccess {
                    _currentUser.value = null
                    _isLoggedIn.value = false
                    _loginState.value = Resource.Loading
                    _registerState.value = Resource.Loading
                }
                .onFailure { exception ->
                    // Handle sign out error
                }
        }
    }
    
    fun resetPassword(email: String) {
        viewModelScope.launch {
            authRepository.resetPassword(email)
                .onSuccess {
                    // Handle password reset success
                }
                .onFailure { exception ->
                    // Handle password reset error
                }
        }
    }
    
    fun updateProfile(userProfile: UserProfile) {
        viewModelScope.launch {
            authRepository.updateUserProfile(userProfile)
                .onSuccess { updatedProfile ->
                    _currentUser.value = updatedProfile
                }
                .onFailure { exception ->
                    // Handle update error
                }
        }
    }
    
    fun clearLoginState() {
        _loginState.value = Resource.Loading
    }
    
    fun clearRegisterState() {
        _registerState.value = Resource.Loading
    }
}
