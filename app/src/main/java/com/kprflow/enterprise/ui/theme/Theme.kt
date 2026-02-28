package com.kprflow.enterprise.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// KPRFlow Enterprise Color Scheme with Sapphire Blue (Trust) and Emerald (Success)
private val KPRFlowDarkColorScheme = darkColorScheme(
    primary = SapphireBlue,
    onPrimary = Color.White,
    primaryContainer = SapphireBlueDark,
    onPrimaryContainer = Color.White,
    secondary = Emerald,
    onSecondary = Color.White,
    secondaryContainer = EmeraldDark,
    onSecondaryContainer = Color.White,
    tertiary = KPRFlowTertiary,
    onTertiary = Color.White,
    tertiaryContainer = KPRFlowTertiaryDark,
    onTertiaryContainer = Color.White,
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorDark,
    onErrorContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.DarkGray,
    onSurface = Color.White,
    surfaceVariant = Color.Gray,
    onSurfaceVariant = Color.White,
    outline = Color.Gray,
    outlineVariant = Color.DarkGray,
    scrim = Color.Black,
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = SapphireBlueLight,
    surfaceTint = SapphireBlue,
    surfaceBright = Color.LightGray,
    surfaceDim = Color.DarkGray,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.DarkGray,
    surfaceContainer = Color.Gray,
    surfaceContainerHigh = Color.LightGray,
    surfaceContainerHighest = Color.White
)

private val KPRFlowLightColorScheme = lightColorScheme(
    primary = SapphireBlue,
    onPrimary = Color.White,
    primaryContainer = SapphireBlueLight,
    onPrimaryContainer = SapphireBlue,
    secondary = Emerald,
    onSecondary = Color.White,
    secondaryContainer = EmeraldLight,
    onSecondaryContainer = Emerald,
    tertiary = KPRFlowTertiary,
    onTertiary = Color.White,
    tertiaryContainer = KPRFlowTertiaryLight,
    onTertiaryContainer = KPRFlowTertiary,
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorLight,
    onErrorContainer = StatusError,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color.LightGray,
    onSurfaceVariant = Color.DarkGray,
    outline = Color.Gray,
    outlineVariant = Color.LightGray,
    scrim = Color.Black,
    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,
    inversePrimary = SapphireBlueLight,
    surfaceTint = SapphireBlue,
    surfaceBright = Color.White,
    surfaceDim = Color.LightGray,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.LightGray,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White
)

@Composable
fun KPRFlowEnterpriseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to use KPRFlow brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> KPRFlowDarkColorScheme
        else -> KPRFlowLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Legacy theme for backward compatibility
@Composable
fun KPRFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    KPRFlowEnterpriseTheme(darkTheme, dynamicColor, content)
}
