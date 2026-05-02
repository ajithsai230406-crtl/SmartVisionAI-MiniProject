package com.smartvision.ai.ui.screens.history

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
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
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    vm: HistoryViewModel = hiltViewModel()
) {
    val c = svColors
    val s by vm.uiState.collectAsState(initial = HistoryUiState())
    
    LaunchedEffect(Unit) { 
        vm.loadHistory() 
    }

    Scaffold(
        topBar = { 
            SmartVisionTopBar("History", onBack = onBack) {
                if (s.selectedIds.isNotEmpty()) {
                    IconButton(onClick = { vm.deleteSelected() }) {
                        Icon(Icons.Rounded.Delete, null, tint = c.error)
                    }
                }
            }
        },
        containerColor = c.background
    ) { pad ->
        when {
            s.isLoading -> Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) { 
                CircularProgressIndicator(color = c.primary) 
            }
            s.items.isEmpty() -> EmptyHistory(pad)
            else -> {
                val grouped = s.items.groupBy { item ->
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.timestampMillis))
                }
                LazyColumn(
                    Modifier.fillMaxSize().padding(pad), 
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    grouped.forEach { (date, items) ->
                        item {
                            Text(
                                date, 
                                style = MaterialTheme.typography.labelLarge, 
                                color = c.subtext,
                                fontWeight = FontWeight.SemiBold, 
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(items, key = { it.id }) { item ->
                            HistoryItem(
                                item = item, 
                                selected = item.id in s.selectedIds,
                                onLong = { vm.toggleSelection(item.id) },
                                onClick = { 
                                    if (s.selectedIds.isNotEmpty()) vm.toggleSelection(item.id) 
                                    else onItemClick(item.id) 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItem(
    item: ScanHistoryItem, 
    selected: Boolean, 
    onLong: () -> Unit, 
    onClick: () -> Unit
) {
    val c = svColors
    val module = ModuleType.entries.find { it.name == item.moduleType }
    val accent = module?.let { moduleAccent(it) } ?: c.primary
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.timestampMillis))

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(.1f) else c.card)
            .border(1.dp, if (selected) accent.copy(.5f) else c.border, RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLong).padding(11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(11.dp)).background(c.surface)) {
            if (item.imageUrl.isNotEmpty()) {
                AsyncImage(item.imageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Icon(
                        module?.let { moduleIcon(it) } ?: Icons.Rounded.History, 
                        null, 
                        tint = accent, 
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(
                    module?.title ?: item.moduleType, 
                    style = MaterialTheme.typography.labelLarge, 
                    color = accent, 
                    fontWeight = FontWeight.Bold
                )
                Text(time, style = MaterialTheme.typography.labelSmall, color = c.subtext)
            }
            Text(item.summary, style = MaterialTheme.typography.bodySmall, color = c.onSurface, maxLines = 2)
        }
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(20.dp))
        } else {
            Icon(Icons.Rounded.ChevronRight, null, tint = c.subtext, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EmptyHistory(pad: PaddingValues) {
    val c = svColors
    Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                Modifier.size(80.dp).background(c.card, CircleShape).border(1.dp, c.border, CircleShape), 
                Alignment.Center
            ) {
                Icon(Icons.Rounded.History, null, tint = c.subtext, modifier = Modifier.size(40.dp))
            }
            Text(
                "No scans yet", 
                style = MaterialTheme.typography.headlineSmall, 
                color = c.onSurface, 
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Your scan history will appear here.", 
                style = MaterialTheme.typography.bodyMedium, 
                color = c.subtext
            )
        }
    }
}
