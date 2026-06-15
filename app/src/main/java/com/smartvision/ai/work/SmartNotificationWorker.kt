package com.smartvision.ai.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartvision.ai.data.db.AppDatabase
import com.smartvision.ai.data.preferences.SettingsRepository
import com.smartvision.ai.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Calendar

class SmartNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "sv_recommendations"
        private const val CHANNEL_NAME = "Smart Recommendations"
        private const val NOTIFICATION_ID = 9081
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationWorkerEntryPoint {
        fun database(): AppDatabase
        fun settingsRepository(): SettingsRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationWorkerEntryPoint::class.java
        )
        val database = entryPoint.database()
        val settingsRepo = entryPoint.settingsRepository()

        // 1. Check if notifications are globally enabled
        val notifEnabled = settingsRepo.notifEnabled.first()
        val recEnabled = settingsRepo.recommendationsEnabled.first()
        if (!notifEnabled || !recEnabled) {
            return Result.success()
        }

        // 2. Check Quiet Hours
        val quietHours = settingsRepo.quietHoursEnabled.first()
        if (quietHours) {
            val start = settingsRepo.quietHoursStart.first()
            val end = settingsRepo.quietHoursEnd.first()
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

            val inQuietHours = if (start <= end) {
                currentHour in start until end
            } else {
                currentHour >= start || currentHour < end
            }

            if (inQuietHours) {
                // Silently return success to avoid bothering the user
                return Result.success()
            }
        }

        // 3. Retrieve local history
        val userId = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "guest_uid"
        } catch (_: Exception) {
            "guest_uid"
        }
        val history = database.historyDao().getAllHistorySync(userId)
        val now = System.currentTimeMillis()

        val ocrLast = history.firstOrNull { it.type == "OCR" || it.type == "Translation" }?.createdAt ?: 0L
        val homeworkLast = history.firstOrNull { it.type == "Study" || it.type == "Homework" }?.createdAt ?: 0L
        val wasteLast = history.firstOrNull { it.type == "Waste" }?.createdAt ?: 0L
        val medLast = history.firstOrNull { it.type == "Medicine" }?.createdAt ?: 0L

        // 4. Smart recommendation engine
        val (title, body) = when {
            now - homeworkLast > 2 * 24 * 3600 * 1000L -> {
                "📚 Stuck on Homework?" to "Need help solving a math or science problem? Let the AI Student Helper guide you!"
            }
            now - ocrLast > 3 * 24 * 3600 * 1000L -> {
                "📝 Quick Text Translator" to "Scan text to extract or translate instantly. Try OCR Translator mode!"
            }
            medLast == 0L -> {
                "💊 Scan Medicine Info" to "Keep track of dosage and safety guidelines. Open Medicine Scanner now."
            }
            now - wasteLast > 5 * 24 * 3600 * 1000L -> {
                "♻️ Smart Waste Classifier" to "Not sure if an item is recyclable? Scan it using Waste Classifier!"
            }
            else -> {
                "🔍 Explore Smart Vision" to "Your AI assistant is ready to help. Open the app to scan objects or documents."
            }
        }

        // 5. Build and fire the notification
        val context = applicationContext
        createNotificationChannel(context)

        // PendingIntent to launch MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            // Check for POST_NOTIFICATIONS permission on Android 13+ (handled at runtime, safe to trigger notify)
            manager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Gracefully ignore if user revoked runtime permission
        }

        return Result.success()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Intelligent reminders based on module usage"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
