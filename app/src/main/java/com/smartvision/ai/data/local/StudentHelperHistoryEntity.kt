package com.smartvision.ai.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "student_helper_history")
data class StudentHelperHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val response: String,
    val timestamp: Long,
    val featureType: String
)

@Dao
interface StudentHelperHistoryDao {
    @Query("SELECT * FROM student_helper_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<StudentHelperHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StudentHelperHistoryEntity): Long

    @Delete
    suspend fun delete(item: StudentHelperHistoryEntity)

    @Query("DELETE FROM student_helper_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM student_helper_history")
    suspend fun clearAll()
}
