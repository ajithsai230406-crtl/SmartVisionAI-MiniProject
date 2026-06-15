package com.smartvision.ai.work

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object SmartNotificationScheduler {
    private const val WORK_NAME = "SmartNotificationWork"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val notificationRequest = PeriodicWorkRequestBuilder<SmartNotificationWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            notificationRequest
        )
    }
}
