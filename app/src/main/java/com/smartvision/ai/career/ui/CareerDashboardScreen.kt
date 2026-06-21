package com.smartvision.ai.career.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.career.presentation.CareerViewModel
import com.smartvision.ai.career.domain.ResumeData
import com.smartvision.ai.compose.components.GradientBackground
import com.smartvision.ai.compose.components.SmartTopBar
import com.smartvision.ai.compose.navigation.Routes
import com.smartvision.ai.ui.theme.*

val AccentCareer = Color(0xFF6C63FF) // Elegant Career Indigo

@Composable
fun CareerDashboardScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CareerViewModel = hiltViewModel()
) {
    val resumes by viewModel.resumes.collectAsStateWithLifecycle()
    val letters by viewModel.coverLetters.collectAsStateWithLifecycle()
    val preps by viewModel.interviewPreps.collectAsStateWithLifecycle()
    val ext = MaterialTheme.extended

    LaunchedEffect(Unit) {
        viewModel.setScreenContext(mode = "DASHBOARD")
        viewModel.loadAllData()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearContext()
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(
                title = "💼 Career Assistant",
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Career Progress / Welcome Card ────────────────────────────────
                item {
                    CareerSummaryCard(
                        resumeCount = resumes.size,
                        letterCount = letters.size,
                        prepCount = preps.size,
                        ext = ext
                    )
                }

                // ── Services Section ─────────────────────────────────────────────
                item {
                    Text(
                        "Career Services",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ServiceCard(
                                title = "Resume Builder",
                                description = "Write ATS-friendly resume drafts",
                                emoji = "📝",
                                accent = NeonBlue,
                                onClick = { onNavigate(Routes.RESUME_BUILDER) },
                                modifier = Modifier.weight(1f)
                            )
                            ServiceCard(
                                title = "ATS Analyzer",
                                description = "Score and optimize your resume",
                                emoji = "📊",
                                accent = NeonGreen,
                                onClick = { onNavigate(Routes.RESUME_ANALYZER) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ServiceCard(
                                title = "Cover Letter",
                                description = "Write tailored letters instantly",
                                emoji = "✉️",
                                accent = NeonPurple,
                                onClick = { onNavigate(Routes.COVER_LETTER) },
                                modifier = Modifier.weight(1f)
                            )
                            ServiceCard(
                                title = "Interview Prep",
                                description = "Mock voice practice sessions",
                                emoji = "🎙️",
                                accent = NeonCyan,
                                onClick = { onNavigate(Routes.INTERVIEW_PREP) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ── Recent Drafts Section ────────────────────────────────────────
                item {
                    Text(
                        "Recent Resumes & Drafts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (resumes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ext.glassCard)
                                .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📄", fontSize = 36.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No resumes found. Create one now!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ext.textHint
                                )
                            }
                        }
                    }
                } else {
                    items(resumes.take(4)) { resume ->
                        ResumeRowItem(
                            resume = resume,
                            ext = ext,
                            onEdit = {
                                viewModel.loadResumeIntoBuilder(resume)
                                onNavigate(Routes.RESUME_BUILDER)
                            },
                            onDelete = { viewModel.deleteResume(resume.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerSummaryCard(
    resumeCount: Int,
    letterCount: Int,
    prepCount: Int,
    ext: ExtendedColors
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(AccentCareer.copy(0.2f), Color.Transparent)
                )
            )
            .border(1.5.dp, AccentCareer.copy(0.35f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Elevate Your Career Path ✨",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Build modern resumes, optimize keywords using ATS algorithms, and practice domain-specific voice interviews.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.8f)
            )
            
            Divider(color = Color.White.copy(0.12f), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCol("Resumes", resumeCount.toString(), NeonBlue)
                StatCol("Cover Letters", letterCount.toString(), NeonPurple)
                StatCol("Interview Sessions", prepCount.toString(), NeonCyan)
            }
        }
    }
}

@Composable
private fun StatCol(label: String, valText: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valText, fontSize = 22.sp, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
    }
}

@Composable
private fun ServiceCard(
    title: String,
    description: String,
    emoji: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(0.12f))
                .border(1.dp, accent.copy(0.3f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ResumeRowItem(
    resume: ResumeData,
    ext: ExtendedColors,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NeonBlue.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("📄", fontSize = 20.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                resume.name.ifBlank { "Untitled Resume" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (resume.isDraft) "Draft" else "Finalized",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (resume.isDraft) NeonOrange else NeonGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Text("•", style = MaterialTheme.typography.bodySmall, color = ext.textHint)
                Text(
                    resume.templateType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.textHint
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = onEdit) {
                Text("✏️", fontSize = 16.sp)
            }
            IconButton(onClick = onDelete) {
                Text("🗑️", fontSize = 16.sp)
            }
        }
    }
}
