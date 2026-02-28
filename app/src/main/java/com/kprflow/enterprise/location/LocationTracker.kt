package com.kprflow.enterprise.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(): Flow<LocationResult> = callbackFlow {
        if (!isLocationEnabled()) {
            trySend(LocationResult.Error("GPS tidak aktif"))
            close()
            return@callbackFlow
        }
        
        if (!hasLocationPermission()) {
            trySend(LocationResult.Error("Izin lokasi tidak diberikan"))
            close()
            return@callbackFlow
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(15000)
            .build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    if (location.accuracy <= 100f) { // Accuracy within 100 meters
                        trySend(LocationResult.Success(location))
                        close()
                    } else {
                        trySend(LocationResult.Error("Akurasi GPS rendah: ${location.accuracy}m"))
                    }
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            trySend(LocationResult.Error("Gagal mendapatkan lokasi: ${e.message}"))
            close()
        }
        
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? {
        return if (hasLocationPermission()) {
            try {
                fusedLocationClient.lastLocation
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}

sealed class LocationResult {
    data class Success(val location: Location) : LocationResult()
    data class Error(val message: String) : LocationResult()
    object Loading : LocationResult()
}

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val address: String? = null
) {
    fun toFormattedString(): String {
        return "Lat: ${String.format("%.6f", latitude)}, " +
                "Lng: ${String.format("%.6f", longitude)}, " +
                "Akurasi: ${accuracy.toInt()}m"
    }
}
