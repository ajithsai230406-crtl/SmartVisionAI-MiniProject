package com.smartvision.ai.ui.screens.qrscanner

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.ui.screens.camera.CameraIconButton
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(onBack: () -> Unit, vm: QrScannerViewModel = hiltViewModel()) {
    val perm = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) { if (!perm.status.isGranted) perm.launchPermissionRequest() }
    if (!perm.status.isGranted) return

    val c = svColors
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val s by vm.uiState.collectAsState()
    val accent = SVColors.orange

    // Auto-open URL in system browser
    LaunchedEffect(s.result) {
        val r = s.result
        if (r is ScanResult.QrCodeResult && r.type == QrType.URL && s.autoOpen) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(r.rawValue))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } catch (_: Exception) { }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PreviewView(context).apply {
                    scope.launch {
                        val provider = ProcessCameraProvider.getInstance(context).get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                        val analyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                            .also { it.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy -> vm.analyze(proxy) } }
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }
        )

        // Gradients
        Box(Modifier.fillMaxWidth().height(130.dp).align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(.8f), Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(220.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.9f)))))

        // Top bar
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically) {
            CameraIconButton(Icons.Rounded.Close, onClick = onBack)
            Text("QR Scanner", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
            CameraIconButton(if (s.autoOpen) Icons.Rounded.OpenInBrowser else Icons.Rounded.LinkOff,
                onClick = { vm.setAutoOpen(!s.autoOpen) })
        }

        // Viewfinder
        QrViewfinder(s.result != null, accent, Modifier.align(Alignment.Center).size(256.dp))

        // Scan line animation
        if (s.result == null) ScanLineAnimation(accent, Modifier.align(Alignment.Center).size(256.dp))

        // Auto-open badge
        if (s.autoOpen) Box(Modifier.align(Alignment.TopEnd).statusBarsPadding()
            .padding(top = 62.dp, end = 16.dp)
            .background(accent.copy(.2f), RoundedCornerShape(8.dp))
            .border(1.dp, accent.copy(.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text("Auto-open ON", style = MaterialTheme.typography.labelSmall, color = accent)
        }

        // Result panel
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp, start = 16.dp, end = 16.dp)) {
            if (s.result == null) {
                Text("Point at any QR code", style = MaterialTheme.typography.titleMedium,
                    color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Text("Supports URL, Text, WiFi, Email, Phone & more",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(.6f),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else if (s.result is ScanResult.QrCodeResult) {
                val qr = s.result as ScanResult.QrCodeResult
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(c.surface).border(1.dp, accent.copy(.4f), RoundedCornerShape(22.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.background(accent.copy(.15f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(qr.type.name, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
                    }
                    Text(qr.displayValue, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, maxLines = 3)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (qr.type == QrType.URL) Button(onClick = {
                            try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(qr.rawValue)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            catch (_: Exception) {}
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Rounded.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text("Open Link", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { vm.rescan() }, modifier = Modifier.weight(.5f),
                            border = BorderStroke(1.dp, c.border), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrViewfinder(detected: Boolean, accent: Color, modifier: Modifier = Modifier) {
    val color = if (detected) SVColors.green else accent
    val inf = rememberInfiniteTransition(label = "q")
    val a by inf.animateFloat(.6f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(modifier) {
        val ck = 22.dp; val th = 3.dp
        // Corners
        listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).forEachIndexed { i, align ->
            Box(Modifier.align(align)) {
                val isLeft = i % 2 == 0; val isTop = i < 2
                Box(Modifier.width(ck).height(th).offset(x = if (!isLeft) -ck else 0.dp, y = if (!isTop) -th else 0.dp)
                    .background(color.copy(a), CircleShape))
                Box(Modifier.width(th).height(ck).offset(x = if (!isLeft) -th else 0.dp, y = if (!isTop) -ck else 0.dp)
                    .background(color.copy(a), CircleShape))
            }
        }
    }
}

@Composable
private fun ScanLineAnimation(accent: Color, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "sl")
    val offset by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "o")
    Canvas(modifier) {
        val y = size.height * offset
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, accent, Color.Transparent)),
            start = Offset(0f, y),
            end   = Offset(size.width, y), strokeWidth = 2f)
    }
}
