package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.viewmodel.BODDashboardViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BODDashboard(
    onDossierClick: (String) -> Unit,
    onQuorumVotingClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: BODDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statisticsState by viewModel.statisticsState.collectAsState()
    val quorumState by viewModel.quorumState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadBODData()
    }
    
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID"))
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
                text = "Board of Directors Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                IconButton(onClick = onQuorumVotingClick) {
                    Icon(Icons.Default.HowToVote, contentDescription = "Quorum Voting")
                }
                IconButton(onClick = onAnalyticsClick) {
                    Icon(Icons.Default.Analytics, contentDescription = "Analytics")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Executive Statistics
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(200.dp)
        ) {
            item {
                BODStatsCard(
                    title = "Total Portfolio Value",
                    value = currencyFormatter.format(statisticsState.totalPortfolioValue),
                    change = "+18%",
                    isPositive = true,
                    icon = Icons.Default.AccountBalance,
                    onClick = onAnalyticsClick
                )
            }
            
            item {
                BODStatsCard(
                    title = "Active Applications",
                    value = statisticsState.activeApplications.toString(),
                    change = "+25",
                    isPositive = true,
                    icon = Icons.Default.Assignment,
                    onClick = { /* Navigate to applications */ }
                )
            }
            
            item {
                BODStatsCard(
                    title = "Success Rate",
                    value = "${statisticsState.successRate}%",
                    change = "+3.2%",
                    isPositive = true,
                    icon = Icons.Default.TrendingUp,
                    onClick = onAnalyticsClick
                )
            }
            
            item {
                BODStatsCard(
                    title = "Active Quorums",
                    value = statisticsState.activeQuorums.toString(),
                    change = "+2",
                    isPositive = true,
                    icon = Icons.Default.HowToVote,
                    onClick = onQuorumVotingClick
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Executive Analytics Dashboard - Phase 25
        ExecutiveAnalyticsSection(
            statisticsState = statisticsState,
            currencyFormatter = currencyFormatter,
            onAnalyticsClick = onAnalyticsClick
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Critical Quorums
        Text(
            text = "Critical Quorums",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        when (quorumState) {
            is BODDashboardState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is BODDashboardState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(250.dp)
                ) {
                    items(quorumState.quorums) { quorum ->
                        BODQuorumCard(
                            quorum = quorum,
                            onVote = { viewModel.castVote(quorum.id, it) },
                            onClick = { onQuorumVotingClick }
                        )
                    }
                }
            }
            
            is BODDashboardState.Error -> {
                ErrorCard(
                    message = quorumState.message,
                    onRetry = { viewModel.loadQuorums() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Performance Metrics
        Text(
            text = "Performance Metrics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BODMetricCard(
                    title = "Monthly Revenue",
                    value = currencyFormatter.format(statisticsState.monthlyRevenue),
                    subtitle = "This month",
                    icon = Icons.Default.MonetizationOn,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                BODMetricCard(
                    title = "Customer Satisfaction",
                    value = "${statisticsState.customerSatisfaction}%",
                    subtitle = "Average rating",
                    icon = Icons.Default.SentimentVerySatisfied,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            item {
                BODMetricCard(
                    title = "Processing Time",
                    value = "${statisticsState.avgProcessingTime} days",
                    subtitle = "Average",
                    icon = Icons.Default.Schedule,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Actions
        Text(
            text = "Executive Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BODActionCard(
                    title = "Quorum Voting",
                    description = "Cast your vote",
                    icon = Icons.Default.HowToVote,
                    onClick = onQuorumVotingClick
                )
            }
            
            item {
                BODActionCard(
                    title = "Analytics",
                    description = "View insights",
                    icon = Icons.Default.Analytics,
                    onClick = onAnalyticsClick
                )
            }
            
            item {
                BODActionCard(
                    title = "Settings",
                    description = "System configuration",
                    icon = Icons.Default.Settings,
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
private fun BODStatsCard(
    title: String,
    value: String,
    change: String,
    isPositive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = change,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPositive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BODQuorumCard(
    quorum: BODQuorum,
    onVote: (String) -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quorum.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = quorum.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Deadline: ${quorum.deadline}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Badge {
                        Text(quorum.priority)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${quorum.votesFor}/${quorum.requiredVotes}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onVote("approve") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Approve")
                }
                
                OutlinedButton(
                    onClick = { onVote("reject") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
private fun BODMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.width(160.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = color
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun BODActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Data classes
data class BODQuorum(
    val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val deadline: String,
    val votesFor: Int,
    val requiredVotes: Int
)

// UI State
sealed class BODDashboardState {
    object Loading : BODDashboardState()
    data class Success(val quorums: List<BODQuorum>) : BODDashboardState()
    data class Error(val message: String) : BODDashboardState()
}
