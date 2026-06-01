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

// ─────────────────────────────────────────────────────────────────────────────
// SETTINGS VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

data class SettingsUiState(
    val appTheme:      String  = "DARK",
    val ttsEnabled:    Boolean = true,
    val saveHistory:   Boolean = true,
    val liveDetection: Boolean = false,
    val defaultLang:   String  = "en"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val auth:  FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.allPrefsFlow.collect { p ->
                _uiState.update {
                    it.copy(
                        appTheme      = p.appTheme,
                        ttsEnabled    = p.enableTts,
                        saveHistory   = p.saveHistory,
                        liveDetection = p.liveDetection,
                        defaultLang   = p.defaultLanguage
                    )
                }
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { prefs.setAppTheme(theme.name) }
    }

    fun setTts(enabled: Boolean) {
        viewModelScope.launch { prefs.setTtsEnabled(enabled) }
        _uiState.update { it.copy(ttsEnabled = enabled) }
    }

    fun setSaveHistory(enabled: Boolean) {
        viewModelScope.launch { prefs.setSaveHistory(enabled) }
        _uiState.update { it.copy(saveHistory = enabled) }
    }

    fun setLiveDetection(enabled: Boolean) {
        viewModelScope.launch { prefs.setLiveDetection(enabled) }
        _uiState.update { it.copy(liveDetection = enabled) }
    }

    fun signOut() { auth.signOut() }

    val isLoggedIn: Boolean get() = auth.currentUser != null
    val userEmail:  String  get() = auth.currentUser?.email ?: "Guest"
    val userName:   String  get() = auth.currentUser?.displayName ?: "User"
}
