package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Network Aware Screen - Wrapper for network-dependent screens
 * Phase Final: Global UI State Wrapper Implementation
 */
@Composable
fun NetworkAwareScreen(
    globalUIViewModel: GlobalUIViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val uiState by globalUIViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Handle network state changes
    LaunchedEffect(uiState.connectionState) {
        when (uiState.connectionState) {
            is ConnectionState.Disconnected -> {
                // Show internet disconnected snackbar
                globalUIViewModel.showSnackbar(
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
            }
            is ConnectionState.Connected -> {
                // Handle reconnection
                if (uiState.snackbarState.title == "Koneksi Internet Hilang") {
                    globalUIViewModel.handleSuccess(
                        title = "Koneksi Pulih",
                        message = "Upload otomatis akan dilanjutkan"
                    )
                }
            }
            else -> {
                // Handle other states
            }
        }
    }
    
    // Main content
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Global UI State Manager
        GlobalUIStateManager(
            uiState = uiState,
            onDismissError = { globalUIViewModel.dismissSnackbar() },
            onRetryAction = { actionId -> globalUIViewModel.handleSnackbarAction(actionId) },
            onRefreshConnection = { globalUIViewModel.refreshConnection() }
        )
        
        // Screen content
        content()
    }
}

/**
 * Permission Aware Screen - Wrapper for permission-dependent screens
 */
@Composable
fun PermissionAwareScreen(
    requiredPermission: String,
    globalUIViewModel: GlobalUIViewModel = hiltViewModel(),
    content: @Composable (hasPermission: Boolean) -> Unit
) {
    var hasPermission by remember { mutableStateOf(false) }
    
    // Check permission
    LaunchedEffect(requiredPermission) {
        // Check permission logic here
        // hasPermission = checkPermission(requiredPermission)
        
        if (!hasPermission) {
            globalUIViewModel.showSnackbar(
                type = SnackbarType.ERROR,
                title = "Akses Ditolak",
                message = "Anda tidak memiliki izin untuk mengakses menu ini. Hubungi administrator.",
                actions = listOf(
                    SnackbarAction("contact_admin", "Hubungi Admin")
                ),
                autoDismiss = false
            )
        }
    }
    
    // Screen content
    content(hasPermission)
}

/**
 * Token Aware Screen - Wrapper for token-dependent screens
 */
@Composable
fun TokenAwareScreen(
    globalUIViewModel: GlobalUIViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Handle token expiration
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycleScope.launch {
            // Observe token expiration
            // If token expires, show warning
        }
    }
    
    // Main content
    NetworkAwareScreen(
        globalUIViewModel = globalUIViewModel
    ) {
        content()
    }
}

/**
 * Estate Photo Upload Screen - Specific implementation for Estate team
 */
@Composable
fun EstatePhotoUploadScreen(
    globalUIViewModel: GlobalUIViewModel = hiltViewModel(),
    onUploadPhoto: (ByteArray) -> Unit
) {
    val uiState by globalUIViewModel.uiState.collectAsStateWithLifecycle()
    
    TokenAwareScreen(
        globalUIViewModel = globalUIViewModel
    ) {
        PermissionAwareScreen(
            requiredPermission = "android.permission.CAMERA"
        ) { hasPermission ->
            if (hasPermission) {
                // Photo upload UI
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Your photo upload UI here
                    
                    // Handle upload with network awareness
                    when (uiState.connectionState) {
                        is ConnectionState.Disconnected -> {
                            // Show offline upload UI
                            OfflineUploadUI(
                                onUploadPhoto = { photoData ->
                                    // Queue photo for upload when connection is restored
                                    globalUIViewModel.showSnackbar(
                                        type = SnackbarType.INFO,
                                        title = "Photo Ditambahkan ke Antrian",
                                        message = "Photo akan diupload otomatis saat koneksi pulih",
                                        autoDismiss = true
                                    )
                                }
                            )
                        }
                        is ConnectionState.Connected -> {
                            // Show online upload UI
                            OnlineUploadUI(
                                onUploadPhoto = onUploadPhoto
                            )
                        }
                        else -> {
                            // Show connecting UI
                            ConnectingUploadUI()
                        }
                    }
                }
            } else {
                // Show permission denied UI
                PermissionDeniedUI()
            }
        }
    }
}

/**
 * WhatsApp Token Refresh Screen - Specific implementation for WhatsApp features
 */
@Composable
fun WhatsAppTokenRefreshScreen(
    globalUIViewModel: GlobalUIViewModel = hiltViewModel(),
    onTokenRefreshed: () -> Unit
) {
    val uiState by globalUIViewModel.uiState.collectAsStateWithLifecycle()
    
    TokenAwareScreen(
        globalUIViewModel = globalUIViewModel
    ) {
        // WhatsApp UI with token awareness
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Your WhatsApp UI here
            
            // Check if token is expired
            if (uiState.snackbarState.title == "Token WhatsApp Expired") {
                // Show token refresh UI
                TokenRefreshUI(
                    onRefresh = {
                        globalUIViewModel.handleSnackbarAction("refresh_token")
                        onTokenRefreshed()
                    }
                )
            }
        }
    }
}

/**
 * Role-Based Access Screen - Specific implementation for role-based access
 */
@Composable
fun RoleBasedAccessScreen(
    requiredRole: String,
    globalUIViewModel: GlobalUIViewModel = hiltViewModel(),
    content: @Composable (hasAccess: Boolean) -> Unit
) {
    var hasAccess by remember { mutableStateOf(false) }
    
    // Check role access
    LaunchedEffect(requiredRole) {
        // Check user role logic here
        // hasAccess = checkRoleAccess(requiredRole)
        
        if (!hasAccess) {
            globalUIViewModel.showSnackbar(
                type = SnackbarType.ERROR,
                title = "Akses Ditolak",
                message = "Anda tidak memiliki izin untuk mengakses menu ini. Hubungi administrator.",
                actions = listOf(
                    SnackbarAction("contact_admin", "Hubungi Admin")
                ),
                autoDismiss = false
            )
        }
    }
    
    // Screen content
    content(hasAccess)
}

// Helper UI components
@Composable
private fun OfflineUploadUI(
    onUploadPhoto: (ByteArray) -> Unit
) {
    // Offline upload UI implementation
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Mode Offline",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Photo akan diupload otomatis saat koneksi pulih",
                style = MaterialTheme.typography.bodyMedium
            )
            // Add photo upload button here
        }
    }
}

@Composable
private fun OnlineUploadUI(
    onUploadPhoto: (ByteArray) -> Unit
) {
    // Online upload UI implementation
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Upload Photo",
                style = MaterialTheme.typography.titleMedium
            )
            // Add photo upload button here
        }
    }
}

@Composable
private fun ConnectingUploadUI() {
    // Connecting UI implementation
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Menghubungkan...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PermissionDeniedUI() {
    // Permission denied UI implementation
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Permission Denied",
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Izin Diperlukan",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Hubungi administrator untuk mendapatkan izin akses",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TokenRefreshUI(
    onRefresh: () -> Unit
) {
    // Token refresh UI implementation
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Token Expired",
                tint = MaterialTheme.colorScheme.warning
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Token WhatsApp Expired",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Refresh token untuk melanjutkan",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRefresh
            ) {
                Text("Refresh Token")
            }
        }
    }
}
