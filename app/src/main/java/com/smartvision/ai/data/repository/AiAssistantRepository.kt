package com.smartvision.ai.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.smartvision.ai.BuildConfig
import com.smartvision.ai.data.db.GeminiCacheDao
import com.smartvision.ai.data.db.GeminiCacheEntity
import com.smartvision.ai.util.retryWithDelay
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val cacheDao: GeminiCacheDao,
    private val fallbackApiClient: FallbackApiClient,
    @ApplicationContext private val context: Context
) {

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Context-aware reply using live Gemini Generative model with local offline Room cache.
     * Incorporates current screen context to provide targeted explanations or eco-friendly tips.
     */
    suspend fun replyTo(prompt: String, contextRoute: String? = null): String =
        replyToWithHistory(prompt, "", contextRoute)

    /**
     * Context-aware conversational AI assistant query supporting history memory.
     */
    suspend fun replyToWithHistory(
        prompt: String,
        historyTranscript: String,
        contextRoute: String? = null
    ): String = withContext(Dispatchers.IO) {
        // Note: We do NOT check isOnline() here — the network check can return false-negatives
        // on some devices even when internet is available. Instead we attempt the call and
        // gracefully fall back to the offline knowledge base if any network exception occurs.
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
                - Support technology queries, healthcare information, and general knowledge.
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

            // Check local offline cache
            val cachedResult = cacheDao.getCache(promptHash)
            if (cachedResult != null) {
                return@runCatching cachedResult.response
            }

            val startTime = System.currentTimeMillis()
            // Remote call with retries and fallback client
            val responseText = runCatching {
                fallbackApiClient.generateContent(finalPrompt)
            }.onSuccess {
                val latency = System.currentTimeMillis() - startTime
                com.smartvision.ai.utils.AnalyticsHelper.trackApiCall(latency, true)
            }.onFailure { e ->
                val latency = System.currentTimeMillis() - startTime
                com.smartvision.ai.utils.AnalyticsHelper.trackApiCall(latency, false)
                com.smartvision.ai.utils.AnalyticsHelper.logException(e)
            }.getOrThrow().trim()
            cacheDao.insertCache(GeminiCacheEntity(promptHash, finalPrompt, responseText))
            responseText
        }.getOrElse { e ->
            generateSmartOfflineReply(prompt, historyTranscript)
        }
    }

    /**
     * Context-aware streaming reply using live Gemini Generative model.
     */
    fun replyToWithHistoryStream(
        prompt: String,
        historyTranscript: String,
        contextRoute: String? = null
    ): Flow<String> = flow {
        // Attempt Gemini streaming directly — the .catch block will emit an offline
        // reply if any network exception occurs, preventing false offline messages.
        
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
            - Support technology queries, healthcare information, and general knowledge.
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

        fallbackApiClient.generateContentStream(finalPrompt).collect { chunk ->
            emit(chunk)
        }
    }.catch { e ->
        emit(generateSmartOfflineReply(prompt, historyTranscript))
    }.flowOn(Dispatchers.IO)

    // ─── Local Offline Knowledge Base ──────────────────────────────────────────
    private data class ModuleOfflineInfo(
        val displayName: String,
        val whatItDoes: String,
        val howItWorks: String,
        val howToUse: String
    )

    private val offlineKnowledge = mapOf(
        "ocr_translator" to ModuleOfflineInfo(
            displayName = "OCR Translator",
            whatItDoes = "Extracts text from live camera frames or selected images and translates it dynamically into your chosen target language.",
            howItWorks = "Utilizes Google ML Kit Text Recognition v2 to detect layout-aware text blocks and reconstruct lines, coupled with on-device translation packs for instant offline translation overlays.",
            howToUse = "1. Point the camera at any text or upload a photo.\n2. The detector highlights recognized text blocks. Tap or drag to select specific blocks.\n3. Choose your source and target languages from the top pill.\n4. View translated text on the glassmorphic overlay, listen via Text-to-Speech (🔊), or copy/share."
        ),
        "object_detector" to ModuleOfflineInfo(
            displayName = "Object Detector",
            whatItDoes = "Identifies objects in the camera view in real time, overlays cyberpunk neon bounding boxes, counts targets, and tracks them across frames.",
            howItWorks = "Runs a highly optimized custom EfficientDet-Lite4 TensorFlow Lite model on-device. It outputs categories, bounding box dimensions, and confidence scores at 30+ FPS.",
            howToUse = "1. Open the Object Detector module.\n2. Point the camera at any item. Neon boxes will track objects (e.g. Laptop, Bottle, Book) with their confidence levels.\n3. The top HUD displays the total active object count.\n4. Tap any bounding box to open a draggable detail sheet with AI-generated safety, eco, and buying insights."
        ),
        "medicine_scanner" to ModuleOfflineInfo(
            displayName = "Medicine Scanner",
            whatItDoes = "Scans pill packaging and prescription labels to identify generic drug names, active ingredients, dosage warnings, and precautions.",
            howItWorks = "Uses OCR to extract text from drug bottles/packages, matches it against a local medicine database of active generic substances, and flags high-risk drugs.",
            howToUse = "1. Open the Medicine Scanner.\n2. Scan the box or bottle label.\n3. The app extracts the medicine name and displays generic substances.\n4. Read the dosage guidelines, side effects, and warning alerts (critical high-risk items display pulsing red tags)."
        ),
        "waste_classifier" to ModuleOfflineInfo(
            displayName = "Waste Classifier",
            whatItDoes = "Scans household items, trash, or packaging, classifies them into recycling categories, and calculates carbon footprint savings.",
            howItWorks = "Leverages on-device image classification to segment waste into Plastic, Paper, Metal, Glass, or Organic, providing specific municipal recycling actions.",
            howToUse = "1. Position the waste item inside the viewfinder.\n2. View the highlighted category badge (e.g., Recycling Blue Bin for PET bottles).\n3. Read the disposal instructions and the estimated carbon offset details."
        ),
        "student_helper" to ModuleOfflineInfo(
            displayName = "Student Helper",
            whatItDoes = "Solves mathematics, physics, and chemistry equations and questions from scanned textbook pages with tutor explanations.",
            howItWorks = "Isolates formula characters via math OCR segmentation, balances chemical equations, and resolves algebraic parameters step-by-step.",
            howToUse = "1. Frame any math/science problem inside the resizable selector box.\n2. Tap 'Solve' to analyze the formula.\n3. View detailed step-by-step mathematical isolates.\n4. Tap the 'Tutor Chat' shortcut to ask follow-up questions contextually."
        ),
        "voice_translator" to ModuleOfflineInfo(
            displayName = "Voice Translator",
            whatItDoes = "Enables real-time voice-to-voice conversation translation between two speakers using different languages.",
            howItWorks = "Translates spoken audio streams into target languages on-the-fly using Google Speech-to-Text, local translation engines, and text-to-speech outputs.",
            howToUse = "1. Select the languages for both speakers.\n2. Tap and hold the mic button corresponding to your language and speak.\n3. The translated voice will play back automatically.\n4. View the scrollable side-by-side neon dialog transcript."
        ),
        "text_translator" to ModuleOfflineInfo(
            displayName = "Smart Text Translator",
            whatItDoes = "Translates typed text blocks while providing native grammar breakdowns, Romanized pronunciations, and contextual nuance explanations.",
            howItWorks = "Processes text inputs through contextual translation models and maps words to native pronunciation syllables.",
            howToUse = "1. Type or paste your text in the input area.\n2. Select your source and target languages.\n3. Read the translation along with grammatical breakdowns and romanized pronunciations.\n4. Tap the speaker icon to hear the native pronunciation."
        ),
        "qr_scanner" to ModuleOfflineInfo(
            displayName = "QR Scanner",
            whatItDoes = "Instantly scans QR codes, barcodes, and code patterns to extract web URLs, text data, contact info, or Wi-Fi configurations.",
            howItWorks = "Uses ML Kit Barcode Scanning to detect barcode formats offline at high speed.",
            howToUse = "1. Align the camera so the QR/barcode is within the targeted reticle.\n2. Tap the glassmorphic card overlay that pops up to open URLs, copy data, or connect to networks."
        ),
        "document_scanner" to ModuleOfflineInfo(
            displayName = "Document Scanner",
            whatItDoes = "Scans physical documents, crops page edges automatically, straightens perspective, and exports them as high-quality multipage PDFs.",
            howItWorks = "Uses Google Play Services Document Scanner to perform real-time edge detection, auto-crop, and color enhancements.",
            howToUse = "1. Align the document in the camera preview; the scanner will automatically detect paper edges.\n2. Review, crop, or rotate the captured pages.\n3. Apply filters (grayscale, black & white, or color enhancement).\n4. Export and share as a PDF file."
        )
    )

    private fun generateSmartOfflineReply(prompt: String, historyTranscript: String): String {
        val lower = prompt.lowercase().trim()

        // 1. Identify which module the user is asking about
        val moduleKey = when {
            lower.contains("ocr") || lower.contains("ocr translator") || (lower.contains("translator") && (lower.contains("camera") || lower.contains("live") || lower.contains("image") || lower.contains("lens"))) -> "ocr_translator"
            lower.contains("object") || lower.contains("detector") || lower.contains("detect") || lower.contains("tracking") -> "object_detector"
            lower.contains("medicine") || lower.contains("pill") || lower.contains("drug") || lower.contains("prescription") || lower.contains("dosage") -> "medicine_scanner"
            lower.contains("waste") || lower.contains("classifier") || lower.contains("recycle") || lower.contains("eco") || lower.contains("plastic") || lower.contains("glass") || lower.contains("recycling") -> "waste_classifier"
            lower.contains("student") || lower.contains("helper") || lower.contains("homework") || lower.contains("math") || lower.contains("solve") || lower.contains("equation") || lower.contains("chemistry") || lower.contains("physics") -> "student_helper"
            lower.contains("voice") && lower.contains("translator") || lower.contains("speech") || lower.contains("speak") || lower.contains("conversation") || lower.contains("voice translator") -> "voice_translator"
            lower.contains("smart text") || lower.contains("text translator") || (lower.contains("translator") && lower.contains("text")) || lower.contains("translate text") || lower.contains("translation") -> "text_translator"
            lower.contains("qr") || lower.contains("barcode") || lower.contains("code") || lower.contains("scan code") || lower.contains("qr scanner") -> "qr_scanner"
            lower.contains("document") || lower.contains("pdf") || lower.contains("scanner") || lower.contains("paper") || lower.contains("document scanner") -> "document_scanner"
            else -> null
        }

        if (moduleKey != null) {
            val mInfo = offlineKnowledge[moduleKey]!!
            
            // 2. Identify the specific question they are asking
            val isHowDoesItWork = lower.contains("how does it work") || lower.contains("how it works") || lower.contains("mechanism") || lower.contains("under the hood") || lower.contains("how work") || lower.contains("underlying")
            val isHowToUse = lower.contains("use") || lower.contains("how do i") || lower.contains("how to use") || lower.contains("guide") || lower.contains("instructions") || lower.contains("steps") || lower.contains("tutorial")
            val isWhatDoesItDo = lower.contains("what") || lower.contains("do") || lower.contains("purpose") || lower.contains("feature") || lower.contains("about") || lower.contains("explain") || lower.contains("describe")

            return when {
                isHowDoesItWork -> "### How the ${mInfo.displayName} Works\n\n${mInfo.howItWorks}"
                isHowToUse -> "### How to Use the ${mInfo.displayName}\n\n${mInfo.howToUse}"
                isWhatDoesItDo -> "### What the ${mInfo.displayName} Does\n\n${mInfo.whatItDoes}"
                else -> "### ${mInfo.displayName} Overview\n\n**What it does:** ${mInfo.whatItDoes}\n\n**How it works:** ${mInfo.howItWorks}\n\n**How to use:** ${mInfo.howToUse}"
            }
        }

        // Generic greetings or general offline guidance
        val isGreeting = lower.contains("hello") || lower.contains("hi ") || lower.matches(Regex("hi|hey|yo")) || lower.contains("greetings")
        return if (isGreeting) {
            "👋 Hello! I am currently in **Offline Mode**. I can explain any of our 9 specialized modules in detail.\n\nTry asking me:\n- *'What does the OCR Translator do?'*\n- *'How does the Object Detector work?'*\n- *'How do I use the Student Helper?'*\n\nRestore internet access to ask general, educational, technology, or healthcare questions!"
        } else {
            "ℹ️ I am currently in **Offline Mode**. I can explain any of our 9 specialized modules (What it does / How it works / How to use).\n\nHere are the modules you can ask me about:\n1. **OCR Translator**\n2. **Object Detector**\n3. **Medicine Scanner**\n4. **Waste Classifier**\n5. **Student Helper**\n6. **Voice Translator**\n7. **Smart Text Translator**\n8. **QR Scanner**\n9. **Document Scanner**\n\n*Please restore your internet connection to ask general, educational, technology, or healthcare questions.*"
        }
    }
}
