package com.example.smartvisionai.data.local

import androidx.room.*
import com.example.smartvisionai.data.model.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScanHistoryEntity)

    @Delete
    suspend fun delete(entity: ScanHistoryEntity)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun getCount(): Int
}
