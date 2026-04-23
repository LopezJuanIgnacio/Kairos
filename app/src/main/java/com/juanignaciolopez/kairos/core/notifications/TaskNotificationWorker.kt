package com.juanignaciolopez.kairos.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.juanignaciolopez.kairos.MainActivity
import com.juanignaciolopez.kairos.R
import com.juanignaciolopez.kairos.data.models.TaskCategory

class TaskNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        ensureNotificationChannel(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val taskId = inputData.getString(KEY_TASK_ID).orEmpty()
        val title = inputData.getString(KEY_TASK_TITLE).orEmpty()
        val categoryRaw = inputData.getString(KEY_TASK_CATEGORY).orEmpty()
        val category = runCatching { TaskCategory.valueOf(categoryRaw) }
            .getOrDefault(TaskCategory.ACTIONABLE)

        if (taskId.isBlank() || title.isBlank()) return Result.success()

        val notificationId = taskId.hashCode()
        val contentText = categoryReminderText(category)
        val launcherBitmap = BitmapFactory.decodeResource(
            applicationContext.resources,
            R.mipmap.ic_launcher_round
        )

        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(launcherBitmap)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)

        val summaryNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(launcherBitmap)
            .setContentTitle(applicationContext.getString(R.string.notification_summary_title))
            .setContentText(applicationContext.getString(R.string.notification_summary_text))
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine(title)
                    .setSummaryText(applicationContext.getString(R.string.notification_summary_inbox_text))
            )
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(SUMMARY_NOTIFICATION_ID, summaryNotification)

        return Result.success()
    }

    private fun categoryReminderText(category: TaskCategory): String = when (category) {
        TaskCategory.RECURRENT,
        TaskCategory.ACTIONABLE -> applicationContext.getString(R.string.notification_reminder_daily)

        TaskCategory.SHORT_TERM -> applicationContext.getString(R.string.notification_reminder_short_term)
        TaskCategory.LONG_TERM -> applicationContext.getString(R.string.notification_reminder_long_term)
        TaskCategory.INCUBATOR -> applicationContext.getString(R.string.notification_reminder_incubator)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existingChannel = manager.getNotificationChannel(CHANNEL_ID)
        if (existingChannel != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "kairos_task_reminders"
        const val GROUP_KEY = "kairos_task_reminders_group"
        const val SUMMARY_NOTIFICATION_ID = 100_000

        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_TASK_CATEGORY = "task_category"

        fun inputData(taskId: String, taskTitle: String, category: TaskCategory): Data {
            return Data.Builder()
                .putString(KEY_TASK_ID, taskId)
                .putString(KEY_TASK_TITLE, taskTitle)
                .putString(KEY_TASK_CATEGORY, category.name)
                .build()
        }
    }
}
