package com.smartvision.ai.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

// ─────────────────────────────────────────────────────────────────────────────
// TTS MANAGER — singleton wrapper around Android TextToSpeech
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            isReady = (status == TextToSpeech.SUCCESS)
            tts?.language = Locale.getDefault()
        }
    }

    fun speak(text: String, locale: Locale = Locale.getDefault()) {
        if (!isReady || text.isBlank()) return
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stop()     { tts?.stop() }
    fun isSpeaking() = tts?.isSpeaking == true

    /** Suspends until speech completes or coroutine is cancelled */
    suspend fun speakAndWait(text: String, locale: Locale = Locale.getDefault()) =
        suspendCancellableCoroutine { cont ->
            if (!isReady || text.isBlank()) { cont.resume(Unit); return@suspendCancellableCoroutine }
            val id = UUID.randomUUID().toString()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?)  {}
                override fun onDone(utteranceId: String?)   { if (utteranceId == id) cont.resume(Unit) }
                override fun onError(utteranceId: String?)  { if (utteranceId == id) cont.resume(Unit) }
            })
            tts?.language = locale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            cont.invokeOnCancellation { tts?.stop() }
        }

    fun setLocale(locale: Locale) {
        if (isReady) tts?.language = locale
    }

    fun shutdown() { tts?.shutdown(); tts = null; isReady = false }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOCALE HELPER
// ─────────────────────────────────────────────────────────────────────────────

fun langCodeToLocale(code: String): Locale = when (code) {
    "hi" -> Locale("hi", "IN")
    "te" -> Locale("te", "IN")
    "ta" -> Locale("ta", "IN")
    "bn" -> Locale("bn", "IN")
    "mr" -> Locale("mr", "IN")
    "gu" -> Locale("gu", "IN")
    "ur" -> Locale("ur", "PK")
    "fr" -> Locale.FRENCH
    "de" -> Locale.GERMAN
    "ja" -> Locale.JAPANESE
    "zh" -> Locale.CHINESE
    "ko" -> Locale.KOREAN
    "es" -> Locale("es", "ES")
    "ar" -> Locale("ar")
    "pt" -> Locale("pt", "PT")
    "ru" -> Locale("ru", "RU")
    "it" -> Locale.ITALIAN
    else -> Locale.ENGLISH
}

// ─────────────────────────────────────────────────────────────────────────────
// DATE HELPERS (duplicate-safe, prefer util/Extensions.kt)
// ─────────────────────────────────────────────────────────────────────────────

fun formatTimestamp(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}
