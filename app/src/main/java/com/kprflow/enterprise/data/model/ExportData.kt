package com.kprflow.enterprise.data.model

import java.util.Date

/**
 * Enhanced Export Data Model for Advanced PDF Generation
 */
data class ExportData(
    val title: String,
    val totalApplications: Int,
    val conversionRate: Double,
    val totalRevenue: Double,
    val avgProcessingTime: Double,
    val detailedData: List<ApplicationData>,
    val revenueByCategory: Map<String, Double>,
    val generatedDate: Date = Date(),
    val reportType: String = "KPR_FLOW_REPORT"
)

/**
 * Individual application data
 */
data class ApplicationData(
    val customerName: String,
    val unitInfo: String,
    val status: String,
    val progress: Int,
    val revenue: Double,
    val lastUpdated: Date,
    val processingTime: Double,
    val documentsCompleted: Int,
    val totalDocuments: Int
)

/**
 * Revenue category breakdown
 */
data class RevenueCategory(
    val category: String,
    val amount: Double,
    val percentage: Double,
    val count: Int
)
