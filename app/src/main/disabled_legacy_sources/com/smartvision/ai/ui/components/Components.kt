package com.smartvision.ai.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.smartvision.ai.domain.models.ModuleType
import com.smartvision.ai.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// MODULE CARD — used in the 2-column LazyVerticalGrid on HomeScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ModuleCard(
    module: ModuleType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = smartColors
    val accent = moduleAccentColor(module)
    val icon   = moduleIcon(module)

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .aspectRatio(0.95f)              // consistent height/width ratio
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .border(
                width  = 1.dp,
                brush  = Brush.linearGradient(
                    colors = listOf(accent.copy(alpha = 0.5f), colors.cardBorder)
                ),
                shape  = RoundedCornerShape(20.dp)
            )
            .clickable(
                onClick = onClick,
                onClickLabel = "Open ${module.title}"
            )
            .padding(16.dp)
    ) {
        // Subtle gradient glow in corner
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp)
                .background(
                    Brush.radialGradient(colors = listOf(accent.copy(alpha = 0.15f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon bubble
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = accent,
                    modifier           = Modifier.size(26.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text       = module.title,
                    style      = MaterialTheme.typography.titleMedium,
                    color      = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 2
                )
                Text(
                    text    = module.subtitle,
                    style   = MaterialTheme.typography.bodySmall,
                    color   = colors.subtext,
                    maxLines = 2
                )
                // Accent bar
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(28.dp)
                        .height(2.dp)
                        .background(accent, CircleShape)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GLASS CARD — semi-transparent frosted container
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderColor: Color = smartColors.cardBorder,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = smartColors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.card.copy(alpha = 0.85f))
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM NAV BAR
// ─────────────────────────────────────────────────────────────────────────────

enum class BottomTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME    ("Home",    Icons.Rounded.Home),
    SCAN    ("Scan",    Icons.Rounded.CameraAlt),
    HISTORY ("History", Icons.Rounded.History),
    SETTINGS("Settings",Icons.Rounded.Settings)
}

@Composable
fun SmartVisionBottomBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit
) {
    val colors = smartColors

    NavigationBar(
        containerColor = colors.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.height(68.dp)
    ) {
        BottomTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            NavigationBarItem(
                selected  = selected,
                onClick   = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector        = tab.icon,
                        contentDescription = tab.label,
                        modifier           = Modifier.size(if (selected) 26.dp else 22.dp)
                    )
                },
                label = {
                    Text(
                        text  = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = colors.primary,
                    selectedTextColor   = colors.primary,
                    unselectedIconColor = colors.subtext,
                    unselectedTextColor = colors.subtext,
                    indicatorColor      = colors.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONFIDENCE BADGE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ConfidenceBadge(confidence: Float, modifier: Modifier = Modifier) {
    val pct   = (confidence * 100).toInt()
    val color = when {
        pct >= 80 -> SmartVisionColors.objectDetect
        pct >= 50 -> SmartVisionColors.translator
        else      -> SmartVisionColors.medicalScanner
    }
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text       = "$pct%",
            color      = color,
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP APP BAR
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartVisionTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = smartColors
    TopAppBar(
        title = {
            Text(
                text       = title,
                style      = MaterialTheme.typography.headlineSmall,
                color      = colors.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.onSurface)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SCANNING ANIMATION — pulsing radar ring
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScanningRing(color: Color = SmartVisionColors.darkPrimary, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "scan")
    val pulse by inf.animateFloat(
        initialValue    = 0.6f,
        targetValue     = 1.0f,
        animationSpec   = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label           = "pulse"
    )
    val rotation by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label         = "rotation"
    )
    Canvas(modifier = modifier.size(200.dp).graphicsLayer { rotationZ = rotation }) {
        drawCircle(color = color.copy(alpha = 0.1f * pulse), radius = size.minDimension / 2)
        drawArc(
            brush     = Brush.sweepGradient(listOf(Color.Transparent, color)),
            startAngle = 0f, sweepAngle = 120f,
            useCenter  = false,
            style      = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

fun moduleAccentColor(module: ModuleType): Color = when (module) {
    ModuleType.OBJECT_DETECTION -> SmartVisionColors.objectDetect
    ModuleType.TEXT_SCANNER     -> SmartVisionColors.textScanner
    ModuleType.TRANSLATOR       -> SmartVisionColors.translator
    ModuleType.VOICE_TRANSLATOR -> SmartVisionColors.voiceTranslator
    ModuleType.STUDENT_HELPER   -> SmartVisionColors.studentHelper
    ModuleType.MEDICAL_SCANNER  -> SmartVisionColors.medicalScanner
    ModuleType.WASTE_CLASSIFIER -> SmartVisionColors.wasteClassifier
    ModuleType.QR_SCANNER       -> SmartVisionColors.qrScanner
}

@Composable
fun moduleIcon(module: ModuleType) = when (module) {
    ModuleType.OBJECT_DETECTION -> Icons.Rounded.RemoveRedEye
    ModuleType.TEXT_SCANNER     -> Icons.Rounded.TextFields
    ModuleType.TRANSLATOR       -> Icons.Rounded.Translate
    ModuleType.VOICE_TRANSLATOR -> Icons.Rounded.RecordVoiceOver
    ModuleType.STUDENT_HELPER   -> Icons.Rounded.School
    ModuleType.MEDICAL_SCANNER  -> Icons.Rounded.MedicalServices
    ModuleType.WASTE_CLASSIFIER -> Icons.Rounded.Recycling
    ModuleType.QR_SCANNER       -> Icons.Rounded.QrCodeScanner
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR — with optional actions slot
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SmartVisionTopBar(
    title:   String,
    onBack:  () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = smartColors
    TopAppBar(
        title = {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color      = colors.onSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint               = colors.onSurface,
                    modifier           = Modifier.size(20.dp)
                )
            }
        },
        actions = actions,
        colors  = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.background
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SCANNING RING ANIMATION — used on Home hero card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScanningRing(color: Color) {
    val inf = rememberInfiniteTransition(label = "ring")
    val angle by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = androidx.compose.animation.core.LinearEasing)),
        label         = "angle"
    )
    Box(
        modifier = Modifier
            .size(140.dp)
            .graphicsLayer { rotationZ = angle }
            .border(
                width = 1.5.dp,
                brush = Brush.sweepGradient(
                    listOf(Color.Transparent, color.copy(0.4f), Color.Transparent)
                ),
                shape = CircleShape
            )
    )
}

// module accent helper
@Composable
fun moduleAccentColor(module: com.smartvision.ai.domain.models.ModuleType): Color =
    when (module) {
        com.smartvision.ai.domain.models.ModuleType.OBJECT_DETECTION -> SmartVisionColors.objectDetect
        com.smartvision.ai.domain.models.ModuleType.TEXT_SCANNER     -> SmartVisionColors.textScanner
        com.smartvision.ai.domain.models.ModuleType.TRANSLATOR       -> SmartVisionColors.translator
        com.smartvision.ai.domain.models.ModuleType.VOICE_TRANSLATOR -> SmartVisionColors.voiceTranslator
        com.smartvision.ai.domain.models.ModuleType.STUDENT_HELPER   -> SmartVisionColors.studentHelper
        com.smartvision.ai.domain.models.ModuleType.MEDICAL_SCANNER  -> SmartVisionColors.medicalScanner
        com.smartvision.ai.domain.models.ModuleType.WASTE_CLASSIFIER -> SmartVisionColors.wasteClassifier
        com.smartvision.ai.domain.models.ModuleType.QR_SCANNER       -> SmartVisionColors.qrScanner
    }

@Composable
fun moduleIcon(module: com.smartvision.ai.domain.models.ModuleType): androidx.compose.ui.graphics.vector.ImageVector =
    when (module) {
        com.smartvision.ai.domain.models.ModuleType.OBJECT_DETECTION -> Icons.Rounded.RemoveRedEye
        com.smartvision.ai.domain.models.ModuleType.TEXT_SCANNER     -> Icons.Rounded.TextFields
        com.smartvision.ai.domain.models.ModuleType.TRANSLATOR       -> Icons.Rounded.Translate
        com.smartvision.ai.domain.models.ModuleType.VOICE_TRANSLATOR -> Icons.Rounded.RecordVoiceOver
        com.smartvision.ai.domain.models.ModuleType.STUDENT_HELPER   -> Icons.Rounded.School
        com.smartvision.ai.domain.models.ModuleType.MEDICAL_SCANNER  -> Icons.Rounded.LocalHospital
        com.smartvision.ai.domain.models.ModuleType.WASTE_CLASSIFIER -> Icons.Rounded.Recycling
        com.smartvision.ai.domain.models.ModuleType.QR_SCANNER       -> Icons.Rounded.QrCodeScanner
    }
