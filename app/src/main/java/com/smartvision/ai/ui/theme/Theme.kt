package com.smartvision.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
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

@Composable
fun SmartVisionTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val extendedColors = ExtendedColors(
        neonBlue      = if (darkTheme) NeonBlue      else LightNeonBlue,
        neonPurple    = if (darkTheme) NeonPurple    else LightNeonPurple,
        neonCyan      = if (darkTheme) NeonCyan      else LightNeonBlue,
        neonGreen     = NeonGreen,
        neonPink      = NeonPink,
        neonOrange    = NeonOrange,
        neonYellow    = AccentQR,
        glassCard     = if (darkTheme) GlassCard     else LightGlassCard,
        glassBorder   = if (darkTheme) GlassBorder   else LightGlassBorder,
        cardBackground= if (darkTheme) NavyCard      else LightCard,
        textHint      = if (darkTheme) TextHint      else LightTextHint,
        isDark        = darkTheme
    )

    // Status bar styling — transparent edge-to-edge
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = SmartVisionTypography,
            content     = content
        )
    }
}

// Convenience accessor
val MaterialTheme.extended: ExtendedColors
    @Composable get() = LocalExtendedColors.current
