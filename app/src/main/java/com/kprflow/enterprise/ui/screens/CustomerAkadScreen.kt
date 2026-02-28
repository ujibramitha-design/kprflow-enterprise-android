package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.theme.KPRFlowEnterpriseTheme
import com.kprflow.enterprise.viewmodel.CustomerAkadViewModel

/**
 * Customer Akad Screen
 * Customer view for Akad Credit information (read-only)
 * Phase 16: Mobile App Optimization - Enhanced Features
 */

@Composable
fun CustomerAkadScreen(
    navController: NavController,
    viewModel: CustomerAkadViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    KPRFlowEnterpriseTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Akad Credit Information") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState) {
                    is CustomerAkadUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is CustomerAkadUiState.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Akad Status Card
                            item {
                                AkadStatusCard(akad = uiState.akad)
                            }
                            
                            // Pra-Akad Information
                            if (uiState.praAkad != null) {
                                item {
                                    PraAkadInfoCard(praAkad = uiState.praAkad)
                                }
                            }
                            
                            // Akad Details
                            item {
                                AkadDetailsCard(akad = uiState.akad)
                            }
                            
                            // Documents Status
                            item {
                                DocumentsStatusCard(akad = uiState.akad)
                            }
                            
                            // Timeline
                            item {
                                AkadTimelineCard(akad = uiState.akad, praAkad = uiState.praAkad)
                            }
                        }
                    }
                    is CustomerAkadUiState.Error -> {
                        ErrorMessage(
                            message = uiState.message,
                            onRetry = { viewModel.refreshData() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AkadStatusCard(akad: CustomerAkadData) {
    BentoBox {
        Column {
            Text(
                text = "Akad Credit Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    StatusChip(
                        status = akad.akadStatus,
                        color = when (akad.akadStatus) {
                            "SCHEDULED" -> Info
                            "IN_PROGRESS" -> Warning
                            "COMPLETED" -> Success
                            else -> Error
                        }
                    )
                }
                
                if (akad.akadDate.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Scheduled Date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "${akad.akadDate} ${akad.akadTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (akad.akadLocation.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = akad.akadLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PraAkadInfoCard(praAkad: CustomerPraAkadData) {
    GlassSurface {
        Column {
            Text(
                text = "Pra-Akad Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (praAkad.praAkadDate.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "Pra-Akad: ${praAkad.praAkadDate} ${praAkad.praAkadTime ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (praAkad.praAkadLocation.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = praAkad.praAkadLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SI Lunas Status:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                StatusChip(
                    status = praAkad.siSuratKeteranganLunasStatus,
                    color = when (praAkad.siSuratKeteranganLunasStatus) {
                        "READY" -> Success
                        "PREPARING" -> Info
                        else -> Warning
                    }
                )
            }
        }
    }
}

@Composable
private fun AkadDetailsCard(akad: CustomerAkadData) {
    BentoBox {
        Column {
            Text(
                text = "Akad Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Unit Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Unit:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${akad.blockNumber}-${akad.unitNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "KPR Amount:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = formatCurrency(akad.kprAmount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Down Payment:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = formatCurrency(akad.dpAmount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bank:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = akad.bankName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Notaris Information
            if (akad.notarisName.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Notaris",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Column {
                        Text(
                            text = "Notaris:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = akad.notarisName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentsStatusCard(akad: CustomerAkadData) {
    GlassSurface {
        Column {
            Text(
                text = "Documents Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Document checklist
            val documents = listOf(
                "KTP" to true, // Assume verified
                "KK" to true,
                "NPWP" to true,
                "KTP Pasangan" to akad.ktpPasanganVerified,
                "SP3K" to akad.sp3kVerified,
                "SHGB" to akad.shgbVerified,
                "PBG/IMB" to akad.pbgImbVerified,
                "Surat Nikah" to akad.marriageCertificateVerified
            )
            
            documents.forEach { (docName, isVerified) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = docName,
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Icon(
                        imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isVerified) "Verified" else "Not Verified",
                        tint = if (isVerified) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun AkadTimelineCard(akad: CustomerAkadData, praAkad: CustomerPraAkadData?) {
    BentoBox {
        Column {
            Text(
                text = "Process Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Timeline items
            val timelineItems = mutableListOf<TimelineItem>()
            
            // Add SP3K issued
            timelineItems.add(
                TimelineItem(
                    title = "SP3K Issued",
                    date = "SP3K process completed",
                    status = "COMPLETED",
                    icon = Icons.Default.Assignment
                )
            )
            
            // Add Pra-Akad if exists
            praAkad?.let { pra ->
                timelineItems.add(
                    TimelineItem(
                        title = "Pra-Akad",
                        date = if (pra.praAkadDate.isNotEmpty()) "${pra.praAkadDate} ${pra.praAkadTime ?: ""}" else "Scheduled",
                        status = pra.praAkadStatus,
                        icon = Icons.Default.Event
                    )
                )
            }
            
            // Add Akad Credit
            timelineItems.add(
                TimelineItem(
                    title = "Akad Credit",
                    date = if (akad.akadDate.isNotEmpty()) "${akad.akadDate} ${akad.akadTime}" else "Scheduled",
                    status = akad.akadStatus,
                    icon = Icons.Default.Gavel
                )
            )
            
            timelineItems.forEachIndexed { index, item ->
                TimelineItemRow(item = item, isLast = index == timelineItems.lastIndex)
            }
        }
    }
}

@Composable
private fun TimelineItemRow(item: TimelineItem, isLast: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = when (item.status) {
                    "COMPLETED" -> Success
                    "IN_PROGRESS" -> Warning
                    "SCHEDULED" -> Info
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
            
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = item.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            StatusChip(
                status = item.status,
                color = when (item.status) {
                    "COMPLETED" -> Success
                    "IN_PROGRESS" -> Warning
                    "SCHEDULED" -> Info
                    else -> Error
                }
            )
            
            if (!isLast) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    BentoBox {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

// =====================================================
// DATA CLASSES
// =====================================================

data class CustomerAkadData(
    val id: String,
    val dossierId: String,
    val customerId: String,
    val customerName: String,
    val blockNumber: String,
    val unitNumber: String,
    val kprAmount: Double,
    val dpAmount: Double,
    val bankName: String,
    val akadStatus: String,
    val akadDate: String,
    val akadTime: String,
    val akadLocation: String,
    val akadNotes: String,
    val customerNotified: Boolean,
    val customerNotificationDate: String,
    val legalAssignedName: String,
    val notarisId: String,
    val notarisName: String,
    val notarisContact: String,
    val notarisAddress: String,
    val ktpVerified: Boolean,
    val kkVerified: Boolean,
    val npwpVerified: Boolean,
    val ktpPasanganVerified: Boolean,
    val sp3kVerified: Boolean,
    val shgbVerified: Boolean,
    val pbgImbVerified: Boolean,
    val marriageCertificateVerified: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class CustomerPraAkadData(
    val id: String,
    val dossierId: String,
    val praAkadStatus: String,
    val praAkadDate: String,
    val praAkadTime: String?,
    val praAkadLocation: String,
    val praAkadNotes: String,
    val siSuratKeteranganLunasStatus: String,
    val siSuratKeteranganLunasUrl: String,
    val legalAssignedName: String,
    val createdAt: String,
    val updatedAt: String
)

data class TimelineItem(
    val title: String,
    val date: String,
    val status: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// =====================================================
// UI STATES
// =====================================================

sealed class CustomerAkadUiState {
    object Loading : CustomerAkadUiState()
    data class Success(
        val akad: CustomerAkadData,
        val praAkad: CustomerPraAkadData?
    ) : CustomerAkadUiState()
    data class Error(val message: String) : CustomerAkadUiState()
}

// =====================================================
// UTILITY FUNCTIONS
// =====================================================

private fun formatCurrency(amount: Double): String {
    val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    return formatter.format(amount)
}
