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
import com.kprflow.enterprise.ui.components.CustomerNotificationCard
import com.kprflow.enterprise.ui.components.PaymentSummaryCard
import com.kprflow.enterprise.ui.components.DocumentUploadCard
import com.kprflow.enterprise.ui.components.CustomerSupportCard
import com.kprflow.enterprise.ui.viewmodel.EnhancedCustomerDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedCustomerDashboard(
    onBackClick: () -> Unit,
    viewModel: EnhancedCustomerDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dossierState by viewModel.dossierState.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val notificationState by viewModel.notificationState.collectAsState()
    val unreadCount by viewModel.unreadCountState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadCustomerData()
        viewModel.loadPaymentSummary()
        viewModel.loadNotifications()
        viewModel.loadUnreadCount()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with notification badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My KPR Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                IconButton(
                    onClick = { viewModel.openNotifications() }
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else unreadCount.toString()
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                }
                
                IconButton(
                    onClick = { viewModel.openCustomerSupport() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Support,
                        contentDescription = "Customer Support"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Dashboard Content
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dossier Status
            item {
                when (dossierState) {
                    is CustomerDashboardState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    is CustomerDashboardState.Success -> {
                        dossierState.dossier?.let { dossier ->
                            CustomerDossierStatusCard(
                                dossier = dossier,
                                onUploadDocument = { documentType ->
                                    viewModel.uploadDocument(documentType)
                                }
                            )
                        }
                    }
                    
                    is CustomerDashboardState.Error -> {
                        ErrorCard(
                            message = dossierState.message,
                            onRetry = { viewModel.loadCustomerData() }
                        )
                    }
                }
            }
            
            // Payment Summary
            item {
                when (paymentState) {
                    is CustomerDashboardState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    is CustomerDashboardState.Success -> {
                        paymentState.summary?.let { summary ->
                            PaymentSummaryCard(
                                summary = summary,
                                onViewSchedule = { viewModel.viewPaymentSchedule() },
                                onMakePayment = { installmentId ->
                                    viewModel.makePayment(installmentId)
                                }
                            )
                        }
                    }
                    
                    is CustomerDashboardState.Error -> {
                        ErrorCard(
                            message = paymentState.message,
                            onRetry = { viewModel.loadPaymentSummary() }
                        )
                    }
                }
            }
            
            // Document Upload
            item {
                DocumentUploadCard(
                    onUploadDocument = { documentType, file ->
                        viewModel.uploadDocumentWithFile(documentType, file)
                    },
                    onTakePhoto = { documentType ->
                        viewModel.takePhotoForDocument(documentType)
                    }
                )
            }
            
            // Recent Notifications
            item {
                when (notificationState) {
                    is CustomerDashboardState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    is CustomerDashboardState.Success -> {
                        CustomerNotificationCard(
                            notifications = notificationState.notifications.take(3),
                            onViewAll = { viewModel.viewAllNotifications() },
                            onMarkAsRead = { notificationId ->
                                viewModel.markNotificationAsRead(notificationId)
                            }
                        )
                    }
                    
                    is CustomerDashboardState.Error -> {
                        ErrorCard(
                            message = notificationState.message,
                            onRetry = { viewModel.loadNotifications() }
                        )
                    }
                }
            }
            
            // Customer Support
            item {
                CustomerSupportCard(
                    onStartChat = { viewModel.startCustomerSupportChat() },
                    onCallSupport = { viewModel.callCustomerSupport() },
                    onEmailSupport = { viewModel.emailCustomerSupport() }
                )
            }
        }
    }
    
    // Notification Dialog
    if (uiState.showNotifications) {
        NotificationDialog(
            notifications = notificationState.notifications,
            unreadCount = unreadCount,
            onDismiss = { viewModel.closeNotifications() },
            onMarkAsRead = { notificationId ->
                viewModel.markNotificationAsRead(notificationId)
            },
            onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() }
        )
    }
    
    // Customer Support Dialog
    if (uiState.showCustomerSupport) {
        CustomerSupportDialog(
            onDismiss = { viewModel.closeCustomerSupport() },
            onStartChat = { viewModel.startCustomerSupportChat() },
            onCallSupport = { viewModel.callCustomerSupport() },
            onEmailSupport = { viewModel.emailCustomerSupport() }
        )
    }
}

@Composable
private fun CustomerDossierStatusCard(
    dossier: com.kprflow.enterprise.data.model.KprDossier,
    onUploadDocument: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Application Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status stepper
            com.kprflow.enterprise.ui.components.KprStepper(
                currentStatus = dossier.status
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Quick actions based on status
            when (dossier.status) {
                com.kprflow.enterprise.data.model.KprStatus.LEAD -> {
                    OutlinedButton(
                        onClick = { onUploadDocument("ID_CARD") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Upload Documents")
                    }
                }
                
                com.kprflow.enterprise.data.model.KprStatus.PEMBERKASAN -> {
                    Text(
                        text = "Documents are being verified",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                com.kprflow.enterprise.data.model.KprStatus.PROSES_BANK -> {
                    Text(
                        text = "Your application is being processed by the bank",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                com.kprflow.enterprise.data.model.KprStatus.PUTUSAN_KREDIT_ACC -> {
                    Text(
                        text = "🎉 Congratulations! Your loan has been approved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                com.kprflow.enterprise.data.model.KprStatus.SP3K_TERBIT -> {
                    Text(
                        text = "SP3K has been issued. Awaiting disbursement.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                com.kprflow.enterprise.data.model.KprStatus.FUNDS_DISBURSED -> {
                    Text(
                        text = "Funds have been disbursed. Awaiting BAST completion.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                com.kprflow.enterprise.data.model.KprStatus.BAST_COMPLETED -> {
                    Text(
                        text = "✅ Process completed successfully!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                else -> {
                    Text(
                        text = "Status: ${dossier.status.name.replace("_", " ")}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "❌ Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onRetry
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun NotificationDialog(
    notifications: List<com.kprflow.enterprise.data.repository.PushNotification>,
    unreadCount: Int,
    onDismiss: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notifications")
                
                if (unreadCount > 0) {
                    TextButton(
                        onClick = onMarkAllAsRead
                    ) {
                        Text("Mark all as read")
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onMarkAsRead = onMarkAsRead
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
private fun NotificationItem(
    notification: com.kprflow.enterprise.data.repository.PushNotification,
    onMarkAsRead: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.readAt == null) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = formatDate(notification.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (notification.readAt == null) {
                Spacer(modifier = Modifier.height(4.dp))
                
                TextButton(
                    onClick = { onMarkAsRead(notification.id) }
                ) {
                    Text("Mark as read")
                }
            }
        }
    }
}

@Composable
private fun CustomerSupportDialog(
    onDismiss: () -> Unit,
    onStartChat: () -> Unit,
    onCallSupport: () -> Unit,
    onEmailSupport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Customer Support")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "How can we help you today?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onStartChat,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Live Chat")
                    }
                    
                    OutlinedButton(
                        onClick = onCallSupport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Support")
                    }
                    
                    OutlinedButton(
                        onClick = onEmailSupport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Email")
                    }
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

// UI States
sealed class CustomerDashboardState<T> {
    object Loading : CustomerDashboardState<Nothing>()
    data class Success<T>(val data: T) : CustomerDashboardState<T>()
    data class Error(val message: String) : CustomerDashboardState<Nothing>()
}

data class CustomerDashboardUiState(
    val showNotifications: Boolean = false,
    val showCustomerSupport: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

private fun formatDate(dateString: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        val date = sdf.parse(dateString)
        val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
        outputFormat.format(date ?: java.util.Date())
    } catch (e: Exception) {
        "Unknown date"
    }
}
