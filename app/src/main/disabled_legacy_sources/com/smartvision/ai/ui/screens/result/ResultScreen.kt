package com.smartvision.ai.ui.screens.result

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.ui.components.*
import com.smartvision.ai.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// RESULT SCREEN (Google Lens–style layout)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    moduleRoute: String,
    onBack:      () -> Unit,
    onRescan:    () -> Unit,
    viewModel:   ResultViewModel = hiltViewModel()
) {
    val colors      = smartColors
    val uiState by viewModel.uiState.collectAsState()
    val module  = ModuleType.entries.find { it.route == moduleRoute } ?: ModuleType.OBJECT_DETECTION
    val accent  = moduleAccentColor(module)

    // Bottom sheet state
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    LaunchedEffect(moduleRoute) { viewModel.loadResult(moduleRoute) }

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 320.dp,
        sheetContainerColor = colors.surface,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.Black,
        sheetContent = {
            ResultBottomSheet(
                uiState  = uiState,
                module   = module,
                accent   = accent,
                onRescan = onRescan,
                onCopy   = viewModel::copyToClipboard,
                onShare  = viewModel::shareResult,
                onSpeak  = viewModel::speak
            )
        }
    ) { padding ->
        // ── Scanned image (fills top portion) ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // Image
            AsyncImage(
                model              = uiState.imagePath,
                contentDescription = "Scanned image",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )

            // Overlay: detected bounding boxes
            DetectionOverlay(
                result = uiState.scanResult,
                accent = accent,
                modifier = Modifier.fillMaxSize()
            )

            // Top gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
            )

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                CameraIconButton(icon = Icons.Rounded.ArrowBackIosNew, onClick = onBack)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CameraIconButton(icon = Icons.Rounded.Share,  onClick = viewModel::shareResult)
                    CameraIconButton(icon = Icons.Rounded.MoreVert, onClick = {})
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DETECTION OVERLAY — draws boxes/highlights over the image
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetectionOverlay(
    result:   ScanResult?,
    accent:   Color,
    modifier: Modifier = Modifier
) {
    when (result) {
        is ScanResult.ObjectDetectionResult -> {
            Canvas(modifier = modifier) {
                result.objects.forEach { obj ->
                    val box = obj.boundingBox
                    drawRect(
                        color    = accent.copy(alpha = 0.35f),
                        topLeft  = Offset(box.left * size.width, box.top * size.height),
                        size     = androidx.compose.ui.geometry.Size(
                            (box.right - box.left) * size.width,
                            (box.bottom - box.top) * size.height
                        )
                    )
                    drawRect(
                        color       = accent,
                        topLeft     = Offset(box.left * size.width, box.top * size.height),
                        size        = androidx.compose.ui.geometry.Size(
                            (box.right - box.left) * size.width,
                            (box.bottom - box.top) * size.height
                        ),
                        style       = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
        is ScanResult.OcrResult -> {
            Canvas(modifier = modifier) {
                result.textBlocks.forEach { block ->
                    val box = block.boundingBox
                    drawRect(
                        color   = Color(0x3300E5FF),
                        topLeft = Offset(box.left * size.width, box.top * size.height),
                        size    = androidx.compose.ui.geometry.Size(
                            (box.right - box.left) * size.width,
                            (box.bottom - box.top) * size.height
                        )
                    )
                }
            }
        }
        else -> {}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RESULT BOTTOM SHEET
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultBottomSheet(
    uiState:  ResultUiState,
    module:   ModuleType,
    accent:   Color,
    onRescan: () -> Unit,
    onCopy:   () -> Unit,
    onShare:  () -> Unit,
    onSpeak:  () -> Unit
) {
    val colors = smartColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(colors.cardBorder, CircleShape)
                .align(Alignment.CenterHorizontally)
        )

        // Module chip + label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(module.title, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
            }
            if (uiState.isLoadingAi) {
                LinearProgressIndicator(
                    color         = accent,
                    trackColor    = colors.cardBorder,
                    modifier      = Modifier.weight(1f).height(2.dp).clip(CircleShape)
                )
            }
        }

        // ── Module-specific result content ────────────────────────────────────
        when (val res = uiState.scanResult) {
            is ScanResult.ObjectDetectionResult -> ObjectDetectionResultContent(res, accent)
            is ScanResult.OcrResult             -> OcrResultContent(res, accent, onCopy, onSpeak)
            is ScanResult.WasteClassifierResult -> WasteResultContent(res)
            is ScanResult.QrCodeResult          -> QrResultContent(res, accent)
            is ScanResult.StudentHelperResult   -> StudentResultContent(res, accent)
            is ScanResult.MedicalScanResult     -> MedicalResultContent(res, accent)
            is ScanResult.Loading               -> LoadingContent(accent)
            is ScanResult.Error                 -> ErrorContent(res.message)
            null                                -> {}
            else                                -> {}
        }

        // ── AI Explanation ────────────────────────────────────────────────────
        if (uiState.aiExplanation.isNotEmpty()) {
            Divider(color = colors.cardBorder)
            AiExplanationCard(explanation = uiState.aiExplanation, accent = accent)
        }

        // ── Action row ────────────────────────────────────────────────────────
        ActionRow(onCopy = onCopy, onShare = onShare, onSpeak = onSpeak, onRescan = onRescan, accent = accent)

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONTENT VARIANTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ObjectDetectionResultContent(result: ScanResult.ObjectDetectionResult, accent: Color) {
    val colors = smartColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "${result.objects.size} object${if (result.objects.size != 1) "s" else ""} detected",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onSurface, fontWeight = FontWeight.Bold
        )
        result.objects.forEach { obj ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.card, RoundedCornerShape(14.dp))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(obj.label, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                ConfidenceBadge(confidence = obj.confidence)
            }
        }
    }
}

@Composable
private fun OcrResultContent(
    result:  ScanResult.OcrResult,
    accent:  Color,
    onCopy:  () -> Unit,
    onSpeak: () -> Unit
) {
    val colors = smartColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Extracted Text", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, fontWeight = FontWeight.Bold)
        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.card, RoundedCornerShape(16.dp))
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(result.rawText, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            }
        }
    }
}

@Composable
private fun WasteResultContent(result: ScanResult.WasteClassifierResult) {
    val colors = smartColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Classification Results", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, fontWeight = FontWeight.Bold)
        result.categories.forEach { cat ->
            val barColor = Color(cat.color)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.card, RoundedCornerShape(14.dp))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(cat.label, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                    ConfidenceBadge(cat.confidence)
                }
                LinearProgressIndicator(
                    progress          = { cat.confidence },
                    color             = barColor,
                    trackColor        = colors.cardBorder,
                    modifier          = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                )
            }
        }
    }
}

@Composable
private fun QrResultContent(result: ScanResult.QrCodeResult, accent: Color) {
    val colors = smartColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("QR Code Decoded", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card, RoundedCornerShape(16.dp))
                .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(result.type.name, style = MaterialTheme.typography.labelLarge, color = accent)
                Text(result.displayValue, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            }
        }
        if (result.type == QrType.URL) {
            Button(
                onClick = { /* open URL */ },
                colors  = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.OpenInBrowser, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Link", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StudentResultContent(result: ScanResult.StudentHelperResult, accent: Color) {
    val colors = smartColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Question", style = MaterialTheme.typography.labelLarge, color = colors.subtext)
        Text(result.question, style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, fontWeight = FontWeight.Bold)
        if (result.steps.isNotEmpty()) {
            Text("Steps", style = MaterialTheme.typography.labelLarge, color = accent)
            result.steps.forEachIndexed { i, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(24.dp).background(accent.copy(0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("${i+1}", style = MaterialTheme.typography.labelLarge, color = accent) }
                    Text(step, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                }
            }
        }
    }
}

@Composable
private fun MedicalResultContent(result: ScanResult.MedicalScanResult, accent: Color) {
    val colors = smartColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(result.medicineName, style = MaterialTheme.typography.headlineMedium, color = colors.onSurface, fontWeight = FontWeight.Bold)
        InfoRow("Usage",   result.usage,  accent)
        InfoRow("Dosage",  result.dosage, accent)
        if (result.sideEffects.isNotEmpty()) {
            Text("Side Effects", style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.SemiBold)
            result.sideEffects.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface) }
        }
        if (result.warnings.isNotEmpty()) {
            Text("⚠️ Warnings", style = MaterialTheme.typography.titleMedium, color = SmartVisionColors.medicalScanner, fontWeight = FontWeight.SemiBold)
            result.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, accent: Color) {
    val colors = smartColors
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AiExplanationCard(explanation: String, accent: Color) {
    val colors = smartColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(0.06f), RoundedCornerShape(16.dp))
            .border(1.dp, accent.copy(0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(18.dp))
                Text("AI Explanation", style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.SemiBold)
            }
            Text(explanation, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        }
    }
}

@Composable
private fun ActionRow(onCopy: () -> Unit, onShare: () -> Unit, onSpeak: () -> Unit, onRescan: () -> Unit, accent: Color) {
    val colors = smartColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            Triple(Icons.Rounded.ContentCopy, "Copy", onCopy),
            Triple(Icons.Rounded.Share,        "Share", onShare),
            Triple(Icons.Rounded.VolumeUp,     "Speak", onSpeak),
            Triple(Icons.Rounded.Refresh,      "Rescan", onRescan),
        ).forEach { (icon, label, action) ->
            OutlinedButton(
                onClick  = action,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
                border   = BorderStroke(1.dp, colors.cardBorder)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, modifier = Modifier.size(18.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(accent: Color) {
    Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
        CircularProgressIndicator(color = accent)
    }
}

@Composable
private fun ErrorContent(message: String) {
    val colors = smartColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.error.copy(0.1f), RoundedCornerShape(14.dp))
            .border(1.dp, colors.error.copy(0.3f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(message, color = colors.error, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SelectionContainer(content: @Composable () -> Unit) {
    // Wrap with SelectionContainer from compose foundation
    androidx.compose.foundation.text.selection.SelectionContainer(content = content)
}
