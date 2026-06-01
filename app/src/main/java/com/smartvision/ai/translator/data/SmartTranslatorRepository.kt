package com.smartvision.ai.translator.data

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.smartvision.ai.BuildConfig
import com.smartvision.ai.data.db.GeminiCacheDao
import com.smartvision.ai.data.db.GeminiCacheEntity
import com.smartvision.ai.translator.domain.*
import com.smartvision.ai.util.NetworkMonitor
import com.smartvision.ai.util.retryWithDelay
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartTranslatorRepository @Inject constructor(
    private val cacheDao: GeminiCacheDao,
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor
) {

    private val langId     = LanguageIdentification.getClient()
    private val geminiModel by lazy {
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    // ── Local Phrase Database Structure ─────────────────────────────────────────
    private data class OfflineSemanticEntry(
        val phrase: String,
        val meaningEnglish: String,
        val meaningOriginal: String,
        val contextExplanation: String,
        val usageExplanation: String,
        val pronunciation: String? = null
    )

    private val offlineSemanticsMap: Map<String, OfflineSemanticEntry> by lazy {
        runCatching {
            val jsonString = context.assets.open("offline_semantics.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val map = mutableMapOf<String, OfflineSemanticEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val phrase = obj.getString("phrase").trim().lowercase()
                map[phrase] = OfflineSemanticEntry(
                    phrase = phrase,
                    meaningEnglish = obj.getString("meaningEnglish"),
                    meaningOriginal = obj.getString("meaningOriginal"),
                    contextExplanation = obj.getString("contextExplanation"),
                    usageExplanation = obj.getString("usageExplanation"),
                    pronunciation = obj.optString("pronunciation").takeIf { it.isNotBlank() && it != "null" }
                )
            }
            map
        }.getOrElse {
            emptyMap()
        }
    }

    // ── Core translation (ML Kit on-device) ────────────────────────────────────
    suspend fun translate(
        text:       String,
        sourceLang: Language,    // "auto" if source.code == "auto"
        targetLang: Language
    ): Result<TranslationResult> = runCatching {
        require(text.isNotBlank()) { "Please enter text to translate." }

        // 1. Detect source language
        val detectedCode = if (sourceLang.code == "auto") {
            langId.identifyLanguage(text).await().takeIf { it != "und" } ?: "en"
        } else sourceLang.code

        val detectedLang = languageByCode(detectedCode)

        // 2. Build ML Kit translator
        val srcMlKit = mlKitCode(detectedCode)
        val tgtMlKit = mlKitCode(targetLang.code)

        // If same language, return original
        if (srcMlKit == tgtMlKit) {
            val isOnline = networkMonitor.isOnline()
            val semanticMeaning = runCatching {
                generateSemanticMeaning(text, text, detectedLang, targetLang, isOnline)
            }.getOrNull()

            return@runCatching TranslationResult(
                originalText    = text,
                translatedText  = text,
                detectedLang    = detectedLang,
                targetLang      = targetLang,
                pronunciation   = semanticMeaning?.pronunciation,
                nativeMeaning   = semanticMeaning?.meaningOriginal,
                semanticMeaning = semanticMeaning,
                isOfflineMode   = !isOnline
            )
        }

        val options    = TranslatorOptions.Builder().setSourceLanguage(srcMlKit).setTargetLanguage(tgtMlKit).build()
        val translator = Translation.getClient(options)
        
        // Wrap model downloading and translation in retries
        val translated = retryWithDelay(retries = 3) {
            translator.downloadModelIfNeeded().await()
            translator.translate(text).await()
        }
        translator.close()

        // 3. Generate Semantic Meaning and Explanation automatically
        val isOnline = networkMonitor.isOnline()
        val semanticMeaning = runCatching {
            generateSemanticMeaning(text, translated, detectedLang, targetLang, isOnline)
        }.getOrNull()

        TranslationResult(
            originalText    = text,
            translatedText  = translated,
            detectedLang    = detectedLang,
            targetLang      = targetLang,
            pronunciation   = semanticMeaning?.pronunciation,
            nativeMeaning   = semanticMeaning?.meaningOriginal,
            semanticMeaning = semanticMeaning,
            isOfflineMode   = !isOnline
        )
    }

    // ── Language identification only ───────────────────────────────────────────
    suspend fun detectLanguage(text: String): Result<Language> = runCatching {
        val code = langId.identifyLanguage(text).await().takeIf { it != "und" } ?: "en"
        languageByCode(code)
    }

    // ── Semantic Meaning Generation ─────────────────────────────────
    suspend fun generateSemanticMeaning(
        original: String,
        translated: String,
        srcLang: Language,
        tgtLang: Language,
        isOnline: Boolean = networkMonitor.isOnline()
    ): SemanticMeaning = withContext(Dispatchers.IO) {
        val normalizedQuery = original.trim().lowercase()
        
        if (isOnline) {
            val prompt = buildMeaningPrompt(original, translated, srcLang, tgtLang)
            val promptHash = prompt.hashCode()

            // 1. Check local offline Room cache
            runCatching {
                val cachedResult = cacheDao.getCache(promptHash)
                if (cachedResult != null) {
                    return@withContext parseSemanticMeaning(cachedResult.response, original, srcLang)
                }
            }

            // 2. Fetch remote via Gemini with exponential retries
            runCatching {
                val response = retryWithDelay(retries = 3) {
                    geminiModel.generateContent(content { text(prompt) })
                }
                val responseText = response.text ?: throw Exception("Empty AI response")
                val meaning = parseSemanticMeaning(responseText, original, srcLang)

                // Cache successfully resolved meanings
                cacheDao.insertCache(GeminiCacheEntity(promptHash, prompt, responseText))
                meaning
            }.getOrElse {
                getFallbackMeaning(original, srcLang)
            }
        } else {
            // OFFLINE MODE: Local Semantic Engine
            // 1. Exact or substring match in offline JSON phrase database
            val dictMatch = offlineSemanticsMap[normalizedQuery] ?: offlineSemanticsMap.values.find {
                normalizedQuery.contains(it.phrase) || it.phrase.contains(normalizedQuery)
            }

            if (dictMatch != null) {
                // If it is in the exact dictionary, translate the native meaning into srcLang offline if it's not English
                val nativeMeaning = if (srcLang.code == "en") {
                    dictMatch.meaningEnglish
                } else if (srcLang.code == "hi") {
                    dictMatch.meaningOriginal // Pre-loaded Hindi translation
                } else {
                    translateTextOffline(dictMatch.meaningEnglish, "en", srcLang.code)
                }

                SemanticMeaning(
                    meaningEnglish     = dictMatch.meaningEnglish,
                    meaningOriginal    = nativeMeaning,
                    contextExplanation = dictMatch.contextExplanation,
                    usageExplanation   = dictMatch.usageExplanation,
                    pronunciation      = dictMatch.pronunciation,
                    isOfflineMode      = true
                )
            } else {
                // 2. Phrase unavailable: run Local Heuristics NLP Engine!
                getFallbackMeaning(original, srcLang)
            }
        }
    }

    private fun buildMeaningPrompt(
        original: String,
        translated: String,
        srcLang: Language,
        tgtLang: Language
    ) = """
You are a linguistic expert. Provide a concise, semantic explanation of the phrase/word: "$original" (which translates to "$translated" in ${tgtLang.displayName}).

Return ONLY a valid JSON object (no markdown, no backticks, no comments). The JSON structure MUST be exactly:
{
  "meaningEnglish": "Concise semantic meaning of '$original' written strictly in English.",
  "meaningOriginal": "Concise semantic meaning of '$original' written strictly in the original language: ${srcLang.displayName} (${srcLang.nativeName}). Do not include any English or translate it.",
  "contextExplanation": "Contextual, cultural background or nuances of '$original'. Keep it brief.",
  "usageExplanation": "Explanation of how/when to use it, followed by 1 short usage example.",
  "pronunciation": "Romanized pronunciation / transliteration of the original text '$original' (or null if the language uses standard Latin script)."
}
""".trimIndent()

    private fun parseSemanticMeaning(
        rawJson: String,
        originalText: String,
        detectedLang: Language
    ): SemanticMeaning {
        val cleaned = rawJson.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(cleaned)
        
        val meaningOriginal = json.optString("meaningOriginal", "").takeIf { it.isNotBlank() } ?: "Meaning not available."
        
        return SemanticMeaning(
            meaningEnglish     = json.optString("meaningEnglish", "Meaning not available."),
            meaningOriginal    = meaningOriginal,
            contextExplanation = json.optString("contextExplanation", "Context not available."),
            usageExplanation   = json.optString("usageExplanation", "Usage instructions not available."),
            pronunciation      = json.optString("pronunciation", "").takeIf { it.isNotBlank() && it != "null" },
            isOfflineMode      = false
        )
    }

    // ── Local Heuristics NLP Engine ───────────────────────────────────────────
    private enum class SemanticCategory {
        GREETINGS, MEDICAL, EDUCATIONAL, TRAVEL, QUESTIONS, EMOTIONAL, CONVERSATIONAL
    }

    private fun detectCategory(text: String): SemanticCategory {
        val lower = text.lowercase().trim()
        val greetingKeywords = listOf("hi", "hello", "hey", "greet", "morning", "night", "evening", "welcome", "bye", "goodbye", "namaste", "hola", "bonjour")
        val medicalKeywords = listOf("hospital", "doctor", "medicine", "pain", "sick", "ill", "pharmacy", "clinic", "health", "bleed", "wound", "fever", "cough", "nurse")
        val educationalKeywords = listOf("solve", "equation", "math", "learn", "study", "formula", "class", "school", "physics", "chemistry", "sum", "explain", "define", "theorem")
        val travelKeywords = listOf("where is", "how to go", "direction", "map", "bus", "train", "flight", "hotel", "station", "airport", "taxi", "ticket", "route")
        val questionKeywords = listOf("what", "why", "how", "who", "when", "which", "query", "whose", "whom")
        val emotionalKeywords = listOf("happy", "sad", "love", "angry", "scared", "fear", "excited", "sorry", "please", "glad", "hate", "worry", "joy")

        return when {
            greetingKeywords.any { lower.contains(it) } -> SemanticCategory.GREETINGS
            medicalKeywords.any { lower.contains(it) } -> SemanticCategory.MEDICAL
            educationalKeywords.any { lower.contains(it) } -> SemanticCategory.EDUCATIONAL
            travelKeywords.any { lower.contains(it) } -> SemanticCategory.TRAVEL
            questionKeywords.any { lower.contains(it) } -> SemanticCategory.QUESTIONS
            emotionalKeywords.any { lower.contains(it) } -> SemanticCategory.EMOTIONAL
            else -> SemanticCategory.CONVERSATIONAL
        }
    }

    private suspend fun getFallbackMeaning(original: String, srcLang: Language): SemanticMeaning {
        val category = detectCategory(original)
        
        val meaningEnglish = when (category) {
            SemanticCategory.GREETINGS -> "A friendly greeting or welcoming expression used for social interaction: \"$original\"."
            SemanticCategory.MEDICAL -> "A health-related or medical inquiry/expression referring to: \"$original\"."
            SemanticCategory.EDUCATIONAL -> "An educational, study-oriented, or scientific query regarding: \"$original\"."
            SemanticCategory.TRAVEL -> "A travel, location, or navigation-related inquiry asking about: \"$original\"."
            SemanticCategory.QUESTIONS -> "An interrogative inquiry seeking information or clarification on: \"$original\"."
            SemanticCategory.EMOTIONAL -> "An expressive phrase displaying human emotion or polite intent: \"$original\"."
            SemanticCategory.CONVERSATIONAL -> "A conversational or general communication expression: \"$original\"."
        }

        val context = when (category) {
            SemanticCategory.GREETINGS -> "Used in normal conversations to establish rapport, greet someone, or begin a warm dialogue."
            SemanticCategory.MEDICAL -> "Typically used in medical contexts, clinical consultations, or health emergencies to convey concerns."
            SemanticCategory.EDUCATIONAL -> "Used in academic, tutoring, or learning environments to solve equations or clarify concepts."
            SemanticCategory.TRAVEL -> "Used by travelers or commuters to seek directions, guide transit, or orient themselves."
            SemanticCategory.QUESTIONS -> "Used in speech when trying to gather details, query facts, or clarify conversational options."
            SemanticCategory.EMOTIONAL -> "Used in social interactions to express feelings, extend polite gestures, or convey empathy."
            SemanticCategory.CONVERSATIONAL -> "Used in daily conversations to communicate standard expressions, thoughts, or actions."
        }

        val usage = when (category) {
            SemanticCategory.GREETINGS -> "Say it when greeting someone. Example: \"$original, how are you?\""
            SemanticCategory.MEDICAL -> "Use when explaining a medical issue. Example: \"Excuse me, $original.\""
            SemanticCategory.EDUCATIONAL -> "Use when studying or doing coursework. Example: \"Can you explain $original?\""
            SemanticCategory.TRAVEL -> "Use when navigating a new place. Example: \"Pardon me, $original?\""
            SemanticCategory.QUESTIONS -> "Use to ask for information. Example: \"$original? Let me know.\""
            SemanticCategory.EMOTIONAL -> "Use to express feelings politely. Example: \"Please note: $original.\""
            SemanticCategory.CONVERSATIONAL -> "Use in general speech. Example: \"They said: $original.\""
        }

        // Dynamic pronunciation heuristic
        val pronunciation = if (srcLang.code != "en") {
            val matched = offlineSemanticsMap.values.find { it.phrase.contains(original.lowercase()) }
            matched?.pronunciation ?: "Pronunciation guide active in offline mode"
        } else null

        // Translate the English fallback explanation into detected source language offline
        val meaningOriginal = if (srcLang.code == "en") {
            meaningEnglish
        } else {
            translateTextOffline(meaningEnglish, "en", srcLang.code)
        }

        return SemanticMeaning(
            meaningEnglish     = meaningEnglish,
            meaningOriginal    = meaningOriginal,
            contextExplanation = context,
            usageExplanation   = usage,
            pronunciation      = pronunciation,
            isOfflineMode      = true
        )
    }

    private suspend fun translateTextOffline(text: String, sourceCode: String, targetCode: String): String = runCatching {
        val src = mlKitCode(sourceCode)
        val tgt = mlKitCode(targetCode)
        if (src == tgt) return@runCatching text

        val options = TranslatorOptions.Builder().setSourceLanguage(src).setTargetLanguage(tgt).build()
        val translator = Translation.getClient(options)
        translator.downloadModelIfNeeded().await()
        val result = translator.translate(text).await()
        translator.close()
        result
    }.getOrDefault(text)

    // ── ML Kit language code mapper ────────────────────────────────────────────
    private fun mlKitCode(code: String): String = when (code) {
        "en" -> TranslateLanguage.ENGLISH
        "hi" -> TranslateLanguage.HINDI
        "te" -> TranslateLanguage.TELUGU
        "ta" -> TranslateLanguage.TAMIL
        "kn" -> TranslateLanguage.KANNADA
        "mr" -> TranslateLanguage.MARATHI
        "bn" -> TranslateLanguage.BENGALI
        "gu" -> TranslateLanguage.GUJARATI
        "fr" -> TranslateLanguage.FRENCH
        "de" -> TranslateLanguage.GERMAN
        "es" -> TranslateLanguage.SPANISH
        "it" -> TranslateLanguage.ITALIAN
        "pt" -> TranslateLanguage.PORTUGUESE
        "ru" -> TranslateLanguage.RUSSIAN
        "zh" -> TranslateLanguage.CHINESE
        "ja" -> TranslateLanguage.JAPANESE
        "ko" -> TranslateLanguage.KOREAN
        "ar" -> TranslateLanguage.ARABIC
        "tr" -> TranslateLanguage.TURKISH
        "nl" -> TranslateLanguage.DUTCH
        "pl" -> TranslateLanguage.POLISH
        "sv" -> TranslateLanguage.SWEDISH
        "id" -> TranslateLanguage.INDONESIAN
        "th" -> TranslateLanguage.THAI
        "vi" -> TranslateLanguage.VIETNAMESE
        else -> TranslateLanguage.ENGLISH
    }
}
