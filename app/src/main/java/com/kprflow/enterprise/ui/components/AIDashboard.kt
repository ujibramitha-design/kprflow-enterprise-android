package com.kprflow.enterprise.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.domain.model.ChurnRiskLevel
import com.kprflow.enterprise.domain.model.InventoryRecommendation
import com.kprflow.enterprise.domain.model.RecommendationPriority
import com.kprflow.enterprise.ui.viewmodel.AIDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIDashboard(
    viewModel: AIDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadAIData()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🤖 AI Analytics Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                IconButton(onClick = { viewModel.refreshData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = { viewModel.trainModels() }) {
                    Icon(Icons.Default.ModelTraining, contentDescription = "Train Models")
                }
            }
        }
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Analyzing data with AI...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            uiState.error != null -> {
                AIErrorCard(
                    error = uiState.error,
                    onRetry = { viewModel.refreshData() }
                )
            }
            
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // AI Performance Metrics
                    item {
                        AIPerformanceCard(
                            modelPerformance = uiState.modelPerformance,
                            totalPredictions = uiState.totalPredictions,
                            accuracy = uiState.overallAccuracy
                        )
                    }
                    
                    // Churn Risk Alert
                    if (uiState.highRiskCustomers.isNotEmpty()) {
                        item {
                            ChurnRiskAlertCard(
                                highRiskCustomers = uiState.highRiskCustomers.take(5),
                                onViewAll = { viewModel.viewAllChurnRisks() }
                            )
                        }
                    }
                    
                    // Top Inventory Recommendations
                    items(uiState.topRecommendations.take(5)) { recommendation ->
                        InventoryRecommendationCard(
                            recommendation = recommendation,
                            onAction = { viewModel.applyRecommendation(recommendation) }
                        )
                    }
                    
                    // AI Insights
                    items(uiState.aiInsights.take(3)) { insight ->
                        AIInsightCard(
                            insight = insight,
                            onAction = { viewModel.applyInsight(insight) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AIPerformanceCard(
    modelPerformance: Map<String, Double>,
    totalPredictions: Long,
    accuracy: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 AI Performance Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Accuracy",
                    value = "${(accuracy * 100).format(1)}%",
                    icon = Icons.Default.TrendingUp,
                    color = if (accuracy > 0.8) Color.Green else Color.Orange
                )
                
                MetricItem(
                    label = "Predictions",
                    value = totalPredictions.toString(),
                    icon = Icons.Default.Analytics
                )
                
                MetricItem(
                    label = "Models",
                    value = modelPerformance.size.toString(),
                    icon = Icons.Default.ModelTraining
                )
            }
            
            // Model performance bars
            modelPerformance.entries.take(3).forEach { (modelName, performance) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = modelName,
                        modifier = Modifier.width(120.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    LinearProgressIndicator(
                        progress = performance.toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (performance > 0.8) Color.Green else Color.Orange
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "${(performance * 100).format(1)}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChurnRiskAlertCard(
    highRiskCustomers: List<ChurnRiskAlert>,
    onViewAll: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    
                    Text(
                        text = "⚠️ High Churn Risk Alert",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                TextButton(onClick = onViewAll) {
                    Text("View All")
                }
            }
            
            highRiskCustomers.forEach { customer ->
                ChurnRiskItem(customer = customer)
            }
        }
    }
}

@Composable
fun ChurnRiskItem(customer: ChurnRiskAlert) {
    val (color, icon) = when (customer.riskLevel) {
        ChurnRiskLevel.CRITICAL -> Color.Red to Icons.Default.Dangerous
        ChurnRiskLevel.HIGH -> Color.Orange to Icons.Default.Warning
        else -> Color.Yellow to Icons.Default.Info
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            
            Column {
                Text(
                    text = customer.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${(customer.churnProbability * 100).format(1)}% risk",
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
        
        Text(
            text = customer.unitName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InventoryRecommendationCard(
    recommendation: InventoryRecommendation,
    onAction: () -> Unit
) {
    val (color, priority) = when (recommendation.priority) {
        RecommendationPriority.CRITICAL -> Color.Red to "Critical"
        RecommendationPriority.HIGH -> Color.Orange to "High"
        RecommendationPriority.MEDIUM -> Color.Blue to "Medium"
        RecommendationPriority.LOW -> Color.Green to "Low"
        RecommendationPriority.VERY_LOW -> Color.Gray to "Very Low"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.unitName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Score: ${(recommendation.score * 100).format(1)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
                
                Text(
                    text = priority,
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (recommendation.reasons.isNotEmpty()) {
                Text(
                    text = recommendation.reasons.take(2).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rp ${recommendation.suggestedPrice.formatCurrency()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
fun AIInsightCard(
    insight: AIInsight,
    onAction: () -> Unit
) {
    val (color, icon) = when (insight.impact) {
        com.kprflow.enterprise.domain.model.ImpactLevel.CRITICAL -> Color.Red to Icons.Default.Dangerous
        com.kprflow.enterprise.domain.model.ImpactLevel.HIGH -> Color.Orange to Icons.Default.Warning
        com.kprflow.enterprise.domain.model.ImpactLevel.MEDIUM -> Color.Blue to Icons.Default.Info
        com.kprflow.enterprise.domain.model.ImpactLevel.LOW -> Color.Green to Icons.Default.Lightbulb
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "${(insight.confidence * 100).format(0)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
            
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (insight.actionable) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Take Action")
                }
            }
        }
    }
}

@Composable
fun AIErrorCard(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "❌ AI Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Double.format(digits: Int): String {
    return String.format("%.${digits}f", this)
}

private fun Double.formatCurrency(): String {
    return String.format("%,.0f", this)
}

data class ChurnRiskAlert(
    val dossierId: String,
    val customerName: String,
    val unitName: String,
    val churnProbability: Double,
    val riskLevel: ChurnRiskLevel
)
