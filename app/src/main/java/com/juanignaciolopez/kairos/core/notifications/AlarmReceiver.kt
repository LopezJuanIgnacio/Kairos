package com.juanignaciolopez.kairos.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.app.PendingIntent
import com.juanignaciolopez.kairos.MainActivity
import com.juanignaciolopez.kairos.R
import com.juanignaciolopez.kairos.data.models.TaskCategory

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val taskId = intent.getStringExtra(TaskNotificationWorker.KEY_TASK_ID).orEmpty()
        val title = intent.getStringExtra(TaskNotificationWorker.KEY_TASK_TITLE).orEmpty()
        val categoryRaw = intent.getStringExtra(TaskNotificationWorker.KEY_TASK_CATEGORY).orEmpty()
        val category = runCatching { TaskCategory.valueOf(categoryRaw) }
            .getOrDefault(TaskCategory.ACTIONABLE)

        if (taskId.isBlank() || title.isBlank()) return

        val notificationId = taskId.hashCode()
        val contentText = categoryReminderText(context, category)
        val launcherBitmap = BitmapFactory.decodeResource(
            context.resources,
            R.mipmap.ic_launcher_round
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, TaskNotificationWorker.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(launcherBitmap)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(TaskNotificationWorker.GROUP_KEY)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)

        val summaryNotification = NotificationCompat.Builder(context, TaskNotificationWorker.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(launcherBitmap)
            .setContentTitle(context.getString(R.string.notification_summary_title))
            .setContentText(context.getString(R.string.notification_summary_text))
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine(title)
                    .setSummaryText(context.getString(R.string.notification_summary_inbox_text))
            )
            .setGroup(TaskNotificationWorker.GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(TaskNotificationWorker.SUMMARY_NOTIFICATION_ID, summaryNotification)
    }

    private fun categoryReminderText(context: Context, category: TaskCategory): String = when (category) {
        TaskCategory.RECURRENT,
        TaskCategory.ACTIONABLE -> context.getString(R.string.notification_reminder_daily)

        TaskCategory.SHORT_TERM -> context.getString(R.string.notification_reminder_short_term)
        TaskCategory.LONG_TERM -> context.getString(R.string.notification_reminder_long_term)
        TaskCategory.INCUBATOR -> context.getString(R.string.notification_reminder_incubator)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val existingChannel = manager.getNotificationChannel(TaskNotificationWorker.CHANNEL_ID)
        if (existingChannel != null) return

        val channel = android.app.NotificationChannel(
            TaskNotificationWorker.CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
