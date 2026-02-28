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
import com.kprflow.enterprise.ui.components.SLAOverviewCard
import com.kprflow.enterprise.ui.components.OverdueItemsCard
import com.kprflow.enterprise.ui.components.SLAChart
import com.kprflow.enterprise.ui.viewmodel.SLAMonitoringViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SLAMonitoringScreen(
    onBackClick: () -> Unit,
    viewModel: SLAMonitoringViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val overviewState by viewModel.overviewState.collectAsState()
    val overdueState by viewModel.overdueState.collectAsState()
    val alertsState by viewModel.alertsState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadSLAOverview()
        viewModel.loadOverdueItems()
        viewModel.loadSLAAlerts()
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
                text = "SLA Monitoring Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.refreshAllData() }
                ) {
                    Text("Refresh")
                }
                
                Button(
                    onClick = { viewModel.generateSLAReport() }
                ) {
                    Text("Generate Report")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // SLA Content
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SLA Overview
            item {
                when (overviewState) {
                    is SLAState.Loading -> {
                        SLAOverviewCard(
                            title = "SLA Compliance Overview",
                            isLoading = true
                        )
                    }
                    is SLAState.Success -> {
                        SLAOverviewCard(
                            title = "SLA Compliance Overview",
                            overview = overviewState.data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is SLAState.Error -> {
                        SLAOverviewCard(
                            title = "SLA Compliance Overview",
                            errorMessage = overviewState.message
                        )
                    }
                }
            }
            
            // SLA Chart
            item {
                when (overviewState) {
                    is SLAState.Success -> {
                        SLAChart(
                            overview = overviewState.data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {}
                }
            }
            
            // Overdue Items
            item {
                when (overdueState) {
                    is SLAState.Loading -> {
                        OverdueItemsCard(
                            title = "Overdue Items",
                            isLoading = true
                        )
                    }
                    is SLAState.Success -> {
                        OverdueItemsCard(
                            title = "Overdue Items",
                            overdueItems = overdueState.data,
                            onSendAlert = { itemId, itemType ->
                                viewModel.sendAlert(itemId, itemType)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is SLAState.Error -> {
                        OverdueItemsCard(
                            title = "Overdue Items",
                            errorMessage = overdueState.message
                        )
                    }
                }
            }
            
            // SLA Alerts
            item {
                when (alertsState) {
                    is SLAState.Loading -> {
                        SLAOverviewCard(
                            title = "SLA Alerts",
                            isLoading = true
                        )
                    }
                    is SLAState.Success -> {
                        SLAAlertsCard(
                            alerts = alertsState.data,
                            onAcknowledge = { alertId ->
                                viewModel.acknowledgeAlert(alertId)
                            }
                        )
                    }
                    is SLAState.Error -> {
                        SLAOverviewCard(
                            title = "SLA Alerts",
                            errorMessage = alertsState.message
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SLAAlertsCard(
    alerts: List<com.kprflow.enterprise.data.repository.SLAAlert>,
    onAcknowledge: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "SLA Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (alerts.isEmpty()) {
                Text(
                    text = "No active SLA alerts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                alerts.forEach { alert ->
                    AlertItem(
                        alert = alert,
                        onAcknowledge = onAcknowledge
                    )
                    
                    if (alerts.last() != alert) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertItem(
    alert: com.kprflow.enterprise.data.repository.SLAAlert,
    onAcknowledge: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = alert.itemName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Team: ${alert.responsibleTeam} | Priority: ${alert.priority}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Badge(
                containerColor = when (alert.priority) {
                    "HIGH" -> MaterialTheme.colorScheme.error
                    "MEDIUM" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = "${alert.overdueDays} days",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { onAcknowledge(alert.id) }
            ) {
                Text("Acknowledge")
            }
        }
    }
}

// UI States
sealed class SLAState<T> {
    object Loading : SLAState<Nothing>()
    data class Success<T>(val data: T) : SLAState<T>()
    data class Error(val message: String) : SLAState<Nothing>()
}
