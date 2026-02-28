package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.components.QuorumVotingCard
import com.kprflow.enterprise.ui.components.VotingSessionCard
import com.kprflow.enterprise.ui.viewmodel.QuorumVotingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuorumVotingScreen(
    dossierId: String,
    onBackClick: () -> Unit,
    viewModel: QuorumVotingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionsState by viewModel.sessionsState.collectAsState()
    val eligibilityState by viewModel.eligibilityState.collectAsState()
    
    LaunchedEffect(dossierId) {
        viewModel.loadVotingSessions(dossierId)
        viewModel.checkVoterEligibility()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quorum Voting",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            when (eligibilityState) {
                is EligibilityState.Eligible -> {
                    Button(
                        onClick = { viewModel.showCreateVotingDialog(dossierId) }
                    ) {
                        Text("Create Voting")
                    }
                }
                is EligibilityState.NotEligible -> {
                    Text(
                        text = "Not Eligible to Vote",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Voting Sessions
        when (sessionsState) {
            is SessionsState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is SessionsState.Success -> {
                if (sessionsState.sessions.isEmpty()) {
                    EmptyVotingState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sessionsState.sessions) { session ->
                            VotingSessionCard(
                                session = session,
                                onVote = { sessionId, vote, comment ->
                                    viewModel.castVote(sessionId, vote, comment)
                                },
                                onViewResults = { sessionId ->
                                    viewModel.viewVotingResults(sessionId)
                                }
                            )
                        }
                    }
                }
            }
            
            is SessionsState.Error -> {
                ErrorState(
                    message = sessionsState.message,
                    onRetry = { viewModel.loadVotingSessions(dossierId) }
                )
            }
        }
    }
    
    // Create Voting Dialog
    if (uiState.showCreateVotingDialog) {
        CreateVotingDialog(
            onDismiss = { viewModel.hideCreateVotingDialog() },
            onCreate = { votingData ->
                viewModel.createVotingSession(dossierId, votingData)
            }
        )
    }
    
    // Cast Vote Dialog
    if (uiState.showCastVoteDialog) {
        CastVoteDialog(
            sessionId = uiState.selectedSessionId ?: "",
            onDismiss = { viewModel.hideCastVoteDialog() },
            onVote = { vote, comment ->
                viewModel.castVote(uiState.selectedSessionId ?: "", vote, comment)
            }
        )
    }
    
    // Voting Results Dialog
    if (uiState.showResultsDialog) {
        VotingResultsDialog(
            result = uiState.votingResults,
            onDismiss = { viewModel.hideResultsDialog() }
        )
    }
}

@Composable
private fun EmptyVotingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Voting Sessions",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Create a voting session to start the quorum process",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error loading voting sessions",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun CreateVotingDialog(
    onDismiss: () -> Unit,
    onCreate: (CreateVotingData) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var votingType by remember { mutableStateOf(com.kprflow.enterprise.data.repository.QuorumVotingType.DOCUMENT_APPROVAL) }
    var expiresHours by remember { mutableStateOf("24") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Voting Session")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Text("Voting Type:")
                com.kprflow.enterprise.data.repository.QuorumVotingType.values().forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = votingType == type,
                            onClick = { votingType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(type.name.replace("_", " "))
                    }
                }
                
                OutlinedTextField(
                    value = expiresHours,
                    onValueChange = { expiresHours = it },
                    label = { Text("Expires in (hours)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onCreate(
                            CreateVotingData(
                                title = title,
                                description = description,
                                votingType = votingType,
                                expiresHours = expiresHours.toIntOrNull() ?: 24
                            )
                        )
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank()
            ) {
                Text("Create")
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
private fun CastVoteDialog(
    sessionId: String,
    onDismiss: () -> Unit,
    onVote: (com.kprflow.enterprise.data.repository.QuorumVote, String?) -> Unit
) {
    var selectedVote by remember { mutableStateOf(com.kprflow.enterprise.data.repository.QuorumVote.APPROVE) }
    var comment by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Cast Your Vote")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select your vote:")
                
                com.kprflow.enterprise.data.repository.QuorumVote.values().forEach { vote ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedVote == vote,
                            onClick = { selectedVote = vote }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(vote.name)
                    }
                }
                
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onVote(selectedVote, comment.takeIf { it.isNotBlank() })
                }
            ) {
                Text("Vote")
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
private fun VotingResultsDialog(
    result: com.kprflow.enterprise.data.repository.QuorumVotingResult?,
    onDismiss: () -> Unit
) {
    result?.let {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Voting Results")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Decision: ${it.decision.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (it.decision) {
                            com.kprflow.enterprise.data.repository.QuorumVotingDecision.APPROVED -> MaterialTheme.colorScheme.primary
                            com.kprflow.enterprise.data.repository.QuorumVotingDecision.REJECTED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
                    
                    Text("Total Votes: ${it.totalVotes}")
                    Text("Approve: ${it.approveVotes}")
                    Text("Reject: ${it.rejectVotes}")
                    
                    if (it.concludedAt != null) {
                        Text("Concluded: ${formatDate(it.concludedAt)}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

// UI States
sealed class SessionsState {
    object Loading : SessionsState()
    data class Success(val sessions: List<com.kprflow.enterprise.data.repository.QuorumVotingSession>) : SessionsState()
    data class Error(val message: String) : SessionsState()
}

sealed class EligibilityState {
    object Loading : EligibilityState()
    object Eligible : EligibilityState()
    data class NotEligible(val reason: String) : EligibilityState()
    data class Error(val message: String) : EligibilityState()
}

sealed class QuorumVotingUiState {
    object Idle : QuorumVotingUiState()
    object Creating : QuorumVotingUiState()
    object Voting : QuorumVotingUiState()
    data class Success(val message: String) : QuorumVotingUiState()
    data class Error(val message: String) : QuorumVotingUiState()
    
    val showCreateVotingDialog: Boolean
        get() = this is ShowCreateVotingDialog
    
    val showCastVoteDialog: Boolean
        get() = this is ShowCastVoteDialog
    
    val showResultsDialog: Boolean
        get() = this is ShowResultsDialog
    
    val selectedSessionId: String?
        get() = when (this) {
            is ShowCastVoteDialog -> sessionId
            is ShowResultsDialog -> votingResults?.sessionId
            else -> null
        }
    
    val votingResults: com.kprflow.enterprise.data.repository.QuorumVotingResult?
        get() = (this as? ShowResultsDialog)?.result
}

object ShowCreateVotingDialog : QuorumVotingUiState()
data class ShowCastVoteDialog(val sessionId: String) : QuorumVotingUiState()
data class ShowResultsDialog(val result: com.kprflow.enterprise.data.repository.QuorumVotingResult) : QuorumVotingUiState()

data class CreateVotingData(
    val title: String,
    val description: String,
    val votingType: com.kprflow.enterprise.data.repository.QuorumVotingType,
    val expiresHours: Int
)

private fun formatDate(dateString: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        val date = sdf.parse(dateString)
        val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
        outputFormat.format(date ?: java.util.Date())
    } catch (e: Exception) {
        "Unknown date"
    }
}
