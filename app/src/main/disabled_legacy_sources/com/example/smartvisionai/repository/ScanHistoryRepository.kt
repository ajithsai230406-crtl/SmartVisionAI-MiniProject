package com.example.smartvisionai.repository

import com.example.smartvisionai.data.local.ScanHistoryDao
import com.example.smartvisionai.data.model.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanHistoryRepository @Inject constructor(
    private val dao: ScanHistoryDao
) {
    fun getAllHistory(): Flow<List<ScanHistoryEntity>> = dao.getAllHistory()

    fun getRecentHistory(limit: Int = 50): Flow<List<ScanHistoryEntity>> =
        dao.getRecentHistory(limit)

    suspend fun insertScan(moduleId: String, title: String, confidence: Int) {
        dao.insert(
            ScanHistoryEntity(
                moduleId    = moduleId,
                title       = title.take(120),
                confidence  = confidence
            )
        )
    }

    suspend fun deleteItem(entity: ScanHistoryEntity) = dao.delete(entity)

    suspend fun clearAll() = dao.clearAll()
}
