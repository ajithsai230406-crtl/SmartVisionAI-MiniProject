package com.smartvision.ai.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.smartvision.ai.domain.models.ModuleType
import com.smartvision.ai.ui.screens.camera.CameraScreen
import com.smartvision.ai.ui.screens.home.HomeScreen
import com.smartvision.ai.ui.screens.profile.ProfileScreen
import com.smartvision.ai.ui.screens.result.ResultScreen
import com.smartvision.ai.ui.screens.ocr.OcrScreen
import com.smartvision.ai.ui.screens.objectdetection.ObjectDetectionScreen
import com.smartvision.ai.ui.screens.qrscanner.QrScannerScreen
import com.smartvision.ai.ui.screens.translator.TranslatorScreen
import com.smartvision.ai.ui.screens.student.StudentHelperScreen
import com.smartvision.ai.ui.screens.MedicalAndWasteScreens
import com.smartvision.ai.ui.screens.settings.SettingsScreen
import com.smartvision.ai.ui.screens.history.HistoryScreen
import com.smartvision.ai.ui.screens.auth.LoginScreen
import com.smartvision.ai.ui.theme.AppTheme

// ─────────────────────────────────────────────────────────────────────────────
// ROUTE CONSTANTS
// ─────────────────────────────────────────────────────────────────────────────

object Routes {
    const val LOGIN          = "login"
    const val HOME           = "home"
    const val PROFILE        = "profile"
    const val CAMERA         = "camera/{moduleRoute}"
    const val RESULT         = "result/{moduleRoute}"
    const val HISTORY        = "history"
    const val SETTINGS       = "settings"

    val OBJECT_DETECTION = ModuleType.OBJECT_DETECTION.route
    val TEXT_SCANNER     = ModuleType.TEXT_SCANNER.route
    val TRANSLATOR       = ModuleType.TRANSLATOR.route
    val VOICE_TRANSLATOR = ModuleType.VOICE_TRANSLATOR.route
    val STUDENT_HELPER   = ModuleType.STUDENT_HELPER.route
    val MEDICAL_SCANNER  = ModuleType.MEDICAL_SCANNER.route
    val WASTE_CLASSIFIER = ModuleType.WASTE_CLASSIFIER.route
    val QR_SCANNER       = ModuleType.QR_SCANNER.route

    fun cameraFor(moduleRoute: String) = "camera/$moduleRoute"
    fun resultFor(moduleRoute: String) = "result/$moduleRoute"
}

// ─────────────────────────────────────────────────────────────────────────────
// APP NAV GRAPH
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SmartVisionNavGraph(
    startDestination: String = Routes.HOME,
    appTheme:         AppTheme,
    onThemeChange:    (AppTheme) -> Unit
) {
    val navController = rememberNavController()

    // Helper to navigate to Home clearing back-stack
    fun goHome() = navController.navigate(Routes.HOME) {
        popUpTo(Routes.HOME) { inclusive = false }
        launchSingleTop = true
    }

    NavHost(
        navController      = navController,
        startDestination   = startDestination,
        enterTransition    = { slideInHorizontally(tween(300)) { it }  + fadeIn(tween(300))  },
        exitTransition     = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300))  },
        popExitTransition  = { slideOutHorizontally(tween(300)) { it }  + fadeOut(tween(300)) }
    ) {

        // ── Auth ─────────────────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }

        // ── Home ─────────────────────────────────────────────────────────────
        composable(Routes.HOME) {
            HomeScreen(
                onModuleClick  = { module ->
                    val cameraModules = setOf(
                        ModuleType.OBJECT_DETECTION, ModuleType.TEXT_SCANNER,
                        ModuleType.MEDICAL_SCANNER,  ModuleType.WASTE_CLASSIFIER,
                        ModuleType.QR_SCANNER,       ModuleType.STUDENT_HELPER
                    )
                    if (module in cameraModules) {
                        navController.navigate(Routes.cameraFor(module.route))
                    } else {
                        navController.navigate(module.route)
                    }
                },
                onHistoryClick  = { navController.navigate(Routes.HISTORY) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onScanClick     = { navController.navigate(Routes.cameraFor("object_detection")) },
                onProfileClick  = { navController.navigate(Routes.PROFILE) }
            )
        }

        // ── Profile ───────────────────────────────────────────────────────────
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack   = { navController.popBackStack() },
                onHome   = { goHome() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        // ── Camera ────────────────────────────────────────────────────────────
        composable(
            route     = Routes.CAMERA,
            arguments = listOf(navArgument("moduleRoute") { type = NavType.StringType })
        ) { back ->
            val moduleRoute = back.arguments?.getString("moduleRoute") ?: "object_detection"
            CameraScreen(
                moduleRoute = moduleRoute,
                onResult    = { navController.navigate(Routes.resultFor(moduleRoute)) { popUpTo(Routes.HOME) } },
                onBack      = { navController.popBackStack() },
                onHome      = { goHome() }
            )
        }

        // ── Result ────────────────────────────────────────────────────────────
        composable(
            route     = Routes.RESULT,
            arguments = listOf(navArgument("moduleRoute") { type = NavType.StringType })
        ) { back ->
            val moduleRoute = back.arguments?.getString("moduleRoute") ?: "object_detection"
            ResultScreen(
                moduleRoute = moduleRoute,
                onBack      = { navController.popBackStack() },
                onRescan    = { navController.navigate(Routes.cameraFor(moduleRoute)) { popUpTo(Routes.HOME) } }
            )
        }

        // ── Standalone Modules ────────────────────────────────────────────────
        composable(Routes.TRANSLATOR) {
            TranslatorScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.VOICE_TRANSLATOR) {
            TranslatorScreen(
                voiceMode = true,
                onBack    = { navController.popBackStack() }
            )
        }
        composable(Routes.QR_SCANNER) {
            QrScannerScreen(onBack = { navController.popBackStack() })
        }

        // ── History & Settings ────────────────────────────────────────────────
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack      = { navController.popBackStack() },
                onItemClick = {}
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                currentTheme  = appTheme,
                onThemeChange = onThemeChange,
                onBack        = { navController.popBackStack() },
                onHome        = { goHome() }
            )
        }
    }
}
