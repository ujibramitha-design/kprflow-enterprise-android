package com.kprflow.enterprise.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kprflow.enterprise.R

// =====================================================
// PLUS JAKARTA SANS TYPEFACE
// Phase 15 Standard - Enterprise Typography
// =====================================================

val PlusJakartaSansFont = FontFamily(
    Font(R.font.plus_jakarta_sans_light, FontWeight.Light),
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

// =====================================================
// TYPOGRAPHY SYSTEM
// Custom typography for KPRFlow Enterprise
// =====================================================

val KPRFlowTypography = Typography(
    // Display Styles - For large numbers and headlines
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    
    // Headline Styles - For section headers
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // Title Styles - For card titles and important labels
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // Body Styles - For content text
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    
    // Label Styles - For buttons, tags, and metadata
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// =====================================================
// SPECIALIZED TYPOGRAPHY FOR SLA COMPONENTS
// =====================================================

object SLATypography {
    // Countdown numbers - Bold and prominent
    val CountdownLarge = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1).sp
    )
    
    val CountdownMedium = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp
    )
    
    val CountdownSmall = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp
    )
    
    // Status labels - Clear and readable
    val StatusLabel = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    
    // Card titles - Medium weight for hierarchy
    val CardTitle = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    )
    
    // Card subtitle - Light weight for secondary info
    val CardSubtitle = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    
    // Metric values - Bold for emphasis
    val MetricValue = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )
    
    // Metric labels - Regular for context
    val MetricLabel = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
}

// =====================================================
// GLASSMORPHISM TYPOGRAPHY
// Enhanced readability on glass surfaces
// =====================================================

object GlassTypography {
    // Headers on glass - Extra bold for contrast
    val GlassHeader = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )
    
    // Body text on glass - Medium for readability
    val GlassBody = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    
    // Labels on glass - SemiBold for emphasis
    val GlassLabel = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    
    // Numbers on glass - Bold for prominence
    val GlassNumbers = TextStyle(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.25).sp
    )
}

// =====================================================
// TYPOGRAPHY EXTENSIONS
// Easy access to common styles
// =====================================================

fun TextStyle.toCountdownStyle(): TextStyle {
    return this.copy(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    )
}

fun TextStyle.toGlassStyle(): TextStyle {
    return this.copy(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Medium
    )
}

fun TextStyle.toCardStyle(): TextStyle {
    return this.copy(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    )
}
