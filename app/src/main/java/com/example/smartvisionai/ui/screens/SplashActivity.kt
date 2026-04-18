package com.example.smartvisionai.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartvisionai.MainActivity
import kotlinx.coroutines.delay

// ✅ NO @AndroidEntryPoint — Splash must NOT use Hilt
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // ✅ Plain Box background — no theme dependency
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF060A0B))
            ) {
                SplashContent {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
private fun SplashContent(onFinished: () -> Unit) {

    val infinite = rememberInfiniteTransition(label = "s")

    val arcRot by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "rot"
    )
    val r1 by infinite.animateFloat(
        0.10f, 0.45f,
        infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "r1"
    )
    val r2 by infinite.animateFloat(
        0.05f, 0.25f,
        infiniteRepeatable(tween(2000, 300), RepeatMode.Reverse), label = "r2"
    )
    val r3 by infinite.animateFloat(
        0.02f, 0.13f,
        infiniteRepeatable(tween(2500, 600), RepeatMode.Reverse), label = "r3"
    )
    val glow by infinite.animateFloat(
        0.60f, 1.0f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "g"
    )
    val scanX by infinite.animateFloat(
        -0.05f, 1.05f,
        infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "sx"
    )

    val alpha  = remember { Animatable(0f) }
    val scale  = remember { Animatable(0.72f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, tween(400))
        delay(3200)
        onFinished()
    }

    val cyan = Color(0xFF00E5CC)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        // ── Full-width horizontal divider line ──────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.Center)
                .offset(y = 150.dp)
        ) {
            // Static dim line
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        cyan.copy(0.08f),
                        cyan.copy(0.55f),
                        cyan.copy(0.08f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, size.height / 2),
                end   = Offset(size.width, size.height / 2),
                strokeWidth = 1f
            )
            // Moving bright spot
            val x = scanX * size.width
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        cyan.copy(0f),
                        cyan.copy(0.95f),
                        cyan.copy(0f),
                        Color.Transparent
                    ),
                    startX = x - 150f,
                    endX   = x + 150f
                ),
                start = Offset(0f, size.height / 2),
                end   = Offset(size.width, size.height / 2),
                strokeWidth = 2.5f
            )
        }

        // ── Rings + arc + SVA logo ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(280.dp)
                .alpha(alpha.value)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = size.minDimension / 2f

                // --- Concentric rings ---
                fun ring(radiusFraction: Float, a: Float, strokeW: Float) =
                    drawCircle(
                        color  = cyan.copy(alpha = a.coerceIn(0f, 1f)),
                        radius = maxR * radiusFraction,
                        center = Offset(cx, cy),
                        style  = Stroke(width = strokeW)
                    )

                ring(0.97f, r3 * 0.9f, 0.7f)
                ring(0.80f, r2 * 1.0f, 0.9f)
                ring(0.63f, r1 * 1.1f, 1.1f)
                ring(0.46f, 0.30f,     1.4f)

                // Dark logo bg circle
                drawCircle(
                    color  = Color(0xFF060A0B),
                    radius = maxR * 0.43f,
                    center = Offset(cx, cy)
                )

                // --- Rotating arcs ---
                val arcR = maxR * 0.90f
                val tl   = Offset(cx - arcR, cy - arcR)
                val sz   = Size(arcR * 2, arcR * 2)

                drawArc(
                    color = cyan.copy(0.62f), startAngle = arcRot,
                    sweepAngle = 120f, useCenter = false,
                    topLeft = tl, size = sz,
                    style = Stroke(1.8f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = cyan.copy(0.20f), startAngle = arcRot + 180f,
                    sweepAngle = 55f, useCenter = false,
                    topLeft = tl, size = sz,
                    style = Stroke(1.0f, cap = StrokeCap.Round)
                )
                val innerR = maxR * 0.75f
                drawArc(
                    color = cyan.copy(0.15f), startAngle = -arcRot * 0.65f,
                    sweepAngle = 75f, useCenter = false,
                    topLeft = Offset(cx - innerR, cy - innerR),
                    size = Size(innerR * 2, innerR * 2),
                    style = Stroke(0.7f, cap = StrokeCap.Round)
                )
            }

            // SVA text — glow layer + sharp layer
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "SVA",
                    fontSize      = 46.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = cyan.copy(glow * 0.28f),
                    letterSpacing = 10.sp,
                    modifier      = Modifier.graphicsLayer { scaleX = 1.12f; scaleY = 1.12f }
                )
                Text(
                    "SVA",
                    fontSize      = 46.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = cyan.copy(0.88f + glow * 0.12f),
                    letterSpacing = 10.sp
                )
            }
        }

        // ── Three bouncing dots ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
                .alpha(alpha.value),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            listOf(0, 180, 360).forEach { del ->
                val da by infinite.animateFloat(
                    0.18f, 1f,
                    infiniteRepeatable(
                        tween(480, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse,
                        StartOffset(del)
                    ),
                    label = "da$del"
                )
                Canvas(Modifier.size(8.dp)) {
                    drawCircle(cyan.copy(alpha = da))
                }
            }
        }
    }
}
