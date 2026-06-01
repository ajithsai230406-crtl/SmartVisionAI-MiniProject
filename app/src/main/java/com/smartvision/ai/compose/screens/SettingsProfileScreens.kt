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

// ══════════════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN — Premium Full-Featured
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
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

                SettingsSection("♿ Accessibility")
                AccessibilityCard()

                SettingsSection("📋 Legal & Support")
                LegalRow("🔒", "Privacy Policy",      "https://smartvisionai.app/privacy",     ctx)
                LegalRow("📄", "Terms & Conditions",  "https://smartvisionai.app/terms",        ctx)
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
private fun AccessibilityCard() {
    val ext = MaterialTheme.extended
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("✅ Screen reader (TalkBack) compatible", "✅ High contrast mode support", "✅ Large text scaling", "✅ Reduced motion respected", "✅ Voice input everywhere").forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
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

// ══════════════════════════════════════════════════════════════════════════════
// PROFILE SCREEN — Premium Animated
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val ext = MaterialTheme.extended
    val inf = rememberInfiniteTransition(label = "profile")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "rot")
    val glow by inf.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "g")

    GradientBackground {
        Column(Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp)).clickable(onClick = onBack), Alignment.Center) { Text("←", color = MaterialTheme.colorScheme.onSurface) }
                Column {
                    Text("👤 Profile", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Smart Vision AI Member", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
                }
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))

                // Animated avatar with rotating gradient ring
                Box(Modifier.size(130.dp), Alignment.Center) {
                    // Outer rotating ring
                    Box(Modifier.size(130.dp).graphicsLayer(rotationZ = rot)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(NeonBlue, NeonPurple, NeonCyan, NeonBlue)))
                    )
                    // Inner dark circle
                    Box(Modifier.size(116.dp).clip(CircleShape).background(if (ext.isDark) Color(0xFF060D1F) else Color.White), Alignment.Center) {
                        Text("A", fontSize = 48.sp, color = NeonBlue, fontWeight = FontWeight.ExtraBold)
                    }
                    // Glow pulse
                    Box(Modifier.size(130.dp).clip(CircleShape).background(NeonBlue.copy(glow * 0.05f)))
                }

                Spacer(Modifier.height(14.dp))
                Text("Ajith Kumar", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                Text("Premium AI Member", style = MaterialTheme.typography.bodySmall, color = NeonBlue)
                Spacer(Modifier.height(4.dp))
                NeonBadge("✨ Pro", NeonPurple)

                Spacer(Modifier.height(24.dp))

                // Stats row
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(20.dp)).padding(16.dp), Arrangement.SpaceEvenly) {
                    ProfileStat("Scans",        "47",  NeonBlue)
                    Box(Modifier.width(1.dp).height(44.dp).background(ext.glassBorder))
                    ProfileStat("Translations", "23",  NeonPurple)
                    Box(Modifier.width(1.dp).height(44.dp).background(ext.glassBorder))
                    ProfileStat("AI Chats",     "12",  NeonCyan)
                    Box(Modifier.width(1.dp).height(44.dp).background(ext.glassBorder))
                    ProfileStat("Docs",         "8",   NeonGreen)
                }

                Spacer(Modifier.height(20.dp))

                // Activity highlights
                ActivityCard()

                Spacer(Modifier.height(20.dp))

                // Modules used
                ModulesUsedCard()

                Spacer(Modifier.height(24.dp))

                // Actions
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(NeonBlue, NeonPurple))).clickable {}.padding(vertical = 15.dp), Alignment.Center) {
                    Text("✏️ Edit Profile", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, NeonPink.copy(0.4f), RoundedCornerShape(16.dp)).clickable {}.padding(vertical = 15.dp), Alignment.Center) {
                    Text("🚪 Sign Out", style = MaterialTheme.typography.titleSmall, color = NeonPink, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(32.dp).navigationBarsPadding())
            }
        }
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
