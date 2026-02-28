package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.components.KanbanBoard
import com.kprflow.enterprise.ui.viewmodel.LegalKanbanViewModel
import com.kprflow.enterprise.ui.screens.LegalKanbanUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalKanban(
    onDossierClick: (String) -> Unit,
    viewModel: LegalKanbanViewModel = hiltViewModel()
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
                text = "KPR Application Pipeline",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Refresh Button
            IconButton(
                onClick = { viewModel.refreshDossiers() }
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stats Row
        when (uiState) {
            is LegalKanbanUiState.Success -> {
                StatsRow(dossiers = uiState.dossiers)
            }
            else -> {}
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Kanban Board
        when (uiState) {
            is LegalKanbanUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is LegalKanbanUiState.Success -> {
                KanbanBoard(
                    dossiers = uiState.dossiers,
                    onStatusChange = { dossierId, newStatus ->
                        viewModel.updateDossierStatus(dossierId, newStatus)
                    },
                    onDossierClick = onDossierClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            is LegalKanbanUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = { viewModel.refreshDossiers() }
                )
            }
        }
    }
}

@Composable
private fun StatsRow(dossiers: List<com.kprflow.enterprise.data.model.KprDossier>) {
    val totalDossiers = dossiers.size
    val inProgress = dossiers.count { it.status in listOf(
        com.kprflow.enterprise.data.model.KprStatus.PEMBERKASAN,
        com.kprflow.enterprise.data.model.KprStatus.PROSES_BANK,
        com.kprflow.enterprise.data.model.KprStatus.PUTUSAN_KREDIT_ACC
    )}
    val completed = dossiers.count { it.status in listOf(
        com.kprflow.enterprise.data.model.KprStatus.SP3K_TERBIT,
        com.kprflow.enterprise.data.model.KprStatus.PRA_AKAD
    )}
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(title = "Total", value = totalDossiers.toString())
        StatCard(title = "In Progress", value = inProgress.toString())
        StatCard(title = "Completed", value = completed.toString())
    }
}

@Composable
private fun StatCard(title: String, value: String) {
    Card(
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = title,
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

