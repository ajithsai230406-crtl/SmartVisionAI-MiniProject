package com.smartvision.ai.waste.presentation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.FirebaseRepository
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.waste.data.WasteClassifierRepository
import com.smartvision.ai.waste.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WasteClassifierViewModel @Inject constructor(
    private val repo:        WasteClassifierRepository,
    private val historyRepo: HistoryRepository,
    private val firebase:    FirebaseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _scanState  = MutableStateFlow<WasteScanState>(WasteScanState.Idle)
    val scanState: StateFlow<WasteScanState> = _scanState.asStateFlow()

    private val _flashEnabled = MutableStateFlow(false)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled.asStateFlow()

    private val _sessionHistory = MutableStateFlow<List<WasteResult>>(emptyList())
    val sessionHistory: StateFlow<List<WasteResult>> = _sessionHistory.asStateFlow()

    /** Live result from real-time frame analysis (not yet confirmed / saved) */
    private val _liveResult = MutableStateFlow<WasteResult?>(null)
    val liveResult: StateFlow<WasteResult?> = _liveResult.asStateFlow()

    private var tts: TextToSpeech? = null
    private var analysisJob: Job? = null
    private var liveFrameJob: Job? = null

    // Tracks last processed frame hash to skip identical frames
    private var lastFrameHash: Int = 0
    private var lastProcessedMs: Long = 0L
    private val LIVE_THROTTLE_MS = 2000L  // 2 seconds between live classifications

    init { tts = TextToSpeech(context) { /* ready */ } }

    /**
     * Real-time live frame classification.
     * Called by PreviewView bitmap polling in the screen.
     * Throttled to 2 seconds between calls to avoid overloading the classifier.
     */
    fun processLiveFrame(bitmap: Bitmap) {
        android.util.Log.d("WasteClassifier", "Live frame received for classification.")
        val now = System.currentTimeMillis()
        if (now - lastProcessedMs < LIVE_THROTTLE_MS) return

        val currentHash = repo.getBitmapHash(bitmap)
        if (currentHash == lastFrameHash) return   // Same frame — skip

        // Only classify when the scan is idle
        if (_scanState.value !is WasteScanState.Idle) return

        lastProcessedMs = now
        lastFrameHash = currentHash

        liveFrameJob?.cancel()
        liveFrameJob = viewModelScope.launch {
            android.util.Log.d("WasteClassifier", "Starting live classification on frame hash: $currentHash")
            repo.classifyFromBitmap(bitmap, isLive = true)
                .onSuccess { result ->
                    android.util.Log.d("WasteClassifier", "Live frame prediction generated: ${result.exactItemName} (${(result.confidence * 100).toInt()}%)")
                    _liveResult.value = result
                    // Transition to Classified so result panel shows live
                    if (_scanState.value is WasteScanState.Idle) {
                        _scanState.value = WasteScanState.Classified(result)
                    }
                }
                .onFailure { e ->
                    android.util.Log.e("WasteClassifier", "Live frame classification failed", e)
                }
        }
    }

    /** Manual capture — saves to Firebase + history */
    fun captureAndClassify(bitmap: Bitmap) = viewModelScope.launch {
        android.util.Log.d("WasteClassifier", "Manual capture frame received for classification.")
        _scanState.value = WasteScanState.Scanning
        repo.classifyFromBitmap(bitmap, isLive = false)
            .onSuccess { result ->
                android.util.Log.d("WasteClassifier", "Manual prediction generated: ${result.exactItemName} (${(result.confidence * 100).toInt()}%)")
                _scanState.value = WasteScanState.Classified(result)
                addToHistory(result)
                saveToFirebase(result)
            }
            .onFailure { e ->
                android.util.Log.e("WasteClassifier", "Manual classification failed", e)
                _scanState.value = WasteScanState.Error(e.message ?: "Classification failed")
            }
    }

    fun analyzeFromUri(context: Context, uri: Uri) = viewModelScope.launch {
        _scanState.value = WasteScanState.Scanning
        repo.classifyFromUri(context, uri)
            .onSuccess { result ->
                _scanState.value = WasteScanState.Classified(result)
                addToHistory(result)
                saveToFirebase(result)
            }
            .onFailure { _scanState.value = WasteScanState.Error(it.message ?: "Failed to analyze image") }
    }

    fun speakResult(result: WasteResult) {
        val text = "${result.exactItemName} detected. Recycling category is ${result.recyclingCategory}. Eco score is ${result.ecoScore} out of 100. Tip: ${result.sustainabilityTip}"
        tts?.language = Locale.ENGLISH
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun resetScan() {
        _scanState.value = WasteScanState.Idle
        _liveResult.value = null
        lastFrameHash = 0
    }
    fun toggleFlash()  { _flashEnabled.update { !it } }
    fun dismissError() { _scanState.value = WasteScanState.Idle }

    private fun addToHistory(result: WasteResult) {
        _sessionHistory.update { (listOf(result) + it).take(20) }
    }

    private suspend fun saveToFirebase(result: WasteResult) {
        val defaultKnow = WasteKnowledgeBase.getKnowledge(result.primaryType)

        historyRepo.save(
            "Waste",
            "${result.primaryType.emoji} ${result.exactItemName} — ${(result.confidence * 100).toInt()}% confidence",
            result.sustainabilityTip.ifBlank { defaultKnow.ecoTip }
        )

        firebase.saveWasteClassification(
            wasteType       = result.primaryType.displayName,
            exactItemName   = result.exactItemName.ifBlank { result.primaryType.displayName },
            recyclable      = result.isRecyclable,
            reusable        = result.isReusable,
            disposalMethod  = result.disposalMethod.ifBlank { result.primaryType.bin },
            carbonReduction = result.carbonReduction,
            carbonImpact    = result.carbonImpact,
            ecoScore        = result.ecoScore,
            recyclingRate   = defaultKnow.recyclingRate,
            confidence      = result.confidence
        )
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop(); tts?.shutdown()
        analysisJob?.cancel()
        liveFrameJob?.cancel()
    }
}
