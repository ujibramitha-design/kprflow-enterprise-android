package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.viewmodel.EstateInspectionViewModel
import com.kprflow.enterprise.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstateInspectionScreen(
    dossierId: String,
    unitId: String,
    userRole: String = "ESTATE",
    viewModel: EstateInspectionViewModel = hiltViewModel()
) {
    val inspectionState by viewModel.inspectionState.collectAsStateWithLifecycle()
    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val photoState by viewModel.photoState.collectAsStateWithLifecycle()
    
    var showCamera by remember { mutableStateOf(false) }
    var showDefectDialog by remember { mutableStateOf(false) }
    var inspectionNotes by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("PENDING") }
    
    LaunchedEffect(dossierId) {
        viewModel.loadInspectionData(dossierId, unitId)
        viewModel.startLocationUpdates()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Estate Inspection")
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
            when (inspectionState) {
                is Resource.Success -> {
                    val inspection = inspectionState.data
                    
                    // Unit Information Card
                    UnitInfoCard(inspection = inspection)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // GPS Validation Card
                    GPSValidationCard(
                        locationState = locationState,
                        inspection = inspection,
                        onRefreshLocation = { viewModel.refreshLocation() }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Photo Evidence Section
                    PhotoEvidenceSection(
                        photoState = photoState,
                        onTakePhoto = { showCamera = true },
                        onViewPhoto = { url -> /* Handle photo view */ }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Inspection Status
                    InspectionStatusCard(
                        currentStatus = selectedStatus,
                        onStatusChange = { selectedStatus = it },
                        notes = inspectionNotes,
                        onNotesChange = { inspectionNotes = it },
                        onAddDefect = { showDefectDialog = true }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action Buttons
                    ActionButtons(
                        inspection = inspection,
                        selectedStatus = selectedStatus,
                        inspectionNotes = inspectionNotes,
                        locationState = locationState,
                        photoState = photoState,
                        onSaveInspection = { status, notes, photos ->
                            viewModel.saveInspection(
                                dossierId = dossierId,
                                unitId = unitId,
                                status = status,
                                notes = notes,
                                photos = photos
                            )
                        }
                    )
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
    
    // Camera Dialog
    if (showCamera) {
        CameraCaptureDialog(
            onDismiss = { showCamera = false },
            onPhotoCaptured = { imageUri, gpsLocation ->
                viewModel.savePhotoEvidence(imageUri, gpsLocation)
                showCamera = false
            },
            locationState = locationState
        )
    }
    
    // Defect Dialog
    if (showDefectDialog) {
        DefectDialog(
            onDismiss = { showDefectDialog = false },
            onAddDefect = { defect ->
                viewModel.addDefect(defect)
                showDefectDialog = false
            }
        )
    }
}

@Composable
private fun UnitInfoCard(
    inspection: com.kprflow.enterprise.data.model.EstateInspection
) {
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
                text = "Unit Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Block",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = inspection.block ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Unit Number",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = inspection.unitNumber ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Customer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = inspection.customerName ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Unit Type",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = inspection.unitType ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun GPSValidationCard(
    locationState: Resource<com.kprflow.enterprise.data.model.LocationData>,
    inspection: com.kprflow.enterprise.data.model.EstateInspection,
    onRefreshLocation: () -> Unit
) {
    val gpsValidationColor = when {
        locationState is Resource.Success && locationState.data.distanceFromUnit <= 50 -> com.kprflow.enterprise.ui.theme.Success
        locationState is Resource.Success && locationState.data.distanceFromUnit <= 100 -> com.kprflow.enterprise.ui.theme.Warning
        else -> com.kprflow.enterprise.ui.theme.Error
    }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = gpsValidationColor.copy(alpha = 0.1f)
        ),
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
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = gpsValidationColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "GPS Location Validation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onRefreshLocation) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Location",
                        tint = gpsValidationColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when (locationState) {
                is Resource.Success -> {
                    val location = locationState.data
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Current Location",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${location.latitude}, ${location.longitude}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Distance from Unit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%.1f", location.distanceFromUnit)}m",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = gpsValidationColor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "GPS Accuracy: ${String.format("%.1f", location.accuracy)}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Validation Status
                    val validationStatus = when {
                        location.distanceFromUnit <= 50 -> "✅ Valid Location"
                        location.distanceFromUnit <= 100 -> "⚠️ Questionable Location"
                        else -> "❌ Invalid Location"
                    }
                    
                    Text(
                        text = validationStatus,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = gpsValidationColor
                    )
                }
                is Resource.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = gpsValidationColor
                        )
                        Text(
                            text = "Getting GPS location...",
                            style = MaterialTheme.typography.bodySmall,
                            color = gpsValidationColor
                        )
                    }
                }
                else -> {
                    Text(
                        text = "Failed to get GPS location",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.kprflow.enterprise.ui.theme.Error
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoEvidenceSection(
    photoState: Resource<List<com.kprflow.enterprise.data.model.PhotoEvidence>>,
    onTakePhoto: () -> Unit,
    onViewPhoto: (String) -> Unit
) {
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Text(
                        text = "Photo Evidence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = onTakePhoto,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take Photo")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when (photoState) {
                is Resource.Success -> {
                    if (photoState.data.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            photoState.data.forEach { photo ->
                                PhotoItem(
                                    photo = photo,
                                    onViewPhoto = onViewPhoto
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No photos taken yet. Tap 'Take Photo' to add evidence.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp)
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun PhotoItem(
    photo: com.kprflow.enterprise.data.model.PhotoEvidence,
    onViewPhoto: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column {
                Text(
                    text = "Photo ${photo.id.take(8)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "GPS: ${String.format("%.1f", photo.gpsDistance)}m from unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        TextButton(
            onClick = { onViewPhoto(photo.url) }
        ) {
            Text("View")
        }
    }
}

@Composable
private fun InspectionStatusCard(
    currentStatus: String,
    onStatusChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onAddDefect: () -> Unit
) {
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
                text = "Inspection Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Result",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { onStatusChange("FAIL") },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (currentStatus == "FAIL") com.kprflow.enterprise.ui.theme.Error else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("FAIL")
                    }
                    
                    Switch(
                        checked = currentStatus == "PASS",
                        onCheckedChange = { 
                            onStatusChange(if (it) "PASS" else "FAIL")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = com.kprflow.enterprise.ui.theme.Success,
                            checkedTrackColor = com.kprflow.enterprise.ui.theme.Success.copy(alpha = 0.5f)
                        )
                    )
                    
                    TextButton(
                        onClick = { onStatusChange("PASS") },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (currentStatus == "PASS") com.kprflow.enterprise.ui.theme.Success else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("PASS")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notes Field
            if (currentStatus == "FAIL") {
                Text(
                    text = "Defect Notes (Required)*",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = com.kprflow.enterprise.ui.theme.Error
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    placeholder = { Text("Describe defects found...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    isError = notes.isBlank()
                )
                
                if (notes.isBlank()) {
                    Text(
                        text = "Defect notes are required when status is FAIL",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.kprflow.enterprise.ui.theme.Error
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onAddDefect,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.kprflow.enterprise.ui.theme.Warning
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Defect Details")
                }
            } else {
                Text(
                    text = "Notes (Optional)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    placeholder = { Text("Add any notes about the inspection...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    inspection: com.kprflow.enterprise.data.model.EstateInspection,
    selectedStatus: String,
    inspectionNotes: String,
    locationState: Resource<com.kprflow.enterprise.data.model.LocationData>,
    photoState: Resource<List<com.kprflow.enterprise.data.model.PhotoEvidence>>,
    onSaveInspection: (String, String, List<String>) -> Unit
) {
    val isValid = when {
        selectedStatus == "FAIL" && inspectionNotes.isBlank() -> false
        locationState !is Resource.Success -> false
        photoState !is Resource.Success || photoState.data.isEmpty() -> false
        locationState is Resource.Success && locationState.data.distanceFromUnit > 100 -> false
        else -> true
    }
    
    val validationMessage = when {
        selectedStatus == "FAIL" && inspectionNotes.isBlank() -> "Defect notes are required"
        locationState !is Resource.Success -> "GPS location required"
        photoState !is Resource.Success || photoState.data.isEmpty() -> "At least one photo required"
        locationState is Resource.Success && locationState.data.distanceFromUnit > 100 -> "Location too far from unit"
        else -> null
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (validationMessage != null) {
            Surface(
                color = com.kprflow.enterprise.ui.theme.Error.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = validationMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = com.kprflow.enterprise.ui.theme.Error
                )
            }
        }
        
        Button(
            onClick = { 
                onSaveInspection(
                    selectedStatus,
                    inspectionNotes,
                    (photoState as? Resource.Success)?.data?.map { it.url } ?: emptyList()
                )
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedStatus == "PASS") com.kprflow.enterprise.ui.theme.Success else com.kprflow.enterprise.ui.theme.Error
            )
        ) {
            Icon(
                imageVector = if (selectedStatus == "PASS") Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = if (selectedStatus == "PASS") "Complete Inspection - PASS" else "Complete Inspection - FAIL"
            )
        }
    }
}

@Composable
private fun CameraCaptureDialog(
    onDismiss: () -> Unit,
    onPhotoCaptured: (String, com.kprflow.enterprise.data.model.LocationData) -> Unit,
    locationState: Resource<com.kprflow.enterprise.data.model.LocationData>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Take Photo Evidence") },
        text = {
            Column {
                Text("Camera will open to take photo evidence. Make sure:")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("• Good lighting conditions")
                Text("• Clear view of the area")
                Text("• Include any defects if present")
                
                if (locationState is Resource.Success) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• GPS location: ${String.format("%.1f", locationState.data.distanceFromUnit)}m from unit")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // This would integrate with CameraX
                    // For now, simulate photo capture
                    val mockImageUri = "content://media/external/images/mock/image_${System.currentTimeMillis()}.jpg"
                    val mockLocation = com.kprflow.enterprise.data.model.LocationData(
                        latitude = -6.1751,
                        longitude = 106.8271,
                        accuracy = 5.0,
                        distanceFromUnit = 25.0
                    )
                    onPhotoCaptured(mockImageUri, mockLocation)
                }
            ) {
                Text("Open Camera")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DefectDialog(
    onDismiss: () -> Unit,
    onAddDefect: (com.kprflow.enterprise.data.model.DefectItem) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("MINOR") }
    var location by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Defect Details") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location in Unit") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Severity:")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("MINOR", "MAJOR", "CRITICAL").forEach { level ->
                        FilterChip(
                            selected = severity == level,
                            onClick = { severity = level },
                            label = { Text(level) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val defect = com.kprflow.enterprise.data.model.DefectItem(
                        category = category,
                        description = description,
                        severity = severity,
                        location = location
                    )
                    onAddDefect(defect)
                },
                enabled = category.isNotBlank() && description.isNotBlank()
            ) {
                Text("Add Defect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
