package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.viewmodel.LegalAgentManagementViewModel
import com.kprflow.enterprise.ui.components.AccessibleButton
import com.kprflow.enterprise.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalAgentManagementScreen(
    onBackClick: () -> Unit,
    viewModel: LegalAgentManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val availableAgents by viewModel.availableAgents.collectAsState()
    val showAssignDialog by viewModel.showAssignDialog.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadAssignments()
        viewModel.loadAvailableAgents()
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
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            
            Text(
                text = "Manajemen Agent",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = { viewModel.openAssignDialog() }
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Assign Agent")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filter Tabs
        FilterTabs(
            selectedFilter = viewModel.selectedFilter,
            onFilterChange = { viewModel.updateFilter(it) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content
        when (uiState) {
            is LegalAgentManagementUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is LegalAgentManagementUiState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(assignments) { assignment ->
                        AssignmentCard(
                            assignment = assignment,
                            onEdit = { viewModel.editAssignment(assignment) },
                            onReassign = { viewModel.reassignAgent(assignment) },
                            onRemove = { viewModel.removeAssignment(assignment) }
                        )
                    }
                }
            }
            
            is LegalAgentManagementUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = { viewModel.loadAssignments() }
                )
            }
        }
    }
    
    // Assign Agent Dialog
    if (showAssignDialog) {
        AssignAgentDialog(
            availableAgents = availableAgents,
            onDismiss = { viewModel.closeAssignDialog() },
            onAssign = { customerId, agentId, reason, priority, isTemporary, temporaryUntil ->
                viewModel.assignAgent(customerId, agentId, reason, priority, isTemporary, temporaryUntil)
            }
        )
    }
}

@Composable
private fun FilterTabs(
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = when (selectedFilter) {
            "ALL" -> 0
            "ACTIVE" -> 1
            "TEMPORARY" -> 2
            "INACTIVE" -> 3
            else -> 0
        }
    ) {
        Tab(
            selected = selectedFilter == "ALL",
            onClick = { onFilterChange("ALL") },
            text = { Text("Semua") }
        )
        
        Tab(
            selected = selectedFilter == "ACTIVE",
            onClick = { onFilterChange("ACTIVE") },
            text = { Text("Aktif") }
        )
        
        Tab(
            selected = selectedFilter == "TEMPORARY",
            onClick = { onFilterChange("TEMPORARY") },
            text = { Text("Sementara") }
        )
        
        Tab(
            selected = selectedFilter == "INACTIVE",
            onClick = { onFilterChange("INACTIVE") },
            text = { Text("Tidak Aktif") }
        )
    }
}

@Composable
private fun AssignmentCard(
    assignment: AgentAssignment,
    onEdit: () -> Unit,
    onReassign: () -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = assignment.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Customer ID: ${assignment.customerId.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Badge
                StatusBadge(
                    status = when (assignment.status) {
                        "ACTIVE" -> if (assignment.isTemporary) "Sementara" else "Aktif"
                        "INACTIVE" -> "Tidak Aktif"
                        "REPLACED" -> "Diganti"
                        else -> assignment.status
                    },
                    color = when (assignment.status) {
                        "ACTIVE" -> MaterialTheme.colorScheme.primary
                        "TEMPORARY" -> MaterialTheme.colorScheme.secondary
                        "INACTIVE" -> MaterialTheme.colorScheme.error
                        "REPLACED" -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Agent Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Agent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${assignment.agentName} (${assignment.agentCode})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = "Prioritas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusBadge(
                        status = assignment.priorityLevel,
                        color = when (assignment.priorityLevel) {
                            "URGENT" -> MaterialTheme.colorScheme.error
                            "HIGH" -> MaterialTheme.colorScheme.secondary
                            "NORMAL" -> MaterialTheme.colorScheme.primary
                            "LOW" -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Assignment Details
            Text(
                text = "Alasan: ${assignment.assignmentReason}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (assignment.isTemporary && assignment.temporaryUntil != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Berlaku sampai: ${formatDate(assignment.temporaryUntil)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Ditugaskan oleh: ${assignment.assignedByName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Tanggal: ${formatDate(assignment.assignedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                
                OutlinedButton(
                    onClick = onReassign,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reassign")
                }
                
                Button(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus")
                }
            }
        }
    }
}

@Composable
private fun AssignAgentDialog(
    availableAgents: List<AgentInfo>,
    onDismiss: () -> Unit,
    onAssign: (String, String, String, String, Boolean, String?) -> Unit
) {
    var selectedCustomerId by remember { mutableStateOf("") }
    var selectedAgentId by remember { mutableStateOf("") }
    var assignmentReason by remember { mutableStateOf("") }
    var priorityLevel by remember { mutableStateOf("NORMAL") }
    var isTemporary by remember { mutableStateOf(false) }
    var temporaryUntil by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Tugaskan Agent")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Customer Selection (simplified - in real app would have customer search)
                OutlinedTextField(
                    value = selectedCustomerId,
                    onValueChange = { selectedCustomerId = it },
                    label = { Text("Customer ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Agent Selection
                if (availableAgents.isNotEmpty()) {
                    Text(
                        text = "Pilih Agent:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableAgents) { agent ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAgentId == agent.id,
                                    onClick = { selectedAgentId = agent.id }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = agent.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${agent.agentCode} - ${agent.specialization}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Assignment Reason
                OutlinedTextField(
                    value = assignmentReason,
                    onValueChange = { assignmentReason = it },
                    label = { Text("Alasan Penugasan") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Priority Level
                Text(
                    text = "Prioritas:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = priorityLevel == "LOW",
                        onClick = { priorityLevel = "LOW" },
                        label = { Text("Rendah") }
                    )
                    
                    FilterChip(
                        selected = priorityLevel == "NORMAL",
                        onClick = { priorityLevel = "NORMAL" },
                        label = { Text("Normal") }
                    )
                    
                    FilterChip(
                        selected = priorityLevel == "HIGH",
                        onClick = { priorityLevel = "HIGH" },
                        label = { Text("Tinggi") }
                    )
                    
                    FilterChip(
                        selected = priorityLevel == "URGENT",
                        onClick = { priorityLevel = "URGENT" },
                        label = { Text("Urgent") }
                    )
                }
                
                // Temporary Assignment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isTemporary,
                        onCheckedChange = { isTemporary = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Penugasan Sementara")
                }
                
                if (isTemporary) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = temporaryUntil,
                        onValueChange = { temporaryUntil = it },
                        label = { Text("Berlaku Sampai (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedCustomerId.isNotBlank() && selectedAgentId.isNotBlank() && assignmentReason.isNotBlank()) {
                        onAssign(
                            selectedCustomerId,
                            selectedAgentId,
                            assignmentReason,
                            priorityLevel,
                            isTemporary,
                            if (isTemporary && temporaryUntil.isNotBlank()) temporaryUntil else null
                        )
                    }
                },
                enabled = selectedCustomerId.isNotBlank() && 
                         selectedAgentId.isNotBlank() && 
                         assignmentReason.isNotBlank()
            ) {
                Text("Tugaskan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Terjadi Kesalahan",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AccessibleButton(
            text = "Coba Lagi",
            onClick = onRetry
        )
    }
}

// Data Models
data class AgentAssignment(
    val id: String,
    val customerId: String,
    val customerName: String,
    val customerEmail: String,
    val agentId: String,
    val agentCode: String,
    val agentName: String,
    val agentEmail: String,
    val assignedBy: String,
    val assignedByName: String,
    val assignmentReason: String,
    val priorityLevel: String,
    val isTemporary: Boolean,
    val temporaryUntil: String?,
    val status: String,
    val assignedAt: String
)

sealed class LegalAgentManagementUiState {
    object Loading : LegalAgentManagementUiState()
    data class Success(val assignments: List<AgentAssignment>) : LegalAgentManagementUiState()
    data class Error(val message: String) : LegalAgentManagementUiState()
}

private fun formatDate(dateString: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        val date = sdf.parse(dateString)
        val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        outputFormat.format(date ?: java.util.Date())
    } catch (e: Exception) {
        dateString
    }
}
