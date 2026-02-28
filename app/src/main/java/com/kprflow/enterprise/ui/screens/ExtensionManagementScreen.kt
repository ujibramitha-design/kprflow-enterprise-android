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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.viewmodel.ExtensionViewModel
import com.kprflow.enterprise.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionManagementScreen(
    userRole: String = "LEGAL",
    viewModel: ExtensionViewModel = hiltViewModel()
) {
    val pendingExtensions by viewModel.pendingExtensions.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val requestState by viewModel.requestState.collectAsStateWithLifecycle()
    val approvalState by viewModel.approvalState.collectAsStateWithLifecycle()
    
    var showRequestDialog by remember { mutableStateOf(false) }
    var selectedDossierId by remember { mutableStateOf("") }
    var selectedCustomerName by remember { mutableStateOf("") }
    var selectedDeadline by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.loadPendingExtensions()
        viewModel.loadStatistics()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extension Management")
                    }
                },
                actions = {
                    if (userRole == "LEGAL") {
                        IconButton(onClick = { showRequestDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Request Extension")
                        }
                    }
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Statistics Section
            item {
                ExtensionStatisticsSection(
                    statistics = statistics,
                    userRole = userRole
                )
            }
            
            // Role-based Information
            item {
                RoleInformationCard(userRole = userRole)
            }
            
            // Pending Extensions
            item {
                when (pendingExtensions) {
                    is Resource.Success -> {
                        if (pendingExtensions.data.isNotEmpty()) {
                            BentoBox {
                                BentoHeader(
                                    title = "Pending Extensions",
                                    subtitle = "${pendingExtensions.data.size} requests awaiting approval"
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    pendingExtensions.data.forEach { extension ->
                                        PendingExtensionCard(
                                            extension = extension,
                                            onApprove = { extensionId, notes ->
                                                viewModel.approveExtension(
                                                    extensionId = extensionId,
                                                    approvedBy = userRole,
                                                    approvalNotes = notes
                                                )
                                            },
                                            onReject = { extensionId, reason ->
                                                viewModel.rejectExtension(
                                                    extensionId = extensionId,
                                                    rejectedBy = userRole,
                                                    rejectionReason = reason
                                                )
                                            },
                                            userRole = userRole
                                        )
                                    }
                                }
                            }
                        } else {
                            BentoBox {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Success,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Text(
                                        text = "No Pending Extensions",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    Text(
                                        text = "All extension requests have been processed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    is Resource.Loading -> {
                        BentoBox {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Loading pending extensions...")
                            }
                        }
                    }
                    is Resource.Error -> {
                        ErrorCard(
                            message = pendingExtensions.message ?: "Unknown error",
                            onRetry = { viewModel.loadPendingExtensions() }
                        )
                    }
                }
            }
        }
    }
    
    // Extension Request Dialog
    ExtensionRequestDialog(
        isVisible = showRequestDialog,
        customerName = selectedCustomerName,
        currentDeadline = selectedDeadline,
        extensionDays = 30,
        extensionReason = "",
        onReasonChange = { /* Handle reason change */ },
        onDaysChange = { /* Handle days change */ },
        onRequest = {
            showRequestDialog = false
            // Handle request
        },
        onDismiss = { showRequestDialog = false },
        validation = com.kprflow.enterprise.data.model.ExtensionValidation(
            canExtend = true,
            reason = "Ready to request",
            remainingExtensions = 2,
            maxExtensions = 3
        )
    )
}

@Composable
private fun ExtensionStatisticsSection(
    statistics: Resource<com.kprflow.enterprise.data.model.ExtensionStatistics>,
    userRole: String
) {
    when (statistics) {
        is Resource.Success -> {
            val stats = statistics.data
            
            BentoBox {
                BentoHeader(
                    title = "Extension Statistics",
                    subtitle = "Overview of extension requests and approvals"
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Main Statistics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Requests",
                        value = stats.totalRequests.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatCard(
                        title = "Pending",
                        value = stats.pendingRequests.toString(),
                        color = com.kprflow.enterprise.ui.theme.Warning,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatCard(
                        title = "Approved",
                        value = stats.approvedRequests.toString(),
                        color = com.kprflow.enterprise.ui.theme.Success,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Additional Statistics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Rejected",
                        value = stats.rejectedRequests.toString(),
                        color = com.kprflow.enterprise.ui.theme.Error,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatCard(
                        title = "Avg Days",
                        value = String.format("%.1f", stats.averageExtensionDays),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatCard(
                        title = "Approval Rate",
                        value = if (stats.totalRequests > 0) {
                            "${((stats.approvedRequests.toFloat() / stats.totalRequests) * 100).toInt()}%"
                        } else "0%",
                        color = com.kprflow.enterprise.ui.theme.Success,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Most Common Reason
                if (stats.mostCommonReason.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Most Common Reason",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = stats.mostCommonReason,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
        is Resource.Loading -> {
            BentoBox {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Loading statistics...")
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        backgroundColor = color.copy(alpha = 0.1f),
        borderColor = color.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
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

@Composable
private fun RoleInformationCard(userRole: String) {
    val roleInfo = when (userRole) {
        "LEGAL" -> RoleInfo(
            title = "Legal Team Permissions",
            description = "You can request and approve extensions for dossiers.",
            permissions = listOf(
                "Request dossier extensions",
                "Approve pending extensions",
                "Reject extension requests",
                "View extension history"
            ),
            color = com.kprflow.enterprise.ui.theme.Success
        )
        "BOD" -> RoleInfo(
            title = "Board of Directors Permissions",
            description = "You can approve extensions and view all extension data.",
            permissions = listOf(
                "Approve pending extensions",
                "Reject extension requests",
                "View extension statistics",
                "Monitor extension trends"
            ),
            color = MaterialTheme.colorScheme.primary
        )
        else -> RoleInfo(
            title = "View Only Permissions",
            description = "You can view extension status but cannot make changes.",
            permissions = listOf(
                "View pending extensions",
                "View extension history",
                "View statistics"
            ),
            color = com.kprflow.enterprise.ui.theme.Warning
        )
    }
    
    GlassCard(
        backgroundColor = roleInfo.color.copy(alpha = 0.1f),
        borderColor = roleInfo.color.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = roleInfo.color,
                    modifier = Modifier.size(24.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = roleInfo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = roleInfo.color
                    )
                    
                    Text(
                        text = roleInfo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                roleInfo.permissions.forEach { permission ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = roleInfo.color,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Text(
                            text = permission,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private data class RoleInfo(
    val title: String,
    val description: String,
    val permissions: List<String>,
    val color: androidx.compose.ui.graphics.Color
)
