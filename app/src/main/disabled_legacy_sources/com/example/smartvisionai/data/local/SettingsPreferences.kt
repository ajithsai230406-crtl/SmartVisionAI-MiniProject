package com.example.smartvisionai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("smart_vision_settings")

data class AppSettings(
    val voiceEnabled:         Boolean = true,
    val ttsEnabled:           Boolean = true,
    val speechSpeed:          Float   = 1.0f,
    val ttsLocale:            String  = "en",
    val defaultLanguage:      String  = "English",
    val translationTarget:    String  = "English",
    val performanceMode:      String  = "Balanced",
    val notificationsEnabled: Boolean = true,
    val offlinePacksCount:    Int     = 3
)

@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val VOICE_ENABLED          = booleanPreferencesKey("voice_enabled")
        val TTS_ENABLED            = booleanPreferencesKey("tts_enabled")
        val SPEECH_SPEED           = floatPreferencesKey("speech_speed")
        val TTS_LOCALE             = stringPreferencesKey("tts_locale")
        val DEFAULT_LANGUAGE       = stringPreferencesKey("default_language")
        val TRANSLATION_TARGET     = stringPreferencesKey("translation_target")
        val PERFORMANCE_MODE       = stringPreferencesKey("performance_mode")
        val NOTIFICATIONS_ENABLED  = booleanPreferencesKey("notifications_enabled")
        val OFFLINE_PACKS_COUNT    = intPreferencesKey("offline_packs_count")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                voiceEnabled         = prefs[VOICE_ENABLED]         ?: true,
                ttsEnabled           = prefs[TTS_ENABLED]           ?: true,
                speechSpeed          = prefs[SPEECH_SPEED]          ?: 1.0f,
                ttsLocale            = prefs[TTS_LOCALE]            ?: "en",
                defaultLanguage      = prefs[DEFAULT_LANGUAGE]      ?: "English",
                translationTarget    = prefs[TRANSLATION_TARGET]    ?: "English",
                performanceMode      = prefs[PERFORMANCE_MODE]      ?: "Balanced",
                notificationsEnabled = prefs[NOTIFICATIONS_ENABLED] ?: true,
                offlinePacksCount    = prefs[OFFLINE_PACKS_COUNT]   ?: 3
            )
        }

    suspend fun setVoiceEnabled(v: Boolean)        = edit { it[VOICE_ENABLED] = v }
    suspend fun setTtsEnabled(v: Boolean)          = edit { it[TTS_ENABLED] = v }
    suspend fun setSpeechSpeed(s: Float)           = edit { it[SPEECH_SPEED] = s }
    suspend fun setTtsLocale(l: String)            = edit { it[TTS_LOCALE] = l }
    suspend fun setDefaultLanguage(l: String)      = edit { it[DEFAULT_LANGUAGE] = l }
    suspend fun setTranslationTarget(l: String)    = edit { it[TRANSLATION_TARGET] = l }
    suspend fun setPerformanceMode(m: String)      = edit { it[PERFORMANCE_MODE] = m }
    suspend fun setNotificationsEnabled(v: Boolean)= edit { it[NOTIFICATIONS_ENABLED] = v }
    suspend fun setOfflinePacksCount(n: Int)       = edit { it[OFFLINE_PACKS_COUNT] = n }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
