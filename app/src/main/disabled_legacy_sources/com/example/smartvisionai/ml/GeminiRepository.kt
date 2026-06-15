package com.example.smartvisionai.ml

import com.smartvision.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor() {

    private val client = OkHttpClient()

    private suspend fun askAI(prompt: String): String =
        withContext(Dispatchers.IO) {

            try {

                val json = JSONObject().apply {

                    put(
                        "model",
                        "openrouter/auto"
                    )

                    put(
                        "messages",
                        JSONArray().put(
                            JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            }
                        )
                    )
                }
                android.util.Log.d(
                    "OPENROUTER_MODEL",
                    json.toString()
                )

                val body =
                    json.toString()
                        .toRequestBody(
                            "application/json"
                                .toMediaType()
                        )

                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .addHeader(
                        "Authorization",
                        "Bearer ${BuildConfig.OPENROUTER_API_KEY}"
                    )
                    .addHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .post(body)
                    .build()

                val response =
                    client.newCall(request).execute()

                val responseBody =
                    response.body?.string()
                        ?: return@withContext "No response"

                JSONObject(responseBody)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

            } catch (e: Exception) {

                "AI Error: ${e.message}"
            }
        }

    suspend fun explainForStudent(
        questionText: String
    ): String {

        val prompt = """
            You are a friendly student assistant.

            Question:

            $questionText

            Provide:
            1. Direct answer
           2. Step-by-step Solution
           3. Explanation
           4. Formula Used
           5. Example
           6. Final Answer

           Make the response easy for engineering students.
        """.trimIndent()

        return askAI(prompt)
    }

    suspend fun analyzeMedicine(
        medicineText: String
    ): String {

        val prompt = """
            Analyze this medicine text:

            $medicineText

            Provide:
            • Name
            • Uses
            • Precautions
            • Doctor consultation advice
        """.trimIndent()

        return askAI(prompt)
    }

    suspend fun classifyWaste(
        detectedLabels: String
    ): String {

        val prompt = """
            Classify this waste:

            $detectedLabels

            Give:
            • Category
            • Reason
            • Disposal instructions
        """.trimIndent()

        return askAI(prompt)
    }

    suspend fun chat(
        userMessage: String
    ): String {

        return askAI(userMessage)
    }
}