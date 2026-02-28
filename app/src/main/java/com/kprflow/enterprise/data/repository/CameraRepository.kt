package com.kprflow.enterprise.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraRepository @Inject constructor(
    private val context: Context
) {
    
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    suspend fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        onImageCaptureReady: (ImageCapture) -> Unit
    ): Result<Unit> {
        return try {
            val provider = ProcessCameraProvider.getInstance(context)
            cameraProvider = provider
            
            val imageCaptureUseCase = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            
            this.imageCapture = imageCaptureUseCase
            
            // Unbind any previous use cases
            provider.unbindAll()
            
            // Bind use cases to camera
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                imageCaptureUseCase
            )
            
            onImageCaptureReady(imageCaptureUseCase)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun captureImage(
        location: com.kprflow.enterprise.data.repository.LocationRepository.LocationData? = null
    ): Result<CapturedImage> {
        return try {
            val imageCapture = this.imageCapture
                ?: return Result.failure(IllegalStateException("Camera not setup"))
            
            val result = suspendCancellableCoroutine<CapturedImage> { continuation ->
                val photoFile = createImageFile()
                
                val metadata = ImageCapture.Metadata().apply {
                    location?.let {
                        // Add location metadata
                        // Note: CameraX doesn't directly support location metadata
                        // We'll add it to the file name and store separately
                    }
                }
                
                val outputOptions = ImageCapture.OutputFileOptions
                    .Builder(photoFile)
                    .setMetadata(metadata)
                    .build()
                
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val capturedImage = CapturedImage(
                                file = photoFile,
                                uri = Uri.fromFile(photoFile),
                                location = location,
                                timestamp = System.currentTimeMillis()
                            )
                            continuation.resume(capturedImage)
                        }
                        
                        override fun onError(exception: ImageCaptureException) {
                            continuation.resumeWith(Result.failure(exception))
                        }
                    }
                )
                
                continuation.invokeOnCancellation {
                    // Cancel capture if coroutine is cancelled
                }
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "KPRFlow_${timeStamp}_"
        val storageDir = File(context.getExternalFilesDir(null), "KPRFlow/Images")
        
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
    
    fun rotateBitmapIfNeeded(bitmap: Bitmap, imageFile: File): Bitmap {
        return try {
            val exif = ExifInterface(imageFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            
            rotatedBitmap
        } catch (e: IOException) {
            bitmap
        }
    }
    
    fun compressBitmap(bitmap: Bitmap, quality: Int = 85): ByteArray {
        return try {
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            byteArrayOf()
        }
    }
    
    suspend fun saveBitmapToFile(bitmap: Bitmap, file: File): Result<File> {
        return try {
            val compressedBytes = compressBitmap(bitmap)
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(compressedBytes)
            fileOutputStream.close()
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getBitmapFromFile(file: File): Result<Bitmap> {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(Exception("Failed to decode bitmap"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun cleanup() {
        cameraProvider?.unbindAll()
        imageCapture = null
        cameraProvider = null
    }
    
    data class CapturedImage(
        val file: File,
        val uri: Uri,
        val location: com.kprflow.enterprise.data.repository.LocationRepository.LocationData?,
        val timestamp: Long
    )
    
    data class WorkplaceVerification(
        val imageFile: File,
        val location: com.kprflow.enterprise.data.repository.LocationRepository.LocationData,
        val timestamp: Long,
        val verificationId: String,
        val dossierId: String
    )
}
