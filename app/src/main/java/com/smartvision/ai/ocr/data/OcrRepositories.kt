package com.smartvision.ai.ocr.data

import android.content.Context
import android.graphics.Bitmap
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
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// ─── OCR Script Types ─────────────────────────────────────────────────────────
enum class OcrScript {
    LATIN,       // English, French, German, Spanish, Portuguese, Italian
    DEVANAGARI,  // Hindi, Sanskrit, Marathi, Nepali
    CHINESE,     // Simplified + Traditional Chinese
    JAPANESE,    // Hiragana, Katakana, Kanji
    KOREAN;      // Hangul

    companion object {
        /** Derive script from BCP-47 language code */
        fun fromLanguageCode(code: String): OcrScript = when (code) {
            "hi", "mr", "ne", "sa" -> DEVANAGARI
            "zh"                    -> CHINESE
            "ja"                    -> JAPANESE
            "ko"                    -> KOREAN
            else                    -> LATIN
        }
    }
}

// ─── Multi-Script OCR Recognition Repository ─────────────────────────────────
@Singleton
class OcrRecognitionRepository @Inject constructor(
    private val preprocessor: OcrImagePreprocessor
) {
    // Lazy-loaded recognizers — only initialised on first use for each script
    private val latinRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private val devanagariRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    }
    private val chineseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }
    private val japaneseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }
    private val koreanRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }

    private fun recognizerFor(script: OcrScript): TextRecognizer = when (script) {
        OcrScript.DEVANAGARI -> devanagariRecognizer
        OcrScript.CHINESE    -> chineseRecognizer
        OcrScript.JAPANESE   -> japaneseRecognizer
        OcrScript.KOREAN     -> koreanRecognizer
        OcrScript.LATIN      -> latinRecognizer
    }

    // ── Live camera frame (raw Image from CameraX) ───────────────────────────
    /**
     * Recognizes text from a live camera [Image].
     * Uses the Latin recognizer by default (fast path for live scanning).
     * For best accuracy, use [recognizeFromBitmap] with preprocessing for captured frames.
     */
    suspend fun recognizeFromImage(
        image: Image,
        rotation: Int,
        script: OcrScript = OcrScript.LATIN
    ): Result<Text> = withContext(Dispatchers.Default) {
        runCatching {
            val input = InputImage.fromMediaImage(image, rotation)
            recognizerFor(script).process(input).await()
        }
    }

    // ── Bitmap (from gallery or captured frame) ───────────────────────────────
    /**
     * Recognizes text from a [Bitmap], applying the full preprocessing pipeline first.
     * Runs dual-pass (Latin + [secondaryScript]) and merges the result with more text.
     */
    suspend fun recognizeFromBitmap(
        bitmap: Bitmap,
        script: OcrScript = OcrScript.LATIN,
        preprocess: Boolean = true,
        enableDualPass: Boolean = false
    ): Result<Text> = withContext(Dispatchers.Default) {
        runCatching {
            val processed = if (preprocess) preprocessor.preprocess(bitmap) else bitmap
            val input = InputImage.fromBitmap(processed, 0)
            val primary = recognizerFor(script).process(input).await()

            // Dual-pass: also run Latin recognizer for mixed-script documents
            if (enableDualPass && script != OcrScript.LATIN) {
                val latinInput = InputImage.fromBitmap(processed, 0)
                val latinResult = latinRecognizer.process(latinInput).await()
                // Return whichever found more text
                if (latinResult.text.length > primary.text.length) latinResult else primary
            } else {
                primary
            }
        }
    }

    // ── Gallery URI ───────────────────────────────────────────────────────────
    /**
     * Recognizes text from a gallery [Uri].
     * Decodes to bitmap first to allow preprocessing.
     */
    suspend fun recognizeFromUri(
        context: Context,
        uri: Uri,
        script: OcrScript = OcrScript.LATIN
    ): Result<Text> = withContext(Dispatchers.Default) {
        runCatching {
            val bitmap = android.graphics.BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(uri)
            ) ?: throw IllegalArgumentException("Could not decode image from URI")
            recognizeFromBitmap(bitmap, script, preprocess = true, enableDualPass = true).getOrThrow()
        }
    }

    /**
     * Returns image dimensions from a URI without fully decoding it.
     */
    fun readDimensions(context: Context, uri: Uri): Pair<Int, Int> {
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        return try {
            context.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, opts)
            }
            val w = if (opts.outWidth > 0) opts.outWidth else 1080
            val h = if (opts.outHeight > 0) opts.outHeight else 1920
            Pair(w, h)
        } catch (e: Exception) {
            Pair(1080, 1920)
        }
    }
}

// ─── Language Detection Repository ───────────────────────────────────────────
@Singleton
class LanguageDetectionRepository @Inject constructor() {
    private val identifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder().setConfidenceThreshold(0.35f).build()
    )

    /**
     * Returns BCP-47 language code (e.g. "en", "hi", "te") or "und" if undetermined.
     */
    suspend fun identifyLanguage(text: String): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            identifier.identifyLanguage(text).await()
        }
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
        "mr" -> "Marathi"
        "bn" -> "Bengali"
        "pa" -> "Punjabi"
        "und" -> "Unknown"
        else -> code.uppercase()
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
    ): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
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
    }

    /** Returns list of target language display names (excluding source) */
    fun availableTargetLanguages(sourceName: String = ""): List<String> =
        SUPPORTED_LANGUAGES.keys.filter { it != sourceName }
}
