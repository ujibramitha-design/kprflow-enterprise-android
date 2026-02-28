package com.kprflow.enterprise.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWrapper(
    onPermissionsGranted: () -> Unit,
    content: @Composable (requestPermissions: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    var showRationale by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }
    
    // Check if any permission is permanently denied
    val permanentlyDenied = permissionsState.permissions.any { 
        !it.status.isGranted && !it.status.shouldShowRationale 
    }
    
    if (permanentlyDenied) {
        PermissionSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onOpenSettings = {
                openAppSettings(context)
                showSettingsDialog = false
            }
        )
    } else if (permissionsState.shouldShowRationale) {
        PermissionRationaleDialog(
            onDismiss = { showRationale = false },
            onGrantPermissions = {
                permissionsState.launchMultiplePermissionRequest()
                showRationale = false
            }
        )
    }
    
    content {
        if (!permissionsState.allPermissionsGranted) {
            requestPermissions = {
                if (permissionsState.shouldShowRationale) {
                    showRationale = true
                } else {
                    permissionsState.launchMultiplePermissionRequest()
                }
            }
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onGrantPermissions: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            androidx.compose.material3.Text("Izin Diperlukan")
        },
        text = {
            androidx.compose.material3.Text(
                "KPRFlow memerlukan izin kamera untuk mengambil foto tempat kerja " +
                "dan izin lokasi untuk memverifikasi lokasi survei bank. " +
                "Data Anda akan aman dan hanya digunakan untuk keperluan verifikasi KPR."
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onGrantPermissions
            ) {
                androidx.compose.material3.Text("Berikan Izin")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss
            ) {
                androidx.compose.material3.Text("Tolak")
            }
        }
    )
}

@Composable
fun PermissionSettingsDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            androidx.compose.material3.Text("Izin Diperlukan")
        },
        text = {
            androidx.compose.material3.Text(
                "KPRFlow memerlukan izin kamera dan lokasi untuk berfungsi. " +
                "Silakan buka Pengaturan dan aktifkan izin yang diperlukan."
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onOpenSettings
            ) {
                androidx.compose.material3.Text("Buka Pengaturan")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss
            ) {
                androidx.compose.material3.Text("Batal")
            }
        }
    )
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

@Composable
fun checkCameraPermission(): Boolean {
    val context = LocalContext.current
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun checkLocationPermission(): Boolean {
    val context = LocalContext.current
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}
