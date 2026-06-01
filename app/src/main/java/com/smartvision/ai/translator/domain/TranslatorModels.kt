package com.smartvision.ai.translator.domain

// ─── Supported Languages ───────────────────────────────────────────────────────

data class Language(
    val code:        String,   // ML Kit TranslateLanguage code
    val displayName: String,
    val flag:        String,
    val nativeName:  String
)

val ALL_LANGUAGES = listOf(
    Language("en", "English",    "🇺🇸", "English"),
    Language("hi", "Hindi",      "🇮🇳", "हिन्दी"),
    Language("te", "Telugu",     "🇮🇳", "తెలుగు"),
    Language("ta", "Tamil",      "🇮🇳", "தமிழ்"),
    Language("kn", "Kannada",    "🇮🇳", "ಕನ್ನಡ"),
    Language("ml", "Malayalam",  "🇮🇳", "മലയാളം"),
    Language("mr", "Marathi",    "🇮🇳", "मराठी"),
    Language("bn", "Bengali",    "🇧🇩", "বাংলা"),
    Language("gu", "Gujarati",   "🇮🇳", "ગુજરાતી"),
    Language("pa", "Punjabi",    "🇮🇳", "ਪੰਜਾਬੀ"),
    Language("ur", "Urdu",       "🇵🇰", "اردو"),
    Language("fr", "French",     "🇫🇷", "Français"),
    Language("de", "German",     "🇩🇪", "Deutsch"),
    Language("es", "Spanish",    "🇪🇸", "Español"),
    Language("it", "Italian",    "🇮🇹", "Italiano"),
    Language("pt", "Portuguese", "🇧🇷", "Português"),
    Language("ru", "Russian",    "🇷🇺", "Русский"),
    Language("zh", "Chinese",    "🇨🇳", "中文"),
    Language("ja", "Japanese",   "🇯🇵", "日本語"),
    Language("ko", "Korean",     "🇰🇷", "한국어"),
    Language("ar", "Arabic",     "🇸🇦", "العربية"),
    Language("tr", "Turkish",    "🇹🇷", "Türkçe"),
    Language("nl", "Dutch",      "🇳🇱", "Nederlands"),
    Language("pl", "Polish",     "🇵🇱", "Polski"),
    Language("sv", "Swedish",    "🇸🇪", "Svenska"),
    Language("id", "Indonesian", "🇮🇩", "Bahasa Indonesia"),
    Language("th", "Thai",       "🇹🇭", "ภาษาไทย"),
    Language("vi", "Vietnamese", "🇻🇳", "Tiếng Việt"),
)

fun languageByCode(code: String): Language =
    ALL_LANGUAGES.find { it.code == code } ?: ALL_LANGUAGES.first()

data class SemanticMeaning(
    val meaningEnglish: String,
    val meaningOriginal: String,
    val contextExplanation: String,
    val usageExplanation: String,
    val pronunciation: String? = null,
    val isOfflineMode: Boolean = false
)

// ─── Translation Result ────────────────────────────────────────────────────────

data class TranslationResult(
    val originalText:    String,
    val translatedText:  String,
    val detectedLang:    Language,
    val targetLang:      Language,
    val pronunciation:   String? = null,
    val characterCount:  Int     = originalText.length,
    val nativeMeaning:   String? = null,
    val semanticMeaning: SemanticMeaning? = null,
    val isOfflineMode:   Boolean = false
)

// ─── Voice Conversation Entry ──────────────────────────────────────────────────

data class ConversationEntry(
    val id:             Long    = System.currentTimeMillis(),
    val originalText:   String,
    val translatedText: String,
    val sourceLang:     Language,
    val targetLang:     Language,
    val isUserSide:     Boolean = true,              // true = left mic, false = right mic
    val nativeMeaning:  String? = null,
    val semanticMeaning: SemanticMeaning? = null
)

// ─── UI States ────────────────────────────────────────────────────────────────

sealed class TranslatorState {
    object Idle      : TranslatorState()
    object Translating : TranslatorState()
    object Downloading : TranslatorState()           // downloading ML model
    data class Success(val result: TranslationResult) : TranslatorState()
    data class Error(val message: String)             : TranslatorState()
}

enum class VoiceListeningState {
    IDLE, LISTENING, PROCESSING
}
