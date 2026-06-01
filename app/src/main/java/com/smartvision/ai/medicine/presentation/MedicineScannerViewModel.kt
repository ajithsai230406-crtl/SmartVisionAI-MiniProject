package com.smartvision.ai.medicine.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.medicine.data.MedicineScannerRepository
import com.smartvision.ai.medicine.domain.MedicineInfo
import com.smartvision.ai.medicine.domain.MedicineScanRecord
import com.smartvision.ai.medicine.domain.MedicineScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicineScannerViewModel @Inject constructor(
    private val repository: MedicineScannerRepository
) : ViewModel() {

    // ── UI State ──────────────────────────────────────────────────────────────
    private val _state = MutableStateFlow<MedicineScanState>(MedicineScanState.Idle)
    val state: StateFlow<MedicineScanState> = _state.asStateFlow()

    // ── Search & Navigation ───────────────────────────────────────────────────
    var searchQuery      by mutableStateOf("")
        private set
    var selectedTab      by mutableStateOf(0)
        private set
    var showDisclaimer   by mutableStateOf(false)
        private set
    var showCamera       by mutableStateOf(false)
        private set
    var isSpeaking       by mutableStateOf(false)
        private set
    var showHistory      by mutableStateOf(false)
        private set
    var selectedLanguage by mutableStateOf("English")
        private set
    var isFavorite       by mutableStateOf(false)
        private set

    // ── History & Favorites ───────────────────────────────────────────────────
    val history   = repository.history
    val favorites = repository.favorites

    // ── Search suggestions ────────────────────────────────────────────────────
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private var analyzeJob: Job? = null

    init {
        // Initialize TTS
        repository.initTts { /* ready */ }
        // Show disclaimer on first open
        showDisclaimer = true
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    fun onQueryChange(q: String) {
        searchQuery = q
        _suggestions.value = repository.getSearchSuggestions(q)
    }

    fun analyzeByName(name: String = searchQuery) {
        if (name.isBlank()) return
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _state.value = MedicineScanState.OcrRunning(0.2f)
            delay(300)
            _state.value = MedicineScanState.AiAnalyzing(0.6f)
            repository.analyzeByName(name)
                .onSuccess { info ->
                    isFavorite = false
                    _state.value = MedicineScanState.Result(info)
                }
                .onFailure { e ->
                    _state.value = MedicineScanState.Error(e.message ?: "Analysis failed")
                }
        }
    }

    fun analyzeFromOcr(ocrText: String) {
        if (ocrText.isBlank()) {
            _state.value = MedicineScanState.Error("No text detected. Try again with a clearer image.")
            return
        }
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _state.value = MedicineScanState.OcrRunning(0.4f)
            delay(500)
            _state.value = MedicineScanState.AiAnalyzing(0.75f)
            repository.analyzeFromOcrText(ocrText)
                .onSuccess { info ->
                    isFavorite = false
                    showCamera = false
                    _state.value = MedicineScanState.Result(info)
                }
                .onFailure { e ->
                    _state.value = MedicineScanState.Error(e.message ?: "OCR analysis failed")
                }
        }
    }

    // Quick demo — random from offline DB
    fun quickDemoScan() {
        val demoMedicines = listOf("Paracetamol", "Ibuprofen", "Amoxicillin", "Cetirizine", "Omeprazole")
        analyzeByName(demoMedicines.random())
    }

    // ── Voice TTS ──────────────────────────────────────────────────────────────
    fun toggleVoice(info: MedicineInfo) {
        if (isSpeaking) {
            repository.stopSpeaking()
            isSpeaking = false
        } else {
            repository.speakMedicineSummary(info)
            isSpeaking = true
        }
    }

    fun stopVoice() {
        repository.stopSpeaking()
        isSpeaking = false
    }

    // ── Favorites ──────────────────────────────────────────────────────────────
    fun toggleFavorite(record: MedicineScanRecord) {
        repository.toggleFavorite(record)
        isFavorite = !isFavorite
    }

    fun toggleCurrentFavorite() {
        val current = (_state.value as? MedicineScanState.Result)?.info ?: return
        val record  = history.value.firstOrNull { it.info.name == current.name }
            ?: MedicineScanRecord(info = current)
        repository.toggleFavorite(record)
        isFavorite = !isFavorite
    }

    // ── History ────────────────────────────────────────────────────────────────
    fun deleteRecord(id: String) = repository.deleteRecord(id)
    fun clearHistory()           = repository.clearHistory()
    fun openHistory()            { showHistory = true }
    fun closeHistory()           { showHistory = false }
    fun reanalyze(record: MedicineScanRecord) {
        closeHistory()
        analyzeByName(record.info.name)
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    fun openCamera()       { showCamera = true }
    fun closeCamera()      { showCamera = false }
    fun selectTab(t: Int)  { selectedTab = t }
    fun dismissDisclaimer(){ showDisclaimer = false }
    fun setLanguage(lang: String) { selectedLanguage = lang }
    fun reset() {
        analyzeJob?.cancel()
        _state.value = MedicineScanState.Idle
        searchQuery = ""
        selectedTab = 0
        isSpeaking = false
        stopVoice()
    }

    override fun onCleared() {
        super.onCleared()
        repository.shutdownTts()
    }
}
