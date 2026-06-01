package com.smartvision.ai

import com.smartvision.ai.data.repository.AiAssistantRepository
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAssistantRepositoryTest {
    private val repository = AiAssistantRepository()

    @Test
    fun replyToOcrPromptMentionsScanner() {
        val reply = repository.replyTo("How do I scan OCR text?")
        assertTrue(reply.contains("OCR Scanner"))
    }

    @Test
    fun replyToWastePromptMentionsClassifier() {
        val reply = repository.replyTo("How should I recycle plastic waste?")
        assertTrue(reply.contains("Waste Classifier"))
    }
}
