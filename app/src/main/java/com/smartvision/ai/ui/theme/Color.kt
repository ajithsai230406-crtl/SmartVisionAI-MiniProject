package com.smartvision.ai.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

var currentAppTheme by mutableStateOf("NEON_DARK")

val DeepNavy: Color
    get() = when (currentAppTheme) {
        "PURE_BLACK" -> Color(0xFF000000)
        "LIGHT" -> Color(0xFFFFFFFF)
        else -> Color(0xFF050816)
    }

val DeepNavyAlt: Color
    get() = when (currentAppTheme) {
        "PURE_BLACK" -> Color(0xFF000000)
        "LIGHT" -> Color(0xFFF8FAFF)
        else -> Color(0xFF080D20)
    }

val NavyCard: Color
    get() = when (currentAppTheme) {
        "PURE_BLACK" -> Color(0xFF0C0C0C)
        "LIGHT" -> Color(0xFFF0F5FF)
        else -> Color(0xFF0F172A)
    }

val NavyCardDark: Color
    get() = when (currentAppTheme) {
        "PURE_BLACK" -> Color(0xFF050505)
        "LIGHT" -> Color(0xFFE5EEFF)
        else -> Color(0xFF07101E)
    }

val GlassCard: Color
    get() = when (currentAppTheme) {
        "PURE_BLACK" -> Color(0xFF080808)
        "LIGHT" -> Color(0xFFFFFFFF)
        else -> Color(0x1A1E3A6E)
    }

val GlassBorder: Color
    get() = when (currentAppTheme) {
        "PURE_BLACK" -> Color(0xFF1E1E1E)
        "LIGHT" -> Color(0xFFD0DCFF)
        else -> Color(0x331E4A9E)
    }

val NeonBlue: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFF0077CC)
        else -> Color(0xFF00D9FF)
    }

val NeonBlueDim: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFF005F9E)
        else -> Color(0xFF009ECC)
    }

val NeonBlueGlow: Color
    get() = when (currentAppTheme) {
        "NEON_DARK" -> Color(0x4400D9FF)
        else -> Color.Transparent
    }

val NeonPurple: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFF5B2FBB)
        else -> Color(0xFF7C3AED)
    }

val NeonPurpleDim: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFF4A259E)
        else -> Color(0xFF5B2FBB)
    }

val NeonPurpleGlow: Color
    get() = when (currentAppTheme) {
        "NEON_DARK" -> Color(0x447C3AED)
        else -> Color.Transparent
    }

val NeonCyan: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFF0088AA)
        else -> Color(0xFF22D3EE)
    }

val NeonPink: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFFCC1155)
        else -> Color(0xFFFF2D78)
    }

val NeonGreen: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFF008844)
        else -> Color(0xFF00FF88)
    }

val NeonOrange: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFFCC5500)
        else -> Color(0xFFFF8C00)
    }

val NeonYellow: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFFB59000)
        else -> Color(0xFFFFE600)
    }

val TextPrimary: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFF0A1020)
        else -> Color(0xFFEAF0FF)
    }

val TextSecondary: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFF4A5A7A)
        else -> Color(0xFF8BA3CC)
    }

val TextHint: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFF8090B0)
        else -> Color(0xFF4A6080)
    }

val DividerDark: Color
    get() = when (currentAppTheme) {
        "PURE_BLACK" -> Color(0xFF1E1E1E)
        "LIGHT" -> Color(0xFFE0E8F8)
        else -> Color(0xFF1A2A50)
    }

val OverlayDark: Color
    get() = when (currentAppTheme) {
        "PURE_BLACK" -> Color(0xCC000000)
        "LIGHT" -> Color(0x80FFFFFF)
        else -> Color(0xCC050816)
    }

// ─── Module card accent colors ─────────────────────────────────────────────────
val AccentOCR: Color get() = NeonBlue
val AccentTranslate: Color get() = NeonPurple
val AccentVoice: Color get() = NeonCyan
val AccentWaste: Color get() = NeonGreen
val AccentMedicine: Color get() = NeonPink
val AccentStudent: Color get() = NeonOrange
val AccentQR: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFF887700)
        else -> Color(0xFFFFD700)
    }
val AccentObject: Color get() = NeonCyan
val AccentDocument: Color get() = NeonBlue
val AccentAI: Color get() = NeonPurple
val AccentCareer: Color
    get() = when (currentAppTheme) {
        "LIGHT" -> Color(0xFF5A4FCF)
        else -> Color(0xFF6C63FF)
    }

// ─── Light Theme Palette ──────────────────────────────────────────────────────
val LightBackground   = Color(0xFFFFFFFF)
val LightSurface      = Color(0xFFFFFFFF)
val LightCard         = Color(0xFFF0F5FF)
val LightCardBorder   = Color(0xFFD0DCFF)

val LightNeonBlue     = Color(0xFF0077CC)
val LightNeonPurple   = Color(0xFF5B2FBB)

val LightTextPrimary  = Color(0xFF0A1020)
val LightTextSecondary= Color(0xFF4A5A7A)
val LightTextHint     = Color(0xFF8090B0)

val LightDivider      = Color(0xFFE0E8F8)
val LightGlassCard    = Color(0xFFF8FAFF)
val LightGlassBorder  = Color(0xFFD5E0FF)

