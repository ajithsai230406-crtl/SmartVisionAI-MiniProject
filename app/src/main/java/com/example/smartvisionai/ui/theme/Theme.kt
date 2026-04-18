package com.example.smartvisionai.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand Colors (matching screenshots exactly) ──────────────────────────────
val CyanAccent       = Color(0xFF00E5CC)
val CyanDark         = Color(0xFF00B89C)
val CyanGlow         = Color(0xFF00FFE0)
val PurpleAccent     = Color(0xFF9B59F5)
val OrangeAccent     = Color(0xFFFF9800)
val RedAccent        = Color(0xFFEF5350)
val GreenAccent      = Color(0xFF26A65B)
val YellowAccent     = Color(0xFFF5C518)

// ── Background Layers ─────────────────────────────────────────────────────────
val BackgroundDark   = Color(0xFF080C0D)
val SurfaceDark      = Color(0xFF0F1518)
val CardDark         = Color(0xFF111820)
val CardBorder       = Color(0xFF1C2A32)
val NavBarBg         = Color(0xFF0F1518)

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary      = Color(0xFFEEEEEE)
val TextSecondary    = Color(0xFF8899AA)
val TextMuted        = Color(0xFF445566)

private val DarkColorScheme = darkColorScheme(
    primary          = CyanAccent,
    onPrimary        = Color(0xFF001A16),
    primaryContainer = Color(0xFF003D33),
    onPrimaryContainer = CyanGlow,
    secondary        = PurpleAccent,
    onSecondary      = Color(0xFF1A0040),
    secondaryContainer = Color(0xFF2D0073),
    onSecondaryContainer = Color(0xFFD6BEFF),
    background       = BackgroundDark,
    onBackground     = TextPrimary,
    surface          = SurfaceDark,
    onSurface        = TextPrimary,
    surfaceVariant   = CardDark,
    onSurfaceVariant = TextSecondary,
    outline          = CardBorder,
    error            = RedAccent,
    onError          = Color.White,
)

@Composable
fun SmartVisionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography   = SmartVisionTypography,
        content      = content
    )
}
