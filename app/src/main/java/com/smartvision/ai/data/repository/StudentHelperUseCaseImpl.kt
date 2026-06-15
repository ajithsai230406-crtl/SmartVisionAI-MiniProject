package com.smartvision.ai.data.repository

import android.graphics.Bitmap
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.domain.usecase.StudentHelperUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentHelperUseCaseImpl @Inject constructor(
    private val openRouterManager: OpenRouterManager
) : StudentHelperUseCase {

    override suspend fun invoke(questionBitmap: Bitmap): ScanResult = try {
        val responseText = openRouterManager.generateContent(
            prompt = "You are a helpful tutor. Analyze this question. Provide a clear explanation and step-by-step solution.",
            image = questionBitmap,
            modelName = "openrouter/auto"
        )
        ScanResult.StudentHelperResult(
            question      = "From image",
            aiExplanation = responseText,
            steps         = emptyList()
        )
    } catch (e: Exception) {
        val offlineMsg = if (e is OpenRouterError.Offline || e is OpenRouterError.NetworkTimeout) {
            "⚠️ Offline Mode: AI explanation is unavailable. Please check your internet connection and try again."
        } else {
            e.message ?: "AI failed"
        }
        ScanResult.Error(offlineMsg)
    }

    override suspend fun explainText(text: String): ScanResult = try {
        val responseText = openRouterManager.generateContent(
            prompt = "Explain clearly and step-by-step: $text",
            modelName = "openrouter/auto"
        )
        ScanResult.StudentHelperResult(
            question      = text,
            aiExplanation = responseText,
            steps         = emptyList()
        )
    } catch (e: Exception) {
        val offlineMsg = if (e is OpenRouterError.Offline || e is OpenRouterError.NetworkTimeout) {
            "⚠️ Offline Mode: AI explanation is unavailable. Please check your internet connection and try again."
        } else {
            e.message ?: "AI failed"
        }
        ScanResult.Error(offlineMsg)
    }
}
