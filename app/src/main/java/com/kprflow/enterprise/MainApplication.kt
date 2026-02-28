package com.kprflow.enterprise

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.perf.FirebasePerformance
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * KPRFlow Enterprise Main Application
 * Firebase Crashlytics & CI/CD Integration
 */
@HiltAndroidApp
class MainApplication : Application() {
    
    @Inject
    lateinit var crashlytics: FirebaseCrashlytics
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase with error handling
        initializeFirebase()
        
        // Initialize logging
        initializeLogging()
        
        // Initialize crash reporting
        initializeCrashReporting()
        
        // Initialize performance monitoring
        initializePerformanceMonitoring()
    }
    
    /**
     * Initialize Firebase with try-catch to prevent crashes
     */
    private fun initializeFirebase() {
        try {
            FirebaseApp.getInstance(this)
            Timber.d("Firebase initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase - using dummy config")
            // Continue without Firebase - app will still work
        }
    }
    
    /**
     * Initialize logging system
     */
    private fun initializeLogging() {
        if (BuildConfig.DEBUG_MODE) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
    
    /**
     * Initialize crash reporting with error handling
     */
    private fun initializeCrashReporting() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            
            // Set user identifier (if available)
            // crashlytics.setUserId("user_id")
            
            // Enable crashlytics collection
            crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG_MODE)
            
            // Add custom keys for debugging
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            crashlytics.setCustomKey("debug_mode", BuildConfig.DEBUG_MODE)
            
            Timber.d("Firebase Crashlytics initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase Crashlytics")
            // Continue without crashlytics - app will still work
        }
    }
    
    /**
     * Initialize performance monitoring with error handling
     */
    private fun initializePerformanceMonitoring() {
        try {
            val performance = FirebasePerformance.getInstance()
            
            // Enable performance collection
            performance.isPerformanceCollectionEnabled = !BuildConfig.DEBUG_MODE
            
            Timber.d("Firebase Performance initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase Performance")
            // Continue without performance monitoring - app will still work
        }
    }
    
    /**
     * Custom release tree that only logs warnings and errors
     */
    private class ReleaseTree : Timber.Tree() {
        override fun isLoggable(tag: String?, priority: Int): Boolean {
            // Only log warnings and errors in release
            return priority >= Log.WARN
        }
        
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (!isLoggable(tag, priority)) return
            
            // Log to Firebase Crashlytics for errors
            if (priority == Log.ERROR && t != null) {
                try {
                    FirebaseCrashlytics.getInstance().recordException(t)
                } catch (e: Exception) {
                    // Failed to log to Crashlytics
                }
            }
            
            // You can also log to other services here
            // For example: send to your logging service
        }
    }
}

/**
 * Extension function to log custom events to Firebase
 */
fun Application.logCustomEvent(eventName: String, parameters: Map<String, Any> = emptyMap()) {
    try {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle = android.os.Bundle()
        
        parameters.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putFloat(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        
        firebaseAnalytics.logEvent(eventName, bundle)
    } catch (e: Exception) {
        Timber.e(e, "Failed to log custom event to Firebase")
    }
}

/**
 * Extension function to log non-fatal exceptions
 */
fun Application.logNonFatalException(throwable: Throwable, message: String? = null) {
    try {
        val crashlytics = FirebaseCrashlytics.getInstance()
        
        message?.let {
            crashlytics.setMessage(it)
        }
        
        crashlytics.recordException(throwable)
    } catch (e: Exception) {
        Timber.e(e, "Failed to log non-fatal exception to Firebase")
    }
}

/**
 * Extension function to set user identifier
 */
fun Application.setFirebaseUserId(userId: String) {
    try {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setUserId(userId)
        
        val analytics = FirebaseAnalytics.getInstance(this)
        analytics.setUserId(userId)
    } catch (e: Exception) {
        Timber.e(e, "Failed to set Firebase user ID")
    }
}

/**
 * Extension function to set user properties
 */
fun Application.setFirebaseUserProperty(name: String, value: String) {
    try {
        val analytics = FirebaseAnalytics.getInstance(this)
        analytics.setUserProperty(name, value)
    } catch (e: Exception) {
        Timber.e(e, "Failed to set Firebase user property")
    }
}
