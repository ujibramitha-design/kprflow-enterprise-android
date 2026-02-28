package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kprflow.enterprise.domain.usecase.sla.SLAColorConfig
import com.kprflow.enterprise.domain.usecase.sla.SLAWarningLevel
import com.kprflow.enterprise.domain.usecase.sla.SlaStatus

// =====================================================
// SLA WARNING SYSTEM COMPONENTS
// =====================================================

@Composable
fun SLAWarningIndicator(
    slaStatus: SlaStatus,
    colorConfig: SLAColorConfig,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Warning Icon
        Icon(
            imageVector = when {
                slaStatus.isOverdue -> Icons.Default.Error
                slaStatus.isCritical -> Icons.Default.Warning
                else -> Icons.Default.CheckCircle
            },
            contentDescription = null,
            tint = colorConfig.primary,
            modifier = Modifier.size(20.dp)
        )
        
        // Days Remaining
        Text(
            text = "${slaStatus.daysRemaining} days",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colorConfig.primary
        )
        
        // Optional Label
        if (showLabel) {
            Text(
                text = when {
                    slaStatus.isOverdue -> "Overdue"
                    slaStatus.isCritical -> "Critical"
                    slaStatus.isWarning -> "Warning"
                    else -> "Normal"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colorConfig.text
            )
        }
    }
}

@Composable
fun SLAWarningCard(
    slaStatus: SlaStatus,
    colorConfig: SLAColorConfig,
    customerName: String,
    dossierStatus: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorConfig.background, RoundedCornerShape(12.dp))
            .then(
                onClick?.let { clickable(onClick = it) } ?: Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = colorConfig.background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with customer name and status
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
                    
                    Text(
                        text = dossierStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Badge
                Surface(
                    color = colorConfig.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when {
                            slaStatus.isOverdue -> "OVERDUE"
                            slaStatus.isCritical -> "CRITICAL"
                            slaStatus.isWarning -> "WARNING"
                            else -> "NORMAL"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorConfig.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // SLA Progress
            SLAProgressBar(
                daysLeft = slaStatus.daysRemaining,
                totalDays = 60,
                label = "Bank Processing SLA",
                isOverdue = slaStatus.isOverdue,
                colorConfig = colorConfig
            )
            
            if (slaStatus.docDaysRemaining > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                SLAProgressBar(
                    daysLeft = slaStatus.docDaysRemaining,
                    totalDays = 14,
                    label = "Document Collection SLA",
                    isOverdue = slaStatus.isOverdue,
                    colorConfig = colorConfig
                )
            }
        }
    }
}

@Composable
fun SLAProgressBar(
    daysLeft: Int,
    totalDays: Int,
    label: String,
    isOverdue: Boolean,
    colorConfig: SLAColorConfig,
    modifier: Modifier = Modifier
) {
    val progress = if (isOverdue) 1f else (totalDays - daysLeft).toFloat() / totalDays
    
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
                color = colorConfig.primary
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            color = colorConfig.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun SLAWarningBanner(
    warningLevel: SLAWarningLevel,
    message: String,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorConfig = when (warningLevel) {
        SLAWarningLevel.NORMAL -> SLAColorConfig.DEFAULT
        SLAWarningLevel.WARNING -> SLAColorConfig(
            primary = Color(0xFFFFBF00),
            background = Color(0xFFFFF8E1),
            text = Color(0xFFFF6F00)
        )
        SLAWarningLevel.CRITICAL -> SLAColorConfig(
            primary = Color(0xFFFF6F00),
            background = Color(0xFFFFE082),
            text = Color(0xFFE65100)
        )
        SLAWarningLevel.OVERDUE -> SLAColorConfig(
            primary = Color(0xFFD32F2F),
            background = Color(0xFFFFEBEE),
            text = Color(0xFFB71C1C)
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colorConfig.background, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = colorConfig.background
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (warningLevel) {
                    SLAWarningLevel.NORMAL -> Icons.Default.CheckCircle
                    SLAWarningLevel.WARNING -> Icons.Default.Warning
                    SLAWarningLevel.CRITICAL -> Icons.Default.Warning
                    SLAWarningLevel.OVERDUE -> Icons.Default.Error
                },
                contentDescription = null,
                tint = colorConfig.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = colorConfig.text
            )
            
            onDismiss?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = colorConfig.text
                    )
                }
            }
        }
    }
}

@Composable
fun SLAStatusChip(
    slaStatus: SlaStatus,
    modifier: Modifier = Modifier
) {
    val colorConfig = when {
        slaStatus.isOverdue -> SLAColorConfig(
            primary = Color(0xFFD32F2F),
            background = Color(0xFFFFEBEE),
            text = Color(0xFFB71C1C)
        )
        slaStatus.isCritical -> SLAColorConfig(
            primary = Color(0xFFFF6F00),
            background = Color(0xFFFFE082),
            text = Color(0xFFE65100)
        )
        slaStatus.isWarning -> SLAColorConfig(
            primary = Color(0xFFFFBF00),
            background = Color(0xFFFFF8E1),
            text = Color(0xFFFF6F00)
        )
        else -> SLAColorConfig(
            primary = Color(0xFF004B87),
            background = Color(0xFFE3F2FD),
            text = Color(0xFF0D47A1)
        )
    }
    
    Surface(
        modifier = modifier,
        color = colorConfig.background,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        colorConfig.primary,
                        RoundedCornerShape(4.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Text(
                text = "${slaStatus.daysRemaining} days",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colorConfig.primary
            )
        }
    }
}

@Composable
fun SLADashboardSummary(
    normalCount: Int,
    warningCount: Int,
    criticalCount: Int,
    overdueCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SLASummaryItem(
            label = "Normal",
            count = normalCount,
            color = Color(0xFF004B87),
            modifier = Modifier.weight(1f)
        )
        
        SLASummaryItem(
            label = "Warning",
            count = warningCount,
            color = Color(0xFFFFBF00),
            modifier = Modifier.weight(1f)
        )
        
        SLASummaryItem(
            label = "Critical",
            count = criticalCount,
            color = Color(0xFFFF6F00),
            modifier = Modifier.weight(1f)
        )
        
        SLASummaryItem(
            label = "Overdue",
            count = overdueCount,
            color = Color(0xFFD32F2F),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SLASummaryItem(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
