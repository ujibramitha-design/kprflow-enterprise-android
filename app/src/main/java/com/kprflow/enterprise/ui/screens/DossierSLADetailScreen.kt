package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.viewmodel.DossierSLAViewModel
import com.kprflow.enterprise.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DossierSLADetailScreen(
    dossierId: String,
    onNavigateBack: () -> Unit,
    viewModel: DossierSLAViewModel = hiltViewModel()
) {
    val slaStatus by viewModel.slaStatus.collectAsStateWithLifecycle()
    val documentSlaStatus by viewModel.documentSlaStatus.collectAsStateWithLifecycle()
    val warningLevel by viewModel.warningLevel.collectAsStateWithLifecycle()
    val colorConfig by viewModel.colorConfig.collectAsStateWithLifecycle()
    
    LaunchedEffect(dossierId) {
        viewModel.loadSLAStatus(dossierId)
        viewModel.loadDocumentSLAStatus(dossierId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SLA Status Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshSLAStatus(dossierId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        )
    { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warning Banner
            item {
                warningLevel.let { level ->
                    when (level) {
                        is Resource.Success -> {
                            SLAWarningBanner(
                                warningLevel = level.data,
                                message = when (level.data) {
                                    com.kprflow.enterprise.domain.usecase.sla.SLAWarningLevel.NORMAL -> 
                                        "SLA status is normal. All deadlines are being met."
                                    com.kprflow.enterprise.domain.usecase.sla.SLAWarningLevel.WARNING -> 
                                        "Some deadlines are approaching. Please monitor closely."
                                    com.kprflow.enterprise.domain.usecase.sla.SLAWarningLevel.CRITICAL -> 
                                        "Critical deadlines approaching! Immediate attention required."
                                    com.kprflow.enterprise.domain.usecase.sla.SLAWarningLevel.OVERDUE -> 
                                        "Deadlines have been missed! Urgent action needed."
                                }
                            )
                        }
                        is Resource.Loading -> {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        else -> {}
                    }
                }
            }
            
            // Main SLA Status Card
            item {
                slaStatus.let { status ->
                    when (status) {
                        is Resource.Success -> {
                            SLAWarningCard(
                                slaStatus = status.data,
                                colorConfig = colorConfig.value,
                                customerName = status.data.customerName,
                                dossierStatus = status.data.dossierStatus
                            )
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
                                    Text("Loading SLA status...")
                                }
                            }
                        }
                        is Resource.Error -> {
                            BentoBox {
                                Text(
                                    text = "Error: ${status.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            
            // Document SLA Status
            item {
                documentSlaStatus.let { status ->
                    when (status) {
                        is Resource.Success -> {
                            BentoBox {
                                BentoHeader(
                                    title = "Document Collection SLA",
                                    subtitle = "14-day deadline for document submission"
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                SLAProgressBar(
                                    daysLeft = status.data.daysRemaining,
                                    totalDays = 14,
                                    label = "Document Collection",
                                    isOverdue = status.data.isOverdue,
                                    colorConfig = colorConfig.value
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Completion",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Text(
                                        text = "${status.data.completionPercentage}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colorConfig.value.primary
                                    )
                                }
                            }
                        }
                        is Resource.Loading -> {
                            BentoBox {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                        else -> {}
                    }
                }
            }
            
            // SLA Timeline
            item {
                SLATimelineSection(
                    slaStatus = slaStatus,
                    documentSlaStatus = documentSlaStatus,
                    colorConfig = colorConfig
                )
            }
            
            // Action Items
            item {
                SLAActionItemsSection(
                    slaStatus = slaStatus,
                    warningLevel = warningLevel
                )
            }
        }
    }
}

@Composable
private fun SLATimelineSection(
    slaStatus: Resource<com.kprflow.enterprise.domain.usecase.sla.SlaStatus>,
    documentSlaStatus: Resource<com.kprflow.enterprise.domain.usecase.sla.DocumentSlaStatus>,
    colorConfig: androidx.compose.ui.graphics.Color
) {
    BentoBox {
        BentoHeader(
            title = "SLA Timeline",
            subtitle = "Important deadlines and milestones"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Document Collection (Day 14)
            SLATimelineItem(
                title = "Document Collection",
                deadline = "Day 14",
                status = when (documentSlaStatus) {
                    is Resource.Success -> {
                        when {
                            documentSlaStatus.data.isOverdue -> "OVERDUE"
                            documentSlaStatus.data.isCritical -> "CRITICAL"
                            documentSlaStatus.data.isWarning -> "WARNING"
                            else -> "ON TRACK"
                        }
                    }
                    else -> "LOADING"
                },
                color = when (documentSlaStatus) {
                    is Resource.Success -> {
                        when {
                            documentSlaStatus.data.isOverdue -> Color(0xFFD32F2F)
                            documentSlaStatus.data.isCritical -> Color(0xFFFF6F00)
                            documentSlaStatus.data.isWarning -> Color(0xFFFFBF00)
                            else -> Color(0xFF004B87)
                        }
                    }
                    else -> Color.Gray
                }
            )
            
            // Bank Processing (Day 60)
            SLATimelineItem(
                title = "Bank Processing",
                deadline = "Day 60",
                status = when (slaStatus) {
                    is Resource.Success -> {
                        when {
                            slaStatus.data.isOverdue -> "OVERDUE"
                            slaStatus.data.isCritical -> "CRITICAL"
                            slaStatus.data.isWarning -> "WARNING"
                            else -> "ON TRACK"
                        }
                    }
                    else -> "LOADING"
                },
                color = when (slaStatus) {
                    is Resource.Success -> {
                        when {
                            slaStatus.data.isOverdue -> Color(0xFFD32F2F)
                            slaStatus.data.isCritical -> Color(0xFFFF6F00)
                            slaStatus.data.isWarning -> Color(0xFFFFBF00)
                            else -> Color(0xFF004B87)
                        }
                    }
                    else -> Color.Gray
                }
            )
        }
    }
}

@Composable
private fun SLATimelineItem(
    title: String,
    deadline: String,
    status: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = deadline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun SLAActionItemsSection(
    slaStatus: Resource<com.kprflow.enterprise.domain.usecase.sla.SlaStatus>,
    warningLevel: Resource<com.kprflow.enterprise.domain.usecase.sla.SLAWarningLevel>
) {
    BentoBox {
        BentoHeader(
            title = "Recommended Actions",
            subtitle = "Based on current SLA status"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val actions = when {
            slaStatus is Resource.Success && slaStatus.data.isOverdue -> {
                listOf(
                    "Contact customer immediately",
                    "Escalate to management",
                    "Document overdue reasons",
                    "Create recovery plan"
                )
            }
            slaStatus is Resource.Success && slaStatus.data.isCritical -> {
                listOf(
                    "Send reminder to customer",
                    "Schedule follow-up call",
                    "Check document status",
                    "Prepare escalation"
                )
            }
            slaStatus is Resource.Success && slaStatus.data.isWarning -> {
                listOf(
                    "Send friendly reminder",
                    "Monitor progress closely",
                    "Prepare required documents",
                    "Schedule next check-in"
                )
            }
            else -> {
                listOf(
                    "Maintain regular contact",
                    "Monitor progress",
                    "Prepare for next milestones",
                    "Document everything"
                )
            }
        }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            actions.forEach { action ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "•",
                        modifier = Modifier.width(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = action,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
