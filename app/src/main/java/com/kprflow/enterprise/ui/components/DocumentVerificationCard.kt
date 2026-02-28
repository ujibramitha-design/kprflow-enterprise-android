package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kprflow.enterprise.data.model.Document
import com.kprflow.enterprise.data.model.DocumentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentVerificationCard(
    document: Document,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onBatchAction: (List<String>, Boolean, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRejectionDialog by remember { mutableStateOf(false) }
    var selectedDocuments by remember { mutableStateOf(setOf<String>()) }
    var isSelected by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            isSelected = checked
                            if (checked) {
                                selectedDocuments = selectedDocuments + document.id
                            } else {
                                selectedDocuments = selectedDocuments - document.id
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = document.type.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Uploaded: ${formatDate(document.uploadedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status Badge
                Badge(
                    containerColor = if (document.isVerified) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (document.isVerified) "Verified" else "Pending",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Document Info
            DocumentInfoRow(
                label = "File Name",
                value = document.fileName
            )
            
            DocumentInfoRow(
                label = "File Size",
                value = formatFileSize(document.fileSize)
            )
            
            if (document.verifiedBy != null) {
                DocumentInfoRow(
                    label = "Verified By",
                    value = document.verifiedBy!!
                )
            }
            
            if (document.rejectionReason != null) {
                DocumentInfoRow(
                    label = "Rejection Reason",
                    value = document.rejectionReason!!,
                    isError = true
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            if (!document.isVerified) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onApprove(document.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Approve")
                    }
                    
                    OutlinedButton(
                        onClick = { showRejectionDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Reject")
                    }
                }
            }
        }
    }
    
    // Rejection Dialog
    if (showRejectionDialog) {
        RejectionDialog(
            onDismiss = { showRejectionDialog = false },
            onReject = { reason ->
                onReject(document.id)
                showRejectionDialog = false
            }
        )
    }
}

@Composable
private fun DocumentInfoRow(
    label: String,
    value: String,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RejectionDialog(
    onDismiss: () -> Unit,
    onReject: (String) -> Unit
) {
    var rejectionReason by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Reject Document")
        },
        text = {
            Column {
                Text("Please provide a reason for rejection:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rejectionReason,
                    onValueChange = { rejectionReason = it },
                    label = { Text("Rejection Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rejectionReason.isNotBlank()) {
                        onReject(rejectionReason)
                    }
                },
                enabled = rejectionReason.isNotBlank()
            ) {
                Text("Reject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun VerificationStatsCard(
    stats: com.kprflow.enterprise.data.repository.VerificationStats,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Verification Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total",
                    value = stats.totalDocuments.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    label = "Verified",
                    value = stats.verifiedDocuments.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    label = "Pending",
                    value = stats.pendingDocuments.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
                
                StatItem(
                    label = "Rejected",
                    value = stats.rejectedDocuments.toString(),
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
            style = MaterialTheme.typography.headlineSmall,
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

private fun formatDate(dateString: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        val date = sdf.parse(dateString)
        val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        outputFormat.format(date ?: java.util.Date())
    } catch (e: Exception) {
        "Unknown date"
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
