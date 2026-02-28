package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kprflow.enterprise.data.repository.BankDecisionRecord
import com.kprflow.enterprise.data.repository.BankDecisionResultType
import com.kprflow.enterprise.data.repository.BankDecisionStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDecisionCard(
    decision: BankDecisionRecord,
    onDownload: (String) -> Unit,
    onDelete: (String) -> Unit,
    onUpdateStatus: (BankDecisionStatus, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(decision.status) }
    var statusNotes by remember { mutableStateOf("") }
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with decision info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bank Decision",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bank: ${decision.bankName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Decision Badge
                Badge(
                    containerColor = getDecisionColor(decision.decisionType)
                ) {
                    Text(
                        text = decision.decisionType.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Decision Details
            DecisionInfoRow(
                label = "Decision Date",
                value = formatDate(decision.uploadedAt)
            )
            
            DecisionInfoRow(
                label = "File Name",
                value = decision.fileName
            )
            
            decision.approvedAmount?.let { amount ->
                DecisionInfoRow(
                    label = "Approved Amount",
                    value = formatCurrency(amount)
                )
            }
            
            decision.decisionReason?.let { reason ->
                DecisionInfoRow(
                    label = "Decision Reason",
                    value = reason
                )
            }
            
            decision.rejectionReason?.let { reason ->
                DecisionInfoRow(
                    label = "Rejection Reason",
                    value = reason,
                    isError = true
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onDownload(decision.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Download")
                }
                
                OutlinedButton(
                    onClick = { showStatusDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Update Status")
                }
                
                OutlinedButton(
                    onClick = { onDelete(decision.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
    
    // Status Update Dialog
    if (showStatusDialog) {
        StatusUpdateDialog(
            currentStatus = decision.status,
            onDismiss = { showStatusDialog = false },
            onUpdate = { status, notes ->
                onUpdateStatus(status, notes)
                showStatusDialog = false
            }
        )
    }
}

@Composable
private fun DecisionInfoRow(
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
private fun StatusUpdateDialog(
    currentStatus: BankDecisionStatus,
    onDismiss: () -> Unit,
    onUpdate: (BankDecisionStatus, String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Update Decision Status")
        },
        text = {
            Column {
                Text("Select new status:")
                Spacer(modifier = Modifier.height(8.dp))
                
                BankDecisionStatus.values().forEach { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(status.name)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdate(selectedStatus, notes.ifBlank { null })
                }
            ) {
                Text("Update")
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
fun BankDecisionStatsCard(
    stats: com.kprflow.enterprise.data.repository.BankDecisionStats,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Decision Statistics",
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
                    value = stats.totalDecisions.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    label = "Approved",
                    value = stats.approvedDecisions.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    label = "Rejected",
                    value = stats.rejectedDecisions.toString(),
                    color = MaterialTheme.colorScheme.error
                )
                
                StatItem(
                    label = "Approval Rate",
                    value = "${String.format("%.1f", stats.approvalRate)}%",
                    color = MaterialTheme.colorScheme.secondary
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

private fun getDecisionColor(decisionType: BankDecisionResultType): androidx.compose.ui.graphics.Color {
    return when (decisionType) {
        BankDecisionResultType.APPROVED -> MaterialTheme.colorScheme.primary
        BankDecisionResultType.REJECTED -> MaterialTheme.colorScheme.error
        BankDecisionResultType.PENDING -> MaterialTheme.colorScheme.secondary
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date = sdf.parse(dateString)
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        "Unknown date"
    }
}

private fun formatCurrency(amount: java.math.BigDecimal): String {
    return "Rp ${String.format("%,.0f", amount)}"
}
