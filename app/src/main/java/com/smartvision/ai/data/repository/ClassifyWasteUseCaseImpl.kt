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
        
        val responseText = try {
            val response = retryWithDelay(retries = 3) {
                model.generateContent(content {
                    image(bitmap)
                    text(promptText)
                })
            }
            response.text ?: "recyclable"
        } catch (e: Exception) {
            // Local fallback classification based on average bitmap colors
            val w = bitmap.width
            val h = bitmap.height
            var rSum = 0L; var gSum = 0L; var bSum = 0L
            val samplePoints = listOf(
                Pair(w/4, h/4), Pair(w/2, h/4), Pair(3*w/4, h/4),
                Pair(w/4, h/2), Pair(w/2, h/2), Pair(3*w/4, h/2),
                Pair(w/4, 3*h/4), Pair(w/2, 3*h/4), Pair(3*w/4, 3*h/4)
            )
            for (p in samplePoints) {
                val pixel = if (w > 0 && h > 0) bitmap.getPixel(p.first, p.second) else 0
                rSum += (pixel shr 16) and 0xFF
                gSum += (pixel shr 8) and 0xFF
                bSum += pixel and 0xFF
            }
            val avgR = rSum / samplePoints.size
            val avgG = gSum / samplePoints.size
            val avgB = bSum / samplePoints.size

            when {
                avgG > avgR + 10 && avgG > avgB + 10 -> "organic"
                avgR < 50 && avgG < 50 && avgB < 50 -> "non-recyclable"
                else -> "recyclable"
            }
        }
        
        val reply = responseText.lowercase()
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
