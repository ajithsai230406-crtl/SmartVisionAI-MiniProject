package com.smartvision.ai.domain

class GeminiAssistant {
    fun reply(input: String): String {
        val command = input.lowercase()
        return when {
            "ocr" in command || "read" in command || "scan text" in command ->
                "Opening OCR scanner so I can read the text for you."
            "translate" in command ->
                "Opening translation mode with camera and voice options."
            "medicine" in command ->
                "Opening medicine identifier. Scan the strip or bottle label clearly."
            "waste" in command || "plastic" in command ->
                "Opening waste classifier for dry, wet, plastic, and metal detection."
            "object" in command || "detect" in command ->
                "Opening object detection with AI bounding boxes."
            else ->
                "I am your Gemini-powered SmartVision assistant placeholder. Connect your Gemini API key to generate live responses."
        }
    }
}
