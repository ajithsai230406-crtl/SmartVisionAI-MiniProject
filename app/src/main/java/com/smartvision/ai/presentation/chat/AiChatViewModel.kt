package com.smartvision.ai.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.AiAssistantRepository
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.domain.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiAssistantRepository: AiAssistantRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _messages = MutableStateFlow(
        listOf(ChatMessage("👋 Hello! I'm your Smart Vision AI assistant powered by Gemini. I can help you scan, translate, detect objects, classify waste, solve problems, and more! How can I assist you today?", false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    var isLoading by mutableStateOf(false)
        private set

    var isStreaming by mutableStateOf(false)
        private set

    private var lastQuery: String? = null
    private var lastContextRoute: String? = null

    /**
     * Sends the user question, builds a memory transcript window of the last 8 messages,
     * and streams the online Gemini AI reply, updating the text content in-place dynamically.
     */
    fun send(text: String, contextRoute: String? = null) {
        val query = text.trim()
        if (query.isBlank() || isLoading || isStreaming) return
        
        lastQuery = query
        lastContextRoute = contextRoute
        
        val currentHistory = _messages.value
        _messages.value = currentHistory + ChatMessage(query, true)
        isLoading = true
        
        viewModelScope.launch {
            // Build safe conversational history memory transcript window of last 8 turns
            val historyTranscript = currentHistory.takeLast(8).joinToString("\n") { msg ->
                if (msg.fromUser) "User: ${msg.text}" else "Assistant: ${msg.text}"
            }
            
            try {
                // Initialize an empty message at the end of the chat list to be updated in-place by the stream
                var streamedReply = ""
                val indexToUpdate = _messages.value.size
                _messages.value = _messages.value + ChatMessage("", false)

                aiAssistantRepository.replyToWithHistoryStream(
                    prompt            = query,
                    historyTranscript = historyTranscript,
                    contextRoute      = contextRoute
                ).collect { chunk ->
                    // Turn off loading typing indicator dots as soon as the first stream chunk arrives,
                    // but mark active streaming as true to keep the inputs disabled.
                    if (isLoading) {
                        isLoading = false
                        isStreaming = true
                    }
                    streamedReply += chunk
                    
                    // Update the message at the target index in-place
                    val updatedList = _messages.value.toMutableList()
                    if (indexToUpdate < updatedList.size) {
                        updatedList[indexToUpdate] = ChatMessage(streamedReply, false)
                        _messages.value = updatedList
                    }
                }
                
                // Final streaming reset
                isStreaming = false
                isLoading = false
                
                // Save conversation to global local history database
                historyRepository.save("Chat", query.take(32), streamedReply.take(220))
            } catch (e: Exception) {
                isLoading = false
                isStreaming = false
                // Remove the empty placeholder if it failed before starting, and append the error bubble
                val cleanList = _messages.value.filter { it.text.isNotEmpty() }
                _messages.value = cleanList + ChatMessage("⚠️ Error: ${e.message ?: "Failed to generate dynamic streaming reply"}. Tap the top bar to try again or retry your message.", false)
            }
        }
    }

    fun retryLastMessage() {
        val query = lastQuery ?: return
        send(query, lastContextRoute)
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessage("Chat cleared! I'm here contextually. Ask me anything about this screen or other features.", false)
        )
        isLoading = false
        isStreaming = false
        lastQuery = null
        lastContextRoute = null
    }
}
