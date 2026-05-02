package com.smartvision.ai.ui.screens.translator

import android.app.Application
import android.content.*
import android.speech.*
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.*
import com.smartvision.ai.ui.theme.SVColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
// ── Screen ────────────────────────────────────────────────────────────────────
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

// ── Language list ─────────────────────────────────────────────────────────────
val LANGUAGES = listOf(
    "English" to "en", "Hindi" to "hi", "Telugu" to "te", "Tamil" to "ta",
    "Kannada" to "kn", "Bengali" to "bn", "Marathi" to "mr", "Gujarati" to "gu",
    "Malayalam" to "ml", "Punjabi" to "pa", "Odia" to "or", "Urdu" to "ur",
    "Spanish" to "es", "French" to "fr", "German" to "de", "Chinese" to "zh",
    "Japanese" to "ja", "Arabic" to "ar"
)

@Composable
fun TranslatorScreen(voiceMode: Boolean = false, onBack: () -> Unit, vm: TranslatorViewModel = hiltViewModel()) {
    val c = svColors; val s by vm.uiState.collectAsState()
    val accent = if (voiceMode) SVColors.green else SVColors.amber

    Scaffold(topBar = { SmartVisionTopBar(if (voiceMode) "Voice Translator" else "Translator", onBack = onBack) },
        containerColor = c.background) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(2.dp))

            // Language bar
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.card)
                .border(1.dp, c.border, RoundedCornerShape(14.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                var showSrc by remember { mutableStateOf(false) }
                var showTgt by remember { mutableStateOf(false) }
                Box(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.surface)
                        .clickable { showSrc = true }.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(LANGUAGES.find { it.second == s.sourceLang }?.first ?: s.sourceLang,
                                style = MaterialTheme.typography.titleMedium, color = c.onSurface, fontWeight = FontWeight.Medium)
                            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    DropdownMenu(showSrc, { showSrc = false }) {
                        LANGUAGES.forEach { (name, code) ->
                            DropdownMenuItem(text = { Text(name, color = c.onSurface) }, onClick = { vm.setSourceLang(code); showSrc = false })
                        }
                    }
                }
                Box(Modifier.size(34.dp).background(accent.copy(.15f), CircleShape).border(1.dp, accent.copy(.3f), CircleShape)
                    .clickable { vm.swapLanguages() }, Alignment.Center) {
                    Icon(Icons.Rounded.SwapHoriz, null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Box(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.surface)
                        .clickable { showTgt = true }.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(LANGUAGES.find { it.second == s.targetLang }?.first ?: s.targetLang,
                                style = MaterialTheme.typography.titleMedium, color = c.onSurface, fontWeight = FontWeight.Medium)
                            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    DropdownMenu(showTgt, { showTgt = false }) {
                        LANGUAGES.forEach { (name, code) ->
                            DropdownMenuItem(text = { Text(name, color = c.onSurface) }, onClick = { vm.setTargetLang(code); showTgt = false })
                        }
                    }
                }
            }

            if (voiceMode) {
                // Voice input panel
                val inf = rememberInfiniteTransition(label = "mic")
                val pulse by inf.animateFloat(1f, 1.14f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "p")
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(Modifier.size(110.dp).scale(if (s.isListening) pulse else 1f)
                        .background(if (s.isListening) accent.copy(.22f) else accent.copy(.1f), CircleShape)
                        .border(2.dp, if (s.isListening) accent else accent.copy(.3f), CircleShape)
                        .clickable { if (s.isListening) vm.stopListening() else vm.startListening() }, Alignment.Center) {
                        Icon(if (s.isListening) Icons.Rounded.MicNone else Icons.Rounded.Mic,
                            null, tint = accent, modifier = Modifier.size(48.dp))
                    }
                    Text(if (s.isListening) "Listening…" else "Tap to speak",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (s.isListening) accent else c.subtext, fontWeight = FontWeight.Medium)
                    // Show recognized text
                    if (s.inputText.isNotEmpty()) Box(Modifier.fillMaxWidth().background(c.card, RoundedCornerShape(14.dp))
                        .border(1.dp, c.border, RoundedCornerShape(14.dp)).padding(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("You said:", style = MaterialTheme.typography.labelLarge, color = c.subtext)
                            Text(s.inputText, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                        }
                    }
                }
            } else {
                // Text input
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
                    .border(1.dp, c.border, RoundedCornerShape(16.dp)).padding(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        androidx.compose.foundation.text.BasicTextField(s.inputText, { vm.setInputText(it) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 190.dp),
                            decorationBox = { inner ->
                                if (s.inputText.isEmpty()) Text("Enter text to translate…", style = MaterialTheme.typography.bodyLarge, color = c.subtext)
                                inner()
                            })
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("${s.inputText.length}/1000", style = MaterialTheme.typography.labelSmall, color = c.subtext)
                            Button(onClick = { vm.translate() }, enabled = s.inputText.isNotEmpty() && !s.isTranslating,
                                colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(12.dp)) {
                                if (s.isTranslating || s.isDownloadingModel) CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                                else { Icon(Icons.Rounded.Translate, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Translate", fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }

            // Output
            AnimatedVisibility(s.outputText.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(accent.copy(.06f))
                    .border(1.dp, accent.copy(.24f), RoundedCornerShape(16.dp)).padding(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Translation", style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { vm.copyOutput() }, Modifier.size(30.dp)) { Icon(Icons.Rounded.ContentCopy, null, tint = accent, modifier = Modifier.size(17.dp)) }
                                IconButton(onClick = { vm.speakOutput() }, Modifier.size(30.dp)) { Icon(Icons.Rounded.VolumeUp, null, tint = accent, modifier = Modifier.size(17.dp)) }
                            }
                        }
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(s.outputText, style = MaterialTheme.typography.bodyLarge, color = c.onSurface)
                        }
                    }
                }
            }

            // Error
            s.error?.let { err ->
                Box(Modifier.fillMaxWidth().background(c.error.copy(.1f), RoundedCornerShape(12.dp))
                    .border(1.dp, c.error.copy(.3f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text(err, color = c.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Downloading notice
            if (s.isDownloadingModel) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), color = accent, strokeWidth = 2.dp)
                Text("Downloading language model…", style = MaterialTheme.typography.bodySmall, color = c.subtext)
            }

            // Recent
            if (s.recentTranslations.isNotEmpty()) {
                Text("RECENT", style = MaterialTheme.typography.labelSmall, color = c.subtext, letterSpacing = 2.sp)
                s.recentTranslations.takeLast(3).forEach { (src, dst) ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.card)
                        .border(1.dp, c.border, RoundedCornerShape(12.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(src, style = MaterialTheme.typography.bodySmall, color = c.subtext, maxLines = 1)
                            Text(dst, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, maxLines = 1, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.Rounded.ArrowForwardIos, null, tint = accent, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
