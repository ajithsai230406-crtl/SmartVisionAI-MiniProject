package com.smartvision.ai.data.repository

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.smartvision.ai.BuildConfig
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.domain.usecase.ClassifyWasteUseCase
import com.smartvision.ai.util.retryWithDelay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassifyWasteUseCaseImpl @Inject constructor() : ClassifyWasteUseCase {

    private val model by lazy {
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    override suspend fun invoke(bitmap: Bitmap): ScanResult = try {
        val promptText = "Classify this waste item as Recyclable, Organic, or Non-recyclable. Be brief."
        
        val response = retryWithDelay(retries = 3) {
            model.generateContent(content {
                image(bitmap)
                text(promptText)
            })
        }
        
        val reply = response.text?.lowercase() ?: ""
        val (rec, org, nonRec) = when {
            "non-recyclable" in reply || "non recyclable" in reply || "trash" in reply || "landfill" in reply || "hazardous" in reply -> Triple(0.10f, 0.05f, 0.85f)
            "organic" in reply || "food" in reply || "compost" in reply || "fruit" in reply || "vegetable" in reply || "leaf" in reply || "plant" in reply -> Triple(0.05f, 0.85f, 0.10f)
            else -> Triple(0.85f, 0.10f, 0.05f) // Recyclable default (plastic, metal, paper, glass)
        }

        ScanResult.WasteClassifierResult(
            categories = listOf(
                WasteCategory("Recyclable",     rec, 0xFF00C853),
                WasteCategory("Organic",         org, 0xFF76FF03),
                WasteCategory("Non-recyclable",  nonRec, 0xFFFF1744)
            )
        )
    } catch (e: Exception) { 
        ScanResult.Error(e.message ?: "Classification failed") 
    }
}
