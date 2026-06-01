package com.smartvision.ai

import com.smartvision.ai.domain.model.ChatMessage
import com.smartvision.ai.domain.model.HistoryRecord

object FakeDataFactory {
    fun historyRecord(type: String = "OCR") = HistoryRecord(
        type = type,
        title = "$type Sample",
        details = "Generated fake test record"
    )

    fun chatMessage(text: String = "Explain Smart Vision AI") = ChatMessage(
        text = text,
        fromUser = true
    )
}
