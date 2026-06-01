package com.smartvision.ai.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.smartvision.ai.BuildConfig
import com.smartvision.ai.data.db.GeminiCacheDao
import com.smartvision.ai.data.db.GeminiCacheEntity
import com.smartvision.ai.util.retryWithDelay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiAssistantRepository @Inject constructor(
    private val cacheDao: GeminiCacheDao
) {
    private val geminiModel by lazy {
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    /**
     * Context-aware reply using live Gemini Generative model with local offline Room cache.
     * Incorporates current screen context to provide targeted explanations or eco-friendly tips.
     */
    suspend fun replyTo(prompt: String, contextRoute: String? = null): String =
        replyToWithHistory(prompt, "", contextRoute)

    /**
     * Rebuilt context-aware conversational AI assistant query supporting history memory.
     */
    suspend fun replyToWithHistory(
        prompt: String,
        historyTranscript: String,
        contextRoute: String? = null
    ): String = withContext(Dispatchers.IO) {
        runCatching {
            val systemPrompt = """
                You are a helpful, professional, and highly intelligent AI Assistant for the "Smart Vision AI" mobile app.
                Smart Vision AI is a dark-neon premium Android app designed with modules such as:
                1. Object Detector (Cyan badges, real-time object overlays)
                2. OCR Translator (Google Lens style viewfinder selective scanner)
                3. Student Helper (Google Lens homework steps solver, tutor chats)
                4. Waste Classifier (Green badges, eco-friendly recycling classifications)
                5. Voice Translator (real-time conversations)
                6. Smart Text Translator (context translation with native explanations)
                7. QR Scanner (fast scan codes)
                8. Document Scanner (PDF export filters)
                9. Medicine Scanner (pill scanner guidelines)
                
                Current App Screen Route Context: ${contextRoute ?: "Home Dashboard"}
                
                Maintain conversational context. Analyze the previous conversation history provided below and continue the discussion naturally like a premium assistant (e.g. ChatGPT/Gemini). Focus on resolving user doubts, describing active screen module features, or giving practical advice.
                Keep responses concise, clear, and informative (2-4 clear sentences).
            """.trimIndent()

            val finalPrompt = """
                $systemPrompt
                
                Conversation History:
                $historyTranscript
                
                User: $prompt
                Assistant:
            """.trimIndent()
            
            val promptHash = finalPrompt.hashCode()

            // ── Check local offline cache ──
            val cachedResult = cacheDao.getCache(promptHash)
            if (cachedResult != null) {
                return@runCatching cachedResult.response
            }

            // ── Remote call with retries ──
            val response = retryWithDelay(retries = 3) {
                geminiModel.generateContent(content {
                    text(finalPrompt)
                })
            }
            
            val responseText = response.text?.trim() ?: throw Exception("Empty response")
            cacheDao.insertCache(GeminiCacheEntity(promptHash, finalPrompt, responseText))
            responseText
        }.getOrElse {
            // Safe offline fallback
            val lower = prompt.lowercase()
            when {
                "ocr" in lower || "scan" in lower -> "Use the OCR Scanner to crop, translate, and extract native-language meanings from printed texts!"
                "medicine" in lower -> "Point the Medicine Scanner at pill bottles to read educational warning and dosage guides."
                "waste" in lower || "recycle" in lower -> "Open the Waste Classifier to identify plastic, glass, and paper recyclables."
                "translate" in lower -> "Text and Voice Translators support offline translation and native context explanations."
                "student" in lower || "math" in lower -> "Use the Student Helper module to crop any formulas and solve them step-by-step!"
                else -> "I can assist you with OCR translations, voice recording, recycling guidelines, or project viva details!"
            }
        }
    }

    /**
     * Context-aware streaming reply using live Gemini Generative model.
     * Incorporates current screen context to provide targeted explanations, viva answers, coding help, or features.
     */
    fun replyToWithHistoryStream(
        prompt: String,
        historyTranscript: String,
        contextRoute: String? = null
    ): Flow<String> = flow {
        val systemPrompt = """
            You are a helpful, professional, and highly intelligent AI Assistant for the "Smart Vision AI" mobile app.
            Smart Vision AI is a dark-neon premium Android app designed with modules such as:
            1. Object Detector (Cyan badges, real-time object overlays)
            2. OCR Translator (Google Lens style viewfinder selective scanner)
            3. Student Helper (Google Lens homework steps solver, tutor chats)
            4. Waste Classifier (Green badges, eco-friendly recycling classifications)
            5. Voice Translator (real-time conversations)
            6. Smart Text Translator (context translation with native explanations)
            7. QR Scanner (fast scan codes)
            8. Document Scanner (PDF export filters)
            9. Medicine Scanner (pill scanner guidelines)
            
            Current App Screen Route Context: ${contextRoute ?: "Home Dashboard"}
            
            Maintain conversational context. Analyze the previous conversation history provided below and continue the discussion naturally like a premium assistant (e.g. ChatGPT/Gemini/Perplexity AI). 
            Generate unique, intelligent, and contextually rich answers dynamically. Answer any educational queries, solve academic questions, explain medicine warnings, or give programming code snippets and complexity analyses. 
            Do NOT repeat generic templates. Ensure responses are unique, tailored, and continue conversations naturally.
            Keep responses clear, comprehensive, yet relatively concise for a premium mobile chat interface.
        """.trimIndent()

        val finalPrompt = """
            $systemPrompt
            
            Conversation History:
            $historyTranscript
            
            User: $prompt
            Assistant:
        """.trimIndent()

        // Stream the content chunk-by-chunk using the official Gemini SDK stream method!
        geminiModel.generateContentStream(content {
            text(finalPrompt)
        }).collect { response ->
            response.text?.let { emit(it) }
        }
    }.catch { e ->
        // Safe offline fallback
        val lower = prompt.lowercase()
        val offlineReply = when {
            "ocr" in lower || "scan" in lower -> "Use the OCR Scanner to crop, translate, and extract native-language meanings from printed texts!"
            "medicine" in lower -> "Point the Medicine Scanner at pill bottles to read educational warning and dosage guides."
            "waste" in lower || "recycle" in lower -> "Open the Waste Classifier to identify plastic, glass, and paper recyclables."
            "translate" in lower -> "Text and Voice Translators support offline translation and native context explanations."
            "student" in lower || "math" in lower -> "Use the Student Helper module to crop any formulas and solve them step-by-step!"
            else -> "I can assist you with OCR translations, voice recording, recycling guidelines, or project viva details!"
        }
        emit(offlineReply)
    }.flowOn(Dispatchers.IO)
}
