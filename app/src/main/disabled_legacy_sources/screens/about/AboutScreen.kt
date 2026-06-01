package com.smartvision.ai.ui.screens.about

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.smartvision.ai.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// ABOUT SCREEN — college presentation ready with architecture & features
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AboutScreen(onBack: () -> Unit) {

    Scaffold(
        containerColor      = Color(0xFF050816),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text("About Project", style = MaterialTheme.typography.titleLarge,
                    color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(48.dp))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)
            .padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Hero ──────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF121826), Color(0xFF1A0A2E))))
                .border(1.dp, Brush.linearGradient(listOf(Color(0xFF00E5FF).copy(0.5f), Color(0xFF7B61FF).copy(0.5f))), RoundedCornerShape(20.dp))
                .padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(Modifier.size(80.dp)) {
                        drawCircle(Brush.radialGradient(listOf(Color(0x407B61FF), Color.Transparent)))
                        drawCircle(Brush.radialGradient(listOf(Color(0xFF00E5FF), Color(0xFF7B61FF))), radius=size.minDimension*0.3f)
                        drawCircle(Color(0xFF050816), radius=size.minDimension*0.12f)
                    }
                    Text("SmartVision AI", style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, fontWeight = FontWeight.Bold)
                    Text("v2.0.0 — AI-Powered Vision App", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8892B0))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoChip("Kotlin", Color(0xFF7B61FF))
                        InfoChip("Jetpack Compose", Color(0xFF00E5FF))
                        InfoChip("ML Kit", Color(0xFF00C853))
                    }
                }
            }

            // ── Architecture ──────────────────────────────────────────────────
            AboutSection("Architecture") {
                listOf(
                    Triple(Icons.Rounded.Layers,   "Pattern",      "MVVM + Clean Architecture"),
                    Triple(Icons.Rounded.AccountTree,"DI",          "Hilt Dependency Injection"),
                    Triple(Icons.Rounded.Storage,  "Database",     "Room + DataStore"),
                    Triple(Icons.Rounded.Cloud,    "Cloud",        "Firebase Auth + Firestore"),
                    Triple(Icons.Rounded.CameraAlt,"Camera",       "CameraX API"),
                    Triple(Icons.Rounded.TextFields,"OCR",         "Google ML Kit Text Recognition"),
                    Triple(Icons.Rounded.Translate, "Translation", "ML Kit Translation"),
                    Triple(Icons.Rounded.Psychology,"AI Models",   "TensorFlow Lite on-device"),
                ).forEach { triple ->
                    AboutRow(triple.first, triple.second, triple.third)
                }
            }

            // ── Features ──────────────────────────────────────────────────────
            AboutSection("Core Features") {
                listOf(
                    "Real-time OCR Text Extraction",
                    "18-language Neural Translation",
                    "Live Object Detection with Bounding Boxes",
                    "Medicine Identification via TFLite",
                    "Waste Classification (5 categories)",
                    "AI Chat Assistant with Module Navigation",
                    "Voice Commands & Text-to-Speech",
                    "Offline-first Room Database",
                    "Firebase Cloud Sync",
                    "AMOLED Cyberpunk UI with Animations"
                ).forEachIndexed { i, feature ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                        Text("${i + 1}.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        Text(feature, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8892B0))
                    }
                }
            }

            // ── Tech Stack ────────────────────────────────────────────────────
            AboutSection("Technology Stack") {
                val techs = listOf(
                    "Kotlin 2.1" to Color(0xFF7B61FF),
                    "Jetpack Compose" to Color(0xFF00E5FF),
                    "Hilt" to Color(0xFF00C853),
                    "Room DB" to Color(0xFFFF6D00),
                    "Firebase" to Color(0xFFFFD600),
                    "ML Kit" to Color(0xFF00BCD4),
                    "TensorFlow Lite" to Color(0xFFFF5252),
                    "CameraX" to Color(0xFF7B61FF),
                    "Coroutines" to Color(0xFF00E5FF),
                    "Navigation Compose" to Color(0xFF00C853)
                )
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(techs.size) { i ->
                        val (name, color) = techs[i]
                        Box(modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(color.copy(0.12f))
                            .border(1.dp, color.copy(0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp)) {
                            Text(name, style = MaterialTheme.typography.labelSmall,
                                color = color, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Future Scope ──────────────────────────────────────────────────
            AboutSection("Future Scope") {
                listOf(
                    "Augmented Reality object overlays",
                    "Multi-model Gemini AI integration",
                    "Plant & food identification",
                    "Real-time sign language translation",
                    "Cloud ML model updates OTA",
                    "Wearable / smartwatch companion"
                ).forEach { scope ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(Icons.Rounded.ArrowForward, null, tint = Color(0xFF7B61FF), modifier = Modifier.size(14.dp))
                        Text(scope, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8892B0))
                    }
                }
            }

            // ── Developed By ──────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF121826))
                .border(1.dp, Color(0xFF1E2D47), RoundedCornerShape(16.dp))
                .padding(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Developed for College Mini Project", style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8892B0), letterSpacing = 1.sp)
                    Text("SmartVision AI Team", style = MaterialTheme.typography.titleMedium,
                        color = Color.White, fontWeight = FontWeight.Bold)
                    Text("© 2026 All Rights Reserved", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8892B0))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AboutSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF8892B0), letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF121826))
            .border(1.dp, Color(0xFF1E2D47), RoundedCornerShape(16.dp))
            .padding(16.dp)) {
            Column(content = content, verticalArrangement = Arrangement.spacedBy(8.dp))
        }
    }
}

@Composable
private fun AboutRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8892B0))
        }
        Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoChip(label: String, color: Color) {
    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))
        .background(color.copy(0.12f))
        .border(1.dp, color.copy(0.4f), RoundedCornerShape(12.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}
