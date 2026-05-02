package com.smartvision.ai.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.domain.usecase.ResultRepository
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResultRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ResultRepository {

    private var cachedResult:    ScanResult? = null
    private var cachedImagePath: String?     = null

    override suspend fun cacheResult(result: ScanResult)    { cachedResult    = result }
    override suspend fun getCachedResult(): ScanResult?      = cachedResult
    override suspend fun cacheImagePath(path: String)       { cachedImagePath = path   }
    override suspend fun getCachedImagePath(): String?       = cachedImagePath
    override suspend fun cacheUri(uri: Uri)                 { cachedImagePath = uri.toString() }

    override suspend fun saveToHistory(item: ScanHistoryItem): Result<Unit> = try {
        val id = item.id.ifEmpty { UUID.randomUUID().toString() }
        firestore.collection("scan_history").document(id).set(item.copy(id = id)).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getHistory(userId: String): Result<List<ScanHistoryItem>> = try {
        val snap = firestore.collection("scan_history")
            .whereEqualTo("userId", userId)
            .orderBy("timestampMillis", Query.Direction.DESCENDING)
            .limit(50).get().await()
        Result.success(snap.documents.mapNotNull { it.toObject(ScanHistoryItem::class.java) })
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun deleteHistory(itemId: String): Result<Unit> = try {
        firestore.collection("scan_history").document(itemId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
