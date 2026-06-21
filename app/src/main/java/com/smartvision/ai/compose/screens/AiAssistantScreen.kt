package com.smartvision.ai.compose.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import android.widget.TextView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import io.noties.markwon.Markwon
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.presentation.chat.ConversationChatViewModel
import com.smartvision.ai.ui.theme.*
import com.smartvision.ai.utils.FileExtractor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ══════════════════════════════════════════════════════════════════════════════
// CONTEXT-AWARE QUICK SUGGESTIONS
// ══════════════════════════════════════════════════════════════════════════════

private val contextSuggestions = listOf(
    "💡 What can Smart Vision AI do?",
    "📷 How do I scan a document?",
    "🌐 How do I translate text?",
    "♻️ How does waste detection work?",
    "🔍 How does object detection work?",
    "📚 Can you solve math problems?",
    "🎙️ How do I use Voice Translator?",
    "📄 How do I export as PDF?",
    "🔋 Is this app offline-capable?",
    "🤖 Which AI model powers this app?"
)

// ══════════════════════════════════════════════════════════════════════════════
// LOCAL MARKDOWN TEXT PARSER
// ══════════════════════════════════════════════════════════════════════════════

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var remainingLine = line
            var isHeader = false
            var headerStyle: SpanStyle? = null

            if (line.startsWith("### ")) {
                isHeader = true
                headerStyle = SpanStyle(fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 16.sp)
                remainingLine = line.substring(4)
            } else if (line.startsWith("## ")) {
                isHeader = true
                headerStyle = SpanStyle(fontWeight = FontWeight.Bold, color = NeonBlue, fontSize = 18.sp)
                remainingLine = line.substring(3)
            } else if (line.startsWith("# ")) {
                isHeader = true
                headerStyle = SpanStyle(fontWeight = FontWeight.Bold, color = NeonPurple, fontSize = 20.sp)
                remainingLine = line.substring(2)
            }

            if (isHeader && headerStyle != null) {
                withStyle(headerStyle) {
                    append(remainingLine)
                }
            } else {
                var i = 0
                while (i < remainingLine.length) {
                    when {
                        remainingLine.startsWith("**", i) -> {
                            val endIdx = remainingLine.indexOf("**", i + 2)
                            if (endIdx != -1) {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = NeonCyan)) {
                                    append(remainingLine.substring(i + 2, endIdx))
                                }
                                i = endIdx + 2
                            } else {
                                append("**")
                                i += 2
                            }
                        }
                        remainingLine.startsWith("`", i) -> {
                            val endIdx = remainingLine.indexOf("`", i + 1)
                            if (endIdx != -1) {
                                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.Black.copy(0.3f), color = NeonBlue)) {
                                    append(remainingLine.substring(i + 1, endIdx))
                                }
                                i = endIdx + 1
                            } else {
                                append("`")
                                i += 1
                            }
                        }
                        else -> {
                            append(remainingLine[i])
                            i++
                        }
                    }
                }
            }

            if (index < lines.lastIndex) {
                append("\n")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MARKDOWN TABLE PARSER & RENDERER
// ══════════════════════════════════════════════════════════════════════════════

sealed class ChatSegment {
    data class Text(val content: String) : ChatSegment()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : ChatSegment()
}

fun parseChatSegments(text: String): List<ChatSegment> {
    val segments = mutableListOf<ChatSegment>()
    val lines = text.split("\n")
    
    var inTable = false
    var currentTableHeaders = mutableListOf<String>()
    var currentTableRows = mutableListOf<List<String>>()
    val currentTextAccumulator = StringBuilder()

    fun flushText() {
        val txt = currentTextAccumulator.toString().trim()
        if (txt.isNotEmpty()) {
            segments.add(ChatSegment.Text(txt))
        }
        currentTextAccumulator.clear()
    }

    fun flushTable() {
        if (inTable) {
            if (currentTableHeaders.isNotEmpty() || currentTableRows.isNotEmpty()) {
                segments.add(ChatSegment.Table(currentTableHeaders.toList(), currentTableRows.toList()))
            }
            currentTableHeaders.clear()
            currentTableRows.clear()
            inTable = false
        }
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        
        val isSeparator = trimmed.startsWith("|") && trimmed.contains("-") && trimmed.all { it == '|' || it == '-' || it == ' ' || it == ':' }
        val isTableLine = trimmed.startsWith("|") && trimmed.contains("|") && trimmed.count { it == '|' } >= 2

        if (isTableLine && !isSeparator) {
            if (!inTable) {
                val nextLine = lines.getOrNull(i + 1)?.trim() ?: ""
                val nextIsSeparator = nextLine.startsWith("|") && nextLine.contains("-") && nextLine.all { it == '|' || it == '-' || it == ' ' || it == ':' }
                
                if (nextIsSeparator) {
                    flushText()
                    inTable = true
                    
                    val rawParts = trimmed.split("|").map { it.trim() }
                    val parts = if (rawParts.first().isEmpty() && rawParts.last().isEmpty() && rawParts.size > 2) {
                        rawParts.subList(1, rawParts.size - 1)
                    } else {
                        rawParts.filter { it.isNotEmpty() }
                    }
                    currentTableHeaders.addAll(parts)
                    
                    i++
                } else {
                    currentTextAccumulator.append(line).append("\n")
                }
            } else {
                val rawParts = trimmed.split("|").map { it.trim() }
                val parts = if (rawParts.first().isEmpty() && rawParts.last().isEmpty() && rawParts.size > 2) {
                    rawParts.subList(1, rawParts.size - 1)
                } else {
                    rawParts.filter { it.isNotEmpty() }
                }
                currentTableRows.add(parts)
            }
        } else {
            if (inTable) {
                flushTable()
            }
            currentTextAccumulator.append(line).append("\n")
        }
        i++
    }
    
    flushTable()
    flushText()
    
    return segments
}

@Composable
fun TableCardList(headers: List<String>, rows: List<List<String>>) {
    val ext = MaterialTheme.extended
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ext.glassCard)
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(NeonCyan.copy(0.3f), NeonPurple.copy(0.2f))),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val title = row.getOrNull(0) ?: "Item"
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(0.08f))
                )
                
                for (colIdx in 1 until maxOf(headers.size, row.size)) {
                    val header = headers.getOrNull(colIdx) ?: ""
                    val value = row.getOrNull(colIdx) ?: ""
                    if (header.isNotBlank() && value.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray.copy(0.8f),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(0.4f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(0.9f),
                                modifier = Modifier.weight(0.6f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MARKWON MARKDOWN COMPOSABLE
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun MarkdownText(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val markwon = remember(context) { Markwon.create(context) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = 14f
                setLineSpacing(4f, 1.15f)
            }
        },
        update = { textView ->
            textView.setTextColor(textColor.toArgb())
            markwon.setMarkdown(textView, text)
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// AI ASSISTANT SCREEN — ChatGPT-like with Recent Chats Sidebar
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AiAssistantScreen(
    onBack: () -> Unit,
    viewModel: ConversationChatViewModel = hiltViewModel()
) {
    val ext = MaterialTheme.extended
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val currentConvoId by viewModel.currentConversationId.collectAsStateWithLifecycle()
    val isSidebarOpen by viewModel.isSidebarOpen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val pendingAttachmentName by viewModel.pendingAttachmentName.collectAsStateWithLifecycle()
    val pendingAttachmentType by viewModel.pendingAttachmentType.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // ── Voice input launcher ────────────────────────────────────────────────
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                input = spokenText
            }
        }
    }

    // ── File picker launcher ────────────────────────────────────────────────
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
            val cursor = context.contentResolver.query(it, null, null, null, null)
            val fileName = cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                c.moveToFirst()
                if (nameIndex >= 0) c.getString(nameIndex) else "File"
            } ?: "File"
            viewModel.setAttachment(it, mimeType, fileName)
        }
    }

    // ── Image picker launcher ───────────────────────────────────────────────
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it) ?: "image/*"
            viewModel.setAttachment(it, mimeType, "Image")
        }
    }

    // ── Camera launcher ─────────────────────────────────────────────────────
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // Camera returns a thumbnail bitmap; for the chat, we note it
        if (bitmap != null) {
            Toast.makeText(context, "📷 Image captured! Type your question.", Toast.LENGTH_SHORT).show()
        }
    }

    // Scroll to bottom on new messages
    val lastMessage = messages.lastOrNull()
    LaunchedEffect(lastMessage?.timestamp, lastMessage?.text) {
        if (messages.isNotEmpty() && (lastMessage?.fromUser == true || viewModel.isStreaming || viewModel.isLoading)) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // Rename dialog state
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameConversationId by remember { mutableStateOf("") }
    var renameText by remember { mutableStateOf("") }

    // Attachment bottom sheet state
    var showAttachmentSheet by remember { mutableStateOf(false) }

    GradientBackground {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {

                // ── Top Bar ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Back button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ext.glassCard)
                            .border(1.dp, ext.glassBorder, RoundedCornerShape(10.dp))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
                    }

                    // AI avatar + title
                    Box(modifier = Modifier.size(40.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(NeonPurple.copy(0.3f), NeonCyan.copy(0.1f))))
                                .border(1.5.dp, Brush.linearGradient(listOf(NeonCyan, NeonPurple)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 18.sp)
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FF88))
                                .border(1.5.dp, Color.Black, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            "AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        // Show current conversation title
                        val currentTitle = conversations.find { it.id == currentConvoId }?.title ?: "New Chat"
                        Text(
                            currentTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // New Chat button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(NeonCyan.copy(0.2f), NeonPurple.copy(0.2f))))
                            .border(1.dp, NeonCyan.copy(0.4f), RoundedCornerShape(10.dp))
                            .clickable { viewModel.startNewConversation() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✏️", fontSize = 14.sp)
                    }

                    // Sidebar toggle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSidebarOpen) NeonPurple.copy(0.3f) else ext.glassCard)
                            .border(1.dp, if (isSidebarOpen) NeonPurple else ext.glassBorder, RoundedCornerShape(10.dp))
                            .clickable { viewModel.toggleSidebar() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("☰", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    }
                }

                // ── AI Orb ──────────────────────────────────────────────────
                AiOrbHeader(isProcessing = viewModel.isLoading || viewModel.isStreaming)

                // ── Messages ────────────────────────────────────────────────
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillParentMaxWidth()
                            ) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Ask me anything! Upload files, images,\nor just chat freely.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }

                    items(messages) { msg ->
                        ConversationChatBubble(
                            text = msg.text,
                            isUser = msg.fromUser,
                            timestamp = msg.timestamp,
                            attachmentType = msg.attachmentType,
                            onSpeakClick = { viewModel.speak(msg.text) }
                        )
                    }

                    if (viewModel.isLoading) {
                        item { AiTypingIndicator() }
                    }
                }

                // ── Suggestion pills ────────────────────────────────────────
                if (messages.size <= 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(contextSuggestions.take(6)) { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ext.glassCard)
                                    .border(1.dp, NeonPurple.copy(0.35f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (!(viewModel.isLoading || viewModel.isStreaming)) {
                                            viewModel.sendMessage(suggestion)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // ── Attachment Preview ──────────────────────────────────────
                AnimatedVisibility(visible = pendingAttachmentName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonPurple.copy(0.15f))
                            .border(1.dp, NeonPurple.copy(0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val emoji = FileExtractor.getFileTypeEmoji(pendingAttachmentType ?: "")
                        Text(emoji, fontSize = 20.sp)
                        Text(
                            pendingAttachmentName ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ext.glassCard)
                                .clickable { viewModel.clearPendingAttachment() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    }
                }

                // ── Input Row ───────────────────────────────────────────────
                ChatGptInputRow(
                    input = input,
                    isLoading = viewModel.isLoading || viewModel.isStreaming,
                    onChange = { input = it },
                    onSend = {
                        if (input.isNotBlank() && !(viewModel.isLoading || viewModel.isStreaming)) {
                            viewModel.sendMessage(input.trim())
                            input = ""
                        }
                    },
                    onMicClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                            }
                            speechRecognizerLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onAttachClick = { showAttachmentSheet = true }
                )
            }

            // ── Recent Chats Sidebar ────────────────────────────────────────
            AnimatedVisibility(
                visible = isSidebarOpen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                RecentChatsSidebar(
                    conversations = conversations,
                    currentConvoId = currentConvoId,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.searchConversations(it) },
                    onConversationClick = { id ->
                        viewModel.switchConversation(id)
                        viewModel.toggleSidebar()
                    },
                    onNewChat = {
                        viewModel.startNewConversation()
                        viewModel.toggleSidebar()
                    },
                    onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                    onRenameConversation = { id, currentTitle ->
                        renameConversationId = id
                        renameText = currentTitle
                        showRenameDialog = true
                    },
                    onClose = { viewModel.toggleSidebar() }
                )
            }

            // ── Attachment Bottom Sheet ──────────────────────────────────────
            AnimatedVisibility(
                visible = showAttachmentSheet,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                AttachmentBottomSheet(
                    onDismiss = { showAttachmentSheet = false },
                    onCameraClick = {
                        showAttachmentSheet = false
                        cameraLauncher.launch(null)
                    },
                    onGalleryClick = {
                        showAttachmentSheet = false
                        imagePickerLauncher.launch("image/*")
                    },
                    onFileClick = {
                        showAttachmentSheet = false
                        filePickerLauncher.launch(arrayOf(
                            "application/pdf",
                            "text/plain",
                            "text/csv",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "image/*"
                        ))
                    }
                )
            }
        }
    }

    // ── Rename Dialog ───────────────────────────────────────────────────────
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = ext.cardBackground,
            title = {
                Text("Rename Conversation", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                BasicTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ext.glassCard)
                        .padding(12.dp),
                    cursorBrush = SolidColor(NeonCyan)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameConversation(renameConversationId, renameText)
                    showRenameDialog = false
                }) {
                    Text("Rename", color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// RECENT CHATS SIDEBAR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RecentChatsSidebar(
    conversations: List<com.smartvision.ai.domain.model.Conversation>,
    currentConvoId: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onConversationClick: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onClose: () -> Unit
) {
    val ext = MaterialTheme.extended
    Row(Modifier.fillMaxSize()) {
        // Dim backdrop
        Box(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight()
                .background(Color.Black.copy(0.5f))
                .clickable(onClick = onClose)
        )

        // Sidebar panel
        Column(
            modifier = Modifier
                .weight(0.85f)
                .fillMaxHeight()
                .background(if (ext.isDark) Color(0xF0070E1B) else Color(0xFFF3F7FF))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(listOf(NeonPurple.copy(0.4f), NeonBlue.copy(0.2f))),
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                )
                .padding(top = 16.dp)
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Chats",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ext.glassCard)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // New Chat button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(NeonCyan.copy(0.15f), NeonPurple.copy(0.15f))))
                    .border(1.dp, NeonCyan.copy(0.4f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onNewChat)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("✏️", fontSize = 16.sp)
                    Text(
                        "New Chat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ext.glassCard)
                    .border(1.dp, ext.glassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    cursorBrush = SolidColor(NeonCyan),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text("🔍 Search chats...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), style = MaterialTheme.typography.bodySmall)
                        }
                        inner()
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = ext.glassBorder)

            // Conversations list
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Group by date
                val today = System.currentTimeMillis()
                val dayMs = 86400000L

                val todayChats = conversations.filter { (today - it.updatedAt) < dayMs }
                val yesterdayChats = conversations.filter { (today - it.updatedAt) in dayMs until (2 * dayMs) }
                val olderChats = conversations.filter { (today - it.updatedAt) >= (2 * dayMs) }

                if (todayChats.isNotEmpty()) {
                    item {
                        Text(
                            "Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    items(todayChats) { convo ->
                        ConversationItem(
                            conversation = convo,
                            isSelected = convo.id == currentConvoId,
                            onClick = { onConversationClick(convo.id) },
                            onDelete = { onDeleteConversation(convo.id) },
                            onRename = { onRenameConversation(convo.id, convo.title) }
                        )
                    }
                }

                if (yesterdayChats.isNotEmpty()) {
                    item {
                        Text(
                            "Yesterday",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    items(yesterdayChats) { convo ->
                        ConversationItem(
                            conversation = convo,
                            isSelected = convo.id == currentConvoId,
                            onClick = { onConversationClick(convo.id) },
                            onDelete = { onDeleteConversation(convo.id) },
                            onRename = { onRenameConversation(convo.id, convo.title) }
                        )
                    }
                }

                if (olderChats.isNotEmpty()) {
                    item {
                        Text(
                            "Previous",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    items(olderChats) { convo ->
                        ConversationItem(
                            conversation = convo,
                            isSelected = convo.id == currentConvoId,
                            onClick = { onConversationClick(convo.id) },
                            onDelete = { onDeleteConversation(convo.id) },
                            onRename = { onRenameConversation(convo.id, convo.title) }
                        )
                    }
                }

                if (conversations.isEmpty()) {
                    item {
                        Text(
                            "No conversations yet.\nStart a new chat!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: com.smartvision.ai.domain.model.Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonPurple.copy(0.15f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) NeonPurple.copy(0.4f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("💬", fontSize = 14.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                conversation.title,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (conversation.lastMessage.isNotBlank()) {
                Text(
                    conversation.lastMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Action dots
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable { showActions = !showActions },
            contentAlignment = Alignment.Center
        ) {
            Text("⋯", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }

    // Actions dropdown
    AnimatedVisibility(visible = showActions) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonBlue.copy(0.1f))
                    .clickable {
                        showActions = false
                        onRename()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("✏️ Rename", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonPink.copy(0.1f))
                    .clickable {
                        showActions = false
                        onDelete()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("🗑️ Delete", style = MaterialTheme.typography.labelSmall, color = NeonPink)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ATTACHMENT BOTTOM SHEET
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFileClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(if (ext.isDark) Color(0xF0070E1B) else Color(0xFFFFFFFF))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(NeonPurple.copy(0.4f), NeonCyan.copy(0.3f))),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(20.dp)
            .navigationBarsPadding()
    ) {
        // Handle
        Box(
            modifier = Modifier
                .width(40.dp).height(4.dp)
                .clip(CircleShape)
                .background(NeonPurple.copy(0.4f))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))

        Text(
            "Attach Files",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachmentOption(emoji = "📷", label = "Camera", onClick = onCameraClick)
            AttachmentOption(emoji = "🖼️", label = "Gallery", onClick = onGalleryClick)
            AttachmentOption(emoji = "📁", label = "Files", onClick = onFileClick)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Supports: Images, PDF, DOCX, TXT, CSV",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ext.glassCard)
                .clickable(onClick = onDismiss)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Cancel", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AttachmentOption(emoji: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(NeonPurple.copy(0.2f), NeonBlue.copy(0.1f))))
                .border(1.dp, NeonPurple.copy(0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 24.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// AI ORB HEADER — Animated plasma orb
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AiOrbHeader(isProcessing: Boolean = false) {
    val inf  = rememberInfiniteTransition(label = "orb")
    val rot  by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "rot")
    val rot2 by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "rot2")
    val pulse by inf.animateFloat(if (isProcessing) 0.8f else 0.92f, if (isProcessing) 1.2f else 1.08f, infiniteRepeatable(tween(if (isProcessing) 500 else 2000), RepeatMode.Reverse), label = "p")
    val glow  by inf.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "g")

    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(90.dp).scale(pulse)) {
            val cx = size.width / 2; val cy = size.height / 2; val r = size.width / 2

            listOf(0.95f, 0.80f, 0.65f).forEachIndexed { i, scale ->
                val ringR = r * scale
                val pts   = 60
                val path  = Path()
                for (j in 0..pts) {
                    val angle = (2 * PI * j / pts).toFloat()
                    val wave  = 4f * sin((angle * 3 + rot + i * 1.2f).toDouble()).toFloat()
                    val x     = cx + (ringR + wave) * cos(angle.toDouble()).toFloat()
                    val y     = cy + (ringR + wave) * sin(angle.toDouble()).toFloat()
                    if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, Brush.sweepGradient(
                    listOf(NeonBlue.copy(glow - i * 0.1f), NeonPurple.copy(glow - i * 0.1f), NeonCyan.copy(glow - i * 0.15f), NeonBlue.copy(glow - i * 0.1f))
                ), style = Stroke(2.5f - i * 0.5f))
            }

            val px = cx + r * 0.6f * cos(rot2.toDouble()).toFloat()
            val py = cy + r * 0.6f * sin(rot2.toDouble()).toFloat()
            drawCircle(NeonCyan.copy(glow), 5f, Offset(px, py))

            drawCircle(Brush.radialGradient(listOf(NeonPurple.copy(0.5f), NeonBlue.copy(0.2f), Color.Transparent)), r * 0.45f)
        }
        Text("🤖", fontSize = 28.sp, modifier = Modifier.scale(pulse * 0.9f))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CONVERSATION CHAT BUBBLE (supports attachment badges)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ConversationChatBubble(
    text: String,
    isUser: Boolean,
    timestamp: Long,
    attachmentType: String? = null,
    onSpeakClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    val isError = text.startsWith("⚠️ Error")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(NeonPurple.copy(0.3f), NeonCyan.copy(0.1f))))
                    .border(1.dp, Brush.linearGradient(listOf(NeonCyan, NeonPurple)), CircleShape)
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        val bubbleBackground = when {
            isUser -> Brush.horizontalGradient(listOf(Color(0xFF00D4FF), Color(0xFF8B5CF6)))
            isError -> Brush.linearGradient(listOf(NeonPink.copy(0.12f), NeonPink.copy(0.04f)))
            else -> Brush.linearGradient(listOf(ext.glassCard, ext.glassCard))
        }

        val bubbleBorderModifier = when {
            isUser -> Modifier
            isError -> Modifier.border(1.dp, NeonPink.copy(0.5f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
            else -> Modifier.border(1.dp, ext.glassBorder, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
        }

        val bubbleTextColor = when {
            isUser -> Color.White
            isError -> NeonPink
            else -> MaterialTheme.colorScheme.onSurface
        }

        Box(
            modifier = Modifier.widthIn(max = 285.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (isUser) 18.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 18.dp))
                .background(bubbleBackground)
                .then(bubbleBorderModifier)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Column {
                // Attachment badge
                if (attachmentType != null) {
                    val emoji = FileExtractor.getFileTypeEmoji(attachmentType)
                    val label = FileExtractor.getFileTypeLabel(attachmentType)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonPurple.copy(0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(emoji, fontSize = 12.sp)
                        Text(label, style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                }

                if (isUser) {
                    val parsedText = remember(text) { parseMarkdown(text) }
                    Text(
                        text = parsedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = bubbleTextColor,
                        lineHeight = 22.sp
                    )
                } else {
                    val segments = remember(text) { parseChatSegments(text) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        segments.forEach { segment ->
                            when (segment) {
                                is ChatSegment.Text -> {
                                    if (segment.content.isNotBlank()) {
                                        MarkdownText(
                                            text = segment.content,
                                            textColor = bubbleTextColor,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                is ChatSegment.Table -> {
                                    TableCardList(headers = segment.headers, rows = segment.rows)
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeString = remember(timestamp) {
                        try {
                            val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
                            sdf.format(java.util.Date(timestamp))
                        } catch (e: Exception) { "" }
                    }
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) Color.White.copy(0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                        fontSize = 10.sp
                    )

                    if (!isUser && text.isNotEmpty() && !isError) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ext.glassCard)
                                .border(1.dp, NeonPurple.copy(0.3f), CircleShape)
                                .clickable(onClick = onSpeakClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔊", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TYPING INDICATOR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiTypingIndicator() {
    val inf = rememberInfiniteTransition(label = "typing")
    val dots = (0..2).map { i ->
        inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, delayMillis = i * 170), RepeatMode.Reverse), label = "d$i")
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(NeonPurple.copy(0.3f), NeonCyan.copy(0.1f))))
                .border(1.dp, Brush.linearGradient(listOf(NeonCyan, NeonPurple)), CircleShape)
                .align(Alignment.Bottom),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp))
                .background(MaterialTheme.extended.glassCard)
                .border(1.dp, Brush.linearGradient(listOf(NeonCyan.copy(0.3f), NeonPurple.copy(0.3f))), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                dots.forEach { dot ->
                    Box(Modifier.size(7.dp).alpha(dot.value).clip(CircleShape).background(NeonBlue))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CHATGPT-STYLE INPUT ROW (with attachment button)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatGptInputRow(
    input: String,
    isLoading: Boolean,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onAttachClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            .imePadding()
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Attachment button (📎)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ext.glassCard)
                .border(1.dp, ext.glassBorder, CircleShape)
                .clickable(enabled = !isLoading, onClick = onAttachClick),
            contentAlignment = Alignment.Center
        ) {
            Text("📎", fontSize = 18.sp)
        }

        // Text input
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(ext.glassCard)
                .border(1.dp, ext.glassBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = input,
                onValueChange = onChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(NeonCyan),
                decorationBox = { inner ->
                    if (input.isEmpty()) {
                        Text("Message AI Assistant...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                    inner()
                }
            )
        }

        // Voice input button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ext.glassCard)
                .border(1.dp, ext.glassBorder, CircleShape)
                .clickable(enabled = !isLoading, onClick = onMicClick),
            contentAlignment = Alignment.Center
        ) {
            Text("🎙️", fontSize = 16.sp)
        }

        // Send button
        val sendModifier = if (isLoading) {
            Modifier.size(44.dp).clip(CircleShape).background(ext.glassCard)
        } else {
            Modifier.size(44.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))))
        }
        Box(
            modifier = sendModifier.clickable(enabled = !isLoading && input.isNotBlank(), onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NeonBlue, strokeWidth = 2.dp)
            } else {
                Text("➤", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}
