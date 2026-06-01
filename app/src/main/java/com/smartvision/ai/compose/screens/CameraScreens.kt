package com.smartvision.ai.compose.screens

import android.Manifest
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import com.google.accompanist.permissions.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(onBack: () -> Unit) {
    val ext = MaterialTheme.extended
    val camPerm = rememberPermissionState(Manifest.permission.CAMERA)
    var scannedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!camPerm.status.isGranted) camPerm.launchPermissionRequest()
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera preview
            if (camPerm.status.isGranted) {
                QrCameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onQrDetected = { scannedUrl = it }
                )
            }

            SmartTopBar(
                title = "QR Scanner",
                onBack = onBack,
                actions = {
                    Text("⚡", fontSize = 22.sp, modifier = Modifier.clickable {})
                    Spacer(Modifier.width(8.dp))
                    Text("🖼️", fontSize = 22.sp, modifier = Modifier.clickable {})
                }
            )

            // Corner bracket overlay
            Box(modifier = Modifier.align(Alignment.Center).size(220.dp)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val len  = 50f
                    val w    = size.width; val h = size.height
                    val c    = NeonBlue
                    val str  = 4f
                    drawLine(c, Offset(0f, len), Offset(0f, 0f), str)
                    drawLine(c, Offset(0f, 0f), Offset(len, 0f), str)
                    drawLine(c, Offset(w - len, 0f), Offset(w, 0f), str)
                    drawLine(c, Offset(w, 0f), Offset(w, len), str)
                    drawLine(c, Offset(0f, h - len), Offset(0f, h), str)
                    drawLine(c, Offset(0f, h), Offset(len, h), str)
                    drawLine(c, Offset(w - len, h), Offset(w, h), str)
                    drawLine(c, Offset(w, h - len), Offset(w, h), str)
                }
            }

            // Bottom result panel
            scannedUrl?.let { url ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(ext.glassCard)
                        .border(1.dp, ext.glassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(20.dp)
                        .navigationBarsPadding()
                ) {
                    Column {
                        NeonBadge("Website", NeonBlue)
                        Spacer(Modifier.height(8.dp))
                        Text(url, style = MaterialTheme.typography.bodyMedium, color = NeonBlue)
                        Spacer(Modifier.height(12.dp))
                        NeonButton("Open Link", onClick = {}, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCameraPreview(modifier: Modifier, onQrDetected: (String) -> Unit) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { ctx ->
            val pv = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            ProcessCameraProvider.getInstance(ctx).addListener({
                val prov    = ProcessCameraProvider.getInstance(ctx).get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                prov.unbindAll()
                prov.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            }, ContextCompat.getMainExecutor(ctx))
            pv
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ObjectDetectorScreen(onBack: () -> Unit) {
    val ext = MaterialTheme.extended
    val camPerm = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) { if (!camPerm.status.isGranted) camPerm.launchPermissionRequest() }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            if (camPerm.status.isGranted) {
                QrCameraPreview(modifier = Modifier.fillMaxSize(), onQrDetected = {})
            }
            SmartTopBar(title = "Object Detector", onBack = onBack)

            // Demo detection overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(140.dp, 100.dp)
                    .border(2.dp, NeonGreen, RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier.align(Alignment.Center).offset(y = (-60).dp)
            ) {
                NeonBadge("Plant 🌱", NeonGreen)
            }

            // Bottom info card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(ext.glassCard)
                    .border(1.dp, ext.glassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Column {
                    Text("Plant", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Category: Living Thing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Plants are living organisms that grow in soil or water. They produce oxygen and are essential for life.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Uses: Decoration, Medicinal, Improve air quality", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    NeonButton("Learn More", onClick = {}, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
