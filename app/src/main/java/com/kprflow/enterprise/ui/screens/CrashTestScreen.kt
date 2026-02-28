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
import com.kprflow.enterprise.ui.viewmodel.CrashTestViewModel
import com.kprflow.enterprise.ui.components.StatusBadge

/**
 * Crash Test Screen for Firebase Crashlytics Testing
 * Tests various crash scenarios to verify Firebase Crashlytics integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashTestScreen(
    viewModel: CrashTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "🔥 Firebase Crashlytics Test",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Test various crash scenarios to verify Firebase Crashlytics integration",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Status Badge
        StatusBadge(
            status = if (uiState.firebaseInitialized) "Firebase Connected" else "Firebase Not Connected",
            isActive = uiState.firebaseInitialized,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Test Categories
        TestCategory("🚨 Fatal Crashes") {
            FatalCrashTestButtons(viewModel)
        }
        
        TestCategory("⚠️ Non-Fatal Exceptions") {
            NonFatalTestButtons(viewModel)
        }
        
        TestCategory("📊 Custom Events") {
            CustomEventTestButtons(viewModel)
        }
        
        TestCategory("🔧 Performance Tests") {
            PerformanceTestButtons(viewModel)
        }
        
        TestCategory("📱 ANR Tests") {
            ANRTestButtons(viewModel)
        }
        
        // Test Results
        if (uiState.testResults.isNotEmpty()) {
            TestResultsSection(uiState.testResults)
        }
    }
}

@Composable
private fun TestCategory(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun FatalCrashTestButtons(viewModel: CrashTestViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { viewModel.testNullPointerException() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("⚡ Test NullPointerException")
        }
        
        Button(
            onClick = { viewModel.testArrayIndexOutOfBounds() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("🎯 Test Array Index Out of Bounds")
        }
        
        Button(
            onClick = { viewModel.testClassCastException() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("🔄 Test Class Cast Exception")
        }
        
        Button(
            onClick = { viewModel.testArithmeticException() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("🔢 Test Arithmetic Exception")
        }
    }
}

@Composable
private fun NonFatalTestButtons(viewModel: CrashTestViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { viewModel.testNonFatalException() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("⚠️ Test Non-Fatal Exception")
        }
        
        Button(
            onClick = { viewModel.testCustomException() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("🎭 Test Custom Exception")
        }
        
        Button(
            onClick = { viewModel.testNetworkException() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("🌐 Test Network Exception")
        }
    }
}

@Composable
private fun CustomEventTestButtons(viewModel: CrashTestViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { viewModel.testCustomEvent() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("📊 Test Custom Event")
        }
        
        Button(
            onClick = { viewModel.testUserProperty() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("👤 Test User Property")
        }
        
        Button(
            onClick = { viewModel.testCustomKey() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("🔑 Test Custom Key")
        }
    }
}

@Composable
private fun PerformanceTestButtons(viewModel: CrashTestViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { viewModel.testSlowOperation() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text("🐌 Test Slow Operation")
        }
        
        Button(
            onClick = { viewModel.testMemoryLeak() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text("💾 Test Memory Leak")
        }
        
        Button(
            onClick = { viewModel.testCPUIntensive() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text("⚙️ Test CPU Intensive")
        }
    }
}

@Composable
private fun ANRTestButtons(viewModel: CrashTestViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { viewModel.testMainThreadBlocking() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text("🚫 Test Main Thread Blocking")
        }
        
        Button(
            onClick = { viewModel.testInfiniteLoop() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text("🔄 Test Infinite Loop")
        }
        
        Button(
            onClick = { viewModel.testDeadlock() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text("🔒 Test Deadlock")
        }
    }
}

@Composable
private fun TestResultsSection(testResults: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📋 Test Results",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            testResults.forEach { result ->
                Text(
                    text = "• $result",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
