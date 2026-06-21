package com.smartvision.ai.translator.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.translator.domain.*
import com.smartvision.ai.translator.presentation.TextTranslatorViewModel
import com.smartvision.ai.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
// SMART TEXT TRANSLATOR — MAIN SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SmartTextTranslatorScreen(
    onBack: () -> Unit,
    viewModel: TextTranslatorViewModel = hiltViewModel()
) {
    val state          by viewModel.state.collectAsStateWithLifecycle()
    val inputText      by viewModel.inputText.collectAsStateWithLifecycle()
    val sourceLang     by viewModel.sourceLang.collectAsStateWithLifecycle()
    val targetLang     by viewModel.targetLang.collectAsStateWithLifecycle()
    val explanation    by viewModel.explanation.collectAsStateWithLifecycle()
    val showLangPicker by viewModel.showLangPicker.collectAsStateWithLifecycle()
    val isAutoDetect   by viewModel.isAutoDetect.collectAsStateWithLifecycle()
    val recent         by viewModel.recentTranslations.collectAsStateWithLifecycle()
    val ocrState       by viewModel.ocrInputState.collectAsStateWithLifecycle()
    val keyboard       = LocalSoftwareKeyboardController.current
    val ctx            = LocalContext.current

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.analyzeImageUri(it) }
    }

    // Camera launcher (use a temp file approach with TakePicture)
    val cameraUri = remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri.value?.let { uri ->
                val bmp = android.graphics.BitmapFactory.decodeStream(
                    ctx.contentResolver.openInputStream(uri)
                )
                bmp?.let { viewModel.analyzeImageBitmap(it) }
            }
        }
    }

    fun launchCamera() {
        val file = java.io.File(ctx.cacheDir, "smart_translator_ocr_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            ctx, "${ctx.packageName}.fileprovider", file
        )
        cameraUri.value = uri
        cameraLauncher.launch(uri)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopSpeaking() }
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Top bar ─────────────────────────────────────────────────
                TranslatorTopBar(onBack = onBack)

                // ── Language selector row ──────────────────────────────────
                LanguageSelectorRow(
                    sourceLang     = sourceLang,
                    targetLang     = targetLang,
                    isAutoDetect   = isAutoDetect,
                    onSourceClick  = { viewModel.showSourcePicker() },
                    onTargetClick  = { viewModel.showTargetPicker() },
                    onSwap         = { viewModel.swapLanguages() },
                    onAutoDetect   = { viewModel.toggleAutoDetect() }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    // ── Image source row (Camera | Gallery) ────────────────
                    ImageSourceRow(
                        onCamera  = { launchCamera() },
                        onGallery = { galleryLauncher.launch("image/*") }
                    )

                    // ── OCR processing feedback ────────────────────────────
                    AnimatedVisibility(visible = ocrState is OcrInputState.Processing) {
                        OcrProcessingBanner()
                    }
                    AnimatedVisibility(visible = ocrState is OcrInputState.Error) {
                        val errMsg = (ocrState as? OcrInputState.Error)?.message ?: ""
                        ErrorBanner("📷 $errMsg")
                    }
                    AnimatedVisibility(visible = ocrState is OcrInputState.Success) {
                        OcrSuccessBanner {
                            viewModel.dismissOcrState()
                        }
                    }

                    // ── Input card ──────────────────────────────────────────
                    TranslatorInputCard(
                        text        = inputText,
                        sourceLang  = sourceLang,
                        isAutoDetect = isAutoDetect,
                        onChange    = { viewModel.setInput(it) },
                        onTranslate = { keyboard?.hide(); viewModel.translate() },
                        onClear     = { viewModel.clearAll() },
                        onSpeak     = { viewModel.speakOriginal() }
                    )

                    // ── Loading ────────────────────────────────────────────
                    AnimatedVisibility(
                        visible = state is TranslatorState.Translating || state is TranslatorState.Downloading
                    ) {
                        TranslatingIndicator(
                            label = if (state is TranslatorState.Downloading)
                                "Downloading language model..." else "Translating..."
                        )
                    }

                    // ── Translation result card ────────────────────────────
                    AnimatedVisibility(
                        visible = state is TranslatorState.Success,
                        enter   = fadeIn() + expandVertically()
                    ) {
                        (state as? TranslatorState.Success)?.let { s ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TranslationResultCard(
                                    result       = s.result,
                                    onSpeakSrc   = { viewModel.speakOriginal() },
                                    onSpeak      = { viewModel.speakTranslation() },
                                    onCopy       = {
                                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Translation", s.result.translatedText))
                                    },
                                    onShare      = { shareText(ctx, s.result.translatedText) },
                                    onSave       = {
                                        val filename = "SmartVisionAI_Translation_${System.currentTimeMillis() / 1000}.txt"
                                        com.smartvision.ai.utils.FileExportHelper.saveTextFile(ctx, filename, s.result.translatedText)
                                            .onSuccess {
                                                android.widget.Toast.makeText(ctx, "Exported: $filename to Documents", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                            .onFailure {
                                                android.widget.Toast.makeText(ctx, "Export failed: ${it.message}", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                    }
                                )

                                s.result.semanticMeaning?.let { meaning ->
                                    SemanticMeaningCard(
                                        meaning            = meaning,
                                        sourceLangName     = s.result.detectedLang.displayName,
                                        targetLangName     = s.result.targetLang.displayName,
                                        targetLangMeaning  = s.result.meaningInTarget ?: meaning.meaningInTarget
                                    )
                                }
                            }
                        }
                    }

                    // ── Error ──────────────────────────────────────────────
                    AnimatedVisibility(visible = state is TranslatorState.Error) {
                        val msg = (state as? TranslatorState.Error)?.message ?: ""
                        ErrorBanner(msg)
                    }

                    // ── Recent translations ────────────────────────────────
                    if (recent.isNotEmpty() && state is TranslatorState.Idle) {
                        RecentTranslationsSection(
                            recent  = recent,
                            onTap   = {
                                viewModel.setInput(it.originalText)
                                viewModel.setTargetLang(it.targetLang)
                                viewModel.translate(it.originalText)
                            }
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }

            // ── Language picker overlay ────────────────────────────────────
            AnimatedVisibility(
                visible  = showLangPicker != null,
                enter    = slideInVertically { it } + fadeIn(),
                exit     = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LanguagePickerSheet(
                    isSource  = showLangPicker == true,
                    current   = if (showLangPicker == true) sourceLang else targetLang,
                    onSelect  = {
                        if (showLangPicker == true) viewModel.setSourceLang(it)
                        else viewModel.setTargetLang(it)
                    },
                    onDismiss = { viewModel.hideLangPicker() }
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TOP BAR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TranslatorTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.extended.glassCard)
                .border(1.dp, MaterialTheme.extended.glassBorder, RoundedCornerShape(10.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Text("←", color = MaterialTheme.colorScheme.onSurface) }
        Column {
            Text("🌐 Smart Translator", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text("ML Kit · On-device · AI Context", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// IMAGE SOURCE ROW (Camera | Gallery)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ImageSourceRow(onCamera: () -> Unit, onGallery: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ImageSourceButton(
            icon    = "📷",
            label   = "Camera",
            color   = NeonCyan,
            modifier = Modifier.weight(1f),
            onClick = onCamera
        )
        ImageSourceButton(
            icon    = "🖼️",
            label   = "Gallery",
            color   = NeonPurple,
            modifier = Modifier.weight(1f),
            onClick = onGallery
        )
    }
}

@Composable
private fun ImageSourceButton(icon: String, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(0.10f))
            .border(1.dp, color.copy(0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 16.sp)
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// OCR STATUS BANNERS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OcrProcessingBanner() {
    val inf   = rememberInfiniteTransition(label = "ocr")
    val alpha by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonCyan.copy(0.10f))
            .border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
        Text("Extracting text from image...", style = MaterialTheme.typography.bodySmall, color = NeonCyan.copy(alpha = alpha))
    }
}

@Composable
private fun OcrSuccessBanner(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonGreen.copy(0.10f))
            .border(1.dp, NeonGreen.copy(0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onDismiss)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("✓", fontSize = 14.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
        Text("Text extracted from image — translating…", style = MaterialTheme.typography.bodySmall, color = NeonGreen, modifier = Modifier.weight(1f))
        Text("✕", fontSize = 11.sp, color = NeonGreen.copy(0.7f))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// LANGUAGE SELECTOR ROW
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LanguageSelectorRow(
    sourceLang:   Language,
    targetLang:   Language,
    isAutoDetect: Boolean,
    onSourceClick: () -> Unit,
    onTargetClick: () -> Unit,
    onSwap:        () -> Unit,
    onAutoDetect:  () -> Unit
) {
    val ext      = MaterialTheme.extended
    val swapAnim by animateFloatAsState(
        targetValue  = if (isAutoDetect) 0f else 180f,
        animationSpec = tween(400), label = "swap"
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        LangButton(
            lang      = if (isAutoDetect) Language("auto", "Auto-detect", "🌐", "Auto") else sourceLang,
            isActive  = true,
            modifier  = Modifier.weight(1f),
            onClick   = onSourceClick
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(NeonBlue.copy(0.2f), NeonPurple.copy(0.15f))))
                    .border(1.dp, NeonBlue.copy(0.4f), CircleShape)
                    .clickable(onClick = onSwap)
                    .graphicsLayer(rotationZ = swapAnim),
                contentAlignment = Alignment.Center
            ) { Text("⇄", color = NeonBlue, fontSize = 16.sp) }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .then(if (isAutoDetect) Modifier.background(NeonBlue.copy(0.15f)).border(1.dp, NeonBlue.copy(0.4f), RoundedCornerShape(6.dp)) else Modifier.background(ext.glassCard))
                    .clickable(onClick = onAutoDetect)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) { Text("Auto", style = MaterialTheme.typography.labelSmall, color = if (isAutoDetect) NeonBlue else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold) }
        }

        LangButton(
            lang    = targetLang,
            isActive = false,
            modifier = Modifier.weight(1f),
            onClick  = onTargetClick
        )
    }
}

@Composable
private fun LangButton(lang: Language, isActive: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ext = MaterialTheme.extended
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isActive)
                    Modifier.background(Brush.linearGradient(listOf(NeonBlue.copy(0.15f), NeonPurple.copy(0.10f))))
                        .border(1.dp, NeonBlue.copy(0.4f), RoundedCornerShape(14.dp))
                else
                    Modifier.background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp))
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(lang.flag, fontSize = 20.sp)
            Text(lang.displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(lang.nativeName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// INPUT CARD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TranslatorInputCard(
    text:         String,
    sourceLang:   Language,
    isAutoDetect: Boolean,
    onChange:     (String) -> Unit,
    onTranslate:  () -> Unit,
    onClear:      () -> Unit,
    onSpeak:      () -> Unit
) {
    val ext       = MaterialTheme.extended
    val charCount = text.length

    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ext.glassCard)
            .border(1.dp, NeonBlue.copy(0.3f), RoundedCornerShape(20.dp))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isAutoDetect) "🌐 Detect language" else "${sourceLang.flag} ${sourceLang.displayName}",
                    style = MaterialTheme.typography.labelSmall, color = NeonBlue
                )
                if (text.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(ext.glassCard).border(1.dp, ext.glassBorder, CircleShape).clickable(onClick = onSpeak), Alignment.Center) { Text("🔊", fontSize = 12.sp) }
                        Box(Modifier.size(26.dp).clip(CircleShape).background(NeonPink.copy(0.1f)).border(1.dp, NeonPink.copy(0.3f), CircleShape).clickable(onClick = onClear), Alignment.Center) { Text("✕", fontSize = 12.sp, color = NeonPink) }
                    }
                }
            }
            HorizontalDivider(color = NeonBlue.copy(0.15f))

            OutlinedTextField(
                value         = text,
                onValueChange = onChange,
                modifier      = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
                placeholder   = {
                    Text(
                        "Enter text to translate…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                    )
                },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = Color.Transparent,
                    unfocusedBorderColor  = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor           = NeonBlue
                ),
                textStyle     = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onTranslate() })
            )

            HorizontalDivider(color = NeonBlue.copy(0.15f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$charCount / 5000", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (text.isNotBlank()) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(NeonBlue, NeonPurple)))
                            .clickable(onClick = onTranslate)
                            .padding(horizontal = 18.dp, vertical = 7.dp)
                    ) { Text("Translate →", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TRANSLATING INDICATOR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TranslatingIndicator(label: String) {
    val inf   = rememberInfiniteTransition(label = "tl")
    val alpha by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonBlue, strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = NeonBlue.copy(alpha = alpha))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TRANSLATION RESULT CARD (enhanced — with source + target TTS)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TranslationResultCard(
    result:    TranslationResult,
    onSpeakSrc: () -> Unit,
    onSpeak:   () -> Unit,
    onCopy:    () -> Unit,
    onShare:   () -> Unit,
    onSave:    () -> Unit
) {
    val ext = MaterialTheme.extended
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ext.glassCard)
            .border(1.dp, NeonPurple.copy(0.4f), RoundedCornerShape(20.dp))
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${result.targetLang.flag} ${result.targetLang.displayName}", style = MaterialTheme.typography.labelSmall, color = NeonPurple)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("🔊", onClick = onSpeak)
                    ActionChip("📋", onClick = onCopy)
                    ActionChip("↑", onClick = onShare)
                    ActionChip("💾", onClick = onSave)
                }
            }
            HorizontalDivider(color = NeonPurple.copy(0.15f))

            // Translated text
            Text(
                text     = result.translatedText,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                style    = MaterialTheme.typography.bodyLarge,
                color    = MaterialTheme.colorScheme.onSurface,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium
            )

            // Pronunciation (romanization)
            result.pronunciation?.let { pron ->
                HorizontalDivider(color = NeonPurple.copy(0.10f))
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
                    Text("🗣", fontSize = 12.sp)
                    Text(pron, style = MaterialTheme.typography.bodySmall, color = NeonPurple.copy(0.8f), fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
                }
            }

            // Meaning in Source Language
            result.nativeMeaning?.let { meaning ->
                HorizontalDivider(color = NeonBlue.copy(0.10f))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 12.sp)
                        Text(
                            "Meaning in ${result.detectedLang.displayName}:",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        // Speak source text
                        Box(
                            Modifier.size(24.dp).clip(CircleShape)
                                .background(NeonBlue.copy(0.12f))
                                .border(1.dp, NeonBlue.copy(0.3f), CircleShape)
                                .clickable(onClick = onSpeakSrc),
                            Alignment.Center
                        ) { Text("🔊", fontSize = 11.sp) }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = meaning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                }
            }

            // Meaning in Target Language
            result.meaningInTarget?.let { meaning ->
                HorizontalDivider(color = NeonCyan.copy(0.10f))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🎯", fontSize = 12.sp)
                        Text(
                            "Meaning in ${result.targetLang.displayName}:",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = meaning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                }
            }

            // Meaning in English (if source and target are not English)
            val srcCode = result.detectedLang.code
            val tgtCode = result.targetLang.code
            if (srcCode != "en" && tgtCode != "en") {
                result.semanticMeaning?.meaningEnglish?.let { engMeaning ->
                    HorizontalDivider(color = Color(0xFF4CAF50).copy(0.10f))
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🇺🇸", fontSize = 12.sp)
                            Text(
                                "Meaning in English:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = engMeaning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Detected language badge
            HorizontalDivider(color = NeonPurple.copy(0.10f))
            Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), Arrangement.spacedBy(6.dp)) {
                NeonBadge("Detected: ${result.detectedLang.flag} ${result.detectedLang.displayName}", NeonBlue.copy(0.7f))
                NeonBadge("${result.characterCount} chars", NeonPurple.copy(0.7f))
            }
        }
    }
}

@Composable
private fun ActionChip(icon: String, onClick: () -> Unit) {
    Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.extended.glassCard).border(1.dp, MaterialTheme.extended.glassBorder, CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Text(icon, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// RECENT TRANSLATIONS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RecentTranslationsSection(recent: List<TranslationResult>, onTap: (TranslationResult) -> Unit) {
    val ext = MaterialTheme.extended
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("🕒 Recent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        recent.take(5).forEach { r ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ext.glassCard)
                    .border(1.dp, ext.glassBorder, RoundedCornerShape(12.dp))
                    .clickable { onTap(r) }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${r.detectedLang.flag}→${r.targetLang.flag}", fontSize = 16.sp)
                Column(Modifier.weight(1f)) {
                    Text(r.originalText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(r.translatedText, style = MaterialTheme.typography.bodySmall, color = NeonPurple, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NeonPink.copy(0.10f)).border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚠️", fontSize = 16.sp)
        Text(message, style = MaterialTheme.typography.bodySmall, color = NeonPink, modifier = Modifier.weight(1f))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// LANGUAGE PICKER SHEET
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun LanguagePickerSheet(
    isSource:  Boolean,
    current:   Language,
    onSelect:  (Language) -> Unit,
    onDismiss: () -> Unit
) {
    val ext   = MaterialTheme.extended
    var query by remember { mutableStateOf("") }
    val filtered = ALL_LANGUAGES.filter {
        query.isBlank() || it.displayName.contains(query, ignoreCase = true) || it.nativeName.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth().fillMaxHeight(0.7f)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(if (ext.isDark) Color(0xF7060D1F) else Color(0xF7F8FAFF))
            .border(1.dp, ext.glassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding()
    ) {
        Box(Modifier.padding(top = 10.dp).width(40.dp).height(4.dp).clip(CircleShape).background(NeonBlue.copy(0.4f)).align(Alignment.CenterHorizontally))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isSource) "Select Source Language" else "Select Target Language",
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold
            )
            Box(Modifier.clip(CircleShape).background(ext.glassCard).clickable(onClick = onDismiss).padding(6.dp)) {
                Text("✕", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            }
        }

        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder   = { Text("Search language...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)) },
            shape         = RoundedCornerShape(12.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor    = NeonBlue,
                unfocusedBorderColor  = ext.glassBorder,
                focusedContainerColor = ext.glassCard,
                unfocusedContainerColor = ext.glassCard,
                cursorColor           = NeonBlue
            ),
            singleLine = true
        )

        LazyColumn(
            modifier        = Modifier.fillMaxSize(),
            contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filtered, key = { it.code }) { lang ->
                val isSelected = lang.code == current.code
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isSelected) Modifier.background(NeonBlue.copy(0.15f)).border(1.dp, NeonBlue.copy(0.4f), RoundedCornerShape(10.dp))
                            else Modifier.background(ext.glassCard)
                        )
                        .clickable { onSelect(lang) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(lang.flag, fontSize = 20.sp)
                    Column(Modifier.weight(1f)) {
                        Text(lang.displayName, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) NeonBlue else MaterialTheme.colorScheme.onSurface, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        Text(lang.nativeName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isSelected) Text("✓", color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────
private fun shareText(context: Context, text: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text)
    }.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(android.content.Intent.createChooser(intent, "Share").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
