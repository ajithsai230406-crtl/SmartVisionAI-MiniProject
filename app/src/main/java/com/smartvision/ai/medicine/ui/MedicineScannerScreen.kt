package com.smartvision.ai.medicine.ui

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.medicine.domain.*
import com.smartvision.ai.medicine.presentation.MedicineScannerViewModel
import com.smartvision.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Module accent colors ───────────────────────────────────────────────────────
private val MedCyan   = Color(0xFF00E5FF)
private val MedBlue   = Color(0xFF2979FF)
private val MedRed    = Color(0xFFFF4D6D)
private val MedGreen  = Color(0xFF00E676)
private val MedOrange = Color(0xFFFFD166)
private val MedPurple = Color(0xFFD500F9)

// ══════════════════════════════════════════════════════════════════════════════
//  MEDICINE SCANNER SCREEN — MAIN ENTRY POINT
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun MedicineScannerScreen(
    onBack: () -> Unit,
    viewModel: MedicineScannerViewModel = hiltViewModel()
) {
    val state       by viewModel.state.collectAsStateWithLifecycle()
    val history     by viewModel.history.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val keyboard    = LocalSoftwareKeyboardController.current

    // Disclaimer dialog
    if (viewModel.showDisclaimer) {
        MedDisclaimerDialog(onAccept = { viewModel.dismissDisclaimer() })
    }

    // Camera overlay (full screen)
    if (viewModel.showCamera) {
        MedCameraScreen(
            onTextDetected = { text ->
                viewModel.analyzeFromOcr(text)
            },
            onClose = { viewModel.closeCamera() }
        )
        return
    }

    // History sheet
    if (viewModel.showHistory) {
        MedHistoryScreen(
            history  = history,
            onBack   = { viewModel.closeHistory() },
            onSelect = { viewModel.reanalyze(it) },
            onDelete = { viewModel.deleteRecord(it.id) },
            onFavorite = { viewModel.toggleFavorite(it) }
        )
        return
    }

    GradientBackground {
        Column(Modifier.fillMaxSize()) {

            // ── Top Bar ───────────────────────────────────────────────────────
            MedTopBar(
                onBack       = onBack,
                showResult   = state is MedicineScanState.Result,
                onHistory    = { viewModel.openHistory() },
                onReset      = { viewModel.reset() }
            )

            // ── Content switch ────────────────────────────────────────────────
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                label = "medicineContent"
            ) { currentState ->
                when (currentState) {
                    is MedicineScanState.Idle -> MedIdleScreen(
                        query       = viewModel.searchQuery,
                        suggestions = suggestions,
                        history     = history.take(5),
                        onQuery     = { viewModel.onQueryChange(it) },
                        onAnalyze   = { viewModel.analyzeByName(); keyboard?.hide() },
                        onCamera    = { viewModel.openCamera() },
                        onDemo      = { viewModel.quickDemoScan() },
                        onSuggestion = { viewModel.analyzeByName(it) },
                        onHistory   = { viewModel.openHistory() }
                    )
                    is MedicineScanState.OcrRunning, is MedicineScanState.AiAnalyzing -> {
                        val progress = when (currentState) {
                            is MedicineScanState.OcrRunning   -> currentState.progress
                            is MedicineScanState.AiAnalyzing  -> currentState.progress
                            else -> 0.5f
                        }
                        val label = if (currentState is MedicineScanState.OcrRunning)
                            "📷 Reading medicine label..." else "🤖 AI analyzing medicine..."
                        MedAnalyzingScreen(progress = progress, label = label)
                    }
                    is MedicineScanState.Result -> MedResultScreen(
                        info       = currentState.info,
                        selectedTab = viewModel.selectedTab,
                        isFavorite = viewModel.isFavorite,
                        isSpeaking = viewModel.isSpeaking,
                        onTab      = { viewModel.selectTab(it) },
                        onVoice    = { viewModel.toggleVoice(currentState.info) },
                        onFavorite = { viewModel.toggleCurrentFavorite() }
                    )
                    is MedicineScanState.Error -> MedErrorScreen(
                        message = currentState.message,
                        onRetry = { viewModel.reset() }
                    )
                    else -> Unit
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  TOP BAR
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun MedTopBar(
    onBack: () -> Unit, showResult: Boolean,
    onHistory: () -> Unit, onReset: () -> Unit
) {
    val ext = MaterialTheme.extended
    val inf = rememberInfiniteTransition(label = "medBar")
    val glow by inf.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "g")

    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Back button
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(11.dp)).clickable(onClick = onBack),
            Alignment.Center) {
            Text("←", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
        }

        // Title with animated glow
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💊", fontSize = 18.sp)
                Text(
                    "Medicine Scanner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                "AI-powered drug information",
                style = MaterialTheme.typography.labelSmall,
                color = MedCyan.copy(glow)
            )
        }

        // History button
        Box(Modifier.clip(RoundedCornerShape(9.dp)).background(MedBlue.copy(0.12f))
            .border(1.dp, MedBlue.copy(0.3f), RoundedCornerShape(9.dp))
            .clickable(onClick = onHistory).padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text("🕐", fontSize = 16.sp)
        }

        // Reset / New scan
        if (showResult) {
            Box(Modifier.clip(RoundedCornerShape(9.dp)).background(MedRed.copy(0.1f))
                .border(1.dp, MedRed.copy(0.3f), RoundedCornerShape(9.dp))
                .clickable(onClick = onReset).padding(horizontal = 10.dp, vertical = 7.dp)) {
                Text("New", style = MaterialTheme.typography.labelSmall, color = MedRed, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  IDLE SCREEN — search + scan hero + quick picks
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun MedIdleScreen(
    query: String, suggestions: List<String>, history: List<MedicineScanRecord>,
    onQuery: (String) -> Unit, onAnalyze: () -> Unit, onCamera: () -> Unit,
    onDemo: () -> Unit, onSuggestion: (String) -> Unit, onHistory: () -> Unit
) {
    val ext = MaterialTheme.extended
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero scan area
        item { MedScanHero(onCamera = onCamera, onDemo = onDemo) }

        // Search field
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔍 Search Medicine", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = query, onValueChange = onQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paracetamol, Ibuprofen, Amoxicillin...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                        style = MaterialTheme.typography.bodyMedium) },
                    shape         = RoundedCornerShape(16.dp),
                    singleLine    = true,
                    leadingIcon   = { Text("💊", fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp)) },
                    trailingIcon  = if (query.isNotEmpty()) {{ Box(Modifier.clickable { onQuery("") }.padding(end = 4.dp)) { Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant) } }} else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MedCyan,
                        unfocusedBorderColor    = ext.glassBorder,
                        focusedContainerColor   = ext.glassCard,
                        unfocusedContainerColor = ext.glassCard,
                        cursorColor             = MedCyan,
                        focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor      = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onAnalyze() })
                )
            }
        }

        // Suggestions dropdown
        if (suggestions.isNotEmpty() && query.isNotEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(12.dp))) {
                    suggestions.take(5).forEach { sug ->
                        Row(Modifier.fillMaxWidth().clickable { onSuggestion(sug) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("💊", fontSize = 14.sp)
                            Text(sug, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text("→", color = MedCyan)
                        }
                        HorizontalDivider(color = ext.glassBorder)
                    }
                }
            }
        }

        // Analyze button
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(MedBlue, MedCyan.copy(0.8f))))
                .clickable(onClick = onAnalyze).padding(vertical = 17.dp), Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💊", fontSize = 20.sp)
                    Text("Analyze Medicine", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick picks row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚡ Quick Search", style = MaterialTheme.typography.labelMedium, color = MedCyan, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val quick = listOf("Paracetamol" to "💊", "Ibuprofen" to "💊", "Amoxicillin" to "💊",
                        "Cetirizine" to "💊", "Omeprazole" to "💊", "Metformin" to "💊",
                        "Pantoprazole" to "💊", "Azithromycin" to "💊")
                    items(quick) { (name, emoji) ->
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ext.glassCard)
                            .border(1.dp, MedBlue.copy(0.25f), RoundedCornerShape(20.dp))
                            .clickable { onSuggestion(name) }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 14.sp)
                                Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // Medicine types grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🏥 Medicine Types", style = MaterialTheme.typography.labelMedium, color = MedCyan, fontWeight = FontWeight.SemiBold)
                val types = MedicineType.values().toList()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                            row.forEach { type ->
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(ext.glassCard)
                                    .border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp))
                                    .clickable { onSuggestion(type.label) }.padding(vertical = 8.dp), Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(type.emoji, fontSize = 18.sp)
                                        Text(type.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                            repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // Recent history preview
        if (history.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("🕐 Recent Scans", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text("See All →", style = MaterialTheme.typography.labelSmall, color = MedCyan, modifier = Modifier.clickable(onClick = onHistory))
                }
            }
            items(history) { record ->
                MedHistoryRowItem(record = record, onTap = { onSuggestion(record.info.name) })
            }
        }

        item { Spacer(Modifier.height(24.dp).navigationBarsPadding()) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  HERO SCAN AREA — animated camera scan with neon brackets
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun MedScanHero(onCamera: () -> Unit, onDemo: () -> Unit) {
    val inf   = rememberInfiniteTransition(label = "medHero")
    val scanY by inf.animateFloat(0.05f, 0.95f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "scan")
    val glow  by inf.animateFloat(0.3f, 0.85f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "glow")
    val pulse by inf.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "pulse")
    // Orbiting particles
    val orbit by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "orbit")

    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0A1628), Color(0xFF0D1F3C))))
            .border(1.5.dp, MedCyan.copy(glow * 0.5f), RoundedCornerShape(24.dp))
            .clickable(onClick = onCamera),
        contentAlignment = Alignment.Center
    ) {
        // Scan line (horizontal sweep)
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val y = size.height * scanY
            drawLine(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, MedCyan.copy(0.8f), Color.Transparent)),
                start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 2f
            )
            // Scan glow under line
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, MedCyan.copy(0.05f), Color.Transparent),
                startY = y - 20f, endY = y + 20f))

            // Corner brackets
            val c = 28f; val s = 30.dp.toPx(); val col = MedCyan.copy(glow)
            val stroke = Stroke(3f)
            // TL
            drawLine(col, Offset(c, c), Offset(c + s, c), 3f)
            drawLine(col, Offset(c, c), Offset(c, c + s), 3f)
            // TR
            drawLine(col, Offset(size.width - c, c), Offset(size.width - c - s, c), 3f)
            drawLine(col, Offset(size.width - c, c), Offset(size.width - c, c + s), 3f)
            // BL
            drawLine(col, Offset(c, size.height - c), Offset(c + s, size.height - c), 3f)
            drawLine(col, Offset(c, size.height - c), Offset(c, size.height - c - s), 3f)
            // BR
            drawLine(col, Offset(size.width - c, size.height - c), Offset(size.width - c - s, size.height - c), 3f)
            drawLine(col, Offset(size.width - c, size.height - c), Offset(size.width - c, size.height - c - s), 3f)

            // Orbiting particles around center
            val cx = size.width / 2; val cy = size.height / 2; val r = 52.dp.toPx()
            listOf(0f, PI.toFloat(), (PI / 2).toFloat(), (3 * PI / 2).toFloat()).forEachIndexed { i, offset ->
                val a = orbit + offset
                drawCircle(MedCyan.copy(glow * 0.7f), 4f, Offset(cx + r * cos(a), cy + r * sin(a)))
            }
        }

        // Center icon
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.scale(pulse)) {
            Text("💊", fontSize = 44.sp)
            Text("📷 Tap to scan medicine label", style = MaterialTheme.typography.bodyMedium,
                color = MedCyan, fontWeight = FontWeight.SemiBold)
            Text("or type medicine name below", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
        }

        // Demo button (bottom-right)
        Box(Modifier.align(Alignment.BottomEnd).padding(12.dp)
            .clip(RoundedCornerShape(8.dp)).background(MedBlue.copy(0.2f))
            .border(1.dp, MedBlue.copy(0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onDemo).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("Demo", style = MaterialTheme.typography.labelSmall, color = MedBlue, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ANALYZING SCREEN — animated progress orb
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun MedAnalyzingScreen(progress: Float, label: String) {
    val inf   = rememberInfiniteTransition(label = "medAnalyze")
    val rot   by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "rot")
    val alpha by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")

    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)) {
            // Animated orb
            Box(Modifier.size(120.dp), Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize().rotate(rot)) {
                    val cx = size.width / 2; val cy = size.height / 2; val r = size.width / 2 - 8
                    val sweep = Brush.sweepGradient(listOf(MedCyan.copy(0.9f), MedBlue.copy(0.5f), Color.Transparent))
                    drawArc(brush = sweep, startAngle = 0f, sweepAngle = 270f, useCenter = false, style = Stroke(6f))
                }
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(100.dp),
                    color = MedCyan, trackColor = MaterialTheme.extended.glassBorder, strokeWidth = 4.dp)
                Text("💊", fontSize = 36.sp)
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MedCyan.copy(alpha = alpha), textAlign = TextAlign.Center)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            // Progress steps
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("📷 Capturing image" to (progress > 0.1f),
                    "📝 OCR text extraction" to (progress > 0.35f),
                    "🤖 AI medicine analysis" to (progress > 0.6f),
                    "✅ Preparing results" to (progress > 0.85f)
                ).forEach { (step, done) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (done) "✅" else "⏳", fontSize = 12.sp)
                        Text(step, style = MaterialTheme.typography.labelSmall,
                            color = if (done) MedGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  RESULT SCREEN — 7-tab full medicine info
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun MedResultScreen(
    info: MedicineInfo, selectedTab: Int, isFavorite: Boolean, isSpeaking: Boolean,
    onTab: (Int) -> Unit, onVoice: () -> Unit, onFavorite: () -> Unit
) {
    val ext = MaterialTheme.extended

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Emergency warning banner
        if (info.emergencyWarning != null) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MedRed.copy(0.12f)).border(2.dp, MedRed, RoundedCornerShape(14.dp))
                    .padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🚨", fontSize = 20.sp)
                    Text(info.emergencyWarning, style = MaterialTheme.typography.bodySmall,
                        color = MedRed, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Header card
        item {
            MedHeaderCard(info = info, isFavorite = isFavorite, isSpeaking = isSpeaking,
                onVoice = onVoice, onFavorite = onFavorite)
        }

        // Safety card
        item { MedSafetyCard(info = info) }

        // Dosage info card
        item { MedDosageCard(info = info) }

        // Tabs
        item {
            MedTabRow(selectedTab = selectedTab, onTab = onTab)
        }

        // Tab content
        item {
            AnimatedContent(targetState = selectedTab, transitionSpec = {
                slideInHorizontally { if (targetState > initialState) it else -it } togetherWith
                slideOutHorizontally { if (targetState > initialState) -it else it }
            }, label = "medTab") { tab ->
                when (tab) {
                    0 -> MedInfoTab(info)
                    1 -> MedSideEffectsTab(info)
                    2 -> MedInteractionsTab(info)
                    3 -> MedWarningsTab(info)
                    4 -> MedStorageTab(info)
                    5 -> MedAiSummaryTab(info)
                    else -> Unit
                }
            }
        }

        // Disclaimer
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(MedOrange.copy(0.08f)).border(1.dp, MedOrange.copy(0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚕️", fontSize = 14.sp)
                    Text(DISCLAIMER, style = MaterialTheme.typography.labelSmall,
                        color = MedOrange.copy(0.9f), lineHeight = 18.sp)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp).navigationBarsPadding()) }
    }
}

// ── Header card ───────────────────────────────────────────────────────────────
@Composable
private fun MedHeaderCard(info: MedicineInfo, isFavorite: Boolean, isSpeaking: Boolean,
                           onVoice: () -> Unit, onFavorite: () -> Unit) {
    val ext = MaterialTheme.extended
    val safetyColor = Color(info.safetyLevel.colorHex)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .background(Brush.linearGradient(listOf(safetyColor.copy(0.12f), safetyColor.copy(0.04f))))
        .border(2.dp, safetyColor.copy(0.4f), RoundedCornerShape(20.dp)).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(info.name, style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
                    if (info.genericName.isNotBlank())
                        Text(info.genericName, style = MaterialTheme.typography.bodySmall, color = safetyColor)
                    if (info.brandName != info.name && info.brandName.isNotBlank()) {
                        Text("Also sold as: ${info.brandName}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MedSafetyScoreBadge(info.safetyScore, safetyColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Voice button
                        Box(Modifier.size(34.dp).clip(CircleShape).background(if (isSpeaking) MedCyan.copy(0.2f) else ext.glassCard)
                            .border(1.dp, if (isSpeaking) MedCyan else ext.glassBorder, CircleShape).clickable(onClick = onVoice), Alignment.Center) {
                            Text(if (isSpeaking) "🔇" else "🔊", fontSize = 14.sp)
                        }
                        // Favorite button
                        Box(Modifier.size(34.dp).clip(CircleShape).background(if (isFavorite) MedOrange.copy(0.2f) else ext.glassCard)
                            .border(1.dp, if (isFavorite) MedOrange else ext.glassBorder, CircleShape).clickable(onClick = onFavorite), Alignment.Center) {
                            Text(if (isFavorite) "⭐" else "☆", fontSize = 14.sp)
                        }
                    }
                }
            }
            // Badges
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { NeonBadge(info.medicineType.emoji + " " + info.medicineType.label, MedBlue) }
                item { NeonBadge(info.category, MedCyan) }
                if (info.prescriptionOnly) item { NeonBadge("Rx Only", MedRed) }
                item { NeonBadge(info.safetyLevel.label, safetyColor) }
            }
            if (info.composition.isNotBlank()) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ext.glassCard)
                    .border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp)).padding(10.dp)) {
                    Column {
                        Text("Composition", style = MaterialTheme.typography.labelSmall, color = MedCyan)
                        Text(info.composition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// ── Safety card ───────────────────────────────────────────────────────────────
@Composable
private fun MedSafetyCard(info: MedicineInfo) {
    val ext = MaterialTheme.extended
    val safetyColor = Color(info.safetyLevel.colorHex)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard)
        .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("🛡️ Safety Level", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(info.safetyLevel.label, style = MaterialTheme.typography.labelMedium,
                    color = safetyColor, fontWeight = FontWeight.Bold)
            }
            // Safety bar
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.extended.glassBorder)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(info.safetyScore / 10f).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(safetyColor.copy(0.7f), safetyColor))))
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Safety Score: ${info.safetyScore}/10", style = MaterialTheme.typography.labelSmall,
                    color = safetyColor, fontWeight = FontWeight.SemiBold)
                Text(if (info.prescriptionOnly) "🔒 Prescription Required" else "✅ Over the Counter",
                    style = MaterialTheme.typography.labelSmall, color = if (info.prescriptionOnly) MedRed else MedGreen)
            }
        }
    }
}

// ── Dosage card ───────────────────────────────────────────────────────────────
@Composable
private fun MedDosageCard(info: MedicineInfo) {
    val ext = MaterialTheme.extended
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Brush.linearGradient(listOf(MedBlue.copy(0.08f), MedCyan.copy(0.04f))))
        .border(1.dp, MedBlue.copy(0.3f), RoundedCornerShape(16.dp)).padding(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("💉 Dosage Guide", style = MaterialTheme.typography.labelMedium,
                color = MedCyan, fontWeight = FontWeight.SemiBold)
            val items = listOf(
                "💊 Dose" to info.dosage,
                "🕐 Frequency" to info.frequency,
                "👥 Age Group" to info.ageGroup,
                "🍽️ With Food" to info.beforeOrAfterFood,
                "📋 How to Use" to info.howToUse
            )
            items.forEach { (label, value) ->
                if (value.isNotBlank()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
                        Text(value, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(color = MaterialTheme.extended.glassBorder, thickness = 0.5.dp)
                }
            }
        }
    }
}

// ── Tab row ───────────────────────────────────────────────────────────────────
@Composable
private fun MedTabRow(selectedTab: Int, onTab: (Int) -> Unit) {
    val tabs = listOf("📋 Uses" to MedBlue, "⚠️ Side Effects" to MedOrange,
        "🔄 Interactions" to MedPurple, "⚡ Warnings" to MedRed,
        "🌡️ Storage" to MedCyan, "🤖 AI Summary" to MedGreen)

    ScrollableTabRow(
        selectedTabIndex = selectedTab, edgePadding = 0.dp,
        containerColor = Color.Transparent, contentColor = Color.Transparent,
        indicator = {}, divider = {}
    ) {
        tabs.forEachIndexed { idx, (label, color) ->
            val sel = idx == selectedTab
            Box(Modifier.clip(RoundedCornerShape(10.dp))
                .then(if (sel) Modifier.background(color.copy(0.15f)).border(1.5.dp, color, RoundedCornerShape(10.dp)) else Modifier.background(MaterialTheme.extended.glassCard).border(1.dp, MaterialTheme.extended.glassBorder, RoundedCornerShape(10.dp)))
                .clickable { onTab(idx) }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = if (sel) color else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
            Spacer(Modifier.width(6.dp))
        }
    }
}

// ── Tab content composables ───────────────────────────────────────────────────
@Composable
private fun MedInfoTab(info: MedicineInfo) {
    val ext = MaterialTheme.extended
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MedListCard("✅ Uses & Indications", info.uses, MedGreen)
        if (info.symptomsItTreats.isNotEmpty()) MedListCard("🩺 Symptoms It Treats", info.symptomsItTreats, MedBlue)
        if (info.whenToUse.isNotBlank()) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp)).padding(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("⏰ When to Use", style = MaterialTheme.typography.labelMedium, color = MedCyan, fontWeight = FontWeight.SemiBold)
                    Text(info.whenToUse, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        if (info.whoShouldAvoid.isNotEmpty()) MedListCard("🚫 Who Should Avoid", info.whoShouldAvoid, MedRed)
    }
}

@Composable
private fun MedSideEffectsTab(info: MedicineInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MedListCard("⚠️ Side Effects", info.sideEffects, MedOrange)
        if (info.allergyWarnings.isNotEmpty()) MedListCard("🤧 Allergy Warnings", info.allergyWarnings, MedRed)
    }
}

@Composable
private fun MedInteractionsTab(info: MedicineInfo) {
    val ext = MaterialTheme.extended
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (info.interactions.isEmpty()) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp)).padding(16.dp), Alignment.Center) {
                Text("No drug interactions listed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🔄 Drug Interactions", style = MaterialTheme.typography.labelMedium, color = MedPurple, fontWeight = FontWeight.SemiBold)
                info.interactions.forEach { interaction ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MedPurple).align(Alignment.CenterVertically))
                        Text(interaction, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        // Generic interaction note
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MedOrange.copy(0.08f)).border(1.dp, MedOrange.copy(0.3f), RoundedCornerShape(12.dp)).padding(12.dp)) {
            Text("💡 Always inform your doctor or pharmacist about ALL medicines, supplements, and herbal products you are taking.", style = MaterialTheme.typography.labelSmall, color = MedOrange)
        }
    }
}

@Composable
private fun MedWarningsTab(info: MedicineInfo) {
    val ext = MaterialTheme.extended
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (info.warnings.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MedRed.copy(0.08f)).border(2.dp, MedRed.copy(0.4f), RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚡ Warnings & Precautions", style = MaterialTheme.typography.labelMedium, color = MedRed, fontWeight = FontWeight.Bold)
                info.warnings.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MedRed.copy(0.9f)) }
            }
        }
        if (info.emergencyWarning != null) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MedRed.copy(0.15f)).border(2.dp, MedRed, RoundedCornerShape(14.dp)).padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🚨", fontSize = 20.sp)
                    Column {
                        Text("Emergency Warning", style = MaterialTheme.typography.labelMedium, color = MedRed, fontWeight = FontWeight.Bold)
                        Text(info.emergencyWarning, style = MaterialTheme.typography.bodySmall, color = MedRed)
                        Spacer(Modifier.height(4.dp))
                        Text("Emergency: 📞 112", style = MaterialTheme.typography.labelSmall, color = MedRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MedStorageTab(info: MedicineInfo) {
    val ext = MaterialTheme.extended
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp)).padding(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🌡️", fontSize = 24.sp)
                    Column {
                        Text("Storage Conditions", style = MaterialTheme.typography.labelMedium, color = MedCyan, fontWeight = FontWeight.SemiBold)
                        Text(info.storageInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                HorizontalDivider(color = ext.glassBorder)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 24.sp)
                    Column {
                        Text("Expiry Information", style = MaterialTheme.typography.labelMedium, color = MedOrange, fontWeight = FontWeight.SemiBold)
                        Text(info.expiryNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        // Storage tips
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("💡 General Storage Tips", style = MaterialTheme.typography.labelMedium, color = MedCyan, fontWeight = FontWeight.SemiBold)
            listOf("Keep away from direct sunlight", "Store away from children's reach", "Do not store in bathroom (humidity)", "Keep in original packaging", "Do not freeze unless instructed").forEach {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", color = MedCyan, fontWeight = FontWeight.Bold)
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun MedAiSummaryTab(info: MedicineInfo) {
    val ext = MaterialTheme.extended
    val inf = rememberInfiniteTransition(label = "aiTab")
    val glow by inf.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "g")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(MedGreen.copy(0.08f), MedCyan.copy(0.05f))))
            .border(1.5.dp, MedGreen.copy(glow * 0.5f), RoundedCornerShape(16.dp)).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🤖", fontSize = 20.sp)
                    Text("AI Summary", style = MaterialTheme.typography.titleSmall, color = MedGreen, fontWeight = FontWeight.Bold)
                    NeonBadge("Gemini AI", MedGreen)
                }
                Text(
                    info.aiSummary.ifBlank { "${info.name} is a ${info.category} used for ${info.uses.take(2).joinToString(" and ")}. Dosage: ${info.dosage}. Safety Level: ${info.safetyLevel.label}." },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp
                )
            }
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(12.dp)).padding(12.dp)) {
            Text("⚕️ $DISCLAIMER", style = MaterialTheme.typography.labelSmall, color = MedOrange, lineHeight = 18.sp)
        }
    }
}

// ── Shared list card ───────────────────────────────────────────────────────────
@Composable
private fun MedListCard(title: String, items: List<String>, color: Color) {
    val ext = MaterialTheme.extended
    if (items.isEmpty()) return
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color).align(Alignment.CenterVertically))
                Text(item, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ── Safety score badge ─────────────────────────────────────────────────────────
@Composable
private fun MedSafetyScoreBadge(score: Int, color: Color) {
    Box(Modifier.size(52.dp).clip(CircleShape).background(color.copy(0.12f)).border(2.dp, color.copy(0.5f), CircleShape), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.ExtraBold)
            Text("/10", style = MaterialTheme.typography.labelSmall, color = color.copy(0.7f))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  CAMERA SCREEN — Live OCR scanning
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun MedCameraScreen(onTextDetected: (String) -> Unit, onClose: () -> Unit) {
    val ctx         = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isProcessing by remember { mutableStateOf(false) }

    val inf  = rememberInfiniteTransition(label = "camScan")
    val scanY by inf.animateFloat(0.05f, 0.95f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "s")
    val glow  by inf.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "g")

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // CameraX preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val previewView = PreviewView(context)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

                    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                    imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        if (!isProcessing) {
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                textRecognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        val text = visionText.text.trim()
                                        if (text.length > 5) {
                                            isProcessing = true
                                            onTextDetected(text)
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else { imageProxy.close() }
                        } else { imageProxy.close() }
                    }

                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
                    }
                }, ContextCompat.getMainExecutor(context))
                previewView
            }
        )

        // Scan overlay
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val y = size.height * scanY
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent, MedCyan.copy(0.9f), Color.Transparent)), Offset(0f, y), Offset(size.width, y), 3f)
            // Corner brackets
            val c = 60f; val s = 60f; val col = MedCyan.copy(glow)
            drawLine(col, Offset(c, c), Offset(c + s, c), 4f)
            drawLine(col, Offset(c, c), Offset(c, c + s), 4f)
            drawLine(col, Offset(size.width - c, c), Offset(size.width - c - s, c), 4f)
            drawLine(col, Offset(size.width - c, c), Offset(size.width - c, c + s), 4f)
            drawLine(col, Offset(c, size.height - c), Offset(c + s, size.height - c), 4f)
            drawLine(col, Offset(c, size.height - c), Offset(c, size.height - c - s), 4f)
            drawLine(col, Offset(size.width - c, size.height - c), Offset(size.width - c - s, size.height - c), 4f)
            drawLine(col, Offset(size.width - c, size.height - c), Offset(size.width - c, size.height - c - s), 4f)
        }

        // Top instruction
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(0.6f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Point camera at medicine package label", style = MaterialTheme.typography.bodyMedium,
                    color = MedCyan, textAlign = TextAlign.Center)
            }
        }

        // Bottom: processing / close
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp)) {
            if (isProcessing) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MedCyan, strokeWidth = 2.dp)
                    Text("Reading label...", color = MedCyan, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(0.7f))
                    .border(1.dp, MedRed.copy(0.5f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onClose).padding(horizontal = 28.dp, vertical = 14.dp)) {
                    Text("✕ Close Camera", color = MedRed, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  HISTORY SCREEN
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun MedHistoryScreen(
    history: List<MedicineScanRecord>, onBack: () -> Unit,
    onSelect: (MedicineScanRecord) -> Unit, onDelete: (MedicineScanRecord) -> Unit,
    onFavorite: (MedicineScanRecord) -> Unit
) {
    val ext = MaterialTheme.extended
    GradientBackground {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp)).clickable(onClick = onBack), Alignment.Center) {
                    Text("←", color = MaterialTheme.colorScheme.onSurface)
                }
                Column(Modifier.weight(1f)) {
                    Text("🕐 Scan History", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("${history.size} medicines scanned", style = MaterialTheme.typography.labelSmall, color = MedCyan)
                }
            }
            if (history.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("💊", fontSize = 56.sp)
                        Text("No scan history yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Scan a medicine to see it here", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(history) { record ->
                        MedHistoryRowItem(record = record, onTap = { onSelect(record) },
                            onFavorite = { onFavorite(record) }, onDelete = { onDelete(record) })
                    }
                    item { Spacer(Modifier.height(24.dp).navigationBarsPadding()) }
                }
            }
        }
    }
}

@Composable
private fun MedHistoryRowItem(
    record: MedicineScanRecord,
    onTap: () -> Unit,
    onFavorite: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val ext = MaterialTheme.extended
    val safetyColor = Color(record.info.safetyLevel.colorHex)
    val fmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ext.glassCard)
        .border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp)).clickable(onClick = onTap)
        .padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(safetyColor.copy(0.12f)).border(1.dp, safetyColor.copy(0.3f), RoundedCornerShape(12.dp)), Alignment.Center) {
            Text(record.info.medicineType.emoji, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(record.info.name, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(record.info.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(fmt.format(record.scannedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
            NeonBadge("${record.info.safetyScore}/10", safetyColor)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onFavorite != null) Box(Modifier.size(28.dp).clip(CircleShape).background(ext.glassCard).clickable(onClick = onFavorite), Alignment.Center) { Text(if (record.isFavorite) "⭐" else "☆", fontSize = 12.sp) }
                if (onDelete != null) Box(Modifier.size(28.dp).clip(CircleShape).background(ext.glassCard).clickable(onClick = onDelete), Alignment.Center) { Text("🗑️", fontSize = 12.sp) }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  DISCLAIMER DIALOG
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun MedDisclaimerDialog(onAccept: () -> Unit) {
    val ext = MaterialTheme.extended
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.7f)), Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0A1628), Color(0xFF0D1F3C))))
                .border(2.dp, MedCyan.copy(0.4f), RoundedCornerShape(24.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("⚕️", fontSize = 48.sp)
            Text("Medical Information Disclaimer", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MedOrange.copy(0.1f))
                .border(1.dp, MedOrange.copy(0.4f), RoundedCornerShape(14.dp)).padding(14.dp)) {
                Text(DISCLAIMER, style = MaterialTheme.typography.bodySmall, color = MedOrange, lineHeight = 20.sp, textAlign = TextAlign.Center)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("✅ For educational purposes only", "✅ Always consult a doctor", "✅ Do not self-medicate", "✅ Emergency: Call 112").forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(MedBlue, MedCyan.copy(0.8f))))
                .clickable(onClick = onAccept).padding(vertical = 16.dp), Alignment.Center) {
                Text("✓ I Understand — Continue", style = MaterialTheme.typography.titleSmall,
                    color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ERROR SCREEN
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun MedErrorScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Text("💊", fontSize = 56.sp)
            Text("Analysis Failed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MedRed, textAlign = TextAlign.Center)
            Box(Modifier.clip(RoundedCornerShape(14.dp)).background(MedBlue.copy(0.15f)).border(1.dp, MedBlue.copy(0.4f), RoundedCornerShape(14.dp)).clickable(onClick = onRetry).padding(horizontal = 32.dp, vertical = 14.dp)) {
                Text("Try Again", style = MaterialTheme.typography.titleSmall, color = MedBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}
