package com.smartvision.ai.presentation.chat

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.ScreenContext
import com.smartvision.ai.data.ScreenContextManager
import com.smartvision.ai.data.repository.AiAssistantRepository
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

/**
 * ViewModel for the floating contextual AI assistant.
 * Reads ScreenContextManager for screen-aware answers.
 * 
 * Key differences from ConversationChatViewModel:
 * - NO Firebase persistence — messages are ephemeral (session only)
 * - Reads live ScreenContext from every module
 * - Provides contextual summaries and suggestions
 * - Independent conversation context — does NOT affect main chat history
 */
@HiltViewModel
class FloatingContextViewModel @Inject constructor(
    private val aiAssistantRepository: AiAssistantRepository,
    private val screenContextManager: ScreenContextManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Ephemeral Messages (no persistence) ─────────────────────────────────
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // ── UI State ────────────────────────────────────────────────────────────
    var isLoading by mutableStateOf(false)
        private set
    var isStreaming by mutableStateOf(false)
        private set

    private val _currentScreenSummary = MutableStateFlow("")
    val currentScreenSummary: StateFlow<String> = _currentScreenSummary.asStateFlow()

    private val _hasContextData = MutableStateFlow(false)
    val hasContextData: StateFlow<Boolean> = _hasContextData.asStateFlow()

    // ── TTS ─────────────────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                ttsReady = true
            }
        }
    }

    /**
     * Called when the floating panel expands.
     * Reads current screen context and generates an initial summary message.
     */
    fun summarizeCurrentScreen() {
        val currentContext = screenContextManager.currentContext.value
        val moduleName = screenContextManager.currentModuleName.value
        val summary = screenContextManager.generateContextSummary()

        _hasContextData.value = currentContext !is ScreenContext.Empty
        _currentScreenSummary.value = summary

        if (currentContext is ScreenContext.Empty) {
            // No context available — show generic welcome
            if (_messages.value.isEmpty()) {
                _messages.value = listOf(
                    ChatMessage(
                        text = "👋 Hello! I'm your contextual AI assistant. Navigate to any module and I'll help you understand the results on your screen!",
                        fromUser = false
                    )
                )
            }
            return
        }

        // Build a contextual welcome message
        val welcomeText = buildString {
            append("🔍 **Here's what I found on the current screen** ($moduleName):\n\n")
            append(summary)
            append("\n\n💬 Do you have any questions related to this?")
        }

        // Replace messages with fresh context (ephemeral)
        _messages.value = listOf(ChatMessage(text = welcomeText, fromUser = false))
    }

    /**
     * Send a message with screen context injected into the prompt.
     */
    fun sendContextualMessage(text: String, currentRoute: String? = null) {
        val query = text.trim()
        if (query.isBlank() || isLoading || isStreaming) return

        val currentMessages = _messages.value
        _messages.value = currentMessages + ChatMessage(query, fromUser = true)
        isLoading = true

        viewModelScope.launch {
            // Build history from recent messages
            val historyTranscript = currentMessages.takeLast(8).joinToString("\n") { msg ->
                if (msg.fromUser) "User: ${msg.text}" else "Assistant: ${msg.text}"
            }

            // Inject screen context into the prompt
            val contextSummary = screenContextManager.generateContextSummary()
            val contextPrompt = if (contextSummary.isNotBlank()) {
                """
                CURRENT SCREEN CONTEXT (the user is looking at this data right now):
                $contextSummary
                
                The user's question is about the above screen context. Answer based on the visible data.
                If the question is unrelated to the screen, still answer helpfully.
                
                User's question: $query
                """.trimIndent()
            } else {
                query
            }

            try {
                // Create placeholder for AI response
                val aiMsgIndex = _messages.value.size
                _messages.update { it + ChatMessage("", fromUser = false) }

                var streamedReply = ""

                aiAssistantRepository.replyToWithHistoryStream(
                    prompt = contextPrompt,
                    historyTranscript = historyTranscript,
                    contextRoute = currentRoute
                ).collect { chunk ->
                    if (isLoading) {
                        isLoading = false
                        isStreaming = true
                    }
                    streamedReply += chunk

                    val updatedList = _messages.value.toMutableList()
                    if (aiMsgIndex < updatedList.size) {
                        updatedList[aiMsgIndex] = ChatMessage(streamedReply, fromUser = false)
                        _messages.value = updatedList
                    }
                }

                isStreaming = false
                isLoading = false

            } catch (e: Exception) {
                isLoading = false
                isStreaming = false
                val errorMsg = "⚠️ Error: ${e.message ?: "Failed to generate reply"}. Please try again."
                _messages.update { list ->
                    val cleaned = list.filter { it.text.isNotEmpty() }
                    cleaned + ChatMessage(errorMsg, fromUser = false)
                }
            }
        }
    }

    /**
     * Generate dynamic suggestions based on the current screen context.
     */
    fun getDynamicSuggestions(): List<String> {
        return when (screenContextManager.currentContext.value) {
            is ScreenContext.TranslatorContext -> listOf(
                "💡 Explain the grammar in this translation",
                "🔤 What are alternative translations?",
                "🗣️ How do I pronounce the translated text?"
            )
            is ScreenContext.QrContext -> listOf(
                "🔗 Is this URL safe to visit?",
                "📱 What does this QR data contain?",
                "ℹ️ Tell me more about this content"
            )
            is ScreenContext.WasteContext -> listOf(
                "♻️ Is this recyclable?",
                "🗑️ Where should I dispose this?",
                "⚠️ Why is it hazardous?",
                "🔄 What are safer alternatives?"
            )
            is ScreenContext.MedicineContext -> listOf(
                "💊 What are the side effects?",
                "⏰ When should I take this medicine?",
                "⚠️ Are there any drug interactions?"
            )
            is ScreenContext.StudentContext -> listOf(
                "📝 Explain the solution step by step",
                "🧮 What formula was used here?",
                "📚 Give me a similar practice problem"
            )
            is ScreenContext.ObjectDetectorContext -> listOf(
                "🛠️ How do I use/care for this object?",
                "📋 What are its main specs & features?",
                "🔄 How does it compare to alternatives/similar items?",
                "📜 Tell me the history/origin of this object",
                "🛒 What related items do you recommend?"
            )
            is ScreenContext.OcrContext -> listOf(
                "📖 Summarize the extracted text",
                "🌐 Translate this text to another language",
                "📝 Fix any errors in the extracted text"
            )
            is ScreenContext.VoiceTranslatorContext -> listOf(
                "🎙️ Summarize this conversation",
                "📝 What were the key points discussed?",
                "🌐 Translate the conversation to English"
            )
            is ScreenContext.CareerContext -> listOf(
                "📈 How can I optimize this resume for ATS?",
                "✍️ Generate an alternative professional summary",
                "🎯 Write a cover letter for this resume",
                "🎙️ Start a mock interview session"
            )
            is ScreenContext.Empty -> listOf(
                "🤖 What can Smart Vision AI do?",
                "📁 Explain the 10-module features",
                "🔋 Which features work offline?"
            )
        }
    }

    /**
     * Clear the floating chat (ephemeral — no Firebase deletion needed).
     */
    fun clearContextChat() {
        stopSpeaking()
        _messages.value = emptyList()
        isLoading = false
        isStreaming = false
    }

    // ── TTS ─────────────────────────────────────────────────────────────────

    fun speak(text: String) {
        if (ttsReady) {
            tts?.stop()
            val clean = text.replace(Regex("[#*`]"), "").trim()
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "floating_tts")
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
