package com.kprflow.enterprise.hardware

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CameraX Manager - Hardware Integration (Indra Penglihatan)
 * Phase Sensor & Hardware Integration: Complete Camera Implementation
 */
@Singleton
class CameraXManager @Inject constructor() {
    
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()
    
    private val _capturedImage = MutableStateFlow<String?>(null)
    val capturedImage: StateFlow<String?> = _capturedImage.asStateFlow()
    
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    
    /**
     * Check camera permissions
     */
    fun checkCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Initialize camera
     */
    fun initializeCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        try {
            _cameraState.value = CameraState.Initializing
            
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
                
                preview = Preview.Builder().build()
                preview?.setSurfaceProvider(previewView.surfaceProvider)
                
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    
                    _cameraState.value = CameraState.Ready
                    
                } catch (exc: Exception) {
                    _cameraState.value = CameraState.Error("Camera binding failed: ${exc.message}")
                }
                
            }, ContextCompat.getMainExecutor(context))
            
        } catch (exc: Exception) {
            _cameraState.value = CameraState.Error("Camera initialization failed: ${exc.message}")
        }
    }
    
    /**
     * Capture image
     */
    fun captureImage(context: Context) {
        val imageCapture = imageCapture ?: run {
            _cameraState.value = CameraState.Error("Camera not initialized")
            return
        }
        
        _cameraState.value = CameraState.Capturing
        
        val photoFile = File(
            getOutputDirectory(context),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )
        
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    _capturedImage.value = photoFile.absolutePath
                    _cameraState.value = CameraState.Ready
                }
                
                override fun onError(exception: ImageCaptureException) {
                    _cameraState.value = CameraState.Error("Photo capture failed: ${exception.message}")
                }
            }
        )
    }
    
    /**
     * Get output directory
     */
    private fun getOutputDirectory(context: Context): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, "KPRFlow/Camera").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }
    
    /**
     * Release camera
     */
    fun releaseCamera() {
        cameraProvider?.unbindAll()
        _cameraState.value = CameraState.Idle
    }
    
    /**
     * Get camera state
     */
    fun getCameraState(): CameraState = _cameraState.value
    
    /**
     * Clear captured image
     */
    fun clearCapturedImage() {
        _capturedImage.value = null
    }
}

/**
 * Camera State
 */
sealed class CameraState {
    object Idle : CameraState()
    object Initializing : CameraState()
    object Ready : CameraState()
    object Capturing : CameraState()
    data class Error(val message: String) : CameraState()
}
