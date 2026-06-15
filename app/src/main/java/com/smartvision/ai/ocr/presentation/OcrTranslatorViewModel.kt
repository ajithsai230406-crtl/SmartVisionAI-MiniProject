package com.smartvision.ai.ocr.presentation

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.local.OcrScanDao
import com.smartvision.ai.data.local.OcrScanEntity
import com.smartvision.ai.data.repository.FirebaseRepository
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.ocr.data.LanguageDetectionRepository
import com.smartvision.ai.ocr.data.OcrRecognitionRepository
import com.smartvision.ai.ocr.data.OcrScript
import com.smartvision.ai.ocr.domain.*
import com.smartvision.ai.translator.data.SmartTranslatorRepository
import com.smartvision.ai.translator.domain.ALL_LANGUAGES
import com.smartvision.ai.translator.domain.Language
import com.smartvision.ai.translator.domain.languageByCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OcrTranslatorViewModel @Inject constructor(
    private val ocrRepo:         OcrRecognitionRepository,
    private val langRepo:        LanguageDetectionRepository,
    private val translationRepo: SmartTranslatorRepository,
    private val historyRepo:     HistoryRepository,
    private val firebase:        FirebaseRepository,
    private val ocrScanDao:      OcrScanDao,
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

    // ── Overlay Paragraphs ────────────────────────────────────────────────────
    private val _overlayParagraphs = MutableStateFlow<List<RecognizedParagraph>>(emptyList())
    val overlayParagraphs: StateFlow<List<RecognizedParagraph>> = _overlayParagraphs.asStateFlow()

    // ── Selected Block ────────────────────────────────────────────────────────
    private val _selectedBlock = MutableStateFlow<RecognizedBlock?>(null)
    val selectedBlock: StateFlow<RecognizedBlock?> = _selectedBlock.asStateFlow()

    // ── Selected Paragraph ────────────────────────────────────────────────────
    private val _selectedParagraph = MutableStateFlow<RecognizedParagraph?>(null)
    val selectedParagraph: StateFlow<RecognizedParagraph?> = _selectedParagraph.asStateFlow()

    // ── Target Language ───────────────────────────────────────────────────────
    private val _targetLanguage = MutableStateFlow("Hindi")
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    // ── Source Language ───────────────────────────────────────────────────────
    private val _sourceLanguage = MutableStateFlow<Language>(Language("auto", "Auto-detect", "🌐", "Auto"))
    val sourceLanguage: StateFlow<Language> = _sourceLanguage.asStateFlow()

    // ── OCR Mode (TEXT vs TRANSLATE) ──────────────────────────────────────────
    private val _ocrMode = MutableStateFlow<OcrMode>(OcrMode.TRANSLATE)
    val ocrMode: StateFlow<OcrMode> = _ocrMode.asStateFlow()

    // ── Block Translations ────────────────────────────────────────────────────
    private val _blockTranslations = MutableStateFlow<Map<String, String>>(emptyMap())
    val blockTranslations: StateFlow<Map<String, String>> = _blockTranslations.asStateFlow()

    private val _isTranslatingBlocks = MutableStateFlow(false)
    val isTranslatingBlocks: StateFlow<Boolean> = _isTranslatingBlocks.asStateFlow()

    // Dynamic auto-detect script state
    private val activeAutoScript = MutableStateFlow(OcrScript.LATIN)

    // ── Frozen Text ───────────────────────────────────────────────────────────
    private val _frozenResult = MutableStateFlow<OcrResult?>(null)
    val frozenResult: StateFlow<OcrResult?> = _frozenResult.asStateFlow()

    // ── Live Scan Mode ────────────────────────────────────────────────────────
    private val _isLiveScan = MutableStateFlow(true)
    val isLiveScan: StateFlow<Boolean> = _isLiveScan.asStateFlow()

    // ── Flash / Torch ─────────────────────────────────────────────────────────
    private val _flashEnabled = MutableStateFlow(false)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled.asStateFlow()

    // ── Preprocessing toggle ──────────────────────────────────────────────────
    private val _preprocessingEnabled = MutableStateFlow(true)
    val preprocessingEnabled: StateFlow<Boolean> = _preprocessingEnabled.asStateFlow()

    // ── Detected script ───────────────────────────────────────────────────────
    private val _detectedScript = MutableStateFlow("Latin")
    val detectedScript: StateFlow<String> = _detectedScript.asStateFlow()

    // ── Scan quality (0–1) ────────────────────────────────────────────────────
    private val _scanQuality = MutableStateFlow(0f)
    val scanQuality: StateFlow<Float> = _scanQuality.asStateFlow()

    // ── OCR Scan History ──────────────────────────────────────────────────────
    val scanHistory: StateFlow<List<OcrScanEntity>> = ocrScanDao.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Active Bottom Tab (0=Text, 1=Translate, 2=History) ────────────────────
    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    val supportedLanguages: List<String> get() = ALL_LANGUAGES.map { it.displayName }

    // TTS & internal state
    private var tts: TextToSpeech? = null
    private var lastAnalyzedText = ""
    private val imageWidth  = MutableStateFlow(1)
    private val imageHeight = MutableStateFlow(1)
    private var analysisJob: Job? = null
    private var liveTranslateJob: Job? = null

    init {
        tts = TextToSpeech(context) { /* init ok */ }
    }

    // ── Called every camera frame ─────────────────────────────────────────────
    suspend fun analyzeFrame(image: Image, rotation: Int, width: Int, height: Int) = withContext(Dispatchers.Default) {
        if (!_isLiveScan.value) return@withContext
        imageWidth.value  = width
        imageHeight.value = height

        val selectedSourceCode = _sourceLanguage.value.code
        val script = if (selectedSourceCode == "auto") {
            activeAutoScript.value
        } else {
            OcrScript.fromLanguageCode(selectedSourceCode)
        }

        ocrRepo.recognizeFromImage(image, rotation, script)
            .onSuccess { mlText ->
                val domain = mlText.toDomain(width, height, rotation)
                val frameW = if (rotation == 90 || rotation == 270) height else width
                val frameH = if (rotation == 90 || rotation == 270) width else height

                if (domain.fullText.isBlank()) {
                    _scanState.value     = OcrScanState.NoText
                    _overlayBlocks.value = emptyList()
                    _overlayParagraphs.value = emptyList()
                    _scanQuality.value   = 0f
                } else {
                    val newText = domain.fullText
                    val enriched = if (newText != lastAnalyzedText) {
                        lastAnalyzedText = newText
                        val langCode = langRepo.identifyLanguage(newText).getOrDefault("und")
                        
                        // Adaptive Auto-Detect: automatically switch to the correct script engine if needed
                        if (selectedSourceCode == "auto") {
                            val detectedScriptEnum = OcrScript.fromLanguageCode(langCode)
                            if (detectedScriptEnum != activeAutoScript.value) {
                                activeAutoScript.value = detectedScriptEnum
                            }
                        }

                        _detectedScript.value = OcrScript.fromLanguageCode(langCode).name
                            .lowercase().replaceFirstChar { it.uppercase() }
                        domain.copy(
                            detectedLanguage     = langCode,
                            detectedLanguageName = langRepo.languageDisplayName(langCode),
                            detectedScript       = _detectedScript.value
                        )
                    } else domain

                    // Quality: average confidence of all blocks
                    val avgConf = enriched.blocks.mapNotNull { it.confidence }
                        .let { if (it.isEmpty()) 0.8f else it.average().toFloat() }
                    _scanQuality.value       = avgConf
                    _overlayBlocks.value     = enriched.blocks
                    _overlayParagraphs.value = enriched.paragraphs
                    _scanState.value         = OcrScanState.TextDetected(enriched, frameW, frameH)
                }
            }
            .onFailure { _scanState.value = OcrScanState.Error(it.message ?: "OCR failed") }
    }

    private var ocrImageUri: Uri? = null

    private fun saveTempBitmap(bitmap: Bitmap): Uri? {
        return try {
            val file = java.io.File(context.cacheDir, "ocr_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    /** Freeze current scan and trigger translation */
    fun freezeAndCapture(bitmap: Bitmap? = null) {
        val current = (_scanState.value as? OcrScanState.TextDetected)?.result ?: return
        val w = (_scanState.value as? OcrScanState.TextDetected)?.frameWidth ?: 1080
        val h = (_scanState.value as? OcrScanState.TextDetected)?.frameHeight ?: 1920
        _frozenResult.value = current
        _isLiveScan.value   = false
        _scanState.value    = OcrScanState.TextDetected(current, w, h)
        _activeTab.value    = 0  // Show Text tab first
        
        ocrImageUri = bitmap?.let { saveTempBitmap(it) }
    }

    /** Resume live scanning */
    fun resumeLiveScan() {
        _frozenResult.value      = null
        _isLiveScan.value        = true
        _translationState.value  = TranslationState.Idle
        _overlayBlocks.value     = emptyList()
        _overlayParagraphs.value = emptyList()
        _selectedBlock.value     = null
        _selectedParagraph.value = null
        _scanQuality.value       = 0f
        _blockTranslations.value = emptyMap()
        activeAutoScript.value   = OcrScript.LATIN
        ocrImageUri              = null
        liveTranslateJob?.cancel()
    }

    /** Select a single line block */
    fun selectBlock(block: RecognizedBlock?) {
        _selectedBlock.value     = block
        _selectedParagraph.value = null
        _translationState.value  = TranslationState.Idle
    }

    /** Select an entire paragraph */
    fun selectParagraph(paragraph: RecognizedParagraph?) {
        _selectedParagraph.value = paragraph
        _selectedBlock.value     = null
        _translationState.value  = TranslationState.Idle
    }

    /** Analyze image from gallery URI */
    fun analyzeFromGallery(context: Context, uri: Uri) = viewModelScope.launch {
        _scanState.value = OcrScanState.Scanning
        _selectedBlock.value     = null
        _selectedParagraph.value = null
        ocrImageUri              = uri

        val (w, h) = ocrRepo.readDimensions(context, uri)
        val script = OcrScript.fromLanguageCode(
            _frozenResult.value?.detectedLanguage ?: "en"
        )

        ocrRepo.recognizeFromUri(context, uri, script)
            .onSuccess { mlText ->
                val domain = mlText.toDomain(w, h, 0)
                if (domain.fullText.isBlank()) {
                    _scanState.value = OcrScanState.NoText
                } else {
                    val langCode = langRepo.identifyLanguage(domain.fullText).getOrDefault("und")
                    val enriched = domain.copy(
                        detectedLanguage     = langCode,
                        detectedLanguageName = langRepo.languageDisplayName(langCode),
                        detectedScript       = OcrScript.fromLanguageCode(langCode).name
                            .lowercase().replaceFirstChar { it.uppercase() }
                    )
                    _detectedScript.value    = enriched.detectedScript
                    _frozenResult.value      = enriched
                    _isLiveScan.value        = false
                    _overlayParagraphs.value = enriched.paragraphs
                    _overlayBlocks.value     = enriched.blocks
                    _scanState.value         = OcrScanState.TextDetected(enriched, w, h)
                    _activeTab.value         = 0
                    
                    // Pre-translate gallery blocks if in Translate mode
                    if (_ocrMode.value == OcrMode.TRANSLATE) {
                        translateAllBlocks(enriched.blocks, langCode, _targetLanguage.value)
                    }
                }
            }
            .onFailure { _scanState.value = OcrScanState.Error(it.message ?: "Failed to read image") }
    }

    /** Translate the current frozen/selected text */
    fun translateCurrentText() {
        val text = _selectedParagraph.value?.text
            ?: _selectedBlock.value?.text
            ?: (_frozenResult.value ?: (_scanState.value as? OcrScanState.TextDetected)?.result)?.fullText
            ?: return

        val result = _frozenResult.value ?: (_scanState.value as? OcrScanState.TextDetected)?.result
        val sourceLang = result?.detectedLanguage ?: "und"

        val sourceLangObj = languageByCode(sourceLang)
        val targetLangObj = ALL_LANGUAGES.firstOrNull {
            it.displayName.equals(_targetLanguage.value, ignoreCase = true)
        } ?: ALL_LANGUAGES.first { it.code == "hi" }

        viewModelScope.launch {
            _translationState.value = TranslationState.Downloading
            _activeTab.value        = 1  // Switch to Translate tab
            
            // Upload OCR image if present
            var downloadUrl: String? = null
            val imgUri = ocrImageUri
            if (imgUri != null) {
                val storagePath = "users/${firebase.getCurrentUid()}/ocr/ocr_${System.currentTimeMillis()}.jpg"
                val uploadResult = firebase.uploadFile(storagePath, imgUri)
                downloadUrl = uploadResult.getOrNull()
            }

            // Translate all blocks in parallel for Google Lens Translate Overlay
            result?.blocks?.let { blocks ->
                translateAllBlocks(blocks, sourceLang, _targetLanguage.value)
            }

            translationRepo.translate(text, sourceLangObj, targetLangObj)
                .onSuccess { res ->
                    _translationState.value = TranslationState.Success(
                        translatedText  = res.translatedText,
                        sourceLang      = res.detectedLang.displayName,
                        targetLang      = res.targetLang.displayName,
                        nativeMeaning   = res.nativeMeaning,
                        semanticMeaning = res.semanticMeaning
                    )

                    // Save to dedicated OCR scan history
                    saveScanRecord(
                        extracted   = text,
                        translated  = res.translatedText,
                        sourceLang  = res.detectedLang.displayName,
                        targetLang  = res.targetLang.displayName,
                        result      = result
                    )

                    // Generic history entry (for in-app History tab)
                    val histTitle = buildString {
                        if (_selectedParagraph.value != null) append("Para OCR")
                        else if (_selectedBlock.value != null) append("Block OCR")
                        else append("OCR")
                        append(" → ${res.targetLang.displayName}")
                    }
                    historyRepo.save("OCR", histTitle, res.translatedText.take(220))

                    // Structured Firebase entry with rich OCR data
                    firebase.saveOCRHistory(
                        extractedText  = text,
                        sourceLanguage = res.detectedLang.displayName,
                        translatedText = res.translatedText,
                        targetLanguage = res.targetLang.displayName,
                        wordCount      = text.split("\\s+".toRegex()).size,
                        imageUrl       = downloadUrl
                    )
                }
                .onFailure {
                    _translationState.value = TranslationState.Error(
                        it.message ?: "Translation failed. Check internet connection."
                    )
                }
        }
    }

    private fun saveScanRecord(
        extracted: String,
        translated: String?,
        sourceLang: String,
        targetLang: String?,
        result: OcrResult?
    ) = viewModelScope.launch {
        val entity = OcrScanEntity(
            id             = UUID.randomUUID().toString(),
            timestamp      = System.currentTimeMillis(),
            extractedText  = extracted,
            translatedText = translated,
            sourceLang     = sourceLang,
            targetLang     = targetLang,
            wordCount      = extracted.split("\\s+".toRegex()).size,
            characterCount = extracted.length,
            paragraphCount = result?.paragraphs?.size ?: 1,
            confidence     = result?.blocks?.mapNotNull { it.confidence }
                ?.let { if (it.isEmpty()) 0.8f else it.average().toFloat() } ?: 0.8f,
            script         = result?.detectedScript ?: "Latin"
        )
        ocrScanDao.insert(entity)
    }

    fun deleteScanFromHistory(id: String) = viewModelScope.launch {
        ocrScanDao.deleteById(id)
    }

    fun setTargetLanguage(lang: String) {
        _targetLanguage.value   = lang
        _translationState.value = TranslationState.Idle
    }

    fun setSourceLanguage(lang: Language) {
        _sourceLanguage.value = lang
        resumeLiveScan()
    }

    fun setOcrMode(mode: OcrMode) {
        _ocrMode.value = mode
        if (mode == OcrMode.TRANSLATE) {
            val result = _frozenResult.value ?: (_scanState.value as? OcrScanState.TextDetected)?.result
            if (result != null) {
                translateAllBlocks(result.blocks, result.detectedLanguage, _targetLanguage.value)
            }
        }
    }

    private var blockTranslationJob: Job? = null

    fun translateAllBlocks(blocks: List<RecognizedBlock>, sourceLangCode: String, targetLangName: String) {
        val sourceLangObj = languageByCode(sourceLangCode)
        val targetLangObj = ALL_LANGUAGES.firstOrNull {
            it.displayName.equals(targetLangName, ignoreCase = true)
        } ?: ALL_LANGUAGES.first { it.code == "hi" }

        blockTranslationJob?.cancel()
        blockTranslationJob = viewModelScope.launch(Dispatchers.Default) {
            _isTranslatingBlocks.value = true
            val newTranslations = mutableMapOf<String, String>()
            blocks.forEach { block ->
                val originalText = block.text
                if (originalText.isNotBlank()) {
                    translationRepo.translate(originalText, sourceLangObj, targetLangObj)
                        .onSuccess { res ->
                            newTranslations[originalText] = res.translatedText
                        }.onFailure {
                            newTranslations[originalText] = originalText
                        }
                }
            }
            _blockTranslations.value = newTranslations
            _isTranslatingBlocks.value = false
        }
    }

    fun setActiveTab(tab: Int) { _activeTab.value = tab }
    fun toggleFlash() { _flashEnabled.update { !it } }
    fun togglePreprocessing() { _preprocessingEnabled.update { !it } }

    /** Text-to-Speech */
    fun speak(text: String, languageCode: String = "en") {
        val locale = when (languageCode) {
            "hi" -> Locale("hi", "IN")
            "te" -> Locale("te", "IN")
            "ta" -> Locale("ta", "IN")
            "mr" -> Locale("mr", "IN")
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "es" -> Locale("es", "ES")
            "ja" -> Locale.JAPANESE
            "zh" -> Locale.CHINESE
            "ko" -> Locale.KOREAN
            "ru" -> Locale("ru", "RU")
            "ar" -> Locale("ar", "SA")
            "it" -> Locale.ITALIAN
            "pt" -> Locale("pt", "BR")
            else -> Locale.ENGLISH
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun dismissError() { _scanState.value = OcrScanState.Idle }
    fun showError(message: String) { _scanState.value = OcrScanState.Error(message) }

    fun stopSpeaking() {
        tts?.stop()
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        analysisJob?.cancel()
        liveTranslateJob?.cancel()
    }
}
