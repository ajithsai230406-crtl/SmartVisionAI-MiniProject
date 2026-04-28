package com.smartvision.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.auth.FirebaseAuth
import com.smartvision.ai.data.repository.UserPreferencesRepository
import com.smartvision.ai.ui.navigation.Routes
import com.smartvision.ai.ui.navigation.SmartVisionNavGraph
import com.smartvision.ai.ui.theme.AppTheme
import com.smartvision.ai.ui.theme.SmartVisionTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var auth:  FirebaseAuth
    @Inject lateinit var prefs: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var prefsReady = false
        splash.setKeepOnScreenCondition { !prefsReady }

        setContent {
            var appTheme by remember { mutableStateOf(AppTheme.DARK) }
            val systemUiController = rememberSystemUiController()

            LaunchedEffect(Unit) {
                val saved = prefs.appThemeFlow.first()
                appTheme = runCatching { AppTheme.valueOf(saved) }.getOrDefault(AppTheme.DARK)
                prefsReady = true
            }

            LaunchedEffect(appTheme) {
                systemUiController.setSystemBarsColor(
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    darkIcons = appTheme == AppTheme.LIGHT,
                    isNavigationBarContrastEnforced = false
                )
            }

            SmartVisionTheme(appTheme = appTheme) {
                SmartVisionNavGraph(
                    startDestination = if (auth.currentUser != null) Routes.HOME else Routes.LOGIN,
                    appTheme         = appTheme,
                    onThemeChange    = { newTheme ->
                        appTheme = newTheme
                        lifecycleScope.launch { prefs.setAppTheme(newTheme.name) }
                    }
                )
            }
        }
    }
}
