package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.theme.KPRFlowEnterpriseTheme
import com.kprflow.enterprise.viewmodel.TargetBankManagementViewModel

/**
 * Target Bank Management Screen
 * Legal-only access for managing target banks and their specific features
 * Phase 16: Mobile App Optimization - Enhanced Features
 */

@Composable
fun TargetBankManagementScreen(
    navController: NavController,
    viewModel: TargetBankManagementViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddBankDialog by remember { mutableStateOf(false) }
    var showFeatureConfigDialog by remember { mutableStateOf(false) }
    var selectedBank by remember { mutableStateOf<TargetBankData?>(null) }
    
    KPRFlowEnterpriseTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Target Bank Management") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { showAddBankDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Bank")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filter tabs
                FilterTabs(
                    selectedFilter = viewModel.selectedFilter.value,
                    onFilterSelected = { filter ->
                        viewModel.setFilter(filter)
                    },
                    filters = listOf("ALL", "CONVENTIONAL", "SYARIAH", "STATE", "PRIVATE")
                )
                
                // Content
                when (uiState) {
                    is TargetBankManagementUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is TargetBankManagementUiState.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.banks) { bank ->
                                TargetBankCard(
                                    bank = bank,
                                    onConfigureFeatures = {
                                        selectedBank = bank
                                        showFeatureConfigDialog = true
                                    },
                                    onEdit = { bankId ->
                                        navController.navigate("edit_bank/$bankId")
                                    },
                                    onToggleStatus = { bankId, isActive ->
                                        viewModel.toggleBankStatus(bankId, isActive)
                                    }
                                )
                            }
                        }
                    }
                    is TargetBankManagementUiState.Error -> {
                        ErrorMessage(
                            message = uiState.message,
                            onRetry = { viewModel.refreshData() }
                        )
                    }
                }
            }
        }
        
        // Add Bank Dialog
        if (showAddBankDialog) {
            AddBankDialog(
                onAdd = { bankData ->
                    viewModel.addBank(bankData)
                    showAddBankDialog = false
                },
                onDismiss = { showAddBankDialog = false }
            )
        }
        
        // Feature Configuration Dialog
        if (showFeatureConfigDialog && selectedBank != null) {
            FeatureConfigDialog(
                bank = selectedBank!!,
                onUpdateFeatures = { features ->
                    viewModel.updateBankFeatures(selectedBank!!.id, features)
                    showFeatureConfigDialog = false
                },
                onDismiss = { 
                    showFeatureConfigDialog = false
                    selectedBank = null
                }
            )
        }
    }
}

@Composable
private fun FilterTabs(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    filters: List<String>
) {
    ScrollableTabRow(
        selectedTabIndex = filters.indexOf(selectedFilter)
    ) {
        filters.forEachIndexed { index, filter ->
            Tab(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                text = { Text(filter) }
            )
        }
    }
}

@Composable
private fun TargetBankCard(
    bank: TargetBankData,
    onConfigureFeatures: (TargetBankData) -> Unit,
    onEdit: (String) -> Unit,
    onToggleStatus: (String, Boolean) -> Unit
) {
    BentoBox {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bank.bankName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = bank.bankCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row {
                        StatusChip(
                            status = bank.bankType,
                            color = when (bank.bankType) {
                                "CONVENTIONAL" -> Info
                                "SYARIAH" -> Success
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        StatusChip(
                            status = bank.bankCategory,
                            color = when (bank.bankCategory) {
                                "STATE" -> Warning
                                "PRIVATE" -> Info
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                Switch(
                    checked = bank.isActive,
                    onCheckedChange = { onToggleStatus(bank.id, it) }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bank Features
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Supported Features:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (bank.supportsAppraisalLpa) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Appraisal/LPA",
                            tint = if (bank.supportsAppraisalLpa) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "Appraisal/LPA",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (bank.supportsBpnClearance) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "BPN/Clearance",
                            tint = if (bank.supportsBpnClearance) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "BPN/Clearance",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    if (bank.supportsAppraisalLpa) {
                        Text(
                            text = "Auto: ${bank.appraisalProcessingDays} days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (bank.supportsBpnClearance) {
                        Text(
                            text = "Warning: ${bank.bpnProcessingDays} days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Contact Info
            if (bank.contactInfo.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Contact",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = bank.contactInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onConfigureFeatures(bank) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Configure")
                }
                
                OutlinedButton(
                    onClick = { onEdit(bank.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
private fun AddBankDialog(
    onAdd: (TargetBankData) -> Unit,
    onDismiss: () -> Unit
) {
    var bankCode by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankType by remember { mutableStateOf("CONVENTIONAL") }
    var bankCategory by remember { mutableStateOf("STATE") }
    var contactInfo by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Target Bank") },
        text = {
            Column {
                OutlinedTextField(
                    value = bankCode,
                    onValueChange = { bankCode = it },
                    label = { Text("Bank Code") },
                    placeholder = { Text("e.g., BTN, BNI, BSI") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name") },
                    placeholder = { Text("e.g., Bank BTN") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Bank Type
                Text(
                    text = "Bank Type",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row {
                    RadioButton(
                        selected = bankType == "CONVENTIONAL",
                        onClick = { bankType = "CONVENTIONAL" }
                    )
                    Text("Conventional")
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    RadioButton(
                        selected = bankType == "SYARIAH",
                        onClick = { bankType = "SYARIAH" }
                    )
                    Text("Syariah")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Bank Category
                Text(
                    text = "Bank Category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row {
                    RadioButton(
                        selected = bankCategory == "STATE",
                        onClick = { bankCategory = "STATE" }
                    )
                    Text("State")
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    RadioButton(
                        selected = bankCategory == "PRIVATE",
                        onClick = { bankCategory = "PRIVATE" }
                    )
                    Text("Private")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = contactInfo,
                    onValueChange = { contactInfo = it },
                    label = { Text("Contact Info") },
                    placeholder = { Text("Phone, email, address") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bankCode.isNotEmpty() && bankName.isNotEmpty()) {
                        onAdd(
                            TargetBankData(
                                id = "",
                                bankCode = bankCode,
                                bankName = bankName,
                                bankType = bankType,
                                bankCategory = bankCategory,
                                supportsAppraisalLpa = false,
                                supportsBpnClearance = false,
                                appraisalProcessingDays = 0,
                                bpnProcessingDays = 0,
                                contactInfo = contactInfo,
                                isActive = true,
                                createdAt = "",
                                updatedAt = ""
                            )
                        )
                    }
                },
                enabled = bankCode.isNotEmpty() && bankName.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FeatureConfigDialog(
    bank: TargetBankData,
    onUpdateFeatures: (BankFeaturesData) -> Unit,
    onDismiss: () -> Unit
) {
    var supportsAppraisalLpa by remember { mutableStateOf(bank.supportsAppraisalLpa) }
    var supportsBpnClearance by remember { mutableStateOf(bank.supportsBpnClearance) }
    var appraisalProcessingDays by remember { mutableStateOf(bank.appraisalProcessingDays.toString()) }
    var bpnProcessingDays by remember { mutableStateOf(bank.bpnProcessingDays.toString()) }
    var autoGenerateAppraisal by remember { mutableStateOf(bank.bankCode == "BTN") }
    var additionalInstructions by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Features - ${bank.bankName}") },
        text = {
            Column {
                Text(
                    text = "Appraisal/LPA Features",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = supportsAppraisalLpa,
                        onCheckedChange = { supportsAppraisalLpa = it }
                    )
                    Text("Supports Appraisal/LPA")
                }
                
                if (supportsAppraisalLpa) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoGenerateAppraisal,
                            onCheckedChange = { autoGenerateAppraisal = it }
                        )
                        Text("Auto-generate requests")
                    }
                    
                    OutlinedTextField(
                        value = appraisalProcessingDays,
                        onValueChange = { appraisalProcessingDays = it },
                        label = { Text("Processing Days") },
                        placeholder = { Text("7") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "BPN/Clearance Features",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = supportsBpnClearance,
                        onCheckedChange = { supportsBpnClearance = it }
                    )
                    Text("Supports BPN/Clearance")
                }
                
                if (supportsBpnClearance) {
                    OutlinedTextField(
                        value = bpnProcessingDays,
                        onValueChange = { bpnProcessingDays = it },
                        label = { Text("Processing Days") },
                        placeholder = { Text("10") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = additionalInstructions,
                        onValueChange = { additionalInstructions = it },
                        label = { Text("Additional Instructions") },
                        placeholder = { Text("Special requirements") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bank-specific warnings
                if (bank.bankCode == "BTN") {
                    Text(
                        text = "Bank BTN supports both Appraisal/LPA (auto-generate) and BPN/Clearance (warning)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (bank.bankCode == "BNI" || bank.bankCode == "BSI") {
                    Text(
                        text = "Bank ${bank.bankCode} supports BPN/Clearance with warning only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateFeatures(
                        BankFeaturesData(
                            supportsAppraisalLpa = supportsAppraisalLpa,
                            supportsBpnClearance = supportsBpnClearance,
                            appraisalProcessingDays = appraisalProcessingDays.toIntOrNull() ?: 0,
                            bpnProcessingDays = bpnProcessingDays.toIntOrNull() ?: 0,
                            autoGenerateAppraisal = autoGenerateAppraisal,
                            additionalInstructions = additionalInstructions
                        )
                    )
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    BentoBox {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

// =====================================================
// DATA CLASSES
// =====================================================

data class TargetBankData(
    val id: String,
    val bankCode: String,
    val bankName: String,
    val bankType: String,
    val bankCategory: String,
    val supportsAppraisalLpa: Boolean,
    val supportsBpnClearance: Boolean,
    val appraisalProcessingDays: Int,
    val bpnProcessingDays: Int,
    val contactInfo: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class BankFeaturesData(
    val supportsAppraisalLpa: Boolean,
    val supportsBpnClearance: Boolean,
    val appraisalProcessingDays: Int,
    val bpnProcessingDays: Int,
    val autoGenerateAppraisal: Boolean,
    val additionalInstructions: String
)

// =====================================================
// UI STATES
// =====================================================

sealed class TargetBankManagementUiState {
    object Loading : TargetBankManagementUiState()
    data class Success(val banks: List<TargetBankData>) : TargetBankManagementUiState()
    data class Error(val message: String) : TargetBankManagementUiState()
}
