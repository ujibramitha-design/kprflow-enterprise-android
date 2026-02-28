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
import com.kprflow.enterprise.ui.viewmodel.BulkZipViewModel
import com.kprflow.enterprise.utils.BulkZipGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkZipGeneratorCard(
    dossierId: String,
    customerName: String,
    unitBlock: String,
    onZipGenerated: (android.net.Uri) -> Unit,
    viewModel: BulkZipViewModel = hiltViewModel()
) {
    var showDetails by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.zipResult) {
        uiState.zipResult?.let { result ->
            if (result.isSuccess) {
                result.getOrNull()?.let { uri ->
                    onZipGenerated(uri)
                    viewModel.clearZipResult()
                }
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
                    text = "📦 Bank Submission Pack",
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
                    Text("Unit: $unitBlock", style = MaterialTheme.typography.bodySmall)
                    Text("ID: $dossierId", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            // Document validation
            uiState.validationResult?.let { validation ->
                if (!validation.isValid) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "❌ Validasi Dokumen Gagal",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            validation.errors.forEach { error ->
                                Text(
                                    text = "• $error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                if (validation.warnings.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠️ Peringatan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            validation.warnings.forEach { warning ->
                                Text(
                                    text = "• $warning",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            // Progress indicator
            if (uiState.isGenerating) {
                uiState.progress?.let { progress ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "📁 Membuat Paket Bank",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            LinearProgressIndicator(
                                progress = if (progress.totalFiles > 0) {
                                    progress.currentFile.toFloat() / progress.totalFiles.toFloat()
                                } else 0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Text(
                                text = "File: ${progress.currentFileName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Text(
                                text = "Progress: ${progress.currentFile}/${progress.totalFiles}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            if (progress.totalBytes > 0) {
                                val percentage = (progress.bytesDownloaded.toFloat() / progress.totalBytes.toFloat() * 100).toInt()
                                Text(
                                    text = "Download: ${percentage}% (${formatBytes(progress.bytesDownloaded)}/${formatBytes(progress.totalBytes)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            // Details section
            if (showDetails) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Struktur Folder ZIP:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FolderStructure(
                            folder = "01_Legal/",
                            description = "Dokumen legal (KTP, KK, Akta Kelahiran, Surat Nikah)"
                        )
                        FolderStructure(
                            folder = "02_Income/",
                            description = "Dokumen penghasilan (Slip Gaji, Rekening Koran, Buku Tabungan)"
                        )
                        FolderStructure(
                            folder = "03_Property/",
                            description = "Dokumen properti (Sertifikat, IMB, PBB)"
                        )
                        FolderStructure(
                            folder = "04_Dossier/",
                            description = "Dossier tergabung (hasil dari Phase 8)"
                        )
                    }
                    
                    Text(
                        text = "Dokumen Wajib:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("• KTP (Kartu Tanda Penduduk)", style = MaterialTheme.typography.bodySmall)
                        Text("• KK (Kartu Keluarga)", style = MaterialTheme.typography.bodySmall)
                        Text("• Slip Gaji (3 bulan terakhir)", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Text(
                        text = "Informasi Paket:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("• Nama file: ${customerName.replace(" ", "_")}_${unitBlock}_KPR.zip", style = MaterialTheme.typography.bodySmall)
                        Text("• Maksimal ukuran: 100MB", style = MaterialTheme.typography.bodySmall)
                        Text("• Format: ZIP dengan struktur folder bank", style = MaterialTheme.typography.bodySmall)
                        Text("• Include: File SUMMARY.txt dengan daftar dokumen", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            // Action buttons
            if (!uiState.isGenerating) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.generateBankPack(dossierId, customerName, unitBlock)
                        },
                        enabled = uiState.validationResult?.isValid != false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Bank Pack")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            viewModel.validateDocuments(dossierId)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Validasi Dokumen")
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
fun FolderStructure(
    folder: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = folder,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    
    return when {
        mb >= 1 -> "%.1fMB".format(mb)
        kb >= 1 -> "%.0fKB".format(kb)
        else -> "${bytes}B"
    }
}
