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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Data Migration Screen - Real Data Implementation
 * Phase Data Migration: Real Data Implementation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataMigrationScreen(
    migrationViewModel: DataMigrationViewModel = hiltViewModel(),
    onMigrationComplete: () -> Unit
) {
    val uiState by migrationViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    // Auto-start migration when screen loads
    LaunchedEffect(Unit) {
        migrationViewModel.startDataMigration()
    }
    
    // Handle migration completion
    LaunchedEffect(uiState.migrationResult) {
        if (uiState.migrationResult != null && uiState.migrationResult?.status == "SUCCESS") {
            onMigrationComplete()
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
            MigrationHeader(
                title = "Data Migration & Seeding",
                subtitle = "Mengisi Data Riil untuk Aplikasi ERP",
                onRefresh = { scope.launch { migrationViewModel.startDataMigration() } }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Migration Progress
            MigrationProgressSection(
                progress = uiState.progress,
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
                completedSteps = uiState.completedSteps
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Migration Results
            MigrationResultsSection(
                migrationResult = uiState.migrationResult,
                isLoading = uiState.isLoading
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Data Summary
            DataSummarySection(
                migrationResult = uiState.migrationResult
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            ActionButtonsSection(
                isLoading = uiState.isLoading,
                migrationResult = uiState.migrationResult,
                onRetry = { scope.launch { migrationViewModel.startDataMigration() } },
                onExport = { scope.launch { migrationViewModel.exportMigrationReport() } },
                onComplete = onMigrationComplete
            )
        }
    }
}

/**
 * Migration Header
 */
@Composable
private fun MigrationHeader(
    title: String,
    subtitle: String,
    onRefresh: () -> Unit
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
                    onClick = onRefresh,
                    modifier = Modifier
                        .background(
                            Color(0xFFF1F5F9),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Migration Status
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Data Migration",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = "Master Data Unit: 25 Units | Historical KPR: 8 Applications | Documents: 15 Files",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

/**
 * Migration Progress Section
 */
@Composable
private fun MigrationProgressSection(
    progress: Double,
    currentStep: String,
    totalSteps: Int,
    completedSteps: Int
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
                text = "Migration Progress",
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
                        text = "Step $completedSteps of $totalSteps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                    
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF3B82F6),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFFF1F5F9),
                    strokeCap = StrokeCap.Round
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current Step
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF3B82F6),
                    strokeWidth = 3.dp
                )
                
                Text(
                    text = currentStep,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Migration Results Section
 */
@Composable
private fun MigrationResultsSection(
    migrationResult: MigrationResult?,
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
                text = "Migration Results",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                LoadingMigrationResults()
            } else if (migrationResult != null) {
                MigrationResultsList(migrationResult)
            } else {
                NoMigrationResults()
            }
        }
    }
}

/**
 * Loading Migration Results
 */
@Composable
private fun LoadingMigrationResults() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Color(0xFF3B82F6),
            strokeWidth = 4.dp
        )
        
        Text(
            text = "Processing migration...",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "This may take a few moments to complete",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Migration Results List
 */
@Composable
private fun MigrationResultsList(migrationResult: MigrationResult) {
    val results = listOf(
        Triple("Master Data Unit", migrationResult.unitMigration, Icons.Default.Home),
        Triple("User Profiles", migrationResult.userMigration, Icons.Default.Person),
        Triple("Historical KPR Data", migrationResult.kprMigration, Icons.Default.Assignment),
        Triple("Financial Transactions", migrationResult.financialMigration, Icons.Default.AccountBalance),
        Triple("Documents", migrationResult.documentMigration, Icons.Default.Description),
        Triple("Audit Logs", migrationResult.auditMigration, Icons.Default.History),
        Triple("Notifications", migrationResult.notificationMigration, Icons.Default.Notifications),
        Triple("Performance Metrics", migrationResult.metricsMigration, Icons.Default.Analytics),
        Triple("Storage Configuration", migrationResult.storageConfiguration, Icons.Default.Cloud),
        Triple("System Configuration", migrationResult.systemConfiguration, Icons.Default.Settings)
    )
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(results) { (title, result, icon) ->
            MigrationResultItem(
                title = title,
                result = result,
                icon = icon
            )
        }
    }
}

/**
 * Migration Result Item
 */
@Composable
private fun MigrationResultItem(
    title: String,
    result: Any?,
    icon: ImageVector
) {
    val (status, statusColor) = when {
        result == null -> Pair("PENDING", Color(0xFFF59E0B))
        result is UnitMigrationResult && result.status == "SUCCESS" -> Pair("SUCCESS", Color(0xFF10B981))
        result is UserMigrationResult && result.status == "SUCCESS" -> Pair("SUCCESS", Color(0xFF10B981))
        result is KPRMigrationResult && result.status == "SUCCESS" -> Pair("SUCCESS", Color(0xFF10B981))
        result is FinancialMigrationResult && result.status == "SUCCESS" -> Pair("SUCCESS", Color(0xFF10B981))
        result is DocumentMigrationResult && result.status == "SUCCESS" -> Pair("SUCCESS", Color(0xFF10B981))
        result is AuditMigrationResult && result.status == "SUCCESS" -> Pair("SUCCESS", Color(0xFF10B981))
        result is NotificationMigrationResult && result.status == "SUCCESS" -> Pair("SUCCESS", Color(0xFF10B981))
        result is MetricsMigrationResult && result.status == "SUCCESS" -> Pair("SUCCESS", Color(0xFF10B981))
        result is StorageConfigurationResult && result.status == "SUCCESS" -> Pair("SUCCESS", Color(0xFF10B981))
        result is SystemConfigurationResult && result.status == "SUCCESS" -> Pair("SUCCESS", Color(0xFF10B981))
        else -> Pair("ERROR", Color(0xFFEF4444))
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Add details based on result type
                    when (result) {
                        is UnitMigrationResult -> {
                            Text(
                                text = "${result.successCount}/${result.totalUnits} units",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        is UserMigrationResult -> {
                            Text(
                                text = "${result.successCount}/${result.totalUsers} users",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        is KPRMigrationResult -> {
                            Text(
                                text = "${result.successCount}/${result.totalDossiers} dossiers",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        is FinancialMigrationResult -> {
                            Text(
                                text = "${result.successCount}/${result.totalTransactions} transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        is DocumentMigrationResult -> {
                            Text(
                                text = "${result.successCount}/${result.totalDocuments} documents",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        else -> {
                            Text(
                                text = "Processing...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
            
            // Status Badge
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * No Migration Results
 */
@Composable
private fun NoMigrationResults() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = "No Data",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(48.dp)
        )
        
        Text(
            text = "No migration results yet",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Start the migration to see results",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Data Summary Section
 */
@Composable
private fun DataSummarySection(
    migrationResult: MigrationResult?
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
                text = "Data Summary",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Summary Grid
            val summaryItems = listOf(
                Triple("Total Units", migrationResult?.unitMigration?.totalUnits ?: 0, Icons.Default.Home),
                Triple("Total Users", migrationResult?.userMigration?.totalUsers ?: 0, Icons.Default.Person),
                Triple("KPR Dossiers", migrationResult?.kprMigration?.totalDossiers ?: 0, Icons.Default.Assignment),
                Triple("Transactions", migrationResult?.financialMigration?.totalTransactions ?: 0, Icons.Default.AccountBalance),
                Triple("Documents", migrationResult?.documentMigration?.totalDocuments ?: 0, Icons.Default.Description),
                Triple("Audit Logs", migrationResult?.auditMigration?.totalLogs ?: 0, Icons.Default.History)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(summaryItems.chunked(2)) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { (title, count, icon) ->
                            DataSummaryItem(
                                title = title,
                                count = count,
                                icon = icon,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data Summary Item
 */
@Composable
private fun DataSummaryItem(
    title: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
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
    migrationResult: MigrationResult?,
    onRetry: () -> Unit,
    onExport: () -> Unit,
    onComplete: () -> Unit
) {
    val isComplete = migrationResult != null && migrationResult.status == "SUCCESS"
    
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
                Text("Retry Migration")
            }
        }
        
        // Export Button
        Button(
            onClick = onExport,
            modifier = Modifier.weight(1f),
            enabled = migrationResult != null,
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
