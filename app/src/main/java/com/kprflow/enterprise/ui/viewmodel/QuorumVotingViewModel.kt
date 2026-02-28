package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.QuorumVotingRepository
import com.kprflow.enterprise.data.repository.QuorumVotingSession
import com.kprflow.enterprise.data.repository.QuorumVotingResult
import com.kprflow.enterprise.data.repository.QuorumVote
import com.kprflow.enterprise.ui.screens.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuorumVotingViewModel @Inject constructor(
    private val quorumVotingRepository: QuorumVotingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<QuorumVotingUiState>(QuorumVotingUiState.Idle)
    val uiState: StateFlow<QuorumVotingUiState> = _uiState.asStateFlow()
    
    private val _sessionsState = MutableStateFlow<SessionsState>(SessionsState.Loading)
    val sessionsState: StateFlow<SessionsState> = _sessionsState.asStateFlow()
    
    private val _eligibilityState = MutableStateFlow<EligibilityState>(EligibilityState.Loading)
    val eligibilityState: StateFlow<EligibilityState> = _eligibilityState.asStateState()
    
    private val _currentUserId = MutableStateFlow<String?>(null)
    
    init {
        // TODO: Get current user from AuthRepository
        _currentUserId.value = "current-legal-user-id" // Placeholder
    }
    
    fun loadVotingSessions(dossierId: String) {
        viewModelScope.launch {
            _sessionsState.value = SessionsState.Loading
            
            try {
                val sessions = quorumVotingRepository.getVotingSessions(dossierId)
                    .getOrNull().orEmpty()
                _sessionsState.value = SessionsState.Success(sessions)
            } catch (e: Exception) {
                _sessionsState.value = SessionsState.Error(e.message ?: "Failed to load voting sessions")
            }
        }
    }
    
    fun checkVoterEligibility() {
        viewModelScope.launch {
            _eligibilityState.value = EligibilityState.Loading
            
            val userId = _currentUserId.value ?: return@launch
            
            try {
                val eligibility = quorumVotingRepository.getVoterEligibility(userId)
                    .getOrNull()
                
                if (eligibility != null) {
                    if (eligibility.isEligible) {
                        _eligibilityState.value = EligibilityState.Eligible
                    } else {
                        _eligibilityState.value = EligibilityState.NotEligible(
                            "Role: ${eligibility.role}, Active: ${eligibility.isActive}"
                        )
                    }
                } else {
                    _eligibilityState.value = EligibilityState.Error("Failed to check eligibility")
                }
            } catch (e: Exception) {
                _eligibilityState.value = EligibilityState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun createVotingSession(dossierId: String, votingData: CreateVotingData) {
        viewModelScope.launch {
            try {
                _uiState.value = QuorumVotingUiState.Creating
                
                val userId = _currentUserId.value ?: return@launch
                
                val expiresAt = if (votingData.expiresHours > 0) {
                    java.time.Instant.now().plusSeconds(votingData.expiresHours.toLong() * 3600)
                } else null
                
                val result = quorumVotingRepository.createVotingSession(
                    dossierId = dossierId,
                    votingType = votingData.votingType,
                    title = votingData.title,
                    description = votingData.description,
                    createdBy = userId,
                    expiresAt = expiresAt
                )
                
                if (result.isSuccess) {
                    _uiState.value = QuorumVotingUiState.Success("Voting session created successfully")
                    hideCreateVotingDialog()
                    loadVotingSessions(dossierId)
                } else {
                    _uiState.value = QuorumVotingUiState.Error("Failed to create voting session")
                }
            } catch (e: Exception) {
                _uiState.value = QuorumVotingUiState.Error(e.message ?: "Failed to create voting session")
            }
        }
    }
    
    fun castVote(sessionId: String, vote: QuorumVote, comment: String?) {
        viewModelScope.launch {
            try {
                _uiState.value = QuorumVotingUiState.Voting
                
                val userId = _currentUserId.value ?: return@launch
                
                val result = quorumVotingRepository.castVote(
                    sessionId = sessionId,
                    voterId = userId,
                    vote = vote,
                    comment = comment
                )
                
                if (result.isSuccess) {
                    _uiState.value = QuorumVotingUiState.Success("Vote cast successfully")
                    hideCastVoteDialog()
                    
                    // Check if voting concluded
                    val votingResult = quorumVotingRepository.getVotingResults(sessionId)
                        .getOrNull()
                    
                    if (votingResult?.decision != com.kprflow.enterprise.data.repository.QuorumVotingDecision.PENDING) {
                        showResultsDialog(votingResult)
                    }
                } else {
                    _uiState.value = QuorumVotingUiState.Error("Failed to cast vote")
                }
            } catch (e: Exception) {
                _uiState.value = QuorumVotingUiState.Error(e.message ?: "Failed to cast vote")
            }
        }
    }
    
    fun viewVotingResults(sessionId: String) {
        viewModelScope.launch {
            try {
                val result = quorumVotingRepository.getVotingResults(sessionId)
                    .getOrNull()
                
                if (result != null) {
                    showResultsDialog(result)
                } else {
                    _uiState.value = QuorumVotingUiState.Error("Failed to load voting results")
                }
            } catch (e: Exception) {
                _uiState.value = QuorumVotingUiState.Error(e.message ?: "Failed to load voting results")
            }
        }
    }
    
    fun showCreateVotingDialog(dossierId: String) {
        _uiState.value = ShowCreateVotingDialog
    }
    
    fun hideCreateVotingDialog() {
        _uiState.value = QuorumVotingUiState.Idle
    }
    
    fun showCastVoteDialog(sessionId: String) {
        _uiState.value = ShowCastVoteDialog(sessionId)
    }
    
    fun hideCastVoteDialog() {
        _uiState.value = QuorumVotingUiState.Idle
    }
    
    fun showResultsDialog(result: QuorumVotingResult) {
        _uiState.value = ShowResultsDialog(result)
    }
    
    fun hideResultsDialog() {
        _uiState.value = QuorumVotingUiState.Idle
    }
    
    fun concludeVotingSession(sessionId: String, reason: String?) {
        viewModelScope.launch {
            try {
                val userId = _currentUserId.value ?: return@launch
                
                val result = quorumVotingRepository.concludeVotingSession(
                    sessionId = sessionId,
                    concludedBy = userId,
                    reason = reason
                )
                
                if (result.isSuccess) {
                    _uiState.value = QuorumVotingUiState.Success("Voting session concluded")
                    // Refresh sessions
                    // TODO: Get current dossier ID and refresh
                } else {
                    _uiState.value = QuorumVotingUiState.Error("Failed to conclude voting session")
                }
            } catch (e: Exception) {
                _uiState.value = QuorumVotingUiState.Error(e.message ?: "Failed to conclude voting session")
            }
        }
    }
    
    fun cancelVotingSession(sessionId: String, reason: String?) {
        viewModelScope.launch {
            try {
                val userId = _currentUserId.value ?: return@launch
                
                val result = quorumVotingRepository.cancelVotingSession(
                    sessionId = sessionId,
                    cancelledBy = userId,
                    reason = reason
                )
                
                if (result.isSuccess) {
                    _uiState.value = QuorumVotingUiState.Success("Voting session cancelled")
                    // Refresh sessions
                    // TODO: Get current dossier ID and refresh
                } else {
                    _uiState.value = QuorumVotingUiState.Error("Failed to cancel voting session")
                }
            } catch (e: Exception) {
                _uiState.value = QuorumVotingUiState.Error(e.message ?: "Failed to cancel voting session")
            }
        }
    }
    
    fun refreshVotingSessions(dossierId: String) {
        loadVotingSessions(dossierId)
    }
    
    fun clearState() {
        _uiState.value = QuorumVotingUiState.Idle
    }
}
