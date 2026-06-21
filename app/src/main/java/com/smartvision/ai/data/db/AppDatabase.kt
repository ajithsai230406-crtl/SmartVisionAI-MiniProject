package com.smartvision.ai.data.db

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartvision.ai.data.local.HistoryDao
import com.smartvision.ai.data.local.HistoryEntity
import com.smartvision.ai.data.local.OcrScanDao
import com.smartvision.ai.data.local.OcrScanEntity
import com.smartvision.ai.data.local.StudentHelperHistoryDao
import com.smartvision.ai.data.local.StudentHelperHistoryEntity
import com.smartvision.ai.domain.models.ScanHistoryItem
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// ROOM ENTITY
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "scan_history_legacy")
data class ScanHistoryEntity(
    @PrimaryKey val id: String,
    val userId:          String = "",
    val moduleType:      String = "",
    val summary:         String = "",
    val imageUrl:        String = "",
    val timestampMillis: Long   = 0L,
    val resultJson:      String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history_legacy ORDER BY timestampMillis DESC")
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history_legacy WHERE moduleType = :type ORDER BY timestampMillis DESC")
    fun getHistoryByType(type: String): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ScanHistoryEntity)

    @Delete
    suspend fun delete(item: ScanHistoryEntity)

    @Query("DELETE FROM scan_history_legacy")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM scan_history_legacy")
    suspend fun count(): Int

    @Query("SELECT * FROM scan_history_legacy WHERE summary LIKE '%' || :query || '%' ORDER BY timestampMillis DESC")
    fun search(query: String): Flow<List<ScanHistoryEntity>>
}

// ─────────────────────────────────────────────────────────────────────────────
// DATABASE
// ─────────────────────────────────────────────────────────────────────────────

@Database(
    entities  = [ScanHistoryEntity::class, HistoryEntity::class, GeminiCacheEntity::class, OcrScanEntity::class, StudentHelperHistoryEntity::class],
    version   = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun historyDao(): HistoryDao
    abstract fun geminiCacheDao(): GeminiCacheDao
    abstract fun ocrScanDao(): OcrScanDao
    abstract fun studentHelperHistoryDao(): StudentHelperHistoryDao

    companion object {
        const val DATABASE_NAME = "smartvision_db"

        /** Migration from v2 → v3: add OCR scan history table */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ocr_scan_history` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `timestamp` INTEGER NOT NULL,
                        `extractedText` TEXT NOT NULL,
                        `translatedText` TEXT,
                        `sourceLang` TEXT NOT NULL,
                        `targetLang` TEXT,
                        `wordCount` INTEGER NOT NULL,
                        `characterCount` INTEGER NOT NULL,
                        `paragraphCount` INTEGER NOT NULL,
                        `confidence` REAL NOT NULL,
                        `script` TEXT NOT NULL DEFAULT 'Latin'
                    )
                    """.trimIndent()
                )
            }
        }

        /** Migration from v5 → v6: add detailsJson column to history_records */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE history_records ADD COLUMN detailsJson TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAPPERS
// ─────────────────────────────────────────────────────────────────────────────

fun ScanHistoryEntity.toDomain() = ScanHistoryItem(
    id              = id,
    userId          = userId,
    moduleType      = moduleType,
    summary         = summary,
    imageUrl        = imageUrl,
    timestampMillis = timestampMillis,
    resultJson      = resultJson
)

fun ScanHistoryItem.toEntity() = ScanHistoryEntity(
    id              = id,
    userId          = userId,
    moduleType      = moduleType,
    summary         = summary,
    imageUrl        = imageUrl,
    timestampMillis = timestampMillis,
    resultJson      = resultJson
)
