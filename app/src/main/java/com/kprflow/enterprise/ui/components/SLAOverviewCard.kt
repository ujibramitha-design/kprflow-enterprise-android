package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kprflow.enterprise.data.repository.SLAComplianceOverview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SLAOverviewCard(
    title: String,
    overview: SLAComplianceOverview,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "❌",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                else -> {
                    // Overall Compliance Rate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overall Compliance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "${String.format("%.1f", overview.overallComplianceRate)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = getComplianceColor(overview.overallComplianceRate)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // SLA Categories
                    SLACategoryRow(
                        title = "Document Processing",
                        compliant = overview.documentProcessingSLA.compliantItems,
                        overdue = overview.documentProcessingSLA.overdueItems,
                        total = overview.documentProcessingSLA.totalItems,
                        avgDays = overview.documentProcessingSLA.averageProcessingDays
                    )
                    
                    SLACategoryRow(
                        title = "Bank Decisions",
                        compliant = overview.bankDecisionSLA.compliantItems,
                        overdue = overview.bankDecisionSLA.overdueItems,
                        total = overview.bankDecisionSLA.totalItems,
                        avgDays = overview.bankDecisionSLA.averageProcessingDays
                    )
                    
                    SLACategoryRow(
                        title = "SP3K Issuance",
                        compliant = overview.sp3kIssuanceSLA.compliantItems,
                        overdue = overview.sp3kIssuanceSLA.overdueItems,
                        total = overview.sp3kIssuanceSLA.totalItems,
                        avgDays = overview.sp3kIssuanceSLA.averageProcessingDays
                    )
                    
                    SLACategoryRow(
                        title = "Disbursement",
                        compliant = overview.disbursementSLA.compliantItems,
                        overdue = overview.disbursementSLA.overdueItems,
                        total = overview.disbursementSLA.totalItems,
                        avgDays = overview.disbursementSLA.averageProcessingDays
                    )
                    
                    SLACategoryRow(
                        title = "BAST Completion",
                        compliant = overview.bastCompletionSLA.compliantItems,
                        overdue = overview.bastCompletionSLA.overdueItems,
                        total = overview.bastCompletionSLA.totalItems,
                        avgDays = overview.bastCompletionSLA.averageProcessingDays
                    )
                }
            }
        }
    }
}

@Composable
private fun SLACategoryRow(
    title: String,
    compliant: Int,
    overdue: Int,
    total: Int,
    avgDays: Double
) {
    val complianceRate = if (total > 0) (compliant.toDouble() / total) * 100 else 0.0
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "${String.format("%.1f", complianceRate)}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = getComplianceColor(complianceRate)
            )
        }
        
        // Progress bar
        LinearProgressIndicator(
            progress = { complianceRate / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = getComplianceColor(complianceRate)
        )
        
        // Details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✅ $compliant | ⚠️ $overdue | 📊 $total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Avg: ${String.format("%.1f", avgDays)}d",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun OverdueItemsCard(
    title: String,
    overdueItems: List<com.kprflow.enterprise.data.repository.OverdueItem>,
    onSendAlert: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = overdueItems.size.toString(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "❌",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                overdueItems.isEmpty() -> {
                    Text(
                        text = "No overdue items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                else -> {
                    overdueItems.take(5).forEach { item ->
                        OverdueItemRow(
                            item = item,
                            onSendAlert = onSendAlert
                        )
                        
                        if (overdueItems.indexOf(item) < minOf(4, overdueItems.size - 1)) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                    
                    if (overdueItems.size > 5) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "... and ${overdueItems.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverdueItemRow(
    item: com.kprflow.enterprise.data.repository.OverdueItem,
    onSendAlert: (String, String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Team: ${item.responsibleTeam} | Type: ${item.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Badge(
                    containerColor = when (item.priority) {
                        "HIGH" -> MaterialTheme.colorScheme.error
                        "MEDIUM" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = "${item.overdueDays}d",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                
                Text(
                    text = "SLA: ${item.slaDays}d",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = { onSendAlert(item.id, item.type) },
                modifier = Modifier.widthIn(min = 100.dp)
            ) {
                Text("Send Alert")
            }
        }
    }
}

@Composable
fun SLAChart(
    overview: SLAComplianceOverview,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "SLA Compliance Chart",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Overall compliance indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Overall Compliance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${String.format("%.1f", overview.overallComplianceRate)}%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = getComplianceColor(overview.overallComplianceRate)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { overview.overallComplianceRate / 100f },
                    modifier = Modifier
                        .width(200.dp)
                        .height(8.dp),
                    color = getComplianceColor(overview.overallComplianceRate)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Summary stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total Items",
                    value = overview.totalItems.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    label = "Compliant",
                    value = overview.totalCompliantItems.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    label = "Overdue",
                    value = overview.totalOverdueItems.toString(),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
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

private fun getComplianceColor(rate: Double): androidx.compose.ui.graphics.Color {
    return when {
        rate >= 90 -> MaterialTheme.colorScheme.primary
        rate >= 75 -> MaterialTheme.colorScheme.secondary
        rate >= 50 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
}
