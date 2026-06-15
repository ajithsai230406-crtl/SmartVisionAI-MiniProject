package com.smartvision.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_records WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeHistory(userId: String): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity): Long

    @Query("DELETE FROM history_records WHERE userId = :userId")
    suspend fun clear(userId: String)

    @Query("DELETE FROM history_records WHERE type = :type AND createdAt < :timestamp AND userId = :userId")
    suspend fun deleteByTypeOlderThan(type: String, timestamp: Long, userId: String): Int

    @Query("DELETE FROM history_records WHERE type IN (:types) AND createdAt < :timestamp AND userId = :userId")
    suspend fun deleteByTypesOlderThan(types: List<String>, timestamp: Long, userId: String): Int

    @Query("SELECT * FROM history_records WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllHistorySync(userId: String): List<HistoryEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM history_records WHERE type = :type AND title = :title AND details = :details AND createdAt = :createdAt AND userId = :userId)")
    suspend fun exists(type: String, title: String, details: String, createdAt: Long, userId: String): Boolean

    @Query("SELECT * FROM history_records WHERE type IN ('ChatUser', 'ChatBot') AND userId = :userId ORDER BY createdAt ASC")
    suspend fun getChatHistory(userId: String): List<HistoryEntity>

    @Query("DELETE FROM history_records WHERE type IN ('ChatUser', 'ChatBot') AND userId = :userId")
    suspend fun clearChatHistory(userId: String)
}

