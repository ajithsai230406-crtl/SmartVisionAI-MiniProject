package com.smartvision.ai.detector.data

import android.graphics.Bitmap
import android.media.Image
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.smartvision.ai.detector.domain.DetectedItem
import com.smartvision.ai.detector.domain.DetectionBox
import com.smartvision.ai.detector.domain.toCategory
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ML Kit Object Detection repository — uses the on-device ML Kit model with STREAM_MODE
 * and ImageLabeler fallback for real-time object detection and classification.
 */
@Singleton
class MlKitObjectDetectionRepository @Inject constructor() {

    // Stream mode: optimized for continuous camera feed analysis
    private val streamDetector: ObjectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()          // returns category labels
            .enableMultipleObjects()         // detect up to 5 objects per frame
            .build()
        ObjectDetection.getClient(options)
    }

    // Single-image mode: for gallery/captured images (higher accuracy)
    private val singleDetector: ObjectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableClassification()
            .enableMultipleObjects()
            .build()
        ObjectDetection.getClient(options)
    }

    // Image Labeler: fallback for high-quality specific object classification on cropped boxes
    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Detect objects from a live camera [Image] in stream mode.
     */
    suspend fun detectFromLiveFrame(image: Image, rotationDegrees: Int): Result<List<DetectedItem>> =
        withContext(Dispatchers.Default) {
            runCatching {
                val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
                val results = streamDetector.process(inputImage).await()
                mapResults(results, inputImage.width.toFloat(), inputImage.height.toFloat(), null)
            }
        }

    /**
     * Detect objects from a captured [Bitmap] in single-image mode.
     */
    suspend fun detectFromBitmap(bitmap: Bitmap): Result<List<DetectedItem>> =
        withContext(Dispatchers.Default) {
            runCatching {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val results = singleDetector.process(inputImage).await()
                mapResults(results, bitmap.width.toFloat(), bitmap.height.toFloat(), bitmap)
            }
        }

    private suspend fun mapResults(
        results: List<DetectedObject>,
        imgW: Float,
        imgH: Float,
        bitmap: Bitmap?
    ): List<DetectedItem> {
        val detectedItems = mutableListOf<DetectedItem>()
        for ((idx, obj) in results.withIndex()) {
            val bbox = obj.boundingBox

            // Normalize coordinates
            val left   = (bbox.left.toFloat() / imgW).coerceIn(0f, 1f)
            val top    = (bbox.top.toFloat() / imgH).coerceIn(0f, 1f)
            val right  = (bbox.right.toFloat() / imgW).coerceIn(0f, 1f)
            val bottom = (bbox.bottom.toFloat() / imgH).coerceIn(0f, 1f)

            var finalLabel = "Object"
            var confidence = 0.75f

            // 1. Check if ML Kit already has a specific label
            val bestLabel = obj.labels.maxByOrNull { it.confidence }
            if (bestLabel != null) {
                val mapped = mapToTargetClass(bestLabel.text)
                if (mapped != null) {
                    finalLabel = mapped
                    confidence = bestLabel.confidence
                } else {
                    finalLabel = mapMlKitLabel(bestLabel.text)
                    confidence = bestLabel.confidence
                }
            }

            // 2. Crop and refine classification using on-device ImageLabeler if generic
            if (bitmap != null && (finalLabel == "Object" || finalLabel == "Household Object" || finalLabel == "Clothing / Fashion Item" || finalLabel == "Food Item")) {
                val cropped = cropBitmap(bitmap, bbox)
                if (cropped != null) {
                    try {
                        val cropInputImage = InputImage.fromBitmap(cropped, 0)
                        val labels = labeler.process(cropInputImage).await()
                        val bestCropLabel = labels.maxByOrNull { it.confidence }
                        if (bestCropLabel != null) {
                            val mapped = mapToTargetClass(bestCropLabel.text)
                            if (mapped != null) {
                                finalLabel = mapped
                                confidence = bestCropLabel.confidence
                            } else {
                                finalLabel = bestCropLabel.text.replaceFirstChar { it.uppercase() }
                                confidence = bestCropLabel.confidence
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MlKitObjectDetector", "ImageLabeling crop failed", e)
                    }
                }
            }

            val category = finalLabel.toCategory()

            detectedItems.add(
                DetectedItem(
                    id         = idx,
                    label      = finalLabel,
                    confidence = confidence,
                    box        = DetectionBox(left, top, right, bottom),
                    category   = category,
                    accentColor = category.colorArgb
                )
            )
        }
        return detectedItems
    }

    private fun cropBitmap(src: Bitmap, rect: android.graphics.Rect): Bitmap? {
        val left = rect.left.coerceIn(0, src.width - 1)
        val top = rect.top.coerceIn(0, src.height - 1)
        val right = rect.right.coerceIn(0, src.width)
        val bottom = rect.bottom.coerceIn(0, src.height)
        val width = right - left
        val height = bottom - top
        if (width <= 0 || height <= 0) return null
        return Bitmap.createBitmap(src, left, top, width, height)
    }

    private fun mapToTargetClass(label: String): String? {
        val lower = label.lowercase().trim()
        return when {
            lower.contains("bottle") -> "Bottle"
            lower.contains("phone") || lower.contains("mobile") || lower.contains("cellular") || lower.contains("telephone") -> "Mobile Phone"
            lower.contains("laptop") || lower.contains("notebook") || (lower.contains("computer") && !lower.contains("keyboard") && !lower.contains("mouse") && !lower.contains("monitor")) -> "Laptop"
            lower.contains("keyboard") -> "Keyboard"
            lower.contains("mouse") -> "Mouse"
            lower.contains("book") || lower.contains("novel") || lower.contains("textbook") -> "Book"
            lower.contains("person") || lower.contains("man") || lower.contains("woman") || lower.contains("child") || lower.contains("human") -> "Person"
            lower.contains("chair") || lower.contains("stool") || lower.contains("seat") -> "Chair"
            lower.contains("table") || lower.contains("desk") -> "Table"
            lower.contains("cup") || lower.contains("mug") || lower.contains("glass") || lower.contains("tumbler") -> "Cup"
            lower.contains("monitor") || lower.contains("screen") || lower.contains("display") -> "Monitor"
            lower.contains("television") || lower.contains("tv") -> "Television"
            lower.contains("bag") || lower.contains("handbag") || lower.contains("backpack") || lower.contains("suitcase") -> "Bag"
            else -> null
        }
    }

    /**
     * Maps ML Kit's generic category labels to user-friendly names.
     */
    private fun mapMlKitLabel(rawLabel: String): String = when (rawLabel.lowercase().trim()) {
        "fashion"    -> "Clothing / Fashion Item"
        "food"       -> "Food Item"
        "home goods" -> "Household Object"
        "plants"     -> "Plant"
        "places"     -> "Location / Scene"
        "animals"    -> "Animal"
        "vehicles"   -> "Vehicle"
        "person"     -> "Person"
        else         -> rawLabel
    }
}
