package com.smartvision.ai.ui.screens.profile

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

data class ActivityEntry(
    val screen:  String,
    val detail:  String,
    val timeAgo: String,
    val icon:    androidx.compose.ui.graphics.vector.ImageVector,
    val accent:  Color
)

@Composable
fun ProfileScreen(
    userEmail: String = "user@smartvision.ai",
    onBack:    () -> Unit,
    onHome:    () -> Unit = onBack,
    onLogout:  () -> Unit = {}
) {
    val colors = smartColors
    val activities = remember {
        listOf(
            ActivityEntry("Home",            "Opened app",                  "2 min ago",  Icons.Rounded.Home,             colors.primary),
            ActivityEntry("Camera",          "Opened camera scanner",       "5 min ago",  Icons.Rounded.CameraAlt,        SmartVisionColors.objectDetect),
            ActivityEntry("Result",          "Viewed detection result",     "6 min ago",  Icons.Rounded.Assignment,       SmartVisionColors.studentHelper),
            ActivityEntry("OCR",             "Scanned & translated text",   "15 min ago", Icons.Rounded.TextFields,       SmartVisionColors.textScanner),
            ActivityEntry("QR Scanner",      "Decoded QR link",             "1 hr ago",   Icons.Rounded.QrCodeScanner,    SmartVisionColors.qrScanner),
            ActivityEntry("Translator",      "English → Hindi",             "2 hr ago",   Icons.Rounded.Translate,        SmartVisionColors.translator),
            ActivityEntry("History",         "Browsed scan history",        "Yesterday",  Icons.Rounded.History,          SmartVisionColors.voiceTranslator),
            ActivityEntry("Settings",        "Changed theme to Dark",       "3 days ago", Icons.Rounded.Settings,         colors.subtext)
        )
    }

    Scaffold(
        topBar         = {
            SmartVisionTopBar(
                title   = "Profile",
                onBack  = onBack,
                actions = {
                    IconButton(onClick = onHome) {
                        Icon(Icons.Rounded.Home, "Home", tint = colors.subtext, modifier = Modifier.size(22.dp))
                    }
                }
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(colors.primary.copy(0.1f), Color.Transparent)))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(Brush.linearGradient(listOf(colors.primary.copy(0.3f), colors.secondary.copy(0.3f))), CircleShape)
                            .border(3.dp, colors.primary.copy(0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Person, null, tint = colors.primary, modifier = Modifier.size(48.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineMedium, color = colors.onSurface, fontWeight = FontWeight.Bold)
                        Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = colors.subtext)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("48",  "Scans",        colors.primary)
                        StatItem("12",  "Translations", SmartVisionColors.translator)
                        StatItem("7",   "Days Active",  SmartVisionColors.studentHelper)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Screens / Activity
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("S C R E E N S", style = MaterialTheme.typography.labelLarge, color = colors.subtext, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp, modifier = Modifier.padding(bottom = 4.dp))
                activities.forEach { ActivityRow(it) }
            }

            Spacer(Modifier.height(20.dp))

            // Account
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("A C C O U N T", style = MaterialTheme.typography.labelLarge, color = colors.subtext, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp, modifier = Modifier.padding(bottom = 4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.card)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccountRow(Icons.Rounded.Edit,         "Edit Profile",       colors.primary, {})
                        HorizontalDivider(color = colors.cardBorder)
                        AccountRow(Icons.Rounded.Notifications,"Notifications",     colors.subtext, {})
                        HorizontalDivider(color = colors.cardBorder)
                        AccountRow(Icons.Rounded.Security,     "Privacy & Security",colors.subtext, {})
                        HorizontalDivider(color = colors.cardBorder)
                        AccountRow(Icons.Rounded.Logout,       "Sign Out",           colors.error,   onLogout)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable private fun StatItem(value: String, label: String, accent: Color) {
    val colors = smartColors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = accent, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium,   color = colors.subtext)
    }
}

@Composable private fun ActivityRow(a: ActivityEntry) {
    val colors = smartColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(1.dp, a.accent.copy(0.2f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(a.accent, CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(a.screen, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
            Text(a.detail, style = MaterialTheme.typography.bodySmall,  color = colors.subtext)
        }
        Text(a.timeAgo, style = MaterialTheme.typography.labelSmall, color = colors.subtext)
    }
}

@Composable private fun AccountRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    val colors = smartColors
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, tint = colors.subtext, modifier = Modifier.size(18.dp))
    }
}
