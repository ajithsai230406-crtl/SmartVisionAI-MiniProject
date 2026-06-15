package com.smartvision.ai.compose.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.GradientBackground
import com.smartvision.ai.compose.components.NeonBadge
import com.smartvision.ai.presentation.settings.SettingsViewModel
import com.smartvision.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val ext = MaterialTheme.extended

    val notifEnabled by viewModel.notifEnabled.collectAsStateWithLifecycle()
    val recommendationsEnabled by viewModel.recommendationsEnabled.collectAsStateWithLifecycle()
    val quietHoursEnabled by viewModel.quietHoursEnabled.collectAsStateWithLifecycle()
    val quietHoursStart by viewModel.quietHoursStart.collectAsStateWithLifecycle()
    val quietHoursEnd by viewModel.quietHoursEnd.collectAsStateWithLifecycle()

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
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = MaterialTheme.colorScheme.onSurface)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "🔔 Notification Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Configure alerts and silent hours",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.neonBlue
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Section 1: Global Settings
                NotificationSectionHeader("🔔 Global Notifications", "Control system notifications")

                // Push Notifications Global Toggle
                NotificationSwitchCard(
                    icon = "🔔",
                    title = "Push Notifications",
                    sub = "Receive notifications from camera scan alerts and tips",
                    checked = notifEnabled,
                    onCheckedChange = { viewModel.setNotifEnabled(it) }
                )

                // Recommendations Toggle
                NotificationSwitchCard(
                    icon = "💡",
                    title = "Intelligent Recommendations",
                    sub = "Suggest unused modules or study tasks based on usage",
                    checked = recommendationsEnabled,
                    onCheckedChange = { viewModel.setRecommendationsEnabled(it) }
                )

                // Section 2: Quiet Hours
                NotificationSectionHeader("🌙 Quiet Hours", "Prevent notifications during rest periods")

                // Quiet Hours Enable
                NotificationSwitchCard(
                    icon = "🌙",
                    title = "Enable Quiet Hours",
                    sub = "Silence reminders during specified intervals",
                    checked = quietHoursEnabled,
                    onCheckedChange = { viewModel.setQuietHoursEnabled(it) }
                )

                // Quiet Hours Range Selector
                if (quietHoursEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ext.glassCard)
                            .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Silence Window",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            HourSelectorRow("Start Hour", quietHoursStart) { viewModel.setQuietHoursStart(it) }
                            HourSelectorRow("End Hour", quietHoursEnd) { viewModel.setQuietHoursEnd(it) }
                        }
                    }
                }

                // Section 3: Diagnostic Tests
                NotificationSectionHeader("🛠️ Diagnostics & Testing", "Preview notifications instantly")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ext.glassCard)
                        .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Preview Suggestion Alert",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Sends a test notification immediately to preview how intelligent recommendation alerts appear on your lock screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(ext.neonBlue, ext.neonPurple)))
                                .clickable { fireDemoNotification(ctx) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "🔔 Test Notification Alert",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun NotificationSectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NotificationSwitchCard(
    icon: String,
    title: String,
    sub: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val ext = MaterialTheme.extended
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
private fun HourSelectorRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ext.glassCard)
                    .border(1.dp, ext.glassBorder, CircleShape)
                    .clickable { if (value > 0) onValueChange(value - 1) },
                contentAlignment = Alignment.Center
            ) {
                Text("-", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(
                text = formatHour(value),
                style = MaterialTheme.typography.bodyMedium,
                color = ext.neonBlue,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ext.glassCard)
                    .border(1.dp, ext.glassBorder, CircleShape)
                    .clickable { if (value < 23) onValueChange(value + 1) },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour == 12 -> "12 PM"
        hour > 12 -> "${hour - 12} PM"
        else -> "$hour AM"
    }
}

private fun fireDemoNotification(context: Context) {
    val channelId = "sv_recommendations"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Smart Recommendations", NotificationManager.IMPORTANCE_DEFAULT)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("📚 AI Student Tutor (Demo)")
        .setContentText("Stuck on a tricky problem? Let the AI Student Helper guide you step-by-step!")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    val manager = NotificationManagerCompat.from(context)
    try {
        manager.notify(9999, builder.build())
    } catch (e: SecurityException) {
        // ignore
    }
}
