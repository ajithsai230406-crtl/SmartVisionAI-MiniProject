package com.smartvision.ai

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
    private val repository = AiAssistantRepository(mockCacheDao)

    @Test
    fun replyToOcrPromptMentionsScanner() = runBlocking {
        val reply = repository.replyTo("How do I scan OCR text?")
        assertTrue(reply.contains("OCR Scanner"))
    }

    @Test
    fun replyToWastePromptMentionsClassifier() = runBlocking {
        val reply = repository.replyTo("How should I recycle plastic waste?")
        assertTrue(reply.contains("Waste Classifier"))
    }
}
