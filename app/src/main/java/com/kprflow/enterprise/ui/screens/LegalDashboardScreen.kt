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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.viewmodel.LegalDocumentViewModel
import com.kprflow.enterprise.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDashboardScreen(
    userRole: String = "LEGAL",
    viewModel: LegalDocumentViewModel = hiltViewModel()
) {
    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val documentStatuses by viewModel.documentStatuses.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    
    var showSyncDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Legal Dashboard")
                    }
                },
                actions = {
                    if (userRole == "LEGAL") {
                        IconButton(onClick = { showSyncDialog = true }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync Documents")
                        }
                    }
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
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
                ExecutiveSummaryCards(
                    summary = dashboardSummary,
                    userRole = userRole
                )
            }
            
            // Sync Status Card
            item {
                SyncStatusCard(
                    syncStatus = syncStatus,
                    onTriggerSync = { 
                        viewModel.triggerDocumentSync()
                        showSyncDialog = false
                    },
                    userRole = userRole
                )
            }
            
            // Filter Chips
            item {
                FilterChips(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    userRole = userRole
                )
            }
            
            // Document Status List
            item {
                DocumentStatusList(
                    documentStatuses = documentStatuses,
                    filter = selectedFilter,
                    onViewDocument = { url -> /* Handle document view */ },
                    onSyncUnit = { unitId -> viewModel.syncUnit(unitId) },
                    userRole = userRole
                )
            }
        }
    }
    
    // Sync Confirmation Dialog
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Sync Legal Documents") },
            text = { 
                Text("This will trigger the automated document sync process from Google Drive. Continue?") 
            },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.triggerDocumentSync()
                        showSyncDialog = false
                    }
                ) {
                    Text("Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExecutiveSummaryCards(
    summary: Resource<com.kprflow.enterprise.data.model.LegalDashboardSummary>,
    userRole: String
) {
    when (summary) {
        is Resource.Success -> {
            val data = summary.data
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ready Units Card - Emerald Elevated
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = com.kprflow.enterprise.ui.theme.Success.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = com.kprflow.enterprise.ui.theme.Success,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = data.readyUnits.toString(),
                            style = com.kprflow.enterprise.ui.theme.SLATypography.CountdownSmall,
                            color = com.kprflow.enterprise.ui.theme.Success,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Ready Units",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.kprflow.enterprise.ui.theme.Success,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Partial Units Card - Warning Elevated
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = com.kprflow.enterprise.ui.theme.Warning.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = com.kprflow.enterprise.ui.theme.Warning,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = data.partialUnits.toString(),
                            style = com.kprflow.enterprise.ui.theme.SLATypography.CountdownSmall,
                            color = com.kprflow.enterprise.ui.theme.Warning,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Partial Units",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.kprflow.enterprise.ui.theme.Warning,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Incomplete Units Card - Error Elevated
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = com.kprflow.enterprise.ui.theme.Error.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = com.kprflow.enterprise.ui.theme.Error,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = data.incompleteUnits.toString(),
                            style = com.kprflow.enterprise.ui.theme.SLATypography.CountdownSmall,
                            color = com.kprflow.enterprise.ui.theme.Error,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Incomplete",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.kprflow.enterprise.ui.theme.Error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Document Coverage Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SHGB Coverage
                DocumentCoverageCard(
                    title = "SHGB",
                    percentage = data.shgbCoveragePercentage,
                    count = data.unitsWithSHGB,
                    color = com.kprflow.enterprise.ui.theme.Success,
                    modifier = Modifier.weight(1f)
                )
                
                // PBG Coverage
                DocumentCoverageCard(
                    title = "PBG",
                    percentage = data.pbgCoveragePercentage,
                    count = data.unitsWithPBG,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                // IMB Coverage
                DocumentCoverageCard(
                    title = "IMB",
                    percentage = data.imbCoveragePercentage,
                    count = data.unitsWithIMB,
                    color = com.kprflow.enterprise.ui.theme.Warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        is Resource.Loading -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun DocumentCoverageCard(
    title: String,
    percentage: Double,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${String.format("%.1f", percentage)}%",
                style = com.kprflow.enterprise.ui.theme.SLATypography.MetricValue,
                color = color
            )
            
            Text(
                text = "$count units",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun SyncStatusCard(
    syncStatus: Resource<com.kprflow.enterprise.data.model.DocumentSyncResult>,
    onTriggerSync: () -> Unit,
    userRole: String
) {
    when (syncStatus) {
        is Resource.Success -> {
            val result = syncStatus.data
            
            ElevatedCard(
                colors = CardDefaults.cardColors(
                    containerColor = if (result.success) 
                        com.kprflow.enterprise.ui.theme.Success.copy(alpha = 0.1f)
                    else 
                        com.kprflow.enterprise.ui.theme.Error.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (result.success) Icons.Default.Sync else Icons.Default.SyncProblem,
                                contentDescription = null,
                                tint = if (result.success) com.kprflow.enterprise.ui.theme.Success else com.kprflow.enterprise.ui.theme.Error,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Column {
                                Text(
                                    text = "Last Sync Status",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = result.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        if (userRole == "LEGAL") {
                            Button(
                                onClick = onTriggerSync,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = com.kprflow.enterprise.ui.theme.Success
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync")
                            }
                        }
                    }
                    
                    if (result.processed > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Documents Processed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Text(
                                    text = result.processed.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = com.kprflow.enterprise.ui.theme.Success
                                )
                            }
                        }
                    }
                    
                    if (result.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Surface(
                            color = com.kprflow.enterprise.ui.theme.Error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Sync Errors (${result.errors.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.kprflow.enterprise.ui.theme.Error,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                result.errors.take(2).forEach { error ->
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = com.kprflow.enterprise.ui.theme.Error
                                    )
                                }
                                
                                if (result.errors.size > 2) {
                                    Text(
                                        text = "... and ${result.errors.size - 2} more",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = com.kprflow.enterprise.ui.theme.Error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        is Resource.Loading -> {
            ElevatedCard(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Checking sync status...")
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun FilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    userRole: String
) {
    val filters = listOf("ALL", "READY", "PARTIAL", "INCOMPLETE")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) }
            )
        }
    }
}

@Composable
private fun DocumentStatusList(
    documentStatuses: Resource<List<com.kprflow.enterprise.data.model.LegalDocumentStatus>>,
    filter: String,
    onViewDocument: (String) -> Unit,
    onSyncUnit: (String) -> Unit,
    userRole: String
) {
    when (documentStatuses) {
        is Resource.Success -> {
            val filteredStatuses = when (filter) {
                "READY" -> documentStatuses.data.filter { it.legalReadinessStatus == com.kprflow.enterprise.data.model.LegalReadinessStatus.READY }
                "PARTIAL" -> documentStatuses.data.filter { it.legalReadinessStatus == com.kprflow.enterprise.data.model.LegalReadinessStatus.PARTIAL }
                "INCOMPLETE" -> documentStatuses.data.filter { it.legalReadinessStatus == com.kprflow.enterprise.data.model.LegalReadinessStatus.INCOMPLETE }
                else -> documentStatuses.data
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredStatuses.forEach { status ->
                    LegalDocumentStatusCard(
                        status = status,
                        onViewDocument = onViewDocument,
                        onSyncUnit = onSyncUnit,
                        userRole = userRole
                    )
                }
            }
        }
        is Resource.Loading -> {
            repeat(5) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun LegalDocumentStatusCard(
    status: com.kprflow.enterprise.data.model.LegalDocumentStatus,
    onViewDocument: (String) -> Unit,
    onSyncUnit: (String) -> Unit,
    userRole: String
) {
    val readinessColor = when (status.legalReadinessStatus) {
        com.kprflow.enterprise.data.model.LegalReadinessStatus.READY -> com.kprflow.enterprise.ui.theme.Success
        com.kprflow.enterprise.data.model.LegalReadinessStatus.PARTIAL -> com.kprflow.enterprise.ui.theme.Warning
        com.kprflow.enterprise.data.model.LegalReadinessStatus.INCOMPLETE -> com.kprflow.enterprise.ui.theme.Error
    }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = readinessColor.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with unit info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unit ${status.block} - ${status.unitNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = status.unitType ?: "Unknown Type",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Badge
                Surface(
                    color = readinessColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (status.legalReadinessStatus) {
                            com.kprflow.enterprise.data.model.LegalReadinessStatus.READY -> "SHGB READY"
                            com.kprflow.enterprise.data.model.LegalReadinessStatus.PARTIAL -> "PARTIAL"
                            com.kprflow.enterprise.data.model.LegalReadinessStatus.INCOMPLETE -> "INCOMPLETE"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = readinessColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Document Status Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // SHGB Status
                DocumentStatusIndicator(
                    title = "SHGB",
                    isAvailable = status.hasSHGB,
                    url = status.shgbUrl,
                    onViewDocument = onViewDocument,
                    modifier = Modifier.weight(1f)
                )
                
                // PBG Status
                DocumentStatusIndicator(
                    title = "PBG",
                    isAvailable = status.hasPBG,
                    url = status.pbgUrl,
                    onViewDocument = onViewDocument,
                    modifier = Modifier.weight(1f)
                )
                
                // IMB Status
                DocumentStatusIndicator(
                    title = "IMB",
                    isAvailable = status.hasIMB,
                    url = status.imbUrl,
                    onViewDocument = onViewDocument,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sync Info and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sync Status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = status.legalSyncStatus,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = when (status.legalSyncStatus) {
                            "SYNCED" -> com.kprflow.enterprise.ui.theme.Success
                            "ERROR" -> com.kprflow.enterprise.ui.theme.Error
                            else -> com.kprflow.enterprise.ui.theme.Warning
                        }
                    )
                }
                
                if (userRole == "LEGAL") {
                    OutlinedButton(
                        onClick = { onSyncUnit(status.unitId) },
                        modifier = Modifier.heightIn(min = 32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync")
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentStatusIndicator(
    title: String,
    isAvailable: Boolean,
    url: String?,
    onViewDocument: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isAvailable) com.kprflow.enterprise.ui.theme.Success else com.kprflow.enterprise.ui.theme.Error
    
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Circle,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            
            if (isAvailable && url != null) {
                Spacer(modifier = Modifier.height(4.dp))
                
                TextButton(
                    onClick = { onViewDocument(url) },
                    modifier = Modifier.heightIn(min = 24.dp)
                ) {
                    Text(
                        text = "View",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
