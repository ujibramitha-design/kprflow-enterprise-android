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
import com.kprflow.enterprise.ui.components.RateLimitStatisticsCard
import com.kprflow.enterprise.ui.components.TopViolatorsCard
import com.kprflow.enterprise.ui.components.RateLimitConfigCard
import com.kprflow.enterprise.ui.viewmodel.RateLimitingDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateLimitingDashboard(
    onBackClick: () -> Unit,
    viewModel: RateLimitingDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statisticsState by viewModel.statisticsState.collectAsState()
    val violatorsState by viewModel.violatorsState.collectAsState()
    val configsState by viewModel.configsState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadStatistics()
        viewModel.loadTopViolators()
        viewModel.loadConfigs()
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
                text = "Rate Limiting Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.refreshAllData() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
                
                Button(
                    onClick = { viewModel.showConfigDialog() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Config")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Dashboard Content
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Statistics
            item {
                when (statisticsState) {
                    is RateLimitState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    is RateLimitState.Success -> {
                        RateLimitStatisticsCard(
                            statistics = statisticsState.data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    is RateLimitState.Error -> {
                        ErrorCard(
                            message = statisticsState.message,
                            onRetry = { viewModel.loadStatistics() }
                        )
                    }
                }
            }
            
            // Rate Limit Configs
            item {
                when (configsState) {
                    is RateLimitState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    is RateLimitState.Success -> {
                        RateLimitConfigCard(
                            configs = configsState.data,
                            onEditConfig = { config ->
                                viewModel.editConfig(config)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    is RateLimitState.Error -> {
                        ErrorCard(
                            message = configsState.message,
                            onRetry = { viewModel.loadConfigs() }
                        )
                    }
                }
            }
            
            // Top Violators
            item {
                when (violatorsState) {
                    is RateLimitState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    is RateLimitState.Success -> {
                        TopViolatorsCard(
                            violators = violatorsState.data,
                            onBlockViolator = { identifier, limitType ->
                                viewModel.blockIdentifier(identifier, limitType)
                            },
                            onUnblockViolator = { identifier, limitType ->
                                viewModel.unblockIdentifier(identifier, limitType)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    is RateLimitState.Error -> {
                        ErrorCard(
                            message = violatorsState.message,
                            onRetry = { viewModel.loadTopViolators() }
                        )
                    }
                }
            }
        }
    }
    
    // Config Edit Dialog
    if (uiState.showConfigDialog) {
        ConfigEditDialog(
            config = uiState.selectedConfig,
            onDismiss = { viewModel.hideConfigDialog() },
            onSave = { config ->
                viewModel.saveConfig(config)
            }
        )
    }
    
    // Block Identifier Dialog
    if (uiState.showBlockDialog) {
        BlockIdentifierDialog(
            onDismiss = { viewModel.hideBlockDialog() },
            onBlock = { identifier, limitType, reason, duration ->
                viewModel.blockIdentifier(identifier, limitType, reason, duration)
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigEditDialog(
    config: com.kprflow.enterprise.data.repository.RateLimitConfig?,
    onDismiss: () -> Unit,
    onSave: (com.kprflow.enterprise.data.repository.RateLimitConfig) -> Unit
) {
    var maxRequests by remember { mutableStateOf(config?.maxRequests?.toString() ?: "100") }
    var windowMinutes by remember { mutableStateOf(config?.windowMinutes?.toString() ?: "1") }
    var blockDurationMinutes by remember { mutableStateOf(config?.blockDurationMinutes?.toString() ?: "60") }
    var isActive by remember { mutableStateOf(config?.isActive ?: true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Rate Limit Config")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (config != null) {
                    Text(
                        text = "Type: ${config.limitType}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                OutlinedTextField(
                    value = maxRequests,
                    onValueChange = { maxRequests = it },
                    label = { Text("Max Requests") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = windowMinutes,
                    onValueChange = { windowMinutes = it },
                    label = { Text("Window (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = blockDurationMinutes,
                    onValueChange = { blockDurationMinutes = it },
                    label = { Text("Block Duration (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Active")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newConfig = com.kprflow.enterprise.data.repository.RateLimitConfig(
                        limitType = config?.limitType ?: "CUSTOM",
                        maxRequests = maxRequests.toIntOrNull() ?: 100,
                        windowMinutes = windowMinutes.toIntOrNull() ?: 1,
                        blockDurationMinutes = blockDurationMinutes.toIntOrNull() ?: 60,
                        isActive = isActive
                    )
                    onSave(newConfig)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockIdentifierDialog(
    onDismiss: () -> Unit,
    onBlock: (String, String, String, Int) -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var limitType by remember { mutableStateOf("USER") }
    var reason by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    
    val limitTypes = listOf("USER", "IP", "API_KEY", "ENDPOINT")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Block Identifier")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Identifier") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Limit Type:")
                limitTypes.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = limitType == type,
                            onClick = { limitType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(type)
                    }
                }
                
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (identifier.isNotBlank()) {
                        onBlock(identifier, limitType, reason, duration.toIntOrNull() ?: 60)
                    }
                },
                enabled = identifier.isNotBlank()
            ) {
                Text("Block")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// UI States
sealed class RateLimitState<T> {
    object Loading : RateLimitState<Nothing>()
    data class Success<T>(val data: T) : RateLimitState<T>()
    data class Error(val message: String) : RateLimitState<Nothing>()
}

data class RateLimitingUiState(
    val showConfigDialog: Boolean = false,
    val showBlockDialog: Boolean = false,
    val selectedConfig: com.kprflow.enterprise.data.repository.RateLimitConfig? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
