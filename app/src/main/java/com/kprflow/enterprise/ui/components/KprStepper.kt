package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kprflow.enterprise.data.model.KprStatus

@Composable
fun KprStepper(
    currentStatus: KprStatus,
    modifier: Modifier = Modifier
) {
    val progressSteps = KprStatus.getProgressStatuses()
    val currentStepIndex = progressSteps.indexOf(currentStatus)
    
    Column(
        modifier = modifier
    ) {
        progressSteps.forEachIndexed { index, status ->
            StepItem(
                status = status,
                isCompleted = index < currentStepIndex,
                isCurrent = index == currentStepIndex,
                isLast = index == progressSteps.lastIndex,
                showLine = index < progressSteps.lastIndex
            )
        }
    }
}

@Composable
private fun StepItem(
    status: KprStatus,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLast: Boolean,
    showLine: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step Circle
        StepCircle(
            isCompleted = isCompleted,
            isCurrent = isCurrent,
            status = status
        )
        
        // Step Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = status.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            // Add additional info for current step
            if (isCurrent) {
                Text(
                    text = getStatusDescription(status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Progress Line
    if (showLine) {
        Spacer(modifier = Modifier.height(8.dp))
        ProgressLine(isCompleted = isCompleted)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StepCircle(
    isCompleted: Boolean,
    isCurrent: Boolean,
    status: KprStatus
) {
    val containerColor = when {
        isCompleted -> MaterialTheme.colorScheme.primary
        isCurrent -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when {
        isCompleted -> MaterialTheme.colorScheme.onPrimary
        isCurrent -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = containerColor
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    // Checkmark for completed steps
                    Text(
                        text = "✓",
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // Step number for current and future steps
                    Text(
                        text = "${status.order + 1}",
                        color = contentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressLine(isCompleted: Boolean) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(20.dp)
            .padding(start = 11.dp) // Center align with circle
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ) {}
    }
}

private fun getStatusDescription(status: KprStatus): String {
    return when (status) {
        KprStatus.LEAD -> "Initial application received"
        KprStatus.PEMBERKASAN -> "Collecting required documents"
        KprStatus.PROSES_BANK -> "Bank reviewing application"
        KprStatus.PUTUSAN_KREDIT_ACC -> "Credit application approved"
        KprStatus.SP3K_TERBIT -> "SP3K letter issued"
        KprStatus.PRA_AKAD -> "Preparing for Akad ceremony"
        KprStatus.AKAD_BELUM_CAIR -> "Akad signed, awaiting disbursement"
        KprStatus.FUNDS_DISBURSED -> "Funds disbursed to developer"
        KprStatus.BAST_READY -> "Property ready for handover"
        KprStatus.BAST_COMPLETED -> "Handover completed"
        KprStatus.FLOATING_DOSSIER -> "Processing without unit assignment"
        KprStatus.CANCELLED_BY_SYSTEM -> "Application cancelled"
    }
}
