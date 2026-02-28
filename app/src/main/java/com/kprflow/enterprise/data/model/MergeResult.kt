package com.kprflow.enterprise.data.model

/**
 * Result of document merge operation
 */
data class MergeResult(
    val success: Boolean,
    val outputPath: String,
    val processingTimeMs: Long,
    val documentsProcessed: Int,
    val errors: List<String>,
    val warnings: List<String>,
    val fileSizeBytes: Long,
    val qualityScore: Double,
    val timestamp: Long = System.currentTimeMillis()
)
