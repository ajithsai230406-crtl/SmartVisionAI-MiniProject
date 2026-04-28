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

@Composable
fun SettingsScreen(
    currentTheme:  AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onBack:        () -> Unit
) {
    val colors = smartColors

    Scaffold(
        topBar          = { SmartVisionTopBar(title = "Settings", onBack = onBack) },
        containerColor  = colors.background
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

            // ── Theme Section ─────────────────────────────────────────────────
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
                            theme     = theme,
                            isSelected = theme == currentTheme,
                            onClick   = { onThemeChange(theme) },
                            modifier  = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Preferences ───────────────────────────────────────────────────
            SettingsSection(title = "Preferences") {
                SettingsToggleRow(
                    icon    = Icons.Rounded.MotionPhotosAuto,
                    label   = "Live Detection",
                    sublabel = "Continuously scan in real-time",
                    checked  = false,
                    onToggle = {}
                )
                Divider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                SettingsToggleRow(
                    icon    = Icons.Rounded.VolumeUp,
                    label   = "Text-to-Speech",
                    sublabel = "Read detected text aloud",
                    checked  = true,
                    onToggle = {}
                )
                Divider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                SettingsToggleRow(
                    icon    = Icons.Rounded.History,
                    label   = "Save Scan History",
                    sublabel = "Store scans to cloud",
                    checked  = true,
                    onToggle = {}
                )
            }

            // ── About ─────────────────────────────────────────────────────────
            SettingsSection(title = "About") {
                SettingsLinkRow(Icons.Rounded.Info,           "Version",       "1.0.0")
                Divider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                SettingsLinkRow(Icons.Rounded.PrivacyTip,    "Privacy Policy",  "")
                Divider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                SettingsLinkRow(Icons.Rounded.Feedback,      "Send Feedback",   "")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeChip(
    theme:      AppTheme,
    isSelected: Boolean,
    onClick:    () -> Unit,
    modifier:   Modifier = Modifier
) {
    val colors     = smartColors
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
        // Mini preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .border(1.dp, accent.copy(0.4f), RoundedCornerShape(10.dp))
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp, 4.dp)
                    .align(Alignment.Center)
                    .background(accent, CircleShape)
            )
        }
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelLarge,
            color      = if (isSelected) accent else colors.subtext,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (isSelected) {
            Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = smartColors
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text     = title.uppercase(),
            style    = MaterialTheme.typography.labelLarge,
            color    = colors.subtext,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
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
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, null, tint = colors.primary, modifier = Modifier.size(22.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(sublabel, style = MaterialTheme.typography.bodySmall, color = colors.subtext)
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor      = colors.background,
                checkedTrackColor      = colors.primary,
                uncheckedTrackColor    = colors.cardBorder
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
