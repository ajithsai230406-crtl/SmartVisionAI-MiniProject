package com.smartvision.ai.compose.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.domain.model.FeedbackEntry
import com.smartvision.ai.presentation.feedback.FeedbackSubmitState
import com.smartvision.ai.presentation.feedback.FeedbackViewModel
import com.smartvision.ai.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val ext = MaterialTheme.extended
    var activeTab by remember { mutableStateOf(0) } // 0 = Form, 1 = History

    val category by viewModel.category.collectAsStateWithLifecycle()
    val subject by viewModel.subject.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val rating by viewModel.rating.collectAsStateWithLifecycle()
    val attachmentUri by viewModel.attachmentUri.collectAsStateWithLifecycle()
    val attachmentName by viewModel.attachmentName.collectAsStateWithLifecycle()
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf<FeedbackEntry?>(null) }

    // Launches pickers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setAttachment(it, "image_gallery.jpg") }
    }

    val docLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.setAttachment(it, "document.pdf") }
    }

    // Temporary URI for camera
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { viewModel.setAttachment(it, "camera_capture.jpg") }
        }
    }

    fun triggerCamera() {
        val tempFile = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempFile
        )
        tempCameraUri = uri
        cameraLauncher.launch(uri)
    }

    LaunchedEffect(activeTab) {
        if (activeTab == 1) {
            viewModel.loadUserHistory()
        }
    }

    if (showEditDialog != null) {
        EditFeedbackDialog(
            entry = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onSave = { updatedEntry ->
                viewModel.updatePendingFeedback(updatedEntry)
                showEditDialog = null
            }
        )
    }

    GradientBackground {
        Column(Modifier.fillMaxSize()) {
            // Topbar
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
                    Text("💬 Help & Feedback", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Your voice shapes SVA", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
                }
            }

            // Tab row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp)).background(ext.glassCard).padding(4.dp)
            ) {
                listOf("Submit Feedback", "My History").forEachIndexed { index, title ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonBlue.copy(0.15f) else Color.Transparent)
                            .clickable { activeTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NeonBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Body
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (activeTab == 0) {
                    // Form Tab
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard {
                            Text("👤 Reporter Profile", style = MaterialTheme.typography.labelMedium, color = NeonBlue, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Name:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(viewModel.userProfile.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Email:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(viewModel.userProfile.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Category Dropdown selector
                        var expanded by remember { mutableStateOf(false) }
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                label = { Text("Category") },
                                readOnly = true,
                                trailingIcon = { Text("▼", modifier = Modifier.clickable { expanded = true }) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonBlue,
                                    unfocusedBorderColor = ext.glassBorder,
                                    focusedContainerColor = ext.glassCard,
                                    unfocusedContainerColor = ext.glassCard
                                )
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f).background(ext.cardBackground).border(1.dp, ext.glassBorder, RoundedCornerShape(12.dp))
                            ) {
                                listOf("Bug Report", "Feature Request", "General Feedback", "UI/UX Suggestion", "Performance Issue").forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            viewModel.category.value = cat
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Subject
                        SmartTextField(
                            value = subject,
                            onValueChange = { viewModel.subject.value = it },
                            placeholder = "Subject of feedback",
                            singleLine = true
                        )

                        // Message body
                        OutlinedTextField(
                            value = message,
                            onValueChange = { viewModel.message.value = it },
                            placeholder = { Text("Describe your feedback detail...", color = ext.textHint) },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            shape = RoundedCornerShape(14.dp),
                            maxLines = 10,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = ext.glassBorder,
                                cursorColor = NeonBlue,
                                focusedContainerColor = ext.glassCard,
                                unfocusedContainerColor = ext.glassCard
                            )
                        )

                        // Rating row (5 stars)
                        GlassCard {
                            Text("⭐ Rating", style = MaterialTheme.typography.labelMedium, color = NeonBlue, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..5).forEach { star ->
                                    Text(
                                        text = if (rating >= star) "★" else "☆",
                                        fontSize = 32.sp,
                                        color = if (rating >= star) AccentQR else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable { viewModel.rating.value = star }.padding(6.dp)
                                    )
                                }
                            }
                        }

                        // File Attachment Actions
                        GlassCard {
                            Text("📁 Attachments", style = MaterialTheme.typography.labelMedium, color = NeonBlue, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp)).clickable { galleryLauncher.launch("image/*") }.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🖼️ Gallery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp)).clickable { triggerCamera() }.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📷 Camera", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp)).clickable { docLauncher.launch(arrayOf("application/pdf")) }.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📄 PDF File", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            // Show attachment preview
                            if (attachmentUri != null) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(NeonBlue.copy(0.08f)).border(1.dp, NeonBlue.copy(0.2f), RoundedCornerShape(8.dp)).padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📎", fontSize = 16.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = attachmentName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeonBlue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier.size(24.dp).clip(CircleShape).clickable { viewModel.setAttachment(null, "") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("❌", fontSize = 10.sp, color = NeonPink)
                                    }
                                }
                            }
                        }

                        // Submit action
                        when (submitState) {
                            is FeedbackSubmitState.Submitting -> {
                                Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = NeonBlue)
                                }
                            }
                            is FeedbackSubmitState.Success -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NeonGreen.copy(0.12f)).border(1.dp, NeonGreen.copy(0.3f), RoundedCornerShape(12.dp)).padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("✓ Feedback Submitted Successfully!", color = NeonGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Thank you for helping improve Smart Vision AI.", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "Submit Another",
                                            color = NeonBlue,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.clickable { viewModel.clearForm() }
                                        )
                                    }
                                }
                            }
                            is FeedbackSubmitState.Error -> {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text((submitState as FeedbackSubmitState.Error).message, color = NeonPink, style = MaterialTheme.typography.labelSmall)
                                    NeonButton(text = "Submit Feedback", onClick = { viewModel.submitFeedback() }, modifier = Modifier.fillMaxWidth())
                                }
                            }
                            else -> {
                                NeonButton(text = "Submit Feedback", onClick = { viewModel.submitFeedback() }, modifier = Modifier.fillMaxWidth())
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                } else {
                    // History Tab
                    val historyList by viewModel.userHistory.collectAsStateWithLifecycle()
                    val isLoading by viewModel.isLoadingHistory.collectAsStateWithLifecycle()

                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonBlue)
                        }
                    } else if (historyList.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📜", fontSize = 48.sp)
                                Text("No feedback records found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(historyList) { entry ->
                                FeedbackHistoryCard(
                                    entry = entry,
                                    onEdit = { showEditDialog = entry },
                                    onDelete = { viewModel.deleteUserFeedback(entry.feedbackId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackHistoryCard(
    entry: FeedbackEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = MaterialTheme.extended
    val dateStr = remember(entry.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(entry.timestamp))
    }

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ext.glassCard).border(1.dp, ext.glassBorder, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(entry.category, style = MaterialTheme.typography.labelSmall, color = NeonBlue, fontWeight = FontWeight.Bold)
                // Status badge
                val isResolved = entry.status == "Resolved"
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isResolved) NeonGreen.copy(0.12f) else NeonOrange.copy(0.12f)).border(1.dp, if (isResolved) NeonGreen.copy(0.4f) else NeonOrange.copy(0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(entry.status, style = MaterialTheme.typography.labelSmall, color = if (isResolved) NeonGreen else NeonOrange, fontWeight = FontWeight.Bold)
                }
            }

            Text(entry.subject, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(entry.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                    Text("📎 Attachment uploaded", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = ext.textHint)
                if (entry.status == "Pending") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(NeonBlue.copy(0.1f)).border(1.dp, NeonBlue.copy(0.2f), RoundedCornerShape(8.dp)).clickable(onClick = onEdit).padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Edit", style = MaterialTheme.typography.labelSmall, color = NeonBlue, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(NeonPink.copy(0.1f)).border(1.dp, NeonPink.copy(0.2f), RoundedCornerShape(8.dp)).clickable(onClick = onDelete).padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Delete", style = MaterialTheme.typography.labelSmall, color = NeonPink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditFeedbackDialog(
    entry: FeedbackEntry,
    onDismiss: () -> Unit,
    onSave: (FeedbackEntry) -> Unit
) {
    var subject by remember { mutableStateOf(entry.subject) }
    var message by remember { mutableStateOf(entry.message) }
    var rating by remember { mutableStateOf(entry.rating) }
    val ext = MaterialTheme.extended

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Pending Feedback", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                // rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        Text(
                            text = if (rating >= star) "★" else "☆",
                            fontSize = 24.sp,
                            color = if (rating >= star) AccentQR else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { rating = star }.padding(4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(entry.copy(subject = subject, message = message, rating = rating)) }) {
                Text("Save Changes", color = NeonBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NeonPink)
            }
        },
        containerColor = ext.cardBackground,
        shape = RoundedCornerShape(16.dp)
    )
}
