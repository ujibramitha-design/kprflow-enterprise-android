package com.kprflow.enterprise.domain.model

import java.math.BigDecimal

data class CrashStatistics(
    val totalCrashes: Int,
    val crashRate: Double,
    val mostAffectedVersion: String,
    val topCrashReasons: List<CrashReason>,
    val crashesByDevice: List<DeviceCrashData>,
    val crashesByOSVersion: List<OSCrashData>,
    val averageRecoveryTime: Long,
    val userImpactScore: Double
)

data class CrashReason(
    val reason: String,
    val count: Int,
    val percentage: Double,
    val firstOccurrence: Long,
    val lastOccurrence: Long
)

data class DeviceCrashData(
    val deviceName: String,
    val crashCount: Int,
    val userCount: Int,
    val crashRate: Double
)

data class OSCrashData(
    val osVersion: String,
    val crashCount: Int,
    val userCount: Int,
    val crashRate: Double
)

data class PerformanceMetrics(
    val appStartupTime: Long,
    val screenLoadTime: Long,
    val apiResponseTime: Long,
    val memoryUsage: Long,
    val batteryUsage: Double,
    val networkLatency: Long,
    val cpuUsage: Double
)

data class UserActivityMetrics(
    val activeUsers: Int,
    val totalSessions: Int,
    val averageSessionDuration: Long,
    val retentionRate: Double,
    val churnRate: Double,
    val dailyActiveUsers: Int,
    val weeklyActiveUsers: Int,
    val monthlyActiveUsers: Int
)
