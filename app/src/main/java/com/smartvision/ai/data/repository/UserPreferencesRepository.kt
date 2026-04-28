package com.smartvision.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

// Extension to create a single DataStore instance per process
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smartvision_prefs")

// ─────────────────────────────────────────────────────────────────────────────
// KEYS
// ─────────────────────────────────────────────────────────────────────────────

private object PrefsKeys {
    val APP_THEME        = stringPreferencesKey("app_theme")          // AppTheme.name
    val DEFAULT_LANG     = stringPreferencesKey("default_language")
    val TTS_ENABLED      = booleanPreferencesKey("tts_enabled")
    val SAVE_HISTORY     = booleanPreferencesKey("save_history")
    val LIVE_DETECTION   = booleanPreferencesKey("live_detection")
    val ONBOARDED        = booleanPreferencesKey("onboarded")
}

// ─────────────────────────────────────────────────────────────────────────────
// USER PREFERENCES REPOSITORY
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ── Read flows ────────────────────────────────────────────────────────────

    val appThemeFlow: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[PrefsKeys.APP_THEME] ?: "DARK" }

    val ttsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[PrefsKeys.TTS_ENABLED] ?: true }

    val saveHistoryFlow: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[PrefsKeys.SAVE_HISTORY] ?: true }

    val liveDetectionFlow: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[PrefsKeys.LIVE_DETECTION] ?: false }

    val defaultLangFlow: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[PrefsKeys.DEFAULT_LANG] ?: "en" }

    val onboardedFlow: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[PrefsKeys.ONBOARDED] ?: false }

    // ── Combined preferences ──────────────────────────────────────────────────

    val allPrefsFlow: Flow<com.smartvision.ai.domain.models.UserPreferences> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                com.smartvision.ai.domain.models.UserPreferences(
                    appTheme        = prefs[PrefsKeys.APP_THEME]      ?: "DARK",
                    defaultLanguage = prefs[PrefsKeys.DEFAULT_LANG]   ?: "en",
                    enableTts       = prefs[PrefsKeys.TTS_ENABLED]    ?: true,
                    saveHistory     = prefs[PrefsKeys.SAVE_HISTORY]   ?: true,
                    liveDetection   = prefs[PrefsKeys.LIVE_DETECTION] ?: false
                )
            }

    // ── Write ─────────────────────────────────────────────────────────────────

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { it[PrefsKeys.APP_THEME] = theme }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.TTS_ENABLED] = enabled }
    }

    suspend fun setSaveHistory(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.SAVE_HISTORY] = enabled }
    }

    suspend fun setLiveDetection(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.LIVE_DETECTION] = enabled }
    }

    suspend fun setDefaultLang(code: String) {
        context.dataStore.edit { it[PrefsKeys.DEFAULT_LANG] = code }
    }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.ONBOARDED] = value }
    }
}
