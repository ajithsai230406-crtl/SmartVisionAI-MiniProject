package com.smartvision.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartvision.ai.domain.model.HistoryRecord

@Entity(tableName = "history_records")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val details: String,
    val createdAt: Long,
    val userId: String = ""
) {
    fun toDomain() = HistoryRecord(id, type, title, details, createdAt)
}

fun HistoryRecord.toEntity(userId: String) = HistoryEntity(id, type, title, details, createdAt, userId)
