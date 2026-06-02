package com.smartvision.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.navigation.SmartVisionNavHost
import com.smartvision.ai.ui.theme.SmartVisionTheme
import com.smartvision.ai.data.preferences.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val scope = rememberCoroutineScope()
            val darkMode by settingsRepository.darkMode.collectAsState(initial = true)
            val fontSizeSetting by settingsRepository.fontSize.collectAsState(initial = 1)
            val highContrast by settingsRepository.highContrast.collectAsState(initial = false)
            val colorblindMode by settingsRepository.colorblindMode.collectAsState(initial = "NONE")
            val largeButtons by settingsRepository.largeButtons.collectAsState(initial = false)
            val voiceGuidance by settingsRepository.voiceGuidance.collectAsState(initial = false)
            val aiVoiceGuidance by settingsRepository.aiVoiceGuidance.collectAsState(initial = false)

            val fontSizeMultiplier = when (fontSizeSetting) {
                0 -> 0.85f
                1 -> 1.0f
                2 -> 1.2f
                3 -> 1.4f
                else -> 1.0f
            }

            SmartVisionTheme(
                darkTheme = darkMode,
                fontSizeMultiplier = fontSizeMultiplier,
                highContrast = highContrast,
                colorblindMode = colorblindMode,
                largeButtons = largeButtons,
                voiceGuidance = voiceGuidance,
                aiVoiceGuidance = aiVoiceGuidance
            ) {
                SmartVisionNavHost(
                    isDarkTheme   = darkMode,
                    onThemeToggle = {
                        val current = darkMode
                        scope.launch { settingsRepository.setDarkMode(!current) }
                    }
                )
            }
        }
    }
}

