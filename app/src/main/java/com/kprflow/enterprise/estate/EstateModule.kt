package com.kprflow.enterprise.estate

import android.content.Context
import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estate Module - Phase 24: Quality Control with Sensory Setup
 * Complete Estate Management with Camera, GPS, and Field Verification
 */
@Singleton
class EstateModule @Inject constructor(
    private val cameraManager: com.kprflow.enterprise.hardware.CameraXManager,
    private val locationService: com.kprflow.enterprise.hardware.LocationService,
    private val ocrEngine: com.kprflow.enterprise.hardware.OCREngine,
    private val hardwareIntegrationManager: com.kprflow.enterprise.hardware.HardwareIntegrationManager
) {
    
    private val _estateState = MutableStateFlow<EstateState>(EstateState.Idle)
    val estateState: StateFlow<EstateState> = _estateState.asStateFlow()
    
    private val _fieldVerification = MutableStateFlow<FieldVerification?>(null)
    val fieldVerification: StateFlow<FieldVerification?> = _fieldVerification.asStateFlow()
    
    private val _sensoryStatus = MutableStateFlow<SensoryStatus>(SensoryStatus.Initializing)
    val sensoryStatus: StateFlow<SensoryStatus> = _sensoryStatus.asStateFlow()
    
    /**
     * Initialize Estate Module with Sensory Setup
     */
    fun initializeEstateModule(context: Context) {
        try {
            _estateState.value = EstateState.Initializing
            _sensoryStatus.value = SensoryStatus.Initializing
            
            // Initialize hardware components
            hardwareIntegrationManager.initializeHardware(context)
            
            // Check sensory setup
            val sensoryCheck = performSensorySetup(context)
            _sensoryStatus.value = sensoryCheck
            
            if (sensoryCheck.isReady) {
                _estateState.value = EstateState.Ready
            } else {
                _estateState.value = EstateState.Error("Sensory setup incomplete")
            }
            
        } catch (exc: Exception) {
            _estateState.value = EstateState.Error("Estate module initialization failed: ${exc.message}")
            _sensoryStatus.value = SensoryStatus.Error(exc.message)
        }
    }
    
    /**
     * Perform Sensory Setup - Camera & GPS Verification
     */
    private fun performSensorySetup(context: Context): SensoryStatus {
        return try {
            val permissions = hardwareIntegrationManager.checkAllPermissions(context)
            val cameraReady = cameraManager.getCameraState() is com.kprflow.enterprise.hardware.CameraXManager.CameraState.Ready
            val locationReady = locationService.getLocationState() is com.kprflow.enterprise.hardware.LocationService.LocationState.Ready
            val ocrReady = ocrEngine.getOCRState() is com.kprflow.enterprise.hardware.OCREngine.OCRState.Ready
            
            SensoryStatus(
                cameraReady = cameraReady && permissions.cameraPermission,
                gpsReady = locationReady && permissions.locationPermission && permissions.gpsEnabled,
                ocrReady = ocrReady,
                permissions = permissions,
                isReady = cameraReady && locationReady && ocrReady && permissions.allGranted,
                error = null
            )
        } catch (exc: Exception) {
            SensoryStatus(
                cameraReady = false,
                gpsReady = false,
                ocrReady = false,
                permissions = com.kprflow.enterprise.hardware.HardwarePermissions(false, false, false),
                isReady = false,
                error = exc.message
            )
        }
    }
    
    /**
     * Start Field Verification - Phase 24 Quality Control
     */
    suspend fun startFieldVerification(
        context: Context,
        propertyId: String,
        targetLocation: Location
    ): FieldVerificationResult {
        return try {
            _estateState.value = EstateState.Verifying
            
            // Step 1: Get current location
            val locationResult = hardwareIntegrationManager.getLocationWithDocumentContext(context)
            
            if (!locationResult.success) {
                return FieldVerificationResult(
                    success = false,
                    error = locationResult.error,
                    verification = null
                )
            }
            
            // Step 2: Capture property photo
            val captureResult = hardwareIntegrationManager.captureAndProcessDocument(
                context,
                com.kprflow.enterprise.hardware.OCREngine.DocumentType.KTP
            )
            
            if (!captureResult.success) {
                return FieldVerificationResult(
                    success = false,
                    error = captureResult.error,
                    verification = null
                )
            }
            
            // Step 3: Verify document at location
            val verificationResult = hardwareIntegrationManager.verifyDocumentAtLocation(
                context,
                captureResult.extractedData,
                targetLocation,
                100.0f // 100 meter radius
            )
            
            // Step 4: Create field verification record
            val verification = FieldVerification(
                propertyId = propertyId,
                timestamp = System.currentTimeMillis(),
                location = locationResult.location,
                documentData = captureResult.extractedData,
                photoPath = hardwareIntegrationManager.capturedImage.value,
                verificationResult = verificationResult,
                verified = verificationResult.success,
                verificationDetails = mapOf(
                    "distance_to_target" to "${verificationResult.distance ?: 0} meters",
                    "location_verified" to verificationResult.locationVerified.toString(),
                    "document_verified" to verificationResult.documentVerified.toString(),
                    "camera_status" to "CAPTURED",
                    "gps_status" to "ACTIVE",
                    "ocr_status" to "PROCESSED"
                )
            )
            
            _fieldVerification.value = verification
            _estateState.value = if (verification.verified) EstateState.Verified else EstateState.VerificationFailed
            
            FieldVerificationResult(
                success = verification.verified,
                error = null,
                verification = verification
            )
            
        } catch (exc: Exception) {
            _estateState.value = EstateState.Error("Field verification failed: ${exc.message}")
            FieldVerificationResult(
                success = false,
                error = exc.message,
                verification = null
            )
        }
    }
    
    /**
     * Get Estate Module Status
     */
    fun getEstateStatus(): EstateState = _estateState.value
    
    /**
     * Get Sensory Status
     */
    fun getSensoryStatus(): SensoryStatus = _sensoryStatus.value
    
    /**
     * Get Field Verification
     */
    fun getFieldVerification(): FieldVerification? = _fieldVerification.value
    
    /**
     * Clear Verification Data
     */
    fun clearVerificationData() {
        _fieldVerification.value = null
        _estateState.value = EstateState.Ready
    }
    
    /**
     * Generate Dummy Field Verification (for testing)
     */
    fun generateDummyFieldVerification(propertyId: String): FieldVerification {
        val dummyLocation = Location("dummy").apply {
            latitude = -6.2088
            longitude = 106.8456
            accuracy = 10.0f
            time = System.currentTimeMillis()
        }
        
        val dummyDocumentData = mapOf(
            "nik" to "3171051502950001",
            "name" to "JOHN DOE",
            "address" to "JL. TEUKU UMAR NO. 123, JAKARTA SELATAN",
            "birth_date" to "29-05-1995"
        )
        
        return FieldVerification(
            propertyId = propertyId,
            timestamp = System.currentTimeMillis(),
            location = dummyLocation,
            documentData = dummyDocumentData,
            photoPath = "/dummy/path/property_photo.jpg",
            verificationResult = com.kprflow.enterprise.hardware.VerificationResult(
                success = true,
                error = null,
                locationVerified = true,
                documentVerified = true,
                currentLocation = dummyLocation,
                targetLocation = dummyLocation,
                distance = 0.0f
            ),
            verified = true,
            verificationDetails = mapOf(
                "distance_to_target" to "0 meters",
                "location_verified" to "true",
                "document_verified" to "true",
                "camera_status" to "CAPTURED",
                "gps_status" to "ACTIVE",
                "ocr_status" to "PROCESSED",
                "dummy_data" to "true"
            )
        )
    }
}

/**
 * Estate State
 */
sealed class EstateState {
    object Idle : EstateState()
    object Initializing : EstateState()
    object Ready : EstateState()
    object Verifying : EstateState()
    object Verified : EstateState()
    object VerificationFailed : EstateState()
    data class Error(val message: String) : EstateState()
}

/**
 * Sensory Status
 */
data class SensoryStatus(
    val cameraReady: Boolean,
    val gpsReady: Boolean,
    val ocrReady: Boolean,
    val permissions: com.kprflow.enterprise.hardware.HardwarePermissions,
    val isReady: Boolean,
    val error: String?
)

/**
 * Field Verification
 */
data class FieldVerification(
    val propertyId: String,
    val timestamp: Long,
    val location: Location?,
    val documentData: Map<String, String>,
    val photoPath: String?,
    val verificationResult: com.kprflow.enterprise.hardware.VerificationResult,
    val verified: Boolean,
    val verificationDetails: Map<String, String>
)

/**
 * Field Verification Result
 */
data class FieldVerificationResult(
    val success: Boolean,
    val error: String?,
    val verification: FieldVerification?
)
