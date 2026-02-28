package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun KanbanBoard(
    dossiers: List<KprDossier>,
    onStatusChange: (String, KprStatus) -> Unit,
    onDossierClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = listOf(
        KanbanColumn("Document Collection", KprStatus.PEMBERKASAN),
        KanbanColumn("Bank Process", KprStatus.PROSES_BANK),
        KanbanColumn("Credit Approved", KprStatus.PUTUSAN_KREDIT_ACC),
        KanbanColumn("SP3K Issued", KprStatus.SP3K_TERBIT),
        KanbanColumn("Pre-Akad", KprStatus.PRA_AKAD)
    )
    
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        columns.forEach { column ->
            KanbanColumn(
                title = column.title,
                status = column.status,
                dossiers = dossiers.filter { it.status == column.status },
                onStatusChange = onStatusChange,
                onDossierClick = onDossierClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KanbanColumn(
    title: String,
    status: KprStatus,
    dossiers: List<KprDossier>,
    onStatusChange: (String, KprStatus) -> Unit,
    onDossierClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Column Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Badge {
                    Text(
                        text = "${dossiers.size}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Dossiers List
            if (dossiers.isEmpty()) {
                EmptyKanbanColumn()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dossiers) { dossier ->
                        KanbanCard(
                            dossier = dossier,
                            onClick = { onDossierClick(dossier.id) },
                            onStatusChange = { newStatus -> 
                                onStatusChange(dossier.id, newStatus) 
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyKanbanColumn() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No items",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class KanbanColumn(
    val title: String,
    val status: KprStatus
)
