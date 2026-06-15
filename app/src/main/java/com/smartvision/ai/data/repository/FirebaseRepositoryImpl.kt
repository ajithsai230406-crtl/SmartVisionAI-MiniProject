package com.smartvision.ai.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.smartvision.ai.domain.model.StudyHistoryItem
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : FirebaseRepository {

    private val uid get() = auth.currentUser?.uid ?: "guest_uid"

    override suspend fun uploadFile(path: String, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val ref = storage.reference.child(path)
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }
    }

    override suspend fun uploadFileBytes(path: String, bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val ref = storage.reference.child(path)
            ref.putBytes(bytes).await()
            ref.downloadUrl.await().toString()
        }
    }

    override fun getCurrentUid(): String = uid

    override suspend fun saveScannedDocument(
        name: String,
        downloadUrl: String,
        fileSizeKb: Long,
        pageCount: Int,
        format: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val docId = UUID.randomUUID().toString()
            val data = hashMapOf(
                "id" to docId,
                "uid" to uid,
                "name" to name,
                "downloadUrl" to downloadUrl,
                "fileSizeKb" to fileSizeKb,
                "pageCount" to pageCount,
                "format" to format,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("ScannedDocuments").document(docId).set(data).await()
            trackModuleUsage("Document")
            Unit
        }
    }


    override suspend fun saveUser(
        uid: String,
        name: String,
        email: String,
        photoUrl: String?,
        loginProvider: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val lastLogin = System.currentTimeMillis()
            val deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
            val isGuest = loginProvider == "Guest Account" || auth.currentUser?.isAnonymous == true
            
            val userMap = hashMapOf(
                "uid" to uid,
                "name" to name,
                "email" to email,
                "photoUrl" to photoUrl,
                "loginProvider" to loginProvider,
                "lastLogin" to lastLogin,
                "deviceInfo" to deviceInfo
            )
            if (isGuest) {
                userMap["loginType"] = "guest"
            }
            
            // Check if document exists to preserve 'createdAt'
            val docRef = firestore.collection("users").document(uid)
            val docSnapshot = runCatching { docRef.get().await() }.getOrNull()
            if (docSnapshot == null || !docSnapshot.exists()) {
                userMap["createdAt"] = lastLogin
            } else {
                val existingCreatedAt = docSnapshot.getLong("createdAt") ?: lastLogin
                userMap["createdAt"] = existingCreatedAt
            }
            
            docRef.set(userMap, SetOptions.merge()).await()
            
            // Also write to Users capitalized collection
            val usersCapitalizedRef = firestore.collection("Users").document(uid)
            usersCapitalizedRef.set(userMap, SetOptions.merge()).await()
            Unit
        }
    }

    override suspend fun saveStudyHistory(
        uid: String,
        question: String,
        answer: String,
        subject: String,
        timestamp: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val docId = UUID.randomUUID().toString()
            val data = hashMapOf(
                "id" to docId,
                "uid" to uid,
                "question" to question.take(2000),
                "query" to question.take(2000), // backward compatibility
                "answer" to answer.take(10000),
                "response" to answer.take(10000), // backward compatibility
                "subject" to subject,
                "featureType" to subject, // backward compatibility
                "timestamp" to timestamp
            )
            firestore.collection("StudyHistory").document(docId).set(data).await()
            trackModuleUsage("Student")
            Unit
        }
    }

    override suspend fun getStudyHistoryPaginated(
        uid: String,
        limit: Long,
        lastVisibleTimestamp: Long?
    ): Result<List<StudyHistoryItem>> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                // Primary path: Order by timestamp descending (requires composite index uid ASC, timestamp DESC)
                var query = firestore.collection("StudyHistory")
                    .whereEqualTo("uid", uid)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit)
                
                if (lastVisibleTimestamp != null) {
                    query = query.startAfter(lastVisibleTimestamp)
                }
                
                val snapshot = query.get().await()
                snapshot.documents.map { doc ->
                    StudyHistoryItem(
                        id = doc.id,
                        uid = doc.getString("uid") ?: "",
                        question = doc.getString("question") ?: doc.getString("query") ?: "",
                        answer = doc.getString("answer") ?: doc.getString("response") ?: "",
                        subject = doc.getString("subject") ?: doc.getString("featureType") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("FirebaseRepository", "Firestore index required, falling back to simple sorting: ${e.message}")
                // Fallback path: If index is missing in Firestore, do equality query without orderBy to prevent crashes, then sort client-side
                val query = firestore.collection("StudyHistory")
                    .whereEqualTo("uid", uid)
                    // Fetch a slightly larger batch to sort client-side in case index setup is pending
                    .limit(limit * 3)
                
                val snapshot = query.get().await()
                val items = snapshot.documents.map { doc ->
                    StudyHistoryItem(
                        id = doc.id,
                        uid = doc.getString("uid") ?: "",
                        question = doc.getString("question") ?: doc.getString("query") ?: "",
                        answer = doc.getString("answer") ?: doc.getString("response") ?: "",
                        subject = doc.getString("subject") ?: doc.getString("featureType") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }.sortedByDescending { it.timestamp }
                
                // Perform client-side pagination if lastVisibleTimestamp is provided
                if (lastVisibleTimestamp != null) {
                    items.filter { it.timestamp < lastVisibleTimestamp }.take(limit.toInt())
                } else {
                    items.take(limit.toInt())
                }
            }
        }
    }

    override suspend fun deleteStudyHistoryItem(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            firestore.collection("StudyHistory").document(id).delete().await()
            Unit
        }
    }

    override suspend fun clearAllStudyHistory(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = firestore.collection("StudyHistory")
                .whereEqualTo("uid", uid)
                .get()
                .await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Unit
        }
    }

    override suspend fun saveMedicineScan(
        medicineName: String,
        genericName: String,
        category: String,
        safetyScore: Int,
        composition: String,
        uses: String,
        sideEffects: String,
        confidence: Float,
        imageUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val docId = UUID.randomUUID().toString()
            val data = hashMapOf(
                "id" to docId,
                "uid" to uid,
                "medicineName" to medicineName,
                "genericName" to genericName,
                "category" to category,
                "safetyScore" to safetyScore,
                "composition" to composition.take(1000),
                "uses" to uses.take(1000),
                "sideEffects" to sideEffects.take(1000),
                "confidence" to confidence,
                "imageUrl" to imageUrl,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("MedicineScans").document(docId).set(data).await()
            trackModuleUsage("Medicine")
            Unit
        }
    }

    override suspend fun saveOCRHistory(
        extractedText: String,
        sourceLanguage: String,
        translatedText: String,
        targetLanguage: String,
        wordCount: Int,
        imageUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val docId = UUID.randomUUID().toString()
            val data = hashMapOf(
                "id" to docId,
                "uid" to uid,
                "extractedText" to extractedText.take(5000),
                "sourceLanguage" to sourceLanguage,
                "translatedText" to translatedText.take(5000),
                "targetLanguage" to targetLanguage,
                "wordCount" to wordCount,
                "imageUrl" to imageUrl,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("OCRHistory").document(docId).set(data).await()
            trackModuleUsage("OCR")
            Unit
        }
    }

    override suspend fun saveChatHistory(
        uid: String,
        userMessage: String,
        aiResponse: String,
        timestamp: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val docId = UUID.randomUUID().toString()
            val data = hashMapOf(
                "id" to docId,
                "uid" to uid,
                "userMessage" to userMessage.take(5000),
                "aiResponse" to aiResponse.take(5000),
                "timestamp" to timestamp
            )
            firestore.collection("ChatHistory").document(docId).set(data).await()
            trackModuleUsage("Chat")
            Unit
        }
    }

    override suspend fun getChatHistoryPaginated(
        uid: String,
        limit: Long,
        lastVisibleTimestamp: Long?
    ): Result<List<com.smartvision.ai.domain.model.ChatHistoryItem>> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                var query = firestore.collection("ChatHistory")
                    .whereEqualTo("uid", uid)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit)
                
                if (lastVisibleTimestamp != null) {
                    query = query.startAfter(lastVisibleTimestamp)
                }
                
                val snapshot = query.get().await()
                snapshot.documents.map { doc ->
                    com.smartvision.ai.domain.model.ChatHistoryItem(
                        id = doc.id,
                        uid = doc.getString("uid") ?: "",
                        userMessage = doc.getString("userMessage") ?: "",
                        aiResponse = doc.getString("aiResponse") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("FirebaseRepository", "Firestore index required for ChatHistory, falling back to client sorting: ${e.message}")
                val query = firestore.collection("ChatHistory")
                    .whereEqualTo("uid", uid)
                    .limit(limit * 3)
                
                val snapshot = query.get().await()
                val items = snapshot.documents.map { doc ->
                    com.smartvision.ai.domain.model.ChatHistoryItem(
                        id = doc.id,
                        uid = doc.getString("uid") ?: "",
                        userMessage = doc.getString("userMessage") ?: "",
                        aiResponse = doc.getString("aiResponse") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }.sortedByDescending { it.timestamp }
                
                if (lastVisibleTimestamp != null) {
                    items.filter { it.timestamp < lastVisibleTimestamp }.take(limit.toInt())
                } else {
                    items.take(limit.toInt())
                }
            }
        }
    }

    override suspend fun clearAllChatHistory(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = firestore.collection("ChatHistory")
                .whereEqualTo("uid", uid)
                .get()
                .await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Unit
        }
    }

    override suspend fun saveTranslation(
        sourceText: String,
        translatedText: String,
        sourceLang: String,
        targetLang: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val docId = UUID.randomUUID().toString()
            val data = hashMapOf(
                "id" to docId,
                "uid" to uid,
                "sourceText" to sourceText.take(5000),
                "translatedText" to translatedText.take(5000),
                "sourceLang" to sourceLang,
                "targetLang" to targetLang,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("Translations").document(docId).set(data).await()
            trackModuleUsage("Translation")
            Unit
        }
    }

    override suspend fun saveObjectDetection(
        objectName: String,
        confidence: Float,
        category: String,
        description: String,
        uses: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val docId = UUID.randomUUID().toString()
            val data = hashMapOf(
                "id" to docId,
                "uid" to uid,
                "objectName" to objectName,
                "confidence" to confidence,
                "category" to category,
                "description" to description.take(2000),
                "uses" to uses.take(2000),
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("ObjectDetections").document(docId).set(data).await()
            trackModuleUsage("Object")
            Unit
        }
    }

    override suspend fun saveWasteClassification(
        wasteType: String,
        exactItemName: String,
        recyclable: Boolean,
        reusable: Boolean,
        disposalMethod: String,
        carbonReduction: String,
        carbonImpact: String,
        ecoScore: Int,
        recyclingRate: Float,
        confidence: Float
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val docId = UUID.randomUUID().toString()
            val data = hashMapOf(
                "id" to docId,
                "uid" to uid,
                "wasteType" to wasteType,
                "exactItemName" to exactItemName,
                "recyclable" to recyclable,
                "reusable" to reusable,
                "disposalMethod" to disposalMethod,
                "carbonReduction" to carbonReduction,
                "carbonImpact" to carbonImpact,
                "ecoScore" to ecoScore,
                "recyclingRate" to recyclingRate,
                "confidence" to confidence,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("WasteClassifications").document(docId).set(data).await()
            trackModuleUsage("Waste")
            Unit
        }
    }

    override suspend fun trackModuleUsage(module: String) {
        val field = when (module) {
            "OCR" -> "ocr_scans"
            "Medicine" -> "medicine_scans"
            "Waste" -> "waste_scans"
            "Student" -> "student_sessions"
            "Object" -> "object_detections"
            "Voice" -> "voice_translations"
            "Translation" -> "text_translations"
            "Chat" -> "chat_interactions"
            else -> "other_scans"
        }
        runCatching {
            val ref = firestore.collection("app_analytics").document("global_stats")
            ref.set(
                mapOf(
                    "total_scans" to FieldValue.increment(1),
                    field to FieldValue.increment(1),
                    "last_updated" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
        }
    }
}
