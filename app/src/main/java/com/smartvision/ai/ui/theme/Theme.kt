package com.smartvision.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ─── Dark Neon Color Scheme ───────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary             = NeonBlue,
    onPrimary           = DeepNavy,
    primaryContainer    = NavyCard,
    onPrimaryContainer  = TextPrimary,

    secondary           = NeonPurple,
    onSecondary         = DeepNavy,
    secondaryContainer  = NavyCardDark,
    onSecondaryContainer= TextPrimary,

    tertiary            = NeonCyan,
    onTertiary          = DeepNavy,
    tertiaryContainer   = Color(0xFF0D1F3C),
    onTertiaryContainer = TextPrimary,

    background          = DeepNavy,
    onBackground        = TextPrimary,
    surface             = NavyCard,
    onSurface           = TextPrimary,
    surfaceVariant      = GlassCard,
    onSurfaceVariant    = TextSecondary,

    outline             = GlassBorder,
    outlineVariant      = DividerDark,

    error               = NeonPink,
    onError             = DeepNavy,
    errorContainer      = Color(0x44FF2D78),
    onErrorContainer    = NeonPink,

    inverseSurface      = TextPrimary,
    inverseOnSurface    = DeepNavy,
    inversePrimary      = NeonBlueDim
)

// ─── Light Color Scheme ───────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary             = LightNeonBlue,
    onPrimary           = LightSurface,
    primaryContainer    = LightCard,
    onPrimaryContainer  = LightTextPrimary,

    secondary           = LightNeonPurple,
    onSecondary         = LightSurface,
    secondaryContainer  = LightCard,
    onSecondaryContainer= LightTextPrimary,

    tertiary            = LightNeonBlue,
    onTertiary          = LightSurface,
    tertiaryContainer   = LightCard,
    onTertiaryContainer = LightTextPrimary,

    background          = LightBackground,
    onBackground        = LightTextPrimary,
    surface             = LightSurface,
    onSurface           = LightTextPrimary,
    surfaceVariant      = LightCard,
    onSurfaceVariant    = LightTextSecondary,

    outline             = LightCardBorder,
    outlineVariant      = LightDivider,

    error               = NeonPink,
    onError             = LightSurface,
    errorContainer      = Color(0xFFFFE5EE),
    onErrorContainer    = Color(0xFFAA0040),

    inverseSurface      = LightTextPrimary,
    inverseOnSurface    = LightSurface,
    inversePrimary      = LightNeonBlue
)

// ─── Extended colors (neon accents not in M3 spec) ───────────────────────────
data class ExtendedColors(
    val neonBlue: Color,
    val neonPurple: Color,
    val neonCyan: Color,
    val neonGreen: Color,
    val neonPink: Color,
    val neonOrange: Color,
    val neonYellow: Color,
    val glassCard: Color,
    val glassBorder: Color,
    val cardBackground: Color,
    val textHint: Color,
    val isDark: Boolean
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        neonBlue      = NeonBlue,
        neonPurple    = NeonPurple,
        neonCyan      = NeonCyan,
        neonGreen     = NeonGreen,
        neonPink      = NeonPink,
        neonOrange    = NeonOrange,
        neonYellow    = AccentQR,
        glassCard     = GlassCard,
        glassBorder   = GlassBorder,
        cardBackground= NavyCard,
        textHint      = TextHint,
        isDark        = true
    )
}

val LocalLargeButtons = staticCompositionLocalOf { false }
val LocalHighContrast = staticCompositionLocalOf { false }
val LocalColorblindMode = staticCompositionLocalOf { "NONE" }
val LocalVoiceGuidance = staticCompositionLocalOf { false }
val LocalAiVoiceGuidance = staticCompositionLocalOf { false }

fun getAdjustedColors(
    darkTheme: Boolean,
    colorblindMode: String,
    highContrast: Boolean
): ExtendedColors {
    var blue   = if (darkTheme) NeonBlue      else LightNeonBlue
    var purple = if (darkTheme) NeonPurple    else LightNeonPurple
    var cyan   = if (darkTheme) NeonCyan      else LightNeonBlue
    var green  = NeonGreen
    var pink   = NeonPink
    var orange = NeonOrange
    var yellow = AccentQR

    when (colorblindMode) {
        "PROTANOMALY", "DEUTERANOMALY" -> {
            blue = Color(0xFF007DFF)
            purple = Color(0xFFC37FFF)
            cyan = Color(0xFF00FFFF)
            green = Color(0xFFFFFF00)
            pink = Color(0xFFFFFFFF)
            orange = Color(0xFFFFA500)
            yellow = Color(0xFFFFFF00)
        }
        "TRITANOMALY" -> {
            blue = Color(0xFFFF2D78)
            purple = Color(0xFFC37FFF)
            cyan = Color(0xFF00FF00)
            green = Color(0xFF00FF00)
            pink = Color(0xFFFF2D78)
            orange = Color(0xFFFF5722)
            yellow = Color(0xFFFF2D78)
        }
    }

    var glassCard = if (darkTheme) GlassCard else LightGlassCard
    var glassBorder = if (darkTheme) GlassBorder else LightGlassBorder
    var cardBackground = if (darkTheme) NavyCard else LightCard
    var textHint = if (darkTheme) TextHint else LightTextHint

    if (highContrast) {
        glassCard = if (darkTheme) Color.Black else Color.White
        glassBorder = if (darkTheme) Color.White else Color.Black
        cardBackground = if (darkTheme) Color.Black else Color.White
        textHint = if (darkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
        blue = if (darkTheme) Color(0xFF00E5FF) else Color(0xFF005F73)
        purple = if (darkTheme) Color(0xFFD500F9) else Color(0xFF4A0072)
        cyan = if (darkTheme) Color(0xFF00FFFF) else Color(0xFF005F73)
        green = if (darkTheme) Color(0xFF00E676) else Color(0xFF1B5E20)
        pink = if (darkTheme) Color(0xFFFF1744) else Color(0xFFB71C1C)
        orange = if (darkTheme) Color(0xFFFF9100) else Color(0xFFE65100)
        yellow = if (darkTheme) Color(0xFFFFEA00) else Color(0xFFF57F17)
    }

    return ExtendedColors(
        neonBlue = blue,
        neonPurple = purple,
        neonCyan = cyan,
        neonGreen = green,
        neonPink = pink,
        neonOrange = orange,
        neonYellow = yellow,
        glassCard = glassCard,
        glassBorder = glassBorder,
        cardBackground = cardBackground,
        textHint = textHint,
        isDark = darkTheme
    )
}

fun getScaledTypography(multiplier: Float): Typography {
    return Typography(
        displayLarge = SmartVisionTypography.displayLarge.copy(fontSize = (36 * multiplier).sp, lineHeight = (44 * multiplier).sp),
        displayMedium = SmartVisionTypography.displayMedium.copy(fontSize = (28 * multiplier).sp, lineHeight = (34 * multiplier).sp),
        headlineLarge = SmartVisionTypography.headlineLarge.copy(fontSize = (24 * multiplier).sp, lineHeight = (30 * multiplier).sp),
        headlineMedium = SmartVisionTypography.headlineMedium.copy(fontSize = (20 * multiplier).sp, lineHeight = (26 * multiplier).sp),
        headlineSmall = SmartVisionTypography.headlineSmall.copy(fontSize = (18 * multiplier).sp, lineHeight = (24 * multiplier).sp),
        titleLarge = SmartVisionTypography.titleLarge.copy(fontSize = (16 * multiplier).sp, lineHeight = (22 * multiplier).sp),
        titleMedium = SmartVisionTypography.titleMedium.copy(fontSize = (15 * multiplier).sp, lineHeight = (20 * multiplier).sp),
        titleSmall = SmartVisionTypography.titleSmall.copy(fontSize = (14 * multiplier).sp, lineHeight = (19 * multiplier).sp),
        bodyLarge = SmartVisionTypography.bodyLarge.copy(fontSize = (16 * multiplier).sp, lineHeight = (24 * multiplier).sp),
        bodyMedium = SmartVisionTypography.bodyMedium.copy(fontSize = (14 * multiplier).sp, lineHeight = (20 * multiplier).sp),
        bodySmall = SmartVisionTypography.bodySmall.copy(fontSize = (12 * multiplier).sp, lineHeight = (16 * multiplier).sp),
        labelLarge = SmartVisionTypography.labelLarge.copy(fontSize = (14 * multiplier).sp, lineHeight = (18 * multiplier).sp),
        labelMedium = SmartVisionTypography.labelMedium.copy(fontSize = (12 * multiplier).sp, lineHeight = (16 * multiplier).sp),
        labelSmall = SmartVisionTypography.labelSmall.copy(fontSize = (11 * multiplier).sp, lineHeight = (14 * multiplier).sp)
    )
}

@Composable
fun SmartVisionTheme(
    darkTheme: Boolean = true,
    fontSizeMultiplier: Float = 1.0f,
    highContrast: Boolean = false,
    colorblindMode: String = "NONE",
    largeButtons: Boolean = false,
    voiceGuidance: Boolean = false,
    aiVoiceGuidance: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = if (highContrast) {
        if (darkTheme) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White,
                surfaceVariant = Color(0xFF121212),
                onSurfaceVariant = Color.White,
                outline = Color.White
            )
        } else {
            baseScheme.copy(
                background = Color.White,
                surface = Color.White,
                onBackground = Color.Black,
                onSurface = Color.Black,
                surfaceVariant = Color(0xFFF5F5F5),
                onSurfaceVariant = Color.Black,
                outline = Color.Black
            )
        }
    } else {
        baseScheme
    }

    val extendedColors = getAdjustedColors(darkTheme, colorblindMode, highContrast)
    val scaledTypography = getScaledTypography(fontSizeMultiplier)

    // Status bar styling — transparent edge-to-edge
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalLargeButtons provides largeButtons,
        LocalHighContrast provides highContrast,
        LocalColorblindMode provides colorblindMode,
        LocalVoiceGuidance provides voiceGuidance,
        LocalAiVoiceGuidance provides aiVoiceGuidance
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = scaledTypography,
            content     = content
        )
    }
}

// Convenience accessor
val MaterialTheme.extended: ExtendedColors
    @Composable get() = LocalExtendedColors.current
