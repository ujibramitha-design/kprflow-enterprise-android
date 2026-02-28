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
import com.kprflow.enterprise.viewmodel.AppraisalLpaViewModel

/**
 * Appraisal/LPA Management Screen
 * Legal-only access for managing Appraisal/LPA requests
 * Phase 16: Mobile App Optimization - Enhanced Features
 */

@Composable
fun AppraisalLpaScreen(
    navController: NavController,
    viewModel: AppraisalLpaViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showGenerateDialog by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<AppraisalLpaData?>(null) }
    
    KPRFlowEnterpriseTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Appraisal/LPA Management") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { showGenerateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Generate Request")
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
                    filters = listOf("ALL", "PENDING", "PROCESSING", "COMPLETED", "CANCELLED")
                )
                
                // Bank filter (only show banks that support Appraisal/LPA)
                BankFilterTabs(
                    selectedBank = viewModel.selectedBank.value,
                    onBankSelected = { bank ->
                        viewModel.setBankFilter(bank)
                    },
                    banks = listOf("ALL", "Bank BTN") // Only Bank BTN supports Appraisal/LPA
                )
                
                // Content
                when (uiState) {
                    is AppraisalLpaUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is AppraisalLpaUiState.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.requests) { request ->
                                AppraisalLpaCard(
                                    request = request,
                                    onViewDetails = { requestId ->
                                        navController.navigate("appraisal_detail/$requestId")
                                    },
                                    onUpdateStatus = { requestId, status ->
                                        viewModel.updateRequestStatus(requestId, status)
                                    },
                                    onUploadDocument = { requestId ->
                                        navController.navigate("upload_appraisal_docs/$requestId")
                                    }
                                )
                            }
                        }
                    }
                    is AppraisalLpaUiState.Error -> {
                        ErrorMessage(
                            message = uiState.message,
                            onRetry = { viewModel.refreshData() }
                        )
                    }
                }
            }
        }
        
        // Generate Request Dialog
        if (showGenerateDialog) {
            GenerateAppraisalLpaDialog(
                onGenerate = { request ->
                    viewModel.generateRequest(request)
                    showGenerateDialog = false
                },
                onDismiss = { showGenerateDialog = false }
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
private fun BankFilterTabs(
    selectedBank: String,
    onBankSelected: (String) -> Unit,
    banks: List<String>
) {
    ScrollableTabRow(
        selectedTabIndex = banks.indexOf(selectedBank)
    ) {
        banks.forEachIndexed { index, bank ->
            Tab(
                selected = selectedBank == bank,
                onClick = { onBankSelected(bank) },
                text = { Text(bank) }
            )
        }
    }
}

@Composable
private fun AppraisalLpaCard(
    request: AppraisalLpaData,
    onViewDetails: (String) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onUploadDocument: (String) -> Unit
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
                        text = request.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${request.blockNumber}-${request.unitNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Bank: ${request.bankName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                StatusChip(
                    status = request.requestStatus,
                    color = when (request.requestStatus) {
                        "COMPLETED" -> Success
                        "PROCESSING" -> Info
                        "PENDING" -> Warning
                        "CANCELLED" -> Error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Request Details
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = "Request Type",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "Request: ${request.requestType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = "Request Date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "Request Date: ${request.requestDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Expected Completion",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "Expected: ${request.expectedCompletionDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        request.daysStatus != null && request.daysStatus < 0 -> Error
                        request.daysStatus != null && request.daysStatus <= 3 -> Warning
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            if (request.appraisalValue > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = "Appraisal Value",
                        tint = Success,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "Value: ${formatCurrency(request.appraisalValue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Success
                    )
                }
            }
            
            if (request.appraiserName.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Appraiser",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "Appraiser: ${request.appraiserName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Property Details
            if (request.propertyAddress.isNotEmpty()) {
                Text(
                    text = "Property: ${request.propertyType} - ${request.propertySize}m²",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = request.propertyAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (request.requestStatus == "PENDING") {
                    Button(
                        onClick = { onUpdateStatus(request.id, "PROCESSING") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start")
                    }
                }
                
                if (request.requestStatus == "PROCESSING") {
                    Button(
                        onClick = { onUpdateStatus(request.id, "COMPLETED") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Complete")
                    }
                }
                
                OutlinedButton(
                    onClick = { onUploadDocument(request.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Upload Docs")
                }
                
                OutlinedButton(
                    onClick = { onViewDetails(request.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details")
                }
            }
        }
    }
}

@Composable
private fun GenerateAppraisalLpaDialog(
    onGenerate: (AppraisalLpaRequestData) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDossier by remember { mutableStateOf("") }
    var requestType by remember { mutableStateOf("APPRAISAL") }
    var propertyAddress by remember { mutableStateOf("") }
    var propertyType by remember { mutableStateOf("RUMAH") }
    var propertySize by remember { mutableStateOf("") }
    var buildingYear by remember { mutableStateOf("") }
    var buildingCondition by remember { mutableStateOf("BAGUS") }
    var additionalNotes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Appraisal/LPA Request") },
        text = {
            Column {
                Text(
                    text = "Note: Only Bank BTN supports Appraisal/LPA requests",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Request Type
                Text(
                    text = "Request Type",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row {
                    RadioButton(
                        selected = requestType == "APPRAISAL",
                        onClick = { requestType = "APPRAISAL" }
                    )
                    Text("Appraisal")
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    RadioButton(
                        selected = requestType == "LPA",
                        onClick = { requestType = "LPA" }
                    )
                    Text("LPA")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Property Type
                Text(
                    text = "Property Type",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row {
                    RadioButton(
                        selected = propertyType == "RUMAH",
                        onClick = { propertyType = "RUMAH" }
                    )
                    Text("Rumah")
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    RadioButton(
                        selected = propertyType == "RUKO",
                        onClick = { propertyType = "RUKO" }
                    )
                    Text("Ruko")
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    RadioButton(
                        selected = propertyType == "APARTEMEN",
                        onClick = { propertyType = "APARTEMEN" }
                    )
                    Text("Apartemen")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = propertyAddress,
                    onValueChange = { propertyAddress = it },
                    label = { Text("Property Address") },
                    placeholder = { Text("Enter property address") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = propertySize,
                    onValueChange = { propertySize = it },
                    label = { Text("Property Size (m²)") },
                    placeholder = { Text("120") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = buildingYear,
                    onValueChange = { buildingYear = it },
                    label = { Text("Building Year") },
                    placeholder = { Text("2020") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Building Condition
                Text(
                    text = "Building Condition",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row {
                    RadioButton(
                        selected = buildingCondition == "BAGUS",
                        onClick = { buildingCondition = "BAGUS" }
                    )
                    Text("Bagus")
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    RadioButton(
                        selected = buildingCondition == "SEDANG",
                        onClick = { buildingCondition = "SEDANG" }
                    )
                    Text("Sedang")
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    RadioButton(
                        selected = buildingCondition == "KURANG",
                        onClick = { buildingCondition = "KURANG" }
                    )
                    Text("Kurang")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = additionalNotes,
                    onValueChange = { additionalNotes = it },
                    label = { Text("Additional Notes") },
                    placeholder = { Text("Enter notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (propertyAddress.isNotEmpty() && propertySize.isNotEmpty()) {
                        onGenerate(
                            AppraisalLpaRequestData(
                                dossierId = selectedDossier,
                                requestType = requestType,
                                propertyAddress = propertyAddress,
                                propertyType = propertyType,
                                propertySize = propertySize.toDoubleOrNull() ?: 0.0,
                                buildingYear = buildingYear.toIntOrNull(),
                                buildingCondition = buildingCondition,
                                additionalNotes = additionalNotes
                            )
                        )
                    }
                },
                enabled = propertyAddress.isNotEmpty() && propertySize.isNotEmpty()
            ) {
                Text("Generate")
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

data class AppraisalLpaData(
    val id: String,
    val dossierId: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val bankId: String,
    val bankName: String,
    val requestType: String,
    val requestStatus: String,
    val requestDate: String,
    val expectedCompletionDate: String,
    val actualCompletionDate: String,
    val appraisalCompany: String,
    val appraiserName: String,
    val appraiserLicense: String,
    val appraisalValue: Double,
    val lpaValue: Double,
    val propertyAddress: String,
    val propertyDescription: String,
    val propertyType: String,
    val propertySize: Double,
    val buildingYear: Int,
    val buildingCondition: String,
    val documentsSubmitted: String,
    val additionalNotes: String,
    val blockNumber: String,
    val unitNumber: String,
    val daysStatus: Int?,
    val createdAt: String,
    val updatedAt: String
)

data class AppraisalLpaRequestData(
    val dossierId: String,
    val requestType: String,
    val propertyAddress: String,
    val propertyType: String,
    val propertySize: Double,
    val buildingYear: Int?,
    val buildingCondition: String,
    val additionalNotes: String
)

// =====================================================
// UI STATES
// =====================================================

sealed class AppraisalLpaUiState {
    object Loading : AppraisalLpaUiState()
    data class Success(val requests: List<AppraisalLpaData>) : AppraisalLpaUiState()
    data class Error(val message: String) : AppraisalLpaUiState()
}

// =====================================================
// UTILITY FUNCTIONS
// =====================================================

private fun formatCurrency(amount: Double): String {
    val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    return formatter.format(amount)
}
