package com.smartvision.ai.domain.models

import android.graphics.Bitmap
import android.net.Uri

// ─────────────────────────────────────────────────────────────────────────────
// MODULE DEFINITIONS
// ─────────────────────────────────────────────────────────────────────────────

enum class ModuleType(
    val title: String,
    val subtitle: String,
    val route: String
) {
    OBJECT_DETECTION(
        title    = "Object Detection",
        subtitle = "Detect objects with AI confidence scores",
        route    = "object_detection"
    ),
    TEXT_SCANNER(
        title    = "Text Scanner (OCR)",
        subtitle = "Extract text — copy, translate, speak",
        route    = "text_scanner"
    ),
    TRANSLATOR(
        title    = "Translator",
        subtitle = "Translate text in real-time",
        route    = "translator"
    ),
    VOICE_TRANSLATOR(
        title    = "Voice Translator",
        subtitle = "Speak & translate instantly",
        route    = "voice_translator"
    ),
    STUDENT_HELPER(
        title    = "Student Helper",
        subtitle = "Solve questions & formulas with Gemini",
        route    = "student_helper"
    ),
    MEDICAL_SCANNER(
        title    = "Medical Scanner",
        subtitle = "Scan medicine strips for dosage & info",
        route    = "medical_scanner"
    ),
    WASTE_CLASSIFIER(
        title    = "Waste Classifier",
        subtitle = "Classify recyclable / organic waste",
        route    = "waste_classifier"
    ),
    QR_SCANNER(
        title    = "QR Scanner",
        subtitle = "Scan QR codes — auto-open links",
        route    = "qr_scanner"
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SCAN RESULTS
// ─────────────────────────────────────────────────────────────────────────────

sealed class ScanResult {

    data class ObjectDetectionResult(
        val objects: List<DetectedObject>
    ) : ScanResult()

    data class OcrResult(
        val rawText: String,
        val textBlocks: List<TextBlock>,
        val selectedText: String? = null
    ) : ScanResult()

    data class TranslationResult(
        val originalText: String,
        val translatedText: String,
        val sourceLanguage: String,
        val targetLanguage: String
    ) : ScanResult()

    data class StudentHelperResult(
        val question: String,
        val aiExplanation: String,
        val steps: List<String> = emptyList()
    ) : ScanResult()

    data class MedicalScanResult(
        val medicineName: String,
        val usage: String,
        val dosage: String,
        val sideEffects: List<String>,
        val warnings: List<String>
    ) : ScanResult()

    data class WasteClassifierResult(
        val categories: List<WasteCategory>
    ) : ScanResult()

    data class QrCodeResult(
        val rawValue: String,
        val type: QrType,
        val displayValue: String
    ) : ScanResult()

    data class Error(val message: String) : ScanResult()
    object Loading : ScanResult()
}

// ─────────────────────────────────────────────────────────────────────────────
// SUB-MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class DetectedObject(
    val label: String,
    val confidence: Float,       // 0.0 – 1.0
    val boundingBox: BoundingBox
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class TextBlock(
    val text: String,
    val boundingBox: BoundingBox
)

data class WasteCategory(
    val label: String,           // e.g. "Recyclable", "Organic", "Non-recyclable"
    val confidence: Float,
    val color: Long              // ARGB color for badge
)

enum class QrType { URL, TEXT, EMAIL, PHONE, SMS, WIFI, CONTACT, OTHER }

// ─────────────────────────────────────────────────────────────────────────────
// SCAN HISTORY ITEM (Firestore model)
// ─────────────────────────────────────────────────────────────────────────────

data class ScanHistoryItem(
    val id: String            = "",
    val userId: String        = "",
    val moduleType: String    = "",          // ModuleType.name
    val summary: String       = "",
    val imageUrl: String      = "",
    val timestampMillis: Long = 0L,
    val resultJson: String    = ""           // JSON of ScanResult
)

// ─────────────────────────────────────────────────────────────────────────────
// USER PREFERENCES
// ─────────────────────────────────────────────────────────────────────────────

data class UserPreferences(
    val appTheme: String        = "DARK",    // AppTheme.name
    val defaultLanguage: String = "en",
    val enableTts: Boolean      = true,
    val saveHistory: Boolean    = true,
    val liveDetection: Boolean  = false
)
