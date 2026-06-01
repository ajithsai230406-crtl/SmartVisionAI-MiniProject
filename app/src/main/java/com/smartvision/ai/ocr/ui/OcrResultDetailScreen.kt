package com.smartvision.ai.ocr.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.ocr.domain.*
import com.smartvision.ai.ocr.presentation.OcrTranslatorViewModel
import com.smartvision.ai.ui.theme.*

/**
 * Full-screen translation result view — shown after text is captured.
 * Displays source text, language pair, translation with word-by-word highlight,
 * TTS controls, copy/share, and offline model status.
 */
@Composable
fun OcrResultDetailScreen(
    onBack: () -> Unit,
    viewModel: OcrTranslatorViewModel = hiltViewModel()
) {
    val ext              = MaterialTheme.extended
    val frozenResult     by viewModel.frozenResult.collectAsStateWithLifecycle()
    val translationState by viewModel.translationState.collectAsStateWithLifecycle()
    val targetLanguage   by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val ctx              = LocalContext.current

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(
                title  = "Translation Result",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Source Text Panel ──────────────────────────────────────────
                frozenResult?.let { result ->
                    SourceTextPanel(
                        result      = result,
                        onSpeak     = { viewModel.speak(result.fullText, result.detectedLanguage) },
                        onCopy      = { copyText(ctx, result.fullText) }
                    )
                }

                // ── Language Pair & Translate ──────────────────────────────────
                frozenResult?.let { result ->
                    TranslationControlRow(
                        sourceLangName = result.detectedLanguageName,
                        targetLanguage = targetLanguage,
                        languages      = viewModel.supportedLanguages,
                        onLangChange   = { viewModel.setTargetLanguage(it) },
                        onTranslate    = { viewModel.translateCurrentText() }
                    )
                }

                // ── Offline Model Status ───────────────────────────────────────
                OfflineModelInfo()

                // ── Translation Result Panel ───────────────────────────────────
                FullTranslationPanel(
                    state   = translationState,
                    onSpeak = { text -> viewModel.speak(text) },
                    onCopy  = { text -> copyText(ctx, text) },
                    onShare = { text -> shareText(ctx, text) }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SOURCE TEXT PANEL
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SourceTextPanel(result: OcrResult, onSpeak: () -> Unit, onCopy: () -> Unit) {
    val ext = MaterialTheme.extended
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ext.glassCard)
            .border(1.dp, NeonBlue.copy(0.3f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Detected Text",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            result.detectedLanguageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (result.detectedLanguage != "und") {
                            NeonBadge("✓ Detected", NeonGreen)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionPill("🔊", onClick = onSpeak)
                    ActionPill("📋", onClick = onCopy)
                }
            }

            Spacer(Modifier.height(10.dp))
            Divider(color = ext.glassBorder)
            Spacer(Modifier.height(10.dp))

            // Full text
            Text(
                text       = result.fullText,
                style      = MaterialTheme.typography.bodyLarge,
                color      = MaterialTheme.colorScheme.onSurface,
                lineHeight = 26.sp
            )

            // Block count
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("${result.blocks.size} text blocks")
                InfoChip("${result.fullText.length} characters")
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// TRANSLATION CONTROL ROW
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TranslationControlRow(
    sourceLangName: String,
    targetLanguage: String,
    languages:      List<String>,
    onLangChange:   (String) -> Unit,
    onTranslate:    () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val ext = MaterialTheme.extended

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Source
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(ext.glassCard)
                .border(1.dp, ext.glassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column {
                Text("Source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(sourceLangName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            }
        }

        Text("→", color = NeonBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        // Target dropdown
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ext.glassCard)
                    .border(1.dp, NeonBlue.copy(0.4f), RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column {
                    Text("Translate to", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(targetLanguage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        Text("▾", color = NeonBlue, fontSize = 12.sp)
                    }
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                languages.forEach { lang ->
                    DropdownMenuItem(
                        text    = { Text(lang) },
                        onClick = { onLangChange(lang); expanded = false },
                        trailingIcon = if (lang == targetLanguage) ({ Text("✓", color = NeonBlue) }) else null
                    )
                }
            }
        }
    }

    NeonButton(
        text     = "Translate Now",
        onClick  = onTranslate,
        modifier = Modifier.fillMaxWidth()
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// OFFLINE MODEL INFO
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun OfflineModelInfo() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NeonGreen.copy(0.07f))
            .border(1.dp, NeonGreen.copy(0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text("📶", fontSize = 14.sp)
        Column {
            Text(
                "Offline capable after first use",
                style = MaterialTheme.typography.labelSmall,
                color = NeonGreen,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "ML Kit translation models are downloaded once and cached locally",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// FULL TRANSLATION PANEL
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun FullTranslationPanel(
    state:   TranslationState,
    onSpeak: (String) -> Unit,
    onCopy:  (String) -> Unit,
    onShare: (String) -> Unit
) {
    val ext = MaterialTheme.extended
    when (state) {
        is TranslationState.Idle -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ext.glassCard)
                    .border(1.dp, ext.glassBorder, RoundedCornerShape(18.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌐", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Select a language and tap Translate", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        is TranslationState.Downloading -> LoadingTranslationCard("⬇ Downloading language model...", NeonPurple)
        is TranslationState.Translating  -> LoadingTranslationCard("Translating...", NeonBlue)

        is TranslationState.Success -> {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ext.glassCard)
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(NeonPurple.copy(0.5f), NeonBlue.copy(0.3f))),
                        RoundedCornerShape(18.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            NeonBadge(state.sourceLang, NeonBlue)
                            Text("→", color = Color.White.copy(0.4f), fontSize = 12.sp)
                            NeonBadge(state.targetLang, NeonPurple)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActionPill("🔊") { onSpeak(state.translatedText) }
                            ActionPill("📋") { onCopy(state.translatedText) }
                            ActionPill("↑")  { onShare(state.translatedText) }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Divider(color = ext.glassBorder)
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text       = state.translatedText,
                        style      = MaterialTheme.typography.bodyLarge,
                        color      = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 28.sp
                    )
                }
            }
        }

        is TranslationState.Error -> {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeonPink.copy(0.08f))
                    .border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.Top
                ) {
                    Text("⚠️", fontSize = 20.sp)
                    Column {
                        Text("Translation Failed", style = MaterialTheme.typography.titleSmall, color = NeonPink, fontWeight = FontWeight.SemiBold)
                        Text(state.message, style = MaterialTheme.typography.bodySmall, color = NeonPink.copy(0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingTranslationCard(msg: String, color: Color) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(0.07f))
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = color, strokeWidth = 2.dp)
            Text(msg, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SMALL HELPERS
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActionPill(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(icon, fontSize = 14.sp) }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.extended.glassCard)
            .border(1.dp, MaterialTheme.extended.glassBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun copyText(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Translation", text))
}

private fun shareText(ctx: Context, text: String) {
    ctx.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) },
            "Share translation"
        )
    )
}
