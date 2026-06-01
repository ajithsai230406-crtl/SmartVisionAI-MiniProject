package com.smartvision.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// Conflict resolved: AppDatabase in com.smartvision.ai.data.db is the primary database.
// @Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
abstract class SmartVisionDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
