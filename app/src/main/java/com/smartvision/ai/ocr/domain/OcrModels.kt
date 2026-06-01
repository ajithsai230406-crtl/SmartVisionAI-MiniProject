package com.smartvision.ai.ocr.domain

import com.google.mlkit.vision.text.Text

// ─── Domain Models ────────────────────────────────────────────────────────────

/** A single recognized text block with its bounding box info */
data class RecognizedBlock(
    val text: String,
    val confidence: Float?,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val angle: Float = 0f
)

/** Full OCR result from a single frame */
data class OcrResult(
    val fullText: String,
    val blocks: List<RecognizedBlock>,
    val detectedLanguage: String = "und",   // BCP-47 code
    val detectedLanguageName: String = "Detecting..."
)

/** Translation state for the UI */
sealed class TranslationState {
    object Idle : TranslationState()
    object Downloading : TranslationState()   // Model download in progress
    object Translating : TranslationState()
    data class Success(
        val translatedText: String,
        val sourceLang: String,              // Display name
        val targetLang: String,
        val nativeMeaning: String? = null,
        val semanticMeaning: com.smartvision.ai.translator.domain.SemanticMeaning? = null
    ) : TranslationState()
    data class Error(val message: String) : TranslationState()
}

/** Overall OCR scanner state */
sealed class OcrScanState {
    object Idle : OcrScanState()
    object Scanning : OcrScanState()
    data class TextDetected(val result: OcrResult) : OcrScanState()
    object NoText : OcrScanState()
    data class Error(val message: String) : OcrScanState()
}

// ─── Mapper: ML Kit Text → Domain ────────────────────────────────────────────
fun Text.toDomain(imageWidth: Int, imageHeight: Int): OcrResult {
    val blocks = this.textBlocks.flatMap { block ->
        block.lines.map { line ->
            val bbox = line.boundingBox
            RecognizedBlock(
                text       = line.text,
                confidence = line.confidence,
                left       = (bbox?.left?.toFloat() ?: 0f) / imageWidth,
                top        = (bbox?.top?.toFloat() ?: 0f) / imageHeight,
                right      = (bbox?.right?.toFloat() ?: 0f) / imageWidth,
                bottom     = (bbox?.bottom?.toFloat() ?: 0f) / imageHeight,
                angle      = line.angle
            )
        }
    }
    return OcrResult(
        fullText = this.text,
        blocks   = blocks
    )
}
