package com.smartvision.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────────────────────────────────────
// COLOR PALETTE
// ─────────────────────────────────────────────────────────────────────────────

object SmartVisionColors {
    // Dark Theme
    val darkBackground    = Color(0xFF080C14)
    val darkSurface       = Color(0xFF0E1420)
    val darkCard          = Color(0xFF141B2D)
    val darkCardBorder    = Color(0xFF1E2A42)
    val darkPrimary       = Color(0xFF00E5FF)
    val darkSecondary     = Color(0xFF7C4DFF)
    val darkTertiary      = Color(0xFF00BFA5)
    val darkOnSurface     = Color(0xFFE8EAF6)
    val darkSubtext       = Color(0xFF8892B0)
    val darkError         = Color(0xFFFF5252)

    // Light Theme
    val lightBackground   = Color(0xFFF4F6FC)
    val lightSurface      = Color(0xFFFFFFFF)
    val lightCard         = Color(0xFFFFFFFF)
    val lightCardBorder   = Color(0xFFE8ECF5)
    val lightPrimary      = Color(0xFF0070FF)
    val lightSecondary    = Color(0xFF6200EE)
    val lightTertiary     = Color(0xFF00897B)
    val lightOnSurface    = Color(0xFF0D1B2A)
    val lightSubtext      = Color(0xFF6B7280)
    val lightError        = Color(0xFFD32F2F)

    // Blue Theme (Accent)
    val blueBackground    = Color(0xFF040D1E)
    val blueSurface       = Color(0xFF071428)
    val blueCard          = Color(0xFF0A1D36)
    val blueCardBorder    = Color(0xFF1A3A6B)
    val bluePrimary       = Color(0xFF4FC3F7)
    val blueSecondary     = Color(0xFF40C4FF)
    val blueTertiary      = Color(0xFF00E5FF)
    val blueOnSurface     = Color(0xFFE3F2FD)
    val blueSubtext       = Color(0xFF90CAF9)
    val blueError         = Color(0xFFEF9A9A)

    // Module Icon Colors
    val objectDetect      = Color(0xFF00E5FF)
    val textScanner       = Color(0xFFAA00FF)
    val translator        = Color(0xFFFFAB00)
    val voiceTranslator   = Color(0xFF00E676)
    val studentHelper     = Color(0xFF2979FF)
    val medicalScanner    = Color(0xFFFF1744)
    val wasteClassifier   = Color(0xFF00C853)
    val qrScanner         = Color(0xFFFF6D00)
}

// ─────────────────────────────────────────────────────────────────────────────
// THEME ENUM
// ─────────────────────────────────────────────────────────────────────────────

enum class AppTheme { DARK, LIGHT, BLUE }

// ─────────────────────────────────────────────────────────────────────────────
// COMPOSITION LOCAL FOR CUSTOM COLORS
// ─────────────────────────────────────────────────────────────────────────────

data class SmartVisionColorScheme(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardBorder: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val onSurface: Color,
    val subtext: Color,
    val error: Color,
    val isLight: Boolean
)

val LocalSmartVisionColors = staticCompositionLocalOf {
    SmartVisionColorScheme(
        background  = SmartVisionColors.darkBackground,
        surface     = SmartVisionColors.darkSurface,
        card        = SmartVisionColors.darkCard,
        cardBorder  = SmartVisionColors.darkCardBorder,
        primary     = SmartVisionColors.darkPrimary,
        secondary   = SmartVisionColors.darkSecondary,
        tertiary    = SmartVisionColors.darkTertiary,
        onSurface   = SmartVisionColors.darkOnSurface,
        subtext     = SmartVisionColors.darkSubtext,
        error       = SmartVisionColors.darkError,
        isLight     = false
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// TYPOGRAPHY
// ─────────────────────────────────────────────────────────────────────────────

val SmartVisionTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 32.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 28.sp),
    displaySmall  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.1.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.2.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 1.sp),
)

// ─────────────────────────────────────────────────────────────────────────────
// THEME BUILDER
// ─────────────────────────────────────────────────────────────────────────────

private fun buildMaterial3Scheme(colors: SmartVisionColorScheme): ColorScheme =
    if (colors.isLight) {
        lightColorScheme(
            primary         = colors.primary,
            secondary       = colors.secondary,
            tertiary        = colors.tertiary,
            background      = colors.background,
            surface         = colors.surface,
            onBackground    = colors.onSurface,
            onSurface       = colors.onSurface,
            error           = colors.error,
        )
    } else {
        darkColorScheme(
            primary         = colors.primary,
            secondary       = colors.secondary,
            tertiary        = colors.tertiary,
            background      = colors.background,
            surface         = colors.surface,
            onBackground    = colors.onSurface,
            onSurface       = colors.onSurface,
            error           = colors.error,
        )
    }

@Composable
fun SmartVisionTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.DARK  -> SmartVisionColorScheme(
            background  = SmartVisionColors.darkBackground,
            surface     = SmartVisionColors.darkSurface,
            card        = SmartVisionColors.darkCard,
            cardBorder  = SmartVisionColors.darkCardBorder,
            primary     = SmartVisionColors.darkPrimary,
            secondary   = SmartVisionColors.darkSecondary,
            tertiary    = SmartVisionColors.darkTertiary,
            onSurface   = SmartVisionColors.darkOnSurface,
            subtext     = SmartVisionColors.darkSubtext,
            error       = SmartVisionColors.darkError,
            isLight     = false
        )
        AppTheme.LIGHT -> SmartVisionColorScheme(
            background  = SmartVisionColors.lightBackground,
            surface     = SmartVisionColors.lightSurface,
            card        = SmartVisionColors.lightCard,
            cardBorder  = SmartVisionColors.lightCardBorder,
            primary     = SmartVisionColors.lightPrimary,
            secondary   = SmartVisionColors.lightSecondary,
            tertiary    = SmartVisionColors.lightTertiary,
            onSurface   = SmartVisionColors.lightOnSurface,
            subtext     = SmartVisionColors.lightSubtext,
            error       = SmartVisionColors.lightError,
            isLight     = true
        )
        AppTheme.BLUE  -> SmartVisionColorScheme(
            background  = SmartVisionColors.blueBackground,
            surface     = SmartVisionColors.blueSurface,
            card        = SmartVisionColors.blueCard,
            cardBorder  = SmartVisionColors.blueCardBorder,
            primary     = SmartVisionColors.bluePrimary,
            secondary   = SmartVisionColors.blueSecondary,
            tertiary    = SmartVisionColors.blueTertiary,
            onSurface   = SmartVisionColors.blueOnSurface,
            subtext     = SmartVisionColors.blueSubtext,
            error       = SmartVisionColors.blueError,
            isLight     = false
        )
    }

    CompositionLocalProvider(LocalSmartVisionColors provides colorScheme) {
        MaterialTheme(
            colorScheme = buildMaterial3Scheme(colorScheme),
            typography  = SmartVisionTypography,
            content     = content
        )
    }
}

// Shortcut accessor
val smartColors: SmartVisionColorScheme
    @Composable get() = LocalSmartVisionColors.current
