package com.smartvision.ai.compose.screens

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
import com.smartvision.ai.presentation.history.HistoryViewModel
import com.smartvision.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val ext     = MaterialTheme.extended
    val items   by viewModel.history.collectAsStateWithLifecycle()
    var filter  by remember { mutableStateOf("All") }
    val filters = listOf("All", "OCR", "Chat", "Waste", "Medicine")
    val fmt     = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    val filtered = if (filter == "All") items else items.filter { it.type == filter }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartTopBar(
                title  = "History",
                onBack = onBack,
                actions = {
                    Text(
                        "Clear All",
                        color = NeonPink,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable { viewModel.clearHistory() }
                    )
                }
            )

            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(filters) { f ->
                    val isSelected = filter == f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .then(
                                if (isSelected)
                                    Modifier.background(Brush.horizontalGradient(listOf(ext.neonBlue, ext.neonPurple)))
                                else
                                    Modifier.background(ext.glassCard)
                                         .border(1.dp, ext.glassBorder, RoundedCornerShape(20.dp))
                            )
                            .clickable { filter = f }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(f, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📜", fontSize = 56.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No history yet", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered) { item ->
                        val accent = when (item.type) {
                            "OCR"      -> AccentOCR
                            "Chat"     -> AccentAI
                            "Waste"    -> AccentWaste
                            "Medicine" -> AccentMedicine
                            else       -> NeonBlue
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ext.glassCard)
                                .border(1.dp, accent.copy(0.2f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item.type.take(2).uppercase(), color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                Text(item.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                            Text(fmt.format(item.createdAt), style = MaterialTheme.typography.labelSmall, color = ext.textHint)
                        }
                    }
                }
            }
        }
    }
}
