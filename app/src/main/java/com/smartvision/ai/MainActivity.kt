package com.smartvision.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.navigation.SmartVisionNavHost
import com.smartvision.ai.ui.theme.SmartVisionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Theme toggle state — defaults to dark neon
            var isDarkTheme by remember { mutableStateOf(true) }

            SmartVisionTheme(darkTheme = isDarkTheme) {
                SmartVisionNavHost(
                    isDarkTheme   = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}
