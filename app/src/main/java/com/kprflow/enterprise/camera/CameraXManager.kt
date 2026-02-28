package com.kprflow.enterprise.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX Manager for Estate Inspection
 * Live capture with GPS metadata integration
 */
class CameraXManager(
    private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private val _capturedImageUri = MutableStateFlow<Uri?>(null)
    val capturedImageUri: StateFlow<Uri?> = _capturedImageUri.asStateFlow()
    
    private val _cameraError = MutableStateFlow<String?>(null)
    val cameraError: StateFlow<String?> = _cameraError.asStateFlow()
    
    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()
    
    // GPS location for photo metadata
    private var currentLocation: com.kprflow.enterprise.data.model.LocationData? = null
    
    fun updateLocation(location: com.kprflow.enterprise.data.model.LocationData) {
        currentLocation = location
    }
    
    suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ): Boolean {
        return try {
            // Get camera provider
            cameraProvider = ProcessCameraProvider.getInstance(context).get()
            
            // Set up preview
            preview = Preview.Builder().build()
            preview?.setSurfaceProvider(previewView.surfaceProvider)
            
            // Set up image capture
            imageCapture = ImageCapture.Builder()
                .setJpegQuality(95)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            
            // Select back camera as default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // Unbind any previous use cases
            cameraProvider?.unbindAll()
            
            // Bind use cases to camera
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            
            _isCameraReady.value = true
            true
            
        } catch (e: Exception) {
            _cameraError.value = "Failed to initialize camera: ${e.message}"
            false
        }
    }
    
    fun capturePhoto(
        onPhotoCaptured: (Uri, com.kprflow.enterprise.data.model.LocationData) -> Unit,
        onError: (String) -> Unit
    ) {
        val imageCapture = imageCapture ?: run {
            onError("Camera not initialized")
            return
        }
        
        val currentLocation = currentLocation ?: run {
            onError("GPS location not available")
            return
        }
        
        // Create photo file with timestamp
        val photoFile = createPhotoFile()
        
        val metadata = ImageCapture.Metadata().apply {
            // Add GPS location metadata
            location = android.location.Location("GPS").apply {
                latitude = currentLocation.latitude
                longitude = currentLocation.longitude
                accuracy = currentLocation.accuracy.toFloat()
            }
            
            // Add additional metadata
            isReversedHorizontal = false
            isReversedVertical = false
        }
        
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()
        
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    _capturedImageUri.value = savedUri
                    onPhotoCaptured(savedUri, currentLocation)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    onError("Failed to capture photo: ${exception.message}")
                }
            }
        )
    }
    
    private fun createPhotoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir("inspection_photos")
        return File(storageDir, "inspection_${timestamp}.jpg").apply {
            parentFile?.mkdirs()
        }
    }
    
    fun release() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
    
    companion object {
        private const val TAG = "CameraXManager"
        
        fun hasCameraPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        fun requiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            } else {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }
}

/**
 * Camera Preview Composable
 */
@Composable
fun CameraPreview(
    cameraManager: CameraXManager,
    modifier: Modifier = Modifier,
    onCameraError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraError by cameraManager.cameraError.collectAsState()
    
    LaunchedEffect(cameraError) {
        cameraError?.let { error ->
            onCameraError(error)
        }
    }
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { previewView ->
            LaunchedEffect(Unit) {
                cameraManager.initializeCamera(lifecycleOwner, previewView)
            }
        }
    )
}

/**
 * Location-aware Photo Capture
 */
class LocationAwarePhotoCapture(
    private val cameraManager: CameraXManager,
    private val locationService: com.kprflow.enterprise.location.LocationService
) {
    
    suspend fun capturePhotoWithLocation(): Result<Pair<Uri, com.kprflow.enterprise.data.model.LocationData>> {
        return try {
            // Get current location
            val location = locationService.getCurrentLocation()
                ?: return Result.failure(Exception("Unable to get GPS location"))
            
            // Update camera manager with location
            cameraManager.updateLocation(location)
            
            // Capture photo and wait for result
            var capturedResult: Pair<Uri, com.kprflow.enterprise.data.model.LocationData>? = null
            var error: String? = null
            
            cameraManager.capturePhoto(
                onPhotoCaptured = { uri, locationData ->
                    capturedResult = Pair(uri, locationData)
                },
                onError = { errorMessage ->
                    error = errorMessage
                }
            )
            
            // Wait for capture result (in real implementation, use proper coroutine flow)
            kotlinx.coroutines.delay(1000) // Simulate capture time
            
            capturedResult?.let { Result.success(it) }
                ?: Result.failure(Exception(error ?: "Photo capture failed"))
                
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun validatePhotoLocation(
        photoLocation: com.kprflow.enterprise.data.model.LocationData,
        expectedLocation: com.kprflow.enterprise.data.model.LocationData,
        maxDistanceMeters: Double = 50.0
    ): Boolean {
        return calculateDistance(photoLocation, expectedLocation) <= maxDistanceMeters
    }
    
    private fun calculateDistance(
        location1: com.kprflow.enterprise.data.model.LocationData,
        location2: com.kprflow.enterprise.data.model.LocationData
    ): Double {
        val lat1 = Math.toRadians(location1.latitude)
        val lon1 = Math.toRadians(location1.longitude)
        val lat2 = Math.toRadians(location2.latitude)
        val lon2 = Math.toRadians(location2.longitude)
        
        val dLat = lat2 - lat1
        val dLon = lon2 - lon1
        
        val a = Math.sin(dLat / 2).pow(2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLon / 2).pow(2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return 6371000 * c // Earth's radius in meters
    }
}

/**
 * Photo Metadata Extractor
 */
class PhotoMetadataExtractor {
    
    fun extractMetadata(imageUri: Uri): PhotoMetadata {
        // In real implementation, use ExifInterface to extract metadata
        return PhotoMetadata(
            uri = imageUri.toString(),
            timestamp = System.currentTimeMillis(),
            gpsLocation = null, // Extract from EXIF
            cameraInfo = null, // Extract from EXIF
            imageSize = null, // Get image dimensions
            fileSize = null   // Get file size
        )
    }
}

data class PhotoMetadata(
    val uri: String,
    val timestamp: Long,
    val gpsLocation: com.kprflow.enterprise.data.model.LocationData?,
    val cameraInfo: CameraInfo?,
    val imageSize: Pair<Int, Int>?,
    val fileSize: Long?
)

data class CameraInfo(
    val make: String?,
    val model: String?,
    val focalLength: Float?,
    val aperture: Float?,
    val iso: Int?,
    val exposureTime: Float?
)
