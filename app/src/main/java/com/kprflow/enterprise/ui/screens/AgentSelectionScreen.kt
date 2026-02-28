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
import com.kprflow.enterprise.ui.viewmodel.AgentSelectionViewModel
import com.kprflow.enterprise.ui.components.AccessibleButton
import com.kprflow.enterprise.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSelectionScreen(
    onBackClick: () -> Unit,
    onAgentSelected: (String) -> Unit,
    viewModel: AgentSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableAgents by viewModel.availableAgents.collectAsState()
    
    LaunchedEffect(Unit) {
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
                text = "Pilih Agent Anda",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = { viewModel.refreshAgents() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filter Options
        AgentFilterSection(
            selectedSpecialization = viewModel.selectedSpecialization,
            selectedLanguage = viewModel.selectedLanguage,
            onSpecializationChange = { viewModel.updateSpecialization(it) },
            onLanguageChange = { viewModel.updateLanguage(it) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Agent List
        when (uiState) {
            is AgentSelectionUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is AgentSelectionUiState.Success -> {
                if (availableAgents.isEmpty()) {
                    EmptyAgentState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(availableAgents) { agent ->
                            AgentCard(
                                agent = agent,
                                onSelect = { 
                                    viewModel.selectAgent(agent.id, "Customer selected agent")
                                    onAgentSelected(agent.id)
                                }
                            )
                        }
                    }
                }
            }
            
            is AgentSelectionUiState.Error -> {
                ErrorAgentState(
                    message = uiState.message,
                    onRetry = { viewModel.loadAvailableAgents() }
                )
            }
        }
    }
}

@Composable
private fun AgentFilterSection(
    selectedSpecialization: String?,
    selectedLanguage: String,
    onSpecializationChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filter Agent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Specialization Filter
            Text(
                text = "Spesialisasi",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSpecialization == null,
                    onClick = { onSpecializationChange(null) },
                    label = { Text("Semua") }
                )
                
                FilterChip(
                    selected = selectedSpecialization == "LEGAL",
                    onClick = { onSpecializationChange("LEGAL") },
                    label = { Text("Legal") }
                )
                
                FilterChip(
                    selected = selectedSpecialization == "MARKETING",
                    onClick = { onSpecializationChange("MARKETING") },
                    label = { Text("Marketing") }
                )
                
                FilterChip(
                    selected = selectedSpecialization == "SUPPORT",
                    onClick = { onSpecializationChange("SUPPORT") },
                    label = { Text("Support") }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Language Filter
            Text(
                text = "Bahasa",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedLanguage == "id",
                    onClick = { onLanguageChange("id") },
                    label = { Text("Indonesia") }
                )
                
                FilterChip(
                    selected = selectedLanguage == "en",
                    onClick = { onLanguageChange("en") },
                    label = { Text("English") }
                )
                
                FilterChip(
                    selected = selectedLanguage == "zh",
                    onClick = { onLanguageChange("zh") },
                    label = { Text("Chinese") }
                )
            }
        }
    }
}

@Composable
private fun AgentCard(
    agent: AgentInfo,
    onSelect: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Agent Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = agent.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Agent Code: ${agent.agentCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Availability Badge
                StatusBadge(
                    status = if (agent.isAvailable) "Tersedia" else "Tidak Tersedia",
                    color = if (agent.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Agent Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rating
                Column {
                    Text(
                        text = "Rating",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", agent.rating),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Specialization
                Column {
                    Text(
                        text = "Spesialisasi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = agent.specialization,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Tier
                Column {
                    Text(
                        text = "Tier",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = agent.tier,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Languages
            if (agent.languages.isNotEmpty()) {
                Text(
                    text = "Bahasa: ${agent.languages.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Expertise Areas
            if (agent.expertiseAreas.isNotEmpty()) {
                Text(
                    text = "Keahlian: ${agent.expertiseAreas.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Response Time
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
                    text = "Response Time: ${agent.responseTimeMinutes} menit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Current Load
            LinearProgressIndicator(
                progress = agent.currentCustomers.toFloat() / agent.maxCustomers,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "Beban: ${agent.currentCustomers}/${agent.maxCustomers} customer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Match Score
            if (agent.matchScore > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Kecocokan: ${agent.matchScore}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAgentState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PersonSearch,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Tidak Ada Agent Tersedia",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Coba ubah filter atau coba lagi nanti",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorAgentState(
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
data class AgentInfo(
    val id: String,
    val agentCode: String,
    val name: String,
    val email: String,
    val phone: String,
    val specialization: String,
    val tier: String,
    val rating: Double,
    val languages: List<String>,
    val expertiseAreas: List<String>,
    val responseTimeMinutes: Int,
    val currentCustomers: Int,
    val maxCustomers: Int,
    val isAvailable: Boolean,
    val matchScore: Int
)

sealed class AgentSelectionUiState {
    object Loading : AgentSelectionUiState()
    data class Success(val agents: List<AgentInfo>) : AgentSelectionUiState()
    data class Error(val message: String) : AgentSelectionUiState()
}
