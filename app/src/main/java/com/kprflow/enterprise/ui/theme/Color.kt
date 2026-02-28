package com.kprflow.enterprise.ui.theme

import androidx.compose.ui.graphics.Color

// Legacy colors (kept for backward compatibility)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// KPRFlow Brand Colors
val KPRFlowPrimary = Color(0xFF1976D2)
val KPRFlowSecondary = Color(0xFF2196F3)
val KPRFlowTertiary = Color(0xFF03DAC6)

// Status Colors
val StatusSuccess = Color(0xFF4CAF50)
val StatusWarning = Color(0xFFFF9800)
val StatusError = Color(0xFFF44336)
val StatusInfo = Color(0xFF2196F3)

// Role-based Colors
val CustomerColor = Color(0xFF4CAF50)
val MarketingColor = Color(0xFFFF9800)
val LegalColor = Color(0xFF2196F3)
val FinanceColor = Color(0xFF9C27B0)
val BODColor = Color(0xFFF44336)

// =====================================================
// KPRFLOW ENTERPRISE COLORS - PHASE 15 STANDARD
// =====================================================

// Sapphire Blue (Trust) - Primary Brand Color
val SapphireBlue = Color(0xFF0F4C75)
val SapphireBlueLight = Color(0xFF1565C0)
val SapphireBlueDark = Color(0xFF0A3D5E)
val SapphireBlueSurface = Color(0xFFE3F2FD)

// Emerald (Success) - Secondary Brand Color
val Emerald = Color(0xFF00BFA5)
val EmeraldLight = Color(0xFF1ED8C1)
val EmeraldDark = Color(0xFF00897B)
val EmeraldSurface = Color(0xFFE0F2F1)

// Extended Color Palette
val SapphireBlueVariant = Color(0xFF1976D2)
val EmeraldVariant = Color(0xFF26A69A)

// Glassmorphism Colors
val GlassBackground = Color(0x40FFFFFF) // 25% opacity white
val GlassSurface = Color(0x60FFFFFF)   // 38% opacity white
val GlassBorder = Color(0x80FFFFFF)    // 50% opacity white
val GlassShadow = Color(0x20000000)    // 12% opacity black

// Bento UI Colors
val BentoBackground = Color(0xFFF5F5F5)
val BentoSurface = Color(0xFFFFFFFF)
val BentoBorder = Color(0xFFE0E0E0)
val BentoShadow = Color(0x1A000000)    // 10% opacity black

// Semantic Colors (Enterprise)
val Success = Emerald
val SuccessLight = EmeraldLight
val SuccessDark = EmeraldDark
val SuccessSurface = EmeraldSurface

val Info = SapphireBlue
val InfoLight = SapphireBlueLight
val InfoDark = SapphireBlueDark
val InfoSurface = SapphireBlueSurface

val Warning = StatusWarning
val WarningLight = Color(0xFFFFB74D)
val WarningDark = Color(0xFFF57C00)
val WarningSurface = Color(0xFFFFF8E1)

val Error = StatusError
val ErrorLight = Color(0xFFEF5350)
val ErrorDark = Color(0xFFC62828)
val ErrorSurface = Color(0xFFFFEBEE)

// Neutral Colors
val Neutral50 = Color(0xFFFAFAFA)
val Neutral100 = Color(0xFFF5F5F5)
val Neutral200 = Color(0xFFEEEEEE)
val Neutral300 = Color(0xFFE0E0E0)
val Neutral400 = Color(0xFFBDBDBD)
val Neutral500 = Color(0xFF9E9E9E)
val Neutral600 = Color(0xFF757575)
val Neutral700 = Color(0xFF616161)
val Neutral800 = Color(0xFF424242)
val Neutral900 = Color(0xFF212121)

// Surface Colors
val Surface1 = Color(0xFFFFFFFF)
val Surface2 = Color(0xFFF8F9FA)
val Surface3 = Color(0xFFF1F3F4)
val Surface4 = Color(0xFFE8EAED)
val Surface5 = Color(0xFFDADCE0)

// Interactive Colors
val InteractiveHover = Color(0x0A000000)
val InteractivePressed = Color(0x14000000)
val InteractiveFocused = Color(0x1F000000)
val InteractiveDisabled = Color(0x61000000)

// Gradient Colors
val SapphireBlueGradient = listOf(SapphireBlueLight, SapphireBlue)
val EmeraldGradient = listOf(EmeraldLight, Emerald)
val SunsetGradient = listOf(Color(0xFFFF6B6B), Color(0xFF4ECDC4))
val OceanGradient = listOf(Color(0xFF667EEA), Color(0xFF764BA2))

// Glassmorphism Variants
val GlassLight = Color(0x30FFFFFF)    // 19% opacity
val GlassMedium = Color(0x50FFFFFF)   // 31% opacity
val GlassHeavy = Color(0x70FFFFFF)    // 44% opacity

// Bento UI Variants
val BentoPrimary = Color(0xFFFFFFFF)
val BentoSecondary = Color(0xFFF8F9FA)
val BentoTertiary = Color(0xFFF1F3F4)
val BentoAccent = Color(0xFFE3F2FD)

// Shadow Colors
val ShadowLight = Color(0x0F000000)    // 6% opacity
val ShadowMedium = Color(0x1A000000)   // 10% opacity
val ShadowHeavy = Color(0x26000000)    // 15% opacity
val ShadowExtraHeavy = Color(0x40000000) // 25% opacity

// Border Colors
val BorderLight = Color(0x1F000000)    // 12% opacity
val BorderMedium = Color(0x33000000)   // 20% opacity
val BorderHeavy = Color(0x4D000000)    // 30% opacity

// Status Color Extensions
val SuccessContainer = Color(0xFFC8E6C9)
val SuccessOnContainer = Color(0xFF1B5E20)

val InfoContainer = Color(0xFFBBDEFB)
val InfoOnContainer = Color(0xFF0D47A1)

val WarningContainer = Color(0xFFFFE082)
val WarningOnContainer = Color(0xFFE65100)

val ErrorContainer = Color(0xFFFFCDD2)
val ErrorOnContainer = Color(0xFFB71C1C)
