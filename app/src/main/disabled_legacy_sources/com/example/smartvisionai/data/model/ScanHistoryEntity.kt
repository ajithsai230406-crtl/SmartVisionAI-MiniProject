package com.example.smartvisionai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val moduleId: String,
    val title: String,
    val confidence: Int,          // 0–100, 0 means N/A
    val timestamp: Long = System.currentTimeMillis()
) {
    /** Human-readable relative time (e.g. "2 min ago") */
    val timeAgo: String
        get() {
            val diff = System.currentTimeMillis() - timestamp
            return when {
                diff < 60_000             -> "just now"
                diff < 3_600_000         -> "${diff / 60_000} min ago"
                diff < 86_400_000        -> "${diff / 3_600_000} hr${if (diff / 3_600_000 > 1) "s" else ""} ago"
                else                     -> "${diff / 86_400_000} day${if (diff / 86_400_000 > 1) "s" else ""} ago"
            }
        }
}
