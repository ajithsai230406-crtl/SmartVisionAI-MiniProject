package com.smartvision.ai.ocr.domain

import com.google.mlkit.vision.text.Text
import kotlin.math.abs

// ─── Domain Models ────────────────────────────────────────────────────────────

/** A single recognized text line with its normalized bounding coordinates */
data class RecognizedBlock(
    val text: String,
    val confidence: Float?,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val angle: Float = 0f
)

/** A paragraph — a group of lines that logically belong together */
data class RecognizedParagraph(
    val text: String,
    val lines: List<RecognizedBlock>,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float = 1f
)

/** Full OCR result from a single frame */
data class OcrResult(
    val fullText: String,
    val paragraphs: List<RecognizedParagraph>,
    val blocks: List<RecognizedBlock>,           // flat list for overlay rendering
    val detectedLanguage: String = "und",        // BCP-47 code
    val detectedLanguageName: String = "Detecting...",
    val detectedScript: String = "Latin",
    val hasRotatedText: Boolean = false,
    val processingTimeMs: Long = 0L
)

enum class OcrMode {
    TEXT, TRANSLATE
}

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
        val result: OcrResult,
        val frameWidth: Int,
        val frameHeight: Int
    ) : OcrScanState()
    object NoText : OcrScanState()
    data class Error(val message: String) : OcrScanState()
}

// ─── Helper for coordinate rotation ──────────────────────────────────────────
private data class Float4(val f1: Float, val f2: Float, val f3: Float, val f4: Float)
private fun val4(f1: Float, f2: Float, f3: Float, f4: Float) = Float4(f1, f2, f3, f4)

// ─── Mapper: ML Kit Text → Domain ────────────────────────────────────────────
// MIN_CONFIDENCE: blocks below this are considered noise and are dropped in live mode
private const val MIN_LINE_CONFIDENCE = 0.40f
// MIN_TEXT_LENGTH: lines with fewer chars are likely OCR artifacts (single char noise)
private const val MIN_TEXT_LENGTH = 2

fun Text.toDomain(imageWidth: Int, imageHeight: Int, rotation: Int = 0): OcrResult {
    val startTime = System.currentTimeMillis()
    val w = imageWidth.toFloat().coerceAtLeast(1f)
    val h = imageHeight.toFloat().coerceAtLeast(1f)

    // In live camera mode (rotation != 0), apply confidence filtering to reduce noise.
    // In still image mode (rotation == 0, e.g. gallery), keep all blocks for completeness.
    val filterLowConf = rotation != 0

    // Build paragraphs from ML Kit text blocks (each block = a paragraph)
    val paragraphs = this.textBlocks.mapNotNull { block ->
        if (block.text.isBlank()) return@mapNotNull null

        val lines = block.lines.mapNotNull { line ->
            // Filter single-char artifacts and low-confidence lines in live mode
            if (line.text.trim().length < MIN_TEXT_LENGTH) return@mapNotNull null
            val conf = line.confidence
            if (filterLowConf && conf != null && conf < MIN_LINE_CONFIDENCE) return@mapNotNull null

            val bbox = line.boundingBox
            // Filter micro-noise regions (bounding box too tiny to contain real text)
            val bboxW = (bbox?.width()?.toFloat() ?: 1f) / w
            val bboxH = (bbox?.height()?.toFloat() ?: 1f) / h
            if (filterLowConf && (bboxW < 0.01f || bboxH < 0.005f)) return@mapNotNull null

            val leftNorm   = (bbox?.left?.toFloat()   ?: 0f) / w
            val topNorm    = (bbox?.top?.toFloat()    ?: 0f) / h
            val rightNorm  = (bbox?.right?.toFloat()  ?: 0f) / w
            val bottomNorm = (bbox?.bottom?.toFloat() ?: 0f) / h

            val mapped = rotateNorm(leftNorm, topNorm, rightNorm, bottomNorm, rotation)
            RecognizedBlock(
                text       = line.text,
                confidence = conf,
                left       = mapped.f1.coerceIn(0f, 1f),
                top        = mapped.f2.coerceIn(0f, 1f),
                right      = mapped.f3.coerceIn(0f, 1f),
                bottom     = mapped.f4.coerceIn(0f, 1f),
                angle      = line.angle
            )
        }

        if (lines.isEmpty()) return@mapNotNull null

        // Paragraph bounding box from block
        val bbbox = block.boundingBox
        val pLeftNorm   = (bbbox?.left?.toFloat()   ?: 0f) / w
        val pTopNorm    = (bbbox?.top?.toFloat()    ?: 0f) / h
        val pRightNorm  = (bbbox?.right?.toFloat()  ?: 0f) / w
        val pBottomNorm = (bbbox?.bottom?.toFloat() ?: 0f) / h
        val pMapped = rotateNorm(pLeftNorm, pTopNorm, pRightNorm, pBottomNorm, rotation)

        val avgConfidence = lines.mapNotNull { it.confidence }.let { cs ->
            if (cs.isEmpty()) 1f else cs.average().toFloat()
        }

        RecognizedParagraph(
            text       = lines.joinToString("\n") { it.text },
            lines      = lines,
            left       = pMapped.f1.coerceIn(0f, 1f),
            top        = pMapped.f2.coerceIn(0f, 1f),
            right      = pMapped.f3.coerceIn(0f, 1f),
            bottom     = pMapped.f4.coerceIn(0f, 1f),
            confidence = avgConfidence
        )
    }.sortedWith(compareBy({ it.top }, { it.left }))  // reading order: top-to-bottom, left-to-right

    // Flat blocks list for overlay rendering (all lines across all paragraphs)
    val allBlocks = paragraphs.flatMap { it.lines }

    // Detect rotated text: any block with |angle| > 5°
    val hasRotated = allBlocks.any { abs(it.angle) > 5f }

    // Reconstruct full text preserving paragraph structure (double newline between paragraphs)
    val reconstructedText = paragraphs.joinToString("\n\n") { para ->
        para.lines.joinToString("\n") { it.text }
    }

    return OcrResult(
        fullText         = reconstructedText.ifBlank { this.text },
        paragraphs       = paragraphs,
        blocks           = allBlocks,
        hasRotatedText   = hasRotated,
        processingTimeMs = System.currentTimeMillis() - startTime
    )
}

private fun rotateNorm(l: Float, t: Float, r: Float, b: Float, rotation: Int): Float4 = when (rotation) {
    90  -> val4(1f - b, l, 1f - t, r)
    270 -> val4(t, 1f - r, b, 1f - l)
    180 -> val4(1f - r, 1f - b, 1f - l, 1f - t)
    else -> val4(l, t, r, b)
}
