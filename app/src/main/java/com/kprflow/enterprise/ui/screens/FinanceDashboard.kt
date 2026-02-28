package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.viewmodel.FinanceDashboardViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceDashboard(
    onDossierClick: (String) -> Unit,
    onPaymentApprovalClick: () -> Unit,
    onQuorumVotingClick: () -> Unit,
    onReportsClick: () -> Unit,
    viewModel: FinanceDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statisticsState by viewModel.statisticsState.collectAsState()
    val pendingPaymentsState by viewModel.pendingPaymentsState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadFinanceData()
    }
    
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID"))
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
            Text(
                text = "Finance Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                IconButton(onClick = onPaymentApprovalClick) {
                    Icon(Icons.Default.Payments, contentDescription = "Payment Approvals")
                }
                IconButton(onClick = onQuorumVotingClick) {
                    Icon(Icons.Default.HowToVote, contentDescription = "Quorum Voting")
                }
                IconButton(onClick = onReportsClick) {
                    Icon(Icons.Default.Assessment, contentDescription = "Reports")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Financial Statistics
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(200.dp)
        ) {
            item {
                FinanceStatsCard(
                    title = "Total Disbursed",
                    value = currencyFormatter.format(statisticsState.totalDisbursed),
                    change = "+15%",
                    isPositive = true,
                    icon = Icons.Default.AccountBalance,
                    onClick = { /* Navigate to disbursed loans */ }
                )
            }
            
            item {
                FinanceStatsCard(
                    title = "Pending Approvals",
                    value = statisticsState.pendingApprovals.toString(),
                    change = "+3",
                    isPositive = false,
                    icon = Icons.Default.PendingActions,
                    onClick = onPaymentApprovalClick
                )
            }
            
            item {
                FinanceStatsCard(
                    title = "Monthly Revenue",
                    value = currencyFormatter.format(statisticsState.monthlyRevenue),
                    change = "+8%",
                    isPositive = true,
                    icon = Icons.Default.TrendingUp,
                    onClick = onReportsClick
                )
            }
            
            item {
                FinanceStatsCard(
                    title = "Active Quorums",
                    value = statisticsState.activeQuorums.toString(),
                    change = "+1",
                    isPositive = true,
                    icon = Icons.Default.HowToVote,
                    onClick = onQuorumVotingClick
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Pending Payment Approvals
        Text(
            text = "Pending Payment Approvals",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        when (pendingPaymentsState) {
            is FinanceDashboardState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is FinanceDashboardState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(pendingPaymentsState.payments) { payment ->
                        FinancePaymentCard(
                            payment = payment,
                            onApprove = { viewModel.approvePayment(payment.id) },
                            onReject = { viewModel.rejectPayment(payment.id) },
                            onClick = { onDossierClick(payment.dossierId) }
                        )
                    }
                }
            }
            
            is FinanceDashboardState.Error -> {
                ErrorCard(
                    message = pendingPaymentsState.message,
                    onRetry = { viewModel.loadPendingPayments() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FinanceActionCard(
                    title = "Payment Approvals",
                    description = "Review pending payments",
                    icon = Icons.Default.Payments,
                    onClick = onPaymentApprovalClick
                )
            }
            
            item {
                FinanceActionCard(
                    title = "Quorum Voting",
                    description = "Participate in decisions",
                    icon = Icons.Default.HowToVote,
                    onClick = onQuorumVotingClick
                )
            }
            
            item {
                FinanceActionCard(
                    title = "Financial Reports",
                    description = "Generate reports",
                    icon = Icons.Default.Assessment,
                    onClick = onReportsClick
                )
            }
        }
    }
}

@Composable
private fun FinanceStatsCard(
    title: String,
    value: String,
    change: String,
    isPositive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = change,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPositive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FinancePaymentCard(
    payment: FinancePayment,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onClick: () -> Unit
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        text = payment.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Amount: ${currencyFormatter.format(payment.amount)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "Type: ${payment.paymentType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Requested: ${payment.requestedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Badge {
                        Text(payment.priority)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row {
                        IconButton(
                            onClick = onApprove,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Approve",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        IconButton(
                            onClick = onReject,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Reject",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Data classes
data class FinancePayment(
    val id: String,
    val dossierId: String,
    val customerName: String,
    val amount: BigDecimal,
    val paymentType: String,
    val priority: String,
    val requestedDate: String
)

// UI State
sealed class FinanceDashboardState {
    object Loading : FinanceDashboardState()
    data class Success(val payments: List<FinancePayment>) : FinanceDashboardState()
    data class Error(val message: String) : FinanceDashboardState()
}
