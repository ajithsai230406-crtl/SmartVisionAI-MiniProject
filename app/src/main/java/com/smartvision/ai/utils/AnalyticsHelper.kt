package com.smartvision.ai.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object AnalyticsHelper {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun logEvent(name: String, params: Bundle? = null) {
        try {
            firebaseAnalytics?.logEvent(name, params)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun logScanEvent(moduleName: String, success: Boolean, latencyMs: Long = 0L) {
        val bundle = Bundle().apply {
            putString("module_name", moduleName)
            putBoolean("success", success)
            putLong("latency_ms", latencyMs)
        }
        logEvent("scan_completed", bundle)
    }

    fun logException(throwable: Throwable) {
        try {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun trackApiCall(latencyMs: Long, success: Boolean) {
        val firestore = try { FirebaseFirestore.getInstance() } catch(e: Exception) { null }
        if (firestore != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = firestore.collection("app_analytics").document("global_stats")
                    val updates = mapOf(
                        "gemini_latency_sum" to com.google.firebase.firestore.FieldValue.increment(latencyMs),
                        "gemini_latency_count" to com.google.firebase.firestore.FieldValue.increment(1),
                        if (success) "gemini_api_success" to com.google.firebase.firestore.FieldValue.increment(1)
                        else "gemini_api_fail" to com.google.firebase.firestore.FieldValue.increment(1)
                    )
                    db.update(updates).await()
                } catch (e: Exception) {
                    try {
                        val db = firestore.collection("app_analytics").document("global_stats")
                        val fieldName = if (success) "gemini_api_success" else "gemini_api_fail"
                        val initialData = mapOf(
                            "total_scans" to 0L,
                            "ocr_scans" to 0L,
                            "translation_scans" to 0L,
                            "object_detections" to 0L,
                            "medicine_scans" to 0L,
                            "waste_scans" to 0L,
                            "assistant_chats" to 0L,
                            "other_scans" to 0L,
                            "gemini_latency_sum" to latencyMs,
                            "gemini_latency_count" to 1L,
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
        }
    }
}
