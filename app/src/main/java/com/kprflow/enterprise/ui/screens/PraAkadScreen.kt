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
import com.kprflow.enterprise.viewmodel.PraAkadViewModel

/**
 * Pra-Akad Screen
 * Legal-only access for managing Pra-Akad process
 * Phase 16: Mobile App Optimization - Enhanced Features
 */

@Composable
fun PraAkadScreen(
    navController: NavController,
    viewModel: PraAkadViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedPraAkad by remember { mutableStateOf<PraAkadData?>(null) }
    
    KPRFlowEnterpriseTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pra-Akad Management") },
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
                // Filter tabs
                FilterTabs(
                    selectedFilter = viewModel.selectedFilter.value,
                    onFilterSelected = { filter ->
                        viewModel.setFilter(filter)
                    },
                    filters = listOf("ALL", "PENDING", "SCHEDULED", "COMPLETED")
                )
                
                // Content
                when (uiState) {
                    is PraAkadUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is PraAkadUiState.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.praAkads) { praAkad ->
                                PraAkadCard(
                                    praAkad = praAkad,
                                    onScheduleClick = {
                                        selectedPraAkad = praAkad
                                        showScheduleDialog = true
                                    },
                                    onViewDetails = { praAkadId ->
                                        navController.navigate("pra_akad_detail/$praAkadId")
                                    },
                                    onGenerateSI = { praAkadId ->
                                        viewModel.generateSILunas(praAkadId)
                                    }
                                )
                            }
                        }
                    }
                    is PraAkadUiState.Error -> {
                        ErrorMessage(
                            message = uiState.message,
                            onRetry = { viewModel.refreshData() }
                        )
                    }
                }
            }
        }
        
        // Schedule Dialog
        if (showScheduleDialog && selectedPraAkad != null) {
            SchedulePraAkadDialog(
                praAkad = selectedPraAkad!!,
                onSchedule = { date, time, location, notes ->
                    viewModel.schedulePraAkad(
                        selectedPraAkad!!.id,
                        date,
                        time,
                        location,
                        notes
                    )
                    showScheduleDialog = false
                },
                onDismiss = { 
                    showScheduleDialog = false
                    selectedPraAkad = null
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
private fun PraAkadCard(
    praAkad: PraAkadData,
    onScheduleClick: (String) -> Unit,
    onViewDetails: (String) -> Unit,
    onGenerateSI: (String) -> Unit
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
                        text = praAkad.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${praAkad.blockNumber}-${praAkad.unitNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "KPR: ${formatCurrency(praAkad.kprAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                StatusChip(
                    status = praAkad.praAkadStatus,
                    color = when (praAkad.praAkadStatus) {
                        "PENDING" -> Warning
                        "SCHEDULED" -> Info
                        "COMPLETED" -> Success
                        else -> Error
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Pra-Akad Info
            if (praAkad.praAkadDate != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "${praAkad.praAkadDate} ${praAkad.praAkadTime ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (praAkad.praAkadLocation.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = praAkad.praAkadLocation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // SI Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SI Lunas Status:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                StatusChip(
                    status = praAkad.siSuratKeteranganLunasStatus,
                    color = when (praAkad.siSuratKeteranganLunasStatus) {
                        "READY" -> Success
                        "PREPARING" -> Info
                        else -> Warning
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (praAkad.praAkadStatus == "PENDING") {
                    Button(
                        onClick = { onScheduleClick(praAkad.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Schedule")
                    }
                }
                
                OutlinedButton(
                    onClick = { onViewDetails(praAkad.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details")
                }
                
                if (praAkad.siSuratKeteranganLunasStatus != "READY") {
                    OutlinedButton(
                        onClick = { onGenerateSI(praAkad.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Generate SI")
                    }
                }
            }
        }
    }
}

@Composable
private fun SchedulePraAkadDialog(
    praAkad: PraAkadData,
    onSchedule: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Pra-Akad") },
        text = {
            Column {
                Text(
                    text = "Customer: ${praAkad.customerName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Date picker
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it },
                    label = { Text("Date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Time picker
                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { selectedTime = it },
                    label = { Text("Time") },
                    placeholder = { Text("HH:MM") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    placeholder = { Text("Enter location") },
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
                    if (selectedDate.isNotEmpty() && selectedTime.isNotEmpty() && location.isNotEmpty()) {
                        onSchedule(selectedDate, selectedTime, location, notes)
                    }
                },
                enabled = selectedDate.isNotEmpty() && selectedTime.isNotEmpty() && location.isNotEmpty()
            ) {
                Text("Schedule")
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

data class PraAkadData(
    val id: String,
    val dossierId: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val blockNumber: String,
    val unitNumber: String,
    val kprAmount: Double,
    val dpAmount: Double,
    val bankName: String,
    val praAkadStatus: String,
    val praAkadDate: String?,
    val praAkadTime: String?,
    val praAkadLocation: String,
    val praAkadNotes: String,
    val siSuratKeteranganLunasStatus: String,
    val siSuratKeteranganLunasUrl: String,
    val financeWarningSent: Boolean,
    val legalAssignedName: String,
    val legalPhone: String,
    val financeNotifiedName: String,
    val createdAt: String,
    val updatedAt: String
)

// =====================================================
// UI STATES
// =====================================================

sealed class PraAkadUiState {
    object Loading : PraAkadUiState()
    data class Success(val praAkads: List<PraAkadData>) : PraAkadUiState()
    data class Error(val message: String) : PraAkadUiState()
}

// =====================================================
// UTILITY FUNCTIONS
// =====================================================

private fun formatCurrency(amount: Double): String {
    val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    return formatter.format(amount)
}
