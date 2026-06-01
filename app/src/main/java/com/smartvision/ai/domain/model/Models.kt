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
    val createdAt: Long = System.currentTimeMillis()
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
