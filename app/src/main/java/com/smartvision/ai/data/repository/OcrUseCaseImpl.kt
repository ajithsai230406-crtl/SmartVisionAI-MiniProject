package com.smartvision.ai.data.repository

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.domain.usecase.OcrUseCase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrUseCaseImpl @Inject constructor() : OcrUseCase {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun invoke(bitmap: Bitmap): ScanResult = try {
        val image  = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        val w = bitmap.width.toFloat(); val h = bitmap.height.toFloat()
        val blocks = result.textBlocks.map { block ->
            val bb = block.boundingBox ?: Rect()
            TextBlock(block.text, BoundingBox(bb.left/w, bb.top/h, bb.right/w, bb.bottom/h))
        }
        ScanResult.OcrResult(rawText = result.text, textBlocks = blocks)
    } catch (e: Exception) { ScanResult.Error(e.message ?: "OCR failed") }

    override suspend fun extractFromArea(bitmap: Bitmap, area: Rect): ScanResult {
        val cropped = Bitmap.createBitmap(bitmap, area.left, area.top, area.width(), area.height())
        return invoke(cropped)
    }
}
