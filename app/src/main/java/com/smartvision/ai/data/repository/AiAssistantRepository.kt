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
                You are a highly intelligent, premium AI assistant for the "Smart Vision AI" app.
                Smart Vision AI is a dark-neon premium Android app designed with these specialized modules:
                1. Object Detector (Cyan badges, real-time object overlays)
                2. OCR Translator (Google Lens style selective scanner)
                3. Student Helper (Google Lens homework solver, tutor chats, math/science formulas)
                4. Waste Classifier (Green badges, eco-friendly recycling classifications)
                5. Voice Translator (real-time conversational audio translation)
                6. Smart Text Translator (context translation with native explanations)
                7. QR Scanner (fast scan codes)
                8. Document Scanner (PDF export filters)
                9. Medicine Scanner (pill scanner warning and dosage guidelines)
                
                Current App Screen Route Context: ${contextRoute ?: "Home Dashboard"}
                
                Guidelines:
                - You can answer ANY educational, scientific, coding, mathematical, or general inquiry dynamically.
                - Maintain context based on the conversation history.
                - Never use template or hardcoded fallback responses. 
                - Be comprehensive, professional, and extremely helpful.
                - Keep responses clean, descriptive, and direct (between 2 to 5 sentences depending on complexity).
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
        }.getOrElse { e ->
            // Smart dynamic offline fallback that feels conversational and respects the query
            val lower = prompt.lowercase()
            val queryTopic = prompt.split("\\s+".toRegex())
                .map { it.replace(Regex("[^a-zA-Z]"), "") }
                .firstOrNull { it.length > 4 } ?: "topics"
            
            when {
                "ocr" in lower || "scan" in lower || "text" in lower -> 
                    "I am currently operating in offline mode, but you can use our premium OCR Scanner module to crop, translate, and extract native-language meanings from printed texts directly!"
                "medicine" in lower || "pill" in lower || "drug" in lower -> 
                    "While offline, I recommend opening the Medicine Scanner and pointing it at pill bottles to read essential dosage and safety guidelines."
                "waste" in lower || "recycle" in lower || "plastic" in lower -> 
                    "Under offline mode, you can still launch the Waste Classifier to scan and identify plastic, glass, or paper recyclables."
                "translate" in lower || "voice" in lower || "speech" in lower -> 
                    "You can access our Text and Voice Translators which support offline translation and context-rich definitions."
                "student" in lower || "math" in lower || "science" in lower || "solve" in lower -> 
                    "While offline, the Student Helper is ready! Simply crop any formula or problem to receive structured, step-by-step assistance."
                else -> 
                    "I'd love to help you with '$queryTopic'! Currently, I'm running in offline mode. Once your connection is restored, I can provide full Gemini AI analysis. For now, feel free to try our offline OCR, Waste Classifier, or Student Helper modules!"
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
            You are a highly intelligent, premium AI assistant for the "Smart Vision AI" app.
            Smart Vision AI is a dark-neon premium Android app designed with these specialized modules:
            1. Object Detector (Cyan badges, real-time object overlays)
            2. OCR Translator (Google Lens style selective scanner)
            3. Student Helper (Google Lens homework solver, tutor chats, math/science formulas)
            4. Waste Classifier (Green badges, eco-friendly recycling classifications)
            5. Voice Translator (real-time conversational audio translation)
            6. Smart Text Translator (context translation with native explanations)
            7. QR Scanner (fast scan codes)
            8. Document Scanner (PDF export filters)
            9. Medicine Scanner (pill scanner warning and dosage guidelines)
            
            Current App Screen Route Context: ${contextRoute ?: "Home Dashboard"}
            
            Guidelines:
            - You can answer ANY educational, scientific, coding, mathematical, or general inquiry dynamically.
            - Maintain context based on the conversation history.
            - Generate unique, intelligent, and contextually rich answers. Feel free to explain concepts, provide structured steps, code snippets, or analytical breakdowns.
            - Keep responses comprehensive yet concise enough to fit comfortably on a mobile chat interface.
        """.trimIndent()

        val finalPrompt = """
            $systemPrompt
            
            Conversation History:
            $historyTranscript
            
            User: $prompt
            Assistant:
        """.trimIndent()

        // Stream the content chunk-by-chunk using the official Gemini SDK stream method
        geminiModel.generateContentStream(content {
            text(finalPrompt)
        }).collect { response ->
            response.text?.let { emit(it) }
        }
    }.catch { e ->
        val lower = prompt.lowercase()
        val queryTopic = prompt.split("\\s+".toRegex())
            .map { it.replace(Regex("[^a-zA-Z]"), "") }
            .firstOrNull { it.length > 4 } ?: "topics"
            
        val offlineReply = when {
            "ocr" in lower || "scan" in lower || "text" in lower -> 
                "I am currently operating in offline mode, but you can use our premium OCR Scanner module to crop, translate, and extract native-language meanings from printed texts directly!"
            "medicine" in lower || "pill" in lower || "drug" in lower -> 
                "While offline, I recommend opening the Medicine Scanner and pointing it at pill bottles to read essential dosage and safety guidelines."
            "waste" in lower || "recycle" in lower || "plastic" in lower -> 
                "Under offline mode, you can still launch the Waste Classifier to scan and identify plastic, glass, or paper recyclables."
            "translate" in lower || "voice" in lower || "speech" in lower -> 
                "You can access our Text and Voice Translators which support offline translation and context-rich definitions."
            "student" in lower || "math" in lower || "science" in lower || "solve" in lower -> 
                "While offline, the Student Helper is ready! Simply crop any formula or problem to receive structured, step-by-step assistance."
            else -> 
                "I'd love to help you with '$queryTopic'! Currently, I'm running in offline mode. Once your connection is restored, I can provide full Gemini AI analysis. For now, feel free to try our offline OCR, Waste Classifier, or Student Helper modules!"
        }
        emit(offlineReply)
    }.flowOn(Dispatchers.IO)
}
