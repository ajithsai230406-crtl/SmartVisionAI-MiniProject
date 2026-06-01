package com.smartvision.ai.compose.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.ui.theme.*

@Composable
fun VoiceTranslatorScreen(onBack: () -> Unit) {
    val ext = MaterialTheme.extended

    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    // Animate 5 waveform bars
    val bars = (1..5).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue  = 1f,
            animationSpec = infiniteRepeatable(
                animation   = tween(400 + i * 100, easing = FastOutSlowInEasing),
                repeatMode  = RepeatMode.Reverse
            ),
            label = "bar$i"
        )
    }

    var detected     by remember { mutableStateOf("Hindi") }
    var translateTo  by remember { mutableStateOf("English") }
    var isListening  by remember { mutableStateOf(false) }
    var transcript   by remember { mutableStateOf("आप कैसे हो?") }
    var translation  by remember { mutableStateOf("Hello, how are you?") }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(title = "Voice Translator", onBack = onBack)

            Column(
                modifier = Modifier.weight(1f).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                // Listening label
                Text(
                    if (isListening) "Listening..." else "Tap mic to speak",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isListening) ext.neonBlue else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(32.dp))

                // Waveform visualizer
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(60.dp)
                ) {
                    bars.forEach { barHeight ->
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight(barHeight.value)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.verticalGradient(listOf(ext.neonBlue, ext.neonPurple))
                                )
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Language selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LanguagePill("Detected", detected, listOf("Hindi","English","Telugu","Tamil")) {
                        detected = it
                    }
                    Text("→", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    LanguagePill("Translate to", translateTo, listOf("English","Hindi","Telugu","French")) {
                        translateTo = it
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Transcript
                if (transcript.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ext.glassCard)
                            .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            NeonBadge(detected, ext.neonPurple)
                            Spacer(Modifier.height(8.dp))
                            Text(transcript, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ext.glassCard)
                            .border(1.dp, ext.neonBlue.copy(0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            NeonBadge(translateTo, ext.neonBlue)
                            Spacer(Modifier.height(8.dp))
                            Text(translation, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(8.dp))
                            Text("🔊", fontSize = 20.sp, modifier = Modifier.clickable {})
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Mic button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening)
                                Brush.radialGradient(listOf(NeonPink, NeonPurple))
                            else
                                Brush.radialGradient(listOf(ext.neonBlue, ext.neonPurple))
                        )
                        .clickable { isListening = !isListening },
                    contentAlignment = Alignment.Center
                ) { Text("🎙️", fontSize = 32.sp) }

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlineNeonButton("Copy", onClick = {}, modifier = Modifier.weight(1f))
                    OutlineNeonButton("Speak", onClick = {}, modifier = Modifier.weight(1f))
                    OutlineNeonButton("Share", onClick = {}, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RowScope.LanguagePill(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    val ext = MaterialTheme.extended
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Box {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ext.glassCard)
                    .border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp))
                    .clickable { expanded = true }
                    .padding(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(selected, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("▾", color = ext.neonBlue)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
                }
            }
        }
    }
}
