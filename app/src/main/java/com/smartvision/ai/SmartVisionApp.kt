package com.smartvision.ai

import android.app.Application
import com.smartvision.ai.work.HistoryCleanupScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartVisionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        HistoryCleanupScheduler.schedule(this)
    }
}
