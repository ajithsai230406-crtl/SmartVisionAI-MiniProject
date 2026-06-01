package com.smartvision.ai.compose.screens

import android.Manifest
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.presentation.ocr.OcrScannerViewModel
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OcrScannerScreen(
    onBack: () -> Unit,
    onResult: (String) -> Unit,
    viewModel: OcrScannerViewModel = hiltViewModel()
) {
    val ext = MaterialTheme.extended
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var extractedText    by remember { mutableStateOf("Point camera at text...") }
    var flashEnabled     by remember { mutableStateOf(false) }
    val cameraRef        = remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val scope            = rememberCoroutineScope()

    LaunchedEffect(cameraPermission.status) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            if (cameraPermission.status.isGranted) {
                CameraPreview(
                    modifier         = Modifier.fillMaxSize(),
                    onCameraReady    = { cameraRef.value = it },
                    onTextRecognized = { text ->
                        scope.launch {
                            val result = viewModel.recognize(text, 0)
                            extractedText = result
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(DeepNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Camera permission required", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(16.dp))
                        NeonButton("Grant Permission", onClick = { cameraPermission.launchPermissionRequest() })
                    }
                }
            }

            // Top bar overlay
            SmartTopBar(
                title = "OCR Scanner",
                onBack = onBack,
                actions = {
                    // Flash toggle
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(GlassCard).border(1.dp, GlassBorder, CircleShape)
                            .clickable {
                                flashEnabled = !flashEnabled
                                cameraRef.value?.cameraControl?.enableTorch(flashEnabled)
                            },
                        contentAlignment = Alignment.Center
                    ) { Text(if (flashEnabled) "⚡" else "🔦", fontSize = 18.sp) }
                }
            )

            // Scanning frame overlay
            ScanFrame(modifier = Modifier.align(Alignment.Center))

            // Bottom panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(GlassCard)
                    .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Column {
                    Text("Extracted Text", style = MaterialTheme.typography.labelMedium, color = NeonBlue)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        extractedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 4
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonButton("Translate →", onClick = { onResult(extractedText) }, modifier = Modifier.weight(1f))
                        OutlineNeonButton("Copy", onClick = {
                            scope.launch { viewModel.saveManual(extractedText) }
                        }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onCameraReady: (androidx.camera.core.Camera) -> Unit,
    onTextRecognized: (android.media.Image) -> Unit
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor      = remember { Executors.newSingleThreadExecutor() }
    val processing    = remember { AtomicBoolean(false) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                val preview  = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    val img = proxy.image
                    if (img != null && processing.compareAndSet(false, true)) {
                        onTextRecognized(img)
                        processing.set(false)
                    }
                    proxy.close()
                }
                provider.unbindAll()
                val cam = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview, analysis
                )
                onCameraReady(cam)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

@Composable
private fun ScanFrame(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "scanLine"
    )
    Box(modifier = modifier.size(260.dp, 180.dp)) {
        // Corner brackets
        val cornerColor = NeonBlue
        // Draw corner brackets via canvas
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3f
            val len    = 40f
            val w = size.width; val h = size.height
            // TL
            drawLine(cornerColor, start = Offset(0f, len), end = Offset(0f, 0f), strokeWidth = stroke)
            drawLine(cornerColor, start = Offset(0f, 0f), end = Offset(len, 0f), strokeWidth = stroke)
            // TR
            drawLine(cornerColor, start = Offset(w - len, 0f), end = Offset(w, 0f), strokeWidth = stroke)
            drawLine(cornerColor, start = Offset(w, 0f), end = Offset(w, len), strokeWidth = stroke)
            // BL
            drawLine(cornerColor, start = Offset(0f, h - len), end = Offset(0f, h), strokeWidth = stroke)
            drawLine(cornerColor, start = Offset(0f, h), end = Offset(len, h), strokeWidth = stroke)
            // BR
            drawLine(cornerColor, start = Offset(w - len, h), end = Offset(w, h), strokeWidth = stroke)
            drawLine(cornerColor, start = Offset(w, h - len), end = Offset(w, h), strokeWidth = stroke)
            // Scan line
            drawLine(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, NeonBlue.copy(0.8f), Color.Transparent)),
                start = Offset(0f, h * scanY),
                end   = Offset(w, h * scanY),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
fun OcrResultScreen(
    extractedText: String,
    onBack: () -> Unit,
    onTranslate: () -> Unit
) {
    val ext = MaterialTheme.extended
    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(title = "OCR Translator", onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                NeonBadge("Detected: English", color = NeonBlue)
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ext.glassCard)
                        .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        extractedText.ifBlank { "No text extracted." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineNeonButton("Copy", onClick = {}, modifier = Modifier.weight(1f))
                    OutlineNeonButton("Share", onClick = {}, modifier = Modifier.weight(1f))
                    OutlineNeonButton("🔊 Speak", onClick = {}, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                NeonButton("Translate →", onClick = onTranslate, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
