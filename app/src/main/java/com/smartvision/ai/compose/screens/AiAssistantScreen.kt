package com.smartvision.ai.compose.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.presentation.chat.AiChatViewModel
import com.smartvision.ai.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ══════════════════════════════════════════════════════════════════════════════
// CONTEXT-AWARE QUICK SUGGESTIONS
// ══════════════════════════════════════════════════════════════════════════════

private val contextSuggestions = listOf(
    "💡 What can Smart Vision AI do?",
    "📷 How do I scan a document?",
    "🌐 How do I translate text?",
    "♻️ How does waste detection work?",
    "🔍 How does object detection work?",
    "📚 Can you solve math problems?",
    "🎙️ How do I use Voice Translator?",
    "📄 How do I export as PDF?",
    "🔋 Is this app offline-capable?",
    "🤖 Which AI model powers this app?"
)

// ══════════════════════════════════════════════════════════════════════════════
// AI ASSISTANT SCREEN — Premium
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AiAssistantScreen(
    onBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val ext       = MaterialTheme.extended
    val messages  by viewModel.messages.collectAsStateWithLifecycle()
    var input     by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Smoothly scroll to bottom whenever message count changes or the last message's content streams new text
    val lastMessageText = remember { derivedStateOf { messages.lastOrNull()?.text ?: "" } }
    LaunchedEffect(messages.size, lastMessageText.value) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    GradientBackground {
        Column(Modifier.fillMaxSize()) {

            // ── Top bar ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp)).clickable(onClick = onBack), Alignment.Center) {
                    Text("←", color = MaterialTheme.colorScheme.onSurface)
                }
                Column(Modifier.weight(1f)) {
                    Text("🤖 AI Assistant", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Powered by Gemini AI", style = MaterialTheme.typography.labelSmall, color = NeonPurple)
                }
                // Clear chat
                if (messages.isNotEmpty()) {
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(NeonPink.copy(0.1f)).border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(8.dp)).clickable { viewModel.clearChat() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall, color = NeonPink)
                    }
                }
            }

            // ── AI Orb header ──────────────────────────────────────────────────
            // Pulse and spin the orb while the AI is thinking (loading) or streaming!
            AiOrbHeader(isProcessing = viewModel.isLoading || viewModel.isStreaming)

            // ── Messages + suggestions ─────────────────────────────────────────
            LazyColumn(
                state           = listState,
                modifier        = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding  = PaddingValues(vertical = 12.dp)
            ) {
                // Empty state
                if (messages.isEmpty()) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillParentMaxWidth()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Ask me anything about Smart Vision AI\nor any general question!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Text("💡 Quick Suggestions", style = MaterialTheme.typography.labelMedium, color = NeonBlue, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    items(contextSuggestions.take(6)) { chip ->
                        AiSuggestionChip(label = chip, onClick = { viewModel.send(chip) })
                    }
                }

                // Messages
                items(messages) { msg ->
                    AiChatBubble(text = msg.text, isUser = msg.fromUser)
                }

                // Loading indicator (active while model is thinking before first chunk)
                if (viewModel.isLoading) {
                    item { AiTypingIndicator() }
                }
            }

            // ── Input row ──────────────────────────────────────────────────────
            AiInputRow(
                input     = input,
                isLoading = viewModel.isLoading || viewModel.isStreaming,
                onChange  = { input = it },
                onSend    = {
                    if (input.isNotBlank() && !(viewModel.isLoading || viewModel.isStreaming)) {
                        viewModel.send(input.trim())
                        input = ""
                    }
                }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// AI ORB HEADER — Animated plasma orb
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AiOrbHeader(isProcessing: Boolean = false) {
    val inf  = rememberInfiniteTransition(label = "orb")
    val rot  by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "rot")
    val rot2 by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "rot2")
    val pulse by inf.animateFloat(if (isProcessing) 0.8f else 0.92f, if (isProcessing) 1.2f else 1.08f, infiniteRepeatable(tween(if (isProcessing) 500 else 2000), RepeatMode.Reverse), label = "p")
    val glow  by inf.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "g")

    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(90.dp).scale(pulse)) {
            val cx = size.width / 2; val cy = size.height / 2; val r = size.width / 2

            // Outer plasma rings
            listOf(0.95f, 0.80f, 0.65f).forEachIndexed { i, scale ->
                val ringR = r * scale
                val pts   = 60
                val path  = Path()
                for (j in 0..pts) {
                    val angle = (2 * PI * j / pts).toFloat()
                    val wave  = 4f * sin((angle * 3 + rot + i * 1.2f).toDouble()).toFloat()
                    val x     = cx + (ringR + wave) * cos(angle.toDouble()).toFloat()
                    val y     = cy + (ringR + wave) * sin(angle.toDouble()).toFloat()
                    if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, Brush.sweepGradient(
                    listOf(NeonBlue.copy(glow - i * 0.1f), NeonPurple.copy(glow - i * 0.1f), NeonCyan.copy(glow - i * 0.15f), NeonBlue.copy(glow - i * 0.1f))
                ), style = Stroke(2.5f - i * 0.5f))
            }

            // Orbiting particle
            val px = cx + r * 0.6f * cos(rot2.toDouble()).toFloat()
            val py = cy + r * 0.6f * sin(rot2.toDouble()).toFloat()
            drawCircle(NeonCyan.copy(glow), 5f, Offset(px, py))

            // Core glow
            drawCircle(Brush.radialGradient(listOf(NeonPurple.copy(0.5f), NeonBlue.copy(0.2f), Color.Transparent)), r * 0.45f)
        }
        Text("🤖", fontSize = 28.sp, modifier = Modifier.scale(pulse * 0.9f))
    }
}

// FloatingAiOrb is defined in Components.kt and imported via wildcard

// ══════════════════════════════════════════════════════════════════════════════
// CHAT BUBBLE
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiChatBubble(text: String, isUser: Boolean) {
    val ext = MaterialTheme.extended
    val isError = text.startsWith("⚠️ Error")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Futuristic assistant avatar with linear neon border
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(NeonPurple.copy(0.3f), NeonCyan.copy(0.1f))))
                    .border(1.dp, Brush.linearGradient(listOf(NeonCyan, NeonPurple)), CircleShape)
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        // Custom premium coloring and linear neon gradient borders
        val bubbleBackground = when {
            isUser -> Brush.linearGradient(listOf(NeonPurple, NeonBlue))
            isError -> Brush.linearGradient(listOf(NeonPink.copy(0.12f), NeonPink.copy(0.04f)))
            else -> Brush.linearGradient(listOf(ext.glassCard, ext.glassCard))
        }

        val bubbleBorderModifier = when {
            isUser -> Modifier
            isError -> Modifier.border(1.dp, NeonPink.copy(0.5f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
            else -> Modifier.border(
                1.dp, 
                Brush.linearGradient(listOf(NeonCyan.copy(0.4f), NeonPurple.copy(0.4f))), 
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
            )
        }

        val bubbleTextColor = when {
            isUser -> Color.White
            isError -> NeonPink
            else -> MaterialTheme.colorScheme.onSurface
        }

        Box(
            modifier = Modifier.widthIn(max = 285.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (isUser) 18.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 18.dp))
                .background(bubbleBackground)
                .then(bubbleBorderModifier)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                text = text, 
                style = MaterialTheme.typography.bodyMedium, 
                color = bubbleTextColor, 
                lineHeight = 22.sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TYPING INDICATOR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiTypingIndicator() {
    val inf = rememberInfiniteTransition(label = "typing")
    val dots = (0..2).map { i ->
        inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, delayMillis = i * 170), RepeatMode.Reverse), label = "d$i")
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(NeonPurple.copy(0.3f), NeonCyan.copy(0.1f))))
                .border(1.dp, Brush.linearGradient(listOf(NeonCyan, NeonPurple)), CircleShape)
                .align(Alignment.Bottom),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp))
                .background(MaterialTheme.extended.glassCard)
                .border(1.dp, Brush.linearGradient(listOf(NeonCyan.copy(0.3f), NeonPurple.copy(0.3f))), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                dots.forEach { dot ->
                    Box(Modifier.size(7.dp).alpha(dot.value).clip(CircleShape).background(NeonBlue))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SUGGESTION CHIP
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiSuggestionChip(label: String, onClick: () -> Unit) {
    val ext = MaterialTheme.extended
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ext.glassCard)
            .border(1.dp, NeonPurple.copy(0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text("→", color = NeonPurple)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// INPUT ROW
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiInputRow(input: String, isLoading: Boolean, onChange: (String) -> Unit, onSend: () -> Unit) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding().imePadding(),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(ext.glassCard).border(1.dp, NeonPurple.copy(0.4f), RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = input, onValueChange = onChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (input.isEmpty()) Text("Ask me anything...", color = ext.textHint, style = MaterialTheme.typography.bodyMedium)
                    inner()
                }
            )
        }
        val sendModifier = if (isLoading)
            Modifier.size(48.dp).clip(CircleShape).background(ext.glassCard)
        else
            Modifier.size(48.dp).clip(CircleShape).background(Brush.radialGradient(listOf(NeonPurple, NeonBlue)))
        Box(
            modifier = sendModifier.clickable(enabled = !isLoading, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NeonBlue, strokeWidth = 2.dp)
            else Text("➤", color = Color.White, fontSize = 18.sp)
        }
    }
}
