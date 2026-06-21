package com.smartvision.ai.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sealed class representing contextual data from the currently active module screen.
 * The floating AI assistant reads this to provide screen-aware answers.
 */
sealed class ScreenContext {
    object Empty : ScreenContext()

    data class TranslatorContext(
        val sourceText: String,
        val translatedText: String,
        val sourceLang: String,
        val targetLang: String,
        val semanticBreakdown: String = ""
    ) : ScreenContext()

    data class QrContext(
        val content: String,
        val type: String,    // URL, UPI, Contact, WiFi, Text, etc.
        val rawValue: String = ""
    ) : ScreenContext()

    data class WasteContext(
        val itemName: String,
        val wasteType: String,
        val confidence: Float,
        val isRecyclable: Boolean = true,
        val disposalMethod: String = "",
        val environmentalImpact: String = "",
        val recyclingSuggestions: String = "",
        val safetyPrecautions: String = "",
        val carbonImpact: String = ""
    ) : ScreenContext()

    data class MedicineContext(
        val name: String,
        val genericName: String = "",
        val type: String = "",
        val uses: String = "",
        val dosage: String = "",
        val sideEffects: String = "",
        val precautions: String = ""
    ) : ScreenContext()

    data class StudentContext(
        val question: String,
        val solution: String,
        val answer: String = "",
        val keyFormula: String = "",
        val subject: String = ""
    ) : ScreenContext()

    data class ObjectDetectorContext(
        val detectedObjects: List<String>,
        val confidences: List<Float> = emptyList(),
        val objectCount: Int = 0,
        val selectedLabel: String? = null,
        val selectedDescription: String? = null,
        val searchSummary: String? = null
    ) : ScreenContext()

    data class OcrContext(
        val extractedText: String,
        val translatedText: String = "",
        val sourceLang: String = "",
        val targetLang: String = ""
    ) : ScreenContext()

    data class VoiceTranslatorContext(
        val conversationTranscript: String,
        val sourceLang: String = "",
        val targetLang: String = ""
    ) : ScreenContext()

    data class CareerContext(
        val mode: String, // "RESUME_BUILDER", "RESUME_ANALYZER", "COVER_LETTER", "INTERVIEW_PREP"
        val resumeSummary: String = "",
        val analysisScore: Int = -1,
        val coverLetterContent: String = "",
        val currentQuestion: String = "",
        val userMockAnswer: String = ""
    ) : ScreenContext()
}

/**
 * Singleton that every module ViewModel writes to when results change.
 * The FloatingContextViewModel reads this to build screen-aware prompts.
 * 
 * Usage from any module ViewModel:
 *   screenContextManager.updateContext(ScreenContext.TranslatorContext(...))
 * 
 * Usage from floating assistant:
 *   val context by screenContextManager.currentContext.collectAsStateWithLifecycle()
 */
@Singleton
class ScreenContextManager @Inject constructor() {

    private val _currentContext = MutableStateFlow<ScreenContext>(ScreenContext.Empty)
    val currentContext: StateFlow<ScreenContext> = _currentContext.asStateFlow()

    private val _currentModuleName = MutableStateFlow("Home Dashboard")
    val currentModuleName: StateFlow<String> = _currentModuleName.asStateFlow()

    /**
     * Called by module ViewModels when their screen produces a result.
     */
    fun updateContext(context: ScreenContext, moduleName: String = "") {
        _currentContext.value = context
        if (moduleName.isNotBlank()) {
            _currentModuleName.value = moduleName
        }
    }

    /**
     * Called when navigating away from a module or clearing results.
     */
    fun clearContext() {
        _currentContext.value = ScreenContext.Empty
        _currentModuleName.value = "Home Dashboard"
    }

    /**
     * Generates a human-readable summary of the current screen context
     * for use in the floating assistant's initial message.
     */
    fun generateContextSummary(): String {
        return when (val ctx = _currentContext.value) {
            is ScreenContext.Empty -> ""
            is ScreenContext.TranslatorContext -> buildString {
                append("📝 **Translation Result**\n")
                append("• Source (${ctx.sourceLang}): \"${ctx.sourceText.take(200)}\"\n")
                append("• Translated (${ctx.targetLang}): \"${ctx.translatedText.take(200)}\"\n")
                if (ctx.semanticBreakdown.isNotBlank()) {
                    append("• Semantic breakdown available\n")
                }
            }
            is ScreenContext.QrContext -> buildString {
                append("📱 **QR/Barcode Scan Result**\n")
                append("• Type: ${ctx.type}\n")
                append("• Content: ${ctx.content.take(300)}\n")
            }
            is ScreenContext.WasteContext -> buildString {
                append("♻️ **Waste Classification Result**\n")
                append("• Item Name: ${ctx.itemName}\n")
                append("• Waste Category: ${ctx.wasteType}\n")
                append("• Confidence: ${(ctx.confidence * 100).toInt()}%\n")
                append("• Recyclable: ${if (ctx.isRecyclable) "Yes" else "No"}\n")
                if (ctx.disposalMethod.isNotBlank()) {
                    append("• Disposal Method: ${ctx.disposalMethod}\n")
                }
                if (ctx.environmentalImpact.isNotBlank()) {
                    append("• Environmental Impact: ${ctx.environmentalImpact}\n")
                }
                if (ctx.recyclingSuggestions.isNotBlank()) {
                    append("• Recycling Tips: ${ctx.recyclingSuggestions}\n")
                }
                if (ctx.safetyPrecautions.isNotBlank()) {
                    append("• Safety Precautions: ${ctx.safetyPrecautions}\n")
                }
            }
            is ScreenContext.MedicineContext -> buildString {
                append("💊 **Medicine Scan Result**\n")
                append("• Name: ${ctx.name}\n")
                if (ctx.genericName.isNotBlank()) append("• Generic: ${ctx.genericName}\n")
                if (ctx.type.isNotBlank()) append("• Type: ${ctx.type}\n")
                if (ctx.uses.isNotBlank()) append("• Uses: ${ctx.uses.take(200)}\n")
            }
            is ScreenContext.StudentContext -> buildString {
                append("📚 **Student Helper Result**\n")
                if (ctx.subject.isNotBlank()) append("• Subject: ${ctx.subject}\n")
                append("• Question: \"${ctx.question.take(200)}\"\n")
                if (ctx.answer.isNotBlank()) append("• Answer: ${ctx.answer.take(200)}\n")
            }
            is ScreenContext.ObjectDetectorContext -> buildString {
                append("🔍 **Visual Search Result**\n")
                if (ctx.selectedLabel != null) {
                    append("• Selected Object: ${ctx.selectedLabel}\n")
                    append("• Description: ${ctx.selectedDescription}\n")
                    if (!ctx.searchSummary.isNullOrBlank()) {
                        append("• AI Search Summary: ${ctx.searchSummary}\n")
                    }
                } else {
                    append("• ${ctx.objectCount} object(s) detected\n")
                    if (ctx.detectedObjects.isNotEmpty()) {
                        append("• Objects: ${ctx.detectedObjects.take(10).joinToString(", ")}\n")
                    }
                }
            }
            is ScreenContext.OcrContext -> buildString {
                append("📷 **OCR Extraction Result**\n")
                append("• Extracted text: \"${ctx.extractedText.take(200)}\"\n")
                if (ctx.translatedText.isNotBlank()) {
                    append("• Translation (${ctx.targetLang}): \"${ctx.translatedText.take(200)}\"\n")
                }
            }
            is ScreenContext.VoiceTranslatorContext -> buildString {
                append("🎙️ **Voice Translation**\n")
                append("• Languages: ${ctx.sourceLang} ↔ ${ctx.targetLang}\n")
                append("• Conversation transcript available (${ctx.conversationTranscript.length} chars)\n")
            }
            is ScreenContext.CareerContext -> buildString {
                append("💼 **Career Assistant**\n")
                append("• Feature: ${ctx.mode.replace("_", " ")}\n")
                if (ctx.resumeSummary.isNotBlank()) append("• Resume Summary: ${ctx.resumeSummary.take(150)}\n")
                if (ctx.analysisScore >= 0) append("• Resume ATS Score: ${ctx.analysisScore}%\n")
                if (ctx.coverLetterContent.isNotBlank()) append("• Cover Letter length: ${ctx.coverLetterContent.length} chars\n")
                if (ctx.currentQuestion.isNotBlank()) append("• Current Question: ${ctx.currentQuestion}\n")
                if (ctx.userMockAnswer.isNotBlank()) append("• User Mock Answer: ${ctx.userMockAnswer.take(150)}\n")
            }
        }
    }
}
