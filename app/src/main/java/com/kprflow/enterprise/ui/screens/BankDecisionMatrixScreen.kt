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
import com.kprflow.enterprise.ui.components.BankDecisionCard
import com.kprflow.enterprise.ui.components.BankDecisionStatsCard
import com.kprflow.enterprise.ui.viewmodel.BankDecisionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDecisionMatrixScreen(
    onBackClick: () -> Unit,
    viewModel: BankDecisionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadDecisions()
        viewModel.loadStats()
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
                text = "Bank Decision Matrix",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = { /* TODO: Open upload dialog */ }
            ) {
                Text("Upload Decision")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Statistics Card
        when (statsState) {
            is BankDecisionStatsState.Success -> {
                BankDecisionStatsCard(stats = statsState.stats)
            }
            is BankDecisionStatsState.Loading -> {
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
        
        // Filter Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                onClick = { viewModel.setFilter(BankDecisionFilter.ALL) },
                label = { Text("All") },
                selected = viewModel.selectedFilter == BankDecisionFilter.ALL
            )
            
            FilterChip(
                onClick = { viewModel.setFilter(BankDecisionFilter.APPROVED) },
                label = { Text("Approved") },
                selected = viewModel.selectedFilter == BankDecisionFilter.APPROVED
            )
            
            FilterChip(
                onClick = { viewModel.setFilter(BankDecisionFilter.REJECTED) },
                label = { Text("Rejected") },
                selected = viewModel.selectedFilter == BankDecisionFilter.REJECTED
            )
            
            FilterChip(
                onClick = { viewModel.setFilter(BankDecisionFilter.PENDING) },
                label = { Text("Pending") },
                selected = viewModel.selectedFilter == BankDecisionFilter.PENDING
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Decisions List
        when (uiState) {
            is BankDecisionUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is BankDecisionUiState.Success -> {
                val decisions = viewModel.getFilteredDecisions(uiState.decisions)
                
                if (decisions.isEmpty()) {
                    EmptyDecisionsState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(decisions) { decision ->
                            BankDecisionCard(
                                decision = decision,
                                onDownload = { viewModel.downloadDecision(decision.id) },
                                onDelete = { viewModel.deleteDecision(decision.id) },
                                onUpdateStatus = { status, notes ->
                                    viewModel.updateDecisionStatus(decision.id, status, notes)
                                }
                            )
                        }
                    }
                }
            }
            
            is BankDecisionUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = { viewModel.loadDecisions() }
                )
            }
        }
    }
}

@Composable
private fun EmptyDecisionsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Bank Decisions Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Upload bank decisions to see them here",
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
            text = "Error loading decisions",
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
sealed class BankDecisionUiState {
    object Loading : BankDecisionUiState()
    data class Success(val decisions: List<com.kprflow.enterprise.data.repository.BankDecisionRecord>) : BankDecisionUiState()
    data class Error(val message: String) : BankDecisionUiState()
}

sealed class BankDecisionStatsState {
    object Loading : BankDecisionStatsState()
    data class Success(val stats: com.kprflow.enterprise.data.repository.BankDecisionStats) : BankDecisionStatsState()
    data class Error(val message: String) : BankDecisionStatsState()
}

enum class BankDecisionFilter {
    ALL, APPROVED, REJECTED, PENDING
}
