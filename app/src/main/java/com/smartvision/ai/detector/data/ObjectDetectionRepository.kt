package com.smartvision.ai.detector.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.smartvision.ai.detector.domain.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ─── ML Kit Object Detection Repository ──────────────────────────────────────
@Singleton
class ObjectDetectionRepository @Inject constructor() {

    /** Streaming / live camera detector — optimised for speed */
    private val streamDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    /** Single-image detector — accurate for gallery/captured images */
    private val singleDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    /**
     * Detect objects from a live camera [Image] (YUV_420_888).
     * Uses STREAM_MODE for low-latency processing.
     */
    suspend fun detectFromImage(image: Image, rotation: Int): Result<List<DetectedItem>> =
        runCatching {
            val input   = InputImage.fromMediaImage(image, rotation)
            val results = streamDetector.process(input).await()
            results.mapIndexedNotNull { idx, obj -> obj.toDetectedItem(idx, image.width, image.height) }
        }

    /**
     * Detect objects from a gallery [Uri].
     * Uses SINGLE_IMAGE_MODE for maximum accuracy.
     */
    suspend fun detectFromUri(context: Context, uri: Uri): Result<List<DetectedItem>> =
        runCatching {
            val input   = InputImage.fromFilePath(context, uri)
            val results = singleDetector.process(input).await()
            val bitmap  = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
            results.mapIndexedNotNull { idx, obj ->
                obj.toDetectedItem(idx, bitmap?.width ?: 1, bitmap?.height ?: 1)
            }
        }

    /**
     * Detect objects from an in-memory [Bitmap].
     */
    suspend fun detectFromBitmap(bitmap: Bitmap): Result<List<DetectedItem>> =
        runCatching {
            val input   = InputImage.fromBitmap(bitmap, 0)
            val results = singleDetector.process(input).await()
            results.mapIndexedNotNull { idx, obj ->
                obj.toDetectedItem(idx, bitmap.width, bitmap.height)
            }
        }

    // ── Private mapper ────────────────────────────────────────────────────────
    private fun DetectedObject.toDetectedItem(
        idx:    Int,
        imgW:   Int,
        imgH:   Int
    ): DetectedItem? {
        val topLabel    = labels.maxByOrNull { it.confidence } ?: return null
        val label       = topLabel.text.ifBlank { "Object ${idx + 1}" }
        val category    = label.toCategory()
        val bb          = boundingBox
        val w           = imgW.toFloat().coerceAtLeast(1f)
        val h           = imgH.toFloat().coerceAtLeast(1f)

        return DetectedItem(
            id          = trackingId ?: idx,
            label       = label,
            confidence  = topLabel.confidence,
            box         = DetectionBox(
                left   = (bb.left   / w).coerceIn(0f, 1f),
                top    = (bb.top    / h).coerceIn(0f, 1f),
                right  = (bb.right  / w).coerceIn(0f, 1f),
                bottom = (bb.bottom / h).coerceIn(0f, 1f)
            ),
            category    = category,
            accentColor = category.colorArgb
        )
    }
}
