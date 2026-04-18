package com.example.smartvisionai.ui.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartvisionai.ui.components.LanguagePickerDialog
import com.example.smartvisionai.ui.theme.*
import com.example.smartvisionai.viewmodel.SettingsViewModel
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context  = LocalContext.current

    var showLanguagePicker by remember { mutableStateOf(false) }
    var showOfflinePacks   by remember { mutableStateOf(false) }
    var ttsTestMessage     by remember { mutableStateOf<String?>(null) }

    // TTS engine instance for live test
    val tts = remember {
        TextToSpeech(context) { }
    }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp)) {
            Text("SETTINGS", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                color = CyanAccent, letterSpacing = 2.sp)
            Text("Configure your experience", fontSize = 13.sp, color = TextSecondary)
        }

        // ── VOICE & SPEECH ─────────────────────────────────────────────────
        SettingsSection(title = "VOICE & SPEECH") {

            // Voice Commands toggle
            SettingsToggleRow(
                icon      = Icons.Default.Mic,
                iconColor = CyanAccent,
                title     = "Voice Commands",
                subtitle  = if (uiState.voiceEnabled) "Tap mic to speak commands" else "Voice input disabled",
                checked   = uiState.voiceEnabled,
                onToggle  = { enabled ->
                    viewModel.setVoiceEnabled(enabled)
                }
            )

            SettingsDivider()

            // Text-to-Speech toggle + test button
            Column {
                SettingsToggleRow(
                    icon      = Icons.Default.VolumeUp,
                    iconColor = PurpleAccent,
                    title     = "Text-to-Speech",
                    subtitle  = if (uiState.ttsEnabled) "Results spoken aloud" else "Audio output disabled",
                    checked   = uiState.ttsEnabled,
                    onToggle  = { enabled ->
                        viewModel.setTtsEnabled(enabled)
                        if (enabled) {
                            tts.language = Locale.forLanguageTag(uiState.ttsLocale)
                            tts.speak("Text to speech enabled", TextToSpeech.QUEUE_FLUSH, null, "test")
                        } else {
                            tts.stop()
                        }
                    }
                )

                // Test TTS button (shown when TTS is ON)
                AnimatedVisibility(visible = uiState.ttsEnabled) {
                    TextButton(
                        onClick = {
                            tts.language = Locale.forLanguageTag(uiState.ttsLocale)
                            tts.speak(
                                "Smart Vision AI is ready. Point your camera at any object.",
                                TextToSpeech.QUEUE_FLUSH, null, "demo"
                            )
                        },
                        modifier = Modifier.padding(start = 52.dp, bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = CyanAccent,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Test voice", fontSize = 12.sp, color = CyanAccent)
                    }
                }
            }

            SettingsDivider()

            // TTS Speed (shown when TTS enabled)
            AnimatedVisibility(visible = uiState.ttsEnabled) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, null, tint = CyanAccent,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Speech Speed", fontSize = 14.sp,
                                fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("${uiState.speechSpeed}x", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Slider(
                        value = uiState.speechSpeed,
                        onValueChange = { speed ->
                            viewModel.setSpeechSpeed(speed)
                            tts.setSpeechRate(speed)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor       = CyanAccent,
                            activeTrackColor = CyanAccent,
                            inactiveTrackColor = CardBorder
                        )
                    )
                    SettingsDivider()
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── LANGUAGE ───────────────────────────────────────────────────────
        SettingsSection(title = "LANGUAGE") {
            SettingsNavRow(
                icon      = Icons.Default.Language,
                iconColor = OrangeAccent,
                title     = "Default Language",
                value     = uiState.defaultLanguage,
                onClick   = { showLanguagePicker = true }
            )
            SettingsDivider()
            SettingsNavRow(
                icon      = Icons.Default.Translate,
                iconColor = Color(0xFF2196F3),
                title     = "Translation Target",
                value     = uiState.translationTarget,
                onClick   = { showLanguagePicker = true }
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── PERFORMANCE ────────────────────────────────────────────────────
        SettingsSection(title = "PERFORMANCE") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FlashOn, null, tint = GreenAccent,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Performance Mode", fontSize = 14.sp,
                            fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(
                            when (uiState.performanceMode) {
                                "Power Saver"  -> "Reduced frame rate, longer battery"
                                "Performance"  -> "Max speed, higher battery usage"
                                else           -> "Balanced speed and battery"
                            },
                            fontSize = 11.sp, color = TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                PerformanceModeSelector(
                    selected = uiState.performanceMode,
                    onSelect = { mode ->
                        viewModel.setPerformanceMode(mode)
                    }
                )
            }

            SettingsDivider()

            // Analysis interval (based on performance mode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Timer, null, tint = TextMuted,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(14.dp))
                Text("Frame Analysis Interval", fontSize = 14.sp,
                    fontWeight = FontWeight.Medium, color = TextPrimary,
                    modifier = Modifier.weight(1f))
                Text(
                    when (uiState.performanceMode) {
                        "Power Saver"  -> "4 sec"
                        "Performance"  -> "1 sec"
                        else           -> "2 sec"
                    },
                    fontSize = 13.sp, color = CyanAccent, fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── GENERAL ────────────────────────────────────────────────────────
        SettingsSection(title = "GENERAL") {

            // AI Model
            SettingsInfoRow(
                icon      = Icons.Default.Memory,
                iconColor = CyanAccent,
                title     = "AI Model",
                value     = "TensorFlow Lite v2"
            )

            SettingsDivider()

            // Offline Packs
            SettingsNavRow(
                icon      = Icons.Default.Download,
                iconColor = PurpleAccent,
                title     = "Offline Packs",
                value     = "${uiState.offlinePacksCount} downloaded",
                onClick   = { showOfflinePacks = true }
            )

            SettingsDivider()

            // Notifications
            SettingsToggleRow(
                icon      = Icons.Default.Notifications,
                iconColor = OrangeAccent,
                title     = "Notifications",
                subtitle  = "Scan alerts & updates",
                checked   = uiState.notificationsEnabled,
                onToggle  = { enabled ->
                    viewModel.setNotificationsEnabled(enabled)
                    if (enabled) {
                        createNotificationChannel(context)
                    }
                }
            )

            SettingsDivider()

            // App permissions shortcut
            SettingsNavRow(
                icon      = Icons.Default.Security,
                iconColor = GreenAccent,
                title     = "App Permissions",
                value     = "Camera, Mic",
                onClick   = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
            )

            SettingsDivider()

            SettingsInfoRow(
                icon      = Icons.Default.Info,
                iconColor = TextMuted,
                title     = "Version",
                value     = "1.0.0"
            )
        }

        Spacer(Modifier.height(28.dp))
    }

    // ── Language Picker Dialog ─────────────────────────────────────────────
    if (showLanguagePicker) {
        LanguagePickerDialog(
            currentLanguage = uiState.defaultLanguage,
            onSelect = { lang ->
                viewModel.setDefaultLanguage(lang)
                // Update TTS language
                val locale = languageToLocale(lang)
                tts.language = locale
                if (uiState.ttsEnabled) {
                    tts.speak("Language changed to $lang", TextToSpeech.QUEUE_FLUSH, null, "lang")
                }
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false }
        )
    }

    // ── Offline Packs Screen ───────────────────────────────────────────────
    if (showOfflinePacks) {
        OfflinePacksScreen(onBack = { showOfflinePacks = false })
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

fun languageToLocale(language: String): Locale = when (language) {
    "Hindi"                  -> Locale("hi", "IN")
    "Spanish"                -> Locale("es", "ES")
    "French"                 -> Locale("fr", "FR")
    "German"                 -> Locale("de", "DE")
    "Arabic"                 -> Locale("ar", "SA")
    "Chinese (Simplified)"   -> Locale("zh", "CN")
    "Chinese (Traditional)"  -> Locale("zh", "TW")
    "Japanese"               -> Locale("ja", "JP")
    "Korean"                 -> Locale("ko", "KR")
    "Portuguese"             -> Locale("pt", "BR")
    "Russian"                -> Locale("ru", "RU")
    "Italian"                -> Locale("it", "IT")
    "Tamil"                  -> Locale("ta", "IN")
    "Telugu"                 -> Locale("te", "IN")
    "Bengali"                -> Locale("bn", "IN")
    else                     -> Locale.ENGLISH
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "smart_vision_channel",
            "Smart Vision AI",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Scan results and alerts" }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

// ── UI Components ──────────────────────────────────────────────────────────────

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
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = CyanAccent,
                uncheckedTrackColor  = CardBorder,
                uncheckedThumbColor  = TextMuted
            )
        )
    }
}

@Composable
fun SettingsNavRow(
    icon: ImageVector, iconColor: Color,
    title: String, value: String, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(value, fontSize = 12.sp, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SettingsInfoRow(icon: ImageVector, iconColor: Color, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            color = TextPrimary, modifier = Modifier.weight(1f))
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
            val shape = when (idx) {
                0    -> RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                2    -> RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                else -> RoundedCornerShape(0.dp)
            }
            val modeColor = when (mode) {
                "Power Saver" -> Color(0xFF2196F3)
                "Performance" -> Color(0xFFFF5722)
                else          -> CyanAccent
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (isSelected) modeColor.copy(.18f) else Color.Transparent)
                    .border(if (isSelected) 1.dp else 0.dp,
                        if (isSelected) modeColor.copy(.5f) else Color.Transparent, shape)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mode, fontSize = 11.sp,
                    color      = if (isSelected) modeColor else TextMuted,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
