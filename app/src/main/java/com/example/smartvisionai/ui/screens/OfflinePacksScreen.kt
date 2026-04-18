package com.example.smartvisionai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartvisionai.ui.theme.*

data class OfflinePack(
    val id: String,
    val name: String,
    val description: String,
    val sizeMB: Float,
    val isDownloaded: Boolean,
    val iconEmoji: String,
    val accentColor: Color
)

@Composable
fun OfflinePacksScreen(onBack: () -> Unit) {
    var packs by remember {
        mutableStateOf(
            listOf(
                OfflinePack("translate_en", "English Translate", "Offline translation for English", 48f, true, "🇬🇧", CyanAccent),
                OfflinePack("translate_hi", "Hindi Translate", "Offline translation for Hindi", 52f, true, "🇮🇳", OrangeAccent),
                OfflinePack("translate_es", "Spanish Translate", "Offline translation for Spanish", 49f, true, "🇪🇸", Color(0xFFE91E63)),
                OfflinePack("translate_fr", "French Translate", "Offline translation for French", 47f, false, "🇫🇷", Color(0xFF2196F3)),
                OfflinePack("translate_de", "German Translate", "Offline translation for German", 50f, false, "🇩🇪", Color(0xFFFF9800)),
                OfflinePack("translate_ar", "Arabic Translate", "Offline translation for Arabic", 55f, false, "🇸🇦", GreenAccent),
                OfflinePack("translate_zh", "Chinese Translate", "Offline translation for Chinese", 60f, false, "🇨🇳", RedAccent),
                OfflinePack("tflite_waste", "Waste Classifier Model", "On-device waste classification", 12f, true, "♻", GreenAccent),
                OfflinePack("tflite_obj", "Object Detection Model", "EfficientDet-Lite0 model", 6f, true, "🔍", CyanAccent),
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clickable { onBack() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary, modifier = Modifier.size(20.dp))
            Text("Back", fontSize = 14.sp, color = TextSecondary)
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("OFFLINE PACKS", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                color = CyanAccent, letterSpacing = 2.sp)
            Text("Download for offline use", fontSize = 12.sp, color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp))
        }

        val downloaded = packs.count { it.isDownloaded }
        val totalSize  = packs.filter { it.isDownloaded }.sumOf { it.sizeMB.toDouble() }

        // Stats row
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(label = "Downloaded", value = "$downloaded", modifier = Modifier.weight(1f))
            StatCard(label = "Storage Used", value = "${totalSize.toInt()} MB", modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Language packs section
            Text("LANGUAGE PACKS", fontSize = 10.sp, color = TextMuted, letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp))

            packs.filter { it.id.startsWith("translate") }.forEach { pack ->
                PackRow(pack = pack, onToggle = { id ->
                    packs = packs.map { if (it.id == id) it.copy(isDownloaded = !it.isDownloaded) else it }
                })
            }

            Spacer(Modifier.height(8.dp))

            Text("ML MODELS", fontSize = 10.sp, color = TextMuted, letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))

            packs.filter { it.id.startsWith("tflite") }.forEach { pack ->
                PackRow(pack = pack, onToggle = { id ->
                    packs = packs.map { if (it.id == id) it.copy(isDownloaded = !it.isDownloaded) else it }
                })
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun PackRow(pack: OfflinePack, onToggle: (String) -> Unit) {
    var isLoading by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Text(pack.iconEmoji, fontSize = 22.sp, modifier = Modifier.width(36.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(pack.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(pack.description, fontSize = 11.sp, color = TextSecondary)
            Text("${pack.sizeMB.toInt()} MB", fontSize = 10.sp, color = TextMuted,
                modifier = Modifier.padding(top = 2.dp))
        }

        Spacer(Modifier.width(8.dp))

        if (pack.isDownloaded) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = GreenAccent.copy(.12f),
                border = BorderStroke(1.dp, GreenAccent.copy(.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Check, null, tint = GreenAccent, modifier = Modifier.size(12.dp))
                    Text("Ready", fontSize = 11.sp, color = GreenAccent)
                }
            }
        } else {
            IconButton(
                onClick = { isLoading = true; onToggle(pack.id) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyanAccent.copy(.12f))
                    .border(1.dp, CyanAccent.copy(.4f), RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.Download, "Download", tint = CyanAccent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
        Text(label, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
    }
}
