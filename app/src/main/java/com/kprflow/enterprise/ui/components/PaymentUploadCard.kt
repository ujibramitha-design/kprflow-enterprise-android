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
import com.kprflow.enterprise.ui.viewmodel.FinancialViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentUploadCard(
    dossierId: String,
    unitBlock: String,
    customerName: String,
    onPaymentUploaded: () -> Unit,
    viewModel: FinancialViewModel = hiltViewModel()
) {
    var selectedCategory by remember { mutableStateOf<PaymentCategory?>(null) }
    var amount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("TRANSFER") }
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var evidenceUri by remember { mutableStateOf<String?>(null) }
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.uploadResult) {
        uiState.uploadResult?.let { result ->
            if (result.isSuccess) {
                onPaymentUploaded()
                viewModel.clearUploadResult()
                // Reset form
                selectedCategory = null
                amount = ""
                bankName = ""
                accountNumber = ""
                accountName = ""
                referenceNumber = ""
                notes = ""
                evidenceUri = null
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
            Text(
                text = "💰 Upload Bukti Pembayaran",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
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
            
            // Payment category
            Text(
                text = "Jenis Pembayaran*",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            PaymentCategory.values().forEach { category ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(category.displayName)
                        Text(
                            text = category.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Auto-fill amount when category is selected
            selectedCategory?.let { category ->
                if (amount.isEmpty()) {
                    amount = category.defaultAmount.toString()
                }
            }
            
            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Nominal Pembayaran*") },
                placeholder = { Text("0") },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("Rp ") }
            )
            
            // Payment method
            Text(
                text = "Metode Pembayaran",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { paymentMethod = "TRANSFER" },
                    label = { Text("Transfer") },
                    selected = paymentMethod == "TRANSFER"
                )
                FilterChip(
                    onClick = { paymentMethod = "CASH" },
                    label = { Text("Tunai") },
                    selected = paymentMethod == "CASH"
                )
                FilterChip(
                    onClick = { paymentMethod = "QRIS" },
                    label = { Text("QRIS") },
                    selected = paymentMethod == "QRIS"
                )
            }
            
            // Bank details (for transfer)
            if (paymentMethod == "TRANSFER") {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Nama Bank") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("No. Rekening") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Nama Pemilik Rekening") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Reference number
            OutlinedTextField(
                value = referenceNumber,
                onValueChange = { referenceNumber = it },
                label = { Text("No. Referensi") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Evidence upload
            Text(
                text = "Bukti Pembayaran*",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            if (evidenceUri != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✅ Bukti terupload",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        TextButton(
                            onClick = { evidenceUri = null }
                        ) {
                            Text("Ganti")
                        }
                    }
                }
            } else {
                Button(
                    onClick = { /* TODO: Implement image picker */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Bukti Pembayaran")
                }
            }
            
            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            // Submit button
            Button(
                onClick = {
                    selectedCategory?.let { category ->
                        val amountValue = amount.replace("[^\\d]".toRegex(), "").toBigDecimalOrNull()
                        
                        viewModel.uploadPaymentProof(
                            dossierId = dossierId,
                            category = category,
                            amount = amountValue ?: BigDecimal.ZERO,
                            paymentMethod = paymentMethod,
                            bankName = bankName.takeIf { it.isNotBlank() },
                            accountNumber = accountNumber.takeIf { it.isNotBlank() },
                            accountName = accountName.takeIf { it.isNotBlank() },
                            referenceNumber = referenceNumber.takeIf { it.isNotBlank() },
                            evidenceUri = evidenceUri,
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    }
                },
                enabled = selectedCategory != null && 
                         amount.isNotBlank() && 
                         amount.replace("[^\\d]".toRegex(), "").toBigDecimalOrNull() != null &&
                         amount.replace("[^\\d]".toRegex(), "").toBigDecimalOrNull()!! > BigDecimal.ZERO &&
                         evidenceUri != null &&
                         !uiState.isUploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mengupload...")
                } else {
                    Text("Upload Bukti Pembayaran")
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

enum class PaymentCategory(
    val displayName: String,
    val description: String,
    val defaultAmount: BigDecimal
) {
    BOOKING_FEE(
        "Booking Fee",
        "Uang tanda jadi reservasi unit",
        BigDecimal("5000000")
    ),
    DP_1(
        "DP 1",
        "Down payment pertama (30%)",
        BigDecimal("0") // Will be calculated based on unit price
    ),
    DP_2(
        "DP 2", 
        "Down payment kedua (20%)",
        BigDecimal("0") // Will be calculated based on unit price
    ),
    DP_PELUNASAN(
        "DP Pelunasan",
        "Pelunasan down payment (50%)",
        BigDecimal("0") // Will be calculated based on unit price
    ),
    BIAYA_STRATEGIS(
        "Biaya Strategis",
        "Biaya administrasi dan legal",
        BigDecimal("2500000")
    ),
    ADMIN_FEE(
        "Biaya Admin",
        "Biaya administrasi bank",
        BigDecimal("500000")
    ),
    NOTARY_FEE(
        "Biaya Notaris",
        "Biaya balik nama dan notaris",
        BigDecimal("7500000")
    ),
    INSURANCE_FEE(
        "Biaya Asuransi",
        "Premi asuransi properti",
        BigDecimal("0") // Will be calculated based on coverage
    )
}
