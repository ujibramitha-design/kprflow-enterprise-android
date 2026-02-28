package com.kprflow.enterprise.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun OfflineSyncIndicator(
    syncStatus: SyncStatus,
    pendingCount: Int,
    modifier: Modifier = Modifier,
    onSyncClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (syncStatus) {
                is SyncStatus.Syncing -> MaterialTheme.colorScheme.primaryContainer
                is SyncStatus.Offline -> MaterialTheme.colorScheme.errorContainer
                is SyncStatus.Online -> MaterialTheme.colorScheme.surface
                is SyncStatus.Error -> MaterialTheme.colorScheme.errorContainer
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon and Text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SyncStatusIcon(
                    status = syncStatus,
                    modifier = Modifier.size(24.dp)
                )
                
                Column {
                    Text(
                        text = getStatusText(syncStatus),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = when (syncStatus) {
                            is SyncStatus.Syncing -> MaterialTheme.colorScheme.onPrimaryContainer
                            is SyncStatus.Offline -> MaterialTheme.colorScheme.onErrorContainer
                            is SyncStatus.Online -> MaterialTheme.colorScheme.onSurface
                            is SyncStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    
                    if (pendingCount > 0) {
                        Text(
                            text = "$pendingCount items pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (syncStatus) {
                                is SyncStatus.Syncing -> MaterialTheme.colorScheme.onPrimaryContainer
                                is SyncStatus.Offline -> MaterialTheme.colorScheme.onErrorContainer
                                is SyncStatus.Online -> MaterialTheme.colorScheme.onSurfaceVariant
                                is SyncStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                }
            }
            
            // Action Button
            when (syncStatus) {
                is SyncStatus.Syncing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = when (syncStatus) {
                            is SyncStatus.Syncing -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
                
                is SyncStatus.Offline -> {
                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sync when online",
                            tint = Color.White
                        )
                    }
                }
                
                is SyncStatus.Online -> {
                    if (pendingCount > 0) {
                        IconButton(
                            onClick = onSyncClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Sync now",
                                tint = Color.White
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "All synced",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                is SyncStatus.Error -> {
                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Retry sync",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncStatusIcon(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (status) {
        is SyncStatus.Syncing -> Icons.Default.Sync to MaterialTheme.colorScheme.primary
        is SyncStatus.Offline -> Icons.Default.CloudOff to MaterialTheme.colorScheme.error
        is SyncStatus.Online -> Icons.Default.CloudDone to MaterialTheme.colorScheme.primary
        is SyncStatus.Error -> Icons.Default.Error to MaterialTheme.colorScheme.error
    }
    
    if (status is SyncStatus.Syncing) {
        val infiniteTransition = rememberInfiniteTransition()
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        
        Icon(
            icon,
            contentDescription = null,
            modifier = modifier.rotate(rotation),
            tint = color
        )
    } else {
        Icon(
            icon,
            contentDescription = null,
            modifier = modifier,
            tint = color
        )
    }
}

@Composable
fun SyncProgressBar(
    progress: SyncProgress,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Text(
                    text = "Syncing ${progress.currentEntity}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${progress.processedEntities}/${progress.totalEntities}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            LinearProgressIndicator(
                progress = progress.progress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            if (progress.failedEntities > 0) {
                Text(
                    text = "${progress.failedEntities} failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun OfflineModeBanner(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text(
                    text = "Offline Mode - Limited functionality",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private fun getStatusText(status: SyncStatus): String {
    return when (status) {
        is SyncStatus.Syncing -> "Syncing..."
        is SyncStatus.Offline -> "Offline Mode"
        is SyncStatus.Online -> "All Synced"
        is SyncStatus.Error -> "Sync Failed"
    }
}

sealed class SyncStatus {
    object Syncing : SyncStatus()
    object Offline : SyncStatus()
    object Online : SyncStatus()
    object Error : SyncStatus()
}
