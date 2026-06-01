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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartvisionai.ui.theme.*

data class ModuleDetail(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconText: String,
    val iconColor: Color,
    val accentColor: Color,
    val howItWorks: List<String>,
    val capabilities: List<String>,
    val techStack: List<String>
)

val moduleDetails = mapOf(
    "object" to ModuleDetail(
        "object", "Object Detection", "Detect & identify everyday objects in real-time",
        "👁", CyanAccent, CyanAccent,
        listOf("Camera captures frame", "Image preprocessed for ML",
            "Model predicts object class", "Label + confidence displayed"),
        listOf("Detect 80+ object categories", "Real-time confidence scores",
            "Bounding box overlay", "Works offline"),
        listOf("TensorFlow Lite", "CameraX API", "Kotlin")
    ),
    "ocr" to ModuleDetail(
        "ocr", "Text Scanner (OCR)", "Extract text from any surface instantly",
        "T", PurpleAccent, PurpleAccent,
        listOf("Camera captures document/screen", "ML Kit detects text regions",
            "Characters recognized accurately", "Text exported or copied"),
        listOf("Multi-language text recognition", "Handwriting support",
            "Copy / Share extracted text", "Works offline"),
        listOf("ML Kit Text Recognition", "CameraX API", "Kotlin")
    ),
    "translate" to ModuleDetail(
        "translate", "Translator", "Real-time scan & translate to any language",
        "文A", OrangeAccent, OrangeAccent,
        listOf("OCR extracts source text", "Language auto-detected",
            "Translation API called", "Result shown on screen"),
        listOf("50+ languages supported", "Auto language detection",
            "Offline language packs", "Voice output"),
        listOf("ML Kit Translate", "Google Translate API", "Kotlin")
    ),
    "student" to ModuleDetail(
        "student", "Student Helper", "Scan questions — get instant AI explanations",
        "🎓", Color(0xFF2196F3), Color(0xFF2196F3),
        listOf("Scan question or formula", "Text extracted via OCR",
            "Gemini AI analyzes content", "Step-by-step answer displayed"),
        listOf("Maths & Science problems", "Step-by-step solutions",
            "Voice explanations", "Multi-language support"),
        listOf("Gemini API", "ML Kit OCR", "Kotlin")
    ),
    "medical" to ModuleDetail(
        "medical", "Medical Scanner", "Identify medicines and get usage info",
        "💊", RedAccent, RedAccent,
        listOf("Scan medicine strip or pack", "Text & logo recognized",
            "Database lookup performed", "Uses & precautions shown"),
        listOf("Medicine name recognition", "Dosage information",
            "Side effects summary", "Safe general advice"),
        listOf("Gemini API", "ML Kit OCR", "Custom Dataset")
    ),
    "waste" to ModuleDetail(
        "waste", "Waste Classifier", "Identify and sort waste for recycling",
        "♻", GreenAccent, GreenAccent,
        listOf("Camera captures waste item", "TFLite model classifies type",
            "Category identified (plastic/metal/organic)", "Disposal method suggested"),
        listOf("3 waste categories", "Disposal instructions",
            "Works fully offline", "Confidence scoring"),
        listOf("TensorFlow Lite", "CameraX API", "Kotlin")
    )
)

@Composable
fun ModuleDetailScreen(moduleId: String, onBack: () -> Unit, onStartScan: () -> Unit) {
    val detail = moduleDetails[moduleId] ?: moduleDetails["object"]!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Back button
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clickable { onBack() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                tint = TextSecondary, modifier = Modifier.size(20.dp))
            Text("Back", fontSize = 14.sp, color = TextSecondary)
        }

        // Module header
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(detail.accentColor.copy(0.15f))
                    .border(1.dp, detail.accentColor.copy(0.4f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (detail.iconText == "T") {
                    Text(detail.iconText, color = detail.iconColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(detail.iconText, fontSize = 22.sp)
                }
            }
            Column {
                Text(detail.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(detail.subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }

        HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))

        // How it works
        DetailSection(title = "HOW IT WORKS") {
            detail.howItWorks.forEachIndexed { idx, step ->
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(detail.accentColor.copy(0.2f))
                            .border(1.dp, detail.accentColor.copy(0.5f), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${idx + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = detail.accentColor)
                    }
                    Text(step, fontSize = 14.sp, color = TextPrimary)
                }
                if (idx < detail.howItWorks.lastIndex)
                    HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 58.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        // Capabilities
        DetailSection(title = "CAPABILITIES") {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                detail.capabilities.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        pair.forEach { cap ->
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, null,
                                    tint = detail.accentColor, modifier = Modifier.size(14.dp))
                                Text(cap, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Tech Stack
        DetailSection(title = "TECH STACK") {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                detail.techStack.forEach { tech ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Text(tech, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // START SCANNING CTA
        Button(
            onClick = onStartScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(CyanAccent, CyanDark)),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null,
                        tint = Color(0xFF001A16), modifier = Modifier.size(20.dp))
                    Text("START SCANNING", fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFF001A16),
                        letterSpacing = 1.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Text(title, fontSize = 10.sp, color = TextMuted, letterSpacing = 3.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
            content = content
        )
    }
}
