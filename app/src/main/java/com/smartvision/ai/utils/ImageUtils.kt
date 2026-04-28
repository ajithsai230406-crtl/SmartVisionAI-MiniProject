package com.smartvision.ai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// FIREBASE STORAGE UPLOAD HELPER
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class StorageUploadHelper @Inject constructor(
    private val storage: FirebaseStorage
) {
    /**
     * Uploads a bitmap to Firebase Storage under users/{uid}/scans/{uuid}.jpg
     * Returns the public download URL.
     */
    suspend fun uploadScanImage(
        bitmap:  Bitmap,
        userId:  String,
        quality: Int = 75         // JPEG compression quality 0–100
    ): Result<String> {
        return try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            val bytes = baos.toByteArray()

            val ref: StorageReference = storage.reference
                .child("users/$userId/scans/${UUID.randomUUID()}.jpg")

            ref.putBytes(bytes).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Delete a previously uploaded image by its download URL */
    suspend fun deleteScanImage(downloadUrl: String): Result<Unit> {
        return try {
            storage.getReferenceFromUrl(downloadUrl).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BITMAP UTILITIES
// ─────────────────────────────────────────────────────────────────────────────

object ImageUtils {

    /**
     * Scales a bitmap down so its longest edge ≤ maxEdge.
     * Preserves aspect ratio. Returns the original if already small enough.
     */
    fun scaleBitmap(bitmap: Bitmap, maxEdge: Int = 1024): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxEdge && h <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    /**
     * Loads a Bitmap from a file path, with efficient sub-sampling so large
     * photos don't cause OOM. Always returns ≤ maxEdge on each side.
     */
    fun loadBitmapFromPath(path: String, maxEdge: Int = 1280): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        opts.inSampleSize  = calculateSampleSize(opts.outWidth, opts.outHeight, maxEdge, maxEdge)
        opts.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, opts)
    }

    /**
     * Loads a Bitmap from a content URI.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri, maxEdge: Int = 1280): Bitmap? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val tmp    = BitmapFactory.decodeStream(stream)
            stream.close()
            scaleBitmap(tmp, maxEdge)
        } catch (e: Exception) { null }
    }

    /**
     * Crops a normalised rectangle (0.0–1.0) from a full bitmap.
     * Used by the OCR drag-to-select feature.
     */
    fun cropNormalized(
        bitmap: Bitmap,
        left:   Float, top: Float,
        right:  Float, bottom: Float
    ): Bitmap {
        val x = (left   * bitmap.width).toInt().coerceIn(0, bitmap.width  - 1)
        val y = (top    * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val w = ((right  - left)  * bitmap.width).toInt().coerceAtLeast(1)
        val h = ((bottom - top)   * bitmap.height).toInt().coerceAtLeast(1)
        val safeW = w.coerceAtMost(bitmap.width  - x)
        val safeH = h.coerceAtMost(bitmap.height - y)
        return Bitmap.createBitmap(bitmap, x, y, safeW, safeH)
    }

    /**
     * Saves a bitmap to the app's cache directory and returns the file path.
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, name: String? = null): String {
        val file = File(context.cacheDir, name ?: "sv_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return file.absolutePath
    }

    private fun calculateSampleSize(w: Int, h: Int, reqW: Int, reqH: Int): Int {
        var sample = 1
        if (h > reqH || w > reqW) {
            val halfH = h / 2
            val halfW = w / 2
            while (halfH / sample >= reqH && halfW / sample >= reqW) sample *= 2
        }
        return sample
    }
}
