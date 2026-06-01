package com.smartvision.ai.ui.screens.camera

import android.Manifest
import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.*
import com.smartvision.ai.domain.models.ModuleType
import com.smartvision.ai.ui.components.*
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    moduleRoute: String,
    onResult: () -> Unit,
    onBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val perm = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) { if (!perm.status.isGranted) perm.launchPermissionRequest() }
    when {
        perm.status.isGranted -> CameraContent(moduleRoute, onResult, onBack, viewModel)
        perm.status.shouldShowRationale -> PermRationale({ perm.launchPermissionRequest() }, onBack)
        else -> PermDenied(onBack)
    }
}

@Composable
private fun CameraContent(
    moduleRoute: String,
    onResult: () -> Unit,
    onBack: () -> Unit,
    vm: CameraViewModel
) {
    val c = svColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by vm.uiState.collectAsState()
    val module = ModuleType.entries.find { it.route == moduleRoute } ?: ModuleType.OBJECT_DETECTION
    var selectedMode by remember { mutableStateOf(module) }
    var flashEnabled by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Camera preview
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                scope.launch {
                    val provider = ProcessCameraProvider.getInstance(ctx).get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                    val cap = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                        .also { imageCapture = it }
                    val analyzer = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                        .also { it.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy -> vm.analyzeFrame(proxy, selectedMode) } }
                    try {
                        provider.unbindAll()
                        val cam = provider.bindToLifecycle(lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA, preview, cap, analyzer)
                        cameraControl = cam.cameraControl
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        })

        // Live overlay
        if (uiState.liveMode && uiState.overlayBoxes.isNotEmpty()) {
            val accent = moduleAccent(selectedMode)
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                uiState.overlayBoxes.forEach { box ->
                    val l = box.left * size.width; val t = box.top * size.height
                    val w = (box.right - box.left) * size.width
                    val h = (box.bottom - box.top) * size.height
                    drawRect(color = accent.copy(.2f), topLeft = Offset(l, t), size = androidx.compose.ui.geometry.Size(w, h))
                    drawRect(color = accent, topLeft = Offset(l, t), size = androidx.compose.ui.geometry.Size(w, h), style = Stroke(2f))
                }
            }
        }

        // Gradients
        Box(Modifier.fillMaxWidth().height(150.dp).align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(.75f), Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(250.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.88f)))))

        // Top bar — back + flash only (no flip per doc requirement)
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically) {
            CameraIconButton(Icons.Rounded.ArrowBackIosNew, onClick = onBack)
            Text(selectedMode.title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
            CameraIconButton(if (flashEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff, onClick = {
                flashEnabled = !flashEnabled; cameraControl?.enableTorch(flashEnabled)
            })
        }

        // Mode chips
        LazyRow(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 62.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
            items(listOf(ModuleType.OBJECT_DETECTION, ModuleType.TEXT_SCANNER,
                ModuleType.STUDENT_HELPER, ModuleType.QR_SCANNER)) { mode ->
                val sel = mode == selectedMode
                val accent = moduleAccent(mode)
                FilterChip(selected = sel, onClick = { selectedMode = mode },
                    label = { Text(mode.title.split(" ").first()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent.copy(.25f), selectedLabelColor = accent,
                        containerColor = Color.Black.copy(.4f), labelColor = Color.White),
                    border = FilterChipDefaults.filterChipBorder(true, sel,
                        selectedBorderColor = accent, borderColor = Color.White.copy(.3f)))
            }
        }

        // Focus ring
        FocusRing(moduleAccent(selectedMode), Modifier.align(Alignment.Center))

        // Bottom
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {

            // Recent captures strip (scoped storage)
            RecentCapturesRow(context = context, onSelect = { path ->
                vm.processImagePath(path, selectedMode)
                onResult()
            })
            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth().padding(horizontal = 44.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically) {
                // Gallery
                CameraIconButton(Icons.Rounded.PhotoLibrary, 52.dp, onClick = {})
                // Shutter
                val scale by animateFloatAsState(if (uiState.isProcessing) .9f else 1f, label = "s")
                Box(Modifier.size(74.dp).scale(scale)
                    .background(Color.White.copy(.15f), CircleShape)
                    .border(3.dp, Color.White, CircleShape)
                    .clickable(enabled = !uiState.isProcessing) {
                        imageCapture?.let { cap -> vm.captureImage(cap, context, selectedMode) { onResult() } }
                    }, Alignment.Center) {
                    if (uiState.isProcessing)
                        CircularProgressIndicator(color = moduleAccent(selectedMode), modifier = Modifier.size(34.dp), strokeWidth = 3.dp)
                    else Box(Modifier.size(54.dp).background(Color.White, CircleShape))
                }
                // Live toggle
                CameraIconButton(
                    if (uiState.liveMode) Icons.Rounded.MotionPhotosAuto else Icons.Rounded.MotionPhotosOff,
                    52.dp, onClick = { vm.toggleLiveMode() })
            }
        }
    }
}

@Composable
private fun RecentCapturesRow(context: Context, onSelect: (String) -> Unit) {
    // Load from app-specific cache (scoped storage — no external permission needed)
    val recentFiles = remember {
        context.cacheDir.listFiles { f -> f.name.startsWith("sv_") && f.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }?.take(5) ?: emptyList()
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        recentFiles.forEach { file ->
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).clickable { onSelect(file.absolutePath) }) {
                AsyncImage(model = file, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        // Placeholders if empty
        repeat((5 - recentFiles.size).coerceAtLeast(0)) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(.1f)).border(1.dp, Color.White.copy(.2f), RoundedCornerShape(10.dp)),
                Alignment.Center) {
                Icon(Icons.Rounded.Image, null, tint = Color.White.copy(.35f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FocusRing(accent: Color, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "f")
    val a by inf.animateFloat(.4f, .9f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "a")
    Box(modifier.size(200.dp).border(2.dp, accent.copy(a), RoundedCornerShape(22.dp)))
}

@Composable
private fun PermRationale(onRequest: () -> Unit, onBack: () -> Unit) {
    val c = svColors
    Box(Modifier.fillMaxSize().background(c.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Rounded.CameraAlt, null, tint = c.primary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Camera Permission Required", style = MaterialTheme.typography.headlineSmall, color = c.onSurface)
            Spacer(Modifier.height(8.dp))
            Text("SmartVision needs camera access.", style = MaterialTheme.typography.bodyMedium, color = c.subtext)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest) { Text("Grant Permission") }
            TextButton(onClick = onBack) { Text("Go Back", color = c.subtext) }
        }
    }
}

@Composable
private fun PermDenied(onBack: () -> Unit) {
    val c = svColors
    Box(Modifier.fillMaxSize().background(c.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Rounded.NoPhotography, null, tint = c.error, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Camera Access Denied", style = MaterialTheme.typography.headlineSmall, color = c.onSurface)
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onBack) { Text("Go Back", color = c.subtext) }
        }
    }
}

// Shared button used by ResultScreen / QrScannerScreen
@Composable
fun CameraIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, size: Dp = 44.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).background(Color.Black.copy(.4f), CircleShape)
        .border(1.dp, Color.White.copy(.2f), CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.48f))
    }
}
