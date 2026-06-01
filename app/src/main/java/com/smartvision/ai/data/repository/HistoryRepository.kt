package com.smartvision.ai.data.repository

import com.smartvision.ai.data.local.HistoryDao
import com.smartvision.ai.data.local.toEntity
import com.smartvision.ai.domain.model.HistoryRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dao: HistoryDao
) {
    fun observeHistory(): Flow<List<HistoryRecord>> = dao.observeHistory().map { rows -> rows.map { it.toDomain() } }

    suspend fun save(type: String, title: String, details: String) {
        dao.insert(HistoryRecord(type = type, title = title, details = details).toEntity())
    }

    suspend fun clear() = dao.clear()
}
