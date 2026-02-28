package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.viewmodel.SLADashboardViewModel
import com.kprflow.enterprise.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlaDashboardScreen(
    onNavigateToSLADetail: (String) -> Unit,
    viewModel: SLADashboardViewModel = hiltViewModel()
) {
    val slaSummary by viewModel.slaSummary.collectAsStateWithLifecycle()
    val allSLAStatuses by viewModel.allSLAStatuses.collectAsStateWithLifecycle()
    val overdueDossiers by viewModel.overdueDossiers.collectAsStateWithLifecycle()
    val criticalDossiers by viewModel.criticalDossiers.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadSLAData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SLA Dashboard")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSLAData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Section
            item {
                SLASummarySection(
                    slaSummary = slaSummary,
                    criticalCount = when (criticalDossiers) {
                        is Resource.Success -> criticalDossiers.data.size
                        else -> 0
                    },
                    overdueCount = when (overdueDossiers) {
                        is Resource.Success -> overdueDossiers.data.size
                        else -> 0
                    }
                )
            }
            
            // Quick Stats Cards
            item {
                QuickStatsGrid(
                    slaSummary = slaSummary,
                    criticalDossiers = criticalDossiers,
                    overdueDossiers = overdueDossiers
                )
            }
            
            // Critical Dossiers Alert
            item {
                when (criticalDossiers) {
                    is Resource.Success -> {
                        if (criticalDossiers.data.isNotEmpty()) {
                            CriticalDossiersAlert(
                                criticalCount = criticalDossiers.data.size,
                                onViewAll = { /* Navigate to critical dossiers */ }
                            )
                        }
                    }
                    else -> {}
                }
            }
            
            // SLA Status Cards Grid
            item {
                when (allSLAStatuses) {
                    is Resource.Success -> {
                        SLAStatusesGrid(
                            slaStatuses = allSLAStatuses.data.take(6), // Show top 6
                            onCardClick = onNavigateToSLADetail
                        )
                    }
                    is Resource.Loading -> {
                        LoadingGrid()
                    }
                    is Resource.Error -> {
                        ErrorCard(
                            message = allSLAStatuses.message ?: "Unknown error",
                            onRetry = { viewModel.loadSLAData() }
                        )
                    }
                }
            }
            
            // Overdue Dossiers Section
            item {
                when (overdueDossiers) {
                    is Resource.Success -> {
                        if (overdueDossiers.data.isNotEmpty()) {
                            OverdueDossiersSection(
                                overdueDossiers = overdueDossiers.data,
                                onCardClick = onNavigateToSLADetail
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun SLASummarySection(
    slaSummary: Resource<com.kprflow.enterprise.domain.repository.SLASummary>,
    criticalCount: Int,
    overdueCount: Int
) {
    when (slaSummary) {
        is Resource.Success -> {
            val summary = slaSummary.data
            
            BentoBox {
                BentoHeader(
                    title = "SLA Overview",
                    subtitle = "Real-time Service Level Agreement monitoring"
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Main Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Total Active",
                        value = summary.totalDossiers.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    MetricCard(
                        title = "On Track",
                        value = summary.normalCount.toString(),
                        color = com.kprflow.enterprise.ui.theme.Success,
                        modifier = Modifier.weight(1f)
                    )
                    
                    MetricCard(
                        title = "Warning",
                        value = summary.warningCount.toString(),
                        color = com.kprflow.enterprise.ui.theme.Warning,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Critical and Overdue Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Critical",
                        value = criticalCount.toString(),
                        color = Color(0xFFFF6F00),
                        modifier = Modifier.weight(1f)
                    )
                    
                    MetricCard(
                        title = "Overdue",
                        value = overdueCount.toString(),
                        color = com.kprflow.enterprise.ui.theme.Error,
                        modifier = Modifier.weight(1f)
                    )
                    
                    MetricCard(
                        title = "Avg Days",
                        value = String.format("%.1f", summary.avgDaysRemaining),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        is Resource.Loading -> {
            BentoBox {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Loading SLA summary...")
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun QuickStatsGrid(
    slaSummary: Resource<com.kprflow.enterprise.domain.repository.SLASummary>,
    criticalDossiers: Resource<List<com.kprflow.enterprise.domain.repository.SLAStatus>>,
    overdueDossiers: Resource<List<com.kprflow.enterprise.domain.repository.SLAStatus>>
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(200.dp)
    ) {
        item {
            QuickStatCard(
                title = "Document SLA",
                subtitle = "14-day deadline",
                value = when (slaSummary) {
                    is Resource.Success -> slaSummary.data.avgDaysRemaining.toString()
                    else -> "--"
                },
                unit = "days avg",
                color = com.kprflow.enterprise.ui.theme.Success
            )
        }
        
        item {
            QuickStatCard(
                title = "Bank SLA",
                subtitle = "60-day deadline",
                value = when (slaSummary) {
                    is Resource.Success -> String.format("%.1f", slaSummary.data.avgDaysRemaining)
                    else -> "--"
                },
                unit = "days avg",
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        item {
            QuickStatCard(
                title = "Critical",
                subtitle = "Immediate action",
                value = when (criticalDossiers) {
                    is Resource.Success -> criticalDossiers.data.size.toString()
                    else -> "0"
                },
                unit = "dossiers",
                color = Color(0xFFFF6F00)
            )
        }
        
        item {
            QuickStatCard(
                title = "Overdue",
                subtitle = "Missed deadlines",
                value = when (overdueDossiers) {
                    is Resource.Success -> overdueDossiers.data.size.toString()
                    else -> "0"
                },
                unit = "dossiers",
                color = com.kprflow.enterprise.ui.theme.Error
            )
        }
    }
}

@Composable
private fun SLAStatusesGrid(
    slaStatuses: List<com.kprflow.enterprise.domain.repository.SLAStatus>,
    onCardClick: (String) -> Unit
) {
    BentoBox {
        BentoHeader(
            title = "Active Dossiers",
            subtitle = "${slaStatuses.size} dossiers with SLA tracking"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(400.dp)
        ) {
            items(slaStatuses) { slaStatus ->
                CompactSLACard(
                    customerName = slaStatus.customerName,
                    docDaysLeft = slaStatus.docDaysLeft,
                    bankDaysLeft = slaStatus.bankDaysLeft,
                    status = slaStatus.slaStatus,
                    onClick = { onCardClick(slaStatus.dossierId) }
                )
            }
        }
    }
}

@Composable
private fun CompactSLACard(
    customerName: String,
    docDaysLeft: Int,
    bankDaysLeft: Int,
    status: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = customerName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Docs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$docDaysLeft days",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            docDaysLeft <= 0 -> com.kprflow.enterprise.ui.theme.Error
                            docDaysLeft <= 3 -> Color(0xFFFF6F00)
                            docDaysLeft <= 7 -> com.kprflow.enterprise.ui.theme.Warning
                            else -> com.kprflow.enterprise.ui.theme.Success
                        }
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Bank",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$bankDaysLeft days",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            bankDaysLeft <= 0 -> com.kprflow.enterprise.ui.theme.Error
                            bankDaysLeft <= 3 -> Color(0xFFFF6F00)
                            bankDaysLeft <= 7 -> com.kprflow.enterprise.ui.theme.Warning
                            else -> com.kprflow.enterprise.ui.theme.Success
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                color = when (status) {
                    "CRITICAL" -> Color(0xFFFF6F00).copy(alpha = 0.2f)
                    "WARNING" -> com.kprflow.enterprise.ui.theme.Warning.copy(alpha = 0.2f)
                    "DOC_OVERDUE", "BANK_OVERDUE" -> com.kprflow.enterprise.ui.theme.Error.copy(alpha = 0.2f)
                    else -> com.kprflow.enterprise.ui.theme.Success.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = status.replace("_", " "),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (status) {
                        "CRITICAL" -> Color(0xFFFF6F00)
                        "WARNING" -> com.kprflow.enterprise.ui.theme.Warning
                        "DOC_OVERDUE", "BANK_OVERDUE" -> com.kprflow.enterprise.ui.theme.Error
                        else -> com.kprflow.enterprise.ui.theme.Success
                    }
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        backgroundColor = color.copy(alpha = 0.1f),
        borderColor = color.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    subtitle: String,
    value: String,
    unit: String,
    color: androidx.compose.ui.graphics.Color
) {
    GlassCard(
        backgroundColor = color.copy(alpha = 0.1f),
        borderColor = color.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CriticalDossiersAlert(
    criticalCount: Int,
    onViewAll: () -> Unit
) {
    SLAWarningBanner(
        warningLevel = com.kprflow.enterprise.domain.usecase.sla.SLAWarningLevel.CRITICAL,
        message = "You have $criticalCount critical dossiers requiring immediate attention.",
        onDismiss = null
    )
}

@Composable
private fun OverdueDossiersSection(
    overdueDossiers: List<com.kprflow.enterprise.domain.repository.SLAStatus>,
    onCardClick: (String) -> Unit
) {
    BentoBox {
        BentoHeader(
            title = "Overdue Dossiers",
            subtitle = "${overdueDossiers.size} dossiers with missed deadlines"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            overdueDossiers.take(3).forEach { overdue ->
                CompactSLACard(
                    customerName = overdue.customerName,
                    docDaysLeft = overdue.docDaysLeft,
                    bankDaysLeft = overdue.bankDaysLeft,
                    status = overdue.slaStatus,
                    onClick = { onCardClick(overdue.dossierId) }
                )
            }
            
            if (overdueDossiers.size > 3) {
                TextButton(
                    onClick = { /* View all overdue */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View all ${overdueDossiers.size} overdue dossiers")
                }
            }
        }
    }
}

@Composable
private fun LoadingGrid() {
    BentoBox {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Loading SLA statuses...")
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    BentoBox {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error loading data",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
