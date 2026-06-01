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
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.allPrefsFlow.collect { p ->
                _state.update {
                    it.copy(
                        theme = p.appTheme,
                        tts = p.enableTts,
                        history = p.saveHistory,
                        liveDetection = p.liveDetection,
                        defaultLang = p.defaultLanguage
                    )
                }
            }
        }
    }

    fun setTheme(t: AppTheme) {
        viewModelScope.launch { prefs.setAppTheme(t.name) }
        _state.update { it.copy(theme = t.name) }
    }

    fun setTts(v: Boolean) {
        viewModelScope.launch { prefs.setTtsEnabled(v) }
        _state.update { it.copy(tts = v) }
    }

    fun setHistory(v: Boolean) {
        viewModelScope.launch { prefs.setSaveHistory(v) }
        _state.update { it.copy(history = v) }
    }

    fun setLive(v: Boolean) {
        viewModelScope.launch { prefs.setLiveDetection(v) }
        _state.update { it.copy(liveDetection = v) }
    }

    fun setAutoDelete(days: Int) {
        _state.update { it.copy(autoDeleteDays = days) }
    }

    fun signOut() {
        auth.signOut()
    }

    val userName: String get() = auth.currentUser?.displayName ?: "Guest"
    val userEmail: String get() = auth.currentUser?.email ?: "Not signed in"
    val isLoggedIn: Boolean get() = auth.currentUser != null
}
