package com.smartvision.ai.compose.navigation

// ─── All application routes ───────────────────────────────────────────────────
object Routes {
    const val SPLASH          = "splash"
    const val ONBOARDING      = "onboarding"
    const val LOGIN           = "login"
    const val HOME            = "home"
    const val AI_ASSISTANT    = "ai_assistant"
    const val OCR_SCANNER     = "ocr_scanner"
    const val OCR_RESULT      = "ocr_result/{extractedText}"
    const val OCR_TRANSLATOR  = "ocr_translator"
    const val OCR_RESULT_DETAIL = "ocr_result_detail"
    const val QR_SCANNER      = "qr_scanner"
    const val QR_SCANNER_V2   = "qr_scanner_v2"
    const val DOCUMENT_SCANNER = "document_scanner_v2"
    const val TRANSLATOR      = "translator"
    const val TRANSLATOR_V2   = "translator_v2"
    const val VOICE_TRANSLATOR= "voice_translator"
    const val VOICE_TRANSLATOR_V2 = "voice_translator_v2"
    const val OBJECT_DETECTOR = "object_detector"
    const val OBJECT_DETECTOR_V2 = "object_detector_v2"
    const val DETECTION_HISTORY  = "detection_history"
    const val STUDENT_HELPER  = "student_helper"
    const val STUDENT_HELPER_V2= "student_helper_v2"
    const val MEDICINE_SCANNER= "medicine_scanner"
    const val WASTE_CLASSIFIER= "waste_classifier"
    const val WASTE_CLASSIFIER_V2 = "waste_classifier_v2"
    const val HISTORY         = "history"
    const val SETTINGS        = "settings"
    const val PROFILE         = "profile"
    const val ABOUT           = "about"
    const val PRIVACY_POLICY   = "privacy_policy"
    const val TERMS_CONDITIONS = "terms_conditions"
    const val ACCESSIBILITY    = "accessibility"

    fun ocrResult(text: String) = "ocr_result/$text"
}

// ─── Bottom Navigation Items — 4 tabs matching reference ─────────────────────
sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: String
) {
    object Home     : BottomNavItem(Routes.HOME,         "Home",     "🏠")
    object History  : BottomNavItem(Routes.HISTORY,      "History",  "📜")
    object AI       : BottomNavItem(Routes.AI_ASSISTANT, "AI",       "🤖")
    object Settings : BottomNavItem(Routes.SETTINGS,     "Settings", "⚙️")
}
