package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kprflow.enterprise.ui.theme.*

/**
 * Reusable Bento UI Components for KPRFlow Enterprise
 * Eliminates code duplication across 8 different dashboards
 */

// =====================================================
// BENTO BOX COMPONENTS
// =====================================================

@Composable
fun BentoBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BentoSurface,
    borderColor: Color = BentoBorder,
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 12.dp,
    elevation: Dp = 2.dp,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Card(
        modifier = modifier
            .shadow(elevation, shape)
            .then(
                onClick?.let { clickable(onClick = it) } ?: Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Shadow handled separately
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = shape
                ),
            content = content
        )
    }
}

@Composable
fun BentoBoxSmall(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BentoSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BentoBox(
        modifier = modifier,
        backgroundColor = backgroundColor,
        cornerRadius = 8.dp,
        elevation = 1.dp,
        padding = 12.dp,
        onClick = onClick,
        content = content
    )
}

@Composable
fun BentoBoxLarge(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BentoSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BentoBox(
        modifier = modifier,
        backgroundColor = backgroundColor,
        cornerRadius = 16.dp,
        elevation = 4.dp,
        padding = 20.dp,
        onClick = onClick,
        content = content
    )
}

// =====================================================
// GLASSMORPHISM COMPONENTS
// =====================================================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 16.dp,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = GlassShadow,
                spotColor = GlassShadow
            )
            .then(
                onClick?.let { clickable(onClick = it) } ?: Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = shape
                ),
            content = content
        )
    }
}

@Composable
fun GlassCardSmall(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassLight,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        backgroundColor = backgroundColor,
        cornerRadius = 12.dp,
        padding = 12.dp,
        onClick = onClick,
        content = content
    )
}

@Composable
fun GlassCardLarge(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassMedium,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        backgroundColor = backgroundColor,
        cornerRadius = 20.dp,
        padding = 24.dp,
        onClick = onClick,
        content = content
    )
}

// =====================================================
// BENTO GRID COMPONENTS
// =====================================================

@Composable
fun BentoGrid(
    modifier: Modifier = Modifier,
    columns: Int = 2,
    horizontalSpacing: Dp = 12.dp,
    verticalSpacing: Dp = 12.dp,
    content: @Composable (BentoGridScope.() -> Unit)
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content
    )
}

@Composable
fun BentoGridSmall(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable (BentoGridScope.() -> Unit)
) {
    BentoGrid(
        modifier = modifier,
        columns = 3,
        horizontalSpacing = horizontalSpacing,
        verticalSpacing = verticalSpacing,
        content = content
    )
}

@Composable
fun BentoGridLarge(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 16.dp,
    verticalSpacing: Dp = 16.dp,
    content: @Composable (BentoGridScope.() -> Unit)
) {
    BentoGrid(
        modifier = modifier,
        columns = 2,
        horizontalSpacing = horizontalSpacing,
        verticalSpacing = verticalSpacing,
        content = content
    )
}

// =====================================================
// BENTO CONTENT COMPONENTS
// =====================================================

@Composable
fun BentoHeader(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor
                )
            }
        }
        
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun BentoMetric(
    value: String,
    label: String,
    change: String? = null,
    isPositive: Boolean = true,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor
        )
        
        change?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (isPositive) Success else Error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun BentoProgress(
    progress: Float,
    label: String,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            color = color,
            trackColor = backgroundColor
        )
    }
}

@Composable
fun BentoStatus(
    status: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// =====================================================
// SPECIALIZED BENTO COMPONENTS
// =====================================================

@Composable
fun BentoStatsCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = BentoSurface,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BentoBox(
        modifier = modifier,
        backgroundColor = backgroundColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun BentoActionCard(
    title: String,
    description: String,
    actionText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color = BentoSurface,
    modifier: Modifier = Modifier
) {
    BentoBox(
        modifier = modifier,
        backgroundColor = backgroundColor,
        onClick = onClick
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = actionText)
            }
        }
    }
}

@Composable
fun BentoChartCard(
    title: String,
    chart: @Composable () -> Unit,
    subtitle: String? = null,
    backgroundColor: Color = BentoSurface,
    modifier: Modifier = Modifier
) {
    BentoBox(
        modifier = modifier,
        backgroundColor = backgroundColor
    ) {
        BentoHeader(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            chart()
        }
    }
}

@Composable
fun BentoListCard(
    title: String,
    items: List<String>,
    backgroundColor: Color = BentoSurface,
    modifier: Modifier = Modifier
) {
    BentoBox(
        modifier = modifier,
        backgroundColor = backgroundColor
    ) {
        BentoHeader(
            title = title,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Column {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// =====================================================
// GRADIENT BENTO COMPONENTS
// =====================================================

@Composable
fun BentoGradientCard(
    title: String,
    subtitle: String? = null,
    gradient: Brush,
    contentColor: Color = Color.White,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .then(
                onClick?.let { clickable(onClick = it) } ?: Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// =====================================================
// RESPONSIVE BENTO COMPONENTS
// =====================================================

@Composable
fun ResponsiveBentoBox(
    modifier: Modifier = Modifier,
    content: @Composable (BoxScope.() -> Unit)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .background(
                BentoSurface,
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                BentoBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        content = content
    )
}

// =====================================================
// ANIMATED BENTO COMPONENTS
// =====================================================

@Composable
fun AnimatedBentoBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BentoSurface,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    BentoBox(
        modifier = modifier
            .clickable {
                isPressed = true
                onClick()
            },
        backgroundColor = if (isPressed) 
            backgroundColor.copy(alpha = 0.8f) 
        else 
            backgroundColor,
        elevation = if (isPressed) 1.dp else 2.dp,
        content = content
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}
