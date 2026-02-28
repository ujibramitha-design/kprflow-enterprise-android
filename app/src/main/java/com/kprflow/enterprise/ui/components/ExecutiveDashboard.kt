package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.viewmodel.AnalyticsViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutiveDashboard(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadExecutiveData()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "📊 Executive Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // KPI Cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(getKPICards(uiState.executiveSummary)) { kpi ->
                    KPICard(kpi = kpi)
                }
            }
            
            // Revenue Chart
            uiState.revenueData?.let { revenue ->
                RevenueChartCard(data = revenue)
            }
            
            // Performance Metrics
            uiState.performanceMetrics?.let { metrics ->
                PerformanceMetricsCard(metrics = metrics)
            }
            
            // Risk Analysis
            uiState.riskAnalysis?.let { risk ->
                RiskAnalysisCard(risk = risk)
            }
            
            // Team Performance
            uiState.teamPerformance?.let { team ->
                TeamPerformanceCard(team = team)
            }
        }
        
        // Error handling
        uiState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun KPICard(kpi: KPIData) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = kpi.backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = kpi.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = kpi.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = kpi.valueColor
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = kpi.trend,
                    style = MaterialTheme.typography.bodySmall,
                    color = kpi.trendColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (kpi.isPositiveTrend) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = kpi.trendColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun RevenueChartCard(data: RevenueData) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "💰 Revenue Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Revenue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(data.totalRevenue),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Monthly Average",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(data.monthlyAverage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Simple bar chart representation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.monthlyData.takeLast(6).forEach { month ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height((month.revenue / data.maxRevenue * 60).dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = month.month.substring(0, 3),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceMetricsCard(metrics: PerformanceMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📈 Performance Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(metrics.items) { metric ->
                    PerformanceMetricItem(metric = metric)
                }
            }
        }
    }
}

@Composable
fun PerformanceMetricItem(metric: MetricItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = metric.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        LinearProgressIndicator(
            progress = metric.progress,
            modifier = Modifier.width(100.dp),
            color = when {
                metric.progress >= 0.8 -> MaterialTheme.colorScheme.primary
                metric.progress >= 0.6 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
fun RiskAnalysisCard(risk: RiskAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚠️ Risk Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Overall Risk Score",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${risk.overallRiskScore}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        risk.overallRiskScore >= 70 -> MaterialTheme.colorScheme.error
                        risk.overallRiskScore >= 40 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                risk.categories.forEach { category ->
                    RiskCategoryItem(category = category)
                }
            }
        }
    }
}

@Composable
fun RiskCategoryItem(category: RiskCategory) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = "${category.count} applications",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Badge(
            containerColor = when {
                category.riskLevel == "HIGH" -> MaterialTheme.colorScheme.errorContainer
                category.riskLevel == "MEDIUM" -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        ) {
            Text(
                text = category.riskLevel,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    category.riskLevel == "HIGH" -> MaterialTheme.colorScheme.onErrorContainer
                    category.riskLevel == "MEDIUM" -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    }
}

@Composable
fun TeamPerformanceCard(team: TeamPerformance) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "👥 Team Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(150.dp)
            ) {
                items(team.members) { member ->
                    TeamMemberItem(member = member)
                }
            }
        }
    }
}

@Composable
fun TeamMemberItem(member: TeamMember) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = member.role,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${member.completedApplications}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Data classes
data class KPIData(
    val title: String,
    val value: String,
    val trend: String,
    val isPositiveTrend: Boolean,
    val backgroundColor: Color,
    val valueColor: Color,
    val trendColor: Color
)

data class RevenueData(
    val totalRevenue: BigDecimal,
    val monthlyAverage: BigDecimal,
    val maxRevenue: BigDecimal,
    val monthlyData: List<MonthlyRevenue>
)

data class MonthlyRevenue(
    val month: String,
    val revenue: BigDecimal
)

data class PerformanceMetrics(
    val items: List<MetricItem>
)

data class MetricItem(
    val label: String,
    val value: String,
    val progress: Float
)

data class RiskAnalysis(
    val overallRiskScore: Int,
    val categories: List<RiskCategory>
)

data class RiskCategory(
    val name: String,
    val count: Int,
    val riskLevel: String
)

data class TeamPerformance(
    val members: List<TeamMember>
)

data class TeamMember(
    val name: String,
    val role: String,
    val completedApplications: Int
)

private fun formatCurrency(amount: BigDecimal): String {
    return "Rp ${String.format("%,.0f", amount)}"
}

private fun getKPICards(summary: ExecutiveSummary?): List<KPIData> {
    if (summary == null) return emptyList()
    
    return listOf(
        KPIData(
            title = "Total Applications",
            value = summary.totalApplications.toString(),
            trend = "+12%",
            isPositiveTrend = true,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            valueColor = MaterialTheme.colorScheme.primary,
            trendColor = MaterialTheme.colorScheme.primary
        ),
        KPIData(
            title = "Conversion Rate",
            value = "${summary.conversionRate}%",
            trend = "+3.2%",
            isPositiveTrend = true,
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            valueColor = MaterialTheme.colorScheme.secondary,
            trendColor = MaterialTheme.colorScheme.secondary
        ),
        KPIData(
            title = "Avg Processing",
            value = "${summary.avgProcessingDays}d",
            trend = "-2.1d",
            isPositiveTrend = true,
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            valueColor = MaterialTheme.colorScheme.tertiary,
            trendColor = MaterialTheme.colorScheme.tertiary
        ),
        KPIData(
            title = "Total Revenue",
            value = formatCurrency(summary.totalRevenue),
            trend = "+18%",
            isPositiveTrend = true,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
            trendColor = MaterialTheme.colorScheme.primary
        )
    )
}

data class ExecutiveSummary(
    val totalApplications: Int,
    val conversionRate: Double,
    val avgProcessingDays: Double,
    val totalRevenue: BigDecimal
)
