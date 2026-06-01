package com.smartvision.ai.domain.usecase

import android.net.Uri
import com.smartvision.ai.domain.models.ScanHistoryItem
import com.smartvision.ai.domain.models.ScanResult

interface ResultRepository {
    suspend fun cacheResult(result: ScanResult)
    suspend fun getCachedResult(): ScanResult?
    suspend fun cacheImagePath(path: String)
    suspend fun getCachedImagePath(): String?
    suspend fun cacheUri(uri: Uri)
    
    suspend fun saveToHistory(item: ScanHistoryItem): Result<Unit>
    suspend fun getHistory(userId: String): Result<List<ScanHistoryItem>>
    suspend fun deleteHistory(itemId: String): Result<Unit>
}
