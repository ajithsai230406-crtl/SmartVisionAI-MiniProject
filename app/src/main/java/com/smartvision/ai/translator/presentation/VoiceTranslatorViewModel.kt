package com.smartvision.ai.translator.presentation

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.translator.data.SmartTranslatorRepository
import com.smartvision.ai.translator.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class VoiceTranslatorViewModel @Inject constructor(
    private val repo:        SmartTranslatorRepository,
    private val historyRepo: HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Language config ────────────────────────────────────────────────────────
    private val _leftLang  = MutableStateFlow(ALL_LANGUAGES.first { it.code == "en" })
    private val _rightLang = MutableStateFlow(ALL_LANGUAGES.first { it.code == "hi" })
    val leftLang:  StateFlow<Language> = _leftLang.asStateFlow()
    val rightLang: StateFlow<Language> = _rightLang.asStateFlow()

    // ── Voice listening state ──────────────────────────────────────────────────
    private val _listeningState = MutableStateFlow(VoiceListeningState.IDLE)
    val listeningState: StateFlow<VoiceListeningState> = _listeningState.asStateFlow()

    private val _activeChannel = MutableStateFlow<Boolean?>(null)   // null=none, true=left, false=right
    val activeChannel: StateFlow<Boolean?> = _activeChannel.asStateFlow()

    // ── Waveform amplitude (0..1 per bar) ─────────────────────────────────────
    private val _waveAmplitudes = MutableStateFlow(List(12) { 0.1f })
    val waveAmplitudes: StateFlow<List<Float>> = _waveAmplitudes.asStateFlow()

    // ── Partial recognition text ───────────────────────────────────────────────
    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    // ── Conversation history ───────────────────────────────────────────────────
    private val _conversation = MutableStateFlow<List<ConversationEntry>>(emptyList())
    val conversation: StateFlow<List<ConversationEntry>> = _conversation.asStateFlow()

    // ── Error ──────────────────────────────────────────────────────────────────
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── TTS ────────────────────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        tts = TextToSpeech(context) { }
    }

    // ── Start listening from a channel ────────────────────────────────────────
    fun startListening(isLeft: Boolean) {
        if (_listeningState.value == VoiceListeningState.LISTENING) { stopListening(); return }
        _activeChannel.value   = isLeft
        _listeningState.value  = VoiceListeningState.LISTENING
        _partialText.value     = ""
        _error.value           = null

        val srcLang = if (isLeft) _leftLang.value else _rightLang.value
        val locale  = Locale.forLanguageTag(srcLang.code)

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { _listeningState.value = VoiceListeningState.PROCESSING }
            override fun onError(code: Int) {
                _listeningState.value = VoiceListeningState.IDLE
                _activeChannel.value  = null
                _waveAmplitudes.value = List(12) { 0.1f }
                _error.value = speechErrorMessage(code)
            }
            override fun onPartialResults(results: Bundle?) {
                val partial = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                _partialText.value = partial ?: ""
                // Simulate waveform from rms
                _waveAmplitudes.value = List(12) { (0.2f + Math.random().toFloat() * 0.8f) }
            }
            override fun onRmsChanged(rms: Float) {
                val norm = ((rms + 2f) / 12f).coerceIn(0.05f, 1f)
                _waveAmplitudes.value = List(12) { i ->
                    val offset = Math.sin((i + norm * 3).toDouble()).toFloat() * 0.3f
                    (norm + offset).coerceIn(0.05f, 1f)
                }
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                _partialText.value     = ""
                _waveAmplitudes.value  = List(12) { 0.1f }
                if (text.isNotBlank()) translateVoice(text, isLeft)
                else { _listeningState.value = VoiceListeningState.IDLE; _activeChannel.value = null }
            }
            override fun onEvent(type: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _listeningState.value = VoiceListeningState.IDLE
        _activeChannel.value  = null
        _waveAmplitudes.value = List(12) { 0.1f }
    }

    private fun translateVoice(spokenText: String, isLeft: Boolean) {
        val srcLang = if (isLeft) _leftLang.value else _rightLang.value
        val tgtLang = if (isLeft) _rightLang.value else _leftLang.value

        viewModelScope.launch {
            repo.translate(spokenText, srcLang, tgtLang)
                .onSuccess { result ->
                    val entry = ConversationEntry(
                        originalText   = spokenText,
                        translatedText = result.translatedText,
                        sourceLang     = srcLang,
                        targetLang     = tgtLang,
                        isUserSide     = isLeft,
                        nativeMeaning  = result.nativeMeaning,
                        semanticMeaning = result.semanticMeaning
                    )
                    _conversation.update { it + entry }
                    // Auto-play translation
                    speak(result.translatedText, tgtLang.code)
                    historyRepo.save("Voice", "${srcLang.flag}→${tgtLang.flag} $spokenText", result.translatedText.take(200))
                }
                .onFailure { _error.value = it.message ?: "Translation failed" }
            _listeningState.value = VoiceListeningState.IDLE
            _activeChannel.value  = null
        }
    }

    fun speakEntry(entry: ConversationEntry) = speak(entry.translatedText, entry.targetLang.code)

    fun setLeftLang(l: Language)  { _leftLang.value = l }
    fun setRightLang(l: Language) { _rightLang.value = l }
    fun swapLanguages()           { val l = _leftLang.value; _leftLang.value = _rightLang.value; _rightLang.value = l }
    fun clearConversation()       { _conversation.value = emptyList() }
    fun dismissError()            { _error.value = null }

    private fun speak(text: String, langCode: String) {
        val locale = try { Locale.forLanguageTag(langCode) } catch (e: Exception) { Locale.ENGLISH }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun speechErrorMessage(code: Int) = when (code) {
        SpeechRecognizer.ERROR_AUDIO             -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT            -> "Client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed"
        SpeechRecognizer.ERROR_NETWORK           -> "Network error — check connection"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT   -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH          -> "No speech detected — try again"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY   -> "Recognizer busy — try again"
        SpeechRecognizer.ERROR_SERVER            -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT    -> "No speech input detected"
        else                                     -> "Speech recognition error ($code)"
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        tts?.stop(); tts?.shutdown()
    }
}
