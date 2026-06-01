package com.smartvision.ai.student.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.ocr.domain.RecognizedBlock
import com.smartvision.ai.student.domain.*
import com.smartvision.ai.student.presentation.*
import com.smartvision.ai.ui.theme.*
import com.smartvision.ai.utils.ImageUtils
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StudentHelperScreen(
    onBack: () -> Unit,
    viewModel: StudentHelperViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val scanState        by viewModel.scanState.collectAsStateWithLifecycle()
    val subject          by viewModel.subject.collectAsStateWithLifecycle()
    val overlayBlocks    by viewModel.overlayBlocks.collectAsStateWithLifecycle()
    val chatMessages     by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isThinking       by viewModel.isThinking.collectAsStateWithLifecycle()
    val flashEnabled     by viewModel.flashEnabled.collectAsStateWithLifecycle()
    val activeTab        by viewModel.activeTab.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val galleryLauncher = rememberGalleryPickerLauncher { uri ->
        val bmp = ImageUtils(com.google.firebase.storage.FirebaseStorage.getInstance()).loadBitmapFromUri(context, uri)
        if (bmp != null) {
            viewModel.solveCroppedQuestion(bmp)
        }
    }

    val cameraRef = remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }
    LaunchedEffect(flashEnabled) {
        cameraRef.value?.cameraControl?.enableTorch(flashEnabled)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera Feed or Perm view ──────────────────────────────────────────
        if (cameraPermission.status.isGranted) {
            if (scanState is StudentScanState.CameraActive) {
                StudentCameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    imageCapture = imageCapture,
                    onCameraReady = { cameraRef.value = it },
                    onFrame = { img, rot, w, h -> viewModel.analyzeFrame(img, rot, w, h) }
                )
            }
        } else {
            NoCameraPermView(
                onRequest = { cameraPermission.launchPermissionRequest() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Resizable Viewfinder Crop Box & Punched Overlay ───────────────────
        var cropRect by remember { mutableStateOf(Rect(150f, 400f, 900f, 1100f)) }
        var canvasWidth by remember { mutableStateOf(1) }
        var canvasHeight by remember { mutableStateOf(1) }

        if (scanState is StudentScanState.CameraActive && cameraPermission.status.isGranted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        canvasWidth = coords.size.width
                        canvasHeight = coords.size.height
                    }
            ) {
                ResizableViewfinder(
                    rect = cropRect,
                    onRectChange = { cropRect = it },
                    modifier = Modifier.fillMaxSize()
                )

                // Highlighted OCR Blocks inside Viewfinder
                val normLeft = cropRect.left / canvasWidth
                val normTop = cropRect.top / canvasHeight
                val normRight = cropRect.right / canvasWidth
                val normBottom = cropRect.bottom / canvasHeight

                val insideBlocks = overlayBlocks.filter { block ->
                    block.left >= normLeft && block.right <= normRight &&
                    block.top >= normTop && block.bottom <= normBottom
                }

                ViewfinderTextHighlights(
                    blocks = insideBlocks,
                    onTapBlock = { block ->
                        // Quick capture and crop block area directly if tapped!
                        captureAndCropPhoto(context, imageCapture, cropRect, canvasWidth, canvasHeight) { croppedBmp ->
                            viewModel.solveCroppedQuestion(croppedBmp, block.text)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ── Top HUD ────────────────────────────────────────────────────────────
        if (scanState is StudentScanState.CameraActive) {
            StudentTopHud(
                subject = subject,
                flashEnabled = flashEnabled,
                onBack = onBack,
                onSubjectChange = { viewModel.setSubject(it) },
                onFlashToggle = { viewModel.toggleFlash() },
                onGalleryPicker = { galleryLauncher() }
            )
        }

        // ── Bottom Capture / Solve Action Button ──────────────────────────────
        if (scanState is StudentScanState.CameraActive && cameraPermission.status.isGranted) {
            StudentCaptureRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 40.dp),
                onCapture = {
                    captureAndCropPhoto(context, imageCapture, cropRect, canvasWidth, canvasHeight) { croppedBmp ->
                        viewModel.solveCroppedQuestion(croppedBmp)
                    }
                }
            )
        }

        // ── Solving / Loading Screen Overlay ──────────────────────────────────
        AnimatedVisibility(
            visible = scanState is StudentScanState.Solving,
            enter = fadeIn(), exit = fadeOut()
        ) {
            StudentSolvingOverlay()
        }

        // ── Solved Solution Sheet (Google Lens Homework Card Style) ──────────
        AnimatedVisibility(
            visible = scanState is StudentScanState.Solved,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val solvedState = scanState as? StudentScanState.Solved
            if (solvedState != null) {
                StudentSolutionSheet(
                    solution = solvedState.solution,
                    croppedBmp = solvedState.croppedImage,
                    chatMessages = chatMessages,
                    isThinking = isThinking,
                    activeTab = activeTab,
                    onTabSelect = { viewModel.setTab(it) },
                    onSendTutorMsg = { viewModel.sendTutorChatMessage(it) },
                    onExplainSimply = { viewModel.explainSolutionSimply() },
                    onSpeak = { viewModel.speakSolution() },
                    onRescan = { viewModel.resetScanner() }
                )
            }
        }

        // ── Error Banner ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = scanState is StudentScanState.Error,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
        ) {
            val errState = scanState as? StudentScanState.Error
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeonPink.copy(0.15f))
                    .border(1.dp, NeonPink.copy(0.4f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
                    .clickable { viewModel.resetScanner() }
            ) {
                Text(
                    "⚠️ ${errState?.message ?: "An error occurred."} Tap to rescan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonPink
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CAMERA PREVIEW COMPOSABLE
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StudentCameraPreview(
    modifier: Modifier,
    imageCapture: ImageCapture,
    onCameraReady: (Camera) -> Unit,
    onFrame: (android.media.Image, Int, Int, Int) -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val processing = remember { AtomicBoolean(false) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val preview = PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            ProcessCameraProvider.getInstance(context).addListener({
                val provider = ProcessCameraProvider.getInstance(context).get()
                provider.unbindAll()

                val previewUC = Preview.Builder().build()
                    .also { it.setSurfaceProvider(preview.surfaceProvider) }

                val analysisUC = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                analysisUC.setAnalyzer(executor) { proxy ->
                    val img = proxy.image
                    if (img != null && processing.compareAndSet(false, true)) {
                        onFrame(img, proxy.imageInfo.rotationDegrees, img.width, img.height)
                        processing.set(false)
                    }
                    proxy.close()
                }

                val cam = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUC, analysisUC, imageCapture
                )
                onCameraReady(cam)
            }, ContextCompat.getMainExecutor(context))
            preview
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// RESIZABLE VIEWFINDER OVERLAY
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ResizableViewfinder(
    rect: Rect,
    onRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val handleSize = 40.dp
    val strokeWidth = 3.dp

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerPath = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
            }
            val innerPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = rect,
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    )
                )
            }
            val punchedPath = Path.combine(PathOperation.Difference, outerPath, innerPath)
            drawPath(punchedPath, Color.Black.copy(alpha = 0.65f))

            // Glowing border
            drawRoundRect(
                color = NeonCyan,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = strokeWidth.toPx())
            )
        }

        // Resizable drag corner handles
        // Top-Left
        Box(
            modifier = Modifier
                .offset(
                    x = (rect.left - handleSize.value / 2).dp,
                    y = (rect.top - handleSize.value / 2).dp
                )
                .size(handleSize)
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newLeft = (rect.left + dragAmount.x).coerceAtMost(rect.right - 180f).coerceAtLeast(40f)
                        val newTop = (rect.top + dragAmount.y).coerceAtMost(rect.bottom - 180f).coerceAtLeast(120f)
                        onRectChange(Rect(newLeft, newTop, rect.right, rect.bottom))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val s = size.width
                drawLine(NeonCyan, Offset(s/2, s/2), Offset(s, s/2), 4.dp.toPx(), StrokeCap.Round)
                drawLine(NeonCyan, Offset(s/2, s/2), Offset(s/2, s), 4.dp.toPx(), StrokeCap.Round)
            }
        }

        // Top-Right
        Box(
            modifier = Modifier
                .offset(
                    x = (rect.right - handleSize.value / 2).dp,
                    y = (rect.top - handleSize.value / 2).dp
                )
                .size(handleSize)
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newRight = (rect.right + dragAmount.x).coerceAtLeast(rect.left + 180f).coerceAtMost(widthPx - 40f)
                        val newTop = (rect.top + dragAmount.y).coerceAtMost(rect.bottom - 180f).coerceAtLeast(120f)
                        onRectChange(Rect(rect.left, newTop, newRight, rect.bottom))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val s = size.width
                drawLine(NeonCyan, Offset(s/2, s/2), Offset(0f, s/2), 4.dp.toPx(), StrokeCap.Round)
                drawLine(NeonCyan, Offset(s/2, s/2), Offset(s/2, s), 4.dp.toPx(), StrokeCap.Round)
            }
        }

        // Bottom-Left
        Box(
            modifier = Modifier
                .offset(
                    x = (rect.left - handleSize.value / 2).dp,
                    y = (rect.bottom - handleSize.value / 2).dp
                )
                .size(handleSize)
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newLeft = (rect.left + dragAmount.x).coerceAtMost(rect.right - 180f).coerceAtLeast(40f)
                        val newBottom = (rect.bottom + dragAmount.y).coerceAtLeast(rect.top + 180f).coerceAtMost(heightPx - 200f)
                        onRectChange(Rect(newLeft, rect.top, rect.right, newBottom))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val s = size.width
                drawLine(NeonCyan, Offset(s/2, s/2), Offset(s, s/2), 4.dp.toPx(), StrokeCap.Round)
                drawLine(NeonCyan, Offset(s/2, s/2), Offset(s/2, 0f), 4.dp.toPx(), StrokeCap.Round)
            }
        }

        // Bottom-Right
        Box(
            modifier = Modifier
                .offset(
                    x = (rect.right - handleSize.value / 2).dp,
                    y = (rect.bottom - handleSize.value / 2).dp
                )
                .size(handleSize)
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newRight = (rect.right + dragAmount.x).coerceAtLeast(rect.left + 180f).coerceAtMost(widthPx - 40f)
                        val newBottom = (rect.bottom + dragAmount.y).coerceAtLeast(rect.top + 180f).coerceAtMost(heightPx - 200f)
                        onRectChange(Rect(rect.left, rect.top, newRight, newBottom))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val s = size.width
                drawLine(NeonCyan, Offset(s/2, s/2), Offset(0f, s/2), 4.dp.toPx(), StrokeCap.Round)
                drawLine(NeonCyan, Offset(s/2, s/2), Offset(s/2, 0f), 4.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// OCR TEXT HIGHLIGHTS OVERLAY
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ViewfinderTextHighlights(
    blocks: List<RecognizedBlock>,
    onTapBlock: (RecognizedBlock) -> Unit,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        blocks.forEach { block ->
            val left   = block.left   * size.width
            val top    = block.top    * size.height
            val right  = block.right  * size.width
            val bottom = block.bottom * size.height

            // Small subtle glowing highlight box over detected lines
            drawRect(
                color = NeonCyan.copy(alpha = 0.15f),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top)
            )
            drawRect(
                color = NeonCyan.copy(alpha = 0.50f),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 1f)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TOP HUD PILLS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StudentTopHud(
    subject: Subject,
    flashEnabled: Boolean,
    onBack: () -> Unit,
    onSubjectChange: (Subject) -> Unit,
    onFlashToggle: () -> Unit,
    onGalleryPicker: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Text("←", color = Color.White) }

            Text(
                "🎓 Google Lens Homework Mode",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(0.4f))
                        .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onFlashToggle)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) { Text(if (flashEnabled) "⚡" else "🔦", color = Color.White) }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(0.4f))
                        .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onGalleryPicker)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) { Text("🖼️", color = Color.White) }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Horizontal Subject Pills
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Subject.values()) { subj ->
                val isSelected = subj == subject
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) Brush.horizontalGradient(listOf(NeonBlue, NeonCyan))
                            else Brush.horizontalGradient(listOf(Color.Black.copy(0.40f), Color.Black.copy(0.40f)))
                        )
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onSubjectChange(subj) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        "${subj.emoji} ${subj.displayName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// BOTTOM CAPTURE CONTROLS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StudentCaptureRow(
    modifier: Modifier,
    onCapture: () -> Unit
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(NeonCyan, NeonBlue)))
            .border(3.dp, Color.White, CircleShape)
            .clickable(onClick = onCapture),
        contentAlignment = Alignment.Center
    ) {
        Text("🎓", fontSize = 28.sp)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SOLVING OVERLAY
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StudentSolvingOverlay() {
    val inf = rememberInfiniteTransition(label = "solving")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "r")
    val pulse by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "p")

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(Modifier.size(80.dp).graphicsLayer(rotationZ = rot)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(
                        Brush.sweepGradient(listOf(Color.Transparent, NeonCyan)),
                        0f, 270f, false,
                        style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            Text("AI Solving Homework...", style = MaterialTheme.typography.bodyLarge, color = NeonCyan.copy(alpha = pulse), fontWeight = FontWeight.SemiBold)
            Text("Formulating step-by-step solution • Gemini", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SOLUTION BOTTOM SHEET (Google Lens Homework panel)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StudentSolutionSheet(
    solution: AiSolution,
    croppedBmp: Bitmap?,
    chatMessages: List<ChatMessage>,
    isThinking: Boolean,
    activeTab: StudentTab,
    onTabSelect: (StudentTab) -> Unit,
    onSendTutorMsg: (String) -> Unit,
    onExplainSimply: () -> Unit,
    onSpeak: () -> Unit,
    onRescan: () -> Unit
) {
    val ext = MaterialTheme.extended

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(DeepNavy)
    ) {
        // Top Card Header with cropped Bitmap
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color.Black)
        ) {
            if (croppedBmp != null) {
                Image(
                    bitmap = croppedBmp.asImageBitmap(),
                    contentDescription = "Scanned question",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                )
            }

            // Top dismiss / Rescan row
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(0.6f))
                        .clickable(onClick = onRescan)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) { Text("← Scan Another", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold) }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(0.6f)).clickable(onClick = onSpeak),
                        contentAlignment = Alignment.Center
                    ) { Text("🔊", fontSize = 14.sp) }
                }
            }
        }

        // Sliding tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ext.glassCard)
                .border(1.dp, ext.glassBorder, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            StudentTab.values().forEach { tab ->
                val active = tab == activeTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) NeonCyan.copy(0.2f) else Color.Transparent)
                        .clickable { onTabSelect(tab) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${tab.emoji} ${tab.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // Dynamic Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                StudentTab.SOLUTION -> {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Main Answer Card
                        SolutionCard(title = "Final Solution", icon = "✅", color = NeonGreen) {
                            Text(solution.mainAnswer, style = MaterialTheme.typography.bodyMedium, color = Color.White, lineHeight = 22.sp)
                        }

                        // Steps Card
                        if (solution.steps.isNotEmpty()) {
                            SolutionCard(title = "Step-by-Step Explanation", icon = "📍", color = NeonCyan) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    solution.steps.forEachIndexed { idx, step ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(
                                                modifier = Modifier.size(22.dp).clip(CircleShape).background(NeonCyan.copy(0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) { Text("${idx + 1}", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold) }
                                            Text(step, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), modifier = Modifier.weight(1f), lineHeight = 18.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Key Formula Card
                        solution.keyFormula?.let { formula ->
                            SolutionCard(title = "Key Formula Summary", icon = "📐", color = NeonPurple) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(NeonPurple.copy(0.08f)).border(1.dp, NeonPurple.copy(0.25f), RoundedCornerShape(8.dp)).padding(12.dp)
                                ) {
                                    Text(formula, style = MaterialTheme.typography.bodyLarge, color = NeonPurple, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Quick Tip Card
                        solution.quickTip?.let { tip ->
                            SolutionCard(title = "Tutor's Eco Tip", icon = "🌱", color = NeonOrange) {
                                Text(tip, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
                            }
                        }

                        // Explain Simply CTA
                        NeonButton(
                            text = "🧒 Explain Solutions Simply",
                            onClick = onExplainSimply,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                }

                StudentTab.CHAT -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(chatMessages) { msg ->
                                TutorChatBubble(msg)
                            }
                        }
                        TutorChatInputBar(isThinking = isThinking, onSend = onSendTutorMsg)
                    }
                }

                StudentTab.PRACTICE -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(solution.practiceQ) { q ->
                            PracticeQuestionCard(q, onSolve = { onSendTutorMsg(it) })
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun SolutionCard(title: String, icon: String, color: Color, content: @Composable () -> Unit) {
    val ext = MaterialTheme.extended
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, color.copy(0.2f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 14.sp)
                Text(title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = color.copy(0.12f))
            content()
        }
    }
}

@Composable
private fun TutorChatBubble(msg: ChatMessage) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isUser) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(Brush.radialGradient(listOf(NeonCyan, NeonBlue))),
                contentAlignment = Alignment.Center
            ) { Text("🤖", fontSize = 14.sp) }
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (msg.isUser) NeonCyan.copy(0.25f) else ext.glassCard)
                .border(1.dp, if (msg.isUser) NeonCyan.copy(0.4f) else ext.glassBorder, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            if (msg.isLoading) {
                ThinkingDots()
            } else {
                Text(msg.text, style = MaterialTheme.typography.bodySmall, color = Color.White, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun TutorChatInputBar(isThinking: Boolean, onSend: (String) -> Unit) {
    var txt by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = txt,
            onValueChange = { txt = it },
            placeholder = { Text("Ask follow-up question...", fontSize = 13.sp, color = Color.White.copy(0.4f)) },
            modifier = Modifier.weight(1f),
            maxLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.White.copy(0.15f),
                cursorColor = NeonCyan
            ),
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (txt.isNotBlank() && !isThinking) {
                    onSend(txt)
                    txt = ""
                    keyboard?.hide()
                }
            })
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (txt.isNotBlank() && !isThinking) NeonCyan else Color.White.copy(0.1f))
                .clickable {
                    if (txt.isNotBlank() && !isThinking) {
                        onSend(txt)
                        txt = ""
                        keyboard?.hide()
                    }
                },
            contentAlignment = Alignment.Center
        ) { Text("↑", color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PracticeQuestionCard(question: String, onSolve: (String) -> Unit) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.glassCard)
            .border(1.dp, NeonCyan.copy(0.2f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🏋️", fontSize = 18.sp)
        Text(question, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), modifier = Modifier.weight(1f), lineHeight = 18.sp)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(NeonCyan.copy(0.15f))
                .clickable { onSolve("Solve this: $question") }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) { Text("Solve", style = MaterialTheme.typography.labelSmall, color = NeonCyan) }
    }
}

@Composable
private fun ThinkingDots() {
    val inf = rememberInfiniteTransition(label = "chatdots")
    val d1 by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "d1")
    val d2 by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, 150), RepeatMode.Reverse), label = "d2")
    val d3 by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, 300), RepeatMode.Reverse), label = "d3")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(d1, d2, d3).forEach { a ->
            Box(Modifier.size(6.dp).clip(CircleShape).background(NeonCyan.copy(a)))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CAPTURE & CROP HELPER METHODS
// ══════════════════════════════════════════════════════════════════════════════

private fun captureAndCropPhoto(
    context: Context,
    imageCapture: ImageCapture,
    cropRect: Rect,
    canvasWidth: Int,
    canvasHeight: Int,
    onSuccess: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val fullBmp = imageProxyToBitmap(image)
                image.close()

                if (fullBmp != null) {
                    val cropLeft = cropRect.left / canvasWidth
                    val cropTop = cropRect.top / canvasHeight
                    val cropRight = cropRect.right / canvasWidth
                    val cropBottom = cropRect.bottom / canvasHeight

                    val croppedBmp = ImageUtils(com.google.firebase.storage.FirebaseStorage.getInstance()).cropBitmap(
                        fullBmp, cropLeft, cropTop, cropRight, cropBottom
                    )
                    onSuccess(croppedBmp)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                // Ignore capture errors
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

    if (image.imageInfo.rotationDegrees != 0) {
        val matrix = android.graphics.Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    }
    return raw
}

@Composable
private fun NoCameraPermView(onRequest: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("📷", fontSize = 64.sp)
            Text("Camera permission required", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("Required for Google Lens Homework solver", color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodySmall)
            NeonButton("Grant Permission", onClick = onRequest, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}
