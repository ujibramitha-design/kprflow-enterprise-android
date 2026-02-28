package com.kprflow.enterprise.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.location.LocationData
import com.kprflow.enterprise.location.LocationResult
import com.kprflow.enterprise.location.LocationTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkplaceVerificationViewModel @Inject constructor(
    private val locationTracker: LocationTracker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorkplaceVerificationUiState())
    val uiState: StateFlow<WorkplaceVerificationUiState> = _uiState.asStateFlow()
    
    init {
        checkPermissions()
        startLocationTracking()
    }
    
    fun checkPermissions() {
        val hasCamera = checkCameraPermission()
        val hasLocation = locationTracker.hasLocationPermission()
        
        _uiState.value = _uiState.value.copy(
            hasPermissions = hasCamera && hasLocation
        )
    }
    
    fun requestPermissions(context: Context) {
        // This will be handled by PermissionWrapper composable
        checkPermissions()
    }
    
    private fun startLocationTracking() {
        if (!locationTracker.isLocationEnabled()) {
            _uiState.value = _uiState.value.copy(
                locationError = "GPS tidak aktif. Silakan aktifkan GPS."
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGettingLocation = true)
            
            locationTracker.getCurrentLocation().collect { result ->
                when (result) {
                    is LocationResult.Success -> {
                        val locationData = LocationData(
                            latitude = result.location.latitude,
                            longitude = result.location.longitude,
                            accuracy = result.location.accuracy,
                            timestamp = result.location.time
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            locationData = locationData,
                            isGettingLocation = false,
                            locationError = null
                        )
                    }
                    is LocationResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            locationError = result.message,
                            isGettingLocation = false
                        )
                    }
                    else -> {
                        // Loading state handled by isGettingLocation
                    }
                }
            }
        }
    }
    
    fun setPhotoUri(uri: Uri) {
        _uiState.value = _uiState.value.copy(photoUri = uri)
    }
    
    fun capturePhoto() {
        _uiState.value = _uiState.value.copy(isCapturing = true)
    }
    
    fun completeVerification() {
        val locationData = _uiState.value.locationData
        val photoUri = _uiState.value.photoUri
        
        if (locationData != null && photoUri != null) {
            _uiState.value = _uiState.value.copy(
                isVerificationComplete = true,
                isCapturing = false
            )
        }
    }
    
    fun reset() {
        _uiState.value = WorkplaceVerificationUiState()
        startLocationTracking()
    }
}

data class WorkplaceVerificationUiState(
    val hasPermissions: Boolean = false,
    val locationData: LocationData? = null,
    val photoUri: Uri? = null,
    val isGettingLocation: Boolean = false,
    val isCapturing: Boolean = false,
    val isVerificationComplete: Boolean = false,
    val locationError: String? = null
)
