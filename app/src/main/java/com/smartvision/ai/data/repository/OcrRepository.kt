package com.smartvision.ai.data.repository

import android.content.Context
import android.media.Image
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrRepository @Inject constructor() {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(image: Image, rotationDegrees: Int): Result<String> = runCatching {
        val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
        val result = recognizer.process(inputImage).await()
        result.text.ifBlank { "No readable text found. Try better lighting and hold the camera steady." }
    }

    suspend fun recognizeUri(context: Context, uri: Uri): Result<String> = runCatching {
        val inputImage = InputImage.fromFilePath(context, uri)
        val result = recognizer.process(inputImage).await()
        result.text.ifBlank { "No readable text found in selected image." }
    }
}
