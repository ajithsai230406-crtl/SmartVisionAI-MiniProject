package com.smartvision.ai.domain.models

import android.net.Uri

// ── Module definitions ────────────────────────────────────────────────────────
enum class ModuleType(val title: String, val subtitle: String, val route: String) {
    OBJECT_DETECTION("Object Detection", "Detect objects with AI confidence scores", "object_detection"),
    TEXT_SCANNER    ("Text Scanner (OCR)", "Extract text — copy, translate, speak",   "text_scanner"),
    TRANSLATOR      ("Translator",         "Translate text in real-time",              "translator"),
    VOICE_TRANSLATOR("Voice Translator",   "Speak & translate instantly",             "voice_translator"),
    STUDENT_HELPER  ("Student Helper",     "Solve questions with Gemini AI",          "student_helper"),
    MEDICAL_SCANNER ("Medical Scanner",    "Scan medicine strips for dosage & info",  "medical_scanner"),
    WASTE_CLASSIFIER("Waste Classifier",   "Identify recyclable / organic waste",     "waste_classifier"),
    QR_SCANNER      ("QR Scanner",         "Scan QR codes — auto-open links",         "qr_scanner")
}

// ── Scan results ──────────────────────────────────────────────────────────────
sealed class ScanResult {
    data class ObjectDetectionResult(val objects: List<DetectedObject>) : ScanResult()
    data class OcrResult(val rawText: String, val textBlocks: List<TextBlock>, val selectedText: String? = null) : ScanResult()
    data class TranslationResult(val originalText: String, val translatedText: String, val sourceLanguage: String, val targetLanguage: String) : ScanResult()
    data class StudentHelperResult(val question: String, val aiExplanation: String, val steps: List<String> = emptyList()) : ScanResult()
    data class MedicalScanResult(val medicineName: String, val usage: String, val dosage: String, val sideEffects: List<String>, val warnings: List<String>) : ScanResult()
    data class WasteClassifierResult(val categories: List<WasteCategory>) : ScanResult()
    data class QrCodeResult(val rawValue: String, val type: QrType, val displayValue: String) : ScanResult()
    data class Error(val message: String) : ScanResult()
    object Loading : ScanResult()
}

// ── Sub-models ────────────────────────────────────────────────────────────────
data class DetectedObject(val label: String, val confidence: Float, val boundingBox: BoundingBox)
data class BoundingBox(val left: Float, val top: Float, val right: Float, val bottom: Float)
data class TextBlock(val text: String, val boundingBox: BoundingBox)
data class WasteCategory(val label: String, val confidence: Float, val color: Long)
enum class QrType { URL, TEXT, EMAIL, PHONE, SMS, WIFI, CONTACT, OTHER }

// ── Firestore model ───────────────────────────────────────────────────────────
data class ScanHistoryItem(
    val id: String = "", val userId: String = "", val moduleType: String = "",
    val summary: String = "", val imageUrl: String = "",
    val timestampMillis: Long = 0L, val resultJson: String = ""
)

// ── User prefs ────────────────────────────────────────────────────────────────
data class UserPreferences(
    val appTheme: String = "DARK", val defaultLanguage: String = "en",
    val enableTts: Boolean = true, val saveHistory: Boolean = true,
    val liveDetection: Boolean = false
)
