package com.smartvision.ai.career.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.career.presentation.CareerViewModel
import com.smartvision.ai.career.domain.InterviewQuestion
import com.smartvision.ai.compose.components.GradientBackground
import com.smartvision.ai.compose.components.SmartTopBar
import com.smartvision.ai.ui.theme.*

@Composable
fun InterviewPrepScreen(
    onBack: () -> Unit,
    viewModel: CareerViewModel = hiltViewModel()
) {
    val ext = MaterialTheme.extended
    val context = LocalContext.current
    
    val role by viewModel.prepRole.collectAsStateWithLifecycle()
    val domain by viewModel.prepDomain.collectAsStateWithLifecycle()
    val resumeText by viewModel.prepResumeText.collectAsStateWithLifecycle()
    
    val questions by viewModel.questions.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isPrepGenerating.collectAsStateWithLifecycle()
    
    val activeIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val isMock by viewModel.isMockMode.collectAsStateWithLifecycle()
    val mockAnswer by viewModel.userMockAnswerText.collectAsStateWithLifecycle()
    val isEvaluating by viewModel.isEvaluatingAnswer.collectAsStateWithLifecycle()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.userMockAnswerText.value = spoken
            }
        }
    }

    LaunchedEffect(activeIndex, questions) {
        val currentQ = questions.getOrNull(activeIndex)?.question ?: ""
        viewModel.setScreenContext(mode = "INTERVIEW_PREP", currentQuestion = currentQ)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeaking()
            viewModel.clearContext()
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(
                title = "🎙️ Interview Preparation",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Config Panel if empty
                if (questions.isEmpty()) {
                    StepHeader("Setup Prep Session")
                    StyledField(value = role, label = "Job Role (e.g. iOS Developer)", onValueChange = { viewModel.prepRole.value = it })
                    StyledField(value = domain, label = "Domain / Industry (e.g. Technology)", onValueChange = { viewModel.prepDomain.value = it })
                    StyledField(value = resumeText, label = "Resume Details / Skills (Optional)", singleLine = false, maxLines = 3, onValueChange = { viewModel.prepResumeText.value = it })

                    Button(
                        onClick = { viewModel.generateQuestions() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Generate Prep Questions ⚡", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Prep Session Dashboard
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$role - $domain",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        // Reset button
                        Button(
                            onClick = { viewModel.prepRole.value = ""; viewModel.questions.let { viewModel.loadAllData() } }, // trigger reset
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.06f))
                        ) {
                            Text("New Session", color = Color.White)
                        }
                    }

                    // Mode Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(0.04f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modes = listOf("Study Mode", "Voice Mock")
                        modes.forEachIndexed { idx, title ->
                            val isSel = (idx == 1 && isMock) || (idx == 0 && !isMock)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) NeonCyan.copy(0.18f) else Color.Transparent)
                                    .border(1.dp, if (isSel) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.isMockMode.value = (idx == 1) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSel) Color.White else ext.textHint,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    if (!isMock) {
                        // Study Mode: List of Expandable Questions
                        questions.forEachIndexed { index, q ->
                            var expanded by remember { mutableStateOf(false) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ext.glassCard)
                                    .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
                                    .clickable { expanded = !expanded }
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}. ${q.question}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (q.difficulty) {
                                                    "EASY" -> NeonGreen.copy(0.15f)
                                                    "HARD" -> NeonPink.copy(0.15f)
                                                    else -> NeonBlue.copy(0.15f)
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            q.difficulty,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (q.difficulty) {
                                                "EASY" -> NeonGreen
                                                "HARD" -> NeonPink
                                                else -> NeonBlue
                                            }
                                        )
                                    }
                                }

                                if (expanded) {
                                    Divider(color = Color.White.copy(0.08f))
                                    Text("Suggested Answer", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                                    Text(q.suggestedAnswer, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.9f))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Explanation / recruiter tip", style = MaterialTheme.typography.labelSmall, color = NeonPurple, fontWeight = FontWeight.Bold)
                                    Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                                }
                            }
                        }
                    } else {
                        // Voice Mock Mode: Question by Question practice
                        val activeQ = questions.getOrNull(activeIndex)
                        if (activeQ != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ext.glassCard)
                                    .border(1.dp, ext.glassBorder, RoundedCornerShape(20.dp))
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Question Text
                                Text(
                                    text = "Question ${activeIndex + 1} of ${questions.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                                Text(
                                    text = activeQ.question,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                // Speak Aloud Action
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(0.12f))
                                        .border(1.5.dp, NeonCyan, CircleShape)
                                        .clickable { viewModel.speakQuestionAloud() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🔊", fontSize = 24.sp)
                                }

                                Divider(color = Color.White.copy(0.08f))

                                // Spoken Response Input
                                Text(
                                    text = activeQ.userAnswer.ifBlank { mockAnswer.ifBlank { "Tap Mic and Speak Your Answer" } },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (activeQ.userAnswer.isNotBlank() || mockAnswer.isNotBlank()) Color.White else ext.textHint,
                                    textAlign = TextAlign.Center
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Voice Record trigger
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(NeonPink.copy(0.12f))
                                            .border(1.5.dp, NeonPink, CircleShape)
                                            .clickable {
                                                viewModel.stopSpeaking()
                                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your answer...")
                                                }
                                                speechLauncher.launch(intent)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🎤", fontSize = 24.sp)
                                    }

                                    // Evaluation button
                                    Button(
                                        onClick = { viewModel.submitMockAnswer() },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                        enabled = mockAnswer.isNotBlank() && !isEvaluating
                                    ) {
                                        if (isEvaluating) {
                                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
                                        } else {
                                            Text("Submit Response ✅", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Answer Score & Feedback
                                if (activeQ.evaluationScore >= 0) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(0.04f))
                                            .border(1.dp, NeonGreen.copy(0.2f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("AI Evaluation Feedback", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontWeight = FontWeight.Bold)
                                            Text("Score: ${activeQ.evaluationScore}%", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontWeight = FontWeight.ExtraBold)
                                        }
                                        Text(
                                            text = activeQ.evaluationFeedback,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(0.9f)
                                        )
                                    }
                                }

                                // Nav buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Button(
                                        onClick = { if (activeIndex > 0) viewModel.selectQuestionIndex(activeIndex - 1) },
                                        enabled = activeIndex > 0,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.06f))
                                    ) { Text("Prev Q", color = Color.White) }

                                    Button(
                                        onClick = { if (activeIndex < questions.size - 1) viewModel.selectQuestionIndex(activeIndex + 1) },
                                        enabled = activeIndex < questions.size - 1,
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                    ) { Text("Next Q", color = Color.Black, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
