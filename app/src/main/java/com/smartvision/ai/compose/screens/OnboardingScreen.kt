package com.smartvision.ai.compose.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.*

// ══════════════════════════════════════════════════════════════════════════════
// GLIMPSE / ONBOARDING SCREEN — Feature Showcase
// ══════════════════════════════════════════════════════════════════════════════

private data class GlimpsePage(
    val emoji:        String,
    val title:        String,
    val tagline:      String,
    val description:  String,
    val accent:       Color,
    val accentDim:    Color,
    val features:     List<String>
)

private val glimpsePages = listOf(
    GlimpsePage(
        emoji       = "👁",
        title       = "Smart Vision AI",
        tagline     = "See the world differently",
        description = "Point your camera at anything and let AI do the heavy lifting. Text, objects, medicines — recognized instantly.",
        accent      = NeonBlue,
        accentDim   = NeonBlueDim,
        features    = listOf("📷 Live camera scanning", "🌐 40+ languages", "⚡ Real-time analysis")
    ),
    GlimpsePage(
        emoji       = "📝",
        title       = "OCR Translator",
        tagline     = "Google Lens style OCR",
        description = "Drag to select any text in the real world. Instantly recognize, copy, translate, or speak it in any language.",
        accent      = NeonCyan,
        accentDim   = Color(0xFF0EA5C9),
        features    = listOf("✂️ Drag-to-select text", "🌍 ML Kit translation", "🔊 Text-to-speech")
    ),
    GlimpsePage(
        emoji       = "🔍",
        title       = "Object Detector",
        tagline     = "Real-time AI detection",
        description = "Continuous live detection of people, vehicles, gadgets, and 80+ COCO objects — all with neon bounding boxes.",
        accent      = AccentObject,
        accentDim   = Color(0xFF0891B2),
        features    = listOf("🎯 80+ COCO objects", "📊 Confidence scores", "⚡ STREAM_MODE AI")
    ),
    GlimpsePage(
        emoji       = "💊",
        title       = "Medicine Scanner",
        tagline     = "Know what you're taking",
        description = "Scan any medicine and get instant dosage info, side effects, interactions, and safety warnings — offline too.",
        accent      = NeonPink,
        accentDim   = Color(0xFFBE185D),
        features    = listOf("⚠️ Safety warnings", "💉 Dosage guide", "🔗 Drug interactions")
    ),
    GlimpsePage(
        emoji       = "♻️",
        title       = "Waste Classifier",
        tagline     = "Scan. Sort. Save the planet.",
        description = "Point your camera at any waste item and get instant recycling instructions, eco-score, and sustainability tips.",
        accent      = NeonGreen,
        accentDim   = Color(0xFF16A34A),
        features    = listOf("🌱 8 waste categories", "📈 Eco score", "🔄 Live detection")
    ),
    GlimpsePage(
        emoji       = "🎓",
        title       = "Student Helper",
        tagline     = "Your AI homework tutor",
        description = "Snap any question — math, physics, chemistry, or history. Get step-by-step solutions powered by Gemini AI.",
        accent      = NeonOrange,
        accentDim   = Color(0xFFD97706),
        features    = listOf("🧮 Step-by-step solve", "🧪 Multi-subject", "💡 Tutor chat")
    ),
    GlimpsePage(
        emoji       = "🤖",
        title       = "AI Assistant",
        tagline     = "Your pocket Gemini AI",
        description = "Chat with a context-aware AI assistant that knows every module of this app. Ask anything, anytime.",
        accent      = NeonPurple,
        accentDim   = NeonPurpleDim,
        features    = listOf("💬 Conversation memory", "📱 App context-aware", "✈️ Offline capable")
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { glimpsePages.size })
    val scope      = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "global_ambient")
    val globalPulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "globalPulse"
    )

    val currentPage = glimpsePages[pagerState.currentPage]

    // Animate accent color between pages
    val accentColorAnim by animateColorAsState(
        targetValue   = currentPage.accent,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label         = "accentColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        // ── Animated background ───────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            // Top glow
            drawCircle(
                brush  = Brush.radialGradient(
                    listOf(accentColorAnim.copy(0.12f + 0.05f * sin(globalPulse * 2 * PI.toFloat()).absoluteValue), Color.Transparent),
                    center = Offset(w * 0.5f, -h * 0.1f),
                    radius = h * 0.5f
                ),
                radius = h * 0.5f,
                center = Offset(w * 0.5f, -h * 0.1f)
            )
            // Bottom accent glow
            drawCircle(
                brush  = Brush.radialGradient(
                    listOf(NeonPurple.copy(0.08f), Color.Transparent),
                    center = Offset(w * 0.1f, h * 1.0f),
                    radius = h * 0.4f
                ),
                radius = h * 0.4f,
                center = Offset(w * 0.1f, h * 1.0f)
            )
            // Subtle horizontal scanline
            val scanY = (h * 0.7f * sin(globalPulse * PI.toFloat()).absoluteValue + h * 0.05f)
            drawLine(
                brush       = Brush.horizontalGradient(listOf(Color.Transparent, accentColorAnim.copy(0.3f), Color.Transparent)),
                start       = Offset(0f, scanY),
                end         = Offset(w, scanY),
                strokeWidth = 1.5f
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar: page indicator & skip ────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Page dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(glimpsePages.size) { idx ->
                        val isActive = pagerState.currentPage == idx
                        val w by animateDpAsState(if (isActive) 24.dp else 6.dp, tween(300), label = "dot$idx")
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(w)
                                .clip(CircleShape)
                                .background(
                                    if (isActive)
                                        Brush.horizontalGradient(listOf(accentColorAnim, accentColorAnim.copy(0.6f)))
                                    else
                                        Brush.horizontalGradient(listOf(GlassBorder, GlassBorder))
                                )
                        )
                    }
                }
                // Skip
                Text(
                    "Skip",
                    modifier  = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassBorder.copy(alpha = 0.3f))
                        .clickable(onClick = onFinished)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    color     = TextSecondary,
                    style     = MaterialTheme.typography.labelLarge
                )
            }

            // ── Pager ──────────────────────────────────────────────────────
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                GlimpsePageContent(
                    page        = glimpsePages[page],
                    globalPulse = globalPulse
                )
            }

            // ── Bottom navigation ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                val isLastPage = pagerState.currentPage == glimpsePages.lastIndex

                // Primary button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColorAnim, accentColorAnim.copy(0.7f))
                            )
                        )
                        .clickable {
                            if (isLastPage) {
                                onFinished()
                            } else {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isLastPage) "Get Started 🚀" else "Next  →",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.titleMedium
                    )
                }

                if (!isLastPage) {
                    Text(
                        "Swipe to explore features",
                        color = TextHint,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

// ── Single Glimpse Page ───────────────────────────────────────────────────────
@Composable
private fun GlimpsePageContent(page: GlimpsePage, globalPulse: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "page_${page.title}")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "ringAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Animated icon zone ─────────────────────────────────────────────
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer ring
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(page.accent.copy(ringAlpha * 0.15f), r)
                drawCircle(
                    page.accent.copy(ringAlpha * 0.5f), r,
                    style = Stroke(2f)
                )
                // Rotating tick marks
                for (i in 0 until 24) {
                    val angle = (i * 15f + globalPulse * 60f) * (PI / 180f).toFloat()
                    val innerR = r - 14f
                    val outerR = r - 6f
                    val sx = size.width / 2f + innerR * cos(angle)
                    val sy = size.height / 2f + innerR * sin(angle)
                    val ex = size.width / 2f + outerR * cos(angle)
                    val ey = size.height / 2f + outerR * sin(angle)
                    drawLine(page.accent.copy(if (i % 6 == 0) ringAlpha else ringAlpha * 0.3f), Offset(sx, sy), Offset(ex, ey), 1.5f)
                }
            }
            // Inner circle with icon
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(page.accent.copy(0.3f), page.accentDim.copy(0.1f), DeepNavy))
                    )
                    .border(2.dp, Brush.horizontalGradient(listOf(page.accent, page.accentDim)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(page.emoji, fontSize = 60.sp)
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Module name badge ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(page.accent.copy(0.12f))
                .border(1.dp, page.accent.copy(0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                page.tagline.uppercase(),
                color      = page.accent,
                fontWeight = FontWeight.Bold,
                style      = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Title ──────────────────────────────────────────────────────────
        Text(
            page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                brush = Brush.horizontalGradient(listOf(page.accent, TextPrimary, page.accentDim))
            ),
            fontWeight = FontWeight.ExtraBold,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        // ── Description ────────────────────────────────────────────────────
        Text(
            page.description,
            style     = MaterialTheme.typography.bodyMedium,
            color     = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(28.dp))

        // ── Feature pills ──────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            page.features.forEachIndexed { idx, feat ->
                FeaturePill(text = feat, accent = page.accent, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ── Small feature pill ────────────────────────────────────────────────────────
@Composable
private fun FeaturePill(text: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(0.08f))
            .border(1.dp, accent.copy(0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style     = MaterialTheme.typography.labelSmall,
            color     = accent,
            textAlign = TextAlign.Center,
            maxLines  = 2,
            lineHeight = 14.sp
        )
    }
}
