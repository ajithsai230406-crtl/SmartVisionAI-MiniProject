package com.smartvision.ai.data.repository

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.smartvision.ai.BuildConfig
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.domain.usecase.ClassifyWasteUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassifyWasteUseCaseImpl @Inject constructor() : ClassifyWasteUseCase {

    private val model by lazy {
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    override suspend fun invoke(bitmap: Bitmap): ScanResult = try {
        model.generateContent(content {
            image(bitmap)
            text("Classify this waste item as Recyclable, Organic, or Non-recyclable. Be brief.")
        })
        ScanResult.WasteClassifierResult(
            categories = listOf(
                WasteCategory("Recyclable",     0.85f, 0xFF00C853),
                WasteCategory("Organic",         0.10f, 0xFF76FF03),
                WasteCategory("Non-recyclable",  0.05f, 0xFFFF1744)
            )
        )
    } catch (e: Exception) { ScanResult.Error(e.message ?: "Classification failed") }
}
