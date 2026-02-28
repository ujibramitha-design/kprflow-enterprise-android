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
import com.kprflow.enterprise.ui.viewmodel.PPJBDeveloperViewModel
import com.kprflow.enterprise.domain.model.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * PPJB Developer Screen - Akad Pengikatan Bawah Tangan
 * Phase 16: Legal & Documentation Automation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PPJBDeveloperScreen(
    viewModel: PPJBDeveloperViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadPPJBProcesses()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        HeaderSection(
            title = "PPJB Developer",
            subtitle = "Akad Pengikatan Bawah Tangan"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filter and Actions
        FilterAndActionsSection(
            onRefresh = { scope.launch { viewModel.refreshData() } },
            onNewPPJB = { scope.launch { viewModel.createPPJBProcess() } },
            onFilterChange = { viewModel.setFilter(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when (uiState.screenState) {
            is PPJBScreenState.Loading -> {
                LoadingSection()
            }
            is PPJBScreenState.Success -> {
                PPJBListSection(
                    processes = uiState.ppjbProcesses,
                    onProcessClick = { processId ->
                        /* Navigate to PPJB details */
                    },
                    onGenerateDocument = { processId ->
                        scope.launch { viewModel.generatePPJBDocument(processId) }
                    },
                    onSendInvitation = { processId ->
                        scope.launch { viewModel.sendInvitation(processId) }
                    },
                    onSendReminder = { processId ->
                        scope.launch { viewModel.sendReminder(processId) }
                    },
                    onCancel = { processId ->
                        scope.launch { viewModel.cancelPPJB(processId) }
                    }
                )
            }
            is PPJBScreenState.Error -> {
                ErrorSection(
                    error = uiState.screenState.error,
                    onRetry = { scope.launch { viewModel.loadPPJBProcesses() } }
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilterAndActionsSection(
    onRefresh: () -> Unit,
    onNewPPJB: () -> Unit,
    onFilterChange: (PPJBFilter) -> Unit
) {
    var filterType by remember { mutableStateOf(PPJBFilter.ALL) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PPJBFilterChip(
                text = "Semua",
                selected = filterType == PPJBFilter.ALL,
                onClick = {
                    filterType = PPJBFilter.ALL
                    onFilterChange(filterType)
                }
            )
            PPJBFilterChip(
                text = "Aktif",
                selected = filterType == PPJBFilter.ACTIVE,
                onClick = {
                    filterType = PPJBFilter.ACTIVE
                    onFilterChange(filterType)
                }
            )
            PPJBFilterChip(
                text = "Selesai",
                selected = filterType == PPJBFilter.COMPLETED,
                onClick = {
                    filterType = PPJBFilter.COMPLETED
                    onFilterChange(filterType)
                }
            )
            PPJBFilterChip(
                text = "Dibatalkan",
                selected = filterType == PPJBFilter.CANCELLED,
                onClick = {
                    filterType = PPJBFilter.CANCELLED
                    onFilterChange(filterType)
                }
            )
        }

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
            FloatingActionButton(
                onClick = onNewPPJB,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New PPJB",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun PPJBFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall
            )
        }
    )
}

@Composable
private fun LoadingSection() {
    Box(
        modifier = Modifier.fillMaxSize(),
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
                text = "Memuat data PPJB...",
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
private fun PPJBListSection(
    processes: List<PPJBDeveloperProcess>,
    onProcessClick: (String) -> Unit,
    onGenerateDocument: (String) -> Unit,
    onSendInvitation: (String) -> Unit,
    onSendReminder: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(processes) { process ->
            PPJBProcessCard(
                process = process,
                onClick = { onProcessClick(process.id) },
                onGenerateDocument = { onGenerateDocument(process.id) },
                onSendInvitation = { onSendInvitation(process.id) },
                onSendReminder = { onSendReminder(process.id) },
                onCancel = { onCancel(process.id) }
            )
        }
    }
}

@Composable
private fun PPJBProcessCard(
    process: PPJBDeveloperProcess,
    onClick: () -> Unit,
    onGenerateDocument: () -> Unit,
    onSendInvitation: () -> Unit,
    onSendReminder: () -> Unit,
    onCancel: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    val daysRemaining = calculateDaysRemaining(process.expiryDate)
    
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "PPJB ${process.ppjbType.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Unit: ${process.unitBlock}-${process.unitNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PPJBStatusBadge(status = process.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DetailItem(
                    label = "Jadwal",
                    value = dateFormat.format(process.scheduledDate)
                )
                DetailItem(
                    label = "Batas",
                    value = dateFormat.format(process.expiryDate)
                )
                DetailItem(
                    label = "Sisa",
                    value = "$daysRemaining hari"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            ProgressIndicator(
                progress = calculateProgress(process),
                label = "Progress: ${calculateProgress(process).toInt()}%"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            ActionButtonsRow(
                status = process.status,
                reminderCount = process.reminderCount,
                maxReminders = process.maxReminders,
                daysRemaining = daysRemaining,
                onGenerateDocument = onGenerateDocument,
                onSendInvitation = onSendInvitation,
                onSendReminder = onSendReminder,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PPJBStatusBadge(
    status: PPJBStatus
) {
    val (color, text) = when (status) {
        PPJBStatus.SCHEDULED -> MaterialTheme.colorScheme.primary to "Dijadwalkan"
        PPJBStatus.REMINDED -> MaterialTheme.colorScheme.secondary to "Diingatkan"
        PPJBStatus.WARNING_SENT -> MaterialTheme.colorScheme.tertiary to "Peringatan"
        PPJBStatus.COMPLETED -> MaterialTheme.colorScheme.primary to "Selesai"
        PPJBStatus.CANCELLED -> MaterialTheme.colorScheme.error to "Dibatalkan"
    }

    Badge(
        containerColor = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun ActionButtonsRow(
    status: PPJBStatus,
    reminderCount: Int,
    maxReminders: Int,
    daysRemaining: Int,
    onGenerateDocument: () -> Unit,
    onSendInvitation: () -> Unit,
    onSendReminder: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Generate Document Button
        OutlinedButton(
            onClick = onGenerateDocument,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Generate Document",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Dokumen")
        }

        // Send Invitation Button
        OutlinedButton(
            onClick = onSendInvitation,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Mail,
                contentDescription = "Send Invitation",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Undangan")
        }

        // Send Reminder Button (if applicable)
        if (status == PPJBStatus.SCHEDULED && reminderCount < maxReminders) {
            OutlinedButton(
                onClick = onSendReminder,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Send Reminder",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reminder")
            }
        }

        // Cancel Button (if applicable)
        if (status != PPJBStatus.COMPLETED && status != PPJBStatus.CANCELLED) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Cancel",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Batal")
            }
        }
    }
}

// Helper functions
private fun calculateDaysRemaining(expiryDate: Date): Int {
    val now = Date()
    return ((expiryDate.time - now.time) / (24 * 60 * 60 * 1000)).toInt()
}

private fun calculateProgress(process: PPJBDeveloperProcess): Float {
    return when (process.status) {
        PPJBStatus.SCHEDULED -> 0.2f
        PPJBStatus.REMINDED -> 0.4f
        PPJBStatus.WARNING_SENT -> 0.6f
        PPJBStatus.COMPLETED -> 1.0f
        PPJBStatus.CANCELLED -> 0.0f
    }
}

// Data classes for UI state
sealed class PPJBScreenState {
    object Loading : PPJBScreenState()
    data class Success(val ppjbProcesses: List<PPJBDeveloperProcess>) : PPJBScreenState()
    data class Error(val error: String) : PPJBScreenState()
}

enum class PPJBFilter {
    ALL, ACTIVE, COMPLETED, CANCELLED
}
