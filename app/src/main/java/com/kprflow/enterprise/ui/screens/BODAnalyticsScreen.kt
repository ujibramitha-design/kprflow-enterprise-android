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
import com.kprflow.enterprise.ui.components.AnalyticsCard
import com.kprflow.enterprise.ui.components.FunnelChart
import com.kprflow.enterprise.ui.components.RevenueChart
import com.kprflow.enterprise.ui.viewmodel.BODAnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BODAnalyticsScreen(
    onBackClick: () -> Unit,
    viewModel: BODAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val funnelState by viewModel.funnelState.collectAsState()
    val processingTimeState by viewModel.processingTimeState.collectAsState()
    val bankStatsState by viewModel.bankStatsState.collectAsState()
    val revenueState by viewModel.revenueState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadAllAnalytics()
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
                text = "BOD Analytics Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = { viewModel.refreshAllAnalytics() }
            ) {
                Text("Refresh")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Analytics Content
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPR Pipeline Funnel
            item {
                when (funnelState) {
                    is AnalyticsState.Loading -> {
                        AnalyticsCard(
                            title = "KPR Pipeline Funnel",
                            isLoading = true
                        )
                    }
                    is AnalyticsState.Success -> {
                        FunnelChart(
                            funnel = funnelState.data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is AnalyticsState.Error -> {
                        AnalyticsCard(
                            title = "KPR Pipeline Funnel",
                            errorMessage = funnelState.message
                        )
                    }
                }
            }
            
            // Processing Time Analytics
            item {
                when (processingTimeState) {
                    is AnalyticsState.Loading -> {
                        AnalyticsCard(
                            title = "Phase Processing Times",
                            isLoading = true
                        )
                    }
                    is AnalyticsState.Success -> {
                        ProcessingTimeAnalytics(
                            processingTimes = processingTimeState.data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is AnalyticsState.Error -> {
                        AnalyticsCard(
                            title = "Phase Processing Times",
                            errorMessage = processingTimeState.message
                        )
                    }
                }
            }
            
            // Bank Approval Statistics
            item {
                when (bankStatsState) {
                    is AnalyticsState.Loading -> {
                        AnalyticsCard(
                            title = "Bank Approval Statistics",
                            isLoading = true
                        )
                    }
                    is AnalyticsState.Success -> {
                        BankApprovalAnalytics(
                            bankStats = bankStatsState.data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is AnalyticsState.Error -> {
                        AnalyticsCard(
                            title = "Bank Approval Statistics",
                            errorMessage = bankStatsState.message
                        )
                    }
                }
            }
            
            // Revenue Projection
            item {
                when (revenueState) {
                    is AnalyticsState.Loading -> {
                        AnalyticsCard(
                            title = "Revenue Projection",
                            isLoading = true
                        )
                    }
                    is AnalyticsState.Success -> {
                        RevenueChart(
                            projection = revenueState.data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is AnalyticsState.Error -> {
                        AnalyticsCard(
                            title = "Revenue Projection",
                            errorMessage = revenueState.message
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessingTimeAnalytics(
    processingTimes: List<com.kprflow.enterprise.data.repository.PhaseProcessingTime>,
    modifier: Modifier = Modifier
) {
    AnalyticsCard(
        title = "Phase Processing Times",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            processingTimes.forEach { phase ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = phase.phaseName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Sample: ${phase.sampleSize} cases",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${String.format("%.1f", phase.averageDays)} days",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${String.format("%.1f", phase.minDays)} - ${String.format("%.1f", phase.maxDays)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (processingTimes.last() != phase) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun BankApprovalAnalytics(
    bankStats: List<com.kprflow.enterprise.data.repository.BankApprovalStats>,
    modifier: Modifier = Modifier
) {
    AnalyticsCard(
        title = "Bank Approval Statistics",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            bankStats.forEach { bank ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = bank.bankName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Total: ${bank.totalDecisions} decisions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${String.format("%.1f", bank.approvalRate)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (bank.approvalRate >= 70) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Approval Rate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Approval/Rejection bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Approved: ${bank.approvedDecisions}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(bank.approvalRate / 100)
                    )
                    Text(
                        text = "Rejected: ${bank.rejectedDecisions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(bank.rejectionRate / 100)
                    )
                }
                
                if (bankStats.last() != bank) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

// UI State for analytics
sealed class AnalyticsState<T> {
    object Loading : AnalyticsState<Nothing>()
    data class Success<T>(val data: T) : AnalyticsState<T>()
    data class Error(val message: String) : AnalyticsState<Nothing>()
}
