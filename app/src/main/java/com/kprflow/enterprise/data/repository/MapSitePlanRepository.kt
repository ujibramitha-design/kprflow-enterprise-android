package com.kprflow.enterprise.data.repository

import com.kprflow.enterprise.data.model.SitePlanData
import com.kprflow.enterprise.data.remote.MapSitePlanApiService
import com.kprflow.enterprise.data.local.MapSitePlanDao
import com.kprflow.enterprise.ui.common.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Map Site Plan Repository
 * Handles data operations for site plan master functionality
 * Phase 16: Mobile App Optimization - Enhanced Features
 */

@Singleton
class MapSitePlanRepository @Inject constructor(
    private val apiService: MapSitePlanApiService,
    private val dao: MapSitePlanDao
) {
    
    fun getSitePlans(): Flow<Result<List<SitePlanData>>> = flow {
        emit(Result.Loading)
        
        try {
            // Try to get from local cache first
            val cachedPlans = dao.getAllSitePlans()
            if (cachedPlans.isNotEmpty()) {
                emit(Result.Success(cachedPlans.map { it.toSitePlanData() }))
            }
            
            // Fetch from remote API
            val remotePlans = apiService.getSitePlans()
            dao.insertSitePlans(remotePlans.map { it.toEntity() })
            
            emit(Result.Success(remotePlans.map { it.toSitePlanData() }))
        } catch (e: Exception) {
            // If remote fails, try to get from cache
            val cachedPlans = dao.getAllSitePlans()
            if (cachedPlans.isNotEmpty()) {
                emit(Result.Success(cachedPlans.map { it.toSitePlanData() }))
            } else {
                emit(Result.Error(e.message ?: "Failed to load site plans"))
            }
        }
    }
    
    fun getSitePlanById(sitePlanId: String): Flow<Result<SitePlanData>> = flow {
        emit(Result.Loading)
        
        try {
            val sitePlan = dao.getSitePlanById(sitePlanId)
            if (sitePlan != null) {
                emit(Result.Success(sitePlan.toSitePlanData()))
            } else {
                // Try remote if not in cache
                val remoteSitePlan = apiService.getSitePlanById(sitePlanId)
                dao.insertSitePlan(remoteSitePlan.toEntity())
                emit(Result.Success(remoteSitePlan.toSitePlanData()))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to load site plan"))
        }
    }
    
    fun searchSitePlans(query: String): Flow<Result<List<SitePlanData>>> = flow {
        emit(Result.Loading)
        
        try {
            val searchResults = dao.searchSitePlans(query)
            emit(Result.Success(searchResults.map { it.toSitePlanData() }))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Search failed"))
        }
    }
    
    fun filterSitePlansByStatus(status: String): Flow<Result<List<SitePlanData>>> = flow {
        emit(Result.Loading)
        
        try {
            val filteredPlans = if (status == "ALL") {
                dao.getAllSitePlans()
            } else {
                dao.getSitePlansByStatus(status)
            }
            emit(Result.Success(filteredPlans.map { it.toSitePlanData() }))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Filter failed"))
        }
    }
    
    fun getBlocksBySitePlan(sitePlanId: String): Flow<Result<List<BlockData>>> = flow {
        emit(Result.Loading)
        
        try {
            val blocks = dao.getBlocksBySitePlan(sitePlanId)
            emit(Result.Success(blocks.map { it.toBlockData() }))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to load blocks"))
        }
    }
    
    fun getUnitsByBlock(blockId: String): Flow<Result<List<UnitData>>> = flow {
        emit(Result.Loading)
        
        try {
            val units = dao.getUnitsByBlock(blockId)
            emit(Result.Success(units.map { it.toUnitData() }))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to load units"))
        }
    }
    
    fun getUnitsNearCoordinates(latitude: Double, longitude: Double): Flow<Result<List<UnitData>>> = flow {
        emit(Result.Loading)
        
        try {
            val nearbyUnits = dao.getUnitsNearCoordinates(latitude, longitude)
            emit(Result.Success(nearbyUnits.map { it.toUnitData() }))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to find nearby units"))
        }
    }
    
    suspend fun bookUnit(unitId: String): Result<Unit> {
        return try {
            apiService.bookUnit(unitId)
            dao.updateUnitStatus(unitId, "BOOKED")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to book unit")
        }
    }
    
    suspend fun reserveUnit(unitId: String): Result<Unit> {
        return try {
            apiService.reserveUnit(unitId)
            dao.updateUnitStatus(unitId, "RESERVED")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to reserve unit")
        }
    }
    
    suspend fun getProjectStatistics(projectCode: String): Result<ProjectStatistics> {
        return try {
            val stats = apiService.getProjectStatistics(projectCode)
            Result.Success(stats)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load statistics")
        }
    }
    
    suspend fun getAvailableUnitsByProject(projectCode: String): Result<List<UnitData>> {
        return try {
            val units = apiService.getAvailableUnitsByProject(projectCode)
            Result.Success(units.map { it.toUnitData() })
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load available units")
        }
    }
    
    suspend fun searchUnits(
        projectCode: String? = null,
        unitType: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        bedrooms: Int? = null,
        status: String = "AVAILABLE"
    ): Result<List<UnitData>> {
        return try {
            val units = apiService.searchUnits(
                projectCode = projectCode,
                unitType = unitType,
                minPrice = minPrice,
                maxPrice = maxPrice,
                bedrooms = bedrooms,
                status = status
            )
            Result.Success(units.map { it.toUnitData() })
        } catch (e: Exception) {
            Result.Error(e.message ?: "Search failed")
        }
    }
}

// Data classes for API responses
data class ProjectStatistics(
    val totalUnits: Int,
    val availableUnits: Int,
    val bookedUnits: Int,
    val soldUnits: Int,
    val reservedUnits: Int,
    val avgPricePerMeter: Double,
    val minPrice: Double,
    val maxPrice: Double
)

// Extension functions for mapping
private fun SitePlanEntity.toSitePlanData(): SitePlanData {
    return SitePlanData(
        id = this.id,
        projectName = this.projectName,
        projectCode = this.projectCode,
        developerName = this.developerName,
        locationAddress = this.locationAddress,
        city = this.city,
        province = this.province,
        totalUnits = this.totalUnits,
        availableUnits = this.availableUnits,
        soldUnits = this.soldUnits,
        projectStatus = this.projectStatus,
        startDate = this.startDate,
        completionDate = this.completionDate,
        description = this.description ?: ""
    )
}

private fun BlockEntity.toBlockData(): BlockData {
    return BlockData(
        id = this.id,
        sitePlanId = this.sitePlanId,
        blockName = this.blockName,
        blockType = this.blockType,
        totalLots = this.totalLots,
        availableLots = this.availableLots,
        bookedLots = this.bookedLots,
        soldLots = this.soldLots,
        blockArea = this.blockArea,
        description = this.description ?: ""
    )
}

private fun UnitEntity.toUnitData(): UnitData {
    return UnitData(
        id = this.id,
        sitePlanId = this.sitePlanId,
        blockId = this.blockId,
        unitNumber = this.unitNumber,
        unitType = this.unitType,
        unitCategory = this.unitCategory,
        landArea = this.landArea,
        buildingArea = this.buildingArea,
        totalPrice = this.totalPrice,
        status = this.status,
        orientation = this.orientation,
        floorLevel = this.floorLevel,
        bedrooms = this.bedrooms,
        bathrooms = this.bathrooms,
        carParking = this.carParking,
        coordinates = this.latitude?.let { lat ->
            this.longitude?.let { lng -> Pair(lat, lng) }
        }
    )
}
