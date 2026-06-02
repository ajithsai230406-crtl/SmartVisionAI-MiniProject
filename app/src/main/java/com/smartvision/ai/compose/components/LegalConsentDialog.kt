package com.smartvision.ai.compose.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smartvision.ai.ui.theme.NeonBlue
import com.smartvision.ai.ui.theme.NeonGreen
import com.smartvision.ai.ui.theme.NeonPink
import com.smartvision.ai.ui.theme.NeonPurple
import com.smartvision.ai.ui.theme.extended


@Composable
fun LegalConsentDialog(
    onViewPrivacy: () -> Unit,
    onViewTerms: () -> Unit,
    onConsentAccepted: () -> Unit
) {
    val ctx = LocalContext.current
    val ext = MaterialTheme.extended
    val prefs = remember { ctx.getSharedPreferences("smartvision_prefs", Context.MODE_PRIVATE) }
    var isAccepted by remember { mutableStateOf(prefs.getBoolean("legal_consent_accepted", false)) }

    if (!isAccepted) {
        Dialog(
            onDismissRequest = { /* Force acceptance, do not allow dismiss */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0C142A), Color(0xFF060B18))
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(NeonBlue.copy(0.7f), NeonPurple.copy(0.4f))),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Text(
                        "⚖️ Legal Consent & Privacy",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        "Welcome to Smart Vision AI! Before getting started, please review our core privacy-first terms:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.7f),
                        textAlign = TextAlign.Center
                    )

                    // Scrollable summary area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.04f))
                            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConsentBullet("🛡️", "Privacy First", "Your captured images are processed securely in volatile memory. No personal photo feeds are sold or compiled.")
                            ConsentBullet("🚫", "No Erotic Content", "Uploading or scanning sexually explicit, violent, or erotic image content is strictly prohibited.")
                            ConsentBullet("⚖️", "AI Accuracy Disclaimer", "AI predictions, translations, and homework solver logic are references only and may not be 100% accurate.")
                            ConsentBullet("💊", "Medical Information Disclaimer", "The Medicine Scanner compiled guidelines are purely informational. Always consult medical professionals.")
                            ConsentBullet("📚", "Academic Integrity", "The Student Helper solver supports comprehension. Users agree to respect standard academic standards.")
                        }
                    }

                    // Links to full policy pages
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            buildAnnotatedString {
                                append("Read the full ")
                                withStyle(SpanStyle(color = NeonBlue, fontWeight = FontWeight.Bold)) {
                                    append("Privacy Policy")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { onViewPrivacy() }
                        )
                        Text(
                            buildAnnotatedString {
                                append("Read full ")
                                withStyle(SpanStyle(color = NeonBlue, fontWeight = FontWeight.Bold)) {
                                    append("Terms & Conditions")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { onViewTerms() }
                        )
                    }

                    // Glowing Acceptance button
                    NeonButton(
                        text = "I Accept & Agree ✓",
                        onClick = {
                            prefs.edit().putBoolean("legal_consent_accepted", true).apply()
                            isAccepted = true
                            onConsentAccepted()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsentBullet(icon: String, title: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(icon, fontSize = 16.sp, modifier = Modifier.padding(top = 1.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f), lineHeight = 16.sp)
        }
    }
}
