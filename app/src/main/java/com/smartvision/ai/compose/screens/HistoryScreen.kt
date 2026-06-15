package com.smartvision.ai.compose.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.presentation.history.HistoryViewModel
import com.smartvision.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val ext     = MaterialTheme.extended
    val items   by viewModel.history.collectAsStateWithLifecycle()
    var filter  by remember { mutableStateOf("All") }
    val filters = listOf("All", "OCR", "Translation", "Chat", "Waste", "Medicine", "QR Scan", "Document", "Object")
    val fmt     = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val ctx     = LocalContext.current

    val filtered = items.filter { item ->
        when (filter) {
            "All" -> true
            "OCR" -> item.type == "OCR"
            "Translation" -> item.type == "Translation" || item.type == "Voice"
            "Chat" -> item.type == "ChatUser" || item.type == "ChatBot"
            "Waste" -> item.type == "Waste"
            "Medicine" -> item.type == "Medicine"
            "QR Scan" -> item.type == "QR Scan" || item.type == "QR"
            "Document" -> item.type == "Document"
            "Object" -> item.type == "Object"
            else -> false
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(
                title  = "History",
                onBack = onBack,
                actions = {
                    if (filtered.isNotEmpty()) {
                        Text(
                            "Download Report 📥",
                            color = NeonBlue,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .clickable {
                                    val reportBuilder = StringBuilder()
                                    reportBuilder.append("========================================\n")
                                    reportBuilder.append("        SMART VISION AI HISTORY REPORT  \n")
                                    reportBuilder.append("        Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
                                    reportBuilder.append("========================================\n\n")
                                    filtered.forEachIndexed { index, record ->
                                        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(record.createdAt))
                                        val mappedType = when (record.type) {
                                            "ChatUser" -> "Chat (You)"
                                            "ChatBot" -> "Chat (AI)"
                                            else -> record.type
                                        }
                                        val title = record.title
                                        val details = if (record.type == "ChatUser" || record.type == "ChatBot") "" else record.details
                                        reportBuilder.append("[${index + 1}] TYPE: $mappedType | DATE: $dateStr\n")
                                        reportBuilder.append("CONTENT/TITLE: $title\n")
                                        if (details.isNotEmpty()) {
                                            reportBuilder.append("DETAILS: $details\n")
                                        }
                                        reportBuilder.append("----------------------------------------\n\n")
                                    }
                                    val filename = "SmartVisionAI_HistoryReport_${System.currentTimeMillis() / 1000}.txt"
                                    com.smartvision.ai.utils.FileExportHelper.saveTextFile(ctx, filename, reportBuilder.toString())
                                        .onSuccess {
                                            android.widget.Toast.makeText(ctx, "Report saved: $filename", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                        .onFailure {
                                            android.widget.Toast.makeText(ctx, "Failed to export: ${it.message}", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                }
                        )
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(
                        "Clear All",
                        color = NeonPink,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable { viewModel.clearHistory() }
                    )
                }
            )

            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(filters) { f ->
                    val isSelected = filter == f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .then(
                                if (isSelected)
                                    Modifier.background(Brush.horizontalGradient(listOf(ext.neonBlue, ext.neonPurple)))
                                else
                                    Modifier.background(ext.glassCard)
                                         .border(1.dp, ext.glassBorder, RoundedCornerShape(20.dp))
                            )
                            .clickable { filter = f }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(f, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📜", fontSize = 56.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No history yet", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered) { item ->
                        val accent = when (item.type) {
                            "OCR"      -> AccentOCR
                            "Translation" -> AccentTranslate
                            "Voice"    -> AccentVoice
                            "ChatUser", "ChatBot" -> AccentAI
                            "Waste"    -> AccentWaste
                            "Medicine" -> AccentMedicine
                            "QR Scan", "QR" -> AccentQR
                            "Object"   -> AccentObject
                            "Document" -> AccentDocument
                            else       -> NeonBlue
                        }
                        val emoji = when (item.type) {
                            "OCR"      -> "📷"
                            "Translation" -> "🌐"
                            "Voice"    -> "🎙️"
                            "ChatUser" -> "👤"
                            "ChatBot"  -> "🤖"
                            "Waste"    -> "♻️"
                            "Medicine" -> "💊"
                            "QR Scan", "QR" -> "🔍"
                            "Object"   -> "👁️"
                            "Document" -> "📄"
                            else       -> "📜"
                        }
                        val isChat = item.type == "ChatUser" || item.type == "ChatBot"
                        val titleText = when (item.type) {
                            "ChatUser" -> "You"
                            "ChatBot"  -> "AI Assistant"
                            else       -> item.title
                        }
                        val detailsText = if (isChat) item.title else item.details

                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ext.glassCard)
                                .border(1.dp, accent.copy(0.2f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isChat) accent else MaterialTheme.colorScheme.onSurface,
                                    maxLines = if (isChat) 1 else 2
                                )
                                Text(
                                    text = detailsText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (isChat) 5 else 2
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = fmt.format(java.util.Date(item.createdAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.textHint
                            )
                        }
                    }
                }
            }
        }
    }
}

