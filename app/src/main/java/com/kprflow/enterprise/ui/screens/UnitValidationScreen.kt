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
import com.kprflow.enterprise.viewmodel.UnitValidationViewModel

/**
 * Unit Validation Screen
 * Customer & Marketing access for checking unit completion status
 * Phase 16: Mobile App Optimization - Unit Completion Validation
 */

@Composable
fun UnitValidationScreen(
    navController: NavController,
    viewModel: UnitValidationViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedUnit by remember { mutableStateOf<UnitValidationData?>(null) }
    var showValidationDialog by remember { mutableStateOf(false) }
    
    KPRFlowEnterpriseTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Unit Validation") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                // Filter Tabs by Completion Status
                UnitFilterTabs(
                    selectedFilter = viewModel.selectedFilter.value,
                    onFilterSelected = { filter ->
                        viewModel.setFilter(filter)
                    },
                    filters = listOf("ALL", "FULLY_COMPLETE", "PARTIALLY_COMPLETE", "INCOMPLETE")
                )
                
                // Content
                when (uiState) {
                    is UnitValidationUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is UnitValidationUiState.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.units) { unit ->
                                UnitValidationCard(
                                    unit = unit,
                                    onViewDetails = {
                                        selectedUnit = unit
                                        showValidationDialog = true
                                    },
                                    onSelectBank = { unitId ->
                                        if (unit.canSelectBank) {
                                            navController.navigate("bank_selection/$unitId")
                                        }
                                    },
                                    onUploadDocuments = { unitId ->
                                        navController.navigate("document_upload/$unitId")
                                    }
                                )
                            }
                        }
                    }
                    is UnitValidationUiState.Error -> {
                        ErrorMessage(
                            message = uiState.message,
                            onRetry = { viewModel.refreshData() }
                        )
                    }
                }
            }
        }
        
        // Unit Validation Details Dialog
        if (showValidationDialog && selectedUnit != null) {
            UnitValidationDialog(
                unit = selectedUnit!!,
                onDismiss = { 
                    showValidationDialog = false
                    selectedUnit = null
                }
            )
        }
    }
}

@Composable
private fun UnitFilterTabs(
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
                text = { Text(filter.replace("_", " ")) }
            )
        }
    }
}

@Composable
private fun UnitValidationCard(
    unit: UnitValidationData,
    onViewDetails: (UnitValidationData) -> Unit,
    onSelectBank: (String) -> Unit,
    onUploadDocuments: (String) -> Unit
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
                        text = "${unit.blockNumber} - ${unit.unitNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = unit.unitType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Customer: ${unit.customerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Completion Status
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${unit.completionPercentage}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            unit.completionPercentage >= 100 -> Success
                            unit.completionPercentage >= 60 -> Warning
                            else -> Error
                        },
                        fontWeight = FontWeight.Bold
                    )
                    
                    StatusChip(
                        status = unit.validationStatus.replace("_", " "),
                        color = when {
                            unit.completionPercentage >= 100 -> Success
                            unit.completionPercentage >= 60 -> Warning
                            else -> Error
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress Bar
            Column {
                Text(
                    text = "Unit Completion Progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                
                LinearProgressIndicator(
                    progress = unit.completionPercentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when {
                        unit.completionPercentage >= 100 -> Success
                        unit.completionPercentage >= 60 -> Warning
                        else -> Error
                    }
                )
                
                Text(
                    text = "${unit.completionPercentage}% Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Validation Rules
            Column {
                Text(
                    text = "Validation Rules:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                
                // Bank Selection Rule
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                ) {
                    Icon(
                        imageVector = if (unit.canSelectBank) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = "Bank Selection",
                        tint = if (unit.canSelectBank) Success else Error,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Bank Selection: ${if (unit.canSelectBank) "Allowed" else "Not Allowed"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unit.canSelectBank) Success else Error
                    )
                }
                
                // Document Upload Rule
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Document Upload",
                        tint = Success,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Document Upload: Always Allowed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Success
                    )
                }
                
                // Processing Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Processing Time",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Processing Time: 14 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Recommendations
            if (unit.recommendations.isNotEmpty()) {
                Column {
                    Text(
                        text = "Recommendations:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    
                    unit.recommendations.take(3).forEach { recommendation ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Recommendation",
                                tint = Info,
                                modifier = Modifier.size(12.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = recommendation,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    if (unit.recommendations.size > 3) {
                        Text(
                            text = "... and ${unit.recommendations.size - 3} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onViewDetails(unit) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Details")
                }
                
                Button(
                    onClick = { onSelectBank(unit.id) },
                    enabled = unit.canSelectBank,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Select Bank")
                }
                
                OutlinedButton(
                    onClick = { onUploadDocuments(unit.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Upload Docs")
                }
            }
        }
    }
}

@Composable
private fun UnitValidationDialog(
    unit: UnitValidationData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unit Validation Details - ${unit.blockNumber}-${unit.unitNumber}") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Unit Information:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column {
                        Text("Unit Number: ${unit.blockNumber}-${unit.unitNumber}")
                        Text("Unit Type: ${unit.unitType}")
                        Text("Customer: ${unit.customerName}")
                        Text("Completion: ${unit.completionPercentage}%")
                        Text("Status: ${unit.validationStatus.replace("_", " ")}")
                        Text("Price: ${unit.price}")
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Validation Results:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column {
                        Text("Bank Selection: ${if (unit.canSelectBank) "✅ Allowed" else "❌ Not Allowed"}")
                        Text("Document Upload: ✅ Always Allowed")
                        Text("Processing Time: 14 days")
                        Text("Min Completion Required: 60%")
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Recommendations:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    unit.recommendations.forEach { recommendation ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Recommendation",
                                        tint = Info,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = recommendation,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Next Steps:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column {
                        if (unit.canSelectBank) {
                            Text("✅ Can proceed with bank selection")
                            Text("✅ Can upload documents")
                            Text("⏱️ Processing time: 14 days")
                        } else {
                            Text("❌ Cannot select bank (completion < 60%)")
                            Text("✅ Can upload documents for preparation")
                            Text("⏳ Wait for unit completion to reach 60%")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
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

data class UnitValidationData(
    val id: String,
    val blockNumber: String,
    val unitNumber: String,
    val unitType: String,
    val customerName: String,
    val completionPercentage: Int,
    val validationStatus: String,
    val canSelectBank: Boolean,
    val canUploadDocuments: Boolean,
    val price: String,
    val recommendations: List<String>,
    val processingDays: Int = 14,
    val minCompletionRequired: Int = 60
)

// =====================================================
// UI STATES
// =====================================================

sealed class UnitValidationUiState {
    object Loading : UnitValidationUiState()
    data class Success(val units: List<UnitValidationData>) : UnitValidationUiState()
    data class Error(val message: String) : UnitValidationUiState()
}
