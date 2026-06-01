package com.example.smartvisionai.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class SpeechResult {
    data class Partial(val text: String) : SpeechResult()
    data class Final(val text: String)   : SpeechResult()
    data class Error(val code: Int)      : SpeechResult()
    object Started                       : SpeechResult()
    object Stopped                       : SpeechResult()
}

@Singleton
class VoiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isTtsReady = true
            }
        }
    }

    // ── Speech-to-Text ────────────────────────────────────────────────────────
    fun startListening(language: String = "en-US"): Flow<SpeechResult> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(SpeechResult.Error(-1))
            close()
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = trySend(SpeechResult.Started).let {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() = trySend(SpeechResult.Stopped).let {}

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                trySend(SpeechResult.Partial(text))
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                trySend(SpeechResult.Final(text))
                close()
            }

            override fun onError(error: Int) {
                trySend(SpeechResult.Error(error))
                close()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)

        awaitClose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }

    // ── Text-to-Speech ────────────────────────────────────────────────────────
    fun speak(text: String, utteranceId: String = "smart_vision_tts") {
        if (!isTtsReady) return
        tts?.speak(
            text.take(500),          // cap length for smooth playback
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )
    }

    fun stopSpeaking() = tts?.stop()

    fun setLanguage(locale: Locale) {
        tts?.language = locale
    }

    fun setOnDoneListener(onDone: () -> Unit) {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = onDone()
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {}
        })
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
