package com.smartvision.ai.data.repository

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.smartvision.ai.BuildConfig
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.domain.usecase.MedicalScanUseCase
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicalScanUseCaseImpl @Inject constructor() : MedicalScanUseCase {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey    = BuildConfig.GEMINI_API_KEY
    )

    override suspend fun invoke(bitmap: Bitmap): ScanResult {
        return try {
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text(
                        """Analyze this medicine/drug packaging or strip. Extract and return JSON ONLY (no markdown, no backticks):
{
  "medicineName": "Full medicine name",
  "usage": "What this medicine treats",
  "dosage": "Recommended dosage",
  "sideEffects": ["side effect 1", "side effect 2"],
  "warnings": ["warning 1", "warning 2"]
}
If this is not a medicine image, return: {"error": "Not a medicine image"}"""
                    )
                }
            )

            val raw  = response.text?.trim() ?: return ScanResult.Error("No response")
            val json = JSONObject(raw)

            if (json.has("error")) {
                return ScanResult.Error(json.getString("error"))
            }

            val sideEffects = json.optJSONArray("sideEffects")
                ?.let { arr -> (0 until arr.length()).map { arr.getString(it) } }
                ?: emptyList()

            val warnings = json.optJSONArray("warnings")
                ?.let { arr -> (0 until arr.length()).map { arr.getString(it) } }
                ?: emptyList()

            ScanResult.MedicalScanResult(
                medicineName = json.optString("medicineName", "Unknown medicine"),
                usage        = json.optString("usage",   "Not available"),
                dosage       = json.optString("dosage",  "Consult a doctor"),
                sideEffects  = sideEffects,
                warnings     = warnings
            )
        } catch (e: Exception) {
            ScanResult.Error("Medical scan failed: ${e.message}")
        }
    }
}
