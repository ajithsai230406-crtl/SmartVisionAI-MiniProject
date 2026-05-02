package com.smartvision.ai.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.*
import com.smartvision.ai.domain.models.ModuleType
import com.smartvision.ai.ui.screens.auth.LoginScreen
import com.smartvision.ai.ui.screens.camera.CameraScreen
import com.smartvision.ai.ui.screens.history.HistoryScreen
import com.smartvision.ai.ui.screens.home.HomeScreen
import com.smartvision.ai.ui.screens.medical.MedicalScannerScreen
import com.smartvision.ai.ui.screens.objectdetection.ObjectDetectionScreen
import com.smartvision.ai.ui.screens.ocr.OcrScreen
import com.smartvision.ai.ui.screens.qrscanner.QrScannerScreen
import com.smartvision.ai.ui.screens.result.ResultScreen
import com.smartvision.ai.ui.screens.settings.SettingsScreen
import com.smartvision.ai.ui.screens.student.StudentHelperScreen
import com.smartvision.ai.ui.screens.translator.TranslatorScreen
import com.smartvision.ai.ui.screens.waste.WasteClassifierScreen
import com.smartvision.ai.ui.theme.AppTheme

object Routes {
    const val LOGIN            = "login"
    const val HOME             = "home"
    const val HISTORY          = "history"
    const val SETTINGS         = "settings"
    const val CAMERA           = "camera/{moduleRoute}"
    const val RESULT           = "result/{moduleRoute}"
    const val QR               = "qr_scanner"
    const val TRANSLATOR       = "translator"
    const val VOICE_TRANSLATOR = "voice_translator"
    const val STUDENT          = "student_helper"
    const val MEDICAL          = "medical_scanner"
    const val WASTE            = "waste_classifier"
    const val OCR              = "text_scanner_detail"
    const val OBJECT_DETECT    = "object_detection_detail"
    fun camera(r: String) = "camera/$r"
    fun result(r: String) = "result/$r"
}

@Composable
fun SmartVisionNavGraph(startDestination: String, appTheme: AppTheme, onThemeChange: (AppTheme) -> Unit) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startDestination,
        enterTransition  = { slideInHorizontally(tween(260)) { it } + fadeIn(tween(260)) },
        exitTransition   = { slideOutHorizontally(tween(260)) { -it } + fadeOut(tween(260)) },
        popEnterTransition  = { slideInHorizontally(tween(260)) { -it } + fadeIn(tween(260)) },
        popExitTransition   = { slideOutHorizontally(tween(260)) { it } + fadeOut(tween(260)) }
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = { nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onModuleClick = { module -> when (module) {
                    ModuleType.QR_SCANNER       -> nav.navigate(Routes.QR)
                    ModuleType.TRANSLATOR       -> nav.navigate(Routes.TRANSLATOR)
                    ModuleType.VOICE_TRANSLATOR -> nav.navigate(Routes.VOICE_TRANSLATOR)
                    ModuleType.STUDENT_HELPER   -> nav.navigate(Routes.STUDENT)
                    else -> nav.navigate(Routes.camera(module.route))
                }},
                onHistoryClick = { nav.navigate(Routes.HISTORY) },
                onSettingsClick = { nav.navigate(Routes.SETTINGS) },
                onScanClick = { nav.navigate(Routes.camera(ModuleType.OBJECT_DETECTION.route)) }
            )
        }
        composable(Routes.CAMERA, listOf(navArgument("moduleRoute") { type = NavType.StringType })) { back ->
            val route = back.arguments?.getString("moduleRoute") ?: ModuleType.OBJECT_DETECTION.route
            CameraScreen(moduleRoute = route, onResult = {
                val dest = when (route) {
                    ModuleType.TEXT_SCANNER.route     -> Routes.OCR
                    ModuleType.OBJECT_DETECTION.route -> Routes.OBJECT_DETECT
                    ModuleType.MEDICAL_SCANNER.route  -> Routes.MEDICAL
                    ModuleType.WASTE_CLASSIFIER.route -> Routes.WASTE
                    else -> Routes.result(route)
                }
                nav.navigate(dest) { popUpTo(Routes.HOME) }
            }, onBack = { nav.popBackStack() })
        }
        composable(Routes.RESULT, listOf(navArgument("moduleRoute") { type = NavType.StringType })) { back ->
            val route = back.arguments?.getString("moduleRoute") ?: ModuleType.OBJECT_DETECTION.route
            ResultScreen(moduleRoute = route, onBack = { nav.popBackStack() },
                onRescan = { nav.navigate(Routes.camera(route)) { popUpTo(Routes.HOME) } })
        }
        composable(Routes.OCR) { OcrScreen(onBack = { nav.popBackStack() }, onTranslate = { nav.navigate(Routes.TRANSLATOR) }) }
        composable(Routes.OBJECT_DETECT) { ObjectDetectionScreen(onBack = { nav.popBackStack() }, onCapture = { nav.navigate(Routes.camera(ModuleType.OBJECT_DETECTION.route)) }) }
        composable(Routes.MEDICAL) { MedicalScannerScreen(onBack = { nav.popBackStack() }, onCapture = { nav.navigate(Routes.camera(ModuleType.MEDICAL_SCANNER.route)) }) }
        composable(Routes.WASTE) { WasteClassifierScreen(onBack = { nav.popBackStack() }, onCapture = { nav.navigate(Routes.camera(ModuleType.WASTE_CLASSIFIER.route)) }) }
        composable(Routes.QR)             { QrScannerScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.TRANSLATOR)     { TranslatorScreen(voiceMode = false, onBack = { nav.popBackStack() }) }
        composable(Routes.VOICE_TRANSLATOR) { TranslatorScreen(voiceMode = true, onBack = { nav.popBackStack() }) }
        composable(Routes.STUDENT)        { StudentHelperScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.HISTORY)        { HistoryScreen(onBack = { nav.popBackStack() }, onItemClick = {}) }
        composable(Routes.SETTINGS)       { SettingsScreen(currentTheme = appTheme, onThemeChange = onThemeChange, onBack = { nav.popBackStack() }) }
    }
}
