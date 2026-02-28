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
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DossierCard(
    dossier: KprDossier,
    onDossierClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onDossierClick(dossier.id) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with ID and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Application #${dossier.id.take(8).uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Created: ${dossier.bookingDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Badge(
                    containerColor = getStatusColor(dossier.status)
                ) {
                    Text(
                        text = dossier.status.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Financial Information
            dossier.kprAmount?.let { amount ->
                FinancialInfoRow(
                    label = "KPR Amount",
                    value = formatCurrency(amount)
                )
            }
            
            dossier.dpAmount?.let { amount ->
                FinancialInfoRow(
                    label = "Down Payment",
                    value = formatCurrency(amount)
                )
            }
            
            dossier.bankName?.let { bank ->
                FinancialInfoRow(
                    label = "Bank",
                    value = bank
                )
            }
            
            // Important Dates
            ImportantDatesSection(dossier)
            
            // Progress Indicator
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { getProgressPercentage(dossier.status) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FinancialInfoRow(
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
private fun ImportantDatesSection(dossier: KprDossier) {
    val dates = listOfNotNull(
        dossier.sp3kIssuedDate?.let { "SP3K" to it.toString() },
        dossier.akadDate?.let { "Akad" to it.toString() },
        dossier.disbursedDate?.let { "Disbursed" to it.toString() },
        dossier.bastDate?.let { "BAST" to it.toString() }
    )
    
    if (dates.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Important Dates",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        dates.forEach { (label, date) ->
            Text(
                text = "$label: $date",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun getStatusColor(status: com.kprflow.enterprise.data.model.KprStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        com.kprflow.enterprise.data.model.KprStatus.LEAD -> MaterialTheme.colorScheme.surfaceVariant
        com.kprflow.enterprise.data.model.KprStatus.PEMBERKASAN -> MaterialTheme.colorScheme.secondaryContainer
        com.kprflow.enterprise.data.model.KprStatus.PROSES_BANK -> MaterialTheme.colorScheme.tertiaryContainer
        com.kprflow.enterprise.data.model.KprStatus.PUTUSAN_KREDIT_ACC -> MaterialTheme.colorScheme.primaryContainer
        com.kprflow.enterprise.data.model.KprStatus.SP3K_TERBIT -> MaterialTheme.colorScheme.primary
        com.kprflow.enterprise.data.model.KprStatus.PRA_AKAD -> MaterialTheme.colorScheme.secondary
        com.kprflow.enterprise.data.model.KprStatus.AKAD_BELUM_CAIR -> MaterialTheme.colorScheme.tertiary
        com.kprflow.enterprise.data.model.KprStatus.FUNDS_DISBURSED -> MaterialTheme.colorScheme.primary
        com.kprflow.enterprise.data.model.KprStatus.BAST_READY -> MaterialTheme.colorScheme.secondary
        com.kprflow.enterprise.data.model.KprStatus.BAST_COMPLETED -> MaterialTheme.colorScheme.primary
        com.kprflow.enterprise.data.model.KprStatus.FLOATING_DOSSIER -> MaterialTheme.colorScheme.surfaceVariant
        com.kprflow.enterprise.data.model.KprStatus.CANCELLED_BY_SYSTEM -> MaterialTheme.colorScheme.error
    }
}

private fun getProgressPercentage(status: com.kprflow.enterprise.data.model.KprStatus): Float {
    val progressSteps = com.kprflow.enterprise.data.model.KprStatus.getProgressStatuses()
    val currentIndex = progressSteps.indexOf(status)
    return if (currentIndex >= 0) {
        (currentIndex + 1).toFloat() / progressSteps.size
    } else {
        0f
    }
}

private fun formatCurrency(amount: BigDecimal): String {
    return "Rp ${String.format("%,.0f", amount)}"
}
