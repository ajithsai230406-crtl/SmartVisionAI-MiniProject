package com.smartvision.ai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// IMAGE UTILITIES
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class ImageUtils @Inject constructor(
    private val storage: FirebaseStorage
) {
    companion object {
        const val MAX_IMAGE_SIZE_PX = 1024
        const val JPEG_QUALITY       = 85
    }

    /** Load bitmap from content URI, auto-rotate via EXIF */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val raw = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            raw?.let { scaleBitmap(it, MAX_IMAGE_SIZE_PX) }
        } catch (e: Exception) {
            null
        }
    }

    /** Scale bitmap so its largest dimension <= maxSizePx, preserving aspect */
    fun scaleBitmap(bitmap: Bitmap, maxSizePx: Int = MAX_IMAGE_SIZE_PX): Bitmap {
        val w = bitmap.width; val h = bitmap.height
        if (w <= maxSizePx && h <= maxSizePx) return bitmap
        val ratio = maxSizePx.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    /** Compress bitmap to JPEG byte array */
    fun compressBitmap(bitmap: Bitmap, quality: Int = JPEG_QUALITY): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    /** Convert bitmap to Base64 string (for API calls) */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val bytes = compressBitmap(bitmap)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** Rotate bitmap by degrees */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Upload bitmap to Firebase Storage, return download URL */
    suspend fun uploadToFirebase(bitmap: Bitmap, path: String): Result<String> {
        return try {
            val bytes = compressBitmap(bitmap)
            val ref   = storage.reference.child(path)
            ref.putBytes(bytes).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Crop bitmap to a normalised rect (0..1 coordinates) */
    fun cropBitmap(bitmap: Bitmap, left: Float, top: Float, right: Float, bottom: Float): Bitmap {
        val x      = (left   * bitmap.width).toInt().coerceIn(0, bitmap.width)
        val y      = (top    * bitmap.height).toInt().coerceIn(0, bitmap.height)
        val width  = ((right  - left) * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
        val height = ((bottom - top)  * bitmap.height).toInt().coerceIn(1, bitmap.height - y)
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }
}
