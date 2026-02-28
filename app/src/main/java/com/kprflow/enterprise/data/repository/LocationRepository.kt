package com.kprflow.enterprise.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val context: Context
) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    suspend fun getCurrentLocation(): Result<Location> {
        return try {
            if (!hasLocationPermission()) {
                return Result.failure(SecurityException("Location permission not granted"))
            }
            
            val location = suspendCancellableCoroutine<Location> { continuation ->
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener { exception ->
                    continuation.resumeWith(Result.failure(exception))
                }
                
                continuation.invokeOnCancellation {
                    // Cancel location request if coroutine is cancelled
                }
            }
            
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLastKnownLocation(): Result<Location?> {
        return try {
            if (!hasLocationPermission()) {
                return Result.failure(SecurityException("Location permission not granted"))
            }
            
            val location = fusedLocationClient.lastLocation.await()
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    suspend fun getLocationWithTimeout(timeoutMs: Long = 10000L): Result<Location> {
        return try {
            // Try to get current location first
            val currentLocation = getCurrentLocation()
            if (currentLocation.isSuccess) {
                return currentLocation
            }
            
            // Fallback to last known location
            val lastLocation = getLastKnownLocation()
            if (lastLocation.isSuccess && lastLocation.getOrNull() != null) {
                return Result.success(lastLocation.getOrNull()!!)
            }
            
            Result.failure(Exception("Unable to get location"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun formatLocation(location: Location): String {
        return "${location.latitude}, ${location.longitude}"
    }
    
    fun calculateDistance(
        lat1: Double, 
        lon1: Double, 
        lat2: Double, 
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    
    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val timestamp: Long,
        val provider: String?
    )
    
    fun Location.toLocationData(): LocationData {
        return LocationData(
            latitude = this.latitude,
            longitude = this.longitude,
            accuracy = this.accuracy,
            timestamp = this.time,
            provider = this.provider
        )
    }
}
