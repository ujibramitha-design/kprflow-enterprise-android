package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kprflow.enterprise.data.repository.FinalReportRecord
import com.kprflow.enterprise.data.repository.ReportType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalReportCard(
    report: FinalReportRecord,
    onDownload: (String) -> Unit,
    onDelete: (String) -> Unit,
    onUpdateSignatures: (Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSignatureDialog by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with report info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (report.reportType) {
                            ReportType.BAST -> "BAST Report"
                            ReportType.HANDOVER_CERTIFICATE -> "Handover Certificate"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Generated: ${formatDate(report.generatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Report Type Badge
                Badge(
                    containerColor = getReportTypeColor(report.reportType)
                ) {
                    Text(
                        text = report.reportType.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Report Details
            ReportInfoRow(
                label = "Handover Date",
                value = report.handoverDate
            )
            
            report.handoverTime?.let { time ->
                ReportInfoRow(
                    label = "Handover Time",
                    value = time
                )
            }
            
            report.handoverLocation?.let { location ->
                ReportInfoRow(
                    label = "Location",
                    value = location
                )
            }
            
            report.witnessName?.let { witness ->
                ReportInfoRow(
                    label = "Witness",
                    value = "$witness - ${report.witnessPosition ?: "N/A"}"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Signature Status
            SignatureStatusRow(
                customerSigned = report.customerSigned,
                developerSigned = report.developerSigned
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onDownload(report.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Download")
                }
                
                OutlinedButton(
                    onClick = { showSignatureDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Signatures")
                }
                
                OutlinedButton(
                    onClick = { onDelete(report.id) },
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
    
    // Signature Update Dialog
    if (showSignatureDialog) {
        SignatureUpdateDialog(
            currentCustomerSigned = report.customerSigned,
            currentDeveloperSigned = report.developerSigned,
            onDismiss = { showSignatureDialog = false },
            onUpdate = { customerSigned, developerSigned ->
                onUpdateSignatures(customerSigned, developerSigned)
                showSignatureDialog = false
            }
        )
    }
}

@Composable
private fun ReportInfoRow(
    label: String,
    value: String
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
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SignatureStatusRow(
    customerSigned: Boolean,
    developerSigned: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SignatureStatusItem(
            label = "Customer",
            isSigned = customerSigned
        )
        
        SignatureStatusItem(
            label = "Developer",
            isSigned = developerSigned
        )
    }
}

@Composable
private fun SignatureStatusItem(
    label: String,
    isSigned: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Badge(
            containerColor = if (isSigned) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = if (isSigned) "Signed" else "Pending",
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SignatureUpdateDialog(
    currentCustomerSigned: Boolean,
    currentDeveloperSigned: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (Boolean, Boolean) -> Unit
) {
    var customerSigned by remember { mutableStateOf(currentCustomerSigned) }
    var developerSigned by remember { mutableStateOf(currentDeveloperSigned) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Update Signatures")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Update signature status for this report:")
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = customerSigned,
                        onCheckedChange = { customerSigned = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Customer Signed")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = developerSigned,
                        onCheckedChange = { developerSigned = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Developer Signed")
                }
                
                if (customerSigned && developerSigned) {
                    Text(
                        text = "✅ Both signatures complete! Dossier status will be updated to BAST_COMPLETED.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdate(customerSigned, developerSigned)
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

private fun getReportTypeColor(reportType: ReportType): androidx.compose.ui.graphics.Color {
    return when (reportType) {
        ReportType.BAST -> MaterialTheme.colorScheme.primary
        ReportType.HANDOVER_CERTIFICATE -> MaterialTheme.colorScheme.secondary
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
