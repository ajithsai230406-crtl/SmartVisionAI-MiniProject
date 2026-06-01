package com.smartvision.ai.data

import androidx.annotation.DrawableRes

data class ModuleItem(
    val title: String,
    @DrawableRes val iconRes: Int,
    val destinationId: Int
)

data class ChatMessage(
    val text: String,
    val fromUser: Boolean
)

data class HistoryItem(
    val title: String,
    val subtitle: String,
    val time: String,
    @DrawableRes val iconRes: Int
)
