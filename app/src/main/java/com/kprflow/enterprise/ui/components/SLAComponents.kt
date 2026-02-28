package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kprflow.enterprise.ui.theme.*

// =====================================================
// SLA STATUS COMPONENTS
// =====================================================

@Composable
fun SLAStatusCard(
    customerName: String,
    status: String,
    docDaysLeft: Int,
    bankDaysLeft: Int,
    isDocOverdue: Boolean,
    isBankOverdue: Boolean,
    slaStatus: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val statusColor = when (slaStatus) {
        "CRITICAL" -> Error
        "WARNING" -> Warning
        "DOC_OVERDUE" -> Error
        "BANK_OVERDUE" -> Error
        "NORMAL" -> Success
        else -> MaterialTheme.colorScheme.primary
    }
    
    val backgroundColor = when (slaStatus) {
        "CRITICAL" -> Error.copy(alpha = 0.1f)
        "WARNING" -> Warning.copy(alpha = 0.1f)
        "DOC_OVERDUE" -> Error.copy(alpha = 0.1f)
        "BANK_OVERDUE" -> Error.copy(alpha = 0.1f)
        "NORMAL" -> Success.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    BentoBox(
        modifier = modifier,
        backgroundColor = backgroundColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Document SLA
                    Column {
                        Text(
                            text = "Documents",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isDocOverdue) "Overdue" else "$docDaysLeft days",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDocOverdue) Error else statusColor
                        )
                    }
                    
                    // Bank SLA
                    Column {
                        Text(
                            text = "Bank",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isBankOverdue) "Overdue" else "$bankDaysLeft days",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isBankOverdue) Error else statusColor
                        )
                    }
                }
            }
            
            // Status Badge
            Surface(
                color = statusColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = slaStatus.replace("_", " "),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun SLASummaryCard(
    title: String,
    totalDossiers: Int,
    overdueCount: Int,
    criticalCount: Int,
    avgDaysRemaining: Double,
    modifier: Modifier = Modifier
) {
    BentoBox(modifier = modifier) {
        BentoHeader(
            title = title,
            subtitle = "SLA Overview"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SLAMetric(
                value = totalDossiers.toString(),
                label = "Total",
                color = MaterialTheme.colorScheme.primary
            )
            
            SLAMetric(
                value = overdueCount.toString(),
                label = "Overdue",
                color = if (overdueCount > 0) Error else Success
            )
            
            SLAMetric(
                value = criticalCount.toString(),
                label = "Critical",
                color = if (criticalCount > 0) Error else Success
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Avg Days Remaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "${String.format("%.1f", avgDaysRemaining)} days",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SLAMetric(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SLAProgressBar(
    daysLeft: Int,
    totalDays: Int,
    label: String,
    isOverdue: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (isOverdue) 1f else (totalDays - daysLeft).toFloat() / totalDays
    val progressColor = when {
        isOverdue -> Error
        daysLeft <= 3 -> Error
        daysLeft <= 7 -> Warning
        else -> Success
    }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = if (isOverdue) "Overdue" else "$daysLeft days left",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun SLAFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        modifier = modifier
    )
}

@Composable
fun CriticalDossierAlert(
    criticalCount: Int,
    onDismiss: () -> Unit,
    onViewCritical: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (criticalCount > 0) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Critical SLA Alert",
                    color = Error
                )
            },
            text = {
                Text("You have $criticalCount critical dossiers requiring immediate attention.")
            },
            confirmButton = {
                Button(
                    onClick = onViewCritical
                ) {
                    Text("View Critical")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            },
            modifier = modifier
        )
    }
}
