package com.smartvision.ai.translator.presentation

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.ScreenContext
import com.smartvision.ai.data.ScreenContextManager
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.data.repository.FirebaseRepository
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
    private val firebaseRepository: FirebaseRepository,
    private val screenContextManager: ScreenContextManager,
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

    // ── Auto-play & continuous mode ──────────────────────────────────────────────
    private val _autoPlay       = MutableStateFlow(true)    // auto-play translated TTS
    val autoPlay: StateFlow<Boolean> = _autoPlay.asStateFlow()

    private val _continuousMode = MutableStateFlow(false)   // auto-restart listening after TTS
    val continuousMode: StateFlow<Boolean> = _continuousMode.asStateFlow()

    // remembers which channel was last active (for continuous mode restart)
    private var lastIsLeft: Boolean = true

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null
    private var silenceTimerJob: kotlinx.coroutines.Job? = null
    private var lastSpeechTime = 0L
    private var isRecordingContinuous = false

    // ── TTS ──────────────────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        // Keep legacy continuous mode triggers if Loop toggle is activated
                        if (_continuousMode.value && _listeningState.value == VoiceListeningState.IDLE) {
                            viewModelScope.launch {
                                startListening(!lastIsLeft)
                            }
                        }
                    }
                })
            }
        }
    }

    // ── Start listening from a channel ────────────────────────────────────────
    fun startListening(isLeft: Boolean) {
        if (_activeChannel.value == isLeft && _listeningState.value != VoiceListeningState.IDLE) {
            stopListening()
            return
        }

        stopListening() // stop other active channel first

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _error.value = "Speech recognition is not available on this device"
            return
        }

        _activeChannel.value   = isLeft
        _listeningState.value  = VoiceListeningState.LISTENING
        _partialText.value     = ""
        _error.value           = null
        isRecordingContinuous  = true
        lastSpeechTime         = System.currentTimeMillis()

        _recordingDuration.value = 0
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _recordingDuration.value += 1
            }
        }

        startSilenceWatchdog(isLeft)
        runSpeechRecognizer(isLeft)
    }

    private fun startSilenceWatchdog(isLeft: Boolean) {
        silenceTimerJob?.cancel()
        silenceTimerJob = viewModelScope.launch {
            while (isRecordingContinuous) {
                kotlinx.coroutines.delay(1000)
                val silenceDuration = System.currentTimeMillis() - lastSpeechTime
                if (silenceDuration >= 20000) {
                    stopListening()
                    _error.value = "Session timed out due to 20s of silence"
                    break
                }
            }
        }
    }

    private fun runSpeechRecognizer(isLeft: Boolean) {
        if (!isRecordingContinuous) return
        val srcLang = if (isLeft) _leftLang.value else _rightLang.value
        val locale  = Locale.forLanguageTag(srcLang.code)

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {
                lastSpeechTime = System.currentTimeMillis()
            }
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(code: Int) {
                if (isRecordingContinuous) {
                    val silenceDuration = System.currentTimeMillis() - lastSpeechTime
                    if (silenceDuration >= 20000) {
                        stopListening()
                        _error.value = "Session timed out due to 20s of silence"
                    } else {
                        // Silently restart SpeechRecognizer to continue continuous recording
                        runSpeechRecognizer(isLeft)
                    }
                } else {
                    _listeningState.value = VoiceListeningState.IDLE
                    _activeChannel.value  = null
                    _waveAmplitudes.value = List(12) { 0.1f }
                    _error.value = speechErrorMessage(code)
                }
            }
            override fun onPartialResults(results: Bundle?) {
                val partial = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    _partialText.value = partial
                    lastSpeechTime = System.currentTimeMillis()
                }
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
                if (text.isNotBlank()) {
                    lastSpeechTime = System.currentTimeMillis()
                    translateVoice(text, isLeft)
                    if (isRecordingContinuous) {
                        runSpeechRecognizer(isLeft)
                    }
                } else {
                    if (isRecordingContinuous) {
                        val silenceDuration = System.currentTimeMillis() - lastSpeechTime
                        if (silenceDuration >= 20000) {
                            stopListening()
                            _error.value = "Session timed out due to 20s of silence"
                        } else {
                            runSpeechRecognizer(isLeft)
                        }
                    } else {
                        _listeningState.value = VoiceListeningState.IDLE
                        _activeChannel.value = null
                    }
                }
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
        isRecordingContinuous = false
        timerJob?.cancel()
        timerJob = null
        silenceTimerJob?.cancel()
        silenceTimerJob = null
        _recordingDuration.value = 0
        _partialText.value = ""
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _listeningState.value = VoiceListeningState.IDLE
        _activeChannel.value  = null
        _waveAmplitudes.value = List(12) { 0.1f }
    }

    private fun translateVoice(spokenText: String, isLeft: Boolean) {
        val srcLang = if (isLeft) _leftLang.value else _rightLang.value
        val tgtLang = if (isLeft) _rightLang.value else _leftLang.value

        viewModelScope.launch {
            _listeningState.value = VoiceListeningState.PROCESSING
            repo.translate(spokenText, srcLang, tgtLang)
                .onSuccess { result ->
                    val entry = ConversationEntry(
                        originalText    = spokenText,
                        translatedText  = result.translatedText,
                        sourceLang      = srcLang,
                        targetLang      = tgtLang,
                        isUserSide      = isLeft,
                        nativeMeaning   = result.nativeMeaning,
                        meaningInTarget = result.meaningInTarget,
                        semanticMeaning = result.semanticMeaning
                    )
                    _conversation.update { it + entry }
                    lastIsLeft = isLeft
                    screenContextManager.updateContext(
                        ScreenContext.VoiceTranslatorContext(
                            conversationTranscript = _conversation.value.joinToString("\n") {
                                "${it.sourceLang.displayName}: ${it.originalText} → ${it.targetLang.displayName}: ${it.translatedText}"
                            },
                            sourceLang = srcLang.displayName,
                            targetLang = tgtLang.displayName
                        ),
                        moduleName = "Voice Translator"
                    )
                    if (_autoPlay.value) {
                        speakWithId(result.translatedText, tgtLang.code, "translated_${entry.id}")
                    }
                    val allEntries = _conversation.value
                    val histJson = com.smartvision.ai.domain.model.HistoryJsonSerializer.serializeVoice(
                        com.smartvision.ai.domain.model.VoiceHistoryDetail(
                            entries = allEntries.map { e ->
                                com.smartvision.ai.domain.model.VoiceEntryDetail(
                                    originalText = e.originalText,
                                    translatedText = e.translatedText,
                                    sourceLang = e.sourceLang.displayName,
                                    targetLang = e.targetLang.displayName,
                                    isFromUser = e.isUserSide,
                                    meaningInSource = e.nativeMeaning,
                                    meaningInTarget = e.meaningInTarget,
                                    meaningInEnglish = e.semanticMeaning?.meaningEnglish,
                                    context = e.semanticMeaning?.contextExplanation,
                                    usageExamples = e.semanticMeaning?.usageExplanation?.let { listOf(it) } ?: emptyList(),
                                    alternativeMeanings = emptyList(),
                                    pronunciation = e.semanticMeaning?.pronunciation
                                )
                            }
                        )
                    )
                    historyRepo.save("Voice", "${srcLang.flag}→${tgtLang.flag} $spokenText", result.translatedText.take(200), histJson)
                    viewModelScope.launch {
                        firebaseRepository.saveTranslation(
                            sourceText = spokenText,
                            translatedText = result.translatedText,
                            sourceLang = srcLang.displayName,
                            targetLang = tgtLang.displayName
                        )
                    }
                }
                .onFailure { _error.value = it.message ?: "Translation failed" }
            
            if (isRecordingContinuous) {
                _listeningState.value = VoiceListeningState.LISTENING
            } else {
                _listeningState.value = VoiceListeningState.IDLE
                _activeChannel.value  = null
            }
        }
    }

    /** Speak the translated text of an entry */
    fun speakEntry(entry: ConversationEntry) = speak(entry.translatedText, entry.targetLang.code)

    /** Speak the original spoken text of an entry */
    fun speakOriginalEntry(entry: ConversationEntry) = speak(entry.originalText, entry.sourceLang.code)

    fun toggleAutoPlay()       { _autoPlay.update { !it } }
    fun toggleContinuousMode() { _continuousMode.update { !it } }

    fun setLeftLang(l: Language)  { _leftLang.value = l }
    fun setRightLang(l: Language) { _rightLang.value = l }
    fun swapLanguages()           { val l = _leftLang.value; _leftLang.value = _rightLang.value; _rightLang.value = l }
    fun clearConversation()       { _conversation.value = emptyList() }
    fun dismissError()            { _error.value = null }
    fun stopSpeaking()            { tts?.stop() }

    private fun speak(text: String, langCode: String) {
        val locale = try { Locale.forLanguageTag(langCode) } catch (e: Exception) { Locale.ENGLISH }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun speakWithId(text: String, langCode: String, utteranceId: String) {
        val locale = try { Locale.forLanguageTag(langCode) } catch (e: Exception) { Locale.ENGLISH }
        tts?.language = locale
        val params = android.os.Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId) }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
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
