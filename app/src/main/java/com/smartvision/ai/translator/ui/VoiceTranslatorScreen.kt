package com.smartvision.ai.translator.ui

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.translator.domain.*
import com.smartvision.ai.translator.presentation.VoiceTranslatorViewModel
import com.smartvision.ai.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

// ══════════════════════════════════════════════════════════════════════════════
// VOICE TRANSLATOR — MAIN SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceTranslatorScreen(
    onBack: () -> Unit,
    viewModel: VoiceTranslatorViewModel = hiltViewModel()
) {
    val micPerm      = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val leftLang     by viewModel.leftLang.collectAsStateWithLifecycle()
    val rightLang    by viewModel.rightLang.collectAsStateWithLifecycle()
    val listenState  by viewModel.listeningState.collectAsStateWithLifecycle()
    val activeChannel by viewModel.activeChannel.collectAsStateWithLifecycle()
    val waveAmps     by viewModel.waveAmplitudes.collectAsStateWithLifecycle()
    val partialText  by viewModel.partialText.collectAsStateWithLifecycle()
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val error        by viewModel.error.collectAsStateWithLifecycle()
    var showLangPicker by remember { mutableStateOf<Boolean?>(null) }   // true=left, false=right

    LaunchedEffect(Unit) { if (!micPerm.status.isGranted) micPerm.launchPermissionRequest() }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(modifier = Modifier.fillMaxSize()) {

                // ── Top bar ───────────────────────────────────────────────────
                VoiceTopBar(
                    onBack       = onBack,
                    onClear      = { viewModel.clearConversation() },
                    onSwap       = { viewModel.swapLanguages() },
                    hasConversation = conversation.isNotEmpty()
                )

                // ── Language header ────────────────────────────────────────────
                VoiceLanguageHeader(
                    leftLang  = leftLang,
                    rightLang = rightLang,
                    onLeftClick  = { showLangPicker = true },
                    onRightClick = { showLangPicker = false }
                )

                // ── Conversation thread ────────────────────────────────────────
                Box(modifier = Modifier.weight(1f)) {
                    if (conversation.isEmpty() && listenState == VoiceListeningState.IDLE) {
                        // Empty state
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("🎙️", fontSize = 56.sp)
                                Text("Tap a mic to start speaking", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                Text("Translation plays automatically", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        ConversationThread(
                            entries      = conversation,
                            partialText  = partialText,
                            activeChannel = activeChannel,
                            onSpeakEntry = { viewModel.speakEntry(it) }
                        )
                    }
                }

                // ── Waveform visualizer ────────────────────────────────────────
                AnimatedVisibility(visible = listenState == VoiceListeningState.LISTENING) {
                    VoiceWaveformBar(
                        amplitudes    = waveAmps,
                        activeChannel = activeChannel,
                        modifier      = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 16.dp)
                    )
                }

                // ── Dual mic buttons ───────────────────────────────────────────
                DualMicRow(
                    leftLang      = leftLang,
                    rightLang     = rightLang,
                    listenState   = listenState,
                    activeChannel = activeChannel,
                    hasMicPerm    = micPerm.status.isGranted,
                    onLeftMic     = { if (micPerm.status.isGranted) viewModel.startListening(true) else micPerm.launchPermissionRequest() },
                    onRightMic    = { if (micPerm.status.isGranted) viewModel.startListening(false) else micPerm.launchPermissionRequest() },
                    onStop        = { viewModel.stopListening() }
                )

                Spacer(Modifier.navigationBarsPadding().height(16.dp))
            }

            // ── Error toast ────────────────────────────────────────────────────
            AnimatedVisibility(
                visible  = error != null,
                enter    = slideInVertically { -it } + fadeIn(),
                exit     = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp, start = 16.dp, end = 16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NeonPink.copy(0.15f)).border(1.dp, NeonPink.copy(0.4f), RoundedCornerShape(12.dp)).clickable { viewModel.dismissError() }.padding(12.dp)
                ) { Text("⚠️ ${error ?: ""}", style = MaterialTheme.typography.bodySmall, color = NeonPink) }
            }

            // ── Language picker sheet ──────────────────────────────────────────
            AnimatedVisibility(
                visible  = showLangPicker != null,
                enter    = slideInVertically { it } + fadeIn(),
                exit     = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LanguagePickerSheet(
                    isSource  = showLangPicker == true,
                    current   = if (showLangPicker == true) leftLang else rightLang,
                    onSelect  = {
                        if (showLangPicker == true) viewModel.setLeftLang(it)
                        else viewModel.setRightLang(it)
                        showLangPicker = null
                    },
                    onDismiss = { showLangPicker = null }
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TOP BAR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun VoiceTopBar(onBack: () -> Unit, onClear: () -> Unit, onSwap: () -> Unit, hasConversation: Boolean) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp)).clickable(onClick = onBack), Alignment.Center) {
                Text("←", color = MaterialTheme.colorScheme.onSurface)
            }
            Column {
                Text("🎙️ Voice Translator", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("Conversation Mode · Auto-play", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(8.dp)).clickable(onClick = onSwap).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("⇄ Swap", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
            }
            if (hasConversation) {
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(NeonPink.copy(0.1f)).border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(8.dp)).clickable(onClick = onClear).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("Clear", style = MaterialTheme.typography.labelSmall, color = NeonPink)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// LANGUAGE HEADER
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun VoiceLanguageHeader(
    leftLang:     Language,
    rightLang:    Language,
    onLeftClick:  () -> Unit,
    onRightClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp)).background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Left language
        Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                .background(NeonBlue.copy(0.10f)).border(1.dp, NeonBlue.copy(0.3f), RoundedCornerShape(12.dp))
                .clickable(onClick = onLeftClick).padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(leftLang.flag, fontSize = 22.sp)
                Text(leftLang.displayName, style = MaterialTheme.typography.labelSmall, color = NeonBlue, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) { Text("⇌", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) }
        // Right language
        Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                .background(NeonPurple.copy(0.10f)).border(1.dp, NeonPurple.copy(0.3f), RoundedCornerShape(12.dp))
                .clickable(onClick = onRightClick).padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(rightLang.flag, fontSize = 22.sp)
                Text(rightLang.displayName, style = MaterialTheme.typography.labelSmall, color = NeonPurple, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CONVERSATION THREAD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ConversationThread(
    entries:       List<ConversationEntry>,
    partialText:   String,
    activeChannel: Boolean?,
    onSpeakEntry:  (ConversationEntry) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(entries.size) { if (entries.isNotEmpty()) listState.animateScrollToItem(entries.lastIndex) }

    LazyColumn(
        state          = listState,
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(entries, key = { it.id }) { entry ->
            ConversationBubble(entry = entry, onSpeak = { onSpeakEntry(entry) })
        }

        // Partial recognition text
        if (partialText.isNotBlank()) {
            item {
                val isLeft = activeChannel == true
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isLeft) Arrangement.Start else Arrangement.End
                ) {
                    Box(
                        modifier = Modifier.widthIn(max = 260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isLeft) NeonBlue.copy(0.12f) else NeonPurple.copy(0.12f))
                            .border(1.dp, (if (isLeft) NeonBlue else NeonPurple).copy(0.3f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "$partialText...",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLeft) NeonBlue else NeonPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationBubble(entry: ConversationEntry, onSpeak: () -> Unit) {
    val isLeft  = entry.isUserSide
    val color   = if (isLeft) NeonBlue else NeonPurple
    val ext     = MaterialTheme.extended

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isLeft) Alignment.Start else Alignment.End
    ) {
        // Language label
        Text(
            "${entry.sourceLang.flag} → ${entry.targetLang.flag}",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Box(
            modifier = Modifier.widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isLeft) 4.dp else 18.dp,
                        topEnd   = if (isLeft) 18.dp else 4.dp,
                        bottomStart = 18.dp, bottomEnd = 18.dp
                    )
                )
                .background(ext.glassCard)
                .border(1.dp, color.copy(0.35f), RoundedCornerShape(topStart = if (isLeft) 4.dp else 18.dp, topEnd = if (isLeft) 18.dp else 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Original
                Text(entry.originalText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                HorizontalDivider(color = color.copy(0.15f))
                // Translation
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                    Text(entry.translatedText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), lineHeight = 20.sp)
                    Box(Modifier.size(24.dp).clip(CircleShape).background(color.copy(0.12f)).clickable(onClick = onSpeak), Alignment.Center) {
                        Text("🔊", fontSize = 11.sp)
                    }
                }

                // Premium AI Semantic Breakdown inside Voice bubble
                entry.semanticMeaning?.let { meaning ->
                    val isOffline = meaning.isOfflineMode
                    HorizontalDivider(color = if (isOffline) Color(0xFF4CAF50).copy(0.2f) else color.copy(0.12f))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖", fontSize = 10.sp)
                            Text(
                                if (isOffline) "Offline Smart Semantic Mode" else "AI Semantic Breakdown",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOffline) Color(0xFF81C784) else color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isOffline) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                            }
                        }
                        
                        // English meaning
                        Text("🇺🇸 English:", style = MaterialTheme.typography.labelSmall, color = color.copy(0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(meaning.meaningEnglish, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.5.sp, lineHeight = 14.sp)
                        
                        // Native meaning
                        Text("💡 ${entry.sourceLang.displayName}:", style = MaterialTheme.typography.labelSmall, color = color.copy(0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(meaning.meaningOriginal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.9f), fontSize = 10.5.sp, lineHeight = 14.sp)

                        // Context
                        Text("🎭 Context & Nuance:", style = MaterialTheme.typography.labelSmall, color = color.copy(0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(meaning.contextExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 10.sp, lineHeight = 13.sp)

                        // Usage
                        Text("📝 Usage & Examples:", style = MaterialTheme.typography.labelSmall, color = color.copy(0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(meaning.usageExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 10.sp, lineHeight = 13.sp)

                        // Pronunciation
                        meaning.pronunciation?.let { pron ->
                            Text("🗣 Pronunciation: $pron", style = MaterialTheme.typography.bodySmall, color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } ?: entry.nativeMeaning?.let { meaning ->
                    HorizontalDivider(color = color.copy(0.12f))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 10.sp)
                            Text(
                                "Meaning in ${entry.sourceLang.displayName}:",
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = meaning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 16.sp,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// WAVEFORM VISUALIZER
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun VoiceWaveformBar(amplitudes: List<Float>, activeChannel: Boolean?, modifier: Modifier) {
    val color = if (activeChannel == true) NeonBlue else NeonPurple
    val inf   = rememberInfiniteTransition(label = "wave")
    // Phase shift animation for idle bars
    val phase by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "ph")

    Canvas(modifier = modifier) {
        val barCount = amplitudes.size
        val barWidth = (size.width / barCount) * 0.6f
        val gap      = (size.width / barCount) * 0.4f
        val centerY  = size.height / 2

        amplitudes.forEachIndexed { i, amp ->
            val x         = i * (barWidth + gap) + barWidth / 2
            val barHeight = (size.height * 0.8f) * amp
            val gradient  = Brush.verticalGradient(
                listOf(color.copy(0.3f), color, color.copy(0.3f)),
                startY = centerY - barHeight / 2,
                endY   = centerY + barHeight / 2
            )
            drawRoundRect(
                brush       = gradient,
                topLeft     = Offset(x - barWidth / 2, centerY - barHeight / 2),
                size        = Size(barWidth, barHeight),
                cornerRadius= CornerRadius(barWidth / 2)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// DUAL MIC BUTTONS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DualMicRow(
    leftLang:      Language,
    rightLang:     Language,
    listenState:   VoiceListeningState,
    activeChannel: Boolean?,
    hasMicPerm:    Boolean,
    onLeftMic:     () -> Unit,
    onRightMic:    () -> Unit,
    onStop:        () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
    ) {
        MicButton(
            lang        = leftLang,
            color       = NeonBlue,
            isActive    = activeChannel == true && listenState == VoiceListeningState.LISTENING,
            isProcessing = activeChannel == true && listenState == VoiceListeningState.PROCESSING,
            onClick     = if (listenState != VoiceListeningState.IDLE && activeChannel == true) onStop else onLeftMic
        )

        // Center stop button
        AnimatedVisibility(visible = listenState != VoiceListeningState.IDLE) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(NeonPink.copy(0.2f)).border(2.dp, NeonPink.copy(0.5f), CircleShape)
                    .clickable(onClick = onStop),
                contentAlignment = Alignment.Center
            ) { Text("⏹", fontSize = 18.sp) }
        }

        MicButton(
            lang        = rightLang,
            color       = NeonPurple,
            isActive    = activeChannel == false && listenState == VoiceListeningState.LISTENING,
            isProcessing = activeChannel == false && listenState == VoiceListeningState.PROCESSING,
            onClick     = if (listenState != VoiceListeningState.IDLE && activeChannel == false) onStop else onRightMic
        )
    }
}

@Composable
private fun MicButton(lang: Language, color: Color, isActive: Boolean, isProcessing: Boolean, onClick: () -> Unit) {
    val inf   = rememberInfiniteTransition(label = "mic")
    val pulse by inf.animateFloat(1f, 1.15f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "p")
    val scale  = if (isActive) pulse else 1f

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(scale)
                .clip(CircleShape)
                .then(
                    if (isActive)
                        Modifier.background(Brush.radialGradient(listOf(color, color.copy(0.6f))))
                    else
                        Modifier.background(MaterialTheme.extended.glassCard)
                )
                .border(2.dp, color.copy(if (isActive) 1f else 0.4f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = color, strokeWidth = 3.dp)
            } else {
                Text(if (isActive) "🎙️" else "🎤", fontSize = 28.sp)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(lang.flag, fontSize = 18.sp)
            Text(
                lang.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
        if (isActive) {
            Text("Listening...", style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}
