package com.smartvision.ai.ui.screens.ocr

import android.app.Application
import android.content.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import coil.compose.AsyncImage
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.domain.usecase.*
import com.smartvision.ai.ui.components.SmartVisionTopBar
import com.smartvision.ai.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun OcrScreen(onBack: () -> Unit, onTranslate: ((String) -> Unit)? = null, vm: OcrViewModel = hiltViewModel()) {
    val c = svColors; val s by vm.uiState.collectAsState()
    val accent = SVColors.purple
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var dragEnd   by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Scaffold(
        topBar = { SmartVisionTopBar("Text Scanner (OCR)", onBack = onBack) {
            if (s.selectedText.isNotEmpty() || s.fullText.isNotEmpty()) {
                IconButton(onClick = { vm.copySelected() }) { Icon(Icons.Rounded.ContentCopy, null, tint = c.primary) }
                IconButton(onClick = { vm.speakSelected() }) { Icon(Icons.Rounded.VolumeUp, null, tint = c.primary) }
            }
        }},
        containerColor = c.background
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Image with drag-to-select overlay
            Box(Modifier.fillMaxWidth().weight(.55f).padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(18.dp)).background(c.card)
                .border(1.dp, accent.copy(.3f), RoundedCornerShape(18.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> isDragging = true; dragStart = offset; dragEnd = offset },
                        onDrag = { change, _ ->
                            dragEnd = change.position
                            if (canvasSize != Size.Zero) {
                                val r = Rect(
                                    (minOf(dragStart.x, dragEnd.x) / canvasSize.width).coerceIn(0f,1f),
                                    (minOf(dragStart.y, dragEnd.y) / canvasSize.height).coerceIn(0f,1f),
                                    (maxOf(dragStart.x, dragEnd.x) / canvasSize.width).coerceIn(0f,1f),
                                    (maxOf(dragStart.y, dragEnd.y) / canvasSize.height).coerceIn(0f,1f)
                                )
                                vm.selectArea(r)
                            }
                        },
                        onDragEnd = { isDragging = false }
                    )
                }
            ) {
                if (s.imagePath != null) {
                    AsyncImage(s.imagePath, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().onSizeChanged { sz ->
                            canvasSize = Size(sz.width.toFloat(), sz.height.toFloat())
                        })
                } else {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.TextFields, null, tint = accent, modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Capture image first", style = MaterialTheme.typography.bodyMedium, color = c.subtext)
                        }
                    }
                }

                // Draw text block highlights + selection
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    s.textBlocks.forEach { block ->
                        val b = block.boundingBox
                        drawRect(accent.copy(.18f), Offset(b.left * size.width, b.top * size.height),
                            Size((b.right - b.left) * size.width, (b.bottom - b.top) * size.height))
                        drawRect(accent, Offset(b.left * size.width, b.top * size.height),
                            Size((b.right - b.left) * size.width, (b.bottom - b.top) * size.height),
                            style = Stroke(1.8f))
                    }
                    // Selection rect
                    if (isDragging || s.selectedText.isNotEmpty()) {
                        val l = minOf(dragStart.x, dragEnd.x); val t = minOf(dragStart.y, dragEnd.y)
                        val w = kotlin.math.abs(dragEnd.x - dragStart.x); val h = kotlin.math.abs(dragEnd.y - dragStart.y)
                        drawRect(Color(0x3300E5FF), Offset(l, t), Size(w, h))
                        drawRect(Color(0xFF00E5FF), Offset(l, t), Size(w, h), style = Stroke(2.2f))
                        // Handles
                        val hr = 7.dp.toPx()
                        listOf(Offset(l, t), Offset(l+w, t), Offset(l, t+h), Offset(l+w, t+h))
                            .forEach { drawCircle(Color(0xFF00E5FF), hr, it) }
                    }
                }
                // Hint
                Box(Modifier.align(Alignment.BottomCenter).padding(8.dp)
                    .background(Color.Black.copy(.6f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 11.dp, vertical = 5.dp)) {
                    Text(if (s.textBlocks.isEmpty()) "Drag to select text area" else "Drag to select specific text",
                        style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }

            // Text result panel
            Column(Modifier.fillMaxWidth().weight(.45f).padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(if (s.selectedText.isNotEmpty()) "Selected Text" else "Full Text",
                        style = MaterialTheme.typography.titleMedium, color = c.onSurface, fontWeight = FontWeight.SemiBold)
                    if (s.selectedText.isNotEmpty())
                        TextButton(onClick = { vm.clearSelection() }) { Text("Clear", color = c.subtext) }
                }
                val displayText = s.selectedText.ifEmpty { s.fullText }
                if (displayText.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                        Text("No text detected yet", style = MaterialTheme.typography.bodyMedium, color = c.subtext)
                    }
                } else {
                    Box(Modifier.fillMaxWidth().weight(1f)
                        .clip(RoundedCornerShape(14.dp)).background(c.card)
                        .border(1.dp, accent.copy(.28f), RoundedCornerShape(14.dp))
                        .padding(14.dp).verticalScroll(rememberScrollState())) {
                        SelectionContainer { Text(displayText, style = MaterialTheme.typography.bodyMedium, color = c.onSurface) }
                    }
                }
                // Action chips
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple(Icons.Rounded.ContentCopy, "Copy", { vm.copySelected() }),
                        Triple(Icons.Rounded.Translate, "Translate", {
                            val t = s.selectedText.ifEmpty { s.fullText }
                            onTranslate?.invoke(t)
                            Unit
                        }),
                        Triple(Icons.Rounded.VolumeUp, "Speak", { vm.speakSelected() })).forEach { (icon, lbl, action) ->
                        FilterChip(selected = false, onClick = action, label = { Text(lbl, style = MaterialTheme.typography.labelLarge) },
                            leadingIcon = { Icon(icon, null, modifier = Modifier.size(15.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = c.card, labelColor = c.onSurface, iconColor = accent))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
