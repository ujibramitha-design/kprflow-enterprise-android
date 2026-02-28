package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.theme.KPRFlowEnterpriseTheme
import com.kprflow.enterprise.viewmodel.MultiSitePlanViewModel

/**
 * Multi-Site Plan Master Screen
 * Screen for selecting and comparing multiple site plans
 * Phase 16: Mobile App Optimization - Enhanced Features
 */

@Composable
fun MultiSitePlanScreen(
    navController: NavController,
    viewModel: MultiSitePlanViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showComparisonDialog by remember { mutableStateOf(false) }
    var selectedSitePlans by remember { mutableStateOf<List<String>>(emptyList()) }
    
    KPRFlowEnterpriseTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Site Plans") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search bar
                MapSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { viewModel.searchSitePlans(searchQuery) },
                    onFilterClick = { showFilterDialog = true }
                )
                
                // Quick filter chips
                QuickFilterChips(
                    selectedFilter = viewModel.selectedFilter.value,
                    onFilterSelected = { filter ->
                        viewModel.setFilter(filter)
                    }
                )
                
                // Site plans content
                when (uiState) {
                    is MultiSitePlanUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is MultiSitePlanUiState.Success -> {
                        if (uiState.isSelectionMode) {
                            // Selection mode for comparison
                            MultiSitePlanSelector(
                                sitePlans = uiState.sitePlans,
                                selectedSitePlans = selectedSitePlans,
                                onSitePlanSelected = { sitePlanId, isSelected ->
                                    selectedSitePlans = if (isSelected) {
                                        selectedSitePlans + sitePlanId
                                    } else {
                                        selectedSitePlans - sitePlanId
                                    }
                                },
                                onCompareSelected = { sitePlanIds ->
                                    viewModel.compareSitePlans(sitePlanIds)
                                    showComparisonDialog = true
                                }
                            )
                        } else {
                            // Normal view mode
                            SitePlansGrid(
                                sitePlans = uiState.sitePlans,
                                onSitePlanClick = { sitePlanId ->
                                    navController.navigate("site_plan_detail/$sitePlanId")
                                },
                                onFavoriteClick = { sitePlanId, isFavorite ->
                                    viewModel.toggleFavorite(sitePlanId, isFavorite)
                                }
                            )
                        }
                    }
                    is MultiSitePlanUiState.Error -> {
                        ErrorMessage(
                            message = uiState.message,
                            onRetry = { viewModel.refreshData() }
                        )
                    }
                }
            }
        }
        
        // Filter dialog
        if (showFilterDialog) {
            MultiSitePlanFilterDialog(
                availableCities = viewModel.availableCities.value,
                availableProvinces = viewModel.availableProvinces.value,
                availableCategories = viewModel.availableCategories.value,
                currentFilters = viewModel.currentFilters.value,
                onFiltersApplied = { filters ->
                    viewModel.applyFilters(filters)
                    showFilterDialog = false
                },
                onDismiss = { showFilterDialog = false }
            )
        }
        
        // Comparison dialog
        if (showComparisonDialog) {
            when (val comparisonState = viewModel.comparisonState.value) {
                is ComparisonUiState.Success -> {
                    SitePlanComparisonDialog(
                        comparisonData = comparisonState.comparisonData,
                        onClose = { showComparisonDialog = false }
                    )
                }
                is ComparisonUiState.Error -> {
                    AlertDialog(
                        onDismissRequest = { showComparisonDialog = false },
                        title = { Text("Comparison Error") },
                        text = { Text(comparisonState.message) },
                        confirmButton = {
                            TextButton(onClick = { showComparisonDialog = false }) {
                                Text("OK")
                            }
                        }
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickFilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("ALL", "FEATURED", "RESIDENTIAL", "COMMERCIAL", "DEVELOPMENT")
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                label = filter,
                isSelected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
private fun SitePlansGrid(
    sitePlans: List<SitePlanRegistryData>,
    onSitePlanClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(sitePlans) { sitePlan ->
            SitePlanCard(
                sitePlan = sitePlan,
                onSitePlanClick = onSitePlanClick,
                onFavoriteClick = onFavoriteClick
            )
        }
    }
}

@Composable
private fun SitePlanCard(
    sitePlan: SitePlanRegistryData,
    onSitePlanClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSitePlanClick(sitePlan.id) }
    ) {
        Column {
            // Header with favorite button
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
                
                IconButton(
                    onClick = { onFavoriteClick(sitePlan.id, !sitePlan.isFavorite) }
                ) {
                    Icon(
                        imageVector = if (sitePlan.isFavorite) 
                            Icons.Default.Favorite 
                        else 
                            Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (sitePlan.isFavorite) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Project info
            Text(
                text = sitePlan.projectName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = sitePlan.projectCode,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "${sitePlan.city}, ${sitePlan.province}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatCurrency(sitePlan.avgUnitPrice),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Avg Price",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun MultiSitePlanFilterDialog(
    availableCities: List<String>,
    availableProvinces: List<String>,
    availableCategories: List<String>,
    currentFilters: SitePlanFilters,
    onFiltersApplied: (SitePlanFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var tempFilters by remember { mutableStateOf(currentFilters) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Site Plans") },
        text = {
            Column {
                // City filter
                FilterDropdown(
                    label = "City",
                    options = availableCities,
                    selectedOption = tempFilters.city,
                    onOptionSelected = { tempFilters = tempFilters.copy(city = it) },
                    placeholder = "All Cities"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Province filter
                FilterDropdown(
                    label = "Province",
                    options = availableProvinces,
                    selectedOption = tempFilters.province,
                    onOptionSelected = { tempFilters = tempFilters.copy(province = it) },
                    placeholder = "All Provinces"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Category filter
                FilterDropdown(
                    label = "Category",
                    options = availableCategories,
                    selectedOption = tempFilters.category,
                    onOptionSelected = { tempFilters = tempFilters.copy(category = it) },
                    placeholder = "All Categories"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Featured only
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
                        checked = tempFilters.featuredOnly,
                        onCheckedChange = { tempFilters = tempFilters.copy(featuredOnly = it) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onFiltersApplied(tempFilters) }) {
                Text("Apply")
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
private fun SitePlanComparisonDialog(
    comparisonData: List<SitePlanComparisonData>,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Site Plan Comparison") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header
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
                        label = "Comparison Score",
                        values = comparisonData.map { "${(it.comparisonScore * 100).toInt()}%" }
                    )
                }
                
                item {
                    ComparisonDataRow(
                        label = "Rank",
                        values = comparisonData.map { "#${it.rankPosition}" }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    BentoBox {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

// =====================================================
// DATA CLASSES
// =====================================================

data class SitePlanFilters(
    val city: String? = null,
    val province: String? = null,
    val category: String? = null,
    val priceMin: Double? = null,
    val priceMax: Double? = null,
    val featuredOnly: Boolean = false
)

// =====================================================
// UI STATES
// =====================================================

sealed class MultiSitePlanUiState {
    object Loading : MultiSitePlanUiState()
    data class Success(
        val sitePlans: List<SitePlanRegistryData>,
        val isSelectionMode: Boolean = false
    ) : MultiSitePlanUiState()
    data class Error(val message: String) : MultiSitePlanUiState()
}

sealed class ComparisonUiState {
    object Loading : ComparisonUiState()
    data class Success(val comparisonData: List<SitePlanComparisonData>) : ComparisonUiState()
    data class Error(val message: String) : ComparisonUiState()
}

// =====================================================
// UTILITY FUNCTIONS
// =====================================================

private fun formatCurrency(amount: Double): String {
    val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    return formatter.format(amount)
}
