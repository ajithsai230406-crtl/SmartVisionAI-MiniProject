package com.smartvision.ai

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import com.smartvision.ai.ui.navigation.Routes
import com.smartvision.ai.ui.navigation.SmartVisionNavGraph
import com.smartvision.ai.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Edge-to-edge with transparent status bar — removes the white bar
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // Keep splash visible until first frame drawn (optional: keep until
        // auth state resolved for smoother UX)
        splashScreen.setKeepOnScreenCondition { false }

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
