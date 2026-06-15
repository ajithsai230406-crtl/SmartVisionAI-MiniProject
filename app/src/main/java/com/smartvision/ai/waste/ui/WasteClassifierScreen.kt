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
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast


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
        try {
            val source = android.graphics.ImageDecoder.createSource(ctx.contentResolver, uri)
            val bmp = android.graphics.ImageDecoder.decodeBitmap(source)
            capturedBmp = bmp
        } catch (e: Exception) {
            e.printStackTrace()
        }
        viewModel.analyzeFromUri(ctx, uri)
    }

    LaunchedEffect(Unit) { if (!camPerm.status.isGranted) camPerm.launchPermissionRequest() }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeaking()
        }
    }

    LaunchedEffect(flashEnabled) { cameraRef.value?.cameraControl?.enableTorch(flashEnabled) }

    val isClassified = scanState is WasteScanState.Classified

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040A0E))) {

        // ── Camera preview ────────────────────────────────────────────────────
        if (!isClassified) {
            if (camPerm.status.isGranted) {
                WasteCameraPreview(
                    modifier      = Modifier.fillMaxSize(),
                    onPreviewViewCreated = { previewViewRef = it },
                    onCameraReady = { cameraRef.value = it },
                    onFrame       = { bitmap -> viewModel.processLiveFrame(bitmap) }
                )
            } else {
                NoCamPermView(
                    onRequest = { camPerm.launchPermissionRequest() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ── Eco scanner grid ──────────────────────────────────────────────────
        if (scanState is WasteScanState.Idle && !isClassified) EcoScannerOverlay(Modifier.fillMaxSize())

        // ── Top HUD ────────────────────────────────────────────────────────
        WasteTopHud(
            isClassified  = isClassified,
            flashEnabled  = flashEnabled,
            onBack        = {
                if (isClassified) {
                    viewModel.resetScan()
                    capturedBmp = null
                } else {
                    onBack()
                }
            },
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
            visible  = scanState is WasteScanState.Idle && camPerm.status.isGranted && !isClassified,
            enter    = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) { WasteViewfinder() }

        AnimatedVisibility(
            visible  = scanState is WasteScanState.Idle && camPerm.status.isGranted && !isClassified,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        ) {
            WasteCaptureButton(
                modifier = Modifier,
                onCapture = {
                    previewViewRef?.bitmap?.let { bmp ->
                        capturedBmp = bmp
                        viewModel.captureAndClassify(bmp)
                    }
                }
            )
        }

        // ── Full-Screen Classified Result view ────────────────────────────────
        if (isClassified) {
            val state = scanState as WasteScanState.Classified
            val result = state.result
            val know = WasteKnowledgeBase.getKnowledge(result.primaryType)
            val pct = (result.confidence * 100).toInt()
            val context = LocalContext.current

            // Monitor state changes to fetch preview bitmap if needed
            LaunchedEffect(scanState) {
                if (capturedBmp == null) {
                    capturedBmp = previewViewRef?.bitmap
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 70.dp) // Leave space for Top HUD
                    .background(Color(0xFF040A0E))
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Image and Identified Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left: Captured Image Card
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, EcoGreen, RoundedCornerShape(16.dp))
                                .background(Color.DarkGray)
                        ) {
                            if (capturedBmp != null) {
                                Image(
                                    bitmap = capturedBmp!!.asImageBitmap(),
                                    contentDescription = "Captured Waste",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("♻️", fontSize = 48.sp)
                                }
                            }
                        }

                        // Right: Identified As & Confidence Score
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Identified As:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(0.6f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "♻️",
                                    fontSize = 20.sp,
                                    color = EcoGreen
                                )
                                Text(
                                    text = result.exactItemName.ifBlank { result.primaryType.displayName },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = EcoGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Confidence Score Card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(0.03f))
                                    .border(1.dp, EcoGreen.copy(0.15f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Confidence Score",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(0.5f)
                                )
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = EcoGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                // Custom progress bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(0.1f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(result.confidence)
                                            .background(EcoGreen, CircleShape)
                                    )
                                }
                                Text(
                                    text = if (pct >= 85) "High Confidence" else if (pct >= 60) "Medium Confidence" else "Low Confidence",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EcoGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // About This Item Card
                    EcoCard(title = "About This Item") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result.environmentalImpact.ifBlank { know.description },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(0.9f),
                                modifier = Modifier.weight(1f),
                                lineHeight = 20.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EcoGreen.copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("♻️", color = EcoGreen, fontSize = 16.sp)
                            }
                        }
                    }

                    // How to Dispose Card
                    EcoCard(title = "How to Dispose") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (result.isRecyclable) "Recyclable Waste" else "Non-Recyclable Waste",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = result.disposalMethod.ifBlank {
                                        if (result.isRecyclable) "Dispose in recycling bin" else "Dispose in general waste bin"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(0.6f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EcoGreen.copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🗑️", fontSize = 22.sp)
                            }
                        }
                    }

                    // Other Possible Matches Card
                    EcoCard(title = "Other Possible Matches") {
                        val matches = getPossibleMatches(result.primaryType, result.confidence, result.exactItemName)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            matches.forEachIndexed { index, match ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Emoji/Icon
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (index == 0) EcoGreen.copy(0.15f) else Color.White.copy(0.05f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(match.emoji, fontSize = 14.sp)
                                    }
                                    
                                    // Label
                                    Text(
                                        text = match.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (index == 0) Color.White else Color.White.copy(0.6f),
                                        modifier = Modifier.width(130.dp),
                                        maxLines = 1
                                    )

                                    // Progress Bar
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(0.08f))
                                    ) {
                                        val barColor = if (index == 0) EcoGreen else Color.White.copy(0.2f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(match.percentage / 100f)
                                                .background(barColor, CircleShape)
                                        )
                                    }

                                    // Percentage
                                    Text(
                                        text = "${match.percentage}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (index == 0) EcoGreen else Color.White.copy(0.5f),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bottom Buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF040A0E))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Outlined Scan Again
                    OutlinedButton(
                        onClick = {
                            viewModel.resetScan()
                            capturedBmp = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        border = BorderStroke(1.5.dp, EcoGreen),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EcoGreen)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔄", fontSize = 14.sp)
                            Text("Scan Again", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }

                    // Solid Confirm & Save
                    Button(
                        onClick = {
                            Toast.makeText(context, "Waste details confirmed & saved!", Toast.LENGTH_SHORT).show()
                            viewModel.resetScan()
                            capturedBmp = null
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EcoGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✓", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Confirm & Save", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
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
        if (history.isNotEmpty() && scanState is WasteScanState.Idle && !isClassified) {
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
    onCameraReady: (Camera) -> Unit,
    onFrame: (Bitmap) -> Unit
) {
    val ctx            = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope          = rememberCoroutineScope()
    val executor       = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    val processing     = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            executor.shutdown()
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                try {
                    future.get().unbindAll()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
        }
    }

    AndroidView(modifier = modifier, factory = { c ->
        val pv = PreviewView(c).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType    = PreviewView.ScaleType.FILL_CENTER
        }
        onPreviewViewCreated(pv)
        ProcessCameraProvider.getInstance(c).addListener({
            val prov    = ProcessCameraProvider.getInstance(c).get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
            
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(640, 640))
                .build()

            analysis.setAnalyzer(executor) { proxy ->
                if (processing.compareAndSet(false, true)) {
                    scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                        try {
                            val rotation = proxy.imageInfo.rotationDegrees
                            val bitmap = proxy.toBitmap()
                            val rotatedBitmap = if (rotation != 0) {
                                val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            } else {
                                bitmap
                            }
                            onFrame(rotatedBitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            proxy.close()
                            processing.set(false)
                        }
                    }
                } else {
                    proxy.close()
                }
            }

            val cam = prov.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
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
private fun WasteTopHud(
    isClassified: Boolean,
    flashEnabled: Boolean,
    onBack: () -> Unit,
    onFlash: () -> Unit,
    onGallery: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(0.5f))
                    .border(1.dp, EcoGreen.copy(0.5f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(
                    text = if (isClassified) "♻️ Waste Recognized" else "♻️ Waste Classifier",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text("AI • Eco Intelligence", style = MaterialTheme.typography.labelSmall, color = EcoGreen)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(0.5f))
                    .border(1.dp, EcoGreen.copy(0.5f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onGallery)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("🖼️", fontSize = 16.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(0.5f))
                    .border(1.dp, EcoGreen.copy(0.5f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onFlash)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(if (flashEnabled) "⚡" else "🔦", fontSize = 16.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// RESULTS SCREEN LAYOUT HELPER COMPONENTS & TYPES
// ══════════════════════════════════════════════════════════════════════════════

private data class PossibleMatch(val emoji: String, val label: String, val percentage: Int)

private fun getPossibleMatches(primaryType: WasteType, confidence: Float, exactName: String): List<PossibleMatch> {
    val pct = (confidence * 100).toInt().coerceIn(1, 100)
    val name = exactName.ifBlank { primaryType.displayName }
    
    val pct2 = ((100 - pct) * 0.8f).toInt().coerceAtLeast(1)
    val pct3 = ((100 - pct) * 0.2f).toInt().coerceAtLeast(1)

    return when (primaryType) {
        WasteType.PLASTIC -> listOf(
            PossibleMatch("♻️", name, pct),
            PossibleMatch("🛍️", "Plastic Bag", pct2),
            PossibleMatch("⬜", "Packaging Wrapper", pct3)
        )
        WasteType.METAL -> listOf(
            PossibleMatch("♻️", name, pct),
            PossibleMatch("🥫", "Steel Can", pct2),
            PossibleMatch("⬜", "Aluminium Foil", pct3)
        )
        WasteType.ORGANIC -> listOf(
            PossibleMatch("♻️", name, pct),
            PossibleMatch("🍌", "Fruit Peels", pct2),
            PossibleMatch("🍂", "Garden Waste", pct3)
        )
        WasteType.PAPER -> listOf(
            PossibleMatch("♻️", name, pct),
            PossibleMatch("📰", "Newspaper", pct2),
            PossibleMatch("⬜", "Cardboard Sheet", pct3)
        )
        WasteType.GLASS -> listOf(
            PossibleMatch("♻️", name, pct),
            PossibleMatch("🏺", "Glass Jar", pct2),
            PossibleMatch("⬜", "Broken Glass", pct3)
        )
        else -> listOf(
            PossibleMatch("♻️", name, pct),
            PossibleMatch("🗑️", "Mixed Waste", pct2),
            PossibleMatch("⬜", "Other Material", pct3)
        )
    }
}

@Composable
private fun EcoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.02f))
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = EcoGreen,
            fontWeight = FontWeight.Bold
        )
        content()
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
        Text("AI powered by OpenRouter", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
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
                        Text(result.exactItemName.ifBlank { result.primaryType.displayName }, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            NeonBadge(result.recyclingCategory.ifBlank { result.primaryType.bin }, color)
                            NeonBadge("$pct% match", EcoGreen)
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

            // ── Eco Dashboard (Eco Score + Carbon Reduction) ──────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(0.02f))
                    .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EcoScoreGauge(score = result.ecoScore)
                    Box(modifier = Modifier.weight(1f)) {
                        CarbonReductionCard(carbonReduction = result.carbonReduction)
                    }
                }
            }

            // ── Recyclability & Reusability Badges ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Recyclable card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (result.isRecyclable) EcoGreen.copy(0.1f) else NeonPink.copy(0.1f))
                        .border(1.dp, if (result.isRecyclable) EcoGreen.copy(0.3f) else NeonPink.copy(0.3f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (result.isRecyclable) "♻️ Recyclable" else "🗑️ Non-Recyclable", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = if (result.isRecyclable) EcoGreen else NeonPink,
                        fontWeight = FontWeight.Bold)
                }
                
                // Reusable card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (result.isReusable) NeonBlue.copy(0.1f) else NeonOrange.copy(0.1f))
                        .border(1.dp, if (result.isReusable) NeonBlue.copy(0.3f) else NeonOrange.copy(0.3f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (result.isReusable) "🔄 Reusable" else "🚫 Single-Use Only", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = if (result.isReusable) NeonBlue else NeonOrange,
                        fontWeight = FontWeight.Bold)
                }
            }

            // ── Category bars ─────────────────────────────────────────────────
            WasteCategoryBars(result.allCategories.take(5))

            // ── Recycling rate ────────────────────────────────────────────────
            RecyclingRateCard(know.recyclingRate, color)

            // ── Disposal Steps ────────────────────────────────────────────────
            val steps = result.disposalSteps.ifEmpty { know.disposalSteps }
            WasteSection("Disposal Steps", "🗑️", color) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    steps.forEachIndexed { i, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(0.15f))
                                    .border(1.dp, color.copy(0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                            }
                            Text(step, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), modifier = Modifier.weight(1f), lineHeight = 18.sp)
                        }
                    }
                }
            }

            // ── Sustainability Tip ──────────────────────────────────────────
            val tip = result.sustainabilityTip.ifBlank { know.ecoTip }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EcoGreen.copy(0.08f))
                    .border(1.dp, EcoGreen.copy(0.25f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🌱", fontSize = 20.sp)
                Column {
                    Text("Sustainability Tip", style = MaterialTheme.typography.labelSmall, color = EcoGreen, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(tip, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.9f), lineHeight = 18.sp)
                }
            }

            // ── Reuse Suggestions ────────────────────────────────────────────
            val reuse = result.reuseSuggestions.ifBlank { know.reusabilityInfo }
            WasteSection("Reuse Suggestions", "♻️", EcoGreen) {
                Text(reuse, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
            }

            // ── Environmental Impact ─────────────────────────────────────────
            val impact = result.environmentalImpact.ifBlank { know.description }
            WasteSection("Environmental Impact", "🌍", NeonOrange) {
                Text(impact, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
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
private fun EcoScoreGauge(score: Int) {
    val animScore by animateFloatAsState(targetValue = score.toFloat() / 100f, animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "eco")
    val color = when {
        score >= 80 -> EcoGreen
        score >= 50 -> EcoYellow
        else -> NeonPink
    }
    
    Box(
        contentAlignment = Alignment.Center, 
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.03f))
            .border(1.dp, color.copy(0.15f), CircleShape)
    ) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            // Draw background track
            drawArc(Color.White.copy(0.06f), -90f, 360f, false, style = Stroke(6f))
            // Draw animated progress track
            drawArc(
                brush = Brush.sweepGradient(listOf(color.copy(0.3f), color)),
                startAngle = -90f,
                sweepAngle = 360f * animScore,
                useCenter = false,
                style = Stroke(6f, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Score",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun CarbonReductionCard(carbonReduction: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(0.04f))
            .border(1.5.dp, Brush.horizontalGradient(listOf(EcoGreen.copy(0.2f), Color.Cyan.copy(0.1f))), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(EcoGreen.copy(0.12f))
                .border(1.dp, EcoGreen.copy(0.3f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("🌍", fontSize = 18.sp)
        }
        Column {
            Text(
                text = "Carbon Saved Est.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.5f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = carbonReduction.ifBlank { "0.8 kg CO₂" },
                style = MaterialTheme.typography.bodyMedium,
                color = EcoGreen,
                fontWeight = FontWeight.Bold
            )
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

@Composable
private fun NoCamPermView(onRequest: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("♻️", fontSize = 64.sp)
            Text("Camera permission required", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("Required to classify waste in real time", color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = EcoGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
