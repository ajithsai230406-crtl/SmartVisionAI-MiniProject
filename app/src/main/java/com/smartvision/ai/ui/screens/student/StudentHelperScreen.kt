package com.smartvision.ai.ui.screens.student

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.ui.components.SmartVisionTopBar
import com.smartvision.ai.ui.components.ConfidenceBadge
import com.smartvision.ai.ui.theme.*

@Composable
fun StudentHelperScreen(
    onBack:    () -> Unit,
    viewModel: StudentHelperViewModel = hiltViewModel()
) {
    val colors  = smartColors
    val uiState by viewModel.uiState.collectAsState()
    val accent  = SmartVisionColors.studentHelper

    Scaffold(
        topBar         = { SmartVisionTopBar(title = "Student Helper", onBack = onBack) },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Ask a question manually ───────────────────────────────────────
            ManualInputCard(
                query      = uiState.manualQuery,
                onQueryChange = viewModel::setManualQuery,
                onAsk      = viewModel::askQuestion,
                isLoading  = uiState.isLoading,
                accent     = accent
            )

            // ── Result ────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.result != null,
                enter   = fadeIn() + expandVertically(expandFrom = Alignment.Top)
            ) {
                uiState.result?.let { result ->
                    when (result) {
                        is ScanResult.StudentHelperResult -> StudentResultCard(result, accent)
                        is ScanResult.Error               -> ErrorCard(result.message, accent)
                        else                              -> {}
                    }
                }
            }

            // ── Quick prompts ─────────────────────────────────────────────────
            if (uiState.result == null && !uiState.isLoading) {
                QuickPromptSection(onPromptClick = viewModel::askFromPrompt, accent = accent)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ManualInputCard(
    query:         String,
    onQueryChange: (String) -> Unit,
    onAsk:         () -> Unit,
    isLoading:     Boolean,
    accent:        Color
) {
    val colors = smartColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card)
            .border(1.dp, accent.copy(0.25f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(accent.copy(0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, accent.copy(0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.School, null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Text(
                    "Ask Gemini AI",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedTextField(
                value         = query,
                onValueChange = onQueryChange,
                placeholder   = { Text("Type a question, formula, or problem…", color = colors.subtext) },
                modifier      = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                shape         = RoundedCornerShape(14.dp),
                maxLines      = 5,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor     = accent,
                    unfocusedBorderColor   = colors.cardBorder,
                    focusedTextColor       = colors.onSurface,
                    unfocusedTextColor     = colors.onSurface,
                    cursorColor            = accent,
                    focusedContainerColor  = colors.surface,
                    unfocusedContainerColor = colors.surface
                )
            )

            Button(
                onClick  = onAsk,
                enabled  = query.isNotEmpty() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Thinking…", fontWeight = FontWeight.SemiBold, color = Color.White)
                } else {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Get Explanation", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StudentResultCard(result: ScanResult.StudentHelperResult, accent: Color) {
    val colors = smartColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(accent.copy(0.05f))
            .border(1.dp, accent.copy(0.2f), RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(18.dp))
            Text("AI Explanation", style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.SemiBold)
        }

        if (result.question.isNotEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(colors.card, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(result.question, style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
            }
        }

        Text(result.aiExplanation, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, lineHeight = 22.sp)

        if (result.steps.isNotEmpty()) {
            Divider(color = accent.copy(0.15f))
            Text("Step-by-step", style = MaterialTheme.typography.titleSmall, color = accent, fontWeight = FontWeight.SemiBold)
            result.steps.forEachIndexed { i, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .background(accent.copy(0.15f), CircleShape)
                            .border(1.dp, accent.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i+1}", style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
                    }
                    Text(step, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, accent: Color) {
    val colors = smartColors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.error.copy(0.07f))
            .border(1.dp, colors.error.copy(0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = colors.error, modifier = Modifier.size(20.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.error)
        }
    }
}

@Composable
private fun QuickPromptSection(onPromptClick: (String) -> Unit, accent: Color) {
    val colors   = smartColors
    val prompts  = listOf(
        "Explain the Pythagorean theorem",
        "What is photosynthesis?",
        "Solve: 2x + 5 = 13",
        "Explain Newton's laws of motion",
        "What is the quadratic formula?",
        "Explain DNA replication"
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Quick asks",
            style      = MaterialTheme.typography.labelLarge,
            color      = colors.subtext,
            letterSpacing = 2.sp
        )
        prompts.forEach { prompt ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                    .clickable { onPromptClick(prompt) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(prompt, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                Icon(Icons.Rounded.ArrowForwardIos, null, tint = accent, modifier = Modifier.size(14.dp))
            }
        }
    }
}
