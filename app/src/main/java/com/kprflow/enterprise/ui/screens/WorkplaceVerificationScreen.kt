package com.kprflow.enterprise.ui.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.viewmodel.WorkplaceVerificationViewModel
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkplaceVerificationScreen(
    dossierId: String,
    onBackClick: () -> Unit,
    onVerificationComplete: (String) -> Unit,
    viewModel: WorkplaceVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    LaunchedEffect(Unit) {
        viewModel.initializeCamera(context, lifecycleOwner)
    }
    
    LaunchedEffect(uiState.verificationComplete) {
        if (uiState.verificationComplete) {
            uiState.verificationId?.let { verificationId ->
                onVerificationComplete(verificationId)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Workplace Verification",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (uiState.location != null) {
                Text(
                    text = "📍 Location: ${String.format("%.6f", uiState.location.latitude)}, ${String.format("%.6f", uiState.location.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (uiState) {
            is WorkplaceVerificationUiState.Initializing -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Initializing Camera...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            is WorkplaceVerificationUiState.Ready -> {
                // Camera Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CameraPreview(
                        onCameraReady = viewModel::onCameraReady,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Capture Button
                CaptureButton(
                    onCapture = { viewModel.capturePhoto(dossierId) },
                    isLoading = uiState.isCapturing,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            
            is WorkplaceVerificationUiState.Capturing -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Capturing Photo...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            is WorkplaceVerificationUiState.Success -> {
                VerificationSuccessScreen(
                    verificationId = uiState.verificationId,
                    onContinue = { onVerificationComplete(it) },
                    onRetake = { viewModel.resetVerification() }
                )
            }
            
            is WorkplaceVerificationUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = { viewModel.retryInitialization(context, lifecycleOwner) }
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onCameraReady: (ImageCapture) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(ctx)
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                
                onCameraReady(imageCapture)
            } catch (e: Exception) {
                // Handle camera initialization error
            }
            
            previewView
        },
        modifier = modifier
    )
}

@Composable
private fun CaptureButton(
    onCapture: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onCapture,
        enabled = !isLoading,
        modifier = modifier.size(80.dp),
        shape = RoundedCornerShape(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 3.dp
            )
        } else {
            Text(
                text = "📷",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
private fun VerificationSuccessScreen(
    verificationId: String,
    onContinue: (String) -> Unit,
    onRetake: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✅",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Workplace Photo Captured!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Location and timestamp have been recorded",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onRetake
            ) {
                Text("Retake Photo")
            }
            
            Button(
                onClick = { onContinue(verificationId) }
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "❌",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Camera Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry
        ) {
            Text("Retry")
        }
    }
}

// UI State
sealed class WorkplaceVerificationUiState {
    object Initializing : WorkplaceVerificationUiState()
    object Ready : WorkplaceVerificationUiState()
    object Capturing : WorkplaceVerificationUiState()
    data class Success(val verificationId: String) : WorkplaceVerificationUiState()
    data class Error(val message: String) : WorkplaceVerificationUiState()
    
    val isCapturing: Boolean
        get() = this is Capturing
    
    val verificationComplete: Boolean
        get() = this is Success
    
    val verificationId: String?
        get() = (this as? Success)?.verificationId
}
