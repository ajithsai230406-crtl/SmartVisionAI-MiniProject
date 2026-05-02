package com.smartvision.ai.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.smartvision.ai.domain.models.*

interface DetectObjectsUseCase { suspend operator fun invoke(bitmap: Bitmap): ScanResult }
interface OcrUseCase {
    suspend operator fun invoke(bitmap: Bitmap): ScanResult
    suspend fun extractFromArea(bitmap: Bitmap, area: android.graphics.Rect): ScanResult
}
interface ClassifyWasteUseCase  { suspend operator fun invoke(bitmap: Bitmap): ScanResult }
interface ScanQrUseCase         { suspend operator fun invoke(bitmap: Bitmap): ScanResult }
interface StudentHelperUseCase  {
    suspend operator fun invoke(questionBitmap: Bitmap): ScanResult
    suspend fun explainText(text: String): ScanResult
}
interface MedicalScanUseCase    { suspend operator fun invoke(bitmap: Bitmap): ScanResult }

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
