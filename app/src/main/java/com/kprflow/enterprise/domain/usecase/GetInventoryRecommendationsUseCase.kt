package com.kprflow.enterprise.domain.usecase

import com.kprflow.enterprise.domain.model.InventoryRecommendation
import com.kprflow.enterprise.domain.repository.AIRepository
import com.kprflow.enterprise.domain.repository.UnitRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetInventoryRecommendationsUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val unitRepository: UnitRepository
) {
    suspend operator fun invoke(
        customerPreferences: Map<String, Any> = emptyMap(),
        limit: Int = 10
    ): Result<List<InventoryRecommendation>> {
        return try {
            // Get available units
            val availableUnits = unitRepository.getAvailableUnits()
                .getOrNull() ?: emptyList()
            
            if (availableUnits.isEmpty()) {
                return Result.success(emptyList())
            }
            
            // Get AI recommendations
            val recommendations = aiRepository.getInventoryRecommendations(customerPreferences)
                .getOrNull() ?: emptyList()
            
            // Filter and sort recommendations
            val filteredRecommendations = recommendations
                .filter { it.recommendationType != com.kprflow.enterprise.domain.model.RecommendationType.NOT_RECOMMENDED }
                .sortedByDescending { it.score }
                .take(limit)
            
            Result.success(filteredRecommendations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRecommendationsForUnit(unitId: String): Result<InventoryRecommendation?> {
        return try {
            val recommendation = aiRepository.generateUnitRecommendation(unitId)
            recommendation
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getHighPriorityRecommendations(
        customerPreferences: Map<String, Any> = emptyMap()
    ): Result<List<InventoryRecommendation>> {
        return try {
            val allRecommendations = invoke(customerPreferences, 50)
                .getOrNull() ?: emptyList()
            
            val highPriorityRecommendations = allRecommendations.filter { 
                it.priority == com.kprflow.enterprise.domain.model.RecommendationPriority.CRITICAL ||
                it.priority == com.kprflow.enterprise.domain.model.RecommendationPriority.HIGH
            }
            
            Result.success(highPriorityRecommendations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRecommendationsByType(
        recommendationType: com.kprflow.enterprise.domain.model.RecommendationType,
        customerPreferences: Map<String, Any> = emptyMap()
    ): Result<List<InventoryRecommendation>> {
        return try {
            val allRecommendations = invoke(customerPreferences, 100)
                .getOrNull() ?: emptyList()
            
            val filteredRecommendations = allRecommendations.filter { 
                it.recommendationType == recommendationType
            }
            
            Result.success(filteredRecommendations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
