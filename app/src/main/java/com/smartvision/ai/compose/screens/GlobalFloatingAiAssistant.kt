package com.smartvision.ai.compose.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.smartvision.ai.compose.components.GradientBackground
import com.smartvision.ai.presentation.chat.AiChatViewModel
import com.smartvision.ai.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

// ──────────────────────────────────────────────────────────────────────────────
// PREMIUM CONTEXT-AWARE GLOBAL FLOATING AI ASSISTANT OVERLAY
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GlobalFloatingAiAssistant(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    
    // Do NOT render the global assistant floating orb during onboarding, login, or splash screens
    if (currentRoute == "splash" || currentRoute == "login" || currentRoute == "onboarding") {
        return
    }

    val context = LocalContext.current
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var isExpanded by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    // Draggable coordinate states for floating orb offsets (starts at bottom right)
    var orbOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    // Voice Typing mic properties
    var isListening by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && isExpanded) {
            chatListState.animateScrollToItem(messages.lastIndex)
        }
    }

    val routeName = remember(currentRoute) {
        when (currentRoute) {
            "home" -> "Home Dashboard"
            "ocr_translator" -> "OCR Text Translator"
            "translator_v2" -> "Smart Text Translator"
            "voice_translator_v2" -> "Voice conversation mode"
            "student_helper_v2" -> "AI Student Helper"
            "waste_classifier_v2" -> "AI Waste Classifier"
            "qr_scanner_v2" -> "QR Code Scanner"
            "document_scanner" -> "Document PDF Scanner"
            "medicine_scanner" -> "Premium Medicine Scanner"
            else -> "Smart Vision AI"
        }
    }

    val dynamicSuggestions = remember(currentRoute) {
        when (currentRoute) {
            "ocr_translator" -> listOf(
                "💡 How do I select and translate targeted text?",
                "📸 Can I translate text inside a custom crop box?",
                "❓ Where is the original detected language explanation?"
            )
            "student_helper_v2" -> listOf(
                "🎓 Explain math equations step-by-step",
                "📚 How do I chat with the AI Math Tutor?",
                "📝 Provide a mathematical practice question"
            )
            "waste_classifier_v2" -> listOf(
                "♻️ Show plastic recycling guidelines",
                "🍃 Is organic food waste compostable?",
                "🧴 How are clean glass containers classified?"
            )
            "voice_translator_v2" -> listOf(
                "🎙️ Does voice translator auto-play translations?",
                "🗣️ How do I swap recording channels?",
                "🌐 Is voice processing context-aware?"
            )
            else -> listOf(
                "🤖 What tasks can Smart Vision AI do?",
                "📁 Explain the 9-module features",
                "🔋 Is document scanning offline-capable?"
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── Dimming Shield Overlay ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .pointerInput(Unit) {
                        detectTapGestures { isExpanded = false }
                    }
            )
        }

        // ── 1. COLLAPSED PREMIUM FLOATING AI ORB ──────────────────────────────
        AnimatedVisibility(
            visible = !isExpanded,
            enter = scaleIn(tween(500, easing = EaseOutBack)) + fadeIn(),
            exit = scaleOut(tween(300)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp)
                .offset { IntOffset(orbOffset.x.roundToInt(), orbOffset.y.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        orbOffset = Offset(
                            x = orbOffset.x + dragAmount.x,
                            y = orbOffset.y + dragAmount.y
                        )
                    }
                }
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "orbFloat")
            
            // Vertical floating breathing animation
            val floatAnim by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "float"
            )
            
            // Neon pulse scale
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.32f,
                animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "pulse"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.65f,
                targetValue = 0.05f,
                animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "pulseAlpha"
            )

            var isTapped by remember { mutableStateOf(false) }
            val tapScale by animateFloatAsState(
                targetValue = if (isTapped) 0.85f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "tapScale"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer(translationY = floatAnim, scaleX = tapScale, scaleY = tapScale)
                    .size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                // Radial Neon pulse ring
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale, alpha = pulseAlpha)
                        .background(
                            Brush.radialGradient(listOf(NeonPurple, NeonBlue, Color.Transparent)),
                            shape = CircleShape
                        )
                )

                // Concentric glowing sweep rings
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFF0F172A), Color(0xFF050816)))
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(listOf(NeonBlue, NeonPurple, NeonCyan, NeonBlue)),
                            shape = CircleShape
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isTapped = true
                                    tryAwaitRelease()
                                    isTapped = false
                                    isExpanded = true
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Central Concentric Plasma Spark
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(NeonPurple.copy(0.7f), Color.Transparent))
                            )
                    )
                    Text("🤖", fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        // ── 2. EXPANDED PREMIUM GLASSMORPHIC CHAT PANEL ───────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(350, easing = EaseInExpo)
            ) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xF0050816))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(NeonPurple.copy(0.5f), NeonBlue.copy(0.3f))),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            ) {
                // Drag dismiss handle
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .width(44.dp).height(4.dp)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(0.4f))
                        .align(Alignment.CenterHorizontally)
                        .clickable { isExpanded = false }
                )

                // ── Modern Premium Top Bar ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Blinking glowing avatar
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(NeonPurple.copy(0.4f), NeonBlue.copy(0.2f))))
                                .border(1.5.dp, NeonPurple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 20.sp)
                        }
                        
                        // Blinking online green dot
                        val inf = rememberInfiniteTransition(label = "statusDot")
                        val dotAlpha by inf.animateFloat(
                            initialValue = 0.4f, targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                            label = "dotAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.BottomEnd)
                                .graphicsLayer(alpha = dotAlpha)
                                .clip(CircleShape)
                                .background(NeonCyan)
                                .border(1.5.dp, Color(0xFF050816), CircleShape)
                        )
                    }
                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SmartVision AI Chatbot",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Gemini 1.5 Pro",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text("•", color = Color.White.copy(0.4f), fontSize = 10.sp)
                            Text(
                                routeName,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Clear Chat and Close drawer icons
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Clear conversation history",
                            tint = Color.White.copy(0.6f)
                        )
                    }
                    IconButton(
                        onClick = { isExpanded = false },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.08f))
                    ) {
                        Text("✕", color = Color.White, fontSize = 14.sp)
                    }
                }

                HorizontalDivider(color = NeonPurple.copy(0.18f))

                // Scrollable Chat Window
                LazyColumn(
                    state = chatListState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    if (messages.size <= 1) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Futuristic context-aware assistant. Ask me questions, solve math formulas, identify objects, or clear up doubts regarding the current screen details!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.55f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(Modifier.height(18.dp))
                                Text(
                                    "⚡ Dynamic Suggestions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonPurple,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                        items(dynamicSuggestions) { chip ->
                            SuggestionChipItem(
                                text = chip,
                                onClick = {
                                    viewModel.send(chip, contextRoute = currentRoute)
                                }
                            )
                        }
                    } else {
                        items(messages) { msg ->
                            AssistantChatBubble(
                                text = msg.text,
                                isUser = msg.fromUser
                            )
                        }

                        if (viewModel.isLoading) {
                            item {
                                AssistantTypingIndicator()
                            }
                        }
                    }
                }

                // Chat Input mic text row
                InputVoiceRowItem(
                    input = input,
                    isLoading = viewModel.isLoading,
                    isListening = isListening,
                    hasMicPermission = micPermission.status.isGranted,
                    onVoiceToggle = {
                        if (micPermission.status.isGranted) {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                                isListening = false
                            } else {
                                isListening = true
                                speechRecognizer?.destroy()
                                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                                    setRecognitionListener(object : RecognitionListener {
                                        override fun onReadyForSpeech(params: Bundle?) {}
                                        override fun onBeginningOfSpeech() {}
                                        override fun onRmsChanged(rmsdB: Float) {}
                                        override fun onBufferReceived(buffer: ByteArray?) {}
                                        override fun onEndOfSpeech() { isListening = false }
                                        override fun onError(error: Int) { isListening = false }
                                        override fun onResults(results: Bundle?) {
                                            val voiceText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                                            if (voiceText.isNotBlank()) {
                                                input = voiceText
                                            }
                                            isListening = false
                                        }
                                        override fun onPartialResults(partialResults: Bundle?) {
                                            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                                            if (partial.isNotBlank()) {
                                                input = partial
                                            }
                                        }
                                        override fun onEvent(eventType: Int, params: Bundle?) {}
                                    })
                                }
                                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                                }
                                speechRecognizer?.startListening(speechIntent)
                            }
                        } else {
                            micPermission.launchPermissionRequest()
                        }
                    },
                    onChange = { input = it },
                    onSend = {
                        if (input.isNotBlank() && !viewModel.isLoading) {
                            viewModel.send(input.trim(), contextRoute = currentRoute)
                            input = ""
                        }
                    }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CHAT PANEL CHIPS WIDGET
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuggestionChipItem(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(0.03f))
            .border(1.dp, NeonPurple.copy(0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f)
            )
            Text("⚡", color = NeonPurple, fontSize = 12.sp)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PREMIUM CHAT BUBBLES WITH MONOSPACE CODE BLOCK PARSING
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AssistantChatBubble(text: String, isUser: Boolean) {
    val bubbleBrush = if (isUser) {
        Brush.linearGradient(listOf(NeonBlue, NeonPurple))
    } else {
        Brush.linearGradient(listOf(Color(0x7F111827), Color(0x7F1F2937)))
    }
    
    val bubbleBorderColor = if (isUser) Color.Transparent else NeonPurple.copy(0.25f)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(NeonPurple.copy(0.4f), NeonBlue.copy(0.2f))))
                    .border(1.dp, NeonPurple.copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleBrush)
                .border(
                    width = 1.dp,
                    color = bubbleBorderColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            // Render markdown/codeblocks parsing
            MessageTextParser(text = text)
        }
    }
}

@Composable
private fun MessageTextParser(text: String) {
    if ("```" in text) {
        val parts = text.split("```")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEachIndexed { idx, part ->
                if (idx % 2 == 1) { // Code block
                    val lines = part.trim().lines()
                    val lang = if ((lines.firstOrNull()?.length ?: 0) < 10) lines.firstOrNull() ?: "" else ""
                    val code = if (lang.isNotEmpty()) lines.drop(1).joinToString("\n") else part.trim()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .border(0.5.dp, NeonBlue.copy(0.35f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan,
                                lineHeight = 16.sp
                            )
                        )
                    }
                } else { // Normal text
                    if (part.isNotBlank()) {
                        Text(
                            text = part.trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            lineHeight = 20.sp
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// STREAMING TYPING INDICATOR
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AssistantTypingIndicator() {
    val inf = rememberInfiniteTransition(label = "typing")
    val dots = (0..2).map { i ->
        inf.animateFloat(
            initialValue = 0.3f, targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse),
            label = "dot$i"
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(NeonPurple.copy(0.4f), NeonBlue.copy(0.2f))))
                .border(1.dp, NeonPurple.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                .background(Color.White.copy(0.06f))
                .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                dots.forEach { opacity ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .graphicsLayer(alpha = opacity.value)
                            .clip(CircleShape)
                            .background(NeonBlue)
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// INPUT AND MIC ROW WIDGET
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun InputVoiceRowItem(
    input: String,
    isLoading: Boolean,
    isListening: Boolean,
    hasMicPermission: Boolean,
    onVoiceToggle: () -> Unit,
    onChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Voice mic button with breathing neon pulses
        val inf = rememberInfiniteTransition(label = "micPulse")
        val pulseScale by inf.animateFloat(
            initialValue = 1f, targetValue = 1.18f,
            animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
            label = "pulse"
        )
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer(scaleX = if (isListening) pulseScale else 1f, scaleY = if (isListening) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    if (isListening) NeonPink.copy(0.18f) else Color.White.copy(0.05f)
                )
                .border(
                    width = 1.5.dp,
                    color = if (isListening) NeonPink else NeonPurple.copy(0.35f),
                    shape = CircleShape
                )
                .clickable(onClick = onVoiceToggle),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isListening) "🎙️" else "🎤",
                fontSize = 16.sp
            )
        }

        // Futuristic dark text input field
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(0.05f))
                .border(1.dp, NeonPurple.copy(0.35f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = input,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                cursorBrush = SolidColor(NeonCyan),
                decorationBox = { inner ->
                    if (input.isEmpty()) {
                        Text(
                            text = if (isListening) "Listening spoken voice..." else "Ask chatbot contextually...",
                            color = Color.White.copy(0.40f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    inner()
                }
            )
        }

        // Send bubble
        val sendBgColor = if (isLoading) {
            Color.White.copy(0.05f)
        } else {
            NeonPurple
        }
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(sendBgColor)
                .clickable(enabled = !isLoading) {
                    keyboard?.hide()
                    onSend()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = NeonBlue,
                    strokeWidth = 2.dp
                )
            } else {
                Text("➤", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}
