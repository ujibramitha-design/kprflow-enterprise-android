package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.viewmodel.SplashViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigationComplete: (SplashDestination) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val splashState by viewModel.splashState.collectAsState()
    
    LaunchedEffect(splashState) {
        when (splashState) {
            is SplashState.CheckingAuth -> {
                // Show loading while checking authentication
                delay(2000) // Minimum splash display time
                viewModel.checkAuthentication()
            }
            is SplashState.NavigateToLogin -> {
                delay(500) // Smooth transition
                onNavigationComplete(SplashDestination.Login)
            }
            is SplashState.NavigateToDashboard -> {
                delay(500) // Smooth transition
                onNavigationComplete(SplashDestination.Dashboard(splashState.userRole))
            }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Logo placeholder
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // TODO: Replace with actual logo
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "KPR",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // App Name
            Text(
                text = "KPRFlow Enterprise",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Tagline
            Text(
                text = "Property Developer ERP & CRM",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Loading indicator
            if (splashState is SplashState.CheckingAuth) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

// Splash state management
sealed class SplashState {
    object CheckingAuth : SplashState()
    object NavigateToLogin : SplashState()
    data class NavigateToDashboard(val userRole: com.kprflow.enterprise.data.model.UserRole) : SplashState()
}

// Splash destination for navigation
sealed class SplashDestination {
    object Login : SplashDestination()
    data class Dashboard(val userRole: com.kprflow.enterprise.data.model.UserRole) : SplashDestination()
}
