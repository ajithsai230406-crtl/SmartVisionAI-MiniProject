package com.smartvision.ai.qr.ui

import android.Manifest
import android.view.ViewGroup
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.qr.domain.*
import com.smartvision.ai.qr.presentation.QrScannerViewModel
import com.smartvision.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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

    LaunchedEffect(Unit) { if (!camPerm.status.isGranted) camPerm.launchPermissionRequest() }
    LaunchedEffect(flashEnabled) { cameraRef.value?.cameraControl?.enableTorch(flashEnabled) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera feed ────────────────────────────────────────────────────────
        if (camPerm.status.isGranted) {
            QrCameraPreview(
                modifier      = Modifier.fillMaxSize(),
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

        // ── Animated QR frame ─────────────────────────────────────────────────
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
            Box(modifier = Modifier.fillMaxSize().background(NeonGreen.copy(0.08f)))
        }

        // ── Top HUD ────────────────────────────────────────────────────────────
        QrTopHud(
            flashEnabled = flashEnabled,
            historyCount = history.size,
            onBack       = onBack,
            onFlash      = { viewModel.toggleFlash() },
            onHistory    = { viewModel.toggleHistory() }
        )

        // ── Bottom hint ────────────────────────────────────────────────────────
        if (scanState is QrScanState.Idle) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Point camera at any QR code or barcode", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                Text("Supports QR · EAN · Code128 · DataMatrix · Aztec", style = MaterialTheme.typography.labelSmall, color = NeonBlue.copy(0.6f))
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
                    onAction  = { handleQrAction(state.code, viewModel) }
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
                onTap     = { viewModel.copyToClipboard(it.rawValue) }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CAMERA PREVIEW
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QrCameraPreview(modifier: Modifier, onCameraReady: (Camera) -> Unit, onFrame: (android.media.Image, Int) -> Unit) {
    val ctx            = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { Executors.newSingleThreadExecutor() }
    val processing     = remember { AtomicBoolean(false) }
    AndroidView(modifier = modifier, factory = { c ->
        val pv = PreviewView(c).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType    = PreviewView.ScaleType.FILL_CENTER
        }
        ProcessCameraProvider.getInstance(c).addListener({
            val prov     = ProcessCameraProvider.getInstance(c).get()
            val preview  = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor) { proxy ->
                val img = proxy.image
                if (img != null && processing.compareAndSet(false, true)) {
                    onFrame(img, proxy.imageInfo.rotationDegrees)
                    processing.set(false)
                }
                proxy.close()
            }
            val cam = prov.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            onCameraReady(cam)
        }, ContextCompat.getMainExecutor(c))
        pv
    })
}

// ══════════════════════════════════════════════════════════════════════════════
// ANIMATED QR FRAME
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AnimatedQrFrame(isScanning: Boolean) {
    val inf    = rememberInfiniteTransition(label = "qrf")
    val glow   by inf.animateFloat(0.6f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "g")
    val scanY  by inf.animateFloat(0.05f, 0.95f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "s")
    val frameSize = 260.dp

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(frameSize)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val len = w * 0.20f; val str = 4.5f
            val color = Color(0xFF00B4FF)
            val glowC = color.copy(glow)

            // Corner brackets
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
private fun QrTopHud(flashEnabled: Boolean, historyCount: Int, onBack: () -> Unit, onFlash: () -> Unit, onHistory: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(0.55f)).border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(10.dp)).clickable(onClick = onBack), Alignment.Center) {
                Text("←", color = Color.White)
            }
            Column {
                Text("QR & Barcode Scanner", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("ML Kit · All formats", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // History button with badge
            Box {
                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(0.55f)).border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(10.dp)).clickable(onClick = onHistory).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("📜", fontSize = 16.sp)
                }
                if (historyCount > 0) {
                    Box(Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(NeonBlue), Alignment.Center) {
                        Text("$historyCount".take(2), style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(0.55f)).border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(10.dp)).clickable(onClick = onFlash).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(if (flashEnabled) "⚡" else "🔦", fontSize = 16.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// QR RESULT CARD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QrResultCard(code: ScannedCode, onRescan: () -> Unit, onCopy: () -> Unit, onShare: () -> Unit, onAction: () -> Unit) {
    val color = Color(code.contentType.colorArgb)
    val ext   = MaterialTheme.extended

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
                        NeonBadge(code.formatName, color)
                    }
                }
                Box(Modifier.clip(CircleShape).background(Color.White.copy(0.08f)).clickable(onClick = onRescan).padding(8.dp)) {
                    Text("🔄", fontSize = 16.sp)
                }
            }

            // ── Content value ─────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(0.08f)).border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp)).padding(12.dp)
            ) {
                Text(code.displayValue.ifBlank { code.rawValue }, style = MaterialTheme.typography.bodyMedium, color = Color.White, lineHeight = 22.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
            }

            // ── WiFi special view ─────────────────────────────────────────────
            code.wifiInfo?.let { wifi ->
                WifiInfoCard(wifi, color)
            }

            // ── Action buttons ────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBtn(text = code.contentType.actionLabel, color = color, modifier = Modifier.weight(2f), onClick = onAction)
                ActionBtn(text = "📋 Copy", color = NeonBlue.copy(0.7f), modifier = Modifier.weight(1f), onClick = onCopy)
                ActionBtn(text = "↑ Share", color = NeonPurple.copy(0.7f), modifier = Modifier.weight(1f), onClick = onShare)
            }
        }
    }
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
        else                  -> vm.copyToClipboard(code.rawValue)
    }
}
