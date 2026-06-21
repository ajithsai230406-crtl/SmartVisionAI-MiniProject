package com.smartvision.ai.domain.model

data class FeedbackEntry(
    val feedbackId: String = "",
    val userName: String = "",
    val email: String = "",
    val category: String = "",
    val subject: String = "",
    val message: String = "",
    val rating: Int = 5,
    val imageUrl: String? = null,
    val deviceInfo: String = "",
    val appVersion: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Pending" // "Pending" or "Resolved"
)
