package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.viewmodel.CustomerDashboardViewModel
import com.kprflow.enterprise.ui.components.StatusBadge
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

/**
 * Customer Dashboard with Firebase Crashlytics Test Button
 * Enhanced version of CustomerDashboard with crash testing capability
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardWithCrashTest(
    viewModel: CustomerDashboardViewModel = hiltViewModel(),
    crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
    onNavigateToCrashTest: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🏠 Customer Dashboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Manage your KPR applications and track progress",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Firebase Status
                StatusBadge(
                    status = "Firebase Connected",
                    isActive = true,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        // Quick Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🚀 Quick Actions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* Navigate to new application */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("New Application")
                    }
                    
                    Button(
                        onClick = { /* View applications */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("My Applications")
                    }
                }
            }
        }
        
        // Firebase Crashlytics Test Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔥 Firebase Crashlytics Test",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "Test Firebase Crashlytics integration by triggering a test crash",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Test Crash Button
                Button(
                    onClick = {
                        try {
                            // Log custom event before crash
                            crashlytics.setCustomKey("test_screen", "CustomerDashboard")
                            crashlytics.setCustomKey("test_action", "crash_test_button")
                            crashlytics.setCustomKey("user_role", "customer")
                            
                            // Log analytics event
                            crashlytics.log("Test crash triggered from Customer Dashboard")
                            
                            // Trigger test crash
                            throw RuntimeException("Test Crash KPRFlow - Customer Dashboard")
                        } catch (e: Exception) {
                            // This shouldn't be reached if the crash occurs
                            Timber.e(e, "Failed to trigger test crash")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("🚨 Test Crash (App Will Crash)")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Navigate to Crash Test Screen
                OutlinedButton(
                    onClick = onNavigateToCrashTest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔥 Open Crash Test Screen")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "⚠️ Warning: The crash test will close the app. Use for testing Firebase Crashlytics integration only.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        // Recent Applications (Placeholder)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📋 Recent Applications",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Placeholder content
                Text(
                    text = "No recent applications found. Start a new application to see your progress here.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Progress Overview (Placeholder)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📊 Progress Overview",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Placeholder progress indicators
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProgressItem("Application Status", "In Progress", 0.6f)
                    ProgressItem("Document Upload", "Completed", 1.0f)
                    ProgressItem("Verification", "Pending", 0.3f)
                }
            }
        }
    }
}

@Composable
private fun ProgressItem(
    title: String,
    status: String,
    progress: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = status,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
