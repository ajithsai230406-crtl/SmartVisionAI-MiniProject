package com.smartvision.ai

import android.app.Application
import android.content.Intent
import com.smartvision.ai.work.HistoryCleanupScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlin.system.exitProcess

@HiltAndroidApp
class SmartVisionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.smartvision.ai.work.HistoryCleanupScheduler.schedule(this)
        com.smartvision.ai.work.SmartNotificationScheduler.schedule(this)
        com.smartvision.ai.utils.AnalyticsHelper.init(this)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("SmartVisionApp", "Fatal uncaught crash on thread: ${thread.name}", throwable)
            com.smartvision.ai.utils.AnalyticsHelper.logException(throwable)
            try {
                val intent = Intent(this, CrashReportActivity::class.java).apply {
                    putExtra("error_msg", throwable.localizedMessage ?: throwable.toString())
                    putExtra("stack_trace", throwable.stackTraceToString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("SmartVisionApp", "Failed to launch crash recovery activity", e)
            }
            kotlin.system.exitProcess(2)
        }
    }
}
