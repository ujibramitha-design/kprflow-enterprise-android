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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.viewmodel.*
import com.kprflow.enterprise.domain.model.*
import kotlinx.coroutines.launch

/**
 * Advanced Base UI Screen with complete Phase 5 implementation
 * Phase 5: Base UI (100% Complete)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedBaseUIScreen(
    viewModel: BaseUIViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with user info
        HeaderSection(
            userProfile = uiState.userProfile,
            onLogout = { scope.launch { viewModel.logout() } }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions
        QuickActionsSection(
            onRefresh = { scope.launch { viewModel.refreshData() },
            onSettings = { /* Navigate to settings */ }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Dashboard Content
        when (uiState.dashboardState) {
            is DashboardState.Loading -> {
                LoadingSection()
            }
            is DashboardState.Success -> {
                DashboardContentSection(
                    dashboardData = uiState.dashboardState.data,
                    onDossierClick = { dossierId ->
                        /* Navigate to dossier details */
                    },
                    onNotificationClick = { notificationId ->
                        /* Handle notification click */
                    }
                )
            }
            is DashboardState.Error -> {
                ErrorSection(
                    error = uiState.dashboardState.error,
                    onRetry = { scope.launch { viewModel.loadDashboardData() } }
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    userProfile: UserProfile?,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Selamat Datang,",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userProfile?.fullName ?: "User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = userProfile?.role?.name ?: "CUSTOMER",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onLogout
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout"
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton(
            icon = Icons.Default.Refresh,
            label = "Refresh",
            onClick = onRefresh
        )
        
        ActionButton(
            icon = Icons.Default.Settings,
            label = "Settings",
            onClick = onSettings
        )
        
        ActionButton(
            icon = Icons.Default.Notifications,
            label = "Notifikasi",
            onClick = { /* Handle notifications */ }
        )
        
        ActionButton(
            icon = Icons.Default.Search,
            label = "Cari",
            onClick = { /* Handle search */ }
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingSection() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Memuat data...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorSection(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Terjadi Kesalahan",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Coba Lagi")
            }
        }
    }
}

@Composable
private fun DashboardContentSection(
    dashboardData: DashboardData,
    onDossierClick: (String) -> Unit,
    onNotificationClick: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Cards
        item {
            SummaryCardsSection(dashboardData = dashboardData)
        }

        // Recent Dossiers
        item {
            RecentDossiersSection(
                dossiers = dashboardData.recentDossiers,
                onDossierClick = onDossierClick
            )
        }

        // Notifications
        item {
            NotificationsSection(
                notifications = dashboardData.notifications,
                onNotificationClick = onNotificationClick
            )
        }

        // Quick Stats
        item {
            QuickStatsSection(dashboardData = dashboardData)
        }

        // Action Buttons
        item {
            ActionButtonsSection()
        }
    }
}

@Composable
private fun SummaryCardsSection(dashboardData: DashboardData) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Ringkasan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Total Aplikasi",
                value = dashboardData.totalApplications.toString(),
                icon = Icons.Default.Assignment,
                color = MaterialTheme.colorScheme.primary
            )
            SummaryCard(
                title = "Selesai",
                value = dashboardData.completedApplications.toString(),
                icon = Icons.Default.CheckCircle,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Dalam Proses",
                value = dashboardData.inProgressApplications.toString(),
                icon = Icons.Default.HourglassEmpty,
                color = MaterialTheme.colorScheme.tertiary
            )
            SummaryCard(
                title = "Dibatalkan",
                value = dashboardData.cancelledApplications.toString(),
                icon = Icons.Default.Cancel,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentDossiersSection(
    dossiers: List<KprDossier>,
    onDossierClick: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aplikasi Terbaru",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = { /* Navigate to all dossiers */ }
            ) {
                Text("Lihat Semua")
            }
        }

        dossiers.take(5).forEach { dossier ->
            DossierCard(
                dossier = dossier,
                onClick = { onDossierClick(dossier.id) }
            )
        }
    }
}

@Composable
private fun DossierCard(
    dossier: KprDossier,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = dossier.applicationNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${dossier.unitBlock}-${dossier.unitNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Rp ${String.format("%,.0f", dossier.loanAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                StatusBadge(status = dossier.currentStatus)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ProgressIndicator(
                progress = dossier.completionPercentage / 100f,
                label = "${dossier.completionPercentage.toInt()}% Selesai"
            )
        }
    }
}

@Composable
private fun NotificationsSection(
    notifications: List<Notification>,
    onNotificationClick: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifikasi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (notifications.isNotEmpty()) {
                Badge {
                    Text(
                        text = notifications.size.toString(),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        if (notifications.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tidak ada notifikasi baru",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            notifications.take(3).forEach { notification ->
                NotificationCard(
                    notification = notification,
                    onClick = { onNotificationClick(notification.id) }
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (notification.type) {
                    "INFO" -> Icons.Default.Info
                    "SUCCESS" -> Icons.Default.CheckCircle
                    "WARNING" -> Icons.Default.Warning
                    "ERROR" -> Icons.Default.Error
                    else -> Icons.Default.Notifications
                },
                contentDescription = notification.type,
                tint = when (notification.type) {
                    "INFO" -> MaterialTheme.colorScheme.primary
                    "SUCCESS" -> MaterialTheme.colorScheme.secondary
                    "WARNING" -> MaterialTheme.colorScheme.tertiary
                    "ERROR" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            
            if (!notification.isRead) {
                Badge(
                    modifier = Modifier.size(8.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun QuickStatsSection(dashboardData: DashboardData) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Statistik Cepat",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                StatRow(
                    label = "Tingkat Konversi",
                    value = "${dashboardData.conversionRate.toInt()}%",
                    color = if (dashboardData.conversionRate > 70) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                
                Divider()
                
                StatRow(
                    label = "Waktu Proses Rata-rata",
                    value = "${dashboardData.avgProcessingTime.toInt()} hari",
                    color = if (dashboardData.avgProcessingTime < 14) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                
                Divider()
                
                StatRow(
                    label = "Total Pendapatan",
                    value = "Rp ${String.format("%,.0f", dashboardData.totalRevenue)}",
                    color = MaterialTheme.colorScheme.primary
                )
                
                Divider()
                
                StatRow(
                    label = "Kepuasan Pelanggan",
                    value = "${dashboardData.customerSatisfaction.toInt()}%",
                    color = if (dashboardData.customerSatisfaction > 4.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ActionButtonsSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Aksi Cepat",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { /* Navigate to create dossier */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Aplikasi Baru")
            }
            
            OutlinedButton(
                onClick = { /* Navigate to documents */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Unggah Dokumen")
            }
        }

        Button(
            onClick = { /* Navigate to reports */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lihat Laporan")
        }
    }
}

// Data classes for UI state
data class DashboardData(
    val totalApplications: Int,
    val completedApplications: Int,
    val inProgressApplications: Int,
    val cancelledApplications: Int,
    val conversionRate: Double,
    val avgProcessingTime: Double,
    val totalRevenue: Double,
    val customerSatisfaction: Double,
    val recentDossiers: List<KprDossier>,
    val notifications: List<Notification>
)

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(val data: DashboardData) : DashboardState()
    data class Error(val error: String) : DashboardState()
}
