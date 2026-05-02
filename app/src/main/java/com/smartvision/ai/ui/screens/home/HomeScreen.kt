package com.smartvision.ai.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.smartvision.ai.domain.models.ModuleType
import com.smartvision.ai.ui.components.*
import com.smartvision.ai.ui.theme.*

@Composable
fun HomeScreen(
    onModuleClick: (ModuleType) -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onScanClick: () -> Unit
) {
    val c = svColors
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        containerColor = c.background,
        bottomBar = {
            SVBottomBar(selected = BottomTab.HOME, onTab = { tab ->
                when (tab) {
                    BottomTab.SCAN     -> onScanClick()
                    BottomTab.HISTORY  -> onHistoryClick()
                    BottomTab.SETTINGS -> onSettingsClick()
                    else -> {}
                }
            })
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp,
                top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 14.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(2) }) {
                AnimatedVisibility(visible, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -30 }) {
                    HomeHeader(onScanClick = onScanClick, c = c)
                }
            }
            item(span = { GridItemSpan(2) }) {
                Text("M O D U L E S", style = MaterialTheme.typography.labelLarge, color = c.subtext,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
            }
            itemsIndexed(ModuleType.entries) { idx, module ->
                AnimatedVisibility(visible, enter = fadeIn(tween(400, idx * 55)) + slideInVertically(tween(400, idx * 55)) { 50 }) {
                    ModuleCard(module = module, onClick = { onModuleClick(module) })
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onScanClick: () -> Unit, c: SVColorScheme) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("SmartVision", style = MaterialTheme.typography.displaySmall, color = c.onSurface, fontWeight = FontWeight.Bold)
                Text("AI-Powered Scanner", style = MaterialTheme.typography.bodyMedium, color = c.subtext)
            }
            Box(Modifier.size(40.dp).background(c.card, CircleShape).border(1.dp, c.border, CircleShape), Alignment.Center) {
                Icon(Icons.Rounded.Person, null, tint = c.primary, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(156.dp).clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(c.card, c.primary.copy(0.07f))))
            .border(1.dp, Brush.linearGradient(listOf(c.primary.copy(0.55f), c.border)), RoundedCornerShape(22.dp))
            .clickable(onClick = onScanClick), Alignment.Center
        ) {
            Box(Modifier.size(160.dp).background(Brush.radialGradient(listOf(c.primary.copy(0.07f), Color.Transparent)), CircleShape))
            ScanningRing(color = c.primary)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(52.dp).background(c.primary.copy(0.14f), CircleShape).border(2.dp, c.primary.copy(0.45f), CircleShape), Alignment.Center) {
                    Icon(Icons.Rounded.CameraAlt, "Open camera", tint = c.primary, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("Point your camera at anything", style = MaterialTheme.typography.titleMedium, color = c.onSurface, fontWeight = FontWeight.SemiBold)
                Text("detect · read text · translate & more", style = MaterialTheme.typography.bodySmall, color = c.subtext)
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Search", "Translate", "Homework", "QR").forEach { mode ->
                    Box(Modifier.background(c.background.copy(0.7f), CircleShape).border(1.dp, c.border, CircleShape).padding(horizontal = 11.dp, vertical = 4.dp)) {
                        Text(mode, style = MaterialTheme.typography.labelLarge, color = c.subtext)
                    }
                }
            }
        }
    }
}
