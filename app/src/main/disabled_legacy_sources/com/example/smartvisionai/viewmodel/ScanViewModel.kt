package com.example.smartvisionai.viewmodel

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvisionai.ml.GeminiRepository
import com.example.smartvisionai.ml.MLKitRepository
import com.example.smartvisionai.ml.TFLiteRepository
import com.example.smartvisionai.repository.ScanHistoryRepository
import com.example.smartvisionai.ui.components.DetectionBox
import com.example.smartvisionai.ui.components.toDetectionBox
import com.example.smartvisionai.ui.screens.ScanMode
import com.example.smartvisionai.utils.SpeechResult
import com.example.smartvisionai.utils.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val selectedMode: ScanMode           = ScanMode.OBJECT,
    val isProcessing: Boolean            = false,
    val result: String?                  = null,
    val cameraFacing: Int                = CameraSelector.LENS_FACING_BACK,
    val isOnline: Boolean                = true,
    val statusLabel: String?             = "Rear · Object Detect",
    val errorMessage: String?            = null,
    val detectionBoxes: List<DetectionBox> = emptyList(),
    val isListeningVoice: Boolean        = false,
    val voicePartialText: String         = "",
    val previewWidth: Int                = 640,
    val previewHeight: Int               = 480
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val mlKitRepo: MLKitRepository,
    private val geminiRepo: GeminiRepository,
    private val tfliteRepo: TFLiteRepository,
    private val historyRepo: ScanHistoryRepository,
    private val voiceManager: VoiceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var analyzeJob: Job? = null
    private var voiceJob: Job? = null
    private var lastAnalyzedTime = 0L
    private val THROTTLE_MS = 2000L

    // ── Mode / Camera ─────────────────────────────────────────────────────────

    fun setMode(mode: ScanMode) {
        _uiState.update {
            it.copy(
                selectedMode   = mode,
                result         = null,
                detectionBoxes = emptyList(),
                statusLabel    = "${facingLabel()} · ${mode.label}"
            )
        }
    }

    fun flipCamera() {
        val newFacing = if (_uiState.value.cameraFacing == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        _uiState.update {
            it.copy(
                cameraFacing = newFacing,
                statusLabel  = "${if (newFacing == CameraSelector.LENS_FACING_BACK) "Rear" else "Front"} · ${it.selectedMode.label}"
            )
        }
    }

    fun setPreviewSize(w: Int, h: Int) {
        _uiState.update { it.copy(previewWidth = w, previewHeight = h) }
    }

    // ── Live Frame Analysis (throttled) ───────────────────────────────────────

    fun analyzeFrame(bitmap: Bitmap) {
        val now = System.currentTimeMillis()
        if (now - lastAnalyzedTime < THROTTLE_MS) return
        if (_uiState.value.isProcessing) return
        lastAnalyzedTime = now

        viewModelScope.launch {
            runCatching {
                when (_uiState.value.selectedMode) {
                    ScanMode.OBJECT -> {
                        val objects = mlKitRepo.detectObjects(bitmap)
                        val boxes   = objects.mapIndexed { i, o -> o.toDetectionBox(i) }
                        val top     = objects.firstOrNull()?.labels?.firstOrNull()
                        _uiState.update {
                            it.copy(
                                detectionBoxes = boxes,
                                statusLabel    = if (top != null)
                                    "${facingLabel()} · ${top.text} ${(top.confidence * 100).toInt()}%"
                                else "${facingLabel()} · Scanning…"
                            )
                        }
                    }
                    ScanMode.OCR -> {
                        val text = mlKitRepo.extractText(bitmap)
                        if (text.isNotBlank())
                            _uiState.update { it.copy(statusLabel = "${facingLabel()} · Text detected") }
                    }
                    else -> {
                        val labels = mlKitRepo.labelImage(bitmap)
                        labels.firstOrNull()?.let { lbl ->
                            _uiState.update {
                                it.copy(statusLabel = "${facingLabel()} · ${lbl.text} ${(lbl.confidence * 100).toInt()}%")
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Manual Capture → Full Analysis ────────────────────────────────────────

    /** Called by capture button. ScanScreen should then also call processCapture(bitmap). */
    fun captureAndAnalyze() {
        if (_uiState.value.isProcessing) { cancelProcessing(); return }
        _uiState.update { it.copy(isProcessing = true, result = null) }
    }

    fun processCapture(bitmap: Bitmap) {
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = runCatching {
                when (_uiState.value.selectedMode) {
                    ScanMode.OBJECT    -> processObjectDetection(bitmap)
                    ScanMode.OCR       -> processOCR(bitmap)
                    ScanMode.TRANSLATE -> processTranslation(bitmap)
                    ScanMode.STUDENT   -> processStudentHelper(bitmap)
                    ScanMode.MEDICAL   -> processMedical(bitmap)
                    ScanMode.WASTE     -> processWaste(bitmap)
                }
            }.getOrElse { e -> "⚠ Error: ${e.message}" }

            _uiState.update { it.copy(result = result, isProcessing = false) }
            saveHistory(result)
            voiceManager.speak(result.take(200))
        }
    }

    fun cancelProcessing() {
        analyzeJob?.cancel()
        _uiState.update { it.copy(isProcessing = false) }
    }

    // ── AI Processing Implementations ─────────────────────────────────────────

    private suspend fun processObjectDetection(bitmap: Bitmap): String {
        val tflite = tfliteRepo.detectObjectsTFLite(bitmap)
        if (tflite.isNotEmpty()) {
            return buildString {
                appendLine("🔍 Objects Detected:")
                tflite.take(5).forEach {
                    appendLine("• ${it.label.replaceFirstChar { c -> c.uppercase() }} — ${(it.confidence * 100).toInt()}%")
                }
            }.trim()
        }
        val objects = mlKitRepo.detectObjects(bitmap)
        val labels  = mlKitRepo.labelImage(bitmap)
        return buildString {
            if (objects.isNotEmpty()) {
                appendLine("🔍 Detected Objects:")
                objects.forEach { obj ->
                    val lbl = obj.labels.firstOrNull()
                    appendLine("• ${lbl?.text ?: "Unknown"} — ${((lbl?.confidence ?: 0f) * 100).toInt()}%")
                }
            } else if (labels.isNotEmpty()) {
                appendLine("🏷 Scene Labels:")
                labels.take(5).forEach { lbl ->
                    appendLine("• ${lbl.text} — ${(lbl.confidence * 100).toInt()}%")
                }
            } else {
                append("No objects detected. Try a clearer, well-lit image.")
            }
        }.trim()
    }

    private suspend fun processOCR(bitmap: Bitmap): String {
        val text = mlKitRepo.extractText(bitmap)
        return if (text.isNotBlank()) "📄 Extracted Text:\n\n$text"
        else "No text found. Ensure text is clearly visible and in focus."
    }

    private suspend fun processTranslation(bitmap: Bitmap): String {
        val text = mlKitRepo.extractText(bitmap)
        if (text.isBlank()) return "No text detected to translate."
        return runCatching {
            val translated = mlKitRepo.translateText(text, targetLanguage = "en")
            "🌐 Original:\n$text\n\n✅ Translated (English):\n$translated"
        }.getOrElse { "📄 Text found:\n$text\n\n⚠ Translation unavailable — download language pack in Settings." }
    }

    private suspend fun processStudentHelper(bitmap: Bitmap): String {
        val text = mlKitRepo.extractText(bitmap)
        if (text.isBlank()) return "No question or formula detected. Point at text clearly."
        return geminiRepo.explainForStudent(text)
    }

    private suspend fun processMedical(bitmap: Bitmap): String {
        val text = mlKitRepo.extractText(bitmap)
        if (text.isBlank()) return "No medicine label detected. Scan the packaging directly."
        return geminiRepo.analyzeMedicine(text)
    }

    private suspend fun processWaste(bitmap: Bitmap): String {
        val tflite = tfliteRepo.classifyWaste(bitmap)
        val queryLabel = if (tflite.isNotEmpty()) tflite.first().label
        else {
            val labels = mlKitRepo.labelImage(bitmap)
            if (labels.isEmpty()) return "Could not identify waste type. Ensure the item fills the frame."
            labels.take(5).joinToString(", ") { it.text }
        }
        return geminiRepo.classifyWaste(queryLabel)
    }

    // ── Voice I/O ─────────────────────────────────────────────────────────────

    fun startVoiceInput() {
        if (_uiState.value.isListeningVoice) { stopVoiceInput(); return }
        voiceJob?.cancel()
        voiceJob = viewModelScope.launch {
            _uiState.update { it.copy(isListeningVoice = true, voicePartialText = "") }
            voiceManager.startListening().collect { event ->
                when (event) {
                    is SpeechResult.Partial -> _uiState.update { it.copy(voicePartialText = event.text) }
                    is SpeechResult.Final   -> {
                        _uiState.update { it.copy(isListeningVoice = false, voicePartialText = "") }
                        if (event.text.isNotBlank()) askQuestion(event.text)
                    }
                    else -> _uiState.update { it.copy(isListeningVoice = false) }
                }
            }
        }
    }

    fun stopVoiceInput() {
        voiceJob?.cancel()
        _uiState.update { it.copy(isListeningVoice = false, voicePartialText = "") }
    }

    fun askQuestion(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val answer = runCatching { geminiRepo.chat(query) }
                .getOrElse { e -> "⚠ ${e.message}" }
            _uiState.update { it.copy(result = answer, isProcessing = false) }
            voiceManager.speak(answer.take(200))
        }
    }

    fun clearResult() = _uiState.update { it.copy(result = null, detectionBoxes = emptyList()) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun facingLabel() =
        if (_uiState.value.cameraFacing == CameraSelector.LENS_FACING_BACK) "Rear" else "Front"

    private fun saveHistory(result: String) {
        viewModelScope.launch {
            historyRepo.insertScan(
                moduleId   = _uiState.value.selectedMode.name.lowercase(),
                title      = result.lines().firstOrNull()
                    ?.trimStart { !it.isLetterOrDigit() && !it.isWhitespace() }
                    ?.take(60) ?: "Scan result",
                confidence = 0
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
