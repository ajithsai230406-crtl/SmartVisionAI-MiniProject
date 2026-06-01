package com.smartvision.ai.ui.screens.profile

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
import com.smartvision.ai.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE / SETTINGS SCREEN — matches image screen 12
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    userEmail:    String = "ajithkumar@email.com",
    onBack:       () -> Unit,
    onHome:       () -> Unit = onBack,
    onLogout:     () -> Unit = {}
) {
    val colors        = svColors
    var notifEnabled  by remember { mutableStateOf(true) }
    var selectedTheme by remember { mutableStateOf("Dark") }
    var selectedLang  by remember { mutableStateOf("English") }
    var showFeedback  by remember { mutableStateOf(false) }
    var feedbackSent  by remember { mutableStateOf(false) }

    val userName = userEmail.substringBefore("@")
        .split(".").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    Scaffold(
        containerColor      = Color(0xFF050816),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.Settings, null, tint = Color(0xFF8892B0), modifier = Modifier.size(22.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {

            // ── Profile hero ──────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF7B61FF).copy(0.15f), Color.Transparent)))
                .padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    // Avatar with glow ring
                    Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(90.dp)
                            .background(Brush.radialGradient(listOf(Color(0x407B61FF), Color.Transparent)), CircleShape))
                        Box(modifier = Modifier.size(74.dp)
                            .background(Brush.linearGradient(listOf(Color(0xFF7B61FF), Color(0xFF00E5FF))), CircleShape)
                            .border(3.dp, Color(0xFF00E5FF).copy(0.5f), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(userName, style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, fontWeight = FontWeight.Bold)
                    Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8892B0))
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Settings list ─────────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Personal Information
                ProfileSettingRow(
                    icon    = Icons.Rounded.Person,
                    label   = "Personal Information",
                    tint    = Color(0xFF00E5FF),
                    onClick = {}
                )

                // Theme
                ProfileSettingRow(
                    icon    = Icons.Rounded.DarkMode,
                    label   = "Theme",
                    tint    = Color(0xFF8892B0),
                    value   = selectedTheme,
                    onClick = {
                        selectedTheme = if (selectedTheme == "Dark") "Light" else "Dark"
                    }
                )

                // Language
                ProfileSettingRow(
                    icon    = Icons.Rounded.Language,
                    label   = "Language",
                    tint    = Color(0xFF8892B0),
                    value   = selectedLang,
                    onClick = {}
                )

                // AI Assistant Settings
                ProfileSettingRow(
                    icon    = Icons.Rounded.AutoAwesome,
                    label   = "AI Assistant Settings",
                    tint    = Color(0xFF8892B0),
                    onClick = {}
                )

                // Notifications toggle
                Row(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF121826))
                    .border(1.dp, Color(0xFF1E2D47), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Notifications, null, tint = Color(0xFF8892B0), modifier = Modifier.size(22.dp))
                        Text("Notifications", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    Switch(checked = notifEnabled, onCheckedChange = { notifEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor  = Color(0xFF050816),
                            checkedTrackColor  = Color(0xFF00E5FF),
                            uncheckedTrackColor= Color(0xFF1E2D47)))
                }

                ProfileSettingRow(Icons.Rounded.PrivacyTip, "Privacy Policy", Color(0xFF8892B0)) {}
                ProfileSettingRow(Icons.Rounded.HelpOutline, "Help & Support", Color(0xFF8892B0)) {}
                ProfileSettingRow(Icons.Rounded.Info, "About SmartVision AI", Color(0xFF8892B0)) {}

                // Feedback row
                ProfileSettingRow(Icons.Rounded.Feedback, "Send Feedback", Color(0xFF7B61FF)) { showFeedback = true }

                if (feedbackSent) {
                    Box(modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFF00E5FF).copy(0.1f), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF00E5FF).copy(0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                            Text("Feedback sent! The developer will review your report.",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFF00E5FF))
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Logout
                Box(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A0A0A))
                    .border(1.dp, Color(0xFFFF5252).copy(0.3f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onLogout)
                    .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center) {
                    Text("Logout", style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFFF5252), fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // ── Feedback dialog ───────────────────────────────────────────────────────
    if (showFeedback) {
        FeedbackDialog(
            onDismiss = { showFeedback = false },
            onSend    = { _, _ -> showFeedback = false; feedbackSent = true }
        )
    }
}

@Composable
private fun ProfileSettingRow(
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    label:   String,
    tint:    Color,
    value:   String   = "",
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xFF121826))
        .border(1.dp, Color(0xFF1E2D47), RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8892B0))
            Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF8892B0), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FeedbackDialog(onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    var type    by remember { mutableStateOf("Bug Report") }
    var message by remember { mutableStateOf("") }
    val types   = listOf("Bug Report", "UI Issue", "Feature Request", "Performance", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF121826),
        title = {
            Text("Send Feedback", style = MaterialTheme.typography.titleLarge,
                color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Help us improve SmartVision AI", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8892B0))

                // Type chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Issue type", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8892B0))
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(types.size) { i ->
                            val t = types[i]; val sel = t == type
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (sel) Color(0xFF7B61FF).copy(0.2f) else Color(0xFF0A0F1E))
                                .border(1.dp, if (sel) Color(0xFF7B61FF) else Color(0xFF1E2D47), RoundedCornerShape(20.dp))
                                .clickable { type = t }.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                Text(t, style = MaterialTheme.typography.labelLarge,
                                    color = if (sel) Color(0xFF7B61FF) else Color(0xFF8892B0),
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // Message
                Box(modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF0A0F1E), RoundedCornerShape(12.dp))
                    .border(1.dp, if (message.isNotEmpty()) Color(0xFF7B61FF).copy(0.5f) else Color(0xFF1E2D47), RoundedCornerShape(12.dp))
                    .padding(12.dp)) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = message, onValueChange = { if (it.length <= 500) message = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        modifier  = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 140.dp),
                        decorationBox = { inner ->
                            if (message.isEmpty()) Text("Describe the issue in detail…", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8892B0))
                            inner()
                        })
                }
                Text("${message.length}/500", style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8892B0), modifier = Modifier.align(Alignment.End))
            }
        },
        confirmButton = {
            Button(onClick = { onSend(type, message) },
                enabled = message.trim().length >= 10,
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B61FF))) {
                Icon(Icons.Rounded.Send, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Send to Developer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF8892B0)) }
        }
    )
}
