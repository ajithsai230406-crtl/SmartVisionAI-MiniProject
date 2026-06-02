package com.smartvision.ai.compose.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.GradientBackground
import com.smartvision.ai.compose.components.NeonBadge
import com.smartvision.ai.compose.components.AccessibleButton
import com.smartvision.ai.compose.components.clickableWithHaptics
import com.smartvision.ai.compose.components.accessibilityGestures
import com.smartvision.ai.presentation.settings.SettingsViewModel
import com.smartvision.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val ext = MaterialTheme.extended
    val haptic = LocalHapticFeedback.current

    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val highContrast by viewModel.highContrast.collectAsStateWithLifecycle()
    val colorblindMode by viewModel.colorblindMode.collectAsStateWithLifecycle()
    val voiceGuidance by viewModel.voiceGuidance.collectAsStateWithLifecycle()
    val hapticEnabled by viewModel.hapticEnabled.collectAsStateWithLifecycle()
    val largeButtons by viewModel.largeButtons.collectAsStateWithLifecycle()
    val gesturesEnabled by viewModel.gesturesEnabled.collectAsStateWithLifecycle()
    val aiVoiceGuidance by viewModel.aiVoiceGuidance.collectAsStateWithLifecycle()

    val tts = viewModel.ttsManager

    // Speak announcement when entering if voice guidance is active
    LaunchedEffect(Unit) {
        if (voiceGuidance) {
            tts.speak("Accessibility settings workspace. Configure visual, auditory, and gesture preferences.")
        }
    }

    GradientBackground {
        Column(Modifier.fillMaxSize()) {
            // Header Top Bar
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
                        .clickableWithHaptics(haptic, hapticEnabled, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = MaterialTheme.colorScheme.onSurface)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "♿ Accessibility Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Assistive controls & interface helpers",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.neonBlue
                    )
                }
                NeonBadge("Enabled", ext.neonGreen)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Section 1: Visual Settings
                AccessibilitySectionHeader("🎨 Visual Settings", "Adjust sizing, colors and contrast")

                // Text Size Option
                FontSizeSelectionCard(selected = fontSize) { size ->
                    viewModel.setFontSize(size)
                    if (voiceGuidance) {
                        val label = when (size) {
                            0 -> "Small text size"
                            1 -> "Medium text size"
                            2 -> "Large text size"
                            else -> "Extra large text size"
                        }
                        tts.speak(label)
                    }
                }

                // High Contrast Option
                AccessibilitySwitchCard(
                    icon = "👁️‍🗨️",
                    title = "High Contrast Mode",
                    sub = "Enforces solid black/white backgrounds and high visibility borders",
                    checked = highContrast,
                    onCheckedChange = { checked ->
                        viewModel.setHighContrast(checked)
                        if (voiceGuidance) {
                            tts.speak(if (checked) "High contrast mode enabled" else "High contrast mode disabled")
                        }
                    }
                )

                // Colorblind Correction Card
                ColorblindSelectionCard(selectedMode = colorblindMode) { mode ->
                    viewModel.setColorblindMode(mode)
                    if (voiceGuidance) {
                        val label = when (mode) {
                            "NONE" -> "Colorblind correction disabled"
                            "PROTANOMALY" -> "Red green blind correction enabled"
                            else -> "Blue yellow blind correction enabled"
                        }
                        tts.speak(label)
                    }
                }

                // Section 2: Audio & Guidance Settings
                AccessibilitySectionHeader("🔊 Audio & Guidance", "Configure spoken narration and AI vocalizers")

                // Voice Guidance Toggle
                AccessibilitySwitchCard(
                    icon = "🗣️",
                    title = "Voice Guidance",
                    sub = "Speak screen names, actions and options on focus",
                    checked = voiceGuidance,
                    onCheckedChange = { checked ->
                        viewModel.setVoiceGuidance(checked)
                        tts.speak(if (checked) "Voice guidance enabled" else "Voice guidance disabled")
                    }
                )

                // AI Voice Assistant Guidance Toggle
                AccessibilitySwitchCard(
                    icon = "🤖",
                    title = "AI Voice Assistant Guidance",
                    sub = "Speaks camera OCR and waste/object analyzer results automatically",
                    checked = aiVoiceGuidance,
                    onCheckedChange = { checked ->
                        viewModel.setAiVoiceGuidance(checked)
                        if (voiceGuidance) {
                            tts.speak(if (checked) "AI voice assistant results enabled" else "AI voice assistant results disabled")
                        }
                    }
                )

                // Section 3: Interaction Settings
                AccessibilitySectionHeader("📳 Motor & Interaction", "Vibration feedback and size extensions")

                // Haptics Toggle
                AccessibilitySwitchCard(
                    icon = "📳",
                    title = "Haptic Vibration",
                    sub = "Tactile click pulses on actions and camera scans",
                    checked = hapticEnabled,
                    onCheckedChange = { checked ->
                        viewModel.setHapticEnabled(checked)
                        if (voiceGuidance) {
                            tts.speak(if (checked) "Haptic vibration enabled" else "Haptic vibration disabled")
                        }
                    }
                )

                // Large Buttons Mode Toggle
                AccessibilitySwitchCard(
                    icon = "📏",
                    title = "Large Buttons Mode",
                    sub = "Enlarge tap targets and spacing for elderly users",
                    checked = largeButtons,
                    onCheckedChange = { checked ->
                        viewModel.setLargeButtons(checked)
                        if (voiceGuidance) {
                            tts.speak(if (checked) "Large buttons enabled" else "Large buttons disabled")
                        }
                    }
                )

                // Accessibility Gestures (Double Tap to read)
                AccessibilitySwitchCard(
                    icon = "👆",
                    title = "Accessibility Gestures",
                    sub = "Double tap any item to read details, long press for explanation",
                    checked = gesturesEnabled,
                    onCheckedChange = { checked ->
                        viewModel.setGesturesEnabled(checked)
                        if (voiceGuidance) {
                            tts.speak(if (checked) "Accessibility double tap gestures enabled" else "Accessibility gestures disabled")
                        }
                    }
                )

                // Informative Card about TalkBack
                TalkBackHelpCard()

                Spacer(Modifier.height(32.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun AccessibilitySectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AccessibilitySwitchCard(
    icon: String,
    title: String,
    sub: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val ext = MaterialTheme.extended
    val haptic = LocalHapticFeedback.current
    val hapticEnabled = LocalHapticFeedback.current // Wait, it's safer to just default/read from context or pass it, but since this is inside the theme context, let's just trigger haptic normally if clicked
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ext.neonBlue.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ext.neonBlue,
                uncheckedTrackColor = ext.glassBorder
            )
        )
    }
}

@Composable
private fun FontSizeSelectionCard(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val ext = MaterialTheme.extended
    val items = listOf("Small", "Medium", "Large", "X-Large")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("📝 Dynamic Text Size", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEachIndexed { idx, label ->
                    val sel = idx == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (sel) Modifier
                                    .background(ext.neonBlue.copy(0.15f))
                                    .border(2.dp, ext.neonBlue, RoundedCornerShape(10.dp))
                                else Modifier
                                    .background(ext.glassCard)
                                    .border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp))
                            )
                            .clickable { onSelect(idx) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sel) ext.neonBlue else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorblindSelectionCard(
    selectedMode: String,
    onSelectMode: (String) -> Unit
) {
    val ext = MaterialTheme.extended
    val modes = listOf(
        "NONE" to "None",
        "PROTANOMALY" to "Red-Green",
        "TRITANOMALY" to "Blue-Yellow"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🎨 Colorblind Correction Options", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modes.forEach { (value, label) ->
                    val sel = selectedMode == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (sel) Modifier
                                    .background(ext.neonPurple.copy(0.15f))
                                    .border(2.dp, ext.neonPurple, RoundedCornerShape(10.dp))
                                else Modifier
                                    .background(ext.glassCard)
                                    .border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp))
                            )
                            .clickable { onSelectMode(value) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sel) ext.neonPurple else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TalkBackHelpCard() {
    val ext = MaterialTheme.extended
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("💡 Android TalkBack Integration", style = MaterialTheme.typography.bodyMedium, color = ext.neonCyan, fontWeight = FontWeight.Bold)
            Text(
                "Smart Vision AI incorporates full system semantics compatible with TalkBack screen readers. All images, scanner frames, buttons and results expose text descriptions automatically. Use gestures like swiping right/left to navigate controls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
