package com.kprflow.enterprise

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KprFlowApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase Crashlytics for Phase 26
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }
}
