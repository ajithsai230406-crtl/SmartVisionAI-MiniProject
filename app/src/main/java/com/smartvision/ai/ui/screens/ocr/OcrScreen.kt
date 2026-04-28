package com.smartvision.ai.ui.screens.ocr

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.ui.components.*
import com.smartvision.ai.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// OCR SCREEN — with drag-to-select area
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OcrScreen(
    onBack:    () -> Unit,
    viewModel: OcrViewModel = hiltViewModel()
) {
    val colors  = smartColors
    val uiState by viewModel.uiState.collectAsState()

    // Selection drag state (normalised 0.0–1.0)
    var dragStart  by remember { mutableStateOf(Offset.Zero) }
    var dragEnd    by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Scaffold(
        topBar = {
            SmartVisionTopBar(title = "Text Scanner (OCR)", onBack = onBack) {
                if (uiState.selectedText.isNotEmpty()) {
                    IconButton(onClick = viewModel::copySelected) {
                        Icon(Icons.Rounded.ContentCopy, null, tint = colors.primary)
                    }
                    IconButton(onClick = viewModel::speakSelected) {
                        Icon(Icons.Rounded.VolumeUp, null, tint = colors.primary)
                    }
                }
            }
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Image with selectable overlay ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                dragStart  = offset
                                dragEnd    = offset
                            },
                            onDrag = { change, _ ->
                                dragEnd = change.position
                                // Extract text inside selection
                                if (canvasSize != Size.Zero) {
                                    val normRect = Rect(
                                        left   = (minOf(dragStart.x, dragEnd.x) / canvasSize.width).coerceIn(0f, 1f),
                                        top    = (minOf(dragStart.y, dragEnd.y) / canvasSize.height).coerceIn(0f, 1f),
                                        right  = (maxOf(dragStart.x, dragEnd.x) / canvasSize.width).coerceIn(0f, 1f),
                                        bottom = (maxOf(dragStart.y, dragEnd.y) / canvasSize.height).coerceIn(0f, 1f)
                                    )
                                    viewModel.selectArea(normRect)
                                }
                            },
                            onDragEnd = { isDragging = false }
                        )
                    }
            ) {
                // Placeholder image (in production: show captured bitmap via Coil)
                Box(
                    modifier = Modifier.fillMaxSize().background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.imagePath == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.TextFields, null, tint = SmartVisionColors.textScanner, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Capture or load an image", style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
                        }
                    } else {
                        AsyncImage(
                            model              = uiState.imagePath,
                            contentDescription = null,
                            modifier           = Modifier.fillMaxSize().onSizeChanged { size ->
                                canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                            }
                        )
                    }
                }

                // Text block highlights
                Canvas(modifier = Modifier.fillMaxSize()) {
                    uiState.textBlocks.forEach { block ->
                        val b = block.boundingBox
                        drawRect(
                            color    = SmartVisionColors.textScanner.copy(alpha = 0.2f),
                            topLeft  = Offset(b.left * size.width, b.top * size.height),
                            size     = Size((b.right - b.left) * size.width, (b.bottom - b.top) * size.height)
                        )
                        drawRect(
                            color    = SmartVisionColors.textScanner,
                            topLeft  = Offset(b.left * size.width, b.top * size.height),
                            size     = Size((b.right - b.left) * size.width, (b.bottom - b.top) * size.height),
                            style    = androidx.compose.ui.graphics.drawscope.Stroke(2f)
                        )
                    }

                    // Drag selection box
                    if (isDragging || uiState.selectedText.isNotEmpty()) {
                        val selRect = Rect(
                            left   = minOf(dragStart.x, dragEnd.x),
                            top    = minOf(dragStart.y, dragEnd.y),
                            right  = maxOf(dragStart.x, dragEnd.x),
                            bottom = maxOf(dragStart.y, dragEnd.y)
                        )
                        drawRect(
                            color    = Color(0x3300E5FF),
                            topLeft  = Offset(selRect.left, selRect.top),
                            size     = Size(selRect.width, selRect.height)
                        )
                        drawRect(
                            color    = Color(0xFF00E5FF),
                            topLeft  = Offset(selRect.left, selRect.top),
                            size     = Size(selRect.width, selRect.height),
                            style    = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                        // Corner handles
                        val handleRadius = 6.dp.toPx()
                        listOf(
                            Offset(selRect.left, selRect.top),
                            Offset(selRect.right, selRect.top),
                            Offset(selRect.left, selRect.bottom),
                            Offset(selRect.right, selRect.bottom)
                        ).forEach { corner ->
                            drawCircle(Color(0xFF00E5FF), handleRadius, corner)
                        }
                    }
                }

                // Instruction hint
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .background(Color.Black.copy(0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text  = if (uiState.textBlocks.isEmpty()) "Drag to select text area"
                                else "Drag over text to select",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }

            // ── Extracted text panel ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.selectedText.isNotEmpty()) "Selected Text" else "Full Text",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (uiState.selectedText.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearSelection) {
                            Text("Clear", color = colors.subtext)
                        }
                    }
                }

                val displayText = uiState.selectedText.ifEmpty { uiState.fullText }
                if (displayText.isEmpty() && !uiState.isLoading) {
                    Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                        Text("No text detected yet", style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(colors.card, RoundedCornerShape(16.dp))
                            .border(1.dp, SmartVisionColors.textScanner.copy(0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(displayText, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                        }
                    }
                }

                // Action chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(Icons.Rounded.ContentCopy, "Copy",      viewModel::copySelected),
                        Triple(Icons.Rounded.Translate,   "Translate", viewModel::translateSelected),
                        Triple(Icons.Rounded.VolumeUp,    "Speak",     viewModel::speakSelected)
                    ).forEach { (icon, label, action) ->
                        FilterChip(
                            selected = false,
                            onClick  = action,
                            label    = { Text(label, style = MaterialTheme.typography.labelLarge) },
                            leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                            colors   = FilterChipDefaults.filterChipColors(
                                containerColor = colors.card,
                                labelColor     = colors.onSurface,
                                iconColor      = SmartVisionColors.textScanner
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
