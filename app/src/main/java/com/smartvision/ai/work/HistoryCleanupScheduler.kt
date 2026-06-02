package com.smartvision.ai.work

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object HistoryCleanupScheduler {
    private const val WORK_NAME = "HistoryCleanupWork"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<HistoryCleanupWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }
}
