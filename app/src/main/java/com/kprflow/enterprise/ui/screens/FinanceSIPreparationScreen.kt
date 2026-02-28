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
import com.kprflow.enterprise.viewmodel.FinanceSIPreparationViewModel

/**
 * Finance SI Preparation Screen
- Finance-only access for SI Surat Keterangan Lunas preparation
- Only shows records where Akad Credit is scheduled
- Priority-based view for efficient resource allocation
- Phase 16: Mobile App Optimization - Enhanced Features
 */

@Composable
fun FinanceSIPreparationScreen(
    navController: NavController,
    viewModel: FinanceSIPreparationViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showGenerateSIDialog by remember { mutableStateOf(false) }
    var selectedSI by remember { mutableStateOf<SIPreparationData?>(null) }
    
    KPRFlowEnterpriseTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("SI Surat Keterangan Lunas") },
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
                // Priority Filter Tabs
                PriorityFilterTabs(
                    selectedFilter = viewModel.selectedPriority.value,
                    onFilterSelected = { filter ->
                        viewModel.setPriorityFilter(filter)
                    },
                    filters = listOf("ALL", "CRITICAL", "URGENT", "HIGH", "COMPLETED")
                )
                
                // Content
                when (uiState) {
                    is FinanceSIPreparationUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is FinanceSIPreparationUiState.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.siPreparations) { siPreparation ->
                                SIPreparationCard(
                                    siPreparation = siPreparation,
                                    onGenerateSI = {
                                        selectedSI = siPreparation
                                        showGenerateSIDialog = true
                                    },
                                    onViewDetails = { siId ->
                                        navController.navigate("si_detail/$siId")
                                    },
                                    onMarkReady = { siId ->
                                        viewModel.markSIReady(siId)
                                    }
                                )
                            }
                        }
                    }
                    is FinanceSIPreparationUiState.Error -> {
                        ErrorMessage(
                            message = uiState.message,
                            onRetry = { viewModel.refreshData() }
                        )
                    }
                }
            }
        }
        
        // Generate SI Dialog
        if (showGenerateSIDialog && selectedSI != null) {
            GenerateSIDialog(
                siPreparation = selectedSI!!,
                onGenerate = { siUrl ->
                    viewModel.generateSI(selectedSI!!.id, siUrl)
                    showGenerateSIDialog = false
                },
                onDismiss = { 
                    showGenerateSIDialog = false
                    selectedSI = null
                }
            )
        }
    }
}

@Composable
private fun PriorityFilterTabs(
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
private fun SIPreparationCard(
    siPreparation: SIPreparationData,
    onGenerateSI: (String) -> Unit,
    onViewDetails: (String) -> Unit,
    onMarkReady: (String) -> Unit
) {
    BentoBox {
        Column {
            // Header with Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = siPreparation.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${siPreparation.blockNumber}-${siPreparation.unitNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "KPR: ${formatCurrency(siPreparation.kprAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                StatusChip(
                    status = siPreparation.priorityLevel,
                    color = when (siPreparation.priorityLevel) {
                        "CRITICAL" -> Error
                        "URGENT" -> Warning
                        "HIGH" -> Info
                        "COMPLETED" -> Success
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Akad Information
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = "Akad Date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "Akad: ${siPreparation.akadDate} ${siPreparation.akadTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Days Until",
                    tint = when {
                        siPreparation.daysUntilAkad <= 3 -> Error
                        siPreparation.daysUntilAkad <= 7 -> Warning
                        siPreparation.daysUntilAkad <= 14 -> Info
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "${siPreparation.daysUntilAkad} hari lagi",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        siPreparation.daysUntilAkad <= 3 -> Error
                        siPreparation.daysUntilAkad <= 7 -> Warning
                        siPreparation.daysUntilAkad <= 14 -> Info
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Notaris",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "Notaris: ${siPreparation.notarisName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // SI Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SI Status:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                StatusChip(
                    status = siPreparation.siStatus,
                    color = when (siPreparation.siStatus) {
                        "READY" -> Success
                        "PREPARING" -> Info
                        else -> Warning
                    }
                )
            }
            
            // Finance Warning Status
            if (siPreparation.financeWarningSent) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Warning Sent",
                        tint = Success,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "Finance Warning: ${siPreparation.financeWarningDate}",
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
                if (siPreparation.siStatus != "READY") {
                    Button(
                        onClick = { onGenerateSI(siPreparation.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Generate SI")
                    }
                    
                    OutlinedButton(
                        onClick = { onMarkReady(siPreparation.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark Ready")
                    }
                }
                
                OutlinedButton(
                    onClick = { onViewDetails(siPreparation.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details")
                }
            }
        }
    }
}

@Composable
private fun GenerateSIDialog(
    siPreparation: SIPreparationData,
    onGenerate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var siUrl by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate SI Surat Keterangan Lunas") },
        text = {
            Column {
                Text(
                    text = "Customer: ${siPreparation.customerName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Akad: ${siPreparation.akadDate} ${siPreparation.akadTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Priority: ${siPreparation.priorityLevel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (siPreparation.priorityLevel) {
                        "CRITICAL" -> Error
                        "URGENT" -> Warning
                        "HIGH" -> Info
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // SI URL
                OutlinedTextField(
                    value = siUrl,
                    onValueChange = { siUrl = it },
                    label = { Text("SI Document URL") },
                    placeholder = { Text("Enter SI document URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    placeholder = { Text("Enter notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (siUrl.isNotEmpty()) {
                        onGenerate(siUrl)
                    }
                },
                enabled = siUrl.isNotEmpty()
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

data class SIPreparationData(
    val praAkadId: String,
    val dossierId: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val blockNumber: String,
    val unitNumber: String,
    val kprAmount: Double,
    val dpAmount: Double,
    val bankName: String,
    val praAkadStatus: String,
    val siStatus: String,
    val siUrl: String,
    val financeWarningSent: Boolean,
    val financeWarningDate: String,
    val akadDate: String,
    val akadTime: String,
    val notarisName: String,
    val daysUntilAkad: Int,
    val priorityLevel: String,
    val legalAssignedName: String,
    val legalPhone: String,
    val createdAt: String,
    val updatedAt: String
)

// =====================================================
// UI STATES
// =====================================================

sealed class FinanceSIPreparationUiState {
    object Loading : FinanceSIPreparationUiState()
    data class Success(val siPreparations: List<SIPreparationData>) : FinanceSIPreparationUiState()
    data class Error(val message: String) : FinanceSIPreparationUiState()
}

// =====================================================
// UTILITY FUNCTIONS
// =====================================================

private fun formatCurrency(amount: Double): String {
    val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    return formatter.format(amount)
}
