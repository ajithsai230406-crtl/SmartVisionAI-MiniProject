package com.smartvision.ai.student.presentation

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.ocr.data.OcrRecognitionRepository
import com.smartvision.ai.ocr.domain.RecognizedBlock
import com.smartvision.ai.ocr.domain.toDomain
import com.smartvision.ai.student.data.StudentAiRepository
import com.smartvision.ai.student.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

sealed class StudentScanState {
    object CameraActive : StudentScanState()
    object Solving : StudentScanState()
    data class Solved(val solution: AiSolution, val croppedImage: Bitmap?) : StudentScanState()
    data class Error(val message: String) : StudentScanState()
}

@HiltViewModel
class StudentHelperViewModel @Inject constructor(
    private val aiRepo:      StudentAiRepository,
    private val ocrRepo:     OcrRecognitionRepository,
    private val historyRepo: HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Scan State ────────────────────────────────────────────────────────────
    private val _scanState = MutableStateFlow<StudentScanState>(StudentScanState.CameraActive)
    val scanState: StateFlow<StudentScanState> = _scanState.asStateFlow()

    // ── Subject ───────────────────────────────────────────────────────────────
    private val _subject = MutableStateFlow(Subject.MATHEMATICS)
    val subject: StateFlow<Subject> = _subject.asStateFlow()

    // ── OCR Overlay Blocks ────────────────────────────────────────────────────
    private val _overlayBlocks = MutableStateFlow<List<RecognizedBlock>>(emptyList())
    val overlayBlocks: StateFlow<List<RecognizedBlock>> = _overlayBlocks.asStateFlow()

    // ── Chat & Tutor messages (conversational follow-up inside solved sheet) ──
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _flashEnabled = MutableStateFlow(false)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled.asStateFlow()

    private val _activeTab = MutableStateFlow(StudentTab.SOLUTION)
    val activeTab: StateFlow<StudentTab> = _activeTab.asStateFlow()

    // TTS Voice Explanation
    private var tts: TextToSpeech? = null
    private var ocrJob: Job? = null

    init {
        tts = TextToSpeech(context) { /* tts init */ }
    }

    // ── Continual Camera Frame OCR ────────────────────────────────────────────
    fun analyzeFrame(image: Image, rotation: Int, width: Int, height: Int) {
        if (_scanState.value !is StudentScanState.CameraActive) return
        ocrJob?.cancel()
        ocrJob = viewModelScope.launch {
            delay(250) // debounce
            ocrRepo.recognizeFromImage(image, rotation)
                .onSuccess { text ->
                    val domain = text.toDomain(width, height)
                    _overlayBlocks.value = domain.blocks
                }
                .onFailure {
                    // Ignore transient live frame OCR failures
                }
        }
    }

    // ── Solve Question from Cropped Bitmap ─────────────────────────────────────
    fun solveCroppedQuestion(bitmap: Bitmap, questionLabel: String = "Scanned Question") {
        _scanState.value = StudentScanState.Solving
        _activeTab.value = StudentTab.SOLUTION
        _chatMessages.value = listOf(
            ChatMessage(
                text = "📚 I am analyzing this problem for you step-by-step. Ask me any follow-up questions here!",
                isUser = false
            )
        )

        viewModelScope.launch {
            aiRepo.askFromImage(bitmap, _subject.value)
                .onSuccess { solution ->
                    _scanState.value = StudentScanState.Solved(solution, bitmap)
                    
                    // Save to global history repository
                    historyRepo.save(
                        "Study",
                        "Homework Solved (${_subject.value.displayName})",
                        solution.mainAnswer.take(220)
                    )
                }
                .onFailure { e ->
                    _scanState.value = StudentScanState.Error(e.message ?: "Failed to solve question. Try again.")
                }
        }
    }

    // ── Conversational Follow-up Tutor Chat ──────────────────────────────────
    fun sendTutorChatMessage(text: String) {
        val msg = text.trim()
        if (msg.isBlank() || _isThinking.value) return

        val currentState = _scanState.value as? StudentScanState.Solved ?: return
        val sol = currentState.solution

        _isThinking.value = true
        // Add user message
        _chatMessages.update { it + ChatMessage(text = msg, isUser = true) }

        // Add thinking placeholder
        val thinkingId = System.currentTimeMillis() + 1
        _chatMessages.update { it + ChatMessage(id = thinkingId, text = "...", isUser = false, isLoading = true) }

        viewModelScope.launch {
            // Build chat context using the original solution + conversation history
            aiRepo.chat(
                userMessage = msg,
                subject     = _subject.value,
                history     = _chatMessages.value.filter { it.id != thinkingId }
            ).onSuccess { response ->
                _chatMessages.update { it.filter { m -> m.id != thinkingId } + ChatMessage(text = response, isUser = false) }
            }.onFailure { e ->
                _chatMessages.update { it.filter { m -> m.id != thinkingId } + ChatMessage(text = "⚠️ Failed: ${e.message}", isUser = false, hasError = true) }
            }
            _isThinking.value = false
        }
    }

    // ── Explain Simply Follow-up ─────────────────────────────────────────────
    fun explainSolutionSimply() {
        val currentState = _scanState.value as? StudentScanState.Solved ?: return
        val sol = currentState.solution

        _isThinking.value = true
        _activeTab.value = StudentTab.CHAT

        // Add user follow-up text
        _chatMessages.update { it + ChatMessage(text = "Can you explain this solution in simpler terms?", isUser = true) }

        val thinkingId = System.currentTimeMillis() + 1
        _chatMessages.update { it + ChatMessage(id = thinkingId, text = "...", isUser = false, isLoading = true) }

        viewModelScope.launch {
            aiRepo.explainMore(sol.mainAnswer, _subject.value)
                .onSuccess { simplified ->
                    _chatMessages.update { it.filter { m -> m.id != thinkingId } + ChatMessage(text = simplified, isUser = false) }
                }
                .onFailure { e ->
                    _chatMessages.update { it.filter { m -> m.id != thinkingId } + ChatMessage(text = "⚠️ ${e.message}", isUser = false, hasError = true) }
                }
            _isThinking.value = false
        }
    }

    // ── Read Solution Aloud ───────────────────────────────────────────────────
    fun speakSolution() {
        val currentState = _scanState.value as? StudentScanState.Solved ?: return
        val sol = currentState.solution

        val textToSpeak = buildString {
            append("The main answer is: ${sol.mainAnswer}. ")
            if (sol.steps.isNotEmpty()) {
                append("Here are the step by step explanations. ")
                sol.steps.forEachIndexed { index, step ->
                    append("Step ${index + 1}: $step. ")
                }
            }
            sol.quickTip?.let { append("Quick tip: $it") }
        }

        tts?.language = Locale.ENGLISH
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun toggleFlash() { _flashEnabled.update { !it } }

    fun setSubject(s: Subject) { _subject.value = s }

    fun setTab(t: StudentTab) { _activeTab.value = t }

    fun resetScanner() {
        tts?.stop()
        _scanState.value = StudentScanState.CameraActive
        _overlayBlocks.value = emptyList()
        _chatMessages.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        ocrJob?.cancel()
    }
}

enum class StudentTab(val label: String, val emoji: String) {
    CHAT    ("Chat",     "💬"),
    SOLUTION("Solution", "📋"),
    PRACTICE("Practice", "🏋️"),
    NOTES   ("Notes",    "📓")
}
