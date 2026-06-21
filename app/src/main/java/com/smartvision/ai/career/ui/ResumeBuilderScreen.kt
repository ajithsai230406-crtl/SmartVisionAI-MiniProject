package com.smartvision.ai.career.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.career.presentation.CareerViewModel
import com.smartvision.ai.career.domain.EducationEntry
import com.smartvision.ai.career.domain.ExperienceEntry
import com.smartvision.ai.career.domain.ProjectEntry
import com.smartvision.ai.compose.components.GradientBackground
import com.smartvision.ai.compose.components.SmartTopBar
import com.smartvision.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeBuilderScreen(
    onBack: () -> Unit,
    viewModel: CareerViewModel = hiltViewModel()
) {
    val ext = MaterialTheme.extended
    val context = LocalContext.current
    
    // Inputs
    val name by viewModel.builderName.collectAsStateWithLifecycle()
    val email by viewModel.builderEmail.collectAsStateWithLifecycle()
    val phone by viewModel.builderPhone.collectAsStateWithLifecycle()
    val address by viewModel.builderAddress.collectAsStateWithLifecycle()
    val summary by viewModel.builderSummary.collectAsStateWithLifecycle()
    val github by viewModel.builderGithub.collectAsStateWithLifecycle()
    val linkedin by viewModel.builderLinkedin.collectAsStateWithLifecycle()
    val skills by viewModel.builderSkills.collectAsStateWithLifecycle()
    val education by viewModel.builderEducation.collectAsStateWithLifecycle()
    val experience by viewModel.builderExperience.collectAsStateWithLifecycle()
    val projects by viewModel.builderProjects.collectAsStateWithLifecycle()
    val template by viewModel.builderTemplate.collectAsStateWithLifecycle()

    var activeStep by remember { mutableStateOf(0) }
    val steps = listOf("Contact", "Summary & Skills", "Education", "Experience & Projects", "Review & Export")

    LaunchedEffect(Unit) {
        viewModel.setScreenContext(mode = "RESUME_BUILDER", resumeText = summary)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearContext()
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(
                title = "📝 Resume Builder",
                onBack = onBack
            )

            // Step Indicator Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                steps.forEachIndexed { index, label ->
                    val isCurrent = index == activeStep
                    val isPassed = index < activeStep
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isCurrent -> NeonBlue.copy(0.18f)
                                    isPassed -> NeonGreen.copy(0.12f)
                                    else -> ext.glassCard
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isCurrent -> NeonBlue
                                    isPassed -> NeonGreen
                                    else -> ext.glassBorder
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { activeStep = index }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}. $label",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isCurrent -> Color.White
                                isPassed -> NeonGreen
                                else -> ext.textHint
                            },
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (activeStep) {
                    0 -> { // Contact Info
                        StepHeader("Personal Details")
                        StyledField(value = name, label = "Full Name", onValueChange = { viewModel.builderName.value = it })
                        StyledField(value = email, label = "Email Address", onValueChange = { viewModel.builderEmail.value = it })
                        StyledField(value = phone, label = "Phone Number", onValueChange = { viewModel.builderPhone.value = it })
                        StyledField(value = address, label = "Home Address / Location", onValueChange = { viewModel.builderAddress.value = it })
                        StyledField(value = linkedin, label = "LinkedIn URL", onValueChange = { viewModel.builderLinkedin.value = it })
                        StyledField(value = github, label = "GitHub URL", onValueChange = { viewModel.builderGithub.value = it })
                    }
                    1 -> { // Summary & Skills
                        StepHeader("Professional Summary & Skills")
                        StyledField(
                            value = summary,
                            label = "Professional Summary",
                            singleLine = false,
                            maxLines = 5,
                            onValueChange = { viewModel.builderSummary.value = it }
                        )

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Skills Tags (comma separated)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonBlue
                        )
                        val skillsText = remember(skills) { skills.joinToString(", ") }
                        StyledField(
                            value = skillsText,
                            label = "e.g. Kotlin, Compose, Firebase, Git",
                            onValueChange = { raw ->
                                viewModel.builderSkills.value = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            }
                        )
                    }
                    2 -> { // Education
                        StepHeader("Education Timeline")
                        
                        var school by remember { mutableStateOf("") }
                        var degree by remember { mutableStateOf("") }
                        var field by remember { mutableStateOf("") }
                        var startD by remember { mutableStateOf("") }
                        var endD by remember { mutableStateOf("") }
                        var grade by remember { mutableStateOf("") }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ext.glassCard)
                                .border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Add Education", style = MaterialTheme.typography.labelMedium, color = NeonBlue, fontWeight = FontWeight.Bold)
                            StyledField(value = school, label = "School/University", onValueChange = { school = it })
                            StyledField(value = degree, label = "Degree (e.g. B.Tech)", onValueChange = { degree = it })
                            StyledField(value = field, label = "Field of Study", onValueChange = { field = it })
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StyledField(value = startD, label = "Start Date", onValueChange = { startD = it }, modifier = Modifier.weight(1f))
                                StyledField(value = endD, label = "End Date", onValueChange = { endD = it }, modifier = Modifier.weight(1f))
                            }
                            StyledField(value = grade, label = "Grade / GPA", onValueChange = { grade = it })

                            Button(
                                onClick = {
                                    if (school.isNotBlank() && degree.isNotBlank()) {
                                        viewModel.builderEducation.value = education + EducationEntry(
                                            school = school, degree = degree, fieldOfStudy = field,
                                            startDate = startD, endDate = endD, grade = grade
                                        )
                                        school = ""; degree = ""; field = ""; startD = ""; endD = ""; grade = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue.copy(0.2f)),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Add", color = Color.White)
                            }
                        }

                        // Added list
                        education.forEachIndexed { i, edu ->
                            AddedItemRow(
                                title = "${edu.degree} - ${edu.school}",
                                subtitle = "${edu.startDate} - ${edu.endDate} | ${edu.grade}",
                                onDelete = {
                                    viewModel.builderEducation.value = education.toMutableList().apply { removeAt(i) }
                                }
                            )
                        }
                    }
                    3 -> { // Experience & Projects
                        StepHeader("Experience & Projects")

                        var company by remember { mutableStateOf("") }
                        var role by remember { mutableStateOf("") }
                        var startD by remember { mutableStateOf("") }
                        var endD by remember { mutableStateOf("") }
                        var desc by remember { mutableStateOf("") }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ext.glassCard)
                                .border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Add Professional Experience", style = MaterialTheme.typography.labelMedium, color = NeonBlue, fontWeight = FontWeight.Bold)
                            StyledField(value = company, label = "Company Name", onValueChange = { company = it })
                            StyledField(value = role, label = "Job Role/Title", onValueChange = { role = it })
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StyledField(value = startD, label = "Start Date", onValueChange = { startD = it }, modifier = Modifier.weight(1f))
                                StyledField(value = endD, label = "End Date", onValueChange = { endD = it }, modifier = Modifier.weight(1f))
                            }
                            StyledField(value = desc, label = "Job Description", singleLine = false, maxLines = 3, onValueChange = { desc = it })

                            Button(
                                onClick = {
                                    if (company.isNotBlank() && role.isNotBlank()) {
                                        viewModel.builderExperience.value = experience + ExperienceEntry(
                                            company = company, role = role, startDate = startD, endDate = endD, description = desc
                                        )
                                        company = ""; role = ""; startD = ""; endD = ""; desc = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(0.2f)),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Add Experience", color = Color.White)
                            }
                        }

                        // Experience list
                        experience.forEachIndexed { i, exp ->
                            AddedItemRow(
                                title = "${exp.role} at ${exp.company}",
                                subtitle = "${exp.startDate} - ${exp.endDate}",
                                onDelete = {
                                    viewModel.builderExperience.value = experience.toMutableList().apply { removeAt(i) }
                                }
                            )
                        }
                    }
                    4 -> { // Review & Export
                        StepHeader("Template selection & Export")

                        Text("Select Design Template", style = MaterialTheme.typography.labelMedium, color = ext.textHint)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val templates = listOf("ATS_FRIENDLY", "PROFESSIONAL", "MODERN", "FRESHER", "INTERNSHIP")
                            templates.forEach { type ->
                                val isSel = template == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) NeonBlue.copy(0.18f) else ext.glassCard)
                                        .border(1.dp, if (isSel) NeonBlue else ext.glassBorder, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.builderTemplate.value = type }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        type.replace("_", " "),
                                        color = if (isSel) Color.White else ext.textHint,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Actions Row
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.saveResumeDraft(isDraft = true) { success ->
                                            android.widget.Toast.makeText(
                                                context,
                                                if (success) "Draft saved successfully!" else "Draft save failed.",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                            if (success) onBack()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.08f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Save Draft 📁", color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        viewModel.saveResumeDraft(isDraft = false) { success ->
                                            android.widget.Toast.makeText(
                                                context,
                                                if (success) "Resume saved & finalized!" else "Save failed.",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                            if (success) onBack()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Finalize & Save ✅", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    val r = com.smartvision.ai.career.domain.ResumeData(
                                        name = name, email = email, phone = phone, address = address,
                                        summary = summary, skills = skills, education = education, experience = experience,
                                        projects = projects, templateType = template
                                    )
                                    val uri = viewModel.exportResumeToPdf(context, r, "Resume_${name.replace(" ", "_")}")
                                    if (uri != null) {
                                        android.widget.Toast.makeText(context, "PDF saved to Downloads!", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Failed to export PDF.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Export to PDF 📄", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val r = com.smartvision.ai.career.domain.ResumeData(
                                        name = name, email = email, phone = phone, address = address,
                                        summary = summary, skills = skills, education = education, experience = experience,
                                        projects = projects, templateType = template
                                    )
                                    val uri = viewModel.exportResumeToDocx(context, r, "Resume_${name.replace(" ", "_")}")
                                    if (uri != null) {
                                        android.widget.Toast.makeText(context, "DOCX saved to Downloads!", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Failed to export DOCX.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Export to DOCX 📝", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Bottom Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { if (activeStep > 0) activeStep-- },
                    enabled = activeStep > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.06f))
                ) { Text("Back", color = Color.White) }

                Button(
                    onClick = { if (activeStep < steps.size - 1) activeStep++ },
                    enabled = activeStep < steps.size - 1,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) { Text("Next", color = Color.Black, fontWeight = FontWeight.Bold) }
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

@Composable
fun StyledField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    val ext = MaterialTheme.extended
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall, color = ext.textHint) },
        singleLine = singleLine,
        maxLines = maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonBlue,
            unfocusedBorderColor = ext.glassBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = ext.glassCard,
            unfocusedContainerColor = ext.glassCard
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun AddedItemRow(
    title: String,
    subtitle: String,
    onDelete: () -> Unit
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = ext.textHint)
        }
        IconButton(onClick = onDelete) {
            Text("🗑️", fontSize = 16.sp)
        }
    }
}
