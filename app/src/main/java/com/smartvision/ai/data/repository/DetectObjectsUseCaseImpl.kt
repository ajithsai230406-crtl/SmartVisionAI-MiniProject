package com.smartvision.ai.data.repository

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.domain.usecase.DetectObjectsUseCase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetectObjectsUseCaseImpl @Inject constructor() : DetectObjectsUseCase {

    private val detector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    override suspend fun invoke(bitmap: Bitmap): ScanResult {
        return try {
            val image  = InputImage.fromBitmap(bitmap, 0)
            val result = detector.process(image).await()
            val w = bitmap.width.toFloat()
            val h = bitmap.height.toFloat()
            val objects = result.map { obj ->
                val label = obj.labels.maxByOrNull { it.confidence }
                val bb    = obj.boundingBox
                DetectedObject(
                    label       = label?.text ?: "Unknown",
                    confidence  = label?.confidence ?: 0f,
                    boundingBox = BoundingBox(bb.left/w, bb.top/h, bb.right/w, bb.bottom/h)
                )
            }
            ScanResult.ObjectDetectionResult(objects)
        } catch (e: Exception) { ScanResult.Error(e.message ?: "Detection failed") }
    }
}
