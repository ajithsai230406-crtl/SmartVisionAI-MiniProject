package com.smartvision.ai.career.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.career.presentation.CareerViewModel
import com.smartvision.ai.career.domain.AtsAnalysis
import com.smartvision.ai.compose.components.GradientBackground
import com.smartvision.ai.compose.components.SmartTopBar
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ResumeAnalyzerScreen(
    onBack: () -> Unit,
    viewModel: CareerViewModel = hiltViewModel()
) {
    val ext = MaterialTheme.extended
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val atsResult by viewModel.atsResult.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    
    var fileName by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            fileName = it.path?.substringAfterLast("/") ?: "resume.pdf"
            val mimeType = context.contentResolver.getType(it) ?: ""
            if (mimeType.contains("pdf")) {
                viewModel.extractTextFromPdf(context, it) { text ->
                    if (text.isNotBlank()) {
                        viewModel.analyzeResume(text)
                    } else {
                        android.widget.Toast.makeText(context, "No text found in PDF.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                scope.launch {
                    val result = viewModel.ocrRecognitionRepository.recognizeFromUri(context, it).getOrNull()
                    val text = result?.text ?: ""
                    if (text.isNotBlank()) {
                        viewModel.analyzeResume(text)
                    } else {
                        android.widget.Toast.makeText(context, "No text found in image.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearContext()
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(
                title = "📊 AI Resume Analyzer",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Upload Card ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(ext.glassCard)
                        .border(1.5.dp, NeonBlue.copy(0.3f), RoundedCornerShape(20.dp))
                        .clickable { filePicker.launch("*/*") }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📤", fontSize = 42.sp)
                        Text(
                            text = if (fileName.isNotBlank()) fileName else "Select Resume (PDF or Image)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Supports PDF documents, gallery photos, and scans",
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.textHint,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (isAnalyzing) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = NeonBlue)
                        Text("Extracting resume & scoring ATS...", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }

                atsResult?.let { result ->
                    // Score Gauge
                    AtsScoreGauge(score = result.score)

                    // Detail Feedback
                    AtsDetailedFeedback(result = result, ext = ext)
                }
            }
        }
    }
}

@Composable
private fun AtsScoreGauge(score: Int) {
    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        val sweepAngle = (score / 100f) * 360f
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Track
            drawArc(
                color = Color.White.copy(0.08f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            // Color sweep
            drawArc(
                brush = Brush.sweepGradient(listOf(NeonGreen, NeonBlue)),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score%",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonGreen
            )
            Text(
                text = "ATS Match",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.6f)
            )
        }
    }
}

@Composable
private fun AtsDetailedFeedback(result: AtsAnalysis, ext: ExtendedColors) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Strengths & Weaknesses
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoBlock("Strengths ✅", result.strengths, NeonGreen, Modifier.weight(1f), ext)
            InfoBlock("Weaknesses ❌", result.weaknesses, NeonPink, Modifier.weight(1f), ext)
        }

        // Keywords Optimization
        if (result.keywordOptimization.isNotEmpty()) {
            DetailCard("Recommended Keywords 🔑", NeonYellow, ext) {
                FlowRowLayout(items = result.keywordOptimization, color = NeonYellow)
            }
        }

        // Missing Sections
        if (result.missingSections.isNotEmpty()) {
            DetailCard("Missing Sections 📁", NeonOrange, ext) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    result.missingSections.forEach { section ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", color = NeonOrange)
                            Text(section, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }
            }
        }

        // Grammar & Formatting
        if (result.grammarIssues.isNotEmpty() || result.formattingIssues.isNotEmpty()) {
            DetailCard("Formatting & Grammar Check ✍️", NeonPurple, ext) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (result.grammarIssues + result.formattingIssues).forEach { issue ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⚠", color = NeonPurple)
                            Text(issue, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.9f))
                        }
                    }
                }
            }
        }

        // Summary suggestion
        if (result.rewrittenSuggestions.isNotBlank()) {
            DetailCard("Summary Optimization Rewrite ✨", NeonBlue, ext) {
                Text(
                    text = result.rewrittenSuggestions,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.9f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun InfoBlock(
    title: String,
    items: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    ext: ExtendedColors
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items.take(4).forEach { item ->
                Text("• $item", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
            }
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    accent: Color,
    ext: ExtendedColors,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun FlowRowLayout(items: List<String>, color: Color) {
    // Basic comma separated flow display
    Text(
        text = items.joinToString(" • "),
        color = color,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold
    )
}
