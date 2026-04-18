package com.example.smartvisionai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartvisionai.ui.theme.*
import com.example.smartvisionai.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp)) {
            Text("SETTINGS", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                color = CyanAccent, letterSpacing = 2.sp)
            Text("Configure your experience", fontSize = 13.sp, color = TextSecondary)
        }

        // ── Voice & Speech ─────────────────────────────────────────────────
        SettingsSection(title = "VOICE & SPEECH") {
            SettingsToggleRow(
                icon = Icons.Default.Mic,
                iconColor = CyanAccent,
                title = "Voice Commands",
                subtitle = "Control app with voice",
                checked = uiState.voiceEnabled,
                onToggle = { viewModel.setVoiceEnabled(it) }
            )
            SettingsDivider()
            SettingsToggleRow(
                icon = Icons.Default.VolumeUp,
                iconColor = PurpleAccent,
                title = "Text-to-Speech",
                subtitle = "Speak detected text aloud",
                checked = uiState.ttsEnabled,
                onToggle = { viewModel.setTtsEnabled(it) }
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Language ─────────────────────────────────────────────────────
        SettingsSection(title = "LANGUAGE") {
            SettingsNavRow(
                icon = Icons.Default.Language,
                iconColor = OrangeAccent,
                title = "Default Language",
                value = uiState.defaultLanguage,
                onClick = { viewModel.showLanguagePicker() }
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Performance ─────────────────────────────────────────────────
        SettingsSection(title = "PERFORMANCE") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FlashOn, contentDescription = null,
                        tint = GreenAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Performance Mode", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                }
                Spacer(Modifier.height(12.dp))
                // 3-segment toggle
                PerformanceModeSelector(
                    selected = uiState.performanceMode,
                    onSelect = { viewModel.setPerformanceMode(it) }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── General ─────────────────────────────────────────────────────
        SettingsSection(title = "GENERAL") {
            SettingsInfoRow(
                icon = Icons.Default.Memory,
                iconColor = CyanAccent,
                title = "AI Model",
                value = "TensorFlow Lite v2"
            )
            SettingsDivider()
            SettingsNavRow(
                icon = Icons.Default.Download,
                iconColor = PurpleAccent,
                title = "Offline Packs",
                value = "${uiState.offlinePacksCount} downloaded",
                onClick = { viewModel.openOfflinePacks() }
            )
            SettingsDivider()
            SettingsToggleRow(
                icon = Icons.Default.Notifications,
                iconColor = OrangeAccent,
                title = "Notifications",
                subtitle = "Scan alerts & updates",
                checked = uiState.notificationsEnabled,
                onToggle = { viewModel.setNotificationsEnabled(it) }
            )
            SettingsDivider()
            SettingsInfoRow(
                icon = Icons.Default.Info,
                iconColor = TextMuted,
                title = "Version",
                value = "1.0.0"
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Text(title, fontSize = 10.sp, color = TextMuted, letterSpacing = 3.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
            content = content
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = CardBorder, thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp))
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector, iconColor: Color,
    title: String, subtitle: String,
    checked: Boolean, onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CyanAccent,
                uncheckedTrackColor = CardBorder
            )
        )
    }
}

@Composable
fun SettingsNavRow(
    icon: ImageVector, iconColor: Color,
    title: String, value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(value, fontSize = 12.sp, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SettingsInfoRow(icon: ImageVector, iconColor: Color, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = TextSecondary)
    }
}

@Composable
fun PerformanceModeSelector(selected: String, onSelect: (String) -> Unit) {
    val modes = listOf("Power Saver", "Balanced", "Performance")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BackgroundDark)
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
    ) {
        modes.forEachIndexed { idx, mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(
                        when (idx) {
                            0 -> RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                            2 -> RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                            else -> RoundedCornerShape(0.dp)
                        }
                    )
                    .background(if (isSelected) CyanDark.copy(alpha = 0.5f) else Color.Transparent)
                    .border(
                        if (isSelected) 1.dp else 0.dp,
                        if (isSelected) CyanAccent.copy(0.5f) else Color.Transparent,
                        when (idx) {
                            0 -> RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                            2 -> RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                            else -> RoundedCornerShape(0.dp)
                        }
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(mode, fontSize = 12.sp,
                    color = if (isSelected) CyanAccent else TextMuted,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}
