package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kprflow.enterprise.data.model.ExtensionRequest
import com.kprflow.enterprise.data.model.ExtensionStatus
import com.kprflow.enterprise.data.model.ExtensionValidation
import com.kprflow.enterprise.ui.theme.*

// =====================================================
// EXTENSION SYSTEM UI COMPONENTS
// Phase 20 Implementation - Role-based Extension Management
// =====================================================

@Composable
fun ExtensionRequestCard(
    customerName: String,
    currentDeadline: String,
    extensionDays: Int,
    extensionReason: String,
    validation: ExtensionValidation,
    onRequestExtension: () -> Unit,
    modifier: Modifier = Modifier,
    userRole: String = "MARKETING"
) {
    // Only show extension button for LEGAL role
    val canRequestExtension = validation.canExtend && userRole == "LEGAL"
    
    GlassCard(
        modifier = modifier,
        backgroundColor = if (canRequestExtension) 
            Success.copy(alpha = 0.1f) 
        else 
            GlassSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
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
                        text = "Current Deadline: $currentDeadline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Extension count badge
                Surface(
                    color = when {
                        validation.remainingExtensions == 0 -> Error
                        validation.remainingExtensions == 1 -> Warning
                        else -> Success
                    }.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${validation.remainingExtensions}/3 left",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            validation.remainingExtensions == 0 -> Error
                            validation.remainingExtensions == 1 -> Warning
                            else -> Success
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Extension details
            if (extensionReason.isNotBlank()) {
                Text(
                    text = "Extension Request:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = extensionReason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Extension Days: $extensionDays",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Validation message
            Text(
                text = validation.reason,
                style = MaterialTheme.typography.bodySmall,
                color = if (validation.canExtend) Success else Error,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action button
            Button(
                onClick = onRequestExtension,
                enabled = canRequestExtension,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canRequestExtension) Success else GlassSurface,
                    contentColor = if (canRequestExtension) Color.White else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = when {
                        userRole != "LEGAL" -> "Only Legal team can request extensions"
                        validation.remainingExtensions == 0 -> "Maximum extensions reached"
                        !validation.canExtend -> "Not eligible for extension"
                        else -> "Request +${extensionDays} Days Extension"
                    }
                )
            }
        }
    }
}

@Composable
fun PendingExtensionCard(
    extension: ExtensionRequest,
    onApprove: (String, String) -> Unit,
    onReject: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    userRole: String = "LEGAL"
) {
    val canApprove = userRole == "LEGAL" || userRole == "BOD"
    
    GlassCard(
        modifier = modifier,
        backgroundColor = Warning.copy(alpha = 0.1f),
        borderColor = Warning.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = extension.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "Extension Request",
                        style = MaterialTheme.typography.bodySmall,
                        color = Warning
                    )
                }
                
                Surface(
                    color = Warning.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = extension.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Warning
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Extension details
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow(
                    label = "Extension Days",
                    value = "+${extension.extensionDays} days"
                )
                
                DetailRow(
                    label = "Previous Deadline",
                    value = extension.previousDeadline
                )
                
                DetailRow(
                    label = "New Deadline",
                    value = extension.newDeadline,
                    valueColor = Success
                )
                
                DetailRow(
                    label = "Requested By",
                    value = extension.extendedByName
                )
                
                DetailRow(
                    label = "Request Date",
                    value = extension.extensionDate
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reason
            Text(
                text = "Reason:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = extension.extensionReason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            if (canApprove) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onReject(extension.id, "Rejected by $userRole") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Error
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = null,
                            width = 1.dp,
                            color = Error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text("Reject")
                    }
                    
                    Button(
                        onClick = { onApprove(extension.id, "Approved by $userRole") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Success
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text("Approve")
                    }
                }
            } else {
                // Show info for non-legal users
                Surface(
                    color = GlassSurface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = "Waiting for Legal team approval",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExtensionHistoryCard(
    extension: ExtensionRequest,
    modifier: Modifier = Modifier
) {
    val statusColor = when (extension.status) {
        ExtensionStatus.APPROVED -> Success
        ExtensionStatus.REJECTED -> Error
        ExtensionStatus.PENDING -> Warning
    }
    
    val statusIcon = when (extension.status) {
        ExtensionStatus.APPROVED -> Icons.Default.CheckCircle
        ExtensionStatus.REJECTED -> Icons.Default.Cancel
        ExtensionStatus.PENDING -> Icons.Default.Schedule
    }
    
    GlassCard(
        modifier = modifier,
        backgroundColor = statusColor.copy(alpha = 0.1f),
        borderColor = statusColor.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
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
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Text(
                        text = when (extension.status) {
                            ExtensionStatus.APPROVED -> "Extension Approved"
                            ExtensionStatus.REJECTED -> "Extension Rejected"
                            ExtensionStatus.PENDING -> "Extension Pending"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                
                Text(
                    text = "+${extension.extensionDays} days",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Details
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Reason: ${extension.extensionReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "From: ${extension.previousDeadline}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "To: ${extension.newDeadline}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (extension.status != ExtensionStatus.PENDING) {
                    Text(
                        text = "Processed by: ${extension.approvedByName ?: "System"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (extension.rejectionReason != null) {
                    Text(
                        text = "Rejection reason: ${extension.rejectionReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Error
                    )
                }
            }
        }
    }
}

@Composable
fun ExtensionRequestDialog(
    isVisible: Boolean,
    customerName: String,
    currentDeadline: String,
    extensionDays: Int,
    extensionReason: String,
    onReasonChange: (String) -> Unit,
    onDaysChange: (Int) -> Unit,
    onRequest: () -> Unit,
    onDismiss: () -> Unit,
    validation: ExtensionValidation,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Request Extension")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Customer info
                    BentoBox {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Customer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = customerName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "Current Deadline: $currentDeadline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Extension days
                    Column {
                        Text(
                            text = "Extension Days",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onDaysChange((extensionDays - 1).coerceAtLeast(1)) },
                                enabled = extensionDays > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Remove day")
                            }
                            
                            Text(
                                text = "+$extensionDays days",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            
                            IconButton(
                                onClick = { onDaysChange((extensionDays + 1).coerceAtMost(30)) },
                                enabled = extensionDays < 30
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add day")
                            }
                        }
                    }
                    
                    // Extension reason
                    Column {
                        Text(
                            text = "Extension Reason *",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = extensionReason,
                            onValueChange = onReasonChange,
                            placeholder = { Text("Enter detailed reason for extension request...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            isError = extensionReason.isNotBlank() && extensionReason.length < 10
                        )
                        
                        if (extensionReason.isNotBlank() && extensionReason.length < 10) {
                            Text(
                                text = "Reason must be at least 10 characters",
                                style = MaterialTheme.typography.bodySmall,
                                color = Error
                            )
                        }
                    }
                    
                    // Validation message
                    if (!validation.canExtend) {
                        Surface(
                            color = Error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = validation.reason,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onRequest,
                    enabled = validation.canExtend && extensionReason.length >= 10
                ) {
                    Text("Request Extension")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            modifier = modifier
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}
