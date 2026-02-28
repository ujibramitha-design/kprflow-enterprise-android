package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kprflow.enterprise.data.model.KprDossier
import com.kprflow.enterprise.data.model.KprStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanCard(
    dossier: KprDossier,
    onClick: () -> Unit,
    onStatusChange: (KprStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Customer Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dossier.customerName ?: "Unknown Customer",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    dossier.unitId?.let { unitId ->
                        Text(
                            text = "Unit: $unitId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status Menu
                Box {
                    IconButton(
                        onClick = { showStatusMenu = true }
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.MoreVert,
                            contentDescription = "Change Status",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        KprStatus.values().forEach { status ->
                            if (isValidStatusTransition(dossier.status, status)) {
                                DropdownMenuItem(
                                    text = { Text(status.displayName) },
                                    onClick = {
                                        onStatusChange(status)
                                        showStatusMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Bank Info
            dossier.bankName?.let { bank ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.AccountBalance,
                        contentDescription = "Bank",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = bank,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // KPR Amount
            dossier.kprAmount?.let { amount ->
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Rp ${String.format("%,.0f", amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Progress Indicator
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { getProgressPercentage(dossier.status) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${getProgressPercentage(dossier.status).times(100).toInt()}% Complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun isValidStatusTransition(currentStatus: KprStatus, newStatus: KprStatus): Boolean {
    // Define valid status transitions for Legal role
    return when (currentStatus) {
        KprStatus.PEMBERKASAN -> newStatus in listOf(KprStatus.PROSES_BANK, KprStatus.CANCELLED_BY_SYSTEM)
        KprStatus.PROSES_BANK -> newStatus in listOf(KprStatus.PUTUSAN_KREDIT_ACC, KprStatus.PEMBERKASAN, KprStatus.CANCELLED_BY_SYSTEM)
        KprStatus.PUTUSAN_KREDIT_ACC -> newStatus in listOf(KprStatus.SP3K_TERBIT, KprStatus.PROSES_BANK)
        KprStatus.SP3K_TERBIT -> newStatus in listOf(KprStatus.PRA_AKAD, KprStatus.PUTUSAN_KREDIT_ACC)
        KprStatus.PRA_AKAD -> newStatus in listOf(KprStatus.AKAD_BELUM_CAIR, KprStatus.SP3K_TERBIT)
        else -> false
    }
}

private fun getProgressPercentage(status: KprStatus): Float {
    val progressStatuses = listOf(
        KprStatus.LEAD,
        KprStatus.PEMBERKASAN,
        KprStatus.PROSES_BANK,
        KprStatus.PUTUSAN_KREDIT_ACC,
        KprStatus.SP3K_TERBIT,
        KprStatus.PRA_AKAD,
        KprStatus.AKAD_BELUM_CAIR,
        KprStatus.FUNDS_DISBURSED,
        KprStatus.BAST_READY,
        KprStatus.BAST_COMPLETED
    )
    
    val currentIndex = progressStatuses.indexOf(status)
    return if (currentIndex >= 0) {
        (currentIndex + 1).toFloat() / progressStatuses.size.toFloat()
    } else {
        0f
    }
}
