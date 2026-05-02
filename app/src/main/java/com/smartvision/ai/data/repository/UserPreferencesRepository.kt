package com.smartvision.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("sv_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME    = stringPreferencesKey("app_theme")
        val TTS      = booleanPreferencesKey("tts_enabled")
        val HISTORY  = booleanPreferencesKey("save_history")
        val LIVE     = booleanPreferencesKey("live_detection")
        val LANG     = stringPreferencesKey("default_lang")
    }

    val appThemeFlow:    Flow<String>  = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[Keys.THEME]   ?: "DARK" }
    val ttsEnabledFlow:  Flow<Boolean> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[Keys.TTS]     ?: true }
    val saveHistoryFlow: Flow<Boolean> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[Keys.HISTORY] ?: true }
    val liveDetFlow:     Flow<Boolean> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[Keys.LIVE]    ?: false }

    suspend fun setAppTheme(v: String)  { context.dataStore.edit { it[Keys.THEME]   = v } }
    suspend fun setTts(v: Boolean)      { context.dataStore.edit { it[Keys.TTS]     = v } }
    suspend fun setSaveHistory(v: Boolean) { context.dataStore.edit { it[Keys.HISTORY] = v } }
    suspend fun setLiveDetection(v: Boolean) { context.dataStore.edit { it[Keys.LIVE]  = v } }
}
