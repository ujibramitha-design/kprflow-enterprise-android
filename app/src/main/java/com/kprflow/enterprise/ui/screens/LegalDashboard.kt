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
import com.kprflow.enterprise.data.model.KprStatus
import com.kprflow.enterprise.ui.components.KanbanBoard
import com.kprflow.enterprise.ui.viewmodel.LegalDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDashboard(
    onDossierClick: (String) -> Unit,
    onDocumentReviewClick: (String) -> Unit,
    viewModel: LegalDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
                text = "Legal Document Review",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { viewModel.setFilter(KprStatus.PEMBERKASAN) },
                    label = { Text("Document Collection") },
                    selected = viewModel.selectedFilter == KprStatus.PEMBERKASAN
                )
                
                FilterChip(
                    onClick = { viewModel.setFilter(KprStatus.PROSES_BANK) },
                    label = { Text("Bank Processing") },
                    selected = viewModel.selectedFilter == KprStatus.PROSES_BANK
                )
                
                FilterChip(
                    onClick = { viewModel.setFilter(null) },
                    label = { Text("All") },
                    selected = viewModel.selectedFilter == null
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (uiState) {
            is LegalDashboardUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is LegalDashboardUiState.Success -> {
                KanbanBoard(
                    dossiersByStatus = uiState.dossiersByStatus,
                    onDossierClick = onDossierClick,
                    onDocumentReviewClick = onDocumentReviewClick,
                    onStatusChange = { dossierId, newStatus ->
                        viewModel.updateDossierStatus(dossierId, newStatus)
                    }
                )
            }
            
            is LegalDashboardUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = { viewModel.loadDossiers() }
                )
            }
        }
    }
}

@Composable
private fun KanbanBoard(
    dossiersByStatus: Map<KprStatus, List<com.kprflow.enterprise.data.model.KprDossier>>,
    onDossierClick: (String) -> Unit,
    onDocumentReviewClick: (String) -> Unit,
    onStatusChange: (String, KprStatus) -> Unit
) {
    val statuses = listOf(
        KprStatus.PEMBERKASAN,
        KprStatus.PROSES_BANK,
        KprStatus.PUTUSAN_KREDIT_ACC,
        KprStatus.SP3K_TERBIT,
        KprStatus.PRA_AKAD
    )
    
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        statuses.forEach { status ->
            KanbanColumn(
                status = status,
                dossiers = dossiersByStatus[status].orEmpty(),
                onDossierClick = onDossierClick,
                onDocumentReviewClick = onDocumentReviewClick,
                onStatusChange = onStatusChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KanbanColumn(
    status: KprStatus,
    dossiers: List<com.kprflow.enterprise.data.model.KprDossier>,
    onDossierClick: (String) -> Unit,
    onDocumentReviewClick: (String) -> Unit,
    onStatusChange: (String, KprStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Column Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = status.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${dossiers.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Dossier Cards
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dossiers) { dossier ->
                LegalDossierCard(
                    dossier = dossier,
                    onDossierClick = onDossierClick,
                    onDocumentReviewClick = onDocumentReviewClick,
                    onStatusChange = onStatusChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegalDossierCard(
    dossier: com.kprflow.enterprise.data.model.KprDossier,
    onDossierClick: (String) -> Unit,
    onDocumentReviewClick: (String) -> Unit,
    onStatusChange: (String, KprStatus) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Dossier Info
            Text(
                text = "ID: ${dossier.id.take(8)}...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Customer: ${dossier.userId.take(8)}...", // TODO: Get actual customer name
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            dossier.kprAmount?.let { amount ->
                Text(
                    text = "Rp ${String.format("%,.0f", amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = { onDocumentReviewClick(dossier.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Review", style = MaterialTheme.typography.bodySmall)
                }
                
                OutlinedButton(
                    onClick = { onDossierClick(dossier.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            // Status Change Dropdown (for specific statuses)
            if (dossier.status in listOf(KprStatus.PEMBERKASAN, KprStatus.PROSES_BANK)) {
                Spacer(modifier = Modifier.height(8.dp))
                
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Move to...")
                    }
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        val nextStatuses = when (dossier.status) {
                            KprStatus.PEMBERKASAN -> listOf(KprStatus.PROSES_BANK)
                            KprStatus.PROSES_BANK -> listOf(KprStatus.PUTUSAN_KREDIT_ACC, KprStatus.PEMBERKASAN)
                            else -> emptyList()
                        }
                        
                        nextStatuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.displayName) },
                                onClick = {
                                    onStatusChange(dossier.id, status)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
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
        Text(
            text = "Error loading dossiers",
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

// UI State
sealed class LegalDashboardUiState {
    object Loading : LegalDashboardUiState()
    data class Success(val dossiersByStatus: Map<KprStatus, List<com.kprflow.enterprise.data.model.KprDossier>>) : LegalDashboardUiState()
    data class Error(val message: String) : LegalDashboardUiState()
}
