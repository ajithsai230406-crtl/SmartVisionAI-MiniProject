package com.smartvision.ai.ocr.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.smartvision.ai.ocr.domain.*
import com.smartvision.ai.ocr.presentation.OcrTranslatorViewModel
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private val NeonBlueExact = NeonBlue

// ──────────────────────────────────────────────────────────────────────────────
// OCR TRANSLATOR — HIGH-FIDELITY GOOGLE LENS TEXT SELECTION SCREEN
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OcrTranslatorScreen(
    onBack: () -> Unit,
    viewModel: OcrTranslatorViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    val scanState         by viewModel.scanState.collectAsStateWithLifecycle()
    val translationState  by viewModel.translationState.collectAsStateWithLifecycle()
    val overlayBlocks     by viewModel.overlayBlocks.collectAsStateWithLifecycle()
    val selectedBlock     by viewModel.selectedBlock.collectAsStateWithLifecycle()
    val frozenResult      by viewModel.frozenResult.collectAsStateWithLifecycle()
    val targetLanguage    by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val isLiveScan        by viewModel.isLiveScan.collectAsStateWithLifecycle()
    val flashEnabled      by viewModel.flashEnabled.collectAsStateWithLifecycle()
    val cameraRef         = remember { mutableStateOf<Camera?>(null) }
    val previewViewRef    = remember { mutableStateOf<PreviewView?>(null) }

    // Sync flash to camera
    LaunchedEffect(flashEnabled) {
        cameraRef.value?.cameraControl?.enableTorch(flashEnabled)
    }

    val ctx = LocalContext.current
    val galleryLauncher = rememberGalleryPickerLauncher { uri ->
        viewModel.analyzeFromGallery(ctx, uri)
    }

    // Google Lens Resizable targeting viewfinder crop box
    var cropRect by remember { mutableStateOf(Rect(120f, 350f, 960f, 1150f)) }
    var canvasWidth by remember { mutableStateOf(1) }
    var canvasHeight by remember { mutableStateOf(1) }

    Box(modifier = Modifier.fillMaxSize().background(DeepNavy)) {
        // ── Camera Preview ───────────────────────────────────────────────────────
        if (cameraPermission.status.isGranted) {
            OcrCameraPreview(
                modifier             = Modifier.fillMaxSize(),
                onCameraReady        = { cameraRef.value = it },
                onPreviewViewCreated = { previewViewRef.value = it },
                onFrame              = { image, rotation, w, h ->
                    viewModel.analyzeFrame(image, rotation, w, h)
                }
            )

            // Resizable dimming background + targeted selector viewfinder box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        canvasWidth = coords.size.width
                        canvasHeight = coords.size.height
                    }
            ) {
                ResizableSelectorViewfinder(
                    rect = cropRect,
                    onRectChange = { cropRect = it },
                    modifier = Modifier.fillMaxSize()
                )

                // Normalized crop bounds to check inside collision blocks
                val normLeft = cropRect.left / canvasWidth
                val normTop = cropRect.top / canvasHeight
                val normRight = cropRect.right / canvasWidth
                val normBottom = cropRect.bottom / canvasHeight

                val activeBlocks = if (isLiveScan) overlayBlocks else (frozenResult?.blocks ?: emptyList())
                val activeState = scanState as? OcrScanState.TextDetected
                val frameW = activeState?.frameWidth ?: 1080
                val frameH = activeState?.frameHeight ?: 1920

                // Draw detected highlighted text blocks inside viewfinder
                if (activeBlocks.isNotEmpty()) {
                    TextSelectorOverlay(
                        blocks = activeBlocks,
                        selectedBlock = selectedBlock,
                        frameWidth = frameW,
                        frameHeight = frameH,
                        viewfinderLeft = normLeft,
                        viewfinderTop = normTop,
                        viewfinderRight = normRight,
                        viewfinderBottom = normBottom,
                        onBlockClick = { block ->
                            viewModel.selectBlock(block)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            NoCameraPermissionView(
                onRequest = { cameraPermission.launchPermissionRequest() },
                modifier  = Modifier.fillMaxSize()
            )
        }

        // ── Top HUD Navigation Bar ──────────────────────────────────────────────
        OcrTopBar(
            onBack        = onBack,
            flashEnabled  = flashEnabled,
            isLive        = isLiveScan,
            onFlash       = { viewModel.toggleFlash() },
            onGallery     = { galleryLauncher() },
            onResume      = { viewModel.resumeLiveScan() }
        )

        // ── Center scan indicators ──────────────────────────────────────────────
        if (isLiveScan && (scanState is OcrScanState.Idle || scanState is OcrScanState.NoText)) {
            ScanHintLabel(modifier = Modifier.align(Alignment.Center))
        }

        // ── Dimming overlay for frozen frames ───────────────────────────────────
        if (!isLiveScan && frozenResult != null) {
            FrozenOverlay(modifier = Modifier.fillMaxSize())
        }

        // ── Dedicated Lens-Style Scan & Translate Pulsing Button ─────────────────
        if (isLiveScan) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(4.dp, NeonCyan, CircleShape)
                    .clickable {
                        viewModel.freezeAndCapture()
                        viewModel.translateCurrentText()
                    },
                contentAlignment = Alignment.Center
            ) {
                val inf = rememberInfiniteTransition(label = "ringPulse")
                val ringScale by inf.animateFloat(1f, 1.25f, infiniteRepeatable(tween(1200), RepeatMode.Reverse))
                val ringAlpha by inf.animateFloat(0.4f, 0f, infiniteRepeatable(tween(1200), RepeatMode.Reverse))

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .graphicsLayer(scaleX = ringScale, scaleY = ringScale)
                        .border(2.dp, NeonCyan.copy(alpha = ringAlpha), CircleShape)
                )
                Text("🔍", fontSize = 24.sp)
            }
        }

        // ── Bottom Panel Translation Console (Only visible when frozen/scanned) ──
        AnimatedVisibility(
            visible = !isLiveScan,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY  = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val result = (frozenResult ?: (scanState as? OcrScanState.TextDetected)?.result)
            if (result != null) {
                // If a specific block is tapped, translate that block. 
                // Otherwise, automatically aggregate all text blocks falling inside the active viewfinder!
                val viewfinderText = remember(result, cropRect, canvasWidth, canvasHeight) {
                    val normLeft = cropRect.left / canvasWidth
                    val normTop = cropRect.top / canvasHeight
                    val normRight = cropRect.right / canvasWidth
                    val normBottom = cropRect.bottom / canvasHeight

                    val targeted = result.blocks.filter { block ->
                        block.left >= normLeft && block.right <= normRight &&
                        block.top >= normTop && block.bottom <= normBottom
                    }
                    if (targeted.isNotEmpty()) {
                        targeted.joinToString(" ") { it.text }
                    } else {
                        result.fullText
                    }
                }

                val displayText = selectedBlock?.text ?: viewfinderText

                OcrBottomPanel(
                    resultText       = displayText,
                    detectedLangName = result.detectedLanguageName,
                    detectedLangCode = result.detectedLanguage,
                    blockCount       = result.blocks.size,
                    selectedBlock    = selectedBlock,
                    onClearSelection = { viewModel.selectBlock(null) },
                    translationState = translationState,
                    targetLanguage   = targetLanguage,
                    languages        = viewModel.supportedLanguages,
                    isLiveScan       = isLiveScan,
                    onFreeze         = { viewModel.freezeAndCapture() },
                    onTranslate      = { viewModel.translateCurrentText() },
                    onSpeak          = { text, lang -> viewModel.speak(text, lang) },
                    onLangChange     = { viewModel.setTargetLanguage(it) }
                )
            }
        }

        // ── Floating Error Alerts ───────────────────────────────────────────────
        if (scanState is OcrScanState.Error) {
            OcrErrorBanner(
                message  = (scanState as OcrScanState.Error).message,
                onDismiss = { viewModel.dismissError() },
                modifier  = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CAMERA PREVIEW
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun OcrCameraPreview(
    modifier:             Modifier,
    onCameraReady:        (Camera) -> Unit,
    onPreviewViewCreated: (PreviewView) -> Unit,
    onFrame:              suspend (android.media.Image, Int, Int, Int) -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope          = rememberCoroutineScope()
    val executor       = remember { Executors.newSingleThreadExecutor() }
    val processing     = remember { AtomicBoolean(false) }

    AndroidView(
        modifier = modifier,
        factory  = { ctx ->
            val preview = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            onPreviewViewCreated(preview)
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider   = future.get()
                provider.unbindAll()

                val previewUC  = Preview.Builder().build()
                    .also { it.setSurfaceProvider(preview.surfaceProvider) }

                val analysisUC = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                analysisUC.setAnalyzer(executor) { proxy ->
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

                val cam = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUC, analysisUC
                )
                onCameraReady(cam)
            }, ContextCompat.getMainExecutor(ctx))
            preview
        }
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// GOOGLE LENS RESIZABLE VIEWFINDER CROP TARGET
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResizableSelectorViewfinder(
    rect:         Rect,
    onRectChange: (Rect) -> Unit,
    modifier:     Modifier = Modifier
) {
    val handleSize = 44.dp
    val strokeWidth = 2.5.dp

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // Draggable box translation (moving the entire viewfinder crop rect)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newLeft = (rect.left + dragAmount.x).coerceIn(10f, widthPx - rect.width - 10f)
                        val newTop = (rect.top + dragAmount.y).coerceIn(80f, heightPx - rect.height - 180f)
                        onRectChange(Rect(newLeft, newTop, newLeft + rect.width, newTop + rect.height))
                    }
                }
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Cutout overlay
            val outerPath = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
            }
            val innerPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = rect,
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                    )
                )
            }
            val punchedPath = Path.combine(PathOperation.Difference, outerPath, innerPath)
            drawPath(punchedPath, Color.Black.copy(alpha = 0.50f))

            // Neon targeted frame border
            drawRoundRect(
                color = NeonBlueExact,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = strokeWidth.toPx())
            )
        }

        // Draggable corner selection handles
        // Top-Left corner
        Box(
            modifier = Modifier
                .offset(x = (rect.left - handleSize.value / 2).dp, y = (rect.top - handleSize.value / 2).dp)
                .size(handleSize)
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newLeft = (rect.left + dragAmount.x).coerceAtMost(rect.right - 180f).coerceAtLeast(30f)
                        val newTop = (rect.top + dragAmount.y).coerceAtMost(rect.bottom - 180f).coerceAtLeast(100f)
                        onRectChange(Rect(newLeft, newTop, rect.right, rect.bottom))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val s = size.width
                drawLine(NeonPink, Offset(s/2, s/2), Offset(s, s/2), 4.dp.toPx(), StrokeCap.Round)
                drawLine(NeonPink, Offset(s/2, s/2), Offset(s/2, s), 4.dp.toPx(), StrokeCap.Round)
            }
        }

        // Top-Right corner
        Box(
            modifier = Modifier
                .offset(x = (rect.right - handleSize.value / 2).dp, y = (rect.top - handleSize.value / 2).dp)
                .size(handleSize)
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newRight = (rect.right + dragAmount.x).coerceAtLeast(rect.left + 180f).coerceAtMost(widthPx - 30f)
                        val newTop = (rect.top + dragAmount.y).coerceAtMost(rect.bottom - 180f).coerceAtLeast(100f)
                        onRectChange(Rect(rect.left, newTop, newRight, rect.bottom))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val s = size.width
                drawLine(NeonPink, Offset(s/2, s/2), Offset(0f, s/2), 4.dp.toPx(), StrokeCap.Round)
                drawLine(NeonPink, Offset(s/2, s/2), Offset(s/2, s), 4.dp.toPx(), StrokeCap.Round)
            }
        }

        // Bottom-Left corner
        Box(
            modifier = Modifier
                .offset(x = (rect.left - handleSize.value / 2).dp, y = (rect.bottom - handleSize.value / 2).dp)
                .size(handleSize)
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newLeft = (rect.left + dragAmount.x).coerceAtMost(rect.right - 180f).coerceAtLeast(30f)
                        val newBottom = (rect.bottom + dragAmount.y).coerceAtLeast(rect.top + 180f).coerceAtMost(heightPx - 180f)
                        onRectChange(Rect(newLeft, rect.top, rect.right, newBottom))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val s = size.width
                drawLine(NeonPink, Offset(s/2, s/2), Offset(s, s/2), 4.dp.toPx(), StrokeCap.Round)
                drawLine(NeonPink, Offset(s/2, s/2), Offset(s/2, 0f), 4.dp.toPx(), StrokeCap.Round)
            }
        }

        // Bottom-Right corner
        Box(
            modifier = Modifier
                .offset(x = (rect.right - handleSize.value / 2).dp, y = (rect.bottom - handleSize.value / 2).dp)
                .size(handleSize)
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newRight = (rect.right + dragAmount.x).coerceAtLeast(rect.left + 180f).coerceAtMost(widthPx - 30f)
                        val newBottom = (rect.bottom + dragAmount.y).coerceAtLeast(rect.top + 180f).coerceAtMost(heightPx - 180f)
                        onRectChange(Rect(rect.left, rect.top, newRight, newBottom))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val s = size.width
                drawLine(NeonPink, Offset(s/2, s/2), Offset(0f, s/2), 4.dp.toPx(), StrokeCap.Round)
                drawLine(NeonPink, Offset(s/2, s/2), Offset(s/2, 0f), 4.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// TEXT SELECTOR OVERLAY (GOOGLE LENS HIGHLIGHT CANVAS)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TextSelectorOverlay(
    blocks:           List<RecognizedBlock>,
    selectedBlock:    RecognizedBlock?,
    frameWidth:       Int,
    frameHeight:      Int,
    viewfinderLeft:   Float,
    viewfinderTop:    Float,
    viewfinderRight:  Float,
    viewfinderBottom: Float,
    onBlockClick:     (RecognizedBlock?) -> Unit,
    modifier:         Modifier
) {
    val highlightAlpha by animateFloatAsState(
        targetValue  = if (blocks.isEmpty()) 0f else 0.85f,
        animationSpec = tween(300),
        label         = "highlightAlpha"
    )

    // Pulsing selection indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_lens")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label         = "pulseGlow"
    )

    var canvasWidth by remember { mutableStateOf(1) }
    var canvasHeight by remember { mutableStateOf(1) }
    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                canvasWidth = coordinates.size.width
                canvasHeight = coordinates.size.height
            }
            .pointerInput(blocks, frameWidth, frameHeight) {
                detectTapGestures { offset ->
                    val screenW = canvasWidth.toFloat()
                    val screenH = canvasHeight.toFloat()
                    val frameW = frameWidth.toFloat().coerceAtLeast(1f)
                    val frameH = frameHeight.toFloat().coerceAtLeast(1f)

                    val scale = maxOf(screenW / frameW, screenH / frameH)
                    val scaledW = frameW * scale
                    val scaledH = frameH * scale
                    val offsetX = (screenW - scaledW) / 2f
                    val offsetY = (screenH - scaledH) / 2f

                    // Click coordinates projected back to frame space
                    val clickXNorm = (offset.x - offsetX) / scaledW
                    val clickYNorm = (offset.y - offsetY) / scaledH

                    // Tapping blocks strictly inside viewfinder
                    val clickedBlock = blocks.firstOrNull { block ->
                        clickXNorm >= block.left && clickXNorm <= block.right &&
                        clickYNorm >= block.top && clickYNorm <= block.bottom &&
                        block.left >= viewfinderLeft && block.right <= viewfinderRight &&
                        block.top >= viewfinderTop && block.bottom <= viewfinderBottom
                    }
                    if (clickedBlock != null) {
                        if (selectedBlock == clickedBlock) {
                            onBlockClick(null)
                        } else {
                            onBlockClick(clickedBlock)
                        }
                    } else {
                        onBlockClick(null)
                    }
                }
            }
    ) {
        blocks.forEach { block ->
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
            val left   = block.left   * scaledW + offsetX
            val top    = block.top    * scaledH + offsetY
            val right  = block.right  * scaledW + offsetX
            val bottom = block.bottom * scaledH + offsetY
            val w = right - left
            val h = bottom - top

            val isInsideViewfinder = block.left >= viewfinderLeft && block.right <= viewfinderRight &&
                                     block.top >= viewfinderTop && block.bottom <= viewfinderBottom

            val isSelected = selectedBlock == block

            // Render block highlight overlay
            val baseColor = if (isSelected) NeonPink else NeonCyan
            val alphaFill = if (isSelected) 0.28f else if (isInsideViewfinder) 0.16f else 0.04f
            
            drawRect(
                color   = baseColor.copy(alpha = alphaFill * highlightAlpha),
                topLeft = Offset(left, top),
                size    = Size(w, h)
            )

            // Neon borders for active targeted texts
            if (isInsideViewfinder || isSelected) {
                val borderColor = if (isSelected) {
                    NeonPink.copy(alpha = (0.7f + 0.3f * pulseGlow) * highlightAlpha)
                } else {
                    NeonCyan.copy(alpha = 0.5f * highlightAlpha)
                }
                val borderWidth = if (isSelected) 2.2f else 1.2f

                drawRect(
                    color       = borderColor,
                    topLeft     = Offset(left, top),
                    size        = Size(w, h),
                    style       = Stroke(width = borderWidth)
                )

                // Lens style crop corners around selected text
                if (isSelected) {
                    val cLen = (w * 0.15f).coerceAtMost(10f)
                    val cStr = 2.8f
                    // TL
                    drawLine(NeonPink, Offset(left, top + cLen), Offset(left, top), cStr)
                    drawLine(NeonPink, Offset(left, top), Offset(left + cLen, top), cStr)
                    // TR
                    drawLine(NeonPink, Offset(right - cLen, top), Offset(right, top), cStr)
                    drawLine(NeonPink, Offset(right, top), Offset(right, top + cLen), cStr)
                    // BL
                    drawLine(NeonPink, Offset(left, bottom - cLen), Offset(left, bottom), cStr)
                    drawLine(NeonPink, Offset(left, bottom), Offset(left + cLen, bottom), cStr)
                    // BR
                    drawLine(NeonPink, Offset(right - cLen, bottom), Offset(right, bottom), cStr)
                    drawLine(NeonPink, Offset(right, bottom), Offset(right, bottom - cLen), cStr)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// TOP APP HUD BAR
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun OcrTopBar(
    onBack: () -> Unit,
    flashEnabled: Boolean,
    isLive: Boolean,
    onFlash: () -> Unit,
    onGallery: () -> Unit,
    onResume: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back pill
        TopBarIcon(icon = "←", onClick = onBack)

        // Premium Neon HUD Title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "OCR TRANSLATOR",
                style      = MaterialTheme.typography.titleMedium,
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                "Google Lens Text Selection",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.Medium
            )
        }

        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isLive) TopBarIcon("▶ Live", onClick = onResume, isText = true)
            TopBarIcon(if (flashEnabled) "⚡" else "🔦", onClick = onFlash)
            TopBarIcon("🖼️", onClick = onGallery)
        }
    }
}

@Composable
private fun TopBarIcon(icon: String, onClick: () -> Unit, isText: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.50f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = if (isText) 12.dp else 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            icon,
            fontSize   = if (isText) 11.sp else 16.sp,
            color      = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SCANNING HINT LABEL
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScanHintLabel(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "hint")
    val alpha by infiniteTransition.animateFloat(
        initialValue  = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label         = "hintAlpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.60f))
            .border(0.5.dp, NeonBlueExact.copy(0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "🔎 Target text in viewfinder",
            color = Color.White.copy(alpha = alpha),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// FROZEN dimming FRAME OVERLAY
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun FrozenOverlay(modifier: Modifier) {
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.35f))
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// BOTTOM SLIDING CONTROL PANEL
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun OcrBottomPanel(
    resultText:       String,
    detectedLangName: String,
    detectedLangCode: String,
    blockCount:       Int,
    selectedBlock:    RecognizedBlock?,
    onClearSelection: () -> Unit,
    translationState: TranslationState,
    targetLanguage:   String,
    languages:        List<String>,
    isLiveScan:       Boolean,
    onFreeze:         () -> Unit,
    onTranslate:      () -> Unit,
    onSpeak:          (String, String) -> Unit,
    onLangChange:     (String) -> Unit
) {
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xDC060D1F), Color(0xF502040A))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(NeonBlueExact.copy(0.4f), NeonPurple.copy(0.3f))),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .width(44.dp).height(4.dp)
                .clip(CircleShape)
                .background(NeonBlueExact.copy(0.4f))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))

        // Info Badges Row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                DetectedLangBadge(detectedLangName)
                if (selectedBlock != null) {
                    NeonBadge("Selection Focus", NeonPink)
                } else {
                    NeonBadge("Viewfinder Targeted", NeonCyan)
                }
            }

            if (isLiveScan) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(NeonBlueExact, NeonPurple)))
                        .clickable(onClick = onFreeze)
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        "📸 Capture Frame",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Detected Input Content Card
        val displayTitle = if (selectedBlock != null) "Selected Text Block" else "Targeted Text"
        val accentColor = if (selectedBlock != null) NeonPink else NeonBlueExact

        DetectedTextCard(
            text        = resultText,
            title       = displayTitle,
            accentColor = accentColor,
            onSpeak     = { onSpeak(resultText, detectedLangCode) },
            onCopy      = { copyToClipboard(ctx, resultText) },
            showClear   = selectedBlock != null,
            onClear     = onClearSelection
        )

        Spacer(Modifier.height(12.dp))

        // Language Select + Action Button
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            LanguageDropdown(
                selected  = targetLanguage,
                languages = languages,
                onChange  = onLangChange,
                modifier  = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(NeonBlueExact, NeonPurple)))
                    .clickable(onClick = onTranslate)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Translate",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Translation detail results with custom Meaning Cards
        AnimatedVisibility(
            visible = translationState !is TranslationState.Idle,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                TranslationResultCard(
                    state    = translationState,
                    onSpeak  = { text, lang -> onSpeak(text, lang) },
                    onCopy   = { text -> copyToClipboard(ctx, text) },
                    onShare  = { text -> shareText(ctx, text) }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// INPUT TEXT PREVIEW CARD
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetectedTextCard(
    text: String,
    title: String,
    accentColor: Color,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    showClear: Boolean = false,
    onClear: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, accentColor.copy(0.20f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (showClear) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonPink.copy(0.12f))
                                .border(0.5.dp, NeonPink.copy(0.35f), RoundedCornerShape(8.dp))
                                .clickable(onClick = onClear)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Clear Select ✕",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonPink,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallIconBtn("🔊", onClick = onSpeak)
                    SmallIconBtn("📋", onClick = onCopy)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text       = text,
                style      = MaterialTheme.typography.bodyMedium,
                color      = Color.White.copy(0.95f),
                maxLines   = 4,
                overflow   = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// TRANSLATION SUCCESS DETAILS CARD WITH NATIVE ORIGINAL LANGUAGE MEANING (CRITICAL)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TranslationResultCard(
    state:   TranslationState,
    onSpeak: (String, String) -> Unit,
    onCopy:  (String) -> Unit,
    onShare: (String) -> Unit
) {
    when (state) {
        is TranslationState.Downloading, is TranslationState.Translating -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeonPurple.copy(0.08f))
                    .border(1.dp, NeonPurple.copy(0.3f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color    = NeonPurple,
                        strokeWidth = 2.dp
                    )
                    Text(
                        if (state is TranslationState.Downloading)
                            "⬇ Downloading language modules..."
                        else "Translating focused text...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonPurple
                    )
                }
            }
        }

        is TranslationState.Success -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(0.06f))
                    .border(1.dp, NeonPurple.copy(0.35f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    // Header badges
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            NeonBadge(state.sourceLang, NeonBlueExact)
                            Text("→", color = Color.White.copy(0.4f), fontSize = 12.sp)
                            NeonBadge(state.targetLang, NeonPurple)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SmallIconBtn("🔊") { onSpeak(state.translatedText, "en") }
                            SmallIconBtn("📋") { onCopy(state.translatedText) }
                            SmallIconBtn("↑")  { onShare(state.translatedText) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    // Translated Main Text Card
                    Text(
                        text      = state.translatedText,
                        style     = MaterialTheme.typography.bodyLarge,
                        color     = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp
                    )

                    // ── Premium AI Semantic Breakdown ──────────────────────────────────
                    state.semanticMeaning?.let { meaning ->
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(Modifier.height(10.dp))
                        SemanticMeaningCard(
                            meaning        = meaning,
                            sourceLangName = state.sourceLang,
                            targetLangName = state.targetLang
                        )
                    } ?: state.nativeMeaning?.let { meaning ->
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(Modifier.height(10.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("💡", fontSize = 12.sp)
                            Text(
                                "Meaning in ${state.sourceLang}:",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonBlueExact,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = meaning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        is TranslationState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeonPink.copy(0.08f))
                    .border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.Top
                ) {
                    Text("⚠️", fontSize = 18.sp)
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonPink
                    )
                }
            }
        }

        else -> {}
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// LANGUAGE DROPDOWN SELECTOR
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LanguageDropdown(
    selected:  String,
    languages: List<String>,
    onChange:  (String) -> Unit,
    modifier:  Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(0.06f))
                .border(1.dp, NeonBlueExact.copy(0.3f), RoundedCornerShape(24.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "→ $selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text("▼", color = NeonBlueExact, fontSize = 10.sp)
            }
        }
        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF0F172A)).border(0.5.dp, NeonBlueExact.copy(0.3f), RoundedCornerShape(8.dp))
        ) {
            languages.forEach { lang ->
                DropdownMenuItem(
                    text    = { Text(lang, color = Color.White) },
                    onClick = { onChange(lang); expanded = false },
                    trailingIcon = if (lang == selected) ({ Text("✓", color = NeonBlueExact) }) else null
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// WIDGET HELPERS
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetectedLangBadge(name: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NeonBlueExact.copy(0.12f))
            .border(1.dp, NeonBlueExact.copy(0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            PulsingIndicatorDot(NeonBlueExact)
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                color = NeonBlueExact,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PulsingIndicatorDot(color: Color) {
    val scale by rememberInfiniteTransition(label = "indicator").animateFloat(
        initialValue  = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label         = "dotScale"
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun SmallIconBtn(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 12.sp)
    }
}

@Composable
private fun NoCameraPermissionView(onRequest: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.background(DeepNavy), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("📷", fontSize = 56.sp)
            Text("Camera permission required", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(
                "Grant permission to translate text from real world images",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            NeonButton("Grant Permission", onClick = onRequest, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}

@Composable
private fun OcrErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NeonPink.copy(0.15f))
            .border(1.dp, NeonPink.copy(0.4f), RoundedCornerShape(12.dp))
            .clickable(onClick = onDismiss)
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("⚠️")
            Text(message, style = MaterialTheme.typography.bodySmall, color = NeonPink, modifier = Modifier.weight(1f))
            Text("✕", color = NeonPink, fontSize = 12.sp)
        }
    }
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("OCR Text", text))
}

private fun shareText(ctx: Context, text: String) {
    ctx.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "Share via"
        )
    )
}
