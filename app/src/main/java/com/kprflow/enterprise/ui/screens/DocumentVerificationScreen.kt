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
import com.kprflow.enterprise.ui.components.DocumentVerificationCard
import com.kprflow.enterprise.ui.components.VerificationStatsCard
import com.kprflow.enterprise.ui.viewmodel.DocumentVerificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentVerificationScreen(
    onBackClick: () -> Unit,
    viewModel: DocumentVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadPendingDocuments()
        viewModel.loadVerificationStats()
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
                text = "Document Verification",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Filter options
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { viewModel.setFilter(DocumentVerificationFilter.ALL) },
                    label = { Text("All") },
                    selected = viewModel.selectedFilter == DocumentVerificationFilter.ALL
                )
                
                FilterChip(
                    onClick = { viewModel.setFilter(DocumentVerificationFilter.PENDING) },
                    label = { Text("Pending") },
                    selected = viewModel.selectedFilter == DocumentVerificationFilter.PENDING
                )
                
                FilterChip(
                    onClick = { viewModel.setFilter(DocumentVerificationFilter.VERIFIED) },
                    label = { Text("Verified") },
                    selected = viewModel.selectedFilter == DocumentVerificationFilter.VERIFIED
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Verification Stats
        when (val statsState = viewModel.statsState) {
            is DocumentVerificationStatsState.Success -> {
                VerificationStatsCard(stats = statsState.stats)
            }
            is DocumentVerificationStatsState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {}
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Documents List
        when (uiState) {
            is DocumentVerificationUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is DocumentVerificationUiState.Success -> {
                val documents = viewModel.getFilteredDocuments(uiState.documents)
                
                if (documents.isEmpty()) {
                    EmptyVerificationState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(documents) { document ->
                            DocumentVerificationCard(
                                document = document,
                                onApprove = { viewModel.verifyDocument(document.id, true) },
                                onReject = { reason -> viewModel.verifyDocument(document.id, false, reason) },
                                onBatchAction = { documentIds, isApproved, reason ->
                                    viewModel.batchVerifyDocuments(documentIds, isApproved, reason)
                                }
                            )
                        }
                    }
                }
            }
            
            is DocumentVerificationUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = { viewModel.loadPendingDocuments() }
                )
            }
        }
    }
}

@Composable
private fun EmptyVerificationState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Documents to Verify",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "All documents are up to date",
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
            text = "Error loading documents",
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

// UI States
sealed class DocumentVerificationUiState {
    object Loading : DocumentVerificationUiState()
    data class Success(val documents: List<com.kprflow.enterprise.data.model.Document>) : DocumentVerificationUiState()
    data class Error(val message: String) : DocumentVerificationUiState()
}

sealed class DocumentVerificationStatsState {
    object Loading : DocumentVerificationStatsState()
    data class Success(val stats: com.kprflow.enterprise.data.repository.VerificationStats) : DocumentVerificationStatsState()
    data class Error(val message: String) : DocumentVerificationStatsState()
}

enum class DocumentVerificationFilter {
    ALL, PENDING, VERIFIED, REJECTED
}
