package com.smartvision.ai.ui.screens.translator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartvision.ai.ui.components.*
import com.smartvision.ai.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// TRANSLATOR SCREEN — matches image screen 8
// Shows translated text + original beneath it
// ─────────────────────────────────────────────────────────────────────────────

private val LANGUAGES = listOf(
    "Auto Detect" to "auto", "English" to "en", "Hindi" to "hi",
    "Spanish" to "es", "French" to "fr", "German" to "de",
    "Japanese" to "ja", "Chinese" to "zh", "Arabic" to "ar",
    "Portuguese" to "pt", "Russian" to "ru", "Korean" to "ko",
    "Telugu" to "te", "Tamil" to "ta", "Bengali" to "bn",
    "Urdu" to "ur", "Marathi" to "mr", "Gujarati" to "gu",
    "Italian" to "it"
)

@Composable
fun TranslatorScreen(
    voiceMode: Boolean = false,
    onBack:    () -> Unit,
    viewModel: TranslatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor      = Color(0xFF050816),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(if (voiceMode) "Voice Translator" else "Translation",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White, fontWeight = FontWeight.Bold)
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.Settings, null, tint = Color(0xFF8892B0), modifier = Modifier.size(20.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)
            .padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // ── Language selector ─────────────────────────────────────────────
            TranslatorLangBar(
                sourceLang     = uiState.sourceLang,
                targetLang     = uiState.targetLang,
                onSwap         = viewModel::swapLanguages,
                onSourceChange = viewModel::setSourceLang,
                onTargetChange = viewModel::setTargetLang
            )

            // ── Source text input ─────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF121826))
                .border(1.dp, Color(0xFF1E2D47), RoundedCornerShape(18.dp))
                .padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Source Text", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8892B0), letterSpacing = 1.sp)
                        IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.ContentCopy, null, tint = Color(0xFF8892B0), modifier = Modifier.size(16.dp))
                        }
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value         = uiState.inputText,
                        onValueChange = { if (it.length <= 1000) viewModel.setInputText(it) },
                        textStyle     = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        modifier      = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                        decorationBox = { inner ->
                            if (uiState.inputText.isEmpty())
                                Text("Enter text to translate…", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF8892B0))
                            inner()
                        }
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${uiState.inputText.length}/1000", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8892B0))
                        IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.VolumeUp, null, tint = Color(0xFF8892B0), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ── Translate button ──────────────────────────────────────────────
            Button(
                onClick  = viewModel::translate,
                enabled  = uiState.inputText.isNotEmpty() && !uiState.isTranslating,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF00B4E6), Color(0xFF00E5FF))),
                        RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center) {
                    if (uiState.isTranslating) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Translate, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("Translate", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Translation output — with original beneath ────────────────────
            AnimatedVisibility(uiState.outputText.isNotEmpty(), enter = fadeIn() + expandVertically()) {
                Box(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0D1628))
                    .border(1.dp, Color(0xFF1E2D47), RoundedCornerShape(18.dp))
                    .padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Translated text label + actions
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Translated Text", style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8892B0), letterSpacing = 1.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = viewModel::copyOutput, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.ContentCopy, null, tint = Color(0xFF8892B0), modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = viewModel::speakOutput, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.VolumeUp, null, tint = Color(0xFF8892B0), modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.Share, null, tint = Color(0xFF8892B0), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Translated text (target language)
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(uiState.outputText, style = MaterialTheme.typography.bodyLarge,
                                color = Color.White, fontWeight = FontWeight.Medium)
                        }

                        // ── Divider ────────────────────────────────────────────
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF1E2D47), Color.Transparent))))

                        // ── Original text beneath (requirement: show in source language) ──
                        if (uiState.inputText.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                val sourceName = LANGUAGES.find { it.second == uiState.sourceLang }?.first ?: "Original"
                                Text("Original ($sourceName)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8892B0).copy(0.7f), letterSpacing = 0.5.sp)
                                Text(uiState.inputText, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8892B0))
                            }
                        }
                    }
                }
            }

            // ── Voice mode panel ──────────────────────────────────────────────
            if (voiceMode) {
                VoicePanel(uiState.isListening,
                    onStart = viewModel::startListening,
                    onStop  = viewModel::stopListening)
            }

            // ── Recent ────────────────────────────────────────────────────────
            if (uiState.recentTranslations.isNotEmpty()) {
                Text("Recent", style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF8892B0), letterSpacing = 2.sp)
                uiState.recentTranslations.take(3).forEach { (src, dst) ->
                    Box(modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF121826))
                        .border(1.dp, Color(0xFF1E2D47), RoundedCornerShape(12.dp))
                        .padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(src, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8892B0), maxLines = 1)
                                Text(dst, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 1, fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Rounded.ArrowForwardIos, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TranslatorLangBar(
    sourceLang: String, targetLang: String,
    onSwap: () -> Unit, onSourceChange: (String) -> Unit, onTargetChange: (String) -> Unit
) {
    var showSrc by remember { mutableStateOf(false) }
    var showTgt by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xFF121826))
        .border(1.dp, Color(0xFF1E2D47), RoundedCornerShape(14.dp))
        .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically) {

        // Source
        Box(Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0A0F1E))
                .clickable { showSrc = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(LANGUAGES.find { it.second == sourceLang }?.first ?: sourceLang,
                        style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                }
            }
            DropdownMenu(expanded = showSrc, onDismissRequest = { showSrc = false },
                modifier = Modifier.background(Color(0xFF121826))) {
                LANGUAGES.forEach { (name, code) ->
                    DropdownMenuItem(text = { Text(name, color = Color.White) },
                        onClick = { onSourceChange(code); showSrc = false })
                }
            }
        }

        // Swap
        Box(Modifier.size(34.dp)
            .background(Color(0xFF7B61FF).copy(0.2f), CircleShape)
            .border(1.dp, Color(0xFF7B61FF).copy(0.4f), CircleShape)
            .clickable(onClick = onSwap), Alignment.Center) {
            Icon(Icons.Rounded.SwapHoriz, null, tint = Color(0xFF7B61FF), modifier = Modifier.size(18.dp))
        }

        // Target
        Box(Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0A0F1E))
                .clickable { showTgt = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(LANGUAGES.find { it.second == targetLang }?.first ?: targetLang,
                        style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                }
            }
            DropdownMenu(expanded = showTgt, onDismissRequest = { showTgt = false },
                modifier = Modifier.background(Color(0xFF121826))) {
                LANGUAGES.forEach { (name, code) ->
                    DropdownMenuItem(text = { Text(name, color = Color.White) },
                        onClick = { onTargetChange(code); showTgt = false })
                }
            }
        }
    }
}

@Composable
private fun VoicePanel(isListening: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val inf   = rememberInfiniteTransition(label = "v")
    val pulse by inf.animateFloat(1f, 1.2f,
        infiniteRepeatable(tween(600), RepeatMode.Reverse), "vp")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(90.dp).scale(if (isListening) pulse else 1f)
            .background(if (isListening) Color(0xFF00E5FF).copy(0.2f) else Color(0xFF121826), CircleShape)
            .border(2.dp, if (isListening) Color(0xFF00E5FF) else Color(0xFF1E2D47), CircleShape)
            .clickable { if (isListening) onStop() else onStart() },
            Alignment.Center) {
            Icon(if (isListening) Icons.Rounded.MicNone else Icons.Rounded.Mic, null,
                tint = if (isListening) Color(0xFF00E5FF) else Color.White, modifier = Modifier.size(38.dp))
        }
        Text(if (isListening) "Listening…" else "Tap to speak",
            style = MaterialTheme.typography.titleSmall,
            color = if (isListening) Color(0xFF00E5FF) else Color(0xFF8892B0))
    }
}
