package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.kprflow.enterprise.ui.viewmodel.ExecutiveReportsViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutiveReportsScreen(
    onBackClick: () -> Unit,
    onExportReport: (String) -> Unit,
    viewModel: ExecutiveReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val summaryState by viewModel.summaryState.collectAsState()
    val performanceState by viewModel.performanceState.collectAsState()
    val financialState by viewModel.financialState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadExecutiveReports()
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
                text = "Executive Reports",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                IconButton(onClick = { viewModel.refreshData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = { onExportReport("executive_summary") }) {
                    Icon(Icons.Default.Download, contentDescription = "Export")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Executive Summary Cards
        Text(
            text = "Executive Summary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(200.dp)
        ) {
            item {
                ExecutiveSummaryCard(
                    title = "Total Portfolio",
                    value = currencyFormatter.format(summaryState.totalPortfolio),
                    change = "+18.5%",
                    isPositive = true,
                    icon = Icons.Default.AccountBalance,
                    subtitle = "This quarter"
                )
            }
            
            item {
                ExecutiveSummaryCard(
                    title = "Active Applications",
                    value = summaryState.activeApplications.toString(),
                    change = "+25",
                    isPositive = true,
                    icon = Icons.Default.Assignment,
                    subtitle = "This month"
                )
            }
            
            item {
                ExecutiveSummaryCard(
                    title = "Success Rate",
                    value = "${summaryState.successRate}%",
                    change = "+3.2%",
                    isPositive = true,
                    icon = Icons.Default.TrendingUp,
                    subtitle = "Approval rate"
                )
            }
            
            item {
                ExecutiveSummaryCard(
                    title = "Avg Processing Time",
                    value = "${summaryState.avgProcessingTime} days",
                    change = "-2.1",
                    isPositive = true,
                    icon = Icons.Default.Schedule,
                    subtitle = "Improvement"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Performance Metrics
        Text(
            text = "Performance Metrics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        when (performanceState) {
            is ExecutiveReportsState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is ExecutiveReportsState.Success -> {
                // Funnel Chart
                StatsCardWithChart(
                    title = "Application Funnel",
                    value = "${performanceState.conversionRate}%",
                    subtitle = "Overall conversion rate",
                    chartData = performanceState.funnelData,
                    modifier = Modifier.fillMaxWidth(),
                    chartType = ChartType.FUNNEL
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Revenue Trend
                StatsCardWithChart(
                    title = "Revenue Trend",
                    value = currencyFormatter.format(performanceState.currentRevenue),
                    subtitle = "This month revenue",
                    chartData = performanceState.revenueData,
                    modifier = Modifier.fillMaxWidth(),
                    chartType = ChartType.LINE
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Department Performance
                StatsCardWithChart(
                    title = "Department Performance",
                    value = "85%",
                    subtitle = "Overall efficiency",
                    chartData = performanceState.departmentData,
                    modifier = Modifier.fillMaxWidth(),
                    chartType = ChartType.BAR
                )
            }
            
            is ExecutiveReportsState.Error -> {
                ErrorCard(
                    message = performanceState.message,
                    onRetry = { viewModel.loadPerformanceMetrics() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Financial Reports
        Text(
            text = "Financial Reports",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        when (financialState) {
            is ExecutiveReportsState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is ExecutiveReportsState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    item {
                        FinancialReportCard(
                            title = "Revenue Analysis",
                            period = "Q4 2024",
                            totalRevenue = currencyFormatter.format(financialState.quarterlyRevenue),
                            growth = "+15.3%",
                            profit = currencyFormatter.format(financialState.quarterlyProfit),
                            profitMargin = "23.5%",
                            onClick = { onExportReport("revenue_analysis") }
                        )
                    }
                    
                    item {
                        FinancialReportCard(
                            title = "Cost Analysis",
                            period = "Q4 2024",
                            totalRevenue = currencyFormatter.format(financialState.totalCosts),
                            growth = "+8.7%",
                            profit = currencyFormatter.format(financialState.operatingCosts),
                            profitMargin = "12.3%",
                            onClick = { onExportReport("cost_analysis") }
                        )
                    }
                    
                    item {
                        FinancialReportCard(
                            title = "Cash Flow Report",
                            period = "Q4 2024",
                            totalRevenue = currencyFormatter.format(financialState.cashFlow),
                            growth = "+22.1%",
                            profit = currencyFormatter.format(financialState.netCashFlow),
                            profitMargin = "18.9%",
                            onClick = { onExportReport("cash_flow") }
                        )
                    }
                }
            }
            
            is ExecutiveReportsState.Error -> {
                ErrorCard(
                    message = financialState.message,
                    onRetry = { viewModel.loadFinancialReports() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick Actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ExecutiveActionCard(
                    title = "Generate Full Report",
                    description = "Complete executive report",
                    icon = Icons.Default.Assessment,
                    onClick = { onExportReport("full_executive") }
                )
            }
            
            item {
                ExecutiveActionCard(
                    title = "Board Presentation",
                    description = "Board-ready presentation",
                    icon = Icons.Default.Slideshow,
                    onClick = { onExportReport("board_presentation") }
                )
            }
            
            item {
                ExecutiveActionCard(
                    title = "Investor Report",
                    description = "Investor-focused report",
                    icon = Icons.Default.TrendingUp,
                    onClick = { onExportReport("investor_report") }
                )
            }
        }
    }
}

@Composable
private fun ExecutiveSummaryCard(
    title: String,
    value: String,
    change: String,
    isPositive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FinancialReportCard(
    title: String,
    period: String,
    totalRevenue: String,
    growth: String,
    profit: String,
    profitMargin: String,
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
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = period,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Revenue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalRevenue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Growth",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = growth,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Profit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = profit,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Margin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = profitMargin,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ExecutiveActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

// Data classes for executive reports
data class ExecutiveSummary(
    val totalPortfolio: Double,
    val activeApplications: Int,
    val successRate: Int,
    val avgProcessingTime: Int
)

data class PerformanceMetrics(
    val conversionRate: Int,
    val currentRevenue: Double,
    val funnelData: List<ChartData>,
    val revenueData: List<ChartData>,
    val departmentData: List<ChartData>
)

data class FinancialReports(
    val quarterlyRevenue: Double,
    val quarterlyProfit: Double,
    val totalCosts: Double,
    val operatingCosts: Double,
    val cashFlow: Double,
    val netCashFlow: Double
)

// UI State
sealed class ExecutiveReportsState {
    object Loading : ExecutiveReportsState()
    data class Success<T>(val data: T) : ExecutiveReportsState()
    data class Error(val message: String) : ExecutiveReportsState()
}
