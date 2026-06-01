package com.example.smartvisionai.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.example.smartvisionai.ui.components.*
import com.example.smartvisionai.ui.theme.*
import com.example.smartvisionai.viewmodel.ScanViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class ScanMode(val label: String, val icon: String) {
    OBJECT("Object Detect", "👁"),
    OCR("OCR / Text", "T"),
    TRANSLATE("Translate", "文A"),
    STUDENT("Student Help", "🎓"),
    MEDICAL("Medical", "💊"),
    WASTE("Waste Classify", "♻")
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(viewModel: ScanViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val camPerm  = rememberPermissionState(Manifest.permission.CAMERA)
    val micPerm  = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (!camPerm.status.isGranted) camPerm.launchPermissionRequest()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        ScanHeader(
            isProcessing = uiState.isProcessing,
            onMicClick   = {
                if (!micPerm.status.isGranted) micPerm.launchPermissionRequest()
                else viewModel.startVoiceInput()
            }
        )

        ModeSelectorRow(
            selectedMode = uiState.selectedMode,
            onModeSelect = { viewModel.setMode(it) }
        )

        // Camera area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0D1520))
        ) {
            if (camPerm.status.isGranted) {
                CameraPreviewBox(
                    modifier        = Modifier.fillMaxSize(),
                    onFrameCaptured = { viewModel.analyzeFrame(it) },
                    facing          = uiState.cameraFacing
                )
                if (uiState.detectionBoxes.isNotEmpty()) {
                    ObjectDetectionOverlay(
                        modifier      = Modifier.fillMaxSize(),
                        detections    = uiState.detectionBoxes,
                        previewWidth  = uiState.previewWidth,
                        previewHeight = uiState.previewHeight
                    )
                }
                ScanOverlay(modifier = Modifier.fillMaxSize(), isProcessing = uiState.isProcessing)
            } else {
                PermissionDeniedView(onRequest = { camPerm.launchPermissionRequest() })
            }

            // Status chip
            uiState.statusLabel?.let {
                StatusChip(
                    label    = it,
                    isOnline = uiState.isOnline,
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible  = uiState.isProcessing,
                enter    = fadeIn(),
                exit     = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
            ) {
                ProcessingBanner()
            }
        }

        // Result panel
        this.AnimatedVisibility(
            visible = uiState.result != null,
            enter   = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(200)),
            exit    = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(tween(150))
        ) {
            uiState.result?.let {
                ResultPanel(result = it, onDismiss = { viewModel.clearResult() })
            }
        }

        BottomActionBar(
            isProcessing = uiState.isProcessing,
            onCapture    = { viewModel.captureAndAnalyze() },
            onFlip       = { viewModel.flipCamera() },
            onVoice      = {
                if (!micPerm.status.isGranted) micPerm.launchPermissionRequest()
                else viewModel.startVoiceInput()
            },
            onAsk        = { viewModel.askQuestion(it) }
        )
    }

    if (uiState.isListeningVoice) {
        VoiceInputDialog(
            state       = VoiceState.LISTENING,
            partialText = uiState.voicePartialText,
            onDismiss   = { viewModel.stopVoiceInput() }
        )
    }
}

@Composable
fun ScanHeader(isProcessing: Boolean, onMicClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("SCAN MODE", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = CyanAccent, letterSpacing = 1.sp)
            Text(
                if (isProcessing) "Analyzing…" else "Starting camera…",
                fontSize = 12.sp, color = TextSecondary
            )
        }
        IconButton(
            onClick  = onMicClick,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(CardDark)
        ) {
            Icon(Icons.Default.Mic, "Voice", tint = CyanAccent, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ModeSelectorRow(selectedMode: ScanMode, onModeSelect: (ScanMode) -> Unit) {
    LazyRow(
        modifier        = Modifier.fillMaxWidth(),
        contentPadding  = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ScanMode.values()) { mode ->
            val active = mode == selectedMode
            Surface(
                onClick = { onModeSelect(mode) },
                shape   = RoundedCornerShape(20.dp),
                color   = if (active) CyanAccent.copy(.15f) else CardDark,
                border  = BorderStroke(1.dp, if (active) CyanAccent else CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(mode.icon, fontSize = 12.sp)
                    Text(
                        mode.label,
                        fontSize   = 12.sp,
                        color      = if (active) CyanAccent else TextSecondary,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun CameraPreviewBox(
    modifier: Modifier,
    onFrameCaptured: (Bitmap) -> Unit,
    facing: Int
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(facing) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview  = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor) { proxy ->
                            runCatching { onFrameCaptured(proxy.toBitmap()) }
                            proxy.close()
                        }
                    }

                val selector = if (facing == CameraSelector.LENS_FACING_BACK)
                    CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (e: Exception) {
                Log.e("CameraPreview", "Bind failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { executor.shutdown() }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
fun ScanOverlay(modifier: Modifier, isProcessing: Boolean) {
    val inf      = rememberInfiniteTransition(label = "scan")
    val lineY    by inf.animateFloat(0f, 1f,   infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "ly")
    val cAlpha   by inf.animateFloat(.5f, 1f,  infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "ca")

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val len = 36f
        val clr = CyanAccent.copy(if (isProcessing) cAlpha else .65f)
        val pnt = Paint().apply { color = clr; strokeWidth = 2.5f; style = PaintingStyle.Stroke; strokeCap = StrokeCap.Round }
        drawContext.canvas.run {
            drawLine(Offset(0f, len), Offset(0f, 0f), pnt);   drawLine(Offset(0f, 0f), Offset(len, 0f), pnt)
            drawLine(Offset(w - len, 0f), Offset(w, 0f), pnt); drawLine(Offset(w, 0f), Offset(w, len), pnt)
            drawLine(Offset(0f, h - len), Offset(0f, h), pnt); drawLine(Offset(0f, h), Offset(len, h), pnt)
            drawLine(Offset(w - len, h), Offset(w, h), pnt);   drawLine(Offset(w, h), Offset(w, h - len), pnt)
        }
        if (isProcessing) {
            val y = lineY * h
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent, CyanAccent.copy(.85f), Color.Transparent)), Offset(0f, y), Offset(w, y), 2f)
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent, CyanAccent.copy(.12f), Color.Transparent)), Offset(0f, y + 4f), Offset(w, y + 4f), 8f)
        }
    }
}

@Composable
fun StatusChip(label: String, isOnline: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = Color(0xCC0F1820)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape)
                .background(if (isOnline) CyanAccent else Color(0xFFEF5350)))
            Text(label, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
fun PermissionDeniedView(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CameraAlt, null, tint = TextMuted, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("Permission dismissed", fontSize = 13.sp, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRequest) { Text("Grant Permission", color = CyanAccent) }
    }
}

@Composable
fun ResultPanel(result: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape    = RoundedCornerShape(16.dp),
        color    = CardDark,
        border   = BorderStroke(1.dp, CyanAccent.copy(.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically) {
                Text("RESULT", fontSize = 10.sp, color = CyanAccent, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(result, fontSize = 13.sp, color = TextPrimary, lineHeight = 21.sp,
                modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp).verticalScroll(rememberScrollState()))
        }
    }
}

@Composable
fun BottomActionBar(
    isProcessing: Boolean,
    onCapture: () -> Unit,
    onFlip: () -> Unit,
    onVoice: () -> Unit,
    onAsk: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val pulse by rememberInfiniteTransition(label = "p")
        .animateFloat(.94f, 1.06f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "ps")

    Column(modifier = Modifier.fillMaxWidth().background(SurfaceDark).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)).background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query, onValueChange = { query = it },
                modifier  = Modifier.weight(1f).padding(vertical = 10.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Ask anything about scanned content…", fontSize = 13.sp, color = TextMuted)
                    inner()
                },
                singleLine = true
            )
            this.AnimatedVisibility(visible = query.isNotEmpty()) {
                IconButton(onClick = { onAsk(query); query = "" }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Send, null, tint = CyanAccent, modifier = Modifier.size(17.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically) {

            IconButton(onClick = onFlip,
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(CardDark).border(1.dp, CardBorder, CircleShape)) {
                Icon(Icons.Default.FlipCameraAndroid, "Flip", tint = TextSecondary, modifier = Modifier.size(22.dp))
            }

            Box(
                modifier = Modifier.size(70.dp).scale(if (isProcessing) pulse else 1f)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(CyanAccent, Color(0xFF00A896), Color(0xFF7C3AED))))
                    .clickable { onCapture() },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (isProcessing) Icons.Default.Stop else Icons.Default.CameraAlt,
                    "Capture", tint = Color.White, modifier = Modifier.size(28.dp))
            }

            IconButton(onClick = onVoice,
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(CardDark).border(1.dp, CardBorder, CircleShape)) {
                Icon(Icons.Default.Mic, "Voice", tint = TextSecondary, modifier = Modifier.size(22.dp))
            }
        }
    }
}
