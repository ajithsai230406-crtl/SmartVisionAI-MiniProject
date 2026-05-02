package com.smartvision.ai.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.smartvision.ai.domain.models.ModuleType

enum class AppTheme { DARK, LIGHT, BLUE }

object SVColors {
    val darkBg = Color(0xFF080C14); val darkSurface = Color(0xFF0E1420)
    val darkCard = Color(0xFF141B2D); val darkBorder = Color(0xFF1E2A42)
    val darkPrimary = Color(0xFF00E5FF); val darkSecondary = Color(0xFF7C4DFF)
    val darkText = Color(0xFFE8EAF6); val darkSub = Color(0xFF8892B0)
    val lightBg = Color(0xFFF4F6FC); val lightSurface = Color(0xFFFFFFFF)
    val lightCard = Color(0xFFFFFFFF); val lightBorder = Color(0xFFE8ECF5)
    val lightPrimary = Color(0xFF0070FF); val lightText = Color(0xFF0D1B2A); val lightSub = Color(0xFF6B7280)
    val blueBg = Color(0xFF040D1E); val blueSurface = Color(0xFF071428)
    val blueCard = Color(0xFF0A1D36); val blueBorder = Color(0xFF1A3A6B)
    val bluePrimary = Color(0xFF4FC3F7); val blueText = Color(0xFFE3F2FD); val blueSub = Color(0xFF90CAF9)
    val cyan = Color(0xFF00E5FF); val purple = Color(0xFFAA00FF); val amber = Color(0xFFFFAB00)
    val green = Color(0xFF00E676); val blue = Color(0xFF2979FF); val red = Color(0xFFFF1744)
    val green2 = Color(0xFF00C853); val orange = Color(0xFFFF6D00); val error = Color(0xFFFF5252)
}

fun moduleAccent(module: ModuleType): Color = when (module) {
    ModuleType.OBJECT_DETECTION -> SVColors.cyan
    ModuleType.TEXT_SCANNER     -> SVColors.purple
    ModuleType.TRANSLATOR       -> SVColors.amber
    ModuleType.VOICE_TRANSLATOR -> SVColors.green
    ModuleType.STUDENT_HELPER   -> SVColors.blue
    ModuleType.MEDICAL_SCANNER  -> SVColors.red
    ModuleType.WASTE_CLASSIFIER -> SVColors.green2
    ModuleType.QR_SCANNER       -> SVColors.orange
}

data class SVColorScheme(
    val background: Color, val surface: Color, val card: Color, val border: Color,
    val primary: Color, val secondary: Color, val onSurface: Color, val subtext: Color,
    val error: Color = SVColors.error, val isLight: Boolean = false
)

val LocalSVColors = staticCompositionLocalOf {
    SVColorScheme(SVColors.darkBg, SVColors.darkSurface, SVColors.darkCard, SVColors.darkBorder,
        SVColors.darkPrimary, SVColors.darkSecondary, SVColors.darkText, SVColors.darkSub)
}

val svTypography = Typography(
    displaySmall  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 24.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 14.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 12.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 11.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 12.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 10.sp),
)

@Composable
fun SmartVisionTheme(appTheme: AppTheme = AppTheme.DARK, content: @Composable () -> Unit) {
    val s = when (appTheme) {
        AppTheme.DARK  -> SVColorScheme(SVColors.darkBg, SVColors.darkSurface, SVColors.darkCard, SVColors.darkBorder, SVColors.darkPrimary, SVColors.darkSecondary, SVColors.darkText, SVColors.darkSub)
        AppTheme.LIGHT -> SVColorScheme(SVColors.lightBg, SVColors.lightSurface, SVColors.lightCard, SVColors.lightBorder, SVColors.lightPrimary, Color(0xFF6200EE), SVColors.lightText, SVColors.lightSub, isLight = true)
        AppTheme.BLUE  -> SVColorScheme(SVColors.blueBg, SVColors.blueSurface, SVColors.blueCard, SVColors.blueBorder, SVColors.bluePrimary, Color(0xFF40C4FF), SVColors.blueText, SVColors.blueSub)
    }
    val m3 = if (s.isLight) lightColorScheme(primary = s.primary, background = s.background, surface = s.surface, onBackground = s.onSurface, onSurface = s.onSurface)
    else darkColorScheme(primary = s.primary, background = s.background, surface = s.surface, onBackground = s.onSurface, onSurface = s.onSurface)
    CompositionLocalProvider(LocalSVColors provides s) {
        MaterialTheme(colorScheme = m3, typography = svTypography, content = content)
    }
}

val svColors: SVColorScheme @Composable get() = LocalSVColors.current
