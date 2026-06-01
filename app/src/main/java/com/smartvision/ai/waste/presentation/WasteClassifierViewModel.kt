package com.smartvision.ai.waste.presentation

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.waste.data.WasteClassifierRepository
import com.smartvision.ai.waste.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WasteClassifierViewModel @Inject constructor(
    private val repo:        WasteClassifierRepository,
    private val historyRepo: HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _scanState  = MutableStateFlow<WasteScanState>(WasteScanState.Idle)
    val scanState: StateFlow<WasteScanState> = _scanState.asStateFlow()

    private val _flashEnabled = MutableStateFlow(false)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled.asStateFlow()

    private val _sessionHistory = MutableStateFlow<List<WasteResult>>(emptyList())
    val sessionHistory: StateFlow<List<WasteResult>> = _sessionHistory.asStateFlow()

    private var tts: TextToSpeech? = null
    private var analysisJob: Job? = null

    init { tts = TextToSpeech(context) { /* ready */ } }

    fun captureAndClassify(bitmap: android.graphics.Bitmap) = viewModelScope.launch {
        _scanState.value = WasteScanState.Scanning
        repo.classifyFromBitmap(bitmap)
            .onSuccess { result ->
                _scanState.value = WasteScanState.Classified(result)
                addToHistory(result)
                saveToHistory(result)
            }
            .onFailure { _scanState.value = WasteScanState.Error(it.message ?: "Classification failed") }
    }

    fun analyzeFromUri(context: Context, uri: Uri) = viewModelScope.launch {
        _scanState.value = WasteScanState.Scanning
        repo.classifyFromUri(context, uri)
            .onSuccess { result ->
                _scanState.value = WasteScanState.Classified(result)
                addToHistory(result)
                saveToHistory(result)
            }
            .onFailure { _scanState.value = WasteScanState.Error(it.message ?: "Failed to analyze image") }
    }

    fun speakResult(result: WasteResult) {
        val know = WasteKnowledgeBase.getKnowledge(result.primaryType)
        val text = "${result.primaryType.displayName} waste detected. " +
                "${know.ecoTip} Disposal: ${know.disposalSteps.firstOrNull() ?: "Place in ${result.primaryType.bin}."}"
        tts?.language = Locale.ENGLISH
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun resetScan()       { _scanState.value = WasteScanState.Idle }
    fun toggleFlash()     { _flashEnabled.update { !it } }
    fun dismissError()    { _scanState.value = WasteScanState.Idle }

    private fun addToHistory(result: WasteResult) {
        _sessionHistory.update { (listOf(result) + it).take(20) }
    }

    private suspend fun saveToHistory(result: WasteResult) {
        val know = WasteKnowledgeBase.getKnowledge(result.primaryType)
        historyRepo.save(
            "Waste",
            "${result.primaryType.emoji} ${result.primaryType.displayName} — ${(result.confidence * 100).toInt()}% confidence",
            know.ecoTip
        )
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop(); tts?.shutdown()
        analysisJob?.cancel()
    }
}
