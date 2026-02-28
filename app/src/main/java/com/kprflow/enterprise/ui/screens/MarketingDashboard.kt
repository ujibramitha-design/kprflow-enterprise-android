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
import com.kprflow.enterprise.ui.viewmodel.MarketingDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketingDashboard(
    onDossierClick: (String) -> Unit,
    onUnitManagementClick: () -> Unit,
    onCampaignClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    viewModel: MarketingDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statisticsState by viewModel.statisticsState.collectAsState()
    val campaignsState by viewModel.campaignsState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadMarketingData()
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
                text = "Marketing Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                IconButton(onClick = onCampaignClick) {
                    Icon(Icons.Default.Campaign, contentDescription = "Campaigns")
                }
                IconButton(onClick = onAnalyticsClick) {
                    Icon(Icons.Default.Analytics, contentDescription = "Analytics")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Statistics Cards
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(200.dp)
        ) {
            item {
                MarketingStatsCard(
                    title = "Total Leads",
                    value = statisticsState.totalLeads.toString(),
                    change = "+12%",
                    isPositive = true,
                    icon = Icons.Default.People,
                    onClick = { /* Navigate to leads */ }
                )
            }
            
            item {
                MarketingStatsCard(
                    title = "Active Campaigns",
                    value = statisticsState.activeCampaigns.toString(),
                    change = "+3",
                    isPositive = true,
                    icon = Icons.Default.Campaign,
                    onClick = onCampaignClick
                )
            }
            
            item {
                MarketingStatsCard(
                    title = "Conversion Rate",
                    value = "${statisticsState.conversionRate}%",
                    change = "+2.1%",
                    isPositive = true,
                    icon = Icons.Default.TrendingUp,
                    onClick = onAnalyticsClick
                )
            }
            
            item {
                MarketingStatsCard(
                    title = "Units Available",
                    value = statisticsState.availableUnits.toString(),
                    change = "-5",
                    isPositive = false,
                    icon = Icons.Default.Home,
                    onClick = onUnitManagementClick
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Recent Leads
        Text(
            text = "Recent Leads",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        when (uiState) {
            is MarketingDashboardState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is MarketingDashboardState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(uiState.recentLeads) { lead ->
                        MarketingLeadCard(
                            lead = lead,
                            onClick = { onDossierClick(lead.id) }
                        )
                    }
                }
            }
            
            is MarketingDashboardState.Error -> {
                ErrorCard(
                    message = uiState.message,
                    onRetry = { viewModel.loadMarketingData() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Active Campaigns
        Text(
            text = "Active Campaigns",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        when (campaignsState) {
            is MarketingDashboardState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is MarketingDashboardState.Success -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(campaignsState.campaigns) { campaign ->
                        MarketingCampaignCard(
                            campaign = campaign,
                            onClick = { /* Navigate to campaign details */ }
                        )
                    }
                }
            }
            
            is MarketingDashboardState.Error -> {
                ErrorCard(
                    message = campaignsState.message,
                    onRetry = { viewModel.loadCampaigns() }
                )
            }
        }
    }
}

@Composable
private fun MarketingStatsCard(
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
private fun MarketingLeadCard(
    lead: MarketingLead,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lead.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = lead.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Created: ${lead.createdAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Badge {
                    Text(lead.status)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = lead.interest,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MarketingCampaignCard(
    campaign: MarketingCampaign,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(200.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = campaign.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${campaign.leadsGenerated} leads",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Conversion: ${campaign.conversionRate}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = campaign.progress / 100f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Data classes
data class MarketingLead(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val status: String,
    val interest: String,
    val createdAt: String
)

data class MarketingCampaign(
    val id: String,
    val name: String,
    val leadsGenerated: Int,
    val conversionRate: Int,
    val progress: Int
)

// UI State
sealed class MarketingDashboardState {
    object Loading : MarketingDashboardState()
    data class Success(val recentLeads: List<MarketingLead>) : MarketingDashboardState()
    data class Error(val message: String) : MarketingDashboardState()
}
