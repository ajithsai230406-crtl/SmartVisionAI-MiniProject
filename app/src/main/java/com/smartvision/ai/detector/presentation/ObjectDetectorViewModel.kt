package com.smartvision.ai.detector.presentation

import android.content.Context
import android.media.Image
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.detector.data.ObjectDetectionRepository
import com.smartvision.ai.detector.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ObjectDetectorViewModel @Inject constructor(
    private val detectionRepo: ObjectDetectionRepository,
    private val historyRepo:   HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Detection state ───────────────────────────────────────────────────────
    private val _detectionState = MutableStateFlow<DetectionState>(DetectionState.Idle)
    val detectionState: StateFlow<DetectionState> = _detectionState.asStateFlow()

    // ── Selected object for detail card ───────────────────────────────────────
    private val _selectedItem = MutableStateFlow<DetectedItem?>(null)
    val selectedItem: StateFlow<DetectedItem?> = _selectedItem.asStateFlow()

    // ── Knowledge for selected object ─────────────────────────────────────────
    private val _knowledge = MutableStateFlow<ObjectKnowledge?>(null)
    val knowledge: StateFlow<ObjectKnowledge?> = _knowledge.asStateFlow()

    // ── Detection history (current session) ───────────────────────────────────
    private val _sessionHistory = MutableStateFlow<List<DetectionHistoryEntry>>(emptyList())
    val sessionHistory: StateFlow<List<DetectionHistoryEntry>> = _sessionHistory.asStateFlow()

    // ── Flash & overlay toggles ───────────────────────────────────────────────
    private val _flashEnabled   = MutableStateFlow(false)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled.asStateFlow()

    private val _showOverlay    = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()

    private val _isLive         = MutableStateFlow(true)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    // ── FPS counter ───────────────────────────────────────────────────────────
    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private var analysisJob:   Job? = null
    private var fpsFrameCount  = 0
    private var fpsLastTime    = System.currentTimeMillis()
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { /* ready */ }
        startFpsCounter()
    }

    // ── Live frame analysis (called every camera frame) ───────────────────────
    fun analyzeFrame(image: Image, rotation: Int, width: Int, height: Int) {
        if (!_isLive.value) return
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            delay(80) // ~12 fps cap to avoid overloading
            val t0 = System.currentTimeMillis()
            detectionRepo.detectFromImage(image, rotation)
                .onSuccess { items ->
                    updateFps(t0)
                    if (items.isEmpty()) {
                        _detectionState.value = DetectionState.NoObjects
                    } else {
                        _detectionState.value = DetectionState.Active(items, System.currentTimeMillis() - t0)
                        // Auto-select first item if none selected
                        if (_selectedItem.value == null) selectItem(items.first())
                    }
                }
                .onFailure {
                    _detectionState.value = DetectionState.Error(it.message ?: "Detection failed")
                }
        }
    }

    /** Detect from gallery URI */
    fun analyzeFromUri(context: Context, uri: Uri) = viewModelScope.launch {
        _isLive.value         = false
        _detectionState.value = DetectionState.Scanning
        detectionRepo.detectFromUri(context, uri)
            .onSuccess { items ->
                if (items.isEmpty()) {
                    _detectionState.value = DetectionState.NoObjects
                } else {
                    _detectionState.value = DetectionState.Active(items, 0L)
                    selectItem(items.first())
                    addToSessionHistory(items)
                }
            }
            .onFailure { _detectionState.value = DetectionState.Error(it.message ?: "Failed to analyze image") }
    }

    /** Select a detected object to show detail card */
    fun selectItem(item: DetectedItem) {
        _selectedItem.value = item
        val know = ObjectKnowledgeBase.getKnowledge(item.label)
        _knowledge.value    = know
    }

    fun dismissDetail() {
        _selectedItem.value = null
        _knowledge.value    = null
    }

    fun resumeLive() {
        _isLive.value         = true
        _selectedItem.value   = null
        _knowledge.value      = null
        _detectionState.value = DetectionState.Idle
    }

    /** Speak the object name and description */
    fun speakObject(item: DetectedItem) {
        val know = ObjectKnowledgeBase.getKnowledge(item.label)
        val text = "${item.label}. ${know.description}"
        tts?.language = Locale.ENGLISH
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        // Save to history
        viewModelScope.launch {
            historyRepo.save(
                "Object",
                "${item.label} detected (${(item.confidence * 100).toInt()}%)",
                know.description.take(220)
            )
        }
    }

    fun toggleFlash()   { _flashEnabled.update { !it } }
    fun toggleOverlay() { _showOverlay.update  { !it } }
    fun dismissError()  { _detectionState.value = DetectionState.Idle }

    // ── Session history helpers ────────────────────────────────────────────────
    private fun addToSessionHistory(items: List<DetectedItem>) {
        val entries = items.map {
            DetectionHistoryEntry(
                label      = it.label,
                confidence = it.confidence,
                category   = it.category,
                timestamp  = System.currentTimeMillis()
            )
        }
        _sessionHistory.update { (entries + it).take(30) }
    }

    private fun updateFps(frameStartMs: Long) {
        fpsFrameCount++
        val elapsed = System.currentTimeMillis() - fpsLastTime
        if (elapsed >= 1000L) {
            _fps.value    = fpsFrameCount
            fpsFrameCount = 0
            fpsLastTime   = System.currentTimeMillis()
        }
    }

    private fun startFpsCounter() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                // fps updated inside updateFps()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        analysisJob?.cancel()
    }
}

// ─── Session history entry ────────────────────────────────────────────────────
data class DetectionHistoryEntry(
    val label:      String,
    val confidence: Float,
    val category:   ObjectCategory,
    val timestamp:  Long
)
