package com.smartvision.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.smartvision.ai.ui.navigation.Routes
import com.smartvision.ai.ui.navigation.SmartVisionNavGraph
import com.smartvision.ai.ui.theme.AppTheme
import com.smartvision.ai.ui.theme.SmartVisionTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var appTheme by remember { mutableStateOf(AppTheme.DARK) }
            SmartVisionTheme(appTheme = appTheme) {
                SmartVisionNavGraph(
                    startDestination = if (auth.currentUser != null) Routes.HOME else Routes.LOGIN,
                    appTheme         = appTheme,
                    onThemeChange    = { appTheme = it }
                )
            }
        }
    }
}
