package com.smartvision.ai.ui.screens.qrscanner

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.smartvision.ai.domain.models.QrType
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.ui.screens.camera.CameraIconButton
import com.smartvision.ai.ui.theme.SVColors
import com.smartvision.ai.ui.theme.svColors
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import com.google.accompanist.permissions.isGranted
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    vm: QrScannerViewModel = hiltViewModel()
) {

    val permissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    if (!permissionState.status.isGranted) return

    val colors = svColors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val state by vm.uiState.collectAsState()

    val accent = SVColors.orange

    // Auto open URL
    LaunchedEffect(state.result) {

        val result = state.result

        if (
            result is ScanResult.QrCodeResult &&
            result.type == QrType.URL &&
            state.autoOpen
        ) {

            try {

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(result.rawValue)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)

            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // Camera Preview
        AndroidView(

            modifier = Modifier.fillMaxSize(),

            factory = { ctx ->

                PreviewView(ctx).apply {

                    scope.launch {

                        val cameraProvider =
                            ProcessCameraProvider
                                .getInstance(ctx)
                                .get()

                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(surfaceProvider)
                            }

                        val imageAnalyzer =
                            ImageAnalysis.Builder()
                                .setBackpressureStrategy(
                                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                )
                                .build()
                                .also {

                                    it.setAnalyzer(
                                        Executors.newSingleThreadExecutor()
                                    ) { imageProxy ->

                                        vm.analyze(imageProxy)
                                    }
                                }

                        try {

                            cameraProvider.unbindAll()

                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalyzer
                            )

                        } catch (e: Exception) {

                            e.printStackTrace()
                        }
                    }
                }
            }
        )

        // Top Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),

            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            CameraIconButton(
                icon = Icons.Rounded.Close,
                onClick = onBack
            )

            Text(
                text = "QR Scanner",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            CameraIconButton(
                icon = if (state.autoOpen)
                    Icons.Rounded.OpenInBrowser
                else
                    Icons.Rounded.LinkOff,

                onClick = {
                    vm.setAutoOpen(!state.autoOpen)
                }
            )
        }

        // Scanner Frame
        QrViewfinder(
            detected = state.result != null,
            accent = accent,
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
        )

        // Scan Line
        if (state.result == null) {

            ScanLineAnimation(
                accent = accent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(260.dp)
            )
        }

        // Auto Open Badge
        if (state.autoOpen) {

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(
                        top = 62.dp,
                        end = 16.dp
                    )
                    .background(
                        accent.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    )
            ) {

                Text(
                    text = "Auto-open ON",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }

        // Bottom Result Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 28.dp
                )
        ) {

            if (state.result == null) {

                Text(
                    text = "Point at any QR code",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Supports URL, Text, WiFi, Email, Phone & more",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

            } else if (state.result is ScanResult.QrCodeResult) {

                val qr = state.result as ScanResult.QrCodeResult

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.surface)
                        .border(
                            width = 1.dp,
                            color = accent.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(18.dp),

                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .background(
                                accent.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 4.dp
                            )
                    ) {

                        Text(
                            text = qr.type.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = qr.displayValue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        maxLines = 3
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        if (qr.type == QrType.URL) {

                            Button(

                                onClick = {

                                    try {

                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(qr.rawValue)
                                            ).addFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK
                                            )
                                        )

                                    } catch (_: Exception) {
                                    }
                                },

                                modifier = Modifier.weight(1f),

                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent
                                ),

                                shape = RoundedCornerShape(12.dp)
                            ) {

                                Icon(
                                    imageVector = Icons.Rounded.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = "Open Link",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(

                            onClick = {
                                vm.rescan()
                            },

                            modifier = Modifier.weight(0.5f),

                            border = BorderStroke(
                                width = 1.dp,
                                color = colors.border
                            ),

                            shape = RoundedCornerShape(12.dp)

                        ) {

                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrViewfinder(
    detected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {

    val color = if (detected) {
        SVColors.green
    } else {
        accent
    }

    val infiniteTransition =
        rememberInfiniteTransition(label = "viewfinder")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(modifier = modifier) {

        val cornerSize = 22.dp
        val thickness = 3.dp

        listOf(
            Alignment.TopStart,
            Alignment.TopEnd,
            Alignment.BottomStart,
            Alignment.BottomEnd
        ).forEachIndexed { index, alignment ->

            Box(
                modifier = Modifier.align(alignment)
            ) {

                val isLeft = index % 2 == 0
                val isTop = index < 2

                Box(
                    modifier = Modifier
                        .width(cornerSize)
                        .height(thickness)
                        .offset(
                            x = if (!isLeft) -cornerSize else 0.dp,
                            y = if (!isTop) -thickness else 0.dp
                        )
                        .background(
                            color.copy(alpha = alpha),
                            CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .width(thickness)
                        .height(cornerSize)
                        .offset(
                            x = if (!isLeft) -thickness else 0.dp,
                            y = if (!isTop) -cornerSize else 0.dp
                        )
                        .background(
                            color.copy(alpha = alpha),
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun ScanLineAnimation(
    accent: Color,
    modifier: Modifier = Modifier
) {

    val infiniteTransition =
        rememberInfiniteTransition(label = "scan")

    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Canvas(modifier = modifier) {

        val y = size.height * offsetY

        drawLine(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    accent,
                    Color.Transparent
                )
            ),

            start = Offset(0f, y),

            end = Offset(size.width, y),

            strokeWidth = 2f
        )
    }
}