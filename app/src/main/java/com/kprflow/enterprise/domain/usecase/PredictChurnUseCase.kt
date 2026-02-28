package com.kprflow.enterprise.domain.usecase

import com.kprflow.enterprise.domain.model.ChurnPrediction
import com.kprflow.enterprise.domain.repository.AIRepository
import com.kprflow.enterprise.domain.repository.KprRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictChurnUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val kprRepository: KprRepository
) {
    suspend operator fun invoke(dossierId: String): Result<ChurnPrediction> {
        return try {
            // Get current prediction if exists
            val existingPrediction = aiRepository.predictChurn(dossierId)
            
            // Check if prediction is still valid (less than 24 hours old)
            if (existingPrediction.isSuccess) {
                val prediction = existingPrediction.getOrNull()
                if (prediction != null && isPredictionValid(prediction.predictedAt)) {
                    return Result.success(prediction)
                }
            }
            
            // Generate new prediction
            val newPrediction = aiRepository.updateChurnPrediction(dossierId)
            
            if (newPrediction.isSuccess) {
                // Log prediction for analytics
                logPredictionEvent(newPrediction.getOrNull())
            }
            
            newPrediction
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun predictBatch(dossierIds: List<String>): Result<List<ChurnPrediction>> {
        return try {
            val predictions = mutableListOf<ChurnPrediction>()
            
            for (dossierId in dossierIds) {
                val prediction = invoke(dossierId)
                if (prediction.isSuccess) {
                    predictions.add(prediction.getOrNull()!!)
                }
            }
            
            Result.success(predictions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun isPredictionValid(predictedAt: Long): Boolean {
        val twentyFourHours = 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - predictedAt) < twentyFourHours
    }
    
    private fun logPredictionEvent(prediction: ChurnPrediction?) {
        // Log prediction for analytics and model improvement
        // This would be implemented with proper logging
    }
}
