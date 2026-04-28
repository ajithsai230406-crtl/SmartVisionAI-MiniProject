package com.smartvision.ai.ui.screens.qrscanner

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.smartvision.ai.ui.screens.camera.CameraIconButton
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.ui.screens.camera.CameraIconButton
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(
    onBack:    () -> Unit,
    viewModel: QrScannerViewModel = hiltViewModel()
) {
    val permission = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        if (!permission.status.isGranted) permission.launchPermissionRequest()
    }

    if (permission.status.isGranted) {
        QrCameraContent(onBack = onBack, viewModel = viewModel)
    }
}

@Composable
private fun QrCameraContent(
    onBack:    () -> Unit,
    viewModel: QrScannerViewModel
) {
    val colors         = smartColors
    val uiState by    viewModel.uiState.collectAsState()
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val accent        = SmartVisionColors.qrScanner

    // Auto-open URL when detected
    LaunchedEffect(uiState.result) {
        val res = uiState.result
        if (res is ScanResult.QrCodeResult && res.type == QrType.URL && uiState.autoOpen) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.rawValue))
                context.startActivity(intent)
                viewModel.setAutoOpen(false) // prevent re-opening
            } catch (e: Exception) { /* invalid URL */ }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera Preview ────────────────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType          = PreviewView.ScaleType.FILL_CENTER
                }
                scope.launch {
                    val provider = ProcessCameraProvider.getInstance(ctx).await()
                    val preview  = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().apply {
                            setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                                viewModel.analyze(proxy)
                            }
                        }
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
                    } catch (e: Exception) { e.printStackTrace() }
                }
                previewView
            }
        )

        // ── Gradients ─────────────────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth().height(140.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(0.8f), Color.Transparent)))
        )
        Box(
            Modifier.fillMaxWidth().height(200.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f))))
        )

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            CameraIconButton(Icons.Rounded.Close, onClick = onBack)
            Text("QR Scanner", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
            CameraIconButton(
                icon    = if (uiState.autoOpen) Icons.Rounded.LinkOff else Icons.Rounded.OpenInBrowser,
                onClick = { viewModel.setAutoOpen(!uiState.autoOpen) }
            )
        }

        // ── QR Viewfinder ─────────────────────────────────────────────────────
        QrViewfinder(
            isDetected  = uiState.result is ScanResult.QrCodeResult,
            accent      = accent,
            modifier    = Modifier.align(Alignment.Center).size(260.dp)
        )

        // ── Scan line animation ───────────────────────────────────────────────
        if (uiState.result == null) {
            ScanLine(accent = accent, modifier = Modifier.align(Alignment.Center).size(260.dp))
        }

        // ── Result panel ──────────────────────────────────────────────────────
        QrResultPanel(
            result   = uiState.result,
            autoOpen = uiState.autoOpen,
            accent   = accent,
            onOpenLink = { url ->
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {}
            },
            onCopy   = viewModel::copyResult,
            onRescan = viewModel::rescan,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        )

        // Auto-open toggle label
        if (uiState.autoOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 64.dp, end = 16.dp)
                    .background(accent.copy(0.2f), RoundedCornerShape(8.dp))
                    .border(1.dp, accent.copy(0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("Auto-open ON", style = MaterialTheme.typography.labelSmall, color = accent)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QR VIEWFINDER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QrViewfinder(isDetected: Boolean, accent: Color, modifier: Modifier = Modifier) {
    val color = if (isDetected) Color(0xFF00E676) else accent
    val alpha by animateFloatAsState(if (isDetected) 1f else 0.7f, label = "qrAlpha")

    Box(modifier = modifier) {
        val cornerLen = 32.dp
        val thickness = 3.dp
        // Corners
        listOf(
            Alignment.TopStart    to Pair(true,  true),
            Alignment.TopEnd      to Pair(true,  false),
            Alignment.BottomStart to Pair(false, true),
            Alignment.BottomEnd   to Pair(false, false)
        ).forEach { (alignment, flags) ->
            val (isTop, isLeft) = flags
            Box(modifier = Modifier.align(alignment)) {
                // Horizontal bar
                Box(Modifier.width(cornerLen).height(thickness)
                    .offset(x = if (!isLeft) (-cornerLen) else 0.dp, y = if (!isTop) (-thickness) else 0.dp)
                    .background(color.copy(alpha), if (isLeft) RoundedCornerShape(topStart = 4.dp) else RoundedCornerShape(topEnd = 4.dp))
                )
                // Vertical bar
                Box(Modifier.width(thickness).height(cornerLen)
                    .offset(x = if (!isLeft) (-thickness) else 0.dp, y = if (!isTop) (-cornerLen) else 0.dp)
                    .background(color.copy(alpha), if (isTop) RoundedCornerShape(topStart = 4.dp) else RoundedCornerShape(bottomStart = 4.dp))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCAN LINE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScanLine(accent: Color, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "scanLine")
    val offset by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label         = "lineOffset"
    )
    Canvas(modifier = modifier) {
        val y = size.height * offset
        drawLine(
            brush       = Brush.horizontalGradient(listOf(Color.Transparent, accent, Color.Transparent)),
            start       = Offset(0f, y),
            end         = Offset(size.width, y),
            strokeWidth = 2.dp.toPx()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QR RESULT PANEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QrResultPanel(
    result:     ScanResult?,
    autoOpen:   Boolean,
    accent:     Color,
    onOpenLink: (String) -> Unit,
    onCopy:     () -> Unit,
    onRescan:   () -> Unit,
    modifier:   Modifier = Modifier
) {
    val colors = smartColors

    if (result == null) {
        // Scanning hint
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Point at a QR code",
                style     = MaterialTheme.typography.titleMedium,
                color     = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                "Supports URL, Text, WiFi, Email, Phone & more",
                style     = MaterialTheme.typography.bodySmall,
                color     = Color.White.copy(0.6f),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    if (result is ScanResult.QrCodeResult) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .border(1.dp, accent.copy(0.4f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.background(accent.copy(0.15f), RoundedCornerShape(10.dp)).padding(8.dp)
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(result.type.name, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
                    Text("QR Code detected", style = MaterialTheme.typography.bodySmall, color = colors.subtext)
                }
            }

            Text(
                result.displayValue,
                style   = MaterialTheme.typography.bodyMedium,
                color   = colors.onSurface,
                maxLines = 3
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (result.type == QrType.URL) {
                    Button(
                        onClick  = { onOpenLink(result.rawValue) },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = accent),
                        shape    = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open Link", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    }
                }
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(if (result.type == QrType.URL) 0.7f else 1f),
                    border  = BorderStroke(1.dp, colors.cardBorder),
                    shape   = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onRescan,
                    modifier = Modifier.weight(0.6f),
                    border  = BorderStroke(1.dp, colors.cardBorder),
                    shape   = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
