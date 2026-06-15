package com.smartvision.ai.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// ─── Entity ───────────────────────────────────────────────────────────────────

@Entity(tableName = "ocr_scan_history")
data class OcrScanEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val extractedText: String,
    val translatedText: String?,
    val sourceLang: String,
    val targetLang: String?,
    val wordCount: Int,
    val characterCount: Int,
    val paragraphCount: Int,
    val confidence: Float,
    val script: String = "Latin"
)

// ─── DAO ─────────────────────────────────────────────────────────────────────

@Dao
interface OcrScanDao {
    @Query("SELECT * FROM ocr_scan_history ORDER BY timestamp DESC LIMIT 50")
    fun observeHistory(): Flow<List<OcrScanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OcrScanEntity)

    @Query("DELETE FROM ocr_scan_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM ocr_scan_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM ocr_scan_history")
    suspend fun count(): Int
}
