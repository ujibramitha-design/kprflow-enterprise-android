package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.kprflow.enterprise.ui.viewmodel.ProfileCompletionViewModel
import com.kprflow.enterprise.ui.components.AccessibleButton
import com.kprflow.enterprise.ui.components.StatusBadge
import com.kprflow.enterprise.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCompletionScreen(
    onProfileComplete: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: ProfileCompletionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val profileData by viewModel.profileData.collectAsState()
    val completionStatus by viewModel.completionStatus.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadProfileData()
        viewModel.calculateCompletionStatus()
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
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            
            Text(
                text = "Lengkapi Profil",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = { viewModel.refreshProfile() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Indicator
        ProfileProgressCard(completionStatus = completionStatus)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Profile Form
        when (uiState) {
            is ProfileCompletionUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is ProfileCompletionUiState.Success -> {
                ProfileForm(
                    profileData = profileData,
                    onProfileDataChange = { viewModel.updateProfileData(it) },
                    onSaveProfile = { viewModel.saveProfile() },
                    onProfileComplete = onProfileComplete
                )
            }
            
            is ProfileCompletionUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = { viewModel.loadProfileData() }
                )
            }
        }
    }
}

@Composable
private fun ProfileProgressCard(completionStatus: ProfileCompletionStatus) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progress Profil",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                StatusBadge(
                    status = "${completionStatus.completionPercentage}%",
                    color = when {
                        completionStatus.completionPercentage == 100 -> MaterialTheme.colorScheme.primary
                        completionStatus.completionPercentage >= 80 -> MaterialTheme.colorScheme.secondary
                        completionStatus.completionPercentage >= 50 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = completionStatus.completionPercentage / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${completionStatus.completedFields} dari ${completionStatus.totalFields} field dilengkapi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (completionStatus.missingFields.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Field yang belum diisi: ${completionStatus.missingFields.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ProfileForm(
    profileData: EnhancedUserProfile,
    onProfileDataChange: (EnhancedUserProfile) -> Unit,
    onSaveProfile: () -> Unit,
    onProfileComplete: () -> Unit
) {
    var name by remember { mutableStateOf(profileData.name ?: "") }
    var email by remember { mutableStateOf(profileData.email ?: "") }
    var phoneNumber by remember { mutableStateOf(profileData.phoneNumber ?: "") }
    var maritalStatus by remember { mutableStateOf(profileData.maritalStatus ?: "") }
    var birthPlaceDate by remember { mutableStateOf(profileData.birthPlaceDate ?: "") }
    var companyName by remember { mutableStateOf(profileData.companyName ?: "") }
    var position by remember { mutableStateOf(profileData.position ?: "") }
    var monthlyIncome by remember { mutableStateOf(profileData.monthlyIncome?.toString() ?: "") }
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Personal Information Section
        item {
            ProfileSectionCard(
                title = "Informasi Pribadi",
                icon = Icons.Default.Person
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            onProfileDataChange(profileData.copy(name = it.ifBlank { null }))
                        },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = profileData.name == null && profileData.profileCompletionPercentage < 100,
                        supportingText = if (profileData.name == null && profileData.profileCompletionPercentage < 100) {
                            { Text("Nama lengkap wajib diisi") }
                        } else null
                    )
                    
                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            onProfileDataChange(profileData.copy(email = it.ifBlank { null }))
                        },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        isError = profileData.email == null && profileData.profileCompletionPercentage < 100,
                        supportingText = if (profileData.email == null && profileData.profileCompletionPercentage < 100) {
                            { Text("Email wajib diisi") }
                        } else null
                    )
                    
                    // Phone Number Field
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { 
                            phoneNumber = it
                            onProfileDataChange(profileData.copy(phoneNumber = it.ifBlank { null }))
                        },
                        label = { Text("Nomor Telepon") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        isError = profileData.phoneNumber == null && profileData.profileCompletionPercentage < 100,
                        supportingText = if (profileData.phoneNumber == null && profileData.profileCompletionPercentage < 100) {
                            { Text("Nomor telepon wajib diisi") }
                        } else null
                    )
                    
                    // Marital Status Field
                    MaritalStatusDropdown(
                        selectedStatus = maritalStatus,
                        onStatusSelected = { 
                            maritalStatus = it
                            onProfileDataChange(profileData.copy(maritalStatus = it.ifBlank { null }))
                        },
                        isError = profileData.maritalStatus == null && profileData.profileCompletionPercentage < 100
                    )
                    
                    // Birth Place/Date Field
                    OutlinedTextField(
                        value = birthPlaceDate,
                        onValueChange = { 
                            birthPlaceDate = it
                            onProfileDataChange(profileData.copy(birthPlaceDate = it.ifBlank { null }))
                        },
                        label = { Text("Tempat/Tanggal Lahir") },
                        placeholder = { Text("Contoh: Jakarta/01-01-1990") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = profileData.birthPlaceDate == null && profileData.profileCompletionPercentage < 100,
                        supportingText = {
                            Text("Format: Tempat/DD-MM-YYYY")
                        }
                    )
                }
            }
        }
        
        // Employment Information Section
        item {
            ProfileSectionCard(
                title = "Informasi Pekerjaan",
                icon = Icons.Default.Work
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Job Category Field
                    JobCategoryDropdown(
                        selectedJobId = profileData.currentJobId,
                        onJobSelected = { jobId ->
                            onProfileDataChange(profileData.copy(currentJobId = jobId))
                        },
                        isError = profileData.currentJobId == null && profileData.profileCompletionPercentage < 100
                    )
                    
                    // Company Name Field
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { 
                            companyName = it
                            onProfileDataChange(profileData.copy(companyName = it.ifBlank { null }))
                        },
                        label = { Text("Nama Perusahaan") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = profileData.companyName == null && profileData.profileCompletionPercentage < 100,
                        supportingText = if (profileData.companyName == null && profileData.profileCompletionPercentage < 100) {
                            { Text("Nama perusahaan wajib diisi") }
                        } else null
                    )
                    
                    // Position Field
                    OutlinedTextField(
                        value = position,
                        onValueChange = { 
                            position = it
                            onProfileDataChange(profileData.copy(position = it.ifBlank { null }))
                        },
                        label = { Text("Posisi/Jabatan") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = profileData.position == null && profileData.profileCompletionPercentage < 100,
                        supportingText = if (profileData.position == null && profileData.profileCompletionPercentage < 100) {
                            { Text("Posisi/jabatan wajib diisi") }
                        } else null
                    )
                }
            }
        }
        
        // Income Information Section
        item {
            ProfileSectionCard(
                title = "Informasi Income",
                icon = Icons.Default.AccountBalance
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income Source Field
                    IncomeSourceDropdown(
                        selectedSourceId = profileData.incomeSourceId,
                        onSourceSelected = { sourceId ->
                            onProfileDataChange(profileData.copy(incomeSourceId = sourceId))
                        },
                        isError = profileData.incomeSourceId == null && profileData.profileCompletionPercentage < 100
                    )
                    
                    // Income Type Field
                    IncomeTypeDropdown(
                        selectedTypeId = profileData.incomeTypeId,
                        onTypeSelected = { typeId ->
                            onProfileDataChange(profileData.copy(incomeTypeId = typeId))
                        },
                        isError = profileData.incomeTypeId == null && profileData.profileCompletionPercentage < 100
                    )
                    
                    // Monthly Income Field
                    OutlinedTextField(
                        value = monthlyIncome,
                        onValueChange = { 
                            monthlyIncome = it
                            val income = it.toDoubleOrNull()?.let { java.math.BigDecimal(it) }
                            onProfileDataChange(profileData.copy(monthlyIncome = income))
                        },
                        label = { Text("Income Bulanan") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = profileData.monthlyIncome == null && profileData.profileCompletionPercentage < 100,
                        supportingText = {
                            Text("Masukkan jumlah income bulanan")
                        }
                    )
                }
            }
        }
        
        // KTP Verification Section
        item {
            KtpVerificationCard(
                profileData = profileData,
                onKtpUpload = { /* Handle KTP upload */ }
            )
        }
        
        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveProfile,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Simpan Draft")
                }
                
                Button(
                    onClick = onProfileComplete,
                    modifier = Modifier.weight(1f),
                    enabled = profileData.profileCompletionPercentage == 100
                ) {
                    Text("Lanjutkan")
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
private fun MaritalStatusDropdown(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedStatus,
            onValueChange = { },
            readOnly = true,
            label = { Text("Status Pernikahan") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            isError = isError,
            supportingText = if (isError) {
                { Text("Status pernikahan wajib diisi") }
            } else null
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf("Single", "Menikah", "Cerai", "Duda/Janda").forEach { status ->
                DropdownMenuItem(
                    text = { Text(status) },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun JobCategoryDropdown(
    selectedJobId: String?,
    onJobSelected: (String) -> Unit,
    isError: Boolean = false
) {
    // Implementation would load job categories from repository
    // For now, using hardcoded options
    var expanded by remember { mutableStateOf(false) }
    val jobCategories = listOf(
        "KARYAWAN" to "Karyawan",
        "WIRAUSAHA" to "Wirausaha"
    )
    
    val selectedJob = jobCategories.find { it.first == selectedJobId }?.second ?: ""
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedJob,
            onValueChange = { },
            readOnly = true,
            label = { Text("Kategori Pekerjaan") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            isError = isError,
            supportingText = if (isError) {
                { Text("Kategori pekerjaan wajib diisi") }
            } else null
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            jobCategories.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onJobSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun IncomeSourceDropdown(
    selectedSourceId: String?,
    onSourceSelected: (String) -> Unit,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val incomeSources = listOf(
        "CASH" to "Cash",
        "TRANSFER" to "Transfer Bank",
        "CEKGIRO" to "Cek/Giro",
        "DIGITAL" to "Digital Wallet",
        "LAINNYA" to "Lainnya"
    )
    
    val selectedSource = incomeSources.find { it.first == selectedSourceId }?.second ?: ""
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedSource,
            onValueChange = { },
            readOnly = true,
            label = { Text("Sumber Income") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            isError = isError,
            supportingText = if (isError) {
                { Text("Sumber income wajib diisi") }
            } else null
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            incomeSources.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSourceSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun IncomeTypeDropdown(
    selectedTypeId: String?,
    onTypeSelected: (String) -> Unit,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val incomeTypes = listOf(
        "PAYROLL" to "Payroll",
        "NONPAYROLL" to "Non-Payroll",
        "INVESTASI" to "Investasi",
        "LAINNYA" to "Lainnya"
    )
    
    val selectedType = incomeTypes.find { it.first == selectedTypeId }?.second ?: ""
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedType,
            onValueChange = { },
            readOnly = true,
            label = { Text("Jenis Income") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            isError = isError,
            supportingText = if (isError) {
                { Text("Jenis income wajib diisi") }
            } else null
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            incomeTypes.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onTypeSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun KtpVerificationCard(
    profileData: EnhancedUserProfile,
    onKtpUpload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Verifikasi KTP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                StatusBadge(
                    status = if (profileData.ktpVerified) "Terverifikasi" else "Belum",
                    color = if (profileData.ktpVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (profileData.ktpVerified) {
                Text(
                    text = "KTP Anda telah terverifikasi. Data berhasil diekstrak otomatis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (profileData.ktpExtractedData != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Confidence Score: ${profileData.ktpExtractedData.confidenceScore}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Upload KTP Anda untuk verifikasi otomatis. Sistem akan mengekstrak data secara otomatis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                AccessibleButton(
                    text = "Upload KTP",
                    onClick = onKtpUpload,
                    modifier = Modifier.fillMaxWidth()
                )
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
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Terjadi Kesalahan",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AccessibleButton(
            text = "Coba Lagi",
            onClick = onRetry
        )
    }
}

sealed class ProfileCompletionUiState {
    object Loading : ProfileCompletionUiState()
    data class Success(val profile: EnhancedUserProfile) : ProfileCompletionUiState()
    data class Error(val message: String) : ProfileCompletionUiState()
}
