package com.smartvision.ai.career.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.career.presentation.CareerViewModel
import com.smartvision.ai.compose.components.GradientBackground
import com.smartvision.ai.compose.components.SmartTopBar
import com.smartvision.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverLetterScreen(
    onBack: () -> Unit,
    viewModel: CareerViewModel = hiltViewModel()
) {
    val ext = MaterialTheme.extended
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val role by viewModel.letterRole.collectAsStateWithLifecycle()
    val company by viewModel.letterCompany.collectAsStateWithLifecycle()
    val experience by viewModel.letterExperience.collectAsStateWithLifecycle()
    val style by viewModel.letterStyle.collectAsStateWithLifecycle()
    val resumeText by viewModel.letterResumeText.collectAsStateWithLifecycle()
    
    val generatedLetter by viewModel.generatedLetter.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isLetterGenerating.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.setScreenContext(mode = "COVER_LETTER", letterText = generatedLetter)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearContext()
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(
                title = "✉️ Cover Letter Generator",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Forms
                StyledField(value = role, label = "Target Job Role (e.g. Android Engineer)", onValueChange = { viewModel.letterRole.value = it })
                StyledField(value = company, label = "Company Name (e.g. Google)", onValueChange = { viewModel.letterCompany.value = it })
                StyledField(value = experience, label = "Experience Level (e.g. 3 years)", onValueChange = { viewModel.letterExperience.value = it })
                
                // Style Selector
                Text("Cover Letter Style", style = MaterialTheme.typography.labelSmall, color = ext.textHint)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val styles = listOf("PROFESSIONAL", "MODERN", "FORMAL")
                    styles.forEach { s ->
                        val isSel = style == s
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) NeonPurple.copy(0.18f) else ext.glassCard)
                                .border(1.dp, if (isSel) NeonPurple else ext.glassBorder, RoundedCornerShape(10.dp))
                                .clickable { viewModel.letterStyle.value = s }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = s.lowercase().replaceFirstChar { it.uppercase() },
                                color = if (isSel) Color.White else ext.textHint,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Optional Resume text input
                StyledField(
                    value = resumeText,
                    label = "Candidate Skills Summary (Optional, feeds details to letter)",
                    singleLine = false,
                    maxLines = 4,
                    onValueChange = { viewModel.letterResumeText.value = it }
                )

                // Generate Button
                Button(
                    onClick = { viewModel.generateLetter() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Generate Cover Letter ✨", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (generatedLetter.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Generated Letter",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    // Output Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ext.glassCard)
                            .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = generatedLetter,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(0.9f),
                                lineHeight = 18.sp
                            )

                            Divider(color = Color.White.copy(0.08f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(generatedLetter))
                                        android.widget.Toast.makeText(context, "Copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.06f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Copy 📋", color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        val uri = viewModel.exportCoverLetterToPdf(context, generatedLetter, "CoverLetter_${company.replace(" ", "_")}")
                                        if (uri != null) {
                                            android.widget.Toast.makeText(context, "PDF saved to Downloads!", android.widget.Toast.LENGTH_LONG).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Failed to save PDF.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Export PDF 📄", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
