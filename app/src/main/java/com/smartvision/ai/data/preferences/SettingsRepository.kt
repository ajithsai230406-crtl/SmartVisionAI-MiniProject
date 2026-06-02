package com.smartvision.ai.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore("smartvision_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val autoDeletePeriodKey = stringPreferencesKey("auto_delete_period")
    private val autoDeleteOcrKey = booleanPreferencesKey("auto_delete_ocr")
    private val autoDeleteHomeworkKey = booleanPreferencesKey("auto_delete_homework")
    private val autoDeleteQrKey = booleanPreferencesKey("auto_delete_qr")
    private val autoDeleteObjectKey = booleanPreferencesKey("auto_delete_object")
    private val autoDeleteMedicineKey = booleanPreferencesKey("auto_delete_medicine")
    private val autoDeleteTranslationKey = booleanPreferencesKey("auto_delete_translation")
    private val voiceSpeedKey = floatPreferencesKey("voice_speed")
    private val fontSizeKey = intPreferencesKey("font_size")
    private val notifEnabledKey = booleanPreferencesKey("notif_enabled")
    private val hapticEnabledKey = booleanPreferencesKey("haptic_enabled")
    private val autoSaveKey = booleanPreferencesKey("auto_save")
    private val highContrastKey = booleanPreferencesKey("high_contrast")
    private val voiceGuidanceKey = booleanPreferencesKey("voice_guidance")
    private val colorblindModeKey = stringPreferencesKey("colorblind_mode")
    private val largeButtonsKey = booleanPreferencesKey("large_buttons")
    private val gesturesEnabledKey = booleanPreferencesKey("gestures_enabled")
    private val aiVoiceGuidanceKey = booleanPreferencesKey("ai_voice_guidance")

    val darkMode: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[darkModeKey] ?: true
    }

    val autoDeletePeriod: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[autoDeletePeriodKey] ?: "NEVER"
    }

    val autoDeleteOcr: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[autoDeleteOcrKey] ?: false
    }

    val autoDeleteHomework: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[autoDeleteHomeworkKey] ?: false
    }

    val autoDeleteQr: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[autoDeleteQrKey] ?: false
    }

    val autoDeleteObject: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[autoDeleteObjectKey] ?: false
    }

    val autoDeleteMedicine: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[autoDeleteMedicineKey] ?: false
    }

    val autoDeleteTranslation: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[autoDeleteTranslationKey] ?: false
    }

    val voiceSpeed: Flow<Float> = context.settingsStore.data.map { preferences ->
        preferences[voiceSpeedKey] ?: 1.0f
    }

    val fontSize: Flow<Int> = context.settingsStore.data.map { preferences ->
        preferences[fontSizeKey] ?: 1
    }

    val notifEnabled: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[notifEnabledKey] ?: true
    }

    val hapticEnabled: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[hapticEnabledKey] ?: true
    }

    val autoSave: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[autoSaveKey] ?: true
    }

    val highContrast: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[highContrastKey] ?: false
    }

    val voiceGuidance: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[voiceGuidanceKey] ?: false
    }

    val colorblindMode: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[colorblindModeKey] ?: "NONE"
    }

    val largeButtons: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[largeButtonsKey] ?: false
    }

    val gesturesEnabled: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[gesturesEnabledKey] ?: false
    }

    val aiVoiceGuidance: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[aiVoiceGuidanceKey] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsStore.edit { it[darkModeKey] = enabled }
    }

    suspend fun setAutoDeletePeriod(period: String) {
        context.settingsStore.edit { it[autoDeletePeriodKey] = period }
    }

    suspend fun setAutoDeleteOcr(enabled: Boolean) {
        context.settingsStore.edit { it[autoDeleteOcrKey] = enabled }
    }

    suspend fun setAutoDeleteHomework(enabled: Boolean) {
        context.settingsStore.edit { it[autoDeleteHomeworkKey] = enabled }
    }

    suspend fun setAutoDeleteQr(enabled: Boolean) {
        context.settingsStore.edit { it[autoDeleteQrKey] = enabled }
    }

    suspend fun setAutoDeleteObject(enabled: Boolean) {
        context.settingsStore.edit { it[autoDeleteObjectKey] = enabled }
    }

    suspend fun setAutoDeleteMedicine(enabled: Boolean) {
        context.settingsStore.edit { it[autoDeleteMedicineKey] = enabled }
    }

    suspend fun setAutoDeleteTranslation(enabled: Boolean) {
        context.settingsStore.edit { it[autoDeleteTranslationKey] = enabled }
    }

    suspend fun setVoiceSpeed(speed: Float) {
        context.settingsStore.edit { it[voiceSpeedKey] = speed }
    }

    suspend fun setFontSize(size: Int) {
        context.settingsStore.edit { it[fontSizeKey] = size }
    }

    suspend fun setNotifEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[notifEnabledKey] = enabled }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[hapticEnabledKey] = enabled }
    }

    suspend fun setAutoSave(enabled: Boolean) {
        context.settingsStore.edit { it[autoSaveKey] = enabled }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.settingsStore.edit { it[highContrastKey] = enabled }
    }

    suspend fun setVoiceGuidance(enabled: Boolean) {
        context.settingsStore.edit { it[voiceGuidanceKey] = enabled }
    }

    suspend fun setColorblindMode(mode: String) {
        context.settingsStore.edit { it[colorblindModeKey] = mode }
    }

    suspend fun setLargeButtons(enabled: Boolean) {
        context.settingsStore.edit { it[largeButtonsKey] = enabled }
    }

    suspend fun setGesturesEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[gesturesEnabledKey] = enabled }
    }

    suspend fun setAiVoiceGuidance(enabled: Boolean) {
        context.settingsStore.edit { it[aiVoiceGuidanceKey] = enabled }
    }
}



