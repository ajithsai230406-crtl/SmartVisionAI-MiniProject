package com.smartvision.ai

import android.content.Context
import io.mockk.*
import com.smartvision.ai.data.db.GeminiCacheDao
import com.smartvision.ai.data.db.GeminiCacheEntity
import com.smartvision.ai.data.repository.AiAssistantRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAssistantRepositoryTest {
    private val mockCacheDao = object : GeminiCacheDao {
        override suspend fun getCache(hash: Int): GeminiCacheEntity? = null
        override suspend fun insertCache(entity: GeminiCacheEntity) {}
        override suspend fun deleteExpiredCaches(expiryTime: Long) {}
        override suspend fun clearCache() {}
    }
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockFallbackApiClient = mockk<com.smartvision.ai.data.repository.FallbackApiClient>().apply {
        coEvery { generateContent(any()) } throws Exception("Network Offline")
    }
    private val repository = AiAssistantRepository(mockCacheDao, mockFallbackApiClient, mockContext)

    @Test
    fun replyToOcrPromptMentionsScanner() = runBlocking {
        val reply = repository.replyTo("How do I scan OCR text?")
        assertTrue(reply.contains("OCR"))
    }

    @Test
    fun replyToWastePromptMentionsClassifier() = runBlocking {
        val reply = repository.replyTo("How should I recycle plastic waste?")
        assertTrue(reply.contains("Waste Classifier"))
    }
}
