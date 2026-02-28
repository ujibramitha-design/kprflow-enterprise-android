package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.components.FinalReportCard
import com.kprflow.enterprise.ui.viewmodel.ReportGenerationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportGenerationScreen(
    dossierId: String,
    onBackClick: () -> Unit,
    viewModel: ReportGenerationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reportsState by viewModel.reportsState.collectAsState()
    
    LaunchedEffect(dossierId) {
        viewModel.loadReports(dossierId)
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
            Text(
                text = "Final Reports",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        viewModel.showBASTDialog(dossierId)
                    }
                ) {
                    Text("Generate BAST")
                }
                
                Button(
                    onClick = { 
                        viewModel.showHandoverDialog(dossierId)
                    }
                ) {
                    Text("Generate Certificate")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Reports List
        when (reportsState) {
            is ReportsState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is ReportsState.Success -> {
                if (reportsState.reports.isEmpty()) {
                    EmptyReportsState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reportsState.reports) { report ->
                            FinalReportCard(
                                report = report,
                                onDownload = { viewModel.downloadReport(report.id) },
                                onDelete = { viewModel.deleteReport(report.id) },
                                onUpdateSignatures = { customerSigned, developerSigned ->
                                    viewModel.updateSignatures(report.id, customerSigned, developerSigned)
                                }
                            )
                        }
                    }
                }
            }
            
            is ReportsState.Error -> {
                ErrorState(
                    message = reportsState.message,
                    onRetry = { viewModel.loadReports(dossierId) }
                )
            }
        }
    }
    
    // BAST Generation Dialog
    if (uiState.showBASTDialog) {
        BASTGenerationDialog(
            onDismiss = { viewModel.hideBASTDialog() },
            onGenerate = { bastData ->
                viewModel.generateBASTReport(dossierId, bastData)
            }
        )
    }
    
    // Handover Certificate Dialog
    if (uiState.showHandoverDialog) {
        HandoverCertificateDialog(
            onDismiss = { viewModel.hideHandoverDialog() },
            onGenerate = { certificateData ->
                viewModel.generateHandoverCertificate(dossierId, certificateData)
            }
        )
    }
}

@Composable
private fun EmptyReportsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Final Reports Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Generate BAST or Handover Certificate to see them here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        Text(
            text = "Error loading reports",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun BASTGenerationDialog(
    onDismiss: () -> Unit,
    onGenerate: (BASTData) -> Unit
) {
    var handoverDate by remember { mutableStateOf("") }
    var handoverTime by remember { mutableStateOf("") }
    var handoverLocation by remember { mutableStateOf("") }
    var witnessName by remember { mutableStateOf("") }
    var witnessPosition by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Generate BAST Report")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = handoverDate,
                    onValueChange = { handoverDate = it },
                    label = { Text("Handover Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = handoverTime,
                    onValueChange = { handoverTime = it },
                    label = { Text("Handover Time (HH:MM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = handoverLocation,
                    onValueChange = { handoverLocation = it },
                    label = { Text("Handover Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = witnessName,
                    onValueChange = { witnessName = it },
                    label = { Text("Witness Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = witnessPosition,
                    onValueChange = { witnessPosition = it },
                    label = { Text("Witness Position") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (handoverDate.isNotBlank() && handoverLocation.isNotBlank()) {
                        onGenerate(
                            BASTData(
                                handoverDate = handoverDate,
                                handoverTime = handoverTime,
                                handoverLocation = handoverLocation,
                                witnessName = witnessName,
                                witnessPosition = witnessPosition
                            )
                        )
                    }
                },
                enabled = handoverDate.isNotBlank() && handoverLocation.isNotBlank()
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
private fun HandoverCertificateDialog(
    onDismiss: () -> Unit,
    onGenerate: (HandoverCertificateData) -> Unit
) {
    var handoverDate by remember { mutableStateOf("") }
    var propertyCondition by remember { mutableStateOf("") }
    var includedItems by remember { mutableStateOf("") }
    var specialNotes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Generate Handover Certificate")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = handoverDate,
                    onValueChange = { handoverDate = it },
                    label = { Text("Handover Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = propertyCondition,
                    onValueChange = { propertyCondition = it },
                    label = { Text("Property Condition") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                OutlinedTextField(
                    value = includedItems,
                    onValueChange = { includedItems = it },
                    label = { Text("Included Items (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                OutlinedTextField(
                    value = specialNotes,
                    onValueChange = { specialNotes = it },
                    label = { Text("Special Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (handoverDate.isNotBlank() && propertyCondition.isNotBlank()) {
                        onGenerate(
                            HandoverCertificateData(
                                handoverDate = handoverDate,
                                propertyCondition = propertyCondition,
                                includedItems = includedItems.split(",").map { it.trim() },
                                specialNotes = specialNotes.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                },
                enabled = handoverDate.isNotBlank() && propertyCondition.isNotBlank()
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

// UI States
sealed class ReportsState {
    object Loading : ReportsState()
    data class Success(val reports: List<com.kprflow.enterprise.data.repository.FinalReportRecord>) : ReportsState()
    data class Error(val message: String) : ReportsState()
}

sealed class ReportGenerationUiState {
    object Idle : ReportGenerationUiState()
    object Generating : ReportGenerationUiState()
    data class Success(val reportId: String, val publicUrl: String) : ReportGenerationUiState()
    data class Error(val message: String) : ReportGenerationUiState()
    
    val showBASTDialog: Boolean
        get() = this is ShowBASTDialog
    
    val showHandoverDialog: Boolean
        get() = this is ShowHandoverDialog
    
    val isGenerating: Boolean
        get() = this is Generating
}

object ShowBASTDialog : ReportGenerationUiState()
object ShowHandoverDialog : ReportGenerationUiState()

data class BASTData(
    val handoverDate: String,
    val handoverTime: String,
    val handoverLocation: String,
    val witnessName: String,
    val witnessPosition: String
)

data class HandoverCertificateData(
    val handoverDate: String,
    val propertyCondition: String,
    val includedItems: List<String>,
    val specialNotes: String?
)
