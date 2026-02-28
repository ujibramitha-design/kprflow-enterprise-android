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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.data.model.KprStatus
import com.kprflow.enterprise.ui.components.KprStepper
import com.kprflow.enterprise.ui.viewmodel.CustomerDashboardViewModel
import com.kprflow.enterprise.ui.screens.CustomerDashboardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboard(
    onDossierClick: (String) -> Unit,
    onDocumentUploadClick: (String) -> Unit,
    viewModel: CustomerDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "My KPR Applications",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        when (uiState) {
            is CustomerDashboardUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is CustomerDashboardUiState.Success -> {
                if (uiState.dossiers.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.dossiers) { dossier ->
                            CustomerDossierCard(
                                dossier = dossier,
                                onDossierClick = onDossierClick,
                                onDocumentUploadClick = onDocumentUploadClick
                            )
                        }
                    }
                }
            }
            
            is CustomerDashboardUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = { viewModel.loadDossiers() }
                )
            }
        }
    }
}

@Composable
private fun CustomerDossierCard(
    dossier: com.kprflow.enterprise.data.model.KprDossier,
    onDossierClick: (String) -> Unit,
    onDocumentUploadClick: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onDossierClick(dossier.id) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Dossier Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "KPR Application",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${dossier.id.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Badge
                Badge {
                    Text(
                        text = dossier.status.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Stepper
            KprStepper(
                currentStatus = dossier.status,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onDocumentUploadClick(dossier.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Upload Documents")
                }
                
                Button(
                    onClick = { onDossierClick(dossier.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Details")
                }
            }
            
            // Additional Info
            dossier.kprAmount?.let { amount ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "KPR Amount: Rp ${String.format("%,.0f", amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            dossier.bankName?.let { bank ->
                Text(
                    text = "Bank: $bank",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No KPR Applications Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Start your KPR journey with us",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { /* TODO: Navigate to create new dossier */ }
        ) {
            Text("Apply for KPR")
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
            text = "Something went wrong",
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

