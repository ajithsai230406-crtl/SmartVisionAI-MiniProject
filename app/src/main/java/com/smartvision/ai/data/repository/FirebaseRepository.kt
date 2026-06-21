package com.smartvision.ai.data.repository

import android.net.Uri
import com.smartvision.ai.domain.model.StudyHistoryItem

interface FirebaseRepository {
    suspend fun uploadFile(path: String, uri: Uri): Result<String>
    suspend fun uploadFileBytes(path: String, bytes: ByteArray): Result<String>
    fun getCurrentUid(): String
    suspend fun saveScannedDocument(
        name: String,
        downloadUrl: String,
        fileSizeKb: Long,
        pageCount: Int,
        format: String
    ): Result<Unit>

    suspend fun saveUser(
        uid: String,
        name: String,
        email: String,
        photoUrl: String?,
        loginProvider: String
    ): Result<Unit>

    suspend fun saveStudyHistory(
        uid: String,
        question: String,
        answer: String,
        subject: String,
        timestamp: Long
    ): Result<Unit>

    suspend fun getStudyHistoryPaginated(
        uid: String,
        limit: Long,
        lastVisibleTimestamp: Long?
    ): Result<List<StudyHistoryItem>>

    suspend fun deleteStudyHistoryItem(id: String): Result<Unit>

    suspend fun clearAllStudyHistory(uid: String): Result<Unit>

    suspend fun saveMedicineScan(
        medicineName: String,
        genericName: String,
        category: String,
        safetyScore: Int,
        composition: String,
        uses: String,
        sideEffects: String,
        confidence: Float,
        imageUrl: String? = null
    ): Result<Unit>

    suspend fun saveOCRHistory(
        extractedText: String,
        sourceLanguage: String,
        translatedText: String,
        targetLanguage: String,
        wordCount: Int,
        imageUrl: String? = null
    ): Result<Unit>

    suspend fun saveChatHistory(
        uid: String,
        userMessage: String,
        aiResponse: String,
        timestamp: Long
    ): Result<Unit>

    suspend fun getChatHistoryPaginated(
        uid: String,
        limit: Long,
        lastVisibleTimestamp: Long?
    ): Result<List<com.smartvision.ai.domain.model.ChatHistoryItem>>

    suspend fun clearAllChatHistory(uid: String): Result<Unit>

    suspend fun saveTranslation(
        sourceText: String,
        translatedText: String,
        sourceLang: String,
        targetLang: String
    ): Result<Unit>

    suspend fun saveObjectDetection(
        objectName: String,
        confidence: Float,
        category: String,
        description: String,
        uses: String
    ): Result<Unit>

    suspend fun saveWasteClassification(
        wasteType: String,
        exactItemName: String,
        recyclable: Boolean,
        reusable: Boolean,
        disposalMethod: String,
        carbonReduction: String,
        carbonImpact: String,
        ecoScore: Int,
        recyclingRate: Float,
        confidence: Float
    ): Result<Unit>

    suspend fun trackModuleUsage(module: String)

    // ── Conversation Management (ChatGPT-like) ─────────────────────────────

    suspend fun createConversation(
        uid: String,
        title: String = "New Chat"
    ): Result<String>

    suspend fun getConversations(
        uid: String,
        limit: Long = 30,
        lastTimestamp: Long? = null
    ): Result<List<com.smartvision.ai.domain.model.Conversation>>

    suspend fun getConversationMessages(
        conversationId: String,
        limit: Long = 50,
        lastTimestamp: Long? = null
    ): Result<List<com.smartvision.ai.domain.model.ConversationMessage>>

    suspend fun saveConversationMessage(
        conversationId: String,
        message: com.smartvision.ai.domain.model.ConversationMessage
    ): Result<Unit>

    suspend fun updateConversationMeta(
        conversationId: String,
        title: String? = null,
        lastMessage: String? = null,
        messageCount: Int? = null
    ): Result<Unit>

    suspend fun deleteConversation(conversationId: String): Result<Unit>

    suspend fun searchConversations(
        uid: String,
        query: String
    ): Result<List<com.smartvision.ai.domain.model.Conversation>>

    // ── In-App Feedback System ─────────────────────────────────────────────
    suspend fun saveFeedback(feedback: com.smartvision.ai.domain.model.FeedbackEntry): Result<Unit>
    suspend fun getUserFeedbackHistory(email: String): Result<List<com.smartvision.ai.domain.model.FeedbackEntry>>
    suspend fun getAllFeedback(): Result<List<com.smartvision.ai.domain.model.FeedbackEntry>>
    suspend fun updateFeedbackStatus(feedbackId: String, status: String): Result<Unit>
    suspend fun updateFeedback(feedback: com.smartvision.ai.domain.model.FeedbackEntry): Result<Unit>
    suspend fun deleteFeedback(feedbackId: String): Result<Unit>
}
