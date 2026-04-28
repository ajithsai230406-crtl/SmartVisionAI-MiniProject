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
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.smartvision.ai.domain.models.ModuleType
import com.smartvision.ai.ui.components.*
import com.smartvision.ai.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// HOME SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onModuleClick:  (ModuleType) -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick:() -> Unit,
    onScanClick:    () -> Unit
) {
    val colors  = smartColors
    val modules = ModuleType.entries

    // Staggered entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            SmartVisionBottomBar(
                selectedTab   = BottomTab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomTab.SCAN     -> onScanClick()
                        BottomTab.HISTORY  -> onHistoryClick()
                        BottomTab.SETTINGS -> onSettingsClick()
                        else -> {}
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns             = GridCells.Fixed(2),
            contentPadding      = PaddingValues(
                start  = 16.dp, end    = 16.dp,
                top    = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement   = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {

            // ── Header: Camera CTA + tagline (spans 2 columns) ───────────────
            item(span = { GridItemSpan(2) }) {
                AnimatedVisibility(
                    visible = visible,
                    enter   = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }
                ) {
                    HomeHeader(onScanClick = onScanClick, colors = colors)
                }
            }

            // ── Section Label ────────────────────────────────────────────────
            item(span = { GridItemSpan(2) }) {
                Text(
                    text     = "M O D U L E S",
                    style    = MaterialTheme.typography.labelLarge,
                    color    = colors.subtext,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }

            // ── Module Cards ─────────────────────────────────────────────────
            itemsIndexed(modules) { index, module ->
                val delay = index * 60
                AnimatedVisibility(
                    visible = visible,
                    enter   = fadeIn(tween(500, delayMillis = delay)) +
                              slideInVertically(tween(500, delayMillis = delay)) { 60 }
                ) {
                    ModuleCard(
                        module  = module,
                        onClick = { onModuleClick(module) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HOME HEADER — Camera CTA card (inspired by Google Lens but unique)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    onScanClick: () -> Unit,
    colors: SmartVisionColorScheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        // App title row
        Row(
            modifier             = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment    = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text       = "SmartVision",
                    style      = MaterialTheme.typography.displaySmall,
                    color      = colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = "AI-Powered Scanner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.subtext
                )
            }
            // Notification / avatar placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.card, CircleShape)
                    .border(1.dp, colors.cardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, null, tint = colors.primary, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(18.dp))

        // Camera CTA Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            colors.card,
                            colors.primary.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(colors.primary.copy(alpha = 0.6f), colors.cardBorder)
                    ),
                    RoundedCornerShape(24.dp)
                )
                .clickable(onClick = onScanClick),
            contentAlignment = Alignment.Center
        ) {
            // Radial glow behind icon
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(colors.primary.copy(alpha = 0.08f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            // Scanning ring
            ScanningRing(color = colors.primary)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(colors.primary.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, colors.primary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Open camera",
                        tint = colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text       = "Point your camera at anything",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = colors.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = "detect objects, read text, translate & more",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.subtext
                )
            }

            // Quick mode chips
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Search", "Translate", "Homework", "QR").forEach { mode ->
                    Box(
                        modifier = Modifier
                            .background(colors.background.copy(alpha = 0.7f), CircleShape)
                            .border(1.dp, colors.cardBorder, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(mode, style = MaterialTheme.typography.labelLarge, color = colors.subtext)
                    }
                }
            }
        }
    }
}
