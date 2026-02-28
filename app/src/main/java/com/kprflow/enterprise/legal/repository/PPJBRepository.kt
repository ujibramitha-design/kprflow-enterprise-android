package com.kprflow.enterprise.legal.repository

import com.kprflow.enterprise.legal.model.*
import kotlinx.coroutines.flow.Flow

/**
 * PPJB Repository Interface - Clean Architecture
 * Phase 16: Legal & Documentation Automation
 */
interface PPJBRepository {
    
    /**
     * Get all PPJB processes with filtering
     */
    fun getPPJBProcesses(filter: PPJBFilter = PPJBFilter.ALL): Flow<List<PPJBDeveloperProcess>>
    
    /**
     * Get PPJB process by ID
     */
    suspend fun getPPJBProcessById(id: String): PPJBDeveloperProcess?
    
    /**
     * Create new PPJB process
     */
    suspend fun createPPJBProcess(process: PPJBDeveloperProcess): PPJBDeveloperProcess
    
    /**
     * Update PPJB process
     */
    suspend fun updatePPJBProcess(process: PPJBDeveloperProcess): PPJBDeveloperProcess
    
    /**
     * Update PPJB status
     */
    suspend fun updatePPJBStatus(id: String, status: PPJBStatus): Unit
    
    /**
     * Delete PPJB process
     */
    suspend fun deletePPJBProcess(id: String): Unit
    
    /**
     * Get expired PPJB processes
     */
    suspend fun getExpiredPPJBProcesses(): List<PPJBDeveloperProcess>
    
    /**
     * Get PPJB processes by customer
     */
    fun getPPJBProcessesByCustomerId(customerId: String): Flow<List<PPJBDeveloperProcess>>
    
    /**
     * Get PPJB processes by dossier
     */
    fun getPPJBProcessesByDossierId(dossierId: String): Flow<List<PPJBDeveloperProcess>>
    
    /**
     * Get PPJB statistics
     */
    fun getPPJBStatistics(): Flow<PPJBStatistics>
    
    /**
     * Get PPJB processes by date range
     */
    fun getPPJBProcessesByDateRange(startDate: Long, endDate: Long): Flow<List<PPJBDeveloperProcess>>
    
    /**
     * Search PPJB processes
     */
    suspend fun searchPPJBProcesses(query: String): List<PPJBDeveloperProcess>
    
    /**
     * Get PPJB documents
     */
    suspend fun getPPJBDocuments(ppjbId: String): List<PPJBDocument>
    
    /**
     * Save PPJB document
     */
    suspend fun savePPJBDocument(document: PPJBDocument): PPJBDocument
    
    /**
     * Get PPJB reminders
     */
    suspend fun getPPJBReminders(ppjbId: String): List<PPJBReminder>
    
    /**
     * Save PPJB reminder
     */
    suspend fun savePPJBReminder(reminder: PPJBReminder): PPJBReminder
    
    /**
     * Get PPJB invitations
     */
    suspend fun getPPJBInvitations(ppjbId: String): List<PPJBInvitation>
    
    /**
     * Save PPJB invitation
     */
    suspend fun savePPJBInvitation(invitation: PPJBInvitation): PPJBInvitation
    
    /**
     * Log PPJB audit
     */
    suspend fun logPPJBAudit(audit: PPJBAuditLog): Unit
}
