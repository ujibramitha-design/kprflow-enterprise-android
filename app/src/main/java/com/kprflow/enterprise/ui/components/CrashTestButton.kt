package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Crash Test Button Component
 * Provides a floating action button to access crash test screen
 */
@Composable
fun CrashTestButton(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var showCrashTestDialog by remember { mutableStateOf(false) }
    
    // Floating Action Button for Crash Test
    FloatingActionButton(
        onClick = { showCrashTestDialog = true },
        modifier = modifier
    ) {
        Text(
            text = "🔥",
            fontSize = 20.sp
        )
    }
    
    // Confirmation Dialog
    if (showCrashTestDialog) {
        AlertDialog(
            onDismissRequest = { showCrashTestDialog = false },
            title = {
                Text(
                    text = "🔥 Firebase Crashlytics Test",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "This will open the Firebase Crashlytics test screen where you can:",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "• Test various crash scenarios",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Verify Firebase Crashlytics integration",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Test Firebase Analytics events",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Monitor Firebase Performance",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "⚠️ Warning: Some tests may crash the app!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        navController.navigate("crash_test")
                        showCrashTestDialog = false
                    }
                ) {
                    Text("Open Test Screen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCrashTestDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Quick Crash Test Button
 * Simple button for quick access to crash tests
 */
@Composable
fun QuickCrashTestButton(
    onTestClick: (String) -> Unit,
    testType: String,
    testDescription: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = testType,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = testDescription,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onTestClick(testType) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test $testType")
            }
        }
    }
}

/**
 * Crash Test Status Indicator
 * Shows the current status of Firebase Crashlytics
 */
@Composable
fun CrashTestStatusIndicator(
    isFirebaseConnected: Boolean,
    lastTestTime: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFirebaseConnected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isFirebaseConnected) "🔥 Firebase Connected" else "❌ Firebase Not Connected",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFirebaseConnected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
                lastTestTime?.let {
                    Text(
                        text = "Last test: $it",
                        fontSize = 12.sp,
                        color = if (isFirebaseConnected)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            StatusBadge(
                status = if (isFirebaseConnected) "Active" else "Inactive",
                isActive = isFirebaseConnected
            )
        }
    }
}
