package com.smartvision.ai.ui.screens.camera

import android.Manifest
import android.content.Context
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
import androidx.compose.ui.graphics.*
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

// ─────────────────────────────────────────────────────────────────────────────
// CAMERA SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    moduleRoute: String,
    onResult:    () -> Unit,
    onBack:      () -> Unit,
    onHome:      () -> Unit = onBack,
    viewModel:   CameraViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val module = ModuleType.entries.find { it.route == moduleRoute }
                 ?: ModuleType.OBJECT_DETECTION

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    when {
        cameraPermission.status.isGranted -> CameraContent(
            module    = module,
            onResult  = onResult,
            onBack    = onBack,
            onHome    = onHome,
            viewModel = viewModel
        )
        cameraPermission.status.shouldShowRationale ->
            PermissionRationale(onRequest = { cameraPermission.launchPermissionRequest() }, onBack = onBack)
        else ->
            PermissionDenied(onBack = onBack)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CAMERA CONTENT — full-screen, styled exactly like Image 4
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CameraContent(
    module:    ModuleType,
    onResult:  () -> Unit,
    onBack:    () -> Unit,
    onHome:    () -> Unit,
    viewModel: CameraViewModel
) {
    val colors         = smartColors
    val context        = LocalContext.current
    val scope          = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()
    var selectedMode   by remember { mutableStateOf(module) }
    var isCaptureReady by remember { mutableStateOf(false) }
    var flashEnabled   by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }

    var imageCapture  by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // accent for current mode
    val accent = moduleAccentColor(selectedMode)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera preview ────────────────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType          = PreviewView.ScaleType.FILL_CENTER
                }
                scope.launch {
                    val provider = ProcessCameraProvider.getInstance(ctx).await()
                    cameraProvider = provider
                    bindCamera(
                        provider       = provider,
                        ctx            = ctx,
                        lifecycleOwner = lifecycleOwner,
                        previewView    = previewView,
                        useFront       = useFrontCamera,
                        selectedMode   = selectedMode,
                        viewModel      = viewModel,
                        onCapture      = { imageCapture = it },
                        onControl      = { cameraControl = it },
                        onReady        = { isCaptureReady = true }
                    )
                }
                previewView
            }
        )

        // ── Live overlay ──────────────────────────────────────────────────────
        if (uiState.liveMode && uiState.overlayBoxes.isNotEmpty()) {
            androidx.compose.ui.graphics.Canvas(modifier = Modifier.fillMaxSize())
        }

        // ── Top gradient scrim ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Black.copy(0.75f), Color.Transparent))
                )
        )

        // ── Bottom gradient scrim ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.88f)))
                )
        )

        // ── TOP BAR: X + title + flash + flip ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Back/close
            CameraIconButton(icon = Icons.Rounded.Close, onClick = onBack)

            Text(
                text       = selectedMode.title,
                style      = MaterialTheme.typography.titleMedium,
                color      = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CameraIconButton(
                    icon    = if (flashEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    tint    = if (flashEnabled) Color(0xFFFFD600) else Color.White,
                    onClick = {
                        flashEnabled = !flashEnabled
                        cameraControl?.enableTorch(flashEnabled)
                    }
                )
                CameraIconButton(
                    icon    = Icons.Rounded.FlipCameraAndroid,
                    onClick = {
                        useFrontCamera = !useFrontCamera
                        scope.launch {
                            cameraProvider?.let { provider ->
                                bindCamera(
                                    provider       = provider,
                                    ctx            = context,
                                    lifecycleOwner = lifecycleOwner,
                                    previewView    = null,
                                    useFront       = useFrontCamera,
                                    selectedMode   = selectedMode,
                                    viewModel      = viewModel,
                                    onCapture      = { imageCapture = it },
                                    onControl      = { cameraControl = it },
                                    onReady        = { isCaptureReady = true }
                                )
                            }
                        }
                    }
                )
            }
        }

        // ── Mode selector tabs (Object | Text | Student | QR | Translate) ────
        // Styled like Image 4: horizontal pill tabs just below top bar
        ModeSelector(
            currentMode    = selectedMode,
            onModeSelected = { selectedMode = it },
            modifier       = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 68.dp)
        )

        // ── Focus / scan frame — cyan corners like Image 4 ───────────────────
        FocusFrame(
            modifier = Modifier.align(Alignment.Center),
            color    = accent
        )

        // ── BOTTOM controls ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recent photos strip
            RecentPhotosRow(onPhotoSelected = { uri -> viewModel.processUri(uri, selectedMode) })

            // Shutter row
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                CameraIconButton(
                    icon    = Icons.Rounded.PhotoLibrary,
                    size    = 54.dp,
                    onClick = { /* launch gallery */ }
                )

                ShutterButton(
                    isReady     = isCaptureReady,
                    isLoading   = uiState.isProcessing,
                    accentColor = accent,
                    onClick     = {
                        imageCapture?.let { capture ->
                            viewModel.captureImage(capture, context, selectedMode) { onResult() }
                        }
                    }
                )

                CameraIconButton(
                    icon    = if (uiState.liveMode) Icons.Rounded.MotionPhotosAuto
                              else Icons.Rounded.MotionPhotosOff,
                    size    = 54.dp,
                    tint    = if (uiState.liveMode) accent else Color.White,
                    onClick = { viewModel.toggleLiveMode() }
                )
            }

            // Home shortcut
            TextButton(onClick = onHome) {
                Icon(Icons.Rounded.Home, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Home", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(0.6f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BIND CAMERA helper (extracted so flip camera can reuse it)
// ─────────────────────────────────────────────────────────────────────────────

private suspend fun bindCamera(
    provider:       ProcessCameraProvider,
    ctx:            Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView:    PreviewView?,
    useFront:       Boolean,
    selectedMode:   ModuleType,
    viewModel:      CameraViewModel,
    onCapture:      (ImageCapture) -> Unit,
    onControl:      (CameraControl) -> Unit,
    onReady:        () -> Unit
) {
    val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA
                   else CameraSelector.DEFAULT_BACK_CAMERA

    val preview = Preview.Builder().build().also {
        if (previewView != null) it.setSurfaceProvider(previewView.surfaceProvider)
    }
    val capture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build().also { onCapture(it) }

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
        val cam = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture, analyzer)
        onControl(cam.cameraControl)
        onReady()
    } catch (e: Exception) { e.printStackTrace() }
}

// ─────────────────────────────────────────────────────────────────────────────
// MODE SELECTOR — pill tabs (Object | Text | Student | QR | Translate)
// ─────────────────────────────────────────────────────────────────────────────

private val CAMERA_MODES = listOf(
    ModuleType.OBJECT_DETECTION,
    ModuleType.TEXT_SCANNER,
    ModuleType.STUDENT_HELPER,
    ModuleType.QR_SCANNER,
    ModuleType.TRANSLATOR
)

@Composable
private fun ModeSelector(
    currentMode:    ModuleType,
    onModeSelected: (ModuleType) -> Unit,
    modifier:       Modifier = Modifier
) {
    LazyRow(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding        = PaddingValues(horizontal = 16.dp)
    ) {
        items(CAMERA_MODES) { mode ->
            val selected = mode == currentMode
            val accent   = moduleAccentColor(mode)
            val label    = mode.title.split(" ").first()

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (selected) accent.copy(0.25f) else Color.Black.copy(0.45f)
                    )
                    .border(
                        1.dp,
                        if (selected) accent else Color.White.copy(0.25f),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.labelLarge,
                    color      = if (selected) accent else Color.White,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FOCUS FRAME — cyan rounded rect + corner handles (Image 4 style)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FocusFrame(color: Color, modifier: Modifier = Modifier) {
    val inf   = rememberInfiniteTransition(label = "frame")
    val alpha by inf.animateFloat(
        initialValue  = 0.45f, targetValue  = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label         = "alpha"
    )

    Box(
        modifier = modifier.size(width = 260.dp, height = 220.dp)
    ) {
        // Main border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, color.copy(alpha = alpha), RoundedCornerShape(20.dp))
        )
        // Corner handles (solid squares — matching Image 4 exactly)
        val cornerSize   = 18.dp
        val cornerRadius = 4.dp
        listOf(
            Alignment.TopStart to Modifier.align(Alignment.TopStart),
            Alignment.TopEnd   to Modifier.align(Alignment.TopEnd),
            Alignment.BottomStart to Modifier.align(Alignment.BottomStart),
            Alignment.BottomEnd   to Modifier.align(Alignment.BottomEnd)
        ).forEach { (_, mod) ->
            Box(
                modifier = mod
                    .size(cornerSize)
                    .background(color, RoundedCornerShape(cornerRadius))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHUTTER BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShutterButton(
    isReady:     Boolean,
    isLoading:   Boolean,
    accentColor: Color,
    onClick:     () -> Unit
) {
    val scale by animateFloatAsState(if (isLoading) 0.9f else 1f, label = "shutterScale")
    Box(
        modifier = Modifier
            .size(76.dp)
            .scale(scale)
            .background(Color.White.copy(0.15f), CircleShape)
            .border(3.dp, Color.White, CircleShape)
            .clickable(enabled = isReady && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color       = accentColor,
                modifier    = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        } else {
            Box(modifier = Modifier.size(56.dp).background(Color.White, CircleShape))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RECENT PHOTOS STRIP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentPhotosRow(onPhotoSelected: (android.net.Uri) -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(0.08f))
                    .border(1.dp, Color.White.copy(0.18f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    Icons.Rounded.Image, null,
                    tint     = Color.White.copy(0.35f),
                    modifier = Modifier.align(Alignment.Center).size(22.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ICON BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CameraIconButton(
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    size:    Dp      = 44.dp,
    tint:    Color   = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color.Black.copy(0.4f), CircleShape)
            .border(1.dp, Color.White.copy(0.18f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.48f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PERMISSION SCREENS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionRationale(onRequest: () -> Unit, onBack: () -> Unit) {
    val colors = smartColors
    Box(Modifier.fillMaxSize().background(colors.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Rounded.CameraAlt, null, tint = colors.primary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Camera Permission Required", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
            Spacer(Modifier.height(8.dp))
            Text("SmartVision needs camera access to detect objects and scan text.", style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = colors.primary)) {
                Text("Grant Permission")
            }
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
            Text("Please enable camera permission in your device settings.", style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onBack) { Text("Go Back", color = colors.subtext) }
        }
    }
}
