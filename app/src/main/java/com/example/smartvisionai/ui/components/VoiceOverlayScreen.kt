package com.example.smartvisionai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.smartvisionai.ui.theme.*

enum class VoiceState { IDLE, LISTENING, PROCESSING, DONE }

@Composable
fun VoiceInputDialog(
    state: VoiceState,
    partialText: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Stop click propagation on card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardDark)
                    .clickable { /* consume */ }
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Animated mic
                    AnimatedMicButton(state = state)

                    // Status text
                    Text(
                        text = when (state) {
                            VoiceState.IDLE       -> "Tap to speak"
                            VoiceState.LISTENING  -> "Listening…"
                            VoiceState.PROCESSING -> "Processing…"
                            VoiceState.DONE       -> "Done"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (state == VoiceState.LISTENING) CyanAccent else TextSecondary
                    )

                    // Partial transcript
                    if (partialText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0D1520)
                        ) {
                            Text(
                                text = partialText,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Close
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedMicButton(state: VoiceState) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic")

    val ring1Scale by infiniteTransition.animateFloat(
        1f, 1.4f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "s1"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        1f, 1.7f,
        infiniteRepeatable(tween(1100, 200), RepeatMode.Reverse), label = "s2"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        0.5f, 0f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a1"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        0.3f, 0f,
        infiniteRepeatable(tween(1100, 200), RepeatMode.Reverse), label = "a2"
    )

    val isListening = state == VoiceState.LISTENING

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp)
    ) {
        // Ripple rings (only when listening)
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(ring2Scale)
                    .clip(CircleShape)
                    .background(CyanAccent.copy(alpha = ring2Alpha))
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(ring1Scale)
                    .clip(CircleShape)
                    .background(CyanAccent.copy(alpha = ring1Alpha))
            )
        }

        // Mic circle button
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    if (isListening)
                        Brush.radialGradient(listOf(CyanAccent, CyanDark))
                    else
                        Brush.radialGradient(listOf(Color(0xFF1C2A32), CardDark))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (state == VoiceState.PROCESSING) Icons.Default.Close else Icons.Default.Mic,
                contentDescription = null,
                tint = if (isListening) Color(0xFF001A16) else CyanAccent,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
