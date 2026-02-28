package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kprflow.enterprise.domain.repository.SLAStatus

// =====================================================
// SLA CARD EXTENSION FUNCTIONS
// Easy integration with existing data models
// =====================================================

/**
 * Extension function to convert SLAStatus to SlaCountdownCard
 */
@Composable
fun SLAStatus.toSlaCountdownCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    compact: Boolean = false
) {
    val status = when {
        isDocOverdue || isBankOverdue -> SlaCardStatus.OVERDUE
        docDaysLeft <= 3 || bankDaysLeft <= 3 -> SlaCardStatus.CRITICAL
        docDaysLeft <= 7 || bankDaysLeft <= 7 -> SlaCardStatus.WARNING
        else -> SlaCardStatus.NORMAL
    }
    
    SlaCountdownCard(
        title = customerName,
        daysRemaining = bankDaysLeft,
        totalDays = 60,
        status = status,
        modifier = modifier,
        onClick = onClick,
        compact = compact
    )
}

/**
 * Extension function to create SLA card list
 */
@Composable
fun SLAStatusList(
    slaStatuses: List<SLAStatus>,
    modifier: Modifier = Modifier,
    onCardClick: (String) -> Unit = {},
    compact: Boolean = false
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(slaStatuses) { slaStatus ->
            slaStatus.toSlaCountdownCard(
                onClick = { onCardClick(slaStatus.dossierId) },
                compact = compact
            )
        }
    }
}

/**
 * Extension function for grouped SLA cards
 */
@Composable
fun GroupedSLACards(
    slaStatuses: List<SLAStatus>,
    modifier: Modifier = Modifier,
    onCardClick: (String) -> Unit = {}
) {
    val groupedByStatus = slaStatuses.groupBy { slaStatus ->
        when {
            slaStatus.isDocOverdue || slaStatus.isBankOverdue -> "OVERDUE"
            slaStatus.docDaysLeft <= 3 || slaStatus.bankDaysLeft <= 3 -> "CRITICAL"
            slaStatus.docDaysLeft <= 7 || slaStatus.bankDaysLeft <= 7 -> "WARNING"
            else -> "NORMAL"
        }
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedByStatus.forEach { (status, items) ->
            BentoBox {
                BentoHeader(
                    title = status.replace("_", " "),
                    subtitle = "${items.size} dossiers"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.take(3).forEach { slaStatus ->
                        slaStatus.toSlaCountdownCard(
                            onClick = { onCardClick(slaStatus.dossierId) },
                            compact = true
                        )
                    }
                    
                    if (items.size > 3) {
                        TextButton(
                            onClick = { /* View all in category */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View all ${items.size} ${status.lowercase()} dossiers")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extension function for priority-based SLA cards
 */
@Composable
fun PrioritySLACards(
    slaStatuses: List<SLAStatus>,
    modifier: Modifier = Modifier,
    onCardClick: (String) -> Unit = {},
    maxCards: Int = 10
) {
    val sortedByPriority = slaStatuses.sortedByDescending { it.priorityLevel }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sortedByPriority.take(maxCards).forEach { slaStatus ->
            slaStatus.toSlaCountdownCard(
                onClick = { onCardClick(slaStatus.dossierId) },
                compact = false
            )
        }
    }
}

/**
 * Extension function for customer-specific SLA cards
 */
@Composable
fun CustomerSLACards(
    slaStatuses: List<SLAStatus>,
    customerId: String,
    modifier: Modifier = Modifier,
    onCardClick: (String) -> Unit = {}
) {
    val customerSLAs = slaStatuses.filter { 
        it.dossierId.contains(customerId) || 
        it.customerName.contains(customerId, ignoreCase = true)
    }
    
    if (customerSLAs.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            customerSLAs.forEach { slaStatus ->
                slaStatus.toSlaCountdownCard(
                    onClick = { onCardClick(slaStatus.dossierId) },
                    compact = false
                )
            }
        }
    }
}

/**
 * Extension function for status-specific SLA cards
 */
@Composable
fun StatusSpecificSLACards(
    slaStatuses: List<SLAStatus>,
    targetStatus: String,
    modifier: Modifier = Modifier,
    onCardClick: (String) -> Unit = {}
) {
    val filteredSLAs = slaStatuses.filter { it.status == targetStatus }
    
    if (filteredSLAs.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoBox {
                BentoHeader(
                    title = targetStatus.replace("_", " "),
                    subtitle = "${filteredSLAs.size} dossiers"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredSLAs.take(5).forEach { slaStatus ->
                        slaStatus.toSlaCountdownCard(
                            onClick = { onCardClick(slaStatus.dossierId) },
                            compact = true
                        )
                    }
                    
                    if (filteredSLAs.size > 5) {
                        TextButton(
                            onClick = { /* View all */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View all ${filteredSLAs.size} dossiers")
                        }
                    }
                }
            }
        }
    }
}

// =====================================================
// PRESET COMBINATIONS FOR COMMON USE CASES
// =====================================================

/**
 * Marketing Dashboard SLA Cards
 */
@Composable
fun MarketingDashboardSLACards(
    slaStatuses: List<SLAStatus>,
    modifier: Modifier = Modifier,
    onCardClick: (String) -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Critical and Overdue First
        val urgentSLAs = slaStatuses.filter { 
            it.isDocOverdue || it.isBankOverdue || 
            it.docDaysLeft <= 3 || it.bankDaysLeft <= 3
        }
        
        if (urgentSLAs.isNotEmpty()) {
            BentoBox {
                BentoHeader(
                    title = "Urgent Attention Required",
                    subtitle = "${urgentSLAs.size} dossiers need immediate action"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    urgentSLAs.take(3).forEach { slaStatus ->
                        slaStatus.toSlaCountdownCard(
                            onClick = { onCardClick(slaStatus.dossierId) },
                            compact = true
                        )
                    }
                    
                    if (urgentSLAs.size > 3) {
                        TextButton(
                            onClick = { /* View all urgent */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View all ${urgentSLAs.size} urgent dossiers")
                        }
                    }
                }
            }
        }
        
        // Warning Level
        val warningSLAs = slaStatuses.filter { 
            !it.isDocOverdue && !it.isBankOverdue &&
            (it.docDaysLeft in 4..7 || it.bankDaysLeft in 4..7)
        }
        
        if (warningSLAs.isNotEmpty()) {
            BentoBox {
                BentoHeader(
                    title = "Warning Level",
                    subtitle = "${warningSLAs.size} dossiers approaching deadlines"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    warningSLAs.take(3).forEach { slaStatus ->
                        slaStatus.toSlaCountdownCard(
                            onClick = { onCardClick(slaStatus.dossierId) },
                            compact = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * Legal Dashboard SLA Cards
 */
@Composable
fun LegalDashboardSLACards(
    slaStatuses: List<SLAStatus>,
    modifier: Modifier = Modifier,
    onCardClick: (String) -> Unit = {}
) {
    val documentSLAs = slaStatuses.filter { 
        it.status in listOf("PEMBERKASAN", "PROSES_BANK")
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        documentSLAs.forEach { slaStatus ->
            DocumentSlaCard(
                daysRemaining = slaStatus.docDaysLeft,
                onClick = { onCardClick(slaStatus.dossierId) },
                compact = false
            )
        }
    }
}

/**
 * Finance Dashboard SLA Cards
 */
@Composable
fun FinanceDashboardSLACards(
    slaStatuses: List<SLAStatus>,
    modifier: Modifier = Modifier,
    onCardClick: (String) -> Unit = {}
) {
    val bankSLAs = slaStatuses.filter { 
        it.status in listOf("PROSES_BANK", "PUTUSAN_KREDIT_ACC", "SP3K_TERBIT", "PRA_AKAD", "AKAD_BELUM_CAIR")
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        bankSLAs.forEach { slaStatus ->
            BankSlaCard(
                daysRemaining = slaStatus.bankDaysLeft,
                onClick = { onCardClick(slaStatus.dossierId) },
                compact = false
            )
        }
    }
}

// =====================================================
// UTILITY FUNCTIONS
// =====================================================

/**
 * Get SLA status color for quick reference
 */
fun SLAStatus.getStatusColor(): androidx.compose.ui.graphics.Color {
    return when {
        isDocOverdue || isBankOverdue -> com.kprflow.enterprise.ui.theme.Error
        docDaysLeft <= 3 || bankDaysLeft <= 3 -> Color(0xFFFF6F00)
        docDaysLeft <= 7 || bankDaysLeft <= 7 -> com.kprflow.enterprise.ui.theme.Warning
        else -> com.kprflow.enterprise.ui.theme.Success
    }
}

/**
 * Get SLA status text for display
 */
fun SLAStatus.getStatusText(): String {
    return when {
        isDocOverdue || isBankOverdue -> "Overdue"
        docDaysLeft <= 3 || bankDaysLeft <= 3 -> "Critical"
        docDaysLeft <= 7 || bankDaysLeft <= 7 -> "Warning"
        else -> "On Track"
    }
}

/**
 * Check if SLA needs attention
 */
fun SLAStatus.needsAttention(): Boolean {
    return isDocOverdue || isBankOverdue || 
           docDaysLeft <= 7 || bankDaysLeft <= 7
}

/**
 * Get priority level for sorting
 */
fun SLAStatus.getPriority(): Int {
    return when {
        isDocOverdue || isBankOverdue -> 4
        docDaysLeft <= 3 || bankDaysLeft <= 3 -> 3
        docDaysLeft <= 7 || bankDaysLeft <= 7 -> 2
        else -> 1
    }
}
