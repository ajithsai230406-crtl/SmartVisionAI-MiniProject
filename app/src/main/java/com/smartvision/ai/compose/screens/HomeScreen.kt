package com.smartvision.ai.compose.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.compose.navigation.Routes
import com.smartvision.ai.presentation.history.HistoryViewModel
import com.smartvision.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ── 9 modules only — AI Assistant is the floating orb, NOT a card ─────────────
private data class QuickModule(
    val title: String,
    val emoji: String,
    val accent: Color,
    val route: String
)

private val modules = listOf(
    QuickModule("Object\nDetector",  "🔍", AccentObject,   Routes.OBJECT_DETECTOR_V2),
    QuickModule("OCR\nTranslator",   "📝", AccentOCR,      Routes.OCR_TRANSLATOR),
    QuickModule("Student\nHelper",   "📚", AccentStudent,  Routes.STUDENT_HELPER_V2),
    QuickModule("Waste\nClassifier", "♻️", AccentWaste,    Routes.WASTE_CLASSIFIER_V2),
    QuickModule("Voice\nTranslator", "🎙️", AccentVoice,    Routes.VOICE_TRANSLATOR_V2),
    QuickModule("Text\nTranslator",  "🌐", AccentTranslate,Routes.TRANSLATOR_V2),
    QuickModule("QR\nScanner",       "📷", AccentQR,       Routes.QR_SCANNER_V2),
    QuickModule("Document\nScanner", "📄", AccentDocument, Routes.DOCUMENT_SCANNER),
    QuickModule("Medicine\nScanner", "💊", AccentMedicine, Routes.MEDICINE_SCANNER)
)

private fun greetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning!"
        hour < 17 -> "Good afternoon!"
        else      -> "Good evening!"
    }
}

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    historyViewModel: HistoryViewModel = hiltViewModel()
) {
    val ext          = MaterialTheme.extended
    val historyItems by historyViewModel.history.collectAsStateWithLifecycle()
    var searchQuery  by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "homeBg")
    val bgGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.14f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bgGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        // ── Ambient background glow orbs ──────────────────────────────────
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset((-100).dp, (-80).dp)
                .clip(CircleShape)
                .background(NeonBlue.copy(alpha = bgGlowAlpha))
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(80.dp, 60.dp)
                .clip(CircleShape)
                .background(NeonPurple.copy(alpha = bgGlowAlpha * 0.8f))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomStart)
                .offset((-40).dp, 40.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = bgGlowAlpha * 0.5f))
        )

        LazyColumn(
            modifier        = Modifier.fillMaxSize(),
            contentPadding  = PaddingValues(bottom = 120.dp)
        ) {
            // ── Greeting Header ───────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Hi, Arjun 👋",
                            style      = MaterialTheme.typography.headlineSmall,
                            color      = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            greetingText(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(NeonBlue.copy(0.6f), NeonPurple))
                            )
                            .border(2.dp, NeonBlue.copy(0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "A",
                            color      = Color.White,
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // ── Search Bar ────────────────────────────────────────────────
            item {
                SmartSearchBar(
                    query         = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier      = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    trailingIcon  = {
                        Text(
                            "🎙️",
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable { onNavigate(Routes.VOICE_TRANSLATOR_V2) }
                        )
                    }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }

            // ── Quick Access Header ───────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "Quick Access",
                        style      = MaterialTheme.typography.titleMedium,
                        color      = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                    // Module count badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(NeonBlue.copy(alpha = 0.12f))
                            .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "9 Modules",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = NeonBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(14.dp)) }

            // ── 3×3 Module Grid — exactly 9 modules, NO AI Assistant ──────
            item {
                val filteredModules = if (searchQuery.isBlank()) modules
                else modules.filter {
                    it.title.lowercase().replace("\n", " ")
                        .contains(searchQuery.lowercase())
                }

                Column(
                    modifier            = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filteredModules.chunked(3).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier              = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { module ->
                                ModuleCard(
                                    title       = module.title,
                                    icon        = {
                                        Text(
                                            module.emoji,
                                            fontSize = 26.sp
                                        )
                                    },
                                    accentColor = module.accent,
                                    onClick     = { onNavigate(module.route) },
                                    modifier    = Modifier.weight(1f)
                                )
                            }
                            // Fill empty slots in last row
                            repeat(3 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(28.dp)) }

            // ── Recent Activity Header ────────────────────────────────────
            item {
                SectionHeader(
                    title         = "Recent Activity",
                    onActionClick = { onNavigate(Routes.HISTORY) },
                    modifier      = Modifier.padding(horizontal = 20.dp)
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            // ── Activity Items ────────────────────────────────────────────
            val recent = historyItems.take(5)
            if (recent.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(NavyCard)
                            .border(1.dp, ext.glassBorder, RoundedCornerShape(18.dp))
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔮", fontSize = 40.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Start scanning to see activity here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recent) { item ->
                    val accentColor = when (item.type) {
                        "OCR"      -> AccentOCR
                        "Chat"     -> AccentAI
                        "Waste"    -> AccentWaste
                        "Medicine" -> AccentMedicine
                        "QR"       -> AccentQR
                        "Document" -> AccentDocument
                        else       -> NeonBlue
                    }
                    val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                    HistoryItemRow(
                        type        = item.type,
                        title       = item.title,
                        subtitle    = item.details,
                        accentColor = accentColor,
                        timestamp   = fmt.format(item.createdAt),
                        modifier    = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Floating AI Assistant Orb — bottom-right, above nav bar ──────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp)
                .navigationBarsPadding()
        ) {
            FloatingAiOrb(onClick = { onNavigate(Routes.AI_ASSISTANT) })
        }
    }
}

// ── HomeScreen-local HistoryItemRow wrapper (delegates to component) ──────────
@Composable
fun HistoryItemRow(
    type: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    timestamp: String,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val cardBg = if (ext.isDark) NavyCard else ext.glassCard
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(0.15f))
                .border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                type.take(2).uppercase(),
                style      = MaterialTheme.typography.labelSmall,
                color      = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style    = MaterialTheme.typography.titleSmall,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                subtitle,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(timestamp, style = MaterialTheme.typography.labelSmall, color = ext.textHint)
    }
}
