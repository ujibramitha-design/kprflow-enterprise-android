package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kprflow.enterprise.data.repository.QuorumVotingSession
import com.kprflow.enterprise.data.repository.QuorumVotingStatus
import com.kprflow.enterprise.data.repository.QuorumVote
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingSessionCard(
    session: QuorumVotingSession,
    onVote: (String, QuorumVote, String?) -> Unit,
    onViewResults: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with session info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Type: ${session.votingType.name.replace("_", " ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Badge
                Badge(
                    containerColor = getStatusColor(session.status)
                ) {
                    Text(
                        text = session.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            if (session.description.isNotBlank()) {
                Text(
                    text = session.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Session Details
            SessionInfoRow(
                label = "Created",
                value = formatDate(session.createdAt)
            )
            
            session.expiresAt?.let { expiry ->
                SessionInfoRow(
                    label = "Expires",
                    value = formatDate(expiry)
                )
            }
            
            session.concludedAt?.let { concluded ->
                SessionInfoRow(
                    label = "Concluded",
                    value = formatDate(concluded)
                )
                
                session.conclusionReason?.let { reason ->
                    SessionInfoRow(
                        label = "Reason",
                        value = reason
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (session.status) {
                    QuorumVotingStatus.ACTIVE -> {
                        Button(
                            onClick = { onVote(session.id, QuorumVote.APPROVE, null) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Approve")
                        }
                        
                        OutlinedButton(
                            onClick = { onVote(session.id, QuorumVote.REJECT, null) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Reject")
                        }
                    }
                    
                    QuorumVotingStatus.CONCLUDED -> {
                        OutlinedButton(
                            onClick = { onViewResults(session.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View Results")
                        }
                    }
                    
                    QuorumVotingStatus.CANCELLED -> {
                        Text(
                            text = "Cancelled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    QuorumVotingStatus.EXPIRED -> {
                        Text(
                            text = "Expired",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun QuorumVotingCard(
    session: QuorumVotingSession,
    votingResult: com.kprflow.enterprise.data.repository.QuorumVotingResult?,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = session.votingType.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Badge(
                    containerColor = getStatusColor(session.status)
                ) {
                    Text(
                        text = session.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Voting Results
            votingResult?.let { result ->
                VotingResultsSection(result)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Session Info
            SessionInfoRow(
                label = "Total Votes",
                value = votingResult?.totalVotes?.toString() ?: "0"
            )
            
            votingResult?.concludedAt?.let { concluded ->
                SessionInfoRow(
                    label = "Concluded",
                    value = formatDate(concluded)
                )
            }
        }
    }
}

@Composable
private fun VotingResultsSection(
    result: com.kprflow.enterprise.data.repository.QuorumVotingResult
) {
    Column {
        // Decision
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Decision:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = result.decision.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = getDecisionColor(result.decision)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Vote Counts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VoteCountItem(
                label = "Approve",
                count = result.approveVotes,
                color = MaterialTheme.colorScheme.primary
            )
            
            VoteCountItem(
                label = "Reject",
                count = result.rejectVotes,
                color = MaterialTheme.colorScheme.error
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Progress Bar
        LinearProgressIndicator(
            progress = { 
                if (result.totalVotes > 0) {
                    result.approveVotes.toFloat() / result.totalVotes
                } else 0f
            },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun VoteCountItem(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getStatusColor(status: QuorumVotingStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        QuorumVotingStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        QuorumVotingStatus.CONCLUDED -> MaterialTheme.colorScheme.secondary
        QuorumVotingStatus.CANCELLED -> MaterialTheme.colorScheme.error
        QuorumVotingStatus.EXPIRED -> MaterialTheme.colorScheme.surfaceVariant
    }
}

private fun getDecisionColor(decision: com.kprflow.enterprise.data.repository.QuorumVotingDecision): androidx.compose.ui.graphics.Color {
    return when (decision) {
        com.kprflow.enterprise.data.repository.QuorumVotingDecision.APPROVED -> MaterialTheme.colorScheme.primary
        com.kprflow.enterprise.data.repository.QuorumVotingDecision.REJECTED -> MaterialTheme.colorScheme.error
        com.kprflow.enterprise.data.repository.QuorumVotingDecision.PENDING -> MaterialTheme.colorScheme.secondary
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date = sdf.parse(dateString)
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        "Unknown date"
    }
}
