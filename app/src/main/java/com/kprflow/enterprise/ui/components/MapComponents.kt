package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kprflow.enterprise.ui.theme.*

/**
 * Reusable Map Components for KPRFlow Enterprise
 * Interactive map components for site plan visualization
 */

// =====================================================
// MAP CONTAINER COMPONENTS
// =====================================================

@Composable
fun MapContainer(
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    onMapClick: ((Double, Double) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    BentoBox(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        backgroundColor = BentoSurface,
        onClick = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(0xFFF0F8FF), // Light blue background for map
                    RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
        ) {
            content()
            
            // Map overlay controls
            if (isInteractive) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    MapControls(onMapClick = onMapClick)
                }
            }
        }
    }
}

@Composable
fun MapControls(
    onMapClick: ((Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Zoom controls
        GlassSurfaceSmall {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { /* Zoom in */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = { /* Zoom out */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Map type controls
        GlassSurfaceSmall {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { /* Switch to satellite */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Satellite,
                        contentDescription = "Satellite View",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = { /* Switch to terrain */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terrain,
                        contentDescription = "Terrain View",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Location controls
        GlassSurfaceSmall {
            IconButton(
                onClick = { /* Get current location */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// =====================================================
// SITE PLAN COMPONENTS
// =====================================================

@Composable
fun SitePlanCard(
    sitePlan: SitePlanData,
    onSitePlanClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BentoBox(
        modifier = modifier,
        onClick = { onSitePlanClick(sitePlan.id) }
    ) {
        Column {
            // Header with project info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sitePlan.projectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = sitePlan.projectCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = sitePlan.locationAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                StatusChip(
                    status = sitePlan.projectStatus,
                    color = when (sitePlan.projectStatus) {
                        "COMPLETED" -> Success
                        "DEVELOPMENT" -> Info
                        else -> Warning
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Statistics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BentoMetric(
                    value = sitePlan.totalUnits.toString(),
                    label = "Total Units",
                    modifier = Modifier.weight(1f)
                )
                
                BentoMetric(
                    value = sitePlan.availableUnits.toString(),
                    label = "Available",
                    modifier = Modifier.weight(1f)
                )
                
                BentoMetric(
                    value = "${sitePlan.soldUnits}",
                    label = "Sold",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            BentoProgress(
                progress = sitePlan.soldUnits.toFloat() / sitePlan.totalUnits.toFloat(),
                label = "Sales Progress",
                color = Success
            )
        }
    }
}

@Composable
fun BlockCard(
    block: BlockData,
    onBlockClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurfaceSmall(
        modifier = modifier,
        onClick = { onBlockClick(block.id) }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Block ${block.blockName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = block.blockType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${block.availableUnits}/${block.totalLots}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${block.availableLots} Available",
                    style = MaterialTheme.typography.bodySmall,
                    color = Success
                )
            }
        }
    }
}

@Composable
fun UnitCard(
    unit: UnitData,
    onUnitClick: (String) -> Unit,
    onBookUnit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurfaceSmall(
        modifier = modifier,
        onClick = { onUnitClick(unit.id) }
    ) {
        Column {
            // Header with unit info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = unit.unitNumber,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = unit.unitType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                StatusChip(
                    status = unit.status,
                    color = when (unit.status) {
                        "AVAILABLE" -> Success
                        "BOOKED" -> Warning
                        "SOLD" -> Error
                        else -> Info
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Unit details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Land: ${unit.landArea}m²",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Building: ${unit.buildingArea}m²",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (unit.bedrooms > 0) {
                        Text(
                            text = "${unit.bedrooms}BR/${unit.bathrooms}BT",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatCurrency(unit.totalPrice),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "/m²",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Action buttons
            if (unit.status == "AVAILABLE") {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onUnitClick(unit.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View")
                    }
                    
                    Button(
                        onClick = { onBookUnit(unit.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Book")
                    }
                }
            }
        }
    }
}

// =====================================================
// MAP OVERLAY COMPONENTS
// =====================================================

@Composable
fun MapOverlayCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .background(
                GlassSurface,
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            content()
        }
    }
}

@Composable
fun UnitInfoOverlay(
    unit: UnitData,
    onBookUnit: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    MapOverlayCard(
        title = "Unit Details",
        modifier = modifier
    ) {
        Column {
            // Unit basic info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${unit.unitNumber} - ${unit.unitType}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Unit details
            Column {
                InfoRow("Type", unit.unitType)
                InfoRow("Category", unit.unitCategory)
                InfoRow("Land Area", "${unit.landArea}m²")
                InfoRow("Building Area", "${unit.buildingArea}m²")
                
                if (unit.bedrooms > 0) {
                    InfoRow("Bedrooms", "${unit.bedrooms}")
                    InfoRow("Bathrooms", "${unit.bathrooms}")
                }
                
                if (unit.carParking > 0) {
                    InfoRow("Car Parking", "${unit.carParking}")
                }
                
                InfoRow("Orientation", unit.orientation ?: "N/A")
                InfoRow("Floor Level", unit.floorLevel?.toString() ?: "N/A")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Price",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = formatCurrency(unit.totalPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action button
            if (unit.status == "AVAILABLE") {
                Button(
                    onClick = { onBookUnit(unit.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Book This Unit")
                }
            } else {
                Text(
                    text = "Unit ${unit.status}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BlockInfoOverlay(
    block: BlockData,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    MapOverlayCard(
        title = "Block ${block.blockName} Details",
        modifier = modifier
    ) {
        Column {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Block ${block.blockName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Block details
            Column {
                InfoRow("Block Type", block.blockType)
                InfoRow("Total Lots", block.totalLots.toString())
                InfoRow("Available Lots", block.availableLots.toString())
                InfoRow("Booked Lots", block.bookedLots.toString())
                InfoRow("Sold Lots", block.soldLots.toString())
                InfoRow("Block Area", "${block.blockArea}m²")
                
                if (block.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = block.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// =====================================================
// SEARCH AND FILTER COMPONENTS
// =====================================================

@Composable
fun MapSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier,
        padding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search projects, blocks, units...") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    containerColor = Color.Transparent
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = onFilterClick
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = { Text(label) },
        selected = isSelected,
        modifier = modifier
    )
}

// =====================================================
// UTILITY COMPONENTS
// =====================================================

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusChip(
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// =====================================================
// DATA CLASSES
// =====================================================

data class SitePlanData(
    val id: String,
    val projectName: String,
    val projectCode: String,
    val developerName: String,
    val locationAddress: String,
    val city: String,
    val province: String,
    val totalUnits: Int,
    val availableUnits: Int,
    val soldUnits: Int,
    val projectStatus: String,
    val startDate: String,
    val completionDate: String,
    val description: String
)

data class BlockData(
    val id: String,
    val blockName: String,
    val blockType: String,
    val totalLots: Int,
    val availableLots: Int,
    val bookedLots: Int,
    val soldLots: Int,
    val blockArea: Double,
    val description: String
)

data class UnitData(
    val id: String,
    val unitNumber: String,
    val unitType: String,
    val unitCategory: String,
    val landArea: Double,
    val buildingArea: Double,
    val totalPrice: Double,
    val status: String,
    val orientation: String?,
    val floorLevel: Int?,
    val bedrooms: Int,
    val bathrooms: Int,
    val carParking: Int
)

// =====================================================
// UTILITY FUNCTIONS
// =====================================================

private fun formatCurrency(amount: Double): String {
    val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    return formatter.format(amount)
}
