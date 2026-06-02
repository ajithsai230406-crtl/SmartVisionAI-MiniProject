package com.smartvision.ai.docscanner.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.docscanner.domain.*
import com.smartvision.ai.docscanner.presentation.DocScannerViewModel
import com.smartvision.ai.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
// SMART DOCUMENT SCANNER — MAIN SCREEN
// ══════════════════════════════════════════════════════════════════════════════

private val DocBlue   = Color(0xFF00B4FF)
private val DocGold   = Color(0xFFFFD700)
private val DocGreen  = Color(0xFF00FF88)

@Composable
fun DocumentScannerScreen(
    onBack: () -> Unit,
    viewModel: DocScannerViewModel = hiltViewModel()
) {
    val ctx          = LocalContext.current
    val activity     = ctx as? Activity
    val state        by viewModel.state.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val exportFormat by viewModel.exportFormat.collectAsStateWithLifecycle()
    val docName      by viewModel.docName.collectAsStateWithLifecycle()
    val savedDocs    by viewModel.savedDocs.collectAsStateWithLifecycle()
    val toast        by viewModel.toast.collectAsStateWithLifecycle()

    // ML Kit scanner launcher
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            viewModel.handleScanResult(scanResult)
        } else {
            viewModel.resetScan()
        }
    }

    // Storage permission launcher for older Android platforms (API <= 28)
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.exportDocument()
        }
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Top HUD ───────────────────────────────────────────────────
                DocScannerTopBar(onBack = onBack)

                when (state) {
                    is DocScanState.Idle -> {
                        DocIdleContent(
                            savedDocs      = savedDocs,
                            onScan         = { activity?.let { viewModel.launchScanner(it, scanLauncher) } },
                            onOpenDoc      = { viewModel.openDocument(it.savedUri!!) },
                            onShareDoc     = { viewModel.shareDocument(it.savedUri!!) }
                        )
                    }
                    is DocScanState.Scanning, is DocScanState.Processing -> {
                        DocLoadingContent(
                            label = if (state is DocScanState.Processing) "Applying filter..." else "Opening scanner..."
                        )
                    }
                    is DocScanState.Preview -> {
                        val preview = (state as DocScanState.Preview).doc
                        DocPreviewContent(
                            doc            = preview,
                            docName        = docName,
                            selectedFilter = selectedFilter,
                            exportFormat   = exportFormat,
                            onFilterSelect = { viewModel.selectFilter(it) },
                            onFormatSelect = { viewModel.setExportFormat(it) },
                            onNameChange   = { viewModel.setDocName(it) },
                            onSave         = {
                                val writePermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                val hasPermission = if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
                                    androidx.core.content.ContextCompat.checkSelfPermission(ctx, writePermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                } else {
                                    true
                                }
                                if (hasPermission) {
                                    viewModel.exportDocument()
                                } else {
                                    storagePermissionLauncher.launch(writePermission)
                                }
                            },
                            onRescan       = {
                                viewModel.resetScan()
                                activity?.let { viewModel.launchScanner(it, scanLauncher) }
                            },
                            onDiscard      = { viewModel.resetScan() }
                        )
                    }
                    is DocScanState.Saved -> {
                        val saved = state as DocScanState.Saved
                        DocSavedContent(
                            doc        = saved.doc,
                            uri        = saved.uri,
                            onOpen     = { viewModel.openDocument(saved.uri) },
                            onShare    = { viewModel.shareDocument(saved.uri) },
                            onScanMore = { viewModel.resetScan() }
                        )
                    }
                    is DocScanState.Error -> {
                        val err = (state as DocScanState.Error).message
                        DocErrorContent(message = err, onRetry = { viewModel.dismissError() })
                    }
                }
            }

            // ── Toast overlay ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible  = toast != null,
                enter    = slideInVertically { -it } + fadeIn(),
                exit     = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp, start = 16.dp, end = 16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DocGreen.copy(0.15f))
                        .border(1.dp, DocGreen.copy(0.4f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.dismissToast() }
                        .padding(14.dp)
                ) {
                    Text(toast ?: "", style = MaterialTheme.typography.bodySmall, color = DocGreen)
                }
                LaunchedEffect(toast) {
                    if (toast != null) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.dismissToast()
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TOP BAR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DocScannerTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.extended.glassCard)
                .border(1.dp, MaterialTheme.extended.glassBorder, RoundedCornerShape(10.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Text("←", color = MaterialTheme.colorScheme.onSurface) }
        Column {
            Text("📄 Document Scanner", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text("ML Kit · Edge Detection · Auto-crop", style = MaterialTheme.typography.labelSmall, color = DocBlue)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// IDLE CONTENT — scanner home with animated launch button + recent docs
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DocIdleContent(
    savedDocs:  List<ScannedDocument>,
    onScan:     () -> Unit,
    onOpenDoc:  (ScannedDocument) -> Unit,
    onShareDoc: (ScannedDocument) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // Animated scanner hero area
        ScannerHeroAnimation(onScan = onScan)

        Spacer(Modifier.height(28.dp))

        // Feature chips
        DocFeatureRow()

        Spacer(Modifier.height(28.dp))

        // Scan button
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(DocBlue, NeonPurple)))
                .clickable(onClick = onScan)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📷", fontSize = 22.sp)
                Text("Scan Document", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Supports up to 10 pages · Auto edge detection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

        // Recent docs
        if (savedDocs.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("📁 Recent Documents", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text("${savedDocs.size} files", style = MaterialTheme.typography.labelSmall, color = DocBlue)
            }
            Spacer(Modifier.height(10.dp))
            savedDocs.forEach { doc ->
                RecentDocRow(doc = doc, onOpen = { onOpenDoc(doc) }, onShare = { onShareDoc(doc) })
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ScannerHeroAnimation(onScan: () -> Unit) {
    val inf   = rememberInfiniteTransition(label = "hero")
    val pulse by inf.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "p")
    val glow  by inf.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "g")
    val scanY by inf.animateFloat(0.1f, 0.9f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "s")
    val rot   by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "r")

    Box(modifier = Modifier.size(220.dp).scale(pulse).clickable(onClick = onScan), contentAlignment = Alignment.Center) {
        // Outer rotating ring
        Canvas(Modifier.fillMaxSize()) {
            drawArc(
                brush  = Brush.sweepGradient(listOf(Color.Transparent, DocBlue.copy(glow), Color.Transparent)),
                startAngle = rot, sweepAngle = 270f, useCenter = false,
                style  = Stroke(3f, cap = StrokeCap.Round)
            )
            drawArc(
                brush  = Brush.sweepGradient(listOf(Color.Transparent, NeonPurple.copy(glow * 0.6f), Color.Transparent)),
                startAngle = rot + 180f, sweepAngle = 200f, useCenter = false,
                style  = Stroke(2f, cap = StrokeCap.Round)
            )
        }
        // Document icon placeholder
        Box(
            modifier = Modifier.size(150.dp).clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.extended.glassCard)
                .border(2.dp, DocBlue.copy(glow * 0.8f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📄", fontSize = 52.sp)
                Text("Tap to Scan", style = MaterialTheme.typography.labelMedium, color = DocBlue)
            }
            // Scan line
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * scanY
                drawLine(
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, DocBlue.copy(0.7f), Color.Transparent)),
                    start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 2f
                )
            }
        }
    }
}

@Composable
private fun DocFeatureRow() {
    val features = listOf("Edge Detection" to "🔲", "Auto-crop" to "✂️", "Perspective Fix" to "📐", "Multi-page" to "📑")
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
        features.forEach { (label, icon) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(DocBlue.copy(0.10f)).border(1.dp, DocBlue.copy(0.25f), RoundedCornerShape(12.dp)), Alignment.Center) {
                    Text(icon, fontSize = 20.sp)
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun RecentDocRow(doc: ScannedDocument, onOpen: () -> Unit, onShare: () -> Unit) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(DocBlue.copy(0.12f)).border(1.dp, DocBlue.copy(0.25f), RoundedCornerShape(10.dp)), Alignment.Center) {
            Text(doc.exportFormat.extension.uppercase(), style = MaterialTheme.typography.labelSmall, color = DocBlue, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(doc.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${doc.pageCount} page(s) · ${if (doc.fileSizeKb > 0) "${doc.fileSizeKb}KB" else doc.exportFormat.displayName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DocActionBtn("📂", onClick = onOpen)
            DocActionBtn("↑", onClick = onShare)
        }
    }
}

@Composable
private fun DocActionBtn(icon: String, onClick: () -> Unit) {
    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.extended.glassCard).border(1.dp, MaterialTheme.extended.glassBorder, RoundedCornerShape(8.dp)).clickable(onClick = onClick), Alignment.Center) {
        Text(icon, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// LOADING CONTENT
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DocLoadingContent(label: String) {
    val inf = rememberInfiniteTransition(label = "load")
    val alpha by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = DocBlue, modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = DocBlue.copy(alpha = alpha))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PREVIEW CONTENT — page previews + filters + export settings
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DocPreviewContent(
    doc:            ScannedDocument,
    docName:        String,
    selectedFilter: DocumentFilter,
    exportFormat:   ExportFormat,
    onFilterSelect: (DocumentFilter) -> Unit,
    onFormatSelect: (ExportFormat) -> Unit,
    onNameChange:   (String) -> Unit,
    onSave:         () -> Unit,
    onRescan:       () -> Unit,
    onDiscard:      () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // ── Page preview carousel ────────────────────────────────────────────
        DocPagePreviewRow(pages = doc.pages)

        // ── Flash success animation ────────────────────────────────────────
        DocScanFlashBanner(pageCount = doc.pageCount)

        // ── Filter picker ─────────────────────────────────────────────────────
        FilterPickerRow(selected = selectedFilter, onSelect = onFilterSelect)

        // ── Document name ─────────────────────────────────────────────────────
        DocNameField(name = docName, onChange = onNameChange, onDone = { keyboard?.hide() })

        // ── Export format ─────────────────────────────────────────────────────
        ExportFormatRow(selected = exportFormat, onSelect = onFormatSelect)

        // ── Action row ────────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.extended.glassCard).border(1.dp, MaterialTheme.extended.glassBorder, RoundedCornerShape(14.dp)).clickable(onClick = onRescan).padding(vertical = 14.dp), Alignment.Center) {
                Text("📷 Rescan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Box(Modifier.weight(2f).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(DocBlue, NeonPurple))).clickable(onClick = onSave).padding(vertical = 14.dp), Alignment.Center) {
                Text("💾 Save ${exportFormat.extension.uppercase()}", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NeonPink.copy(0.08f)).border(1.dp, NeonPink.copy(0.2f), RoundedCornerShape(12.dp)).clickable(onClick = onDiscard).padding(vertical = 12.dp), Alignment.Center) {
            Text("🗑 Discard", style = MaterialTheme.typography.labelMedium, color = NeonPink)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DocPagePreviewRow(pages: List<android.net.Uri>) {
    val ext = MaterialTheme.extended
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("📑 Scanned Pages", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            NeonBadge("${pages.size} page${if (pages.size > 1) "s" else ""}", DocBlue)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(pages) { idx, uri ->
                Box(
                    modifier = Modifier.size(width = 120.dp, height = 160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ext.glassCard)
                        .border(2.dp, DocBlue.copy(if (idx == 0) 0.7f else 0.3f), RoundedCornerShape(14.dp))
                ) {
                    AsyncImage(
                        model = uri, contentDescription = "Page ${idx + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                    )
                    Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                        Box(Modifier.size(20.dp).clip(CircleShape).background(DocBlue), Alignment.Center) {
                            Text("${idx + 1}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocScanFlashBanner(pageCount: Int) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(2500); visible = false }
    AnimatedVisibility(visible = visible, exit = fadeOut() + shrinkVertically()) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DocGreen.copy(0.12f)).border(1.dp, DocGreen.copy(0.35f), RoundedCornerShape(12.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Text("✅", fontSize = 16.sp)
            Text("$pageCount page${if (pageCount > 1) "s" else ""} scanned! Choose filters and save.", style = MaterialTheme.typography.bodySmall, color = DocGreen)
        }
    }
}

@Composable
private fun FilterPickerRow(selected: DocumentFilter, onSelect: (DocumentFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("🎨 Document Filter", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DocumentFilter.values()) { filter ->
                val isSelected = filter == selected
                val color      = Color(filter.colorArgb)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isSelected)
                                Modifier.background(color.copy(0.2f)).border(2.dp, color, RoundedCornerShape(12.dp))
                            else
                                Modifier.background(MaterialTheme.extended.glassCard).border(1.dp, MaterialTheme.extended.glassBorder, RoundedCornerShape(12.dp))
                        )
                        .clickable { onSelect(filter) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(filter.emoji, fontSize = 22.sp)
                        Text(filter.displayName, style = MaterialTheme.typography.labelSmall, color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocNameField(name: String, onChange: (String) -> Unit, onDone: () -> Unit) {
    val ext = MaterialTheme.extended
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("📝 Document Name", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value         = name,
            onValueChange = onChange,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text("Enter document name...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)) },
            shape         = RoundedCornerShape(12.dp),
            singleLine    = true,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor    = DocBlue,
                unfocusedBorderColor  = ext.glassBorder,
                focusedContainerColor = ext.glassCard,
                unfocusedContainerColor = ext.glassCard,
                cursorColor           = DocBlue
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() })
        )
    }
}

@Composable
private fun ExportFormatRow(selected: ExportFormat, onSelect: (ExportFormat) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("💾 Save As", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            ExportFormat.values().forEach { format ->
                val isSelected = format == selected
                val color = when (format) {
                    ExportFormat.PDF  -> NeonPink
                    ExportFormat.JPEG -> DocGold
                    ExportFormat.PNG  -> DocGreen
                }
                Box(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isSelected) Modifier.background(color.copy(0.15f)).border(2.dp, color, RoundedCornerShape(12.dp))
                            else Modifier.background(MaterialTheme.extended.glassCard).border(1.dp, MaterialTheme.extended.glassBorder, RoundedCornerShape(12.dp))
                        )
                        .clickable { onSelect(format) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(format.extension.uppercase(), style = MaterialTheme.typography.titleSmall, color = if (isSelected) color else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
                        Text(format.displayName, style = MaterialTheme.typography.labelSmall, color = if (isSelected) color.copy(0.8f) else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SAVED CONTENT
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DocSavedContent(
    doc:       ScannedDocument,
    uri:       android.net.Uri,
    onOpen:    () -> Unit,
    onShare:   () -> Unit,
    onScanMore: () -> Unit
) {
    val inf   = rememberInfiniteTransition(label = "saved")
    val pulse by inf.animateFloat(0.95f, 1.05f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "p")

    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Success icon with animated neon breathing halo
            Box(
                modifier = Modifier.size(80.dp).scale(pulse)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(DocGreen.copy(0.3f), DocGreen.copy(0.08f))))
                    .border(2.dp, DocGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("✅", fontSize = 38.sp) }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Saved Successfully!", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Available locally in public directories", style = MaterialTheme.typography.bodySmall, color = DocGreen, textAlign = TextAlign.Center)
            }

            // Scanned Page Gallery Carousel Preview
            if (doc.pages.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("📷 Document Preview", style = MaterialTheme.typography.labelMedium, color = DocBlue, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.height(110.dp)
                    ) {
                        itemsIndexed(doc.pages) { idx, pageUri ->
                            Box(
                                modifier = Modifier
                                    .size(width = 80.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.extended.glassCard)
                                    .border(1.dp, DocBlue.copy(0.4f), RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = pageUri,
                                    contentDescription = "Page Preview ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(DocBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Doc info card
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.extended.glassCard)
                    .border(1.dp, DocGreen.copy(0.35f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow("📄 Name", doc.name)
                    InfoRow("📑 Pages", "${doc.pageCount}")
                    InfoRow("💾 Format", doc.exportFormat.displayName)
                    InfoRow("🎨 Filter", doc.filter.displayName)
                    if (doc.fileSizeKb > 0) InfoRow("📦 Size", "${doc.fileSizeKb} KB")
                }
            }

            // Action buttons
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(DocBlue.copy(0.15f)).border(1.dp, DocBlue.copy(0.4f), RoundedCornerShape(14.dp)).clickable(onClick = onOpen).padding(vertical = 14.dp), Alignment.Center) {
                    Text("📂 Open", style = MaterialTheme.typography.labelMedium, color = DocBlue, fontWeight = FontWeight.SemiBold)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(NeonPurple.copy(0.15f)).border(1.dp, NeonPurple.copy(0.4f), RoundedCornerShape(14.dp)).clickable(onClick = onShare).padding(vertical = 14.dp), Alignment.Center) {
                    Text("↑ Share", style = MaterialTheme.typography.labelMedium, color = NeonPurple, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(DocBlue, NeonPurple))).clickable(onClick = onScanMore).padding(vertical = 14.dp), Alignment.Center) {
                Text("📷 Scan Another", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ERROR CONTENT
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DocErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(24.dp)) {
            Text("⚠️", fontSize = 56.sp)
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(message, style = MaterialTheme.typography.bodySmall, color = NeonPink, textAlign = TextAlign.Center)
            Box(Modifier.clip(RoundedCornerShape(14.dp)).background(DocBlue.copy(0.15f)).border(1.dp, DocBlue.copy(0.4f), RoundedCornerShape(14.dp)).clickable(onClick = onRetry).padding(horizontal = 28.dp, vertical = 12.dp)) {
                Text("Try Again", style = MaterialTheme.typography.labelMedium, color = DocBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
