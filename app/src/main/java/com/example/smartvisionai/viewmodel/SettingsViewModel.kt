package com.example.smartvisionai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvisionai.data.local.AppSettings
import com.example.smartvisionai.data.local.SettingsPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SettingsPreferences
) : ViewModel() {

    val uiState: StateFlow<AppSettings> = prefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setVoiceEnabled(v: Boolean)          { viewModelScope.launch { prefs.setVoiceEnabled(v) } }
    fun setTtsEnabled(v: Boolean)            { viewModelScope.launch { prefs.setTtsEnabled(v) } }
    fun setPerformanceMode(mode: String)     { viewModelScope.launch { prefs.setPerformanceMode(mode) } }
    fun setNotificationsEnabled(v: Boolean)  { viewModelScope.launch { prefs.setNotificationsEnabled(v) } }

    fun showLanguagePicker() {
        // Trigger language picker dialog — handled in Compose via state
    }

    fun openOfflinePacks() {
        // Navigate to offline packs screen — handled by nav
    }
}
