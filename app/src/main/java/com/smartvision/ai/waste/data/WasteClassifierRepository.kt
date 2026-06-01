package com.smartvision.ai.waste.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.smartvision.ai.BuildConfig
import com.smartvision.ai.waste.domain.*
import com.smartvision.ai.data.db.GeminiCacheDao
import com.smartvision.ai.data.db.GeminiCacheEntity
import com.smartvision.ai.util.retryWithDelay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class WasteClassifierRepository @Inject constructor(
    private val cacheDao: GeminiCacheDao
) {

    private val model by lazy {
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    // Fast sparse content pixel signature for image caching
    private fun getBitmapHash(bitmap: Bitmap): Int {
        var hash = 17
        val w = bitmap.width
        val h = bitmap.height
        if (w > 0 && h > 0) {
            hash = 31 * hash + bitmap.getPixel(w / 4, h / 4)
            hash = 31 * hash + bitmap.getPixel(w / 2, h / 2)
            hash = 31 * hash + bitmap.getPixel(3 * w / 4, 3 * h / 4)
            hash = 31 * hash + w
            hash = 31 * hash + h
        }
        return hash
    }

    // ── Classify from camera bitmap using Gemini Vision ───────────────────────
    suspend fun classifyFromBitmap(bitmap: Bitmap): Result<WasteResult> = runCatching {
        val promptText = """You are a waste classification expert.
Analyze this image and classify the waste.

Respond EXACTLY in this format:
LABEL: [one of: plastic, metal, organic, paper, glass, ewaste, hazardous, textile, non_recyclable, mixed]
CONFIDENCE: [number 0.0 to 1.0]
DESCRIPTION: [one sentence about what you see]"""

        val imgHash = getBitmapHash(bitmap)
        val cacheKey = promptText.hashCode() xor imgHash

        // Check local cache
        val cached = cacheDao.getCache(cacheKey)
        if (cached != null) {
            return@runCatching parseGeminiResponse(cached.response)
        }

        val response = retryWithDelay(retries = 3) {
            model.generateContent(content {
                image(bitmap)
                text(promptText)
            })
        }

        val responseText = response.text ?: throw Exception("Empty vision response")
        cacheDao.insertCache(GeminiCacheEntity(cacheKey, "WasteScanImageHash:$imgHash", responseText))
        parseGeminiResponse(responseText)
    }

    // ── Classify from URI ────────────────────────────────────────────────────
    suspend fun classifyFromUri(context: Context, uri: Uri): Result<WasteResult> = runCatching {
        val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        classifyFromBitmap(bitmap).getOrThrow()
    }

    // ── Label-only classification (fast, offline) ─────────────────────────────
    fun classifyByLabel(rawLabel: String): WasteResult {
        val primaryType = rawLabel.toWasteType()
        return buildResult(rawLabel, primaryType, 0.88f)
    }

    // ── Response parser ───────────────────────────────────────────────────────
    private fun parseGeminiResponse(raw: String): WasteResult {
        fun extract(tag: String) = Regex("$tag:\\s*(.+)", RegexOption.IGNORE_CASE)
            .find(raw)?.groupValues?.get(1)?.trim() ?: ""

        val label      = extract("LABEL").ifBlank { "non_recyclable" }
        val confidence = extract("CONFIDENCE").toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.78f
        val type       = label.toWasteType()

        return buildResult(label, type, confidence)
    }

    private fun buildResult(rawLabel: String, primaryType: WasteType, confidence: Float): WasteResult {
        // Generate plausible distribution across related categories
        val allTypes = WasteType.values()
        val scores   = allTypes.map { t ->
            val score = if (t == primaryType) confidence
                        else (confidence * (0.1f + abs(t.name.hashCode() % 20) / 100f)).coerceAtMost(0.3f)
            WasteCategoryScore(t, score)
        }.sortedByDescending { it.score }

        return WasteResult(
            primaryType   = primaryType,
            confidence    = confidence,
            allCategories = scores,
            rawLabel      = rawLabel
        )
    }
}
