package com.smartvision.ai.waste.ui

import android.Manifest
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.waste.domain.*
import com.smartvision.ai.waste.presentation.WasteClassifierViewModel
import com.smartvision.ai.ui.theme.*
import java.util.concurrent.Executors

// ══════════════════════════════════════════════════════════════════════════════
// WASTE CLASSIFIER — MAIN SCREEN
// ══════════════════════════════════════════════════════════════════════════════

private val EcoGreen   = Color(0xFF00FF88)
private val EcoGreenDim = Color(0xFF00CC66)
private val EcoYellow  = Color(0xFFFFD700)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WasteClassifierScreen(
    onBack: () -> Unit,
    viewModel: WasteClassifierViewModel = hiltViewModel()
) {
    val camPerm      = rememberPermissionState(Manifest.permission.CAMERA)
    val scanState    by viewModel.scanState.collectAsStateWithLifecycle()
    val flashEnabled by viewModel.flashEnabled.collectAsStateWithLifecycle()
    val history      by viewModel.sessionHistory.collectAsStateWithLifecycle()
    val cameraRef    = remember { mutableStateOf<Camera?>(null) }
    var capturedBmp  by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val ctx         = LocalContext.current
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    
    val galleryLauncher = rememberGalleryPickerLauncher { uri ->
        viewModel.analyzeFromUri(ctx, uri)
    }

    LaunchedEffect(Unit) { if (!camPerm.status.isGranted) camPerm.launchPermissionRequest() }
    LaunchedEffect(flashEnabled) { cameraRef.value?.cameraControl?.enableTorch(flashEnabled) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera preview ────────────────────────────────────────────────────
        if (camPerm.status.isGranted && scanState is WasteScanState.Idle) {
            WasteCameraPreview(
                modifier      = Modifier.fillMaxSize(),
                onPreviewViewCreated = { previewViewRef = it },
                onCameraReady = { cameraRef.value = it }
            )
        }

        // ── Eco scanner grid ──────────────────────────────────────────────────
        if (scanState is WasteScanState.Idle) EcoScannerOverlay(Modifier.fillMaxSize())

        // ── Top HUD ────────────────────────────────────────────────────────
        WasteTopHud(
            flashEnabled  = flashEnabled,
            onBack        = onBack,
            onFlash       = { viewModel.toggleFlash() },
            onGallery     = { galleryLauncher() }
        )

        // ── Scanning indicator ────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = scanState is WasteScanState.Scanning,
            enter    = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) { WasteScanningIndicator() }

        // ── Scan viewfinder + capture button ─────────────────────────────────
        AnimatedVisibility(
            visible  = scanState is WasteScanState.Idle && camPerm.status.isGranted,
            enter    = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) { WasteViewfinder() }

        // ── Capture button ────────────────────────────────────────────────────
        if (scanState is WasteScanState.Idle && camPerm.status.isGranted) {
            WasteCaptureButton(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp),
                onCapture = {
                    // Capture the actual current live frame from the camera preview view
                    val bitmap = previewViewRef?.bitmap ?: android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
                    viewModel.captureAndClassify(bitmap)
                }
            )
        }

        // ── Result sheet ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = scanState is WasteScanState.Classified,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            (scanState as? WasteScanState.Classified)?.let { state ->
                WasteResultSheet(
                    result    = state.result,
                    onRescan  = { viewModel.resetScan() },
                    onSpeak   = { viewModel.speakResult(state.result) }
                )
            }
        }

        // ── Error ─────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = scanState is WasteScanState.Error,
            enter    = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) {
            val msg = (scanState as? WasteScanState.Error)?.message ?: ""
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(NeonPink.copy(0.12f)).border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(14.dp))
                    .padding(14.dp).clickable { viewModel.dismissError() }
            ) {
                Text("⚠️ $msg", style = MaterialTheme.typography.bodySmall, color = NeonPink)
            }
        }

        // ── Mini history strip ────────────────────────────────────────────────
        if (history.isNotEmpty() && scanState is WasteScanState.Idle) {
            WasteHistoryStrip(history = history, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CAMERA PREVIEW
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WasteCameraPreview(
    modifier: Modifier,
    onPreviewViewCreated: (PreviewView) -> Unit,
    onCameraReady: (Camera) -> Unit
) {
    val ctx            = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(modifier = modifier, factory = { c ->
        val pv = PreviewView(c).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType    = PreviewView.ScaleType.FILL_CENTER
        }
        onPreviewViewCreated(pv)
        ProcessCameraProvider.getInstance(c).addListener({
            val prov    = ProcessCameraProvider.getInstance(c).get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
            val cam     = prov.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            onCameraReady(cam)
        }, ContextCompat.getMainExecutor(c))
        pv
    })
}

// ══════════════════════════════════════════════════════════════════════════════
// ECO SCANNER GRID OVERLAY
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EcoScannerOverlay(modifier: Modifier) {
    val inf   = rememberInfiniteTransition(label = "eco")
    val alpha by inf.animateFloat(0.03f, 0.10f, infiniteRepeatable(tween(2500), RepeatMode.Reverse), label = "a")
    val scanY by inf.animateFloat(-0.05f, 1.05f, infiniteRepeatable(tween(3500, easing = LinearEasing)), label = "s")
    val pulse by inf.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "p")

    Canvas(modifier = modifier) {
        val cols = 7; val rows = 12
        val cw = size.width / cols; val rh = size.height / rows
        // Green eco grid
        for (i in 0..cols) drawLine(EcoGreen.copy(alpha), Offset(i * cw, 0f), Offset(i * cw, size.height), 0.5f)
        for (i in 0..rows) drawLine(EcoGreen.copy(alpha), Offset(0f, i * rh), Offset(size.width, i * rh), 0.5f)
        // Scan line
        val y = size.height * scanY
        drawLine(
            brush       = Brush.horizontalGradient(listOf(Color.Transparent, EcoGreen.copy(0.8f), Color.Transparent)),
            start       = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 2.5f
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// WASTE VIEWFINDER (scanning frame)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WasteViewfinder() {
    val inf   = rememberInfiniteTransition(label = "vf")
    val glow  by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "g")
    val size  = 240.dp

    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val len = w * 0.22f; val str = 4f
        // Corner brackets
        listOf(
            Triple(Offset(0f, 0f),       Offset(len, 0f),       Offset(0f, len)),
            Triple(Offset(w - len, 0f),  Offset(w, 0f),         Offset(w, len)),
            Triple(Offset(0f, h - len),  Offset(0f, h),         Offset(len, h)),
            Triple(Offset(w - len, h),   Offset(w, h),          Offset(w, h - len))
        ).forEach { (corner, h1, v1) ->
            // Determine the corner point from the other two
            val cx = if (corner.x < w / 2) 0f else w
            val cy = if (corner.y < h / 2) 0f else h
            drawLine(EcoGreen.copy(glow), Offset(cx, cy), h1, str)
            drawLine(EcoGreen.copy(glow), Offset(cx, cy), v1, str)
        }
        // Center dot
        drawCircle(EcoGreen.copy(glow * 0.5f), 6f, center)
        drawCircle(EcoGreen.copy(glow), 3f, center)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TOP HUD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WasteTopHud(flashEnabled: Boolean, onBack: () -> Unit, onFlash: () -> Unit, onGallery: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(0.5f)).border(1.dp, EcoGreen.copy(0.3f), RoundedCornerShape(10.dp)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Text("←", color = Color.White)
            }
            Column {
                Text("♻️ Waste Classifier", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("AI • Eco Intelligence", style = MaterialTheme.typography.labelSmall, color = EcoGreen)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(0.5f)).border(1.dp, EcoGreen.copy(0.3f), RoundedCornerShape(10.dp)).clickable(onClick = onGallery).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("🖼️", fontSize = 16.sp)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(0.5f)).border(1.dp, EcoGreen.copy(0.3f), RoundedCornerShape(10.dp)).clickable(onClick = onFlash).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(if (flashEnabled) "⚡" else "🔦", fontSize = 16.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CAPTURE BUTTON
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WasteCaptureButton(modifier: Modifier, onCapture: () -> Unit) {
    val inf   = rememberInfiniteTransition(label = "cap")
    val pulse by inf.animateFloat(1f, 1.08f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "sc")
    Box(
        modifier = modifier
            .scale(pulse)
            .size(70.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(EcoGreen, EcoGreenDim)))
            .border(3.dp, Color.White.copy(0.3f), CircleShape)
            .clickable(onClick = onCapture),
        contentAlignment = Alignment.Center
    ) { Text("♻️", fontSize = 30.sp) }
}

// ══════════════════════════════════════════════════════════════════════════════
// SCANNING INDICATOR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WasteScanningIndicator() {
    val inf = rememberInfiniteTransition(label = "wsi")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "r")
    val pulse by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "p")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(90.dp).graphicsLayer(rotationZ = rot)) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(Brush.sweepGradient(listOf(Color.Transparent, EcoGreen)), 0f, 270f, false, style = Stroke(4f, cap = StrokeCap.Round))
            }
        }
        Text("Analyzing waste...", style = MaterialTheme.typography.bodyMedium, color = EcoGreen.copy(alpha = pulse))
        Text("AI powered by Gemini Vision", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// WASTE RESULT SHEET
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WasteResultSheet(result: WasteResult, onRescan: () -> Unit, onSpeak: () -> Unit) {
    val color = Color(result.primaryType.colorArgb)
    val know  = WasteKnowledgeBase.getKnowledge(result.primaryType)
    val pct   = (result.confidence * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Brush.verticalGradient(listOf(Color(0xF0060D1F), Color(0xFA040A10))))
            .border(1.dp, Brush.horizontalGradient(listOf(color.copy(0.7f), EcoGreen.copy(0.3f))), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .navigationBarsPadding()
    ) {
        // Drag handle
        Box(Modifier.padding(top = 10.dp).width(40.dp).height(4.dp).clip(CircleShape).background(color.copy(0.5f)).align(Alignment.CenterHorizontally))

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(0.15f)).border(1.dp, color.copy(0.4f), RoundedCornerShape(14.dp)), Alignment.Center) {
                        Text(result.primaryType.emoji, fontSize = 28.sp)
                    }
                    Column {
                        Text(result.primaryType.displayName, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NeonBadge(result.primaryType.bin, color)
                            NeonBadge("$pct% confident", EcoGreen)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.08f)).clickable(onClick = onSpeak), Alignment.Center) { Text("🔊", fontSize = 14.sp) }
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.08f)).clickable(onClick = onRescan), Alignment.Center) { Text("🔄", fontSize = 14.sp) }
                }
            }

            // Confidence bar
            WasteConfidenceBar(confidence = result.confidence, color = color)

            // ── Category bars ─────────────────────────────────────────────────
            WasteCategoryBars(result.allCategories.take(5))

            // ── Recycling rate ────────────────────────────────────────────────
            RecyclingRateCard(know.recyclingRate, color)

            // ── Disposal Steps ────────────────────────────────────────────────
            WasteSection("Disposal Steps", "🗑️", color) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    know.disposalSteps.forEachIndexed { i, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(22.dp).clip(CircleShape).background(color.copy(0.15f)), Alignment.Center) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                            }
                            Text(step, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), modifier = Modifier.weight(1f), lineHeight = 18.sp)
                        }
                    }
                }
            }

            // ── Eco Tip ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(EcoGreen.copy(0.10f)).border(1.dp, EcoGreen.copy(0.3f), RoundedCornerShape(12.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🌱", fontSize = 18.sp)
                Column {
                    Text("Eco Tip", style = MaterialTheme.typography.labelSmall, color = EcoGreen, fontWeight = FontWeight.Bold)
                    Text(know.ecoTip, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
                }
            }

            // ── Reusability ────────────────────────────────────────────────────
            WasteSection("Reusability", "♻️", EcoGreen) {
                Text(know.reusabilityInfo, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
            }

            // ── Carbon Footprint ──────────────────────────────────────────────
            WasteSection("Carbon Footprint", "🌍", NeonOrange) {
                Text(know.carbonFootprint, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
            }

            // ── Fun fact ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(0.08f)).border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("💡", fontSize = 18.sp)
                Column {
                    Text("Did You Know?", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                    Text(know.funFact, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WasteConfidenceBar(confidence: Float, color: Color) {
    val pct    = (confidence * 100).toInt()
    val anim   by animateFloatAsState(targetValue = confidence, animationSpec = tween(700), label = "c")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Classification confidence", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
            Text("$pct%", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(0.1f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(anim).background(Brush.horizontalGradient(listOf(EcoGreen, color)), CircleShape))
        }
    }
}

@Composable
private fun WasteCategoryBars(categories: List<WasteCategoryScore>) {
    WasteSection("Category Breakdown", "📊", EcoGreen) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { cat ->
                val color = Color(cat.type.colorArgb)
                val anim  by animateFloatAsState(targetValue = cat.score, animationSpec = tween(600), label = "b")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(cat.type.emoji, fontSize = 14.sp, modifier = Modifier.width(24.dp))
                    Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Color.White.copy(0.1f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(anim).background(color, CircleShape))
                    }
                    Text("${(cat.score * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.width(36.dp), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RecyclingRateCard(rate: Float, color: Color) {
    val pct  = (rate * 100).toInt()
    val anim by animateFloatAsState(targetValue = rate, animationSpec = tween(900), label = "rr")
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(0.08f)).border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Global Recycling Rate", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
            Text("$pct% recycled worldwide", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(Color.White.copy(0.1f), -90f, 360f, false, style = Stroke(6f))
                drawArc(color, -90f, 360f * anim, false, style = Stroke(6f, cap = StrokeCap.Round))
            }
            Text("$pct%", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WasteSection(title: String, icon: String, color: Color, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.04f)).border(1.dp, color.copy(0.2f), RoundedCornerShape(14.dp)).padding(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 14.sp)
                Text(title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = color.copy(0.15f))
            content()
        }
    }
}

@Composable
private fun WasteHistoryStrip(history: List<WasteResult>, modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)))).padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        history.take(5).forEach { r ->
            val color = Color(r.primaryType.colorArgb)
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(0.15f)).border(1.dp, color.copy(0.4f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("${r.primaryType.emoji} ${r.primaryType.displayName}", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold) }
        }
    }
}
