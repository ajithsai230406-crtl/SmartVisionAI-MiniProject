 package com.smartvision.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.smartvision.ai.ui.screens.medical.MedicalScannerViewModel
import com.smartvision.ai.ui.screens.waste.WasteClassifierViewModel

@Composable
fun MedicalScannerScreen(
    onBack:    () -> Unit,
    onCapture: () -> Unit,
    viewModel: MedicalScannerViewModel = hiltViewModel()
) {
    val colors  = smartColors
    val uiState by viewModel.uiState.collectAsState()
    val accent  = SmartVisionColors.medicalScanner

    Scaffold(
        topBar         = { SmartVisionTopBar(title = "Medical Scanner", onBack = onBack) },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Scanned Image ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.card)
                    .border(1.dp, accent.copy(0.3f), RoundedCornerShape(22.dp))
            ) {
                if (uiState.imagePath != null) {
                    AsyncImage(
                        model = uiState.imagePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        Modifier.fillMaxSize(),
                        Arrangement.Center,
                        Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.MedicalServices, null, tint = accent, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Scan a medicine strip", style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
                    }
                }
            }

            // Capture button
            if (uiState.imagePath == null) {
                Button(
                    onClick = onCapture,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Medicine", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            // ── Result ────────────────────────────────────────────────────────
            when (val result = uiState.result) {
                is ScanResult.MedicalScanResult -> {
                    MedicalResultDetailCard(result = result, accent = accent)
                }
                is ScanResult.Loading -> {
                    Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                }
                is ScanResult.Error -> {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(colors.error.copy(0.08f)).border(1.dp, colors.error.copy(0.3f), RoundedCornerShape(14.dp)).padding(16.dp)
                    ) {
                        Text(result.message, color = colors.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                else -> {}
            }

            // ── Disclaimer ────────────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(SmartVisionColors.translator.copy(0.07f))
                    .border(1.dp, SmartVisionColors.translator.copy(0.2f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Warning, null, tint = SmartVisionColors.translator, modifier = Modifier.size(18.dp))
                    Text(
                        "This tool provides general information only. Always consult a healthcare professional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.subtext
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MedicalResultDetailCard(result: ScanResult.MedicalScanResult, accent: Color) {
    val colors = smartColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card)
            .border(1.dp, accent.copy(0.25f), RoundedCornerShape(22.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(result.medicineName, style = MaterialTheme.typography.headlineMedium, color = colors.onSurface, fontWeight = FontWeight.Bold)
        Divider(color = colors.cardBorder)

        MedRow(icon = Icons.Rounded.Info, label = "Usage",   value = result.usage,  accent = accent)
        MedRow(icon = Icons.Rounded.Medication, label = "Dosage", value = result.dosage, accent = accent)

        if (result.sideEffects.isNotEmpty()) {
            Text("Side Effects", style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.SemiBold)
            result.sideEffects.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = colors.onSurface) }
        }

        if (result.warnings.isNotEmpty()) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(0.08f)).border(1.dp, accent.copy(0.3f), RoundedCornerShape(12.dp)).padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, tint = accent, modifier = Modifier.size(16.dp))
                        Text("Warnings", style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
                    }
                    result.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = colors.onSurface) }
                }
            }
        }
    }
}

@Composable
private fun MedRow(
    icon:   androidx.compose.ui.graphics.vector.ImageVector,
    label:  String,
    value:  String,
    accent: Color
) {
    val colors = smartColors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = colors.subtext)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WASTE CLASSIFIER SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WasteClassifierScreen(
    onBack:    () -> Unit,
    onCapture: () -> Unit,
    viewModel: WasteClassifierViewModel = hiltViewModel()
) {
    val colors  = smartColors
    val uiState by viewModel.uiState.collectAsState()
    val accent  = SmartVisionColors.wasteClassifier

    Scaffold(
        topBar         = { SmartVisionTopBar(title = "Waste Classifier", onBack = onBack) },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.card)
                    .border(1.dp, accent.copy(0.3f), RoundedCornerShape(22.dp))
            ) {
                if (uiState.imagePath != null) {
                    AsyncImage(uiState.imagePath, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Recycling, null, tint = accent, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Point camera at waste item", style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
                    }
                }
            }

            if (uiState.imagePath == null) {
                Button(
                    onClick = onCapture,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Classify Waste", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            // ── Result: categories with progress bars ─────────────────────────
            when (val result = uiState.result) {
                is ScanResult.WasteClassifierResult -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.card)
                            .border(1.dp, accent.copy(0.2f), RoundedCornerShape(22.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Classification Results", style = MaterialTheme.typography.titleMedium, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                        // Winner badge
                        val winner = result.categories.maxByOrNull { it.confidence }
                        winner?.let {
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(Color(it.color).copy(0.12f))
                                    .border(1.dp, Color(it.color).copy(0.4f), RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = Color(it.color), modifier = Modifier.size(24.dp))
                                    Column {
                                        Text("Primary Classification", style = MaterialTheme.typography.labelLarge, color = Color(it.color))
                                        Text(it.label, style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    ConfidenceBadge(confidence = it.confidence)
                                }
                            }
                        }
                        Divider(color = colors.cardBorder)
                        result.categories.forEach { cat ->
                            WasteCategoryRow(cat = cat)
                        }
                    }

                    // Disposal tip
                    WasteDisposalTip(winner = result.categories.maxByOrNull { it.confidence }, accent = accent)
                }
                is ScanResult.Loading -> {
                    Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                }
                else -> {}
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WasteCategoryRow(cat: WasteCategory) {
    val colors   = smartColors
    val barColor = Color(cat.color)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(cat.label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, fontWeight = FontWeight.Medium)
            Text("${(cat.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = barColor, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress    = { cat.confidence },
            color       = barColor,
            trackColor  = colors.cardBorder,
            modifier    = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
        )
    }
}

@Composable
private fun WasteDisposalTip(winner: WasteCategory?, accent: Color) {
    if (winner == null) return
    val colors = smartColors
    val (tip, icon) = when {
        winner.label.contains("Recyclable", ignoreCase = true) ->
            "Place in the blue recycling bin. Rinse containers before recycling." to Icons.Rounded.Recycling
        winner.label.contains("Organic", ignoreCase = true) ->
            "Place in the green compost bin or home compost pile." to Icons.Rounded.Eco
        else ->
            "Place in the general waste bin. This item cannot be recycled." to Icons.Rounded.DeleteOutline
    }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(accent.copy(0.06f)).border(1.dp, accent.copy(0.2f), RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            Column {
                Text("Disposal Tip", style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.SemiBold)
                Text(tip, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
            }
        }
    }
}
