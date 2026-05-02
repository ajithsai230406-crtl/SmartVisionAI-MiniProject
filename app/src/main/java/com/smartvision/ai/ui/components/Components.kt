package com.smartvision.ai.ui.components

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.smartvision.ai.domain.models.ModuleType
import com.smartvision.ai.ui.theme.*

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

@Composable
fun ModuleCard(module: ModuleType, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = svColors; val accent = moduleAccent(module)
    Box(modifier = modifier
        .aspectRatio(0.95f)
        .clip(RoundedCornerShape(20.dp))
        .background(c.card)
        .border(1.dp, Brush.linearGradient(listOf(accent.copy(0.5f), c.border)), RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .padding(16.dp)
    ) {
        Box(Modifier.size(70.dp).align(Alignment.TopEnd).offset(x = 16.dp, y = (-16).dp)
            .background(Brush.radialGradient(listOf(accent.copy(0.12f), Color.Transparent)), CircleShape))
        Column(Modifier.fillMaxSize(), Arrangement.SpaceBetween) {
            Box(Modifier.size(46.dp).background(accent.copy(0.15f), RoundedCornerShape(13.dp))
                .border(1.dp, accent.copy(0.3f), RoundedCornerShape(13.dp)), Alignment.Center) {
                Icon(moduleIcon(module), null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(module.title, style = MaterialTheme.typography.titleMedium, color = c.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(module.subtitle, style = MaterialTheme.typography.bodySmall, color = c.subtext, maxLines = 2)
                Box(Modifier.padding(top = 5.dp).width(26.dp).height(2.dp).background(accent, CircleShape))
            }
        }
    }
}

@Composable
fun ConfidenceBadge(confidence: Float, modifier: Modifier = Modifier) {
    val pct = (confidence * 100).toInt()
    val color = when { pct >= 80 -> SVColors.cyan; pct >= 50 -> SVColors.amber; else -> SVColors.red }
    Box(modifier.background(color.copy(0.15f), RoundedCornerShape(7.dp))
        .border(1.dp, color.copy(0.4f), RoundedCornerShape(7.dp))
        .padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text("$pct%", color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartVisionTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    val c = svColors
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = c.onSurface, fontWeight = FontWeight.SemiBold) },
        navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = c.onSurface) } },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

enum class BottomTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Rounded.Home), SCAN("Scan", Icons.Rounded.CameraAlt),
    HISTORY("History", Icons.Rounded.History), SETTINGS("Settings", Icons.Rounded.Settings)
}

@Composable
fun SVBottomBar(selected: BottomTab, onTab: (BottomTab) -> Unit) {
    val c = svColors
    NavigationBar(containerColor = c.surface, tonalElevation = 0.dp, modifier = Modifier.height(66.dp)) {
        BottomTab.entries.forEach { tab ->
            val sel = tab == selected
            NavigationBarItem(selected = sel, onClick = { onTab(tab) },
                icon = { Icon(tab.icon, null, modifier = Modifier.size(if (sel) 25.dp else 21.dp)) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.primary, selectedTextColor = c.primary,
                    unselectedIconColor = c.subtext, unselectedTextColor = c.subtext,
                    indicatorColor = c.primary.copy(0.12f)
                )
            )
        }
    }
}

@Composable
fun CameraIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, size: Dp = 44.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).background(Color.Black.copy(0.4f), CircleShape)
        .border(1.dp, Color.White.copy(0.2f), CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.48f))
    }
}

@Composable
fun ScanningRing(color: Color, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "scan")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "r")
    androidx.compose.foundation.Canvas(modifier = modifier.graphicsLayer { rotationZ = rot }) {
        drawArc(brush = Brush.sweepGradient(listOf(Color.Transparent, color)),
            startAngle = 0f, sweepAngle = 120f, useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
    }
}
