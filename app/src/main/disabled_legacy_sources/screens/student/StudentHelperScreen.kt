package com.smartvision.ai.ui.screens.student

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.ui.components.SmartVisionTopBar
import com.smartvision.ai.ui.theme.*

@Composable
fun StudentHelperScreen(onBack: () -> Unit, vm: StudentHelperViewModel = hiltViewModel()) {
    val c = svColors
    val accent = SVColors.blue
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = { SmartVisionTopBar("Student Helper", onBack = onBack) },
        containerColor = c.background
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            // Input card
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.card)
                    .border(1.dp, accent.copy(.25f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .background(accent.copy(.14f), RoundedCornerShape(10.dp))
                                .border(1.dp, accent.copy(.3f), RoundedCornerShape(10.dp)),
                            Alignment.Center
                        ) {
                            Icon(Icons.Rounded.School, null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            "Ask Gemini AI",
                            style = MaterialTheme.typography.titleMedium,
                            color = c.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedTextField(
                        value = uiState.question,
                        onValueChange = { vm.setQuestion(it) },
                        placeholder = { Text("Type a question, formula, equation…", color = c.subtext) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = c.border,
                            focusedTextColor = c.onSurface,
                            unfocusedTextColor = c.onSurface,
                            cursorColor = accent,
                            focusedContainerColor = c.surface,
                            unfocusedContainerColor = c.surface
                        )
                    )
                    Button(
                        onClick = { vm.ask() },
                        enabled = uiState.question.isNotEmpty() && !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Thinking…", color = Color.White, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Get Explanation", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Error display
            uiState.error?.let { errorMsg ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(c.error.copy(.08f), RoundedCornerShape(14.dp))
                        .border(1.dp, c.error.copy(.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(errorMsg, color = c.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Answer display
            if (uiState.answer.isNotEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(accent.copy(.05f))
                        .border(1.dp, accent.copy(.2f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(17.dp))
                        Text(
                            "AI Explanation",
                            style = MaterialTheme.typography.titleMedium,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        uiState.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurface,
                        lineHeight = 22.sp
                    )
                }
            }

            // Quick prompts
            if (uiState.answer.isEmpty() && !uiState.isLoading) {
                Text("QUICK ASKS", style = MaterialTheme.typography.labelSmall, color = c.subtext, letterSpacing = 2.sp)
                listOf(
                    "Explain the Pythagorean theorem",
                    "What is photosynthesis?",
                    "Solve: 2x + 5 = 13",
                    "Explain Newton's 3 laws of motion"
                ).forEach { prompt ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(c.card)
                            .border(1.dp, c.border, RoundedCornerShape(13.dp))
                            .clickable { vm.setQuestion(prompt); vm.ask() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text(prompt, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                        Icon(Icons.Rounded.ArrowForwardIos, null, tint = accent, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
