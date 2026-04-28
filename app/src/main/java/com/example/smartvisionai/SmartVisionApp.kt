package com.example.smartvisionai

import android.app.Application

// Removed @HiltAndroidApp to resolve "multiple app roots" error.
// The main application class is com.smartvision.ai.SmartVisionApplication.
class SmartVisionApp : Application() {

    override fun onCreate() {
        try {
            super.onCreate()
        } catch (e: Exception) {
            android.util.Log.e("SVA_APP", "App init failed: ${e.message}", e)
        }
    }
}
