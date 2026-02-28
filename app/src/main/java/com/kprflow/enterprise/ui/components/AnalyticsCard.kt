package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsCard(
    title: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    content: @Composable (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "❌",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                content != null -> {
                    content()
                }
            }
        }
    }
}

@Composable
fun FunnelChart(
    funnel: com.kprflow.enterprise.data.repository.KPRPipelineFunnel,
    modifier: Modifier = Modifier
) {
    AnalyticsCard(
        title = "KPR Pipeline Funnel",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Overall Conversion Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overall Conversion Rate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${String.format("%.1f", funnel.overallConversionRate)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Funnel Stages
            val stages = listOf(
                "Total Leads" to funnel.totalLeads,
                "Document Collection" to funnel.documentCollection,
                "Bank Processing" to funnel.bankProcessing,
                "Credit Approved" to funnel.creditApproved,
                "SP3K Issued" to funnel.sp3kIssued,
                "Funds Disbursed" to funnel.fundsDisbursed,
                "BAST Completed" to funnel.bastCompleted
            )
            
            stages.forEachIndexed { index, (stageName, count) ->
                val conversionRate = when (index) {
                    0 -> 100.0
                    1 -> funnel.leadToDocumentRate
                    2 -> funnel.documentToBankRate
                    3 -> funnel.bankToApprovalRate
                    4 -> funnel.approvalToSp3kRate
                    5 -> funnel.sp3kToDisbursementRate
                    6 -> funnel.disbursementToBastRate
                    else -> 0.0
                }
                
                FunnelStageRow(
                    stageName = stageName,
                    count = count,
                    conversionRate = conversionRate,
                    isLast = index == stages.lastIndex
                )
            }
        }
    }
}

@Composable
private fun FunnelStageRow(
    stageName: String,
    count: Int,
    conversionRate: Double,
    isLast: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stageName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(60.dp)
            )
            
            Text(
                text = "${String.format("%.1f", conversionRate)}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = getConversionRateColor(conversionRate),
                modifier = Modifier.width(80.dp)
            )
        }
        
        // Progress bar
        LinearProgressIndicator(
            progress = { conversionRate / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = getConversionRateColor(conversionRate)
        )
        
        if (!isLast) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun RevenueChart(
    projection: com.kprflow.enterprise.data.repository.RevenueProjection,
    modifier: Modifier = Modifier
) {
    AnalyticsCard(
        title = "Revenue Projection",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Summary Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Actual Revenue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(projection.actualRevenue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Projected Revenue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(projection.projectedRevenue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Total Projected
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Projected: ",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = formatCurrency(projection.totalProjectedRevenue),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confidence Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Confidence Level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${(projection.confidenceLevel * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Projection Period
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Projection Period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${projection.projectionPeriod} months",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getConversionRateColor(rate: Double): androidx.compose.ui.graphics.Color {
    return when {
        rate >= 80 -> MaterialTheme.colorScheme.primary
        rate >= 60 -> MaterialTheme.colorScheme.secondary
        rate >= 40 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
}

private fun formatCurrency(amount: java.math.BigDecimal): String {
    return "Rp ${String.format("%,.0f", amount)}"
}
