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
import com.kprflow.enterprise.ui.viewmodel.CancellationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancellationDialog(
    kprId: String,
    customerName: String,
    unitBlock: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CancellationViewModel = hiltViewModel()
) {
    var selectedReason by remember { mutableStateOf<CancellationReason?>(null) }
    var additionalNotes by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.cancellationResult) {
        uiState.cancellationResult?.let { result ->
            if (result.isSuccess) {
                onSuccess()
                onDismiss()
                viewModel.clearResult()
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Batalkan Pesanan KPR",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Customer info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Detail Pesanan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Pelanggan: $customerName")
                        Text("Unit: $unitBlock")
                        Text("ID: $kprId")
                    }
                }
                
                // Reason selection
                Text(
                    text = "Alasan Pembatalan*",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                CancellationReason.values().forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(reason.displayName)
                    }
                }
                
                // Additional notes
                Text(
                    text = "Catatan Tambahan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = additionalNotes,
                    onValueChange = { additionalNotes = it },
                    placeholder = { Text("Masukkan catatan tambahan (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                
                // Warning message
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ Perhatian",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Unit akan kembali tersedia untuk pelanggan lain\n" +
                                  "• Semua transaksi pending akan dibatalkan\n" +
                                  "• Tindakan ini tidak dapat dibatalkan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedReason?.let { reason ->
                        viewModel.cancelKprApplication(
                            kprId = kprId,
                            reason = reason,
                            additionalNotes = additionalNotes.takeIf { it.isNotBlank() }
                        )
                    }
                },
                enabled = selectedReason != null && !uiState.isCancelling
            ) {
                if (uiState.isCancelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Membatalkan...")
                } else {
                    Text("Konfirmasi Pembatalan")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isCancelling
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun CancellationButton(
    kprId: String,
    customerName: String,
    unitBlock: String,
    currentStatus: String,
    onCancellationComplete: () -> Unit,
    viewModel: CancellationViewModel = hiltViewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    
    // Check if can be cancelled
    val canCancel = currentStatus !in listOf("CANCELLED", "BAST_COMPLETED")
    
    if (canCancel) {
        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Batalkan Pesanan")
        }
    }
    
    if (showDialog) {
        CancellationDialog(
            kprId = kprId,
            customerName = customerName,
            unitBlock = unitBlock,
            onDismiss = { showDialog = false },
            onSuccess = onCancellationComplete
        )
    }
    
    // Handle errors
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show toast or snackbar with error
            viewModel.clearError()
        }
    }
}

enum class CancellationReason(val displayName: String) {
    DATA_TIDAK_VALID("Data Tidak Valid"),
    CUSTOMER_MUNDUR("Customer Mundur"),
    REJECT_BANK("Ditolak Bank"),
    DOKUMEN_TIDAK_LENGKAP("Dokumen Tidak Lengkap"),
    SYARAT_TIDAK_MEMENUHI("Syarat Tidak Memenuhi"),
    DUPLIKASI_PESANAN("Duplikasi Pesanan"),
    KESALAHAN_SISTEM("Kesalahan Sistem"),
    LAINNYA("Lainnya")
}
