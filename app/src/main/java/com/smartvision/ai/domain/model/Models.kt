package com.smartvision.ai.domain.model

import androidx.annotation.DrawableRes

data class FeatureItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val icon: Int,
    val destinationId: Int
)

data class ChatMessage(
    val text: String,
    val fromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class HistoryRecord(
    val id: Long = 0,
    val type: String,
    val title: String,
    val details: String,
    val createdAt: Long = System.currentTimeMillis(),
    val detailsJson: String = ""
)

data class VisionResult(
    val label: String,
    val confidence: Float,
    val description: String,
    val tip: String
)

data class UserProfile(
    val name: String = "Ajith Kumar",
    val email: String = "Premium AI user",
    val photoUrl: String? = null
)

data class ChatHistoryItem(
    val id: String = "",
    val uid: String = "",
    val userMessage: String = "",
    val aiResponse: String = "",
    val timestamp: Long = 0L
)

// ── ChatGPT-like Conversation Models ────────────────────────────────────────

/**
 * Represents a conversation thread (like a ChatGPT conversation).
 * Stored in Firebase: Conversations/{conversationId}
 */
data class Conversation(
    val id: String = "",
    val uid: String = "",
    val title: String = "New Chat",
    val lastMessage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
)

/**
 * A single message within a conversation.
 * Stored in Firebase: Conversations/{conversationId}/Messages/{messageId}
 */
data class ConversationMessage(
    val id: String = "",
    val conversationId: String = "",
    val text: String = "",
    val fromUser: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentType: String? = null,   // "image", "pdf", "txt", "csv", null
    val attachmentPreview: String? = null  // Extracted text preview or thumbnail URI
)

