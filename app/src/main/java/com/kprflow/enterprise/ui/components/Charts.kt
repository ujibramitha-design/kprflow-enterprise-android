package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// Data classes for charts
data class ChartData(
    val label: String,
    val value: Float,
    val color: Color = Color.Blue
)

data class PieChartData(
    val label: String,
    val value: Float,
    val color: Color
)

data class LineChartData(
    val label: String,
    val value: Float,
    val color: Color = Color.Blue
)

// Pie Chart Component
@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    title: String = "",
    showLegend: Boolean = true
) {
    val density = LocalDensity.current
    val totalValue = data.sumOf { it.value }
    
    Column(
        modifier = modifier
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pie Chart
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                drawPieChart(data, totalValue)
            }
            
            // Legend
            if (showLegend) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(item.color)
                            )
                            
                            Column {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${(item.value / totalValue * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Bar Chart Component
@Composable
fun BarChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    title: String = "",
    maxValue: Float? = null,
    showValues: Boolean = true
) {
    val maxBarValue = maxValue ?: data.maxOfOrNull { it.value } ?: 0f
    
    Column(
        modifier = modifier
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            data.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((item.value / maxBarValue).coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(item.color)
                        )
                    }
                    
                    if (showValues) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.value.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

// Line Chart Component
@Composable
fun LineChart(
    data: List<LineChartData>,
    modifier: Modifier = Modifier,
    title: String = "",
    showPoints: Boolean = true,
    showGrid: Boolean = true
) {
    val density = LocalDensity.current
    
    Column(
        modifier = modifier
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            drawLineChart(data, showPoints, showGrid)
        }
    }
}

// Funnel Chart Component
@Composable
fun FunnelChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    title: String = "",
    showValues: Boolean = true,
    showPercentages: Boolean = true
) {
    val maxValue = data.maxOfOrNull { it.value } ?: 0f
    
    Column(
        modifier = modifier
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            data.forEachIndexed { index, item ->
                val widthPercentage = (item.value / maxValue).coerceIn(0f, 1f)
                val previousPercentage = if (index > 0) {
                    (data[index - 1].value / maxValue).coerceIn(0f, 1f)
                } else {
                    1f
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(100.dp),
                        textAlign = TextAlign.End
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Funnel segment
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        // Background (previous segment)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .fillMaxWidth(previousPercentage)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        
                        // Current segment
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .fillMaxWidth(widthPercentage)
                                .clip(RoundedCornerShape(4.dp))
                                .background(item.color)
                        )
                    }
                    
                    if (showValues || showPercentages) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier = Modifier.width(60.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            if (showValues) {
                                Text(
                                    text = item.value.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (showPercentages && index > 0) {
                                val percentage = ((item.value / data[0].value) * 100).toInt()
                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Progress Chart Component
@Composable
fun ProgressChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    title: String = "",
    showLabels: Boolean = true
) {
    Column(
        modifier = modifier
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            data.forEach { item ->
                Column {
                    if (showLabels) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${item.value.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    LinearProgressIndicator(
                        progress = (item.value / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = item.color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

// Extension function to draw pie chart
private fun DrawScope.drawPieChart(data: List<PieChartData>, totalValue: Float) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val centerX = canvasWidth / 2
    val centerY = canvasHeight / 2
    val radius = minOf(centerX, centerY) * 0.8f
    
    var startAngle = -90f // Start from top
    
    data.forEach { item ->
        val sweepAngle = (item.value / totalValue) * 360f
        
        drawArc(
            color = item.color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2)
        )
        
        startAngle += sweepAngle
    }
}

// Extension function to draw line chart
private fun DrawScope.drawLineChart(
    data: List<LineChartData>,
    showPoints: Boolean,
    showGrid: Boolean
) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val padding = 20f
    val chartWidth = canvasWidth - padding * 2
    val chartHeight = canvasHeight - padding * 2
    
    val maxValue = data.maxOfOrNull { it.value } ?: 0f
    val minValue = 0f
    val valueRange = maxValue - minValue
    
    if (showGrid) {
        // Draw grid lines
        val gridColor = Color.Gray.copy(alpha = 0.3f)
        
        // Horizontal grid lines
        for (i in 0..5) {
            val y = padding + (chartHeight / 5) * i
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(canvasWidth - padding, y),
                strokeWidth = 1f
            )
        }
        
        // Vertical grid lines
        for (i in 0..data.size) {
            val x = padding + (chartWidth / data.size) * i
            drawLine(
                color = gridColor,
                start = Offset(x, padding),
                end = Offset(x, canvasHeight - padding),
                strokeWidth = 1f
            )
        }
    }
    
    // Draw line chart
    if (data.isNotEmpty()) {
        val points = mutableListOf<Offset>()
        
        data.forEachIndexed { index, item ->
            val x = padding + (chartWidth / (data.size - 1)) * index
            val y = padding + chartHeight - ((item.value - minValue) / valueRange) * chartHeight
            points.add(Offset(x, y))
        }
        
        // Draw line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = data[i].color,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3f
            )
        }
        
        // Draw points
        if (showPoints) {
            points.forEachIndexed { index, point ->
                drawCircle(
                    color = data[index].color,
                    radius = 6f,
                    center = point
                )
            }
        }
    }
}

// Stats Card with Chart
@Composable
fun StatsCardWithChart(
    title: String,
    value: String,
    subtitle: String,
    chartData: List<ChartData>,
    modifier: Modifier = Modifier,
    chartType: ChartType = ChartType.BAR
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (chartType) {
                ChartType.BAR -> {
                    BarChart(
                        data = chartData,
                        modifier = Modifier.height(100.dp),
                        showValues = false
                    )
                }
                ChartType.LINE -> {
                    LineChart(
                        data = chartData.map { LineChartData(it.label, it.value, it.color) },
                        modifier = Modifier.height(100.dp),
                        showPoints = false,
                        showGrid = false
                    )
                }
                ChartType.PROGRESS -> {
                    ProgressChart(
                        data = chartData,
                        modifier = Modifier.height(80.dp),
                        showLabels = false
                    )
                }
            }
        }
    }
}

// Chart type enum
enum class ChartType {
    BAR,
    LINE,
    PROGRESS
}
