package com.kprflow.enterprise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.navigation.AppNavigation
import com.kprflow.enterprise.ui.theme.KPRFlowEnterpriseTheme
import com.kprflow.enterprise.ui.components.NetworkAwareScreen
import com.kprflow.enterprise.ui.viewmodel.GlobalUIViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var crashlytics: FirebaseCrashlytics
    
    @Inject
    lateinit var analytics: FirebaseAnalytics
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Crashlytics with error handling
        initializeFirebaseServices()
        
        setContent {
            KPRFlowEnterpriseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Global UI State Manager with Network Awareness
                    NetworkAwareScreen(
                        globalUIViewModel = hiltViewModel()
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }
    
    /**
     * Initialize Firebase services with error handling
     */
    private fun initializeFirebaseServices() {
        try {
            // Log app launch event
            analytics.logEvent("app_opened", null)
            
            // Set custom keys for debugging
            crashlytics.setCustomKey("activity", "MainActivity")
            crashlytics.setCustomKey("screen", "main")
            
            Timber.d("Firebase services initialized in MainActivity")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase services in MainActivity")
            // Continue without Firebase - app will still work
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        try {
            // Log app foreground event
            analytics.logEvent("app_foreground", null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to log app foreground event")
        }
    }
    
    override fun onPause() {
        super.onPause()
        
        try {
            // Log app background event
            analytics.logEvent("app_background", null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to log app background event")
        }
    }
}
