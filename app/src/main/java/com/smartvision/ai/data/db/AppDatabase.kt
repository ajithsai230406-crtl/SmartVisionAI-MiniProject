package com.smartvision.ai.data.db

import androidx.room.*
import com.smartvision.ai.data.local.HistoryDao
import com.smartvision.ai.data.local.HistoryEntity
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
    entities  = [ScanHistoryEntity::class, HistoryEntity::class, GeminiCacheEntity::class],
    version   = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun historyDao(): HistoryDao
    abstract fun geminiCacheDao(): GeminiCacheDao

    companion object {
        const val DATABASE_NAME = "smartvision_db"
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
