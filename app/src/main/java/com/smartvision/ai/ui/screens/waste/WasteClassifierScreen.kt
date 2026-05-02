package com.smartvision.ai.ui.screens.waste

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.ui.components.*
import com.smartvision.ai.ui.theme.*

@Composable
fun WasteClassifierScreen(onBack: () -> Unit, onCapture: () -> Unit, vm: WasteClassifierViewModel = hiltViewModel()) {
    val c = svColors; val accent = SVColors.green2
    val uiState by vm.uiState.collectAsState()
    val result = uiState.result
    val path = uiState.imagePath

    Scaffold(topBar = { SmartVisionTopBar("Waste Classifier", onBack = onBack) }, containerColor = c.background) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(2.dp))
            Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)).background(c.card).border(1.dp, accent.copy(.3f), RoundedCornerShape(20.dp))) {
                if (path != null) AsyncImage(path, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Recycling, null, tint = accent, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Point camera at waste item", style = MaterialTheme.typography.bodyMedium, color = c.subtext)
                }
            }
            if (path == null) Button(onClick = onCapture, modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp)); Text("Classify Waste", fontWeight = FontWeight.Bold, color = Color.White)
            }
            when (result) {
                is ScanResult.WasteClassifierResult -> {
                    val r = result as ScanResult.WasteClassifierResult
                    val winner = r.categories.maxByOrNull { it.confidence }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card)
                        .border(1.dp, accent.copy(.2f), RoundedCornerShape(20.dp)).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Classification Results", style = MaterialTheme.typography.titleMedium, color = c.onSurface, fontWeight = FontWeight.SemiBold)
                        winner?.let { w ->
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                                .background(Color(w.color).copy(.11f)).border(1.dp, Color(w.color).copy(.4f), RoundedCornerShape(13.dp)).padding(13.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = Color(w.color), modifier = Modifier.size(22.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Primary Classification", style = MaterialTheme.typography.labelLarge, color = Color(w.color))
                                        Text(w.label, style = MaterialTheme.typography.headlineSmall, color = c.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                    ConfidenceBadge(w.confidence)
                                }
                            }
                        }
                        HorizontalDivider(color = c.border)
                        r.categories.forEach { cat ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text(cat.label, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, fontWeight = FontWeight.Medium)
                                    Text("${(cat.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Color(cat.color), fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(progress = { cat.confidence }, color = Color(cat.color), trackColor = c.border,
                                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape))
                            }
                        }
                    }
                    // Disposal tip
                    winner?.let { w ->
                        val (tip, icon) = when {
                            w.label.contains("Recyclable", true) -> "Place in the blue recycling bin. Rinse before recycling." to Icons.Rounded.Recycling
                            w.label.contains("Organic", true)    -> "Place in the green compost bin or home compost pile."     to Icons.Rounded.Eco
                            else -> "Place in general waste bin. Cannot be recycled." to Icons.Rounded.DeleteOutline
                        }
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(accent.copy(.06f))
                            .border(1.dp, accent.copy(.2f), RoundedCornerShape(14.dp)).padding(14.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = accent, modifier = Modifier.size(21.dp))
                                Column { Text("Disposal Tip", style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.SemiBold); Text(tip, style = MaterialTheme.typography.bodySmall, color = c.onSurface) }
                            }
                        }
                    }
                }
                is ScanResult.Loading -> Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) { CircularProgressIndicator(color = accent) }
                else -> {}
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
