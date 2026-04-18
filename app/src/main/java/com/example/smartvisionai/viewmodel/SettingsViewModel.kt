package com.example.smartvisionai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvisionai.data.local.SettingsPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val voiceEnabled:        Boolean = true,
    val ttsEnabled:          Boolean = true,
    val speechSpeed:         Float   = 1.0f,
    val ttsLocale:           String  = "en",
    val defaultLanguage:     String  = "English",
    val translationTarget:   String  = "English",
    val performanceMode:     String  = "Balanced",
    val notificationsEnabled:Boolean = true,
    val offlinePacksCount:   Int     = 3
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SettingsPreferences
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = prefs.settingsFlow
        .map { s ->
            SettingsUiState(
                voiceEnabled         = s.voiceEnabled,
                ttsEnabled           = s.ttsEnabled,
                speechSpeed          = s.speechSpeed,
                ttsLocale            = s.ttsLocale,
                defaultLanguage      = s.defaultLanguage,
                translationTarget    = s.translationTarget,
                performanceMode      = s.performanceMode,
                notificationsEnabled = s.notificationsEnabled,
                offlinePacksCount    = s.offlinePacksCount
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setVoiceEnabled(v: Boolean)         = save { prefs.setVoiceEnabled(v) }
    fun setTtsEnabled(v: Boolean)           = save { prefs.setTtsEnabled(v) }
    fun setSpeechSpeed(speed: Float)        = save { prefs.setSpeechSpeed(speed) }
    fun setDefaultLanguage(lang: String)    = save {
        prefs.setDefaultLanguage(lang)
        prefs.setTtsLocale(languageToLocaleTag(lang))
    }
    fun setTranslationTarget(lang: String)  = save { prefs.setTranslationTarget(lang) }
    fun setPerformanceMode(mode: String)    = save { prefs.setPerformanceMode(mode) }
    fun setNotificationsEnabled(v: Boolean) = save { prefs.setNotificationsEnabled(v) }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun languageToLocaleTag(language: String) = when (language) {
        "Hindi"                 -> "hi"
        "Spanish"               -> "es"
        "French"                -> "fr"
        "German"                -> "de"
        "Arabic"                -> "ar"
        "Chinese (Simplified)"  -> "zh-CN"
        "Chinese (Traditional)" -> "zh-TW"
        "Japanese"              -> "ja"
        "Korean"                -> "ko"
        "Portuguese"            -> "pt"
        "Russian"               -> "ru"
        "Italian"               -> "it"
        "Tamil"                 -> "ta"
        "Telugu"                -> "te"
        "Bengali"               -> "bn"
        else                    -> "en"
    }

    // Expose analysis throttle based on performance mode
    val analysisIntervalMs: Long
        get() = when (uiState.value.performanceMode) {
            "Power Saver"  -> 4000L
            "Performance"  -> 1000L
            else           -> 2000L
        }
}
