package com.smartvision.ai.ui.screens.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        visible = true

        val steps = 60
        repeat(steps) {
            delay(40)
            progress = (it + 1).toFloat() / steps
        }

        delay(300)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val outerPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outerPulse"
    )

    val innerRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "innerRotate"
    )

    val particleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particleAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050816)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x507B61FF), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.2f),
                    radius = 350f
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4000E5FF), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.8f),
                    radius = 300f
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x302979FF), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = 500f
                )
            )

            val particlePositions = listOf(
                Offset(size.width * 0.15f, size.height * 0.3f),
                Offset(size.width * 0.8f, size.height * 0.2f),
                Offset(size.width * 0.25f, size.height * 0.7f),
                Offset(size.width * 0.75f, size.height * 0.75f),
                Offset(size.width * 0.5f, size.height * 0.15f),
                Offset(size.width * 0.9f, size.height * 0.55f)
            )

            particlePositions.forEach { position ->
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = particleAlpha * 0.5f),
                    radius = 3f,
                    center = position
                )
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(durationMillis = 800)) +
                    scaleIn(
                        animationSpec = tween(durationMillis = 800, easing = EaseOutBack),
                        initialScale = 0.7f
                    )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(outerPulse)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x307B61FF),
                                        Color(0x1500E5FF),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer { rotationZ = innerRotate }
                            .border(
                                width = 1.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF00E5FF),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .graphicsLayer { rotationZ = -innerRotate * 0.7f }
                            .border(
                                width = 1.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF7B61FF),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF1A1050),
                                        Color(0xFF0A0820)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFF7B61FF)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF00E5FF).copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF7B61FF),
                                            Color(0xFF00E5FF)
                                        )
                                    ),
                                    radius = size.minDimension * 0.4f,
                                    center = Offset(size.width / 2f, size.height / 2f)
                                )

                                drawCircle(
                                    color = Color(0xFF050816),
                                    radius = size.minDimension * 0.18f,
                                    center = Offset(size.width / 2f, size.height / 2f)
                                )

                                drawCircle(
                                    color = Color(0xFF00E5FF).copy(alpha = 0.9f),
                                    radius = size.minDimension * 0.07f,
                                    center = Offset(
                                        x = size.width * 0.62f,
                                        y = size.height * 0.38f
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        text = "SMART",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp
                    )

                    Text(
                        text = "VISION",
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 3.sp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF7B61FF)
                                )
                            )
                        )
                    )

                    Text(
                        text = " AI",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp
                        ),
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your Intelligent Vision Assistant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8892B0),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(60.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2D47))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF7B61FF),
                                            Color(0xFF00E5FF)
                                        )
                                    )
                                )
                        )
                    }

                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8892B0)
                    )
                }
            }
        }
    }
}