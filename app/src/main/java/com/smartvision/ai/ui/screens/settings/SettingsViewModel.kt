package com.smartvision.ai.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.smartvision.ai.data.repository.UserPreferencesRepository
import com.smartvision.ai.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val theme: String = "DARK",
    val tts: Boolean = true,
    val history: Boolean = true,
    val liveDetection: Boolean = false,
    val autoDeleteDays: Int = 7,
    val defaultLang: String = "en"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.appThemeFlow,
                prefs.ttsEnabledFlow,
                prefs.saveHistoryFlow,
                prefs.liveDetFlow
            ) { theme, tts, history, live ->
                SettingsState(
                    theme = theme,
                    tts = tts,
                    history = history,
                    liveDetection = live,
                    autoDeleteDays = _state.value.autoDeleteDays,
                    defaultLang = _state.value.defaultLang
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun setTheme(t: AppTheme) {
        viewModelScope.launch { prefs.setAppTheme(t.name) }
    }

    fun setTts(v: Boolean) {
        viewModelScope.launch { prefs.setTts(v) }
    }

    fun setHistory(v: Boolean) {
        viewModelScope.launch { prefs.setSaveHistory(v) }
    }

    fun setLive(v: Boolean) {
        viewModelScope.launch { prefs.setLiveDetection(v) }
    }

    fun setAutoDelete(days: Int) {
        _state.update { it.copy(autoDeleteDays = days) }
    }

    fun signOut() {
        auth.signOut()
    }

    val userName get() = auth.currentUser?.displayName ?: "Guest"
    val userEmail get() = auth.currentUser?.email ?: "Not signed in"
    val isLoggedIn get() = auth.currentUser != null
}
