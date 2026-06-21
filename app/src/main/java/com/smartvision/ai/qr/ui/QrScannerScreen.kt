package com.smartvision.ai.qr.ui

import android.Manifest
import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
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
import com.smartvision.ai.qr.domain.*
import com.smartvision.ai.qr.presentation.QrScannerViewModel
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

// ══════════════════════════════════════════════════════════════════════════════
// QR & BARCODE SCANNER — MAIN SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    viewModel: QrScannerViewModel = hiltViewModel()
) {
    val camPerm      = rememberPermissionState(Manifest.permission.CAMERA)
    val scanState    by viewModel.scanState.collectAsStateWithLifecycle()
    val history      by viewModel.history.collectAsStateWithLifecycle()
    val flashEnabled by viewModel.flashEnabled.collectAsStateWithLifecycle()
    val showHistory  by viewModel.showHistory.collectAsStateWithLifecycle()
    val cameraRef    = remember { mutableStateOf<Camera?>(null) }
    
    var isFrontCamera by remember { mutableStateOf(false) }
    var autoActionEnabled by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val ctx         = LocalContext.current
    val galleryLauncher = rememberGalleryPickerLauncher { uri ->
        viewModel.analyzeFromUri(ctx, uri)
    }

    LaunchedEffect(Unit) { if (!camPerm.status.isGranted) camPerm.launchPermissionRequest() }
    LaunchedEffect(flashEnabled) { cameraRef.value?.cameraControl?.enableTorch(flashEnabled) }

    // Beep + Haptic feedback + Auto Action launcher on successful scan
    LaunchedEffect(scanState) {
        if (scanState is QrScanState.Success) {
            val code = (scanState as QrScanState.Success).code
            
            // Trigger feedback
            try {
                val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                if (vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(80, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        vibrator.vibrate(80)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Auto Action trigger
            if (autoActionEnabled) {
                if (code.contentType == QrContentType.UPI) {
                    viewModel.payWithUpi(code.rawValue)
                } else {
                    handleQrAction(code, viewModel)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera feed ────────────────────────────────────────────────────────
        if (camPerm.status.isGranted && scanState is QrScanState.Idle) {
            QrCameraPreview(
                modifier      = Modifier.fillMaxSize(),
                isFront       = isFrontCamera,
                onCameraReady = { cameraRef.value = it },
                onFrame       = { img, rot -> viewModel.analyzeFrame(img, rot) }
            )
        }

        // ── Dark vignette ──────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                listOf(Color.Transparent, Color.Black.copy(0.65f)),
                radius = 700f
            )
        ))

        // ── Top HUD ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            QrTopHud(
                flashEnabled   = flashEnabled,
                historyCount   = history.size,
                onBack         = onBack,
                onFlash        = { viewModel.toggleFlash() },
                onHistory      = { viewModel.toggleHistory() },
                onGallery      = { galleryLauncher() },
                onCameraSwitch = { isFrontCamera = !isFrontCamera }
            )

            // Instruction Card capsule below App Bar
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(0.6f))
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ℹ️", fontSize = 12.sp)
                    Text(
                        text = "Point camera at any QR code or barcode",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Auto Action toggle switch below instruction
            Row(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(0.5f))
                    .border(1.dp, NeonBlue.copy(0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚡", fontSize = 10.sp, color = NeonBlue)
                Text("Auto Action Mode", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = autoActionEnabled,
                    onCheckedChange = { autoActionEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonBlue,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.Black
                    ),
                    modifier = Modifier.scale(0.7f).height(20.dp)
                )
            }
        }

        // ── Animated QR frame (Viewfinder Box) ──────────────────────────────────
        if (scanState is QrScanState.Idle) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnimatedQrFrame(isScanning = true)
            }
        }

        // ── Success overlay flash ──────────────────────────────────────────────
        AnimatedVisibility(
            visible  = scanState is QrScanState.Success,
            enter    = fadeIn(tween(100)), exit = fadeOut(tween(500))
        ) {
            Box(modifier = Modifier.fillMaxSize().background(NeonBlue.copy(0.08f)))
        }

        // ── Bottom information and Controls ────────────────────────────────────
        if (scanState is QrScanState.Idle) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Supported Formats Box
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(0.6f))
                        .border(1.dp, NeonBlue.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🔬", fontSize = 16.sp)
                        Text(
                            text = "Supports QR • EAN • Code128 • DataMatrix • Aztec • PDF417 • and more",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonBlue
                        )
                    }
                }
            }

            // Bottom Action Area containing only the Scan Button with a glow animation
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Pulsating glow background circle
                Box(
                    modifier = Modifier
                        .size((84 * glowScale).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    NeonBlue.copy(alpha = glowAlpha * 0.45f),
                                    NeonBlueDim.copy(alpha = glowAlpha * 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Scan Button Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(NeonBlue, NeonBlueDim)))
                            .border(3.dp, Color.White, CircleShape)
                            .clickable {
                                Toast.makeText(ctx, "Ready to Scan", Toast.LENGTH_SHORT).show()
                                viewModel.resumeScan()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔲", fontSize = 32.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Scan",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── Result card ────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = scanState is QrScanState.Success,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            (scanState as? QrScanState.Success)?.let { state ->
                QrResultCard(
                    code      = state.code,
                    onRescan  = { viewModel.resumeScan() },
                    onCopy    = { viewModel.copyToClipboard(state.code.rawValue) },
                    onShare   = { viewModel.shareText(state.code.rawValue) },
                    onAction  = {
                        if (state.code.contentType == QrContentType.UPI) {
                            viewModel.payWithUpi(state.code.rawValue)
                        } else {
                            handleQrAction(state.code, viewModel)
                        }
                    }
                )
            }
        }

        // ── History sheet ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = showHistory,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            QrHistorySheet(
                history   = history,
                onDismiss = { viewModel.toggleHistory() },
                onClear   = { viewModel.clearHistory() },
                onTap     = {
                    viewModel.copyToClipboard(it.rawValue)
                    Toast.makeText(ctx, "Copied content to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // ── Scanning indicator ────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = scanState is QrScanState.Scanning,
            enter    = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CircularProgressIndicator(color = NeonBlue)
                Text("Analyzing gallery QR code...", style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        }

        // ── Error overlay card ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = scanState is QrScanState.Error,
            enter    = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
        ) {
            val msg = (scanState as? QrScanState.Error)?.message ?: ""
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeonPink.copy(0.12f))
                    .border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
                    .clickable { viewModel.resumeScan() }
            ) {
                Text("⚠️ $msg (Tap to dismiss)", style = MaterialTheme.typography.bodySmall, color = NeonPink)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CAMERA PREVIEW
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QrCameraPreview(
    modifier: Modifier,
    isFront: Boolean,
    onCameraReady: (Camera) -> Unit,
    onFrame: suspend (android.media.Image, Int) -> Unit
) {
    val ctx            = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { Executors.newSingleThreadExecutor() }
    val processing     = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val scope          = rememberCoroutineScope()

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

    AndroidView(
        modifier = modifier,
        factory = { c ->
            PreviewView(c).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                scaleType    = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { pv ->
            val future = ProcessCameraProvider.getInstance(pv.context)
            future.addListener({
                try {
                    val prov = future.get()
                    prov.unbindAll()

                    val preview  = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                    val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    analysis.setAnalyzer(executor) { proxy ->
                        val img = proxy.image
                        if (img != null && processing.compareAndSet(false, true)) {
                            scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                                try {
                                    onFrame(img, proxy.imageInfo.rotationDegrees)
                                } catch (e: Exception) {
                                    android.util.Log.e("QrScanner", "Frame analysis failed", e)
                                } finally {
                                    processing.set(false)
                                    proxy.close()
                                }
                            }
                        } else {
                            proxy.close()
                        }
                    }
                    val cameraSelector = if (isFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    val cam = prov.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysis)
                    onCameraReady(cam)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(pv.context))
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// ANIMATED QR FRAME
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AnimatedQrFrame(isScanning: Boolean) {
    val inf    = rememberInfiniteTransition(label = "qrf")
    val glow   by inf.animateFloat(0.6f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "g")
    val scanY  by inf.animateFloat(0.05f, 0.95f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "s")
    val frameSize = 260.dp

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(frameSize)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val len = w * 0.20f; val str = 5f
            val color = Color(0xFF00B4FF)
            val glowC = color.copy(glow)

            // Corner brackets - TLR cyan glow
            // TL
            drawLine(glowC, Offset(0f, len), Offset(0f, 0f), str)
            drawLine(glowC, Offset(0f, 0f), Offset(len, 0f), str)
            // TR
            drawLine(glowC, Offset(w - len, 0f), Offset(w, 0f), str)
            drawLine(glowC, Offset(w, 0f), Offset(w, len), str)
            // BL
            drawLine(glowC, Offset(0f, h - len), Offset(0f, h), str)
            drawLine(glowC, Offset(0f, h), Offset(len, h), str)
            // BR
            drawLine(glowC, Offset(w - len, h), Offset(w, h), str)
            drawLine(glowC, Offset(w, h), Offset(w, h - len), str)

            // Outer glow rect
            drawRect(color.copy(0.08f * glow), topLeft = Offset(-6f, -6f), size = Size(w + 12, h + 12), style = Stroke(8f))

            // Scan line
            val y = h * scanY
            drawLine(
                brush       = Brush.horizontalGradient(listOf(Color.Transparent, color.copy(glow * 0.9f), Color.Transparent)),
                start       = Offset(0f, y), end = Offset(w, y), strokeWidth = 2f
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TOP HUD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QrTopHud(
    flashEnabled: Boolean,
    historyCount: Int,
    onBack: () -> Unit,
    onFlash: () -> Unit,
    onHistory: () -> Unit,
    onGallery: () -> Unit,
    onCameraSwitch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(0.55f)).border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(10.dp)).clickable(onClick = onBack), Alignment.Center) {
                Text("←", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("QR & Barcode Scanner", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("ML Kit • All Formats", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            HudActionBtn(icon = "🖼️", label = "Gallery", active = false, onClick = onGallery)
            Box {
                HudActionBtn(icon = "📜", label = "History", active = false, onClick = onHistory)
                if (historyCount > 0) {
                    Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp).size(16.dp).clip(CircleShape).background(NeonBlue), Alignment.Center) {
                        Text("$historyCount".take(2), style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            HudActionBtn(icon = "🔦", label = "Torch", active = flashEnabled, onClick = onFlash)
            HudActionBtn(icon = "🔄", label = "Camera", active = false, onClick = onCameraSwitch)
        }
    }
}

@Composable
private fun HudActionBtn(
    icon: String,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(52.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) NeonBlue else Color.White.copy(0.08f))
            .clickable(onClick = onClick)
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) Color.White else TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// QR RESULT CARD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QrResultCard(
    code: ScannedCode,
    onRescan: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onAction: () -> Unit
) {
    val color = Color(code.contentType.colorArgb)
    val timeFmt = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
    val formattedTime = timeFmt.format(Date(code.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Brush.verticalGradient(listOf(Color(0xF5060D1F), Color(0xFA040A14))))
            .border(1.dp, Brush.horizontalGradient(listOf(color.copy(0.6f), NeonPurple.copy(0.3f))), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .navigationBarsPadding()
    ) {
        Box(Modifier.padding(top = 10.dp).width(40.dp).height(4.dp).clip(CircleShape).background(color.copy(0.5f)).align(Alignment.CenterHorizontally))

        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // ── Header ────────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(0.15f)).border(1.dp, color.copy(0.4f), RoundedCornerShape(14.dp)), Alignment.Center) {
                        Text(code.contentType.emoji, fontSize = 26.sp)
                    }
                    Column {
                        Text(code.contentType.displayName, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(formattedTime, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                        NeonBadge(code.formatName, color)
                    }
                }
                Box(Modifier.clip(CircleShape).background(Color.White.copy(0.08f)).clickable(onClick = onRescan).padding(8.dp)) {
                    Text("🔄", fontSize = 16.sp)
                }
            }

            // Parse detailed values based on type
            when (code.contentType) {
                QrContentType.UPI -> {
                    val params = remember(code.rawValue) { parseUpiUri(code.rawValue) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(0.08f))
                            .border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("💳 UPI Transaction Info", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payee Name", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                            Text(params["pn"] ?: "Unknown", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("UPI ID", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                            Text(params["pa"] ?: "Unknown", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        if (params.containsKey("am")) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                                Text("₹ ${params["am"]}", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (params.containsKey("tn")) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Note", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                                Text(params["tn"] ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                            }
                        }
                    }
                }
                QrContentType.CONTACT -> {
                    val params = remember(code.rawValue) { parseVCard(code.rawValue) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(0.08f))
                            .border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("👤 Parsed Contact Info", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Full Name", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                            Text(params["name"] ?: code.displayValue, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        if (params.containsKey("phone")) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Phone", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                                Text(params["phone"] ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                        if (params.containsKey("email")) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Email", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                                Text(params["email"] ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                        if (params.containsKey("org")) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Company", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                                Text(params["org"] ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                    }
                }
                QrContentType.SMS -> {
                    val (phone, body) = remember(code.rawValue) { parseSmsUri(code.rawValue) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(0.08f))
                            .border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("💬 SMS Message Details", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Send To", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                            Text(phone, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        if (body.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text("Prefilled Message:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                            Text(body, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }
                QrContentType.EMAIL -> {
                    val (email, subject, body) = remember(code.rawValue) { parseMailtoUri(code.rawValue) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(0.08f))
                            .border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📧 Email Details", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Recipient", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                            Text(email, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        if (subject.isNotBlank()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subject", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                                Text(subject, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                        if (body.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text("Body:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                            Text(body, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }
                QrContentType.GEO -> {
                    val (lat, lng) = remember(code.rawValue) { parseGeoUri(code.rawValue) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(0.08f))
                            .border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📍 Location Coordinates", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Latitude", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                            Text(lat, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Longitude", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                            Text(lng, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                QrContentType.PRODUCT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(0.08f))
                            .border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🛍️ Barcode / Product Code", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Product Barcode", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                            Text(code.rawValue, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "This is a product barcode. Tap 'Search Online' below to look up this product catalog online.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.6f)
                        )
                    }
                }
                else -> {
                    // ── Content value ─────────────────────────────────────────────────
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(0.08f)).border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp)).padding(12.dp)
                    ) {
                        Text(code.displayValue.ifBlank { code.rawValue }, style = MaterialTheme.typography.bodyMedium, color = Color.White, lineHeight = 22.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // ── WiFi special view ─────────────────────────────────────────────
            code.wifiInfo?.let { wifi ->
                WifiInfoCard(wifi, color)
            }

            // ── Action buttons ────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val actLabel = if (code.contentType == QrContentType.PRODUCT) "🔍 Search Online" else code.contentType.actionLabel
                ActionBtn(
                    text = actLabel,
                    color = color,
                    modifier = Modifier.weight(2f),
                    onClick = onAction
                )
                ActionBtn(text = "📋 Copy", color = NeonBlue.copy(0.7f), modifier = Modifier.weight(1f), onClick = onCopy)
                ActionBtn(text = "↑ Share", color = NeonPurple.copy(0.7f), modifier = Modifier.weight(1f), onClick = onShare)
            }
        }
    }
}

// Helpers to parse UPI query parameters
private fun parseUpiUri(uriStr: String): Map<String, String> {
    val params = mutableMapOf<String, String>()
    try {
        val uri = android.net.Uri.parse(uriStr)
        uri.queryParameterNames.forEach { name ->
            val value = uri.getQueryParameter(name)
            if (value != null) {
                params[name] = value
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return params
}

// Helpers to parse SMS URI
private fun parseSmsUri(uriStr: String): Pair<String, String> {
    val clean = uriStr.substringAfter("sms:").substringAfter("smsto:")
    val parts = clean.split(":", limit = 2)
    val phone = parts.getOrNull(0)?.substringBefore("?") ?: ""
    var body = ""
    if (parts.size > 1) {
        body = parts[1]
    } else if (clean.contains("body=")) {
        body = clean.substringAfter("body=").substringBefore("&")
    }
    return Pair(phone, body)
}

// Helpers to parse Mailto URI
private fun parseMailtoUri(uriStr: String): Triple<String, String, String> {
    val clean = uriStr.substringAfter("mailto:")
    val email = clean.substringBefore("?")
    var subject = ""
    var body = ""
    if (clean.contains("subject=")) {
        subject = clean.substringAfter("subject=").substringBefore("&")
    }
    if (clean.contains("body=")) {
        body = clean.substringAfter("body=").substringBefore("&")
    }
    return Triple(email, subject, body)
}

// Helpers to parse Location GEO URI
private fun parseGeoUri(geoStr: String): Pair<String, String> {
    val clean = geoStr.substringAfter("geo:")
    val coords = clean.substringBefore("?").split(",")
    val lat = coords.getOrNull(0) ?: ""
    val lng = coords.getOrNull(1) ?: ""
    return Pair(lat, lng)
}

// Helpers to parse vCard parameters
private fun parseVCard(vcardStr: String): Map<String, String> {
    val params = mutableMapOf<String, String>()
    try {
        vcardStr.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("FN:", ignoreCase = true)) {
                params["name"] = trimmed.substring(3)
            }
            if (trimmed.startsWith("TEL:", ignoreCase = true)) {
                params["phone"] = trimmed.substring(4)
            }
            if (trimmed.startsWith("EMAIL:", ignoreCase = true)) {
                params["email"] = trimmed.substring(6)
            }
            if (trimmed.startsWith("ORG:", ignoreCase = true)) {
                params["org"] = trimmed.substring(4)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return params
}

@Composable
private fun WifiInfoCard(wifi: WifiInfo, color: Color) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(0.10f)).border(1.dp, color.copy(0.3f), RoundedCornerShape(12.dp)).padding(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("📶 WiFi Network", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SSID", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                Text(wifi.ssid, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Security", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                Text(wifi.security, style = MaterialTheme.typography.bodySmall, color = color)
            }
            if (wifi.password.isNotBlank()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Password", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                    Text(wifi.password, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ActionBtn(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(0.15f)).border(1.dp, color.copy(0.4f), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold) }
}

// ══════════════════════════════════════════════════════════════════════════════
// HISTORY SHEET
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QrHistorySheet(history: List<ScannedCode>, onDismiss: () -> Unit, onClear: () -> Unit, onTap: (ScannedCode) -> Unit) {
    val ext = MaterialTheme.extended
    val fmt = SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth().fillMaxHeight(0.6f)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(if (ext.isDark) Color(0xF5060D1F) else Color(0xF5F8FAFF))
            .border(1.dp, ext.glassBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .navigationBarsPadding()
    ) {
        Box(Modifier.padding(top = 10.dp).width(40.dp).height(4.dp).clip(CircleShape).background(NeonBlue.copy(0.5f)).align(Alignment.CenterHorizontally))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("📜 Scan History", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (history.isNotEmpty()) Box(Modifier.clip(RoundedCornerShape(8.dp)).background(NeonPink.copy(0.12f)).border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(8.dp)).clickable(onClick = onClear).padding(horizontal = 10.dp, vertical = 5.dp)) { Text("Clear", style = MaterialTheme.typography.labelSmall, color = NeonPink) }
                Box(Modifier.clip(CircleShape).background(MaterialTheme.extended.glassCard).clickable(onClick = onDismiss).padding(6.dp)) { Text("✕", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp) }
            }
        }
        HorizontalDivider(color = ext.glassBorder)
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No scans yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history, key = { it.id }) { code ->
                    val color = Color(code.contentType.colorArgb)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(0.07f)).border(1.dp, color.copy(0.2f), RoundedCornerShape(12.dp)).clickable { onTap(code) }.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(code.contentType.emoji, fontSize = 22.sp)
                        Column(Modifier.weight(1f)) {
                            Text(code.contentType.displayName, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
                            Text(code.displayValue.ifBlank { code.rawValue }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(fmt.format(code.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ACTION HANDLER
// ══════════════════════════════════════════════════════════════════════════════

private fun handleQrAction(code: ScannedCode, vm: QrScannerViewModel) {
    when (code.contentType) {
        QrContentType.URL     -> vm.openUrl(code.rawValue)
        QrContentType.EMAIL   -> vm.sendEmail(code.displayValue)
        QrContentType.PHONE   -> vm.dialPhone(code.displayValue)
        QrContentType.SMS     -> vm.sendSms(code.displayValue)
        QrContentType.GEO     -> vm.openMap(code.displayValue)
        QrContentType.WIFI    -> vm.copyToClipboard(code.wifiInfo?.password ?: "")
        QrContentType.PRODUCT -> vm.openUrl("https://www.google.com/search?q=${code.rawValue}")
        else                  -> vm.copyToClipboard(code.rawValue)
    }
}
