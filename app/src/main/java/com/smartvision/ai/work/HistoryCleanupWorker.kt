package com.smartvision.ai.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartvision.ai.data.db.AppDatabase
import com.smartvision.ai.data.preferences.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class HistoryCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CleanupWorkerEntryPoint {
        fun database(): AppDatabase
        fun settingsRepository(): SettingsRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            CleanupWorkerEntryPoint::class.java
        )
        val database = entryPoint.database()
        val settingsRepo = entryPoint.settingsRepository()

        // 1. Fetch auto-delete configurations from SettingsRepository
        val period = settingsRepo.autoDeletePeriod.first()
        if (period == "NEVER") {
            return Result.success()
        }

        val deleteOcr = settingsRepo.autoDeleteOcr.first()
        val deleteHomework = settingsRepo.autoDeleteHomework.first()
        val deleteQr = settingsRepo.autoDeleteQr.first()
        val deleteObject = settingsRepo.autoDeleteObject.first()
        val deleteMedicine = settingsRepo.autoDeleteMedicine.first()
        val deleteTranslation = settingsRepo.autoDeleteTranslation.first()

        // 2. Calculate the cutoff timestamp in milliseconds
        val now = System.currentTimeMillis()
        val cutoffDurationMillis = when (period) {
            "3_DAYS" -> TimeUnit.DAYS.toMillis(3)
            "1_WEEK" -> TimeUnit.DAYS.toMillis(7)
            "1_MONTH" -> TimeUnit.DAYS.toMillis(30)
            else -> return Result.success()
        }
        val cutoffTimestamp = now - cutoffDurationMillis

        // 3. Compile the list of categories and their corresponding DB type names
        val typesToDelete = mutableListOf<String>()

        if (deleteOcr) {
            typesToDelete.add("OCR")
        }
        if (deleteHomework) {
            typesToDelete.add("Study")
            typesToDelete.add("Homework")
        }
        if (deleteQr) {
            typesToDelete.add("QR Scan")
            typesToDelete.add("QR")
        }
        if (deleteObject) {
            typesToDelete.add("Object")
            typesToDelete.add("Object Detector")
        }
        if (deleteMedicine) {
            typesToDelete.add("Medicine")
        }
        if (deleteTranslation) {
            typesToDelete.add("Translation")
            typesToDelete.add("Voice")
        }

        // 4. Perform Room database deletion queries if there are types to delete
        if (typesToDelete.isNotEmpty()) {
            database.historyDao().deleteByTypesOlderThan(typesToDelete, cutoffTimestamp)
        }

        return Result.success()
    }
}
