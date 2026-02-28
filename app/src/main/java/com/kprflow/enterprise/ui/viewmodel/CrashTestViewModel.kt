package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel for testing Firebase Crashlytics and Firebase services
 * Provides various test scenarios to verify crash reporting and analytics
 */
@HiltViewModel
class CrashTestViewModel @Inject constructor(
    private val crashlytics: FirebaseCrashlytics,
    private val analytics: FirebaseAnalytics,
    private val performance: FirebasePerformance
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CrashTestUiState())
    val uiState: StateFlow<CrashTestUiState> = _uiState.asStateFlow()
    
    init {
        checkFirebaseInitialization()
    }
    
    /**
     * Check if Firebase services are properly initialized
     */
    private fun checkFirebaseInitialization() {
        viewModelScope.launch {
            try {
                // Test Firebase Analytics
                analytics.logEvent("test_initialization", null)
                
                // Test Firebase Crashlytics
                crashlytics.setCustomKey("test_key", "test_value")
                
                // Test Firebase Performance
                val testTrace = performance.newTrace("test_trace")
                testTrace.start()
                testTrace.stop()
                
                _uiState.value = _uiState.value.copy(
                    firebaseInitialized = true,
                    testResults = _uiState.value.testResults + "✅ Firebase services initialized successfully"
                )
                
                Timber.d("Firebase services initialized successfully")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    firebaseInitialized = false,
                    testResults = _uiState.value.testResults + "❌ Firebase initialization failed: ${e.message}"
                )
                
                Timber.e(e, "Firebase initialization failed")
            }
        }
    }
    
    // ==================== FATAL CRASH TESTS ====================
    
    /**
     * Test NullPointerException - Fatal Crash
     */
    fun testNullPointerException() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "🚨 Testing NullPointerException..."
                )
                
                // This will cause a crash
                val nullString: String? = null
                val length = nullString!!.length // This will crash
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ NullPointerException test failed - no crash occurred"
                )
            } catch (e: Exception) {
                // This shouldn't be reached if the crash occurs
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "⚠️ NullPointerException caught: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Test ArrayIndexOutOfBoundsException - Fatal Crash
     */
    fun testArrayIndexOutOfBounds() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "🚨 Testing ArrayIndexOutOfBoundsException..."
                )
                
                val array = arrayOf(1, 2, 3)
                val value = array[10] // This will crash
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ ArrayIndexOutOfBoundsException test failed - no crash occurred"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "⚠️ ArrayIndexOutOfBoundsException caught: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Test ClassCastException - Fatal Crash
     */
    fun testClassCastException() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "🚨 Testing ClassCastException..."
                )
                
                val obj: Any = "Hello"
                val number: Int = obj as Int // This will crash
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ ClassCastException test failed - no crash occurred"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "⚠️ ClassCastException caught: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Test ArithmeticException - Fatal Crash
     */
    fun testArithmeticException() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "🚨 Testing ArithmeticException..."
                )
                
                val result = 10 / 0 // This will crash
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ ArithmeticException test failed - no crash occurred"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "⚠️ ArithmeticException caught: ${e.message}"
                )
            }
        }
    }
    
    // ==================== NON-FATAL EXCEPTION TESTS ====================
    
    /**
     * Test Non-Fatal Exception - Logged to Crashlytics
     */
    fun testNonFatalException() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "⚠️ Testing Non-Fatal Exception..."
                )
                
                throw RuntimeException("This is a test non-fatal exception")
            } catch (e: Exception) {
                // Log to Crashlytics without crashing the app
                crashlytics.recordException(e)
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "✅ Non-fatal exception logged to Crashlytics"
                )
                
                Timber.d("Non-fatal exception logged: ${e.message}")
            }
        }
    }
    
    /**
     * Test Custom Exception - Logged to Crashlytics
     */
    fun testCustomException() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "🎭 Testing Custom Exception..."
                )
                
                throw CustomTestException("This is a custom test exception")
            } catch (e: Exception) {
                // Log to Crashlytics without crashing the app
                crashlytics.recordException(e)
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "✅ Custom exception logged to Crashlytics"
                )
                
                Timber.d("Custom exception logged: ${e.message}")
            }
        }
    }
    
    /**
     * Test Network Exception - Logged to Crashlytics
     */
    fun testNetworkException() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "🌐 Testing Network Exception..."
                )
                
                throw NetworkTestException("Simulated network connection failure")
            } catch (e: Exception) {
                // Log to Crashlytics without crashing the app
                crashlytics.recordException(e)
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "✅ Network exception logged to Crashlytics"
                )
                
                Timber.d("Network exception logged: ${e.message}")
            }
        }
    }
    
    // ==================== CUSTOM EVENT TESTS ====================
    
    /**
     * Test Custom Event - Logged to Firebase Analytics
     */
    fun testCustomEvent() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "📊 Testing Custom Event..."
                )
                
                val params = android.os.Bundle().apply {
                    putString("test_type", "crash_test")
                    putInt("test_id", System.currentTimeMillis().toInt())
                    putBoolean("success", true)
                }
                
                analytics.logEvent("crash_test_event", params)
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "✅ Custom event logged to Firebase Analytics"
                )
                
                Timber.d("Custom event logged: crash_test_event")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ Custom event failed: ${e.message}"
                )
                
                Timber.e(e, "Custom event failed")
            }
        }
    }
    
    /**
     * Test User Property - Set in Firebase Analytics
     */
    fun testUserProperty() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "👤 Testing User Property..."
                )
                
                analytics.setUserProperty("test_user_type", "crash_tester")
                analytics.setUserProperty("test_version", "1.0.0")
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "✅ User properties set in Firebase Analytics"
                )
                
                Timber.d("User properties set: test_user_type, test_version")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ User property failed: ${e.message}"
                )
                
                Timber.e(e, "User property failed")
            }
        }
    }
    
    /**
     * Test Custom Key - Set in Firebase Crashlytics
     */
    fun testCustomKey() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "🔑 Testing Custom Key..."
                )
                
                crashlytics.setCustomKey("test_session_id", System.currentTimeMillis())
                crashlytics.setCustomKey("test_device_type", "test_device")
                crashlytics.setCustomKey("test_build_variant", "debug")
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "✅ Custom keys set in Firebase Crashlytics"
                )
                
                Timber.d("Custom keys set: test_session_id, test_device_type, test_build_variant")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ Custom key failed: ${e.message}"
                )
                
                Timber.e(e, "Custom key failed")
            }
        }
    }
    
    // ==================== PERFORMANCE TESTS ====================
    
    /**
     * Test Slow Operation - Tracked by Firebase Performance
     */
    fun testSlowOperation() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "🐌 Testing Slow Operation..."
                )
                
                val trace = performance.newTrace("slow_operation_test")
                trace.start()
                
                // Simulate slow operation
                delay(2000) // 2 seconds delay
                
                trace.putMetric("operation_duration_ms", 2000)
                trace.stop()
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "✅ Slow operation tracked by Firebase Performance"
                )
                
                Timber.d("Slow operation completed and tracked")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ Slow operation failed: ${e.message}"
                )
                
                Timber.e(e, "Slow operation failed")
            }
        }
    }
    
    /**
     * Test Memory Leak - Simulated memory pressure
     */
    fun testMemoryLeak() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "💾 Testing Memory Leak Simulation..."
                )
                
                val trace = performance.newTrace("memory_leak_test")
                trace.start()
                
                // Simulate memory leak by creating large objects
                val memoryLeakList = mutableListOf<ByteArray>()
                repeat(10) {
                    memoryLeakList.add(ByteArray(1024 * 1024)) // 1MB each
                }
                
                trace.putMetric("memory_allocated_mb", 10)
                trace.stop()
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "✅ Memory leak simulation tracked by Firebase Performance"
                )
                
                Timber.d("Memory leak simulation completed")
                
                // Clear memory to avoid actual memory leak
                memoryLeakList.clear()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ Memory leak test failed: ${e.message}"
                )
                
                Timber.e(e, "Memory leak test failed")
            }
        }
    }
    
    /**
     * Test CPU Intensive Operation - Tracked by Firebase Performance
     */
    fun testCPUIntensive() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "⚙️ Testing CPU Intensive Operation..."
                )
                
                val trace = performance.newTrace("cpu_intensive_test")
                trace.start()
                
                // Simulate CPU intensive operation
                var result = 0L
                repeat(1000000) {
                    result += it * it
                }
                
                trace.putMetric("calculation_result", result)
                trace.stop()
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "✅ CPU intensive operation tracked by Firebase Performance"
                )
                
                Timber.d("CPU intensive operation completed")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ CPU intensive test failed: ${e.message}"
                )
                
                Timber.e(e, "CPU intensive test failed")
            }
        }
    }
    
    // ==================== ANR TESTS ====================
    
    /**
     * Test Main Thread Blocking - Can cause ANR
     */
    fun testMainThreadBlocking() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "🚫 Testing Main Thread Blocking..."
                )
                
                // This will block the main thread and potentially cause ANR
                Thread.sleep(5000) // 5 seconds block
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "⚠️ Main thread blocking test completed"
                )
                
                Timber.d("Main thread blocking test completed")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ Main thread blocking test failed: ${e.message}"
                )
                
                Timber.e(e, "Main thread blocking test failed")
            }
        }
    }
    
    /**
     * Test Infinite Loop - Can cause ANR
     */
    fun testInfiniteLoop() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "🔄 Testing Infinite Loop..."
                )
                
                // This will create an infinite loop that can cause ANR
                var counter = 0
                while (counter < 1000000) {
                    counter++
                    if (counter % 100000 == 0) {
                        // Allow some breathing room
                        Thread.yield()
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "⚠️ Infinite loop test completed"
                )
                
                Timber.d("Infinite loop test completed")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ Infinite loop test failed: ${e.message}"
                )
                
                Timber.e(e, "Infinite loop test failed")
            }
        }
    }
    
    /**
     * Test Deadlock - Can cause ANR
     */
    fun testDeadlock() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "🔒 Testing Deadlock..."
                )
                
                val lock1 = Any()
                val lock2 = Any()
                val latch = CountDownLatch(2)
                
                // Thread 1
                Thread {
                    synchronized(lock1) {
                        Thread.sleep(100)
                        synchronized(lock2) {
                            latch.countDown()
                        }
                    }
                }.start()
                
                // Thread 2
                Thread {
                    synchronized(lock2) {
                        Thread.sleep(100)
                        synchronized(lock1) {
                            latch.countDown()
                        }
                    }
                }.start()
                
                // Wait for deadlock or timeout
                val completed = latch.await(3, TimeUnit.SECONDS)
                
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + if (completed) {
                        "⚠️ Deadlock test completed (no deadlock occurred)"
                    } else {
                        "⚠️ Deadlock test completed (potential deadlock detected)"
                    }
                )
                
                Timber.d("Deadlock test completed: completed=$completed")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + "❌ Deadlock test failed: ${e.message}"
                )
                
                Timber.e(e, "Deadlock test failed")
            }
        }
    }
}

// ==================== DATA CLASSES ====================

data class CrashTestUiState(
    val firebaseInitialized: Boolean = false,
    val testResults: List<String> = emptyList()
)

// ==================== CUSTOM EXCEPTIONS ====================

class CustomTestException(message: String) : RuntimeException(message)

class NetworkTestException(message: String) : RuntimeException(message)
