package com.classsync.app.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.classsync.app.domain.repository.PreferencesRepository
import com.classsync.app.domain.repository.ScheduleRepository
import com.classsync.app.domain.time.ScheduleCalculator
import com.classsync.app.domain.time.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class AndroidReminderScheduler @Inject constructor(
    @ApplicationContext context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val timeProvider: TimeProvider,
) : ReminderScheduler {
    private val workManager = WorkManager.getInstance(context)

    override suspend fun schedule(scheduleId: Long) = enqueue(scheduleId, ExistingWorkPolicy.REPLACE)

    override suspend fun scheduleNext(scheduleId: Long) = enqueue(scheduleId, ExistingWorkPolicy.APPEND_OR_REPLACE)

    private suspend fun enqueue(scheduleId: Long, policy: ExistingWorkPolicy) {
        val entry = scheduleRepository.getEntry(scheduleId)
        val preferences = preferencesRepository.preferences.first()
        if (
            entry == null ||
            entry.schedule.mode != preferences.selectedMode ||
            !entry.schedule.reminderEnabled ||
            !preferences.remindersEnabled
        ) {
            cancel(scheduleId)
            return
        }
        val now = timeProvider.now()
        val reminderAt = ScheduleCalculator.nextReminderAt(entry, now)
        if (reminderAt == null) {
            cancel(scheduleId)
            return
        }
        val delayMillis = Duration.between(now, reminderAt).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(ReminderWorker.ScheduleIdKey, scheduleId)
                    .putLong(
                        ReminderWorker.OccurrenceStartKey,
                        reminderAt.plusMinutes(entry.schedule.reminderMinutes.toLong()).toInstant().toEpochMilli(),
                    )
                    .build(),
            )
            .addTag(ReminderWorkTag)
            .addTag(workName(scheduleId))
            .build()
        workManager.enqueueUniqueWork(workName(scheduleId), policy, request)
    }

    override suspend fun cancel(scheduleId: Long) {
        workManager.cancelUniqueWork(workName(scheduleId))
    }

    override suspend fun rescheduleAll() {
        if (!preferencesRepository.preferences.first().remindersEnabled) {
            cancelAll()
            return
        }
        scheduleRepository.getAllEntries().forEach { schedule(it.schedule.id) }
    }

    override suspend fun cancelAll() {
        workManager.cancelAllWorkByTag(ReminderWorkTag)
    }

    private fun workName(scheduleId: Long) = "class_reminder_$scheduleId"

    companion object {
        const val ReminderWorkTag = "class_reminders"
    }
}
