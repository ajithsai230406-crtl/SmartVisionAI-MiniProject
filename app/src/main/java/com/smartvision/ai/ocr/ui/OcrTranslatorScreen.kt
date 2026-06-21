package com.smartvision.ai.ocr.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.data.local.OcrScanEntity
import com.smartvision.ai.ocr.domain.*
import com.smartvision.ai.ocr.presentation.OcrTranslatorViewModel
import com.smartvision.ai.translator.domain.Language
import com.smartvision.ai.translator.domain.ALL_LANGUAGES
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private val NeonBlueExact = NeonBlue

// ══════════════════════════════════════════════════════════════════════════════
// OCR TRANSLATOR — GOOGLE LENS INSPIRED UI
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalPermissionsApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    val overlayParagraphs by viewModel.overlayParagraphs.collectAsStateWithLifecycle()
    val selectedBlock     by viewModel.selectedBlock.collectAsStateWithLifecycle()
    val selectedParagraph by viewModel.selectedParagraph.collectAsStateWithLifecycle()
    val frozenResult      by viewModel.frozenResult.collectAsStateWithLifecycle()
    val targetLanguage    by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val isLiveScan        by viewModel.isLiveScan.collectAsStateWithLifecycle()
    val flashEnabled      by viewModel.flashEnabled.collectAsStateWithLifecycle()
    val activeTab         by viewModel.activeTab.collectAsStateWithLifecycle()
    val detectedScript    by viewModel.detectedScript.collectAsStateWithLifecycle()
    val scanQuality       by viewModel.scanQuality.collectAsStateWithLifecycle()
    val scanHistory       by viewModel.scanHistory.collectAsStateWithLifecycle()

    val sourceLanguage    by viewModel.sourceLanguage.collectAsStateWithLifecycle()
    val ocrMode           by viewModel.ocrMode.collectAsStateWithLifecycle()
    val blockTranslations by viewModel.blockTranslations.collectAsStateWithLifecycle()
    val isTranslatingBlocks by viewModel.isTranslatingBlocks.collectAsStateWithLifecycle()
    val targetLanguageCode  by viewModel.targetLanguageCode.collectAsStateWithLifecycle()

    var showSourcePicker by remember { mutableStateOf(false) }

    val cameraRef      = remember { mutableStateOf<Camera?>(null) }
    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(flashEnabled) {
        cameraRef.value?.cameraControl?.enableTorch(flashEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeaking()
        }
    }

    val ctx = LocalContext.current
    val galleryLauncher = rememberGalleryPickerLauncher { uri ->
        viewModel.analyzeFromGallery(ctx, uri)
    }

    // Resizable viewfinder crop rect
    var manualRectInitialized by remember { mutableStateOf(false) }
    var manualRect by remember { mutableStateOf(Rect(60f, 300f, 1020f, 1100f)) }
    var canvasWidth  by remember { mutableStateOf(1) }
    var canvasHeight by remember { mutableStateOf(1) }

    LaunchedEffect(canvasWidth, canvasHeight) {
        if (canvasWidth > 1 && canvasHeight > 1 && !manualRectInitialized) {
            val w = canvasWidth * 0.82f
            val h = canvasHeight * 0.38f
            val l = (canvasWidth - w) / 2f
            val t = (canvasHeight - h) / 2f
            manualRect = Rect(l, t, l + w, t + h)
            manualRectInitialized = true
        }
    }

    val activeState = scanState as? OcrScanState.TextDetected
    val frameW = activeState?.frameWidth  ?: 1080
    val frameH = activeState?.frameHeight ?: 1920

    val density = LocalDensity.current
    val targetRect = remember(selectedBlock, canvasWidth, canvasHeight, frameW, frameH, manualRect) {
        val block = selectedBlock
        if (block != null && canvasWidth > 1 && canvasHeight > 1) {
            val sW = canvasWidth.toFloat(); val sH = canvasHeight.toFloat()
            val fW = frameW.toFloat().coerceAtLeast(1f); val fH = frameH.toFloat().coerceAtLeast(1f)
            val scale = maxOf(sW / fW, sH / fH)
            val ox = (sW - fW * scale) / 2f; val oy = (sH - fH * scale) / 2f
            val pad = 14.dp.value * density.density
            Rect(
                (block.left  * fW * scale + ox - pad).coerceAtLeast(10f),
                (block.top   * fH * scale + oy - pad).coerceAtLeast(80f),
                (block.right * fW * scale + ox + pad).coerceAtMost(sW - 10f),
                (block.bottom* fH * scale + oy + pad).coerceAtMost(sH - 200f)
            )
        } else manualRect
    }

    val animLeft   by animateFloatAsState(targetRect.left,   spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),  label = "l")
    val animTop    by animateFloatAsState(targetRect.top,    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),  label = "t")
    val animRight  by animateFloatAsState(targetRect.right,  spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),  label = "r")
    val animBottom by animateFloatAsState(targetRect.bottom, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),  label = "b")
    val cropRect = Rect(animLeft, animTop, animRight, animBottom)

    val activeBlocks = if (isLiveScan) overlayBlocks else (frozenResult?.blocks ?: emptyList())
    val insideBlocks = remember(activeBlocks, cropRect, canvasWidth, canvasHeight, frameW, frameH) {
        if (canvasWidth <= 1 || canvasHeight <= 1) return@remember emptyList<RecognizedBlock>()
        val sW = canvasWidth.toFloat(); val sH = canvasHeight.toFloat()
        val fW = frameW.toFloat().coerceAtLeast(1f); val fH = frameH.toFloat().coerceAtLeast(1f)
        val scale = maxOf(sW / fW, sH / fH)
        val ox = (sW - fW * scale) / 2f; val oy = (sH - fH * scale) / 2f
        activeBlocks.filter { b ->
            val bl = b.left * fW * scale + ox; val bt = b.top * fH * scale + oy
            val br = b.right * fW * scale + ox; val bb = b.bottom * fH * scale + oy
            bl < cropRect.right && cropRect.left < br && bt < cropRect.bottom && cropRect.top < bb
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepNavy)) {
        // ── Camera Preview ───────────────────────────────────────────────────
        if (cameraPermission.status.isGranted) {
            OcrCameraPreview(
                modifier             = Modifier.fillMaxSize(),
                onCameraReady        = { cameraRef.value = it },
                onPreviewViewCreated = { previewViewRef.value = it },
                onFrame              = { image, rotation, w, h ->
                    viewModel.analyzeFrame(image, rotation, w, h)
                }
            )

            // Viewfinder + text overlays
            Box(modifier = Modifier.fillMaxSize().onGloballyPositioned {
                canvasWidth  = it.size.width
                canvasHeight = it.size.height
            }) {
                ResizableSelectorViewfinder(
                    rect = cropRect,
                    onRectChange = { manualRect = it; if (selectedBlock != null) viewModel.selectBlock(null) },
                    modifier = Modifier.fillMaxSize()
                )

                if (!isLiveScan && ocrMode == OcrMode.TRANSLATE && blockTranslations.isNotEmpty()) {
                    GoogleLensTranslateOverlay(
                        blocks       = insideBlocks,
                        blockTranslations = blockTranslations,
                        frameWidth   = frameW,
                        frameHeight  = frameH,
                        modifier     = Modifier.fillMaxSize()
                    )
                } else if (activeBlocks.isNotEmpty()) {
                    TextSelectorOverlay(
                        blocks        = insideBlocks,
                        insideBlocks  = insideBlocks,
                        selectedBlock = selectedBlock,
                        frameWidth    = frameW,
                        frameHeight   = frameH,
                        onBlockClick  = { viewModel.selectBlock(it) },
                        modifier      = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            NoCameraPermissionView(
                onRequest = { cameraPermission.launchPermissionRequest() },
                modifier  = Modifier.fillMaxSize()
            )
        }

        // ── Top HUD Bar ──────────────────────────────────────────────────────
        OcrTopBar(
            onBack       = onBack,
            flashEnabled = flashEnabled,
            isLive       = isLiveScan,
            detectedScript = detectedScript,
            sourceLanguage = sourceLanguage,
            onSourceLanguageClick = { showSourcePicker = true },
            onFlash      = { viewModel.toggleFlash() },
            onGallery    = { galleryLauncher() },
            onResume     = { viewModel.resumeLiveScan() }
        )

        // ── Script/Language pill ─────────────────────────────────────────────
        val liveResult = (scanState as? OcrScanState.TextDetected)?.result
        if (isLiveScan && liveResult != null) {
            LiveLanguagePill(
                langName = liveResult.detectedLanguageName,
                script   = liveResult.detectedScript,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp)
            )
        }

        // ── Scan hint label ──────────────────────────────────────────────────
        if (isLiveScan) {
            val showWarn = scanState is OcrScanState.NoText || insideBlocks.isEmpty()
            ScanHintLabel(
                message   = if (showWarn) "⚠ Point camera towards text" else "🔎 Adjust viewfinder over text",
                isWarning = showWarn,
                modifier  = Modifier.align(Alignment.Center)
            )
        }

        // ── Frozen overlay dimming ───────────────────────────────────────────
        if (!isLiveScan && frozenResult != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.30f)))
        }

        // ── Quality ring + Capture button ────────────────────────────────────
        if (isLiveScan) {
            QualityCapture(
                quality    = scanQuality,
                hasText    = insideBlocks.isNotEmpty(),
                modifier   = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                onCapture  = {
                    if (insideBlocks.isEmpty()) viewModel.showError("Point camera towards text")
                    else {
                        val bmp = previewViewRef.value?.bitmap
                        viewModel.freezeAndCapture(bmp, insideBlocks)
                        viewModel.translateCurrentText()
                    }
                }
            )
        }

        // ── Bottom Panel (tabs) ──────────────────────────────────────────────
        AnimatedVisibility(
            visible  = !isLiveScan,
            enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val result = frozenResult ?: (scanState as? OcrScanState.TextDetected)?.result
            if (result != null) {
                val viewfinderText = insideBlocks.joinToString(" ") { it.text }.ifBlank { result.fullText }
                val displayText    = selectedParagraph?.text ?: selectedBlock?.text ?: viewfinderText

                OcrBottomPanel(
                    result           = result,
                    displayText      = displayText,
                    selectedBlock    = selectedBlock,
                    selectedParagraph = selectedParagraph,
                    translationState = translationState,
                    targetLanguage   = targetLanguage,
                    targetLangCode   = targetLanguageCode,
                    languages        = viewModel.supportedLanguages,
                    activeTab        = activeTab,
                    scanHistory      = scanHistory,
                    onTabChange      = { viewModel.setActiveTab(it) },
                    onClearSelection = { viewModel.selectBlock(null); viewModel.selectParagraph(null) },
                    onTranslate      = { viewModel.translateCurrentText() },
                    onSpeak          = { text, lang -> viewModel.speak(text, lang) },
                    onLangChange     = { viewModel.setTargetLanguage(it) },
                    onSpeakOriginal  = { viewModel.speakOriginalText() },
                    onDeleteHistory  = { viewModel.deleteScanFromHistory(it) },
                    onResume         = { viewModel.resumeLiveScan() }
                )
            }
        }

        // ── Error Banner ─────────────────────────────────────────────────────
        if (scanState is OcrScanState.Error) {
            OcrErrorBanner(
                message   = (scanState as OcrScanState.Error).message,
                onDismiss = { viewModel.dismissError() },
                modifier  = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
            )
        }
        // ── Mode Selector ────────────────────────────────────────────────────
        if (isLiveScan) {
            OcrModeSelector(
                activeMode   = ocrMode,
                onModeChange = { viewModel.setOcrMode(it) },
                modifier     = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 130.dp)
            )
        }

        // ── Source Language Picker Sheet ─────────────────────────────────────
        AnimatedVisibility(
            visible  = showSourcePicker,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            OcrSourceLanguagePickerSheet(
                current   = sourceLanguage,
                onSelect  = { viewModel.setSourceLanguage(it) },
                onDismiss = { showSourcePicker = false }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CAMERA PREVIEW — AUTO-FOCUS ENABLED
// ══════════════════════════════════════════════════════════════════════════════

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
    val executor       = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    val processing     = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            executor.shutdown()
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    future.get().unbindAll()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

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
                val provider = future.get()
                provider.unbindAll()

                val previewUC = Preview.Builder().build()
                    .also { it.setSurfaceProvider(preview.surfaceProvider) }

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            android.util.Size(1920, 1080),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    ).build()

                val analysisUC = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                analysisUC.setAnalyzer(executor) { proxy ->
                    val img = proxy.image
                    if (img != null && processing.compareAndSet(false, true)) {
                        scope.launch(kotlinx.coroutines.Dispatchers.Default) {
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

                // ── Enable continuous auto-focus ─────────────────────────────
                val factory = preview.meteringPointFactory
                val point   = factory.createPoint(0.5f, 0.5f)
                val action  = FocusMeteringAction.Builder(point)
                    .addPoint(point, FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB)
                    .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                cam.cameraControl.startFocusAndMetering(action)

                onCameraReady(cam)
            }, ContextCompat.getMainExecutor(ctx))
            preview
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// RESIZABLE VIEWFINDER
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ResizableSelectorViewfinder(
    rect:         Rect,
    onRectChange: (Rect) -> Unit,
    modifier:     Modifier = Modifier
) {
    val handleSize = 48.dp
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()

        // Whole-box drag to move
        Box(modifier = Modifier.fillMaxSize().pointerInput(rect) {
            detectDragGestures { change, drag ->
                change.consume()
                val nl = (rect.left + drag.x).coerceIn(10f, wPx - rect.width - 10f)
                val nt = (rect.top  + drag.y).coerceIn(80f, hPx - rect.height - 200f)
                onRectChange(Rect(nl, nt, nl + rect.width, nt + rect.height))
            }
        })

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Dimming outside
            val outer = Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }
            val inner = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(18.dp.toPx()))) }
            drawPath(Path.combine(PathOperation.Difference, outer, inner), Color.Black.copy(0.52f))

            // Animated neon border
            drawRoundRect(
                color        = NeonBlueExact,
                topLeft      = rect.topLeft,
                size         = rect.size,
                cornerRadius = CornerRadius(18.dp.toPx()),
                style        = Stroke(width = 2.dp.toPx())
            )

            // Visible handle circles
            val handleRadius = 6.dp.toPx()
            drawCircle(NeonBlueExact, handleRadius, rect.topLeft)
            drawCircle(NeonBlueExact, handleRadius, rect.topRight)
            drawCircle(NeonBlueExact, handleRadius, rect.bottomLeft)
            drawCircle(NeonBlueExact, handleRadius, rect.bottomRight)

            // Corner accent marks
            val cLen = 22.dp.toPx(); val cStr = 3.5f
            val p = NeonCyan
            listOf(
                Offset(rect.left, rect.top)    to Pair(Offset(rect.left + cLen, rect.top),    Offset(rect.left, rect.top + cLen)),
                Offset(rect.right, rect.top)   to Pair(Offset(rect.right - cLen, rect.top),   Offset(rect.right, rect.top + cLen)),
                Offset(rect.left, rect.bottom) to Pair(Offset(rect.left + cLen, rect.bottom), Offset(rect.left, rect.bottom - cLen)),
                Offset(rect.right, rect.bottom) to Pair(Offset(rect.right - cLen, rect.bottom), Offset(rect.right, rect.bottom - cLen))
            ).forEach { (corner, lines) ->
                drawLine(p, corner, lines.first,  cStr, cap = StrokeCap.Round)
                drawLine(p, corner, lines.second, cStr, cap = StrokeCap.Round)
            }
        }

        // Corner resize handles (invisible hit targets)
        CornerHandle(rect.left, rect.top, handleSize, rect) { d ->
            val nl = (rect.left + d.x).coerceAtMost(rect.right - 160f).coerceAtLeast(30f)
            val nt = (rect.top  + d.y).coerceAtMost(rect.bottom - 160f).coerceAtLeast(100f)
            onRectChange(Rect(nl, nt, rect.right, rect.bottom))
        }
        CornerHandle(rect.right, rect.top, handleSize, rect) { d ->
            val nr = (rect.right + d.x).coerceAtLeast(rect.left + 160f).coerceAtMost(wPx - 30f)
            val nt = (rect.top   + d.y).coerceAtMost(rect.bottom - 160f).coerceAtLeast(100f)
            onRectChange(Rect(rect.left, nt, nr, rect.bottom))
        }
        CornerHandle(rect.left, rect.bottom, handleSize, rect) { d ->
            val nl = (rect.left   + d.x).coerceAtMost(rect.right - 160f).coerceAtLeast(30f)
            val nb = (rect.bottom + d.y).coerceAtLeast(rect.top + 160f).coerceAtMost(hPx - 200f)
            onRectChange(Rect(nl, rect.top, rect.right, nb))
        }
        CornerHandle(rect.right, rect.bottom, handleSize, rect) { d ->
            val nr = (rect.right  + d.x).coerceAtLeast(rect.left + 160f).coerceAtMost(wPx - 30f)
            val nb = (rect.bottom + d.y).coerceAtLeast(rect.top + 160f).coerceAtMost(hPx - 200f)
            onRectChange(Rect(rect.left, rect.top, nr, nb))
        }
    }
}

@Composable
private fun CornerHandle(
    x: Float,
    y: Float,
    handleSize: Dp,
    rect: Rect,
    onDrag: (Offset) -> Unit
) {
    val density = LocalDensity.current
    val handleSizePx = with(density) { handleSize.toPx() }
    Box(modifier = Modifier
        .offset { IntOffset((x - handleSizePx / 2).toInt(), (y - handleSizePx / 2).toInt()) }
        .size(handleSize)
        .pointerInput(rect) {
            detectDragGestures { c, d -> c.consume(); onDrag(d) }
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// TEXT SELECTOR OVERLAY
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TextSelectorOverlay(
    blocks:       List<RecognizedBlock>,
    insideBlocks: List<RecognizedBlock>,
    selectedBlock: RecognizedBlock?,
    frameWidth:   Int,
    frameHeight:  Int,
    onBlockClick: (RecognizedBlock?) -> Unit,
    modifier:     Modifier
) {
    val haptic = LocalHapticFeedback.current
    val inf    = rememberInfiniteTransition(label = "pulse")
    val pulse  by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), "pulse")
    val alpha  by animateFloatAsState(if (blocks.isEmpty()) 0f else 0.9f, tween(250), label = "alpha")

    var cW by remember { mutableStateOf(1) }
    var cH by remember { mutableStateOf(1) }

    Canvas(modifier = modifier
        .onGloballyPositioned { cW = it.size.width; cH = it.size.height }
        .pointerInput(blocks, insideBlocks, frameWidth, frameHeight) {
            detectTapGestures { tap ->
                val sW = cW.toFloat(); val sH = cH.toFloat()
                val fW = frameWidth.toFloat().coerceAtLeast(1f); val fH = frameHeight.toFloat().coerceAtLeast(1f)
                val sc = maxOf(sW / fW, sH / fH)
                val ox = (sW - fW * sc) / 2f; val oy = (sH - fH * sc) / 2f
                val nx = (tap.x - ox) / (fW * sc); val ny = (tap.y - oy) / (fH * sc)
                val hit = blocks.firstOrNull { b ->
                    nx >= b.left && nx <= b.right && ny >= b.top && ny <= b.bottom && b in insideBlocks
                }
                if (hit != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onBlockClick(if (hit == selectedBlock) null else hit)
            }
        }
    ) {
        val fW = frameWidth.toFloat().coerceAtLeast(1f); val fH = frameHeight.toFloat().coerceAtLeast(1f)
        val sW = size.width; val sH = size.height
        val sc = maxOf(sW / fW, sH / fH)
        val ox = (sW - fW * sc) / 2f; val oy = (sH - fH * sc) / 2f

        blocks.forEach { b ->
            val l = b.left * fW * sc + ox; val t = b.top * fH * sc + oy
            val r = b.right * fW * sc + ox; val bo = b.bottom * fH * sc + oy
            val w = r - l; val h = bo - t

            val inside   = b in insideBlocks
            val selected = b == selectedBlock
            val base = if (selected) NeonPink else NeonCyan

            drawRoundRect(
                color        = base.copy(if (selected) 0.25f * alpha else if (inside) 0.12f * alpha else 0.03f),
                topLeft      = Offset(l, t),
                size         = Size(w, h),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            if (inside || selected) {
                drawRoundRect(
                    color        = base.copy((if (selected) 0.7f + 0.3f * pulse else 0.45f) * alpha),
                    topLeft      = Offset(l, t),
                    size         = Size(w, h),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style        = Stroke(if (selected) 2.5f else 1.3f)
                )
                if (selected) {
                    // Corner bracket marks
                    val cLen = minOf(w * 0.18f, 12f); val cSt = 3f
                    drawLine(NeonPink, Offset(l, t + cLen), Offset(l, t), cSt, StrokeCap.Round)
                    drawLine(NeonPink, Offset(l, t), Offset(l + cLen, t), cSt, StrokeCap.Round)
                    drawLine(NeonPink, Offset(r - cLen, t), Offset(r, t), cSt, StrokeCap.Round)
                    drawLine(NeonPink, Offset(r, t), Offset(r, t + cLen), cSt, StrokeCap.Round)
                    drawLine(NeonPink, Offset(l, bo - cLen), Offset(l, bo), cSt, StrokeCap.Round)
                    drawLine(NeonPink, Offset(l, bo), Offset(l + cLen, bo), cSt, StrokeCap.Round)
                    drawLine(NeonPink, Offset(r - cLen, bo), Offset(r, bo), cSt, StrokeCap.Round)
                    drawLine(NeonPink, Offset(r, bo), Offset(r, bo - cLen), cSt, StrokeCap.Round)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// LIVE TRANSLATION OVERLAY (shows translated text floating over detected blocks)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LiveTranslationOverlay(
    blocks:     List<RecognizedBlock>,
    translated: String,
    frameWidth:  Int,
    frameHeight: Int,
    modifier:    Modifier
) {
    var cW by remember { mutableStateOf(1) }
    var cH by remember { mutableStateOf(1) }
    val enter by animateFloatAsState(1f, tween(400), label = "txOverlay")

    Box(modifier = modifier.onGloballyPositioned { cW = it.size.width; cH = it.size.height }) {
        if (blocks.isEmpty() || cW <= 1) return@Box

        val fW = frameWidth.toFloat().coerceAtLeast(1f); val fH = frameHeight.toFloat().coerceAtLeast(1f)
        val sW = cW.toFloat(); val sH = cH.toFloat()
        val sc = maxOf(sW / fW, sH / fH)
        val ox = (sW - fW * sc) / 2f; val oy = (sH - fH * sc) / 2f

        // Place translation label just above the first (topmost) block
        val topBlock = blocks.minByOrNull { it.top } ?: return@Box
        val labelX = topBlock.left * fW * sc + ox
        val labelY = (topBlock.top * fH * sc + oy - 8.dp.value).coerceAtLeast(120f)

        Box(
            modifier = Modifier
                .offset(x = labelX.dp / LocalDensity.current.density, y = labelY.dp / LocalDensity.current.density)
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xE6050816), Color(0xE60A1040))
                    )
                )
                .border(1.dp, NeonPurple.copy(0.6f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .graphicsLayer(alpha = enter)
        ) {
            Text(
                text       = translated,
                style      = MaterialTheme.typography.bodySmall,
                color      = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines   = 3,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TOP HUD BAR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OcrTopBar(
    onBack: () -> Unit,
    flashEnabled: Boolean,
    isLive: Boolean,
    detectedScript: String,
    sourceLanguage: Language,
    onSourceLanguageClick: () -> Unit,
    onFlash: () -> Unit,
    onGallery: () -> Unit,
    onResume: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TopBarIcon("←", onClick = onBack)
            
            // Source Language Pill Trigger Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(0.55f))
                    .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onSourceLanguageClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sourceLanguage.flag, fontSize = 14.sp)
                Text(
                    text = sourceLanguage.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text("▼", color = NeonCyan, fontSize = 8.sp)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "OCR TRANSLATOR",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
            Text(
                if (isLive) "● Live Scanning" else "⏸ Frame Captured",
                style = MaterialTheme.typography.labelSmall,
                color = if (isLive) NeonGreen else NeonCyan,
                fontWeight = FontWeight.Medium
            )
        }

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
            .background(Color.Black.copy(0.55f))
            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = if (isText) 12.dp else 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = if (isText) 11.sp else 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// LIVE LANGUAGE / SCRIPT PILL
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LiveLanguagePill(langName: String, script: String, modifier: Modifier) {
    val inf   = rememberInfiniteTransition(label = "lang")
    val pulse by inf.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), "lp")
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(0.62f))
            .border(1.dp, NeonGreen.copy(0.4f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonGreen.copy(pulse)))
            Text("$langName • $script", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontWeight = FontWeight.Bold)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// QUALITY RING + CAPTURE BUTTON
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QualityCapture(
    quality: Float,
    hasText: Boolean,
    modifier: Modifier,
    onCapture: () -> Unit
) {
    val qualityColor = when {
        quality > 0.75f -> NeonGreen
        quality > 0.45f -> NeonOrange
        else            -> NeonPink
    }
    val inf    = rememberInfiniteTransition(label = "ring")
    val rScale by inf.animateFloat(1f, 1.22f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), "rs")
    val rAlpha by inf.animateFloat(0.5f, 0f,   infiniteRepeatable(tween(1100), RepeatMode.Reverse), "ra")

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Quality label
        if (hasText) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(0.6f))
                    .border(1.dp, qualityColor.copy(0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    when {
                        quality > 0.75f -> "✓ Excellent quality"
                        quality > 0.45f -> "~ Good quality"
                        else            -> "! Poor quality"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = qualityColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Capture button with pulsing ring
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier
                .size(80.dp)
                .graphicsLayer(scaleX = rScale, scaleY = rScale)
                .border(2.dp, (if (hasText) qualityColor else NeonBlueExact).copy(rAlpha), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            if (hasText) listOf(Color.White, Color(0xFFE0F8FF))
                            else listOf(Color(0xFF1A2A4A), Color(0xFF0A1428))
                        )
                    )
                    .border(3.5.dp, if (hasText) NeonCyan else NeonBlueExact.copy(0.5f), CircleShape)
                    .clickable(onClick = onCapture),
                contentAlignment = Alignment.Center
            ) {
                Text("🔍", fontSize = 26.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SCAN HINT LABEL
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ScanHintLabel(message: String, isWarning: Boolean, modifier: Modifier) {
    val inf   = rememberInfiniteTransition(label = "hint")
    val alpha by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), "ha")
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(0.65f))
            .border(1.dp, if (isWarning) NeonPink.copy(0.5f) else NeonCyan.copy(0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            color      = (if (isWarning) NeonPink else Color.White).copy(alpha),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// BOTTOM PANEL — 3-TAB (Text / Translate / History)
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OcrBottomPanel(
    result:           OcrResult,
    displayText:      String,
    selectedBlock:    RecognizedBlock?,
    selectedParagraph: RecognizedParagraph?,
    translationState: TranslationState,
    targetLanguage:   String,
    targetLangCode:   String,
    languages:        List<String>,
    activeTab:        Int,
    scanHistory:      List<OcrScanEntity>,
    onTabChange:      (Int) -> Unit,
    onClearSelection: () -> Unit,
    onTranslate:      () -> Unit,
    onSpeak:          (String, String) -> Unit,
    onLangChange:     (String) -> Unit,
    onSpeakOriginal:  () -> Unit,
    onDeleteHistory:  (String) -> Unit,
    onResume:         () -> Unit
) {
    val ctx = LocalContext.current
    val pagerState = rememberPagerState(initialPage = activeTab) { 3 }
    val scope      = rememberCoroutineScope()

    LaunchedEffect(activeTab) {
        if (pagerState.currentPage != activeTab) pagerState.animateScrollToPage(activeTab)
    }
    LaunchedEffect(pagerState.currentPage) { onTabChange(pagerState.currentPage) }

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val density = LocalDensity.current
    val thresholdPx = remember { with(density) { 100.dp.toPx() } }

    val animatedOffset by animateFloatAsState(
        targetValue = if (isDragging) dragOffsetY else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "sheetOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, animatedOffset.toInt()) }
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Brush.verticalGradient(listOf(Color(0xDC060D1F), Color(0xF502040A))))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(NeonBlueExact.copy(0.4f), NeonPurple.copy(0.3f))),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .navigationBarsPadding()
    ) {
        // Drag header container (large pull-down target)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            isDragging = false
                            if (dragOffsetY > thresholdPx) {
                                onResume()
                            }
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffsetY = 0f
                        }
                    )
                }
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(NeonBlueExact.copy(0.4f))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(10.dp))

            // Language badge + stats row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DetectedLangBadge(result.detectedLanguageName)
                    if (result.detectedScript.isNotBlank()) {
                        NeonBadge(result.detectedScript, NeonCyan)
                    }
                    if (result.hasRotatedText) NeonBadge("Rotated", NeonOrange)
                }
                Text(
                    "${result.paragraphs.size} para · ${result.blocks.size} lines",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.height(10.dp))

            // Tab bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(0.05f))
                    .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("📄 Text", "🌐 Translate", "🕐 History").forEachIndexed { i, label ->
                    val sel = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Brush.horizontalGradient(listOf(NeonBlueExact.copy(0.25f), NeonPurple.copy(0.2f))) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                            .border(if (sel) 1.dp else 0.dp, if (sel) NeonBlueExact.copy(0.4f) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { scope.launch { pagerState.animateScrollToPage(i) } }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sel) Color.White else TextSecondary,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // Pager content
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp)
        ) { page ->
            when (page) {
                0 -> TextTab(
                    result        = result,
                    displayText   = displayText,
                    selectedBlock = selectedBlock,
                    selectedParagraph = selectedParagraph,
                    onClearSelection = onClearSelection,
                    onSpeak       = { onSpeak(displayText, result.detectedLanguage) },
                    onCopy        = { copyToClipboard(ctx, displayText) },
                    modifier      = Modifier.padding(horizontal = 18.dp)
                )
                1 -> TranslateTab(
                    translationState = translationState,
                    targetLanguage   = targetLanguage,
                    targetLangCode   = targetLangCode,
                    languages        = languages,
                    detectedLangCode = result.detectedLanguage,
                    onTranslate      = onTranslate,
                    onLangChange     = onLangChange,
                    onSpeak          = onSpeak,
                    onSpeakOriginal  = onSpeakOriginal,
                    modifier         = Modifier.padding(horizontal = 18.dp)
                )
                2 -> HistoryTab(
                    history   = scanHistory,
                    onDelete  = onDeleteHistory,
                    modifier  = Modifier.padding(horizontal = 18.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 0 — TEXT TAB
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TextTab(
    result:           OcrResult,
    displayText:      String,
    selectedBlock:    RecognizedBlock?,
    selectedParagraph: RecognizedParagraph?,
    onClearSelection: () -> Unit,
    onSpeak:          () -> Unit,
    onCopy:           () -> Unit,
    modifier:         Modifier
) {
    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        // Selection badge
        val selLabel = when {
            selectedParagraph != null -> "Paragraph Selected"
            selectedBlock     != null -> "Line Selected"
            else                      -> "Full Viewfinder Text"
        }
        val selColor = when {
            selectedParagraph != null -> NeonOrange
            selectedBlock     != null -> NeonPink
            else                      -> NeonBlueExact
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeonBadge(selLabel, selColor)
            if (selectedBlock != null || selectedParagraph != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonPink.copy(0.10f))
                        .border(0.5.dp, NeonPink.copy(0.35f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onClearSelection)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Clear ✕", style = MaterialTheme.typography.labelSmall, color = NeonPink, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallIconBtn("🔊", onClick = onSpeak)
                SmallIconBtn("📋", onClick = onCopy)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Text body
        Text(
            text       = displayText,
            style      = MaterialTheme.typography.bodyMedium,
            color      = Color.White.copy(0.93f),
            lineHeight = 22.sp,
            modifier   = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(0.04f))
                .border(1.dp, selColor.copy(0.18f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        )

        // Paragraph count info
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip("${result.paragraphs.size} paragraphs")
            InfoChip("${result.blocks.size} lines")
            InfoChip("${displayText.length} chars")
            if (result.hasRotatedText) InfoChip("↻ Rotated")
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1 — TRANSLATE TAB
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TranslateTab(
    translationState: TranslationState,
    targetLanguage:   String,
    targetLangCode:   String,
    languages:        List<String>,
    detectedLangCode: String,
    onTranslate:      () -> Unit,
    onLangChange:     (String) -> Unit,
    onSpeak:          (String, String) -> Unit,
    onSpeakOriginal:  () -> Unit,
    modifier:         Modifier
) {
    val ctx = LocalContext.current
    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        // Language row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguageDropdown(
                selected  = targetLanguage,
                languages = languages,
                onChange  = onLangChange,
                modifier  = Modifier.weight(1f)
            )
            // Speak original extracted text
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(NeonBlueExact.copy(0.10f))
                    .border(1.5.dp, NeonBlueExact.copy(0.35f), CircleShape)
                    .clickable(onClick = onSpeakOriginal),
                contentAlignment = Alignment.Center
            ) { Text("🔊", fontSize = 16.sp) }
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.horizontalGradient(listOf(NeonBlueExact, NeonPurple)))
                    .clickable(onClick = onTranslate)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Translate", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Translation result
        AnimatedVisibility(
            visible = translationState !is TranslationState.Idle,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            TranslationResultCard(
                state   = translationState,
                onSpeak = { text, _ -> onSpeak(text, targetLangCode) },
                onCopy  = { copyToClipboard(ctx, it) },
                onShare = { shareText(ctx, it) },
                onSave  = { text ->
                    val filename = "SmartVisionAI_Translation_${System.currentTimeMillis() / 1000}.txt"
                    com.smartvision.ai.utils.FileExportHelper.saveTextFile(ctx, filename, text)
                        .onSuccess {
                            android.widget.Toast.makeText(ctx, "Exported: $filename to Documents", android.widget.Toast.LENGTH_LONG).show()
                        }
                        .onFailure {
                            android.widget.Toast.makeText(ctx, "Export failed: ${it.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                }
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — HISTORY TAB
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HistoryTab(
    history:  List<OcrScanEntity>,
    onDelete: (String) -> Unit,
    modifier: Modifier
) {
    if (history.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📂", fontSize = 36.sp)
                Text("No scans yet", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Capture & translate text to see history", style = MaterialTheme.typography.labelSmall, color = TextHint)
            }
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(history, key = { it.id }) { record ->
            OcrHistoryCard(record = record, onDelete = { onDelete(record.id) })
        }
    }
}

@Composable
private fun OcrHistoryCard(record: OcrScanEntity, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val timeStr = fmt.format(Date(record.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.04f))
            .border(1.dp, NeonBlueExact.copy(0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    NeonBadge(record.sourceLang, NeonBlueExact)
                    if (!record.targetLang.isNullOrBlank()) {
                        Text("→", color = TextSecondary, fontSize = 11.sp)
                        NeonBadge(record.targetLang, NeonPurple)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(timeStr, style = MaterialTheme.typography.labelSmall, color = TextHint)
                    SmallIconBtn("✕", onClick = onDelete)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                record.extractedText.take(90) + if (record.extractedText.length > 90) "…" else "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(0.75f),
                lineHeight = 18.sp
            )
            if (!record.translatedText.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    record.translatedText.take(70) + if (record.translatedText.length > 70) "…" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonPurple.copy(0.85f),
                    lineHeight = 16.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoChip("${record.wordCount}w")
                InfoChip("${record.paragraphCount} para")
                InfoChip(record.script)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TRANSLATION RESULT CARD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TranslationResultCard(
    state:   TranslationState,
    onSpeak: (String, String) -> Unit,
    onCopy:  (String) -> Unit,
    onShare: (String) -> Unit,
    onSave:  (String) -> Unit
) {
    // ...
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NeonPurple, strokeWidth = 2.dp)
                    Text(
                        if (state is TranslationState.Downloading) "⬇ Downloading language model..." else "Translating...",
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
                    .background(Color.White.copy(0.05f))
                    .border(1.dp, NeonPurple.copy(0.35f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            NeonBadge(state.sourceLang, NeonBlueExact)
                            Text("→", color = Color.White.copy(0.4f), fontSize = 12.sp)
                            NeonBadge(state.targetLang, NeonPurple)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SmallIconBtn("🔊") { onSpeak(state.translatedText, "en") }
                            SmallIconBtn("📋") { onCopy(state.translatedText) }
                            SmallIconBtn("↑")  { onShare(state.translatedText) }
                            SmallIconBtn("💾") { onSave(state.translatedText) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text       = state.translatedText,
                        style      = MaterialTheme.typography.bodyLarge,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp
                    )

                    // Meaning in Source Language
                    state.nativeMeaning?.let { m ->
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = NeonBlueExact.copy(0.10f))
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("💡", fontSize = 12.sp)
                            Text("Meaning in ${state.sourceLang}:", style = MaterialTheme.typography.labelSmall, color = NeonBlueExact, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(m, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
                    }

                    // Meaning in Target Language (new)
                    state.meaningInTarget?.let { m ->
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = NeonCyan.copy(0.10f))
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("🎯", fontSize = 12.sp)
                            Text("Meaning in ${state.targetLang}:", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(m, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
                    }

                    // AI Semantic Breakdown
                    state.semanticMeaning?.let { meaning ->
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(0.10f))
                        Spacer(Modifier.height(8.dp))
                        SemanticMeaningCard(
                            meaning            = meaning,
                            sourceLangName     = state.sourceLang,
                            targetLangName     = state.targetLang,
                            targetLangMeaning  = state.meaningInTarget ?: meaning.meaningInTarget
                        )
                    } ?: state.nativeMeaning?.let { m ->
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(0.10f))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 12.sp)
                            Text("Meaning in ${state.sourceLang}:", style = MaterialTheme.typography.labelSmall, color = NeonBlueExact, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(m, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f), lineHeight = 18.sp)
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Text("⚠️", fontSize = 18.sp)
                    Text(state.message, style = MaterialTheme.typography.bodySmall, color = NeonPink)
                }
            }
        }
        else -> {}
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// LANGUAGE DROPDOWN
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LanguageDropdown(selected: String, languages: List<String>, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(0.06f))
                .border(1.dp, NeonBlueExact.copy(0.35f), RoundedCornerShape(24.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("→ $selected", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
                Text("▼", color = NeonBlueExact, fontSize = 10.sp)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF0F172A)).border(0.5.dp, NeonBlueExact.copy(0.3f), RoundedCornerShape(8.dp))) {
            languages.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang, color = Color.White) },
                    onClick = { onChange(lang); expanded = false },
                    trailingIcon = if (lang == selected) ({ Text("✓", color = NeonBlueExact) }) else null
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SMALL HELPERS & WIDGETS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DetectedLangBadge(name: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NeonBlueExact.copy(0.12f))
            .border(1.dp, NeonBlueExact.copy(0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            PulsingDot(NeonBlueExact)
            Text(name, style = MaterialTheme.typography.labelSmall, color = NeonBlueExact, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val s by rememberInfiniteTransition(label = "dot").animateFloat(
        0.5f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), "ds"
    )
    Box(modifier = Modifier.size(6.dp).graphicsLayer(scaleX = s, scaleY = s).clip(CircleShape).background(color))
}

@Composable
private fun SmallIconBtn(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(0.08f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(icon, fontSize = 12.sp) }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(0.05f))
            .border(1.dp, Color.White.copy(0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) { Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.5.sp) }
}

@Composable
private fun NoCameraPermissionView(onRequest: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.background(DeepNavy), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("📷", fontSize = 60.sp)
            Text("Camera permission required", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Grant permission to scan and translate text from the real world", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️")
            Text(message, style = MaterialTheme.typography.bodySmall, color = NeonPink, modifier = Modifier.weight(1f))
            Text("✕", color = NeonPink, fontSize = 12.sp)
        }
    }
}

// ── Utility functions ─────────────────────────────────────────────────────────

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("OCR Text", text))
}

private fun shareText(ctx: Context, text: String) {
    ctx.startActivity(Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) },
        "Share via"
    ))
}

// ══════════════════════════════════════════════════════════════════════════════
// GOOGLE LENS TRANSLATE OVERLAY & MODE SELECTOR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GoogleLensTranslateOverlay(
    blocks: List<RecognizedBlock>,
    blockTranslations: Map<String, String>,
    frameWidth: Int,
    frameHeight: Int,
    modifier: Modifier
) {
    var cW by remember { mutableStateOf(1) }
    var cH by remember { mutableStateOf(1) }

    Box(modifier = modifier.onGloballyPositioned { cW = it.size.width; cH = it.size.height }) {
        if (blocks.isEmpty() || cW <= 1) return@Box

        val fW = frameWidth.toFloat().coerceAtLeast(1f)
        val fH = frameHeight.toFloat().coerceAtLeast(1f)
        val sW = cW.toFloat()
        val sH = cH.toFloat()
        val sc = maxOf(sW / fW, sH / fH)
        val ox = (sW - fW * sc) / 2f
        val oy = (sH - fH * sc) / 2f

        blocks.forEach { b ->
            val translatedText = blockTranslations[b.text]
            if (!translatedText.isNullOrBlank() && translatedText != b.text) {
                val l = b.left * fW * sc + ox
                val t = b.top * fH * sc + oy
                val r = b.right * fW * sc + ox
                val bo = b.bottom * fH * sc + oy
                val w = r - l
                val h = bo - t

                Box(
                    modifier = Modifier
                        .offset { IntOffset(l.toInt(), t.toInt()) }
                        .size(width = w.dp / LocalDensity.current.density, height = h.dp / LocalDensity.current.density)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DeepNavy.copy(0.92f))
                        .border(0.5.dp, NeonPurple.copy(0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = translatedText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = (h * 0.45f).coerceIn(8f, 16f).sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun OcrModeSelector(
    activeMode: OcrMode,
    onModeChange: (OcrMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(0.7f))
            .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            OcrMode.TRANSLATE to "🌐 Translate",
            OcrMode.TEXT to "📄 Text"
        ).forEach { (mode, label) ->
            val isSelected = activeMode == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(listOf(NeonBlue, NeonPurple))
                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onModeChange(mode) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun OcrSourceLanguagePickerSheet(
    current: Language,
    onSelect: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val sourceLanguages = remember {
        listOf(Language("auto", "Auto-detect", "🌐", "Auto")) + ALL_LANGUAGES
    }
    val filtered = sourceLanguages.filter {
        query.isBlank() || it.displayName.contains(query, ignoreCase = true) || it.nativeName.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xFF060D1F))
            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding()
    ) {
        Box(Modifier.padding(top = 10.dp).width(40.dp).height(4.dp).clip(CircleShape).background(NeonBlue.copy(0.4f)).align(Alignment.CenterHorizontally))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Select Source Language",
                style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold
            )
            Box(Modifier.clip(CircleShape).background(Color.White.copy(0.08f)).clickable(onClick = onDismiss).padding(6.dp)) {
                Text("✕", color = Color.White, fontSize = 14.sp)
            }
        }

        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder   = { Text("Search language...", color = Color.White.copy(0.4f)) },
            shape         = RoundedCornerShape(12.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor    = NeonBlue,
                unfocusedBorderColor  = Color.White.copy(0.15f),
                focusedContainerColor = Color.White.copy(0.05f),
                unfocusedContainerColor = Color.White.copy(0.05f),
                cursorColor           = NeonBlue,
                focusedTextColor      = Color.White,
                unfocusedTextColor    = Color.White
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
                            else Modifier.background(Color.White.copy(0.04f))
                        )
                        .clickable { onSelect(lang); onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(lang.flag, fontSize = 20.sp)
                    Column(Modifier.weight(1f)) {
                        Text(lang.displayName, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) NeonBlue else Color.White, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        Text(lang.nativeName, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    if (isSelected) Text("✓", color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
