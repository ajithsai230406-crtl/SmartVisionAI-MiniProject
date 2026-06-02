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

// Overall OCR scanner state
sealed class OcrScanState {
    object Idle : OcrScanState()
    object Scanning : OcrScanState()
    data class TextDetected(
        val result:      OcrResult,
        val frameWidth:  Int,
        val frameHeight: Int
    ) : OcrScanState()
    object NoText : OcrScanState()
    data class Error(val message: String) : OcrScanState()
}

// ─── Helper for coordinate rotation ──────────────────────────────────────────
private data class Float4(val f1: Float, val f2: Float, val f3: Float, val f4: Float)
private fun val4(f1: Float, f2: Float, f3: Float, f4: Float) = Float4(f1, f2, f3, f4)

// ─── Mapper: ML Kit Text → Domain ────────────────────────────────────────────
fun Text.toDomain(imageWidth: Int, imageHeight: Int, rotation: Int = 0): OcrResult {
    val blocks = this.textBlocks.flatMap { block ->
        block.lines.map { line ->
            val bbox = line.boundingBox
            val w = imageWidth.toFloat().coerceAtLeast(1f)
            val h = imageHeight.toFloat().coerceAtLeast(1f)

            val leftNorm   = (bbox?.left?.toFloat() ?: 0f) / w
            val topNorm    = (bbox?.top?.toFloat() ?: 0f) / h
            val rightNorm  = (bbox?.right?.toFloat() ?: 0f) / w
            val bottomNorm = (bbox?.bottom?.toFloat() ?: 0f) / h

            val mapped = when (rotation) {
                90 -> val4(
                    1f - bottomNorm,
                    leftNorm,
                    1f - topNorm,
                    rightNorm
                )
                270 -> val4(
                    topNorm,
                    1f - rightNorm,
                    bottomNorm,
                    1f - leftNorm
                )
                180 -> val4(
                    1f - rightNorm,
                    1f - bottomNorm,
                    1f - leftNorm,
                    1f - topNorm
                )
                else -> val4(
                    leftNorm,
                    topNorm,
                    rightNorm,
                    bottomNorm
                )
            }

            RecognizedBlock(
                text       = line.text,
                confidence = line.confidence,
                left       = mapped.f1.coerceIn(0f, 1f),
                top        = mapped.f2.coerceIn(0f, 1f),
                right      = mapped.f3.coerceIn(0f, 1f),
                bottom     = mapped.f4.coerceIn(0f, 1f),
                angle      = line.angle
            )
        }
    }
    return OcrResult(
        fullText = this.text,
        blocks   = blocks
    )
}
