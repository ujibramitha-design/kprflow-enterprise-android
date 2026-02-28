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
import com.kprflow.enterprise.viewmodel.DocumentRequirementsViewModel

/**
 * Document Requirements Screen
 * Legal-only access for managing bank-specific document requirements
 * Phase 16: Mobile App Optimization - Enhanced Features
 */

@Composable
fun DocumentRequirementsScreen(
    navController: NavController,
    viewModel: DocumentRequirementsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedBank by remember { mutableStateOf<BankDocumentData?>(null) }
    var showConditionalDialog by remember { mutableStateOf(false) }
    
    KPRFlowEnterpriseTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Document Requirements") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                // Bank Filter Tabs
                BankFilterTabs(
                    selectedBank = viewModel.selectedBank.value,
                    onBankSelected = { bank ->
                        viewModel.setBankFilter(bank)
                    },
                    banks = listOf("ALL", "Bank BTN", "Bank BNI", "Bank BSI")
                )
                
                // Content
                when (uiState) {
                    is DocumentRequirementsUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is DocumentRequirementsUiState.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.banks) { bank ->
                                BankDocumentCard(
                                    bank = bank,
                                    onViewDetails = {
                                        selectedBank = bank
                                        showConditionalDialog = true
                                    },
                                    onEdit = { bankId ->
                                        navController.navigate("edit_bank_docs/$bankId")
                                    }
                                )
                            }
                        }
                    }
                    is DocumentRequirementsUiState.Error -> {
                        ErrorMessage(
                            message = uiState.message,
                            onRetry = { viewModel.refreshData() }
                        )
                    }
                }
            }
        }
        
        // Conditional Documents Dialog
        if (showConditionalDialog && selectedBank != null) {
            ConditionalDocumentsDialog(
                bank = selectedBank!!,
                onDismiss = { 
                    showConditionalDialog = false
                    selectedBank = null
                }
            )
        }
    }
}

@Composable
private fun BankFilterTabs(
    selectedBank: String,
    onBankSelected: (String) -> Unit,
    banks: List<String>
) {
    ScrollableTabRow(
        selectedTabIndex = banks.indexOf(selectedBank)
    ) {
        banks.forEachIndexed { index, bank ->
            Tab(
                selected = selectedBank == bank,
                onClick = { onBankSelected(bank) },
                text = { Text(bank) }
            )
        }
    }
}

@Composable
private fun BankDocumentCard(
    bank: BankDocumentData,
    onViewDetails: (BankDocumentData) -> Unit,
    onEdit: (String) -> Unit
) {
    BentoBox {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bank.bankName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = bank.bankType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row {
                        if (bank.supportsAppraisalLpa) {
                            StatusChip(
                                status = "Appraisal/LPA",
                                color = Info
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        if (bank.supportsBpnClearance) {
                            StatusChip(
                                status = "BPN/Clearance",
                                color = Warning
                            )
                        }
                    }
                }
                
                Text(
                    text = "${bank.processingDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Document Summary
            Column {
                Text(
                    text = "Document Requirements:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                
                // Base Documents
                Text(
                    text = "Base Documents (${bank.baseDocCount}):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                bank.baseDocuments.take(5).forEach { doc ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Required",
                            tint = Success,
                            modifier = Modifier.size(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = doc,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                if (bank.baseDocCount > 5) {
                    Text(
                        text = "... and ${bank.baseDocCount - 5} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Conditional Documents
                if (bank.conditionalDocCount > 0) {
                    Text(
                        text = "Conditional Documents (${bank.conditionalDocCount}):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    bank.conditionalDocuments.take(3).forEach { doc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Conditional",
                                tint = Warning,
                                modifier = Modifier.size(12.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Text(
                                text = "${doc.name} (${doc.condition})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    if (bank.conditionalDocCount > 3) {
                        Text(
                            text = "... and ${bank.conditionalDocCount - 3} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Total Documents
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "Total",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "Total: ${bank.totalDocCount} documents",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onViewDetails(bank) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Details")
                }
                
                OutlinedButton(
                    onClick = { onEdit(bank.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
private fun ConditionalDocumentsDialog(
    bank: BankDocumentData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conditional Documents - ${bank.bankName}") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Base Documents (${bank.baseDocCount}):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    bank.baseDocuments.forEach { doc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Required",
                                tint = Success,
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = doc,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                if (bank.conditionalDocCount > 0) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Conditional Documents (${bank.conditionalDocCount}):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        bank.conditionalDocuments.forEach { doc ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Conditional",
                                            tint = Warning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        Text(
                                            text = doc.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Text(
                                        text = doc.condition,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                                    )
                                    
                                    Text(
                                        text = doc.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Processing Time: ${bank.processingDays} days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Total Documents Required: ${bank.totalDocCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
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

data class BankDocumentData(
    val id: String,
    val bankName: String,
    val bankType: String,
    val supportsAppraisalLpa: Boolean,
    val supportsBpnClearance: Boolean,
    val processingDays: Int,
    val baseDocuments: List<String>,
    val conditionalDocuments: List<ConditionalDocument>,
    val baseDocCount: Int,
    val conditionalDocCount: Int,
    val totalDocCount: Int
)

data class ConditionalDocument(
    val name: String,
    val condition: String,
    val description: String,
    val isRequired: Boolean
)

// =====================================================
// UI STATES
// =====================================================

sealed class DocumentRequirementsUiState {
    object Loading : DocumentRequirementsUiState()
    data class Success(val banks: List<BankDocumentData>) : DocumentRequirementsUiState()
    data class Error(val message: String) : DocumentRequirementsUiState()
}
