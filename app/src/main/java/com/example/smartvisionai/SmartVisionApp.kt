package com.example.smartvisionai

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartVisionApp : Application() {

    override fun onCreate() {
        // Wrap in try-catch so a Hilt init failure
        // doesn't silently kill the process
        try {
            super.onCreate()
        } catch (e: Exception) {
            android.util.Log.e("SVA_APP", "App init failed: ${e.message}", e)
        }
    }
}
