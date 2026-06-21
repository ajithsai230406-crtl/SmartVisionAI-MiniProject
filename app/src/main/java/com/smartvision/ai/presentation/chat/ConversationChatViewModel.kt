package com.smartvision.ai.presentation.chat

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.AiAssistantRepository
import com.smartvision.ai.data.repository.FirebaseRepository
import com.smartvision.ai.domain.model.Conversation
import com.smartvision.ai.domain.model.ConversationMessage
import com.smartvision.ai.utils.FileExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * ChatGPT-like conversation ViewModel for the full-screen AI Assistant.
 * Manages multiple conversations with Firebase persistence, file attachments,
 * and auto-generated titles.
 *
 * This ViewModel is INDEPENDENT from FloatingContextViewModel —
 * they share the same AiAssistantRepository backend but maintain separate state.
 */
@HiltViewModel
class ConversationChatViewModel @Inject constructor(
    private val aiAssistantRepository: AiAssistantRepository,
    private val firebaseRepository: FirebaseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Conversation List (Recent Chats) ────────────────────────────────────
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    // ── Messages in Current Conversation ────────────────────────────────────
    private val _messages = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val messages: StateFlow<List<ConversationMessage>> = _messages.asStateFlow()

    // ── UI State ────────────────────────────────────────────────────────────
    var isLoading by mutableStateOf(false)
        private set
    var isStreaming by mutableStateOf(false)
        private set

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isSidebarOpen = MutableStateFlow(false)
    val isSidebarOpen: StateFlow<Boolean> = _isSidebarOpen.asStateFlow()

    // ── Attachment State ────────────────────────────────────────────────────
    private val _pendingAttachmentUri = MutableStateFlow<Uri?>(null)
    val pendingAttachmentUri: StateFlow<Uri?> = _pendingAttachmentUri.asStateFlow()

    private val _pendingAttachmentType = MutableStateFlow<String?>(null)
    val pendingAttachmentType: StateFlow<String?> = _pendingAttachmentType.asStateFlow()

    private val _pendingAttachmentName = MutableStateFlow<String?>(null)
    val pendingAttachmentName: StateFlow<String?> = _pendingAttachmentName.asStateFlow()

    // ── TTS ─────────────────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var lastQuery: String? = null

    // ── Voice Output ────────────────────────────────────────────────────────
    private val _voiceOutputEnabled = MutableStateFlow(false)
    val voiceOutputEnabled: StateFlow<Boolean> = _voiceOutputEnabled.asStateFlow()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                ttsReady = true
            }
        }
        // Auto-create a new conversation and load recent chats
        loadConversations()
        startNewConversation()
    }

    // ── Conversation Management ─────────────────────────────────────────────

    fun startNewConversation() {
        viewModelScope.launch {
            val uid = firebaseRepository.getCurrentUid()
            firebaseRepository.createConversation(uid, "New Chat")
                .onSuccess { conversationId ->
                    _currentConversationId.value = conversationId
                    _messages.value = listOf(
                        ConversationMessage(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversationId,
                            text = "👋 Hello! I'm your Smart Vision AI assistant. I can help you with anything — ask me questions, solve problems, analyze files, and more! How can I help you today?",
                            fromUser = false
                        )
                    )
                    // Refresh conversation list
                    loadConversations()
                }
                .onFailure {
                    // Fallback: create a local-only conversation
                    val localId = "local_${System.currentTimeMillis()}"
                    _currentConversationId.value = localId
                    _messages.value = listOf(
                        ConversationMessage(
                            id = UUID.randomUUID().toString(),
                            conversationId = localId,
                            text = "👋 Hello! I'm your Smart Vision AI assistant. How can I help you today?",
                            fromUser = false
                        )
                    )
                }
        }
    }

    fun switchConversation(conversationId: String) {
        if (conversationId == _currentConversationId.value) return
        _currentConversationId.value = conversationId
        _messages.value = emptyList()
        isLoading = true

        viewModelScope.launch {
            firebaseRepository.getConversationMessages(conversationId, limit = 50)
                .onSuccess { msgs ->
                    _messages.value = msgs.ifEmpty {
                        listOf(
                            ConversationMessage(
                                id = UUID.randomUUID().toString(),
                                conversationId = conversationId,
                                text = "👋 Hello! Continue our conversation — ask me anything!",
                                fromUser = false
                            )
                        )
                    }
                }
                .onFailure {
                    _messages.value = listOf(
                        ConversationMessage(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversationId,
                            text = "⚠️ Failed to load conversation messages. Please try again.",
                            fromUser = false
                        )
                    )
                }
            isLoading = false
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            firebaseRepository.deleteConversation(conversationId)
            _conversations.update { it.filter { c -> c.id != conversationId } }

            // If we deleted the current conversation, start a new one
            if (_currentConversationId.value == conversationId) {
                startNewConversation()
            }
        }
    }

    fun renameConversation(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            firebaseRepository.updateConversationMeta(conversationId, title = newTitle)
            _conversations.update { list ->
                list.map { if (it.id == conversationId) it.copy(title = newTitle) else it }
            }
        }
    }

    fun searchConversations(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _isSearching.value = false
            loadConversations()
            return
        }
        _isSearching.value = true
        viewModelScope.launch {
            val uid = firebaseRepository.getCurrentUid()
            firebaseRepository.searchConversations(uid, query)
                .onSuccess { results ->
                    _conversations.value = results
                }
            _isSearching.value = false
        }
    }

    fun toggleSidebar() {
        _isSidebarOpen.update { !it }
    }

    private fun loadConversations() {
        viewModelScope.launch {
            val uid = firebaseRepository.getCurrentUid()
            firebaseRepository.getConversations(uid, limit = 30)
                .onSuccess { convos ->
                    _conversations.value = convos
                }
        }
    }

    // ── Send Message ────────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        val query = text.trim()
        if (query.isBlank() || isLoading || isStreaming) return

        val conversationId = _currentConversationId.value ?: return
        lastQuery = query

        val now = System.currentTimeMillis()

        // Handle pending attachment
        val attachUri = _pendingAttachmentUri.value
        val attachType = _pendingAttachmentType.value
        val attachName = _pendingAttachmentName.value

        var attachmentText: String? = null
        var attachmentBitmap: Bitmap? = null

        if (attachUri != null && attachType != null) {
            if (attachType.startsWith("image/")) {
                attachmentBitmap = FileExtractor.loadBitmapFromUri(context, attachUri)
            }
            attachmentText = FileExtractor.extractText(context, attachUri, attachType)
        }

        // Create user message
        val userMessage = ConversationMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            text = if (attachName != null) "📎 $attachName\n$query" else query,
            fromUser = true,
            timestamp = now,
            attachmentType = attachType,
            attachmentPreview = attachmentText?.take(200)
        )

        _messages.update { it + userMessage }
        clearPendingAttachment()
        isLoading = true

        viewModelScope.launch {
            // Save user message to Firebase
            firebaseRepository.saveConversationMessage(conversationId, userMessage)

            // Build conversation history from messages
            val historyTranscript = _messages.value.takeLast(10).joinToString("\n") { msg ->
                if (msg.fromUser) "User: ${msg.text}" else "Assistant: ${msg.text}"
            }

            // Build the prompt with attachment context
            val fullPrompt = if (attachmentText != null && !attachType.orEmpty().startsWith("image/")) {
                """
                The user has uploaded a file. Here is the extracted content:
                ---FILE CONTENT START---
                $attachmentText
                ---FILE CONTENT END---
                
                User's question about this file: $query
                """.trimIndent()
            } else {
                query
            }

            try {
                // Create placeholder for AI response
                val aiMsgIndex = _messages.value.size
                _messages.update { it + ConversationMessage(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    text = "",
                    fromUser = false,
                    timestamp = System.currentTimeMillis()
                ) }

                var streamedReply = ""

                aiAssistantRepository.replyToWithHistoryStream(
                    prompt = fullPrompt,
                    historyTranscript = historyTranscript,
                    contextRoute = "ai_assistant"
                ).collect { chunk ->
                    if (isLoading) {
                        isLoading = false
                        isStreaming = true
                    }
                    streamedReply += chunk

                    val updatedList = _messages.value.toMutableList()
                    if (aiMsgIndex < updatedList.size) {
                        updatedList[aiMsgIndex] = updatedList[aiMsgIndex].copy(text = streamedReply)
                        _messages.value = updatedList
                    }
                }

                isStreaming = false
                isLoading = false

                // Save AI response to Firebase
                val aiMessage = ConversationMessage(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    text = streamedReply,
                    fromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                firebaseRepository.saveConversationMessage(conversationId, aiMessage)

                // Auto-generate title from first user message
                autoGenerateTitle(conversationId, query)

                // TTS if enabled
                if (_voiceOutputEnabled.value) {
                    speak(streamedReply)
                }

            } catch (e: Exception) {
                isLoading = false
                isStreaming = false
                val errorMsg = "⚠️ Error: ${e.message ?: "Failed to generate reply"}. Please try again."
                _messages.update { list ->
                    val cleaned = list.filter { it.text.isNotEmpty() }
                    cleaned + ConversationMessage(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        text = errorMsg,
                        fromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    fun retryLastMessage() {
        lastQuery?.let { sendMessage(it) }
    }

    // ── Attachment Handling ──────────────────────────────────────────────────

    fun setAttachment(uri: Uri, mimeType: String, fileName: String?) {
        _pendingAttachmentUri.value = uri
        _pendingAttachmentType.value = mimeType
        _pendingAttachmentName.value = fileName ?: "Attached file"
    }

    fun clearPendingAttachment() {
        _pendingAttachmentUri.value = null
        _pendingAttachmentType.value = null
        _pendingAttachmentName.value = null
    }

    // ── Auto Title Generation ───────────────────────────────────────────────

    private fun autoGenerateTitle(conversationId: String, firstMessage: String) {
        viewModelScope.launch {
            // Check if this conversation still has the default title
            val currentConvo = _conversations.value.find { it.id == conversationId }
            if (currentConvo != null && (currentConvo.title == "New Chat" || currentConvo.title.isBlank())) {
                // Generate title from the first ~40 chars of the first message
                val title = firstMessage
                    .replace(Regex("[📎🖼️📄📝📊📃]"), "")
                    .trim()
                    .take(40)
                    .let { if (it.length >= 40) "$it..." else it }
                    .ifBlank { "Chat" }

                firebaseRepository.updateConversationMeta(conversationId, title = title)
                _conversations.update { list ->
                    list.map {
                        if (it.id == conversationId) it.copy(title = title) else it
                    }
                }
            }
        }
    }

    // ── TTS ─────────────────────────────────────────────────────────────────

    fun toggleVoiceOutput() {
        _voiceOutputEnabled.update { !it }
        if (!_voiceOutputEnabled.value) stopSpeaking()
    }

    fun speak(text: String) {
        if (ttsReady) {
            tts?.stop()
            val clean = text.replace(Regex("[#*`]"), "").trim()
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "conv_tts")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    // ── Cleanup ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
