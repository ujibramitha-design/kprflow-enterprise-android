package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.viewmodel.FinanceViewModel
import com.kprflow.enterprise.util.Resource
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceBentoDashboard(
    userRole: String = "FINANCE",
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val financialSummary by viewModel.financialSummary.collectAsStateWithLifecycle()
    val pendingTransactions by viewModel.pendingTransactions.collectAsStateWithLifecycle()
    val transactionStatistics by viewModel.transactionStatistics.collectAsStateWithLifecycle()
    
    var showVerificationBottomSheet by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<com.kprflow.enterprise.data.model.FinancialTransaction?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadFinancialData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finance Dashboard")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        )
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Executive Summary Cards
            item {
                ExecutiveSummarySection(
                    financialSummary = financialSummary,
                    userRole = userRole
                )
            }
            
            // Cash Flow Overview
            item {
                CashFlowOverviewSection(
                    financialSummary = financialSummary
                )
            }
            
            // Transaction Statistics
            item {
                TransactionStatisticsSection(
                    statistics = transactionStatistics
                )
            }
            
            // Pending Transactions
            item {
                PendingTransactionsSection(
                    pendingTransactions = pendingTransactions,
                    onVerifyTransaction = { transaction ->
                        selectedTransaction = transaction
                        showVerificationBottomSheet = true
                    },
                    onRejectTransaction = { transaction ->
                        // Handle rejection
                    },
                    userRole = userRole
                )
            }
        }
    }
    
    // Verification BottomSheet
    if (showVerificationBottomSheet && selectedTransaction != null) {
        TransactionVerificationBottomSheet(
            transaction = selectedTransaction!!,
            onDismiss = { 
                showVerificationBottomSheet = false
                selectedTransaction = null
            },
            onVerify = { verificationNotes ->
                viewModel.verifyTransaction(
                    transactionId = selectedTransaction!!.id,
                    verificationNotes = verificationNotes
                )
                showVerificationBottomSheet = false
                selectedTransaction = null
            },
            onReject = { rejectionReason ->
                viewModel.rejectTransaction(
                    transactionId = selectedTransaction!!.id,
                    rejectionReason = rejectionReason
                )
                showVerificationBottomSheet = false
                selectedTransaction = null
            }
        )
    }
}

@Composable
private fun ExecutiveSummarySection(
    financialSummary: Resource<com.kprflow.enterprise.data.model.FinancialSummary>,
    userRole: String
) {
    when (financialSummary) {
        is Resource.Success -> {
            val summary = financialSummary.data
            
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Revenue Card - Elevated Emerald Style
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = com.kprflow.enterprise.ui.theme.Success.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Realized Cash",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = com.kprflow.enterprise.ui.theme.Success
                                )
                                
                                Text(
                                    text = "Total verified transactions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = com.kprflow.enterprise.ui.theme.Success,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Main amount with Plus Jakarta Sans
                        Text(
                            text = formatCurrency(summary.totalRealizedCash.toDouble()),
                            style = com.kprflow.enterprise.ui.theme.SLATypography.CountdownLarge,
                            color = com.kprflow.enterprise.ui.theme.Success,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Additional metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Avg per dossier",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(summary.avgRealizedCash.toDouble()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = com.kprflow.enterprise.ui.theme.Success
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Completion rate",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%.1f", summary.avgCompletionPercentage)}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = com.kprflow.enterprise.ui.theme.Success
                                )
                            }
                        }
                    }
                }
                
                // Secondary Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Projected Cash Card
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = formatCurrency(summary.totalProjectedCash.toDouble()),
                                style = com.kprflow.enterprise.ui.theme.SLATypography.CountdownSmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = "Projected Cash",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    // Pending Cash Card
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = com.kprflow.enterprise.ui.theme.Warning.copy(alpha = 0.1f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = com.kprflow.enterprise.ui.theme.Warning,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = formatCurrency(summary.totalPendingCash.toDouble()),
                                style = com.kprflow.enterprise.ui.theme.SLATypography.CountdownSmall,
                                color = com.kprflow.enterprise.ui.theme.Warning,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = "Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = com.kprflow.enterprise.ui.theme.Warning,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        is Resource.Loading -> {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Loading financial summary...")
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun CashFlowOverviewSection(
    financialSummary: Resource<com.kprflow.enterprise.data.model.FinancialSummary>
) {
    when (financialSummary) {
        is Resource.Success -> {
            val summary = financialSummary.data
            
            GlassCard {
                BentoHeader(
                    title = "Cash Flow Overview",
                    subtitle = "Transaction breakdown by type"
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Booking Cash
                    CashFlowItem(
                        label = "Booking Fees",
                        amount = summary.totalBookingCash.toDouble(),
                        icon = Icons.Default.Book,
                        color = com.kprflow.enterprise.ui.theme.Success,
                        percentage = if (summary.totalRealizedCash.toDouble() > 0) 
                            (summary.totalBookingCash.toDouble() / summary.totalRealizedCash.toDouble() * 100)
                        else 0.0
                    )
                    
                    // DP Cash
                    CashFlowItem(
                        label = "Down Payments",
                        amount = summary.totalDpCash.toDouble(),
                        icon = Icons.Default.AccountBalanceWallet,
                        color = MaterialTheme.colorScheme.primary,
                        percentage = if (summary.totalRealizedCash.toDouble() > 0) 
                            (summary.totalDpCash.toDouble() / summary.totalRealizedCash.toDouble() * 100)
                        else 0.0
                    )
                    
                    // Disbursement Cash
                    CashFlowItem(
                        label = "KPR Disbursements",
                        amount = summary.totalDisbursementCash.toDouble(),
                        icon = Icons.Default.Paid,
                        color = com.kprflow.enterprise.ui.theme.Warning,
                        percentage = if (summary.totalRealizedCash.toDouble() > 0) 
                            (summary.totalDisbursementCash.toDouble() / summary.totalRealizedCash.toDouble() * 100)
                        else 0.0
                    )
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun CashFlowItem(
    label: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    percentage: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = formatCurrency(amount),
                style = com.kprflow.enterprise.ui.theme.SLATypography.MetricValue,
                color = color
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${String.format("%.1f", percentage)}%",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun TransactionStatisticsSection(
    statistics: Resource<com.kprflow.enterprise.domain.usecase.financial.TransactionStatistics>
) {
    when (statistics) {
        is Resource.Success -> {
            val stats = statistics.data
            
            GlassCard {
                BentoHeader(
                    title = "Transaction Statistics",
                    subtitle = "Overview of all financial transactions"
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total",
                        value = stats.totalTransactions.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatCard(
                        title = "Pending",
                        value = stats.pendingTransactions.toString(),
                        color = com.kprflow.enterprise.ui.theme.Warning,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatCard(
                        title = "Verified",
                        value = stats.verifiedTransactions.toString(),
                        color = com.kprflow.enterprise.ui.theme.Success,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Average transaction value
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Average Transaction Value",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = formatCurrency(stats.averageTransactionAmount),
                            style = com.kprflow.enterprise.ui.theme.SLATypography.MetricValue,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun PendingTransactionsSection(
    pendingTransactions: Resource<List<com.kprflow.enterprise.data.model.FinancialTransaction>>,
    onVerifyTransaction: (com.kprflow.enterprise.data.model.FinancialTransaction) -> Unit,
    onRejectTransaction: (com.kprflow.enterprise.data.model.FinancialTransaction) -> Unit,
    userRole: String
) {
    when (pendingTransactions) {
        is Resource.Success -> {
            if (pendingTransactions.data.isNotEmpty()) {
                GlassCard {
                    BentoHeader(
                        title = "Pending Transactions",
                        subtitle = "${pendingTransactions.data.size} awaiting verification"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pendingTransactions.data.take(5).forEach { transaction ->
                            PendingTransactionCard(
                                transaction = transaction,
                                onVerify = { onVerifyTransaction(transaction) },
                                onReject = { onRejectTransaction(transaction) },
                                userRole = userRole
                            )
                        }
                        
                        if (pendingTransactions.data.size > 5) {
                            TextButton(
                                onClick = { /* View all pending */ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("View all ${pendingTransactions.data.size} pending transactions")
                            }
                        }
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun PendingTransactionCard(
    transaction: com.kprflow.enterprise.data.model.FinancialTransaction,
    onVerify: () -> Unit,
    onReject: () -> Unit,
    userRole: String
) {
    val canVerify = userRole == "FINANCE"
    
    Surface(
        color = com.kprflow.enterprise.ui.theme.Warning.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.type.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "ID: ${transaction.id.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = formatCurrency(transaction.nominal.toDouble()),
                    style = com.kprflow.enterprise.ui.theme.SLATypography.MetricValue,
                    color = com.kprflow.enterprise.ui.theme.Warning
                )
            }
            
            if (transaction.description?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            if (canVerify) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = com.kprflow.enterprise.ui.theme.Error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject")
                    }
                    
                    Button(
                        onClick = onVerify,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.kprflow.enterprise.ui.theme.Success
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verify")
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionVerificationBottomSheet(
    transaction: com.kprflow.enterprise.data.model.FinancialTransaction,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
    onReject: (String) -> Unit
) {
    var verificationNotes by remember { mutableStateOf("") }
    var rejectionReason by remember { mutableStateOf("") }
    var showRejectionDialog by remember { mutableStateOf(false) }
    
    BottomSheetScaffold(
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Verify Transaction",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Transaction Details
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailRow(
                            label = "Type",
                            value = transaction.type.name
                        )
                        
                        DetailRow(
                            label = "Amount",
                            value = formatCurrency(transaction.nominal.toDouble()),
                            valueColor = com.kprflow.enterprise.ui.theme.Success
                        )
                        
                        if (transaction.description?.isNotBlank() == true) {
                            DetailRow(
                                label = "Description",
                                value = transaction.description
                            )
                        }
                        
                        if (transaction.paymentMethod?.isNotBlank() == true) {
                            DetailRow(
                                label = "Payment Method",
                                value = transaction.paymentMethod
                            )
                        }
                        
                        DetailRow(
                            label = "Created",
                            value = transaction.createdAt
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Verification Notes
                Column {
                    Text(
                        text = "Verification Notes (Optional)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = verificationNotes,
                        onValueChange = { verificationNotes = it },
                        placeholder = { Text("Add any verification notes...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRejectionDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = com.kprflow.enterprise.ui.theme.Error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reject")
                    }
                    
                    Button(
                        onClick = { onVerify(verificationNotes) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.kprflow.enterprise.ui.theme.Success
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify")
                    }
                }
            }
        },
        onDismissRequest = onDismiss
    ) {
        // Main content (hidden when bottom sheet is open)
    }
    
    // Rejection Dialog
    if (showRejectionDialog) {
        AlertDialog(
            onDismissRequest = { showRejectionDialog = false },
            title = { Text("Reject Transaction") },
            text = {
                Column {
                    Text("Please provide a reason for rejection:")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        placeholder = { Text("Enter rejection reason...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(rejectionReason)
                        showRejectionDialog = false
                    },
                    enabled = rejectionReason.isNotBlank()
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount)
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = com.kprflow.enterprise.ui.theme.SLATypography.MetricValue,
                color = color
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
