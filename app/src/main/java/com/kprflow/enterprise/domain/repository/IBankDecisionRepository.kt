package com.kprflow.enterprise.domain.repository

import com.kprflow.enterprise.data.model.BankDecision
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Bank Decision Repository
 * Following Clean Architecture principles for testability
 */
interface IBankDecisionRepository {
    suspend fun submitForBankDecision(dossierId: String): Result<Unit>
    suspend fun getBankDecisions(dossierId: String): Result<List<BankDecision>>
    suspend fun updateBankDecision(decisionId: String, decision: BankDecision): Result<BankDecision>
    suspend fun getPendingBankDecisions(): Result<List<BankDecision>>
    fun observeBankDecisionUpdates(dossierId: String): Flow<BankDecision>
    suspend fun getDecisionStatistics(): Result<Map<String, Int>>
}
