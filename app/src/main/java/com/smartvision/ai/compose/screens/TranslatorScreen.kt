package com.smartvision.ai.compose.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.presentation.translation.TranslationViewModel
import com.smartvision.ai.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val languages = listOf("Hindi", "Telugu", "Tamil", "French", "Spanish", "German", "Japanese", "English")

@Composable
fun TranslatorScreen(
    onBack: () -> Unit,
    viewModel: TranslationViewModel = hiltViewModel()
) {
    val ext            = MaterialTheme.extended
    var inputText      by remember { mutableStateOf("") }
    var targetLang     by remember { mutableStateOf("Hindi") }
    var showDropdown   by remember { mutableStateOf(false) }
    val translatedText by viewModel.output.collectAsStateWithLifecycle()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(title = "Text Translator", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text("Enter text", style = MaterialTheme.typography.labelMedium, color = ext.neonBlue)
                Spacer(Modifier.height(8.dp))
                SmartTextField(
                    value         = inputText,
                    onValueChange = { inputText = it },
                    placeholder   = "Type or paste text here...",
                    singleLine    = false,
                    maxLines      = 5
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Detected Language", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ext.glassCard)
                                .border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) { Text("Auto Detect", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium) }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Translate to", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Box {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ext.glassCard)
                                    .border(1.dp, ext.neonBlue.copy(0.4f), RoundedCornerShape(10.dp))
                                    .clickable { showDropdown = true }
                                    .padding(12.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(targetLang, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                                    Text("▾", color = ext.neonBlue)
                                }
                            }
                            DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
                                languages.forEach { lang ->
                                    DropdownMenuItem(text = { Text(lang) }, onClick = { targetLang = lang; showDropdown = false })
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                NeonButton(
                    text     = "Translate",
                    onClick  = { viewModel.translate(inputText, targetLang) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                if (translatedText.isNotBlank() && translatedText != "Translated text will appear here.") {
                    Text("Translation", style = MaterialTheme.typography.labelMedium, color = ext.neonPurple)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ext.glassCard)
                            .border(1.dp, ext.neonPurple.copy(0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(translatedText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlineNeonButton("Copy", onClick = {}, modifier = Modifier.weight(1f))
                        OutlineNeonButton("🔊 Speak", onClick = {}, modifier = Modifier.weight(1f))
                        OutlineNeonButton("Share", onClick = {}, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
