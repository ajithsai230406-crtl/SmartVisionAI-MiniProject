package com.smartvision.ai.presentation.chat

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.AiAssistantRepository
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.data.repository.FirebaseRepository
import com.smartvision.ai.domain.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiAssistantRepository: AiAssistantRepository,
    private val historyRepository: HistoryRepository,
    private val firebaseRepository: FirebaseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _suggestedPrompts = MutableStateFlow<List<String>>(emptyList())
    val suggestedPrompts: StateFlow<List<String>> = _suggestedPrompts.asStateFlow()

    private val _voiceOutputEnabled = MutableStateFlow(false)
    val voiceOutputEnabled: StateFlow<Boolean> = _voiceOutputEnabled.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private val _isLastPage = MutableStateFlow(false)
    val isLastPage: StateFlow<Boolean> = _isLastPage.asStateFlow()

    private var oldestLoadedTimestamp: Long? = null

    var isLoading by mutableStateOf(false)
        private set

    var isStreaming by mutableStateOf(false)
        private set

    private var lastQuery: String? = null
    private var lastContextRoute: String? = null

    // ── TextToSpeech ──────────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                ttsReady = true
            }
        }
        // Default to online suggestions; offline mode is triggered when API calls actually fail
        updateSuggestions(true)
        loadChatHistory()
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun updateSuggestions(online: Boolean) {
        _suggestedPrompts.value = if (online) {
            listOf(
                "💡 Tell me about Smart Vision AI",
                "🔬 Explain quantum physics simply",
                "🍏 Suggest a healthy breakfast menu",
                "💻 Write a quick sort algorithm in Kotlin",
                "📚 How do I use the Student Helper?",
                "♻️ How does the Waste Classifier work?"
            )
        } else {
            listOf(
                "📷 What does the OCR Translator do?",
                "🔍 How do I use the Object Detector?",
                "📚 How does the Student Helper work?",
                "🎙️ How do I use the Voice Translator?",
                "📄 How does the Document Scanner work?",
                "💊 What does the Medicine Scanner do?"
            )
        }
    }

    private fun loadChatHistory() {
        val uid = firebaseRepository.getCurrentUid()
        _isLoadingHistory.value = true
        viewModelScope.launch {
            try {
                firebaseRepository.getChatHistoryPaginated(uid, limit = 15, lastVisibleTimestamp = null)
                    .onSuccess { items ->
                        if (items.isNotEmpty()) {
                            oldestLoadedTimestamp = items.last().timestamp
                            val loadedMessages = items.reversed().flatMap { item ->
                                listOf(
                                    ChatMessage(item.userMessage, true, item.timestamp),
                                    ChatMessage(item.aiResponse, false, item.timestamp)
                                )
                            }
                            _messages.value = loadedMessages
                            if (items.size < 15) {
                                _isLastPage.value = true
                            }
                        } else {
                            val welcomeText = "👋 Hello! I'm your Smart Vision AI assistant. I can help you scan, translate, detect objects, classify waste, solve problems, and more! How can I assist you today?"
                            _messages.value = listOf(ChatMessage(welcomeText, false))
                            // Save to local database for compatibility
                            historyRepository.save("ChatBot", welcomeText, "")
                            _isLastPage.value = true
                        }
                    }
                    .onFailure {
                        // Fallback path
                        val dbHistory = historyRepository.getChatHistory()
                        if (dbHistory.isNotEmpty()) {
                            val loaded = dbHistory.map { record ->
                                ChatMessage(
                                    text = record.title,
                                    fromUser = record.type == "ChatUser",
                                    timestamp = record.createdAt
                                )
                            }
                            _messages.value = loaded
                        } else {
                            val welcomeText = "👋 Hello! I'm your Smart Vision AI assistant. I can help you scan, translate, detect objects, classify waste, solve problems, and more! How can I assist you today?"
                            _messages.value = listOf(ChatMessage(welcomeText, false))
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingHistory.value = false
            }
        }
    }

    fun loadNextPage() {
        if (_isLastPage.value || _isLoadingHistory.value) return
        _isLoadingHistory.value = true
        viewModelScope.launch {
            val uid = firebaseRepository.getCurrentUid()
            firebaseRepository.getChatHistoryPaginated(uid, limit = 15, lastVisibleTimestamp = oldestLoadedTimestamp)
                .onSuccess { items ->
                    if (items.isEmpty()) {
                        _isLastPage.value = true
                    } else {
                        oldestLoadedTimestamp = items.last().timestamp
                        val loadedMessages = items.reversed().flatMap { item ->
                            listOf(
                                ChatMessage(item.userMessage, true, item.timestamp),
                                ChatMessage(item.aiResponse, false, item.timestamp)
                            )
                        }
                        _messages.update { current -> loadedMessages + current }
                        if (items.size < 15) {
                            _isLastPage.value = true
                        }
                    }
                }
                .onFailure {
                    // Silently fail or log
                }
            _isLoadingHistory.value = false
        }
    }

    fun toggleVoiceOutput() {
        _voiceOutputEnabled.update { !it }
        if (!_voiceOutputEnabled.value) {
            stopSpeaking()
        }
    }

    fun speak(text: String) {
        if (ttsReady) {
            tts?.stop()
            val cleanText = text.replace(Regex("[#*`]"), "").trim()
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "ai_assistant_tts")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun send(text: String, contextRoute: String? = null) {
        val query = text.trim()
        if (query.isBlank() || isLoading || isStreaming) return

        lastQuery = query
        lastContextRoute = contextRoute
        
        val currentHistory = _messages.value
        _messages.value = currentHistory + ChatMessage(query, true)
        isLoading = true

        viewModelScope.launch {
            try {
                historyRepository.save("ChatUser", query, "")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val historyTranscript = currentHistory.takeLast(8).joinToString("\n") { msg ->
                if (msg.fromUser) "User: ${msg.text}" else "Assistant: ${msg.text}"
            }

            try {
                var streamedReply = ""
                val indexToUpdate = _messages.value.size
                _messages.value = _messages.value + ChatMessage("", false)

                aiAssistantRepository.replyToWithHistoryStream(
                    prompt            = query,
                    historyTranscript = historyTranscript,
                    contextRoute      = contextRoute
                ).collect { chunk ->
                    if (isLoading) {
                        isLoading = false
                        isStreaming = true
                    }
                    streamedReply += chunk

                    val updatedList = _messages.value.toMutableList()
                    if (indexToUpdate < updatedList.size) {
                        updatedList[indexToUpdate] = ChatMessage(streamedReply, false)
                        _messages.value = updatedList
                    }
                }

                isStreaming = false
                isLoading = false

                try {
                    historyRepository.save("ChatBot", streamedReply, "")
                    val timestamp = System.currentTimeMillis()
                    val uid = firebaseRepository.getCurrentUid()
                    firebaseRepository.saveChatHistory(uid, query, streamedReply, timestamp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (_voiceOutputEnabled.value) {
                    speak(streamedReply)
                }

            } catch (e: Exception) {
                isLoading = false
                isStreaming = false
                val cleanList = _messages.value.filter { it.text.isNotEmpty() }
                val errorMsg = "⚠️ Error: ${e.message ?: "Failed to generate dynamic streaming reply"}. Tap the top bar to try again or retry your message."
                _messages.value = cleanList + ChatMessage(errorMsg, false)
            }
        }
    }

    fun retryLastMessage() {
        val query = lastQuery ?: return
        send(query, lastContextRoute)
    }

    fun clearChat() {
        stopSpeaking()
        viewModelScope.launch {
            try {
                historyRepository.clearChatHistory()
                val uid = firebaseRepository.getCurrentUid()
                firebaseRepository.clearAllChatHistory(uid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val welcomeText = "Chat cleared! I'm here contextually. Ask me anything about this screen or other features."
            _messages.value = listOf(ChatMessage(welcomeText, false))
            try {
                historyRepository.save("ChatBot", welcomeText, "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
        isStreaming = false
        lastQuery = null
        lastContextRoute = null
        oldestLoadedTimestamp = null
        _isLastPage.value = true
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
