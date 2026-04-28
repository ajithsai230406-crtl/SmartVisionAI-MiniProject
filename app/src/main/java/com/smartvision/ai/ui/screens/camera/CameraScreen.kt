package com.smartvision.ai.ui.screens.camera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.smartvision.ai.domain.models.ModuleType
import com.smartvision.ai.ui.components.*
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    moduleRoute: String,
    onResult:    () -> Unit,
    onBack:      () -> Unit,
    viewModel:   CameraViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val module = ModuleType.entries.find { it.route == moduleRoute } ?: ModuleType.OBJECT_DETECTION

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    when {
        cameraPermission.status.isGranted -> {
            CameraContent(module = module, onResult = onResult, onBack = onBack, viewModel = viewModel)
        }
        cameraPermission.status.shouldShowRationale -> {
            PermissionRationale(onRequest = { cameraPermission.launchPermissionRequest() }, onBack = onBack)
        }
        else -> PermissionDenied(onBack = onBack)
    }
}

@Composable
private fun CameraContent(
    module:    ModuleType,
    onResult:  () -> Unit,
    onBack:    () -> Unit,
    viewModel: CameraViewModel
) {
    val colors          = smartColors
    val context         = LocalContext.current
    val scope           = rememberCoroutineScope()
    val lifecycleOwner  = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()
    var selectedMode by remember { mutableStateOf(module) }
    var isCaptureReady by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }

    var imageCapture  by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera preview ────────────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType          = PreviewView.ScaleType.FILL_CENTER
                }
                scope.launch {
                    val provider = ProcessCameraProvider.getInstance(ctx).await()
                    val preview  = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture  = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build().also { imageCapture = it }

                    val analyzer = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also { analysis ->
                            analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                                viewModel.analyzeFrame(proxy, selectedMode)
                            }
                        }
                    try {
                        provider.unbindAll()
                        val cam = provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview, capture, analyzer
                        )
                        cameraControl = cam.cameraControl
                        isCaptureReady = true
                    } catch (e: Exception) { e.printStackTrace() }
                }
                previewView
            }
        )

        // ── Live overlay ──────────────────────────────────────────────────
        LiveOverlay(uiState = uiState, module = selectedMode)

        // ── Top gradient ──────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(0.7f), Color.Transparent)))
        )
        // ── Bottom gradient ───────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().height(260.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f))))
        )

        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            CameraIconButton(icon = Icons.Rounded.Close, onClick = onBack)
            Text(selectedMode.title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CameraIconButton(
                    icon    = if (flashEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    onClick = { flashEnabled = !flashEnabled; cameraControl?.enableTorch(flashEnabled) }
                )
                CameraIconButton(icon = Icons.Rounded.FlipCameraAndroid, onClick = {})
            }
        }

        // ── Mode chips ────────────────────────────────────────────────────
        ModeSelector(
            currentMode    = selectedMode,
            onModeSelected = { selectedMode = it },
            modifier       = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp)
        )

        // ── Focus ring ────────────────────────────────────────────────────
        FocusRing(modifier = Modifier.align(Alignment.Center), color = moduleAccentColor(selectedMode))

        // ── Bottom controls ───────────────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RecentPhotosRow(onPhotoSelected = { uri -> viewModel.processUri(uri, selectedMode) })
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                CameraIconButton(icon = Icons.Rounded.PhotoLibrary, size = 52.dp, onClick = {})
                ShutterButton(
                    isReady     = isCaptureReady,
                    isLoading   = uiState.isProcessing,
                    accentColor = moduleAccentColor(selectedMode),
                    onClick     = {
                        imageCapture?.let { cap ->
                            viewModel.captureImage(cap, context, selectedMode) { onResult() }
                        }
                    }
                )
                CameraIconButton(
                    icon    = if (uiState.liveMode) Icons.Rounded.MotionPhotosAuto else Icons.Rounded.MotionPhotosOff,
                    size    = 52.dp,
                    onClick = { viewModel.toggleLiveMode() }
                )
            }
        }
    }
}

// ── Mode chips ────────────────────────────────────────────────────────────────
private val CAMERA_MODES = listOf(
    ModuleType.OBJECT_DETECTION, ModuleType.TEXT_SCANNER,
    ModuleType.STUDENT_HELPER,   ModuleType.QR_SCANNER, ModuleType.TRANSLATOR
)

@Composable
private fun ModeSelector(currentMode: ModuleType, onModeSelected: (ModuleType) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(CAMERA_MODES) { mode ->
            val selected = mode == currentMode
            val accent   = moduleAccentColor(mode)
            FilterChip(
                selected = selected,
                onClick  = { onModeSelected(mode) },
                label    = { Text(mode.title.split(" ").first()) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(0.25f), selectedLabelColor = accent,
                    containerColor = Color.Black.copy(0.4f),     labelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = selected,
                    selectedBorderColor = accent, borderColor = Color.White.copy(0.3f)
                )
            )
        }
    }
}

// ── Focus ring ────────────────────────────────────────────────────────────────
@Composable
private fun FocusRing(color: Color, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "focus")
    val alpha by inf.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "a")
    Box(modifier = modifier.size(200.dp).border(2.dp, color.copy(alpha), RoundedCornerShape(24.dp))) {
        listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).forEach { a ->
            Box(Modifier.size(16.dp).align(a).background(color.copy(alpha), RoundedCornerShape(3.dp)))
        }
    }
}

// ── Shutter ───────────────────────────────────────────────────────────────────
@Composable
private fun ShutterButton(isReady: Boolean, isLoading: Boolean, accentColor: Color, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isLoading) 0.9f else 1f, label = "s")
    Box(
        modifier = Modifier.size(76.dp).scale(scale)
            .background(Color.White.copy(0.15f), CircleShape)
            .border(3.dp, Color.White, CircleShape)
            .clickable(enabled = isReady && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) CircularProgressIndicator(color = accentColor, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
        else           Box(Modifier.size(56.dp).background(Color.White, CircleShape))
    }
}

// ── Recent photos ─────────────────────────────────────────────────────────────
@Composable
private fun RecentPhotosRow(onPhotoSelected: (android.net.Uri) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(5) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(0.1f)).border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Image, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ── Live overlay ──────────────────────────────────────────────────────────────
@Composable
private fun LiveOverlay(uiState: CameraUiState, module: ModuleType) {
    if (!uiState.liveMode || uiState.overlayBoxes.isEmpty()) return
    val accent = moduleAccentColor(module)
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        uiState.overlayBoxes.forEach { box ->
            val left   = box.left   * size.width
            val top    = box.top    * size.height
            val width  = (box.right  - box.left) * size.width
            val height = (box.bottom - box.top)  * size.height
            drawRect(color = accent.copy(0.25f), topLeft = Offset(left, top), size = GeoSize(width, height))
            drawRect(color = accent, topLeft = Offset(left, top), size = GeoSize(width, height), style = Stroke(2f))
        }
    }
}

// ── Permission screens ────────────────────────────────────────────────────────
@Composable
private fun PermissionRationale(onRequest: () -> Unit, onBack: () -> Unit) {
    val colors = smartColors
    Box(Modifier.fillMaxSize().background(colors.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Rounded.CameraAlt, null, tint = colors.primary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Camera Permission Required", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
            Spacer(Modifier.height(8.dp))
            Text("SmartVision needs camera access.", style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = colors.primary)) { Text("Grant Permission") }
            TextButton(onClick = onBack) { Text("Go Back", color = colors.subtext) }
        }
    }
}

@Composable
private fun PermissionDenied(onBack: () -> Unit) {
    val colors = smartColors
    Box(Modifier.fillMaxSize().background(colors.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Rounded.NoPhotography, null, tint = colors.error, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Camera Access Denied", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
            Spacer(Modifier.height(8.dp))
            Text("Enable camera permission in device settings.", style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onBack) { Text("Go Back", color = colors.subtext) }
        }
    }
}

// ── Shared icon button — also imported by ResultScreen ────────────────────────
@Composable
fun CameraIconButton(
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    size:    Dp = 44.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(size)
            .background(Color.Black.copy(0.4f), CircleShape)
            .border(1.dp, Color.White.copy(0.2f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.48f))
    }
}
