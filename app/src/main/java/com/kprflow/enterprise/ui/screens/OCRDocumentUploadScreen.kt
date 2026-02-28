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
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.viewmodel.OCRDocumentUploadViewModel
import com.kprflow.enterprise.ui.components.AccessibleButton
import com.kprflow.enterprise.ui.components.StatusBadge
import com.kprflow.enterprise.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OCRDocumentUploadScreen(
    onBackClick: () -> Unit,
    onUploadComplete: () -> Unit,
    viewModel: OCRDocumentUploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val documentTypes by viewModel.documentTypes.collectAsState()
    val processingHistory by viewModel.processingHistory.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadDocumentTypes()
        viewModel.loadProcessingHistory()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            
            Text(
                text = "Upload Dokumen OCR",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = { viewModel.refreshData() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Document Type Selection
        DocumentTypeSelectionCard(
            documentTypes = documentTypes,
            selectedType = viewModel.selectedDocumentType,
            onTypeSelected = { viewModel.selectDocumentType(it) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Upload Area
        when (uiState) {
            is OCRUploadUiState.Idle -> {
                UploadAreaCard(
                    selectedType = viewModel.selectedDocumentType,
                    onFileSelected = { file -> viewModel.processDocument(file) }
                )
            }
            
            is OCRUploadUiState.Processing -> {
                ProcessingCard(
                    fileName = uiState.fileName,
                    progress = uiState.progress
                )
            }
            
            is OCRUploadUiState.Success -> {
                SuccessCard(
                    result = uiState.result,
                    onComplete = onUploadComplete
                )
            }
            
            is OCRUploadUiState.Error -> {
                ErrorCard(
                    message = uiState.message,
                    onRetry = { viewModel.resetState() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Processing History
        ProcessingHistoryCard(
            history = processingHistory,
            onViewDetails = { item -> viewModel.viewProcessingDetails(item) }
        )
    }
}

@Composable
private fun DocumentTypeSelectionCard(
    documentTypes: List<OCRDocumentType>,
    selectedType: OCRDocumentType?,
    onTypeSelected: (OCRDocumentType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Pilih Jenis Dokumen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(documentTypes) { type ->
                    DocumentTypeItem(
                        type = type,
                        isSelected = selectedType?.id == type.id,
                        onSelected = { onTypeSelected(type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentTypeItem(
    type: OCRDocumentType,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                           else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clickable { onSelected() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (type.documentCode) {
                    "NO_SPR" -> Icons.Default.Description
                    "BONUS_MEMO" -> Icons.Default.AttachMoney
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = type.documentName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun UploadAreaCard(
    selectedType: OCRDocumentType?,
    onFileSelected: (String) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedType == null) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Pilih jenis dokumen terlebih dahulu",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Upload ${selectedType.documentName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Seret dan lepas file di sini atau klik untuk memilih",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AccessibleButton(
                    text = "Pilih File",
                    onClick = { /* Handle file selection */ }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Format yang didukung: PDF, JPG, PNG (Max 10MB)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProcessingCard(
    fileName: String,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Memproses dokumen...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${(progress * 100).toInt()}% Selesai",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuccessCard(
    result: OCRProcessingResult,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Pemrosesan Berhasil!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Processing Details
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProcessingDetailItem(
                    label = "Jenis Dokumen",
                    value = result.documentType
                )
                
                ProcessingDetailItem(
                    label = "Status Auto-Fill",
                    value = result.autoFillStatus,
                    color = when (result.autoFillStatus) {
                        "SUCCESS" -> MaterialTheme.colorScheme.primary
                        "PARTIAL" -> MaterialTheme.colorScheme.secondary
                        "FAILED" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                ProcessingDetailItem(
                    label = "Confidence Score",
                    value = "${result.confidenceScore}%"
                )
                
                if (result.autoFilledFields.isNotEmpty()) {
                    ProcessingDetailItem(
                        label = "Field Terisi Otomatis",
                        value = result.autoFilledFields.joinToString(", ")
                    )
                }
                
                ProcessingDetailItem(
                    label = "Kategori Otomatis",
                    value = result.assignedCategory
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AccessibleButton(
                text = "Lanjutkan",
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProcessingDetailItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
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
            color = color
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Pemrosesan Gagal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AccessibleButton(
                text = "Coba Lagi",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProcessingHistoryCard(
    history: List<OCRProcessingHistoryItem>,
    onViewDetails: (OCRProcessingHistoryItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Riwayat Pemrosesan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (history.isEmpty()) {
                Text(
                    text = "Belum ada dokumen yang diproses",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(history) { item ->
                        HistoryItem(
                            item = item,
                            onClick = { onViewDetails(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    item: OCRProcessingHistoryItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (item.documentType) {
                    "NO_SPR" -> Icons.Default.Description
                    "BONUS_MEMO" -> Icons.Default.AttachMoney
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${item.documentType} • ${item.processedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            StatusBadge(
                status = item.status,
                color = when (item.status) {
                    "SUCCESS" -> MaterialTheme.colorScheme.primary
                    "FAILED" -> MaterialTheme.colorScheme.error
                    "PROCESSING" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        }
    }
}

// Data Models
data class OCRDocumentType(
    val id: String,
    val documentName: String,
    val documentCode: String,
    val description: String,
    val isActive: Boolean = true
)

data class OCRProcessingResult(
    val processingLogId: String,
    val documentType: String,
    val autoFillStatus: String,
    val confidenceScore: Double,
    val autoFilledFields: List<String>,
    val assignedCategory: String,
    val extractedData: Map<String, Any>
)

data class OCRProcessingHistoryItem(
    val id: String,
    val fileName: String,
    val documentType: String,
    val status: String,
    val confidenceScore: Double,
    val processedAt: String
)

sealed class OCRUploadUiState {
    object Idle : OCRUploadUiState()
    data class Processing(val fileName: String, val progress: Float) : OCRUploadUiState()
    data class Success(val result: OCRProcessingResult) : OCRUploadUiState()
    data class Error(val message: String) : OCRUploadUiState()
}
