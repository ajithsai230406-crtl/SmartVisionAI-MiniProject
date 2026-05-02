package com.smartvision.ai.ui.screens.objectdetection

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
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
    onBack: () -> Unit,
    onCapture: () -> Unit,
    vm: ObjectDetectionViewModel = hiltViewModel()
) {
    val c = svColors
    val accent = SVColors.cyan
    val uiState by vm.uiState.collectAsState()
    val result = uiState.result
    val path = uiState.imagePath
    val loading = uiState.isLoading
    
    val boxColors = listOf(
        SVColors.cyan, SVColors.amber, SVColors.green, 
        SVColors.blue, SVColors.purple, SVColors.orange
    )

    Scaffold(
        topBar = { SmartVisionTopBar("Object Detection", onBack = onBack) },
        containerColor = c.background
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            // Image with bounding box overlay
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.card)
                    .border(1.dp, accent.copy(.3f), RoundedCornerShape(20.dp))
            ) {
                if (path != null) {
                    AsyncImage(
                        path,
                        null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (result is ScanResult.ObjectDetectionResult) {
                        val objs = result.objects
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                            objs.forEachIndexed { i, obj ->
                                val col = boxColors[i % boxColors.size]
                                val b = obj.boundingBox
                                val l = b.left * size.width
                                val t = b.top * size.height
                                val w = (b.right - b.left) * size.width
                                val h = (b.bottom - b.top) * size.height
                                
                                drawRect(col.copy(.18f), Offset(l, t), Size(w, h))
                                drawRect(col, Offset(l, t), Size(w, h), style = Stroke(2.5f))
                                
                                // Corner ticks
                                val ck = 10.dp.toPx()
                                listOf(
                                    Offset(l, t) to listOf(Offset(l + ck, t), Offset(l, t + ck)),
                                    Offset(l + w, t) to listOf(Offset(l + w - ck, t), Offset(l + w, t + ck)),
                                    Offset(l, t + h) to listOf(Offset(l + ck, t + h), Offset(l, t + h - ck)),
                                    Offset(l + w, t + h) to listOf(Offset(l + w - ck, t + h), Offset(l + w, t + h - ck))
                                ).forEach { (start, ends) ->
                                    ends.forEach { end -> drawLine(col, start, end, 3.dp.toPx()) }
                                }
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                        ScanningRing(accent, Modifier.size(90.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Capture an image to detect objects",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.subtext
                        )
                    }
                }
                if (loading) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(.5f)),
                        Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accent)
                    }
                }
            }

            // Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onCapture,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = Color.Black, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan", fontWeight = FontWeight.Bold, color = Color.Black)
                }
                if (path != null) {
                    OutlinedButton(
                        onClick = { vm.reprocess(path) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, accent.copy(.5f))
                    ) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(17.dp), tint = accent)
                        Spacer(Modifier.width(6.dp))
                        Text("Re-detect", color = accent)
                    }
                }
            }

            // Results
            AnimatedVisibility(result is ScanResult.ObjectDetectionResult) {
                val objs = (result as? ScanResult.ObjectDetectionResult)?.objects ?: return@AnimatedVisibility
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(
                            "${objs.size} object${if (objs.size != 1) "s" else ""} detected",
                            style = MaterialTheme.typography.titleMedium,
                            color = c.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            Modifier
                                .background(SVColors.cyan.copy(.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("ML Kit", style = MaterialTheme.typography.labelSmall, color = SVColors.cyan)
                        }
                    }
                    objs.forEachIndexed { i, obj ->
                        val col = boxColors[i % boxColors.size]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.card)
                                .border(1.dp, col.copy(.2f), RoundedCornerShape(14.dp))
                                .padding(13.dp),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(col, CircleShape)
                                )
                                Text(
                                    obj.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = c.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .width(58.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(c.border)
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(obj.confidence)
                                            .background(col, CircleShape)
                                    )
                                }
                                ConfidenceBadge(obj.confidence)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
