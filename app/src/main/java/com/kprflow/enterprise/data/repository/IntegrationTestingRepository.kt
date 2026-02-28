package com.kprflow.enterprise.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntegrationTestingRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    
    companion object {
        // Test types
        const val TEST_TYPE_API = "API"
        const val TEST_TYPE_DATABASE = "DATABASE"
        const val TEST_TYPE_PERFORMANCE = "PERFORMANCE"
        const val TEST_TYPE_SECURITY = "SECURITY"
        const val TEST_TYPE_UI = "UI"
        const val TEST_TYPE_E2E = "E2E"
        
        // Test statuses
        const val TEST_STATUS_PENDING = "PENDING"
        const val TEST_STATUS_RUNNING = "RUNNING"
        const val TEST_STATUS_PASSED = "PASSED"
        const val TEST_STATUS_FAILED = "FAILED"
        const val TEST_STATUS_SKIPPED = "SKIPPED"
        const val TEST_STATUS_CANCELLED = "CANCELLED"
        
        // Test categories
        const val CATEGORY_UNIT = "UNIT"
        const val CATEGORY_INTEGRATION = "INTEGRATION"
        const val CATEGORY_SYSTEM = "SYSTEM"
        const val CATEGORY_ACCEPTANCE = "ACCEPTANCE"
        const val CATEGORY_REGRESSION = "REGRESSION"
        const val CATEGORY_SMOKE = "SMOKE"
        const val CATEGORY_SANITY = "SANITY"
        
        // Performance test types
        const val PERF_TEST_LOAD = "LOAD"
        const val PERF_TEST_STRESS = "STRESS"
        const val PERF_TEST_SPIKE = "SPIKE"
        const val PERF_TEST_VOLUME = "VOLUME"
        const val PERF_TEST_ENDURANCE = "ENDURANCE"
        
        // Test priorities
        const val PRIORITY_CRITICAL = "CRITICAL"
        const val PRIORITY_HIGH = "HIGH"
        const val PRIORITY_MEDIUM = "MEDIUM"
        const val PRIORITY_LOW = "LOW"
    }
    
    suspend fun createTestSuite(
        name: String,
        description: String,
        testType: String,
        category: String,
        testCases: List<TestCase>,
        environment: String = "TEST",
        priority: String = PRIORITY_MEDIUM,
        createdBy: String
    ): Result<String> {
        return try {
            val suiteData = mapOf(
                "name" to name,
                "description" to description,
                "test_type" to testType,
                "category" to category,
                "environment" to environment,
                "priority" to priority,
                "status" to TEST_STATUS_PENDING,
                "total_tests" to testCases.size,
                "created_by" to createdBy,
                "created_at" to Instant.now().toString()
            )
            
            val suite = postgrest.from("test_suites")
                .insert(suiteData)
                .maybeSingle()
                .data
            
            suite?.let { suiteId ->
                // Add test cases
                testCases.forEach { testCase ->
                    val caseData = mapOf(
                        "suite_id" to suiteId.id,
                        "name" to testCase.name,
                        "description" to testCase.description,
                        "test_method" to testCase.testMethod,
                        "test_data" to testCase.testData,
                        "expected_result" to testCase.expectedResult,
                        "timeout_seconds" to testCase.timeoutSeconds,
                        "priority" to testCase.priority,
                        "created_at" to Instant.now().toString()
                    )
                    
                    postgrest.from("test_cases")
                        .insert(caseData)
                        .maybeSingle()
                        .data
                }
                
                Result.success(suiteId.id)
            } ?: Result.failure(Exception("Failed to create test suite"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun executeTestSuite(
        suiteId: String,
        environment: String = "TEST",
        parallel: Boolean = false,
        triggeredBy: String? = null
    ): Result<TestExecutionResult> {
        return try {
            val startTime = Instant.now()
            
            // Update suite status to running
            updateTestSuiteStatus(suiteId, TEST_STATUS_RUNNING)
                .getOrNull()
            
            // Get test cases
            val testCases = getTestCases(suiteId).getOrNull().orEmpty()
            
            val executionResults = mutableListOf<TestCaseResult>()
            var passedCount = 0
            var failedCount = 0
            var skippedCount = 0
            
            testCases.forEach { testCase ->
                val result = executeTestCase(testCase, environment)
                executionResults.add(result)
                
                when (result.status) {
                    TEST_STATUS_PASSED -> passedCount++
                    TEST_STATUS_FAILED -> failedCount++
                    TEST_STATUS_SKIPPED -> skippedCount++
                }
            }
            
            val endTime = Instant.now()
            val duration = ChronoUnit.SECONDS.between(startTime, endTime)
            
            // Update suite with results
            val suiteUpdateData = mapOf(
                "status" to if (failedCount == 0) TEST_STATUS_PASSED else TEST_STATUS_FAILED,
                "passed_tests" to passedCount,
                "failed_tests" to failedCount,
                "skipped_tests" to skippedCount,
                "duration_seconds" to duration,
                "started_at" to startTime.toString(),
                "completed_at" to endTime.toString(),
                "triggered_by" to triggeredBy,
                "updated_at" to endTime.toString()
            )
            
            postgrest.from("test_suites")
                .update(suiteUpdateData)
                .filter { eq("id", suiteId) }
                .maybeSingle()
                .data
            
            val executionResult = TestExecutionResult(
                suiteId = suiteId,
                status = if (failedCount == 0) TEST_STATUS_PASSED else TEST_STATUS_FAILED,
                totalTests = testCases.size,
                passedTests = passedCount,
                failedTests = failedCount,
                skippedTests = skippedCount,
                duration = duration,
                startedAt = startTime.toString(),
                completedAt = endTime.toString(),
                testResults = executionResults
            )
            
            Result.success(executionResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun executeApiIntegrationTest(
        endpoint: String,
        method: String,
        testData: Map<String, Any>,
        expectedResponse: Map<String, Any>,
        headers: Map<String, String> = emptyMap(),
        timeoutSeconds: Int = 30
    ): Result<ApiTestResult> {
        return try {
            val startTime = Instant.now()
            
            // Simulate API call
            val apiResult = simulateApiCall(endpoint, method, testData, headers, timeoutSeconds)
            
            val endTime = Instant.now()
            val responseTime = ChronoUnit.MILLIS.between(startTime, endTime)
            
            // Validate response
            val validationResults = validateApiResponse(apiResult, expectedResponse)
            
            val testResult = ApiTestResult(
                endpoint = endpoint,
                method = method,
                statusCode = apiResult.statusCode,
                responseTime = responseTime,
                success = validationResults.isValid,
                actualResponse = apiResult.responseBody,
                expectedResponse = expectedResponse,
                validationErrors = validationResults.errors,
                executedAt = endTime.toString()
            )
            
            Result.success(testResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun executeDatabaseIntegrationTest(
        operation: String,
        testData: Map<String, Any>,
        expectedData: Map<String, Any>,
        tableName: String
    ): Result<DatabaseTestResult> {
        return try {
            val startTime = Instant.now()
            
            // Simulate database operation
            val dbResult = simulateDatabaseOperation(operation, testData, tableName)
            
            val endTime = Instant.now()
            val executionTime = ChronoUnit.MILLIS.between(startTime, endTime)
            
            // Validate data
            val validationResults = validateDatabaseData(dbResult, expectedData)
            
            val testResult = DatabaseTestResult(
                operation = operation,
                tableName = tableName,
                executionTime = executionTime,
                success = validationResults.isValid,
                actualData = dbResult,
                expectedData = expectedData,
                validationErrors = validationResults.errors,
                executedAt = endTime.toString()
            )
            
            Result.success(testResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun executePerformanceTest(
        testType: String,
        targetEndpoint: String,
        concurrentUsers: Int,
        durationSeconds: Int,
        rampUpSeconds: Int = 10,
        environment: String = "TEST"
    ): Result<PerformanceTestResult> {
        return try {
            val startTime = Instant.now()
            
            // Simulate performance test
            val perfResult = simulatePerformanceTest(
                testType, targetEndpoint, concurrentUsers, durationSeconds, rampUpSeconds
            )
            
            val endTime = Instant.now()
            val actualDuration = ChronoUnit.SECONDS.between(startTime, endTime)
            
            // Analyze performance metrics
            val analysis = analyzePerformanceMetrics(perfResult)
            
            val testResult = PerformanceTestResult(
                testType = testType,
                targetEndpoint = targetEndpoint,
                concurrentUsers = concurrentUsers,
                durationSeconds = durationSeconds,
                rampUpSeconds = rampUpSeconds,
                actualDuration = actualDuration,
                totalRequests = perfResult.totalRequests,
                successfulRequests = perfResult.successfulRequests,
                failedRequests = perfResult.failedRequests,
                averageResponseTime = perfResult.averageResponseTime,
                minResponseTime = perfResult.minResponseTime,
                maxResponseTime = perfResult.maxResponseTime,
                throughput = perfResult.throughput,
                errorRate = perfResult.errorRate,
                performanceGrade = analysis.grade,
                bottlenecks = analysis.bottlenecks,
                recommendations = analysis.recommendations,
                executedAt = endTime.toString()
            )
            
            Result.success(testResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTestSuites(
        testType: String? = null,
        status: String? = null,
        environment: String? = null,
        limit: Int = 100
    ): Result<List<TestSuite>> {
        return try {
            var query = postgrest.from("test_suites")
                .select()
                .order("created_at", ascending = false)
                .limit(limit)
            
            testType?.let { query = query.filter { eq("test_type", it) } }
            status?.let { query = query.filter { eq("status", it) } }
            environment?.let { query = query.filter { eq("environment", it) } }
            
            val suites = query.data
            Result.success(suites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTestResults(
        suiteId: String? = null,
        status: String? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        limit: Int = 100
    ): Result<List<TestResult>> {
        return try {
            var query = postgrest.from("test_results")
                .select()
                .order("executed_at", ascending = false)
                .limit(limit)
            
            suiteId?.let { query = query.filter { eq("suite_id", it) } }
            status?.let { query = query.filter { eq("status", it) } }
            startDate?.let { query = query.filter { gte("executed_at", it.toString()) } }
            endDate?.let { query = query.filter { lte("executed_at", it.toString()) } }
            
            val results = query.data
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTestCoverage(
        testType: String? = null,
        environment: String? = null
    ): Result<TestCoverage> {
        return try {
            // Simulate coverage calculation
            val coverage = TestCoverage(
                totalEndpoints = 150,
                testedEndpoints = 135,
                totalDatabaseTables = 25,
                testedDatabaseTables = 23,
                totalApiMethods = 450,
                testedApiMethods = 420,
                endpointCoverage = 90.0,
                databaseCoverage = 92.0,
                apiMethodCoverage = 93.3,
                overallCoverage = 91.8,
                untestedEndpoints = listOf("/admin/users", "/reports/advanced"),
                untestedTables = listOf("temp_tables"),
                untestedMethods = listOf("DELETE /dossiers/{id}/force"),
                generatedAt = Instant.now().toString()
            )
            
            Result.success(coverage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateTestReport(
        suiteIds: List<String>,
        reportType: String = "COMPREHENSIVE",
        includeDetails: Boolean = true
    ): Result<TestReport> {
        return try {
            val suites = mutableListOf<TestSuite>()
            val allResults = mutableListOf<TestResult>()
            
            suiteIds.forEach { suiteId ->
                val suite = getTestSuiteById(suiteId).getOrNull()
                suite?.let { suites.add(it) }
                
                val results = getTestResults(suiteId).getOrNull().orEmpty()
                allResults.addAll(results)
            }
            
            // Calculate statistics
            val totalTests = allResults.size
            val passedTests = allResults.count { it.status == TEST_STATUS_PASSED }
            val failedTests = allResults.count { it.status == TEST_STATUS_FAILED }
            val skippedTests = allResults.count { it.status == TEST_STATUS_SKIPPED }
            val successRate = if (totalTests > 0) (passedTests.toDouble() / totalTests) * 100 else 0.0
            
            val report = TestReport(
                id = java.util.UUID.randomUUID().toString(),
                reportType = reportType,
                testSuites = suites,
                totalTests = totalTests,
                passedTests = passedTests,
                failedTests = failedTests,
                skippedTests = skippedTests,
                successRate = successRate,
                averageDuration = allResults.mapNotNull { it.durationSeconds }.average(),
                testResults = if (includeDetails) allResults else emptyList(),
                generatedAt = Instant.now().toString()
            )
            
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun scheduleTestExecution(
        suiteId: String,
        schedule: String, // Cron expression
        environment: String = "TEST",
        enabled: Boolean = true
    ): Result<String> {
        return try {
            val scheduleData = mapOf(
                "suite_id" to suiteId,
                "schedule" to schedule,
                "environment" to environment,
                "is_enabled" to enabled,
                "last_run" to null,
                "next_run" to calculateNextRun(schedule),
                "created_at" to Instant.now().toString()
            )
            
            val scheduledTest = postgrest.from("scheduled_tests")
                .insert(scheduleData)
                .maybeSingle()
                .data
            
            scheduledTest?.let { 
                    Result.success(it.id)
                }
                ?: Result.failure(Exception("Failed to schedule test execution"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeTestUpdates(): Flow<TestUpdate> = flow {
        try {
            // TODO: Implement real-time updates via Supabase Realtime
            emit(TestUpdate.TestCompleted)
        } catch (e: Exception) {
            emit(TestUpdate.Error(e.message ?: "Unknown error"))
        }
    }
    
    // Private helper methods
    private suspend fun getTestSuiteById(suiteId: String): Result<TestSuite> {
        return try {
            val suite = postgrest.from("test_suites")
                .select()
                .filter { eq("id", suiteId) }
                .maybeSingle()
                .data
            
            suite?.let { Result.success(it) }
                ?: Result.failure(Exception("Test suite not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getTestCases(suiteId: String): Result<List<TestCase>> {
        return try {
            val testCases = postgrest.from("test_cases")
                .select()
                .filter { eq("suite_id", suiteId) }
                .order("priority", ascending = false)
                .data
            
            Result.success(testCases)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun updateTestSuiteStatus(
        suiteId: String,
        status: String
    ): Result<Unit> {
        return try {
            postgrest.from("test_suites")
                .update(
                    mapOf(
                        "status" to status,
                        "updated_at" to Instant.now().toString()
                    )
                )
                .filter { eq("id", suiteId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeTestCase(
        testCase: TestCase,
        environment: String
    ): TestCaseResult {
        val startTime = Instant.now()
        
        return try {
            // Simulate test execution based on test type
            val result = when (testCase.testMethod) {
                "api_test" -> simulateApiTest(testCase.testData, testCase.expectedResult)
                "database_test" -> simulateDatabaseTest(testCase.testData, testCase.expectedResult)
                "performance_test" -> simulatePerformanceTest(testCase.testData)
                else -> simulateGenericTest(testCase.testData, testCase.expectedResult)
            }
            
            val endTime = Instant.now()
            val duration = ChronoUnit.SECONDS.between(startTime, endTime)
            
            TestCaseResult(
                testCaseId = testCase.id,
                name = testCase.name,
                status = if (result.success) TEST_STATUS_PASSED else TEST_STATUS_FAILED,
                durationSeconds = duration,
                errorMessage = if (result.success) null else result.error,
                actualResult = result.actual,
                expectedResult = testCase.expectedResult,
                executedAt = endTime.toString()
            )
        } catch (e: Exception) {
            TestCaseResult(
                testCaseId = testCase.id,
                name = testCase.name,
                status = TEST_STATUS_FAILED,
                durationSeconds = ChronoUnit.SECONDS.between(startTime, Instant.now()),
                errorMessage = e.message,
                actualResult = null,
                expectedResult = testCase.expectedResult,
                executedAt = Instant.now().toString()
            )
        }
    }
    
    private fun simulateApiCall(
        endpoint: String,
        method: String,
        testData: Map<String, Any>,
        headers: Map<String, String>,
        timeoutSeconds: Int
    ): ApiCallResult {
        // Simulate API call with random success/failure
        val success = (1..100).random() > 10 // 90% success rate
        val statusCode = if (success) {
            when (method) {
                "POST" -> 201
                "DELETE" -> 204
                else -> 200
            }
        } else {
            (400..500).random()
        }
        
        return ApiCallResult(
            statusCode = statusCode,
            responseBody = if (success) {
                mapOf("status" to "success", "data" to testData)
            } else {
                mapOf("error" to "Simulated API error")
            },
            headers = mapOf("Content-Type" to "application/json")
        )
    }
    
    private fun validateApiResponse(
        actual: ApiCallResult,
        expected: Map<String, Any>
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (actual.statusCode >= 400) {
            errors.add("API call failed with status ${actual.statusCode}")
        }
        
        // Validate response structure
        expected.forEach { (key, value) ->
            if (!actual.responseBody.containsKey(key)) {
                errors.add("Missing expected field: $key")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    private fun simulateDatabaseOperation(
        operation: String,
        testData: Map<String, Any>,
        tableName: String
    ): Map<String, Any> {
        // Simulate database operation
        return mapOf(
            "operation" to operation,
            "table" to tableName,
            "affected_rows" to (1..10).random(),
            "data" to testData
        )
    }
    
    private fun validateDatabaseData(
        actual: Map<String, Any>,
        expected: Map<String, Any>
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        expected.forEach { (key, value) ->
            if (!actual.containsKey(key)) {
                errors.add("Missing expected field: $key")
            } else if (actual[key] != value) {
                errors.add("Value mismatch for field $key: expected $value, got ${actual[key]}")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    private fun simulatePerformanceTest(
        testType: String,
        targetEndpoint: String,
        concurrentUsers: Int,
        durationSeconds: Int,
        rampUpSeconds: Int
    ): PerformanceMetrics {
        // Simulate performance test results
        val totalRequests = concurrentUsers * durationSeconds * 2 // 2 requests per second per user
        val successRate = 0.95 // 95% success rate
        val avgResponseTime = (100..500).random().toDouble()
        
        return PerformanceMetrics(
            totalRequests = totalRequests,
            successfulRequests = (totalRequests * successRate).toInt(),
            failedRequests = (totalRequests * (1 - successRate)).toInt(),
            averageResponseTime = avgResponseTime,
            minResponseTime = avgResponseTime * 0.5,
            maxResponseTime = avgResponseTime * 2.0,
            throughput = totalRequests.toDouble() / durationSeconds
        )
    }
    
    private fun analyzePerformanceMetrics(metrics: PerformanceMetrics): PerformanceAnalysis {
        val grade = when {
            metrics.averageResponseTime < 200 -> "A"
            metrics.averageResponseTime < 500 -> "B"
            metrics.averageResponseTime < 1000 -> "C"
            else -> "D"
        }
        
        val bottlenecks = mutableListOf<String>()
        if (metrics.averageResponseTime > 500) {
            bottlenecks.add("High response time")
        }
        if (metrics.errorRate > 5.0) {
            bottlenecks.add("High error rate")
        }
        
        val recommendations = mutableListOf<String>()
        if (metrics.averageResponseTime > 500) {
            recommendations.add("Consider implementing caching")
        }
        if (metrics.errorRate > 5.0) {
            recommendations.add("Investigate error patterns and improve error handling")
        }
        
        return PerformanceAnalysis(
            grade = grade,
            bottlenecks = bottlenecks,
            recommendations = recommendations
        )
    }
    
    private fun simulateApiTest(
        testData: Map<String, Any>,
        expectedResult: Map<String, Any>
    ): TestExecutionResult {
        val success = (1..100).random() > 5 // 95% success rate
        return TestExecutionResult(
            success = success,
            actual = if (success) testData else mapOf("error" to "Test failed"),
            error = if (success) null else "Simulated test failure"
        )
    }
    
    private fun simulateDatabaseTest(
        testData: Map<String, Any>,
        expectedResult: Map<String, Any>
    ): TestExecutionResult {
        val success = (1..100).random() > 3 // 97% success rate
        return TestExecutionResult(
            success = success,
            actual = if (success) testData else mapOf("error" to "Database test failed"),
            error = if (success) null else "Simulated database test failure"
        )
    }
    
    private fun simulatePerformanceTest(testData: Map<String, Any>): TestExecutionResult {
        val success = (1..100).random() > 10 // 90% success rate
        return TestExecutionResult(
            success = success,
            actual = mapOf("performance" to "OK"),
            error = if (success) null else "Performance test failed"
        )
    }
    
    private fun simulateGenericTest(
        testData: Map<String, Any>,
        expectedResult: Map<String, Any>
    ): TestExecutionResult {
        val success = (1..100).random() > 8 // 92% success rate
        return TestExecutionResult(
            success = success,
            actual = if (success) testData else mapOf("error" to "Generic test failed"),
            error = if (success) null else "Simulated generic test failure"
        )
    }
    
    private fun calculateNextRun(schedule: String): String {
        // Simplified cron calculation - would use proper cron library
        return Instant.now().plus(1, ChronoUnit.HOURS).toString()
    }
}

// Data classes for integration testing
data class TestSuite(
    val id: String,
    val name: String,
    val description: String,
    val testType: String,
    val category: String,
    val environment: String,
    val priority: String,
    val status: String,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val skippedTests: Int,
    val durationSeconds: Int,
    val startedAt: String,
    val completedAt: String,
    val triggeredBy: String?,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

data class TestCase(
    val id: String,
    val suiteId: String,
    val name: String,
    val description: String,
    val testMethod: String,
    val testData: Map<String, Any>,
    val expectedResult: Map<String, Any>,
    val timeoutSeconds: Int,
    val priority: String,
    val createdAt: String
)

data class TestResult(
    val id: String,
    val suiteId: String,
    val testCaseId: String,
    val name: String,
    val status: String,
    val durationSeconds: Int,
    val errorMessage: String?,
    val actualResult: Map<String, Any>?,
    val expectedResult: Map<String, Any>,
    val executedAt: String
)

data class TestExecutionResult(
    val suiteId: String,
    val status: String,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val skippedTests: Int,
    val duration: Long,
    val startedAt: String,
    val completedAt: String,
    val testResults: List<TestCaseResult>
)

data class TestCaseResult(
    val testCaseId: String,
    val name: String,
    val status: String,
    val durationSeconds: Long,
    val errorMessage: String?,
    val actualResult: Map<String, Any>?,
    val expectedResult: Map<String, Any>,
    val executedAt: String
)

data class ApiTestResult(
    val endpoint: String,
    val method: String,
    val statusCode: Int,
    val responseTime: Long,
    val success: Boolean,
    val actualResponse: Map<String, Any>,
    val expectedResponse: Map<String, Any>,
    val validationErrors: List<String>,
    val executedAt: String
)

data class DatabaseTestResult(
    val operation: String,
    val tableName: String,
    val executionTime: Long,
    val success: Boolean,
    val actualData: Map<String, Any>,
    val expectedData: Map<String, Any>,
    val validationErrors: List<String>,
    val executedAt: String
)

data class PerformanceTestResult(
    val testType: String,
    val targetEndpoint: String,
    val concurrentUsers: Int,
    val durationSeconds: Int,
    val rampUpSeconds: Int,
    val actualDuration: Long,
    val totalRequests: Int,
    val successfulRequests: Int,
    val failedRequests: Int,
    val averageResponseTime: Double,
    val minResponseTime: Double,
    val maxResponseTime: Double,
    val throughput: Double,
    val errorRate: Double,
    val performanceGrade: String,
    val bottlenecks: List<String>,
    val recommendations: List<String>,
    val executedAt: String
)

data class TestCoverage(
    val totalEndpoints: Int,
    val testedEndpoints: Int,
    val totalDatabaseTables: Int,
    val testedDatabaseTables: Int,
    val totalApiMethods: Int,
    val testedApiMethods: Int,
    val endpointCoverage: Double,
    val databaseCoverage: Double,
    val apiMethodCoverage: Double,
    val overallCoverage: Double,
    val untestedEndpoints: List<String>,
    val untestedTables: List<String>,
    val untestedMethods: List<String>,
    val generatedAt: String
)

data class TestReport(
    val id: String,
    val reportType: String,
    val testSuites: List<TestSuite>,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val skippedTests: Int,
    val successRate: Double,
    val averageDuration: Double,
    val testResults: List<TestResult>,
    val generatedAt: String
)

// Internal data classes
data class ApiCallResult(
    val statusCode: Int,
    val responseBody: Map<String, Any>,
    val headers: Map<String, String>
)

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

data class PerformanceMetrics(
    val totalRequests: Int,
    val successfulRequests: Int,
    val failedRequests: Int,
    val averageResponseTime: Double,
    val minResponseTime: Double,
    val maxResponseTime: Double,
    val throughput: Double
) {
    val errorRate: Double
        get() = if (totalRequests > 0) (failedRequests.toDouble() / totalRequests) * 100 else 0.0
}

data class PerformanceAnalysis(
    val grade: String,
    val bottlenecks: List<String>,
    val recommendations: List<String>
)

data class TestExecutionResult(
    val success: Boolean,
    val actual: Map<String, Any>?,
    val error: String?
)

sealed class TestUpdate {
    object TestCompleted : TestUpdate()
    object TestFailed : TestUpdate()
    object TestStarted : TestUpdate()
    object SuiteCompleted : TestUpdate()
    data class Error(val message: String) : TestUpdate()
}
