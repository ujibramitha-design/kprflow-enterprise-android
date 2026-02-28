package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kprflow.enterprise.ui.components.BentoBox
import com.kprflow.enterprise.ui.components.BentoHeader
import com.kprflow.enterprise.ui.components.BentoStatsCard
import com.kprflow.enterprise.ui.components.AccessibleButton
import com.kprflow.enterprise.ui.viewmodel.WhatsAppSPRViewModel
import com.kprflow.enterprise.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppSPRSyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: WhatsAppSPRViewModel = hiltViewModel()
) {
    val processState by viewModel.processState.collectAsStateWithLifecycle()
    val newMessages by viewModel.newMessages.collectAsStateWithLifecycle()
    val activationState by viewModel.activationState.collectAsStateWithLifecycle()
    
    var groupId by remember { mutableStateOf("KPRFlow_Group_001") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Whatsapp,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp SPR Sync")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Refresh, contentDescription = "Back")
                    }
                }
            )
        )
    { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group Configuration
            BentoBox {
                BentoHeader(
                    title = "WhatsApp Group Configuration",
                    subtitle = "Connect to WhatsApp group for SPR auto-sync"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = groupId,
                    onValueChange = { groupId = it },
                    label = { Text("Group ID") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter WhatsApp Group ID") }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccessibleButton(
                        text = "Process Messages",
                        onClick = { viewModel.processWhatsAppGroup(groupId) },
                        modifier = Modifier.weight(1f),
                        enabled = groupId.isNotBlank()
                    )
                    
                    AccessibleButton(
                        text = "Start Monitoring",
                        onClick = { viewModel.startMonitoring(groupId) },
                        modifier = Modifier.weight(1f),
                        enabled = groupId.isNotBlank()
                    )
                }
            }
            
            // Processing Status
            when (processState) {
                is Resource.Loading -> {
                    BentoBox {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Processing WhatsApp messages...")
                        }
                    }
                }
                
                is Resource.Success -> {
                    BentoStatsCard(
                        title = "SPR Processed",
                        value = "${processState.data.size}",
                        subtitle = "New SPRs created from WhatsApp",
                        icon = Icons.Default.Whatsapp,
                        onClick = { viewModel.clearProcessState() }
                    )
                }
                
                is Resource.Error -> {
                    BentoBox {
                        Text(
                            text = "Error: ${processState.message}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        AccessibleButton(
                            text = "Retry",
                            onClick = { viewModel.processWhatsAppGroup(groupId) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                else -> {}
            }
            
            // Recent Messages
            if (newMessages.isNotEmpty()) {
                BentoBox {
                    BentoHeader(
                        title = "Recent WhatsApp Messages",
                        subtitle = "${newMessages.size} new messages detected"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(newMessages.take(10)) { message ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = message.senderName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                    
                                    Text(
                                        text = message.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Instructions
            BentoBox {
                BentoHeader(
                    title = "How It Works",
                    subtitle = "Automatic SPR processing from WhatsApp"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("• Connect to WhatsApp group containing customer inquiries", style = MaterialTheme.typography.bodyMedium)
                    Text("• System automatically detects SPR-related messages", style = MaterialTheme.typography.bodyMedium)
                    Text("• Extracts customer info and creates INACTIVE SPR", style = MaterialTheme.typography.bodyMedium)
                    Text("• Sends confirmation message to customer", style = MaterialTheme.typography.bodyMedium)
                    Text("• Marketing team can verify and activate SPR", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
