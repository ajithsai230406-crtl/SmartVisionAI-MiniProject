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

// NO @AndroidEntryPoint — must never use Hilt
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Hardcoded background — zero theme dependency
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF060A0B))
            ) {
                SVASplash {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
private fun SVASplash(onDone: () -> Unit) {

    val enterAlpha = remember { Animatable(0f) }
    val enterScale = remember { Animatable(0.65f) }

    LaunchedEffect(Unit) {
        enterScale.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        enterAlpha.animateTo(1f, tween(500))
        delay(3500)
        onDone()
    }

    val inf = rememberInfiniteTransition(label = "sva")

    val arc1 by inf.animateFloat(0f,   360f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "a1")
    val arc2 by inf.animateFloat(360f, 0f,   infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "a2")
    val ri1  by inf.animateFloat(0.12f,0.50f,infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "r1")
    val ri2  by inf.animateFloat(0.05f,0.28f,infiniteRepeatable(tween(1900, delayMillis = 250), RepeatMode.Reverse), label = "r2")
    val ri3  by inf.animateFloat(0.02f,0.15f,infiniteRepeatable(tween(2400, delayMillis = 500), RepeatMode.Reverse), label = "r3")
    val orb1 by inf.animateFloat(0f,   360f, infiniteRepeatable(tween(4500, easing = LinearEasing)), label = "o1")
    val orb2 by inf.animateFloat(360f, 0f,   infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "o2")
    val scanX by inf.animateFloat(-0.05f,1.05f,infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "sx")
    val glow  by inf.animateFloat(0.55f,1.0f, infiniteRepeatable(tween(900),  RepeatMode.Reverse), label = "g")
    val d0 by inf.animateFloat(0.2f,1f,infiniteRepeatable(tween(460),RepeatMode.Reverse, StartOffset(0)),   label="d0")
    val d1 by inf.animateFloat(0.2f,1f,infiniteRepeatable(tween(460),RepeatMode.Reverse, StartOffset(160)), label="d1")
    val d2 by inf.animateFloat(0.2f,1f,infiniteRepeatable(tween(460),RepeatMode.Reverse, StartOffset(320)), label="d2")

    val CYAN   = Color(0xFF00E5CC)
    val PURPLE = Color(0xFF9B59F5)
    val BG     = Color(0xFF060A0B)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        // ── Horizontal divider + scan sweep ────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.Center)
                .offset(y = 152.dp)
        ) {
            val w = size.width
            // Static dim line
            drawLine(
                brush = Brush.horizontalGradient(listOf(
                    Color.Transparent, CYAN.copy(0.08f), CYAN.copy(0.55f),
                    CYAN.copy(0.08f), Color.Transparent
                )),
                start = Offset(0f, 0f), end = Offset(w, 0f), strokeWidth = 1f
            )
            // Moving glow spot
            val sx = scanX * w
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, CYAN.copy(0f),
                        CYAN, CYAN.copy(0f), Color.Transparent),
                    startX = sx - 160f, endX = sx + 160f
                ),
                start = Offset(0f, 0f), end = Offset(w, 0f), strokeWidth = 3f
            )
        }

        // ── Rings + arcs + SVA logo ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(290.dp)
                .graphicsLayer {
                    scaleX = enterScale.value
                    scaleY = enterScale.value
                    alpha  = enterAlpha.value
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val mr = size.minDimension / 2f

                // Centre radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(CYAN.copy(0.08f), Color.Transparent),
                        Offset(cx, cy), mr * 0.6f
                    ),
                    radius = mr * 0.65f, center = Offset(cx, cy)
                )

                // Pulsing rings
                listOf(
                    Triple(0.98f, ri3 * 0.8f, 0.7f),
                    Triple(0.80f, ri2,         0.9f),
                    Triple(0.62f, ri1,         1.2f),
                    Triple(0.45f, 0.30f,       1.5f)
                ).forEach { (fr, a, sw) ->
                    drawCircle(
                        CYAN.copy(a.coerceIn(0f, 1f)),
                        mr * fr, Offset(cx, cy),
                        style = Stroke(sw)
                    )
                }

                // Logo background circle
                drawCircle(BG, mr * 0.42f, Offset(cx, cy))
                drawCircle(CYAN.copy(0.40f), mr * 0.42f, Offset(cx, cy), style = Stroke(1.5f))

                // Rotating arcs
                fun arc(r: Float, start: Float, sweep: Float, color: Color, sw: Float) {
                    val ra = mr * r
                    drawArc(color, start, sweep, false,
                        Offset(cx - ra, cy - ra), Size(ra * 2, ra * 2),
                        style = Stroke(sw, cap = StrokeCap.Round))
                }
                arc(0.92f, arc1,        125f, CYAN.copy(0.65f),   2.0f)
                arc(0.92f, arc1 + 190f,  55f, CYAN.copy(0.20f),   1.0f)
                arc(0.74f, arc2,          85f, CYAN.copy(0.28f),   1.2f)
                arc(0.56f, arc2 + 90f,    70f, PURPLE.copy(0.32f), 1.0f)

                // Orbiting dots
                fun dot(angle: Float, r: Float, col: Color, sz: Float) {
                    val rad = Math.toRadians(angle.toDouble())
                    drawCircle(col, sz, Offset(
                        cx + mr * r * Math.cos(rad).toFloat(),
                        cy + mr * r * Math.sin(rad).toFloat()
                    ))
                }
                dot(orb1,         0.62f, CYAN,            7f)
                dot(orb1 + 180f,  0.62f, CYAN.copy(0.3f), 4f)
                dot(orb2,         0.47f, PURPLE,          5.5f)
                dot(orb2 + 120f,  0.47f, PURPLE.copy(0.25f), 3f)

                // Centre pulse dot
                drawCircle(CYAN.copy(0.20f), mr * 0.09f, Offset(cx, cy))
                drawCircle(CYAN.copy(glow * 0.8f), mr * 0.04f, Offset(cx, cy))
            }

            // SVA glowing text
            Box(contentAlignment = Alignment.Center) {
                // glow layer
                Text("SVA",
                    fontSize = 46.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00E5CC).copy(glow * 0.28f),
                    letterSpacing = 10.sp,
                    modifier = Modifier.graphicsLayer { scaleX = 1.14f; scaleY = 1.14f }
                )
                // sharp text
                Text("SVA",
                    fontSize = 46.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00E5CC).copy(0.88f + glow * 0.12f),
                    letterSpacing = 10.sp
                )
            }
        }

        // ── Subtitle ────────────────────────────────────────────────────────
        Text(
            "SMART VISION AI",
            fontSize = 11.sp,
            color = Color(0xFF00E5CC).copy(0.40f),
            letterSpacing = 5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 168.dp)
                .alpha(enterAlpha.value)
        )

        // ── Three bouncing dots ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
                .alpha(enterAlpha.value),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(d0, d1, d2).forEach { a ->
                Canvas(Modifier.size(8.dp)) {
                    drawCircle(Color(0xFF00E5CC).copy(alpha = a))
                }
            }
        }
    }
}
