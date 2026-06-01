package com.smartvision.ai.ui.screens.result

import android.content.*
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.ui.components.*
import com.smartvision.ai.ui.screens.camera.CameraIconButton
import com.smartvision.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    moduleRoute: String,
    onBack: () -> Unit,
    onRescan: () -> Unit,
    vm: ResultViewModel = hiltViewModel()
) {
    val c = svColors
    val s by vm.uiState.collectAsState()
    val module = ModuleType.entries.find { it.route == moduleRoute } ?: ModuleType.OBJECT_DETECTION
    val accent = moduleAccent(module)
    var showMenu by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.load() }
    val context = LocalContext.current

    BottomSheetScaffold(
        sheetPeekHeight = 300.dp,
        sheetContainerColor = c.surface,
        sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        containerColor = Color.Black,
        sheetContent = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(c.border, CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .background(accent.copy(.14f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            module.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (s.isLoadingAi) LinearProgressIndicator(
                        color = accent,
                        trackColor = c.border,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                    )
                }
                // Module-specific result content
                ResultContent(s.scanResult, accent, c)
                // AI explanation
                if (s.aiExplanation.isNotEmpty()) {
                    Divider(color = c.border)
                    AiCard(s.aiExplanation, accent, c)
                }
                // Action row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple(Icons.Rounded.ContentCopy, "Copy", { vm.copy() }),
                        Triple(Icons.Rounded.Share, "Share", { vm.share() }),
                        Triple(Icons.Rounded.VolumeUp, "Speak", { vm.speak() }),
                        Triple(Icons.Rounded.Refresh, "Rescan", onRescan)
                    ).forEach { (icon, lbl, action) ->
                        OutlinedButton(
                            onClick = action,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, c.border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = c.onSurface)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, null, modifier = Modifier.size(17.dp))
                                Text(lbl, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            AsyncImage(s.imagePath, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            // Draw detection overlays
            if (s.scanResult is ScanResult.ObjectDetectionResult) {
                val r = s.scanResult as ScanResult.ObjectDetectionResult
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    r.objects.forEach { obj ->
                        val b = obj.boundingBox
                        val l = b.left * size.width
                        val t = b.top * size.height
                        val w = (b.right - b.left) * size.width
                        val h = (b.bottom - b.top) * size.height
                        drawRect(
                            accent.copy(.2f),
                            topLeft = androidx.compose.ui.geometry.Offset(l, t),
                            size = androidx.compose.ui.geometry.Size(w, h)
                        )
                        drawRect(
                            accent,
                            topLeft = androidx.compose.ui.geometry.Offset(l, t),
                            size = androidx.compose.ui.geometry.Size(w, h),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f)
                        )
                    }
                }
            }
            // Top gradient + bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(.8f), Color.Transparent)))
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                CameraIconButton(Icons.Rounded.ArrowBackIosNew, onClick = onBack)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CameraIconButton(Icons.Rounded.Share, onClick = { vm.share() })
                    // 3-dot menu
                    Box {
                        CameraIconButton(Icons.Rounded.MoreVert, onClick = { showMenu = true })
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            listOf(
                                "Copy Result" to { vm.copy() },
                                "Share Result" to { vm.share() },
                                "Re-scan" to {
                                    onRescan()
                                    showMenu = false
                                },
                                "Open in Browser" to {
                                    val qr = s.scanResult
                                    if (qr is ScanResult.QrCodeResult && qr.type == QrType.URL)
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(qr.rawValue)).addFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK
                                            )
                                        )
                                    showMenu = false
                                }
                            ).forEach { (label, action) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    action()
                                    showMenu = false
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(result: ScanResult?, accent: Color, c: SVColorScheme) {
    when (result) {
        is ScanResult.ObjectDetectionResult -> {
            Text(
                "${result.objects.size} object(s) detected",
                style = MaterialTheme.typography.headlineSmall,
                color = c.onSurface,
                fontWeight = FontWeight.Bold
            )
            result.objects.forEach { obj ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(c.card, RoundedCornerShape(12.dp))
                        .border(1.dp, c.border, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Text(
                        obj.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = c.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    ConfidenceBadge(obj.confidence)
                }
            }
        }

        is ScanResult.OcrResult -> {
            Text(
                "Extracted Text",
                style = MaterialTheme.typography.titleMedium,
                color = c.onSurface,
                fontWeight = FontWeight.Bold
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(c.card, RoundedCornerShape(14.dp))
                    .border(1.dp, accent.copy(.3f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                SelectionContainer {
                    Text(
                        result.rawText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurface
                    )
                }
            }
        }

        is ScanResult.QrCodeResult -> {
            Text(
                result.type.name,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(c.card, RoundedCornerShape(14.dp))
                    .border(1.dp, accent.copy(.3f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(result.displayValue, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
            }
        }

        is ScanResult.WasteClassifierResult -> {
            result.categories.forEach { cat ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(c.card, RoundedCornerShape(12.dp))
                        .border(1.dp, c.border, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(
                            cat.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        ConfidenceBadge(cat.confidence)
                    }
                    LinearProgressIndicator(
                        { cat.confidence },
                        color = Color(cat.color),
                        trackColor = c.border,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        is ScanResult.Error -> Box(
            Modifier
                .fillMaxWidth()
                .background(c.error.copy(.1f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) { Text(result.message, color = c.error, style = MaterialTheme.typography.bodyMedium) }

        is ScanResult.Loading -> Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
            CircularProgressIndicator(color = accent)
        }

        else -> {}
    }
}

@Composable
private fun AiCard(explanation: String, accent: Color, c: SVColorScheme) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(accent.copy(.06f), RoundedCornerShape(14.dp))
            .border(1.dp, accent.copy(.2f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(16.dp))
                Text(
                    "AI Explanation",
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(explanation, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
        }
    }
}
