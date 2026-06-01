package com.smartvision.ai.compose.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.presentation.vision.VisionViewModel
import com.smartvision.ai.ui.theme.*

// ─── Medicine Scanner ─────────────────────────────────────────────────────────
@Composable
fun MedicineScannerScreen(
    onBack: () -> Unit,
    viewModel: VisionViewModel = hiltViewModel()
) {
    val ext     = MaterialTheme.extended
    val result  by viewModel.medicine.collectAsStateWithLifecycle()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(title = "Medicine Scanner", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Camera preview placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DeepNavy.copy(alpha = 0.8f))
                        .border(1.dp, ext.glassBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💊", fontSize = 56.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Point camera at medicine", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))

                NeonButton(
                    text    = "Analyze Medicine",
                    onClick = { viewModel.analyzeMedicine() },
                    modifier = Modifier.fillMaxWidth()
                )

                result?.let { r ->
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ext.glassCard)
                            .border(1.dp, AccentMedicine.copy(0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(r.label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                NeonBadge("${(r.confidence * 100).toInt()}%", AccentMedicine)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(r.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonPink.copy(0.1f))
                                    .border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚠️ ", fontSize = 16.sp)
                                    Text(r.tip, style = MaterialTheme.typography.bodySmall, color = NeonPink)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Waste Classifier ────────────────────────────────────────────────────────
@Composable
fun WasteClassifierScreen(
    onBack: () -> Unit,
    viewModel: VisionViewModel = hiltViewModel()
) {
    val ext    = MaterialTheme.extended
    val result by viewModel.waste.collectAsStateWithLifecycle()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(title = "Waste Classifier", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Camera preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DeepNavy.copy(0.8f))
                        .border(1.dp, ext.glassBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("♻️", fontSize = 56.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Point camera at waste item", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))

                NeonButton(
                    text    = "Classify Waste",
                    onClick = { viewModel.classifyWaste() },
                    modifier = Modifier.fillMaxWidth()
                )

                result?.let { r ->
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ext.glassCard)
                            .border(1.dp, AccentWaste.copy(0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(r.label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                NeonBadge("${(r.confidence * 100).toInt()}%", AccentWaste)
                            }

                            Spacer(Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NeonBadge("♻ Recyclable (Plastic)", AccentWaste)
                            }

                            Spacer(Modifier.height(10.dp))

                            InfoRow("Can be Reused", "✅")
                            InfoRow("Dispose", "Clean and recycle")
                            InfoRow("Environmental Impact", "Low if recycled properly")

                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentWaste.copy(0.1f))
                                    .padding(10.dp)
                            ) {
                                Text(r.tip, style = MaterialTheme.typography.bodySmall, color = AccentWaste)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val ext = MaterialTheme.extended
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
    }
}

// ─── Student Helper ───────────────────────────────────────────────────────────
@Composable
fun StudentHelperScreen(onBack: () -> Unit) {
    val ext = MaterialTheme.extended
    var questionText by remember { mutableStateOf("∫(x² - 3x + 2) dx") }
    var showResult   by remember { mutableStateOf(false) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(title = "Student Helper", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text("Question", style = MaterialTheme.typography.labelMedium, color = ext.neonBlue)
                Spacer(Modifier.height(8.dp))

                // Question display box (shows scanned formula/text)
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ext.glassCard)
                        .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        questionText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton("Ask Gemini AI", onClick = { showResult = true }, modifier = Modifier.weight(1f))
                    OutlineNeonButton("📷 Scan", onClick = {}, modifier = Modifier.weight(1f))
                }

                if (showResult) {
                    Spacer(Modifier.height(20.dp))
                    Text("Solution", style = MaterialTheme.typography.labelMedium, color = ext.neonPurple)
                    Spacer(Modifier.height(8.dp))
                    Text("Step-by-step Explanation", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))

                    val steps = listOf(
                        "∫(x² - 3x + 2) dx",
                        "= ∫x² dx - ∫3x dx + ∫2 dx",
                        "= x³/3 - 3x²/2 + 2x + C"
                    )
                    steps.forEachIndexed { i, step ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape)
                                    .background(ext.neonPurple.copy(0.2f)),
                                contentAlignment = Alignment.Center
                            ) { Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = ext.neonPurple) }
                            Spacer(Modifier.width(10.dp))
                            Text(step, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlineNeonButton("Show More Steps", onClick = {}, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
