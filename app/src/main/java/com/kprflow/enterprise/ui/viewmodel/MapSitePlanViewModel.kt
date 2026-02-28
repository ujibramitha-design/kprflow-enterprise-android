package com.kprflow.enterprise.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.model.SitePlanData
import com.kprflow.enterprise.data.repository.MapSitePlanRepository
import com.kprflow.enterprise.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Map Site Plan Master ViewModel
 * Manages state and logic for site plan map functionality
 * Phase 16: Mobile App Optimization - Enhanced Features
 */

@HiltViewModel
class MapSitePlanViewModel @Inject constructor(
    private val mapSitePlanRepository: MapSitePlanRepository
) : ViewModel() {
    
    // UI State for site plans
    private val _uiState = mutableStateOf<UiState<List<SitePlanData>>>(UiState.Loading)
    val uiState: State<UiState<List<SitePlanData>>> = _uiState
    
    // Search state
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery
    
    // Filter state
    private val _selectedFilter = mutableStateOf("ALL")
    val selectedFilter: State<String> = _selectedFilter
    
    // Selected site plan for detail view
    private val _selectedSitePlan = mutableStateOf<SitePlanData?>(null)
    val selectedSitePlan: State<SitePlanData?> = _selectedSitePlan
    
    // Map interaction state
    private val _selectedCoordinates = mutableStateOf<Pair<Double, Double>?>(null)
    val selectedCoordinates: State<Pair<Double, Double>?> = _selectedCoordinates
    
    // Loading state
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    init {
        loadSitePlans()
    }
    
    private fun loadSitePlans() {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = UiState.Loading
            
            try {
                val sitePlans = mapSitePlanRepository.getSitePlans()
                _uiState.value = UiState.Success(sitePlans)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load site plans")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshData() {
        loadSitePlans()
    }
    
    fun searchProjects(query: String) {
        _searchQuery.value = query
        
        viewModelScope.launch {
            try {
                val filteredPlans = if (query.isBlank()) {
                    mapSitePlanRepository.getSitePlans()
                } else {
                    mapSitePlanRepository.searchSitePlans(query)
                }
                
                _uiState.value = UiState.Success(filteredPlans)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Search failed")
            }
        }
    }
    
    fun filterProjects(filter: String) {
        _selectedFilter.value = filter
        
        viewModelScope.launch {
            try {
                val filteredPlans = if (filter == "ALL") {
                    mapSitePlanRepository.getSitePlans()
                } else {
                    mapSitePlanRepository.filterSitePlansByStatus(filter)
                }
                
                _uiState.value = UiState.Success(filteredPlans)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Filter failed")
            }
        }
    }
    
    fun selectSitePlan(sitePlan: SitePlanData) {
        _selectedSitePlan.value = sitePlan
    }
    
    fun handleMapClick(latitude: Double, longitude: Double) {
        _selectedCoordinates.value = Pair(latitude, longitude)
        
        // Find units near the clicked coordinates
        viewModelScope.launch {
            try {
                val nearbyUnits = mapSitePlanRepository.getUnitsNearCoordinates(latitude, longitude)
                // Handle nearby units - could show overlay or navigate to detail
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun getSitePlanById(sitePlanId: String): SitePlanData? {
        return when (val state = _uiState.value) {
            is UiState.Success -> state.data.find { it.id == sitePlanId }
            else -> null
        }
    }
    
    fun getBlocksBySitePlan(sitePlanId: String): List<BlockData> {
        // This would be implemented with actual repository call
        return emptyList() // Placeholder
    }
    
    fun getUnitsByBlock(blockId: String): List<UnitData> {
        // This would be implemented with actual repository call
        return emptyList() // Placeholder
    }
    
    fun bookUnit(unitId: String) {
        viewModelScope.launch {
            try {
                mapSitePlanRepository.bookUnit(unitId)
                // Refresh data after booking
                loadSitePlans()
            } catch (e: Exception) {
                // Handle booking error
            }
        }
    }
    
    fun clearSelection() {
        _selectedSitePlan.value = null
        _selectedCoordinates.value = null
    }
}

// Data classes for map functionality
data class BlockData(
    val id: String,
    val sitePlanId: String,
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
    val sitePlanId: String,
    val blockId: String,
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
    val carParking: Int,
    val coordinates: Pair<Double, Double>?
)

// UI State for Map Site Plan
sealed class MapSitePlanUiState {
    object Loading : MapSitePlanUiState()
    data class Success(val sitePlans: List<SitePlanData>) : MapSitePlanUiState()
    data class Error(val message: String) : MapSitePlanUiState()
}
