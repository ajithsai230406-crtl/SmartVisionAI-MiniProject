package com.smartvision.ai.data.repository

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepository @Inject constructor() {
    private val languageIdentifier = LanguageIdentification.getClient()

    suspend fun translate(text: String, targetLabel: String): Result<String> = runCatching {
        require(text.isNotBlank()) { "Enter text to translate." }
        val source = languageIdentifier.identifyLanguage(text).await().takeIf { it != "und" } ?: TranslateLanguage.ENGLISH
        val target = when (targetLabel) {
            "Hindi" -> TranslateLanguage.HINDI
            "Telugu" -> TranslateLanguage.TELUGU
            "Tamil" -> TranslateLanguage.TAMIL
            "French" -> TranslateLanguage.FRENCH
            "Spanish" -> TranslateLanguage.SPANISH
            "German" -> TranslateLanguage.GERMAN
            "Japanese" -> TranslateLanguage.JAPANESE
            else -> TranslateLanguage.ENGLISH
        }
        if (source == target) return@runCatching text
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val translator = Translation.getClient(options)
        val result = com.smartvision.ai.util.retryWithDelay(retries = 3) {
            translator.downloadModelIfNeeded().await()
            translator.translate(text).await()
        }
        translator.close()
        result
    }
}
