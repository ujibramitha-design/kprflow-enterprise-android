package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.viewmodel.ExportViewModel
import com.kprflow.enterprise.utils.ExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportMenu(
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Export Options"
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Export Excel (.xlsx)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onExportExcel()
                }
            )
            
            DropdownMenuItem(
                text = { Text("Export PDF") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onExportPdf()
                }
            )
        }
    }
}

@Composable
fun ExportButton(
    isExporting: Boolean = false,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onExportClick,
        enabled = !isExporting,
        modifier = modifier
    ) {
        if (isExporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mengekspor...")
        } else {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Unduh Laporan")
        }
    }
}

@Composable
fun ExportSection(
    viewModel: ExportViewModel = hiltViewModel(),
    data: List<Any>, // Replace with actual data type
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExportMenu(
            onExportExcel = {
                viewModel.exportToExcel(context, data)
            },
            onExportPdf = {
                viewModel.exportToPdf(context, data)
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        ExportButton(
            isExporting = uiState.isExporting,
            onExportClick = {
                viewModel.exportToExcel(context, data)
            }
        )
    }
    
    // Show toast when export is complete
    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let { result ->
            if (result.isSuccess) {
                Toast.makeText(
                    context,
                    "Laporan berhasil diekspor",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Open the file
                result.getOrNull()?.let { uri ->
                    ExportUtils.openFile(context, uri)
                }
            } else {
                Toast.makeText(
                    context,
                    "Gagal mengekspor laporan",
                    Toast.LENGTH_LONG
                ).show()
            }
            
            viewModel.clearExportResult()
        }
    }
}
