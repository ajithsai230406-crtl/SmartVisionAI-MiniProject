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

    private val model by lazy {
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    override suspend fun invoke(bitmap: Bitmap): ScanResult {
        return try {
            val response = model.generateContent(content {
                image(bitmap)
                text("""Analyze this medicine packaging. Return JSON only, no markdown:
{"medicineName":"","usage":"","dosage":"","sideEffects":[],"warnings":[]}
If not medicine: {"error":"Not a medicine image"}""")
            })
            val raw  = response.text?.trim() ?: return ScanResult.Error("No response")
            val json = JSONObject(raw)
            if (json.has("error")) return ScanResult.Error(json.getString("error"))
            ScanResult.MedicalScanResult(
                medicineName = json.optString("medicineName", "Unknown"),
                usage        = json.optString("usage",   "Not available"),
                dosage       = json.optString("dosage",  "Consult a doctor"),
                sideEffects  = json.optJSONArray("sideEffects")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
                warnings     = json.optJSONArray("warnings")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            )
        } catch (e: Exception) { ScanResult.Error(e.message ?: "Medical scan failed") }
    }
}
