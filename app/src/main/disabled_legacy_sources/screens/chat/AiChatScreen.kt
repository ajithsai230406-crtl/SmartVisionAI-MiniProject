package com.smartvision.ai.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.smartvision.ai.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// AI CHAT ASSISTANT SCREEN — cyberpunk ChatGPT style (image screen 4)
// ─────────────────────────────────────────────────────────────────────────────

data class ChatMessage(
    val text:    String,
    val isUser:  Boolean,
    val time:    String,
    val imageUrl: String? = null
)

@Composable
fun AiChatScreen(
    onBack:         () -> Unit,
    onOpenModule:   (String) -> Unit = {}
) {
    val c = svColors
    var inputText by remember { mutableStateOf("") }
    val messages  = remember {
        mutableStateListOf(
            ChatMessage("Hello Ajith 👋\nI am your AI assistant.\nHow can I help you today?", false, "10:30 AM"),
            ChatMessage("Translate this image", true,  "10:31 AM"),
            ChatMessage("Sure! Please capture or upload the image.", false, "10:31 AM"),
            ChatMessage("Here is the translation:\nनावाचार यहाँ से शुरू होता है", false, "10:32 AM")
        )
    }
    val listState = rememberLazyListState()
    var isTyping  by remember { mutableStateOf(false) }

    val suggestedPrompts = listOf("Scan Text", "What is this?", "Open OCR")

    // Typing indicator animation
    val inf = rememberInfiniteTransition(label = "typing")
    val dot1 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), "d1")
    val dot2 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(600, 200), RepeatMode.Reverse), "d2")
    val dot3 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(600, 400), RepeatMode.Reverse), "d3")

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun send() {
        if (inputText.isBlank()) return
        val txt = inputText.trim()
        messages.add(ChatMessage(txt, true, "Now"))
        inputText = ""
        isTyping  = true

        // Auto-navigate if user asks to open a module
        val lower = txt.lowercase()
        if (lower.contains("ocr") || lower.contains("scan text")) {
            onOpenModule("text_scanner")
        } else if (lower.contains("translate")) {
            onOpenModule("translator")
        } else if (lower.contains("object") || lower.contains("detect")) {
            onOpenModule("object_detection")
        }
    }

    Scaffold(
        containerColor      = Color(0xFF050816),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .background(Color(0xFF050816))
                .border(width=0.dp, color=Color.Transparent)
                .padding(horizontal=16.dp, vertical=10.dp)) {
                Row(modifier=Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick=onBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, null, tint=Color.White, modifier=Modifier.size(20.dp))
                    }
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text("SmartVision AI", style=MaterialTheme.typography.titleMedium, color=Color.White, fontWeight=FontWeight.Bold)
                        Row(horizontalArrangement=Arrangement.spacedBy(5.dp), verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).background(Color(0xFF00E676), CircleShape))
                            Text("Online", style=MaterialTheme.typography.labelSmall, color=Color(0xFF00E676))
                        }
                    }
                    IconButton(onClick={}) {
                        Icon(Icons.Rounded.MoreVert, null, tint=Color.White, modifier=Modifier.size(20.dp))
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier=Modifier.navigationBarsPadding().background(Color(0xFF0A0F1E))) {
                // Suggested prompts
                LazyRow(modifier=Modifier.padding(horizontal=12.dp, vertical=8.dp),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(suggestedPrompts) { prompt ->
                        Box(modifier=Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF121826))
                            .border(1.dp, Color(0xFF7B61FF).copy(0.4f), RoundedCornerShape(20.dp))
                            .clickable { inputText = prompt; send() }
                            .padding(horizontal=14.dp, vertical=7.dp)) {
                            Text(prompt, style=MaterialTheme.typography.labelLarge, color=Color(0xFF7B61FF))
                        }
                    }
                }
                // Input row
                Row(modifier=Modifier.padding(horizontal=12.dp, vertical=8.dp).fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.Bottom) {
                    Box(modifier=Modifier.weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF121826))
                        .border(1.dp, Brush.linearGradient(listOf(Color(0xFF00E5FF).copy(0.3f), Color(0xFF7B61FF).copy(0.3f))), RoundedCornerShape(20.dp))
                        .padding(horizontal=14.dp, vertical=10.dp)) {
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically) {
                            androidx.compose.foundation.text.BasicTextField(
                                value=inputText, onValueChange={ inputText=it },
                                textStyle=MaterialTheme.typography.bodyMedium.copy(color=Color.White),
                                modifier=Modifier.weight(1f),
                                decorationBox={ inner ->
                                    if (inputText.isEmpty()) Text("Type a message...", color=Color(0xFF8892B0), style=MaterialTheme.typography.bodyMedium)
                                    inner()
                                })
                            Icon(Icons.Rounded.Mic, null, tint=Color(0xFF8892B0), modifier=Modifier.size(18.dp))
                        }
                    }
                    // Send button
                    Box(modifier=Modifier.size(48.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFF7B61FF), Color(0xFF00E5FF))), CircleShape)
                        .clickable { send() },
                        contentAlignment=Alignment.Center) {
                        Icon(Icons.Rounded.Send, null, tint=Color.White, modifier=Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state           = listState,
            contentPadding  = PaddingValues(horizontal=14.dp, vertical=8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (isTyping) {
                item {
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp), verticalAlignment=Alignment.CenterVertically,
                        modifier=Modifier.padding(start=8.dp)) {
                        listOf(dot1, dot2, dot3).forEach { alpha ->
                            Box(Modifier.size(7.dp).background(Color(0xFF7B61FF).copy(alpha), CircleShape))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHAT BUBBLE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(msg: ChatMessage) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
    ) {
        if (!msg.isUser) {
            // AI avatar
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically,
                modifier=Modifier.padding(bottom=4.dp)) {
                Box(Modifier.size(28.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF7B61FF), Color(0xFF00E5FF))), CircleShape),
                    contentAlignment=Alignment.Center) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint=Color.White, modifier=Modifier.size(14.dp))
                }
                Text("SmartVision AI", style=MaterialTheme.typography.labelSmall, color=Color(0xFF8892B0))
            }
        }

        Box(modifier=Modifier
            .widthIn(max=280.dp)
            .clip(RoundedCornerShape(
                topStart=if(msg.isUser) 18.dp else 4.dp,
                topEnd=18.dp,
                bottomStart=18.dp,
                bottomEnd=if(msg.isUser) 4.dp else 18.dp))
            .background(if (msg.isUser)
                Brush.linearGradient(listOf(Color(0xFF7B61FF), Color(0xFF5E35B1)))
            else
                Brush.linearGradient(listOf(Color(0xFF121826), Color(0xFF1A2236))))
            .then(if (!msg.isUser)
                Modifier.border(1.dp, Color(0xFF1E2D47), RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
            else Modifier)
            .padding(12.dp)) {
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(msg.text, style=MaterialTheme.typography.bodyMedium, color=Color.White)
            }
        }

        Spacer(Modifier.height(2.dp))
        Text(msg.time, style=MaterialTheme.typography.labelSmall, color=Color(0xFF8892B0),
            modifier=Modifier.padding(horizontal=4.dp))
    }
}
