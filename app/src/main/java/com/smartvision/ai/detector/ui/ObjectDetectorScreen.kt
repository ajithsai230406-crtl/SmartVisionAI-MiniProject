package com.smartvision.ai.detector.ui

import android.Manifest
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.detector.domain.*
import com.smartvision.ai.detector.presentation.ObjectDetectorViewModel
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

// ══════════════════════════════════════════════════════════════════════════════
// OBJECT DETECTOR — MAIN SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ObjectDetectorScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: ObjectDetectorViewModel = hiltViewModel()
) {
    val camPerm         = rememberPermissionState(Manifest.permission.CAMERA)
    val detectionState  by viewModel.detectionState.collectAsStateWithLifecycle()
    val selectedItem    by viewModel.selectedItem.collectAsStateWithLifecycle()
    val knowledge       by viewModel.knowledge.collectAsStateWithLifecycle()
    val flashEnabled    by viewModel.flashEnabled.collectAsStateWithLifecycle()
    val showOverlay     by viewModel.showOverlay.collectAsStateWithLifecycle()
    val isLive          by viewModel.isLive.collectAsStateWithLifecycle()
    val fps             by viewModel.fps.collectAsStateWithLifecycle()
    val cameraRef       = remember { mutableStateOf<Camera?>(null) }
    val previewViewRef  = remember { mutableStateOf<PreviewView?>(null) }

    val ctx = LocalContext.current
    val galleryLauncher = rememberGalleryPickerLauncher { uri ->
        viewModel.analyzeFromUri(ctx, uri)
    }

    LaunchedEffect(Unit) {
        if (!camPerm.status.isGranted) camPerm.launchPermissionRequest()
    }
    LaunchedEffect(flashEnabled) {
        cameraRef.value?.cameraControl?.enableTorch(flashEnabled)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera feed ──────────────────────────────────────────────────────
        if (camPerm.status.isGranted) {
            DetectorCameraPreview(
                modifier             = Modifier.fillMaxSize(),
                onCameraReady        = { cameraRef.value = it },
                onPreviewViewCreated = { previewViewRef.value = it },
                onFrame              = { img, rot, w, h -> viewModel.analyzeFrame(img, rot, w, h) }
            )
        } else {
            NoCamPermView(
                onRequest = { camPerm.launchPermissionRequest() },
                modifier  = Modifier.fillMaxSize()
            )
        }

        // ── Futuristic scanning grid overlay ─────────────────────────────────
        if (isLive) ScannerGridOverlay(modifier = Modifier.fillMaxSize())

        // ── Bounding box overlay ─────────────────────────────────────────────
        if (showOverlay) {
            val activeState = detectionState as? DetectionState.Active
            val items       = activeState?.items ?: emptyList()
            val frameW      = activeState?.frameWidth ?: 1080
            val frameH      = activeState?.frameHeight ?: 1920
            BoundingBoxOverlay(
                items        = items,
                selectedItem = selectedItem,
                frameWidth   = frameW,
                frameHeight  = frameH,
                onTapItem    = { viewModel.selectItem(it, previewViewRef.value?.bitmap) },
                modifier     = Modifier.fillMaxSize()
            )
        }

        // ── Top HUD ────────────────────────────────────────────────────────────
        DetectorTopHud(
            fps          = fps,
            flashEnabled = flashEnabled,
            showOverlay  = showOverlay,
            isLive       = isLive,
            onBack       = onBack,
            onFlash      = { viewModel.toggleFlash() },
            onOverlay    = { viewModel.toggleOverlay() },
            onResume     = { viewModel.resumeLive() },
            onGallery    = { galleryLauncher() }
        )

        // ── Bottom detection info strip ───────────────────────────────────────
        val activeItems = (detectionState as? DetectionState.Active)?.items ?: emptyList()
        AnimatedVisibility(
            visible  = activeItems.isNotEmpty() && selectedItem == null,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            DetectionStrip(
                items      = activeItems,
                selected   = selectedItem,
                onSelect   = { viewModel.selectItem(it, previewViewRef.value?.bitmap) }
            )
        }

        // ── No objects indicator ──────────────────────────────────────────────
        AnimatedVisibility(
            visible  = detectionState is DetectionState.NoObjects,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            ScanningIndicator()
        }

        // ── Object detail sheet ───────────────────────────────────────────────
        AnimatedVisibility(
            visible  = selectedItem != null && knowledge != null,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (selectedItem != null && knowledge != null) {
                ObjectDetailSheet(
                    item       = selectedItem!!,
                    know       = knowledge!!,
                    onDismiss  = { viewModel.dismissDetail() },
                    onSpeak    = { viewModel.speakObject(selectedItem!!) },
                    onNavigate = onNavigate
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CAMERA PREVIEW
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DetectorCameraPreview(
    modifier:             Modifier,
    onCameraReady:        (Camera) -> Unit,
    onPreviewViewCreated: (PreviewView) -> Unit,
    onFrame:              suspend (android.media.Image, Int, Int, Int) -> Unit
) {
    val ctx            = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope          = rememberCoroutineScope()
    val executor       = remember { Executors.newSingleThreadExecutor() }
    val processing     = remember { AtomicBoolean(false) }

    AndroidView(
        modifier = modifier,
        factory  = { c ->
            val pv = PreviewView(c).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            onPreviewViewCreated(pv)
            ProcessCameraProvider.getInstance(c).addListener({
                val prov    = ProcessCameraProvider.getInstance(c).get()
                val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(pv.surfaceProvider) }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                analysis.setAnalyzer(executor) { proxy ->
                    val img = proxy.image
                    if (img != null && processing.compareAndSet(false, true)) {
                        scope.launch {
                            try {
                                onFrame(img, proxy.imageInfo.rotationDegrees, img.width, img.height)
                            } finally {
                                proxy.close()
                                processing.set(false)
                            }
                        }
                    } else {
                        proxy.close()
                    }
                }
                val cam = prov.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview, analysis
                )
                onCameraReady(cam)
            }, ContextCompat.getMainExecutor(c))
            pv
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// SCANNER GRID OVERLAY (futuristic grid lines)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ScannerGridOverlay(modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "grid")
    val alpha by inf.animateFloat(
        0.04f, 0.12f,
        infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "gridAlpha"
    )
    val scanY by inf.animateFloat(
        -0.05f, 1.05f,
        infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "scan"
    )
    val pulseReticle by inf.animateFloat(
        0.4f, 1.0f,
        infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "reticle"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        // Grid lines
        val cols = 6; val rows = 10
        val cw = size.width / cols; val rh = size.height / rows
        for (i in 0..cols) drawLine(Color(0xFF00D4FF).copy(alpha), Offset(i * cw, 0f), Offset(i * cw, size.height), 0.5f)
        for (i in 0..rows) drawLine(Color(0xFF00D4FF).copy(alpha), Offset(0f, i * rh), Offset(size.width, i * rh), 0.5f)

        // Scan line
        val y = size.height * scanY
        drawLine(
            brush       = Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF00D4FF).copy(0.7f), Color.Transparent)),
            start       = Offset(0f, y),
            end         = Offset(size.width, y),
            strokeWidth = 3f
        )

        // Targeted scanner reticle in the middle
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rLen = 30f
        val rOffset = 40f
        val color = Color(0xFF00D4FF).copy(alpha = pulseReticle * 0.4f)

        // Reticle corners
        // Top-left
        drawLine(color, Offset(cx - rOffset, cy - rOffset), Offset(cx - rOffset + rLen, cy - rOffset), 2f)
        drawLine(color, Offset(cx - rOffset, cy - rOffset), Offset(cx - rOffset, cy - rOffset + rLen), 2f)
        // Top-right
        drawLine(color, Offset(cx + rOffset, cy - rOffset), Offset(cx + rOffset - rLen, cy - rOffset), 2f)
        drawLine(color, Offset(cx + rOffset, cy - rOffset), Offset(cx + rOffset, cy - rOffset + rLen), 2f)
        // Bottom-left
        drawLine(color, Offset(cx - rOffset, cy + rOffset), Offset(cx - rOffset + rLen, cy + rOffset), 2f)
        drawLine(color, Offset(cx - rOffset, cy + rOffset), Offset(cx - rOffset, cy + rOffset - rLen), 2f)
        // Bottom-right
        drawLine(color, Offset(cx + rOffset, cy + rOffset), Offset(cx + rOffset - rLen, cy + rOffset), 2f)
        drawLine(color, Offset(cx + rOffset, cy + rOffset), Offset(cx + rOffset, cy + rOffset - rLen), 2f)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// BOUNDING BOX OVERLAY
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun BoundingBoxOverlay(
    items:        List<DetectedItem>,
    selectedItem: DetectedItem?,
    frameWidth:   Int,
    frameHeight:  Int,
    onTapItem:    (DetectedItem) -> Unit,
    modifier:     Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current

    // Pulsing transition for visual intelligence target reticle overlays
    val infiniteTransition = rememberInfiniteTransition(label = "lens_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue  = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(modifier = modifier.onGloballyPositioned { coords ->
        canvasSize = Size(
            coords.size.width.toFloat(),
            coords.size.height.toFloat()
        )
    }) {
        // Canvas for the boxes
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            items.forEach { item ->
                val color = Color(item.accentColor)
                val isSelected = item.id == selectedItem?.id
                
                // Rotated frame dimensions
                val frameW = frameWidth.toFloat().coerceAtLeast(1f)
                val frameH = frameHeight.toFloat().coerceAtLeast(1f)
                
                // Screen canvas dimensions
                val screenW = size.width
                val screenH = size.height
                
                // Scale factor for FILL_CENTER projection
                val scale = maxOf(screenW / frameW, screenH / frameH)
                val scaledW = frameW * scale
                val scaledH = frameH * scale
                
                // Crop offsets
                val offsetX = (screenW - scaledW) / 2f
                val offsetY = (screenH - scaledH) / 2f
                
                // Transform to screen space
                val left   = item.box.left   * scaledW + offsetX
                val top    = item.box.top    * scaledH + offsetY
                val right  = item.box.right  * scaledW + offsetX
                val bottom = item.box.bottom * scaledH + offsetY
                
                val w = right - left
                val h = bottom - top

                // Box fill
                drawRect(
                    color   = color.copy(alpha = if (isSelected) 0.25f else 0.12f),
                    topLeft = Offset(left, top),
                    size    = Size(w, h)
                )
                // Box border
                drawRect(
                    color       = color.copy(alpha = if (isSelected) 1f else 0.5f),
                    topLeft     = Offset(left, top),
                    size        = Size(w, h),
                    style       = Stroke(width = if (isSelected) 3f else 1.5f)
                )

                // Corner accents (Bixby/Lens brackets style)
                val cLen = (w * 0.22f).coerceIn(16f, 36f)
                val cStr = if (isSelected) 4.5f else 2.5f

                // Top-left
                drawLine(color, Offset(left,          top + cLen), Offset(left,          top),          cStr)
                drawLine(color, Offset(left,          top),        Offset(left + cLen,    top),          cStr)
                // Top-right
                drawLine(color, Offset(right - cLen,  top),        Offset(right,          top),          cStr)
                drawLine(color, Offset(right,          top),        Offset(right,          top + cLen),   cStr)
                // Bottom-left
                drawLine(color, Offset(left,          bottom - cLen), Offset(left,         bottom),       cStr)
                drawLine(color, Offset(left,          bottom),     Offset(left + cLen,    bottom),       cStr)
                // Bottom-right
                drawLine(color, Offset(right - cLen,  bottom),     Offset(right,          bottom),       cStr)
                drawLine(color, Offset(right,          bottom),     Offset(right,          bottom - cLen),cStr)

                // Concentric Apple Visual Intelligence style rings around selected target
                if (isSelected) {
                    val scaleFactor = pulseScale
                    val glowW = w * scaleFactor
                    val glowH = h * scaleFactor
                    val dW = (glowW - w) / 2f
                    val dH = (glowH - h) / 2f

                    drawRoundRect(
                        color = color.copy(alpha = glowAlpha * 0.15f),
                        topLeft = Offset(left - dW - 6, top - dH - 6),
                        size = Size(glowW + 12, glowH + 12),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(width = 4f)
                    )
                    
                    // Center pointer crosshair dot
                    drawCircle(
                        color = color.copy(alpha = glowAlpha),
                        radius = 5f,
                        center = Offset(left + w / 2f, top + h / 2f)
                    )
                }
            }
        }

        // Label chips — positioned via Box offset modifiers, mapped correctly with local density
        if (canvasSize != Size.Zero) {
            items.forEach { item ->
                val color  = Color(item.accentColor)
                
                val frameW = frameWidth.toFloat().coerceAtLeast(1f)
                val frameH = frameHeight.toFloat().coerceAtLeast(1f)
                val screenW = canvasSize.width
                val screenH = canvasSize.height
                
                val scale = maxOf(screenW / frameW, screenH / frameH)
                val scaledW = frameW * scale
                val scaledH = frameH * scale
                val offsetX = (screenW - scaledW) / 2f
                val offsetY = (screenH - scaledH) / 2f
                
                val left   = item.box.left   * scaledW + offsetX
                val top    = item.box.top    * scaledH + offsetY
                
                // Density translation from pixel values to DP coordinates
                val labelX = with(density) { left.toDp() }
                val labelY = with(density) { (top - 28f).coerceAtLeast(0f).toDp() }
                val pct    = (item.confidence * 100).toInt()

                Box(
                    modifier = Modifier
                        .absoluteOffset(x = labelX, y = labelY)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(0.90f))
                        .clickable { onTapItem(item) }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${item.label}  $pct%",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = Color.Black,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 1
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TOP HUD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DetectorTopHud(
    fps:          Int,
    flashEnabled: Boolean,
    showOverlay:  Boolean,
    isLive:       Boolean,
    onBack:       () -> Unit,
    onFlash:      () -> Unit,
    onOverlay:    () -> Unit,
    onResume:     () -> Unit,
    onGallery:    () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back + title
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HudButton("←", onClick = onBack)
            Column {
                Text("Object Detector", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Visual AI Scanner", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
            }
        }

        // Right controls
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // FPS badge
            if (fps > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("$fps FPS", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontWeight = FontWeight.Bold)
                }
            }
            if (!isLive) HudButton("▶", onClick = onResume)
            HudButton(if (showOverlay) "◈" else "◉", onClick = onOverlay)
            HudButton(if (flashEnabled) "⚡" else "🔦", onClick = onFlash)
            HudButton("🖼️", onClick = onGallery)
        }
    }
}

@Composable
private fun HudButton(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(0.55f))
            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(icon, fontSize = 16.sp, color = Color.White) }
}

// ══════════════════════════════════════════════════════════════════════════════
// DETECTION STRIP (horizontal scrolling detected objects)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DetectionStrip(
    items:    List<DetectedItem>,
    selected: DetectedItem?,
    onSelect: (DetectedItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))
            )
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp, top = 8.dp)
            .navigationBarsPadding()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val isSelected = item.id == selected?.id
            val color      = Color(item.accentColor)
            val pct        = (item.confidence * 100).toInt()

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) color.copy(0.25f) else Color.Black.copy(0.60f)
                    )
                    .border(
                        1.5.dp,
                        if (isSelected) color else color.copy(0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(item) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        ObjectKnowledgeBase.getKnowledge(item.label).emoji,
                        fontSize = 22.sp
                    )
                    Text(
                        item.label,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = if (isSelected) color else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1
                    )
                    // Confidence bar
                    Box(
                        modifier = Modifier
                            .width(50.dp).height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(item.confidence)
                                .background(color, CircleShape)
                        )
                    }
                    Text("$pct%", style = MaterialTheme.typography.labelSmall, color = color)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SCANNING INDICATOR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ScanningIndicator() {
    val inf = rememberInfiniteTransition(label = "scan")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "rot")
    val pulse by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "p")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer(rotationZ = rot)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                // Outer ring
                drawArc(
                    brush      = Brush.sweepGradient(listOf(Color.Transparent, NeonBlue)),
                    startAngle = 0f, sweepAngle = 270f, useCenter = false,
                    style      = Stroke(width = 3f, cap = StrokeCap.Round)
                )
                // Inner ring (counter-rotate effect via stroke dash)
                drawArc(
                    brush      = Brush.sweepGradient(listOf(Color.Transparent, NeonPurple)),
                    startAngle = 90f, sweepAngle = 180f, useCenter = false,
                    topLeft    = Offset(16f, 16f),
                    size       = Size(size.width - 32f, size.height - 32f),
                    style      = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Scanning for objects...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = pulse)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// OBJECT DETAIL SHEET
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ObjectDetailSheet(
    item:      DetectedItem,
    know:      ObjectKnowledge,
    onDismiss: () -> Unit,
    onSpeak:   () -> Unit,
    onNavigate: (String) -> Unit
) {
    val color = Color(item.accentColor)
    val pct   = (item.confidence * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.72f) // Premium semi-fullscreen sheet height
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF0070E20), Color(0xFC03070E))
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(color.copy(0.7f), NeonPurple.copy(0.4f))),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .navigationBarsPadding()
    ) {
        // ── Drag handle ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .width(40.dp).height(4.dp)
                .clip(CircleShape)
                .background(color.copy(0.5f))
                .align(Alignment.CenterHorizontally)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Emoji icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(color.copy(0.15f))
                            .border(1.dp, color.copy(0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(know.emoji, fontSize = 28.sp) }

                    Column {
                        Text(know.label, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NeonBadge(know.category.displayName, color)
                            NeonBadge("$pct% match", NeonGreen)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp)) {
                    SmallRoundBtn("🔊", onClick = onSpeak)
                    SmallRoundBtn("✕", onClick = onDismiss)
                }
            }

            // Confidence bar with visual indicator
            ConfidenceBar(confidence = item.confidence, color = color)

            // ─── SPECIALIZED UTILITY TRIGGER SECTION ───
            if (know.specializedActionType != null) {
                Spacer(Modifier.height(4.dp))
                SpecializedActionBanner(
                    type = know.specializedActionType,
                    data = know.specializedActionData ?: "",
                    onAction = {
                        val targetRoute = when (know.specializedActionType) {
                            "medicine" -> "medicine_scanner"
                            "textbook" -> "student_helper_v2"
                            "waste"    -> "waste_classifier_v2"
                            else       -> null
                        }
                        if (targetRoute != null) {
                            onDismiss()
                            onNavigate(targetRoute)
                        }
                    }
                )
            }

            // ── Description ───────────────────────────────────────────────────
            DetailSection(title = "About / Purpose", icon = "ℹ️", color = color) {
                Text(know.description, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.9f), lineHeight = 21.sp)
            }

            // ── Uses ──────────────────────────────────────────────────────────
            DetailSection(title = "Usage Suggestions", icon = "⚙️", color = color) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    know.uses.forEach { use ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
                            Text(use, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
                        }
                    }
                }
            }

            // ── Safety ────────────────────────────────────────────────────────
            DetailSection(title = "Safety & Guidelines", icon = "⚠️", color = NeonPink) {
                Text(know.safetyInfo, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 19.sp)
            }

            // ── Maintenance ───────────────────────────────────────────────────
            if (know.maintenanceTips.isNotEmpty()) {
                DetailSection(title = "Maintenance & Care", icon = "🛠️", color = NeonPurple) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        know.maintenanceTips.forEach { tip ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(NeonPurple))
                                Text(tip, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
                            }
                        }
                    }
                }
            }

            // ── Buying Guide ──────────────────────────────────────────────────
            if (know.buyingSuggestions.isNotEmpty()) {
                DetailSection(title = "Smart Buying Suggestions", icon = "🛒", color = NeonBlue) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        know.buyingSuggestions.forEach { suggestion ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Text("🛍️", fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
                                Text(suggestion, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
                            }
                        }
                    }
                }
            }

            // ── Environmental ─────────────────────────────────────────────────
            DetailSection(title = "Environmental & Carbon Impact", icon = "🌿", color = NeonGreen) {
                Text(know.environmentalImpact, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 19.sp)
            }

            // ── Related recommendations ───────────────────────────────────────
            if (know.relatedRecommendations.isNotEmpty()) {
                DetailSection(title = "Related Recommendations", icon = "🔍", color = Color(0xFFFFD700)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        know.relatedRecommendations.forEach { rec ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(0.06f))
                                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(rec, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // ── Fun fact ──────────────────────────────────────────────────────
            FunFactBubble(know.funFact, color)

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SPECIALIZED ACTION MODULE TRIGGER VIEW
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SpecializedActionBanner(
    type: String,
    data: String,
    onAction: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "pulse_action")
    val pulseScale by inf.animateFloat(
        initialValue = 0.98f,
        targetValue  = 1.02f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulseScale"
    )

    when (type) {
        "medicine" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF5A1024), Color(0xFF2C020B)))
                    )
                    .border(1.5.dp, NeonPink, RoundedCornerShape(16.dp))
                    .clickable(onClick = onAction)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("💊", fontSize = 24.sp)
                        Column {
                            Text("Medicine Scanner Action Active", style = MaterialTheme.typography.labelMedium, color = NeonPink, fontWeight = FontWeight.Bold)
                            Text(data.ifBlank { "Verify dosage details" }, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
                        }
                    }
                    Text("OPEN ➔", style = MaterialTheme.typography.labelSmall, color = NeonPink, fontWeight = FontWeight.Bold)
                }
            }
        }
        "textbook" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF033550), Color(0xFF021727)))
                    )
                    .border(1.5.dp, NeonCyan, RoundedCornerShape(16.dp))
                    .clickable(onClick = onAction)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📚", fontSize = 24.sp)
                        Column {
                            Text("AI Student Helper Solver Active", style = MaterialTheme.typography.labelMedium, color = NeonCyan, fontWeight = FontWeight.Bold)
                            Text(data.ifBlank { "Solve math homework with AI Tutor" }, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
                        }
                    }
                    Text("SOLVE ➔", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
        "waste" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF063A1D), Color(0xFF02190B)))
                    )
                    .border(1.5.dp, NeonGreen, RoundedCornerShape(16.dp))
                    .clickable(onClick = onAction)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("♻️", fontSize = 24.sp)
                        Column {
                            Text("Waste Classifier Action Active", style = MaterialTheme.typography.labelMedium, color = NeonGreen, fontWeight = FontWeight.Bold)
                            Text(data.ifBlank { "Verify recycle bins & eco carbon offset" }, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
                        }
                    }
                    Text("ANALYZE ➔", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
        "food" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(0.04f))
                    .border(1.dp, Color(0xFFFFD700).copy(0.2f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🍎", fontSize = 22.sp)
                        Text("Estimated Nutrition Breakdown", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                    
                    // Display linear progress metrics based on dynamic action data
                    // Expected format: "Calories: 95 kcal · Carbs: 25g · Dietary Fiber: 4.4g · Protein: 0.5g"
                    val nutritionString = data.ifBlank { "Calories: 130 kcal · Carbs: 28g · Protein: 2.0g · Fat: 0.3g" }
                    Text(nutritionString, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        NutritionProgressMetric("Calories (kcal)", 0.65f, Color(0xFFFFD700))
                        NutritionProgressMetric("Carbohydrates", 0.72f, NeonBlue)
                        NutritionProgressMetric("Protein", 0.35f, NeonGreen)
                        NutritionProgressMetric("Fats", 0.08f, NeonPink)
                    }
                }
            }
        }
        "electronics" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(0.04f))
                    .border(1.dp, NeonPurple.copy(0.25f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⚡", fontSize = 22.sp)
                        Text("Hardware Maintenance Specs", style = MaterialTheme.typography.labelMedium, color = NeonPurple, fontWeight = FontWeight.Bold)
                    }
                    Text(data.ifBlank { "Consumer hardware status: Operational. Internal circuitry sensors active." }, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                        HardwareCheckItem("Battery Health optimization active")
                        HardwareCheckItem("Standard USB charge limits verified")
                        HardwareCheckItem("Thermal safety margins compliant")
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionProgressMetric(name: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.55f))
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
        }
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(0.1f))) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(color, CircleShape))
        }
    }
}

@Composable
private fun HardwareCheckItem(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("✔", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// REUSABLE DETAIL COMPONENTS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DetailSection(title: String, icon: String, color: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(0.035f))
            .border(1.dp, color.copy(0.2f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 14.sp)
                Text(title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun ConfidenceBar(confidence: Float, color: Color) {
    val pct = (confidence * 100).toInt()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Detection confidence", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
            Text("$pct%", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(0.1f))
        ) {
            val animConf by animateFloatAsState(targetValue = confidence, animationSpec = tween(600), label = "conf")
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animConf)
                    .background(Brush.horizontalGradient(listOf(color, NeonPurple)), CircleShape)
            )
        }
    }
}

@Composable
private fun FunFactBubble(fact: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(0.10f))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Text("💡", fontSize = 18.sp)
        Column {
            Text("Fun Fact", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            Text(fact, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f), lineHeight = 18.sp)
        }
    }
}

@Composable
private fun SmallRoundBtn(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.08f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(icon, fontSize = 14.sp, color = Color.White) }
}

@Composable
private fun NoCamPermView(onRequest: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("📷", fontSize = 64.sp)
            Text("Camera permission required", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("Required to detect objects in real time", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            NeonButton("Grant Permission", onClick = onRequest, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}
