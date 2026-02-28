package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.viewmodel.NikLoginViewModel
import com.kprflow.enterprise.ui.components.AccessibleButton
import com.kprflow.enterprise.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NikLoginScreen(
    onLoginSuccess: (String) -> Unit,
    onProfileCompletion: (String) -> Unit,
    viewModel: NikLoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var nik by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Selamat Datang",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Masuk menggunakan NIK Anda",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Login Form
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Login dengan NIK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = nik,
                    onValueChange = { 
                        if (it.length <= 16) {
                            nik = it.filter { char -> char.isDigit() }
                        }
                    },
                    label = { Text("Nomor Induk Kependudukan") },
                    placeholder = { Text("Masukkan 16 digit NIK") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null
                        )
                    },
                    supportingText = {
                        Text("Masukkan 16 digit NIK Anda")
                    },
                    isError = nik.length > 0 && nik.length != 16
                )
                
                if (nik.length > 0 && nik.length != 16) {
                    Text(
                        text = "NIK harus 16 digit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                AccessibleButton(
                    text = "Masuk",
                    onClick = { 
                        viewModel.loginWithNik(
                            NikLoginRequest(
                                nik = nik,
                                deviceInfo = DeviceInfo(
                                    deviceType = "mobile",
                                    appVersion = "1.0.0"
                                )
                            )
                        )
                    },
                    enabled = nik.length == 16,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Help Text
                Text(
                    text = "Belum punya akun? Sistem akan otomatis membuat akun baru untuk Anda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Features
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Keunggulan Sistem",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                FeatureItem(
                    icon = Icons.Default.CameraAlt,
                    title = "KTP Auto-Scan",
                    description = "Upload KTP dan data otomatis terisi"
                )
                
                FeatureItem(
                    icon = Icons.Default.Speed,
                    title = "Proses Cepat",
                    description = "Proses aplikasi KPR lebih cepat"
                )
                
                FeatureItem(
                    icon = Icons.Default.Security,
                    title = "Aman",
                    description = "Data Anda terenkripsi dan aman"
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Butuh bantuan?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            TextButton(
                onClick = { /* Handle help */ }
            ) {
                Text("Hubungi Support")
            }
        }
    }
    
    // Handle login result
    LaunchedEffect(uiState) {
        when (uiState) {
            is NikLoginUiState.Success -> {
                if (uiState.response.isNewUser || uiState.response.requiresCompletion) {
                    onProfileCompletion(uiState.response.userId!!)
                } else {
                    onLoginSuccess(uiState.response.userId!!)
                }
            }
            is NikLoginUiState.Error -> {
                // Handle error - show snackbar or dialog
            }
            else -> { /* No action needed */ }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NikLoginDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onLogin: (String) -> Unit
) {
    var nik by remember { mutableStateOf("") }
    
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Login dengan NIK")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Masukkan 16 digit NIK Anda untuk melanjutkan",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    OutlinedTextField(
                        value = nik,
                        onValueChange = { 
                            if (it.length <= 16) {
                                nik = it.filter { char -> char.isDigit() }
                            }
                        },
                        label = { Text("NIK") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("16 digit NIK")
                        }
                    )
                    
                    if (nik.length > 0 && nik.length != 16) {
                        Text(
                            text = "NIK harus 16 digit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        if (nik.length == 16) {
                            onLogin(nik)
                        }
                    },
                    enabled = nik.length == 16
                ) {
                    Text("Masuk")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        )
    }
}

sealed class NikLoginUiState {
    object Idle : NikLoginUiState()
    object Loading : NikLoginUiState()
    data class Success(val response: NikLoginResponse) : NikLoginUiState()
    data class Error(val message: String) : NikLoginUiState()
}
