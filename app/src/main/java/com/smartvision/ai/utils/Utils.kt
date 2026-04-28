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

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    init {
        tts = TextToSpeech(context) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) tts?.language = Locale.ENGLISH
        }
    }

    /**
     * Speaks [text] in [languageCode] (BCP-47, e.g. "en", "hi", "fr").
     * Returns after speech finishes (suspends).
     */
    suspend fun speak(text: String, languageCode: String = "en"): Boolean {
        if (!isReady || tts == null) return false
        val locale = Locale.forLanguageTag(languageCode)
        val result = tts!!.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts!!.language = Locale.ENGLISH // fallback
        }
        val id = UUID.randomUUID().toString()
        return suspendCancellableCoroutine { cont ->
            tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?)  { _speaking.value = true  }
                override fun onDone(utteranceId: String?)   { _speaking.value = false; cont.resume(true) }
                override fun onError(utteranceId: String?)  { _speaking.value = false; cont.resume(false) }
            })
            tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        }
    }

    /** Immediately stops any ongoing speech. */
    fun stop() {
        tts?.stop()
        _speaking.value = false
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EXTENSIONS
// ─────────────────────────────────────────────────────────────────────────────

/** Formats confidence float (0.0–1.0) as a percentage string, e.g. "87%" */
fun Float.toConfidenceString(): String = "${(this * 100).toInt()}%"

/** Returns a human-readable label for a QR type */
fun com.smartvision.ai.domain.models.QrType.displayLabel(): String = when (this) {
    com.smartvision.ai.domain.models.QrType.URL     -> "Web Link"
    com.smartvision.ai.domain.models.QrType.TEXT    -> "Plain Text"
    com.smartvision.ai.domain.models.QrType.EMAIL   -> "Email Address"
    com.smartvision.ai.domain.models.QrType.PHONE   -> "Phone Number"
    com.smartvision.ai.domain.models.QrType.SMS     -> "SMS Message"
    com.smartvision.ai.domain.models.QrType.WIFI    -> "Wi-Fi Network"
    com.smartvision.ai.domain.models.QrType.CONTACT -> "Contact Card"
    com.smartvision.ai.domain.models.QrType.OTHER   -> "Other"
}

/** Truncates text with ellipsis for history summaries */
fun String.toHistorySummary(maxLen: Int = 80): String =
    if (length <= maxLen) this else take(maxLen - 1) + "…"

/** Returns true if a URL string is valid */
fun String.isValidUrl(): Boolean =
    startsWith("http://") || startsWith("https://")
