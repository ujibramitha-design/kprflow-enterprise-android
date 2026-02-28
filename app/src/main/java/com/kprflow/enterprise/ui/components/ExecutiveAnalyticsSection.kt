package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kprflow.enterprise.ui.viewmodel.BODDashboardViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

/**
 * Executive Analytics Section for BOD Dashboard
 * Phase 25: Executive Analytics Implementation
 * 
 * Displays comprehensive analytics data for Board of Directors including:
 * 1. Realized vs Projected Cash Flow (Bar Chart)
 * 2. Unit Velocity Progress (Progress Bars)
 * 3. Portfolio Performance Metrics
 * 4. Market Trends Analysis
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutiveAnalyticsSection(
    statisticsState: BODDashboardViewModel.BODStatisticsState,
    currencyFormatter: NumberFormat,
    onAnalyticsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📊 Executive Analytics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Real-time business intelligence dashboard",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onAnalyticsClick) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Analytics,
                        contentDescription = "View Detailed Analytics"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Cash Flow Analytics - Bar Chart
            CashFlowAnalytics(
                statisticsState = statisticsState,
                currencyFormatter = currencyFormatter
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Unit Velocity Analytics - Progress Bars
            UnitVelocityAnalytics(
                statisticsState = statisticsState
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Performance Metrics Grid
            PerformanceMetricsGrid(
                statisticsState = statisticsState,
                currencyFormatter = currencyFormatter
            )
        }
    }
}

/**
 * Cash Flow Analytics with Bar Chart Visualization
 * Shows Realized vs Projected Cash Flow data
 */
@Composable
private fun CashFlowAnalytics(
    statisticsState: BODDashboardViewModel.BODStatisticsState,
    currencyFormatter: NumberFormat
) {
    Column {
        Text(
            text = "💰 Cash Flow Analysis",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Cash Flow Data (Dummy Data for Testing)
        val cashFlowData = listOf(
            CashFlowData("Q1 2024", BigDecimal("2500000000"), BigDecimal("2800000000")),
            CashFlowData("Q2 2024", BigDecimal("3200000000"), BigDecimal("3000000000")),
            CashFlowData("Q3 2024", BigDecimal("2900000000"), BigDecimal("3100000000")),
            CashFlowData("Q4 2024", BigDecimal("3500000000"), BigDecimal("3300000000"))
        )
        
        // Bar Chart Visualization
        CashFlowBarChart(
            data = cashFlowData,
            currencyFormatter = currencyFormatter
        )
        
        // Summary Statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CashFlowSummaryItem(
                title = "Total Realized",
                value = cashFlowData.sumOf { it.realized },
                currencyFormatter = currencyFormatter,
                color = MaterialTheme.colorScheme.primary
            )
            
            CashFlowSummaryItem(
                title = "Total Projected",
                value = cashFlowData.sumOf { it.projected },
                currencyFormatter = currencyFormatter,
                color = MaterialTheme.colorScheme.secondary
            )
            
            CashFlowSummaryItem(
                title = "Variance",
                value = cashFlowData.sumOf { it.realized } - cashFlowData.sumOf { it.projected },
                currencyFormatter = currencyFormatter,
                color = if (cashFlowData.sumOf { it.realized } >= cashFlowData.sumOf { it.projected }) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Unit Velocity Analytics with Progress Bars
 * Shows unit processing velocity and efficiency metrics
 */
@Composable
private fun UnitVelocityAnalytics(
    statisticsState: BODDashboardViewModel.BODStatisticsState
) {
    Column {
        Text(
            text = "🏃 Unit Velocity Metrics",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Unit Velocity Data (Dummy Data for Testing)
        val unitVelocityData = listOf(
            UnitVelocityData("Application Processing", 0.85f, "85%"),
            UnitVelocityData("Document Verification", 0.72f, "72%"),
            UnitVelocityData("Legal Review", 0.68f, "68%"),
            UnitVelocityData("Bank Submission", 0.90f, "90%"),
            UnitVelocityData("Approval Process", 0.78f, "78%")
        )
        
        // Progress Bars for Unit Velocity
        unitVelocityData.forEach { velocity ->
            UnitVelocityProgressBar(
                title = velocity.process,
                progress = velocity.progress,
                percentage = velocity.percentage
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Overall Velocity Score
        val overallVelocity = unitVelocityData.map { it.progress }.average().toFloat()
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Overall Velocity Score",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(overallVelocity * 100).toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                CircularProgressIndicator(
                    progress = overallVelocity,
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Performance Metrics Grid
 * Displays key performance indicators in a grid layout
 */
@Composable
private fun PerformanceMetricsGrid(
    statisticsState: BODDashboardViewModel.BODStatisticsState,
    currencyFormatter: NumberFormat
) {
    Column {
        Text(
            text = "📈 Performance Metrics",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Performance Metrics Data (Dummy Data for Testing)
        val performanceMetrics = listOf(
            PerformanceMetric("Portfolio Value", statisticsState.totalPortfolioValue, "+18%", true),
            PerformanceMetric("Active Applications", statisticsState.activeApplications.toBigDecimal(), "+25", true),
            PerformanceMetric("Success Rate", statisticsState.successRate.toBigDecimal(), "+3.2%", true),
            PerformanceMetric("Market Share", BigDecimal("23.5"), "+2.1%", true),
            PerformanceMetric("Customer Satisfaction", BigDecimal("4.8"), "+0.3", true),
            PerformanceMetric("Operational Efficiency", BigDecimal("87.3"), "+5.2%", true)
        )
        
        // Grid Layout for Metrics
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(performanceMetrics.chunked(2)) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { metric ->
                        PerformanceMetricCard(
                            metric = metric,
                            currencyFormatter = currencyFormatter,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple Bar Chart for Cash Flow Visualization
 */
@Composable
private fun CashFlowBarChart(
    data: List<CashFlowData>,
    currencyFormatter: NumberFormat
) {
    val maxValue = data.maxOf { maxOf(it.realized, it.projected) }
    
    Column {
        data.forEach { cashFlow ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Period Label
                Text(
                    text = cashFlow.period,
                    fontSize = 12.sp,
                    modifier = Modifier.width(80.dp)
                )
                
                // Realized Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                ) {
                    val realizedWidth = (cashFlow.realized.toFloat() / maxValue.toFloat()) * 100
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(realizedWidth / 100f)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // Projected Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                ) {
                    val projectedWidth = (cashFlow.projected.toFloat() / maxValue.toFloat()) * 100
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(projectedWidth / 100f)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                }
            }
        }
        
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Realized", fontSize = 12.sp)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Projected", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Progress Bar for Unit Velocity
 */
@Composable
private fun UnitVelocityProgressBar(
    title: String,
    progress: Float,
    percentage: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = percentage,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            color = when {
                progress >= 0.8f -> MaterialTheme.colorScheme.primary
                progress >= 0.6f -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            }
        )
    }
}

/**
 * Summary Item for Cash Flow
 */
@Composable
private fun CashFlowSummaryItem(
    title: String,
    value: BigDecimal,
    currencyFormatter: NumberFormat,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = currencyFormatter.format(value),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Performance Metric Card
 */
@Composable
private fun PerformanceMetricCard(
    metric: PerformanceMetric,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = metric.title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val displayValue = when (metric.title) {
                "Portfolio Value" -> currencyFormatter.format(metric.value)
                "Customer Satisfaction" -> "${metric.value}/5.0"
                else -> metric.value.toString()
            }
            
            Text(
                text = displayValue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = metric.change,
                fontSize = 12.sp,
                color = if (metric.isPositive) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

// Data Classes for Analytics
data class CashFlowData(
    val period: String,
    val realized: BigDecimal,
    val projected: BigDecimal
)

data class UnitVelocityData(
    val process: String,
    val progress: Float,
    val percentage: String
)

data class PerformanceMetric(
    val title: String,
    val value: BigDecimal,
    val change: String,
    val isPositive: Boolean
)
