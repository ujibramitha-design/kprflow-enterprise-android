package com.kprflow.enterprise.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * User Acceptance Testing (UAT) Screen
 * Phase System Integration: UAT Implementation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UATScreen(
    uatViewModel: UATViewModel = hiltViewModel(),
    onUATComplete: () -> Unit
) {
    val uiState by uatViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    // Auto-start UAT when screen loads
    LaunchedEffect(Unit) {
        uatViewModel.startUAT()
    }
    
    // Handle UAT completion
    LaunchedEffect(uiState.uatStatus) {
        if (uiState.uatStatus == "COMPLETED") {
            onUATComplete()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            UATHeader(
                title = "User Acceptance Testing (UAT)",
                subtitle = "Testing system readiness with real user scenarios",
                onStartUAT = { scope.launch { uatViewModel.startUAT() } }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // UAT Progress
            UATProgressSection(
                progress = uiState.progress,
                currentTest = uiState.currentTest,
                totalTests = uiState.totalTests,
                completedTests = uiState.completedTests
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // UAT Results
            UATResultsSection(
                uatResults = uiState.uatResults,
                isLoading = uiState.isLoading
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Department Feedback
            DepartmentFeedbackSection(
                feedbackData = uiState.departmentFeedback
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            ActionButtonsSection(
                isLoading = uiState.isLoading,
                uatStatus = uiState.uatStatus,
                onRetry = { scope.launch { uatViewModel.startUAT() } },
                onExport = { scope.launch { uatViewModel.exportUATReport() } },
                onComplete = onUATComplete
            )
        }
    }
}

/**
 * UAT Header
 */
@Composable
private fun UATHeader(
    title: String,
    subtitle: String,
    onStartUAT: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF64748B)
                    )
                }
                
                IconButton(
                    onClick = onStartUAT,
                    modifier = Modifier
                        .background(
                            Color(0xFFF1F5F9),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start UAT",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // UAT Status
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = "UAT",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = "5 Departments | 8 Test Scenarios | Real User Validation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

/**
 * UAT Progress Section
 */
@Composable
private fun UATProgressSection(
    progress: Double,
    currentTest: String,
    totalTests: Int,
    completedTests: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "UAT Progress",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Test $completedTests of $totalTests",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                    
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFFF1F5F9),
                    strokeCap = StrokeCap.Round
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current Test
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF10B981),
                    strokeWidth = 3.dp
                )
                
                Text(
                    text = currentTest,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * UAT Results Section
 */
@Composable
private fun UATResultsSection(
    uatResults: List<UATResult>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "UAT Results",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                LoadingUATResults()
            } else if (uatResults.isNotEmpty()) {
                UATResultsList(uatResults)
            } else {
                NoUATResults()
            }
        }
    }
}

/**
 * Loading UAT Results
 */
@Composable
private fun LoadingUATResults() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Color(0xFF10B981),
            strokeWidth = 4.dp
        )
        
        Text(
            text = "Running UAT tests...",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Testing system with real user scenarios",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * UAT Results List
 */
@Composable
private fun UATResultsList(uatResults: List<UATResult>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uatResults) { result ->
            UATResultItem(result = result)
        }
    }
}

/**
 * UAT Result Item
 */
@Composable
private fun UATResultItem(result: UATResult) {
    val (statusColor, statusText) = when {
        result.status == "PASSED" -> Pair(Color(0xFF10B981), "PASSED")
        result.status == "FAILED" -> Pair(Color(0xFFEF4444), "FAILED")
        else -> Pair(Color(0xFFF59E0B), "PENDING")
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = result.testName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = result.department,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    
                    if (result.feedback.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = result.feedback,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                
                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Test Details
            if (result.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    result.details.forEach { detail ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = detail.key,
                                tint = if (detail.value) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Text(
                                text = detail.key,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * No UAT Results
 */
@Composable
private fun NoUATResults() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Assessment,
            contentDescription = "No UAT Results",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(48.dp)
        )
        
        Text(
            text = "No UAT results yet",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Start UAT to see results",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Department Feedback Section
 */
@Composable
private fun DepartmentFeedbackSection(
    feedbackData: Map<String, DepartmentFeedback>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Department Feedback",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            feedbackData.forEach { (department, feedback) ->
                DepartmentFeedbackItem(
                    department = department,
                    feedback = feedback
                )
                
                if (department != feedbackData.keys.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = Color(0xFFE2E8F0),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * Department Feedback Item
 */
@Composable
private fun DepartmentFeedbackItem(
    department: String,
    feedback: DepartmentFeedback
) {
    val (ratingColor, ratingText) = when {
        feedback.rating >= 4.5 -> Pair(Color(0xFF10B981), "Excellent")
        feedback.rating >= 4.0 -> Pair(Color(0xFF3B82F6), "Good")
        feedback.rating >= 3.5 -> Pair(Color(0xFFF59E0B), "Fair")
        else -> Pair(Color(0xFFEF4444), "Poor")
    }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = department,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Medium
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = String.format("%.1f", feedback.rating),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ratingColor,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    color = ratingColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = ratingText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = ratingColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Rating Stars
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..5) {
                Icon(
                    imageVector = if (i <= feedback.rating) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = "Star $i",
                    tint = if (i <= feedback.rating) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Feedback Comments
        feedback.comments.forEach { comment ->
            Text(
                text = "• $comment",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Action Buttons Section
 */
@Composable
private fun ActionButtonsSection(
    isLoading: Boolean,
    uatStatus: String,
    onRetry: () -> Unit,
    onExport: () -> Unit,
    onComplete: () -> Unit
) {
    val isComplete = uatStatus == "COMPLETED"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Retry Button
        Button(
            onClick = onRetry,
            modifier = Modifier.weight(1f),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry UAT")
            }
        }
        
        // Export Button
        Button(
            onClick = onExport,
            modifier = Modifier.weight(1f),
            enabled = uatStatus.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Export",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Report")
        }
        
        // Complete Button
        Button(
            onClick = onComplete,
            modifier = Modifier.weight(1f),
            enabled = isComplete,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B5CF6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Complete",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Complete")
        }
    }
}

// Data classes
data class UATResult(
    val testName: String,
    val department: String,
    val status: String,
    val feedback: String,
    val details: Map<String, Boolean>
)

data class DepartmentFeedback(
    val rating: Double,
    val comments: List<String>
)
