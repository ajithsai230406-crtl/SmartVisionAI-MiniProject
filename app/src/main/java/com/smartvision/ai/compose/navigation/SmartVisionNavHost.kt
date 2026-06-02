package com.smartvision.ai.compose.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.navigation.*
import androidx.navigation.compose.*
import com.smartvision.ai.compose.components.SmartBottomNavBar
import com.smartvision.ai.compose.screens.*
import com.smartvision.ai.ocr.ui.OcrTranslatorScreen
import com.smartvision.ai.ocr.ui.OcrResultDetailScreen
import com.smartvision.ai.detector.ui.ObjectDetectorScreen as ObjectDetectorScreenV2
import com.smartvision.ai.detector.ui.DetectionHistoryScreen
import com.smartvision.ai.student.ui.StudentHelperScreen as StudentHelperScreenV2
import com.smartvision.ai.waste.ui.WasteClassifierScreen as WasteClassifierScreenV2
import com.smartvision.ai.qr.ui.QrScannerScreen as QrScannerScreenV2
import com.smartvision.ai.translator.ui.SmartTextTranslatorScreen
import com.smartvision.ai.translator.ui.VoiceTranslatorScreen as VoiceTranslatorScreenV2
import com.smartvision.ai.docscanner.ui.DocumentScannerScreen
import com.smartvision.ai.medicine.ui.MedicineScannerScreen as PremiumMedicineScannerScreen
import com.smartvision.ai.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartvision.ai.presentation.auth.AuthViewModel

@Composable
fun SmartVisionNavHost(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val navController  = rememberNavController()
    val currentRoute   = navController.currentBackStackEntryAsState().value?.destination?.route
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = null)
    val onboardingCompleted by authViewModel.onboardingCompleted.collectAsState(initial = null)

    // Routes that show the bottom nav bar
    val bottomNavRoutes = setOf(
        Routes.HOME, Routes.AI_ASSISTANT, Routes.HISTORY, Routes.SETTINGS
    )
    val showBottomNav = currentRoute in bottomNavRoutes

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.History,
        BottomNavItem.AI,
        BottomNavItem.Settings
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomNav) {
                    SmartBottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate   = { route ->
                            navController.navigate(route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        items = bottomNavItems
                    )
                }
            }
            // No floatingActionButton — FloatingAiOrb lives inside HomeScreen
        ) { padding ->
            NavHost(
                navController     = navController,
                startDestination  = Routes.SPLASH,
                modifier          = Modifier
                    .background(DeepNavy)
                    .padding(padding),
                enterTransition   = {
                    fadeIn(tween(280)) + slideInHorizontally { it / 5 }
                },
                exitTransition    = {
                    fadeOut(tween(200)) + slideOutHorizontally { -it / 5 }
                },
                popEnterTransition = {
                    fadeIn(tween(220)) + slideInHorizontally { -it / 5 }
                },
                popExitTransition  = {
                    fadeOut(tween(200)) + slideOutHorizontally { it / 5 }
                }
            ) {
                // ── Splash ───────────────────────────────────────────────────
                composable(Routes.SPLASH) {
                    SplashScreen(
                        onFinished = {
                            val target = when {
                                isLoggedIn == true -> Routes.HOME
                                onboardingCompleted == true -> Routes.LOGIN
                                else -> Routes.ONBOARDING
                            }
                            navController.navigate(target) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    )
                }

                // ── Onboarding ───────────────────────────────────────────────
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onFinished = {
                            authViewModel.setOnboardingCompleted(true)
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        },
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }

                // ── Login / Register ─────────────────────────────────────────
                composable(Routes.LOGIN) {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }

                // ── Home Dashboard ───────────────────────────────────────────
                composable(Routes.HOME) {
                    HomeScreen(
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }

                // ── AI Assistant ─────────────────────────────────────────────
                composable(Routes.AI_ASSISTANT) {
                    AiAssistantScreen(onBack = { navController.popBackStack() })
                }

                // ── OCR (legacy stub) ────────────────────────────────────────
                composable(Routes.OCR_SCANNER) {
                    OcrScannerScreen(
                        onBack   = { navController.popBackStack() },
                        onResult = { text -> navController.navigate(Routes.ocrResult(text)) }
                    )
                }
                composable(Routes.OCR_RESULT) { backStack ->
                    val text = backStack.arguments?.getString("extractedText") ?: ""
                    OcrResultScreen(
                        extractedText = text,
                        onBack        = { navController.popBackStack() },
                        onTranslate   = { navController.navigate(Routes.TRANSLATOR) }
                    )
                }

                // ── NEW OCR Translator ───────────────────────────────────────
                composable(Routes.OCR_TRANSLATOR) {
                    OcrTranslatorScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.OCR_RESULT_DETAIL) {
                    OcrResultDetailScreen(onBack = { navController.popBackStack() })
                }

                // ── QR Scanner (legacy) ──────────────────────────────────────
                composable(Routes.QR_SCANNER) {
                    QrScannerScreen(onBack = { navController.popBackStack() })
                }

                // ── NEW QR & Barcode Scanner ─────────────────────────────────
                composable(Routes.QR_SCANNER_V2) {
                    QrScannerScreenV2(onBack = { navController.popBackStack() })
                }

                // ── Translator (legacy) ──────────────────────────────────────
                composable(Routes.TRANSLATOR) {
                    TranslatorScreen(onBack = { navController.popBackStack() })
                }

                // ── NEW Smart Text Translator ────────────────────────────────
                composable(Routes.TRANSLATOR_V2) {
                    SmartTextTranslatorScreen(onBack = { navController.popBackStack() })
                }

                // ── Voice Translator (legacy) ────────────────────────────────
                composable(Routes.VOICE_TRANSLATOR) {
                    VoiceTranslatorScreen(onBack = { navController.popBackStack() })
                }

                // ── NEW Voice Conversation Translator ────────────────────────
                composable(Routes.VOICE_TRANSLATOR_V2) {
                    VoiceTranslatorScreenV2(onBack = { navController.popBackStack() })
                }

                // ── Object Detector (legacy) ─────────────────────────────────
                composable(Routes.OBJECT_DETECTOR) {
                    ObjectDetectorScreen(onBack = { navController.popBackStack() })
                }

                // ── NEW Object Detector ──────────────────────────────────────
                composable(Routes.OBJECT_DETECTOR_V2) {
                    ObjectDetectorScreenV2(
                        onBack     = { navController.popBackStack() },
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }
                composable(Routes.DETECTION_HISTORY) {
                    DetectionHistoryScreen(onBack = { navController.popBackStack() })
                }

                // ── Student Helper (legacy) ──────────────────────────────────
                composable(Routes.STUDENT_HELPER) {
                    StudentHelperScreen(onBack = { navController.popBackStack() })
                }

                // ── NEW AI Student Helper ────────────────────────────────────
                composable(Routes.STUDENT_HELPER_V2) {
                    StudentHelperScreenV2(onBack = { navController.popBackStack() })
                }

                // ── Medicine Scanner ─────────────────────────────────────────
                composable(Routes.MEDICINE_SCANNER) {
                    PremiumMedicineScannerScreen(onBack = { navController.popBackStack() })
                }

                // ── Waste Classifier (legacy) ────────────────────────────────
                composable(Routes.WASTE_CLASSIFIER) {
                    WasteClassifierScreen(onBack = { navController.popBackStack() })
                }

                // ── NEW AI Waste Classifier ──────────────────────────────────
                composable(Routes.WASTE_CLASSIFIER_V2) {
                    WasteClassifierScreenV2(onBack = { navController.popBackStack() })
                }

                // ── Document Scanner ─────────────────────────────────────────
                composable(Routes.DOCUMENT_SCANNER) {
                    DocumentScannerScreen(onBack = { navController.popBackStack() })
                }

                // ── History ──────────────────────────────────────────────────
                composable(Routes.HISTORY) {
                    HistoryScreen(onBack = { navController.popBackStack() })
                }

                // ── Settings ─────────────────────────────────────────────────
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onBack        = { navController.popBackStack() },
                        isDarkTheme   = isDarkTheme,
                        onThemeToggle = onThemeToggle,
                        onNavigate    = { route -> navController.navigate(route) }
                    )
                }

                // ── Privacy Policy ───────────────────────────────────────────
                composable(Routes.PRIVACY_POLICY) {
                    PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                }

                // ── Terms & Conditions ───────────────────────────────────────
                composable(Routes.TERMS_CONDITIONS) {
                    TermsConditionsScreen(onBack = { navController.popBackStack() })
                }

                // ── Profile ──────────────────────────────────────────────────
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        onBack = { navController.popBackStack() },
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.logout {
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.HOME) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // ── About ────────────────────────────────────────────────────
                composable(Routes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }

                // ── Accessibility Settings ──────────────────────────────────
                composable(Routes.ACCESSIBILITY) {
                    AccessibilitySettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // Global Floating AI Assistant draggable overlay
        GlobalFloatingAiAssistant(
            navController = navController,
            modifier      = Modifier.fillMaxSize()
        )
    }
}
