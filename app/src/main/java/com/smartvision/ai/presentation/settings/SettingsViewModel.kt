package com.smartvision.ai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsFragmentViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val darkMode = settingsRepository.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setDarkMode(enabled)
    }
}
