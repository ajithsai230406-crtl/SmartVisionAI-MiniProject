package com.smartvision.ai.ui.screens.settings

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartvision.ai.ui.components.SmartVisionTopBar
import com.smartvision.ai.ui.theme.*

@Composable
fun SettingsScreen(currentTheme: AppTheme, onThemeChange: (AppTheme) -> Unit,
    onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val c = svColors; val s by vm.state.collectAsState()

    Scaffold(topBar = { SmartVisionTopBar("Settings", onBack = onBack) }, containerColor = c.background) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(2.dp))

            // Profile card
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card)
                .border(1.dp, c.border, RoundedCornerShape(20.dp)).padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(c.primary.copy(.15f), CircleShape)
                        .border(2.dp, c.primary.copy(.4f), CircleShape), Alignment.Center) {
                        Icon(Icons.Rounded.Person, null, tint = c.primary, modifier = Modifier.size(28.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(vm.userName, style = MaterialTheme.typography.titleMedium, color = c.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(vm.userEmail, style = MaterialTheme.typography.bodySmall, color = c.subtext)
                        if (vm.isLoggedIn) Box(Modifier.padding(top = 4.dp)
                            .background(SVColors.green.copy(.14f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("Signed In", style = MaterialTheme.typography.labelSmall, color = SVColors.green)
                        }
                    }
                    if (vm.isLoggedIn) TextButton(onClick = { vm.signOut() }) {
                        Text("Sign Out", color = c.error, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Theme
            SectionLabel("APPEARANCE")
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.card)
                .border(1.dp, c.border, RoundedCornerShape(18.dp)).padding(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("App Theme", style = MaterialTheme.typography.titleMedium, color = c.onSurface, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppTheme.entries.forEach { theme ->
                            val sel = theme.name == s.theme
                            val (bg, accent) = when (theme) {
                                AppTheme.DARK  -> Color(0xFF080C14) to Color(0xFF00E5FF)
                                AppTheme.LIGHT -> Color(0xFFF4F6FC) to Color(0xFF0070FF)
                                AppTheme.BLUE  -> Color(0xFF040D1E) to Color(0xFF4FC3F7)
                            }
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .background(if (sel) accent.copy(.1f) else c.surface)
                                .border(if (sel) 2.dp else 1.dp, if (sel) accent else c.border, RoundedCornerShape(14.dp))
                                .clickable { vm.setTheme(theme); onThemeChange(theme) }.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(8.dp)).background(bg)
                                    .border(1.dp, accent.copy(.35f), RoundedCornerShape(8.dp)), Alignment.Center) {
                                    Box(Modifier.width(18.dp).height(3.dp).background(accent, CircleShape))
                                }
                                Text(theme.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (sel) accent else c.subtext, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                                if (sel) Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }

            // Preferences
            SectionLabel("PREFERENCES")
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.card)
                .border(1.dp, c.border, RoundedCornerShape(18.dp))) {
                Column {
                    ToggleRow(Icons.Rounded.MotionPhotosAuto, "Live Detection", "Continuously scan in real-time", s.liveDetection) { vm.setLive(it) }
                    Divider(color = c.border)
                    ToggleRow(Icons.Rounded.VolumeUp, "Text-to-Speech", "Read detected text aloud", s.tts) { vm.setTts(it) }
                    Divider(color = c.border)
                    ToggleRow(Icons.Rounded.History, "Save Scan History", "Store scans to cloud", s.history) { vm.setHistory(it) }
                    Divider(color = c.border)
                    // Auto-delete setting
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.DeleteSweep, null, tint = c.primary, modifier = Modifier.size(22.dp))
                            Column {
                                Text("Auto-delete captures", style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                                Text("${s.autoDeleteDays} days", style = MaterialTheme.typography.bodySmall, color = c.subtext)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(3, 7, 14, 30).forEach { days ->
                                val sel = days == s.autoDeleteDays
                                Box(Modifier.background(if (sel) c.primary.copy(.2f) else c.surface, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (sel) c.primary else c.border, RoundedCornerShape(8.dp))
                                    .clickable { vm.setAutoDelete(days) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("${days}d", style = MaterialTheme.typography.labelSmall, color = if (sel) c.primary else c.subtext)
                                }
                            }
                        }
                    }
                }
            }

            // About
            SectionLabel("ABOUT")
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.card)
                .border(1.dp, c.border, RoundedCornerShape(18.dp))) {
                Column {
                    InfoRow(Icons.Rounded.Info, "Version", "1.0.0")
                    Divider(color = c.border)
                    InfoRow(Icons.Rounded.Shield, "Privacy Policy", "")
                    Divider(color = c.border)
                    InfoRow(Icons.Rounded.Feedback, "Send Feedback", "")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = svColors.subtext,
        fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, modifier = Modifier.padding(start = 2.dp))
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, sub: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val c = svColors
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, null, tint = c.primary, modifier = Modifier.size(22.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = c.subtext)
            }
        }
        Switch(checked, onToggle, colors = SwitchDefaults.colors(checkedThumbColor = c.background, checkedTrackColor = c.primary, uncheckedTrackColor = c.border))
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    val c = svColors
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = c.subtext, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = c.onSurface)
        }
        if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodyMedium, color = c.subtext)
        else Icon(Icons.Rounded.ChevronRight, null, tint = c.subtext, modifier = Modifier.size(17.dp))
    }
}
