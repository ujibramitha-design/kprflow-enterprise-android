package com.kprflow.enterprise.qa

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quality Assurance Manager - Complete QA System
 * Phase Quality Assurance: Unified Quality Management
 */
@Singleton
class QualityAssuranceManager @Inject constructor(
    private val stressTestEngine: StressTestEngine,
    private val bugHunter: BugHunter
) {
    
    /**
     * Run comprehensive quality assurance
     */
    suspend fun runComprehensiveQA(): QAReport {
        return try {
            // Run stress tests
            val stressTestSummary = stressTestEngine.runComprehensiveStressTests()
            
            // Run bug hunt
            val bugHuntSummary = bugHunter.runComprehensiveBugHunt()
            
            // Generate comprehensive QA report
            generateQAReport(stressTestSummary, bugHuntSummary)
            
        } catch (exc: Exception) {
            QAReport(
                overallStatus = "FAILED",
                stressTestSummary = null,
                bugHuntSummary = null,
                recommendations = listOf("Fix QA system errors: ${exc.message}"),
                confidence = 0.0,
                error = exc.message
            )
        }
    }
    
    /**
     * Get combined QA state
     */
    fun getQAState(): StateFlow<QAState> {
        return combine(
            stressTestEngine.testState,
            bugHunter.bugHuntState
        ) { stressTestState, bugHuntState ->
            QAState(
                stressTestState = stressTestState,
                bugHuntState = bugHuntState,
                isRunning = stressTestState is StressTestEngine.StressTestState.Running ||
                        bugHuntState is BugHunter.BugHuntState.Running,
                isCompleted = stressTestState is StressTestEngine.StressTestState.Completed &&
                        bugHuntState is BugHunter.BugHuntState.Completed
            )
        }
    }
    
    /**
     * Get QA health score
     */
    fun getQAHealthScore(): Double {
        val stressTestResults = stressTestEngine.getTestResults()
        val detectedBugs = bugHunter.getDetectedBugs()
        
        val stressTestScore = if (stressTestResults.isNotEmpty()) {
            val passedTests = stressTestResults.count { it.passed }
            (passedTests.toDouble() / stressTestResults.size) * 100
        } else 100.0
        
        val bugHealthScore = if (detectedBugs.isNotEmpty()) {
            val criticalBugs = detectedBugs.count { it.severity == BugSeverity.CRITICAL }
            val majorBugs = detectedBugs.count { it.severity == BugSeverity.MAJOR }
            val penalty = (criticalBugs * 30) + (majorBugs * 15)
            maxOf(0.0, 100.0 - penalty)
        } else 100.0
        
        return (stressTestScore + bugHealthScore) / 2
    }
    
    /**
     * Get QA recommendations
     */
    fun getQARecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val stressTestResults = stressTestEngine.getTestResults()
        val detectedBugs = bugHunter.getDetectedBugs()
        
        // Stress test recommendations
        stressTestResults.filter { !it.passed }.forEach { result ->
            when (result.testName) {
                "Concurrent Users Load Test" -> recommendations.add("Optimize system for concurrent user load")
                "Data Volume Stress Test" -> recommendations.add("Improve data processing performance")
                "Memory Usage Test" -> recommendations.add("Optimize memory usage and implement proper cleanup")
                "Network Failure Simulation" -> recommendations.add("Improve network error handling and retry mechanisms")
                "Performance Under Load" -> recommendations.add("Optimize performance under high load conditions")
                else -> recommendations.add("Address ${result.testName} issues")
            }
        }
        
        // Bug hunt recommendations
        detectedBugs.groupBy { it.type }.forEach { (bugType, bugs) ->
            when (bugType) {
                BugType.MEMORY_LEAK -> recommendations.add("Fix memory leaks and implement proper memory management")
                BugType.NULL_POINTER -> recommendations.add("Add null checks and proper error handling")
                BugType.PERFORMANCE -> recommendations.add("Optimize performance bottlenecks")
                BugType.DATA_CONSISTENCY -> recommendations.add("Fix data consistency issues")
                BugType.SECURITY -> recommendations.add("Address security vulnerabilities immediately")
                BugType.UI_THREAD_BLOCKING -> recommendations.add("Move heavy operations off UI thread")
                BugType.RESOURCE_LEAK -> recommendations.add("Ensure proper resource cleanup")
                BugType.EXCEPTION_HANDLING -> recommendations.add("Improve exception handling practices")
            }
        }
        
        return recommendations.distinct()
    }
    
    /**
     * Generate QA report
     */
    private fun generateQAReport(
        stressTestSummary: StressTestEngine.StressTestSummary,
        bugHuntSummary: BugHunter.BugHuntSummary
    ): QAReport {
        val overallStatus = when {
            stressTestSummary.overallStatus == "FAILED" || bugHuntSummary.overallHealth == "CRITICAL" -> "FAILED"
            stressTestSummary.successRate < 90 || bugHuntSummary.criticalBugs > 0 -> "NEEDS_ATTENTION"
            stressTestSummary.successRate < 95 || bugHuntSummary.majorBugs > 3 -> "GOOD"
            else -> "EXCELLENT"
        }
        
        val confidence = calculateConfidence(stressTestSummary, bugHuntSummary)
        val recommendations = getQARecommendations()
        
        return QAReport(
            overallStatus = overallStatus,
            stressTestSummary = stressTestSummary,
            bugHuntSummary = bugHuntSummary,
            recommendations = recommendations,
            confidence = confidence,
            error = null
        )
    }
    
    /**
     * Calculate confidence score
     */
    private fun calculateConfidence(
        stressTestSummary: StressTestEngine.StressTestSummary,
        bugHuntSummary: BugHunter.BugHuntSummary
    ): Double {
        val stressTestConfidence = stressTestSummary.successRate / 100.0
        
        val bugHuntConfidence = when {
            bugHuntSummary.overallHealth == "CRITICAL" -> 0.0
            bugHuntSummary.overallHealth == "POOR" -> 0.3
            bugHuntSummary.overallHealth == "FAIR" -> 0.6
            bugHuntSummary.overallHealth == "GOOD" -> 0.8
            bugHuntSummary.overallHealth == "EXCELLENT" -> 1.0
            else -> 0.5
        }
        
        return (stressTestConfidence + bugHuntConfidence) / 2
    }
    
    /**
     * Clear QA results
     */
    fun clearQAResults() {
        stressTestEngine.clearTestResults()
        bugHunter.clearBugHuntResults()
    }
    
    /**
     * Get QA summary
     */
    fun getQASummary(): QASummary {
        val stressTestResults = stressTestEngine.getTestResults()
        val detectedBugs = bugHunter.getDetectedBugs()
        
        return QASummary(
            stressTestsRun = stressTestResults.size,
            stressTestsPassed = stressTestResults.count { it.passed },
            bugsDetected = detectedBugs.size,
            criticalBugs = detectedBugs.count { it.severity == BugSeverity.CRITICAL },
            majorBugs = detectedBugs.count { it.severity == BugSeverity.MAJOR },
            minorBugs = detectedBugs.count { it.severity == BugSeverity.MINOR },
            healthScore = getQAHealthScore(),
            isReadyForProduction = getQAHealthScore() >= 90.0 &&
                    detectedBugs.none { it.severity == BugSeverity.CRITICAL }
        )
    }
}

/**
 * QA State
 */
data class QAState(
    val stressTestState: StressTestEngine.StressTestState,
    val bugHuntState: BugHunter.BugHuntState,
    val isRunning: Boolean,
    val isCompleted: Boolean
)

/**
 * QA Report
 */
data class QAReport(
    val overallStatus: String,
    val stressTestSummary: StressTestEngine.StressTestSummary?,
    val bugHuntSummary: BugHunter.BugHuntSummary?,
    val recommendations: List<String>,
    val confidence: Double,
    val error: String?
)

/**
 * QA Summary
 */
data class QASummary(
    val stressTestsRun: Int,
    val stressTestsPassed: Int,
    val bugsDetected: Int,
    val criticalBugs: Int,
    val majorBugs: Int,
    val minorBugs: Int,
    val healthScore: Double,
    val isReadyForProduction: Boolean
)
