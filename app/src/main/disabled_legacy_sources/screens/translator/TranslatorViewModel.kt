package com.smartvision.ai.ui.screens.translator

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.domain.models.TranslationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TranslatorViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TranslationState())
    val uiState: StateFlow<TranslationState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
        }
    }

    fun setInputText(text: String) = _uiState.update { state -> state.copy(inputText = text, error = null) }
    fun setSourceLang(lang: String) = _uiState.update { state -> state.copy(sourceLang = lang) }
    fun setTargetLang(lang: String) = _uiState.update { state -> state.copy(targetLang = lang) }

    fun swapLanguages() = _uiState.update { state ->
        state.copy(sourceLang = state.targetLang, targetLang = state.sourceLang,
            inputText = state.outputText.ifEmpty { state.inputText },
            outputText = if (state.outputText.isNotEmpty()) state.inputText else "")
    }

    fun translate() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return
        _uiState.update { state -> state.copy(isTranslating = true, error = null) }
        viewModelScope.launch {
            delay(600)
            val result = simulateTranslation(text, _uiState.value.targetLang)
            val recent = (_uiState.value.recentTranslations + Pair(text, result)).takeLast(5)
            _uiState.update { state -> state.copy(isTranslating = false, outputText = result, recentTranslations = recent) }
        }
    }

    private fun simulateTranslation(text: String, lang: String): String = when (lang) {
        "hi" -> if (text.equals("hi", true) || text.equals("hello", true)) "नमस्ते"
                else if (text.equals("how are you", true)) "आप कैसे हैं?"
                else "अनुवाद: $text"
        "te" -> "అనువాదం: $text"
        "ta" -> "மொழிபெயர்ப்பு: $text"
        "fr" -> "Traduction: $text"
        "de" -> "Übersetzung: $text"
        "ja" -> "翻訳: $text"
        "ar" -> "ترجمة: $text"
        "es" -> "Traducción: $text"
        "ru" -> "Перевод: $text"
        "ko" -> "번역: $text"
        "zh" -> "翻译: $text"
        "bn" -> "অনুবাদ: $text"
        "ur" -> "ترجمہ: $text"
        else -> text
    }

    fun copyOutput() {
        val text = _uiState.value.outputText.ifEmpty { return }
        val ctx = getApplication<Application>()
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("Translation", text))
    }

    fun speakOutput() {
        val text = _uiState.value.outputText.ifEmpty { return }
        val locale = when (_uiState.value.targetLang) {
            "hi" -> Locale("hi", "IN"); "fr" -> Locale.FRENCH; "de" -> Locale.GERMAN
            "ja" -> Locale.JAPANESE;    "zh" -> Locale.CHINESE; "ko" -> Locale.KOREAN
            "es" -> Locale("es", "ES"); "ar" -> Locale("ar")
            else -> Locale.ENGLISH
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_out")
    }

    fun startListening() = _uiState.update { state -> state.copy(isListening = true) }
    fun stopListening()  = _uiState.update { state -> state.copy(isListening = false) }

    override fun onCleared() { tts?.stop(); tts?.shutdown(); super.onCleared() }
}
