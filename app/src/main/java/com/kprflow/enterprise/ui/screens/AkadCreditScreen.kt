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
import com.kprflow.enterprise.viewmodel.AkadCreditViewModel

/**
 * Akad Credit Screen
 * Legal-only access for managing Akad Credit process
 * Phase 16: Mobile App Optimization - Enhanced Features
 */

@Composable
fun AkadCreditScreen(
    navController: NavController,
    viewModel: AkadCreditViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showNotarisDialog by remember { mutableStateOf(false) }
    var showHariHDocumentsDialog by remember { mutableStateOf(false) }
    var selectedAkad by remember { mutableStateOf<AkadCreditData?>(null) }
    
    KPRFlowEnterpriseTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Akad Credit Management") },
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
                    filters = listOf("ALL", "SCHEDULED", "IN_PROGRESS", "COMPLETED")
                )
                
                // Content
                when (uiState) {
                    is AkadCreditUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is AkadCreditUiState.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.akads) { akad ->
                                AkadCreditCard(
                                    akad = akad,
                                    onScheduleClick = {
                                        selectedAkad = akad
                                        showScheduleDialog = true
                                    },
                                    onViewDetails = { akadId ->
                                        navController.navigate("akad_detail/$akadId")
                                    },
                                    onGenerateHariHDocuments = { akadId ->
                                        selectedAkad = akad
                                        showHariHDocumentsDialog = true
                                    },
                                    onSendInvitation = { akadId ->
                                        viewModel.sendInvitation(akadId)
                                    }
                                )
                            }
                        }
                    }
                    is AkadCreditUiState.Error -> {
                        ErrorMessage(
                            message = uiState.message,
                            onRetry = { viewModel.refreshData() }
                        )
                    }
                }
            }
        }
        
        // Schedule Dialog
        if (showScheduleDialog && selectedAkad != null) {
            ScheduleAkadDialog(
                akad = selectedAkad!!,
                onSchedule = { date, time, location, notes ->
                    viewModel.scheduleAkad(
                        selectedAkad!!.id,
                        date,
                        time,
                        location,
                        notes
                    )
                    showScheduleDialog = false
                },
                onDismiss = { 
                    showScheduleDialog = false
                    selectedAkad = null
                }
            )
        }
        
        // Notaris Selection Dialog
        if (showNotarisDialog && selectedAkad != null) {
            NotarisSelectionDialog(
                onNotarisSelected = { notarisId ->
                    viewModel.assignNotaris(selectedAkad!!.id, notarisId)
                    showNotarisDialog = false
                },
                onDismiss = { 
                    showNotarisDialog = false
                    selectedAkad = null
                }
            )
        }
        
        // Hari H Documents Dialog
        if (showHariHDocumentsDialog && selectedAkad != null) {
            HariHDocumentsDialog(
                akad = selectedAkad!!,
                onGenerateDocuments = {
                    viewModel.generateHariHDocuments(selectedAkad!!.id)
                    showHariHDocumentsDialog = false
                },
                onDismiss = { 
                    showHariHDocumentsDialog = false
                    selectedAkad = null
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
private fun AkadCreditCard(
    akad: AkadCreditData,
    onScheduleClick: (String) -> Unit,
    onViewDetails: (String) -> Unit,
    onGenerateHariHDocuments: (String) -> Unit,
    onSendInvitation: (String) -> Unit
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
                        text = akad.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${akad.blockNumber}-${akad.unitNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "KPR: ${formatCurrency(akad.kprAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                StatusChip(
                    status = akad.akadStatus,
                    color = when (akad.akadStatus) {
                        "SCHEDULED" -> Info
                        "IN_PROGRESS" -> Warning
                        "COMPLETED" -> Success
                        else -> Error
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Akad Info
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
                    text = "${akad.akadDate} ${akad.akadTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
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
                    text = akad.akadLocation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    text = "Notaris: ${akad.notarisName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Notification Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customer Notified:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                StatusChip(
                    status = if (akad.customerNotified) "SENT" else "PENDING",
                    color = if (akad.customerNotified) Success else Warning
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (akad.akadStatus == "SCHEDULED") {
                    Button(
                        onClick = { onGenerateHariHDocuments(akad.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hari H Docs")
                    }
                }
                
                if (!akad.customerNotified) {
                    OutlinedButton(
                        onClick = { onSendInvitation(akad.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Send Invite")
                    }
                }
                
                OutlinedButton(
                    onClick = { onViewDetails(akad.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details")
                }
            }
        }
    }
}

@Composable
private fun ScheduleAkadDialog(
    akad: AkadCreditData,
    onSchedule: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Akad Credit") },
        text = {
            Column {
                Text(
                    text = "Customer: ${akad.customerName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Notaris: ${akad.notarisName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Date picker (Legal only can set)
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it },
                    label = { Text("Akad Date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Time picker (Legal only can set)
                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { selectedTime = it },
                    label = { Text("Akad Time") },
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
private fun NotarisSelectionDialog(
    onNotarisSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // This would typically fetch notaris list from ViewModel
    val notarisList = remember {
        listOf(
            NotarisData("1", "Notaris Ahmad Wijaya, S.H., M.Kn.", "021-5551234"),
            NotarisData("2", "Notaris Siti Nurhaliza, S.H., M.Kn.", "021-6665678"),
            NotarisData("3", "Notaris Budi Santoso, S.H., M.Kn.", "021-7779012")
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Notaris") },
        text = {
            LazyColumn {
                items(notarisList) { notaris ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNotarisSelected(notaris.id) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = false,
                            onClick = { onNotarisSelected(notaris.id) }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column {
                            Text(
                                text = notaris.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = notaris.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun HariHDocumentsDialog(
    akad: AkadCreditData,
    onGenerateDocuments: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Hari H Documents") },
        text = {
            Column {
                Text(
                    text = "Generate documents for Akad Credit:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("• Memo Internal (Blank PDF)")
                Text("• Memo Appraisal Request (Blank PDF)")
                Text("• SO Legal for Notaris (Blank PDF)")
                Text("• Complete document attachments")
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Customer: ${akad.customerName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Akad Date: ${akad.akadDate} ${akad.akadTime}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "Notaris: ${akad.notarisName}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onGenerateDocuments) {
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

data class AkadCreditData(
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
    val akadStatus: String,
    val akadDate: String,
    val akadTime: String,
    val akadLocation: String,
    val akadNotes: String,
    val akadDocumentsReady: Boolean,
    val customerNotified: Boolean,
    val customerNotificationDate: String,
    val whatsappNotificationSent: Boolean,
    val legalAssignedName: String,
    val legalPhone: String,
    val notarisId: String,
    val notarisName: String,
    val notarisContact: String,
    val notarisAddress: String,
    val praAkadDate: String,
    val praAkadStatus: String,
    val createdAt: String,
    val updatedAt: String
)

data class NotarisData(
    val id: String,
    val name: String,
    val phone: String
)

// =====================================================
// UI STATES
// =====================================================

sealed class AkadCreditUiState {
    object Loading : AkadCreditUiState()
    data class Success(val akads: List<AkadCreditData>) : AkadCreditUiState()
    data class Error(val message: String) : AkadCreditUiState()
}

// =====================================================
// UTILITY FUNCTIONS
// =====================================================

private fun formatCurrency(amount: Double): String {
    val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    return formatter.format(amount)
}
