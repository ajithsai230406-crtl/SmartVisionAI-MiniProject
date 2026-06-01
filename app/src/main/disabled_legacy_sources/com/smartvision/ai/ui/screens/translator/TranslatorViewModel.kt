package com.smartvision.ai.ui.screens.translator

import android.app.Application
import android.content.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

data class TranslatorUiState(
    val inputText:          String                  = "",
    val outputText:         String                  = "",
    val sourceLang:         String                  = "en",
    val targetLang:         String                  = "hi",
    val isTranslating:      Boolean                 = false,
    val isListening:        Boolean                 = false,
    val isDownloadingModel: Boolean                 = false,
    val error:              String?                 = null,
    val recentTranslations: List<Pair<String,String>> = emptyList()
)

@HiltViewModel
class TranslatorViewModel @Inject constructor(
    app: Application
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    // TTS
    private var tts: TextToSpeech? = null
    private var mlkitTranslator: com.google.mlkit.nl.translate.Translator? = null

    // Speech recognizer
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        initTts()
        buildTranslator()
    }

    // ── Input / Lang ──────────────────────────────────────────────────────────

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun setSourceLang(code: String) {
        _uiState.update { it.copy(sourceLang = code, outputText = "") }
        buildTranslator()
    }

    fun setTargetLang(code: String) {
        _uiState.update { it.copy(targetLang = code, outputText = "") }
        buildTranslator()
    }

    fun swapLanguages() {
        _uiState.update { state ->
            state.copy(
                sourceLang = state.targetLang,
                targetLang = state.sourceLang,
                inputText  = state.outputText,
                outputText = ""
            )
        }
        buildTranslator()
    }

    // ── Translate ─────────────────────────────────────────────────────────────

    fun translate() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isTranslating = true, error = null) }
            try {
                val translator = mlkitTranslator ?: run {
                    buildTranslator()
                    mlkitTranslator
                } ?: throw Exception("Translator not ready")

                val conditions = DownloadConditions.Builder().build()
                translator.downloadModelIfNeeded(conditions).await()

                val result = translator.translate(text).await()
                val recent = (_uiState.value.recentTranslations + (text to result)).takeLast(5)
                _uiState.update { it.copy(outputText = result, recentTranslations = recent) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Translation failed") }
            } finally {
                _uiState.update { it.copy(isTranslating = false) }
            }
        }
    }

    // ── Voice (Speech-to-Text) ────────────────────────────────────────────────

    fun startListening() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication())

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, _uiState.value.sourceLang)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: android.os.Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken  = matches?.firstOrNull() ?: return
                _uiState.update { it.copy(inputText = spoken, isListening = false) }
                // Auto-translate after speech
                viewModelScope.launch { translate() }
            }
            override fun onError(error: Int) {
                _uiState.update { it.copy(isListening = false, error = "Speech recognition error: $error") }
            }
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                _uiState.update { it.copy(isListening = true) }
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                _uiState.update { it.copy(inputText = partial) }
            }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    // ── Output Actions ────────────────────────────────────────────────────────

    fun copyOutput() {
        val text = _uiState.value.outputText.ifEmpty { return }
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Translation", text))
    }

    fun speakOutput() {
        val text = _uiState.value.outputText.ifEmpty { return }
        tts?.language = Locale(_uiState.value.targetLang)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun buildTranslator() {
        mlkitTranslator?.close()
        val state   = _uiState.value
        val srcLang = try { TranslateLanguage.fromLanguageTag(state.sourceLang)!! }
                      catch (e: Exception) { TranslateLanguage.ENGLISH }
        val tgtLang = try { TranslateLanguage.fromLanguageTag(state.targetLang)!! }
                      catch (e: Exception) { TranslateLanguage.HINDI }
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(srcLang)
            .setTargetLanguage(tgtLang)
            .build()
        mlkitTranslator = Translation.getClient(options)
    }

    private fun initTts() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
        mlkitTranslator?.close()
        speechRecognizer?.destroy()
    }
}
