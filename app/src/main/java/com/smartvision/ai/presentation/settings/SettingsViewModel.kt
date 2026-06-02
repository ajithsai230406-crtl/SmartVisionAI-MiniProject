package com.smartvision.ai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.preferences.SettingsRepository
import com.smartvision.ai.utils.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    val ttsManager: TtsManager
) : ViewModel() {
    val darkMode = settingsRepository.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoDeletePeriod = settingsRepository.autoDeletePeriod.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NEVER")
    val autoDeleteOcr = settingsRepository.autoDeleteOcr.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoDeleteHomework = settingsRepository.autoDeleteHomework.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoDeleteQr = settingsRepository.autoDeleteQr.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoDeleteObject = settingsRepository.autoDeleteObject.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoDeleteMedicine = settingsRepository.autoDeleteMedicine.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoDeleteTranslation = settingsRepository.autoDeleteTranslation.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val voiceSpeed = settingsRepository.voiceSpeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)
    val fontSize = settingsRepository.fontSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
    val notifEnabled = settingsRepository.notifEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val hapticEnabled = settingsRepository.hapticEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoSave = settingsRepository.autoSave.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val highContrast = settingsRepository.highContrast.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val voiceGuidance = settingsRepository.voiceGuidance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val colorblindMode = settingsRepository.colorblindMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NONE")
    val largeButtons = settingsRepository.largeButtons.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val gesturesEnabled = settingsRepository.gesturesEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val aiVoiceGuidance = settingsRepository.aiVoiceGuidance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setDarkMode(enabled)
    }

    fun setAutoDeletePeriod(period: String) = viewModelScope.launch {
        settingsRepository.setAutoDeletePeriod(period)
    }

    fun setAutoDeleteOcr(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoDeleteOcr(enabled)
    }

    fun setAutoDeleteHomework(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoDeleteHomework(enabled)
    }

    fun setAutoDeleteQr(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoDeleteQr(enabled)
    }

    fun setAutoDeleteObject(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoDeleteObject(enabled)
    }

    fun setAutoDeleteMedicine(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoDeleteMedicine(enabled)
    }

    fun setAutoDeleteTranslation(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoDeleteTranslation(enabled)
    }

    fun setVoiceSpeed(speed: Float) = viewModelScope.launch {
        settingsRepository.setVoiceSpeed(speed)
    }

    fun setFontSize(size: Int) = viewModelScope.launch {
        settingsRepository.setFontSize(size)
    }

    fun setNotifEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNotifEnabled(enabled)
    }

    fun setHapticEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setHapticEnabled(enabled)
    }

    fun setAutoSave(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoSave(enabled)
    }

    fun setHighContrast(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setHighContrast(enabled)
    }

    fun setVoiceGuidance(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setVoiceGuidance(enabled)
    }

    fun setColorblindMode(mode: String) = viewModelScope.launch {
        settingsRepository.setColorblindMode(mode)
    }

    fun setLargeButtons(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setLargeButtons(enabled)
    }

    fun setGesturesEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setGesturesEnabled(enabled)
    }

    fun setAiVoiceGuidance(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAiVoiceGuidance(enabled)
    }
}

@HiltViewModel
class SettingsFragmentViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val darkMode = settingsRepository.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setDarkMode(enabled)
    }
}
