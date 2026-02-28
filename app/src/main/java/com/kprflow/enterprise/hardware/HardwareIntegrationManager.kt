package com.kprflow.enterprise.hardware

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.camera.view.PreviewView
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardware Integration Manager - Complete Hardware Integration
 * Phase Sensor & Hardware Integration: Unified Hardware Management
 */
@Singleton
class HardwareIntegrationManager @Inject constructor(
    private val cameraXManager: CameraXManager,
    private val locationService: LocationService,
    private val ocrEngine: OCREngine
) {
    
    /**
     * Initialize all hardware services
     */
    fun initializeHardware(context: Context) {
        try {
            // Initialize camera
            cameraXManager.initializeCamera(context, context as LifecycleOwner, PreviewView(context))
            
            // Initialize location service
            locationService.initializeLocationService(context)
            
            // Initialize OCR engine
            ocrEngine.initializeOCR(context)
            
        } catch (exc: Exception) {
            // Handle initialization errors
        }
    }
    
    /**
     * Check all hardware permissions
     */
    fun checkAllPermissions(context: Context): HardwarePermissions {
        return HardwarePermissions(
            cameraPermission = cameraXManager.checkCameraPermission(context),
            locationPermission = locationService.checkLocationPermissions(context),
            gpsEnabled = locationService.isGPSEnabled(context)
        )
    }
    
    /**
     * Get combined hardware state
     */
    fun getHardwareState(): StateFlow<HardwareState> {
        return combine(
            cameraXManager.cameraState,
            locationService.locationState,
            ocrEngine.ocrState
        ) { cameraState, locationState, ocrState ->
            HardwareState(
                cameraState = cameraState,
                locationState = locationState,
                ocrState = ocrState,
                isReady = cameraState is CameraXManager.CameraState.Ready &&
                        locationState is LocationService.LocationState.Ready &&
                        ocrState is OCREngine.OCRState.Ready
            )
        }
    }
    
    /**
     * Capture and process document
     */
    suspend fun captureAndProcessDocument(
        context: Context,
        documentType: OCREngine.DocumentType
    ): DocumentProcessingResult {
        return try {
            // Capture image
            cameraXManager.captureImage(context)
            val imagePath = cameraXManager.capturedImage.value
            
            if (imagePath == null) {
                return DocumentProcessingResult(
                    success = false,
                    error = "Failed to capture image",
                    extractedData = emptyMap()
                )
            }
            
            // Process with OCR
            val imageFile = java.io.File(imagePath)
            val ocrResult = ocrEngine.extractTextFromFile(imageFile)
            
            if (ocrResult.error != null) {
                return DocumentProcessingResult(
                    success = false,
                    error = ocrResult.error,
                    extractedData = emptyMap()
                )
            }
            
            // Extract structured data
            val structuredData = ocrEngine.extractStructuredData(ocrResult, documentType)
            
            DocumentProcessingResult(
                success = true,
                error = null,
                extractedData = structuredData,
                ocrResult = ocrResult
            )
            
        } catch (exc: Exception) {
            DocumentProcessingResult(
                success = false,
                error = "Document processing failed: ${exc.message}",
                extractedData = emptyMap()
            )
        }
    }
    
    /**
     * Get current location with document context
     */
    suspend fun getLocationWithDocumentContext(context: Context): LocationDocumentResult {
        return try {
            val location = locationService.getCurrentLocation(context)
            
            if (location == null) {
                return LocationDocumentResult(
                    success = false,
                    error = "Failed to get location",
                    location = null,
                    context = emptyMap()
                )
            }
            
            // Create location context for document verification
            val locationContext = mapOf(
                "latitude" to location.latitude.toString(),
                "longitude" to location.longitude.toString(),
                "accuracy" to location.accuracy.toString(),
                "timestamp" to location.time.toString()
            )
            
            LocationDocumentResult(
                success = true,
                error = null,
                location = location,
                context = locationContext
            )
            
        } catch (exc: Exception) {
            LocationDocumentResult(
                success = false,
                error = "Location processing failed: ${exc.message}",
                location = null,
                context = emptyMap()
            )
        }
    }
    
    /**
     * Verify document at location (Phase 24 QC)
     */
    suspend fun verifyDocumentAtLocation(
        context: Context,
        documentData: Map<String, String>,
        targetLocation: android.location.Location,
        verificationRadius: Float = 100.0f
    ): VerificationResult {
        return try {
            // Get current location
            val currentLocation = locationService.getCurrentLocation(context)
            
            if (currentLocation == null) {
                return VerificationResult(
                    success = false,
                    error = "Unable to get current location",
                    locationVerified = false,
                    documentVerified = false
                )
            }
            
            // Check if within verification radius
            val isWithinRadius = locationService.isWithinGeofence(
                currentLocation,
                targetLocation,
                verificationRadius
            )
            
            // Verify document data completeness
            val documentVerified = verifyDocumentData(documentData)
            
            VerificationResult(
                success = isWithinRadius && documentVerified,
                error = null,
                locationVerified = isWithinRadius,
                documentVerified = documentVerified,
                currentLocation = currentLocation,
                targetLocation = targetLocation,
                distance = if (isWithinRadius) 0.0f else locationService.calculateDistance(currentLocation, targetLocation)
            )
            
        } catch (exc: Exception) {
            VerificationResult(
                success = false,
                error = "Verification failed: ${exc.message}",
                locationVerified = false,
                documentVerified = false
            )
        }
    }
    
    /**
     * Verify document data completeness
     */
    private fun verifyDocumentData(documentData: Map<String, String>): Boolean {
        // Check for required fields based on document type
        val requiredFields = listOf("name", "address")
        return requiredFields.all { field ->
            documentData.containsKey(field) && documentData[field]?.isNotEmpty() == true
        }
    }
    
    /**
     * Release all hardware resources
     */
    fun releaseHardware() {
        try {
            cameraXManager.releaseCamera()
            locationService.stopLocationUpdates()
            ocrEngine.clearOCRData()
        } catch (exc: Exception) {
            // Handle release errors
        }
    }
    
    /**
     * Get hardware status summary
     */
    fun getHardwareStatus(): HardwareStatus {
        val cameraState = cameraXManager.getCameraState()
        val locationState = locationService.getLocationState()
        val ocrState = ocrEngine.getOCRState()
        
        return HardwareStatus(
            cameraReady = cameraState is CameraXManager.CameraState.Ready,
            locationReady = locationState is LocationService.LocationState.Ready,
            ocrReady = ocrState is OCREngine.OCRState.Ready,
            overallReady = cameraState is CameraXManager.CameraState.Ready &&
                    locationState is LocationService.LocationState.Ready &&
                    ocrState is OCREngine.OCRState.Ready,
            errors = listOfNotNull(
                (cameraState as? CameraXManager.CameraState.Error)?.message,
                (locationState as? LocationService.LocationState.Error)?.message,
                (ocrState as? OCREngine.OCRState.Error)?.message
            )
        )
    }
}

/**
 * Hardware Permissions
 */
data class HardwarePermissions(
    val cameraPermission: Boolean,
    val locationPermission: Boolean,
    val gpsEnabled: Boolean
) {
    val allGranted: Boolean
        get() = cameraPermission && locationPermission && gpsEnabled
}

/**
 * Hardware State
 */
data class HardwareState(
    val cameraState: CameraXManager.CameraState,
    val locationState: LocationService.LocationState,
    val ocrState: OCREngine.OCRState,
    val isReady: Boolean
)

/**
 * Document Processing Result
 */
data class DocumentProcessingResult(
    val success: Boolean,
    val error: String?,
    val extractedData: Map<String, String>,
    val ocrResult: OCREngine.OCRResult? = null
)

/**
 * Location Document Result
 */
data class LocationDocumentResult(
    val success: Boolean,
    val error: String?,
    val location: android.location.Location?,
    val context: Map<String, String>
)

/**
 * Verification Result
 */
data class VerificationResult(
    val success: Boolean,
    val error: String?,
    val locationVerified: Boolean,
    val documentVerified: Boolean,
    val currentLocation: android.location.Location? = null,
    val targetLocation: android.location.Location? = null,
    val distance: Float? = null
)

/**
 * Hardware Status
 */
data class HardwareStatus(
    val cameraReady: Boolean,
    val locationReady: Boolean,
    val ocrReady: Boolean,
    val overallReady: Boolean,
    val errors: List<String>
)
