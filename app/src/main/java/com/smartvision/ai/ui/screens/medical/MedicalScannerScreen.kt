package com.smartvision.ai.ui.screens.medical

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
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.ui.components.SmartVisionTopBar
import com.smartvision.ai.ui.theme.*

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
                        model              = uiState.imagePath,
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                } else {
                    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.MedicalServices, null, tint = accent, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Scan a medicine strip", style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
                    }
                }
            }

            if (uiState.imagePath == null) {
                Button(
                    onClick  = onCapture,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Medicine", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            when (val result = uiState.result) {
                is ScanResult.MedicalScanResult -> MedicalResultCard(result = result, accent = accent)
                is ScanResult.Loading           -> Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
                is ScanResult.Error             -> ErrorBox(message = result.message, accent = colors.error)
                else                            -> {}
            }

            // Disclaimer
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(SmartVisionColors.translator.copy(0.07f))
                    .border(1.dp, SmartVisionColors.translator.copy(0.2f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Warning, null, tint = SmartVisionColors.translator, modifier = Modifier.size(18.dp))
                    Text(
                        "General info only. Always consult a healthcare professional.",
                        style = MaterialTheme.typography.bodySmall, color = colors.subtext
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MedicalResultCard(result: ScanResult.MedicalScanResult, accent: Color) {
    val colors = smartColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card)
            .border(1.dp, accent.copy(0.25f), RoundedCornerShape(22.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(result.medicineName, style = MaterialTheme.typography.headlineMedium, color = colors.onSurface, fontWeight = FontWeight.Bold)
        Divider(color = colors.cardBorder)
        MedRow(Icons.Rounded.Info,       "Usage",  result.usage,  accent)
        MedRow(Icons.Rounded.Medication, "Dosage", result.dosage, accent)
        if (result.sideEffects.isNotEmpty()) {
            Text("Side Effects", style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.SemiBold)
            result.sideEffects.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = colors.onSurface) }
        }
        if (result.warnings.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
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
private fun MedRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, accent: Color) {
    val colors = smartColors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = colors.subtext)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        }
    }
}

@Composable
internal fun ErrorBox(message: String, accent: Color) {
    val colors = smartColors
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(colors.error.copy(0.08f)).border(1.dp, colors.error.copy(0.3f), RoundedCornerShape(14.dp)).padding(14.dp)
    ) {
        Text(message, color = colors.error, style = MaterialTheme.typography.bodySmall)
    }
}
