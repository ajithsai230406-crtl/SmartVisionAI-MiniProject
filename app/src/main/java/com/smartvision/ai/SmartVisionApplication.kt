package com.smartvision.ai

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartVisionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase initializes automatically via google-services.json
        // If missing, app still runs with local features
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.w("SVA", "Firebase init skipped: ${e.message}")
        }
    }
}
