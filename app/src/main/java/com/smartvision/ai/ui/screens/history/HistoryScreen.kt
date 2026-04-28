package com.smartvision.ai.ui.screens.history

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.ui.components.*
import com.smartvision.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    onBack:      () -> Unit,
    onItemClick: (String) -> Unit,
    viewModel:   HistoryViewModel = hiltViewModel()
) {
    val colors  = smartColors
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Scaffold(
        topBar         = {
            SmartVisionTopBar(title = "History", onBack = onBack) {
                if (uiState.selectedIds.isNotEmpty()) {
                    IconButton(onClick = viewModel::deleteSelected) {
                        Icon(Icons.Rounded.Delete, null, tint = colors.error)
                    }
                }
            }
        },
        containerColor = colors.background
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
            uiState.items.isEmpty() -> {
                EmptyHistoryState()
            }
            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Group by date
                    val grouped = uiState.items.groupBy { item ->
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(item.timestampMillis))
                    }
                    grouped.forEach { (dateLabel, items) ->
                        item {
                            Text(
                                text     = dateLabel,
                                style    = MaterialTheme.typography.labelLarge,
                                color    = colors.subtext,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(items, key = { it.id }) { historyItem ->
                            HistoryCard(
                                item       = historyItem,
                                isSelected = historyItem.id in uiState.selectedIds,
                                onLongPress = { viewModel.toggleSelection(historyItem.id) },
                                onClick    = {
                                    if (uiState.selectedIds.isNotEmpty()) {
                                        viewModel.toggleSelection(historyItem.id)
                                    } else {
                                        onItemClick(historyItem.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HISTORY CARD
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(
    item:       ScanHistoryItem,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onClick:    () -> Unit
) {
    val colors     = smartColors
    val moduleType = ModuleType.entries.find { it.name == item.moduleType }
    val accent     = moduleType?.let { moduleAccentColor(it) } ?: colors.primary
    val time       = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.timestampMillis))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) accent.copy(0.12f) else colors.card)
            .border(
                1.dp,
                if (isSelected) accent.copy(0.5f) else colors.cardBorder,
                RoundedCornerShape(18.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
        ) {
            if (item.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model              = item.imageUrl,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = moduleType?.let { moduleIcon(it) } ?: Icons.Rounded.History,
                    contentDescription = null,
                    tint     = accent,
                    modifier = Modifier.align(Alignment.Center).size(30.dp)
                )
            }
        }

        // Info
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = moduleType?.title ?: item.moduleType,
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
                Text(time, style = MaterialTheme.typography.labelSmall, color = colors.subtext)
            }
            Text(
                text    = item.summary,
                style   = MaterialTheme.typography.bodySmall,
                color   = colors.onSurface,
                maxLines = 2,
            )
        }

        // Selection check
        if (isSelected) {
            Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(22.dp))
        } else {
            Icon(Icons.Rounded.ChevronRight, null, tint = colors.subtext, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMPTY STATE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyHistoryState() {
    val colors = smartColors
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(colors.card, CircleShape)
                    .border(1.dp, colors.cardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.History, null,
                    tint = colors.subtext,
                    modifier = Modifier.size(44.dp)
                )
            }
            Text("No scans yet", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
            Text(
                "Your scan history will appear here.\nStart by scanning an object or text.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.subtext
            )
        }
    }
}
