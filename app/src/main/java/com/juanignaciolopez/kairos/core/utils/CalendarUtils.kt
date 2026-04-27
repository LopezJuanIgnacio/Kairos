package com.juanignaciolopez.kairos.core.utils

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.juanignaciolopez.kairos.data.models.Task
import java.util.TimeZone

data class CalendarExportResult(
    val exportedCount: Int,
    val total: Int
)

val CALENDAR_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CALENDAR,
    Manifest.permission.WRITE_CALENDAR
)

fun <T> handleCalendarExportRequest(
    context: Context,
    request: T,
    onReady: (T) -> Unit,
    onNeedPermission: (T) -> Unit
) {
    if (hasCalendarPermissions(context)) {
        onReady(request)
    } else {
        onNeedPermission(request)
    }
}

fun hasCalendarPermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
}

fun exportTaskToCalendar(context: Context, task: Task): Boolean {
    val startMillis = task.dueDate ?: task.scheduledDate ?: System.currentTimeMillis() + 60 * 60 * 1000
    val endMillis = startMillis + maxOf(task.estimatedMinutes, 30) * 60 * 1000L

    val intent = buildCalendarIntentForTask(context, task, startMillis, endMillis)

    return runCatching {
        context.startActivity(intent)
        true
    }.getOrElse {
        it is ActivityNotFoundException
        false
    }
}

fun exportTasksDirectlyToCalendar(
    context: Context,
    tasks: List<Task>,
    onTaskExported: (Task) -> Unit
): CalendarExportResult {
    if (tasks.isEmpty()) return CalendarExportResult(exportedCount = 0, total = 0)

    val calendarId = getWritableCalendarId(context)
        ?: return CalendarExportResult(exportedCount = 0, total = tasks.size)

    var exportedCount = 0
    tasks.forEach { task ->
        if (insertTaskEventIntoCalendar(context, calendarId, task)) {
            onTaskExported(task)
            exportedCount += 1
        }
    }

    return CalendarExportResult(
        exportedCount = exportedCount,
        total = tasks.size
    )
}

private fun getWritableCalendarId(context: Context): Long? {
    val projection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.IS_PRIMARY,
        CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        CalendarContract.Calendars.VISIBLE,
        CalendarContract.Calendars.SYNC_EVENTS
    )

    val selection = (
        "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
            "${CalendarContract.Calendars.SYNC_EVENTS} = 1 AND " +
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_EDITOR}"
        )

    val sort = "${CalendarContract.Calendars.IS_PRIMARY} DESC"

    return runCatching {
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            sort
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            } else {
                null
            }
        }
    }.getOrNull()
}

private fun insertTaskEventIntoCalendar(
    context: Context,
    calendarId: Long,
    task: Task
): Boolean {
    val startMillis = task.dueDate ?: task.scheduledDate ?: System.currentTimeMillis() + 60 * 60 * 1000
    val endMillis = startMillis + maxOf(task.estimatedMinutes, 30) * 60 * 1000L
    val categoryLabel = EnumUtils.categoryToString(context, task.category)

    val eventDescription = buildString {
        if (task.description.isNotBlank()) {
            append(task.description)
        }
        if (isNotEmpty()) {
            append("\n\n")
        }
        append(context.getString(com.juanignaciolopez.kairos.R.string.dashboard_calendar_event_category_label))
        append(categoryLabel)
    }

    val values = ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.TITLE, task.title)
        put(CalendarContract.Events.DESCRIPTION, eventDescription)
        put(CalendarContract.Events.DTSTART, startMillis)
        put(CalendarContract.Events.DTEND, endMillis)
        put(CalendarContract.Events.ALL_DAY, 0)
        put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
        put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
    }

    return runCatching {
        val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: return@runCatching false

        context.contentResolver.query(
            eventUri,
            arrayOf(CalendarContract.Events._ID),
            null,
            null,
            null
        )?.use { cursor ->
            cursor.moveToFirst()
        } == true
    }.getOrDefault(false)
}

private fun buildCalendarIntentForTask(context: Context, task: Task, startMillis: Long, endMillis: Long): Intent {
    val categoryLabel = EnumUtils.categoryToString(context, task.category)
    val eventDescription = buildString {
        if (task.description.isNotBlank()) {
            append(task.description)
        }
        if (isNotEmpty()) {
            append("\n\n")
        }
        append(context.getString(com.juanignaciolopez.kairos.R.string.dashboard_calendar_event_category_label))
        append(categoryLabel)
    }

    return Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, task.title)
        putExtra(CalendarContract.Events.DESCRIPTION, eventDescription)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
    }
}