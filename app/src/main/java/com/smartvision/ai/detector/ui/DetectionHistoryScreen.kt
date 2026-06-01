package com.smartvision.ai.detector.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.detector.domain.*
import com.smartvision.ai.detector.presentation.DetectionHistoryEntry
import com.smartvision.ai.detector.presentation.ObjectDetectorViewModel
import com.smartvision.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Shows the detection session history — all objects detected in the current session,
 * grouped by category, with stats at the top.
 */
@Composable
fun DetectionHistoryScreen(
    onBack: () -> Unit,
    viewModel: ObjectDetectorViewModel = hiltViewModel()
) {
    val history by viewModel.sessionHistory.collectAsStateWithLifecycle()
    val ext     = MaterialTheme.extended
    val fmt     = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(title = "Detection History", onBack = onBack)

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 60.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No detections yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Start scanning to see objects here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                // Stats bar
                DetectionStats(history)

                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { entry ->
                        DetectionHistoryCard(entry = entry, fmt = fmt)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectionStats(history: List<DetectionHistoryEntry>) {
    val ext = MaterialTheme.extended
    val categoryCount = history.groupingBy { it.category }.eachCount()
    val avgConf = history.map { it.confidence }.average().toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Total", "${history.size}", NeonBlue, Modifier.weight(1f))
        StatCard("Avg Conf", "${(avgConf * 100).toInt()}%", NeonGreen, Modifier.weight(1f))
        StatCard("Categories", "${categoryCount.size}", NeonPurple, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    val ext = MaterialTheme.extended
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ext.glassCard)
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetectionHistoryCard(entry: DetectionHistoryEntry, fmt: SimpleDateFormat) {
    val color = Color(entry.category.colorArgb)
    val pct   = (entry.confidence * 100).toInt()
    val know  = ObjectKnowledgeBase.getKnowledge(entry.label)
    val ext   = MaterialTheme.extended

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.glassCard)
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Emoji icon
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) { Text(know.emoji, fontSize = 24.sp) }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NeonBadge(entry.category.displayName, color)
            }
        }

        // Right side: confidence + time
        Column(horizontalAlignment = Alignment.End) {
            Text("$pct%", style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
            Text(fmt.format(entry.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
