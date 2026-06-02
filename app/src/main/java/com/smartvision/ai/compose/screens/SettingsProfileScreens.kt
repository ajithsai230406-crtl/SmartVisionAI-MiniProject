package com.smartvision.ai.compose.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StatFs
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.presentation.auth.AuthViewModel
import com.smartvision.ai.presentation.settings.SettingsViewModel
import com.smartvision.ai.presentation.history.HistoryViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

// ══════════════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN — Premium Full-Featured
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val ctx   = LocalContext.current
    val ext   = MaterialTheme.extended
    val scope = rememberCoroutineScope()

    var voiceSpeed    by remember { mutableStateOf(1.0f) }
    var fontSize      by remember { mutableStateOf(1) }
    var cacheCleared  by remember { mutableStateOf(false) }
    var notifEnabled  by remember { mutableStateOf(true) }
    var hapticEnabled by remember { mutableStateOf(true) }
    var autoSave      by remember { mutableStateOf(true) }

    val autoDeletePeriod by viewModel.autoDeletePeriod.collectAsStateWithLifecycle()
    val autoDeleteOcr by viewModel.autoDeleteOcr.collectAsStateWithLifecycle()
    val autoDeleteHomework by viewModel.autoDeleteHomework.collectAsStateWithLifecycle()
    val autoDeleteQr by viewModel.autoDeleteQr.collectAsStateWithLifecycle()
    val autoDeleteObject by viewModel.autoDeleteObject.collectAsStateWithLifecycle()
    val autoDeleteMedicine by viewModel.autoDeleteMedicine.collectAsStateWithLifecycle()
    val autoDeleteTranslation by viewModel.autoDeleteTranslation.collectAsStateWithLifecycle()

    val cacheSize   = remember { getAppCacheSize(ctx) }
    val storageInfo = remember { getStorageInfo() }

    GradientBackground {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp)).clickable(onClick = onBack), Alignment.Center) {
                    Text("←", color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("⚙️ Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Customize your experience", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsSection("🎨 Appearance")
                ThemeToggleCard(isDarkTheme = isDarkTheme, onToggle = onThemeToggle)
                FontSizeCard(selected = fontSize, onSelect = { fontSize = it })

                SettingsSection("🔊 Voice & Sound")
                VoiceSpeedCard(speed = voiceSpeed, onSpeedChange = { voiceSpeed = it })
                PremiumToggleRow("🔔", "Push Notifications", "Scan results, AI tips", notifEnabled) { notifEnabled = it }
                PremiumToggleRow("📳", "Haptic Feedback", "Vibration on interactions", hapticEnabled) { hapticEnabled = it }

                SettingsSection("💾 Storage")
                CacheClearCard(sizeLabel = cacheSize, cleared = cacheCleared) {
                    scope.launch { clearCache(ctx); cacheCleared = true }
                }
                PremiumToggleRow("📥", "Auto-save Scans", "Save to gallery automatically", autoSave) { autoSave = it }

                SettingsSection("🧹 Auto Delete History")
                RetentionPeriodCard(selectedPeriod = autoDeletePeriod) { viewModel.setAutoDeletePeriod(it) }
                if (autoDeletePeriod != "NEVER") {
                    PremiumToggleRow("📝", "Auto-Delete OCR Scans", "Clean OCR scan records", autoDeleteOcr) { viewModel.setAutoDeleteOcr(it) }
                    PremiumToggleRow("📚", "Auto-Delete Homework Scans", "Clean homework solver records", autoDeleteHomework) { viewModel.setAutoDeleteHomework(it) }
                    PremiumToggleRow("📷", "Auto-Delete QR Scans", "Clean scanned QR/barcodes", autoDeleteQr) { viewModel.setAutoDeleteQr(it) }
                    PremiumToggleRow("🔍", "Auto-Delete Object Scans", "Clean object detector records", autoDeleteObject) { viewModel.setAutoDeleteObject(it) }
                    PremiumToggleRow("💊", "Auto-Delete Medicine Scans", "Clean medicine identifier records", autoDeleteMedicine) { viewModel.setAutoDeleteMedicine(it) }
                    PremiumToggleRow("🌐", "Auto-Delete Translations", "Clean translation history", autoDeleteTranslation) { viewModel.setAutoDeleteTranslation(it) }
                }

                SettingsSection("♿ Accessibility")
                AccessibilityCard { onNavigate(com.smartvision.ai.compose.navigation.Routes.ACCESSIBILITY) }

                SettingsSection("📋 Legal & Support")
                LegalRowCompose("🔒", "Privacy Policy") { onNavigate(com.smartvision.ai.compose.navigation.Routes.PRIVACY_POLICY) }
                LegalRowCompose("📄", "Terms & Conditions") { onNavigate(com.smartvision.ai.compose.navigation.Routes.TERMS_CONDITIONS) }
                LegalRow("💬", "Send Feedback",       "mailto:support@smartvisionai.app",       ctx)
                LegalRow("⭐", "Rate the App",        "https://play.google.com/store",          ctx)

                SettingsSection("ℹ️ About")
                AppInfoCard()

                Spacer(Modifier.height(32.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable private fun SettingsSection(title: String) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = NeonBlue, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}

@Composable
private fun ThemeToggleCard(isDarkTheme: Boolean, onToggle: () -> Unit) {
    val ext = MaterialTheme.extended
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(if (isDarkTheme) listOf(NeonBlue.copy(0.12f), NeonPurple.copy(0.08f)) else listOf(Color(0xFFFFF8F0), Color(0xFFF0F4FF))))
            .border(1.dp, if (isDarkTheme) NeonBlue.copy(0.3f) else Color(0xFFDDE2FF), RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle).padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (isDarkTheme) NeonBlue.copy(0.15f) else Color(0xFFFFF0C0)), Alignment.Center) { Text(if (isDarkTheme) "🌙" else "☀️", fontSize = 22.sp) }
            Column(Modifier.weight(1f)) {
                Text(if (isDarkTheme) "Dark Neon Mode" else "Light Mode", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(if (isDarkTheme) "Premium futuristic dark theme" else "Clean minimal light theme", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = isDarkTheme, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonBlue, uncheckedTrackColor = ext.glassBorder))
        }
    }
}

@Composable
private fun FontSizeCard(selected: Int, onSelect: (Int) -> Unit) {
    val ext = MaterialTheme.extended
    val items = listOf(Triple("Small", "Aa", 12.sp), Triple("Medium", "Aa", 15.sp), Triple("Large", "Aa", 18.sp))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("📝 Font Size", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { idx, (label, preview, sp) ->
                    val sel = idx == selected
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .then(if (sel) Modifier.background(NeonBlue.copy(0.15f)).border(2.dp, NeonBlue, RoundedCornerShape(10.dp)) else Modifier.background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp)))
                        .clickable { onSelect(idx) }.padding(10.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(preview, fontSize = sp, color = if (sel) NeonBlue else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text(label, style = MaterialTheme.typography.labelSmall, color = if (sel) NeonBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceSpeedCard(speed: Float, onSpeedChange: (Float) -> Unit) {
    val ext = MaterialTheme.extended
    val label = when { speed < 0.75f -> "Slow"; speed < 1.25f -> "Normal"; speed < 1.75f -> "Fast"; else -> "Very Fast" }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("🔊 Voice Speed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                NeonBadge(label, NeonBlue)
            }
            Slider(value = speed, onValueChange = onSpeedChange, valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(thumbColor = NeonBlue, activeTrackColor = NeonBlue, inactiveTrackColor = ext.glassBorder))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("0.5×", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${String.format("%.1f", speed)}×", style = MaterialTheme.typography.labelSmall, color = NeonBlue, fontWeight = FontWeight.SemiBold)
                Text("2.0×", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StorageInfoCard(info: Triple<Long, Long, Long>) {
    val (total, used, free) = info
    val frac = if (total > 0) used.toFloat() / total.toFloat() else 0f
    val ext = MaterialTheme.extended
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("💾 Device Storage", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text("${formatGb(free)} free", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(ext.glassBorder)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(CircleShape).background(Brush.horizontalGradient(listOf(NeonBlue, NeonPurple))))
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Used: ${formatGb(used)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Total: ${formatGb(total)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CacheClearCard(sizeLabel: String, cleared: Boolean, onClear: () -> Unit) {
    val ext = MaterialTheme.extended
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🗑️", fontSize = 22.sp)
        Column(Modifier.weight(1f)) {
            Text("Clear Cache", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(if (cleared) "Cache cleared ✓" else sizeLabel, style = MaterialTheme.typography.labelSmall, color = if (cleared) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (cleared) NeonGreen.copy(0.1f) else NeonPink.copy(0.1f)).border(1.dp, if (cleared) NeonGreen.copy(0.3f) else NeonPink.copy(0.3f), RoundedCornerShape(8.dp)).clickable(enabled = !cleared, onClick = onClear).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(if (cleared) "Done" else "Clear", style = MaterialTheme.typography.labelSmall, color = if (cleared) NeonGreen else NeonPink, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PremiumToggleRow(icon: String, label: String, sublabel: String, isOn: Boolean, onToggle: (Boolean) -> Unit) {
    val ext = MaterialTheme.extended
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(sublabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = isOn, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonBlue, uncheckedTrackColor = ext.glassBorder))
    }
}

@Composable
private fun AccessibilityCard(onClick: () -> Unit) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
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
            Text("♿", fontSize = 20.sp)
        }
        Column(Modifier.weight(1f)) {
            Text("Accessibility Settings", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text("Configure visual, audio and interactive tools", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegalRow(icon: String, label: String, url: String, ctx: Context) {
    val ext = MaterialTheme.extended
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp))
        .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
        .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegalRowCompose(icon: String, label: String, onClick: () -> Unit) {
    val ext = MaterialTheme.extended
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppInfoCard() {
    val ext = MaterialTheme.extended
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("👁️ Smart Vision AI", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text("Version 1.0.0 (Build 100)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Kotlin + Jetpack Compose + Gemini AI", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
            Spacer(Modifier.height(4.dp))
            Text("Made with ❤️ for AI enthusiasts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

private fun getStorageInfo(): Triple<Long, Long, Long> = runCatching {
    val stat = StatFs(android.os.Environment.getDataDirectory().path)
    val total = stat.blockCountLong * stat.blockSizeLong
    val free  = stat.availableBlocksLong * stat.blockSizeLong
    Triple(total, total - free, free)
}.getOrDefault(Triple(64L shl 30, 20L shl 30, 44L shl 30))
private fun formatGb(b: Long): String = if (b > 1L shl 30) String.format("%.1f GB", b / (1L shl 30).toDouble()) else "${b shr 20} MB"
private fun getAppCacheSize(ctx: Context): String = runCatching { val s = ctx.cacheDir.walkTopDown().sumOf { it.length() }; if (s > 1 shl 20) String.format("%.1f MB", s / (1024.0 * 1024)) else "${s shr 10} KB" }.getOrDefault("< 1 MB")
private fun clearCache(ctx: Context) { runCatching { ctx.cacheDir.deleteRecursively(); ctx.cacheDir.mkdir() } }

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val ext = MaterialTheme.extended
    val scope = rememberCoroutineScope()

    val profile by authViewModel.profile.collectAsStateWithLifecycle()
    val provider = remember(profile) { authViewModel.getLoginProvider() }
    var showEditDialog by remember { mutableStateOf(false) }

    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle()
    val autoSave by settingsViewModel.autoSave.collectAsStateWithLifecycle()
    val fontSize by settingsViewModel.fontSize.collectAsStateWithLifecycle()
    val voiceSpeed by settingsViewModel.voiceSpeed.collectAsStateWithLifecycle()
    val notifEnabled by settingsViewModel.notifEnabled.collectAsStateWithLifecycle()
    val hapticEnabled by settingsViewModel.hapticEnabled.collectAsStateWithLifecycle()

    val autoDeletePeriod by settingsViewModel.autoDeletePeriod.collectAsStateWithLifecycle()
    val autoDeleteOcr by settingsViewModel.autoDeleteOcr.collectAsStateWithLifecycle()
    val autoDeleteHomework by settingsViewModel.autoDeleteHomework.collectAsStateWithLifecycle()
    val autoDeleteQr by settingsViewModel.autoDeleteQr.collectAsStateWithLifecycle()
    val autoDeleteObject by settingsViewModel.autoDeleteObject.collectAsStateWithLifecycle()
    val autoDeleteMedicine by settingsViewModel.autoDeleteMedicine.collectAsStateWithLifecycle()
    val autoDeleteTranslation by settingsViewModel.autoDeleteTranslation.collectAsStateWithLifecycle()

    val historyState by historyViewModel.history.collectAsStateWithLifecycle()
    val scansCount = remember(historyState) { historyState.count { it.type == "OCR" || it.type == "Object" || it.type == "Medicine" || it.type == "Waste" } }
    val translationsCount = remember(historyState) { historyState.count { it.type == "Translation" || it.type == "Voice" } }
    val chatsCount = remember(historyState) { historyState.count { it.type == "Chat" } }
    val docsCount = remember(historyState) { historyState.count { it.type == "Document" } }

    var cacheCleared by remember { mutableStateOf(false) }
    val cacheSize = remember(cacheCleared) { getAppCacheSize(ctx) }
    val storageInfo = remember { getStorageInfo() }

    var expandedSection by remember { mutableStateOf<String?>(null) }
    fun toggleSection(sec: String) {
        expandedSection = if (expandedSection == sec) null else sec
    }

    val inf = rememberInfiniteTransition(label = "profile")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "rot")
    val glow by inf.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "g")

    val avatarLetter = (profile.name.firstOrNull() ?: 'A').toString().uppercase()

    if (showEditDialog) {
        EditProfileDialog(
            currentName = profile.name,
            currentEmail = profile.email,
            currentPhotoUrl = profile.photoUrl,
            isGoogleUser = provider == "Google Sign-In",
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail, newPhotoUrl ->
                authViewModel.updateUserProfile(newName, newEmail, newPhotoUrl) {
                    showEditDialog = false
                }
            }
        )
    }

    GradientBackground {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp)).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("👤 Profile Intelligence", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Smart Vision AI Workspace", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                Box(Modifier.size(130.dp), Alignment.Center) {
                    Box(modifier = Modifier.size(130.dp).graphicsLayer(rotationZ = rot).clip(CircleShape).background(Brush.sweepGradient(listOf(NeonBlue, NeonPurple, NeonCyan, NeonBlue))))
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .clip(CircleShape)
                            .background(if (ext.isDark) Color(0xFF060D1F) else Color.White)
                            .clickable { showEditDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        val presetEmoji = remember(profile.photoUrl) { getPresetEmoji(profile.photoUrl) }
                        if (presetEmoji.isNotEmpty()) {
                            Text(presetEmoji, fontSize = 54.sp)
                        } else if (!profile.photoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = profile.photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(avatarLetter, fontSize = 48.sp, color = NeonBlue, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Box(Modifier.size(130.dp).clip(CircleShape).background(NeonBlue.copy(glow * 0.05f)))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonBlue, NeonPurple)))
                            .border(1.dp, Color.White.copy(0.4f), CircleShape)
                            .clickable { showEditDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✏️", fontSize = 14.sp, color = Color.White)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(profile.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    Text(profile.email, style = MaterialTheme.typography.bodySmall, color = NeonBlue)
                    Spacer(Modifier.height(6.dp))
                    NeonBadge("Pro Member", NeonPurple)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(20.dp)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat("Scans", scansCount.toString(), NeonBlue)
                    Box(Modifier.width(1.dp).height(40.dp).background(ext.glassBorder))
                    ProfileStat("Translates", translationsCount.toString(), NeonPurple)
                    Box(Modifier.width(1.dp).height(40.dp).background(ext.glassBorder))
                    ProfileStat("AI Chats", chatsCount.toString(), NeonCyan)
                    Box(Modifier.width(1.dp).height(40.dp).background(ext.glassBorder))
                    ProfileStat("Docs", docsCount.toString(), NeonGreen)
                }

                ProfileSectionCard(title = "Account Security & Credentials", icon = "🛡️", isExpanded = expandedSection == "ACCOUNT", onHeaderClick = { toggleSection("ACCOUNT") }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccountDetailRow("Authentication Type", provider)
                        AccountDetailRow("Profile Status", "Active Secure Session")
                        AccountDetailRow("Local Database Sync", "Connected Room API")
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonBlue.copy(0.12f))
                                .border(1.dp, NeonBlue.copy(0.3f), RoundedCornerShape(10.dp))
                                .clickable { showEditDialog = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✏️ Edit Profile Info", style = MaterialTheme.typography.bodyMedium, color = NeonBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                ProfileSectionCard(title = "App Customization", icon = "🎨", isExpanded = expandedSection == "PREF", onHeaderClick = { toggleSection("PREF") }) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemeToggleCard(isDarkTheme = darkMode, onToggle = { settingsViewModel.setDarkMode(!darkMode) })
                        FontSizeCard(selected = fontSize, onSelect = { settingsViewModel.setFontSize(it) })
                        PremiumToggleRow("📥", "Auto-save Scans", "Store captures directly to gallery", autoSave) { settingsViewModel.setAutoSave(it) }
                    }
                }

                ProfileSectionCard(title = "Accessibility Options", icon = "♿", isExpanded = expandedSection == "ACCESS", onHeaderClick = { toggleSection("ACCESS") }) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        VoiceSpeedCard(speed = voiceSpeed, onSpeedChange = { settingsViewModel.setVoiceSpeed(it) })
                        AccessibilityCard { onNavigate(com.smartvision.ai.compose.navigation.Routes.ACCESSIBILITY) }
                    }
                }

                ProfileSectionCard(title = "Notifications & Haptics", icon = "🔔", isExpanded = expandedSection == "NOTIF", onHeaderClick = { toggleSection("NOTIF") }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PremiumToggleRow("🔔", "Push Notifications", "Alerts on scanned results & AI updates", notifEnabled) { settingsViewModel.setNotifEnabled(it) }
                        PremiumToggleRow("📳", "Haptic Feedback", "Tactile pulse on scans", hapticEnabled) { settingsViewModel.setHapticEnabled(it) }
                    }
                }

                ProfileSectionCard(title = "Local Cache & Disk Space", icon = "💾", isExpanded = expandedSection == "STORAGE", onHeaderClick = { toggleSection("STORAGE") }) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CacheClearCard(sizeLabel = cacheSize, cleared = cacheCleared) { scope.launch { clearCache(ctx); cacheCleared = true } }
                        StorageInfoCard(info = storageInfo)
                    }
                }

                ProfileSectionCard(title = "History Pruning & Policies", icon = "🧹", isExpanded = expandedSection == "PRIVACY", onHeaderClick = { toggleSection("PRIVACY") }) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RetentionPeriodCard(selectedPeriod = autoDeletePeriod) { settingsViewModel.setAutoDeletePeriod(it) }
                        if (autoDeletePeriod != "NEVER") {
                            PremiumToggleRow("📝", "Prune OCR Scans", "OCR scan history auto-clean", autoDeleteOcr) { settingsViewModel.setAutoDeleteOcr(it) }
                            PremiumToggleRow("📚", "Prune Homework", "Homework scans auto-clean", autoDeleteHomework) { settingsViewModel.setAutoDeleteHomework(it) }
                            PremiumToggleRow("📷", "Prune QR Scans", "QR/barcodes auto-clean", autoDeleteQr) { settingsViewModel.setAutoDeleteQr(it) }
                            PremiumToggleRow("🔍", "Prune Objects", "Object detector auto-clean", autoDeleteObject) { settingsViewModel.setAutoDeleteObject(it) }
                            PremiumToggleRow("💊", "Prune Medicine", "Medicine database auto-clean", autoDeleteMedicine) { settingsViewModel.setAutoDeleteMedicine(it) }
                            PremiumToggleRow("🌐", "Prune Translations", "Translation history auto-clean", autoDeleteTranslation) { settingsViewModel.setAutoDeleteTranslation(it) }
                        }
                        HorizontalDivider(color = ext.glassBorder, thickness = 0.8.dp)
                        LegalRowCompose("🔒", "Privacy Policy") { onNavigate(com.smartvision.ai.compose.navigation.Routes.PRIVACY_POLICY) }
                        LegalRowCompose("📄", "Terms & Conditions") { onNavigate(com.smartvision.ai.compose.navigation.Routes.TERMS_CONDITIONS) }
                    }
                }

                ProfileSectionCard(title = "Support Desk & Project Info", icon = "💬", isExpanded = expandedSection == "SUPPORT", onHeaderClick = { toggleSection("SUPPORT") }) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegalRow("💬", "Send Feedback Report", "mailto:support@smartvisionai.app", ctx)
                        LegalRow("⭐", "Rate on Google Play Store", "https://play.google.com/store", ctx)
                        HorizontalDivider(color = ext.glassBorder, thickness = 0.8.dp)
                        AppInfoCard()
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, NeonPink.copy(0.4f), RoundedCornerShape(16.dp)).clickable { onLogout() }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text("🚪 Sign Out Workspace Session", style = MaterialTheme.typography.titleSmall, color = NeonPink, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(32.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun ProfileSectionCard(title: String, icon: String, isExpanded: Boolean, onHeaderClick: () -> Unit, content: @Composable () -> Unit) {
    val ext = MaterialTheme.extended
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "arrowRot")
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).animateContentSize()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().clickable { onHeaderClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(icon, fontSize = 20.sp)
                    Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                Text(text = "▼", color = NeonBlue, fontSize = 11.sp, modifier = Modifier.graphicsLayer(rotationZ = rotation))
            }
            if (isExpanded) {
                HorizontalDivider(color = ext.glassBorder, thickness = 1.dp)
                Box(modifier = Modifier.padding(14.dp)) { content() }
            }
        }
    }
}

@Composable
private fun AccountDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RowScope.ProfileStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActivityCard() {
    val ext = MaterialTheme.extended
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("📊 Recent Activity", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            listOf(
                Triple("📝 OCR Scan",        "Detected 142 characters",  "2 min ago"),
                Triple("🌐 Translation",     "EN → HI · 'Hello World'",  "15 min ago"),
                Triple("📄 Document",        "Saved: Report.pdf · 3 pg", "1 hr ago"),
                Triple("🤖 AI Chat",         "Solved quadratic equation", "3 hrs ago")
            ).forEach { (icon, desc, time) ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 18.sp)
                    Column(Modifier.weight(1f)) {
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ModulesUsedCard() {
    val ext = MaterialTheme.extended
    val modules = listOf("📝 OCR" to NeonBlue, "🌐 Translate" to NeonPurple, "♻️ Waste" to NeonGreen, "📄 Docs" to NeonOrange, "🎙️ Voice" to NeonCyan, "📷 QR" to NeonPink)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🧩 Modules Used", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                modules.forEach { (label, color) ->
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(0.12f)).border(1.dp, color.copy(0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ABOUT SCREEN — Premium
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ext = MaterialTheme.extended
    GradientBackground {
        Column(Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp)).clickable(onClick = onBack), Alignment.Center) { Text("←", color = MaterialTheme.colorScheme.onSurface) }
                Text("ℹ️ About", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("👁️", fontSize = 64.sp)
                Text("Smart Vision AI", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text("Your All-in-One AI Vision Assistant", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🛠️ Technology Stack", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        listOf("Kotlin + Jetpack Compose", "Gemini AI (Generative AI)", "ML Kit (OCR, Translation, Barcode)", "CameraX real-time preview", "TensorFlow Lite vision models", "Firebase Auth & Cloud Storage", "Room database (local history)", "Hilt dependency injection").forEach {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("→", color = NeonBlue, fontWeight = FontWeight.Bold)
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📦 Modules (8)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        listOf("📝 OCR Text Translator", "🔍 Advanced Object Detector", "📚 AI Student Helper", "♻️ Smart Waste Classifier", "📷 QR & Barcode Scanner", "🌐 Smart Text Translator", "🎙️ Voice Translator", "📄 Smart Document Scanner").forEach { mod ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NeonBadge("✓", NeonBlue); Text(mod, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface) }
                        }
                    }
                }

                Text("Version 1.0.0 • Build 100\nMade with ❤️ using Kotlin & Jetpack Compose", style = MaterialTheme.typography.bodySmall, color = ext.textHint, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun RetentionPeriodCard(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val ext = MaterialTheme.extended
    val periods = listOf(
        "NEVER" to "Never",
        "3_DAYS" to "3 Days",
        "1_WEEK" to "1 Wk",
        "1_MONTH" to "1 Mo"
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
            Text(
                "⏱️ Retention Period",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                periods.forEach { (value, label) ->
                    val sel = selectedPeriod == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (sel) Modifier
                                    .background(NeonBlue.copy(0.15f))
                                    .border(2.dp, NeonBlue, RoundedCornerShape(10.dp))
                                else Modifier
                                    .background(ext.glassCard)
                                    .border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp))
                            )
                            .clickable { onPeriodSelected(value) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sel) NeonBlue else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun getPresetEmoji(photoUrl: String?): String {
    if (photoUrl == null) return ""
    if (!photoUrl.startsWith("preset:")) return ""
    return when (photoUrl.substringAfter("preset:")) {
        "cyborg" -> "👾"
        "robot" -> "🤖"
        "dna" -> "🧬"
        "stardust" -> "✨"
        "lens" -> "👁️"
        "synth" -> "🔮"
        else -> "🤖"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentName: String,
    currentEmail: String,
    currentPhotoUrl: String?,
    isGoogleUser: Boolean,
    onDismiss: () -> Unit,
    onSave: (newName: String, newEmail: String, newPhotoUrl: String?) -> Unit
) {
    val ctx = LocalContext.current
    val ext = MaterialTheme.extended

    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    var tempPhotoUrl by remember { mutableStateOf(currentPhotoUrl) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                runCatching {
                    ctx.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                tempPhotoUrl = uri.toString()
            }
        }
    )

    val presets = listOf(
        "cyborg" to "👾",
        "robot" to "🤖",
        "dna" to "🧬",
        "stardust" to "✨",
        "lens" to "👁️",
        "synth" to "🔮"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(24.dp))
            .background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "✏️ Update Profile",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Modify your workspace identity",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonBlue
                    )
                }

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(ext.glassCard)
                        .border(2.dp, NeonBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val emoji = remember(tempPhotoUrl) { getPresetEmoji(tempPhotoUrl) }
                    if (emoji.isNotEmpty()) {
                        Text(emoji, fontSize = 42.sp)
                    } else if (!tempPhotoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = tempPhotoUrl,
                            contentDescription = "Preview photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val init = (name.firstOrNull() ?: 'A').toString().uppercase()
                        Text(init, fontSize = 32.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ext.glassCard)
                        .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Display Name",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonBlue,
                                    unfocusedBorderColor = ext.glassBorder,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Email Address",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isGoogleUser,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = if (isGoogleUser) Color.Gray else Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonBlue,
                                    unfocusedBorderColor = ext.glassBorder,
                                    disabledBorderColor = ext.glassBorder.copy(0.5f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            if (isGoogleUser) {
                                Text(
                                    "⚠️ Email linked via Google Sign-In",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonPurple
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Choose Avatar or Pick Photo",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isCustomSelected = tempPhotoUrl != null && !tempPhotoUrl!!.startsWith("preset:")
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCustomSelected) NeonPurple.copy(0.2f) else ext.glassCard)
                                .border(
                                    1.dp,
                                    if (isCustomSelected) NeonPurple else ext.glassBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🖼️", fontSize = 20.sp)
                        }

                        presets.forEach { (key, emoji) ->
                            val presetUrl = "preset:$key"
                            val isSelected = tempPhotoUrl == presetUrl
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NeonBlue.copy(0.2f) else ext.glassCard)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonBlue else ext.glassBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { tempPhotoUrl = presetUrl },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }

                        val isNoneSelected = tempPhotoUrl == null
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNoneSelected) NeonBlue.copy(0.2f) else ext.glassCard)
                                .border(
                                    1.dp,
                                    if (isNoneSelected) NeonBlue else ext.glassBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { tempPhotoUrl = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("❌", fontSize = 16.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ext.glassCard)
                            .border(1.dp, ext.glassBorder, RoundedCornerShape(12.dp))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(NeonBlue, NeonPurple)))
                            .border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(12.dp))
                            .clickable {
                                if (name.isNotBlank()) {
                                    onSave(name, email, tempPhotoUrl)
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Save Changes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

