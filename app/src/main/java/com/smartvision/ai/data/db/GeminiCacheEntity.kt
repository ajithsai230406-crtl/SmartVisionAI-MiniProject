package com.smartvision.ai.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "gemini_cache")
data class GeminiCacheEntity(
    @PrimaryKey val promptHash: Int,
    val prompt: String,
    val response: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Dao
interface GeminiCacheDao {
    @Query("SELECT * FROM gemini_cache WHERE promptHash = :hash LIMIT 1")
    suspend fun getCache(hash: Int): GeminiCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(entity: GeminiCacheEntity)

    @Query("DELETE FROM gemini_cache WHERE timestampMillis < :expiryTime")
    suspend fun deleteExpiredCaches(expiryTime: Long)

    @Query("DELETE FROM gemini_cache")
    suspend fun clearCache()
}
