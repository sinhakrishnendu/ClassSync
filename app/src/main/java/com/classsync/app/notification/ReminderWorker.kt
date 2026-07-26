package com.classsync.app.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.classsync.app.MainActivity
import com.classsync.app.R
import com.classsync.app.domain.model.TimeFormat
import com.classsync.app.domain.repository.PreferencesRepository
import com.classsync.app.domain.repository.ScheduleRepository
import com.classsync.app.domain.time.ScheduleCalculator
import com.classsync.app.domain.time.TimeProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.format.DateTimeFormatter
import java.time.Instant
import kotlin.math.max
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
    private val timeProvider: TimeProvider,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val scheduleId = inputData.getLong(ScheduleIdKey, 0)
        val occurrenceStartMillis = inputData.getLong(OccurrenceStartKey, 0)
        if (scheduleId <= 0 || occurrenceStartMillis <= 0) return Result.failure()
        return try {
            val entry = scheduleRepository.getEntry(scheduleId) ?: return Result.success()
            val preferences = preferencesRepository.preferences.first()
            val now = timeProvider.now()
            val plannedStart = Instant.ofEpochMilli(occurrenceStartMillis).atZone(now.zone)
            val occurrence = ScheduleCalculator.occurrenceOn(entry, plannedStart.toLocalDate(), now.zone)
            val validOccurrence = occurrence?.takeIf {
                it.isActive &&
                    it.start.toInstant().toEpochMilli() == occurrenceStartMillis &&
                    now.isBefore(it.start)
            }
            if (
                preferences.remindersEnabled &&
                entry.schedule.mode == preferences.selectedMode &&
                entry.schedule.reminderEnabled &&
                validOccurrence != null &&
                canPostNotifications()
            ) {
                val formatter = when (preferences.timeFormat) {
                    TimeFormat.TWELVE_HOUR -> DateTimeFormatter.ofPattern("h:mm a")
                    TimeFormat.TWENTY_FOUR_HOUR -> DateTimeFormatter.ofPattern("HH:mm")
                    TimeFormat.SYSTEM -> if (android.text.format.DateFormat.is24HourFormat(context)) {
                        DateTimeFormatter.ofPattern("HH:mm")
                    } else {
                        DateTimeFormatter.ofPattern("h:mm a")
                    }
                }
                val detail = buildList {
                    add(entry.group.displayName)
                    add(validOccurrence.start.toLocalTime().format(formatter))
                    entry.schedule.classroom?.let(::add)
                }.joinToString(" | ")
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("classsync://schedule/$scheduleId")
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    scheduleId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val secondsUntil = java.time.Duration.between(now, validOccurrence.start).seconds
                val minutesUntil = max(1, (secondsUntil + 59) / 60)
                val notification = NotificationCompat.Builder(context, NotificationChannels.ClassReminders)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notification_title, minutesUntil))
                    .setContentText(entry.subject.name)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("${entry.subject.name}\n$detail"))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
                NotificationManagerCompat.from(context).notify(scheduleId.notificationId(), notification)
            }
            runCatching { reminderScheduler.scheduleNext(scheduleId) }
                .onFailure { Log.e(LogTag, "Unable to schedule the next class reminder", it) }
            Result.success()
        } catch (error: Exception) {
            Log.e(LogTag, "Class reminder worker failed", error)
            Result.retry()
        }
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun Long.notificationId(): Int = (this xor (this ushr 32)).toInt() and Int.MAX_VALUE

    companion object {
        const val ScheduleIdKey = "schedule_id"
        const val OccurrenceStartKey = "occurrence_start"
        private const val LogTag = "ClassSyncReminder"
    }
}
