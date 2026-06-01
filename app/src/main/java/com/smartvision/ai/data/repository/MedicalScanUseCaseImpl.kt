package com.smartvision.ai.data.repository

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.smartvision.ai.BuildConfig
import com.smartvision.ai.data.db.GeminiCacheDao
import com.smartvision.ai.data.db.GeminiCacheEntity
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.domain.usecase.MedicalScanUseCase
import com.smartvision.ai.util.retryWithDelay
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicalScanUseCaseImpl @Inject constructor(
    private val cacheDao: GeminiCacheDao
) : MedicalScanUseCase {

    private val model by lazy {
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    private fun getBitmapHash(bitmap: Bitmap): Int {
        var hash = 17
        val w = bitmap.width
        val h = bitmap.height
        if (w > 0 && h > 0) {
            hash = 31 * hash + bitmap.getPixel(w / 4, h / 4)
            hash = 31 * hash + bitmap.getPixel(w / 2, h / 2)
            hash = 31 * hash + bitmap.getPixel(3 * w / 4, 3 * h / 4)
            hash = 31 * hash + w
            hash = 31 * hash + h
        }
        return hash
    }

    override suspend fun invoke(bitmap: Bitmap): ScanResult = try {
        val promptText = """Analyze this medicine packaging. Return JSON only, no markdown:
{"medicineName":"","usage":"","dosage":"","sideEffects":[],"warnings":[]}
If not medicine: {"error":"Not a medicine image"}"""

        val imgHash = getBitmapHash(bitmap)
        val cacheKey = promptText.hashCode() xor imgHash

        // Check local Room cache first
        val cached = cacheDao.getCache(cacheKey)
        val raw = if (cached != null) {
            cached.response
        } else {
            // Exponential backoff remote retry call
            val response = retryWithDelay(retries = 3) {
                model.generateContent(content {
                    image(bitmap)
                    text(promptText)
                })
            }
            val jsonText = response.text?.trim() ?: throw Exception("No response")
            cacheDao.insertCache(GeminiCacheEntity(cacheKey, "MedicineImageHash:$imgHash", jsonText))
            jsonText
        }

        // Fortified JSON parser cleanup
        val cleaned = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(cleaned)
        
        if (json.has("error")) {
            ScanResult.Error(json.getString("error"))
        } else {
            ScanResult.MedicalScanResult(
                medicineName = json.optString("medicineName", "Unknown"),
                usage        = json.optString("usage",   "Not available"),
                dosage       = json.optString("dosage",  "Consult a doctor"),
                sideEffects  = json.optJSONArray("sideEffects")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
                warnings     = json.optJSONArray("warnings")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            )
        }
    } catch (e: Exception) { 
        ScanResult.Error(e.message ?: "Medical scan failed") 
    }
}
