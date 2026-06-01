package com.example.smartvisionai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartvisionai.ui.theme.*

/**
 * Three bouncing dots loader shown while AI processes.
 */
@Composable
fun ThreeDotsLoader(color: Color = CyanAccent) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    @Composable
    fun dot(delayMs: Int): Float {
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(delayMs)
            ),
            label = "dot_$delayMs"
        )
        return scale
    }

    val scale1 = dot(0)
    val scale2 = dot(150)
    val scale3 = dot(300)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(scale1, scale2, scale3).forEach { s ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(s)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * Full-width processing banner shown below camera preview.
 */
@Composable
fun ProcessingBanner(label: String = "Analyzing…") {
    val shimmerAlpha by rememberInfiniteTransition(label = "shimmer").animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "sAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(CyanAccent.copy(0.12f), CyanAccent.copy(0.05f))
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ThreeDotsLoader(CyanAccent.copy(alpha = shimmerAlpha))
        Text(label, fontSize = 13.sp, color = CyanAccent.copy(shimmerAlpha), fontWeight = FontWeight.Medium)
    }
}

/**
 * Shimmer placeholder for history items while loading from DB.
 */
@Composable
fun ShimmerHistoryItem() {
    val shimmer by rememberInfiniteTransition(label = "sh").animateFloat(
        0.2f, 0.5f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "shAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardDark)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = shimmer))
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = shimmer))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = shimmer * 0.6f))
            )
        }
    }
}
