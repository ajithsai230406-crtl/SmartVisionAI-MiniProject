package com.smartvision.ai.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvision.ai.compose.components.GradientBackground
import com.smartvision.ai.ui.theme.NeonBlue
import com.smartvision.ai.ui.theme.NeonCyan
import com.smartvision.ai.ui.theme.NeonGreen
import com.smartvision.ai.ui.theme.NeonPink
import com.smartvision.ai.ui.theme.NeonPurple
import com.smartvision.ai.ui.theme.extended

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val ext = MaterialTheme.extended

    GradientBackground {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ext.glassCard)
                        .border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text(
                        "🔒 Privacy Policy",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Privacy-first guidelines & disclosures", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Last Updated: June 2, 2026\nSmart Vision AI is committed to privacy-first engineering. Below are our complete data handling practices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ── 1. Privacy-First Commitment ──
                PolicySectionCard(title = "Privacy-First Commitment", icon = "🛡️", color = NeonCyan) {
                    Text(
                        "We believe privacy is a fundamental human right. Smart Vision AI is built from the ground up to operate securely. " +
                                "Most analyses run directly on your device, and any cloud interactions are encrypted end-to-end to protect your personal space.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 2. Data Collection & Processing ──
                PolicySectionCard(title = "Data Collection & Processing", icon = "📊", color = NeonBlue) {
                    Text(
                        "• Image Captures: When you capture images for OCR, Object Detection, Waste Classification, or Student Homework checks, " +
                                "the frames are processed securely in temporary volatile RAM memory.\n" +
                                "• Personal Details: We do not compile, sell, or monetize your images. Your vision feeds are exclusively your own.\n" +
                                "• Diagnostics: Anonymous technical logs may be collected only with your explicit consent.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 3. Camera & Storage Permissions ──
                PolicySectionCard(title = "Device Permissions Disclosure", icon = "📷", color = NeonPurple) {
                    Text(
                        "• Camera Permission: Required to feed the real-time viewfinders in the OCR Translator, Object Detector, Document Scanner, " +
                                "Waste Classifier, Homework Helper, and Barcode scan views. Camera frames are analyzed locally in temporary buffers and are never recorded without consent.\n" +
                                "• Storage Permission: Required to import gallery screenshots for QR, homework, and OCR extraction. Also utilized by the Document Scanner to export processed PDF/JPEG copies to public directories safely under modern Android Scoped Storage compatibility.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 4. OCR & Translation Disclosure ──
                PolicySectionCard(title = "OCR & Translation Disclosure", icon = "🌐", color = NeonGreen) {
                    Text(
                        "When using selective highlighting for text translations, Smart Vision AI utilizes local on-device ML Kit OCR engines. " +
                                "Extracted text is processed locally or translated using secure translation APIs. Your printed sheets, notes, " +
                                "and captured documents are never shared or stored externally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 5. AI Assistant & Model Disclosures ──
                PolicySectionCard(title = "AI Assistant & Generative Models", icon = "🤖", color = NeonBlue) {
                    Text(
                        "The Chat Assistant, Homework Solver, and Waste Classifier modules query Google Gemini AI models securely. " +
                                "Your inputs are processed safely in compliance with enterprise data privacy directives. " +
                                "No undesirable, illegal, or erotic image processing is supported or allowed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 6. Medicine & Health Disclaimers ──
                PolicySectionCard(title = "Medicine Scanner Disclaimer", icon = "💊", color = NeonPink) {
                    Text(
                        "Important: Medical data extracted by the Medicine Scanner is strictly informational and for reference purposes only. " +
                                "It does not constitute medical advice or prescriptions. Always verify chemical compounds, dosages, and " +
                                "safety instructions with a licensed medical practitioner or certified pharmacist before use.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 7. Educational Tutoring Disclaimer ──
                PolicySectionCard(title = "Educational Helper Disclaimer", icon = "📚", color = NeonCyan) {
                    Text(
                        "The Student Helper homework solver provides algebraic step-by-step logic, chemistry stoich properties, and computer programming checks. " +
                                "These explanations are for learning and educational utility only. Users are responsible for validating " +
                                "formula derivations and steps independently to ensure academic integrity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 8. Image Safety & Content Restrictions ──
                PolicySectionCard(title = "Content Restrictions & Safety", icon = "🚫", color = NeonPink) {
                    Text(
                        "Smart Vision AI enforces strict image safety protocols. The upload or scanning of erotic, sexually explicit, " +
                                "violent, highly illegal, or otherwise undesirable visual content is strictly prohibited. Violators will have " +
                                "their accounts suspended immediately.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                Spacer(Modifier.height(32.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
fun PolicySectionCard(
    title: String,
    icon: String,
    color: Color,
    content: @Composable () -> Unit
) {
    val ext = MaterialTheme.extended
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, color.copy(0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 18.sp)
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}
