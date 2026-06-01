package com.smartvision.ai.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.smartvision.ai.domain.models.OnboardingPage
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// ONBOARDING SCREEN — 4-page swipeable intro with animated illustrations
// ─────────────────────────────────────────────────────────────────────────────

private val PAGES = listOf(
    OnboardingPage(
        "AI-Powered Vision",
        "SmartVision AI uses cutting-edge machine learning to understand the world through your camera. Point, scan, and discover.",
        0xFF00E5FF
    ),
    OnboardingPage(
        "OCR & Translation",
        "Extract text from any image instantly. Translate to 18+ languages including Hindi, Telugu, Tamil, French, Japanese and more.",
        0xFF7B61FF
    ),
    OnboardingPage(
        "Medicine & Waste AI",
        "Identify medicines from labels and classify waste for smarter recycling — all powered by on-device AI for privacy.",
        0xFF00C853
    ),
    OnboardingPage(
        "Your AI Assistant",
        "Chat with SmartVision AI, ask questions, open modules by voice, and get intelligent answers — all in one place.",
        0xFFFF6D00
    )
)

private val PAGE_ICONS = listOf(
    Icons.Rounded.AutoAwesome,
    Icons.Rounded.Translate,
    Icons.Rounded.MedicalServices,
    Icons.Rounded.Chat
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState { PAGES.size }
    val scope      = rememberCoroutineScope()
    val isLast     = pagerState.currentPage == PAGES.size - 1

    val inf = rememberInfiniteTransition(label = "ob")
    val floatY by inf.animateFloat(-12f, 12f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), "fy")
    val glowPulse by inf.animateFloat(0.6f, 1f,
        infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse), "gp")

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050816))) {

        // Background canvas
        Canvas(Modifier.fillMaxSize()) {
            val pg = pagerState.currentPage
            val accent = Color(PAGES[pg].accentHex)
            drawCircle(Brush.radialGradient(
                listOf(accent.copy(0.15f * glowPulse), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.3f), radius = 400f))
        }

        Column(modifier = Modifier.fillMaxSize()) {

            // Skip button
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onFinished) {
                    Text("Skip", color = Color(0xFF8892B0), style = MaterialTheme.typography.titleSmall)
                }
            }

            // Pager
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pg     = PAGES[page]
                val accent = Color(pg.accentHex)

                Column(
                    modifier            = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Illustration orb
                    Box(
                        modifier = Modifier.size(200.dp).offset(y = floatY.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow
                        Box(Modifier.size(200.dp)
                            .background(Brush.radialGradient(
                                listOf(accent.copy(0.2f * glowPulse), Color.Transparent)), CircleShape))
                        // Rotating ring
                        val rot by rememberInfiniteTransition(label = "rot$page").animateFloat(0f, 360f,
                            infiniteRepeatable(tween(4000, easing = LinearEasing)), "r$page")
                        Box(Modifier.size(160.dp).graphicsLayer { rotationZ = rot }
                            .border(1.5.dp, Brush.sweepGradient(
                                listOf(Color.Transparent, accent, Color.Transparent)), CircleShape))
                        // Inner icon box
                        Box(Modifier.size(110.dp)
                            .background(Brush.linearGradient(listOf(accent.copy(0.25f), Color(0xFF121826))), CircleShape)
                            .border(2.dp, accent.copy(0.6f), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(PAGE_ICONS[page], null, tint = accent, modifier = Modifier.size(54.dp))
                        }
                    }

                    Spacer(Modifier.height(40.dp))

                    Text(pg.title, style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

                    Spacer(Modifier.height(16.dp))

                    Text(pg.description, style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF8892B0), textAlign = TextAlign.Center, lineHeight = 26.sp)
                }
            }

            // Bottom controls
            Column(
                modifier            = Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Dot indicators
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(PAGES.size) { idx ->
                        val selected  = idx == pagerState.currentPage
                        val accent    = Color(PAGES[pagerState.currentPage].accentHex)
                        val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot")
                        Box(Modifier.height(8.dp).width(width).clip(CircleShape)
                            .background(if (selected) accent else Color(0xFF1E2D47)))
                    }
                }

                // Action button
                Button(
                    onClick  = {
                        if (isLast) onFinished()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    val accent = Color(PAGES[pagerState.currentPage].accentHex)
                    Box(Modifier.fillMaxSize()
                        .background(Brush.linearGradient(
                            if (isLast) listOf(Color(0xFF7B61FF), Color(0xFF00E5FF))
                            else listOf(accent.copy(0.8f), accent)), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isLast) "Get Started" else "Next",
                                style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            if (!isLast) Icon(Icons.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            else Icon(Icons.Rounded.RocketLaunch, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
