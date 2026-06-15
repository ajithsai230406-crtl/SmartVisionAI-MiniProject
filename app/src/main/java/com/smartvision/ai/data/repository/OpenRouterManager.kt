package com.smartvision.ai.data.repository

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.smartvision.ai.BuildConfig
import com.smartvision.ai.util.NetworkMonitor
import com.smartvision.ai.util.retryWithDelay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class OpenRouterError : Exception() {
    object MissingApiKey : OpenRouterError() {
        override val message: String = "API Key Missing: Please check local.properties and define OPENROUTER_API_KEY."
    }

    class InvalidApiKey(val details: String) : OpenRouterError() {
        override val message: String = "API Key Error: The provided OpenRouter API key is invalid. Details: $details"
    }
    class BillingDisabled(val details: String) : OpenRouterError() {
        override val message: String = "Billing Error: Billing is not enabled or project quota is restricted. Details: $details"
    }
    class RateLimitExceeded(val details: String) : OpenRouterError() {
        override val message: String = "Quota Error: Rate limit or quota exceeded. Please try again later. Details: $details"
    }
    class InvalidRequest(val details: String) : OpenRouterError() {
        override val message: String = "Invalid Request Error: The request parameters or model name are invalid. Details: $details"
    }
    object Offline : OpenRouterError() {
        override val message: String = "No Internet Connection. Please check your network status."
    }
    class NetworkTimeout(val details: String) : OpenRouterError() {
        override val message: String = "Network Error: Request timed out or internet connection lost. Details: $details"
    }
    class Unknown(val details: String, override val cause: Throwable? = null) : OpenRouterError() {
        override val message: String = "OpenRouter API Error: $details"
    }
}

@Singleton
class OpenRouterManager @Inject constructor(
    private val networkMonitor: NetworkMonitor
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        if (apiKey.isBlank()) {
            throw OpenRouterError.MissingApiKey
        }
        return apiKey
    }

    private fun buildRequestBody(prompt: String, image: Bitmap?, modelName: String = "openrouter/auto", stream: Boolean = false): String {
        val json = JSONObject()
        json.put("model", "openrouter/auto")
        if (stream) {
            json.put("stream", true)
        }

        val messages = JSONArray()
        val message = JSONObject()
        message.put("role", "user")

        if (image != null) {
            val contentArray = JSONArray()
            val textContent = JSONObject()
            textContent.put("type", "text")
            textContent.put("text", prompt)
            contentArray.put(textContent)

            val imageContent = JSONObject()
            imageContent.put("type", "image_url")
            val imageUrl = JSONObject()

            val baos = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            imageUrl.put("url", "data:image/jpeg;base64,$base64")
            imageContent.put("image_url", imageUrl)

            contentArray.put(imageContent)
            message.put("content", contentArray)
        } else {
            message.put("content", prompt)
        }

        messages.put(message)
        json.put("messages", messages)
        return json.toString()
    }

    suspend fun generateContent(
        prompt: String,
        image: Bitmap? = null,
        modelName: String = "openrouter/auto"
    ): String = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val hasImage = image != null
        val targetModel = "openrouter/auto"
        Log.i("OpenRouterManager", "[Request] Model: $targetModel | Prompt Length: ${prompt.length} | Has Image: $hasImage")

        if (!networkMonitor.isOnline()) {
            Log.w("OpenRouterManager", "[Request Failed] Device is offline.")
            throw OpenRouterError.Offline
        }

        val apiKey = getApiKey()
        val requestBodyString = buildRequestBody(prompt, image, modelName, stream = false)
        val json = JSONObject(requestBodyString)
        Log.d("OPENROUTER_MODEL", json.toString())
        val requestBody = requestBodyString.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://github.com/ajithsai230406-crtl/SmartVisionAI-MiniProject")
            .addHeader("X-Title", "Smart Vision AI")
            .build()

        try {
            withTimeout(45000) {
                retryWithDelay(retries = 3) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errorMsg = response.body?.string() ?: ""
                            throw Exception("HTTP Code: ${response.code} | Response: $errorMsg")
                        }
                        val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                        val responseJson = JSONObject(responseBody)
                        val choices = responseJson.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val message = choice.optJSONObject("message")
                            val content = message?.optString("content")
                            if (!content.isNullOrBlank()) {
                                return@retryWithDelay content
                            }
                        }
                        throw Exception("Failed to parse response content")
                    }
                }
            }
        } catch (e: Throwable) {
            val mapped = mapException(e)
            val latency = System.currentTimeMillis() - startTime
            Log.e("OpenRouterManager", "[Request Failed] Model: $targetModel | Latency: ${latency}ms | Error: ${mapped.message}", e)
            throw mapped
        }
    }

    fun generateContentStream(
        prompt: String,
        modelName: String = "openrouter/auto"
    ): Flow<String> = flow {
        val startTime = System.currentTimeMillis()
        val targetModel = "openrouter/auto"
        Log.i("OpenRouterManager", "[Stream Request] Model: $targetModel | Prompt Length: ${prompt.length}")

        if (!networkMonitor.isOnline()) {
            Log.w("OpenRouterManager", "[Stream Request Failed] Device is offline.")
            throw OpenRouterError.Offline
        }

        val apiKey = getApiKey()
        val requestBodyString = buildRequestBody(prompt, null, modelName, stream = true)
        val json = JSONObject(requestBodyString)
        Log.d("OPENROUTER_MODEL", json.toString())
        val requestBody = requestBodyString.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://github.com/ajithsai230406-crtl/SmartVisionAI-MiniProject")
            .addHeader("X-Title", "Smart Vision AI")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string() ?: ""
                    throw Exception("HTTP Code: ${response.code} | Response: $errorMsg")
                }

                response.body?.charStream()?.buffered()?.use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line!!.trim()
                        if (trimmed.startsWith("data: ")) {
                            val data = trimmed.removePrefix("data: ").trim()
                            if (data == "[DONE]") break
                            try {
                                val json = JSONObject(data)
                                val choices = json.optJSONArray("choices")
                                if (choices != null && choices.length() > 0) {
                                    val delta = choices.getJSONObject(0).optJSONObject("delta")
                                    val content = delta?.optString("content")
                                    if (!content.isNullOrEmpty()) {
                                        emit(content)
                                    }
                                }
                            } catch (e: Exception) {
                                // Ignore json parsing errors on non-json lines
                            }
                        }
                    }
                }
            }
            val latency = System.currentTimeMillis() - startTime
            Log.i("OpenRouterManager", "[Stream Success] Model: $modelName | Total Latency: ${latency}ms")
        } catch (e: Throwable) {
            val mapped = mapException(e)
            val latency = System.currentTimeMillis() - startTime
            Log.e("OpenRouterManager", "[Stream Request Failed] Model: $modelName | Latency: ${latency}ms | Error: ${mapped.message}", e)
            throw mapped
        }
    }.flowOn(Dispatchers.IO)

    private fun mapException(e: Throwable): Throwable {
        if (e is OpenRouterError) return e

        val message = e.message ?: ""

        if (e is java.io.IOException ||
            e is java.net.SocketTimeoutException ||
            e is java.net.UnknownHostException ||
            e is kotlinx.coroutines.TimeoutCancellationException ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("connect", ignoreCase = true)) {
            return OpenRouterError.NetworkTimeout(message)
        }

        return when {
            message.contains("API_KEY_INVALID", ignoreCase = true) ||
            message.contains("API key not valid", ignoreCase = true) ||
            message.contains("API key", ignoreCase = true) ||
            message.contains("401") -> {
                OpenRouterError.InvalidApiKey(message)
            }
            message.contains("billing", ignoreCase = true) || message.contains("credits", ignoreCase = true) -> {
                OpenRouterError.BillingDisabled(message)
            }
            message.contains("quota", ignoreCase = true) ||
            message.contains("rate limit", ignoreCase = true) ||
            message.contains("429") -> {
                OpenRouterError.RateLimitExceeded(message)
            }
            message.contains("400") || message.contains("invalid", ignoreCase = true) -> {
                OpenRouterError.InvalidRequest(message)
            }
            else -> OpenRouterError.Unknown(message, e)
        }
    }
}
