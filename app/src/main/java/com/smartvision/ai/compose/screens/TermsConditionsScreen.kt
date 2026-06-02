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
fun TermsConditionsScreen(onBack: () -> Unit) {
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
                        "📄 Terms & Conditions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text("User agreements & obligations", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
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
                    "Last Updated: June 2, 2026\nBy using the Smart Vision AI mobile application, you agree to comply with and be bound by the following Terms & Conditions. Please read them carefully.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ── 1. Acceptable Use ──
                PolicySectionCard(title = "User Responsibilities & Acceptable Use", icon = "✅", color = NeonCyan) {
                    Text(
                        "You agree to use Smart Vision AI strictly for lawful purposes. You must not use the camera viewfinders or image analyzers " +
                                "to scan illegal items, verify unauthorized credentials, or create security compromises. " +
                                "Users are solely responsible for actions performed using our AI scanning and tutoring features.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 2. Content Restrictions ──
                PolicySectionCard(title = "Content Restrictions & Image Safety", icon = "🚫", color = NeonPink) {
                    Text(
                        "We enforce a zero-tolerance policy against inappropriate usage. You are strictly prohibited from processing, " +
                                "uploading, or scanning any undesirable, sexually explicit, violent, abusive, or erotic images. " +
                                "Attempts to bypass safety guardrails to evaluate hazardous content will result in permanent account termination.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 3. AI Accuracy Caveats & Disclaimer ──
                PolicySectionCard(title = "AI Model Outputs & Accuracy", icon = "⚖️", color = NeonPurple) {
                    Text(
                        "Smart Vision AI implements dynamic Generative AI and deep visual models. You acknowledge that AI outputs " +
                                "and predictions may not always be 100% accurate, reliable, or fully correct. " +
                                "All OCR translations, homework solves, object identifications, and waste classifications " +
                                "should be treated as supportive references rather than definitive truths.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 4. Medicine Scanner Disclaimer ──
                PolicySectionCard(title = "Medicine Scan Terms & Safety", icon = "💊", color = NeonPink) {
                    Text(
                        "The Medicine Scanner compiles details for educational and reference purposes only. " +
                                "Smart Vision AI does not provide professional medical guidance, diagnoses, or prescriptions. " +
                                "Users must consult clinical healthcare professionals or refer directly to printed packaging guidelines " +
                                "before using, purchasing, or dosing any medication.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 5. Homework & Educational Use ──
                PolicySectionCard(title = "Academic Use & Student Integrity", icon = "🎓", color = NeonCyan) {
                    Text(
                        "The Student Helper homework checks provide learning formulas, equations, and space complexities. " +
                                "These tutorials are provided to support homework comprehension and tutoring. " +
                                "Users agree to follow academic integrity guidelines enforced by their educational institutions " +
                                "when utilizing our student tools.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 6. Storage & Data Handling ──
                PolicySectionCard(title = "Storage & Scoped File Handling", icon = "💾", color = NeonGreen) {
                    Text(
                        "Smart Vision AI processes images temporarily in RAM to run live ML analyses. If you choose to use the " +
                                "Document Scanner save features, files are stored on-device inside modern Android Scoped Storage directories. " +
                                "We are not responsible for files deleted by device cache clearing utilities or user resets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 7. Account Management ──
                PolicySectionCard(title = "Account Terms & Management", icon = "👤", color = NeonBlue) {
                    Text(
                        "We reserve the right to suspend or terminate accounts that violate our safety policies, " +
                                "spam authentication routes, or abuse our Gemini AI API endpoints. " +
                                "Users may terminate their account and request deletion of stored diagnostic markers at any time " +
                                "by contacting our support team.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        lineHeight = 20.sp
                    )
                }

                // ── 8. Liabilities & Indemnity ──
                PolicySectionCard(title = "Limitation of Liability", icon = "⚖️", color = NeonBlue) {
                    Text(
                        "Under no circumstances shall Smart Vision AI or its creators be held liable for any direct, indirect, " +
                                "incidental, or consequential damages resulting from incorrect AI predictions, " +
                                "inaccurate OCR extractions, medicine scanning oversights, or lost documents. Use of the app is " +
                                "strictly at your own risk.",
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
