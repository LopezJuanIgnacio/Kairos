package com.juanignaciolopez.kairos.core.notifications

import android.content.Context
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.models.TaskCategory
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object TaskNotificationScheduler {

    private const val PREFS_NAME = "kairos_notifications"
    private const val PREF_KEY_SCHEDULED_TASK_IDS = "scheduled_task_ids"

    fun syncNotifications(context: Context, activeTasks: List<Task>) {
        val workManager = WorkManager.getInstance(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val currentTaskIds = activeTasks.map { it.id }.toSet()
        val previousTaskIds = prefs.getStringSet(PREF_KEY_SCHEDULED_TASK_IDS, emptySet()).orEmpty()

        // Cancel work for tasks no longer active.
        previousTaskIds
            .filter { it !in currentTaskIds }
            .forEach { cancelAllTaskNotificationWork(workManager, it) }

        activeTasks.forEach { task ->
            scheduleTaskNotifications(workManager, task)
        }

        prefs.edit().putStringSet(PREF_KEY_SCHEDULED_TASK_IDS, currentTaskIds).apply()
    }

    private fun scheduleTaskNotifications(workManager: WorkManager, task: Task) {
        when (task.category) {
            TaskCategory.RECURRENT,
            TaskCategory.ACTIONABLE -> {
                cancelLeadNotifications(workManager, task.id)
                scheduleDailyNotification(workManager, task)
            }

            TaskCategory.SHORT_TERM -> {
                cancelDailyNotification(workManager, task.id)
                scheduleDueDateLeadNotification(workManager, task, leadDays = 1)
            }

            TaskCategory.LONG_TERM -> {
                cancelDailyNotification(workManager, task.id)
                scheduleDueDateLeadNotification(workManager, task, leadDays = 7)
            }

            TaskCategory.INCUBATOR -> {
                cancelAllTaskNotificationWork(workManager, task.id)
            }
        }
    }

    private fun scheduleDailyNotification(workManager: WorkManager, task: Task) {
        val request = PeriodicWorkRequestBuilder<TaskNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNextDailyReminderMillis(task.dueDate), TimeUnit.MILLISECONDS)
            .setInputData(TaskNotificationWorker.inputData(task.id, task.title, task.category))
            .addTag(tagForTask(task.id))
            .build()

        workManager.enqueueUniquePeriodicWork(
            dailyWorkName(task.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun scheduleDueDateLeadNotification(workManager: WorkManager, task: Task, leadDays: Int) {
        val dueDate = task.dueDate ?: run {
            cancelLeadNotifications(workManager, task.id)
            return
        }

        val triggerAt = dueDate - TimeUnit.DAYS.toMillis(leadDays.toLong())
        val delay = (triggerAt - System.currentTimeMillis()).coerceAtLeast(0L)

        val request = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(TaskNotificationWorker.inputData(task.id, task.title, task.category))
            .addTag(tagForTask(task.id))
            .build()

        workManager.enqueueUniqueWork(
            leadWorkName(task.id, leadDays),
            ExistingWorkPolicy.REPLACE,
            request
        )

        // Ensure only one lead rule remains active for each task.
        if (leadDays == 1) {
            workManager.cancelUniqueWork(leadWorkName(task.id, 7))
        } else if (leadDays == 7) {
            workManager.cancelUniqueWork(leadWorkName(task.id, 1))
        }
    }

    private fun cancelAllTaskNotificationWork(workManager: WorkManager, taskId: String) {
        cancelDailyNotification(workManager, taskId)
        cancelLeadNotifications(workManager, taskId)
    }

    private fun cancelDailyNotification(workManager: WorkManager, taskId: String) {
        workManager.cancelUniqueWork(dailyWorkName(taskId))
    }

    private fun cancelLeadNotifications(workManager: WorkManager, taskId: String) {
        workManager.cancelUniqueWork(leadWorkName(taskId, 1))
        workManager.cancelUniqueWork(leadWorkName(taskId, 7))
    }

    private fun delayUntilNextDailyReminderMillis(reminderAt: Long?): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis

            val reminderCalendar = Calendar.getInstance().apply {
                if (reminderAt != null) {
                    timeInMillis = reminderAt
                }
            }

            set(Calendar.HOUR_OF_DAY, reminderCalendar.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, reminderCalendar.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return next.timeInMillis - now.timeInMillis
    }

    private fun dailyWorkName(taskId: String): String = "task_daily_$taskId"

    private fun leadWorkName(taskId: String, leadDays: Int): String = "task_due_${leadDays}d_$taskId"

    private fun tagForTask(taskId: String): String = "task_notification_$taskId"
}
