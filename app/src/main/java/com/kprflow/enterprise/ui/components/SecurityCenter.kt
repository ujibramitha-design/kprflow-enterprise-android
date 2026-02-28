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
import com.kprflow.enterprise.domain.model.SecuritySeverity
import com.kprflow.enterprise.domain.model.SecurityViolation
import com.kprflow.enterprise.ui.viewmodel.SecurityCenterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenter(
    viewModel: SecurityCenterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadSecurityData()
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
                text = "🔒 Security Center",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { viewModel.refreshData() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                SecurityErrorCard(
                    error = uiState.error,
                    onRetry = { viewModel.refreshData() }
                )
            }
            
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Security Status Card
                    item {
                        SecurityStatusCard(
                            status = uiState.securityStatus,
                            lastScan = uiState.lastSecurityScan,
                            threatsDetected = uiState.threatsDetected
                        )
                    }
                    
                    // Security Metrics
                    item {
                        SecurityMetricsCard(metrics = uiState.securityMetrics)
                    }
                    
                    // Recent Violations
                    items(uiState.recentViolations) { violation ->
                        SecurityViolationCard(
                            violation = violation,
                            onResolve = { viewModel.resolveViolation(violation.id) }
                        )
                    }
                    
                    // Security Recommendations
                    item {
                        SecurityRecommendationsCard(
                            recommendations = uiState.recommendations,
                            onImplement = { recommendationId ->
                                viewModel.implementRecommendation(recommendationId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityStatusCard(
    status: SecurityStatus,
    lastScan: Long,
    threatsDetected: Int
) {
    val (color, icon, statusText) = when (status) {
        is SecurityStatus.Secure -> 
            Color(0xFF4CAF50) to Icons.Default.Shield to "Secure"
        is SecurityStatus.Warning -> 
            Color(0xFFFF9800) to Icons.Default.Warning to "Warning"
        is SecurityStatus.AtRisk -> 
            Color(0xFFF44336) to Icons.Default.Dangerous to "At Risk"
        is SecurityStatus.Unknown -> 
            Color(0xFF9E9E9E) to Icons.Default.Help to "Unknown"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
                
                Column {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    
                    Text(
                        text = "Last scan: ${formatTimestamp(lastScan)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = threatsDetected.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (threatsDetected > 0) Color.Red else color
                )
                
                Text(
                    text = "Threats",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SecurityMetricsCard(metrics: SecurityMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 Security Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Total Events",
                    value = metrics.totalEvents.toString(),
                    icon = Icons.Default.Event
                )
                
                MetricItem(
                    label = "Critical Events",
                    value = metrics.criticalEvents.toString(),
                    icon = Icons.Default.Error,
                    color = if (metrics.criticalEvents > 0) Color.Red else Color.Green
                )
                
                MetricItem(
                    label = "Auth Failures",
                    value = metrics.authenticationFailures.toString(),
                    icon = Icons.Default.NoAccounts,
                    color = if (metrics.authenticationFailures > 0) Color.Orange else Color.Green
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Data Access",
                    value = metrics.dataAccessAttempts.toString(),
                    icon = Icons.Default.Visibility
                )
                
                MetricItem(
                    label = "Access Denied",
                    value = metrics.dataAccessDenials.toString(),
                    icon = Icons.Default.Block,
                    color = if (metrics.dataAccessDenials > 0) Color.Orange else Color.Green
                )
                
                MetricItem(
                    label = "Encryption Ops",
                    value = metrics.encryptionOperations.toString(),
                    icon = Icons.Default.Lock
                )
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

@Composable
fun SecurityViolationCard(
    violation: SecurityViolation,
    onResolve: () -> Unit
) {
    val (color, icon) = when (violation.severity) {
        SecuritySeverity.CRITICAL -> Color.Red to Icons.Default.Dangerous
        SecuritySeverity.ERROR -> Color.Red to Icons.Default.Error
        SecuritySeverity.WARNING -> Color.Orange to Icons.Default.Warning
        SecuritySeverity.INFO -> Color.Blue to Icons.Default.Info
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
                        text = violation.violationType.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                
                if (!violation.resolved) {
                    OutlinedButton(
                        onClick = onResolve,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Resolve")
                    }
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Resolved",
                        tint = Color.Green,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Text(
                text = violation.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = formatTimestamp(violation.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SecurityRecommendationsCard(
    recommendations: List<SecurityRecommendation>,
    onImplement: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "💡 Security Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            recommendations.take(3).forEach { recommendation ->
                RecommendationItem(
                    recommendation = recommendation,
                    onImplement = { onImplement(recommendation.id) }
                )
            }
        }
    }
}

@Composable
fun RecommendationItem(
    recommendation: SecurityRecommendation,
    onImplement: () -> Unit
) {
    val (color, priority) = when (recommendation.priority) {
        com.kprflow.enterprise.domain.model.SecurityPriority.CRITICAL -> 
            Color.Red to "Critical"
        com.kprflow.enterprise.domain.model.SecurityPriority.HIGH -> 
            Color.Orange to "High"
        com.kprflow.enterprise.domain.model.SecurityPriority.MEDIUM -> 
            Color.Blue to "Medium"
        com.kprflow.enterprise.domain.model.SecurityPriority.LOW -> 
            Color.Green to "Low"
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = priority,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
        
        if (!recommendation.implemented) {
            Button(
                onClick = onImplement,
                modifier = Modifier.height(32.dp)
            ) {
                Text("Implement")
            }
        } else {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Implemented",
                tint = Color.Green,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SecurityErrorCard(
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
                text = "❌ Security Error",
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

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} minutes ago"
        diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
        else -> "${diff / 86_400_000} days ago"
    }
}

sealed class SecurityStatus {
    object Secure : SecurityStatus()
    object Warning : SecurityStatus()
    object AtRisk : SecurityStatus()
    object Unknown : SecurityStatus()
}
