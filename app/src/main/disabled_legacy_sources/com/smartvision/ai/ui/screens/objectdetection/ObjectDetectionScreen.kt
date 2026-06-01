package com.smartvision.ai.ui.screens.objectdetection

import androidx.compose.animation.*
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
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.ui.components.*
import com.smartvision.ai.ui.theme.*

@Composable
fun ObjectDetectionScreen(
    onBack:    () -> Unit,
    onCapture: () -> Unit,
    viewModel: ObjectDetectionViewModel = hiltViewModel()
) {
    val colors  = smartColors
    val uiState by viewModel.uiState.collectAsState()
    val accent  = SmartVisionColors.objectDetect

    Scaffold(
        topBar         = { SmartVisionTopBar(title = "Object Detection", onBack = onBack) },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Image with bounding-box overlay ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.card)
                    .border(1.dp, accent.copy(0.3f), RoundedCornerShape(22.dp))
            ) {
                if (uiState.imagePath != null) {
                    AsyncImage(
                        model              = uiState.imagePath,
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                    // Bounding boxes overlay
                    val result = uiState.result
                    if (result is ScanResult.ObjectDetectionResult) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            result.objects.forEachIndexed { idx, obj ->
                                val boxColor = listOf(
                                    accent,
                                    SmartVisionColors.translator,
                                    SmartVisionColors.voiceTranslator,
                                    SmartVisionColors.studentHelper
                                )[idx % 4]
                                val bb = obj.boundingBox
                                val left   = bb.left   * size.width
                                val top    = bb.top    * size.height
                                val right  = bb.right  * size.width
                                val bottom = bb.bottom * size.height

                                // Fill
                                drawRect(
                                    color   = boxColor.copy(alpha = 0.18f),
                                    topLeft = Offset(left, top),
                                    size    = Size(right - left, bottom - top)
                                )
                                // Stroke
                                drawRect(
                                    color   = boxColor,
                                    topLeft = Offset(left, top),
                                    size    = Size(right - left, bottom - top),
                                    style   = Stroke(width = 2.dp.toPx())
                                )
                                // Corner ticks
                                val ck = 10.dp.toPx()
                                val sw = 3.dp.toPx()
                                // TL
                                drawLine(boxColor, Offset(left, top), Offset(left + ck, top), sw)
                                drawLine(boxColor, Offset(left, top), Offset(left, top + ck), sw)
                                // TR
                                drawLine(boxColor, Offset(right, top), Offset(right - ck, top), sw)
                                drawLine(boxColor, Offset(right, top), Offset(right, top + ck), sw)
                                // BL
                                drawLine(boxColor, Offset(left, bottom), Offset(left + ck, bottom), sw)
                                drawLine(boxColor, Offset(left, bottom), Offset(left, bottom - ck), sw)
                                // BR
                                drawLine(boxColor, Offset(right, bottom), Offset(right - ck, bottom), sw)
                                drawLine(boxColor, Offset(right, bottom), Offset(right, bottom - ck), sw)
                            }
                        }

                        // Label tags
                        val result2 = uiState.result as ScanResult.ObjectDetectionResult
                        result2.objects.take(3).forEach { obj ->
                            val pct = (obj.confidence * 100).toInt()
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(
                                        start = (obj.boundingBox.left * 240).dp,
                                        top   = (obj.boundingBox.top  * 240).dp
                                    )
                                    .background(accent, RoundedCornerShape(0.dp, 6.dp, 6.dp, 0.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${obj.label} $pct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Empty state
                    Column(
                        Modifier.fillMaxSize(),
                        Arrangement.Center,
                        Alignment.CenterHorizontally
                    ) {
                        ScanningRing(color = accent, modifier = Modifier.size(100.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Capture or import an image", style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
                    }
                }

                // Loading overlay
                if (uiState.isLoading) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(0.5f)),
                        Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accent)
                    }
                }
            }

            // Capture button
            if (uiState.imagePath == null) {
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick  = onCapture,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan Object", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    OutlinedButton(
                        onClick  = { /* gallery picker */ },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape    = RoundedCornerShape(14.dp),
                        border   = BorderStroke(1.dp, colors.cardBorder)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }
            }

            // ── Results list ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.result is ScanResult.ObjectDetectionResult,
                enter   = fadeIn() + expandVertically()
            ) {
                val result = uiState.result as? ScanResult.ObjectDetectionResult
                if (result != null) {
                    ObjectResultSection(result = result, accent = accent)
                }
            }

            // Error
            if (uiState.error != null) {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.error.copy(0.08f))
                        .border(1.dp, colors.error.copy(0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(uiState.error!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ObjectResultSection(
    result: ScanResult.ObjectDetectionResult,
    accent: Color
) {
    val colors = smartColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Text(
                "${result.objects.size} object${if (result.objects.size != 1) "s" else ""} detected",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                Modifier
                    .background(accent.copy(0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("ML Kit", style = MaterialTheme.typography.labelSmall, color = accent)
            }
        }

        result.objects.forEachIndexed { idx, obj ->
            val rowAccent = listOf(
                accent,
                SmartVisionColors.translator,
                SmartVisionColors.voiceTranslator,
                SmartVisionColors.studentHelper
            )[idx % 4]
            ObjectRow(obj = obj, accent = rowAccent)
        }
    }
}

@Composable
private fun ObjectRow(obj: DetectedObject, accent: Color) {
    val colors = smartColors
    val pct    = (obj.confidence * 100).toInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(1.dp, accent.copy(0.2f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(accent, CircleShape)
            )
            Text(
                obj.label,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Mini bar
            Box(
                Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.cardBorder)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(obj.confidence)
                        .background(accent, CircleShape)
                )
            }
            ConfidenceBadge(confidence = obj.confidence)
        }
    }
}
