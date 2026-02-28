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
import com.kprflow.enterprise.viewmodel.MapSitePlanViewModel

/**
 * Map Site Plan Master Screen
 * Interactive map visualization for property site plans
 * Phase 16: Mobile App Optimization - Enhanced Features
 */

@Composable
fun MapSitePlanScreen(
    navController: NavController,
    viewModel: MapSitePlanViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    
    KPRFlowEnterpriseTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Site Plan Master") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                    onSearch = { viewModel.searchProjects(searchQuery) },
                    onFilterClick = { showFilterDialog = true }
                )
                
                // Filter chips
                FilterChipsRow(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { 
                        selectedFilter = it
                        viewModel.filterProjects(it)
                    }
                )
                
                // Map container
                MapContainer(
                    modifier = Modifier.weight(1f),
                    isInteractive = true,
                    onMapClick = { lat, lng ->
                        viewModel.handleMapClick(lat, lng)
                    }
                ) {
                    // Map content would be implemented with actual map library
                    // For now, showing placeholder
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Map",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Interactive Map View",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = "Click on map to view unit details",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Site plans list
                when (uiState) {
                    is MapSitePlanUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is MapSitePlanUiState.Success -> {
                        SitePlansList(
                            sitePlans = uiState.sitePlans,
                            onSitePlanClick = { sitePlanId ->
                                navController.navigate("site_plan_detail/$sitePlanId")
                            }
                        )
                    }
                    is MapSitePlanUiState.Error -> {
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
            FilterDialog(
                selectedFilter = selectedFilter,
                onFilterSelected = { filter ->
                    selectedFilter = filter
                    viewModel.filterProjects(filter)
                    showFilterDialog = false
                },
                onDismiss = { showFilterDialog = false }
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("ALL", "DEVELOPMENT", "COMPLETED", "PLANNING")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                label = filter,
                isSelected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
private fun SitePlansList(
    sitePlans: List<SitePlanData>,
    onSitePlanClick: (String) -> Unit
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
                onSitePlanClick = onSitePlanClick
            )
        }
    }
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

@Composable
private fun FilterDialog(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Site Plans") },
        text = {
            Column {
                Text("Select project status to filter:")
                Spacer(modifier = Modifier.height(16.dp))
                
                val filters = listOf("ALL", "DEVELOPMENT", "COMPLETED", "PLANNING")
                filters.forEach { filter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFilterSelected(filter) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFilter == filter,
                            onClick = { onFilterSelected(filter) }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(filter)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
