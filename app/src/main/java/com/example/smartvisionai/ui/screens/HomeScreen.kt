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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartvisionai.ui.theme.*
import com.example.smartvisionai.ui.components.ScannerFrame

data class Module(
    val id: String,
    val title: String,
    val description: String,
    val iconColor: Color,
    val accentColor: Color,
    val gradientStart: Color,
    val gradientEnd: Color
)

val appModules = listOf(
    Module("object", "Object Detection",
        "Detect everyday objects with AI-powered recognition and confidence scores",
        CyanAccent, CyanAccent, Color(0xFF003D33), Color(0xFF001A16)),
    Module("ocr", "Text Scanner (OCR)",
        "Extract text from books, notes, boards — copy or share instantly",
        PurpleAccent, PurpleAccent, Color(0xFF2D0073), Color(0xFF1A0040)),
    Module("translate", "Translator",
        "Scan text and translate to any language in real-time",
        OrangeAccent, OrangeAccent, Color(0xFF3D2200), Color(0xFF1A0E00)),
    Module("student", "Student Helper",
        "Scan questions or formulas — get instant explanations",
        Color(0xFF2196F3), Color(0xFF2196F3), Color(0xFF003873), Color(0xFF001A40)),
    Module("medical", "Medical Scanner",
        "Scan medicine strips to see uses, dosage & side effects",
        RedAccent, RedAccent, Color(0xFF3D0000), Color(0xFF1A0000)),
    Module("waste", "Waste Classifier",
        "Identify recyclable, non-recyclable & organic waste items",
        GreenAccent, GreenAccent, Color(0xFF003D1A), Color(0xFF001A0A))
)

@Composable
fun HomeScreen(onModuleClick: (String) -> Unit, onScanClick: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(scrollState)
    ) {
        // ── Hero Section ──────────────────────────────────────────────────
        HeroSection(onScanClick = onScanClick)

        // ── Modules Label ─────────────────────────────────────────────────
        Text(
            text = "MODULES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
        )

        // ── Module Grid ───────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            appModules.chunked(2).forEach { rowModules ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowModules.forEach { module ->
                        ModuleCard(
                            module = module,
                            modifier = Modifier.weight(1f),
                            onClick = { onModuleClick(module.id) }
                        )
                    }
                    if (rowModules.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun HeroSection(onScanClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val ring1Alpha by infiniteTransition.animateFloat(
        0.15f, 0.55f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "r1"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        0.08f, 0.35f,
        infiniteRepeatable(tween(1800, 300), RepeatMode.Reverse), label = "r2"
    )
    val ring3Alpha by infiniteTransition.animateFloat(
        0.04f, 0.2f,
        infiniteRepeatable(tween(2200, 600), RepeatMode.Reverse), label = "r3"
    )
    val pulseScale by infiniteTransition.animateFloat(
        0.93f, 1.07f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )
    val rotation by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rot"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0A2020), BackgroundDark),
                    radius = 600f
                )
            )
            .padding(top = 36.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SMART VISION", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                color = CyanAccent, letterSpacing = 3.sp)
            Spacer(Modifier.height(4.dp))
            Text("AI-POWERED RECOGNITION", fontSize = 10.sp, color = TextMuted, letterSpacing = 4.sp)

            Spacer(Modifier.height(20.dp))

            // Scanner Animation
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                // Outer ring
                Canvas(modifier = Modifier.size(210.dp)) {
                    drawCircle(color = CyanAccent.copy(alpha = ring3Alpha),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 1f))
                }
                Canvas(modifier = Modifier.size(160.dp)) {
                    drawCircle(color = CyanAccent.copy(alpha = ring2Alpha),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 1f))
                }
                Canvas(modifier = Modifier.size(110.dp)) {
                    drawCircle(color = CyanAccent.copy(alpha = ring1Alpha),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 1.5f))
                }

                // Scanner frame corners (pulsing)
                ScannerFrame(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
                    color = CyanAccent
                )

                // Rotating orbit dots
                Canvas(
                    modifier = Modifier
                        .size(110.dp)
                        .rotate(rotation)
                ) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val r = size.minDimension / 2
                    drawCircle(color = CyanAccent, radius = 7f,
                        center = androidx.compose.ui.geometry.Offset(cx, cy - r))
                    drawCircle(color = PurpleAccent, radius = 5.5f,
                        center = androidx.compose.ui.geometry.Offset(cx + r * 0.7f, cy + r * 0.7f))
                }

                // Center glowing dot
                Canvas(modifier = Modifier.size(18.dp)) {
                    drawCircle(color = CyanAccent.copy(alpha = 0.3f), radius = size.minDimension / 2)
                    drawCircle(color = CyanAccent, radius = size.minDimension / 4)
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "Point your camera at anything — detect objects,\nread text, translate, and more.",
                fontSize = 13.sp, color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun ModuleCard(module: Module, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(CardDark, Color(0xFF0D1520)))
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(module.accentColor.copy(alpha = 0.3f), CardBorder)
                ),
                shape = RoundedCornerShape(16.dp)
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
                    .background(
                        Brush.linearGradient(
                            listOf(module.gradientStart, module.gradientEnd)
                        )
                    )
                    .border(
                        1.dp,
                        module.iconColor.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                ModuleIcon(moduleId = module.id, color = module.iconColor)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                module.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(5.dp))
            Text(
                module.description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            // Bottom accent line
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(module.accentColor.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )
        }
    }
}

@Composable
fun ModuleIcon(moduleId: String, color: Color) {
    val iconText = when (moduleId) {
        "object"  -> "👁"
        "ocr"     -> "T"
        "translate" -> "文A"
        "student" -> "🎓"
        "medical" -> "💊"
        "waste"   -> "♻"
        else      -> "?"
    }
    if (moduleId == "ocr" || moduleId == "object") {
        Text(iconText, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    } else {
        Text(iconText, fontSize = 20.sp)
    }
}
