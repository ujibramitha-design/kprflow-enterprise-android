package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.viewmodel.DocumentMergerViewModel
import com.kprflow.enterprise.utils.ExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentMergerButton(
    dossierId: String,
    customerName: String,
    documentPaths: List<String>,
    viewModel: DocumentMergerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Validate documents on composition
    LaunchedEffect(documentPaths) {
        viewModel.validateDocuments(documentPaths)
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Validation status
        uiState.validationResult?.let { validation ->
            if (!validation.isValid) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ Masalah Dokumen",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        validation.errors.take(3).forEach { error ->
                            Text(
                                text = "• $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                        
                        if (validation.errors.size > 3) {
                            Text(
                                text = "... dan ${validation.errors.size - 3} masalah lainnya",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Merge button
        Button(
            onClick = {
                viewModel.mergeDocuments(
                    context = context,
                    dossierId = dossierId,
                    customerName = customerName,
                    documentPaths = uiState.validationResult?.validPaths ?: emptyList()
                )
            },
            enabled = !uiState.isMerging && (uiState.validationResult?.isValid == true),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isMerging) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Menggabungkan dokumen...")
            } else {
                Icon(
                    imageVector = Icons.Default.MergeType,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gabungkan Dokumen")
            }
        }
        
        // Progress indicator
        if (uiState.isMerging) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "Memproses ${uiState.validationResult?.validPaths?.size ?: 0} dokumen...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    // Handle merge result
    LaunchedEffect(uiState.mergeResult) {
        uiState.mergeResult?.let { result ->
            if (result.isSuccess) {
                result.getOrNull()?.let { mergedFile ->
                    // Open the merged file
                    ExportUtils.openFile(
                        context,
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            mergedFile
                        )
                    )
                }
            }
            
            viewModel.clearMergeResult()
        }
    }
}

@Composable
fun DocumentMergerSection(
    dossierId: String,
    customerName: String,
    documentPaths: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📄 Penggabungan Dokumen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Gabungkan KTP, KK, Slip Gaji, dan dokumen lainnya menjadi satu file PDF siap bank.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DocumentMergerButton(
                dossierId = dossierId,
                customerName = customerName,
                documentPaths = documentPaths
            )
        }
    }
}
