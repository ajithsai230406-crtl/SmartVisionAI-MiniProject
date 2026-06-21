package com.smartvision.ai.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartvision.ai.data.local.HistoryDao
import com.smartvision.ai.data.local.HistoryEntity
import com.smartvision.ai.data.local.toEntity
import com.smartvision.ai.domain.model.HistoryRecord
import com.smartvision.ai.domain.models.ScanHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dao: HistoryDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    fun observeHistory(): Flow<List<HistoryRecord>> {
        val userId = auth.currentUser?.uid ?: "guest_uid"
        return dao.observeHistory(userId).map { rows -> rows.map { it.toDomain() } }
    }

    suspend fun save(type: String, title: String, details: String, detailsJson: String = "") {
        val createdAt = System.currentTimeMillis()
        val userId = auth.currentUser?.uid ?: "guest_uid"
        dao.insert(HistoryRecord(type = type, title = title, details = details, createdAt = createdAt, detailsJson = detailsJson).toEntity(userId))

        if (userId != "guest_uid") {
            try {
                val docId = UUID.randomUUID().toString()
                val rawSummary = if (details.isNotBlank()) "$title - $details" else title
                val encryptedSummary = com.smartvision.ai.utils.CryptoHelper.encrypt(rawSummary, userId)
                val firestoreItem = ScanHistoryItem(
                    id = docId,
                    userId = userId,
                    moduleType = type,
                    summary = encryptedSummary,
                    timestampMillis = createdAt
                )
                firestore.collection("users").document(userId).collection("scan_history").document(docId).set(firestoreItem).await()
                
                // Track usage statistics in Firestore for Admin Dashboard
                incrementMetric(type)
                // Log Firebase Analytics Event
                com.smartvision.ai.utils.AnalyticsHelper.logScanEvent(type, true)
            } catch (e: Exception) {
                // Log Firebase Analytics failure event
                com.smartvision.ai.utils.AnalyticsHelper.logScanEvent(type, false)
                // Log Crashlytics non-fatal exception
                com.smartvision.ai.utils.AnalyticsHelper.logException(e)
            }
        }
    }

    private suspend fun incrementMetric(type: String) {
        val firestoreInstance = firestore
        try {
            val db = firestoreInstance.collection("app_analytics").document("global_stats")
            val fieldName = when (type) {
                "OCR" -> "ocr_scans"
                "Translation", "Voice" -> "translation_scans"
                "Object" -> "object_detections"
                "Medicine" -> "medicine_scans"
                "Waste" -> "waste_scans"
                "ChatUser", "ChatBot" -> "assistant_chats"
                else -> "other_scans"
            }
            val updates = mapOf(
                "total_scans" to com.google.firebase.firestore.FieldValue.increment(1),
                fieldName to com.google.firebase.firestore.FieldValue.increment(1)
            )
            db.update(updates).await()
        } catch (e: Exception) {
            // Document might not exist, create it if update fails
            try {
                val db = firestoreInstance.collection("app_analytics").document("global_stats")
                val fieldName = when (type) {
                    "OCR" -> "ocr_scans"
                    "Translation", "Voice" -> "translation_scans"
                    "Object" -> "object_detections"
                    "Medicine" -> "medicine_scans"
                    "Waste" -> "waste_scans"
                    "ChatUser", "ChatBot" -> "assistant_chats"
                    else -> "other_scans"
                }
                val initialData = mapOf(
                    "total_scans" to 1L,
                    "ocr_scans" to 0L,
                    "translation_scans" to 0L,
                    "object_detections" to 0L,
                    "medicine_scans" to 0L,
                    "waste_scans" to 0L,
                    "assistant_chats" to 0L,
                    "other_scans" to 0L,
                    "gemini_latency_sum" to 0L,
                    "gemini_latency_count" to 0L,
                    "gemini_api_success" to 0L,
                    "gemini_api_fail" to 0L,
                    fieldName to 1L
                )
                db.set(initialData).await()
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    suspend fun getChatHistory(): List<HistoryRecord> {
        val userId = auth.currentUser?.uid ?: "guest_uid"
        return dao.getChatHistory(userId).map { it.toDomain() }
    }

    suspend fun clearChatHistory() {
        val userId = auth.currentUser?.uid ?: "guest_uid"
        dao.clearChatHistory(userId)
    }

    suspend fun clear() {
        val userId = auth.currentUser?.uid ?: "guest_uid"
        dao.clear(userId)
        if (userId != "guest_uid") {
            try {
                val snapshot = firestore.collection("users").document(userId).collection("scan_history")
                    .get()
                    .await()
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            } catch (e: Exception) {
                // Ignore batch deletion failures
            }
        }
    }

    suspend fun syncFromFirestore() {
        val userId = auth.currentUser?.uid ?: return
        try {
            val snapshot = firestore.collection("users").document(userId).collection("scan_history")
                .orderBy("timestampMillis", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            for (doc in snapshot.documents) {
                val moduleType = doc.getString("moduleType") ?: ""
                val encryptedSummary = doc.getString("summary") ?: ""
                val timestampMillis = doc.getLong("timestampMillis") ?: System.currentTimeMillis()

                val decryptedSummary = com.smartvision.ai.utils.CryptoHelper.decrypt(encryptedSummary, userId)
                val parts = decryptedSummary.split(" - ", limit = 2)
                val title = parts.getOrNull(0) ?: moduleType
                val details = parts.getOrNull(1) ?: ""

                val exists = dao.exists(moduleType, title, details, timestampMillis, userId)
                if (!exists) {
                    dao.insert(HistoryEntity(
                        type = moduleType,
                        title = title,
                        details = details,
                        createdAt = timestampMillis,
                        userId = userId
                    ))
                }
            }
        } catch (e: Exception) {
            // Ignore background sync failure
        }
    }

    suspend fun deleteByTypeOlderThan(type: String, timestamp: Long) {
        val userId = auth.currentUser?.uid ?: "guest_uid"
        dao.deleteByTypeOlderThan(type, timestamp, userId)
    }

    suspend fun deleteByTypesOlderThan(types: List<String>, timestamp: Long) {
        val userId = auth.currentUser?.uid ?: "guest_uid"
        dao.deleteByTypesOlderThan(types, timestamp, userId)
    }

    suspend fun getById(id: Long): HistoryRecord? {
        return dao.getById(id)?.toDomain()
    }
}
