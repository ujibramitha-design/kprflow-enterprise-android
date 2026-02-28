package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.kprflow.enterprise.ui.viewmodel.PreAkadVerificationViewModel
import com.kprflow.enterprise.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreAkadVerificationScreen(
    dossierId: String,
    userRole: String = "FINANCE",
    viewModel: PreAkadVerificationViewModel = hiltViewModel()
) {
    val akadReadiness by viewModel.akadReadiness.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val finalizeState by viewModel.finalizeState.collectAsStateWithLifecycle()
    
    var showFinalizeDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(dossierId) {
        viewModel.loadAkadReadiness(dossierId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pre-Akad Verification")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (akadReadiness) {
                is Resource.Success -> {
                    val readiness = akadReadiness.data
                    
                    // Overall Status Card
                    OverallStatusCard(
                        readiness = readiness,
                        userRole = userRole,
                        onFinalizeAkad = { 
                            showFinalizeDialog = true 
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress Overview
                    ProgressOverviewCard(readiness = readiness)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Department Verification Sections
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Finance Section
                        FinanceVerificationSection(
                            readiness = readiness,
                            userRole = userRole,
                            onUpdateVerification = { type, isCompleted, notes, url, amount ->
                                viewModel.updateVerification(
                                    dossierId = dossierId,
                                    verificationType = type,
                                    isCompleted = isCompleted,
                                    notes = notes,
                                    documentUrl = url,
                                    amount = amount
                                )
                            }
                        )
                        
                        // Legal Section
                        LegalVerificationSection(
                            readiness = readiness,
                            userRole = userRole,
                            onUpdateVerification = { type, isCompleted, notes, url ->
                                viewModel.updateVerification(
                                    dossierId = dossierId,
                                    verificationType = type,
                                    isCompleted = isCompleted,
                                    notes = notes,
                                    documentUrl = url,
                                    amount = null
                                )
                            }
                        )
                        
                        // Marketing Section
                        MarketingVerificationSection(
                            readiness = readiness,
                            userRole = userRole,
                            onUpdateVerification = { type, isCompleted, notes, url ->
                                viewModel.updateVerification(
                                    dossierId = dossierId,
                                    verificationType = type,
                                    isCompleted = isCompleted,
                                    notes = notes,
                                    documentUrl = url,
                                    amount = null
                                )
                            }
                        )
                    }
                }
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {}
            }
        }
    }
    
    // Finalize Akad Dialog
    if (showFinalizeDialog) {
        FinalizeAkadDialog(
            readiness = akadReadiness.data,
            onDismiss = { showFinalizeDialog = false },
            onFinalize = { scheduledDate, notes ->
                viewModel.finalizeAkadSchedule(
                    dossierId = dossierId,
                    scheduledDate = scheduledDate,
                    notes = notes
                )
                showFinalizeDialog = false
            },
            userRole = userRole
        )
    }
}

@Composable
private fun OverallStatusCard(
    readiness: com.kprflow.enterprise.data.model.AkadReadiness,
    userRole: String,
    onFinalizeAkad: () -> Unit
) {
    val statusColor = when {
        readiness.isReadyForAkad -> com.kprflow.enterprise.ui.theme.Success
        readiness.completionPercentage >= 50 -> com.kprflow.enterprise.ui.theme.Warning
        else -> com.kprflow.enterprise.ui.theme.Error
    }
    
    val statusText = when {
        readiness.isReadyForAkad -> "Ready for Akad"
        readiness.completionPercentage >= 50 -> "In Progress"
        else -> "Pending Verification"
    }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Akad Readiness",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = readiness.customerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Unit ${readiness.block} - ${readiness.unitNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Progress Circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                CircularProgressIndicator(
                    progress = (readiness.completionPercentage / 100).toFloat(),
                    modifier = Modifier.fillMaxSize(),
                    color = statusColor,
                    strokeWidth = 8.dp
                )
                
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${String.format("%.0f", readiness.completionPercentage)}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    
                    Text(
                        text = "Complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Missing Verifications
            if (readiness.missingVerifications.isNotEmpty()) {
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
                            text = "Missing Verifications:",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.kprflow.enterprise.ui.theme.Error,
                            fontWeight = FontWeight.Bold
                        )
                        
                        readiness.missingVerifications.forEach { missing ->
                            Text(
                                text = "• $missing",
                                style = MaterialTheme.typography.bodySmall,
                                color = com.kprflow.enterprise.ui.theme.Error
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Finalize Akad Button
            Button(
                onClick = onFinalizeAkad,
                enabled = readiness.isReadyForAkad && userRole in listOf("LEGAL", "MANAGER"),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (readiness.isReadyForAkad) 
                        com.kprflow.enterprise.ui.theme.Success 
                    else 
                        com.kprflow.enterprise.ui.theme.Error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.EventAvailable,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = when {
                        !readiness.isReadyForAkad -> "Waiting for Verification"
                        userRole !in listOf("LEGAL", "MANAGER") -> "Contact Legal/Manager"
                        else -> "Schedule Akad"
                    }
                )
            }
        }
    }
}

@Composable
private fun ProgressOverviewCard(readiness: com.kprflow.enterprise.data.model.AkadReadiness) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Department Status Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DepartmentStatusChip(
                    title = "Finance",
                    status = readiness.financeStatus,
                    color = com.kprflow.enterprise.ui.theme.Success,
                    modifier = Modifier.weight(1f)
                )
                
                DepartmentStatusChip(
                    title = "Legal",
                    status = readiness.legalStatus,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                DepartmentStatusChip(
                    title = "Marketing",
                    status = readiness.marketingStatus,
                    color = com.kprflow.enterprise.ui.theme.Warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DepartmentStatusChip(
    title: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
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
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun FinanceVerificationSection(
    readiness: com.kprflow.enterprise.data.model.AkadReadiness,
    userRole: String,
    onUpdateVerification: (
        com.kprflow.enterprise.domain.usecase.verification.VerificationType,
        Boolean,
        String?,
        String?,
        Double?
    ) -> Unit
) {
    val canEdit = userRole == "FINANCE"
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = com.kprflow.enterprise.ui.theme.Success,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "Finance Department",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (!canEdit) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = com.kprflow.enterprise.ui.theme.Error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // PPh Payment
            VerificationItem(
                title = "PPh Payment",
                isCompleted = readiness.isPphPaid,
                completedAt = readiness.pphPaidAt,
                completedBy = readiness.pphPaidBy,
                canEdit = canEdit,
                verificationType = com.kprflow.enterprise.domain.usecase.verification.VerificationType.PPH_PAID,
                onUpdateVerification = onUpdateVerification
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // BPHTB Payment
            VerificationItem(
                title = "BPHTB Payment",
                isCompleted = readiness.isBphtbPaid,
                completedAt = readiness.bphtbPaidAt,
                completedBy = readiness.bphtbPaidBy,
                canEdit = canEdit,
                verificationType = com.kprflow.enterprise.domain.usecase.verification.VerificationType.BPHTB_PAID,
                onUpdateVerification = onUpdateVerification
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Insurance Payment
            VerificationItem(
                title = "Insurance Payment",
                isCompleted = readiness.isInsurancePaid,
                completedAt = readiness.insurancePaidAt,
                completedBy = readiness.insurancePaidBy,
                canEdit = canEdit,
                verificationType = com.kprflow.enterprise.domain.usecase.verification.VerificationType.INSURANCE_PAID,
                onUpdateVerification = onUpdateVerification
            )
        }
    }
}

@Composable
private fun LegalVerificationSection(
    readiness: com.kprflow.enterprise.data.model.AkadReadiness,
    userRole: String,
    onUpdateVerification: (
        com.kprflow.enterprise.domain.usecase.verification.VerificationType,
        Boolean,
        String?,
        String?,
        Double?
    ) -> Unit
) {
    val canEdit = userRole == "LEGAL"
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "Legal Department",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (!canEdit) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = com.kprflow.enterprise.ui.theme.Error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // AJB Draft Ready
            VerificationItem(
                title = "AJB Draft Ready",
                isCompleted = readiness.isAjbdraftReady,
                completedAt = readiness.ajbdraftReadyAt,
                completedBy = readiness.ajbdraftReadyBy,
                canEdit = canEdit,
                verificationType = com.kprflow.enterprise.domain.usecase.verification.VerificationType.AJB_DRAFT_READY,
                onUpdateVerification = onUpdateVerification
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // BAST Ready
            VerificationItem(
                title = "BAST Ready",
                isCompleted = readiness.isBastReady,
                completedAt = readiness.bastReadyAt,
                completedBy = readiness.bastReadyBy,
                canEdit = canEdit,
                verificationType = com.kprflow.enterprise.domain.usecase.verification.VerificationType.BAST_READY,
                onUpdateVerification = onUpdateVerification
            )
        }
    }
}

@Composable
private fun MarketingVerificationSection(
    readiness: com.kprflow.enterprise.data.model.AkadReadiness,
    userRole: String,
    onUpdateVerification: (
        com.kprflow.enterprise.domain.usecase.verification.VerificationType,
        Boolean,
        String?,
        String?,
        Double?
    ) -> Unit
) {
    val canEdit = userRole == "MARKETING"
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = com.kprflow.enterprise.ui.theme.Warning,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "Marketing Department",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (!canEdit) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = com.kprflow.enterprise.ui.theme.Error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // SPR Final Signed
            VerificationItem(
                title = "SPR Final Signed",
                isCompleted = readiness.isSprFinalSigned,
                completedAt = readiness.sprFinalSignedAt,
                completedBy = readiness.sprFinalSignedBy,
                canEdit = canEdit,
                verificationType = com.kprflow.enterprise.domain.usecase.verification.VerificationType.SPR_FINAL_SIGNED,
                onUpdateVerification = onUpdateVerification
            )
        }
    }
}

@Composable
private fun VerificationItem(
    title: String,
    isCompleted: Boolean,
    completedAt: String?,
    completedBy: String?,
    canEdit: Boolean,
    verificationType: com.kprflow.enterprise.domain.usecase.verification.VerificationType,
    onUpdateVerification: (
        com.kprflow.enterprise.domain.usecase.verification.VerificationType,
        Boolean,
        String?,
        String?,
        Double?
    ) -> Unit
) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var documentUrl by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Switch(
                checked = isCompleted,
                onCheckedChange = { 
                    if (canEdit) {
                        showUpdateDialog = true
                    }
                },
                enabled = canEdit,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = com.kprflow.enterprise.ui.theme.Success,
                    checkedTrackColor = com.kprflow.enterprise.ui.theme.Success.copy(alpha = 0.5f)
                )
            )
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                if (isCompleted && completedAt != null) {
                    Text(
                        text = "Completed on ${completedAt.take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.kprflow.enterprise.ui.theme.Success
                    )
                    
                    if (completedBy != null) {
                        Text(
                            text = "By $completedBy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = if (canEdit) "Tap to verify" else "Waiting for verification",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (canEdit) MaterialTheme.colorScheme.primary else com.kprflow.enterprise.ui.theme.Error
                    )
                }
            }
        }
        
        if (!canEdit) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = com.kprflow.enterprise.ui.theme.Error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    
    // Update Dialog
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update $title") },
            text = {
                Column {
                    Text("Please provide verification details:")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (verificationType in listOf(
                        com.kprflow.enterprise.domain.usecase.verification.VerificationType.PPH_PAID,
                        com.kprflow.enterprise.domain.usecase.verification.VerificationType.BPHTB_PAID,
                        com.kprflow.enterprise.domain.usecase.verification.VerificationType.INSURANCE_PAID
                    )) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    OutlinedTextField(
                        value = documentUrl,
                        onValueChange = { documentUrl = it },
                        label = { Text("Document URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        onUpdateVerification(
                            verificationType,
                            true,
                            notes.ifBlank { null },
                            documentUrl.ifBlank { null },
                            amountValue
                        )
                        showUpdateDialog = false
                    }
                ) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FinalizeAkadDialog(
    readiness: com.kprflow.enterprise.data.model.AkadReadiness?,
    onDismiss: () -> Unit,
    onFinalize: (String, String?) -> Unit,
    userRole: String
) {
    var scheduledDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    if (readiness != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Schedule Akad") },
            text = {
                Column {
                    Text(
                        text = "All verifications are complete for ${readiness.customerName}. Please schedule the akad date.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = scheduledDate,
                        onValueChange = { scheduledDate = it },
                        label = { Text("Scheduled Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFinalize(scheduledDate, notes.ifBlank { null })
                    },
                    enabled = scheduledDate.isNotBlank()
                ) {
                    Text("Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
