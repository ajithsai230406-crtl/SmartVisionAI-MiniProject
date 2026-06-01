package com.smartvision.ai.ui.screens.medical

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.ui.components.SmartVisionTopBar
import com.smartvision.ai.ui.theme.SVColors
import com.smartvision.ai.ui.theme.svColors

@Composable
fun MedicalScannerScreen(
    onBack: () -> Unit,
    onCapture: () -> Unit,
    vm: MedicalScannerViewModel = hiltViewModel()
) {
    val colors = svColors
    val accent = SVColors.red
    val uiState by vm.uiState.collectAsState()
    
    val resultState = uiState.result
    val imagePath = uiState.imagePath

    Scaffold(
        topBar = {
            SmartVisionTopBar(
                title = "Medical Scanner",
                onBack = onBack
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // ================= IMAGE =================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.card)
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                if (imagePath != null) {
                    AsyncImage(
                        model = imagePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MedicalServices,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scan a medicine strip",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.subtext
                        )
                    }
                }
            }

            // ================= BUTTON =================
            if (imagePath == null) {
                Button(
                    onClick = onCapture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan Medicine",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // ================= RESULT =================
            when (resultState) {
                is ScanResult.MedicalScanResult -> {
                    val result = resultState as ScanResult.MedicalScanResult
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.card)
                            .border(
                                width = 1.dp,
                                color = accent.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = result.medicineName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(color = colors.border)
                        MedRow(
                            icon = Icons.Rounded.Info,
                            label = "Usage",
                            value = result.usage,
                            accent = accent,
                            colors = colors
                        )
                        MedRow(
                            icon = Icons.Rounded.Medication,
                            label = "Dosage",
                            value = result.dosage,
                            accent = accent,
                            colors = colors
                        )

                        if (result.sideEffects.isNotEmpty()) {
                            Text(
                                text = "Side Effects",
                                style = MaterialTheme.typography.titleSmall,
                                color = accent,
                                fontWeight = FontWeight.SemiBold
                            )
                            result.sideEffects.forEach {
                                Text(
                                    text = "• $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurface
                                )
                            }
                        }

                        if (result.warnings.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accent.copy(alpha = 0.07f))
                                    .border(
                                        width = 1.dp,
                                        color = accent.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Warning,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Warnings",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = accent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    result.warnings.forEach {
                                        Text(
                                            text = "• $it",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is ScanResult.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accent)
                    }
                }

                is ScanResult.Error -> {
                    val error = resultState as ScanResult.Error
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.error.copy(alpha = 0.09f))
                            .border(
                                width = 1.dp,
                                color = colors.error.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = error.message,
                            color = colors.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                else -> {}
            }

            // ================= DISCLAIMER =================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SVColors.amber.copy(alpha = 0.07f))
                    .border(
                        width = 1.dp,
                        color = SVColors.amber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = SVColors.amber,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "General info only. Always consult a healthcare professional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.subtext
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MedRow(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    colors: com.smartvision.ai.ui.theme.SVColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(17.dp).padding(top = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.subtext
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
        }
    }
}
