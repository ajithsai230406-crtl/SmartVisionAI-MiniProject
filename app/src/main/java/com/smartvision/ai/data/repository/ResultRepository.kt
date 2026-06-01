package com.smartvision.ai.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartvision.ai.domain.models.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Renamed to avoid conflict with com.smartvision.ai.domain.usecase.ResultRepository
interface LegacyResultRepository {
    suspend fun saveMedicineResult(result: MedicineResult, userId: String)
    suspend fun saveWasteResult(result: WasteResult, userId: String)
    suspend fun saveOcrResult(text: String, translated: String, userId: String)
}

@Singleton
class LegacyResultRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : LegacyResultRepository {

    override suspend fun saveMedicineResult(result: MedicineResult, userId: String) {
        if (userId.isBlank()) return
        try {
            firestore.collection("users").document(userId)
                .collection("medicine_results")
                .add(mapOf(
                    "name"       to result.name,
                    "category"   to result.category,
                    "confidence" to result.confidence,
                    "timestamp"  to System.currentTimeMillis()
                )).await()
        } catch (_: Exception) {}
    }

    override suspend fun saveWasteResult(result: WasteResult, userId: String) {
        if (userId.isBlank()) return
        try {
            firestore.collection("users").document(userId)
                .collection("waste_results")
                .add(mapOf(
                    "category"   to result.category,
                    "confidence" to result.confidence,
                    "timestamp"  to System.currentTimeMillis()
                )).await()
        } catch (_: Exception) {}
    }

    override suspend fun saveOcrResult(text: String, translated: String, userId: String) {
        if (userId.isBlank()) return
        try {
            firestore.collection("users").document(userId)
                .collection("ocr_results")
                .add(mapOf(
                    "text"       to text,
                    "translated" to translated,
                    "timestamp"  to System.currentTimeMillis()
                )).await()
        } catch (_: Exception) {}
    }
}
