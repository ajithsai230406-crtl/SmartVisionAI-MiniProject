package com.smartvision.ai.compose.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.domain.model.FeedbackEntry
import com.smartvision.ai.presentation.feedback.FeedbackViewModel
import com.smartvision.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFeedbackScreen(
    onBack: () -> Unit,
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    val ext = MaterialTheme.extended
    val context = LocalContext.current

    val rawList by viewModel.allFeedback.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingAdmin.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortByNewest by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.loadAllFeedback()
    }

    // Filtered and sorted feedback list
    val processedList = remember(rawList, searchQuery, selectedCategory, sortByNewest) {
        var list = rawList
        
        // 1. Search filter (email or subject)
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            list = list.filter { it.email.lowercase().contains(q) || it.subject.lowercase().contains(q) }
        }

        // 2. Category filter
        if (selectedCategory != "All") {
            list = list.filter { it.category == selectedCategory }
        }

        // 3. Sorting
        if (sortByNewest) {
            list.sortedByDescending { it.timestamp }
        } else {
            list.sortedBy { it.timestamp }
        }
    }

    GradientBackground {
        Column(Modifier.fillMaxSize()) {
            // Header Top Bar
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
                    Text("🛡️ Feedback Admin Panel", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Moderate user submitted reports", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
                }
            }

            // Search Bar & Filter Options
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmartSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search by email or subject..."
                )

                // Category Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("All", "Bug Report", "Feature Request", "General Feedback", "UI/UX Suggestion", "Performance Issue").forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonBlue.copy(0.15f) else ext.glassCard)
                                .border(1.dp, if (isSelected) NeonBlue.copy(0.4f) else ext.glassBorder, RoundedCornerShape(10.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(cat, style = MaterialTheme.typography.labelSmall, color = if (isSelected) NeonBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Sort Options & Count Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${processedList.size} items found",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ext.glassCard)
                            .border(1.dp, ext.glassBorder, RoundedCornerShape(8.dp))
                            .clickable { sortByNewest = !sortByNewest }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (sortByNewest) "⇅ Newest First" else "⇅ Oldest First",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Body List
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonBlue)
                    }
                } else if (processedList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔍", fontSize = 48.sp)
                            Text("No reports match your filters.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(processedList) { entry ->
                            AdminFeedbackCard(
                                entry = entry,
                                onToggleStatus = {
                                    val nextStatus = if (entry.status == "Pending") "Resolved" else "Pending"
                                    viewModel.updateFeedbackStatus(entry.feedbackId, nextStatus)
                                },
                                onDelete = {
                                    viewModel.deleteFeedbackEntry(entry.feedbackId)
                                },
                                onOpenUrl = { url ->
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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

@Composable
private fun AdminFeedbackCard(
    entry: FeedbackEntry,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val ext = MaterialTheme.extended
    val dateStr = remember(entry.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(entry.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.glassCard)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Category & Status Switch
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(entry.category, style = MaterialTheme.typography.labelSmall, color = NeonPurple, fontWeight = FontWeight.Bold)
                
                val isResolved = entry.status == "Resolved"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isResolved) NeonGreen.copy(0.12f) else NeonOrange.copy(0.12f))
                        .border(1.dp, if (isResolved) NeonGreen.copy(0.4f) else NeonOrange.copy(0.4f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleStatus)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${entry.status} ⇅",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isResolved) NeonGreen else NeonOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // User Info
            Column {
                Text(entry.userName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(entry.email, style = MaterialTheme.typography.bodySmall, color = NeonBlue)
            }

            // Subject & Msg
            Text(entry.subject, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(entry.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Rating & Attachments
            Row(verticalAlignment = Alignment.CenterVertically) {
                (1..5).forEach { star ->
                    Text(
                        text = if (entry.rating >= star) "★" else "☆",
                        fontSize = 16.sp,
                        color = if (entry.rating >= star) AccentQR else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!entry.imageUrl.isNullOrBlank()) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "📎 Open Attachment ↗",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenUrl(entry.imageUrl) }
                    )
                }
            }

            HorizontalDivider(color = ext.glassBorder, thickness = 0.8.dp)

            // Diagnostics Details
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Device: ${entry.deviceInfo}", style = MaterialTheme.typography.labelSmall, color = ext.textHint)
                Text("App Version: ${entry.appVersion}", style = MaterialTheme.typography.labelSmall, color = ext.textHint)
            }

            // Footer
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = ext.textHint)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonPink.copy(0.12f))
                        .border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Delete", style = MaterialTheme.typography.labelSmall, color = NeonPink, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
