package com.kprflow.enterprise.data.datasource

import com.kprflow.enterprise.domain.model.CrashStatistics
import com.kprflow.enterprise.domain.model.PerformanceMetrics
import com.kprflow.enterprise.domain.model.UserActivityMetrics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoringDataSource @Inject constructor() {

    suspend fun getCrashStatistics(): CrashStatistics {
        // In a real implementation, this would fetch from Firebase Crashlytics API
        // For now, returning mock data
        return CrashStatistics(
            totalCrashes = 12,
            crashRate = 0.08, // 0.08%
            mostAffectedVersion = "1.2.0",
            topCrashReasons = listOf(
                CrashReason(
                    reason = "NullPointerException",
                    count = 5,
                    percentage = 41.7,
                    firstOccurrence = System.currentTimeMillis() - 86400000 * 7,
                    lastOccurrence = System.currentTimeMillis() - 3600000
                ),
                CrashReason(
                    reason = "NetworkException",
                    count = 3,
                    percentage = 25.0,
                    firstOccurrence = System.currentTimeMillis() - 86400000 * 5,
                    lastOccurrence = System.currentTimeMillis() - 7200000
                ),
                CrashReason(
                    reason = "OutOfMemoryError",
                    count = 2,
                    percentage = 16.7,
                    firstOccurrence = System.currentTimeMillis() - 86400000 * 3,
                    lastOccurrence = System.currentTimeMillis() - 1800000
                )
            ),
            crashesByDevice = listOf(
                DeviceCrashData(
                    deviceName = "Samsung Galaxy S21",
                    crashCount = 4,
                    userCount = 245,
                    crashRate = 1.63
                ),
                DeviceCrashData(
                    deviceName = "Pixel 6",
                    crashCount = 3,
                    userCount = 189,
                    crashRate = 1.59
                ),
                DeviceCrashData(
                    deviceName = "OnePlus 9",
                    crashCount = 2,
                    userCount = 156,
                    crashRate = 1.28
                )
            ),
            crashesByOSVersion = listOf(
                OSCrashData(
                    osVersion = "Android 12",
                    crashCount = 8,
                    userCount = 412,
                    crashRate = 1.94
                ),
                OSCrashData(
                    osVersion = "Android 11",
                    crashCount = 3,
                    userCount = 278,
                    crashRate = 1.08
                ),
                OSCrashData(
                    osVersion = "Android 13",
                    crashCount = 1,
                    userCount = 89,
                    crashRate = 1.12
                )
            ),
            averageRecoveryTime = 45000L, // 45 seconds
            userImpactScore = 2.3 // Low impact
        )
    }

    suspend fun getPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            appStartupTime = 1200L, // 1.2 seconds
            screenLoadTime = 450L, // 450ms
            apiResponseTime = 280L, // 280ms
            memoryUsage = 128 * 1024 * 1024L, // 128MB
            batteryUsage = 2.3, // 2.3%
            networkLatency = 120L, // 120ms
            cpuUsage = 15.6 // 15.6%
        )
    }

    suspend fun getUserActivityMetrics(): UserActivityMetrics {
        return UserActivityMetrics(
            activeUsers = 1247,
            totalSessions = 3842,
            averageSessionDuration = 864000L, // 14.4 minutes
            retentionRate = 78.5, // 78.5%
            churnRate = 4.2, // 4.2%
            dailyActiveUsers = 892,
            weeklyActiveUsers = 1156,
            monthlyActiveUsers = 1247
        )
    }
}
