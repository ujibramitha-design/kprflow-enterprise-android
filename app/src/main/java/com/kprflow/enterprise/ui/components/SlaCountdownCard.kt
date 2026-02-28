package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kprflow.enterprise.ui.theme.*
import androidx.compose.ui.text.font.FontFamily

// =====================================================
// BENTO STYLE SLA COUNTDOWN CARD
// Phase 15 Standard - Reusable Glassmorphism Component
// =====================================================

@Composable
fun SlaCountdownCard(
    title: String,
    daysRemaining: Int,
    totalDays: Int,
    status: SlaCardStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showProgress: Boolean = true,
    compact: Boolean = false
) {
    val colorScheme = getColorScheme(status)
    
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                onClick?.let { clickable(onClick = it) } ?: Modifier
            ),
        backgroundColor = colorScheme.backgroundColor,
        borderColor = colorScheme.borderColor,
        onClick = onClick
    ) {
        if (compact) {
            CompactSlaContent(
                title = title,
                daysRemaining = daysRemaining,
                status = status,
                colorScheme = colorScheme
            )
        } else {
            FullSlaContent(
                title = title,
                daysRemaining = daysRemaining,
                totalDays = totalDays,
                status = status,
                colorScheme = colorScheme,
                showProgress = showProgress
            )
        }
    }
}

@Composable
private fun FullSlaContent(
    title: String,
    daysRemaining: Int,
    totalDays: Int,
    status: SlaCardStatus,
    colorScheme: SlaColorScheme,
    showProgress: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Header with icon and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = colorScheme.icon,
                    contentDescription = null,
                    tint = colorScheme.iconColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = status.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.textColor
                    )
                }
            }
            
            // Status Badge
            Surface(
                color = colorScheme.badgeColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when {
                        daysRemaining <= 0 -> "OVERDUE"
                        daysRemaining <= 3 -> "CRITICAL"
                        daysRemaining <= 7 -> "WARNING"
                        else -> "ON TRACK"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.badgeColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Countdown Display with Plus Jakarta Sans style
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Days Remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Main countdown number with Plus Jakarta Sans styling
                Text(
                    text = if (daysRemaining < 0) "0" else daysRemaining.toString(),
                    style = SLATypography.CountdownLarge,
                    color = colorScheme.numberColor,
                    textAlign = TextAlign.Start
                )
            }
            
            // Percentage and additional info
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val percentage = ((totalDays - daysRemaining).toFloat() / totalDays * 100).toInt()
                Text(
                    text = "${percentage}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.textColor
                )
                
                Text(
                    text = "completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Progress Indicator
        if (showProgress) {
            Spacer(modifier = Modifier.height(16.dp))
            
            SlaProgressBar(
                daysRemaining = daysRemaining,
                totalDays = totalDays,
                colorScheme = colorScheme
            )
        }
        
        // Additional info row
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total: $totalDays days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = when {
                    daysRemaining <= 0 -> "${-daysRemaining} days overdue"
                    daysRemaining == 1 -> "1 day left"
                    else -> "$daysRemaining days left"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = colorScheme.textColor
            )
        }
    }
}

@Composable
private fun CompactSlaContent(
    title: String,
    daysRemaining: Int,
    status: SlaCardStatus,
    colorScheme: SlaColorScheme
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
                imageVector = colorScheme.icon,
                contentDescription = null,
                tint = colorScheme.iconColor,
                modifier = Modifier.size(20.dp)
            )
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = status.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.textColor
                )
            }
        }
        
        // Compact countdown
        Text(
            text = if (daysRemaining < 0) "0" else daysRemaining.toString(),
            style = SLATypography.CountdownMedium,
            color = colorScheme.numberColor
        )
    }
}

@Composable
private fun SlaProgressBar(
    daysRemaining: Int,
    totalDays: Int,
    colorScheme: SlaColorScheme
) {
    val progress = if (daysRemaining < 0) 1f else (totalDays - daysRemaining).toFloat() / totalDays
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = colorScheme.textColor
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = colorScheme.progressColor,
            trackColor = colorScheme.trackColor
        )
    }
}

// =====================================================
// SLA CARD STATUS ENUM AND COLOR SCHEMES
// =====================================================

enum class SlaCardStatus(val displayName: String) {
    NORMAL("On Track"),
    WARNING("Warning"),
    CRITICAL("Critical"),
    OVERDUE("Overdue")
}

data class SlaColorScheme(
    val backgroundColor: Color,
    val borderColor: Color,
    val icon: ImageVector,
    val iconColor: Color,
    val textColor: Color,
    val numberColor: Color,
    val badgeColor: Color,
    val progressColor: Color,
    val trackColor: Color
) {
    companion object {
        fun getColorScheme(status: SlaCardStatus): SlaColorScheme {
            return when (status) {
                SlaCardStatus.NORMAL -> SlaColorScheme(
                    backgroundColor = GlassLight,
                    borderColor = GlassBorder,
                    icon = Icons.Default.CheckCircle,
                    iconColor = Success,
                    textColor = Success,
                    numberColor = Success,
                    badgeColor = Success,
                    progressColor = Success,
                    trackColor = GlassSurface
                )
                SlaCardStatus.WARNING -> SlaColorScheme(
                    backgroundColor = Color(0xFFFFF8E1).copy(alpha = 0.3f),
                    borderColor = Color(0xFFFFBF00).copy(alpha = 0.5f),
                    icon = Icons.Default.Warning,
                    iconColor = Warning,
                    textColor = Warning,
                    numberColor = Warning,
                    badgeColor = Warning,
                    progressColor = Warning,
                    trackColor = GlassSurface
                )
                SlaCardStatus.CRITICAL -> SlaColorScheme(
                    backgroundColor = Color(0xFFFFE082).copy(alpha = 0.3f),
                    borderColor = Color(0xFFFF6F00).copy(alpha = 0.5f),
                    icon = Icons.Default.Warning,
                    iconColor = Color(0xFFFF6F00),
                    textColor = Color(0xFFFF6F00),
                    numberColor = Color(0xFFFF6F00),
                    badgeColor = Color(0xFFFF6F00),
                    progressColor = Color(0xFFFF6F00),
                    trackColor = GlassSurface
                )
                SlaCardStatus.OVERDUE -> SlaColorScheme(
                    backgroundColor = Color(0xFFFFEBEE).copy(alpha = 0.3f),
                    borderColor = Color(0xFFD32F2F).copy(alpha = 0.5f),
                    icon = Icons.Default.Error,
                    iconColor = Error,
                    textColor = Error,
                    numberColor = Error,
                    badgeColor = Error,
                    progressColor = Error,
                    trackColor = GlassSurface
                )
            }
        }
    }
}

// =====================================================
// PRESET SLA CARDS FOR COMMON USE CASES
// =====================================================

@Composable
fun DocumentSlaCard(
    daysRemaining: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    compact: Boolean = false
) {
    val status = when {
        daysRemaining <= 0 -> SlaCardStatus.OVERDUE
        daysRemaining <= 3 -> SlaCardStatus.CRITICAL
        daysRemaining <= 7 -> SlaCardStatus.WARNING
        else -> SlaCardStatus.NORMAL
    }
    
    SlaCountdownCard(
        title = "Document Collection",
        daysRemaining = daysRemaining,
        totalDays = 14,
        status = status,
        modifier = modifier,
        onClick = onClick,
        compact = compact,
        showProgress = !compact
    )
}

@Composable
fun BankSlaCard(
    daysRemaining: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    compact: Boolean = false
) {
    val status = when {
        daysRemaining <= 0 -> SlaCardStatus.OVERDUE
        daysRemaining <= 3 -> SlaCardStatus.CRITICAL
        daysRemaining <= 7 -> SlaCardStatus.WARNING
        else -> SlaCardStatus.NORMAL
    }
    
    SlaCountdownCard(
        title = "Bank Processing",
        daysRemaining = daysRemaining,
        totalDays = 60,
        status = status,
        modifier = modifier,
        onClick = onClick,
        compact = compact,
        showProgress = !compact
    )
}

@Composable
fun CustomSlaCard(
    title: String,
    daysRemaining: Int,
    totalDays: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    compact: Boolean = false
) {
    val status = when {
        daysRemaining <= 0 -> SlaCardStatus.OVERDUE
        daysRemaining <= 3 -> SlaCardStatus.CRITICAL
        daysRemaining <= 7 -> SlaCardStatus.WARNING
        else -> SlaCardStatus.NORMAL
    }
    
    SlaCountdownCard(
        title = title,
        daysRemaining = daysRemaining,
        totalDays = totalDays,
        status = status,
        modifier = modifier,
        onClick = onClick,
        compact = compact,
        showProgress = !compact
    )
}

// =====================================================
// SLA CARD GRID FOR DASHBOARDS
// =====================================================

@Composable
fun SlaCardGrid(
    documentDays: Int,
    bankDays: Int,
    modifier: Modifier = Modifier,
    onDocumentClick: (() -> Unit)? = null,
    onBankClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DocumentSlaCard(
            daysRemaining = documentDays,
            onClick = onDocumentClick,
            compact = true
        )
        
        BankSlaCard(
            daysRemaining = bankDays,
            onClick = onBankClick,
            compact = true
        )
    }
}
