package com.smartvision.ai.translator.presentation

import android.content.Context
import android.speech.tts.TextToSpeech
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
class TextTranslatorViewModel @Inject constructor(
    private val repo:        SmartTranslatorRepository,
    private val historyRepo: HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── UI State ───────────────────────────────────────────────────────────────
    private val _state        = MutableStateFlow<TranslatorState>(TranslatorState.Idle)
    val state: StateFlow<TranslatorState> = _state.asStateFlow()

    private val _inputText    = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _sourceLang   = MutableStateFlow(ALL_LANGUAGES.first { it.code == "en" })
    val sourceLang: StateFlow<Language> = _sourceLang.asStateFlow()

    private val _targetLang   = MutableStateFlow(ALL_LANGUAGES.first { it.code == "hi" })
    val targetLang: StateFlow<Language> = _targetLang.asStateFlow()

    private val _explanation  = MutableStateFlow<String?>(null)
    val explanation: StateFlow<String?> = _explanation.asStateFlow()

    private val _showLangPicker = MutableStateFlow<Boolean?>(null)   // null=hidden, true=source, false=target
    val showLangPicker: StateFlow<Boolean?> = _showLangPicker.asStateFlow()

    private val _recentTranslations = MutableStateFlow<List<TranslationResult>>(emptyList())
    val recentTranslations: StateFlow<List<TranslationResult>> = _recentTranslations.asStateFlow()

    private val _isAutoDetect   = MutableStateFlow(true)
    val isAutoDetect: StateFlow<Boolean> = _isAutoDetect.asStateFlow()

    // ── TTS ────────────────────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { /* ready */ }
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    fun setInput(t: String)    { _inputText.value = t; if (t.isBlank()) _state.value = TranslatorState.Idle }
    fun setSourceLang(l: Language) { _sourceLang.value = l; _showLangPicker.value = null }
    fun setTargetLang(l: Language) { _targetLang.value = l; _showLangPicker.value = null; translateCurrent() }
    fun showSourcePicker()     { _showLangPicker.value = true }
    fun showTargetPicker()     { _showLangPicker.value = false }
    fun hideLangPicker()       { _showLangPicker.value = null }
    fun toggleAutoDetect()     { _isAutoDetect.update { !it } }

    fun swapLanguages() {
        val src = _sourceLang.value; val tgt = _targetLang.value
        _sourceLang.value = tgt; _targetLang.value = src
        // Swap the text too if we have a translation
        val cur = (_state.value as? TranslatorState.Success)?.result
        if (cur != null) { _inputText.value = cur.translatedText; translateCurrent() }
    }

    fun translate(text: String = _inputText.value) {
        val q = text.trim()
        if (q.isBlank()) return
        _state.value = TranslatorState.Translating
        _explanation.value = null
        viewModelScope.launch {
            val srcLang = if (_isAutoDetect.value) {
                Language("auto", "Auto", "🌐", "Auto")
            } else _sourceLang.value

            repo.translate(q, srcLang, _targetLang.value)
                .onSuccess { result ->
                    _state.value = TranslatorState.Success(result)
                    _sourceLang.value = result.detectedLang
                    _recentTranslations.update { (listOf(result) + it).take(15) }
                    historyRepo.save("Translation", "${result.detectedLang.flag} → ${result.targetLang.flag} ${result.translatedText.take(60)}", result.originalText.take(200))
                    // Auto-fetch explanation
                    fetchExplanation(result)
                }
                .onFailure { _state.value = TranslatorState.Error(it.message ?: "Translation failed") }
        }
    }

    fun speakOriginal() {
        val text = _inputText.value.ifBlank { return }
        speak(text, _sourceLang.value.code)
    }

    fun speakTranslation() {
        val result = (_state.value as? TranslatorState.Success)?.result ?: return
        speak(result.translatedText, result.targetLang.code)
    }

    fun copyTranslation(): String =
        (_state.value as? TranslatorState.Success)?.result?.translatedText ?: ""

    fun clearAll() {
        _inputText.value = ""; _state.value = TranslatorState.Idle; _explanation.value = null
    }

    private fun translateCurrent() {
        if (_inputText.value.isNotBlank()) translate()
    }

    private fun fetchExplanation(result: TranslationResult) {
        _explanation.value = result.semanticMeaning?.contextExplanation
    }

    private fun speak(text: String, langCode: String) {
        val locale = try { Locale.forLanguageTag(langCode) } catch (e: Exception) { Locale.ENGLISH }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onCleared() { super.onCleared(); tts?.stop(); tts?.shutdown() }
}
