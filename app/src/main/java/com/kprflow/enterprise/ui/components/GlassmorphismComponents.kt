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
import androidx.compose.ui.draw.blur
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
 * Reusable Glassmorphism Components for KPRFlow Enterprise
 * Premium UI components with glass-like effects
 */

// =====================================================
// CORE GLASSMORPHISM COMPONENTS
// =====================================================

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 16.dp,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Card(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = GlassShadow,
                spotColor = GlassShadow
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.8f)
                    )
                ),
                shape
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = shape
            )
            .blur(blurRadius)
            .then(
                onClick?.let { clickable(onClick = it) } ?: Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            content = content
        )
    }
}

@Composable
fun GlassSurfaceSmall(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassLight,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        backgroundColor = backgroundColor,
        cornerRadius = 12.dp,
        blurRadius = 12.dp,
        padding = 12.dp,
        onClick = onClick,
        content = content
    )
}

@Composable
fun GlassSurfaceLarge(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassMedium,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        backgroundColor = backgroundColor,
        cornerRadius = 20.dp,
        blurRadius = 20.dp,
        padding = 24.dp,
        onClick = onClick,
        content = content
    )
}

// =====================================================
// GLASSMORPHISM CARDS
// =====================================================

@Composable
fun GlassCard(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    backgroundColor: Color = GlassSurface,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassSurface(
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
                    style = MaterialTheme.typography.titleMedium,
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
            
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun GlassStatsCard(
    title: String,
    value: String,
    change: String? = null,
    isPositive: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color = GlassSurface,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier,
        backgroundColor = backgroundColor
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
                
                change?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPositive) Success else Error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun GlassActionCard(
    title: String,
    description: String,
    actionText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color = GlassSurface,
    modifier: Modifier = Modifier
) {
    GlassSurface(
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            ) {
                Text(text = actionText)
            }
        }
    }
}

// =====================================================
// GLASSMORPHISM CONTAINERS
// =====================================================

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassBackground,
    borderColor: Color = GlassBorder,
    cornerRadius: Dp = 20.dp,
    blurRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.6f)
                    )
                ),
                shape
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
            .blur(blurRadius),
        content = content
    )
}

@Composable
fun GlassPanel(
    title: String,
    backgroundColor: Color = GlassSurface,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        backgroundColor = backgroundColor,
        padding = 20.dp
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        content()
    }
}

// =====================================================
// GLASSMORPHISM LIST COMPONENTS
// =====================================================

@Composable
fun GlassListItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    backgroundColor: Color = GlassLight,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassSurfaceSmall(
        modifier = modifier,
        backgroundColor = backgroundColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp).padding(end = 12.dp)
                    )
                }
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
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
            }
            
            trailing?.invoke()
        }
    }
}

// =====================================================
// GLASSMORPHISM CHART COMPONENTS
// =====================================================

@Composable
fun GlassChartCard(
    title: String,
    chart: @Composable () -> Unit,
    subtitle: String? = null,
    backgroundColor: Color = GlassSurface,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier,
        backgroundColor = backgroundColor,
        padding = 20.dp
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            chart()
        }
    }
}

// =====================================================
// GLASSMORPHISM FORM COMPONENTS
// =====================================================

@Composable
fun GlassFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    backgroundColor: Color = GlassLight,
    modifier: Modifier = Modifier
) {
    GlassSurfaceSmall(
        modifier = modifier,
        backgroundColor = backgroundColor
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = GlassBorder,
                    containerColor = Color.Transparent
                )
            )
        }
    }
}

// =====================================================
// GLASSMORPHISM NAVIGATION COMPONENTS
// =====================================================

@Composable
fun GlassTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    backgroundColor: Color = GlassLight,
    modifier: Modifier = Modifier
) {
    GlassSurfaceSmall(
        modifier = modifier,
        backgroundColor = if (isSelected) 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
        else 
            backgroundColor,
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =====================================================
// GLASSMORPHISM STATUS COMPONENTS
// =====================================================

@Composable
fun GlassStatusChip(
    status: String,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = GlassLight,
    modifier: Modifier = Modifier
) {
    GlassSurfaceSmall(
        modifier = modifier,
        backgroundColor = backgroundColor
    ) {
        Surface(
            color = color.copy(alpha = 0.2f),
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
}

// =====================================================
// ANIMATED GLASSMORPHISM
// =====================================================

@Composable
fun AnimatedGlassCard(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    backgroundColor: Color = GlassSurface,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    GlassSurface(
        modifier = modifier
            .clickable {
                isPressed = true
                onClick()
            },
        backgroundColor = if (isPressed) 
            backgroundColor.copy(alpha = 0.6f) 
        else 
            backgroundColor,
        blurRadius = if (isPressed) 8.dp else 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
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
            
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}
