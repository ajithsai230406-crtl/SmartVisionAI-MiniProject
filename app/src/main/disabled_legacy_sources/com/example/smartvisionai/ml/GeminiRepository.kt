package com.example.smartvisionai.ml

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.smartvision.ai.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor() {

    private val model = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    // ── Student Helper ────────────────────────────────────────────────────────
    suspend fun explainForStudent(questionText: String): String {
        val prompt = """
            You are a friendly student assistant. A student has scanned this text:
            
            "$questionText"
            
            Please:
            1. Identify if this is a question, formula, or concept
            2. Provide a clear, simple explanation
            3. Give step-by-step solution if it's a problem
            4. Use simple language suitable for students
            
            Keep the response concise and well-structured.
        """.trimIndent()

        return try {
            val response = model.generateContent(content { text(prompt) })
            response.text ?: "Could not generate explanation."
        } catch (e: Exception) {
            "⚠ Gemini unavailable. Check your API key and internet connection.\n\nScanned text:\n$questionText"
        }
    }

    // ── Medicine Analysis ─────────────────────────────────────────────────────
    suspend fun analyzeMedicine(medicineText: String): String {
        val prompt = """
            You are a medical information assistant. Analyze this text scanned from a medicine label/strip:
            
            "$medicineText"
            
            Provide ONLY general educational information:
            • Medicine name (if identifiable)
            • Common uses
            • General precautions
            • Important: Always advise to consult a doctor
            
            ⚠ Note: This is for educational purposes only. Always consult a licensed healthcare professional.
        """.trimIndent()

        return try {
            val response = model.generateContent(content { text(prompt) })
            response.text ?: "Could not analyze medicine information."
        } catch (e: Exception) {
            "⚠ Could not analyze. Scanned text:\n$medicineText\n\n⚠ Always consult a doctor before taking any medicine."
        }
    }

    // ── Waste Classification ──────────────────────────────────────────────────
    suspend fun classifyWaste(detectedLabels: String): String {
        val prompt = """
            Based on these detected items: "$detectedLabels"
            
            Classify the waste type and provide disposal instructions:
            
            Categories:
            • ♻ Recyclable (plastic, metal, glass, paper)
            • 🌿 Organic (food waste, plant material)
            • 🗑 Non-recyclable (mixed materials, contaminated items)
            • ⚠ Hazardous (batteries, electronics, chemicals)
            
            Provide: Category, Why, How to dispose properly.
        """.trimIndent()

        return try {
            val response = model.generateContent(content { text(prompt) })
            response.text ?: "Classification unavailable."
        } catch (e: Exception) {
            "⚠ Classification unavailable offline.\nDetected: $detectedLabels"
        }
    }

    // ── General Chat ──────────────────────────────────────────────────────────
    suspend fun chat(userMessage: String): String {
        val prompt = """
            You are Smart Vision AI, an intelligent mobile assistant that helps users understand 
            their surroundings through camera vision. Answer helpfully and concisely.
            
            User: $userMessage
        """.trimIndent()

        return try {
            val response = model.generateContent(content { text(prompt) })
            response.text ?: "I couldn't generate a response."
        } catch (e: Exception) {
            "⚠ AI unavailable. Check internet connection."
        }
    }
}
