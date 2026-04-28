package com.smartvision.ai.data.repository

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.smartvision.ai.BuildConfig
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.domain.usecase.StudentHelperUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentHelperUseCaseImpl @Inject constructor() : StudentHelperUseCase {

    private val model by lazy {
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    override suspend fun invoke(questionBitmap: Bitmap): ScanResult {
        return try {
            val response = model.generateContent(
                content {
                    image(questionBitmap)
                    text(
                        "You are a helpful tutor. Analyze this question. " +
                        "Return JSON only: {\"question\":\"...\",\"explanation\":\"...\",\"steps\":[\"...\"]}"
                    )
                }
            )
            ScanResult.StudentHelperResult(
                question      = "From image",
                aiExplanation = response.text ?: "No response",
                steps         = emptyList()
            )
        } catch (e: Exception) { ScanResult.Error(e.message ?: "AI failed") }
    }

    override suspend fun explainText(text: String): ScanResult {
        return try {
            val response = model.generateContent(
                content { text("Explain clearly and step-by-step: $text") }
            )
            ScanResult.StudentHelperResult(
                question      = text,
                aiExplanation = response.text ?: "No explanation",
                steps         = emptyList()
            )
        } catch (e: Exception) { ScanResult.Error(e.message ?: "AI failed") }
    }
}
