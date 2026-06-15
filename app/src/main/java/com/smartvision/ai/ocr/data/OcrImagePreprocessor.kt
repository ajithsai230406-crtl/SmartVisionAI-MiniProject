package com.smartvision.ai.ocr.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Pre-processing pipeline applied to camera bitmaps before OCR.
 *
 * Steps (in order):
 *  1. Downscale to a sensible resolution (≤ 2 MP) for speed
 *  2. Grayscale conversion
 *  3. Adaptive contrast / brightness correction (CLAHE-inspired histogram stretch)
 *  4. Gaussian-approximation noise reduction (3×3 box blur)
 *  5. Unsharp-mask sharpening
 *  6. Return result for ML Kit ingestion
 *
 * All heavy work runs on [Dispatchers.Default].
 */
@Singleton
class OcrImagePreprocessor @Inject constructor() {

    companion object {
        private const val MAX_DIMENSION = 1920   // cap longest edge
        private const val SHARPEN_STRENGTH = 0.45f
    }

    // ── Public entry point ───────────────────────────────────────────────────

    /** Returns a preprocessed [Bitmap] ready for ML Kit. */
    suspend fun preprocess(source: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        var bmp = downscaleIfNeeded(source)
        bmp = toGrayscale(bmp)
        bmp = adaptiveContrastStretch(bmp)
        bmp = gaussianDenoise(bmp)
        bmp = unsharpSharpen(bmp)
        bmp
    }

    // ── Step 1: Downscale ────────────────────────────────────────────────────

    private fun downscaleIfNeeded(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val longest = max(w, h)
        if (longest <= MAX_DIMENSION) return src
        val scale = MAX_DIMENSION.toFloat() / longest
        return Bitmap.createScaledBitmap(src, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    // ── Step 2: Grayscale ────────────────────────────────────────────────────

    private fun toGrayscale(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint()
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    // ── Step 3: Adaptive contrast stretch (histogram min-max normalisation) ──

    private fun adaptiveContrastStretch(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        // Build luminance histogram
        val hist = IntArray(256)
        for (px in pixels) {
            hist[Color.red(px)]++   // grayscale → R == G == B
        }

        // Clip 1% tails for robustness
        val totalPixels = width * height
        val clipCount = totalPixels / 100

        var low = 0
        var cumSum = 0
        while (low < 255 && cumSum + hist[low] < clipCount) { cumSum += hist[low++] }

        var high = 255
        cumSum = 0
        while (high > 0 && cumSum + hist[high] < clipCount) { cumSum += hist[high--] }

        if (high <= low) return src  // already well-exposed

        val range = (high - low).toFloat()

        // Gamma correction (auto-brightness): push mid-tones toward 0.5
        val avgLum = pixels.map { Color.red(it) }.average().toFloat() / 255f
        val gamma = when {
            avgLum < 0.35f -> 0.75f   // dark image → brighten
            avgLum > 0.70f -> 1.35f   // bright image → darken slightly
            else -> 1.0f
        }

        val lut = IntArray(256) { i ->
            val stretched = ((i - low).toFloat() / range * 255f).toInt().coerceIn(0, 255)
            if (gamma == 1.0f) stretched
            else ((stretched / 255f).toDouble().pow(gamma.toDouble()) * 255).toInt().coerceIn(0, 255)
        }

        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val outPixels = IntArray(width * height)
        out.getPixels(outPixels, 0, width, 0, 0, width, height)
        for (i in outPixels.indices) {
            val v = lut[Color.red(outPixels[i])]
            outPixels[i] = Color.argb(255, v, v, v)
        }
        out.setPixels(outPixels, 0, width, 0, 0, width, height)
        return out
    }

    // ── Step 4: 3×3 Gaussian noise reduction (pure-Kotlin, no RenderScript) ─

    private fun gaussianDenoise(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val srcPixels = IntArray(w * h)
        src.getPixels(srcPixels, 0, w, 0, 0, w, h)
        val dstPixels = IntArray(w * h)

        // Approximated Gaussian kernel: [1, 2, 1; 2, 4, 2; 1, 2, 1] / 16
        val kernel = intArrayOf(1, 2, 1, 2, 4, 2, 1, 2, 1)

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                var sum = 0
                var k = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        sum += Color.red(srcPixels[(y + dy) * w + (x + dx)]) * kernel[k++]
                    }
                }
                val v = (sum / 16).coerceIn(0, 255)
                dstPixels[y * w + x] = Color.argb(255, v, v, v)
            }
        }

        // Copy edge pixels unchanged
        for (x in 0 until w) { dstPixels[x] = srcPixels[x]; dstPixels[(h - 1) * w + x] = srcPixels[(h - 1) * w + x] }
        for (y in 0 until h) { dstPixels[y * w] = srcPixels[y * w]; dstPixels[y * w + w - 1] = srcPixels[y * w + w - 1] }

        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        out.setPixels(dstPixels, 0, w, 0, 0, w, h)
        return out
    }

    // ── Step 5: Unsharp-mask sharpening ──────────────────────────────────────

    private fun unsharpSharpen(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val srcPixels = IntArray(w * h)
        src.getPixels(srcPixels, 0, w, 0, 0, w, h)
        val dstPixels = IntArray(w * h)

        // Laplacian sharpening kernel with strength control
        val s = SHARPEN_STRENGTH
        val center = (1f + 4f * s)

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val c  = Color.red(srcPixels[y * w + x])
                val n  = Color.red(srcPixels[(y - 1) * w + x])
                val s2 = Color.red(srcPixels[(y + 1) * w + x])
                val l  = Color.red(srcPixels[y * w + x - 1])
                val r  = Color.red(srcPixels[y * w + x + 1])
                val v = (c * center - (n + s2 + l + r) * s).toInt().coerceIn(0, 255)
                dstPixels[y * w + x] = Color.argb(255, v, v, v)
            }
        }

        for (x in 0 until w) { dstPixels[x] = srcPixels[x]; dstPixels[(h - 1) * w + x] = srcPixels[(h - 1) * w + x] }
        for (y in 0 until h) { dstPixels[y * w] = srcPixels[y * w]; dstPixels[y * w + w - 1] = srcPixels[y * w + w - 1] }

        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        out.setPixels(dstPixels, 0, w, 0, 0, w, h)
        return out
    }
}
