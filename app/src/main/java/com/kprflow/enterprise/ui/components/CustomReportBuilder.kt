package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.viewmodel.CustomReportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomReportBuilder(
    onBackClick: () -> Unit,
    onGenerateReport: (String) -> Unit,
    viewModel: CustomReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableMetrics by viewModel.availableMetrics.collectAsState()
    val selectedMetrics by viewModel.selectedMetrics.collectAsState()
    val reportConfig by viewModel.reportConfig.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Custom Report Builder",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Report Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Report Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Report Title
                OutlinedTextField(
                    value = reportConfig.title,
                    onValueChange = { viewModel.updateReportConfig(title = it) },
                    label = { Text("Report Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Report Description
                OutlinedTextField(
                    value = reportConfig.description,
                    onValueChange = { viewModel.updateReportConfig(description = it) },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Date Range
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start Date
                    OutlinedTextField(
                        value = reportConfig.startDate,
                        onValueChange = { viewModel.updateReportConfig(startDate = it) },
                        label = { Text("Start Date") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // End Date
                    OutlinedTextField(
                        value = reportConfig.endDate,
                        onValueChange = { viewModel.updateReportConfig(endDate = it) },
                        label = { Text("End Date") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Report Type
                Text(
                    text = "Report Type",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("Summary", "Detailed", "Financial", "Performance")) { type ->
                        FilterChip(
                            onClick = { viewModel.updateReportConfig(reportType = type) },
                            label = { Text(type) },
                            selected = reportConfig.reportType == type
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Metrics Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TextButton(
                        onClick = { viewModel.selectAllMetrics() }
                    ) {
                        Text("Select All")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(availableMetrics) { metric ->
                        MetricSelectionItem(
                            metric = metric,
                            isSelected = selectedMetrics.contains(metric.id),
                            onToggle = { viewModel.toggleMetric(metric.id) }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Visualization Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Visualization Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Chart Types
                Text(
                    text = "Chart Types",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("Bar", "Line", "Pie", "Funnel")) { type ->
                        FilterChip(
                            onClick = { viewModel.toggleChartType(type) },
                            label = { Text(type) },
                            selected = reportConfig.chartTypes.contains(type)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Export Options
                Text(
                    text = "Export Format",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("PDF", "Excel", "CSV")) { format ->
                        FilterChip(
                            onClick = { viewModel.updateReportConfig(exportFormat = format) },
                            label = { Text(format) },
                            selected = reportConfig.exportFormat == format
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Preview Section
        if (selectedMetrics.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Report Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Preview content based on selected metrics
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(150.dp)
                    ) {
                        items(selectedMetrics.take(3)) { metricId ->
                            val metric = availableMetrics.find { it.id == metricId }
                            metric?.let {
                                PreviewMetricItem(metric = it)
                            }
                        }
                    }
                    
                    if (selectedMetrics.size > 3) {
                        Text(
                            text = "... and ${selectedMetrics.size - 3} more metrics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Generate Button
        Button(
            onClick = { 
                viewModel.generateReport { reportContent ->
                    onGenerateReport(reportContent)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedMetrics.isNotEmpty() && reportConfig.title.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Generate Report")
        }
    }
}

@Composable
private fun MetricSelectionItem(
    metric: ReportMetric,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metric.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = metric.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Badge {
            Text(metric.category)
        }
    }
}

@Composable
private fun PreviewMetricItem(
    metric: ReportMetric
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = metric.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metric.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Sample data: ${metric.sampleValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Data classes
data class ReportMetric(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val sampleValue: String
)

data class ReportConfiguration(
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val reportType: String,
    val chartTypes: Set<String>,
    val exportFormat: String
) {
    companion object {
        fun empty() = ReportConfiguration(
            title = "",
            description = "",
            startDate = "",
            endDate = "",
            reportType = "Summary",
            chartTypes = emptySet(),
            exportFormat = "PDF"
        )
    }
}
