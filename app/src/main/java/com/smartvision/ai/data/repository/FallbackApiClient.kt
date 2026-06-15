package com.smartvision.ai.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FallbackApiClient @Inject constructor(
    private val openRouterManager: OpenRouterManager
) {

    /**
     * Generates a single block of content using OpenRouter auto model.
     */
    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val text = openRouterManager.generateContent(prompt, modelName = "openrouter/auto")
            if (text.isNotBlank()) {
                return@withContext text
            }
        } catch (e: Exception) {
            Log.e("FallbackApiClient", "API model failed: ${e.message}")
        }

        throw Exception("API generation failed on OpenRouter")
    }

    /**
     * Streams the generated content using OpenRouter auto model.
     */
    fun generateContentStream(prompt: String): Flow<String> = flow {
        try {
            openRouterManager.generateContentStream(prompt, modelName = "openrouter/auto").collect { chunk ->
                emit(chunk)
            }
        } catch (e: Exception) {
            Log.e("FallbackApiClient", "Stream failed: ${e.message}")
            throw Exception("Streaming failed on OpenRouter", e)
        }
    }.flowOn(Dispatchers.IO)

}
