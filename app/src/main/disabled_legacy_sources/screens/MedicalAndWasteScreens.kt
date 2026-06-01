package com.smartvision.ai.ui.screens.medical

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.smartvision.ai.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// MEDICINE IDENTIFIER SCREEN — matches image screen 9
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MedicalScannerScreen(
    onBack:    () -> Unit,
    onCapture: () -> Unit = {}
) {
    val colors = svColors
    var showResult by remember { mutableStateOf(true) }

    Scaffold(
        containerColor      = Color(0xFF050816),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text("Medicine Identifier", style = MaterialTheme.typography.titleLarge,
                    color = Color.White, fontWeight = FontWeight.Bold)
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.Settings, null, tint = Color(0xFF8892B0), modifier = Modifier.size(20.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Medicine image placeholder ─────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0A0F1E))
                .border(1.dp, Color(0xFFFF1744).copy(0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Brush.radialGradient(listOf(Color(0x20FF1744), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.5f), radius = 200f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.MedicalServices, null, tint = Color(0xFFFF1744),
                        modifier = Modifier.size(64.dp))
                    Text("PARACETAMOL\n500 mg", style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("Scan or upload medicine to identify",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF8892B0))
                }
            }

            // ── Scan / Upload buttons ─────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCapture, modifier = Modifier.weight(1f).height(46.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121826))) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = Color(0xFFFF1744), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan", color = Color.White)
                }
                Button(onClick = {}, modifier = Modifier.weight(1f).height(46.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121826))) {
                    Icon(Icons.Rounded.PhotoLibrary, null, tint = Color(0xFFFF1744), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Upload", color = Color.White)
                }
            }

            // ── Result card ────────────────────────────────────────────────────
            if (showResult) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF121826))
                    .border(1.dp, Color(0xFFFF1744).copy(0.3f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Medicine name
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp)
                            .background(Color(0xFFFF1744).copy(0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFFF1744).copy(0.3f), RoundedCornerShape(12.dp)),
                            Alignment.Center) {
                            Icon(Icons.Rounded.MedicalServices, null, tint = Color(0xFFFF1744), modifier = Modifier.size(26.dp))
                        }
                        Column {
                            Text("Paracetamol 500 mg", style = MaterialTheme.typography.titleLarge,
                                color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Pain Reliever / Fever Reducer", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8892B0))
                        }
                    }

                    MedDivider()

                    // Uses
                    MedSection("Uses", listOf("• Relieves pain", "• Reduces fever", "• Headache & body ache relief"))

                    MedDivider()

                    // Dosage
                    MedSection("Dosage", listOf("1–2 tablets every 6 hours", "Maximum 8 tablets per day", "Take with water"))

                    MedDivider()

                    // Warnings
                    MedSection("Warnings", listOf(
                        "• Do not exceed the dose",
                        "• Consult doctor if symptoms persist",
                        "• Avoid alcohol while taking this"
                    ), color = Color(0xFFFFAB00))

                    MedDivider()

                    // Side effects
                    MedSection("Side Effects", listOf("Nausea", "Skin rash", "Liver damage (overdose)"), color = Color(0xFFFF5252))

                    // See more button
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))) {
                        Text("See More Details", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MedDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp)
        .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF1E2D47), Color.Transparent))))
}

@Composable
private fun MedSection(title: String, items: List<String>, color: Color = Color(0xFFE8EAF6)) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF8892B0), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        items.forEach { item ->
            Text(item, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WASTE CLASSIFIER SCREEN — matches image screen 10
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WasteClassifierScreen(
    onBack:    () -> Unit,
    onCapture: () -> Unit = {}
) {
    var showResult by remember { mutableStateOf(true) }
    val inf        = rememberInfiniteTransition(label = "waste")
    val pulse      by inf.animateFloat(0.95f, 1.05f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse), "wp")

    Scaffold(
        containerColor      = Color(0xFF050816),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text("Waste Classifier", style = MaterialTheme.typography.titleLarge,
                    color = Color.White, fontWeight = FontWeight.Bold)
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.Settings, null, tint = Color(0xFF8892B0), modifier = Modifier.size(20.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Camera view placeholder ────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0A0A14))
                .border(1.dp, Color(0xFF00C853).copy(0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Brush.radialGradient(listOf(Color(0x2000C853), Color.Transparent),
                        center=Offset(size.width*0.5f, size.height*0.5f), radius=240f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Recycling, null, tint = Color(0xFF00C853).copy(0.6f),
                        modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Point camera at waste item", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8892B0))
                }
            }

            // ── Capture button ────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCapture, modifier = Modifier.weight(1f).height(46.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853).copy(0.15f))) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = Color(0xFF00C853), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Capture", color = Color(0xFF00C853))
                }
                Button(onClick = {}, modifier = Modifier.weight(1f).height(46.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121826))) {
                    Icon(Icons.Rounded.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Upload", color = Color.White)
                }
            }

            // ── Classification result card ─────────────────────────────────────
            if (showResult) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF121826))
                    .border(1.dp, Color(0xFF00C853).copy(0.3f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    Text("Classification Result", style = MaterialTheme.typography.titleMedium,
                        color = Color.White, fontWeight = FontWeight.Bold)

                    // Main result
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(60.dp).scale(pulse)
                            .background(Color(0xFF00C853).copy(0.12f), CircleShape)
                            .border(2.dp, Color(0xFF00C853), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Recycling, null, tint = Color(0xFF00C853), modifier = Modifier.size(30.dp))
                        }
                        Column {
                            Text("Plastic (Recyclable)", style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                            Text("Confidence: 92%", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8892B0))
                        }
                    }

                    // Confidence bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Confidence", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8892B0))
                            Text("92%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                            .background(Color(0xFF1E2D47))) {
                            Box(modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight().clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(Color(0xFF00C853), Color(0xFF00E676)))))
                        }
                    }

                    MedDivider()

                    // Suggestions
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Suggestions", style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF8892B0), fontWeight = FontWeight.SemiBold)
                        listOf("• Clean the waste before disposal", "• Recycle for a better environment",
                            "• Check local recycling guidelines").forEach {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE8EAF6))
                        }
                    }

                    // Learn more
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))) {
                        Text("Learn More", color = Color(0xFF050816), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
