package com.smartvision.ai.compose.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Three animated pulsing rings
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.75f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "ring1"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            tween(2400, 200, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "ring2"
    )
    val ring3Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            tween(3000, 400, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "ring3"
    )
    val rotDeg by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(8000, easing = LinearEasing), RepeatMode.Restart
        ), label = "orbRot"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "glow"
    )

    // Content fade-in on launch
    var contentAlpha by remember { mutableFloatStateOf(0f) }
    val animatedAlpha by animateFloatAsState(
        targetValue   = contentAlpha,
        animationSpec = tween(1000),
        label         = "contentFade"
    )

    LaunchedEffect(Unit) {
        contentAlpha = 1f
        delay(3200)
        onFinished()
    }

    // Deep dark background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow blobs
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPurple.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.2f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.15f, size.height * 0.2f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonBlue.copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.75f),
                    radius = size.width * 0.45f
                ),
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.85f, size.height * 0.75f)
            )

            // Star particles
            val stars = listOf(
                0.08f to 0.12f, 0.92f to 0.08f, 0.15f to 0.85f,
                0.88f to 0.78f, 0.5f to 0.04f, 0.05f to 0.5f,
                0.78f to 0.25f, 0.25f to 0.65f, 0.65f to 0.92f
            )
            stars.forEachIndexed { i, (x, y) ->
                val col = if (i % 3 == 0) NeonBlue else if (i % 3 == 1) NeonPurple else Color.White
                drawCircle(
                    color  = col.copy(alpha = 0.4f + (i % 3) * 0.1f),
                    radius = 1.5f + (i % 3) * 0.8f,
                    center = Offset(size.width * x, size.height * y)
                )
            }
        }

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = animatedAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Animated Logo Orb ──────────────────────────────────────────
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                // Ring 3 — outermost faint ring
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .graphicsLayer(scaleX = ring3Scale, scaleY = ring3Scale, alpha = 0.08f)
                        .clip(CircleShape)
                        .background(NeonCyan)
                )
                // Ring 2 — mid ring
                Box(
                    modifier = Modifier
                        .size(185.dp)
                        .graphicsLayer(scaleX = ring2Scale, scaleY = ring2Scale, alpha = 0.15f)
                        .clip(CircleShape)
                        .background(NeonPurple)
                )
                // Ring 1 — inner ring
                Box(
                    modifier = Modifier
                        .size(155.dp)
                        .graphicsLayer(scaleX = ring1Scale, scaleY = ring1Scale, alpha = 0.22f)
                        .clip(CircleShape)
                        .background(NeonBlue)
                )
                // Rotating sweep gradient ring
                Box(
                    modifier = Modifier
                        .size(135.dp)
                        .graphicsLayer(rotationZ = rotDeg)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    NeonBlue.copy(0.8f),
                                    NeonPurple.copy(0.8f),
                                    NeonCyan.copy(0.6f),
                                    NeonBlue.copy(0.8f)
                                )
                            )
                        )
                )
                // White core with eye emoji
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(0.98f),
                                    NeonBlue.copy(0.7f),
                                    NeonPurple
                                )
                            )
                        )
                        .graphicsLayer(alpha = glowAlpha.coerceIn(0.7f, 1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👁", fontSize = 40.sp)
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── App Name ───────────────────────────────────────────────────
            Text(
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color      = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) { append("SMART ") }
                    withStyle(
                        SpanStyle(
                            brush = Brush.horizontalGradient(
                                listOf(NeonBlue, NeonCyan)
                            ),
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) { append("VISION AI") }
                },
                style    = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            // ── Tagline ────────────────────────────────────────────────────
            Text(
                "Your All-in-One AI Assistant",
                style      = MaterialTheme.typography.bodyLarge,
                color      = TextSecondary,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Scan. Translate. Solve. Identify. Learn.",
                style     = MaterialTheme.typography.bodySmall,
                color     = TextHint,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(52.dp))

            // ── Get Started Button ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.horizontalGradient(listOf(NeonBlue, NeonPurple))
                    )
                    .clickable { onFinished() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Get Started",
                    style      = MaterialTheme.typography.titleSmall,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
