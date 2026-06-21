package com.smartvision.ai.waste.presentation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.ScreenContext
import com.smartvision.ai.data.ScreenContextManager
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
    private val screenContextManager: ScreenContextManager,
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

    private fun saveBitmapToCache(bitmap: Bitmap): String? {
        return try {
            val file = java.io.File(context.cacheDir, "waste_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("WasteClassifier", "Failed to save bitmap to cache", e)
            null
        }
    }

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
                .onSuccess { results ->
                    if (results.isNotEmpty()) {
                        val result = results[0]
                        android.util.Log.d("WasteClassifier", "Live frame prediction generated: ${result.exactItemName} (${(result.confidence * 100).toInt()}%)")
                        _liveResult.value = result
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
        
        val cachedPath = saveBitmapToCache(bitmap)
        
        repo.classifyFromBitmap(bitmap, isLive = false)
            .onSuccess { results ->
                val filtered = results.filter { it.confidence >= 0.60f }
                if (filtered.isEmpty()) {
                    _scanState.value = WasteScanState.Error("Unable to classify confidently. Please rescan.")
                    return@onSuccess
                }

                val updatedResults = filtered.map { it.copy(imagePath = cachedPath) }
                _scanState.value = WasteScanState.Classified(updatedResults, selectedIndex = 0)
                
                val primaryResult = updatedResults[0]
                addToHistory(primaryResult)
                saveToFirebase(primaryResult)
                updateAssistantContext(primaryResult)
            }
            .onFailure { e ->
                android.util.Log.e("WasteClassifier", "Manual classification failed", e)
                _scanState.value = WasteScanState.Error(e.message ?: "Classification failed")
            }
    }

    fun analyzeFromUri(context: Context, uri: Uri) = viewModelScope.launch {
        _scanState.value = WasteScanState.Scanning
        val bitmap = try {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source)
        } catch (e: Exception) {
            null
        }
        if (bitmap == null) {
            _scanState.value = WasteScanState.Error("Failed to load image from gallery.")
            return@launch
        }
        val cachedPath = saveBitmapToCache(bitmap)

        repo.classifyFromUri(context, uri)
            .onSuccess { results ->
                val filtered = results.filter { it.confidence >= 0.60f }
                if (filtered.isEmpty()) {
                    _scanState.value = WasteScanState.Error("Unable to classify confidently. Please rescan.")
                    return@onSuccess
                }

                val updatedResults = filtered.map { it.copy(imagePath = cachedPath) }
                _scanState.value = WasteScanState.Classified(updatedResults, selectedIndex = 0)
                
                val primaryResult = updatedResults[0]
                addToHistory(primaryResult)
                saveToFirebase(primaryResult)
                updateAssistantContext(primaryResult)
            }
            .onFailure { _scanState.value = WasteScanState.Error(it.message ?: "Failed to analyze image") }
    }

    fun selectClassifiedItem(index: Int) {
        val currentState = _scanState.value
        if (currentState is WasteScanState.Classified) {
            if (index in 0 until currentState.results.size) {
                _scanState.value = currentState.copy(selectedIndex = index)
                updateAssistantContext(currentState.results[index])
            }
        }
    }

    private fun updateAssistantContext(result: WasteResult) {
        screenContextManager.updateContext(
            ScreenContext.WasteContext(
                itemName = result.exactItemName.ifBlank { result.primaryType.displayName },
                wasteType = result.primaryType.displayName,
                confidence = result.confidence,
                isRecyclable = result.isRecyclable,
                disposalMethod = result.disposalMethod.ifBlank { result.primaryType.bin },
                environmentalImpact = result.environmentalImpact.ifBlank { WasteKnowledgeBase.getKnowledge(result.primaryType).description },
                recyclingSuggestions = result.sustainabilityTip.ifBlank { WasteKnowledgeBase.getKnowledge(result.primaryType).ecoTip },
                safetyPrecautions = result.safetyPrecautions.ifBlank { "Handle with care." },
                carbonImpact = result.carbonReduction
            ),
            moduleName = "Waste Classifier"
        )
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

        val wasteHistJson = com.smartvision.ai.domain.model.HistoryJsonSerializer.serializeWaste(
            com.smartvision.ai.domain.model.WasteHistoryDetail(
                wasteType = result.primaryType.displayName,
                exactItemName = result.exactItemName,
                confidence = result.confidence,
                disposalMethod = result.disposalMethod.ifBlank { result.primaryType.bin },
                disposalSteps = result.disposalSteps.ifEmpty { defaultKnow.disposalSteps },
                reuseSuggestions = result.reuseSuggestions.ifBlank { defaultKnow.reusabilityInfo },
                environmentalImpact = result.environmentalImpact.ifBlank { defaultKnow.carbonFootprint },
                ecoScore = result.ecoScore,
                carbonReduction = result.carbonReduction,
                carbonImpact = result.carbonImpact,
                isRecyclable = result.isRecyclable,
                isReusable = result.isReusable,
                sustainabilityTip = result.sustainabilityTip.ifBlank { defaultKnow.ecoTip },
                imagePath = result.imagePath,
                safetyPrecautions = result.safetyPrecautions
            )
        )

        historyRepo.save(
            "Waste",
            "${result.primaryType.emoji} ${result.exactItemName} — ${(result.confidence * 100).toInt()}% confidence",
            result.sustainabilityTip.ifBlank { defaultKnow.ecoTip },
            wasteHistJson
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
