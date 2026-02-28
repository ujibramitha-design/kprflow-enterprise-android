package com.kprflow.enterprise.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Global UI State Manager - Centralized UI State Management
 * Phase Final: Global UI State Wrapper Implementation
 */
@Composable
fun GlobalUIStateManager(
    uiState: GlobalUIState,
    onDismissError: () -> Unit,
    onRetryAction: (String) -> Unit,
    onRefreshConnection: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content area
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Connection status indicator
            if (uiState.connectionState is ConnectionState.Disconnected) {
                ConnectionStatusBanner(
                    connectionState = uiState.connectionState,
                    onRefresh = onRefreshConnection,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            
            // Global snackbar container
            GlobalSnackbarContainer(
                snackbarState = uiState.snackbarState,
                onDismiss = onDismissError,
                onRetry = onRetryAction,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Loading overlay with Lottie
        if (uiState.isLoading) {
            LoadingOverlay(
                message = uiState.loadingMessage,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Error overlay with Lottie
        if (uiState.errorState is ErrorState.Critical) {
            CriticalErrorOverlay(
                errorState = uiState.errorState,
                onRetry = { onRetryAction("critical_error") },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Connection Status Banner
 */
@Composable
private fun ConnectionStatusBanner(
    connectionState: ConnectionState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, iconColor, textColor) = when (connectionState) {
        is ConnectionState.Disconnected -> {
            Triple(
                Color(0xFFFF6B6B),
                Color.White,
                Color.White
            )
        }
        is ConnectionState.Connecting -> {
            Triple(
                Color(0xFFFFA726),
                Color.White,
                Color.White
            )
        }
        is ConnectionState.Connected -> {
            Triple(
                Color(0xFF66BB6A),
                Color.White,
                Color.White
            )
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (connectionState) {
                    is ConnectionState.Disconnected -> {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "No Internet",
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Column {
                            Text(
                                text = "Koneksi Internet Hilang",
                                style = MaterialTheme.typography.titleSmall,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Upload otomatis akan dilanjutkan saat koneksi pulih",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.9f)
                            )
                        }
                    }
                    is ConnectionState.Connecting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = iconColor,
                            strokeWidth = 3.dp
                        )
                        
                        Text(
                            text = "Menghubungkan kembali...",
                            style = MaterialTheme.typography.titleSmall,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is ConnectionState.Connected -> {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Connected",
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Text(
                            text = "Koneksi Pulih",
                            style = MaterialTheme.typography.titleSmall,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            if (connectionState is ConnectionState.Disconnected) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Global Snackbar Container
 */
@Composable
private fun GlobalSnackbarContainer(
    snackbarState: SnackbarState,
    onDismiss: () -> Unit,
    onRetry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    AnimatedVisibility(
        visible = snackbarState.isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300, easing = EaseOutBack)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300, easing = EaseInBack)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = when (snackbarState.type) {
                    SnackbarType.ERROR -> Color(0xFFFF5252)
                    SnackbarType.WARNING -> Color(0xFFFF9800)
                    SnackbarType.SUCCESS -> Color(0xFF4CAF50)
                    SnackbarType.INFO -> Color(0xFF2196F3)
                }
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with icon and title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = getSnackbarIcon(snackbarState.type),
                            contentDescription = snackbarState.type.name,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Text(
                            text = snackbarState.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Message
                Text(
                    text = snackbarState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f),
                    lineHeight = 20.sp
                )
                
                // Action buttons if available
                if (snackbarState.actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        snackbarState.actions.forEach { action ->
                            Button(
                                onClick = {
                                    onRetry(action.id)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = action.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                // Progress indicator for auto-retry
                if (snackbarState.showProgress) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = { snackbarState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Color.White.copy(alpha = 0.8f),
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

/**
 * Loading Overlay with Lottie Animation
 */
@Composable
private fun LoadingOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lottie Animation placeholder (replace with actual Lottie)
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Critical Error Overlay with Lottie Animation
 */
@Composable
private fun CriticalErrorOverlay(
    errorState: ErrorState.Critical,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(20.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error icon (replace with Lottie animation)
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Critical Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = errorState.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = errorState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Coba Lagi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Helper functions
private fun getSnackbarIcon(type: SnackbarType): ImageVector {
    return when (type) {
        SnackbarType.ERROR -> Icons.Default.Error
        SnackbarType.WARNING -> Icons.Default.Warning
        SnackbarType.SUCCESS -> Icons.Default.CheckCircle
        SnackbarType.INFO -> Icons.Default.Info
    }
}

// Data classes
data class GlobalUIState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "Memuat...",
    val connectionState: ConnectionState = ConnectionState.Connected(),
    val snackbarState: SnackbarState = SnackbarState(),
    val errorState: ErrorState = ErrorState.None
)

sealed class ConnectionState {
    data class Connected(
        val type: ConnectionType = ConnectionType.WIFI,
        val strength: Int = 100
    ) : ConnectionState()
    
    data class Connecting(
        val attempt: Int = 1
    ) : ConnectionState()
    
    data class Disconnected(
        val reason: String = "No internet connection"
    ) : ConnectionState()
}

enum class ConnectionType {
    WIFI, MOBILE, ETHERNET
}

data class SnackbarState(
    val isVisible: Boolean = false,
    val type: SnackbarType = SnackbarType.INFO,
    val title: String = "",
    val message: String = "",
    val actions: List<SnackbarAction> = emptyList(),
    val showProgress: Boolean = false,
    val progress: Float = 0f,
    val autoDismiss: Boolean = true,
    val duration: Long = 4000L
)

enum class SnackbarType {
    ERROR, WARNING, SUCCESS, INFO
}

data class SnackbarAction(
    val id: String,
    val label: String,
    val action: () -> Unit = {}
)

sealed class ErrorState {
    object None : ErrorState()
    object NetworkError : ErrorState()
    object PermissionDenied : ErrorState()
    object TokenExpired : ErrorState()
    data class Critical(
        val title: String,
        val message: String,
        val errorCode: String? = null
    ) : ErrorState()
}

// Predefined error states
object ErrorStates {
    val NetworkError = ErrorState.NetworkError
    val PermissionDenied = ErrorState.PermissionDenied
    val TokenExpired = ErrorState.TokenExpired
    val InternetDisconnected = SnackbarState(
        isVisible = true,
        type = SnackbarType.ERROR,
        title = "Koneksi Internet Hilang",
        message = "Upload otomatis akan dilanjutkan saat koneksi pulih",
        actions = listOf(
            SnackbarAction("retry", "Coba Lagi"),
            SnackbarAction("offline", "Mode Offline")
        ),
        showProgress = true,
        autoDismiss = false
    )
    val WhatsAppTokenExpired = SnackbarState(
        isVisible = true,
        type = SnackbarType.WARNING,
        title = "Token WhatsApp Expired",
        message = "Token API WhatsApp Gateway telah kadaluarsa. Silakan refresh token.",
        actions = listOf(
            SnackbarAction("refresh_token", "Refresh Token")
        ),
        autoDismiss = false
    )
    val PermissionDenied = SnackbarState(
        isVisible = true,
        type = SnackbarType.ERROR,
        title = "Akses Ditolak",
        message = "Anda tidak memiliki izin untuk mengakses menu ini. Hubungi administrator.",
        actions = listOf(
            SnackbarAction("contact_admin", "Hubungi Admin")
        ),
        autoDismiss = false
    )
}
