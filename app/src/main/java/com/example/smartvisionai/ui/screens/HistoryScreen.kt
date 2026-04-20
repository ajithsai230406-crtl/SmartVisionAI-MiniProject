package com.example.smartvisionai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartvisionai.data.model.ScanHistoryEntity
import com.example.smartvisionai.ui.components.ModuleIcon
import com.example.smartvisionai.ui.theme.*
import com.example.smartvisionai.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)) {
            Text("HISTORY", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                color = CyanAccent, letterSpacing = 2.sp)
            Text("Your recent scans", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No scans yet", fontSize = 16.sp, color = TextSecondary)
                    Text("Start scanning to see history", fontSize = 13.sp, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    HistoryItem(item = item, onDelete = { viewModel.deleteItem(item) })
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
fun HistoryItem(item: ScanHistoryEntity, onDelete: () -> Unit) {
    val moduleColor = when (item.moduleId) {
        "object"   -> CyanAccent
        "ocr"      -> PurpleAccent
        "translate"-> OrangeAccent
        "student"  -> Color(0xFF2196F3)
        "medical"  -> RedAccent
        "waste"    -> GreenAccent
        else       -> CyanAccent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(moduleColor.copy(alpha = 0.15f))
                .border(1.dp, moduleColor.copy(0.3f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            ModuleIcon(moduleId = item.moduleId, color = moduleColor)
        }

        Spacer(Modifier.width(14.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⏱ ${item.timeAgo}", fontSize = 11.sp, color = TextMuted)
                if (item.confidence > 0) {
                    Text(
                        "${item.confidence}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = moduleColor
                    )
                }
            }
        }

        // Delete
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete",
                tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}
