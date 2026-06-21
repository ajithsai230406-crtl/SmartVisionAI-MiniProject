package com.smartvision.ai.compose.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.domain.model.*
import com.smartvision.ai.presentation.history.HistoryDetailViewModel
import com.smartvision.ai.ui.theme.*
import androidx.compose.ui.layout.ContentScale
import com.smartvision.ai.detector.domain.ObjectKnowledge
import com.smartvision.ai.detector.domain.toCategory
import com.smartvision.ai.detector.ui.VisualSearchContent

// ══════════════════════════════════════════════════════════════════════════════
// HISTORY DETAIL SCREEN — Full restoration of original scan/translation result
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun HistoryDetailScreen(
    onBack: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel()
) {
    val record  by viewModel.record.collectAsStateWithLifecycle()
    val loading by viewModel.isLoading.collectAsStateWithLifecycle()
    val ext     = MaterialTheme.extended

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(
                title = record?.type?.let { "📜 $it History" } ?: "History Detail",
                onBack = onBack
            )

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            } else if (record == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Record not found", color = ext.textHint, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                val r = record!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dispatch to module-specific renderer
                    when (r.type) {
                        "Translation" -> translationDetailItems(r, viewModel)
                        "Voice"       -> voiceDetailItems(r, viewModel)
                        "OCR"         -> ocrDetailItems(r, viewModel)
                        "QR Scan", "QR" -> qrDetailItems(r)
                        "Object"      -> objectDetailItems(r)
                        "Waste"       -> wasteDetailItems(r)
                        "Medicine"    -> medicineDetailItems(r)
                        "Student"     -> studentDetailItems(r)
                        "Career"      -> careerDetailItems(r)
                        else          -> item { FallbackDetail(r) }
                    }
                }
            }
        }
    }
}

// ─── TRANSLATION ──────────────────────────────────────────────────────────────
private fun LazyListScope.translationDetailItems(r: HistoryRecord, vm: HistoryDetailViewModel) {
    val d = HistoryJsonSerializer.deserializeTranslation(r.detailsJson) ?: return
    item { SectionHeader("🌐 Translation Result") }
    item { DetailCard("Original Text (${d.sourceLang})", d.originalText, AccentTranslate) { vm.speak(d.originalText) } }
    item { DetailCard("Translated Text (${d.targetLang})", d.translatedText, NeonGreen) { vm.speak(d.translatedText) } }
    d.meaningInSource?.let    { item { MeaningCard("Meaning in ${d.sourceLang}", it, NeonCyan) } }
    d.meaningInTarget?.let    { item { MeaningCard("Meaning in ${d.targetLang}", it, NeonPurple) } }
    d.meaningInEnglish?.let   { item { MeaningCard("Meaning in English", it, NeonBlue) } }
    d.pronunciation?.let      { item { MeaningCard("🔊 Pronunciation", it, AccentVoice) } }
    if (d.context != null || d.usageExamples.isNotEmpty() || d.alternativeMeanings.isNotEmpty()) {
        item { SectionHeader("🧠 AI Semantic Breakdown") }
        d.context?.let { item { MeaningCard("Context & Nuance", it, AccentStudent) } }
        if (d.usageExamples.isNotEmpty()) {
            item { ListCard("Usage Examples", d.usageExamples, NeonGreen) }
        }
        if (d.alternativeMeanings.isNotEmpty()) {
            item { ListCard("Alternative Meanings", d.alternativeMeanings, NeonPurple) }
        }
    }
}

// ─── VOICE ────────────────────────────────────────────────────────────────────
private fun LazyListScope.voiceDetailItems(r: HistoryRecord, vm: HistoryDetailViewModel) {
    val d = HistoryJsonSerializer.deserializeVoice(r.detailsJson) ?: return
    item { SectionHeader("🎙️ Voice Conversation (${d.entries.size} entries)") }
    d.entries.forEachIndexed { idx, e ->
        item(key = "voice_$idx") {
            val isUser = e.isFromUser
            val bg = if (isUser) AccentTranslate.copy(0.12f) else NeonGreen.copy(0.12f)
            val border = if (isUser) AccentTranslate.copy(0.3f) else NeonGreen.copy(0.3f)
            val align = if (isUser) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = align
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "${if (isUser) "👤" else "🌐"} ${e.sourceLang} → ${e.targetLang}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) AccentTranslate else NeonGreen
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(e.originalText, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(e.translatedText, style = MaterialTheme.typography.bodyMedium, color = NeonCyan, fontWeight = FontWeight.SemiBold)

                    // TTS buttons
                    Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TtsChip("🔊 Original") { vm.speak(e.originalText) }
                        TtsChip("🔊 Translated") { vm.speak(e.translatedText) }
                    }

                    // Show semantic if available
                    e.meaningInEnglish?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("Meaning (EN): $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    e.context?.let {
                        Text("Context: $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

// ─── OCR ──────────────────────────────────────────────────────────────────────
private fun LazyListScope.ocrDetailItems(r: HistoryRecord, vm: HistoryDetailViewModel) {
    val d = HistoryJsonSerializer.deserializeOcr(r.detailsJson) ?: return
    item { SectionHeader("📷 OCR Translation Result") }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBadge("📝 ${d.wordCount} words", AccentOCR)
            StatBadge("📄 ${d.paragraphCount} para", AccentOCR)
            StatBadge("${d.sourceLang} → ${d.targetLang ?: "?"}", AccentTranslate)
        }
    }
    item { DetailCard("Extracted Text", d.extractedText, AccentOCR) { vm.speak(d.extractedText) } }
    d.translatedText?.let { item { DetailCard("Translated Text (${d.targetLang})", it, NeonGreen) { vm.speak(it) } } }
    d.meaningInSource?.let    { item { MeaningCard("Meaning in ${d.sourceLang}", it, NeonCyan) } }
    d.meaningInTarget?.let    { item { MeaningCard("Meaning in ${d.targetLang ?: "target"}", it, NeonPurple) } }
    d.meaningInEnglish?.let   { item { MeaningCard("Meaning in English", it, NeonBlue) } }
    if (d.context != null || d.usageExamples.isNotEmpty()) {
        item { SectionHeader("🧠 AI Semantic Breakdown") }
        d.context?.let { item { MeaningCard("Context & Nuance", it, AccentStudent) } }
        if (d.usageExamples.isNotEmpty()) {
            item { ListCard("Usage Examples", d.usageExamples, NeonGreen) }
        }
    }
}

// ─── QR SCAN ──────────────────────────────────────────────────────────────────
private fun LazyListScope.qrDetailItems(r: HistoryRecord) {
    val d = HistoryJsonSerializer.deserializeQr(r.detailsJson) ?: return
    item { SectionHeader("🔍 QR/Barcode Scan Result") }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBadge(d.formatName, AccentQR)
            StatBadge(d.contentType.replace("_", " "), NeonGreen)
        }
    }
    item { DetailCard("Raw Value", d.rawValue, AccentQR) }
    if (d.displayValue != d.rawValue) {
        item { DetailCard("Display Value", d.displayValue, NeonCyan) }
    }
    d.wifiSsid?.let { ssid ->
        item {
            GlassInfoCard("📶 WiFi Network") {
                Text("SSID: $ssid", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                d.wifiPassword?.let { Text("Password: $it", style = MaterialTheme.typography.bodyMedium, color = NeonGreen) }
                d.wifiSecurity?.let { Text("Security: $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
            }
        }
    }
}

// ─── OBJECT DETECTION ─────────────────────────────────────────────────────────
private fun LazyListScope.objectDetailItems(r: HistoryRecord) {
    val d = HistoryJsonSerializer.deserializeObject(r.detailsJson) ?: return
    if (d.searchData != null) {
        item {
            ObjectVisualSearchResultView(d)
        }
    } else {
        item { SectionHeader("${d.emoji} ${d.label}") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBadge("${(d.confidence * 100).toInt()}% confidence", NeonGreen)
                StatBadge(d.category, AccentObject)
            }
        }
        item { MeaningCard("Description", d.description, AccentObject) }
        if (d.uses.isNotEmpty()) { item { ListCard("Uses", d.uses, NeonBlue) } }
        if (d.safetyInfo.isNotBlank()) { item { MeaningCard("⚠️ Safety", d.safetyInfo, NeonOrange) } }
        if (d.environmentalImpact.isNotBlank()) { item { MeaningCard("🌍 Environmental Impact", d.environmentalImpact, NeonGreen) } }
        if (d.funFact.isNotBlank()) { item { MeaningCard("💡 Fun Fact", d.funFact, NeonCyan) } }
        if (d.maintenanceTips.isNotEmpty()) { item { ListCard("🔧 Maintenance Tips", d.maintenanceTips, AccentStudent) } }
        if (d.buyingSuggestions.isNotEmpty()) { item { ListCard("🛒 Buying Suggestions", d.buyingSuggestions, NeonPurple) } }
    }
}

@Composable
private fun ObjectVisualSearchResultView(d: ObjectHistoryDetail) {
    val color = AccentObject
    val pct = (d.confidence * 100).toInt()
    var activeTab by remember { mutableStateOf(0) }
    val ext = MaterialTheme.extended

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Side: Image from path
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, color.copy(0.3f), RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(0.4f)),
                contentAlignment = Alignment.Center
            ) {
                val cachedBitmap = remember(d.imagePath) {
                    d.imagePath?.let { path ->
                        try {
                            android.graphics.BitmapFactory.decodeFile(path)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                if (cachedBitmap != null) {
                    Image(
                        bitmap = cachedBitmap.asImageBitmap(),
                        contentDescription = "Scanned Object",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(d.emoji.ifBlank { "🔍" }, fontSize = 44.sp)
                }
            }

            // Right Side: Item name, category & match percentage
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = d.label.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(0.12f))
                            .border(1.dp, color.copy(0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = d.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonGreen.copy(0.12f))
                            .border(1.dp, NeonGreen.copy(0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$pct% Match",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                // Confidence bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Conf: $pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(d.confidence)
                                .background(color, CircleShape)
                        )
                    }
                }
            }
        }

        // Segmented Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(0.04f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val tabs = listOf("Overview", "Visual Match", "Specs")
            tabs.forEachIndexed { index, title ->
                val isSel = activeTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) NeonBlue.copy(0.18f) else Color.Transparent)
                        .border(1.dp, if (isSel) NeonBlue.copy(0.35f) else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { activeTab = index }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSel) Color.White else Color.White.copy(0.6f),
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        val know = remember(d) {
            ObjectKnowledge(
                label = d.label,
                category = d.label.toCategory(),
                description = d.description,
                uses = d.uses,
                safetyInfo = d.safetyInfo,
                environmentalImpact = d.environmentalImpact,
                funFact = d.funFact,
                emoji = d.emoji,
                maintenanceTips = d.maintenanceTips,
                buyingSuggestions = d.buyingSuggestions,
                relatedRecommendations = emptyList(),
                specializedActionType = null,
                specializedActionData = null,
                searchData = d.searchData
            )
        }

        VisualSearchContent(
            know = know,
            activeTab = activeTab,
            color = color,
            onNavigate = {}
        )
    }
}

// ─── WASTE CLASSIFICATION ─────────────────────────────────────────────────────
private fun LazyListScope.wasteDetailItems(r: HistoryRecord) {
    val d = HistoryJsonSerializer.deserializeWaste(r.detailsJson) ?: return
    val pct = (d.confidence * 100).toInt()

    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0C1622))
                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Side: Image from path
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, NeonGreen.copy(0.3f), RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val cachedBitmap = remember(d.imagePath) {
                    d.imagePath?.let { path ->
                        try {
                            android.graphics.BitmapFactory.decodeFile(path)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                if (cachedBitmap != null) {
                    Image(
                        bitmap = cachedBitmap.asImageBitmap(),
                        contentDescription = "Scanned Waste Object",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("♻️", fontSize = 48.sp)
                }
            }

            // Right Side: Item name & details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = d.exactItemName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = d.wasteType + " Waste",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonGreen,
                    fontWeight = FontWeight.SemiBold
                )

                // Recyclability Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (d.isRecyclable) NeonGreen.copy(0.12f) else Color(0xFF33151A))
                        .border(1.dp, if (d.isRecyclable) NeonGreen.copy(0.3f) else Color(0xFFFF073A).copy(0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (d.isRecyclable) "♻️ Recyclable" else "🗑️ Non-Recyclable",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (d.isRecyclable) NeonGreen else Color(0xFFFF073A),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(4.dp))
                // Confidence bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Conf: $pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.6f)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(d.confidence)
                                .background(NeonGreen, CircleShape)
                        )
                    }
                }
            }
        }
    }

    // About This Item
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0C1622))
                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "About This Item",
                style = MaterialTheme.typography.labelMedium,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = d.environmentalImpact,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.85f),
                lineHeight = 20.sp
            )
        }
    }

    // Disposal Instructions
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0C1622))
                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Disposal Instructions",
                style = MaterialTheme.typography.labelMedium,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // How to Dispose
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.04f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✅", fontSize = 16.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("How to Dispose", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontWeight = FontWeight.Bold)
                        Text(d.disposalMethod, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 16.sp)
                    }
                }
                
                // Safety Precautions
                if (d.safetyPrecautions.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.04f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚠️", fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Safety Precautions", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontWeight = FontWeight.Bold)
                            Text(d.safetyPrecautions, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 16.sp)
                        }
                    }
                }

                // Environmental Impact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.04f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌍", fontSize = 16.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Environmental Impact", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontWeight = FontWeight.Bold)
                        Text(d.environmentalImpact, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 16.sp)
                    }
                }

                // Recycling Tips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.04f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💡", fontSize = 16.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Recycling Tips", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontWeight = FontWeight.Bold)
                        Text(d.sustainabilityTip, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

// ─── MEDICINE ─────────────────────────────────────────────────────────────────
private fun LazyListScope.medicineDetailItems(r: HistoryRecord) {
    val d = HistoryJsonSerializer.deserializeMedicine(r.detailsJson) ?: return
    item { SectionHeader("💊 ${d.name}") }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBadge("Safety: ${d.safetyScore}/10", when {
                d.safetyScore >= 8 -> NeonGreen
                d.safetyScore >= 5 -> NeonOrange
                else -> NeonPink
            })
            StatBadge(d.category, AccentMedicine)
            if (d.prescriptionOnly) StatBadge("℞ Prescription", NeonPink)
        }
    }
    if (d.genericName.isNotBlank()) { item { MeaningCard("Generic Name", d.genericName, NeonCyan) } }
    if (d.composition.isNotBlank()) { item { MeaningCard("Composition", d.composition, NeonBlue) } }
    if (d.dosage.isNotBlank()) { item { MeaningCard("💉 Dosage", "${d.dosage} • ${d.frequency}", AccentMedicine) } }
    if (d.uses.isNotEmpty()) { item { ListCard("✅ Uses", d.uses, NeonGreen) } }
    if (d.sideEffects.isNotEmpty()) { item { ListCard("⚠️ Side Effects", d.sideEffects, NeonOrange) } }
    if (d.warnings.isNotEmpty()) { item { ListCard("🚨 Warnings", d.warnings, NeonPink) } }
    if (d.interactions.isNotEmpty()) { item { ListCard("💊 Drug Interactions", d.interactions, NeonPurple) } }
    if (d.whoShouldAvoid.isNotEmpty()) { item { ListCard("🚫 Who Should Avoid", d.whoShouldAvoid, NeonPink) } }
    d.emergencyWarning?.let { item { MeaningCard("🆘 Emergency Warning", it, NeonPink) } }
    if (d.storageInfo.isNotBlank()) { item { MeaningCard("📦 Storage", d.storageInfo, TextSecondary) } }
    if (d.aiSummary.isNotBlank()) { item { MeaningCard("🤖 AI Summary", d.aiSummary, NeonBlue) } }
    item {
        Text(
            "⚕️ Medical information is AI-generated for educational purposes only. Always consult a certified doctor or pharmacist.",
            style = MaterialTheme.typography.bodySmall,
            color = TextHint,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ─── STUDENT HELPER ───────────────────────────────────────────────────────────
private fun LazyListScope.studentDetailItems(r: HistoryRecord) {
    val d = HistoryJsonSerializer.deserializeStudent(r.detailsJson) ?: return
    item { SectionHeader("📚 ${d.subject} — Student Helper") }
    item { DetailCard("❓ Question", d.question, AccentStudent) }
    item { DetailCard("✅ Answer", d.answer, NeonGreen) }
}

// ─── CAREER ASSISTANT ─────────────────────────────────────────────────────────
private fun LazyListScope.careerDetailItems(r: HistoryRecord) {
    val d = HistoryJsonSerializer.deserializeCareer(r.detailsJson) ?: return
    item { SectionHeader("💼 Career Assistant: ${d.title}") }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBadge(d.actionType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, AccentCareer)
            if (d.atsScore >= 0) {
                StatBadge("ATS Score: ${d.atsScore}%", if (d.atsScore >= 70) NeonGreen else NeonOrange)
            }
        }
    }
    
    if (d.details.isNotBlank()) {
        item { MeaningCard("Session Summary", d.details, AccentCareer) }
    }
    
    if (d.generatedContent.isNotBlank()) {
        item {
            val clipboardManager = LocalClipboardManager.current
            val context = LocalContext.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.extended.glassCard)
                    .border(1.dp, AccentCareer.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Generated Content",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentCareer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(d.generatedContent))
                            android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy text",
                            tint = AccentCareer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = d.generatedContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 22.sp
                )
            }
        }
    }

    if (d.interviewQuestionsJson.isNotBlank()) {
        runCatching {
            val jsonArray = org.json.JSONArray(d.interviewQuestionsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val qText = obj.optString("questionText", "")
                val uAns = obj.optString("userTranscript", "")
                val feedback = obj.optString("reviewFeedback", "")
                val score = obj.optInt("score", -1)
                
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.extended.glassCard)
                            .border(1.5.dp, NeonCyan.copy(0.2f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Question ${i + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(qText, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        
                        if (uAns.isNotBlank()) {
                            Divider(color = Color.White.copy(0.08f))
                            Text("Your Answer:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(uAns, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                        
                        if (feedback.isNotBlank()) {
                            Divider(color = Color.White.copy(0.08f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("AI Feedback:", style = MaterialTheme.typography.labelSmall, color = NeonPurple, fontWeight = FontWeight.Bold)
                                if (score >= 0) {
                                    StatBadge("Score: $score/10", if (score >= 7) NeonGreen else NeonOrange)
                                }
                            }
                            Text(feedback, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

// ─── FALLBACK ─────────────────────────────────────────────────────────────────
@Composable
private fun FallbackDetail(r: HistoryRecord) {
    Column {
        SectionHeader("📜 ${r.type}")
        Spacer(Modifier.height(8.dp))
        DetailCardComposable(r.type, r.title, NeonBlue)
        Spacer(Modifier.height(8.dp))
        DetailCardComposable("Details", r.details, TextSecondary)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// REUSABLE UI COMPONENTS — Premium dark-glass cards
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun DetailCardComposable(label: String, content: String, accent: Color, onSpeak: (() -> Unit)? = null) {
    val ext = MaterialTheme.extended
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (onSpeak != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent.copy(0.15f))
                        .clickable { onSpeak() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔊", fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(content, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, lineHeight = 22.sp)
    }
}

/** Non-composable wrapper for LazyListScope item builder */
private fun DetailCard(label: String, content: String, accent: Color, onSpeak: (() -> Unit)? = null): @Composable () -> Unit = {
    DetailCardComposable(label, content, accent, onSpeak)
}

@Composable
private fun MeaningCardComposable(label: String, text: String, accent: Color) {
    val ext = MaterialTheme.extended
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(0.08f))
            .border(1.dp, accent.copy(0.2f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextPrimary, lineHeight = 20.sp)
    }
}

private fun MeaningCard(label: String, text: String, accent: Color): @Composable () -> Unit = {
    MeaningCardComposable(label, text, accent)
}

@Composable
private fun ListCardComposable(label: String, items: List<String>, accent: Color) {
    val ext = MaterialTheme.extended
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.glassCard)
            .border(1.dp, accent.copy(0.2f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        items.forEachIndexed { i, s ->
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text("${i + 1}.", style = MaterialTheme.typography.bodySmall, color = accent, modifier = Modifier.width(22.dp))
                Text(s, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
        }
    }
}

private fun ListCard(label: String, items: List<String>, accent: Color): @Composable () -> Unit = {
    ListCardComposable(label, items, accent)
}

@Composable
private fun StatBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(0.12f))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TtsChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(NeonBlue.copy(0.1f))
            .border(1.dp, NeonBlue.copy(0.25f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = NeonBlue)
    }
}

@Composable
private fun GlassInfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val ext = MaterialTheme.extended
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = NeonBlue, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        content()
    }
}
