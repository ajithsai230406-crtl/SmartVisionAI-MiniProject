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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val voiceEnabled: Boolean       = true,
    val ttsEnabled: Boolean         = true,
    val defaultLanguage: String     = "English",
    val performanceMode: String     = "Balanced",
    val notificationsEnabled: Boolean = true,
    val offlinePacksCount: Int      = 3
)

@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val VOICE_ENABLED         = booleanPreferencesKey("voice_enabled")
        val TTS_ENABLED           = booleanPreferencesKey("tts_enabled")
        val DEFAULT_LANGUAGE      = stringPreferencesKey("default_language")
        val PERFORMANCE_MODE      = stringPreferencesKey("performance_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val OFFLINE_PACKS_COUNT   = intPreferencesKey("offline_packs_count")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            AppSettings(
                voiceEnabled          = prefs[VOICE_ENABLED] ?: true,
                ttsEnabled            = prefs[TTS_ENABLED] ?: true,
                defaultLanguage       = prefs[DEFAULT_LANGUAGE] ?: "English",
                performanceMode       = prefs[PERFORMANCE_MODE] ?: "Balanced",
                notificationsEnabled  = prefs[NOTIFICATIONS_ENABLED] ?: true,
                offlinePacksCount     = prefs[OFFLINE_PACKS_COUNT] ?: 3
            )
        }

    suspend fun setVoiceEnabled(enabled: Boolean) =
        context.dataStore.edit { it[VOICE_ENABLED] = enabled }

    suspend fun setTtsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[TTS_ENABLED] = enabled }

    suspend fun setDefaultLanguage(lang: String) =
        context.dataStore.edit { it[DEFAULT_LANGUAGE] = lang }

    suspend fun setPerformanceMode(mode: String) =
        context.dataStore.edit { it[PERFORMANCE_MODE] = mode }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
}
