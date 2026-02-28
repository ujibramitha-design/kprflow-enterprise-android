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
import com.kprflow.enterprise.ui.components.BankSubmissionCard
import com.kprflow.enterprise.ui.viewmodel.DocumentMergerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentMergerScreen(
    dossierId: String,
    onBackClick: () -> Unit,
    viewModel: DocumentMergerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(dossierId) {
        viewModel.loadSubmissions(dossierId)
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
                text = "Document Merger",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = { viewModel.createBankSubmission(dossierId) },
                enabled = uiState is DocumentMergerUiState.Success
            ) {
                Text("Create Bank Submission")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (uiState) {
            is DocumentMergerUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is DocumentMergerUiState.Success -> {
                if (uiState.submissions.isEmpty()) {
                    EmptySubmissionsState(
                        onCreateClick = { viewModel.createBankSubmission(dossierId) }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.submissions) { submission ->
                            BankSubmissionCard(
                                submission = submission,
                                onDownloadClick = { viewModel.downloadSubmission(submission.id) },
                                onDeleteClick = { viewModel.deleteSubmission(submission.id) }
                            )
                        }
                    }
                }
            }
            
            is DocumentMergerUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = { viewModel.loadSubmissions(dossierId) }
                )
            }
        }
    }
}

@Composable
private fun EmptySubmissionsState(
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Bank Submissions Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Create a merged PDF package for bank submission",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onCreateClick
        ) {
            Text("Create Bank Submission")
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
            text = "Error loading submissions",
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
sealed class DocumentMergerUiState {
    object Loading : DocumentMergerUiState()
    data class Success(val submissions: List<com.kprflow.enterprise.data.model.BankSubmissionRecord>) : DocumentMergerUiState()
    data class Error(val message: String) : DocumentMergerUiState()
}
