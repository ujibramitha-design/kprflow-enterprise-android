package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.viewmodel.SiKasepViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiKasepEligibilityCard(
    userId: String,
    customerName: String,
    nik: String,
    monthlyIncome: BigDecimal?,
    isFirstHome: Boolean?,
    onEligibilityChecked: () -> Unit,
    viewModel: SiKasepViewModel = hiltViewModel()
) {
    var showDetails by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.checkResult) {
        uiState.checkResult?.let { result ->
            if (result.isSuccess) {
                onEligibilityChecked()
                viewModel.clearCheckResult()
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏛️ Kelayakan Subsidi FLPP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = { showDetails = !showDetails }
                ) {
                    Icon(
                        imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showDetails) "Hide details" else "Show details"
                    )
                }
            }
            
            // Customer info
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text("Pelanggan: $customerName", style = MaterialTheme.typography.bodySmall)
                    Text("NIK: $nik", style = MaterialTheme.typography.bodySmall)
                    monthlyIncome?.let {
                        Text("Penghasilan: Rp ${String.format("%,.0f", it)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Rumah Pertama: ${if (isFirstHome == true) "Ya" else "Tidak"}", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            // Current status
            when (uiState.currentStatus) {
                "ELIGIBLE" -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "✅ Layak Subsidi FLPP",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                uiState.sikasepId?.let { id ->
                                    Text(
                                        text = "ID SiKasep: $id",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
                "NOT_ELIGIBLE" -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "❌ Tidak Layak Subsidi",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                uiState.rejectionReason?.let { reason ->
                                    Text(
                                        text = "Alasan: $reason",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
                "NOT_CHECKED" -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Help,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Belum dicek kelayakan subsidi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    // Loading or error state
                }
            }
            
            // Details section
            if (showDetails) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Kriteria Kelayakan FLPP:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EligibilityCriterion(
                            title = "Penghasilan Bulanan Maksimal",
                            requirement = "Rp 8.000.000",
                            met = monthlyIncome != null && monthlyIncome <= BigDecimal("8000000")
                        )
                        
                        EligibilityCriterion(
                            title = "Rumah Pertama",
                            requirement = "Wajib",
                            met = isFirstHome == true
                        )
                        
                        EligibilityCriterion(
                            title = "NIK Valid",
                            requirement = "16 digit",
                            met = nik.length == 16 && nik.all { it.isDigit() }
                        )
                    }
                    
                    // Last checked info
                    uiState.lastChecked?.let { checked ->
                        Text(
                            text = "Terakhir dicek: ${formatDate(checked)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Action button
            if (uiState.currentStatus != "ELIGIBLE" && uiState.currentStatus != "NOT_ELIGIBLE") {
                Button(
                    onClick = {
                        viewModel.checkEligibility(userId, nik, monthlyIncome, isFirstHome)
                    },
                    enabled = nik.length == 16 && nik.all { it.isDigit() } && !uiState.isChecking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Memeriksa kelayakan...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cek Kelayakan Subsidi")
                    }
                }
            }
            
            // Recheck button
            if (uiState.currentStatus in listOf("ELIGIBLE", "NOT_ELIGIBLE")) {
                OutlinedButton(
                    onClick = {
                        viewModel.checkEligibility(userId, nik, monthlyIncome, isFirstHome)
                    },
                    enabled = !uiState.isChecking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Memeriksa ulang...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Periksa Ulang")
                    }
                }
            }
            
            // Error message
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EligibilityCriterion(
    title: String,
    requirement: String,
    met: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (met) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = requirement,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(timestamp: String): String {
    return try {
        val date = java.time.Instant.parse(timestamp)
        java.time.format.DateTimeFormatter
            .ofPattern("dd MMM yyyy, HH:mm")
            .withZone(java.time.ZoneId.of("Asia/Jakarta"))
            .format(date)
    } catch (e: Exception) {
        timestamp
    }
}
