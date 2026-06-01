package com.smartvision.ai.ocr.presentation

import android.content.Context
import android.media.Image
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.ocr.data.LanguageDetectionRepository
import com.smartvision.ai.ocr.data.OcrRecognitionRepository
import com.smartvision.ai.ocr.domain.*
import com.smartvision.ai.translator.data.SmartTranslatorRepository
import com.smartvision.ai.translator.domain.ALL_LANGUAGES
import com.smartvision.ai.translator.domain.languageByCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class OcrTranslatorViewModel @Inject constructor(
    private val ocrRepo:        OcrRecognitionRepository,
    private val langRepo:       LanguageDetectionRepository,
    private val translationRepo: SmartTranslatorRepository,
    private val historyRepo:    HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Scan State ────────────────────────────────────────────────────────────
    private val _scanState = MutableStateFlow<OcrScanState>(OcrScanState.Idle)
    val scanState: StateFlow<OcrScanState> = _scanState.asStateFlow()

    // ── Translation State ─────────────────────────────────────────────────────
    private val _translationState = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> = _translationState.asStateFlow()

    // ── Overlay Blocks ────────────────────────────────────────────────────────
    private val _overlayBlocks = MutableStateFlow<List<RecognizedBlock>>(emptyList())
    val overlayBlocks: StateFlow<List<RecognizedBlock>> = _overlayBlocks.asStateFlow()

    // ── Selected Block (for Google Lens style selective scanning) ──────────────
    private val _selectedBlock = MutableStateFlow<RecognizedBlock?>(null)
    val selectedBlock: StateFlow<RecognizedBlock?> = _selectedBlock.asStateFlow()

    // ── Target Language ───────────────────────────────────────────────────────
    private val _targetLanguage = MutableStateFlow("Hindi")
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    // ── Frozen Text (user tapped to freeze frame) ─────────────────────────────
    private val _frozenResult = MutableStateFlow<OcrResult?>(null)
    val frozenResult: StateFlow<OcrResult?> = _frozenResult.asStateFlow()

    // ── UI Visibility ─────────────────────────────────────────────────────────
    private val _isLiveScan = MutableStateFlow(true)
    val isLiveScan: StateFlow<Boolean> = _isLiveScan.asStateFlow()

    private val _flashEnabled = MutableStateFlow(false)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled.asStateFlow()

    val supportedLanguages: List<String> get() = ALL_LANGUAGES.map { it.displayName }

    // TTS
    private var tts: TextToSpeech? = null
    private var analysisJob: Job? = null
    private var lastAnalyzedText = ""
    private val imageWidth  = MutableStateFlow(1)
    private val imageHeight = MutableStateFlow(1)

    init {
        tts = TextToSpeech(context) { /* init ok */ }
    }

    // ── Called every camera frame (debounced) ─────────────────────────────────
    fun analyzeFrame(image: Image, rotation: Int, width: Int, height: Int) {
        if (!_isLiveScan.value) return
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            delay(300) // debounce - don't process every single frame
            imageWidth.value  = width
            imageHeight.value = height

            ocrRepo.recognizeFromImage(image, rotation)
                .onSuccess { mlText ->
                    val domain = mlText.toDomain(width, height)
                    if (domain.fullText.isBlank()) {
                        _scanState.value    = OcrScanState.NoText
                        _overlayBlocks.value = emptyList()
                    } else {
                        // Only re-detect language if text changed significantly
                        val newText = domain.fullText
                        val enriched = if (newText != lastAnalyzedText) {
                            lastAnalyzedText = newText
                            val langCode = langRepo.identifyLanguage(newText)
                                .getOrDefault("und")
                            domain.copy(
                                detectedLanguage     = langCode,
                                detectedLanguageName = langRepo.languageDisplayName(langCode)
                            )
                        } else domain

                        _overlayBlocks.value = enriched.blocks
                        _scanState.value     = OcrScanState.TextDetected(enriched)
                    }
                }
                .onFailure { _scanState.value = OcrScanState.Error(it.message ?: "OCR failed") }
        }
    }

    /** Freeze current scan (user tapped to capture) */
    fun freezeAndCapture() {
        val current = (_scanState.value as? OcrScanState.TextDetected)?.result ?: return
        _frozenResult.value = current
        _isLiveScan.value   = false
        _scanState.value    = OcrScanState.TextDetected(current)
    }

    /** Resume live scanning */
    fun resumeLiveScan() {
        _frozenResult.value      = null
        _isLiveScan.value        = true
        _translationState.value  = TranslationState.Idle
        _overlayBlocks.value     = emptyList()
        _selectedBlock.value     = null
    }

    /** Select a block for selective scanning */
    fun selectBlock(block: RecognizedBlock?) {
        _selectedBlock.value = block
        _translationState.value = TranslationState.Idle
    }

    /** Analyze image from gallery URI */
    fun analyzeFromGallery(context: Context, uri: Uri) = viewModelScope.launch {
        _scanState.value = OcrScanState.Scanning
        _selectedBlock.value = null
        ocrRepo.recognizeFromUri(context, uri)
            .onSuccess { mlText ->
                val domain = mlText.toDomain(100, 100)
                if (domain.fullText.isBlank()) {
                    _scanState.value = OcrScanState.NoText
                } else {
                    val langCode = langRepo.identifyLanguage(domain.fullText).getOrDefault("und")
                    val enriched = domain.copy(
                        detectedLanguage     = langCode,
                        detectedLanguageName = langRepo.languageDisplayName(langCode)
                    )
                    _frozenResult.value = enriched
                    _isLiveScan.value   = false
                    _scanState.value    = OcrScanState.TextDetected(enriched)
                }
            }
            .onFailure { _scanState.value = OcrScanState.Error(it.message ?: "Failed to read image") }
    }

    /** Translate the current frozen/detected text or selected block */
    fun translateCurrentText() {
        val text = _selectedBlock.value?.text
            ?: (_frozenResult.value ?: (_scanState.value as? OcrScanState.TextDetected)?.result)?.fullText
            ?: return

        val sourceLang = (_frozenResult.value
            ?: (_scanState.value as? OcrScanState.TextDetected)?.result)
            ?.detectedLanguage ?: "und"

        val sourceLangObj = languageByCode(sourceLang)
        val targetLangObj = ALL_LANGUAGES.firstOrNull { it.displayName.equals(_targetLanguage.value, ignoreCase = true) }
            ?: ALL_LANGUAGES.first { it.code == "hi" }

        viewModelScope.launch {
            _translationState.value = TranslationState.Downloading
            translationRepo.translate(text, sourceLangObj, targetLangObj)
                .onSuccess { result ->
                    _translationState.value = TranslationState.Success(
                        translatedText = result.translatedText,
                        sourceLang     = result.detectedLang.displayName,
                        targetLang     = result.targetLang.displayName,
                        nativeMeaning  = result.nativeMeaning,
                        semanticMeaning = result.semanticMeaning
                    )
                    val historyTitle = if (_selectedBlock.value != null) "Selective OCR → ${result.targetLang.displayName}" else "OCR → ${result.targetLang.displayName}"
                    historyRepo.save("OCR", historyTitle, result.translatedText.take(220))
                }
                .onFailure {
                    _translationState.value = TranslationState.Error(
                        it.message ?: "Translation failed. Check internet connection."
                    )
                }
        }
    }

    fun setTargetLanguage(lang: String) {
        _targetLanguage.value       = lang
        _translationState.value     = TranslationState.Idle
    }

    fun toggleFlash() { _flashEnabled.update { !it } }

    /** Text-to-Speech for translated or original text */
    fun speak(text: String, languageCode: String = "en") {
        val locale = when (languageCode) {
            "hi" -> Locale("hi", "IN")
            "te" -> Locale("te", "IN")
            "ta" -> Locale("ta", "IN")
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "es" -> Locale("es", "ES")
            "ja" -> Locale.JAPANESE
            "zh" -> Locale.CHINESE
            else -> Locale.ENGLISH
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun dismissError() { _scanState.value = OcrScanState.Idle }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        analysisJob?.cancel()
    }
}
