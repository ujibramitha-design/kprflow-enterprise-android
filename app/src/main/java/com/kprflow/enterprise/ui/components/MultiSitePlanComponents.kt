package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
 * Multi-Site Plan Master Components
 * Reusable components for managing multiple site plans
 * Phase 16: Mobile App Optimization - Enhanced Features
 */

// =====================================================
// MULTI-SITE PLAN SELECTION COMPONENTS
// =====================================================

@Composable
fun MultiSitePlanSelector(
    sitePlans: List<SitePlanRegistryData>,
    selectedSitePlans: List<String>,
    onSitePlanSelected: (String, Boolean) -> Unit,
    onCompareSelected: (List<String>) -> Unit,
    maxSelection: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Header with selection info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Site Plans (${selectedSitePlans.size}/$maxSelection)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (selectedSitePlans.isNotEmpty()) {
                Button(
                    onClick = { onCompareSelected(selectedSitePlans) },
                    enabled = selectedSitePlans.size >= 2
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = "Compare",
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    Text("Compare (${selectedSitePlans.size})")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Site plans grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(sitePlans) { sitePlan ->
                SelectableSitePlanCard(
                    sitePlan = sitePlan,
                    isSelected = selectedSitePlans.contains(sitePlan.id),
                    onSelectionChanged = { isSelected ->
                        onSitePlanSelected(sitePlan.id, isSelected)
                    }
                )
            }
        }
    }
}

@Composable
fun SelectableSitePlanCard(
    sitePlan: SitePlanRegistryData,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectionChanged(!isSelected) },
        backgroundColor = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            GlassLight
    ) {
        Column {
            // Selection indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sitePlan.isFeatured) {
                    StatusChip(
                        status = "FEATURED",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChanged
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Project info
            Text(
                text = sitePlan.projectName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = sitePlan.projectCode,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "${sitePlan.city}, ${sitePlan.province}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${sitePlan.availableUnits}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Success
                    )
                    Text(
                        text = "Available",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatCurrency(sitePlan.avgUnitPrice),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Avg Price",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Categories
            if (sitePlan.categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(sitePlan.categories.split(", ")) { category ->
                        CategoryChip(category = category.trim())
                    }
                }
            }
        }
    }
}

// =====================================================
// SITE PLAN COMPARISON COMPONENTS
// =====================================================

@Composable
fun SitePlanComparisonTable(
    comparisonData: List<SitePlanComparisonData>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoBox(
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Site Plan Comparison",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Comparison table
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header row
                item {
                    ComparisonHeaderRow(sitePlans = comparisonData.map { it.sitePlan })
                }
                
                // Data rows
                item {
                    ComparisonDataRow(
                        label = "Project Name",
                        values = comparisonData.map { it.sitePlan.projectName }
                    )
                }
                
                item {
                    ComparisonDataRow(
                        label = "Location",
                        values = comparisonData.map { "${it.sitePlan.city}, ${it.sitePlan.province}" }
                    )
                }
                
                item {
                    ComparisonDataRow(
                        label = "Total Units",
                        values = comparisonData.map { it.sitePlan.totalUnits.toString() }
                    )
                }
                
                item {
                    ComparisonDataRow(
                        label = "Available Units",
                        values = comparisonData.map { it.sitePlan.availableUnits.toString() }
                    )
                }
                
                item {
                    ComparisonDataRow(
                        label = "Average Price",
                        values = comparisonData.map { formatCurrency(it.sitePlan.avgUnitPrice) }
                    )
                }
                
                item {
                    ComparisonDataRow(
                        label = "Price Range",
                        values = comparisonData.map { 
                            "${formatCurrency(it.sitePlan.minUnitPrice)} - ${formatCurrency(it.sitePlan.maxUnitPrice)}"
                        }
                    )
                }
                
                item {
                    ComparisonDataRow(
                        label = "Status",
                        values = comparisonData.map { it.sitePlan.projectStatus }
                    )
                }
                
                item {
                    ComparisonDataRow(
                        label = "Categories",
                        values = comparisonData.map { it.sitePlan.categories }
                    )
                }
                
                item {
                    ComparisonDataRow(
                        label = "Comparison Score",
                        values = comparisonData.map { 
                            "${(it.comparisonScore * 100).toInt()}%" 
                        }
                    )
                }
                
                item {
                    ComparisonDataRow(
                        label = "Rank",
                        values = comparisonData.map { "#${it.rankPosition}" }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonHeaderRow(
    sitePlans: List<SitePlanRegistryData>
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Label column
        Text(
            text = "Criteria",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Site plan columns
        sitePlans.forEach { sitePlan ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = sitePlan.projectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                Text(
                    text = sitePlan.projectCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ComparisonDataRow(
    label: String,
    values: List<String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (values.indexOf(values.maxOrNull { it.length }) % 2 == 0) 
                    MaterialTheme.colorScheme.surface 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        // Label column
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        // Value columns
        values.forEach { value ->
            Text(
                text = value,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// =====================================================
// SITE PLAN FILTER COMPONENTS
// =====================================================

@Composable
fun MultiSitePlanFilter(
    availableCities: List<String>,
    availableProvinces: List<String>,
    availableCategories: List<String>,
    selectedCity: String?,
    selectedProvince: String?,
    selectedCategory: String?,
    priceRange: ClosedFloatingPointRange<Float>,
    featuredOnly: Boolean,
    onCitySelected: (String?) -> Unit,
    onProvinceSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onPriceRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    onFeaturedOnlyChanged: (Boolean) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoBox(
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = "Filter Site Plans",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // City filter
            FilterDropdown(
                label = "City",
                options = availableCities,
                selectedOption = selectedCity,
                onOptionSelected = onCitySelected,
                placeholder = "All Cities"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Province filter
            FilterDropdown(
                label = "Province",
                options = availableProvinces,
                selectedOption = selectedProvince,
                onOptionSelected = onProvinceSelected,
                placeholder = "All Provinces"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category filter
            FilterDropdown(
                label = "Category",
                options = availableCategories,
                selectedOption = selectedCategory,
                onOptionSelected = onCategorySelected,
                placeholder = "All Categories"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Price range filter
            PriceRangeFilter(
                priceRange = priceRange,
                onPriceRangeChanged = onPriceRangeChanged
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Featured only filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Featured Only",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Switch(
                    checked = featuredOnly,
                    onCheckedChange = onFeaturedOnlyChanged
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reset button
            OutlinedButton(
                onClick = onResetFilters,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Filters")
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String?) -> Unit,
    placeholder: String
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedOption ?: placeholder,
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(placeholder) },
                    onClick = {
                        onOptionSelected(null)
                        expanded = false
                    }
                )
                
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceRangeFilter(
    priceRange: ClosedFloatingPointRange<Float>,
    onPriceRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit
) {
    var minPrice by remember { mutableStateOf(priceRange.start) }
    var maxPrice by remember { mutableStateOf(priceRange.endInclusive) }
    
    Column {
        Text(
            text = "Price Range (Million IDR)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = minPrice.toString(),
                onValueChange = { 
                    minPrice = it.toFloatOrNull() ?: 0f
                    onPriceRangeChanged(minPrice..maxPrice)
                },
                label = { Text("Min") },
                modifier = Modifier.weight(1f)
            )
            
            OutlinedTextField(
                value = maxPrice.toString(),
                onValueChange = { 
                    maxPrice = it.toFloatOrNull() ?: 0f
                    onPriceRangeChanged(minPrice..maxPrice)
                },
                label = { Text("Max") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =====================================================
// UTILITY COMPONENTS
// =====================================================

@Composable
private fun CategoryChip(
    category: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
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
            modifier = Modifier.padding(horizontal = 6.dp, vertical: 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// =====================================================
// DATA CLASSES
// =====================================================

data class SitePlanRegistryData(
    val id: String,
    val projectName: String,
    val projectCode: String,
    val developerName: String,
    val locationAddress: String,
    val city: String,
    val province: String,
    val totalUnits: Int,
    val availableUnits: Int,
    val bookedUnits: Int,
    val soldUnits: Int,
    val avgUnitPrice: Double,
    val minUnitPrice: Double,
    val maxUnitPrice: Double,
    val projectStatus: String,
    val isFeatured: Boolean,
    val categories: String,
    val isFavorite: Boolean,
    val lastInteraction: String
)

data class SitePlanComparisonData(
    val sitePlan: SitePlanRegistryData,
    val comparisonScore: Double,
    val detailedScores: Map<String, Double>,
    val rankPosition: Int
)

// =====================================================
// UTILITY FUNCTIONS
// =====================================================

private fun formatCurrency(amount: Double): String {
    val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    return formatter.format(amount)
}
