package com.smartvision.ai.compose.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import android.widget.TextView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import io.noties.markwon.Markwon
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
// LOCAL MARKDOWN TEXT PARSER
// ══════════════════════════════════════════════════════════════════════════════

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var remainingLine = line
            var isHeader = false
            var headerStyle: SpanStyle? = null

            if (line.startsWith("### ")) {
                isHeader = true
                headerStyle = SpanStyle(fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 16.sp)
                remainingLine = line.substring(4)
            } else if (line.startsWith("## ")) {
                isHeader = true
                headerStyle = SpanStyle(fontWeight = FontWeight.Bold, color = NeonBlue, fontSize = 18.sp)
                remainingLine = line.substring(3)
            } else if (line.startsWith("# ")) {
                isHeader = true
                headerStyle = SpanStyle(fontWeight = FontWeight.Bold, color = NeonPurple, fontSize = 20.sp)
                remainingLine = line.substring(2)
            }

            if (isHeader && headerStyle != null) {
                withStyle(headerStyle) {
                    append(remainingLine)
                }
            } else {
                var i = 0
                while (i < remainingLine.length) {
                    when {
                        remainingLine.startsWith("**", i) -> {
                            val endIdx = remainingLine.indexOf("**", i + 2)
                            if (endIdx != -1) {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = NeonCyan)) {
                                    append(remainingLine.substring(i + 2, endIdx))
                                }
                                i = endIdx + 2
                            } else {
                                append("**")
                                i += 2
                            }
                        }
                        remainingLine.startsWith("`", i) -> {
                            val endIdx = remainingLine.indexOf("`", i + 1)
                            if (endIdx != -1) {
                                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.Black.copy(0.3f), color = NeonBlue)) {
                                    append(remainingLine.substring(i + 1, endIdx))
                                }
                                i = endIdx + 1
                            } else {
                                append("`")
                                i += 1
                            }
                        }
                        else -> {
                            append(remainingLine[i])
                            i++
                        }
                    }
                }
            }

            if (index < lines.lastIndex) {
                append("\n")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MARKWON MARKDOWN COMPOSABLE
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun MarkdownText(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val markwon = remember(context) { Markwon.create(context) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = 14f
                setLineSpacing(4f, 1.15f)
            }
        },
        update = { textView ->
            textView.setTextColor(textColor.toArgb())
            markwon.setMarkdown(textView, text)
        }
    )
}

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
    val isLastPage by viewModel.isLastPage.collectAsStateWithLifecycle()
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsStateWithLifecycle()
    var input     by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context   = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeaking()
        }
    }

    // Voice input launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                input = spokenText
            }
        }
    }

    // Smoothly scroll to bottom when a new user message is sent or while the AI is loading/streaming
    val lastMessage = messages.lastOrNull()
    LaunchedEffect(lastMessage?.timestamp, lastMessage?.text) {
        if (messages.isNotEmpty() && (lastMessage?.fromUser == true || viewModel.isStreaming || viewModel.isLoading)) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    GradientBackground {
        Column(Modifier.fillMaxSize()) {

            // ── Top bar ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar with green status dot
                Box(
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(NeonPurple.copy(0.3f), NeonCyan.copy(0.1f))))
                            .border(1.5.dp, Brush.linearGradient(listOf(NeonCyan, NeonPurple)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🤖", fontSize = 20.sp)
                    }
                    // Online status indicator dot (green)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FF88))
                            .border(1.5.dp, Color.Black, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        "SmartVision AI Chatbot",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Gemini 1.5 Pro • Home Dashboard",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Delete button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.05f))
                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(10.dp))
                        .clickable { viewModel.clearChat() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🗑️", fontSize = 16.sp)
                }

                // Close button (X)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.05f))
                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White, fontSize = 16.sp)
                }
            }

            // ── AI Orb header ──────────────────────────────────────────────────
            // Pulse and spin the orb while the AI is thinking (loading) or streaming!
            AiOrbHeader(isProcessing = viewModel.isLoading || viewModel.isStreaming)

            // ── Messages ─────────────────────────────────────────
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
                        }
                    }
                }

                // Pagination trigger at the top of the chat
                if (!isLastPage && messages.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingHistory) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = NeonPurple,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                TextButton(
                                    onClick = { viewModel.loadNextPage() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                                ) {
                                    Text("Load Earlier Messages", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }

                // Messages
                items(messages) { msg ->
                    AiChatBubble(
                        text = msg.text, 
                        isUser = msg.fromUser,
                        timestamp = msg.timestamp,
                        onSpeakClick = { viewModel.speak(msg.text) }
                    )
                }

                // Loading indicator (active while model is thinking before first chunk)
                if (viewModel.isLoading) {
                    item { AiTypingIndicator() }
                }
            }

            // ── Suggestions pill row ───────────────────────────────────────────
            val suggestedPrompts by viewModel.suggestedPrompts.collectAsStateWithLifecycle()
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(suggestedPrompts) { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(ext.glassCard)
                            .border(1.dp, NeonPurple.copy(0.35f), RoundedCornerShape(16.dp))
                            .clickable {
                                if (!(viewModel.isLoading || viewModel.isStreaming)) {
                                    viewModel.send(suggestion)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
                },
                onMicClick = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                        }
                        speechRecognizerLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
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

// ══════════════════════════════════════════════════════════════════════════════
// CHAT BUBBLE
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiChatBubble(text: String, isUser: Boolean, timestamp: Long, onSpeakClick: () -> Unit) {
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
            isUser -> Brush.horizontalGradient(listOf(Color(0xFF00D4FF), Color(0xFF8B5CF6)))
            isError -> Brush.linearGradient(listOf(NeonPink.copy(0.12f), NeonPink.copy(0.04f)))
            else -> Brush.linearGradient(listOf(ext.glassCard, ext.glassCard))
        }

        val bubbleBorderModifier = when {
            isUser -> Modifier
            isError -> Modifier.border(1.dp, NeonPink.copy(0.5f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
            else -> Modifier.border(
                1.dp, 
                Color.White.copy(0.1f), 
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
            Column {
                if (isUser) {
                    val formattedText = if (!text.startsWith("🤖")) "🤖 $text" else text
                    val parsedText = remember(formattedText) { parseMarkdown(formattedText) }
                    Text(
                        text = parsedText, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = bubbleTextColor, 
                        lineHeight = 22.sp
                    )
                } else {
                    MarkdownText(
                        text = text,
                        textColor = bubbleTextColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeString = remember(timestamp) {
                        try {
                            val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
                            sdf.format(java.util.Date(timestamp))
                        } catch (e: Exception) {
                            ""
                        }
                    }
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) Color.White.copy(0.6f) else Color.Gray,
                        fontSize = 10.sp
                    )

                    if (!isUser && text.isNotEmpty() && !isError) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ext.glassCard)
                                .border(1.dp, NeonPurple.copy(0.3f), CircleShape)
                                .clickable(onClick = onSpeakClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔊", fontSize = 10.sp)
                        }
                    }
                }
            }
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
// INPUT ROW
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiInputRow(
    input: String, 
    isLoading: Boolean, 
    onChange: (String) -> Unit, 
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding().imePadding(),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Voice Input (Microphone) button on the left
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.08f))
                .border(1.dp, Color.White.copy(0.15f), CircleShape)
                .clickable(enabled = !isLoading, onClick = onMicClick),
            contentAlignment = Alignment.Center
        ) {
            Text("🎙️", fontSize = 18.sp)
        }

        // Text input in the middle
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(0.04f))
                .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = input, onValueChange = onChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (input.isEmpty()) {
                        Text("Ask chatbot contextually...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                    inner()
                }
            )
        }

        // Send button on the right
        val sendModifier = if (isLoading) {
            Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(0.08f))
        } else {
            Modifier.size(48.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))))
        }
        Box(
            modifier = sendModifier.clickable(enabled = !isLoading && input.isNotBlank(), onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NeonBlue, strokeWidth = 2.dp)
            } else {
                Text("➤", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}
