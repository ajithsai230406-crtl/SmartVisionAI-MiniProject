package com.smartvision.ai.ocr.data

import android.content.Context
import android.media.Image
import android.net.Uri
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ─── OCR Recognition Repository ──────────────────────────────────────────────
@Singleton
class OcrRecognitionRepository @Inject constructor() {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Recognizes text from live camera Image. Returns full structured Text result. */
    suspend fun recognizeFromImage(image: Image, rotation: Int): Result<Text> = runCatching {
        val input = InputImage.fromMediaImage(image, rotation)
        recognizer.process(input).await()
    }

    /** Recognizes text from a gallery URI. */
    suspend fun recognizeFromUri(context: Context, uri: Uri): Result<Text> = runCatching {
        val input = InputImage.fromFilePath(context, uri)
        recognizer.process(input).await()
    }
}

// ─── Language Detection Repository ───────────────────────────────────────────
@Singleton
class LanguageDetectionRepository @Inject constructor() {
    private val identifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder().setConfidenceThreshold(0.4f).build()
    )

    /**
     * Returns BCP-47 language code (e.g. "en", "hi", "te") or "und" if undetermined.
     */
    suspend fun identifyLanguage(text: String): Result<String> = runCatching {
        identifier.identifyLanguage(text).await()
    }

    /** Human-readable name for a BCP-47 code */
    fun languageDisplayName(code: String): String = when (code) {
        "en" -> "English"
        "hi" -> "Hindi"
        "te" -> "Telugu"
        "ta" -> "Tamil"
        "fr" -> "French"
        "es" -> "Spanish"
        "de" -> "German"
        "ja" -> "Japanese"
        "zh" -> "Chinese"
        "ar" -> "Arabic"
        "pt" -> "Portuguese"
        "ru" -> "Russian"
        "ko" -> "Korean"
        "it" -> "Italian"
        "und" -> "Unknown"
        else  -> code.uppercase()
    }
}

// ─── ML Kit Translation Repository ───────────────────────────────────────────
@Singleton
class MlKitTranslationRepository @Inject constructor() {

    companion object {
        /** All supported target language display names → ML Kit codes */
        val SUPPORTED_LANGUAGES = linkedMapOf(
            "Hindi"      to TranslateLanguage.HINDI,
            "Telugu"     to TranslateLanguage.TELUGU,
            "Tamil"      to TranslateLanguage.TAMIL,
            "French"     to TranslateLanguage.FRENCH,
            "Spanish"    to TranslateLanguage.SPANISH,
            "German"     to TranslateLanguage.GERMAN,
            "Japanese"   to TranslateLanguage.JAPANESE,
            "English"    to TranslateLanguage.ENGLISH,
            "Chinese"    to TranslateLanguage.CHINESE,
            "Arabic"     to TranslateLanguage.ARABIC,
            "Portuguese" to TranslateLanguage.PORTUGUESE,
            "Russian"    to TranslateLanguage.RUSSIAN,
            "Korean"     to TranslateLanguage.KOREAN,
            "Italian"    to TranslateLanguage.ITALIAN
        )
    }

    /**
     * Translates [text] from [sourceLangCode] (BCP-47) to [targetLabel] (display name).
     * Downloads the model if not cached (offline after first use).
     */
    suspend fun translate(
        text: String,
        sourceLangCode: String,
        targetLabel: String
    ): Result<String> = runCatching {
        require(text.isNotBlank()) { "Text cannot be empty." }

        val sourceMlKit = sourceLangCode.takeIf { it != "und" }
            ?: TranslateLanguage.ENGLISH
        val targetMlKit = SUPPORTED_LANGUAGES[targetLabel]
            ?: TranslateLanguage.ENGLISH

        if (sourceMlKit == targetMlKit) return@runCatching text

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceMlKit)
            .setTargetLanguage(targetMlKit)
            .build()

        val translator = Translation.getClient(options)
        try {
            translator.downloadModelIfNeeded().await()
            translator.translate(text).await()
        } finally {
            translator.close()
        }
    }

    /** Returns list of target language display names (excluding source) */
    fun availableTargetLanguages(sourceName: String = ""): List<String> =
        SUPPORTED_LANGUAGES.keys.filter { it != sourceName }
}
