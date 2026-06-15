package com.smartvision.ai.domain.model

data class StudyHistoryItem(
    val id: String = "",
    val uid: String = "",
    val question: String = "",
    val answer: String = "",
    val subject: String = "",
    val timestamp: Long = 0L
)
