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
import com.smartvision.ai.ui.components.SmartVisionTopBar
import com.smartvision.ai.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// TRANSLATOR SCREEN (text + voice mode)
// ─────────────────────────────────────────────────────────────────────────────

private val LANGUAGES = listOf(
    "English" to "en", "Hindi" to "hi", "Spanish" to "es",
    "French"  to "fr", "German" to "de", "Japanese" to "ja",
    "Chinese" to "zh", "Arabic" to "ar", "Portuguese" to "pt",
    "Russian" to "ru", "Korean" to "ko", "Italian" to "it"
)

@Composable
fun TranslatorScreen(
    voiceMode:  Boolean         = false,
    onBack:     () -> Unit,
    viewModel:  TranslatorViewModel = hiltViewModel()
) {
    val colors  = smartColors
    val uiState by viewModel.uiState.collectAsState()

    val accent = if (voiceMode) SmartVisionColors.voiceTranslator
                 else SmartVisionColors.translator

    Scaffold(
        topBar         = {
            SmartVisionTopBar(
                title = if (voiceMode) "Voice Translator" else "Translator",
                onBack = onBack
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Language selector bar ─────────────────────────────────────────
            LanguageSelectorBar(
                sourceLang     = uiState.sourceLang,
                targetLang     = uiState.targetLang,
                accent         = accent,
                onSwap         = viewModel::swapLanguages,
                onSourceChange = { viewModel.setSourceLang(it) },
                onTargetChange = { viewModel.setTargetLang(it) }
            )

            if (voiceMode) {
                // ── Voice Mode: big mic button ────────────────────────────────
                VoiceModePanel(
                    isListening   = uiState.isListening,
                    spokenText    = uiState.inputText,
                    accent        = accent,
                    onMicClick    = {
                        if (uiState.isListening) viewModel.stopListening()
                        else viewModel.startListening()
                    }
                )
            } else {
                // ── Text Mode: input box ──────────────────────────────────────
                TextInputPanel(
                    text         = uiState.inputText,
                    onTextChange = viewModel::setInputText,
                    accent       = accent,
                    onTranslate  = viewModel::translate,
                    isLoading    = uiState.isTranslating
                )
            }

            // ── Translation output ────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.outputText.isNotEmpty(),
                enter   = fadeIn() + expandVertically()
            ) {
                TranslationOutput(
                    text     = uiState.outputText,
                    accent   = accent,
                    onCopy   = viewModel::copyOutput,
                    onSpeak  = viewModel::speakOutput
                )
            }

            // ── History chips ─────────────────────────────────────────────────
            if (uiState.recentTranslations.isNotEmpty()) {
                Text(
                    "Recent",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.subtext,
                    letterSpacing = 2.sp
                )
                uiState.recentTranslations.take(3).forEach { (src, dst) ->
                    RecentTranslationRow(source = src, translation = dst, accent = accent)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LANGUAGE SELECTOR BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LanguageSelectorBar(
    sourceLang:     String,
    targetLang:     String,
    accent:         Color,
    onSwap:         () -> Unit,
    onSourceChange: (String) -> Unit,
    onTargetChange: (String) -> Unit
) {
    val colors = smartColors
    var showSourceDropdown by remember { mutableStateOf(false) }
    var showTargetDropdown by remember { mutableStateOf(false) }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Source language
        Box(modifier = Modifier.weight(1f)) {
            LanguageButton(
                label   = LANGUAGES.find { it.second == sourceLang }?.first ?: sourceLang,
                accent  = accent,
                onClick = { showSourceDropdown = true }
            )
            DropdownMenu(
                expanded        = showSourceDropdown,
                onDismissRequest = { showSourceDropdown = false }
            ) {
                LANGUAGES.forEach { (name, code) ->
                    DropdownMenuItem(
                        text    = { Text(name, color = colors.onSurface) },
                        onClick = { onSourceChange(code); showSourceDropdown = false }
                    )
                }
            }
        }

        // Swap button
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accent.copy(0.15f), CircleShape)
                .border(1.dp, accent.copy(0.3f), CircleShape)
                .clickable(onClick = onSwap),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SwapHoriz, null, tint = accent, modifier = Modifier.size(20.dp))
        }

        // Target language
        Box(modifier = Modifier.weight(1f)) {
            LanguageButton(
                label   = LANGUAGES.find { it.second == targetLang }?.first ?: targetLang,
                accent  = accent,
                onClick = { showTargetDropdown = true }
            )
            DropdownMenu(
                expanded        = showTargetDropdown,
                onDismissRequest = { showTargetDropdown = false }
            ) {
                LANGUAGES.forEach { (name, code) ->
                    DropdownMenuItem(
                        text    = { Text(name, color = colors.onSurface) },
                        onClick = { onTargetChange(code); showTargetDropdown = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageButton(label: String, accent: Color, onClick: () -> Unit) {
    val colors = smartColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, fontWeight = FontWeight.Medium)
        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = accent, modifier = Modifier.size(18.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TEXT INPUT PANEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TextInputPanel(
    text:         String,
    onTextChange: (String) -> Unit,
    accent:       Color,
    onTranslate:  () -> Unit,
    isLoading:    Boolean
) {
    val colors = smartColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicTextField(
                value         = text,
                onValueChange = onTextChange,
                textStyle     = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text("Enter text to translate...", style = MaterialTheme.typography.bodyLarge, color = colors.subtext)
                    }
                    inner()
                }
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "${text.length}/1000",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.subtext
                )
                Button(
                    onClick  = onTranslate,
                    enabled  = text.isNotEmpty() && !isLoading,
                    colors   = ButtonDefaults.buttonColors(containerColor = accent),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Translate, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Translate", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VOICE MODE PANEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VoiceModePanel(
    isListening: Boolean,
    spokenText:  String,
    accent:      Color,
    onMicClick:  () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "mic")
    val pulse by inf.animateFloat(
        initialValue  = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label         = "pulse"
    )
    val colors = smartColors

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Animated mic button
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(if (isListening) pulse else 1f)
                .background(
                    if (isListening) accent.copy(0.25f) else accent.copy(0.1f),
                    CircleShape
                )
                .border(
                    2.dp,
                    if (isListening) accent else accent.copy(0.3f),
                    CircleShape
                )
                .clickable(onClick = onMicClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Rounded.MicNone else Icons.Rounded.Mic,
                contentDescription = if (isListening) "Stop" else "Start",
                tint     = accent,
                modifier = Modifier.size(52.dp)
            )
        }

        Text(
            text  = if (isListening) "Listening…" else "Tap to speak",
            style = MaterialTheme.typography.titleMedium,
            color = if (isListening) accent else colors.subtext,
            fontWeight = FontWeight.Medium
        )

        if (spokenText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.card, RoundedCornerShape(16.dp))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(spokenText, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TRANSLATION OUTPUT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TranslationOutput(
    text:    String,
    accent:  Color,
    onCopy:  () -> Unit,
    onSpeak: () -> Unit
) {
    val colors = smartColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(0.06f))
            .border(1.dp, accent.copy(0.25f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Translation",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCopy,  modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.ContentCopy, null, tint = accent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onSpeak, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.VolumeUp, null, tint = accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RECENT TRANSLATION ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentTranslationRow(source: String, translation: String, accent: Color) {
    val colors = smartColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(source,      style = MaterialTheme.typography.bodySmall,  color = colors.subtext, maxLines = 1)
            Text(translation, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, maxLines = 1, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Rounded.ArrowForwardIos, null, tint = accent, modifier = Modifier.size(14.dp))
    }
}

// Needed for BasicTextField
@Composable
private fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit
) {
    androidx.compose.foundation.text.BasicTextField(
        value          = value,
        onValueChange  = onValueChange,
        textStyle      = textStyle,
        modifier       = modifier,
        decorationBox  = decorationBox
    )
}
