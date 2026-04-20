package com.example.smartvisionai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartvisionai.ui.theme.*

data class Module(
    val id: String,
    val title: String,
    val description: String,
    val iconText: String,
    val accentColor: Color,
    val bgFrom: Color,
    val bgTo: Color
)

val appModules = listOf(
    Module("object",    "Object Detection",    "Detect everyday objects with AI-powered recognition and confidence scores",  "👁",  CyanAccent,          Color(0xFF003D33), Color(0xFF001A16)),
    Module("ocr",       "Text Scanner (OCR)",  "Extract text from books, notes, boards — copy or share instantly",           "T",   PurpleAccent,        Color(0xFF2D0073), Color(0xFF1A0040)),
    Module("translate", "Translator",          "Scan text and translate to any language in real-time",                       "文A", OrangeAccent,        Color(0xFF3D2200), Color(0xFF1A0E00)),
    Module("student",   "Student Helper",      "Scan questions or formulas — get instant explanations",                      "🎓", Color(0xFF2196F3),   Color(0xFF003873), Color(0xFF001A40)),
    Module("medical",   "Medical Scanner",     "Scan medicine strips to see uses, dosage & side effects",                    "💊", RedAccent,           Color(0xFF3D0000), Color(0xFF1A0000)),
    Module("waste",     "Waste Classifier",    "Identify recyclable, non-recyclable & organic waste items",                  "♻",  GreenAccent,         Color(0xFF003D1A), Color(0xFF001A0A))
)

@Composable
fun HomeScreen(
    onModuleClick: (String) -> Unit,
    onScanClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(scrollState)
    ) {
        AnimatedHeroSection(onScanClick = onScanClick)

        Text(
            "MODULES",
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = TextMuted, letterSpacing = 3.sp,
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 14.dp)
        )

        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            appModules.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { module ->
                        ModuleCard(
                            module   = module,
                            modifier = Modifier.weight(1f),
                            onClick  = { onModuleClick(module.id) }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ── Animated Hero Section ───────────────────────────────────────────────────────

@Composable
fun AnimatedHeroSection(onScanClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "hero")

    // Ring pulses
    val r1 by inf.animateFloat(0.12f, 0.55f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "r1")
    val r2 by inf.animateFloat(0.06f, 0.32f, infiniteRepeatable(tween(1900, 300), RepeatMode.Reverse), label = "r2")
    val r3 by inf.animateFloat(0.03f, 0.18f, infiniteRepeatable(tween(2400, 600), RepeatMode.Reverse), label = "r3")
    val r4 by inf.animateFloat(0.01f, 0.10f, infiniteRepeatable(tween(2900, 900), RepeatMode.Reverse), label = "r4")

    // Arc rotations
    val arc1 by inf.animateFloat(0f, 360f,  infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "a1")
    val arc2 by inf.animateFloat(360f, 0f,  infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "a2")
    val arc3 by inf.animateFloat(0f, 360f,  infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "a3")

    // Orbiting dots
    val orbit1 by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "o1")
    val orbit2 by inf.animateFloat(360f, 0f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "o2")

    // Scan line
    val scanY by inf.animateFloat(-1f, 1f,   infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "sy")

    // Center dot pulse
    val centerPulse by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "cp")

    // Corner frame pulse
    val framePulse by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "fp")

    val CYAN = CyanAccent
    val PURPLE = PurpleAccent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF071820), Color(0xFF030A0C), Color(0xFF020608)),
                    radius = 800f
                )
            )
            .padding(top = 32.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Title
            Text(
                "SMART VISION",
                fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                color = CYAN, letterSpacing = 3.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "AI-POWERED RECOGNITION",
                fontSize = 10.sp, color = TextMuted, letterSpacing = 4.sp
            )

            Spacer(Modifier.height(22.dp))

            // ── Animated Radar Scanner ──────────────────────────────────────
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // All rings + arcs on canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val mr = size.minDimension / 2f

                    // Background subtle glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(CYAN.copy(0.06f), Color.Transparent),
                            center = Offset(cx, cy), radius = mr * 0.8f
                        ),
                        radius = mr * 0.9f, center = Offset(cx, cy)
                    )

                    // Rings (outer to inner)
                    listOf(
                        Triple(0.99f, r4, 0.6f),
                        Triple(0.82f, r3, 0.8f),
                        Triple(0.65f, r2, 1.0f),
                        Triple(0.48f, r1, 1.3f)
                    ).forEach { (frac, alpha, sw) ->
                        drawCircle(
                            color  = CYAN.copy(alpha.coerceIn(0f, 1f)),
                            radius = mr * frac,
                            center = Offset(cx, cy),
                            style  = Stroke(sw)
                        )
                    }

                    // ── Rotating arcs ──────────────────────────────────────
                    // Outer fast arc
                    val r_a1 = mr * 0.93f
                    drawArc(CYAN.copy(0.65f), arc1, 130f, false,
                        Offset(cx-r_a1, cy-r_a1), Size(r_a1*2, r_a1*2),
                        style = Stroke(2f, cap = StrokeCap.Round))
                    drawArc(CYAN.copy(0.20f), arc1+190f, 60f, false,
                        Offset(cx-r_a1, cy-r_a1), Size(r_a1*2, r_a1*2),
                        style = Stroke(1f, cap = StrokeCap.Round))

                    // Middle counter-rotating arc
                    val r_a2 = mr * 0.73f
                    drawArc(CYAN.copy(0.30f), arc2, 90f, false,
                        Offset(cx-r_a2, cy-r_a2), Size(r_a2*2, r_a2*2),
                        style = Stroke(1.2f, cap = StrokeCap.Round))

                    // Inner slow arc (purple tint)
                    val r_a3 = mr * 0.53f
                    drawArc(PURPLE.copy(0.35f), arc3, 70f, false,
                        Offset(cx-r_a3, cy-r_a3), Size(r_a3*2, r_a3*2),
                        style = Stroke(1f, cap = StrokeCap.Round))

                    // ── Scan line ──────────────────────────────────────────
                    val lineY = cy + scanY * mr * 0.45f
                    if (lineY > cy - mr * 0.45f && lineY < cy + mr * 0.45f) {
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, CYAN.copy(0.0f),
                                    CYAN.copy(0.9f), CYAN.copy(0.0f), Color.Transparent),
                                startX = cx - mr * 0.42f, endX = cx + mr * 0.42f
                            ),
                            start = Offset(cx - mr * 0.44f, lineY),
                            end   = Offset(cx + mr * 0.44f, lineY),
                            strokeWidth = 1.8f
                        )
                        // Glow under scan line
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, CYAN.copy(0.1f),
                                    CYAN.copy(0.1f), Color.Transparent),
                                startX = cx - mr * 0.42f, endX = cx + mr * 0.42f
                            ),
                            start = Offset(cx - mr * 0.44f, lineY + 3f),
                            end   = Offset(cx + mr * 0.44f, lineY + 3f),
                            strokeWidth = 6f
                        )
                    }

                    // ── Orbiting dots ──────────────────────────────────────
                    fun orbitDot(angle: Float, radius: Float, color: Color, size: Float) {
                        val rad = Math.toRadians(angle.toDouble())
                        val dx = cx + radius * Math.cos(rad).toFloat()
                        val dy = cy + radius * Math.sin(rad).toFloat()
                        drawCircle(color, size, Offset(dx, dy))
                    }
                    orbitDot(orbit1,         mr * 0.63f, CYAN,   6.5f)
                    orbitDot(orbit1 + 180f,  mr * 0.63f, CYAN.copy(0.3f), 3.5f)
                    orbitDot(orbit2,         mr * 0.48f, PURPLE, 5f)
                    orbitDot(orbit2 + 120f,  mr * 0.48f, PURPLE.copy(0.25f), 3f)

                    // Center dot
                    drawCircle(CYAN.copy(0.25f), mr * 0.08f, Offset(cx, cy))
                    drawCircle(CYAN.copy(centerPulse), mr * 0.04f, Offset(cx, cy))
                }

                // ── Scanner frame corners (drawn on top) ────────────────────
                Canvas(
                    modifier = Modifier
                        .size(148.dp)
                        .align(Alignment.Center)
                ) {
                    val w = size.width; val h = size.height
                    val len = 28f; val sw = 3f
                    val col = CYAN.copy(framePulse)
                    val p = Paint().apply {
                        color = col; strokeWidth = sw
                        style = PaintingStyle.Stroke
                        strokeCap = StrokeCap.Round
                    }
                    drawContext.canvas.apply {
                        drawLine(Offset(0f, len), Offset(0f, 0f), p)
                        drawLine(Offset(0f, 0f), Offset(len, 0f), p)
                        drawLine(Offset(w-len, 0f), Offset(w, 0f), p)
                        drawLine(Offset(w, 0f), Offset(w, len), p)
                        drawLine(Offset(0f, h-len), Offset(0f, h), p)
                        drawLine(Offset(0f, h), Offset(len, h), p)
                        drawLine(Offset(w-len, h), Offset(w, h), p)
                        drawLine(Offset(w, h), Offset(w, h-len), p)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Point your camera at anything — detect objects,\nread text, translate, and more.",
                fontSize = 13.sp, color = TextSecondary,
                textAlign = TextAlign.Center, lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ── Module Card ─────────────────────────────────────────────────────────────────

@Composable
fun ModuleCard(module: Module, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(CardDark, Color(0xFF0D1520))))
            .border(
                1.dp,
                Brush.linearGradient(listOf(module.accentColor.copy(0.35f), CardBorder)),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            // Icon box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(module.bgFrom, module.bgTo)))
                    .border(1.dp, module.accentColor.copy(0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (module.iconText == "T") {
                    Text(module.iconText, color = module.accentColor,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(module.iconText, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(module.title, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(5.dp))
            Text(module.description, fontSize = 11.sp,
                color = TextSecondary, lineHeight = 16.sp)
            Spacer(Modifier.height(14.dp))
            // Bottom accent line
            Box(
                modifier = Modifier.fillMaxWidth().height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(module.accentColor.copy(0.8f), Color.Transparent)
                        )
                    )
            )
        }
    }
}
