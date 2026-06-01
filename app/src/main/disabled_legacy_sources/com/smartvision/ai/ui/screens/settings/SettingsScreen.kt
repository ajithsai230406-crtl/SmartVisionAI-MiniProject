package com.smartvision.ai.ui.screens.settings

import androidx.compose.animation.*
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
import com.smartvision.ai.ui.components.SmartVisionTopBar
import com.smartvision.ai.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// SETTINGS SCREEN
// CHANGE: added "Go to Home" row + functional Feedback dialog that
//         collects issue text and simulates sending to developer.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    currentTheme:  AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onBack:        () -> Unit,
    onHome:        () -> Unit = onBack
) {
    val colors = smartColors
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackSent       by remember { mutableStateOf(false) }

    Scaffold(
        topBar         = { SmartVisionTopBar(title = "Settings", onBack = onBack) },
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

            // ── Go to Home ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.primary.copy(0.1f))
                    .border(1.dp, colors.primary.copy(0.3f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onHome)
                    .padding(16.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Home, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                    Text(
                        "Go to Home",
                        style      = MaterialTheme.typography.titleMedium,
                        color      = colors.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.weight(1f)
                    )
                    Icon(Icons.Rounded.ChevronRight, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                }
            }

            // ── Appearance ────────────────────────────────────────────────────
            SettingsSection(title = "Appearance") {
                Text(
                    "Choose Theme",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppTheme.entries.forEach { theme ->
                        ThemeChip(
                            theme      = theme,
                            isSelected = theme == currentTheme,
                            onClick    = { onThemeChange(theme) },
                            modifier   = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Preferences ───────────────────────────────────────────────────
            SettingsSection(title = "Preferences") {
                SettingsToggleRow(
                    icon     = Icons.Rounded.MotionPhotosAuto,
                    label    = "Live Detection",
                    sublabel = "Continuously scan in real-time",
                    checked  = false,
                    onToggle = {}
                )
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                SettingsToggleRow(
                    icon     = Icons.Rounded.VolumeUp,
                    label    = "Text-to-Speech",
                    sublabel = "Read detected text aloud",
                    checked  = true,
                    onToggle = {}
                )
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                SettingsToggleRow(
                    icon     = Icons.Rounded.History,
                    label    = "Save Scan History",
                    sublabel = "Store scans to cloud",
                    checked  = true,
                    onToggle = {}
                )
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                SettingsToggleRow(
                    icon     = Icons.Rounded.Translate,
                    label    = "Show Original Text in Translations",
                    sublabel = "Display source text below translated output",
                    checked  = true,
                    onToggle = {}
                )
            }

            // ── About + Feedback ──────────────────────────────────────────────
            SettingsSection(title = "About") {
                SettingsLinkRow(Icons.Rounded.Info,         "Version",        "2.0.0")
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                SettingsLinkRow(Icons.Rounded.PrivacyTip,   "Privacy Policy", "")
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                // Feedback row — opens dialog
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .clickable { showFeedbackDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Feedback, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                        Column {
                            Text("Send Feedback", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                            Text(
                                "Report bugs or suggest improvements",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.subtext
                            )
                        }
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = colors.subtext, modifier = Modifier.size(18.dp))
                }

                if (feedbackSent) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.primary.copy(0.12f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                            Text(
                                "Feedback sent! Thank you — the developer will review your report.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Feedback dialog ───────────────────────────────────────────────────────
    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            onSend    = { type, message ->
                // TODO: replace with real Firebase/email submission
                // FirebaseFunctions.getInstance().getHttpsCallable("sendFeedback")
                //     .call(mapOf("type" to type, "message" to message))
                showFeedbackDialog = false
                feedbackSent       = true
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEEDBACK DIALOG — collects issue type + description, sends to developer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSend:    (type: String, message: String) -> Unit
) {
    val colors          = smartColors
    var feedbackType    by remember { mutableStateOf("Bug Report") }
    var feedbackMessage by remember { mutableStateOf("") }
    val types           = listOf("Bug Report", "UI Issue", "Feature Request", "Performance", "Other")

    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = colors.card,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Feedback, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                Text(
                    "Send Feedback",
                    style      = MaterialTheme.typography.titleLarge,
                    color      = colors.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                Text(
                    "Help us improve SmartVision AI",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.subtext
                )

                // Issue type selector
                Text("Issue type", style = MaterialTheme.typography.labelLarge, color = colors.subtext)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(types.size) { i ->
                        val t        = types[i]
                        val selected = t == feedbackType
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) colors.primary.copy(0.2f) else colors.surface)
                                .border(1.dp, if (selected) colors.primary else colors.cardBorder, RoundedCornerShape(20.dp))
                                .clickable { feedbackType = t }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                t,
                                style      = MaterialTheme.typography.labelLarge,
                                color      = if (selected) colors.primary else colors.subtext,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Message box
                Text("Describe the issue", style = MaterialTheme.typography.labelLarge, color = colors.subtext)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, if (feedbackMessage.isNotEmpty()) colors.primary.copy(0.5f) else colors.cardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value         = feedbackMessage,
                        onValueChange = { if (it.length <= 500) feedbackMessage = it },
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                        modifier      = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 140.dp),
                        decorationBox = { inner ->
                            if (feedbackMessage.isEmpty()) {
                                Text(
                                    "Describe the bug or suggestion in detail…\n\nInclude steps to reproduce if reporting a bug.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.subtext
                                )
                            }
                            inner()
                        }
                    )
                }
                Text(
                    "${feedbackMessage.length}/500",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = colors.subtext,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { onSend(feedbackType, feedbackMessage) },
                enabled  = feedbackMessage.trim().length >= 10,
                colors   = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Send, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Send to Developer", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.subtext)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemeChip(
    theme:      AppTheme,
    isSelected: Boolean,
    onClick:    () -> Unit,
    modifier:   Modifier = Modifier
) {
    val colors = smartColors
    val (label, bg, accent) = when (theme) {
        AppTheme.DARK  -> Triple("Dark",  Color(0xFF080C14), Color(0xFF00E5FF))
        AppTheme.LIGHT -> Triple("Light", Color(0xFFF4F6FC), Color(0xFF0070FF))
        AppTheme.BLUE  -> Triple("Blue",  Color(0xFF040D1E), Color(0xFF4FC3F7))
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) accent.copy(0.1f) else colors.card)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accent else colors.cardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .border(1.dp, accent.copy(0.4f), RoundedCornerShape(10.dp))
        ) {
            Box(modifier = Modifier.size(16.dp, 4.dp).align(Alignment.Center).background(accent, CircleShape))
        }
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelLarge,
            color      = if (isSelected) accent else colors.subtext,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = smartColors
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text          = title.uppercase(),
            style         = MaterialTheme.typography.labelLarge,
            color         = colors.subtext,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            modifier      = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.card)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    label:    String,
    sublabel: String,
    checked:  Boolean,
    onToggle: (Boolean) -> Unit
) {
    val colors = smartColors
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.weight(1f)
        ) {
            Icon(icon, null, tint = colors.primary, modifier = Modifier.size(22.dp))
            Column {
                Text(label,    style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(sublabel, style = MaterialTheme.typography.bodySmall,   color = colors.subtext)
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = colors.background,
                checkedTrackColor   = colors.primary,
                uncheckedTrackColor = colors.cardBorder
            )
        )
    }
}

@Composable
private fun SettingsLinkRow(
    icon:  androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    val colors = smartColors
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = colors.subtext, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        }
        if (value.isNotEmpty()) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
        } else {
            Icon(Icons.Rounded.ChevronRight, null, tint = colors.subtext, modifier = Modifier.size(18.dp))
        }
    }
}
