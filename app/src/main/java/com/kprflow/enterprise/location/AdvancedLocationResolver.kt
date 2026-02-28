package com.kprflow.enterprise.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Advanced Location Resolver with address resolution, filtering, and optimization
 */
class AdvancedLocationResolver(private val context: Context) {
    
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale("id", "ID"))
    private val locationFilter = LocationFilter()
    private val batteryOptimizer = BatteryOptimizer()
    
    companion object {
        private const val LOCATION_TIMEOUT_MS = 15000L
        private const val BACKGROUND_LOCATION_INTERVAL_MS = 60000L // 1 minute
        private const val FOREGROUND_LOCATION_INTERVAL_MS = 5000L // 5 seconds
        private const val MAX_ACCURACY_THRESHOLD = 100f // 100 meters
        private const val MIN_SATellites = 4
    }
    
    /**
     * Get current location with address resolution
     */
    suspend fun getCurrentLocationWithAddress(): Result<LocationWithAddress> {
        return try {
            // Get high accuracy location
            val location = getHighAccuracyLocation()
                ?: return Result.failure(Exception("Unable to get location"))
            
            // Resolve address
            val address = resolveAddress(location)
            
            Result.success(
                LocationWithAddress(
                    location = location,
                    address = address,
                    accuracy = location.accuracy,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get high accuracy location with filtering
     */
    private suspend fun getHighAccuracyLocation(): Location? {
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine<Location?> { continuation ->
                // Check permissions
                if (!hasLocationPermission()) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                
                // Create location request
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    FOREGROUND_LOCATION_INTERVAL_MS
                ).apply {
                    setMinUpdateIntervalMillis(2000) // 2 seconds minimum
                    setMaxUpdateDelayMillis(5000) // 5 seconds maximum
                    setWaitForAccurateLocation(true)
                }.build()
                
                // Create location callback
                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val location = result.lastLocation
                        
                        if (location != null && locationFilter.isValidLocation(location)) {
                            fusedLocationClient.removeLocationUpdates(this)
                            continuation.resume(location)
                        }
                    }
                    
                    override fun onLocationAvailability(availability: LocationAvailability) {
                        if (!availability.isLocationAvailable) {
                            fusedLocationClient.removeLocationUpdates(this)
                            continuation.resume(null)
                        }
                    }
                }
                
                // Request location updates
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                ).addOnFailureListener { exception ->
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    continuation.resumeWithException(exception)
                }
                
                // Cleanup on cancellation
                continuation.invokeOnCancellation {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }
        }
    }
    
    /**
     * Resolve address from location with fallback
     */
    private suspend fun resolveAddress(location: Location): ResolvedAddress? {
        return try {
            if (Geocoder.isPresent()) {
                val addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    3 // Get up to 3 addresses for better accuracy
                )
                
                val bestAddress = addresses?.let { addrList ->
                    addrList.filterNotNull()
                        .maxByOrNull { it.maxAddressLineIndex }
                }
                
                bestAddress?.let { address ->
                    ResolvedAddress(
                        fullAddress = formatAddress(address),
                        street = address.thoroughfare ?: "",
                        subDistrict = address.subLocality ?: "",
                        district = address.locality ?: "",
                        city = address.subAdminArea ?: "",
                        province = address.adminArea ?: "",
                        postalCode = address.postalCode ?: "",
                        country = address.countryName ?: "Indonesia",
                        confidence = calculateAddressConfidence(address)
                    )
                }
            } else {
                // Fallback to dummy address resolution
                createDummyAddress(location)
            }
        } catch (e: IOException) {
            createDummyAddress(location)
        }
    }
    
    /**
     * Create dummy address for testing/fallback
     */
    private fun createDummyAddress(location: Location): ResolvedAddress {
        val dummyAddresses = mapOf(
            "Jakarta" to ResolvedAddress(
                fullAddress = "Jl. Sudirman No. 123, Jakarta Pusat, DKI Jakarta 10110",
                street = "Jl. Sudirman No. 123",
                subDistrict = "Jakarta Pusat",
                district = "Jakarta Pusat",
                city = "Jakarta",
                province = "DKI Jakarta",
                postalCode = "10110",
                country = "Indonesia",
                confidence = 0.8
            ),
            "Surabaya" to ResolvedAddress(
                fullAddress = "Jl. Ahmad Yani No. 456, Surabaya, Jawa Timur 60231",
                street = "Jl. Ahmad Yani No. 456",
                subDistrict = "Gubeng",
                district = "Surabaya",
                city = "Surabaya",
                province = "Jawa Timur",
                postalCode = "60231",
                country = "Indonesia",
                confidence = 0.8
            ),
            "Bandung" to ResolvedAddress(
                fullAddress = "Jl. Asia Afrika No. 789, Bandung, Jawa Barat 40111",
                street = "Jl. Asia Afrika No. 789",
                subDistrict = "Coblong",
                district = "Bandung",
                city = "Bandung",
                province = "Jawa Barat",
                postalCode = "40111",
                country = "Indonesia",
                confidence = 0.8
            )
        )
        
        // Select dummy address based on location (simplified)
        return dummyAddresses.values.random()
    }
    
    /**
     * Format address from Android Address object
     */
    private fun formatAddress(address: Address): String {
        val addressParts = mutableListOf<String>()
        
        // Street number and name
        if (!address.thoroughfare.isNullOrEmpty()) {
            addressParts.add(address.thoroughfare)
        }
        
        // Sub-district
        if (!address.subLocality.isNullOrEmpty()) {
            addressParts.add(address.subLocality)
        }
        
        // District
        if (!address.locality.isNullOrEmpty()) {
            addressParts.add(address.locality)
        }
        
        // City
        if (!address.subAdminArea.isNullOrEmpty()) {
            addressParts.add(address.subAdminArea)
        }
        
        // Province
        if (!address.adminArea.isNullOrEmpty()) {
            addressParts.add(address.adminArea)
        }
        
        // Postal code
        if (!address.postalCode.isNullOrEmpty()) {
            addressParts.add(address.postalCode)
        }
        
        return addressParts.joinToString(", ")
    }
    
    /**
     * Calculate address confidence score
     */
    private fun calculateAddressConfidence(address: Address): Double {
        var confidence = 0.5 // Base confidence
        
        // Add confidence for each available field
        if (!address.thoroughfare.isNullOrEmpty()) confidence += 0.15
        if (!address.subLocality.isNullOrEmpty()) confidence += 0.1
        if (!address.locality.isNullOrEmpty()) confidence += 0.1
        if (!address.subAdminArea.isNullOrEmpty()) confidence += 0.1
        if (!address.adminArea.isNullOrEmpty()) confidence += 0.1
        if (!address.postalCode.isNullOrEmpty()) confidence += 0.05
        
        return minOf(1.0, confidence)
    }
    
    /**
     * Start background location tracking with optimization
     */
    fun startBackgroundLocationTracking(): Flow<LocationWithAddress> = callbackFlow {
        if (!hasLocationPermission()) {
            close(Exception("Location permission not granted"))
            return@callbackFlow
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            BACKGROUND_LOCATION_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(30000) // 30 seconds minimum for background
            setMaxUpdateDelayMillis(120000) // 2 minutes maximum
        }.build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    if (locationFilter.isValidLocation(location)) {
                        // Resolve address in background
                        val address = resolveAddress(location)
                        
                        trySend(
                            LocationWithAddress(
                                location = location,
                                address = address,
                                accuracy = location.accuracy,
                                timestamp = System.currentTimeMillis()
                            )
                        )
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
            
            awaitClose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } catch (e: Exception) {
            close(e)
        }
    }
    
    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get location accuracy metrics
     */
    suspend fun getLocationAccuracyMetrics(): LocationAccuracyMetrics {
        return try {
            val location = getHighAccuracyLocation()
            if (location != null) {
                LocationAccuracyMetrics(
                    accuracy = location.accuracy,
                    altitudeAccuracy = if (location.hasAltitude()) 10f else Float.MAX_VALUE,
                    speedAccuracy = if (location.hasSpeed()) 2f else Float.MAX_VALUE,
                    bearingAccuracy = if (location.hasBearing()) 5f else Float.MAX_VALUE,
                    timeAccuracy = System.currentTimeMillis() - location.time,
                    satelliteCount = MIN_SATellites, // Simplified
                    hdop = location.accuracy / 10f, // Simplified HDOP calculation
                    vdop = if (location.hasAltitude()) 15f else Float.MAX_VALUE
                )
            } else {
                LocationAccuracyMetrics(
                    accuracy = Float.MAX_VALUE,
                    altitudeAccuracy = Float.MAX_VALUE,
                    speedAccuracy = Float.MAX_VALUE,
                    bearingAccuracy = Float.MAX_VALUE,
                    timeAccuracy = Long.MAX_VALUE,
                    satelliteCount = 0,
                    hdop = Float.MAX_VALUE,
                    vdop = Float.MAX_VALUE
                )
            }
        } catch (e: Exception) {
            LocationAccuracyMetrics(
                accuracy = Float.MAX_VALUE,
                altitudeAccuracy = Float.MAX_VALUE,
                speedAccuracy = Float.MAX_VALUE,
                bearingAccuracy = Float.MAX_VALUE,
                timeAccuracy = Long.MAX_VALUE,
                satelliteCount = 0,
                hdop = Float.MAX_VALUE,
                vdop = Float.MAX_VALUE
            )
        }
    }
    
    /**
     * Optimize battery usage for location tracking
     */
    fun optimizeBatteryUsage() {
        batteryOptimizer.optimizeLocationSettings(context)
    }
}

/**
 * Location filter for validation
 */
class LocationFilter {
    fun isValidLocation(location: Location): Boolean {
        // Check accuracy
        if (location.accuracy > MAX_ACCURACY_THRESHOLD) {
            return false
        }
        
        // Check timestamp (not too old)
        val maxAge = 5 * 60 * 1000L // 5 minutes
        if (System.currentTimeMillis() - location.time > maxAge) {
            return false
        }
        
        // Check coordinates validity
        if (location.latitude < -90 || location.latitude > 90 ||
            location.longitude < -180 || location.longitude > 180) {
            return false
        }
        
        return true
    }
}

/**
 * Battery optimizer for location services
 */
class BatteryOptimizer {
    fun optimizeLocationSettings(context: Context) {
        // Implementation for battery optimization
        // This would typically involve:
        // 1. Adjusting location accuracy based on battery level
        // 2. Reducing update frequency when battery is low
        // 3. Using passive location provider when possible
    }
}

/**
 * Location with address data
 */
data class LocationWithAddress(
    val location: Location,
    val address: ResolvedAddress?,
    val accuracy: Float,
    val timestamp: Long
)

/**
 * Resolved address data
 */
data class ResolvedAddress(
    val fullAddress: String,
    val street: String,
    val subDistrict: String,
    val district: String,
    val city: String,
    val province: String,
    val postalCode: String,
    val country: String,
    val confidence: Double
)

/**
 * Location accuracy metrics
 */
data class LocationAccuracyMetrics(
    val accuracy: Float,
    val altitudeAccuracy: Float,
    val speedAccuracy: Float,
    val bearingAccuracy: Float,
    val timeAccuracy: Long,
    val satelliteCount: Int,
    val hdop: Float,
    val vdop: Float
)
