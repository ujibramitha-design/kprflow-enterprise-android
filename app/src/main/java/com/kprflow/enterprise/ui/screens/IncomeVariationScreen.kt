package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.kprflow.enterprise.ui.viewmodel.IncomeVariationViewModel
import com.kprflow.enterprise.ui.components.AccessibleButton
import com.kprflow.enterprise.ui.components.StatusBadge
import com.kprflow.enterprise.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeVariationScreen(
    onBackClick: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: IncomeVariationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val incomeVariations by viewModel.incomeVariations.collectAsState()
    val customerVariations by viewModel.customerVariations.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadIncomeVariations()
        viewModel.loadCustomerVariations()
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
                text = "Detail Income",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = { viewModel.refreshData() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Job Category Info
        JobCategoryInfoCard(jobCategory = viewModel.currentJobCategory)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Income Variations
        when (uiState) {
            is IncomeVariationUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is IncomeVariationUiState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(incomeVariations) { variation ->
                        IncomeVariationCard(
                            variation = variation,
                            customerVariation = customerVariations.find { it.variationType.id == variation.id },
                            onSave = { variationData ->
                                viewModel.saveIncomeVariation(variationData)
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Total Income Summary
                        TotalIncomeSummaryCard(
                            totalIncome = viewModel.totalIncome,
                            verifiedCount = viewModel.verifiedCount
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.saveAsDraft() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Simpan Draft")
                            }
                            
                            Button(
                                onClick = onSaveComplete,
                                modifier = Modifier.weight(1f),
                                enabled = viewModel.isComplete
                            ) {
                                Text("Lanjutkan")
                            }
                        }
                    }
                }
            }
            
            is IncomeVariationUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = { viewModel.loadIncomeVariations() }
                )
            }
        }
    }
}

@Composable
private fun JobCategoryInfoCard(jobCategory: JobCategory?) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (jobCategory?.categoryCode) {
                    "KARYAWAN" -> Icons.Default.Work
                    "WIRAUSAHA" -> Icons.Default.Business
                    else -> Icons.Default.Person
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "Kategori Pekerjaan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = jobCategory?.categoryName ?: "Belum dipilih",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = jobCategory?.description ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IncomeVariationCard(
    variation: IncomeVariationType,
    customerVariation: CustomerIncomeVariation?,
    onSave: (CustomerIncomeVariation) -> Unit
) {
    var bankAccountNumber by remember { mutableStateOf(customerVariation?.bankAccountNumber ?: "") }
    var bankName by remember { mutableStateOf(customerVariation?.bankName ?: "") }
    var bankAccountStatus by remember { mutableStateOf(customerVariation?.bankAccountStatus ?: "ACTIVE") }
    var monthlyIncome by remember { mutableStateOf(customerVariation?.monthlyIncome?.toString() ?: "") }
    var additionalIncome by remember { mutableStateOf(customerVariation?.additionalIncome?.toString() ?: "") }
    var notes by remember { mutableStateOf(customerVariation?.notes ?: "") }
    
    var expanded by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = variation.variationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = variation.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            // Status Badge
            if (customerVariation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(
                        status = customerVariation.verificationStatus,
                        color = when (customerVariation.verificationStatus) {
                            "VERIFIED" -> MaterialTheme.colorScheme.primary
                            "PENDING" -> MaterialTheme.colorScheme.secondary
                            "REJECTED" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    
                    StatusBadge(
                        status = customerVariation.bankAccountStatus,
                        color = when (customerVariation.bankAccountStatus) {
                            "ACTIVE" -> MaterialTheme.colorScheme.primary
                            "INACTIVE" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                }
            }
            
            // Expanded Content
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bank Account Number
                    OutlinedTextField(
                        value = bankAccountNumber,
                        onValueChange = { bankAccountNumber = it },
                        label = { Text("Nomor Rekening") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    // Bank Name
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Nama Bank") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Bank Account Status
                    BankAccountStatusDropdown(
                        selectedStatus = bankAccountStatus,
                        onStatusSelected = { bankAccountStatus = it }
                    )
                    
                    // Monthly Income
                    OutlinedTextField(
                        value = monthlyIncome,
                        onValueChange = { monthlyIncome = it },
                        label = { Text("Income Bulanan") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("Masukkan jumlah income bulanan")
                        }
                    )
                    
                    // Additional Income
                    OutlinedTextField(
                        value = additionalIncome,
                        onValueChange = { additionalIncome = it },
                        label = { Text("Income Tambahan") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("Income tambahan (opsional)")
                        }
                    )
                    
                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    
                    // Save Button
                    AccessibleButton(
                        text = if (customerVariation == null) "Tambah Income" else "Update Income",
                        onClick = {
                            val variationData = CustomerIncomeVariation(
                                id = customerVariation?.id ?: "",
                                customerId = "", // Will be filled by ViewModel
                                variationType = variation,
                                bankAccountNumber = bankAccountNumber,
                                bankName = bankName,
                                bankAccountStatus = bankAccountStatus,
                                monthlyIncome = monthlyIncome.toDoubleOrNull()?.let { java.math.BigDecimal(it) },
                                additionalIncome = additionalIncome.toDoubleOrNull()?.let { java.math.BigDecimal(it) } ?: java.math.BigDecimal.ZERO,
                                incomeProofDocumentUrl = customerVariation?.incomeProofDocumentUrl,
                                verificationStatus = "PENDING",
                                verifiedBy = null,
                                verifiedAt = null,
                                notes = notes,
                                createdAt = customerVariation?.createdAt ?: "",
                                updatedAt = ""
                            )
                            onSave(variationData)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun BankAccountStatusDropdown(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit
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
            label = { Text("Status Rekening") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf("ACTIVE", "INACTIVE").forEach { status ->
                DropdownMenuItem(
                    text = { Text(if (status == "ACTIVE") "Aktif" else "Tidak Aktif") },
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
private fun TotalIncomeSummaryCard(
    totalIncome: java.math.BigDecimal,
    verifiedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Ringkasan Income",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Income Terverifikasi:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Rp ${String.format("%,.0f", totalIncome)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Jenis income yang terverifikasi: $verifiedCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

// Data Models
data class IncomeVariationType(
    val id: String,
    val variationName: String,
    val variationCode: String,
    val description: String,
    val isActive: Boolean = true
)

data class CustomerIncomeVariation(
    val id: String,
    val customerId: String,
    val variationType: IncomeVariationType,
    val bankAccountNumber: String,
    val bankName: String,
    val bankAccountStatus: String,
    val monthlyIncome: java.math.BigDecimal?,
    val additionalIncome: java.math.BigDecimal,
    val incomeProofDocumentUrl: String?,
    val verificationStatus: String,
    val verifiedBy: String?,
    val verifiedAt: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)

sealed class IncomeVariationUiState {
    object Loading : IncomeVariationUiState()
    data class Success(val variations: List<IncomeVariationType>) : IncomeVariationUiState()
    data class Error(val message: String) : IncomeVariationUiState()
}
