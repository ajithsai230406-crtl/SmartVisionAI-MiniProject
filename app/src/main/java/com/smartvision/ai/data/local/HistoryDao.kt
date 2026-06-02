package com.smartvision.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_records ORDER BY createdAt DESC")
    fun observeHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity): Long

    @Query("DELETE FROM history_records")
    suspend fun clear()

    @Query("DELETE FROM history_records WHERE type = :type AND createdAt < :timestamp")
    suspend fun deleteByTypeOlderThan(type: String, timestamp: Long): Int

    @Query("DELETE FROM history_records WHERE type IN (:types) AND createdAt < :timestamp")
    suspend fun deleteByTypesOlderThan(types: List<String>, timestamp: Long): Int
}

