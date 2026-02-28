package com.kprflow.enterprise.domain.usecase.sla

import androidx.compose.ui.graphics.Color
import com.kprflow.enterprise.domain.repository.ISLARepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use Case for Getting Dossier SLA with Warning System
 * Clean Android UseCase for SLA status color indicators
 */
class GetDossierSlaUseCase @Inject constructor(
    private val repository: ISLARepository
) {
    operator fun invoke(dossierId: String): Flow<SlaStatus> {
        return repository.observeSLAStatusChanges(dossierId).map { data ->
            SlaStatus(
                daysRemaining = data.bankDaysLeft,
                // Sapphire Blue untuk aman, Amber untuk < 7 hari, Red untuk kritis 
                statusColor = when {
                    data.bankDaysLeft > 7 -> Color(0xFF004B87) // Sapphire Blue
                    data.bankDaysLeft in 1..7 -> Color(0xFFFFBF00) // Amber
                    else -> Color(0xFFD32F2F) // Red
                },
                isCritical = data.bankDaysLeft <= 3,
                isWarning = data.bankDaysLeft in 4..7,
                isOverdue = data.isBankOverdue,
                docDaysRemaining = data.docDaysLeft,
                slaStatus = data.slaStatus,
                priorityLevel = data.priorityLevel,
                customerName = data.customerName,
                dossierStatus = data.status
            )
        }
    }
}

/**
 * Use Case for Getting Document SLA Status
 */
class GetDocumentSlaUseCase @Inject constructor(
    private val repository: ISLARepository
) {
    operator fun invoke(dossierId: String): Flow<DocumentSlaStatus> {
        return repository.observeSLAStatusChanges(dossierId).map { data ->
            DocumentSlaStatus(
                daysRemaining = data.docDaysLeft,
                statusColor = when {
                    data.docDaysLeft > 7 -> Color(0xFF004B87) // Sapphire Blue
                    data.docDaysLeft in 1..7 -> Color(0xFFFFBF00) // Amber
                    else -> Color(0xFFD32F2F) // Red
                },
                isCritical = data.docDaysLeft <= 3,
                isWarning = data.docDaysLeft in 4..7,
                isOverdue = data.isDocOverdue,
                completionPercentage = data.completionPercentage
            )
        }
    }
}

/**
 * Use Case for Getting SLA Warning Level
 */
class GetSLAWarningLevelUseCase @Inject constructor(
    private val repository: ISLARepository
) {
    suspend operator fun invoke(dossierId: String): Result<SLAWarningLevel> {
        return try {
            val slaStatus = repository.getDossierSLAStatus(dossierId).getOrNull()
                ?: return Result.failure(Exception("SLA status not found"))
            
            val warningLevel = when {
                slaStatus.isDocOverdue || slaStatus.isBankOverdue -> SLAWarningLevel.OVERDUE
                slaStatus.docDaysLeft <= 3 || slaStatus.bankDaysLeft <= 3 -> SLAWarningLevel.CRITICAL
                slaStatus.docDaysLeft <= 7 || slaStatus.bankDaysLeft <= 7 -> SLAWarningLevel.WARNING
                else -> SLAWarningLevel.NORMAL
            }
            
            Result.success(warningLevel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use Case for Getting SLA Color Configuration
 */
class GetSLAColorConfigUseCase @Inject constructor() {
    operator fun invoke(warningLevel: SLAWarningLevel): SLAColorConfig {
        return when (warningLevel) {
            SLAWarningLevel.NORMAL -> SLAColorConfig(
                primary = Color(0xFF004B87), // Sapphire Blue
                background = Color(0xFFE3F2FD), // Light Blue
                text = Color(0xFF0D47A1) // Dark Blue
            )
            SLAWarningLevel.WARNING -> SLAColorConfig(
                primary = Color(0xFFFFBF00), // Amber
                background = Color(0xFFFFF8E1), // Light Amber
                text = Color(0xFFFF6F00) // Dark Amber
            )
            SLAWarningLevel.CRITICAL -> SLAColorConfig(
                primary = Color(0xFFFF6F00), // Dark Amber
                background = Color(0xFFFFE082), // Medium Amber
                text = Color(0xFFE65100) // Deep Orange
            )
            SLAWarningLevel.OVERDUE -> SLAColorConfig(
                primary = Color(0xFFD32F2F), // Red
                background = Color(0xFFFFEBEE), // Light Red
                text = Color(0xFFB71C1C) // Dark Red
            )
        }
    }
}

// =====================================================
// DATA MODELS FOR SLA WARNING SYSTEM
// =====================================================

data class SlaStatus(
    val daysRemaining: Int,
    val statusColor: Color,
    val isCritical: Boolean,
    val isWarning: Boolean = false,
    val isOverdue: Boolean = false,
    val docDaysRemaining: Int = 0,
    val slaStatus: String = "NORMAL",
    val priorityLevel: Int = 1,
    val customerName: String = "",
    val dossierStatus: String = ""
)

data class DocumentSlaStatus(
    val daysRemaining: Int,
    val statusColor: Color,
    val isCritical: Boolean,
    val isWarning: Boolean = false,
    val isOverdue: Boolean = false,
    val completionPercentage: Int = 0
)

enum class SLAWarningLevel {
    NORMAL,
    WARNING,
    CRITICAL,
    OVERDUE
}

data class SLAColorConfig(
    val primary: Color,
    val background: Color,
    val text: Color
) {
    companion object {
        val DEFAULT = SLAColorConfig(
            primary = Color(0xFF004B87),
            background = Color(0xFFE3F2FD),
            text = Color(0xFF0D47A1)
        )
    }
}

// =====================================================
// EXTENSION FUNCTIONS FOR SLA
// =====================================================

fun SLAWarningLevel.getDisplayText(): String {
    return when (this) {
        SLAWarningLevel.NORMAL -> "Normal"
        SLAWarningLevel.WARNING -> "Warning"
        SLAWarningLevel.CRITICAL -> "Critical"
        SLAWarningLevel.OVERDUE -> "Overdue"
    }
}

fun SLAWarningLevel.getIcon(): String {
    return when (this) {
        SLAWarningLevel.NORMAL -> "✓"
        SLAWarningLevel.WARNING -> "⚠"
        SLAWarningLevel.CRITICAL -> "🔥"
        SLAWarningLevel.OVERDUE -> "❌"
    }
}

fun Int.getSLAWarningLevel(): SLAWarningLevel {
    return when {
        this <= 0 -> SLAWarningLevel.OVERDUE
        this <= 3 -> SLAWarningLevel.CRITICAL
        this <= 7 -> SLAWarningLevel.WARNING
        else -> SLAWarningLevel.NORMAL
    }
}
