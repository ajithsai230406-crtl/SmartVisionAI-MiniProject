package com.smartvision.ai.ui.screens.medical

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
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.ui.components.SmartVisionTopBar
import com.smartvision.ai.ui.theme.*

@Composable
fun MedicalScannerScreen(onBack: () -> Unit, onCapture: () -> Unit, vm: MedicalScannerViewModel = hiltViewModel()) {
    val c = svColors; val accent = SVColors.red
    val uiState by vm.uiState.collectAsState()
    val result = uiState.result
    val path = uiState.imagePath

    Scaffold(topBar = { SmartVisionTopBar("Medical Scanner", onBack = onBack) }, containerColor = c.background) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(2.dp))
            // Image
            Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp))
                .background(c.card).border(1.dp, accent.copy(.3f), RoundedCornerShape(20.dp))) {
                if (path != null) AsyncImage(path, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.MedicalServices, null, tint = accent, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Scan a medicine strip", style = MaterialTheme.typography.bodyMedium, color = c.subtext)
                }
            }
            if (path == null) Button(onClick = onCapture, modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp)); Text("Scan Medicine", fontWeight = FontWeight.Bold, color = Color.White)
            }
            // Result
            when (result) {
                is ScanResult.MedicalScanResult -> {
                    val r = result as ScanResult.MedicalScanResult
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card)
                        .border(1.dp, accent.copy(.22f), RoundedCornerShape(20.dp)).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(r.medicineName, style = MaterialTheme.typography.headlineMedium, color = c.onSurface, fontWeight = FontWeight.Bold)
                        Divider(color = c.border)
                        MedRow(Icons.Rounded.Info, "Usage", r.usage, accent, c)
                        MedRow(Icons.Rounded.Medication, "Dosage", r.dosage, accent, c)
                        if (r.sideEffects.isNotEmpty()) { Text("Side Effects", style = MaterialTheme.typography.titleSmall, color = accent, fontWeight = FontWeight.SemiBold); r.sideEffects.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = c.onSurface) } }
                        if (r.warnings.isNotEmpty()) Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(.07f)).border(1.dp, accent.copy(.3f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Warning, null, tint = accent, modifier = Modifier.size(15.dp))
                                    Text("Warnings", style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
                                }
                                r.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = c.onSurface) }
                            }
                        }
                    }
                }
                is ScanResult.Loading -> Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) { CircularProgressIndicator(color = accent) }
                is ScanResult.Error   -> Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.error.copy(.09f)).border(1.dp, c.error.copy(.3f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text((result as ScanResult.Error).message, color = c.error, style = MaterialTheme.typography.bodySmall) }
                else -> {}
            }
            // Disclaimer
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SVColors.amber.copy(.07f))
                .border(1.dp, SVColors.amber.copy(.2f), RoundedCornerShape(14.dp)).padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Rounded.Warning, null, tint = SVColors.amber, modifier = Modifier.size(17.dp))
                    Text("General info only. Always consult a healthcare professional.", style = MaterialTheme.typography.bodySmall, color = c.subtext)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MedRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, accent: Color, c: SVColorScheme) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp).padding(top = 2.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = c.subtext)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
        }
    }
}
